package net.minecraft.world.level.levelgen;

import net.minecraft.world.level.block.state.BlockState;

public class SingleBaseStoneSource implements BaseStoneSource {
   private final BlockState state;

   public SingleBaseStoneSource(BlockState var1) {
      this.state = var1;
   }

   public BlockState getBaseBlock(int var1, int var2, int var3) {
      return this.state;
   }
}
