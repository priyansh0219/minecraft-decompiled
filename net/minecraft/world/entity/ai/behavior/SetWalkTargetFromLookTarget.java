package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class SetWalkTargetFromLookTarget extends Behavior<LivingEntity> {
   private final Function<LivingEntity, Float> speedModifier;
   private final int closeEnoughDistance;

   public SetWalkTargetFromLookTarget(float var1, int var2) {
      this((var1x) -> {
         return var1;
      }, var2);
   }

   public SetWalkTargetFromLookTarget(Function<LivingEntity, Float> var1, int var2) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_PRESENT));
      this.speedModifier = var1;
      this.closeEnoughDistance = var2;
   }

   protected void start(ServerLevel var1, LivingEntity var2, long var3) {
      Brain var5 = var2.getBrain();
      PositionTracker var6 = (PositionTracker)var5.getMemory(MemoryModuleType.LOOK_TARGET).get();
      var5.setMemory(MemoryModuleType.WALK_TARGET, (Object)(new WalkTarget(var6, (Float)this.speedModifier.apply(var2), this.closeEnoughDistance)));
   }
}
