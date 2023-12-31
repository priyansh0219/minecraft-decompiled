package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Column;

public class CaveSurfaceDecorator extends FeatureDecorator<CaveDecoratorConfiguration> {
   public CaveSurfaceDecorator(Codec<CaveDecoratorConfiguration> var1) {
      super(var1);
   }

   public Stream<BlockPos> getPositions(DecorationContext var1, Random var2, CaveDecoratorConfiguration var3, BlockPos var4) {
      Optional var5 = Column.scan(var1.getLevel(), var4, var3.floorToCeilingSearchRange, BlockBehaviour.BlockStateBase::isAir, (var0) -> {
         return var0.getMaterial().isSolid();
      });
      if (!var5.isPresent()) {
         return Stream.of();
      } else {
         OptionalInt var6 = var3.surface == CaveSurface.CEILING ? ((Column)var5.get()).getCeiling() : ((Column)var5.get()).getFloor();
         return !var6.isPresent() ? Stream.of() : Stream.of(var4.atY(var6.getAsInt() - var3.surface.getY()));
      }
   }
}
