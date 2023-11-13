package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class NoiseColumn {
   private final int minY;
   private final BlockState[] column;

   public NoiseColumn(int var1, BlockState[] var2) {
      this.minY = var1;
      this.column = var2;
   }

   public BlockState getBlockState(BlockPos var1) {
      int var2 = var1.getY() - this.minY;
      return var2 >= 0 && var2 < this.column.length ? this.column[var2] : Blocks.AIR.defaultBlockState();
   }
}
