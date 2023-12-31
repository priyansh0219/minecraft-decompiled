package net.minecraft.world.entity.player;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public abstract class Player extends LivingEntity {
   public static final String UUID_PREFIX_OFFLINE_PLAYER = "OfflinePlayer:";
   public static final int MAX_NAME_LENGTH = 16;
   public static final int MAX_HEALTH = 20;
   public static final int SLEEP_DURATION = 100;
   public static final int WAKE_UP_DURATION = 10;
   public static final int ENDER_SLOT_OFFSET = 200;
   public static final float CROUCH_BB_HEIGHT = 1.5F;
   public static final float SWIMMING_BB_WIDTH = 0.6F;
   public static final float SWIMMING_BB_HEIGHT = 0.6F;
   public static final float DEFAULT_EYE_HEIGHT = 1.62F;
   public static final EntityDimensions STANDING_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.8F);
   private static final Map<Pose, EntityDimensions> POSES;
   private static final int FLY_ACHIEVEMENT_SPEED = 25;
   private static final EntityDataAccessor<Float> DATA_PLAYER_ABSORPTION_ID;
   private static final EntityDataAccessor<Integer> DATA_SCORE_ID;
   protected static final EntityDataAccessor<Byte> DATA_PLAYER_MODE_CUSTOMISATION;
   protected static final EntityDataAccessor<Byte> DATA_PLAYER_MAIN_HAND;
   protected static final EntityDataAccessor<CompoundTag> DATA_SHOULDER_LEFT;
   protected static final EntityDataAccessor<CompoundTag> DATA_SHOULDER_RIGHT;
   private long timeEntitySatOnShoulder;
   private final Inventory inventory = new Inventory(this);
   protected PlayerEnderChestContainer enderChestInventory = new PlayerEnderChestContainer();
   public final InventoryMenu inventoryMenu;
   public AbstractContainerMenu containerMenu;
   protected FoodData foodData = new FoodData();
   protected int jumpTriggerTime;
   public float oBob;
   public float bob;
   public int takeXpDelay;
   public double xCloakO;
   public double yCloakO;
   public double zCloakO;
   public double xCloak;
   public double yCloak;
   public double zCloak;
   private int sleepCounter;
   protected boolean wasUnderwater;
   private final Abilities abilities = new Abilities();
   public int experienceLevel;
   public int totalExperience;
   public float experienceProgress;
   protected int enchantmentSeed;
   protected final float defaultFlySpeed = 0.02F;
   private int lastLevelUpTime;
   private final GameProfile gameProfile;
   private boolean reducedDebugInfo;
   private ItemStack lastItemInMainHand;
   private final ItemCooldowns cooldowns;
   @Nullable
   public FishingHook fishing;

   public Player(Level var1, BlockPos var2, float var3, GameProfile var4) {
      super(EntityType.PLAYER, var1);
      this.lastItemInMainHand = ItemStack.EMPTY;
      this.cooldowns = this.createItemCooldowns();
      this.setUUID(createPlayerUUID(var4));
      this.gameProfile = var4;
      this.inventoryMenu = new InventoryMenu(this.inventory, !var1.isClientSide, this);
      this.containerMenu = this.inventoryMenu;
      this.moveTo((double)var2.getX() + 0.5D, (double)(var2.getY() + 1), (double)var2.getZ() + 0.5D, var3, 0.0F);
      this.rotOffs = 180.0F;
   }

   public boolean blockActionRestricted(Level var1, BlockPos var2, GameType var3) {
      if (!var3.isBlockPlacingRestricted()) {
         return false;
      } else if (var3 == GameType.SPECTATOR) {
         return true;
      } else if (this.mayBuild()) {
         return false;
      } else {
         ItemStack var4 = this.getMainHandItem();
         return var4.isEmpty() || !var4.hasAdventureModeBreakTagForBlock(var1.getTagManager(), new BlockInWorld(var1, var2, false));
      }
   }

   public static AttributeSupplier.Builder createAttributes() {
      return LivingEntity.createLivingAttributes().add(Attributes.ATTACK_DAMAGE, 1.0D).add(Attributes.MOVEMENT_SPEED, 0.10000000149011612D).add(Attributes.ATTACK_SPEED).add(Attributes.LUCK);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_PLAYER_ABSORPTION_ID, 0.0F);
      this.entityData.define(DATA_SCORE_ID, 0);
      this.entityData.define(DATA_PLAYER_MODE_CUSTOMISATION, (byte)0);
      this.entityData.define(DATA_PLAYER_MAIN_HAND, (byte)1);
      this.entityData.define(DATA_SHOULDER_LEFT, new CompoundTag());
      this.entityData.define(DATA_SHOULDER_RIGHT, new CompoundTag());
   }

   public void tick() {
      this.noPhysics = this.isSpectator();
      if (this.isSpectator()) {
         this.onGround = false;
      }

      if (this.takeXpDelay > 0) {
         --this.takeXpDelay;
      }

      if (this.isSleeping()) {
         ++this.sleepCounter;
         if (this.sleepCounter > 100) {
            this.sleepCounter = 100;
         }

         if (!this.level.isClientSide && this.level.isDay()) {
            this.stopSleepInBed(false, true);
         }
      } else if (this.sleepCounter > 0) {
         ++this.sleepCounter;
         if (this.sleepCounter >= 110) {
            this.sleepCounter = 0;
         }
      }

      this.updateIsUnderwater();
      super.tick();
      if (!this.level.isClientSide && this.containerMenu != null && !this.containerMenu.stillValid(this)) {
         this.closeContainer();
         this.containerMenu = this.inventoryMenu;
      }

      this.moveCloak();
      if (!this.level.isClientSide) {
         this.foodData.tick(this);
         this.awardStat(Stats.PLAY_TIME);
         this.awardStat(Stats.TOTAL_WORLD_TIME);
         if (this.isAlive()) {
            this.awardStat(Stats.TIME_SINCE_DEATH);
         }

         if (this.isDiscrete()) {
            this.awardStat(Stats.CROUCH_TIME);
         }

         if (!this.isSleeping()) {
            this.awardStat(Stats.TIME_SINCE_REST);
         }
      }

      int var1 = 29999999;
      double var2 = Mth.clamp(this.getX(), -2.9999999E7D, 2.9999999E7D);
      double var4 = Mth.clamp(this.getZ(), -2.9999999E7D, 2.9999999E7D);
      if (var2 != this.getX() || var4 != this.getZ()) {
         this.setPos(var2, this.getY(), var4);
      }

      ++this.attackStrengthTicker;
      ItemStack var6 = this.getMainHandItem();
      if (!ItemStack.matches(this.lastItemInMainHand, var6)) {
         if (!ItemStack.isSameIgnoreDurability(this.lastItemInMainHand, var6)) {
            this.resetAttackStrengthTicker();
         }

         this.lastItemInMainHand = var6.copy();
      }

      this.turtleHelmetTick();
      this.cooldowns.tick();
      this.updatePlayerPose();
   }

   public boolean isSecondaryUseActive() {
      return this.isShiftKeyDown();
   }

   protected boolean wantsToStopRiding() {
      return this.isShiftKeyDown();
   }

   protected boolean isStayingOnGroundSurface() {
      return this.isShiftKeyDown();
   }

   protected boolean updateIsUnderwater() {
      this.wasUnderwater = this.isEyeInFluid(FluidTags.WATER);
      return this.wasUnderwater;
   }

   private void turtleHelmetTick() {
      ItemStack var1 = this.getItemBySlot(EquipmentSlot.HEAD);
      if (var1.is(Items.TURTLE_HELMET) && !this.isEyeInFluid(FluidTags.WATER)) {
         this.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false, true));
      }

   }

   protected ItemCooldowns createItemCooldowns() {
      return new ItemCooldowns();
   }

   private void moveCloak() {
      this.xCloakO = this.xCloak;
      this.yCloakO = this.yCloak;
      this.zCloakO = this.zCloak;
      double var1 = this.getX() - this.xCloak;
      double var3 = this.getY() - this.yCloak;
      double var5 = this.getZ() - this.zCloak;
      double var7 = 10.0D;
      if (var1 > 10.0D) {
         this.xCloak = this.getX();
         this.xCloakO = this.xCloak;
      }

      if (var5 > 10.0D) {
         this.zCloak = this.getZ();
         this.zCloakO = this.zCloak;
      }

      if (var3 > 10.0D) {
         this.yCloak = this.getY();
         this.yCloakO = this.yCloak;
      }

      if (var1 < -10.0D) {
         this.xCloak = this.getX();
         this.xCloakO = this.xCloak;
      }

      if (var5 < -10.0D) {
         this.zCloak = this.getZ();
         this.zCloakO = this.zCloak;
      }

      if (var3 < -10.0D) {
         this.yCloak = this.getY();
         this.yCloakO = this.yCloak;
      }

      this.xCloak += var1 * 0.25D;
      this.zCloak += var5 * 0.25D;
      this.yCloak += var3 * 0.25D;
   }

   protected void updatePlayerPose() {
      if (this.canEnterPose(Pose.SWIMMING)) {
         Pose var1;
         if (this.isFallFlying()) {
            var1 = Pose.FALL_FLYING;
         } else if (this.isSleeping()) {
            var1 = Pose.SLEEPING;
         } else if (this.isSwimming()) {
            var1 = Pose.SWIMMING;
         } else if (this.isAutoSpinAttack()) {
            var1 = Pose.SPIN_ATTACK;
         } else if (this.isShiftKeyDown() && !this.abilities.flying) {
            var1 = Pose.CROUCHING;
         } else {
            var1 = Pose.STANDING;
         }

         Pose var2;
         if (!this.isSpectator() && !this.isPassenger() && !this.canEnterPose(var1)) {
            if (this.canEnterPose(Pose.CROUCHING)) {
               var2 = Pose.CROUCHING;
            } else {
               var2 = Pose.SWIMMING;
            }
         } else {
            var2 = var1;
         }

         this.setPose(var2);
      }
   }

   public int getPortalWaitTime() {
      return this.abilities.invulnerable ? 1 : 80;
   }

   protected SoundEvent getSwimSound() {
      return SoundEvents.PLAYER_SWIM;
   }

   protected SoundEvent getSwimSplashSound() {
      return SoundEvents.PLAYER_SPLASH;
   }

   protected SoundEvent getSwimHighSpeedSplashSound() {
      return SoundEvents.PLAYER_SPLASH_HIGH_SPEED;
   }

   public int getDimensionChangingDelay() {
      return 10;
   }

   public void playSound(SoundEvent var1, float var2, float var3) {
      this.level.playSound(this, this.getX(), this.getY(), this.getZ(), var1, this.getSoundSource(), var2, var3);
   }

   public void playNotifySound(SoundEvent var1, SoundSource var2, float var3, float var4) {
   }

   public SoundSource getSoundSource() {
      return SoundSource.PLAYERS;
   }

   protected int getFireImmuneTicks() {
      return 20;
   }

   public void handleEntityEvent(byte var1) {
      if (var1 == 9) {
         this.completeUsingItem();
      } else if (var1 == 23) {
         this.reducedDebugInfo = false;
      } else if (var1 == 22) {
         this.reducedDebugInfo = true;
      } else if (var1 == 43) {
         this.addParticlesAroundSelf(ParticleTypes.CLOUD);
      } else {
         super.handleEntityEvent(var1);
      }

   }

   private void addParticlesAroundSelf(ParticleOptions var1) {
      for(int var2 = 0; var2 < 5; ++var2) {
         double var3 = this.random.nextGaussian() * 0.02D;
         double var5 = this.random.nextGaussian() * 0.02D;
         double var7 = this.random.nextGaussian() * 0.02D;
         this.level.addParticle(var1, this.getRandomX(1.0D), this.getRandomY() + 1.0D, this.getRandomZ(1.0D), var3, var5, var7);
      }

   }

   protected void closeContainer() {
      this.containerMenu = this.inventoryMenu;
   }

   public void rideTick() {
      if (!this.level.isClientSide && this.wantsToStopRiding() && this.isPassenger()) {
         this.stopRiding();
         this.setShiftKeyDown(false);
      } else {
         double var1 = this.getX();
         double var3 = this.getY();
         double var5 = this.getZ();
         super.rideTick();
         this.oBob = this.bob;
         this.bob = 0.0F;
         this.checkRidingStatistics(this.getX() - var1, this.getY() - var3, this.getZ() - var5);
      }
   }

   protected void serverAiStep() {
      super.serverAiStep();
      this.updateSwingTime();
      this.yHeadRot = this.getYRot();
   }

   public void aiStep() {
      if (this.jumpTriggerTime > 0) {
         --this.jumpTriggerTime;
      }

      if (this.level.getDifficulty() == Difficulty.PEACEFUL && this.level.getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) {
         if (this.getHealth() < this.getMaxHealth() && this.tickCount % 20 == 0) {
            this.heal(1.0F);
         }

         if (this.foodData.needsFood() && this.tickCount % 10 == 0) {
            this.foodData.setFoodLevel(this.foodData.getFoodLevel() + 1);
         }
      }

      this.inventory.tick();
      this.oBob = this.bob;
      super.aiStep();
      this.flyingSpeed = 0.02F;
      if (this.isSprinting()) {
         this.flyingSpeed = (float)((double)this.flyingSpeed + 0.005999999865889549D);
      }

      this.setSpeed((float)this.getAttributeValue(Attributes.MOVEMENT_SPEED));
      float var1;
      if (this.onGround && !this.isDeadOrDying() && !this.isSwimming()) {
         var1 = Math.min(0.1F, (float)this.getDeltaMovement().horizontalDistance());
      } else {
         var1 = 0.0F;
      }

      this.bob += (var1 - this.bob) * 0.4F;
      if (this.getHealth() > 0.0F && !this.isSpectator()) {
         AABB var2;
         if (this.isPassenger() && !this.getVehicle().isRemoved()) {
            var2 = this.getBoundingBox().minmax(this.getVehicle().getBoundingBox()).inflate(1.0D, 0.0D, 1.0D);
         } else {
            var2 = this.getBoundingBox().inflate(1.0D, 0.5D, 1.0D);
         }

         List var3 = this.level.getEntities(this, var2);
         ArrayList var4 = Lists.newArrayList();

         for(int var5 = 0; var5 < var3.size(); ++var5) {
            Entity var6 = (Entity)var3.get(var5);
            if (var6.getType() == EntityType.EXPERIENCE_ORB) {
               var4.add(var6);
            } else if (!var6.isRemoved()) {
               this.touch(var6);
            }
         }

         if (!var4.isEmpty()) {
            this.touch((Entity)Util.getRandom((List)var4, this.random));
         }
      }

      this.playShoulderEntityAmbientSound(this.getShoulderEntityLeft());
      this.playShoulderEntityAmbientSound(this.getShoulderEntityRight());
      if (!this.level.isClientSide && (this.fallDistance > 0.5F || this.isInWater()) || this.abilities.flying || this.isSleeping() || this.isInPowderSnow) {
         this.removeEntitiesOnShoulder();
      }

   }

   private void playShoulderEntityAmbientSound(@Nullable CompoundTag var1) {
      if (var1 != null && (!var1.contains("Silent") || !var1.getBoolean("Silent")) && this.level.random.nextInt(200) == 0) {
         String var2 = var1.getString("id");
         EntityType.byString(var2).filter((var0) -> {
            return var0 == EntityType.PARROT;
         }).ifPresent((var1x) -> {
            if (!Parrot.imitateNearbyMobs(this.level, this)) {
               this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), Parrot.getAmbient(this.level, this.level.random), this.getSoundSource(), 1.0F, Parrot.getPitch(this.level.random));
            }

         });
      }

   }

   private void touch(Entity var1) {
      var1.playerTouch(this);
   }

   public int getScore() {
      return (Integer)this.entityData.get(DATA_SCORE_ID);
   }

   public void setScore(int var1) {
      this.entityData.set(DATA_SCORE_ID, var1);
   }

   public void increaseScore(int var1) {
      int var2 = this.getScore();
      this.entityData.set(DATA_SCORE_ID, var2 + var1);
   }

   public void die(DamageSource var1) {
      super.die(var1);
      this.reapplyPosition();
      if (!this.isSpectator()) {
         this.dropAllDeathLoot(var1);
      }

      if (var1 != null) {
         this.setDeltaMovement((double)(-Mth.cos((this.hurtDir + this.getYRot()) * 0.017453292F) * 0.1F), 0.10000000149011612D, (double)(-Mth.sin((this.hurtDir + this.getYRot()) * 0.017453292F) * 0.1F));
      } else {
         this.setDeltaMovement(0.0D, 0.1D, 0.0D);
      }

      this.awardStat(Stats.DEATHS);
      this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
      this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
      this.clearFire();
      this.setSharedFlagOnFire(false);
   }

   protected void dropEquipment() {
      super.dropEquipment();
      if (!this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
         this.destroyVanishingCursedItems();
         this.inventory.dropAll();
      }

   }

   protected void destroyVanishingCursedItems() {
      for(int var1 = 0; var1 < this.inventory.getContainerSize(); ++var1) {
         ItemStack var2 = this.inventory.getItem(var1);
         if (!var2.isEmpty() && EnchantmentHelper.hasVanishingCurse(var2)) {
            this.inventory.removeItemNoUpdate(var1);
         }
      }

   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      if (var1 == DamageSource.ON_FIRE) {
         return SoundEvents.PLAYER_HURT_ON_FIRE;
      } else if (var1 == DamageSource.DROWN) {
         return SoundEvents.PLAYER_HURT_DROWN;
      } else if (var1 == DamageSource.SWEET_BERRY_BUSH) {
         return SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH;
      } else {
         return var1 == DamageSource.FREEZE ? SoundEvents.PLAYER_HURT_FREEZE : SoundEvents.PLAYER_HURT;
      }
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.PLAYER_DEATH;
   }

   public boolean drop(boolean var1) {
      return this.drop(this.inventory.removeItem(this.inventory.selected, var1 && !this.inventory.getSelected().isEmpty() ? this.inventory.getSelected().getCount() : 1), false, true) != null;
   }

   @Nullable
   public ItemEntity drop(ItemStack var1, boolean var2) {
      return this.drop(var1, false, var2);
   }

   @Nullable
   public ItemEntity drop(ItemStack var1, boolean var2, boolean var3) {
      if (var1.isEmpty()) {
         return null;
      } else {
         if (this.level.isClientSide) {
            this.swing(InteractionHand.MAIN_HAND);
         }

         double var4 = this.getEyeY() - 0.30000001192092896D;
         ItemEntity var6 = new ItemEntity(this.level, this.getX(), var4, this.getZ(), var1);
         var6.setPickUpDelay(40);
         if (var3) {
            var6.setThrower(this.getUUID());
         }

         float var7;
         float var8;
         if (var2) {
            var7 = this.random.nextFloat() * 0.5F;
            var8 = this.random.nextFloat() * 6.2831855F;
            var6.setDeltaMovement((double)(-Mth.sin(var8) * var7), 0.20000000298023224D, (double)(Mth.cos(var8) * var7));
         } else {
            var7 = 0.3F;
            var8 = Mth.sin(this.getXRot() * 0.017453292F);
            float var9 = Mth.cos(this.getXRot() * 0.017453292F);
            float var10 = Mth.sin(this.getYRot() * 0.017453292F);
            float var11 = Mth.cos(this.getYRot() * 0.017453292F);
            float var12 = this.random.nextFloat() * 6.2831855F;
            float var13 = 0.02F * this.random.nextFloat();
            var6.setDeltaMovement((double)(-var10 * var9 * 0.3F) + Math.cos((double)var12) * (double)var13, (double)(-var8 * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F), (double)(var11 * var9 * 0.3F) + Math.sin((double)var12) * (double)var13);
         }

         return var6;
      }
   }

   public float getDestroySpeed(BlockState var1) {
      float var2 = this.inventory.getDestroySpeed(var1);
      if (var2 > 1.0F) {
         int var3 = EnchantmentHelper.getBlockEfficiency(this);
         ItemStack var4 = this.getMainHandItem();
         if (var3 > 0 && !var4.isEmpty()) {
            var2 += (float)(var3 * var3 + 1);
         }
      }

      if (MobEffectUtil.hasDigSpeed(this)) {
         var2 *= 1.0F + (float)(MobEffectUtil.getDigSpeedAmplification(this) + 1) * 0.2F;
      }

      if (this.hasEffect(MobEffects.DIG_SLOWDOWN)) {
         float var5;
         switch(this.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) {
         case 0:
            var5 = 0.3F;
            break;
         case 1:
            var5 = 0.09F;
            break;
         case 2:
            var5 = 0.0027F;
            break;
         case 3:
         default:
            var5 = 8.1E-4F;
         }

         var2 *= var5;
      }

      if (this.isEyeInFluid(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(this)) {
         var2 /= 5.0F;
      }

      if (!this.onGround) {
         var2 /= 5.0F;
      }

      return var2;
   }

   public boolean hasCorrectToolForDrops(BlockState var1) {
      return !var1.requiresCorrectToolForDrops() || this.inventory.getSelected().isCorrectToolForDrops(var1);
   }

   public void readAdditionalSaveData(CompoundTag var1) {
      super.readAdditionalSaveData(var1);
      this.setUUID(createPlayerUUID(this.gameProfile));
      ListTag var2 = var1.getList("Inventory", 10);
      this.inventory.load(var2);
      this.inventory.selected = var1.getInt("SelectedItemSlot");
      this.sleepCounter = var1.getShort("SleepTimer");
      this.experienceProgress = var1.getFloat("XpP");
      this.experienceLevel = var1.getInt("XpLevel");
      this.totalExperience = var1.getInt("XpTotal");
      this.enchantmentSeed = var1.getInt("XpSeed");
      if (this.enchantmentSeed == 0) {
         this.enchantmentSeed = this.random.nextInt();
      }

      this.setScore(var1.getInt("Score"));
      this.foodData.readAdditionalSaveData(var1);
      this.abilities.loadSaveData(var1);
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double)this.abilities.getWalkingSpeed());
      if (var1.contains("EnderItems", 9)) {
         this.enderChestInventory.fromTag(var1.getList("EnderItems", 10));
      }

      if (var1.contains("ShoulderEntityLeft", 10)) {
         this.setShoulderEntityLeft(var1.getCompound("ShoulderEntityLeft"));
      }

      if (var1.contains("ShoulderEntityRight", 10)) {
         this.setShoulderEntityRight(var1.getCompound("ShoulderEntityRight"));
      }

   }

   public void addAdditionalSaveData(CompoundTag var1) {
      super.addAdditionalSaveData(var1);
      var1.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
      var1.put("Inventory", this.inventory.save(new ListTag()));
      var1.putInt("SelectedItemSlot", this.inventory.selected);
      var1.putShort("SleepTimer", (short)this.sleepCounter);
      var1.putFloat("XpP", this.experienceProgress);
      var1.putInt("XpLevel", this.experienceLevel);
      var1.putInt("XpTotal", this.totalExperience);
      var1.putInt("XpSeed", this.enchantmentSeed);
      var1.putInt("Score", this.getScore());
      this.foodData.addAdditionalSaveData(var1);
      this.abilities.addSaveData(var1);
      var1.put("EnderItems", this.enderChestInventory.createTag());
      if (!this.getShoulderEntityLeft().isEmpty()) {
         var1.put("ShoulderEntityLeft", this.getShoulderEntityLeft());
      }

      if (!this.getShoulderEntityRight().isEmpty()) {
         var1.put("ShoulderEntityRight", this.getShoulderEntityRight());
      }

   }

   public boolean isInvulnerableTo(DamageSource var1) {
      if (super.isInvulnerableTo(var1)) {
         return true;
      } else if (var1 == DamageSource.DROWN) {
         return !this.level.getGameRules().getBoolean(GameRules.RULE_DROWNING_DAMAGE);
      } else if (var1.isFall()) {
         return !this.level.getGameRules().getBoolean(GameRules.RULE_FALL_DAMAGE);
      } else if (var1.isFire()) {
         return !this.level.getGameRules().getBoolean(GameRules.RULE_FIRE_DAMAGE);
      } else if (var1 == DamageSource.FREEZE) {
         return !this.level.getGameRules().getBoolean(GameRules.RULE_FREEZE_DAMAGE);
      } else {
         return false;
      }
   }

   public boolean hurt(DamageSource var1, float var2) {
      if (this.isInvulnerableTo(var1)) {
         return false;
      } else if (this.abilities.invulnerable && !var1.isBypassInvul()) {
         return false;
      } else {
         this.noActionTime = 0;
         if (this.isDeadOrDying()) {
            return false;
         } else {
            this.removeEntitiesOnShoulder();
            if (var1.scalesWithDifficulty()) {
               if (this.level.getDifficulty() == Difficulty.PEACEFUL) {
                  var2 = 0.0F;
               }

               if (this.level.getDifficulty() == Difficulty.EASY) {
                  var2 = Math.min(var2 / 2.0F + 1.0F, var2);
               }

               if (this.level.getDifficulty() == Difficulty.HARD) {
                  var2 = var2 * 3.0F / 2.0F;
               }
            }

            return var2 == 0.0F ? false : super.hurt(var1, var2);
         }
      }
   }

   protected void blockUsingShield(LivingEntity var1) {
      super.blockUsingShield(var1);
      if (var1.getMainHandItem().getItem() instanceof AxeItem) {
         this.disableShield(true);
      }

   }

   public boolean canBeSeenAsEnemy() {
      return !this.getAbilities().invulnerable && super.canBeSeenAsEnemy();
   }

   public boolean canHarmPlayer(Player var1) {
      Team var2 = this.getTeam();
      Team var3 = var1.getTeam();
      if (var2 == null) {
         return true;
      } else {
         return !var2.isAlliedTo(var3) ? true : var2.isAllowFriendlyFire();
      }
   }

   protected void hurtArmor(DamageSource var1, float var2) {
      this.inventory.hurtArmor(var1, var2, Inventory.ALL_ARMOR_SLOTS);
   }

   protected void hurtHelmet(DamageSource var1, float var2) {
      this.inventory.hurtArmor(var1, var2, Inventory.HELMET_SLOT_ONLY);
   }

   protected void hurtCurrentlyUsedShield(float var1) {
      if (this.useItem.is(Items.SHIELD)) {
         if (!this.level.isClientSide) {
            this.awardStat(Stats.ITEM_USED.get(this.useItem.getItem()));
         }

         if (var1 >= 3.0F) {
            int var2 = 1 + Mth.floor(var1);
            InteractionHand var3 = this.getUsedItemHand();
            this.useItem.hurtAndBreak(var2, this, (var1x) -> {
               var1x.broadcastBreakEvent(var3);
            });
            if (this.useItem.isEmpty()) {
               if (var3 == InteractionHand.MAIN_HAND) {
                  this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
               } else {
                  this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
               }

               this.useItem = ItemStack.EMPTY;
               this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            }
         }

      }
   }

   protected void actuallyHurt(DamageSource var1, float var2) {
      if (!this.isInvulnerableTo(var1)) {
         var2 = this.getDamageAfterArmorAbsorb(var1, var2);
         var2 = this.getDamageAfterMagicAbsorb(var1, var2);
         float var3 = var2;
         var2 = Math.max(var2 - this.getAbsorptionAmount(), 0.0F);
         this.setAbsorptionAmount(this.getAbsorptionAmount() - (var3 - var2));
         float var4 = var3 - var2;
         if (var4 > 0.0F && var4 < 3.4028235E37F) {
            this.awardStat(Stats.DAMAGE_ABSORBED, Math.round(var4 * 10.0F));
         }

         if (var2 != 0.0F) {
            this.causeFoodExhaustion(var1.getFoodExhaustion());
            float var5 = this.getHealth();
            this.setHealth(this.getHealth() - var2);
            this.getCombatTracker().recordDamage(var1, var5, var2);
            if (var2 < 3.4028235E37F) {
               this.awardStat(Stats.DAMAGE_TAKEN, Math.round(var2 * 10.0F));
            }

         }
      }
   }

   protected boolean onSoulSpeedBlock() {
      return !this.abilities.flying && super.onSoulSpeedBlock();
   }

   public void openTextEdit(SignBlockEntity var1) {
   }

   public void openMinecartCommandBlock(BaseCommandBlock var1) {
   }

   public void openCommandBlock(CommandBlockEntity var1) {
   }

   public void openStructureBlock(StructureBlockEntity var1) {
   }

   public void openJigsawBlock(JigsawBlockEntity var1) {
   }

   public void openHorseInventory(AbstractHorse var1, Container var2) {
   }

   public OptionalInt openMenu(@Nullable MenuProvider var1) {
      return OptionalInt.empty();
   }

   public void sendMerchantOffers(int var1, MerchantOffers var2, int var3, int var4, boolean var5, boolean var6) {
   }

   public void openItemGui(ItemStack var1, InteractionHand var2) {
   }

   public InteractionResult interactOn(Entity var1, InteractionHand var2) {
      if (this.isSpectator()) {
         if (var1 instanceof MenuProvider) {
            this.openMenu((MenuProvider)var1);
         }

         return InteractionResult.PASS;
      } else {
         ItemStack var3 = this.getItemInHand(var2);
         ItemStack var4 = var3.copy();
         InteractionResult var5 = var1.interact(this, var2);
         if (var5.consumesAction()) {
            if (this.abilities.instabuild && var3 == this.getItemInHand(var2) && var3.getCount() < var4.getCount()) {
               var3.setCount(var4.getCount());
            }

            return var5;
         } else {
            if (!var3.isEmpty() && var1 instanceof LivingEntity) {
               if (this.abilities.instabuild) {
                  var3 = var4;
               }

               InteractionResult var6 = var3.interactLivingEntity(this, (LivingEntity)var1, var2);
               if (var6.consumesAction()) {
                  if (var3.isEmpty() && !this.abilities.instabuild) {
                     this.setItemInHand(var2, ItemStack.EMPTY);
                  }

                  return var6;
               }
            }

            return InteractionResult.PASS;
         }
      }
   }

   public double getMyRidingOffset() {
      return -0.35D;
   }

   public void removeVehicle() {
      super.removeVehicle();
      this.boardingCooldown = 0;
   }

   protected boolean isImmobile() {
      return super.isImmobile() || this.isSleeping();
   }

   public boolean isAffectedByFluids() {
      return !this.abilities.flying;
   }

   protected Vec3 maybeBackOffFromEdge(Vec3 var1, MoverType var2) {
      if (!this.abilities.flying && (var2 == MoverType.SELF || var2 == MoverType.PLAYER) && this.isStayingOnGroundSurface() && this.isAboveGround()) {
         double var3 = var1.x;
         double var5 = var1.z;
         double var7 = 0.05D;

         while(true) {
            while(var3 != 0.0D && this.level.noCollision(this, this.getBoundingBox().move(var3, (double)(-this.maxUpStep), 0.0D))) {
               if (var3 < 0.05D && var3 >= -0.05D) {
                  var3 = 0.0D;
               } else if (var3 > 0.0D) {
                  var3 -= 0.05D;
               } else {
                  var3 += 0.05D;
               }
            }

            while(true) {
               while(var5 != 0.0D && this.level.noCollision(this, this.getBoundingBox().move(0.0D, (double)(-this.maxUpStep), var5))) {
                  if (var5 < 0.05D && var5 >= -0.05D) {
                     var5 = 0.0D;
                  } else if (var5 > 0.0D) {
                     var5 -= 0.05D;
                  } else {
                     var5 += 0.05D;
                  }
               }

               while(true) {
                  while(var3 != 0.0D && var5 != 0.0D && this.level.noCollision(this, this.getBoundingBox().move(var3, (double)(-this.maxUpStep), var5))) {
                     if (var3 < 0.05D && var3 >= -0.05D) {
                        var3 = 0.0D;
                     } else if (var3 > 0.0D) {
                        var3 -= 0.05D;
                     } else {
                        var3 += 0.05D;
                     }

                     if (var5 < 0.05D && var5 >= -0.05D) {
                        var5 = 0.0D;
                     } else if (var5 > 0.0D) {
                        var5 -= 0.05D;
                     } else {
                        var5 += 0.05D;
                     }
                  }

                  var1 = new Vec3(var3, var1.y, var5);
                  return var1;
               }
            }
         }
      } else {
         return var1;
      }
   }

   private boolean isAboveGround() {
      return this.onGround || this.fallDistance < this.maxUpStep && !this.level.noCollision(this, this.getBoundingBox().move(0.0D, (double)(this.fallDistance - this.maxUpStep), 0.0D));
   }

   public void attack(Entity var1) {
      if (var1.isAttackable()) {
         if (!var1.skipAttackInteraction(this)) {
            float var2 = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float var3;
            if (var1 instanceof LivingEntity) {
               var3 = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity)var1).getMobType());
            } else {
               var3 = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), MobType.UNDEFINED);
            }

            float var4 = this.getAttackStrengthScale(0.5F);
            var2 *= 0.2F + var4 * var4 * 0.8F;
            var3 *= var4;
            this.resetAttackStrengthTicker();
            if (var2 > 0.0F || var3 > 0.0F) {
               boolean var5 = var4 > 0.9F;
               boolean var6 = false;
               byte var7 = 0;
               int var21 = var7 + EnchantmentHelper.getKnockbackBonus(this);
               if (this.isSprinting() && var5) {
                  this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, this.getSoundSource(), 1.0F, 1.0F);
                  ++var21;
                  var6 = true;
               }

               boolean var8 = var5 && this.fallDistance > 0.0F && !this.onGround && !this.onClimbable() && !this.isInWater() && !this.hasEffect(MobEffects.BLINDNESS) && !this.isPassenger() && var1 instanceof LivingEntity;
               var8 = var8 && !this.isSprinting();
               if (var8) {
                  var2 *= 1.5F;
               }

               var2 += var3;
               boolean var9 = false;
               double var10 = (double)(this.walkDist - this.walkDistO);
               if (var5 && !var8 && !var6 && this.onGround && var10 < (double)this.getSpeed()) {
                  ItemStack var12 = this.getItemInHand(InteractionHand.MAIN_HAND);
                  if (var12.getItem() instanceof SwordItem) {
                     var9 = true;
                  }
               }

               float var22 = 0.0F;
               boolean var13 = false;
               int var14 = EnchantmentHelper.getFireAspect(this);
               if (var1 instanceof LivingEntity) {
                  var22 = ((LivingEntity)var1).getHealth();
                  if (var14 > 0 && !var1.isOnFire()) {
                     var13 = true;
                     var1.setSecondsOnFire(1);
                  }
               }

               Vec3 var15 = var1.getDeltaMovement();
               boolean var16 = var1.hurt(DamageSource.playerAttack(this), var2);
               if (var16) {
                  if (var21 > 0) {
                     if (var1 instanceof LivingEntity) {
                        ((LivingEntity)var1).knockback((double)((float)var21 * 0.5F), (double)Mth.sin(this.getYRot() * 0.017453292F), (double)(-Mth.cos(this.getYRot() * 0.017453292F)));
                     } else {
                        var1.push((double)(-Mth.sin(this.getYRot() * 0.017453292F) * (float)var21 * 0.5F), 0.1D, (double)(Mth.cos(this.getYRot() * 0.017453292F) * (float)var21 * 0.5F));
                     }

                     this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
                     this.setSprinting(false);
                  }

                  if (var9) {
                     float var17 = 1.0F + EnchantmentHelper.getSweepingDamageRatio(this) * var2;
                     List var18 = this.level.getEntitiesOfClass(LivingEntity.class, var1.getBoundingBox().inflate(1.0D, 0.25D, 1.0D));
                     Iterator var19 = var18.iterator();

                     label166:
                     while(true) {
                        LivingEntity var20;
                        do {
                           do {
                              do {
                                 do {
                                    if (!var19.hasNext()) {
                                       this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1.0F, 1.0F);
                                       this.sweepAttack();
                                       break label166;
                                    }

                                    var20 = (LivingEntity)var19.next();
                                 } while(var20 == this);
                              } while(var20 == var1);
                           } while(this.isAlliedTo(var20));
                        } while(var20 instanceof ArmorStand && ((ArmorStand)var20).isMarker());

                        if (this.distanceToSqr(var20) < 9.0D) {
                           var20.knockback(0.4000000059604645D, (double)Mth.sin(this.getYRot() * 0.017453292F), (double)(-Mth.cos(this.getYRot() * 0.017453292F)));
                           var20.hurt(DamageSource.playerAttack(this), var17);
                        }
                     }
                  }

                  if (var1 instanceof ServerPlayer && var1.hurtMarked) {
                     ((ServerPlayer)var1).connection.send(new ClientboundSetEntityMotionPacket(var1));
                     var1.hurtMarked = false;
                     var1.setDeltaMovement(var15);
                  }

                  if (var8) {
                     this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundSource(), 1.0F, 1.0F);
                     this.crit(var1);
                  }

                  if (!var8 && !var9) {
                     if (var5) {
                        this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, this.getSoundSource(), 1.0F, 1.0F);
                     } else {
                        this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, this.getSoundSource(), 1.0F, 1.0F);
                     }
                  }

                  if (var3 > 0.0F) {
                     this.magicCrit(var1);
                  }

                  this.setLastHurtMob(var1);
                  if (var1 instanceof LivingEntity) {
                     EnchantmentHelper.doPostHurtEffects((LivingEntity)var1, this);
                  }

                  EnchantmentHelper.doPostDamageEffects(this, var1);
                  ItemStack var23 = this.getMainHandItem();
                  Object var24 = var1;
                  if (var1 instanceof EnderDragonPart) {
                     var24 = ((EnderDragonPart)var1).parentMob;
                  }

                  if (!this.level.isClientSide && !var23.isEmpty() && var24 instanceof LivingEntity) {
                     var23.hurtEnemy((LivingEntity)var24, this);
                     if (var23.isEmpty()) {
                        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                     }
                  }

                  if (var1 instanceof LivingEntity) {
                     float var25 = var22 - ((LivingEntity)var1).getHealth();
                     this.awardStat(Stats.DAMAGE_DEALT, Math.round(var25 * 10.0F));
                     if (var14 > 0) {
                        var1.setSecondsOnFire(var14 * 4);
                     }

                     if (this.level instanceof ServerLevel && var25 > 2.0F) {
                        int var26 = (int)((double)var25 * 0.5D);
                        ((ServerLevel)this.level).sendParticles(ParticleTypes.DAMAGE_INDICATOR, var1.getX(), var1.getY(0.5D), var1.getZ(), var26, 0.1D, 0.0D, 0.1D, 0.2D);
                     }
                  }

                  this.causeFoodExhaustion(0.1F);
               } else {
                  this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource(), 1.0F, 1.0F);
                  if (var13) {
                     var1.clearFire();
                  }
               }
            }

         }
      }
   }

   protected void doAutoAttackOnTouch(LivingEntity var1) {
      this.attack(var1);
   }

   public void disableShield(boolean var1) {
      float var2 = 0.25F + (float)EnchantmentHelper.getBlockEfficiency(this) * 0.05F;
      if (var1) {
         var2 += 0.75F;
      }

      if (this.random.nextFloat() < var2) {
         this.getCooldowns().addCooldown(Items.SHIELD, 100);
         this.stopUsingItem();
         this.level.broadcastEntityEvent(this, (byte)30);
      }

   }

   public void crit(Entity var1) {
   }

   public void magicCrit(Entity var1) {
   }

   public void sweepAttack() {
      double var1 = (double)(-Mth.sin(this.getYRot() * 0.017453292F));
      double var3 = (double)Mth.cos(this.getYRot() * 0.017453292F);
      if (this.level instanceof ServerLevel) {
         ((ServerLevel)this.level).sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + var1, this.getY(0.5D), this.getZ() + var3, 0, var1, 0.0D, var3, 0.0D);
      }

   }

   public void respawn() {
   }

   public void remove(Entity.RemovalReason var1) {
      super.remove(var1);
      this.inventoryMenu.removed(this);
      if (this.containerMenu != null) {
         this.containerMenu.removed(this);
      }

   }

   public boolean isLocalPlayer() {
      return false;
   }

   public GameProfile getGameProfile() {
      return this.gameProfile;
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   public Abilities getAbilities() {
      return this.abilities;
   }

   public void updateTutorialInventoryAction(ItemStack var1, ItemStack var2, ClickAction var3) {
   }

   public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos var1) {
      this.startSleeping(var1);
      this.sleepCounter = 0;
      return Either.right(Unit.INSTANCE);
   }

   public void stopSleepInBed(boolean var1, boolean var2) {
      super.stopSleeping();
      if (this.level instanceof ServerLevel && var2) {
         ((ServerLevel)this.level).updateSleepingPlayerList();
      }

      this.sleepCounter = var1 ? 0 : 100;
   }

   public void stopSleeping() {
      this.stopSleepInBed(true, true);
   }

   public static Optional<Vec3> findRespawnPositionAndUseSpawnBlock(ServerLevel var0, BlockPos var1, float var2, boolean var3, boolean var4) {
      BlockState var5 = var0.getBlockState(var1);
      Block var6 = var5.getBlock();
      if (var6 instanceof RespawnAnchorBlock && (Integer)var5.getValue(RespawnAnchorBlock.CHARGE) > 0 && RespawnAnchorBlock.canSetSpawn(var0)) {
         Optional var9 = RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, var0, var1);
         if (!var4 && var9.isPresent()) {
            var0.setBlock(var1, (BlockState)var5.setValue(RespawnAnchorBlock.CHARGE, (Integer)var5.getValue(RespawnAnchorBlock.CHARGE) - 1), 3);
         }

         return var9;
      } else if (var6 instanceof BedBlock && BedBlock.canSetSpawn(var0)) {
         return BedBlock.findStandUpPosition(EntityType.PLAYER, var0, var1, var2);
      } else if (!var3) {
         return Optional.empty();
      } else {
         boolean var7 = var6.isPossibleToRespawnInThis();
         boolean var8 = var0.getBlockState(var1.above()).getBlock().isPossibleToRespawnInThis();
         return var7 && var8 ? Optional.of(new Vec3((double)var1.getX() + 0.5D, (double)var1.getY() + 0.1D, (double)var1.getZ() + 0.5D)) : Optional.empty();
      }
   }

   public boolean isSleepingLongEnough() {
      return this.isSleeping() && this.sleepCounter >= 100;
   }

   public int getSleepTimer() {
      return this.sleepCounter;
   }

   public void displayClientMessage(Component var1, boolean var2) {
   }

   public void awardStat(ResourceLocation var1) {
      this.awardStat(Stats.CUSTOM.get(var1));
   }

   public void awardStat(ResourceLocation var1, int var2) {
      this.awardStat(Stats.CUSTOM.get(var1), var2);
   }

   public void awardStat(Stat<?> var1) {
      this.awardStat((Stat)var1, 1);
   }

   public void awardStat(Stat<?> var1, int var2) {
   }

   public void resetStat(Stat<?> var1) {
   }

   public int awardRecipes(Collection<Recipe<?>> var1) {
      return 0;
   }

   public void awardRecipesByKey(ResourceLocation[] var1) {
   }

   public int resetRecipes(Collection<Recipe<?>> var1) {
      return 0;
   }

   public void jumpFromGround() {
      super.jumpFromGround();
      this.awardStat(Stats.JUMP);
      if (this.isSprinting()) {
         this.causeFoodExhaustion(0.2F);
      } else {
         this.causeFoodExhaustion(0.05F);
      }

   }

   public void travel(Vec3 var1) {
      double var2 = this.getX();
      double var4 = this.getY();
      double var6 = this.getZ();
      double var8;
      if (this.isSwimming() && !this.isPassenger()) {
         var8 = this.getLookAngle().y;
         double var10 = var8 < -0.2D ? 0.085D : 0.06D;
         if (var8 <= 0.0D || this.jumping || !this.level.getBlockState(new BlockPos(this.getX(), this.getY() + 1.0D - 0.1D, this.getZ())).getFluidState().isEmpty()) {
            Vec3 var12 = this.getDeltaMovement();
            this.setDeltaMovement(var12.add(0.0D, (var8 - var12.y) * var10, 0.0D));
         }
      }

      if (this.abilities.flying && !this.isPassenger()) {
         var8 = this.getDeltaMovement().y;
         float var13 = this.flyingSpeed;
         this.flyingSpeed = this.abilities.getFlyingSpeed() * (float)(this.isSprinting() ? 2 : 1);
         super.travel(var1);
         Vec3 var11 = this.getDeltaMovement();
         this.setDeltaMovement(var11.x, var8 * 0.6D, var11.z);
         this.flyingSpeed = var13;
         this.fallDistance = 0.0F;
         this.setSharedFlag(7, false);
      } else {
         super.travel(var1);
      }

      this.checkMovementStatistics(this.getX() - var2, this.getY() - var4, this.getZ() - var6);
   }

   public void updateSwimming() {
      if (this.abilities.flying) {
         this.setSwimming(false);
      } else {
         super.updateSwimming();
      }

   }

   protected boolean freeAt(BlockPos var1) {
      return !this.level.getBlockState(var1).isSuffocating(this.level, var1);
   }

   public float getSpeed() {
      return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
   }

   public void checkMovementStatistics(double var1, double var3, double var5) {
      if (!this.isPassenger()) {
         int var7;
         if (this.isSwimming()) {
            var7 = Math.round((float)Math.sqrt(var1 * var1 + var3 * var3 + var5 * var5) * 100.0F);
            if (var7 > 0) {
               this.awardStat(Stats.SWIM_ONE_CM, var7);
               this.causeFoodExhaustion(0.01F * (float)var7 * 0.01F);
            }
         } else if (this.isEyeInFluid(FluidTags.WATER)) {
            var7 = Math.round((float)Math.sqrt(var1 * var1 + var3 * var3 + var5 * var5) * 100.0F);
            if (var7 > 0) {
               this.awardStat(Stats.WALK_UNDER_WATER_ONE_CM, var7);
               this.causeFoodExhaustion(0.01F * (float)var7 * 0.01F);
            }
         } else if (this.isInWater()) {
            var7 = Math.round((float)Math.sqrt(var1 * var1 + var5 * var5) * 100.0F);
            if (var7 > 0) {
               this.awardStat(Stats.WALK_ON_WATER_ONE_CM, var7);
               this.causeFoodExhaustion(0.01F * (float)var7 * 0.01F);
            }
         } else if (this.onClimbable()) {
            if (var3 > 0.0D) {
               this.awardStat(Stats.CLIMB_ONE_CM, (int)Math.round(var3 * 100.0D));
            }
         } else if (this.onGround) {
            var7 = Math.round((float)Math.sqrt(var1 * var1 + var5 * var5) * 100.0F);
            if (var7 > 0) {
               if (this.isSprinting()) {
                  this.awardStat(Stats.SPRINT_ONE_CM, var7);
                  this.causeFoodExhaustion(0.1F * (float)var7 * 0.01F);
               } else if (this.isCrouching()) {
                  this.awardStat(Stats.CROUCH_ONE_CM, var7);
                  this.causeFoodExhaustion(0.0F * (float)var7 * 0.01F);
               } else {
                  this.awardStat(Stats.WALK_ONE_CM, var7);
                  this.causeFoodExhaustion(0.0F * (float)var7 * 0.01F);
               }
            }
         } else if (this.isFallFlying()) {
            var7 = Math.round((float)Math.sqrt(var1 * var1 + var3 * var3 + var5 * var5) * 100.0F);
            this.awardStat(Stats.AVIATE_ONE_CM, var7);
         } else {
            var7 = Math.round((float)Math.sqrt(var1 * var1 + var5 * var5) * 100.0F);
            if (var7 > 25) {
               this.awardStat(Stats.FLY_ONE_CM, var7);
            }
         }

      }
   }

   private void checkRidingStatistics(double var1, double var3, double var5) {
      if (this.isPassenger()) {
         int var7 = Math.round((float)Math.sqrt(var1 * var1 + var3 * var3 + var5 * var5) * 100.0F);
         if (var7 > 0) {
            Entity var8 = this.getVehicle();
            if (var8 instanceof AbstractMinecart) {
               this.awardStat(Stats.MINECART_ONE_CM, var7);
            } else if (var8 instanceof Boat) {
               this.awardStat(Stats.BOAT_ONE_CM, var7);
            } else if (var8 instanceof Pig) {
               this.awardStat(Stats.PIG_ONE_CM, var7);
            } else if (var8 instanceof AbstractHorse) {
               this.awardStat(Stats.HORSE_ONE_CM, var7);
            } else if (var8 instanceof Strider) {
               this.awardStat(Stats.STRIDER_ONE_CM, var7);
            }
         }
      }

   }

   public boolean causeFallDamage(float var1, float var2, DamageSource var3) {
      if (this.abilities.mayfly) {
         return false;
      } else {
         if (var1 >= 2.0F) {
            this.awardStat(Stats.FALL_ONE_CM, (int)Math.round((double)var1 * 100.0D));
         }

         return super.causeFallDamage(var1, var2, var3);
      }
   }

   public boolean tryToStartFallFlying() {
      if (!this.onGround && !this.isFallFlying() && !this.isInWater() && !this.hasEffect(MobEffects.LEVITATION)) {
         ItemStack var1 = this.getItemBySlot(EquipmentSlot.CHEST);
         if (var1.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(var1)) {
            this.startFallFlying();
            return true;
         }
      }

      return false;
   }

   public void startFallFlying() {
      this.setSharedFlag(7, true);
   }

   public void stopFallFlying() {
      this.setSharedFlag(7, true);
      this.setSharedFlag(7, false);
   }

   protected void doWaterSplashEffect() {
      if (!this.isSpectator()) {
         super.doWaterSplashEffect();
      }

   }

   protected SoundEvent getFallDamageSound(int var1) {
      return var1 > 4 ? SoundEvents.PLAYER_BIG_FALL : SoundEvents.PLAYER_SMALL_FALL;
   }

   public void killed(ServerLevel var1, LivingEntity var2) {
      this.awardStat(Stats.ENTITY_KILLED.get(var2.getType()));
   }

   public void makeStuckInBlock(BlockState var1, Vec3 var2) {
      if (!this.abilities.flying) {
         super.makeStuckInBlock(var1, var2);
      }

   }

   public void giveExperiencePoints(int var1) {
      this.increaseScore(var1);
      this.experienceProgress += (float)var1 / (float)this.getXpNeededForNextLevel();
      this.totalExperience = Mth.clamp((int)(this.totalExperience + var1), (int)0, (int)Integer.MAX_VALUE);

      while(this.experienceProgress < 0.0F) {
         float var2 = this.experienceProgress * (float)this.getXpNeededForNextLevel();
         if (this.experienceLevel > 0) {
            this.giveExperienceLevels(-1);
            this.experienceProgress = 1.0F + var2 / (float)this.getXpNeededForNextLevel();
         } else {
            this.giveExperienceLevels(-1);
            this.experienceProgress = 0.0F;
         }
      }

      while(this.experienceProgress >= 1.0F) {
         this.experienceProgress = (this.experienceProgress - 1.0F) * (float)this.getXpNeededForNextLevel();
         this.giveExperienceLevels(1);
         this.experienceProgress /= (float)this.getXpNeededForNextLevel();
      }

   }

   public int getEnchantmentSeed() {
      return this.enchantmentSeed;
   }

   public void onEnchantmentPerformed(ItemStack var1, int var2) {
      this.experienceLevel -= var2;
      if (this.experienceLevel < 0) {
         this.experienceLevel = 0;
         this.experienceProgress = 0.0F;
         this.totalExperience = 0;
      }

      this.enchantmentSeed = this.random.nextInt();
   }

   public void giveExperienceLevels(int var1) {
      this.experienceLevel += var1;
      if (this.experienceLevel < 0) {
         this.experienceLevel = 0;
         this.experienceProgress = 0.0F;
         this.totalExperience = 0;
      }

      if (var1 > 0 && this.experienceLevel % 5 == 0 && (float)this.lastLevelUpTime < (float)this.tickCount - 100.0F) {
         float var2 = this.experienceLevel > 30 ? 1.0F : (float)this.experienceLevel / 30.0F;
         this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_LEVELUP, this.getSoundSource(), var2 * 0.75F, 1.0F);
         this.lastLevelUpTime = this.tickCount;
      }

   }

   public int getXpNeededForNextLevel() {
      if (this.experienceLevel >= 30) {
         return 112 + (this.experienceLevel - 30) * 9;
      } else {
         return this.experienceLevel >= 15 ? 37 + (this.experienceLevel - 15) * 5 : 7 + this.experienceLevel * 2;
      }
   }

   public void causeFoodExhaustion(float var1) {
      if (!this.abilities.invulnerable) {
         if (!this.level.isClientSide) {
            this.foodData.addExhaustion(var1);
         }

      }
   }

   public FoodData getFoodData() {
      return this.foodData;
   }

   public boolean canEat(boolean var1) {
      return this.abilities.invulnerable || var1 || this.foodData.needsFood();
   }

   public boolean isHurt() {
      return this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth();
   }

   public boolean mayBuild() {
      return this.abilities.mayBuild;
   }

   public boolean mayUseItemAt(BlockPos var1, Direction var2, ItemStack var3) {
      if (this.abilities.mayBuild) {
         return true;
      } else {
         BlockPos var4 = var1.relative(var2.getOpposite());
         BlockInWorld var5 = new BlockInWorld(this.level, var4, false);
         return var3.hasAdventureModePlaceTagForBlock(this.level.getTagManager(), var5);
      }
   }

   protected int getExperienceReward(Player var1) {
      if (!this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) && !this.isSpectator()) {
         int var2 = this.experienceLevel * 7;
         return var2 > 100 ? 100 : var2;
      } else {
         return 0;
      }
   }

   protected boolean isAlwaysExperienceDropper() {
      return true;
   }

   public boolean shouldShowName() {
      return true;
   }

   protected Entity.MovementEmission getMovementEmission() {
      return this.abilities.flying || this.onGround && this.isDiscrete() ? Entity.MovementEmission.NONE : Entity.MovementEmission.ALL;
   }

   public void onUpdateAbilities() {
   }

   public Component getName() {
      return new TextComponent(this.gameProfile.getName());
   }

   public PlayerEnderChestContainer getEnderChestInventory() {
      return this.enderChestInventory;
   }

   public ItemStack getItemBySlot(EquipmentSlot var1) {
      if (var1 == EquipmentSlot.MAINHAND) {
         return this.inventory.getSelected();
      } else if (var1 == EquipmentSlot.OFFHAND) {
         return (ItemStack)this.inventory.offhand.get(0);
      } else {
         return var1.getType() == EquipmentSlot.Type.ARMOR ? (ItemStack)this.inventory.armor.get(var1.getIndex()) : ItemStack.EMPTY;
      }
   }

   public void setItemSlot(EquipmentSlot var1, ItemStack var2) {
      this.verifyEquippedItem(var2);
      if (var1 == EquipmentSlot.MAINHAND) {
         this.equipEventAndSound(var2);
         this.inventory.items.set(this.inventory.selected, var2);
      } else if (var1 == EquipmentSlot.OFFHAND) {
         this.equipEventAndSound(var2);
         this.inventory.offhand.set(0, var2);
      } else if (var1.getType() == EquipmentSlot.Type.ARMOR) {
         this.equipEventAndSound(var2);
         this.inventory.armor.set(var1.getIndex(), var2);
      }

   }

   public boolean addItem(ItemStack var1) {
      this.equipEventAndSound(var1);
      return this.inventory.add(var1);
   }

   public Iterable<ItemStack> getHandSlots() {
      return Lists.newArrayList(new ItemStack[]{this.getMainHandItem(), this.getOffhandItem()});
   }

   public Iterable<ItemStack> getArmorSlots() {
      return this.inventory.armor;
   }

   public boolean setEntityOnShoulder(CompoundTag var1) {
      if (!this.isPassenger() && this.onGround && !this.isInWater() && !this.isInPowderSnow) {
         if (this.getShoulderEntityLeft().isEmpty()) {
            this.setShoulderEntityLeft(var1);
            this.timeEntitySatOnShoulder = this.level.getGameTime();
            return true;
         } else if (this.getShoulderEntityRight().isEmpty()) {
            this.setShoulderEntityRight(var1);
            this.timeEntitySatOnShoulder = this.level.getGameTime();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   protected void removeEntitiesOnShoulder() {
      if (this.timeEntitySatOnShoulder + 20L < this.level.getGameTime()) {
         this.respawnEntityOnShoulder(this.getShoulderEntityLeft());
         this.setShoulderEntityLeft(new CompoundTag());
         this.respawnEntityOnShoulder(this.getShoulderEntityRight());
         this.setShoulderEntityRight(new CompoundTag());
      }

   }

   private void respawnEntityOnShoulder(CompoundTag var1) {
      if (!this.level.isClientSide && !var1.isEmpty()) {
         EntityType.create(var1, this.level).ifPresent((var1x) -> {
            if (var1x instanceof TamableAnimal) {
               ((TamableAnimal)var1x).setOwnerUUID(this.uuid);
            }

            var1x.setPos(this.getX(), this.getY() + 0.699999988079071D, this.getZ());
            ((ServerLevel)this.level).addWithUUID(var1x);
         });
      }

   }

   public abstract boolean isSpectator();

   public boolean isSwimming() {
      return !this.abilities.flying && !this.isSpectator() && super.isSwimming();
   }

   public abstract boolean isCreative();

   public boolean isPushedByFluid() {
      return !this.abilities.flying;
   }

   public Scoreboard getScoreboard() {
      return this.level.getScoreboard();
   }

   public Component getDisplayName() {
      MutableComponent var1 = PlayerTeam.formatNameForTeam(this.getTeam(), this.getName());
      return this.decorateDisplayNameComponent(var1);
   }

   private MutableComponent decorateDisplayNameComponent(MutableComponent var1) {
      String var2 = this.getGameProfile().getName();
      return var1.withStyle((var2x) -> {
         return var2x.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + var2 + " ")).withHoverEvent(this.createHoverEvent()).withInsertion(var2);
      });
   }

   public String getScoreboardName() {
      return this.getGameProfile().getName();
   }

   public float getStandingEyeHeight(Pose var1, EntityDimensions var2) {
      switch(var1) {
      case SWIMMING:
      case FALL_FLYING:
      case SPIN_ATTACK:
         return 0.4F;
      case CROUCHING:
         return 1.27F;
      default:
         return 1.62F;
      }
   }

   public void setAbsorptionAmount(float var1) {
      if (var1 < 0.0F) {
         var1 = 0.0F;
      }

      this.getEntityData().set(DATA_PLAYER_ABSORPTION_ID, var1);
   }

   public float getAbsorptionAmount() {
      return (Float)this.getEntityData().get(DATA_PLAYER_ABSORPTION_ID);
   }

   public static UUID createPlayerUUID(GameProfile var0) {
      UUID var1 = var0.getId();
      if (var1 == null) {
         var1 = createPlayerUUID(var0.getName());
      }

      return var1;
   }

   public static UUID createPlayerUUID(String var0) {
      return UUID.nameUUIDFromBytes(("OfflinePlayer:" + var0).getBytes(StandardCharsets.UTF_8));
   }

   public boolean isModelPartShown(PlayerModelPart var1) {
      return ((Byte)this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION) & var1.getMask()) == var1.getMask();
   }

   public SlotAccess getSlot(int var1) {
      if (var1 >= 0 && var1 < this.inventory.items.size()) {
         return SlotAccess.forContainer(this.inventory, var1);
      } else {
         int var2 = var1 - 200;
         return var2 >= 0 && var2 < this.enderChestInventory.getContainerSize() ? SlotAccess.forContainer(this.enderChestInventory, var2) : super.getSlot(var1);
      }
   }

   public boolean isReducedDebugInfo() {
      return this.reducedDebugInfo;
   }

   public void setReducedDebugInfo(boolean var1) {
      this.reducedDebugInfo = var1;
   }

   public void setRemainingFireTicks(int var1) {
      super.setRemainingFireTicks(this.abilities.invulnerable ? Math.min(var1, 1) : var1);
   }

   public HumanoidArm getMainArm() {
      return (Byte)this.entityData.get(DATA_PLAYER_MAIN_HAND) == 0 ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
   }

   public void setMainArm(HumanoidArm var1) {
      this.entityData.set(DATA_PLAYER_MAIN_HAND, (byte)(var1 == HumanoidArm.LEFT ? 0 : 1));
   }

   public CompoundTag getShoulderEntityLeft() {
      return (CompoundTag)this.entityData.get(DATA_SHOULDER_LEFT);
   }

   protected void setShoulderEntityLeft(CompoundTag var1) {
      this.entityData.set(DATA_SHOULDER_LEFT, var1);
   }

   public CompoundTag getShoulderEntityRight() {
      return (CompoundTag)this.entityData.get(DATA_SHOULDER_RIGHT);
   }

   protected void setShoulderEntityRight(CompoundTag var1) {
      this.entityData.set(DATA_SHOULDER_RIGHT, var1);
   }

   public float getCurrentItemAttackStrengthDelay() {
      return (float)(1.0D / this.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0D);
   }

   public float getAttackStrengthScale(float var1) {
      return Mth.clamp(((float)this.attackStrengthTicker + var1) / this.getCurrentItemAttackStrengthDelay(), 0.0F, 1.0F);
   }

   public void resetAttackStrengthTicker() {
      this.attackStrengthTicker = 0;
   }

   public ItemCooldowns getCooldowns() {
      return this.cooldowns;
   }

   protected float getBlockSpeedFactor() {
      return !this.abilities.flying && !this.isFallFlying() ? super.getBlockSpeedFactor() : 1.0F;
   }

   public float getLuck() {
      return (float)this.getAttributeValue(Attributes.LUCK);
   }

   public boolean canUseGameMasterBlocks() {
      return this.abilities.instabuild && this.getPermissionLevel() >= 2;
   }

   public boolean canTakeItem(ItemStack var1) {
      EquipmentSlot var2 = Mob.getEquipmentSlotForItem(var1);
      return this.getItemBySlot(var2).isEmpty();
   }

   public EntityDimensions getDimensions(Pose var1) {
      return (EntityDimensions)POSES.getOrDefault(var1, STANDING_DIMENSIONS);
   }

   public ImmutableList<Pose> getDismountPoses() {
      return ImmutableList.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING);
   }

   public ItemStack getProjectile(ItemStack var1) {
      if (!(var1.getItem() instanceof ProjectileWeaponItem)) {
         return ItemStack.EMPTY;
      } else {
         Predicate var2 = ((ProjectileWeaponItem)var1.getItem()).getSupportedHeldProjectiles();
         ItemStack var3 = ProjectileWeaponItem.getHeldProjectile(this, var2);
         if (!var3.isEmpty()) {
            return var3;
         } else {
            var2 = ((ProjectileWeaponItem)var1.getItem()).getAllSupportedProjectiles();

            for(int var4 = 0; var4 < this.inventory.getContainerSize(); ++var4) {
               ItemStack var5 = this.inventory.getItem(var4);
               if (var2.test(var5)) {
                  return var5;
               }
            }

            return this.abilities.instabuild ? new ItemStack(Items.ARROW) : ItemStack.EMPTY;
         }
      }
   }

   public ItemStack eat(Level var1, ItemStack var2) {
      this.getFoodData().eat(var2.getItem(), var2);
      this.awardStat(Stats.ITEM_USED.get(var2.getItem()));
      var1.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, var1.random.nextFloat() * 0.1F + 0.9F);
      if (this instanceof ServerPlayer) {
         CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer)this, var2);
      }

      return super.eat(var1, var2);
   }

   protected boolean shouldRemoveSoulSpeed(BlockState var1) {
      return this.abilities.flying || super.shouldRemoveSoulSpeed(var1);
   }

   public Vec3 getRopeHoldPosition(float var1) {
      double var2 = 0.22D * (this.getMainArm() == HumanoidArm.RIGHT ? -1.0D : 1.0D);
      float var4 = Mth.lerp(var1 * 0.5F, this.getXRot(), this.xRotO) * 0.017453292F;
      float var5 = Mth.lerp(var1, this.yBodyRotO, this.yBodyRot) * 0.017453292F;
      double var8;
      if (!this.isFallFlying() && !this.isAutoSpinAttack()) {
         if (this.isVisuallySwimming()) {
            return this.getPosition(var1).add((new Vec3(var2, 0.2D, -0.15D)).xRot(-var4).yRot(-var5));
         } else {
            double var17 = this.getBoundingBox().getYsize() - 1.0D;
            var8 = this.isCrouching() ? -0.2D : 0.07D;
            return this.getPosition(var1).add((new Vec3(var2, var17, var8)).yRot(-var5));
         }
      } else {
         Vec3 var6 = this.getViewVector(var1);
         Vec3 var7 = this.getDeltaMovement();
         var8 = var7.horizontalDistanceSqr();
         double var10 = var6.horizontalDistanceSqr();
         float var12;
         if (var8 > 0.0D && var10 > 0.0D) {
            double var13 = (var7.x * var6.x + var7.z * var6.z) / Math.sqrt(var8 * var10);
            double var15 = var7.x * var6.z - var7.z * var6.x;
            var12 = (float)(Math.signum(var15) * Math.acos(var13));
         } else {
            var12 = 0.0F;
         }

         return this.getPosition(var1).add((new Vec3(var2, -0.11D, 0.85D)).zRot(-var12).xRot(-var4).yRot(-var5));
      }
   }

   public boolean isAlwaysTicking() {
      return true;
   }

   public boolean isScoping() {
      return this.isUsingItem() && this.getUseItem().is(Items.SPYGLASS);
   }

   public boolean shouldBeSaved() {
      return false;
   }

   static {
      POSES = ImmutableMap.builder().put(Pose.STANDING, STANDING_DIMENSIONS).put(Pose.SLEEPING, SLEEPING_DIMENSIONS).put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SWIMMING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SPIN_ATTACK, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.CROUCHING, EntityDimensions.scalable(0.6F, 1.5F)).put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F)).build();
      DATA_PLAYER_ABSORPTION_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
      DATA_SCORE_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
      DATA_PLAYER_MODE_CUSTOMISATION = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BYTE);
      DATA_PLAYER_MAIN_HAND = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BYTE);
      DATA_SHOULDER_LEFT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
      DATA_SHOULDER_RIGHT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
   }

   public static enum BedSleepingProblem {
      NOT_POSSIBLE_HERE,
      NOT_POSSIBLE_NOW(new TranslatableComponent("block.minecraft.bed.no_sleep")),
      TOO_FAR_AWAY(new TranslatableComponent("block.minecraft.bed.too_far_away")),
      OBSTRUCTED(new TranslatableComponent("block.minecraft.bed.obstructed")),
      OTHER_PROBLEM,
      NOT_SAFE(new TranslatableComponent("block.minecraft.bed.not_safe"));

      @Nullable
      private final Component message;

      private BedSleepingProblem() {
         this.message = null;
      }

      private BedSleepingProblem(Component var3) {
         this.message = var3;
      }

      @Nullable
      public Component getMessage() {
         return this.message;
      }

      // $FF: synthetic method
      private static Player.BedSleepingProblem[] $values() {
         return new Player.BedSleepingProblem[]{NOT_POSSIBLE_HERE, NOT_POSSIBLE_NOW, TOO_FAR_AWAY, OBSTRUCTED, OTHER_PROBLEM, NOT_SAFE};
      }
   }
}
