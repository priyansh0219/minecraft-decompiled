package net.minecraft.world.entity.ai.navigation;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.SwimNodeEvaluator;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class WaterBoundPathNavigation extends PathNavigation {
   private boolean allowBreaching;

   public WaterBoundPathNavigation(Mob var1, Level var2) {
      super(var1, var2);
   }

   protected PathFinder createPathFinder(int var1) {
      this.allowBreaching = this.mob.getType() == EntityType.DOLPHIN;
      this.nodeEvaluator = new SwimNodeEvaluator(this.allowBreaching);
      return new PathFinder(this.nodeEvaluator, var1);
   }

   protected boolean canUpdatePath() {
      return this.allowBreaching || this.isInLiquid();
   }

   protected Vec3 getTempMobPos() {
      return new Vec3(this.mob.getX(), this.mob.getY(0.5D), this.mob.getZ());
   }

   public void tick() {
      ++this.tick;
      if (this.hasDelayedRecomputation) {
         this.recomputePath();
      }

      if (!this.isDone()) {
         Vec3 var1;
         if (this.canUpdatePath()) {
            this.followThePath();
         } else if (this.path != null && !this.path.isDone()) {
            var1 = this.path.getNextEntityPos(this.mob);
            if (this.mob.getBlockX() == Mth.floor(var1.x) && this.mob.getBlockY() == Mth.floor(var1.y) && this.mob.getBlockZ() == Mth.floor(var1.z)) {
               this.path.advance();
            }
         }

         DebugPackets.sendPathFindingPacket(this.level, this.mob, this.path, this.maxDistanceToWaypoint);
         if (!this.isDone()) {
            var1 = this.path.getNextEntityPos(this.mob);
            this.mob.getMoveControl().setWantedPosition(var1.x, var1.y, var1.z, this.speedModifier);
         }
      }
   }

   protected void followThePath() {
      if (this.path != null) {
         Vec3 var1 = this.getTempMobPos();
         float var2 = this.mob.getBbWidth();
         float var3 = var2 > 0.75F ? var2 / 2.0F : 0.75F - var2 / 2.0F;
         Vec3 var4 = this.mob.getDeltaMovement();
         if (Math.abs(var4.x) > 0.2D || Math.abs(var4.z) > 0.2D) {
            var3 = (float)((double)var3 * var4.length() * 6.0D);
         }

         boolean var5 = true;
         Vec3 var6 = Vec3.atBottomCenterOf(this.path.getNextNodePos());
         if (Math.abs(this.mob.getX() - var6.x) < (double)var3 && Math.abs(this.mob.getZ() - var6.z) < (double)var3 && Math.abs(this.mob.getY() - var6.y) < (double)(var3 * 2.0F)) {
            this.path.advance();
         }

         for(int var7 = Math.min(this.path.getNextNodeIndex() + 6, this.path.getNodeCount() - 1); var7 > this.path.getNextNodeIndex(); --var7) {
            var6 = this.path.getEntityPosAtNode(this.mob, var7);
            if (!(var6.distanceToSqr(var1) > 36.0D) && this.canMoveDirectly(var1, var6, 0, 0, 0)) {
               this.path.setNextNodeIndex(var7);
               break;
            }
         }

         this.doStuckDetection(var1);
      }
   }

   protected void doStuckDetection(Vec3 var1) {
      if (this.tick - this.lastStuckCheck > 100) {
         if (var1.distanceToSqr(this.lastStuckCheckPos) < 2.25D) {
            this.stop();
         }

         this.lastStuckCheck = this.tick;
         this.lastStuckCheckPos = var1;
      }

      if (this.path != null && !this.path.isDone()) {
         BlockPos var2 = this.path.getNextNodePos();
         if (var2.equals(this.timeoutCachedNode)) {
            this.timeoutTimer += Util.getMillis() - this.lastTimeoutCheck;
         } else {
            this.timeoutCachedNode = var2;
            double var3 = var1.distanceTo(Vec3.atCenterOf(this.timeoutCachedNode));
            this.timeoutLimit = this.mob.getSpeed() > 0.0F ? var3 / (double)this.mob.getSpeed() * 100.0D : 0.0D;
         }

         if (this.timeoutLimit > 0.0D && (double)this.timeoutTimer > this.timeoutLimit * 2.0D) {
            this.timeoutCachedNode = Vec3i.ZERO;
            this.timeoutTimer = 0L;
            this.timeoutLimit = 0.0D;
            this.stop();
         }

         this.lastTimeoutCheck = Util.getMillis();
      }

   }

   protected boolean canMoveDirectly(Vec3 var1, Vec3 var2, int var3, int var4, int var5) {
      Vec3 var6 = new Vec3(var2.x, var2.y + (double)this.mob.getBbHeight() * 0.5D, var2.z);
      return this.level.clip(new ClipContext(var1, var6, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob)).getType() == HitResult.Type.MISS;
   }

   public boolean isStableDestination(BlockPos var1) {
      return !this.level.getBlockState(var1).isSolidRender(this.level, var1);
   }

   public void setCanFloat(boolean var1) {
   }
}
