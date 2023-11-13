package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;

public class ChanceDecorator extends RepeatingDecorator<ChanceDecoratorConfiguration> {
   public ChanceDecorator(Codec<ChanceDecoratorConfiguration> var1) {
      super(var1);
   }

   protected int count(Random var1, ChanceDecoratorConfiguration var2, BlockPos var3) {
      return var1.nextFloat() < 1.0F / (float)var2.chance ? 1 : 0;
   }
}
