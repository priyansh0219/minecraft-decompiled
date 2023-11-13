package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.world.level.levelgen.feature.configurations.RangeDecoratorConfiguration;

public class RangeDecorator extends VerticalDecorator<RangeDecoratorConfiguration> {
   public RangeDecorator(Codec<RangeDecoratorConfiguration> var1) {
      super(var1);
   }

   protected int y(DecorationContext var1, Random var2, RangeDecoratorConfiguration var3, int var4) {
      return var3.height.sample(var2, var1);
   }
}
