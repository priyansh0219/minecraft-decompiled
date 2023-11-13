package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratedFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.DecorationContext;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class DecoratedFeature extends Feature<DecoratedFeatureConfiguration> {
   public DecoratedFeature(Codec<DecoratedFeatureConfiguration> var1) {
      super(var1);
   }

   public boolean place(FeaturePlaceContext<DecoratedFeatureConfiguration> var1) {
      MutableBoolean var2 = new MutableBoolean();
      WorldGenLevel var3 = var1.level();
      DecoratedFeatureConfiguration var4 = (DecoratedFeatureConfiguration)var1.config();
      ChunkGenerator var5 = var1.chunkGenerator();
      Random var6 = var1.random();
      BlockPos var7 = var1.origin();
      ConfiguredFeature var8 = (ConfiguredFeature)var4.feature.get();
      var4.decorator.getPositions(new DecorationContext(var3, var5), var6, var7).forEach((var5x) -> {
         if (var8.place(var3, var5, var6, var5x)) {
            var2.setTrue();
         }

      });
      return var2.isTrue();
   }

   public String toString() {
      return String.format("< %s [%s] >", this.getClass().getSimpleName(), Registry.FEATURE.getKey(this));
   }
}
