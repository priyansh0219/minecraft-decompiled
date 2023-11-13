package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.DesertPyramidPiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class DesertPyramidFeature extends StructureFeature<NoneFeatureConfiguration> {
   public DesertPyramidFeature(Codec<NoneFeatureConfiguration> var1) {
      super(var1);
   }

   public StructureFeature.StructureStartFactory<NoneFeatureConfiguration> getStartFactory() {
      return DesertPyramidFeature.FeatureStart::new;
   }

   public static class FeatureStart extends StructureStart<NoneFeatureConfiguration> {
      public FeatureStart(StructureFeature<NoneFeatureConfiguration> var1, ChunkPos var2, int var3, long var4) {
         super(var1, var2, var3, var4);
      }

      public void generatePieces(RegistryAccess var1, ChunkGenerator var2, StructureManager var3, ChunkPos var4, Biome var5, NoneFeatureConfiguration var6, LevelHeightAccessor var7) {
         DesertPyramidPiece var8 = new DesertPyramidPiece(this.random, var4.getMinBlockX(), var4.getMinBlockZ());
         this.addPiece(var8);
      }
   }
}
