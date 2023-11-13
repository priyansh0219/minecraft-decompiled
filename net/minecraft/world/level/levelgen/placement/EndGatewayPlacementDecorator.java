package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.world.level.levelgen.feature.configurations.NoneDecoratorConfiguration;

public class EndGatewayPlacementDecorator extends VerticalDecorator<NoneDecoratorConfiguration> {
   public EndGatewayPlacementDecorator(Codec<NoneDecoratorConfiguration> var1) {
      super(var1);
   }

   protected int y(DecorationContext var1, Random var2, NoneDecoratorConfiguration var3, int var4) {
      return var4 + 3 + var2.nextInt(7);
   }
}
