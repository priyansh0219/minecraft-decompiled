package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.WoodlandMansionPieces;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class WoodlandMansionFeature extends StructureFeature<NoneFeatureConfiguration> {
   public WoodlandMansionFeature(Codec<NoneFeatureConfiguration> var1) {
      super(var1);
   }

   protected boolean linearSeparation() {
      return false;
   }

   protected boolean isFeatureChunk(ChunkGenerator var1, BiomeSource var2, long var3, WorldgenRandom var5, ChunkPos var6, Biome var7, ChunkPos var8, NoneFeatureConfiguration var9, LevelHeightAccessor var10) {
      Set var11 = var2.getBiomesWithin(var6.getBlockX(9), var1.getSeaLevel(), var6.getBlockZ(9), 32);
      Iterator var12 = var11.iterator();

      Biome var13;
      do {
         if (!var12.hasNext()) {
            return true;
         }

         var13 = (Biome)var12.next();
      } while(var13.getGenerationSettings().isValidStart(this));

      return false;
   }

   public StructureFeature.StructureStartFactory<NoneFeatureConfiguration> getStartFactory() {
      return WoodlandMansionFeature.WoodlandMansionStart::new;
   }

   public static class WoodlandMansionStart extends StructureStart<NoneFeatureConfiguration> {
      public WoodlandMansionStart(StructureFeature<NoneFeatureConfiguration> var1, ChunkPos var2, int var3, long var4) {
         super(var1, var2, var3, var4);
      }

      public void generatePieces(RegistryAccess var1, ChunkGenerator var2, StructureManager var3, ChunkPos var4, Biome var5, NoneFeatureConfiguration var6, LevelHeightAccessor var7) {
         Rotation var8 = Rotation.getRandom(this.random);
         byte var9 = 5;
         byte var10 = 5;
         if (var8 == Rotation.CLOCKWISE_90) {
            var9 = -5;
         } else if (var8 == Rotation.CLOCKWISE_180) {
            var9 = -5;
            var10 = -5;
         } else if (var8 == Rotation.COUNTERCLOCKWISE_90) {
            var10 = -5;
         }

         int var11 = var4.getBlockX(7);
         int var12 = var4.getBlockZ(7);
         int var13 = var2.getFirstOccupiedHeight(var11, var12, Heightmap.Types.WORLD_SURFACE_WG, var7);
         int var14 = var2.getFirstOccupiedHeight(var11, var12 + var10, Heightmap.Types.WORLD_SURFACE_WG, var7);
         int var15 = var2.getFirstOccupiedHeight(var11 + var9, var12, Heightmap.Types.WORLD_SURFACE_WG, var7);
         int var16 = var2.getFirstOccupiedHeight(var11 + var9, var12 + var10, Heightmap.Types.WORLD_SURFACE_WG, var7);
         int var17 = Math.min(Math.min(var13, var14), Math.min(var15, var16));
         if (var17 >= 60) {
            BlockPos var18 = new BlockPos(var4.getBlockX(8), var17 + 1, var4.getBlockZ(8));
            LinkedList var19 = Lists.newLinkedList();
            WoodlandMansionPieces.generateMansion(var3, var18, var8, var19, this.random);
            var19.forEach(this::addPiece);
         }
      }

      public void placeInChunk(WorldGenLevel var1, StructureFeatureManager var2, ChunkGenerator var3, Random var4, BoundingBox var5, ChunkPos var6) {
         super.placeInChunk(var1, var2, var3, var4, var5, var6);
         BoundingBox var7 = this.getBoundingBox();
         int var8 = var7.minY();

         for(int var9 = var5.minX(); var9 <= var5.maxX(); ++var9) {
            for(int var10 = var5.minZ(); var10 <= var5.maxZ(); ++var10) {
               BlockPos var11 = new BlockPos(var9, var8, var10);
               if (!var1.isEmptyBlock(var11) && var7.isInside(var11) && this.isInsidePiece(var11)) {
                  for(int var12 = var8 - 1; var12 > 1; --var12) {
                     BlockPos var13 = new BlockPos(var9, var12, var10);
                     if (!var1.isEmptyBlock(var13) && !var1.getBlockState(var13).getMaterial().isLiquid()) {
                        break;
                     }

                     var1.setBlock(var13, Blocks.COBBLESTONE.defaultBlockState(), 2);
                  }
               }
            }
         }

      }
   }
}
