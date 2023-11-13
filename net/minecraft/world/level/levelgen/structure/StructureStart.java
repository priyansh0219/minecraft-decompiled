package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.MineshaftConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StructureStart<C extends FeatureConfiguration> implements StructurePieceAccessor {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final String INVALID_START_ID = "INVALID";
   public static final StructureStart<?> INVALID_START = new StructureStart<MineshaftConfiguration>((StructureFeature)null, new ChunkPos(0, 0), 0, 0L) {
      public void generatePieces(RegistryAccess var1, ChunkGenerator var2, StructureManager var3, ChunkPos var4, Biome var5, MineshaftConfiguration var6, LevelHeightAccessor var7) {
      }

      public boolean isValid() {
         return false;
      }
   };
   private final StructureFeature<C> feature;
   protected final List<StructurePiece> pieces = Lists.newArrayList();
   private final ChunkPos chunkPos;
   private int references;
   protected final WorldgenRandom random;
   @Nullable
   private BoundingBox cachedBoundingBox;

   public StructureStart(StructureFeature<C> var1, ChunkPos var2, int var3, long var4) {
      this.feature = var1;
      this.chunkPos = var2;
      this.references = var3;
      this.random = new WorldgenRandom();
      this.random.setLargeFeatureSeed(var4, var2.x, var2.z);
   }

   public abstract void generatePieces(RegistryAccess var1, ChunkGenerator var2, StructureManager var3, ChunkPos var4, Biome var5, C var6, LevelHeightAccessor var7);

   public final BoundingBox getBoundingBox() {
      if (this.cachedBoundingBox == null) {
         this.cachedBoundingBox = this.createBoundingBox();
      }

      return this.cachedBoundingBox;
   }

   protected BoundingBox createBoundingBox() {
      synchronized(this.pieces) {
         Stream var10000 = this.pieces.stream().map(StructurePiece::getBoundingBox);
         Objects.requireNonNull(var10000);
         return (BoundingBox)BoundingBox.encapsulatingBoxes(var10000::iterator).orElseThrow(() -> {
            return new IllegalStateException("Unable to calculate boundingbox without pieces");
         });
      }
   }

   public List<StructurePiece> getPieces() {
      return this.pieces;
   }

   public void placeInChunk(WorldGenLevel var1, StructureFeatureManager var2, ChunkGenerator var3, Random var4, BoundingBox var5, ChunkPos var6) {
      synchronized(this.pieces) {
         if (!this.pieces.isEmpty()) {
            BoundingBox var8 = ((StructurePiece)this.pieces.get(0)).boundingBox;
            BlockPos var9 = var8.getCenter();
            BlockPos var10 = new BlockPos(var9.getX(), var8.minY(), var9.getZ());
            Iterator var11 = this.pieces.iterator();

            while(var11.hasNext()) {
               StructurePiece var12 = (StructurePiece)var11.next();
               if (var12.getBoundingBox().intersects(var5) && !var12.postProcess(var1, var2, var3, var4, var5, var6, var10)) {
                  var11.remove();
               }
            }

         }
      }
   }

   public CompoundTag createTag(ServerLevel var1, ChunkPos var2) {
      CompoundTag var3 = new CompoundTag();
      if (this.isValid()) {
         var3.putString("id", Registry.STRUCTURE_FEATURE.getKey(this.getFeature()).toString());
         var3.putInt("ChunkX", var2.x);
         var3.putInt("ChunkZ", var2.z);
         var3.putInt("references", this.references);
         ListTag var4 = new ListTag();
         synchronized(this.pieces) {
            Iterator var6 = this.pieces.iterator();

            while(true) {
               if (!var6.hasNext()) {
                  break;
               }

               StructurePiece var7 = (StructurePiece)var6.next();
               var4.add(var7.createTag(var1));
            }
         }

         var3.put("Children", var4);
         return var3;
      } else {
         var3.putString("id", "INVALID");
         return var3;
      }
   }

   protected void moveBelowSeaLevel(int var1, int var2, Random var3, int var4) {
      int var5 = var1 - var4;
      BoundingBox var6 = this.getBoundingBox();
      int var7 = var6.getYSpan() + var2 + 1;
      if (var7 < var5) {
         var7 += var3.nextInt(var5 - var7);
      }

      int var8 = var7 - var6.maxY();
      this.offsetPiecesVertically(var8);
   }

   protected void moveInsideHeights(Random var1, int var2, int var3) {
      BoundingBox var4 = this.getBoundingBox();
      int var5 = var3 - var2 + 1 - var4.getYSpan();
      int var6;
      if (var5 > 1) {
         var6 = var2 + var1.nextInt(var5);
      } else {
         var6 = var2;
      }

      int var7 = var6 - var4.minY();
      this.offsetPiecesVertically(var7);
   }

   protected void offsetPiecesVertically(int var1) {
      Iterator var2 = this.pieces.iterator();

      while(var2.hasNext()) {
         StructurePiece var3 = (StructurePiece)var2.next();
         var3.move(0, var1, 0);
      }

      this.invalidateCache();
   }

   private void invalidateCache() {
      this.cachedBoundingBox = null;
   }

   public boolean isValid() {
      return !this.pieces.isEmpty();
   }

   public ChunkPos getChunkPos() {
      return this.chunkPos;
   }

   public BlockPos getLocatePos() {
      return new BlockPos(this.chunkPos.getMinBlockX(), 0, this.chunkPos.getMinBlockZ());
   }

   public boolean canBeReferenced() {
      return this.references < this.getMaxReferences();
   }

   public void addReference() {
      ++this.references;
   }

   public int getReferences() {
      return this.references;
   }

   protected int getMaxReferences() {
      return 1;
   }

   public StructureFeature<?> getFeature() {
      return this.feature;
   }

   public void addPiece(StructurePiece var1) {
      this.pieces.add(var1);
      this.invalidateCache();
   }

   @Nullable
   public StructurePiece findCollisionPiece(BoundingBox var1) {
      return findCollisionPiece(this.pieces, var1);
   }

   public void clearPieces() {
      this.pieces.clear();
      this.invalidateCache();
   }

   public boolean hasNoPieces() {
      return this.pieces.isEmpty();
   }

   @Nullable
   public static StructurePiece findCollisionPiece(List<StructurePiece> var0, BoundingBox var1) {
      Iterator var2 = var0.iterator();

      StructurePiece var3;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         var3 = (StructurePiece)var2.next();
      } while(!var3.getBoundingBox().intersects(var1));

      return var3;
   }

   protected boolean isInsidePiece(BlockPos var1) {
      Iterator var2 = this.pieces.iterator();

      StructurePiece var3;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         var3 = (StructurePiece)var2.next();
      } while(!var3.getBoundingBox().isInside(var1));

      return true;
   }
}
