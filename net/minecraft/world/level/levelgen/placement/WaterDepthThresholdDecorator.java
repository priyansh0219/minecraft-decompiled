package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;

public class WaterDepthThresholdDecorator extends FeatureDecorator<WaterDepthThresholdConfiguration> {
   public WaterDepthThresholdDecorator(Codec<WaterDepthThresholdConfiguration> var1) {
      super(var1);
   }

   public Stream<BlockPos> getPositions(DecorationContext var1, Random var2, WaterDepthThresholdConfiguration var3, BlockPos var4) {
      int var5 = var1.getHeight(Heightmap.Types.OCEAN_FLOOR, var4.getX(), var4.getZ());
      int var6 = var1.getHeight(Heightmap.Types.WORLD_SURFACE, var4.getX(), var4.getZ());
      return var6 - var5 > var3.maxWaterDepth ? Stream.of() : Stream.of(var4);
   }
}
