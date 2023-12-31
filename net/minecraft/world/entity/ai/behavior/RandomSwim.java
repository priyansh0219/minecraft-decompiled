package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class RandomSwim extends RandomStroll {
   public RandomSwim(float var1) {
      super(var1);
   }

   protected boolean checkExtraStartConditions(ServerLevel var1, PathfinderMob var2) {
      return var2.isInWaterOrBubble();
   }

   protected Vec3 getTargetPos(PathfinderMob var1) {
      Vec3 var2 = BehaviorUtils.getRandomSwimmablePos(var1, this.maxHorizontalDistance, this.maxVerticalDistance);
      return var2 != null && var1.level.getFluidState(new BlockPos(var2)).isEmpty() ? null : var2;
   }
}
