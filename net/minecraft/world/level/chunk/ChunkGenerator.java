package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.data.worldgen.StructureFeatures;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.BaseStoneSource;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.SingleBaseStoneSource;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StrongholdConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public abstract class ChunkGenerator {
   public static final Codec<ChunkGenerator> CODEC;
   protected final BiomeSource biomeSource;
   protected final BiomeSource runtimeBiomeSource;
   private final StructureSettings settings;
   private final long strongholdSeed;
   private final List<ChunkPos> strongholdPositions;
   private final BaseStoneSource defaultBaseStoneSource;

   public ChunkGenerator(BiomeSource var1, StructureSettings var2) {
      this(var1, var1, var2, 0L);
   }

   public ChunkGenerator(BiomeSource var1, BiomeSource var2, StructureSettings var3, long var4) {
      this.strongholdPositions = Lists.newArrayList();
      this.biomeSource = var1;
      this.runtimeBiomeSource = var2;
      this.settings = var3;
      this.strongholdSeed = var4;
      this.defaultBaseStoneSource = new SingleBaseStoneSource(Blocks.STONE.defaultBlockState());
   }

   private void generateStrongholds() {
      if (this.strongholdPositions.isEmpty()) {
         StrongholdConfiguration var1 = this.settings.stronghold();
         if (var1 != null && var1.count() != 0) {
            ArrayList var2 = Lists.newArrayList();
            Iterator var3 = this.biomeSource.possibleBiomes().iterator();

            while(var3.hasNext()) {
               Biome var4 = (Biome)var3.next();
               if (var4.getGenerationSettings().isValidStart(StructureFeature.STRONGHOLD)) {
                  var2.add(var4);
               }
            }

            int var17 = var1.distance();
            int var18 = var1.count();
            int var5 = var1.spread();
            Random var6 = new Random();
            var6.setSeed(this.strongholdSeed);
            double var7 = var6.nextDouble() * 3.141592653589793D * 2.0D;
            int var9 = 0;
            int var10 = 0;

            for(int var11 = 0; var11 < var18; ++var11) {
               double var12 = (double)(4 * var17 + var17 * var10 * 6) + (var6.nextDouble() - 0.5D) * (double)var17 * 2.5D;
               int var14 = (int)Math.round(Math.cos(var7) * var12);
               int var15 = (int)Math.round(Math.sin(var7) * var12);
               BiomeSource var10000 = this.biomeSource;
               int var10001 = SectionPos.sectionToBlockCoord(var14, 8);
               int var10003 = SectionPos.sectionToBlockCoord(var15, 8);
               Objects.requireNonNull(var2);
               BlockPos var16 = var10000.findBiomeHorizontal(var10001, 0, var10003, 112, var2::contains, var6);
               if (var16 != null) {
                  var14 = SectionPos.blockToSectionCoord(var16.getX());
                  var15 = SectionPos.blockToSectionCoord(var16.getZ());
               }

               this.strongholdPositions.add(new ChunkPos(var14, var15));
               var7 += 6.283185307179586D / (double)var5;
               ++var9;
               if (var9 == var5) {
                  ++var10;
                  var9 = 0;
                  var5 += 2 * var5 / (var10 + 1);
                  var5 = Math.min(var5, var18 - var11);
                  var7 += var6.nextDouble() * 3.141592653589793D * 2.0D;
               }
            }

         }
      }
   }

   protected abstract Codec<? extends ChunkGenerator> codec();

   public abstract ChunkGenerator withSeed(long var1);

   public void createBiomes(Registry<Biome> var1, ChunkAccess var2) {
      ChunkPos var3 = var2.getPos();
      ((ProtoChunk)var2).setBiomes(new ChunkBiomeContainer(var1, var2, var3, this.runtimeBiomeSource));
   }

   public void applyCarvers(long var1, BiomeManager var3, ChunkAccess var4, GenerationStep.Carving var5) {
      BiomeManager var6 = var3.withDifferentSource(this.biomeSource);
      WorldgenRandom var7 = new WorldgenRandom();
      boolean var8 = true;
      ChunkPos var9 = var4.getPos();
      CarvingContext var10 = new CarvingContext(this);
      Aquifer var11 = this.createAquifer(var4);
      BitSet var12 = ((ProtoChunk)var4).getOrCreateCarvingMask(var5);

      for(int var13 = -8; var13 <= 8; ++var13) {
         for(int var14 = -8; var14 <= 8; ++var14) {
            ChunkPos var15 = new ChunkPos(var9.x + var13, var9.z + var14);
            BiomeGenerationSettings var16 = this.biomeSource.getNoiseBiome(QuartPos.fromBlock(var15.getMinBlockX()), 0, QuartPos.fromBlock(var15.getMinBlockZ())).getGenerationSettings();
            List var17 = var16.getCarvers(var5);
            ListIterator var18 = var17.listIterator();

            while(var18.hasNext()) {
               int var19 = var18.nextIndex();
               ConfiguredWorldCarver var20 = (ConfiguredWorldCarver)((Supplier)var18.next()).get();
               var7.setLargeFeatureSeed(var1 + (long)var19, var15.x, var15.z);
               if (var20.isStartChunk(var7)) {
                  Objects.requireNonNull(var6);
                  var20.carve(var10, var4, var6::getBiome, var7, var11, var15, var12);
               }
            }
         }
      }

   }

   protected Aquifer createAquifer(ChunkAccess var1) {
      return Aquifer.createDisabled(this.getSeaLevel(), Blocks.WATER.defaultBlockState());
   }

   @Nullable
   public BlockPos findNearestMapFeature(ServerLevel var1, StructureFeature<?> var2, BlockPos var3, int var4, boolean var5) {
      if (!this.biomeSource.canGenerateStructure(var2)) {
         return null;
      } else if (var2 == StructureFeature.STRONGHOLD) {
         this.generateStrongholds();
         BlockPos var14 = null;
         double var7 = Double.MAX_VALUE;
         BlockPos.MutableBlockPos var9 = new BlockPos.MutableBlockPos();
         Iterator var10 = this.strongholdPositions.iterator();

         while(var10.hasNext()) {
            ChunkPos var11 = (ChunkPos)var10.next();
            var9.set(SectionPos.sectionToBlockCoord(var11.x, 8), 32, SectionPos.sectionToBlockCoord(var11.z, 8));
            double var12 = var9.distSqr(var3);
            if (var14 == null) {
               var14 = new BlockPos(var9);
               var7 = var12;
            } else if (var12 < var7) {
               var14 = new BlockPos(var9);
               var7 = var12;
            }
         }

         return var14;
      } else {
         StructureFeatureConfiguration var6 = this.settings.getConfig(var2);
         return var6 == null ? null : var2.getNearestGeneratedFeature(var1, var1.structureFeatureManager(), var3, var4, var5, var1.getSeed(), var6);
      }
   }

   public void applyBiomeDecoration(WorldGenRegion var1, StructureFeatureManager var2) {
      ChunkPos var3 = var1.getCenter();
      int var4 = var3.getMinBlockX();
      int var5 = var3.getMinBlockZ();
      BlockPos var6 = new BlockPos(var4, var1.getMinBuildHeight(), var5);
      Biome var7 = this.biomeSource.getPrimaryBiome(var3);
      WorldgenRandom var8 = new WorldgenRandom();
      long var9 = var8.setDecorationSeed(var1.getSeed(), var4, var5);

      try {
         var7.generate(var2, this, var1, var9, var8, var6);
      } catch (Exception var13) {
         CrashReport var12 = CrashReport.forThrowable(var13, "Biome decoration");
         var12.addCategory("Generation").setDetail("CenterX", (Object)var3.x).setDetail("CenterZ", (Object)var3.z).setDetail("Seed", (Object)var9).setDetail("Biome", (Object)var7);
         throw new ReportedException(var12);
      }
   }

   public abstract void buildSurfaceAndBedrock(WorldGenRegion var1, ChunkAccess var2);

   public void spawnOriginalMobs(WorldGenRegion var1) {
   }

   public StructureSettings getSettings() {
      return this.settings;
   }

   public int getSpawnHeight(LevelHeightAccessor var1) {
      return 64;
   }

   public BiomeSource getBiomeSource() {
      return this.runtimeBiomeSource;
   }

   public int getGenDepth() {
      return 256;
   }

   public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Biome var1, StructureFeatureManager var2, MobCategory var3, BlockPos var4) {
      return var1.getMobSettings().getMobs(var3);
   }

   public void createStructures(RegistryAccess var1, StructureFeatureManager var2, ChunkAccess var3, StructureManager var4, long var5) {
      Biome var7 = this.biomeSource.getPrimaryBiome(var3.getPos());
      this.createStructure(StructureFeatures.STRONGHOLD, var1, var2, var3, var4, var5, var7);
      Iterator var8 = var7.getGenerationSettings().structures().iterator();

      while(var8.hasNext()) {
         Supplier var9 = (Supplier)var8.next();
         this.createStructure((ConfiguredStructureFeature)var9.get(), var1, var2, var3, var4, var5, var7);
      }

   }

   private void createStructure(ConfiguredStructureFeature<?, ?> var1, RegistryAccess var2, StructureFeatureManager var3, ChunkAccess var4, StructureManager var5, long var6, Biome var8) {
      ChunkPos var9 = var4.getPos();
      SectionPos var10 = SectionPos.bottomOf(var4);
      StructureStart var11 = var3.getStartForFeature(var10, var1.feature, var4);
      int var12 = var11 != null ? var11.getReferences() : 0;
      StructureFeatureConfiguration var13 = this.settings.getConfig(var1.feature);
      if (var13 != null) {
         StructureStart var14 = var1.generate(var2, this, this.biomeSource, var5, var6, var9, var8, var12, var13, var4);
         var3.setStartForFeature(var10, var1.feature, var14, var4);
      }

   }

   public void createReferences(WorldGenLevel var1, StructureFeatureManager var2, ChunkAccess var3) {
      boolean var4 = true;
      ChunkPos var5 = var3.getPos();
      int var6 = var5.x;
      int var7 = var5.z;
      int var8 = var5.getMinBlockX();
      int var9 = var5.getMinBlockZ();
      SectionPos var10 = SectionPos.bottomOf(var3);

      for(int var11 = var6 - 8; var11 <= var6 + 8; ++var11) {
         for(int var12 = var7 - 8; var12 <= var7 + 8; ++var12) {
            long var13 = ChunkPos.asLong(var11, var12);
            Iterator var15 = var1.getChunk(var11, var12).getAllStarts().values().iterator();

            while(var15.hasNext()) {
               StructureStart var16 = (StructureStart)var15.next();

               try {
                  if (var16.isValid() && var16.getBoundingBox().intersects(var8, var9, var8 + 15, var9 + 15)) {
                     var2.addReferenceForFeature(var10, var16.getFeature(), var13, var3);
                     DebugPackets.sendStructurePacket(var1, var16);
                  }
               } catch (Exception var20) {
                  CrashReport var18 = CrashReport.forThrowable(var20, "Generating structure reference");
                  CrashReportCategory var19 = var18.addCategory("Structure");
                  var19.setDetail("Id", () -> {
                     return Registry.STRUCTURE_FEATURE.getKey(var16.getFeature()).toString();
                  });
                  var19.setDetail("Name", () -> {
                     return var16.getFeature().getFeatureName();
                  });
                  var19.setDetail("Class", () -> {
                     return var16.getFeature().getClass().getCanonicalName();
                  });
                  throw new ReportedException(var18);
               }
            }
         }
      }

   }

   public abstract CompletableFuture<ChunkAccess> fillFromNoise(Executor var1, StructureFeatureManager var2, ChunkAccess var3);

   public int getSeaLevel() {
      return 63;
   }

   public int getMinY() {
      return 0;
   }

   public abstract int getBaseHeight(int var1, int var2, Heightmap.Types var3, LevelHeightAccessor var4);

   public abstract NoiseColumn getBaseColumn(int var1, int var2, LevelHeightAccessor var3);

   public int getFirstFreeHeight(int var1, int var2, Heightmap.Types var3, LevelHeightAccessor var4) {
      return this.getBaseHeight(var1, var2, var3, var4);
   }

   public int getFirstOccupiedHeight(int var1, int var2, Heightmap.Types var3, LevelHeightAccessor var4) {
      return this.getBaseHeight(var1, var2, var3, var4) - 1;
   }

   public boolean hasStronghold(ChunkPos var1) {
      this.generateStrongholds();
      return this.strongholdPositions.contains(var1);
   }

   public BaseStoneSource getBaseStoneSource() {
      return this.defaultBaseStoneSource;
   }

   static {
      Registry.register(Registry.CHUNK_GENERATOR, (String)"noise", NoiseBasedChunkGenerator.CODEC);
      Registry.register(Registry.CHUNK_GENERATOR, (String)"flat", FlatLevelSource.CODEC);
      Registry.register(Registry.CHUNK_GENERATOR, (String)"debug", DebugLevelSource.CODEC);
      CODEC = Registry.CHUNK_GENERATOR.dispatchStable(ChunkGenerator::codec, Function.identity());
   }
}
