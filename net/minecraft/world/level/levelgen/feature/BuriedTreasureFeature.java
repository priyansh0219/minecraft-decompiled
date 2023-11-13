package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BuriedTreasurePieces;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class BuriedTreasureFeature extends StructureFeature<ProbabilityFeatureConfiguration> {
   private static final int RANDOM_SALT = 10387320;

   public BuriedTreasureFeature(Codec<ProbabilityFeatureConfiguration> var1) {
      super(var1);
   }

   protected boolean isFeatureChunk(ChunkGenerator var1, BiomeSource var2, long var3, WorldgenRandom var5, ChunkPos var6, Biome var7, ChunkPos var8, ProbabilityFeatureConfiguration var9, LevelHeightAccessor var10) {
      var5.setLargeFeatureWithSalt(var3, var6.x, var6.z, 10387320);
      return var5.nextFloat() < var9.probability;
   }

   public StructureFeature.StructureStartFactory<ProbabilityFeatureConfiguration> getStartFactory() {
      return BuriedTreasureFeature.BuriedTreasureStart::new;
   }

   public static class BuriedTreasureStart extends StructureStart<ProbabilityFeatureConfiguration> {
      public BuriedTreasureStart(StructureFeature<ProbabilityFeatureConfiguration> var1, ChunkPos var2, int var3, long var4) {
         super(var1, var2, var3, var4);
      }

      public void generatePieces(RegistryAccess var1, ChunkGenerator var2, StructureManager var3, ChunkPos var4, Biome var5, ProbabilityFeatureConfiguration var6, LevelHeightAccessor var7) {
         BlockPos var8 = new BlockPos(var4.getBlockX(9), 90, var4.getBlockZ(9));
         this.addPiece(new BuriedTreasurePieces.BuriedTreasurePiece(var8));
      }

      public BlockPos getLocatePos() {
         ChunkPos var1 = this.getChunkPos();
         return new BlockPos(var1.getBlockX(9), 0, var1.getBlockZ(9));
      }
   }
}
