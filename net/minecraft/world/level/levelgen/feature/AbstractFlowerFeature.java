package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public abstract class AbstractFlowerFeature<U extends FeatureConfiguration> extends Feature<U> {
   public AbstractFlowerFeature(Codec<U> var1) {
      super(var1);
   }

   public boolean place(FeaturePlaceContext<U> var1) {
      Random var2 = var1.random();
      BlockPos var3 = var1.origin();
      WorldGenLevel var4 = var1.level();
      FeatureConfiguration var5 = var1.config();
      BlockState var6 = this.getRandomFlower(var2, var3, var5);
      int var7 = 0;

      for(int var8 = 0; var8 < this.getCount(var5); ++var8) {
         BlockPos var9 = this.getPos(var2, var3, var5);
         if (var4.isEmptyBlock(var9) && var6.canSurvive(var4, var9) && this.isValid(var4, var9, var5)) {
            var4.setBlock(var9, var6, 2);
            ++var7;
         }
      }

      return var7 > 0;
   }

   public abstract boolean isValid(LevelAccessor var1, BlockPos var2, U var3);

   public abstract int getCount(U var1);

   public abstract BlockPos getPos(Random var1, BlockPos var2, U var3);

   public abstract BlockState getRandomFlower(Random var1, BlockPos var2, U var3);
}
