package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

public class NoiseBasedDecorator extends RepeatingDecorator<NoiseCountFactorDecoratorConfiguration> {
   public NoiseBasedDecorator(Codec<NoiseCountFactorDecoratorConfiguration> var1) {
      super(var1);
   }

   protected int count(Random var1, NoiseCountFactorDecoratorConfiguration var2, BlockPos var3) {
      double var4 = Biome.BIOME_INFO_NOISE.getValue((double)var3.getX() / var2.noiseFactor, (double)var3.getZ() / var2.noiseFactor, false);
      return (int)Math.ceil((var4 + var2.noiseOffset) * (double)var2.noiseToCountRatio);
   }
}
