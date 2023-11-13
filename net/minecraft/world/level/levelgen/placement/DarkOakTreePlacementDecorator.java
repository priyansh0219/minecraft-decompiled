package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.configurations.NoneDecoratorConfiguration;

public class DarkOakTreePlacementDecorator extends FeatureDecorator<NoneDecoratorConfiguration> {
   public DarkOakTreePlacementDecorator(Codec<NoneDecoratorConfiguration> var1) {
      super(var1);
   }

   public Stream<BlockPos> getPositions(DecorationContext var1, Random var2, NoneDecoratorConfiguration var3, BlockPos var4) {
      return IntStream.range(0, 16).mapToObj((var2x) -> {
         int var3 = var2x / 4;
         int var4x = var2x % 4;
         int var5 = var3 * 4 + 1 + var2.nextInt(3) + var4.getX();
         int var6 = var4x * 4 + 1 + var2.nextInt(3) + var4.getZ();
         return new BlockPos(var5, var4.getY(), var6);
      });
   }
}
