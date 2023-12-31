package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratorConfiguration;

public abstract class VerticalDecorator<DC extends DecoratorConfiguration> extends FeatureDecorator<DC> {
   public VerticalDecorator(Codec<DC> var1) {
      super(var1);
   }

   protected abstract int y(DecorationContext var1, Random var2, DC var3, int var4);

   public final Stream<BlockPos> getPositions(DecorationContext var1, Random var2, DC var3, BlockPos var4) {
      return Stream.of(new BlockPos(var4.getX(), this.y(var1, var2, var3, var4.getY()), var4.getZ()));
   }
}
