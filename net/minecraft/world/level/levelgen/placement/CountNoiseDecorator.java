package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.configurations.NoiseDependantDecoratorConfiguration;

public class CountNoiseDecorator extends RepeatingDecorator<NoiseDependantDecoratorConfiguration> {
   public CountNoiseDecorator(Codec<NoiseDependantDecoratorConfiguration> var1) {
      super(var1);
   }

   protected int count(Random var1, NoiseDependantDecoratorConfiguration var2, BlockPos var3) {
      double var4 = Biome.BIOME_INFO_NOISE.getValue((double)var3.getX() / 200.0D, (double)var3.getZ() / 200.0D, false);
      return var4 < var2.noiseLevel ? var2.belowNoise : var2.aboveNoise;
   }
}
