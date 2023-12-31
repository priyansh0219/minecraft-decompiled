package net.minecraft.world.entity.raid;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class Raid {
   private static final int SECTION_RADIUS_FOR_FINDING_NEW_VILLAGE_CENTER = 2;
   private static final int ATTEMPT_RAID_FARTHEST = 0;
   private static final int ATTEMPT_RAID_CLOSE = 1;
   private static final int ATTEMPT_RAID_INSIDE = 2;
   private static final int VILLAGE_SEARCH_RADIUS = 32;
   private static final int RAID_TIMEOUT_TICKS = 48000;
   private static final int NUM_SPAWN_ATTEMPTS = 3;
   private static final String OMINOUS_BANNER_PATTERN_NAME = "block.minecraft.ominous_banner";
   private static final String RAIDERS_REMAINING = "event.minecraft.raid.raiders_remaining";
   public static final int VILLAGE_RADIUS_BUFFER = 16;
   private static final int POST_RAID_TICK_LIMIT = 40;
   private static final int DEFAULT_PRE_RAID_TICKS = 300;
   public static final int MAX_NO_ACTION_TIME = 2400;
   public static final int MAX_CELEBRATION_TICKS = 600;
   private static final int OUTSIDE_RAID_BOUNDS_TIMEOUT = 30;
   public static final int TICKS_PER_DAY = 24000;
   public static final int DEFAULT_MAX_BAD_OMEN_LEVEL = 5;
   private static final int LOW_MOB_THRESHOLD = 2;
   private static final Component RAID_NAME_COMPONENT = new TranslatableComponent("event.minecraft.raid");
   private static final Component VICTORY = new TranslatableComponent("event.minecraft.raid.victory");
   private static final Component DEFEAT = new TranslatableComponent("event.minecraft.raid.defeat");
   private static final Component RAID_BAR_VICTORY_COMPONENT;
   private static final Component RAID_BAR_DEFEAT_COMPONENT;
   private static final int HERO_OF_THE_VILLAGE_DURATION = 48000;
   public static final int VALID_RAID_RADIUS_SQR = 9216;
   public static final int RAID_REMOVAL_THRESHOLD_SQR = 12544;
   private final Map<Integer, Raider> groupToLeaderMap = Maps.newHashMap();
   private final Map<Integer, Set<Raider>> groupRaiderMap = Maps.newHashMap();
   private final Set<UUID> heroesOfTheVillage = Sets.newHashSet();
   private long ticksActive;
   private BlockPos center;
   private final ServerLevel level;
   private boolean started;
   private final int id;
   private float totalHealth;
   private int badOmenLevel;
   private boolean active;
   private int groupsSpawned;
   private final ServerBossEvent raidEvent;
   private int postRaidTicks;
   private int raidCooldownTicks;
   private final Random random;
   private final int numGroups;
   private Raid.RaidStatus status;
   private int celebrationTicks;
   private Optional<BlockPos> waveSpawnPos;

   public Raid(int var1, ServerLevel var2, BlockPos var3) {
      this.raidEvent = new ServerBossEvent(RAID_NAME_COMPONENT, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
      this.random = new Random();
      this.waveSpawnPos = Optional.empty();
      this.id = var1;
      this.level = var2;
      this.active = true;
      this.raidCooldownTicks = 300;
      this.raidEvent.setProgress(0.0F);
      this.center = var3;
      this.numGroups = this.getNumGroups(var2.getDifficulty());
      this.status = Raid.RaidStatus.ONGOING;
   }

   public Raid(ServerLevel var1, CompoundTag var2) {
      this.raidEvent = new ServerBossEvent(RAID_NAME_COMPONENT, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
      this.random = new Random();
      this.waveSpawnPos = Optional.empty();
      this.level = var1;
      this.id = var2.getInt("Id");
      this.started = var2.getBoolean("Started");
      this.active = var2.getBoolean("Active");
      this.ticksActive = var2.getLong("TicksActive");
      this.badOmenLevel = var2.getInt("BadOmenLevel");
      this.groupsSpawned = var2.getInt("GroupsSpawned");
      this.raidCooldownTicks = var2.getInt("PreRaidTicks");
      this.postRaidTicks = var2.getInt("PostRaidTicks");
      this.totalHealth = var2.getFloat("TotalHealth");
      this.center = new BlockPos(var2.getInt("CX"), var2.getInt("CY"), var2.getInt("CZ"));
      this.numGroups = var2.getInt("NumGroups");
      this.status = Raid.RaidStatus.getByName(var2.getString("Status"));
      this.heroesOfTheVillage.clear();
      if (var2.contains("HeroesOfTheVillage", 9)) {
         ListTag var3 = var2.getList("HeroesOfTheVillage", 11);

         for(int var4 = 0; var4 < var3.size(); ++var4) {
            this.heroesOfTheVillage.add(NbtUtils.loadUUID(var3.get(var4)));
         }
      }

   }

   public boolean isOver() {
      return this.isVictory() || this.isLoss();
   }

   public boolean isBetweenWaves() {
      return this.hasFirstWaveSpawned() && this.getTotalRaidersAlive() == 0 && this.raidCooldownTicks > 0;
   }

   public boolean hasFirstWaveSpawned() {
      return this.groupsSpawned > 0;
   }

   public boolean isStopped() {
      return this.status == Raid.RaidStatus.STOPPED;
   }

   public boolean isVictory() {
      return this.status == Raid.RaidStatus.VICTORY;
   }

   public boolean isLoss() {
      return this.status == Raid.RaidStatus.LOSS;
   }

   public float getTotalHealth() {
      return this.totalHealth;
   }

   public Set<Raider> getAllRaiders() {
      HashSet var1 = Sets.newHashSet();
      Iterator var2 = this.groupRaiderMap.values().iterator();

      while(var2.hasNext()) {
         Set var3 = (Set)var2.next();
         var1.addAll(var3);
      }

      return var1;
   }

   public Level getLevel() {
      return this.level;
   }

   public boolean isStarted() {
      return this.started;
   }

   public int getGroupsSpawned() {
      return this.groupsSpawned;
   }

   private Predicate<ServerPlayer> validPlayer() {
      return (var1) -> {
         BlockPos var2 = var1.blockPosition();
         return var1.isAlive() && this.level.getRaidAt(var2) == this;
      };
   }

   private void updatePlayers() {
      HashSet var1 = Sets.newHashSet(this.raidEvent.getPlayers());
      List var2 = this.level.getPlayers(this.validPlayer());
      Iterator var3 = var2.iterator();

      ServerPlayer var4;
      while(var3.hasNext()) {
         var4 = (ServerPlayer)var3.next();
         if (!var1.contains(var4)) {
            this.raidEvent.addPlayer(var4);
         }
      }

      var3 = var1.iterator();

      while(var3.hasNext()) {
         var4 = (ServerPlayer)var3.next();
         if (!var2.contains(var4)) {
            this.raidEvent.removePlayer(var4);
         }
      }

   }

   public int getMaxBadOmenLevel() {
      return 5;
   }

   public int getBadOmenLevel() {
      return this.badOmenLevel;
   }

   public void setBadOmenLevel(int var1) {
      this.badOmenLevel = var1;
   }

   public void absorbBadOmen(Player var1) {
      if (var1.hasEffect(MobEffects.BAD_OMEN)) {
         this.badOmenLevel += var1.getEffect(MobEffects.BAD_OMEN).getAmplifier() + 1;
         this.badOmenLevel = Mth.clamp((int)this.badOmenLevel, (int)0, (int)this.getMaxBadOmenLevel());
      }

      var1.removeEffect(MobEffects.BAD_OMEN);
   }

   public void stop() {
      this.active = false;
      this.raidEvent.removeAllPlayers();
      this.status = Raid.RaidStatus.STOPPED;
   }

   public void tick() {
      if (!this.isStopped()) {
         if (this.status == Raid.RaidStatus.ONGOING) {
            boolean var1 = this.active;
            this.active = this.level.hasChunkAt(this.center);
            if (this.level.getDifficulty() == Difficulty.PEACEFUL) {
               this.stop();
               return;
            }

            if (var1 != this.active) {
               this.raidEvent.setVisible(this.active);
            }

            if (!this.active) {
               return;
            }

            if (!this.level.isVillage(this.center)) {
               this.moveRaidCenterToNearbyVillageSection();
            }

            if (!this.level.isVillage(this.center)) {
               if (this.groupsSpawned > 0) {
                  this.status = Raid.RaidStatus.LOSS;
               } else {
                  this.stop();
               }
            }

            ++this.ticksActive;
            if (this.ticksActive >= 48000L) {
               this.stop();
               return;
            }

            int var2 = this.getTotalRaidersAlive();
            boolean var3;
            if (var2 == 0 && this.hasMoreWaves()) {
               if (this.raidCooldownTicks <= 0) {
                  if (this.raidCooldownTicks == 0 && this.groupsSpawned > 0) {
                     this.raidCooldownTicks = 300;
                     this.raidEvent.setName(RAID_NAME_COMPONENT);
                     return;
                  }
               } else {
                  var3 = this.waveSpawnPos.isPresent();
                  boolean var4 = !var3 && this.raidCooldownTicks % 5 == 0;
                  if (var3 && !this.level.isPositionEntityTicking((BlockPos)this.waveSpawnPos.get())) {
                     var4 = true;
                  }

                  if (var4) {
                     byte var5 = 0;
                     if (this.raidCooldownTicks < 100) {
                        var5 = 1;
                     } else if (this.raidCooldownTicks < 40) {
                        var5 = 2;
                     }

                     this.waveSpawnPos = this.getValidSpawnPos(var5);
                  }

                  if (this.raidCooldownTicks == 300 || this.raidCooldownTicks % 20 == 0) {
                     this.updatePlayers();
                  }

                  --this.raidCooldownTicks;
                  this.raidEvent.setProgress(Mth.clamp((float)(300 - this.raidCooldownTicks) / 300.0F, 0.0F, 1.0F));
               }
            }

            if (this.ticksActive % 20L == 0L) {
               this.updatePlayers();
               this.updateRaiders();
               if (var2 > 0) {
                  if (var2 <= 2) {
                     this.raidEvent.setName(RAID_NAME_COMPONENT.copy().append(" - ").append((Component)(new TranslatableComponent("event.minecraft.raid.raiders_remaining", new Object[]{var2}))));
                  } else {
                     this.raidEvent.setName(RAID_NAME_COMPONENT);
                  }
               } else {
                  this.raidEvent.setName(RAID_NAME_COMPONENT);
               }
            }

            var3 = false;
            int var10 = 0;

            while(this.shouldSpawnGroup()) {
               BlockPos var11 = this.waveSpawnPos.isPresent() ? (BlockPos)this.waveSpawnPos.get() : this.findRandomSpawnPos(var10, 20);
               if (var11 != null) {
                  this.started = true;
                  this.spawnGroup(var11);
                  if (!var3) {
                     this.playSound(var11);
                     var3 = true;
                  }
               } else {
                  ++var10;
               }

               if (var10 > 3) {
                  this.stop();
                  break;
               }
            }

            if (this.isStarted() && !this.hasMoreWaves() && var2 == 0) {
               if (this.postRaidTicks < 40) {
                  ++this.postRaidTicks;
               } else {
                  this.status = Raid.RaidStatus.VICTORY;
                  Iterator var12 = this.heroesOfTheVillage.iterator();

                  while(var12.hasNext()) {
                     UUID var6 = (UUID)var12.next();
                     Entity var7 = this.level.getEntity(var6);
                     if (var7 instanceof LivingEntity && !var7.isSpectator()) {
                        LivingEntity var8 = (LivingEntity)var7;
                        var8.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 48000, this.badOmenLevel - 1, false, false, true));
                        if (var8 instanceof ServerPlayer) {
                           ServerPlayer var9 = (ServerPlayer)var8;
                           var9.awardStat(Stats.RAID_WIN);
                           CriteriaTriggers.RAID_WIN.trigger(var9);
                        }
                     }
                  }
               }
            }

            this.setDirty();
         } else if (this.isOver()) {
            ++this.celebrationTicks;
            if (this.celebrationTicks >= 600) {
               this.stop();
               return;
            }

            if (this.celebrationTicks % 20 == 0) {
               this.updatePlayers();
               this.raidEvent.setVisible(true);
               if (this.isVictory()) {
                  this.raidEvent.setProgress(0.0F);
                  this.raidEvent.setName(RAID_BAR_VICTORY_COMPONENT);
               } else {
                  this.raidEvent.setName(RAID_BAR_DEFEAT_COMPONENT);
               }
            }
         }

      }
   }

   private void moveRaidCenterToNearbyVillageSection() {
      Stream var1 = SectionPos.cube(SectionPos.of(this.center), 2);
      ServerLevel var10001 = this.level;
      Objects.requireNonNull(var10001);
      var1.filter(var10001::isVillage).map(SectionPos::center).min(Comparator.comparingDouble((var1x) -> {
         return var1x.distSqr(this.center);
      })).ifPresent(this::setCenter);
   }

   private Optional<BlockPos> getValidSpawnPos(int var1) {
      for(int var2 = 0; var2 < 3; ++var2) {
         BlockPos var3 = this.findRandomSpawnPos(var1, 1);
         if (var3 != null) {
            return Optional.of(var3);
         }
      }

      return Optional.empty();
   }

   private boolean hasMoreWaves() {
      if (this.hasBonusWave()) {
         return !this.hasSpawnedBonusWave();
      } else {
         return !this.isFinalWave();
      }
   }

   private boolean isFinalWave() {
      return this.getGroupsSpawned() == this.numGroups;
   }

   private boolean hasBonusWave() {
      return this.badOmenLevel > 1;
   }

   private boolean hasSpawnedBonusWave() {
      return this.getGroupsSpawned() > this.numGroups;
   }

   private boolean shouldSpawnBonusGroup() {
      return this.isFinalWave() && this.getTotalRaidersAlive() == 0 && this.hasBonusWave();
   }

   private void updateRaiders() {
      Iterator var1 = this.groupRaiderMap.values().iterator();
      HashSet var2 = Sets.newHashSet();

      label54:
      while(var1.hasNext()) {
         Set var3 = (Set)var1.next();
         Iterator var4 = var3.iterator();

         while(true) {
            while(true) {
               if (!var4.hasNext()) {
                  continue label54;
               }

               Raider var5 = (Raider)var4.next();
               BlockPos var6 = var5.blockPosition();
               if (!var5.isRemoved() && var5.level.dimension() == this.level.dimension() && !(this.center.distSqr(var6) >= 12544.0D)) {
                  if (var5.tickCount > 600) {
                     if (this.level.getEntity(var5.getUUID()) == null) {
                        var2.add(var5);
                     }

                     if (!this.level.isVillage(var6) && var5.getNoActionTime() > 2400) {
                        var5.setTicksOutsideRaid(var5.getTicksOutsideRaid() + 1);
                     }

                     if (var5.getTicksOutsideRaid() >= 30) {
                        var2.add(var5);
                     }
                  }
               } else {
                  var2.add(var5);
               }
            }
         }
      }

      Iterator var7 = var2.iterator();

      while(var7.hasNext()) {
         Raider var8 = (Raider)var7.next();
         this.removeFromRaid(var8, true);
      }

   }

   private void playSound(BlockPos var1) {
      float var2 = 13.0F;
      boolean var3 = true;
      Collection var4 = this.raidEvent.getPlayers();
      Iterator var5 = this.level.players().iterator();

      while(true) {
         ServerPlayer var6;
         double var9;
         double var11;
         double var13;
         do {
            if (!var5.hasNext()) {
               return;
            }

            var6 = (ServerPlayer)var5.next();
            Vec3 var7 = var6.position();
            Vec3 var8 = Vec3.atCenterOf(var1);
            var9 = Math.sqrt((var8.x - var7.x) * (var8.x - var7.x) + (var8.z - var7.z) * (var8.z - var7.z));
            var11 = var7.x + 13.0D / var9 * (var8.x - var7.x);
            var13 = var7.z + 13.0D / var9 * (var8.z - var7.z);
         } while(!(var9 <= 64.0D) && !var4.contains(var6));

         var6.connection.send(new ClientboundSoundPacket(SoundEvents.RAID_HORN, SoundSource.NEUTRAL, var11, var6.getY(), var13, 64.0F, 1.0F));
      }
   }

   private void spawnGroup(BlockPos var1) {
      boolean var2 = false;
      int var3 = this.groupsSpawned + 1;
      this.totalHealth = 0.0F;
      DifficultyInstance var4 = this.level.getCurrentDifficultyAt(var1);
      boolean var5 = this.shouldSpawnBonusGroup();
      Raid.RaiderType[] var6 = Raid.RaiderType.VALUES;
      int var7 = var6.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         Raid.RaiderType var9 = var6[var8];
         int var10 = this.getDefaultNumSpawns(var9, var3, var5) + this.getPotentialBonusSpawns(var9, this.random, var3, var4, var5);
         int var11 = 0;

         for(int var12 = 0; var12 < var10; ++var12) {
            Raider var13 = (Raider)var9.entityType.create(this.level);
            if (!var2 && var13.canBeLeader()) {
               var13.setPatrolLeader(true);
               this.setLeader(var3, var13);
               var2 = true;
            }

            this.joinRaid(var3, var13, var1, false);
            if (var9.entityType == EntityType.RAVAGER) {
               Raider var14 = null;
               if (var3 == this.getNumGroups(Difficulty.NORMAL)) {
                  var14 = (Raider)EntityType.PILLAGER.create(this.level);
               } else if (var3 >= this.getNumGroups(Difficulty.HARD)) {
                  if (var11 == 0) {
                     var14 = (Raider)EntityType.EVOKER.create(this.level);
                  } else {
                     var14 = (Raider)EntityType.VINDICATOR.create(this.level);
                  }
               }

               ++var11;
               if (var14 != null) {
                  this.joinRaid(var3, var14, var1, false);
                  var14.moveTo(var1, 0.0F, 0.0F);
                  var14.startRiding(var13);
               }
            }
         }
      }

      this.waveSpawnPos = Optional.empty();
      ++this.groupsSpawned;
      this.updateBossbar();
      this.setDirty();
   }

   public void joinRaid(int var1, Raider var2, @Nullable BlockPos var3, boolean var4) {
      boolean var5 = this.addWaveMob(var1, var2);
      if (var5) {
         var2.setCurrentRaid(this);
         var2.setWave(var1);
         var2.setCanJoinRaid(true);
         var2.setTicksOutsideRaid(0);
         if (!var4 && var3 != null) {
            var2.setPos((double)var3.getX() + 0.5D, (double)var3.getY() + 1.0D, (double)var3.getZ() + 0.5D);
            var2.finalizeSpawn(this.level, this.level.getCurrentDifficultyAt(var3), MobSpawnType.EVENT, (SpawnGroupData)null, (CompoundTag)null);
            var2.applyRaidBuffs(var1, false);
            var2.setOnGround(true);
            this.level.addFreshEntityWithPassengers(var2);
         }
      }

   }

   public void updateBossbar() {
      this.raidEvent.setProgress(Mth.clamp(this.getHealthOfLivingRaiders() / this.totalHealth, 0.0F, 1.0F));
   }

   public float getHealthOfLivingRaiders() {
      float var1 = 0.0F;
      Iterator var2 = this.groupRaiderMap.values().iterator();

      while(var2.hasNext()) {
         Set var3 = (Set)var2.next();

         Raider var5;
         for(Iterator var4 = var3.iterator(); var4.hasNext(); var1 += var5.getHealth()) {
            var5 = (Raider)var4.next();
         }
      }

      return var1;
   }

   private boolean shouldSpawnGroup() {
      return this.raidCooldownTicks == 0 && (this.groupsSpawned < this.numGroups || this.shouldSpawnBonusGroup()) && this.getTotalRaidersAlive() == 0;
   }

   public int getTotalRaidersAlive() {
      return this.groupRaiderMap.values().stream().mapToInt(Set::size).sum();
   }

   public void removeFromRaid(Raider var1, boolean var2) {
      Set var3 = (Set)this.groupRaiderMap.get(var1.getWave());
      if (var3 != null) {
         boolean var4 = var3.remove(var1);
         if (var4) {
            if (var2) {
               this.totalHealth -= var1.getHealth();
            }

            var1.setCurrentRaid((Raid)null);
            this.updateBossbar();
            this.setDirty();
         }
      }

   }

   private void setDirty() {
      this.level.getRaids().setDirty();
   }

   public static ItemStack getLeaderBannerInstance() {
      ItemStack var0 = new ItemStack(Items.WHITE_BANNER);
      CompoundTag var1 = var0.getOrCreateTagElement("BlockEntityTag");
      ListTag var2 = (new BannerPattern.Builder()).addPattern(BannerPattern.RHOMBUS_MIDDLE, DyeColor.CYAN).addPattern(BannerPattern.STRIPE_BOTTOM, DyeColor.LIGHT_GRAY).addPattern(BannerPattern.STRIPE_CENTER, DyeColor.GRAY).addPattern(BannerPattern.BORDER, DyeColor.LIGHT_GRAY).addPattern(BannerPattern.STRIPE_MIDDLE, DyeColor.BLACK).addPattern(BannerPattern.HALF_HORIZONTAL, DyeColor.LIGHT_GRAY).addPattern(BannerPattern.CIRCLE_MIDDLE, DyeColor.LIGHT_GRAY).addPattern(BannerPattern.BORDER, DyeColor.BLACK).toListTag();
      var1.put("Patterns", var2);
      var0.hideTooltipPart(ItemStack.TooltipPart.ADDITIONAL);
      var0.setHoverName((new TranslatableComponent("block.minecraft.ominous_banner")).withStyle(ChatFormatting.GOLD));
      return var0;
   }

   @Nullable
   public Raider getLeader(int var1) {
      return (Raider)this.groupToLeaderMap.get(var1);
   }

   @Nullable
   private BlockPos findRandomSpawnPos(int var1, int var2) {
      int var3 = var1 == 0 ? 2 : 2 - var1;
      BlockPos.MutableBlockPos var7 = new BlockPos.MutableBlockPos();

      for(int var8 = 0; var8 < var2; ++var8) {
         float var9 = this.level.random.nextFloat() * 6.2831855F;
         int var4 = this.center.getX() + Mth.floor(Mth.cos(var9) * 32.0F * (float)var3) + this.level.random.nextInt(5);
         int var6 = this.center.getZ() + Mth.floor(Mth.sin(var9) * 32.0F * (float)var3) + this.level.random.nextInt(5);
         int var5 = this.level.getHeight(Heightmap.Types.WORLD_SURFACE, var4, var6);
         var7.set(var4, var5, var6);
         if (!this.level.isVillage((BlockPos)var7) || var1 >= 2) {
            boolean var10 = true;
            if (this.level.hasChunksAt(var7.getX() - 10, var7.getZ() - 10, var7.getX() + 10, var7.getZ() + 10) && this.level.isPositionEntityTicking((BlockPos)var7) && (NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, this.level, var7, EntityType.RAVAGER) || this.level.getBlockState(var7.below()).is(Blocks.SNOW) && this.level.getBlockState(var7).isAir())) {
               return var7;
            }
         }
      }

      return null;
   }

   private boolean addWaveMob(int var1, Raider var2) {
      return this.addWaveMob(var1, var2, true);
   }

   public boolean addWaveMob(int var1, Raider var2, boolean var3) {
      this.groupRaiderMap.computeIfAbsent(var1, (var0) -> {
         return Sets.newHashSet();
      });
      Set var4 = (Set)this.groupRaiderMap.get(var1);
      Raider var5 = null;
      Iterator var6 = var4.iterator();

      while(var6.hasNext()) {
         Raider var7 = (Raider)var6.next();
         if (var7.getUUID().equals(var2.getUUID())) {
            var5 = var7;
            break;
         }
      }

      if (var5 != null) {
         var4.remove(var5);
         var4.add(var2);
      }

      var4.add(var2);
      if (var3) {
         this.totalHealth += var2.getHealth();
      }

      this.updateBossbar();
      this.setDirty();
      return true;
   }

   public void setLeader(int var1, Raider var2) {
      this.groupToLeaderMap.put(var1, var2);
      var2.setItemSlot(EquipmentSlot.HEAD, getLeaderBannerInstance());
      var2.setDropChance(EquipmentSlot.HEAD, 2.0F);
   }

   public void removeLeader(int var1) {
      this.groupToLeaderMap.remove(var1);
   }

   public BlockPos getCenter() {
      return this.center;
   }

   private void setCenter(BlockPos var1) {
      this.center = var1;
   }

   public int getId() {
      return this.id;
   }

   private int getDefaultNumSpawns(Raid.RaiderType var1, int var2, boolean var3) {
      return var3 ? var1.spawnsPerWaveBeforeBonus[this.numGroups] : var1.spawnsPerWaveBeforeBonus[var2];
   }

   private int getPotentialBonusSpawns(Raid.RaiderType var1, Random var2, int var3, DifficultyInstance var4, boolean var5) {
      Difficulty var6 = var4.getDifficulty();
      boolean var7 = var6 == Difficulty.EASY;
      boolean var8 = var6 == Difficulty.NORMAL;
      int var9;
      switch(var1) {
      case WITCH:
         if (!var7 && var3 > 2 && var3 != 4) {
            var9 = 1;
            break;
         }

         return 0;
      case PILLAGER:
      case VINDICATOR:
         if (var7) {
            var9 = var2.nextInt(2);
         } else if (var8) {
            var9 = 1;
         } else {
            var9 = 2;
         }
         break;
      case RAVAGER:
         var9 = !var7 && var5 ? 1 : 0;
         break;
      default:
         return 0;
      }

      return var9 > 0 ? var2.nextInt(var9 + 1) : 0;
   }

   public boolean isActive() {
      return this.active;
   }

   public CompoundTag save(CompoundTag var1) {
      var1.putInt("Id", this.id);
      var1.putBoolean("Started", this.started);
      var1.putBoolean("Active", this.active);
      var1.putLong("TicksActive", this.ticksActive);
      var1.putInt("BadOmenLevel", this.badOmenLevel);
      var1.putInt("GroupsSpawned", this.groupsSpawned);
      var1.putInt("PreRaidTicks", this.raidCooldownTicks);
      var1.putInt("PostRaidTicks", this.postRaidTicks);
      var1.putFloat("TotalHealth", this.totalHealth);
      var1.putInt("NumGroups", this.numGroups);
      var1.putString("Status", this.status.getName());
      var1.putInt("CX", this.center.getX());
      var1.putInt("CY", this.center.getY());
      var1.putInt("CZ", this.center.getZ());
      ListTag var2 = new ListTag();
      Iterator var3 = this.heroesOfTheVillage.iterator();

      while(var3.hasNext()) {
         UUID var4 = (UUID)var3.next();
         var2.add(NbtUtils.createUUID(var4));
      }

      var1.put("HeroesOfTheVillage", var2);
      return var1;
   }

   public int getNumGroups(Difficulty var1) {
      switch(var1) {
      case EASY:
         return 3;
      case NORMAL:
         return 5;
      case HARD:
         return 7;
      default:
         return 0;
      }
   }

   public float getEnchantOdds() {
      int var1 = this.getBadOmenLevel();
      if (var1 == 2) {
         return 0.1F;
      } else if (var1 == 3) {
         return 0.25F;
      } else if (var1 == 4) {
         return 0.5F;
      } else {
         return var1 == 5 ? 0.75F : 0.0F;
      }
   }

   public void addHeroOfTheVillage(Entity var1) {
      this.heroesOfTheVillage.add(var1.getUUID());
   }

   static {
      RAID_BAR_VICTORY_COMPONENT = RAID_NAME_COMPONENT.copy().append(" - ").append(VICTORY);
      RAID_BAR_DEFEAT_COMPONENT = RAID_NAME_COMPONENT.copy().append(" - ").append(DEFEAT);
   }

   private static enum RaidStatus {
      ONGOING,
      VICTORY,
      LOSS,
      STOPPED;

      private static final Raid.RaidStatus[] VALUES = values();

      static Raid.RaidStatus getByName(String var0) {
         Raid.RaidStatus[] var1 = VALUES;
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            Raid.RaidStatus var4 = var1[var3];
            if (var0.equalsIgnoreCase(var4.name())) {
               return var4;
            }
         }

         return ONGOING;
      }

      public String getName() {
         return this.name().toLowerCase(Locale.ROOT);
      }

      // $FF: synthetic method
      private static Raid.RaidStatus[] $values() {
         return new Raid.RaidStatus[]{ONGOING, VICTORY, LOSS, STOPPED};
      }
   }

   private static enum RaiderType {
      VINDICATOR(EntityType.VINDICATOR, new int[]{0, 0, 2, 0, 1, 4, 2, 5}),
      EVOKER(EntityType.EVOKER, new int[]{0, 0, 0, 0, 0, 1, 1, 2}),
      PILLAGER(EntityType.PILLAGER, new int[]{0, 4, 3, 3, 4, 4, 4, 2}),
      WITCH(EntityType.WITCH, new int[]{0, 0, 0, 0, 3, 0, 0, 1}),
      RAVAGER(EntityType.RAVAGER, new int[]{0, 0, 0, 1, 0, 1, 0, 2});

      static final Raid.RaiderType[] VALUES = values();
      final EntityType<? extends Raider> entityType;
      final int[] spawnsPerWaveBeforeBonus;

      private RaiderType(EntityType<? extends Raider> var3, int[] var4) {
         this.entityType = var3;
         this.spawnsPerWaveBeforeBonus = var4;
      }

      // $FF: synthetic method
      private static Raid.RaiderType[] $values() {
         return new Raid.RaiderType[]{VINDICATOR, EVOKER, PILLAGER, WITCH, RAVAGER};
      }
   }
}
