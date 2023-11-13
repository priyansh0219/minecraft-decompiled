package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.configurations.CountConfiguration;

public class CountDecorator extends RepeatingDecorator<CountConfiguration> {
   public CountDecorator(Codec<CountConfiguration> var1) {
      super(var1);
   }

   protected int count(Random var1, CountConfiguration var2, BlockPos var3) {
      return var2.count().sample(var1);
   }
}
