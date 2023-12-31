package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Iterator;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.SmallDripstoneConfiguration;

public class SmallDripstoneFeature extends Feature<SmallDripstoneConfiguration> {
   public SmallDripstoneFeature(Codec<SmallDripstoneConfiguration> var1) {
      super(var1);
   }

   public boolean place(FeaturePlaceContext<SmallDripstoneConfiguration> var1) {
      WorldGenLevel var2 = var1.level();
      BlockPos var3 = var1.origin();
      Random var4 = var1.random();
      SmallDripstoneConfiguration var5 = (SmallDripstoneConfiguration)var1.config();
      if (!DripstoneUtils.isEmptyOrWater(var2, var3)) {
         return false;
      } else {
         int var6 = Mth.randomBetweenInclusive(var4, 1, var5.maxPlacements);
         boolean var7 = false;

         for(int var8 = 0; var8 < var6; ++var8) {
            BlockPos var9 = randomOffset(var4, var3, var5);
            if (searchAndTryToPlaceDripstone(var2, var4, var9, var5)) {
               var7 = true;
            }
         }

         return var7;
      }
   }

   private static boolean searchAndTryToPlaceDripstone(WorldGenLevel var0, Random var1, BlockPos var2, SmallDripstoneConfiguration var3) {
      Direction var4 = Direction.getRandom(var1);
      Direction var5 = var1.nextBoolean() ? Direction.UP : Direction.DOWN;
      BlockPos.MutableBlockPos var6 = var2.mutable();

      for(int var7 = 0; var7 < var3.emptySpaceSearchRadius; ++var7) {
         if (!DripstoneUtils.isEmptyOrWater(var0, var6)) {
            return false;
         }

         if (tryToPlaceDripstone(var0, var1, var6, var5, var3)) {
            return true;
         }

         if (tryToPlaceDripstone(var0, var1, var6, var5.getOpposite(), var3)) {
            return true;
         }

         var6.move(var4);
      }

      return false;
   }

   private static boolean tryToPlaceDripstone(WorldGenLevel var0, Random var1, BlockPos var2, Direction var3, SmallDripstoneConfiguration var4) {
      if (!DripstoneUtils.isEmptyOrWater(var0, var2)) {
         return false;
      } else {
         BlockPos var5 = var2.relative(var3.getOpposite());
         BlockState var6 = var0.getBlockState(var5);
         if (!DripstoneUtils.isDripstoneBase(var6)) {
            return false;
         } else {
            createPatchOfDripstoneBlocks(var0, var1, var5);
            int var7 = var1.nextFloat() < var4.chanceOfTallerDripstone && DripstoneUtils.isEmptyOrWater(var0, var2.relative(var3)) ? 2 : 1;
            DripstoneUtils.growPointedDripstone(var0, var2, var3, var7, false);
            return true;
         }
      }
   }

   private static void createPatchOfDripstoneBlocks(WorldGenLevel var0, Random var1, BlockPos var2) {
      DripstoneUtils.placeDripstoneBlockIfPossible(var0, var2);
      Iterator var3 = Direction.Plane.HORIZONTAL.iterator();

      while(var3.hasNext()) {
         Direction var4 = (Direction)var3.next();
         if (!(var1.nextFloat() < 0.3F)) {
            BlockPos var5 = var2.relative(var4);
            DripstoneUtils.placeDripstoneBlockIfPossible(var0, var5);
            if (!var1.nextBoolean()) {
               BlockPos var6 = var5.relative(Direction.getRandom(var1));
               DripstoneUtils.placeDripstoneBlockIfPossible(var0, var6);
               if (!var1.nextBoolean()) {
                  BlockPos var7 = var6.relative(Direction.getRandom(var1));
                  DripstoneUtils.placeDripstoneBlockIfPossible(var0, var7);
               }
            }
         }
      }

   }

   private static BlockPos randomOffset(Random var0, BlockPos var1, SmallDripstoneConfiguration var2) {
      return var1.offset(Mth.randomBetweenInclusive(var0, -var2.maxOffsetFromOrigin, var2.maxOffsetFromOrigin), Mth.randomBetweenInclusive(var0, -var2.maxOffsetFromOrigin, var2.maxOffsetFromOrigin), Mth.randomBetweenInclusive(var0, -var2.maxOffsetFromOrigin, var2.maxOffsetFromOrigin));
   }
}
