package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.EndCityPieces;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class EndCityFeature extends StructureFeature<NoneFeatureConfiguration> {
   private static final int RANDOM_SALT = 10387313;

   public EndCityFeature(Codec<NoneFeatureConfiguration> var1) {
      super(var1);
   }

   protected boolean linearSeparation() {
      return false;
   }

   protected boolean isFeatureChunk(ChunkGenerator var1, BiomeSource var2, long var3, WorldgenRandom var5, ChunkPos var6, Biome var7, ChunkPos var8, NoneFeatureConfiguration var9, LevelHeightAccessor var10) {
      return getYPositionForFeature(var6, var1, var10) >= 60;
   }

   public StructureFeature.StructureStartFactory<NoneFeatureConfiguration> getStartFactory() {
      return EndCityFeature.EndCityStart::new;
   }

   static int getYPositionForFeature(ChunkPos var0, ChunkGenerator var1, LevelHeightAccessor var2) {
      Random var3 = new Random((long)(var0.x + var0.z * 10387313));
      Rotation var4 = Rotation.getRandom(var3);
      byte var5 = 5;
      byte var6 = 5;
      if (var4 == Rotation.CLOCKWISE_90) {
         var5 = -5;
      } else if (var4 == Rotation.CLOCKWISE_180) {
         var5 = -5;
         var6 = -5;
      } else if (var4 == Rotation.COUNTERCLOCKWISE_90) {
         var6 = -5;
      }

      int var7 = var0.getBlockX(7);
      int var8 = var0.getBlockZ(7);
      int var9 = var1.getFirstOccupiedHeight(var7, var8, Heightmap.Types.WORLD_SURFACE_WG, var2);
      int var10 = var1.getFirstOccupiedHeight(var7, var8 + var6, Heightmap.Types.WORLD_SURFACE_WG, var2);
      int var11 = var1.getFirstOccupiedHeight(var7 + var5, var8, Heightmap.Types.WORLD_SURFACE_WG, var2);
      int var12 = var1.getFirstOccupiedHeight(var7 + var5, var8 + var6, Heightmap.Types.WORLD_SURFACE_WG, var2);
      return Math.min(Math.min(var9, var10), Math.min(var11, var12));
   }

   public static class EndCityStart extends StructureStart<NoneFeatureConfiguration> {
      public EndCityStart(StructureFeature<NoneFeatureConfiguration> var1, ChunkPos var2, int var3, long var4) {
         super(var1, var2, var3, var4);
      }

      public void generatePieces(RegistryAccess var1, ChunkGenerator var2, StructureManager var3, ChunkPos var4, Biome var5, NoneFeatureConfiguration var6, LevelHeightAccessor var7) {
         Rotation var8 = Rotation.getRandom(this.random);
         int var9 = EndCityFeature.getYPositionForFeature(var4, var2, var7);
         if (var9 >= 60) {
            BlockPos var10 = var4.getMiddleBlockPosition(var9);
            ArrayList var11 = Lists.newArrayList();
            EndCityPieces.startHouseTower(var3, var10, var8, var11, this.random);
            var11.forEach(this::addPiece);
         }
      }
   }
}
