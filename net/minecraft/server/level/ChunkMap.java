package net.minecraft.server.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkMap extends ChunkStorage implements ChunkHolder.PlayerProvider {
   private static final byte CHUNK_TYPE_REPLACEABLE = -1;
   private static final byte CHUNK_TYPE_UNKNOWN = 0;
   private static final byte CHUNK_TYPE_FULL = 1;
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int CHUNK_SAVED_PER_TICK = 200;
   private static final int MIN_VIEW_DISTANCE = 3;
   public static final int MAX_VIEW_DISTANCE = 33;
   public static final int MAX_CHUNK_DISTANCE = 33 + ChunkStatus.maxDistance();
   public static final int FORCED_TICKET_LEVEL = 31;
   private final Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingChunkMap = new Long2ObjectLinkedOpenHashMap();
   private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;
   private final Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads;
   private final LongSet entitiesInLevel;
   final ServerLevel level;
   private final ThreadedLevelLightEngine lightEngine;
   private final BlockableEventLoop<Runnable> mainThreadExecutor;
   private final ChunkGenerator generator;
   private final Supplier<DimensionDataStorage> overworldDataStorage;
   private final PoiManager poiManager;
   final LongSet toDrop;
   private boolean modified;
   private final ChunkTaskPriorityQueueSorter queueSorter;
   private final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> worldgenMailbox;
   private final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> mainThreadMailbox;
   private final ChunkProgressListener progressListener;
   private final ChunkStatusUpdateListener chunkStatusListener;
   private final ChunkMap.DistanceManager distanceManager;
   private final AtomicInteger tickingGenerated;
   private final StructureManager structureManager;
   private final File storageFolder;
   private final PlayerMap playerMap;
   private final Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;
   private final Long2ByteMap chunkTypeCache;
   private final Queue<Runnable> unloadQueue;
   int viewDistance;

   public ChunkMap(ServerLevel var1, LevelStorageSource.LevelStorageAccess var2, DataFixer var3, StructureManager var4, Executor var5, BlockableEventLoop<Runnable> var6, LightChunkGetter var7, ChunkGenerator var8, ChunkProgressListener var9, ChunkStatusUpdateListener var10, Supplier<DimensionDataStorage> var11, int var12, boolean var13) {
      super(new File(var2.getDimensionPath(var1.dimension()), "region"), var3, var13);
      this.visibleChunkMap = this.updatingChunkMap.clone();
      this.pendingUnloads = new Long2ObjectLinkedOpenHashMap();
      this.entitiesInLevel = new LongOpenHashSet();
      this.toDrop = new LongOpenHashSet();
      this.tickingGenerated = new AtomicInteger();
      this.playerMap = new PlayerMap();
      this.entityMap = new Int2ObjectOpenHashMap();
      this.chunkTypeCache = new Long2ByteOpenHashMap();
      this.unloadQueue = Queues.newConcurrentLinkedQueue();
      this.structureManager = var4;
      this.storageFolder = var2.getDimensionPath(var1.dimension());
      this.level = var1;
      this.generator = var8;
      this.mainThreadExecutor = var6;
      ProcessorMailbox var14 = ProcessorMailbox.create(var5, "worldgen");
      Objects.requireNonNull(var6);
      ProcessorHandle var15 = ProcessorHandle.of("main", var6::tell);
      this.progressListener = var9;
      this.chunkStatusListener = var10;
      ProcessorMailbox var16 = ProcessorMailbox.create(var5, "light");
      this.queueSorter = new ChunkTaskPriorityQueueSorter(ImmutableList.of(var14, var15, var16), var5, Integer.MAX_VALUE);
      this.worldgenMailbox = this.queueSorter.getProcessor(var14, false);
      this.mainThreadMailbox = this.queueSorter.getProcessor(var15, false);
      this.lightEngine = new ThreadedLevelLightEngine(var7, this, this.level.dimensionType().hasSkyLight(), var16, this.queueSorter.getProcessor(var16, false));
      this.distanceManager = new ChunkMap.DistanceManager(var5, var6);
      this.overworldDataStorage = var11;
      this.poiManager = new PoiManager(new File(this.storageFolder, "poi"), var3, var13, var1);
      this.setViewDistance(var12);
   }

   private static double euclideanDistanceSquared(ChunkPos var0, Entity var1) {
      double var2 = (double)SectionPos.sectionToBlockCoord(var0.x, 8);
      double var4 = (double)SectionPos.sectionToBlockCoord(var0.z, 8);
      double var6 = var2 - var1.getX();
      double var8 = var4 - var1.getZ();
      return var6 * var6 + var8 * var8;
   }

   private static int checkerboardDistance(ChunkPos var0, ServerPlayer var1, boolean var2) {
      int var3;
      int var4;
      if (var2) {
         SectionPos var5 = var1.getLastSectionPos();
         var3 = var5.x();
         var4 = var5.z();
      } else {
         var3 = SectionPos.blockToSectionCoord(var1.getBlockX());
         var4 = SectionPos.blockToSectionCoord(var1.getBlockZ());
      }

      return checkerboardDistance(var0, var3, var4);
   }

   private static int checkerboardDistance(ChunkPos var0, Entity var1) {
      return checkerboardDistance(var0, SectionPos.blockToSectionCoord(var1.getBlockX()), SectionPos.blockToSectionCoord(var1.getBlockZ()));
   }

   private static int checkerboardDistance(ChunkPos var0, int var1, int var2) {
      int var3 = var0.x - var1;
      int var4 = var0.z - var2;
      return Math.max(Math.abs(var3), Math.abs(var4));
   }

   protected ThreadedLevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   @Nullable
   protected ChunkHolder getUpdatingChunkIfPresent(long var1) {
      return (ChunkHolder)this.updatingChunkMap.get(var1);
   }

   @Nullable
   protected ChunkHolder getVisibleChunkIfPresent(long var1) {
      return (ChunkHolder)this.visibleChunkMap.get(var1);
   }

   protected IntSupplier getChunkQueueLevel(long var1) {
      return () -> {
         ChunkHolder var3 = this.getVisibleChunkIfPresent(var1);
         return var3 == null ? ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1 : Math.min(var3.getQueueLevel(), ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
      };
   }

   public String getChunkDebugData(ChunkPos var1) {
      ChunkHolder var2 = this.getVisibleChunkIfPresent(var1.toLong());
      if (var2 == null) {
         return "null";
      } else {
         String var3 = var2.getTicketLevel() + "\n";
         ChunkStatus var4 = var2.getLastAvailableStatus();
         ChunkAccess var5 = var2.getLastAvailable();
         if (var4 != null) {
            var3 = var3 + "St: §" + var4.getIndex() + var4 + "§r\n";
         }

         if (var5 != null) {
            var3 = var3 + "Ch: §" + var5.getStatus().getIndex() + var5.getStatus() + "§r\n";
         }

         ChunkHolder.FullChunkStatus var6 = var2.getFullStatus();
         var3 = var3 + "§" + var6.ordinal() + var6;
         return var3 + "§r";
      }
   }

   private CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> getChunkRangeFuture(ChunkPos var1, int var2, IntFunction<ChunkStatus> var3) {
      ArrayList var4 = Lists.newArrayList();
      int var5 = var1.x;
      int var6 = var1.z;

      for(int var7 = -var2; var7 <= var2; ++var7) {
         for(int var8 = -var2; var8 <= var2; ++var8) {
            int var9 = Math.max(Math.abs(var8), Math.abs(var7));
            final ChunkPos var10 = new ChunkPos(var5 + var8, var6 + var7);
            long var11 = var10.toLong();
            ChunkHolder var13 = this.getUpdatingChunkIfPresent(var11);
            if (var13 == null) {
               return CompletableFuture.completedFuture(Either.right(new ChunkHolder.ChunkLoadingFailure() {
                  public String toString() {
                     return "Unloaded " + var10;
                  }
               }));
            }

            ChunkStatus var14 = (ChunkStatus)var3.apply(var9);
            CompletableFuture var15 = var13.getOrScheduleFuture(var14, this);
            var4.add(var15);
         }
      }

      CompletableFuture var16 = Util.sequence(var4);
      return var16.thenApply((var4x) -> {
         ArrayList var5x = Lists.newArrayList();
         final int var6x = 0;

         for(Iterator var7 = var4x.iterator(); var7.hasNext(); ++var6x) {
            final Either var8 = (Either)var7.next();
            Optional var9 = var8.left();
            if (!var9.isPresent()) {
               return Either.right(new ChunkHolder.ChunkLoadingFailure() {
                  public String toString() {
                     ChunkPos var10000 = new ChunkPos(var1 + var6 % (var2 * 2 + 1), var3 + var6 / (var2 * 2 + 1));
                     return "Unloaded " + var10000 + " " + var8.right().get();
                  }
               });
            }

            var5x.add((ChunkAccess)var9.get());
         }

         return Either.left(var5x);
      });
   }

   public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareEntityTickingChunk(ChunkPos var1) {
      return this.getChunkRangeFuture(var1, 2, (var0) -> {
         return ChunkStatus.FULL;
      }).thenApplyAsync((var0) -> {
         return var0.mapLeft((var0x) -> {
            return (LevelChunk)var0x.get(var0x.size() / 2);
         });
      }, this.mainThreadExecutor);
   }

   @Nullable
   ChunkHolder updateChunkScheduling(long var1, int var3, @Nullable ChunkHolder var4, int var5) {
      if (var5 > MAX_CHUNK_DISTANCE && var3 > MAX_CHUNK_DISTANCE) {
         return var4;
      } else {
         if (var4 != null) {
            var4.setTicketLevel(var3);
         }

         if (var4 != null) {
            if (var3 > MAX_CHUNK_DISTANCE) {
               this.toDrop.add(var1);
            } else {
               this.toDrop.remove(var1);
            }
         }

         if (var3 <= MAX_CHUNK_DISTANCE && var4 == null) {
            var4 = (ChunkHolder)this.pendingUnloads.remove(var1);
            if (var4 != null) {
               var4.setTicketLevel(var3);
            } else {
               var4 = new ChunkHolder(new ChunkPos(var1), var3, this.level, this.lightEngine, this.queueSorter, this);
            }

            this.updatingChunkMap.put(var1, var4);
            this.modified = true;
         }

         return var4;
      }
   }

   public void close() throws IOException {
      try {
         this.queueSorter.close();
         this.poiManager.close();
      } finally {
         super.close();
      }

   }

   protected void saveAllChunks(boolean var1) {
      if (var1) {
         List var2 = (List)this.visibleChunkMap.values().stream().filter(ChunkHolder::wasAccessibleSinceLastSave).peek(ChunkHolder::refreshAccessibility).collect(Collectors.toList());
         MutableBoolean var3 = new MutableBoolean();

         do {
            var3.setFalse();
            var2.stream().map((var1x) -> {
               CompletableFuture var2;
               do {
                  var2 = var1x.getChunkToSave();
                  BlockableEventLoop var10000 = this.mainThreadExecutor;
                  Objects.requireNonNull(var2);
                  var10000.managedBlock(var2::isDone);
               } while(var2 != var1x.getChunkToSave());

               return (ChunkAccess)var2.join();
            }).filter((var0) -> {
               return var0 instanceof ImposterProtoChunk || var0 instanceof LevelChunk;
            }).filter(this::save).forEach((var1x) -> {
               var3.setTrue();
            });
         } while(var3.isTrue());

         this.processUnloads(() -> {
            return true;
         });
         this.flushWorker();
         LOGGER.info("ThreadedAnvilChunkStorage ({}): All chunks are saved", this.storageFolder.getName());
      } else {
         this.visibleChunkMap.values().stream().filter(ChunkHolder::wasAccessibleSinceLastSave).forEach((var1x) -> {
            ChunkAccess var2 = (ChunkAccess)var1x.getChunkToSave().getNow((Object)null);
            if (var2 instanceof ImposterProtoChunk || var2 instanceof LevelChunk) {
               this.save(var2);
               var1x.refreshAccessibility();
            }

         });
      }

   }

   protected void tick(BooleanSupplier var1) {
      ProfilerFiller var2 = this.level.getProfiler();
      var2.push("poi");
      this.poiManager.tick(var1);
      var2.popPush("chunk_unload");
      if (!this.level.noSave()) {
         this.processUnloads(var1);
      }

      var2.pop();
   }

   private void processUnloads(BooleanSupplier var1) {
      LongIterator var2 = this.toDrop.iterator();

      for(int var3 = 0; var2.hasNext() && (var1.getAsBoolean() || var3 < 200 || this.toDrop.size() > 2000); var2.remove()) {
         long var4 = var2.nextLong();
         ChunkHolder var6 = (ChunkHolder)this.updatingChunkMap.remove(var4);
         if (var6 != null) {
            this.pendingUnloads.put(var4, var6);
            this.modified = true;
            ++var3;
            this.scheduleUnload(var4, var6);
         }
      }

      Runnable var7;
      while((var1.getAsBoolean() || this.unloadQueue.size() > 2000) && (var7 = (Runnable)this.unloadQueue.poll()) != null) {
         var7.run();
      }

   }

   private void scheduleUnload(long var1, ChunkHolder var3) {
      CompletableFuture var4 = var3.getChunkToSave();
      Consumer var10001 = (var5) -> {
         CompletableFuture var6 = var3.getChunkToSave();
         if (var6 != var4) {
            this.scheduleUnload(var1, var3);
         } else {
            if (this.pendingUnloads.remove(var1, var3) && var5 != null) {
               if (var5 instanceof LevelChunk) {
                  ((LevelChunk)var5).setLoaded(false);
               }

               this.save(var5);
               if (this.entitiesInLevel.remove(var1) && var5 instanceof LevelChunk) {
                  LevelChunk var7 = (LevelChunk)var5;
                  this.level.unload(var7);
               }

               this.lightEngine.updateChunkStatus(var5.getPos());
               this.lightEngine.tryScheduleUpdate();
               this.progressListener.onStatusChange(var5.getPos(), (ChunkStatus)null);
            }

         }
      };
      Queue var10002 = this.unloadQueue;
      Objects.requireNonNull(var10002);
      var4.thenAcceptAsync(var10001, var10002::add).whenComplete((var1x, var2) -> {
         if (var2 != null) {
            LOGGER.error("Failed to save chunk {}", var3.getPos(), var2);
         }

      });
   }

   protected boolean promoteChunkMap() {
      if (!this.modified) {
         return false;
      } else {
         this.visibleChunkMap = this.updatingChunkMap.clone();
         this.modified = false;
         return true;
      }
   }

   public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> schedule(ChunkHolder var1, ChunkStatus var2) {
      ChunkPos var3 = var1.getPos();
      if (var2 == ChunkStatus.EMPTY) {
         return this.scheduleChunkLoad(var3);
      } else {
         if (var2 == ChunkStatus.LIGHT) {
            this.distanceManager.addTicket(TicketType.LIGHT, var3, 33 + ChunkStatus.getDistance(ChunkStatus.FEATURES), var3);
         }

         Optional var4 = ((Either)var1.getOrScheduleFuture(var2.getParent(), this).getNow(ChunkHolder.UNLOADED_CHUNK)).left();
         if (var4.isPresent() && ((ChunkAccess)var4.get()).getStatus().isOrAfter(var2)) {
            CompletableFuture var5 = var2.load(this.level, this.structureManager, this.lightEngine, (var2x) -> {
               return this.protoChunkToFullChunk(var1);
            }, (ChunkAccess)var4.get());
            this.progressListener.onStatusChange(var3, var2);
            return var5;
         } else {
            return this.scheduleChunkGeneration(var1, var2);
         }
      }
   }

   private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleChunkLoad(ChunkPos var1) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            this.level.getProfiler().incrementCounter("chunkLoad");
            CompoundTag var2 = this.readChunk(var1);
            if (var2 != null) {
               boolean var7 = var2.contains("Level", 10) && var2.getCompound("Level").contains("Status", 8);
               if (var7) {
                  ProtoChunk var4 = ChunkSerializer.read(this.level, this.structureManager, this.poiManager, var1, var2);
                  this.markPosition(var1, var4.getStatus().getChunkType());
                  return Either.left(var4);
               }

               LOGGER.error("Chunk file at {} is missing level data, skipping", var1);
            }
         } catch (ReportedException var5) {
            Throwable var3 = var5.getCause();
            if (!(var3 instanceof IOException)) {
               this.markPositionReplaceable(var1);
               throw var5;
            }

            LOGGER.error("Couldn't load chunk {}", var1, var3);
         } catch (Exception var6) {
            LOGGER.error("Couldn't load chunk {}", var1, var6);
         }

         this.markPositionReplaceable(var1);
         return Either.left(new ProtoChunk(var1, UpgradeData.EMPTY, this.level));
      }, this.mainThreadExecutor);
   }

   private void markPositionReplaceable(ChunkPos var1) {
      this.chunkTypeCache.put(var1.toLong(), (byte)-1);
   }

   private byte markPosition(ChunkPos var1, ChunkStatus.ChunkType var2) {
      return this.chunkTypeCache.put(var1.toLong(), (byte)(var2 == ChunkStatus.ChunkType.PROTOCHUNK ? -1 : 1));
   }

   private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleChunkGeneration(ChunkHolder var1, ChunkStatus var2) {
      ChunkPos var3 = var1.getPos();
      CompletableFuture var4 = this.getChunkRangeFuture(var3, var2.getRange(), (var2x) -> {
         return this.getDependencyStatus(var2, var2x);
      });
      this.level.getProfiler().incrementCounter(() -> {
         return "chunkGenerate " + var2.getName();
      });
      Executor var5 = (var2x) -> {
         this.worldgenMailbox.tell(ChunkTaskPriorityQueueSorter.message(var1, var2x));
      };
      return var4.thenComposeAsync((var5x) -> {
         return (CompletionStage)var5x.map((var5xx) -> {
            try {
               CompletableFuture var6 = var2.generate(var5, this.level, this.generator, this.structureManager, this.lightEngine, (var2x) -> {
                  return this.protoChunkToFullChunk(var1);
               }, var5xx);
               this.progressListener.onStatusChange(var3, var2);
               return var6;
            } catch (Exception var9) {
               var9.getStackTrace();
               CrashReport var7 = CrashReport.forThrowable(var9, "Exception generating new chunk");
               CrashReportCategory var8 = var7.addCategory("Chunk to be generated");
               var8.setDetail("Location", (Object)String.format("%d,%d", var3.x, var3.z));
               var8.setDetail("Position hash", (Object)ChunkPos.asLong(var3.x, var3.z));
               var8.setDetail("Generator", (Object)this.generator);
               throw new ReportedException(var7);
            }
         }, (var2x) -> {
            this.releaseLightTicket(var3);
            return CompletableFuture.completedFuture(Either.right(var2x));
         });
      }, var5);
   }

   protected void releaseLightTicket(ChunkPos var1) {
      this.mainThreadExecutor.tell(Util.name(() -> {
         this.distanceManager.removeTicket(TicketType.LIGHT, var1, 33 + ChunkStatus.getDistance(ChunkStatus.FEATURES), var1);
      }, () -> {
         return "release light ticket " + var1;
      }));
   }

   private ChunkStatus getDependencyStatus(ChunkStatus var1, int var2) {
      ChunkStatus var3;
      if (var2 == 0) {
         var3 = var1.getParent();
      } else {
         var3 = ChunkStatus.getStatusAroundFullChunk(ChunkStatus.getDistance(var1) + var2);
      }

      return var3;
   }

   private static void postLoadProtoChunk(ServerLevel var0, List<CompoundTag> var1) {
      if (!var1.isEmpty()) {
         var0.addWorldGenChunkEntities(EntityType.loadEntitiesRecursive(var1, var0));
      }

   }

   private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> protoChunkToFullChunk(ChunkHolder var1) {
      CompletableFuture var2 = var1.getFutureIfPresentUnchecked(ChunkStatus.FULL.getParent());
      return var2.thenApplyAsync((var2x) -> {
         ChunkStatus var3 = ChunkHolder.getStatus(var1.getTicketLevel());
         return !var3.isOrAfter(ChunkStatus.FULL) ? ChunkHolder.UNLOADED_CHUNK : var2x.mapLeft((var2) -> {
            ChunkPos var3 = var1.getPos();
            ProtoChunk var4 = (ProtoChunk)var2;
            LevelChunk var5;
            if (var4 instanceof ImposterProtoChunk) {
               var5 = ((ImposterProtoChunk)var4).getWrapped();
            } else {
               var5 = new LevelChunk(this.level, var4, (var2x) -> {
                  postLoadProtoChunk(this.level, var4.getEntities());
               });
               var1.replaceProtoChunk(new ImposterProtoChunk(var5));
            }

            var5.setFullStatus(() -> {
               return ChunkHolder.getFullChunkStatus(var1.getTicketLevel());
            });
            var5.runPostLoad();
            if (this.entitiesInLevel.add(var3.toLong())) {
               var5.setLoaded(true);
               var5.registerAllBlockEntitiesAfterLevelLoad();
            }

            return var5;
         });
      }, (var2x) -> {
         ProcessorHandle var10000 = this.mainThreadMailbox;
         long var10002 = var1.getPos().toLong();
         Objects.requireNonNull(var1);
         var10000.tell(ChunkTaskPriorityQueueSorter.message(var2x, var10002, var1::getTicketLevel));
      });
   }

   public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareTickingChunk(ChunkHolder var1) {
      ChunkPos var2 = var1.getPos();
      CompletableFuture var3 = this.getChunkRangeFuture(var2, 1, (var0) -> {
         return ChunkStatus.FULL;
      });
      CompletableFuture var4 = var3.thenApplyAsync((var0) -> {
         return var0.flatMap((var0x) -> {
            LevelChunk var1 = (LevelChunk)var0x.get(var0x.size() / 2);
            var1.postProcessGeneration();
            return Either.left(var1);
         });
      }, (var2x) -> {
         this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(var1, var2x));
      });
      var4.thenAcceptAsync((var2x) -> {
         var2x.ifLeft((var2xx) -> {
            this.tickingGenerated.getAndIncrement();
            Packet[] var3 = new Packet[2];
            this.getPlayers(var2, false).forEach((var3x) -> {
               this.playerLoadedChunk(var3x, var3, var2xx);
            });
         });
      }, (var2x) -> {
         this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(var1, var2x));
      });
      return var4;
   }

   public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareAccessibleChunk(ChunkHolder var1) {
      return this.getChunkRangeFuture(var1.getPos(), 1, ChunkStatus::getStatusAroundFullChunk).thenApplyAsync((var0) -> {
         return var0.mapLeft((var0x) -> {
            LevelChunk var1 = (LevelChunk)var0x.get(var0x.size() / 2);
            var1.unpackTicks();
            return var1;
         });
      }, (var2) -> {
         this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(var1, var2));
      });
   }

   public int getTickingGenerated() {
      return this.tickingGenerated.get();
   }

   private boolean save(ChunkAccess var1) {
      this.poiManager.flush(var1.getPos());
      if (!var1.isUnsaved()) {
         return false;
      } else {
         var1.setUnsaved(false);
         ChunkPos var2 = var1.getPos();

         try {
            ChunkStatus var3 = var1.getStatus();
            if (var3.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
               if (this.isExistingChunkFull(var2)) {
                  return false;
               }

               if (var3 == ChunkStatus.EMPTY && var1.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
                  return false;
               }
            }

            this.level.getProfiler().incrementCounter("chunkSave");
            CompoundTag var4 = ChunkSerializer.write(this.level, var1);
            this.write(var2, var4);
            this.markPosition(var2, var3.getChunkType());
            return true;
         } catch (Exception var5) {
            LOGGER.error("Failed to save chunk {},{}", var2.x, var2.z, var5);
            return false;
         }
      }
   }

   private boolean isExistingChunkFull(ChunkPos var1) {
      byte var2 = this.chunkTypeCache.get(var1.toLong());
      if (var2 != 0) {
         return var2 == 1;
      } else {
         CompoundTag var3;
         try {
            var3 = this.readChunk(var1);
            if (var3 == null) {
               this.markPositionReplaceable(var1);
               return false;
            }
         } catch (Exception var5) {
            LOGGER.error("Failed to read chunk {}", var1, var5);
            this.markPositionReplaceable(var1);
            return false;
         }

         ChunkStatus.ChunkType var4 = ChunkSerializer.getChunkTypeFromTag(var3);
         return this.markPosition(var1, var4) == 1;
      }
   }

   protected void setViewDistance(int var1) {
      int var2 = Mth.clamp((int)(var1 + 1), (int)3, (int)33);
      if (var2 != this.viewDistance) {
         int var3 = this.viewDistance;
         this.viewDistance = var2;
         this.distanceManager.updatePlayerTickets(this.viewDistance);
         ObjectIterator var4 = this.updatingChunkMap.values().iterator();

         while(var4.hasNext()) {
            ChunkHolder var5 = (ChunkHolder)var4.next();
            ChunkPos var6 = var5.getPos();
            Packet[] var7 = new Packet[2];
            this.getPlayers(var6, false).forEach((var4x) -> {
               int var5 = checkerboardDistance(var6, var4x, true);
               boolean var6x = var5 <= var3;
               boolean var7x = var5 <= this.viewDistance;
               this.updateChunkTracking(var4x, var6, var7, var6x, var7x);
            });
         }
      }

   }

   protected void updateChunkTracking(ServerPlayer var1, ChunkPos var2, Packet<?>[] var3, boolean var4, boolean var5) {
      if (var1.level == this.level) {
         if (var5 && !var4) {
            ChunkHolder var6 = this.getVisibleChunkIfPresent(var2.toLong());
            if (var6 != null) {
               LevelChunk var7 = var6.getTickingChunk();
               if (var7 != null) {
                  this.playerLoadedChunk(var1, var3, var7);
               }

               DebugPackets.sendPoiPacketsForChunk(this.level, var2);
            }
         }

         if (!var5 && var4) {
            var1.untrackChunk(var2);
         }

      }
   }

   public int size() {
      return this.visibleChunkMap.size();
   }

   protected net.minecraft.server.level.DistanceManager getDistanceManager() {
      return this.distanceManager;
   }

   protected Iterable<ChunkHolder> getChunks() {
      return Iterables.unmodifiableIterable(this.visibleChunkMap.values());
   }

   void dumpChunks(Writer var1) throws IOException {
      CsvOutput var2 = CsvOutput.builder().addColumn("x").addColumn("z").addColumn("level").addColumn("in_memory").addColumn("status").addColumn("full_status").addColumn("accessible_ready").addColumn("ticking_ready").addColumn("entity_ticking_ready").addColumn("ticket").addColumn("spawning").addColumn("block_entity_count").build(var1);
      ObjectBidirectionalIterator var3 = this.visibleChunkMap.long2ObjectEntrySet().iterator();

      while(var3.hasNext()) {
         Entry var4 = (Entry)var3.next();
         ChunkPos var5 = new ChunkPos(var4.getLongKey());
         ChunkHolder var6 = (ChunkHolder)var4.getValue();
         Optional var7 = Optional.ofNullable(var6.getLastAvailable());
         Optional var8 = var7.flatMap((var0) -> {
            return var0 instanceof LevelChunk ? Optional.of((LevelChunk)var0) : Optional.empty();
         });
         var2.writeRow(var5.x, var5.z, var6.getTicketLevel(), var7.isPresent(), var7.map(ChunkAccess::getStatus).orElse((Object)null), var8.map(LevelChunk::getFullStatus).orElse((Object)null), printFuture(var6.getFullChunkFuture()), printFuture(var6.getTickingChunkFuture()), printFuture(var6.getEntityTickingChunkFuture()), this.distanceManager.getTicketDebugString(var4.getLongKey()), !this.noPlayersCloseForSpawning(var5), var8.map((var0) -> {
            return var0.getBlockEntities().size();
         }).orElse(0));
      }

   }

   private static String printFuture(CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> var0) {
      try {
         Either var1 = (Either)var0.getNow((Object)null);
         return var1 != null ? (String)var1.map((var0x) -> {
            return "done";
         }, (var0x) -> {
            return "unloaded";
         }) : "not completed";
      } catch (CompletionException var2) {
         return "failed " + var2.getCause().getMessage();
      } catch (CancellationException var3) {
         return "cancelled";
      }
   }

   @Nullable
   private CompoundTag readChunk(ChunkPos var1) throws IOException {
      CompoundTag var2 = this.read(var1);
      return var2 == null ? null : this.upgradeChunkTag(this.level.dimension(), this.overworldDataStorage, var2);
   }

   boolean noPlayersCloseForSpawning(ChunkPos var1) {
      long var2 = var1.toLong();
      return !this.distanceManager.hasPlayersNearby(var2) ? true : this.playerMap.getPlayers(var2).noneMatch((var1x) -> {
         return !var1x.isSpectator() && euclideanDistanceSquared(var1, var1x) < 16384.0D;
      });
   }

   private boolean skipPlayer(ServerPlayer var1) {
      return var1.isSpectator() && !this.level.getGameRules().getBoolean(GameRules.RULE_SPECTATORSGENERATECHUNKS);
   }

   void updatePlayerStatus(ServerPlayer var1, boolean var2) {
      boolean var3 = this.skipPlayer(var1);
      boolean var4 = this.playerMap.ignoredOrUnknown(var1);
      int var5 = SectionPos.blockToSectionCoord(var1.getBlockX());
      int var6 = SectionPos.blockToSectionCoord(var1.getBlockZ());
      if (var2) {
         this.playerMap.addPlayer(ChunkPos.asLong(var5, var6), var1, var3);
         this.updatePlayerPos(var1);
         if (!var3) {
            this.distanceManager.addPlayer(SectionPos.of((Entity)var1), var1);
         }
      } else {
         SectionPos var7 = var1.getLastSectionPos();
         this.playerMap.removePlayer(var7.chunk().toLong(), var1);
         if (!var4) {
            this.distanceManager.removePlayer(var7, var1);
         }
      }

      for(int var10 = var5 - this.viewDistance; var10 <= var5 + this.viewDistance; ++var10) {
         for(int var8 = var6 - this.viewDistance; var8 <= var6 + this.viewDistance; ++var8) {
            ChunkPos var9 = new ChunkPos(var10, var8);
            this.updateChunkTracking(var1, var9, new Packet[2], !var2, var2);
         }
      }

   }

   private SectionPos updatePlayerPos(ServerPlayer var1) {
      SectionPos var2 = SectionPos.of((Entity)var1);
      var1.setLastSectionPos(var2);
      var1.connection.send(new ClientboundSetChunkCacheCenterPacket(var2.x(), var2.z()));
      return var2;
   }

   public void move(ServerPlayer var1) {
      ObjectIterator var2 = this.entityMap.values().iterator();

      while(var2.hasNext()) {
         ChunkMap.TrackedEntity var3 = (ChunkMap.TrackedEntity)var2.next();
         if (var3.entity == var1) {
            var3.updatePlayers(this.level.players());
         } else {
            var3.updatePlayer(var1);
         }
      }

      int var24 = SectionPos.blockToSectionCoord(var1.getBlockX());
      int var25 = SectionPos.blockToSectionCoord(var1.getBlockZ());
      SectionPos var4 = var1.getLastSectionPos();
      SectionPos var5 = SectionPos.of((Entity)var1);
      long var6 = var4.chunk().toLong();
      long var8 = var5.chunk().toLong();
      boolean var10 = this.playerMap.ignored(var1);
      boolean var11 = this.skipPlayer(var1);
      boolean var12 = var4.asLong() != var5.asLong();
      if (var12 || var10 != var11) {
         this.updatePlayerPos(var1);
         if (!var10) {
            this.distanceManager.removePlayer(var4, var1);
         }

         if (!var11) {
            this.distanceManager.addPlayer(var5, var1);
         }

         if (!var10 && var11) {
            this.playerMap.ignorePlayer(var1);
         }

         if (var10 && !var11) {
            this.playerMap.unIgnorePlayer(var1);
         }

         if (var6 != var8) {
            this.playerMap.updatePlayer(var6, var8, var1);
         }
      }

      int var13 = var4.x();
      int var14 = var4.z();
      int var15;
      int var16;
      if (Math.abs(var13 - var24) <= this.viewDistance * 2 && Math.abs(var14 - var25) <= this.viewDistance * 2) {
         var15 = Math.min(var24, var13) - this.viewDistance;
         var16 = Math.min(var25, var14) - this.viewDistance;
         int var26 = Math.max(var24, var13) + this.viewDistance;
         int var27 = Math.max(var25, var14) + this.viewDistance;

         for(int var28 = var15; var28 <= var26; ++var28) {
            for(int var20 = var16; var20 <= var27; ++var20) {
               ChunkPos var21 = new ChunkPos(var28, var20);
               boolean var22 = checkerboardDistance(var21, var13, var14) <= this.viewDistance;
               boolean var23 = checkerboardDistance(var21, var24, var25) <= this.viewDistance;
               this.updateChunkTracking(var1, var21, new Packet[2], var22, var23);
            }
         }
      } else {
         ChunkPos var17;
         boolean var18;
         boolean var19;
         for(var15 = var13 - this.viewDistance; var15 <= var13 + this.viewDistance; ++var15) {
            for(var16 = var14 - this.viewDistance; var16 <= var14 + this.viewDistance; ++var16) {
               var17 = new ChunkPos(var15, var16);
               var18 = true;
               var19 = false;
               this.updateChunkTracking(var1, var17, new Packet[2], true, false);
            }
         }

         for(var15 = var24 - this.viewDistance; var15 <= var24 + this.viewDistance; ++var15) {
            for(var16 = var25 - this.viewDistance; var16 <= var25 + this.viewDistance; ++var16) {
               var17 = new ChunkPos(var15, var16);
               var18 = false;
               var19 = true;
               this.updateChunkTracking(var1, var17, new Packet[2], false, true);
            }
         }
      }

   }

   public Stream<ServerPlayer> getPlayers(ChunkPos var1, boolean var2) {
      return this.playerMap.getPlayers(var1.toLong()).filter((var3) -> {
         int var4 = checkerboardDistance(var1, var3, true);
         if (var4 > this.viewDistance) {
            return false;
         } else {
            return !var2 || var4 == this.viewDistance;
         }
      });
   }

   protected void addEntity(Entity var1) {
      if (!(var1 instanceof EnderDragonPart)) {
         EntityType var2 = var1.getType();
         int var3 = var2.clientTrackingRange() * 16;
         if (var3 != 0) {
            int var4 = var2.updateInterval();
            if (this.entityMap.containsKey(var1.getId())) {
               throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Entity is already tracked!"));
            } else {
               ChunkMap.TrackedEntity var5 = new ChunkMap.TrackedEntity(var1, var3, var4, var2.trackDeltas());
               this.entityMap.put(var1.getId(), var5);
               var5.updatePlayers(this.level.players());
               if (var1 instanceof ServerPlayer) {
                  ServerPlayer var6 = (ServerPlayer)var1;
                  this.updatePlayerStatus(var6, true);
                  ObjectIterator var7 = this.entityMap.values().iterator();

                  while(var7.hasNext()) {
                     ChunkMap.TrackedEntity var8 = (ChunkMap.TrackedEntity)var7.next();
                     if (var8.entity != var6) {
                        var8.updatePlayer(var6);
                     }
                  }
               }

            }
         }
      }
   }

   protected void removeEntity(Entity var1) {
      if (var1 instanceof ServerPlayer) {
         ServerPlayer var2 = (ServerPlayer)var1;
         this.updatePlayerStatus(var2, false);
         ObjectIterator var3 = this.entityMap.values().iterator();

         while(var3.hasNext()) {
            ChunkMap.TrackedEntity var4 = (ChunkMap.TrackedEntity)var3.next();
            var4.removePlayer(var2);
         }
      }

      ChunkMap.TrackedEntity var5 = (ChunkMap.TrackedEntity)this.entityMap.remove(var1.getId());
      if (var5 != null) {
         var5.broadcastRemoved();
      }

   }

   protected void tick() {
      ArrayList var1 = Lists.newArrayList();
      List var2 = this.level.players();

      ObjectIterator var3;
      ChunkMap.TrackedEntity var4;
      for(var3 = this.entityMap.values().iterator(); var3.hasNext(); var4.serverEntity.sendChanges()) {
         var4 = (ChunkMap.TrackedEntity)var3.next();
         SectionPos var5 = var4.lastSectionPos;
         SectionPos var6 = SectionPos.of(var4.entity);
         if (!Objects.equals(var5, var6)) {
            var4.updatePlayers(var2);
            Entity var7 = var4.entity;
            if (var7 instanceof ServerPlayer) {
               var1.add((ServerPlayer)var7);
            }

            var4.lastSectionPos = var6;
         }
      }

      if (!var1.isEmpty()) {
         var3 = this.entityMap.values().iterator();

         while(var3.hasNext()) {
            var4 = (ChunkMap.TrackedEntity)var3.next();
            var4.updatePlayers(var1);
         }
      }

   }

   public void broadcast(Entity var1, Packet<?> var2) {
      ChunkMap.TrackedEntity var3 = (ChunkMap.TrackedEntity)this.entityMap.get(var1.getId());
      if (var3 != null) {
         var3.broadcast(var2);
      }

   }

   protected void broadcastAndSend(Entity var1, Packet<?> var2) {
      ChunkMap.TrackedEntity var3 = (ChunkMap.TrackedEntity)this.entityMap.get(var1.getId());
      if (var3 != null) {
         var3.broadcastAndSend(var2);
      }

   }

   private void playerLoadedChunk(ServerPlayer var1, Packet<?>[] var2, LevelChunk var3) {
      if (var2[0] == null) {
         var2[0] = new ClientboundLevelChunkPacket(var3);
         var2[1] = new ClientboundLightUpdatePacket(var3.getPos(), this.lightEngine, (BitSet)null, (BitSet)null, true);
      }

      var1.trackChunk(var3.getPos(), var2[0], var2[1]);
      DebugPackets.sendPoiPacketsForChunk(this.level, var3.getPos());
      ArrayList var4 = Lists.newArrayList();
      ArrayList var5 = Lists.newArrayList();
      ObjectIterator var6 = this.entityMap.values().iterator();

      while(var6.hasNext()) {
         ChunkMap.TrackedEntity var7 = (ChunkMap.TrackedEntity)var6.next();
         Entity var8 = var7.entity;
         if (var8 != var1 && var8.chunkPosition().equals(var3.getPos())) {
            var7.updatePlayer(var1);
            if (var8 instanceof Mob && ((Mob)var8).getLeashHolder() != null) {
               var4.add(var8);
            }

            if (!var8.getPassengers().isEmpty()) {
               var5.add(var8);
            }
         }
      }

      Iterator var9;
      Entity var10;
      if (!var4.isEmpty()) {
         var9 = var4.iterator();

         while(var9.hasNext()) {
            var10 = (Entity)var9.next();
            var1.connection.send(new ClientboundSetEntityLinkPacket(var10, ((Mob)var10).getLeashHolder()));
         }
      }

      if (!var5.isEmpty()) {
         var9 = var5.iterator();

         while(var9.hasNext()) {
            var10 = (Entity)var9.next();
            var1.connection.send(new ClientboundSetPassengersPacket(var10));
         }
      }

   }

   protected PoiManager getPoiManager() {
      return this.poiManager;
   }

   public CompletableFuture<Void> packTicks(LevelChunk var1) {
      return this.mainThreadExecutor.submit(() -> {
         var1.packTicks(this.level);
      });
   }

   void onFullChunkStatusChange(ChunkPos var1, ChunkHolder.FullChunkStatus var2) {
      this.chunkStatusListener.onChunkStatusChange(var1, var2);
   }

   private class DistanceManager extends net.minecraft.server.level.DistanceManager {
      protected DistanceManager(Executor var2, Executor var3) {
         super(var2, var3);
      }

      protected boolean isChunkToRemove(long var1) {
         return ChunkMap.this.toDrop.contains(var1);
      }

      @Nullable
      protected ChunkHolder getChunk(long var1) {
         return ChunkMap.this.getUpdatingChunkIfPresent(var1);
      }

      @Nullable
      protected ChunkHolder updateChunkScheduling(long var1, int var3, @Nullable ChunkHolder var4, int var5) {
         return ChunkMap.this.updateChunkScheduling(var1, var3, var4, var5);
      }
   }

   class TrackedEntity {
      final ServerEntity serverEntity;
      final Entity entity;
      private final int range;
      SectionPos lastSectionPos;
      private final Set<ServerPlayerConnection> seenBy = Sets.newIdentityHashSet();

      public TrackedEntity(Entity var2, int var3, int var4, boolean var5) {
         this.serverEntity = new ServerEntity(ChunkMap.this.level, var2, var4, var5, this::broadcast);
         this.entity = var2;
         this.range = var3;
         this.lastSectionPos = SectionPos.of(var2);
      }

      public boolean equals(Object var1) {
         if (var1 instanceof ChunkMap.TrackedEntity) {
            return ((ChunkMap.TrackedEntity)var1).entity.getId() == this.entity.getId();
         } else {
            return false;
         }
      }

      public int hashCode() {
         return this.entity.getId();
      }

      public void broadcast(Packet<?> var1) {
         Iterator var2 = this.seenBy.iterator();

         while(var2.hasNext()) {
            ServerPlayerConnection var3 = (ServerPlayerConnection)var2.next();
            var3.send(var1);
         }

      }

      public void broadcastAndSend(Packet<?> var1) {
         this.broadcast(var1);
         if (this.entity instanceof ServerPlayer) {
            ((ServerPlayer)this.entity).connection.send(var1);
         }

      }

      public void broadcastRemoved() {
         Iterator var1 = this.seenBy.iterator();

         while(var1.hasNext()) {
            ServerPlayerConnection var2 = (ServerPlayerConnection)var1.next();
            this.serverEntity.removePairing(var2.getPlayer());
         }

      }

      public void removePlayer(ServerPlayer var1) {
         if (this.seenBy.remove(var1.connection)) {
            this.serverEntity.removePairing(var1);
         }

      }

      public void updatePlayer(ServerPlayer var1) {
         if (var1 != this.entity) {
            Vec3 var2 = var1.position().subtract(this.serverEntity.sentPos());
            int var3 = Math.min(this.getEffectiveRange(), (ChunkMap.this.viewDistance - 1) * 16);
            boolean var4 = var2.x >= (double)(-var3) && var2.x <= (double)var3 && var2.z >= (double)(-var3) && var2.z <= (double)var3 && this.entity.broadcastToPlayer(var1);
            if (var4) {
               if (this.seenBy.add(var1.connection)) {
                  this.serverEntity.addPairing(var1);
               }
            } else if (this.seenBy.remove(var1.connection)) {
               this.serverEntity.removePairing(var1);
            }

         }
      }

      private int scaledRange(int var1) {
         return ChunkMap.this.level.getServer().getScaledTrackingDistance(var1);
      }

      private int getEffectiveRange() {
         int var1 = this.range;
         Iterator var2 = this.entity.getIndirectPassengers().iterator();

         while(var2.hasNext()) {
            Entity var3 = (Entity)var2.next();
            int var4 = var3.getType().clientTrackingRange() * 16;
            if (var4 > var1) {
               var1 = var4;
            }
         }

         return this.scaledRange(var1);
      }

      public void updatePlayers(List<ServerPlayer> var1) {
         Iterator var2 = var1.iterator();

         while(var2.hasNext()) {
            ServerPlayer var3 = (ServerPlayer)var2.next();
            this.updatePlayer(var3);
         }

      }
   }
}
