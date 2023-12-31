package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.configurations.NoneDecoratorConfiguration;

public class IcebergPlacementDecorator extends FeatureDecorator<NoneDecoratorConfiguration> {
   public IcebergPlacementDecorator(Codec<NoneDecoratorConfiguration> var1) {
      super(var1);
   }

   public Stream<BlockPos> getPositions(DecorationContext var1, Random var2, NoneDecoratorConfiguration var3, BlockPos var4) {
      int var5 = var2.nextInt(8) + 4 + var4.getX();
      int var6 = var2.nextInt(8) + 4 + var4.getZ();
      return Stream.of(new BlockPos(var5, var4.getY(), var6));
   }
}
