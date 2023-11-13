package net.minecraft.world.level.levelgen;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class DepthBasedReplacingBaseStoneSource implements BaseStoneSource {
   private static final int ALWAYS_REPLACE_BELOW_Y = -8;
   private static final int NEVER_REPLACE_ABOVE_Y = 0;
   private final WorldgenRandom random;
   private final long seed;
   private final BlockState normalBlock;
   private final BlockState replacementBlock;
   private final NoiseGeneratorSettings settings;

   public DepthBasedReplacingBaseStoneSource(long var1, BlockState var3, BlockState var4, NoiseGeneratorSettings var5) {
      this.random = new WorldgenRandom(var1);
      this.seed = var1;
      this.normalBlock = var3;
      this.replacementBlock = var4;
      this.settings = var5;
   }

   public BlockState getBaseBlock(int var1, int var2, int var3) {
      if (!this.settings.isDeepslateEnabled()) {
         return this.normalBlock;
      } else if (var2 < -8) {
         return this.replacementBlock;
      } else if (var2 > 0) {
         return this.normalBlock;
      } else {
         double var4 = Mth.map((double)var2, -8.0D, 0.0D, 1.0D, 0.0D);
         this.random.setBaseStoneSeed(this.seed, var1, var2, var3);
         return (double)this.random.nextFloat() < var4 ? this.replacementBlock : this.normalBlock;
      }
   }
}
