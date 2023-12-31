package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Iterator;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;

public class RandomSelectorFeature extends Feature<RandomFeatureConfiguration> {
   public RandomSelectorFeature(Codec<RandomFeatureConfiguration> var1) {
      super(var1);
   }

   public boolean place(FeaturePlaceContext<RandomFeatureConfiguration> var1) {
      RandomFeatureConfiguration var2 = (RandomFeatureConfiguration)var1.config();
      Random var3 = var1.random();
      WorldGenLevel var4 = var1.level();
      ChunkGenerator var5 = var1.chunkGenerator();
      BlockPos var6 = var1.origin();
      Iterator var7 = var2.features.iterator();

      WeightedConfiguredFeature var8;
      do {
         if (!var7.hasNext()) {
            return ((ConfiguredFeature)var2.defaultFeature.get()).place(var4, var5, var3, var6);
         }

         var8 = (WeightedConfiguredFeature)var7.next();
      } while(!(var3.nextFloat() < var8.chance));

      return var8.place(var4, var5, var3, var6);
   }
}
