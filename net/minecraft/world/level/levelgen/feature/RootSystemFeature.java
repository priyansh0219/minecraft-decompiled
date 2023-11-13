package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.RootSystemConfiguration;

public class RootSystemFeature extends Feature<RootSystemConfiguration> {
   public RootSystemFeature(Codec<RootSystemConfiguration> var1) {
      super(var1);
   }

   public boolean place(FeaturePlaceContext<RootSystemConfiguration> var1) {
      WorldGenLevel var2 = var1.level();
      BlockPos var3 = var1.origin();
      if (!var2.getBlockState(var3).isAir()) {
         return false;
      } else {
         Random var4 = var1.random();
         BlockPos var5 = var1.origin();
         RootSystemConfiguration var6 = (RootSystemConfiguration)var1.config();
         BlockPos.MutableBlockPos var7 = var5.mutable();
         if (this.placeDirtAndTree(var2, var1.chunkGenerator(), var6, var4, var7, var5)) {
            this.placeRoots(var2, var6, var4, var5, var7);
         }

         return true;
      }
   }

   private boolean spaceForTree(WorldGenLevel var1, RootSystemConfiguration var2, BlockPos var3) {
      BlockPos.MutableBlockPos var4 = var3.mutable();

      for(int var5 = 1; var5 <= var2.requiredVerticalSpaceForTree; ++var5) {
         var4.move(Direction.UP);
         BlockState var6 = var1.getBlockState(var4);
         if (!isAllowedTreeSpace(var6, var5, var2.allowedVerticalWaterForTree)) {
            return false;
         }
      }

      return true;
   }

   private static boolean isAllowedTreeSpace(BlockState var0, int var1, int var2) {
      return var0.isAir() || var1 <= var2 && var0.getFluidState().is(FluidTags.WATER);
   }

   private boolean placeDirtAndTree(WorldGenLevel var1, ChunkGenerator var2, RootSystemConfiguration var3, Random var4, BlockPos.MutableBlockPos var5, BlockPos var6) {
      int var7 = var6.getX();
      int var8 = var6.getZ();

      for(int var9 = 0; var9 < var3.rootColumnMaxHeight; ++var9) {
         var5.move(Direction.UP);
         if (TreeFeature.validTreePos(var1, var5)) {
            if (this.spaceForTree(var1, var3, var5)) {
               BlockPos var10 = var5.below();
               if (var1.getFluidState(var10).is(FluidTags.LAVA) || !var1.getBlockState(var10).getMaterial().isSolid()) {
                  return false;
               }

               if (this.tryPlaceAzaleaTree(var1, var2, var3, var4, var5)) {
                  return true;
               }
            }
         } else {
            this.placeRootedDirt(var1, var3, var4, var7, var8, var5);
         }
      }

      return false;
   }

   private boolean tryPlaceAzaleaTree(WorldGenLevel var1, ChunkGenerator var2, RootSystemConfiguration var3, Random var4, BlockPos var5) {
      return ((ConfiguredFeature)var3.treeFeature.get()).place(var1, var2, var4, var5);
   }

   private void placeRootedDirt(WorldGenLevel var1, RootSystemConfiguration var2, Random var3, int var4, int var5, BlockPos.MutableBlockPos var6) {
      int var7 = var2.rootRadius;
      Tag var8 = BlockTags.getAllTags().getTag(var2.rootReplaceable);
      Predicate var9 = var8 == null ? (var0) -> {
         return true;
      } : (var1x) -> {
         return var1x.is(var8);
      };

      for(int var10 = 0; var10 < var2.rootPlacementAttempts; ++var10) {
         var6.setWithOffset(var6, var3.nextInt(var7) - var3.nextInt(var7), 0, var3.nextInt(var7) - var3.nextInt(var7));
         if (var9.test(var1.getBlockState(var6))) {
            var1.setBlock(var6, var2.rootStateProvider.getState(var3, var6), 2);
         }

         var6.setX(var4);
         var6.setZ(var5);
      }

   }

   private void placeRoots(WorldGenLevel var1, RootSystemConfiguration var2, Random var3, BlockPos var4, BlockPos.MutableBlockPos var5) {
      int var6 = var2.hangingRootRadius;
      int var7 = var2.hangingRootsVerticalSpan;

      for(int var8 = 0; var8 < var2.hangingRootPlacementAttempts; ++var8) {
         var5.setWithOffset(var4, var3.nextInt(var6) - var3.nextInt(var6), var3.nextInt(var7) - var3.nextInt(var7), var3.nextInt(var6) - var3.nextInt(var6));
         if (var1.isEmptyBlock(var5)) {
            BlockState var9 = var2.hangingRootStateProvider.getState(var3, var5);
            if (var9.canSurvive(var1, var5) && var1.getBlockState(var5.above()).isFaceSturdy(var1, var5, Direction.DOWN)) {
               var1.setBlock(var5, var9, 2);
            }
         }
      }

   }
}
