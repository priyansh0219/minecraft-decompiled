package net.minecraft.world.entity.animal;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

public abstract class WaterAnimal extends PathfinderMob {
   protected WaterAnimal(EntityType<? extends WaterAnimal> var1, Level var2) {
      super(var1, var2);
      this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
   }

   public boolean canBreatheUnderwater() {
      return true;
   }

   public MobType getMobType() {
      return MobType.WATER;
   }

   public boolean checkSpawnObstruction(LevelReader var1) {
      return var1.isUnobstructed(this);
   }

   public int getAmbientSoundInterval() {
      return 120;
   }

   protected int getExperienceReward(Player var1) {
      return 1 + this.level.random.nextInt(3);
   }

   protected void handleAirSupply(int var1) {
      if (this.isAlive() && !this.isInWaterOrBubble()) {
         this.setAirSupply(var1 - 1);
         if (this.getAirSupply() == -20) {
            this.setAirSupply(0);
            this.hurt(DamageSource.DROWN, 2.0F);
         }
      } else {
         this.setAirSupply(300);
      }

   }

   public void baseTick() {
      int var1 = this.getAirSupply();
      super.baseTick();
      this.handleAirSupply(var1);
   }

   public boolean isPushedByFluid() {
      return false;
   }

   public boolean canBeLeashed(Player var1) {
      return false;
   }

   public static boolean checkUndergroundWaterCreatureSpawnRules(EntityType<? extends LivingEntity> var0, ServerLevelAccessor var1, MobSpawnType var2, BlockPos var3, Random var4) {
      return var3.getY() < var1.getSeaLevel() && var3.getY() < var1.getHeight(Heightmap.Types.OCEAN_FLOOR, var3.getX(), var3.getZ()) && isDarkEnoughToSpawn(var1, var3) && isBaseStoneBelow(var3, var1);
   }

   public static boolean isBaseStoneBelow(BlockPos var0, ServerLevelAccessor var1) {
      BlockPos.MutableBlockPos var2 = var0.mutable();

      for(int var3 = 0; var3 < 5; ++var3) {
         var2.move(Direction.DOWN);
         BlockState var4 = var1.getBlockState(var2);
         if (var4.is(BlockTags.BASE_STONE_OVERWORLD)) {
            return true;
         }

         if (!var4.is(Blocks.WATER)) {
            return false;
         }
      }

      return false;
   }

   public static boolean isDarkEnoughToSpawn(ServerLevelAccessor var0, BlockPos var1) {
      int var2 = var0.getLevel().isThundering() ? var0.getMaxLocalRawBrightness(var1, 10) : var0.getMaxLocalRawBrightness(var1);
      return var2 == 0;
   }
}
