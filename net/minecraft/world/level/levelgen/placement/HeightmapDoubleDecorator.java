package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.configurations.HeightmapConfiguration;

public class HeightmapDoubleDecorator extends FeatureDecorator<HeightmapConfiguration> {
   public HeightmapDoubleDecorator(Codec<HeightmapConfiguration> var1) {
      super(var1);
   }

   public Stream<BlockPos> getPositions(DecorationContext var1, Random var2, HeightmapConfiguration var3, BlockPos var4) {
      int var5 = var4.getX();
      int var6 = var4.getZ();
      int var7 = var1.getHeight(var3.heightmap, var5, var6);
      return var7 == var1.getMinBuildHeight() ? Stream.of() : Stream.of(new BlockPos(var5, var1.getMinBuildHeight() + var2.nextInt((var7 - var1.getMinBuildHeight()) * 2), var6));
   }
}
