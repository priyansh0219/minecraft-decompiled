package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;

public class PillagerOutpostFeature extends JigsawFeature {
   private static final WeightedRandomList<MobSpawnSettings.SpawnerData> OUTPOST_ENEMIES;

   public PillagerOutpostFeature(Codec<JigsawConfiguration> var1) {
      super(var1, 0, true, true);
   }

   public WeightedRandomList<MobSpawnSettings.SpawnerData> getSpecialEnemies() {
      return OUTPOST_ENEMIES;
   }

   protected boolean isFeatureChunk(ChunkGenerator var1, BiomeSource var2, long var3, WorldgenRandom var5, ChunkPos var6, Biome var7, ChunkPos var8, JigsawConfiguration var9, LevelHeightAccessor var10) {
      int var11 = var6.x >> 4;
      int var12 = var6.z >> 4;
      var5.setSeed((long)(var11 ^ var12 << 4) ^ var3);
      var5.nextInt();
      if (var5.nextInt(5) != 0) {
         return false;
      } else {
         return !this.isNearVillage(var1, var3, var5, var6);
      }
   }

   private boolean isNearVillage(ChunkGenerator var1, long var2, WorldgenRandom var4, ChunkPos var5) {
      StructureFeatureConfiguration var6 = var1.getSettings().getConfig(StructureFeature.VILLAGE);
      if (var6 == null) {
         return false;
      } else {
         int var7 = var5.x;
         int var8 = var5.z;

         for(int var9 = var7 - 10; var9 <= var7 + 10; ++var9) {
            for(int var10 = var8 - 10; var10 <= var8 + 10; ++var10) {
               ChunkPos var11 = StructureFeature.VILLAGE.getPotentialFeatureChunk(var6, var2, var4, var9, var10);
               if (var9 == var11.x && var10 == var11.z) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   static {
      OUTPOST_ENEMIES = WeightedRandomList.create((WeightedEntry[])(new MobSpawnSettings.SpawnerData(EntityType.PILLAGER, 1, 1, 1)));
   }
}
