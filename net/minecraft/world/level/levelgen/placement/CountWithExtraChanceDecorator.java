package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;

public class CountWithExtraChanceDecorator extends RepeatingDecorator<FrequencyWithExtraChanceDecoratorConfiguration> {
   public CountWithExtraChanceDecorator(Codec<FrequencyWithExtraChanceDecoratorConfiguration> var1) {
      super(var1);
   }

   protected int count(Random var1, FrequencyWithExtraChanceDecoratorConfiguration var2, BlockPos var3) {
      return var2.count + (var1.nextFloat() < var2.extraChance ? var2.extraCount : 0);
   }
}
