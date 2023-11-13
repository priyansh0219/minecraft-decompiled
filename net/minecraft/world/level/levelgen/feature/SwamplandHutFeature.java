package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.SwamplandHutPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class SwamplandHutFeature extends StructureFeature<NoneFeatureConfiguration> {
   private static final WeightedRandomList<MobSpawnSettings.SpawnerData> SWAMPHUT_ENEMIES;
   private static final WeightedRandomList<MobSpawnSettings.SpawnerData> SWAMPHUT_ANIMALS;

   public SwamplandHutFeature(Codec<NoneFeatureConfiguration> var1) {
      super(var1);
   }

   public StructureFeature.StructureStartFactory<NoneFeatureConfiguration> getStartFactory() {
      return SwamplandHutFeature.FeatureStart::new;
   }

   public WeightedRandomList<MobSpawnSettings.SpawnerData> getSpecialEnemies() {
      return SWAMPHUT_ENEMIES;
   }

   public WeightedRandomList<MobSpawnSettings.SpawnerData> getSpecialAnimals() {
      return SWAMPHUT_ANIMALS;
   }

   static {
      SWAMPHUT_ENEMIES = WeightedRandomList.create((WeightedEntry[])(new MobSpawnSettings.SpawnerData(EntityType.WITCH, 1, 1, 1)));
      SWAMPHUT_ANIMALS = WeightedRandomList.create((WeightedEntry[])(new MobSpawnSettings.SpawnerData(EntityType.CAT, 1, 1, 1)));
   }

   public static class FeatureStart extends StructureStart<NoneFeatureConfiguration> {
      public FeatureStart(StructureFeature<NoneFeatureConfiguration> var1, ChunkPos var2, int var3, long var4) {
         super(var1, var2, var3, var4);
      }

      public void generatePieces(RegistryAccess var1, ChunkGenerator var2, StructureManager var3, ChunkPos var4, Biome var5, NoneFeatureConfiguration var6, LevelHeightAccessor var7) {
         SwamplandHutPiece var8 = new SwamplandHutPiece(this.random, var4.getMinBlockX(), var4.getMinBlockZ());
         this.addPiece(var8);
      }
   }
}
