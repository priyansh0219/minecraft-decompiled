package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;

public class RandomPatchFeature extends Feature<RandomPatchConfiguration> {
   public RandomPatchFeature(Codec<RandomPatchConfiguration> var1) {
      super(var1);
   }

   public boolean place(FeaturePlaceContext<RandomPatchConfiguration> var1) {
      RandomPatchConfiguration var2 = (RandomPatchConfiguration)var1.config();
      Random var3 = var1.random();
      BlockPos var4 = var1.origin();
      WorldGenLevel var5 = var1.level();
      BlockState var6 = var2.stateProvider.getState(var3, var4);
      BlockPos var7;
      if (var2.project) {
         var7 = var5.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, var4);
      } else {
         var7 = var4;
      }

      int var8 = 0;
      BlockPos.MutableBlockPos var9 = new BlockPos.MutableBlockPos();

      for(int var10 = 0; var10 < var2.tries; ++var10) {
         var9.setWithOffset(var7, var3.nextInt(var2.xspread + 1) - var3.nextInt(var2.xspread + 1), var3.nextInt(var2.yspread + 1) - var3.nextInt(var2.yspread + 1), var3.nextInt(var2.zspread + 1) - var3.nextInt(var2.zspread + 1));
         BlockPos var11 = var9.below();
         BlockState var12 = var5.getBlockState(var11);
         if ((var5.isEmptyBlock(var9) || var2.canReplace && var5.getBlockState(var9).getMaterial().isReplaceable()) && var6.canSurvive(var5, var9) && (var2.whitelist.isEmpty() || var2.whitelist.contains(var12.getBlock())) && !var2.blacklist.contains(var12) && (!var2.needWater || var5.getFluidState(var11.west()).is(FluidTags.WATER) || var5.getFluidState(var11.east()).is(FluidTags.WATER) || var5.getFluidState(var11.north()).is(FluidTags.WATER) || var5.getFluidState(var11.south()).is(FluidTags.WATER))) {
            var2.blockPlacer.place(var5, var9, var6, var3);
            ++var8;
         }
      }

      return var8 > 0;
   }
}
