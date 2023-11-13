package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.feature.structures.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.NoiseAffectingStructureStart;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class JigsawFeature extends StructureFeature<JigsawConfiguration> {
   final int startY;
   final boolean doExpansionHack;
   final boolean projectStartToHeightmap;

   public JigsawFeature(Codec<JigsawConfiguration> var1, int var2, boolean var3, boolean var4) {
      super(var1);
      this.startY = var2;
      this.doExpansionHack = var3;
      this.projectStartToHeightmap = var4;
   }

   public StructureFeature.StructureStartFactory<JigsawConfiguration> getStartFactory() {
      return (var1, var2, var3, var4) -> {
         return new JigsawFeature.FeatureStart(this, var2, var3, var4);
      };
   }

   public static class FeatureStart extends NoiseAffectingStructureStart<JigsawConfiguration> {
      private final JigsawFeature feature;

      public FeatureStart(JigsawFeature var1, ChunkPos var2, int var3, long var4) {
         super(var1, var2, var3, var4);
         this.feature = var1;
      }

      public void generatePieces(RegistryAccess var1, ChunkGenerator var2, StructureManager var3, ChunkPos var4, Biome var5, JigsawConfiguration var6, LevelHeightAccessor var7) {
         BlockPos var8 = new BlockPos(var4.getMinBlockX(), this.feature.startY, var4.getMinBlockZ());
         Pools.bootstrap();
         JigsawPlacement.addPieces(var1, var6, PoolElementStructurePiece::new, var2, var3, var8, this, this.random, this.feature.doExpansionHack, this.feature.projectStartToHeightmap, var7);
      }
   }
}
