package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class CauldronBlock extends AbstractCauldronBlock {
   public CauldronBlock(BlockBehaviour.Properties var1) {
      super(var1, CauldronInteraction.EMPTY);
   }

   public boolean isFull(BlockState var1) {
      return false;
   }

   protected static boolean shouldHandlePrecipitation(Level var0) {
      return var0.random.nextInt(20) == 1;
   }

   public void handlePrecipitation(BlockState var1, Level var2, BlockPos var3, Biome.Precipitation var4) {
      if (shouldHandlePrecipitation(var2)) {
         if (var4 == Biome.Precipitation.RAIN) {
            var2.setBlockAndUpdate(var3, Blocks.WATER_CAULDRON.defaultBlockState());
            var2.gameEvent((Entity)null, GameEvent.FLUID_PLACE, var3);
         } else if (var4 == Biome.Precipitation.SNOW) {
            var2.setBlockAndUpdate(var3, Blocks.POWDER_SNOW_CAULDRON.defaultBlockState());
            var2.gameEvent((Entity)null, GameEvent.FLUID_PLACE, var3);
         }

      }
   }

   protected boolean canReceiveStalactiteDrip(Fluid var1) {
      return true;
   }

   protected void receiveStalactiteDrip(BlockState var1, Level var2, BlockPos var3, Fluid var4) {
      if (var4 == Fluids.WATER) {
         var2.setBlockAndUpdate(var3, Blocks.WATER_CAULDRON.defaultBlockState());
         var2.levelEvent(1047, var3, 0);
         var2.gameEvent((Entity)null, GameEvent.FLUID_PLACE, var3);
      } else if (var4 == Fluids.LAVA) {
         var2.setBlockAndUpdate(var3, Blocks.LAVA_CAULDRON.defaultBlockState());
         var2.levelEvent(1046, var3, 0);
         var2.gameEvent((Entity)null, GameEvent.FLUID_PLACE, var3);
      }

   }
}
