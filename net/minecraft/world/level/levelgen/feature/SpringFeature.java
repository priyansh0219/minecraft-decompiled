package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;

public class SpringFeature extends Feature<SpringConfiguration> {
   public SpringFeature(Codec<SpringConfiguration> var1) {
      super(var1);
   }

   public boolean place(FeaturePlaceContext<SpringConfiguration> var1) {
      SpringConfiguration var2 = (SpringConfiguration)var1.config();
      WorldGenLevel var3 = var1.level();
      BlockPos var4 = var1.origin();
      if (!var2.validBlocks.contains(var3.getBlockState(var4.above()).getBlock())) {
         return false;
      } else if (var2.requiresBlockBelow && !var2.validBlocks.contains(var3.getBlockState(var4.below()).getBlock())) {
         return false;
      } else {
         BlockState var5 = var3.getBlockState(var4);
         if (!var5.isAir() && !var2.validBlocks.contains(var5.getBlock())) {
            return false;
         } else {
            int var6 = 0;
            int var7 = 0;
            if (var2.validBlocks.contains(var3.getBlockState(var4.west()).getBlock())) {
               ++var7;
            }

            if (var2.validBlocks.contains(var3.getBlockState(var4.east()).getBlock())) {
               ++var7;
            }

            if (var2.validBlocks.contains(var3.getBlockState(var4.north()).getBlock())) {
               ++var7;
            }

            if (var2.validBlocks.contains(var3.getBlockState(var4.south()).getBlock())) {
               ++var7;
            }

            if (var2.validBlocks.contains(var3.getBlockState(var4.below()).getBlock())) {
               ++var7;
            }

            int var8 = 0;
            if (var3.isEmptyBlock(var4.west())) {
               ++var8;
            }

            if (var3.isEmptyBlock(var4.east())) {
               ++var8;
            }

            if (var3.isEmptyBlock(var4.north())) {
               ++var8;
            }

            if (var3.isEmptyBlock(var4.south())) {
               ++var8;
            }

            if (var3.isEmptyBlock(var4.below())) {
               ++var8;
            }

            if (var7 == var2.rockCount && var8 == var2.holeCount) {
               var3.setBlock(var4, var2.state.createLegacyBlock(), 2);
               var3.getLiquidTicks().scheduleTick(var4, var2.state.getType(), 0);
               ++var6;
            }

            return var6 > 0;
         }
      }
   }
}
