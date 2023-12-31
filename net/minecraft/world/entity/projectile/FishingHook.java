package net.minecraft.world.entity.projectile;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FishingHook extends Projectile {
   private final Random syncronizedRandom;
   private boolean biting;
   private int outOfWaterTime;
   private static final int MAX_OUT_OF_WATER_TIME = 10;
   private static final EntityDataAccessor<Integer> DATA_HOOKED_ENTITY;
   private static final EntityDataAccessor<Boolean> DATA_BITING;
   private int life;
   private int nibble;
   private int timeUntilLured;
   private int timeUntilHooked;
   private float fishAngle;
   private boolean openWater;
   @Nullable
   private Entity hookedIn;
   private FishingHook.FishHookState currentState;
   private final int luck;
   private final int lureSpeed;

   private FishingHook(EntityType<? extends FishingHook> var1, Level var2, int var3, int var4) {
      super(var1, var2);
      this.syncronizedRandom = new Random();
      this.openWater = true;
      this.currentState = FishingHook.FishHookState.FLYING;
      this.noCulling = true;
      this.luck = Math.max(0, var3);
      this.lureSpeed = Math.max(0, var4);
   }

   public FishingHook(EntityType<? extends FishingHook> var1, Level var2) {
      this((EntityType)var1, var2, 0, 0);
   }

   public FishingHook(Player var1, Level var2, int var3, int var4) {
      this(EntityType.FISHING_BOBBER, var2, var3, var4);
      this.setOwner(var1);
      float var5 = var1.getXRot();
      float var6 = var1.getYRot();
      float var7 = Mth.cos(-var6 * 0.017453292F - 3.1415927F);
      float var8 = Mth.sin(-var6 * 0.017453292F - 3.1415927F);
      float var9 = -Mth.cos(-var5 * 0.017453292F);
      float var10 = Mth.sin(-var5 * 0.017453292F);
      double var11 = var1.getX() - (double)var8 * 0.3D;
      double var13 = var1.getEyeY();
      double var15 = var1.getZ() - (double)var7 * 0.3D;
      this.moveTo(var11, var13, var15, var6, var5);
      Vec3 var17 = new Vec3((double)(-var8), (double)Mth.clamp(-(var10 / var9), -5.0F, 5.0F), (double)(-var7));
      double var18 = var17.length();
      var17 = var17.multiply(0.6D / var18 + 0.5D + this.random.nextGaussian() * 0.0045D, 0.6D / var18 + 0.5D + this.random.nextGaussian() * 0.0045D, 0.6D / var18 + 0.5D + this.random.nextGaussian() * 0.0045D);
      this.setDeltaMovement(var17);
      this.setYRot((float)(Mth.atan2(var17.x, var17.z) * 57.2957763671875D));
      this.setXRot((float)(Mth.atan2(var17.y, var17.horizontalDistance()) * 57.2957763671875D));
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }

   protected void defineSynchedData() {
      this.getEntityData().define(DATA_HOOKED_ENTITY, 0);
      this.getEntityData().define(DATA_BITING, false);
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> var1) {
      if (DATA_HOOKED_ENTITY.equals(var1)) {
         int var2 = (Integer)this.getEntityData().get(DATA_HOOKED_ENTITY);
         this.hookedIn = var2 > 0 ? this.level.getEntity(var2 - 1) : null;
      }

      if (DATA_BITING.equals(var1)) {
         this.biting = (Boolean)this.getEntityData().get(DATA_BITING);
         if (this.biting) {
            this.setDeltaMovement(this.getDeltaMovement().x, (double)(-0.4F * Mth.nextFloat(this.syncronizedRandom, 0.6F, 1.0F)), this.getDeltaMovement().z);
         }
      }

      super.onSyncedDataUpdated(var1);
   }

   public boolean shouldRenderAtSqrDistance(double var1) {
      double var3 = 64.0D;
      return var1 < 4096.0D;
   }

   public void lerpTo(double var1, double var3, double var5, float var7, float var8, int var9, boolean var10) {
   }

   public void tick() {
      this.syncronizedRandom.setSeed(this.getUUID().getLeastSignificantBits() ^ this.level.getGameTime());
      super.tick();
      Player var1 = this.getPlayerOwner();
      if (var1 == null) {
         this.discard();
      } else if (this.level.isClientSide || !this.shouldStopFishing(var1)) {
         if (this.onGround) {
            ++this.life;
            if (this.life >= 1200) {
               this.discard();
               return;
            }
         } else {
            this.life = 0;
         }

         float var2 = 0.0F;
         BlockPos var3 = this.blockPosition();
         FluidState var4 = this.level.getFluidState(var3);
         if (var4.is(FluidTags.WATER)) {
            var2 = var4.getHeight(this.level, var3);
         }

         boolean var5 = var2 > 0.0F;
         if (this.currentState == FishingHook.FishHookState.FLYING) {
            if (this.hookedIn != null) {
               this.setDeltaMovement(Vec3.ZERO);
               this.currentState = FishingHook.FishHookState.HOOKED_IN_ENTITY;
               return;
            }

            if (var5) {
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.3D, 0.2D, 0.3D));
               this.currentState = FishingHook.FishHookState.BOBBING;
               return;
            }

            this.checkCollision();
         } else {
            if (this.currentState == FishingHook.FishHookState.HOOKED_IN_ENTITY) {
               if (this.hookedIn != null) {
                  if (!this.hookedIn.isRemoved() && this.hookedIn.level.dimension() == this.level.dimension()) {
                     this.setPos(this.hookedIn.getX(), this.hookedIn.getY(0.8D), this.hookedIn.getZ());
                  } else {
                     this.setHookedEntity((Entity)null);
                     this.currentState = FishingHook.FishHookState.FLYING;
                  }
               }

               return;
            }

            if (this.currentState == FishingHook.FishHookState.BOBBING) {
               Vec3 var6 = this.getDeltaMovement();
               double var7 = this.getY() + var6.y - (double)var3.getY() - (double)var2;
               if (Math.abs(var7) < 0.01D) {
                  var7 += Math.signum(var7) * 0.1D;
               }

               this.setDeltaMovement(var6.x * 0.9D, var6.y - var7 * (double)this.random.nextFloat() * 0.2D, var6.z * 0.9D);
               if (this.nibble <= 0 && this.timeUntilHooked <= 0) {
                  this.openWater = true;
               } else {
                  this.openWater = this.openWater && this.outOfWaterTime < 10 && this.calculateOpenWater(var3);
               }

               if (var5) {
                  this.outOfWaterTime = Math.max(0, this.outOfWaterTime - 1);
                  if (this.biting) {
                     this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.1D * (double)this.syncronizedRandom.nextFloat() * (double)this.syncronizedRandom.nextFloat(), 0.0D));
                  }

                  if (!this.level.isClientSide) {
                     this.catchingFish(var3);
                  }
               } else {
                  this.outOfWaterTime = Math.min(10, this.outOfWaterTime + 1);
               }
            }
         }

         if (!var4.is(FluidTags.WATER)) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03D, 0.0D));
         }

         this.move(MoverType.SELF, this.getDeltaMovement());
         this.updateRotation();
         if (this.currentState == FishingHook.FishHookState.FLYING && (this.onGround || this.horizontalCollision)) {
            this.setDeltaMovement(Vec3.ZERO);
         }

         double var9 = 0.92D;
         this.setDeltaMovement(this.getDeltaMovement().scale(0.92D));
         this.reapplyPosition();
      }
   }

   private boolean shouldStopFishing(Player var1) {
      ItemStack var2 = var1.getMainHandItem();
      ItemStack var3 = var1.getOffhandItem();
      boolean var4 = var2.is(Items.FISHING_ROD);
      boolean var5 = var3.is(Items.FISHING_ROD);
      if (!var1.isRemoved() && var1.isAlive() && (var4 || var5) && !(this.distanceToSqr(var1) > 1024.0D)) {
         return false;
      } else {
         this.discard();
         return true;
      }
   }

   private void checkCollision() {
      HitResult var1 = ProjectileUtil.getHitResult(this, this::canHitEntity);
      this.onHit(var1);
   }

   protected boolean canHitEntity(Entity var1) {
      return super.canHitEntity(var1) || var1.isAlive() && var1 instanceof ItemEntity;
   }

   protected void onHitEntity(EntityHitResult var1) {
      super.onHitEntity(var1);
      if (!this.level.isClientSide) {
         this.setHookedEntity(var1.getEntity());
      }

   }

   protected void onHitBlock(BlockHitResult var1) {
      super.onHitBlock(var1);
      this.setDeltaMovement(this.getDeltaMovement().normalize().scale(var1.distanceTo(this)));
   }

   private void setHookedEntity(@Nullable Entity var1) {
      this.hookedIn = var1;
      this.getEntityData().set(DATA_HOOKED_ENTITY, var1 == null ? 0 : var1.getId() + 1);
   }

   private void catchingFish(BlockPos var1) {
      ServerLevel var2 = (ServerLevel)this.level;
      int var3 = 1;
      BlockPos var4 = var1.above();
      if (this.random.nextFloat() < 0.25F && this.level.isRainingAt(var4)) {
         ++var3;
      }

      if (this.random.nextFloat() < 0.5F && !this.level.canSeeSky(var4)) {
         --var3;
      }

      if (this.nibble > 0) {
         --this.nibble;
         if (this.nibble <= 0) {
            this.timeUntilLured = 0;
            this.timeUntilHooked = 0;
            this.getEntityData().set(DATA_BITING, false);
         }
      } else {
         float var5;
         float var6;
         float var7;
         double var8;
         double var10;
         double var12;
         BlockState var14;
         if (this.timeUntilHooked > 0) {
            this.timeUntilHooked -= var3;
            if (this.timeUntilHooked > 0) {
               this.fishAngle = (float)((double)this.fishAngle + this.random.nextGaussian() * 4.0D);
               var5 = this.fishAngle * 0.017453292F;
               var6 = Mth.sin(var5);
               var7 = Mth.cos(var5);
               var8 = this.getX() + (double)(var6 * (float)this.timeUntilHooked * 0.1F);
               var10 = (double)((float)Mth.floor(this.getY()) + 1.0F);
               var12 = this.getZ() + (double)(var7 * (float)this.timeUntilHooked * 0.1F);
               var14 = var2.getBlockState(new BlockPos(var8, var10 - 1.0D, var12));
               if (var14.is(Blocks.WATER)) {
                  if (this.random.nextFloat() < 0.15F) {
                     var2.sendParticles(ParticleTypes.BUBBLE, var8, var10 - 0.10000000149011612D, var12, 1, (double)var6, 0.1D, (double)var7, 0.0D);
                  }

                  float var15 = var6 * 0.04F;
                  float var16 = var7 * 0.04F;
                  var2.sendParticles(ParticleTypes.FISHING, var8, var10, var12, 0, (double)var16, 0.01D, (double)(-var15), 1.0D);
                  var2.sendParticles(ParticleTypes.FISHING, var8, var10, var12, 0, (double)(-var16), 0.01D, (double)var15, 1.0D);
               }
            } else {
               this.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
               double var17 = this.getY() + 0.5D;
               var2.sendParticles(ParticleTypes.BUBBLE, this.getX(), var17, this.getZ(), (int)(1.0F + this.getBbWidth() * 20.0F), (double)this.getBbWidth(), 0.0D, (double)this.getBbWidth(), 0.20000000298023224D);
               var2.sendParticles(ParticleTypes.FISHING, this.getX(), var17, this.getZ(), (int)(1.0F + this.getBbWidth() * 20.0F), (double)this.getBbWidth(), 0.0D, (double)this.getBbWidth(), 0.20000000298023224D);
               this.nibble = Mth.nextInt(this.random, 20, 40);
               this.getEntityData().set(DATA_BITING, true);
            }
         } else if (this.timeUntilLured > 0) {
            this.timeUntilLured -= var3;
            var5 = 0.15F;
            if (this.timeUntilLured < 20) {
               var5 = (float)((double)var5 + (double)(20 - this.timeUntilLured) * 0.05D);
            } else if (this.timeUntilLured < 40) {
               var5 = (float)((double)var5 + (double)(40 - this.timeUntilLured) * 0.02D);
            } else if (this.timeUntilLured < 60) {
               var5 = (float)((double)var5 + (double)(60 - this.timeUntilLured) * 0.01D);
            }

            if (this.random.nextFloat() < var5) {
               var6 = Mth.nextFloat(this.random, 0.0F, 360.0F) * 0.017453292F;
               var7 = Mth.nextFloat(this.random, 25.0F, 60.0F);
               var8 = this.getX() + (double)(Mth.sin(var6) * var7 * 0.1F);
               var10 = (double)((float)Mth.floor(this.getY()) + 1.0F);
               var12 = this.getZ() + (double)(Mth.cos(var6) * var7 * 0.1F);
               var14 = var2.getBlockState(new BlockPos(var8, var10 - 1.0D, var12));
               if (var14.is(Blocks.WATER)) {
                  var2.sendParticles(ParticleTypes.SPLASH, var8, var10, var12, 2 + this.random.nextInt(2), 0.10000000149011612D, 0.0D, 0.10000000149011612D, 0.0D);
               }
            }

            if (this.timeUntilLured <= 0) {
               this.fishAngle = Mth.nextFloat(this.random, 0.0F, 360.0F);
               this.timeUntilHooked = Mth.nextInt(this.random, 20, 80);
            }
         } else {
            this.timeUntilLured = Mth.nextInt(this.random, 100, 600);
            this.timeUntilLured -= this.lureSpeed * 20 * 5;
         }
      }

   }

   private boolean calculateOpenWater(BlockPos var1) {
      FishingHook.OpenWaterType var2 = FishingHook.OpenWaterType.INVALID;

      for(int var3 = -1; var3 <= 2; ++var3) {
         FishingHook.OpenWaterType var4 = this.getOpenWaterTypeForArea(var1.offset(-2, var3, -2), var1.offset(2, var3, 2));
         switch(var4) {
         case INVALID:
            return false;
         case ABOVE_WATER:
            if (var2 == FishingHook.OpenWaterType.INVALID) {
               return false;
            }
            break;
         case INSIDE_WATER:
            if (var2 == FishingHook.OpenWaterType.ABOVE_WATER) {
               return false;
            }
         }

         var2 = var4;
      }

      return true;
   }

   private FishingHook.OpenWaterType getOpenWaterTypeForArea(BlockPos var1, BlockPos var2) {
      return (FishingHook.OpenWaterType)BlockPos.betweenClosedStream(var1, var2).map(this::getOpenWaterTypeForBlock).reduce((var0, var1x) -> {
         return var0 == var1x ? var0 : FishingHook.OpenWaterType.INVALID;
      }).orElse(FishingHook.OpenWaterType.INVALID);
   }

   private FishingHook.OpenWaterType getOpenWaterTypeForBlock(BlockPos var1) {
      BlockState var2 = this.level.getBlockState(var1);
      if (!var2.isAir() && !var2.is(Blocks.LILY_PAD)) {
         FluidState var3 = var2.getFluidState();
         return var3.is(FluidTags.WATER) && var3.isSource() && var2.getCollisionShape(this.level, var1).isEmpty() ? FishingHook.OpenWaterType.INSIDE_WATER : FishingHook.OpenWaterType.INVALID;
      } else {
         return FishingHook.OpenWaterType.ABOVE_WATER;
      }
   }

   public boolean isOpenWaterFishing() {
      return this.openWater;
   }

   public void addAdditionalSaveData(CompoundTag var1) {
   }

   public void readAdditionalSaveData(CompoundTag var1) {
   }

   public int retrieve(ItemStack var1) {
      Player var2 = this.getPlayerOwner();
      if (!this.level.isClientSide && var2 != null && !this.shouldStopFishing(var2)) {
         int var3 = 0;
         if (this.hookedIn != null) {
            this.pullEntity(this.hookedIn);
            CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayer)var2, var1, this, Collections.emptyList());
            this.level.broadcastEntityEvent(this, (byte)31);
            var3 = this.hookedIn instanceof ItemEntity ? 3 : 5;
         } else if (this.nibble > 0) {
            LootContext.Builder var4 = (new LootContext.Builder((ServerLevel)this.level)).withParameter(LootContextParams.ORIGIN, this.position()).withParameter(LootContextParams.TOOL, var1).withParameter(LootContextParams.THIS_ENTITY, this).withRandom(this.random).withLuck((float)this.luck + var2.getLuck());
            LootTable var5 = this.level.getServer().getLootTables().get(BuiltInLootTables.FISHING);
            List var6 = var5.getRandomItems(var4.create(LootContextParamSets.FISHING));
            CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayer)var2, var1, this, var6);
            Iterator var7 = var6.iterator();

            while(var7.hasNext()) {
               ItemStack var8 = (ItemStack)var7.next();
               ItemEntity var9 = new ItemEntity(this.level, this.getX(), this.getY(), this.getZ(), var8);
               double var10 = var2.getX() - this.getX();
               double var12 = var2.getY() - this.getY();
               double var14 = var2.getZ() - this.getZ();
               double var16 = 0.1D;
               var9.setDeltaMovement(var10 * 0.1D, var12 * 0.1D + Math.sqrt(Math.sqrt(var10 * var10 + var12 * var12 + var14 * var14)) * 0.08D, var14 * 0.1D);
               this.level.addFreshEntity(var9);
               var2.level.addFreshEntity(new ExperienceOrb(var2.level, var2.getX(), var2.getY() + 0.5D, var2.getZ() + 0.5D, this.random.nextInt(6) + 1));
               if (var8.is((Tag)ItemTags.FISHES)) {
                  var2.awardStat((ResourceLocation)Stats.FISH_CAUGHT, 1);
               }
            }

            var3 = 1;
         }

         if (this.onGround) {
            var3 = 2;
         }

         this.discard();
         return var3;
      } else {
         return 0;
      }
   }

   public void handleEntityEvent(byte var1) {
      if (var1 == 31 && this.level.isClientSide && this.hookedIn instanceof Player && ((Player)this.hookedIn).isLocalPlayer()) {
         this.pullEntity(this.hookedIn);
      }

      super.handleEntityEvent(var1);
   }

   protected void pullEntity(Entity var1) {
      Entity var2 = this.getOwner();
      if (var2 != null) {
         Vec3 var3 = (new Vec3(var2.getX() - this.getX(), var2.getY() - this.getY(), var2.getZ() - this.getZ())).scale(0.1D);
         var1.setDeltaMovement(var1.getDeltaMovement().add(var3));
      }
   }

   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.NONE;
   }

   public void remove(Entity.RemovalReason var1) {
      this.updateOwnerInfo((FishingHook)null);
      super.remove(var1);
   }

   public void onClientRemoval() {
      this.updateOwnerInfo((FishingHook)null);
   }

   public void setOwner(@Nullable Entity var1) {
      super.setOwner(var1);
      this.updateOwnerInfo(this);
   }

   private void updateOwnerInfo(@Nullable FishingHook var1) {
      Player var2 = this.getPlayerOwner();
      if (var2 != null) {
         var2.fishing = var1;
      }

   }

   @Nullable
   public Player getPlayerOwner() {
      Entity var1 = this.getOwner();
      return var1 instanceof Player ? (Player)var1 : null;
   }

   @Nullable
   public Entity getHookedIn() {
      return this.hookedIn;
   }

   public boolean canChangeDimensions() {
      return false;
   }

   public Packet<?> getAddEntityPacket() {
      Entity var1 = this.getOwner();
      return new ClientboundAddEntityPacket(this, var1 == null ? this.getId() : var1.getId());
   }

   public void recreateFromPacket(ClientboundAddEntityPacket var1) {
      super.recreateFromPacket(var1);
      if (this.getPlayerOwner() == null) {
         int var2 = var1.getData();
         LOGGER.error("Failed to recreate fishing hook on client. {} (id: {}) is not a valid owner.", this.level.getEntity(var2), var2);
         this.kill();
      }

   }

   static {
      DATA_HOOKED_ENTITY = SynchedEntityData.defineId(FishingHook.class, EntityDataSerializers.INT);
      DATA_BITING = SynchedEntityData.defineId(FishingHook.class, EntityDataSerializers.BOOLEAN);
   }

   private static enum FishHookState {
      FLYING,
      HOOKED_IN_ENTITY,
      BOBBING;

      // $FF: synthetic method
      private static FishingHook.FishHookState[] $values() {
         return new FishingHook.FishHookState[]{FLYING, HOOKED_IN_ENTITY, BOBBING};
      }
   }

   static enum OpenWaterType {
      ABOVE_WATER,
      INSIDE_WATER,
      INVALID;

      // $FF: synthetic method
      private static FishingHook.OpenWaterType[] $values() {
         return new FishingHook.OpenWaterType[]{ABOVE_WATER, INSIDE_WATER, INVALID};
      }
   }
}
