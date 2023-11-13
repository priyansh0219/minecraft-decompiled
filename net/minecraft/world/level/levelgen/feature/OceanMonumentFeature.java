package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.OceanMonumentPieces;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class OceanMonumentFeature extends StructureFeature<NoneFeatureConfiguration> {
   private static final WeightedRandomList<MobSpawnSettings.SpawnerData> MONUMENT_ENEMIES;

   public OceanMonumentFeature(Codec<NoneFeatureConfiguration> var1) {
      super(var1);
   }

   protected boolean linearSeparation() {
      return false;
   }

   protected boolean isFeatureChunk(ChunkGenerator var1, BiomeSource var2, long var3, WorldgenRandom var5, ChunkPos var6, Biome var7, ChunkPos var8, NoneFeatureConfiguration var9, LevelHeightAccessor var10) {
      int var11 = var6.getBlockX(9);
      int var12 = var6.getBlockZ(9);
      Set var13 = var2.getBiomesWithin(var11, var1.getSeaLevel(), var12, 16);
      Iterator var14 = var13.iterator();

      Biome var15;
      do {
         if (!var14.hasNext()) {
            Set var17 = var2.getBiomesWithin(var11, var1.getSeaLevel(), var12, 29);
            Iterator var18 = var17.iterator();

            Biome var16;
            do {
               if (!var18.hasNext()) {
                  return true;
               }

               var16 = (Biome)var18.next();
            } while(var16.getBiomeCategory() == Biome.BiomeCategory.OCEAN || var16.getBiomeCategory() == Biome.BiomeCategory.RIVER);

            return false;
         }

         var15 = (Biome)var14.next();
      } while(var15.getGenerationSettings().isValidStart(this));

      return false;
   }

   public StructureFeature.StructureStartFactory<NoneFeatureConfiguration> getStartFactory() {
      return OceanMonumentFeature.OceanMonumentStart::new;
   }

   public WeightedRandomList<MobSpawnSettings.SpawnerData> getSpecialEnemies() {
      return MONUMENT_ENEMIES;
   }

   static {
      MONUMENT_ENEMIES = WeightedRandomList.create((WeightedEntry[])(new MobSpawnSettings.SpawnerData(EntityType.GUARDIAN, 1, 2, 4)));
   }

   public static class OceanMonumentStart extends StructureStart<NoneFeatureConfiguration> {
      private boolean isCreated;

      public OceanMonumentStart(StructureFeature<NoneFeatureConfiguration> var1, ChunkPos var2, int var3, long var4) {
         super(var1, var2, var3, var4);
      }

      public void generatePieces(RegistryAccess var1, ChunkGenerator var2, StructureManager var3, ChunkPos var4, Biome var5, NoneFeatureConfiguration var6, LevelHeightAccessor var7) {
         this.generatePieces(var4);
      }

      private void generatePieces(ChunkPos var1) {
         int var2 = var1.getMinBlockX() - 29;
         int var3 = var1.getMinBlockZ() - 29;
         Direction var4 = Direction.Plane.HORIZONTAL.getRandomDirection(this.random);
         this.addPiece(new OceanMonumentPieces.MonumentBuilding(this.random, var2, var3, var4));
         this.isCreated = true;
      }

      public void placeInChunk(WorldGenLevel var1, StructureFeatureManager var2, ChunkGenerator var3, Random var4, BoundingBox var5, ChunkPos var6) {
         if (!this.isCreated) {
            this.pieces.clear();
            this.generatePieces(this.getChunkPos());
         }

         super.placeInChunk(var1, var2, var3, var4, var5, var6);
      }
   }
}
