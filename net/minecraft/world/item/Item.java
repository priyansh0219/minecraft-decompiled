package net.minecraft.world.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Item implements ItemLike {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final Map<Block, Item> BY_BLOCK = Maps.newHashMap();
   protected static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
   protected static final UUID BASE_ATTACK_SPEED_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
   public static final int MAX_STACK_SIZE = 64;
   public static final int EAT_DURATION = 32;
   public static final int MAX_BAR_WIDTH = 13;
   protected final CreativeModeTab category;
   private final Rarity rarity;
   private final int maxStackSize;
   private final int maxDamage;
   private final boolean isFireResistant;
   private final Item craftingRemainingItem;
   @Nullable
   private String descriptionId;
   @Nullable
   private final FoodProperties foodProperties;

   public static int getId(Item var0) {
      return var0 == null ? 0 : Registry.ITEM.getId(var0);
   }

   public static Item byId(int var0) {
      return (Item)Registry.ITEM.byId(var0);
   }

   @Deprecated
   public static Item byBlock(Block var0) {
      return (Item)BY_BLOCK.getOrDefault(var0, Items.AIR);
   }

   public Item(Item.Properties var1) {
      this.category = var1.category;
      this.rarity = var1.rarity;
      this.craftingRemainingItem = var1.craftingRemainingItem;
      this.maxDamage = var1.maxDamage;
      this.maxStackSize = var1.maxStackSize;
      this.foodProperties = var1.foodProperties;
      this.isFireResistant = var1.isFireResistant;
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         String var2 = this.getClass().getSimpleName();
         if (!var2.endsWith("Item")) {
            LOGGER.error("Item classes should end with Item and {} doesn't.", var2);
         }
      }

   }

   public void onUseTick(Level var1, LivingEntity var2, ItemStack var3, int var4) {
   }

   public void onDestroyed(ItemEntity var1) {
   }

   public void verifyTagAfterLoad(CompoundTag var1) {
   }

   public boolean canAttackBlock(BlockState var1, Level var2, BlockPos var3, Player var4) {
      return true;
   }

   public Item asItem() {
      return this;
   }

   public InteractionResult useOn(UseOnContext var1) {
      return InteractionResult.PASS;
   }

   public float getDestroySpeed(ItemStack var1, BlockState var2) {
      return 1.0F;
   }

   public InteractionResultHolder<ItemStack> use(Level var1, Player var2, InteractionHand var3) {
      if (this.isEdible()) {
         ItemStack var4 = var2.getItemInHand(var3);
         if (var2.canEat(this.getFoodProperties().canAlwaysEat())) {
            var2.startUsingItem(var3);
            return InteractionResultHolder.consume(var4);
         } else {
            return InteractionResultHolder.fail(var4);
         }
      } else {
         return InteractionResultHolder.pass(var2.getItemInHand(var3));
      }
   }

   public ItemStack finishUsingItem(ItemStack var1, Level var2, LivingEntity var3) {
      return this.isEdible() ? var3.eat(var2, var1) : var1;
   }

   public final int getMaxStackSize() {
      return this.maxStackSize;
   }

   public final int getMaxDamage() {
      return this.maxDamage;
   }

   public boolean canBeDepleted() {
      return this.maxDamage > 0;
   }

   public boolean isBarVisible(ItemStack var1) {
      return var1.isDamaged();
   }

   public int getBarWidth(ItemStack var1) {
      return Math.round(13.0F - (float)var1.getDamageValue() * 13.0F / (float)this.maxDamage);
   }

   public int getBarColor(ItemStack var1) {
      float var2 = Math.max(0.0F, ((float)this.maxDamage - (float)var1.getDamageValue()) / (float)this.maxDamage);
      return Mth.hsvToRgb(var2 / 3.0F, 1.0F, 1.0F);
   }

   public boolean overrideStackedOnOther(ItemStack var1, Slot var2, ClickAction var3, Player var4) {
      return false;
   }

   public boolean overrideOtherStackedOnMe(ItemStack var1, ItemStack var2, Slot var3, ClickAction var4, Player var5, SlotAccess var6) {
      return false;
   }

   public boolean hurtEnemy(ItemStack var1, LivingEntity var2, LivingEntity var3) {
      return false;
   }

   public boolean mineBlock(ItemStack var1, Level var2, BlockState var3, BlockPos var4, LivingEntity var5) {
      return false;
   }

   public boolean isCorrectToolForDrops(BlockState var1) {
      return false;
   }

   public InteractionResult interactLivingEntity(ItemStack var1, Player var2, LivingEntity var3, InteractionHand var4) {
      return InteractionResult.PASS;
   }

   public Component getDescription() {
      return new TranslatableComponent(this.getDescriptionId());
   }

   public String toString() {
      return Registry.ITEM.getKey(this).getPath();
   }

   protected String getOrCreateDescriptionId() {
      if (this.descriptionId == null) {
         this.descriptionId = Util.makeDescriptionId("item", Registry.ITEM.getKey(this));
      }

      return this.descriptionId;
   }

   public String getDescriptionId() {
      return this.getOrCreateDescriptionId();
   }

   public String getDescriptionId(ItemStack var1) {
      return this.getDescriptionId();
   }

   public boolean shouldOverrideMultiplayerNbt() {
      return true;
   }

   @Nullable
   public final Item getCraftingRemainingItem() {
      return this.craftingRemainingItem;
   }

   public boolean hasCraftingRemainingItem() {
      return this.craftingRemainingItem != null;
   }

   public void inventoryTick(ItemStack var1, Level var2, Entity var3, int var4, boolean var5) {
   }

   public void onCraftedBy(ItemStack var1, Level var2, Player var3) {
   }

   public boolean isComplex() {
      return false;
   }

   public UseAnim getUseAnimation(ItemStack var1) {
      return var1.getItem().isEdible() ? UseAnim.EAT : UseAnim.NONE;
   }

   public int getUseDuration(ItemStack var1) {
      if (var1.getItem().isEdible()) {
         return this.getFoodProperties().isFastFood() ? 16 : 32;
      } else {
         return 0;
      }
   }

   public void releaseUsing(ItemStack var1, Level var2, LivingEntity var3, int var4) {
   }

   public void appendHoverText(ItemStack var1, @Nullable Level var2, List<Component> var3, TooltipFlag var4) {
   }

   public Optional<TooltipComponent> getTooltipImage(ItemStack var1) {
      return Optional.empty();
   }

   public Component getName(ItemStack var1) {
      return new TranslatableComponent(this.getDescriptionId(var1));
   }

   public boolean isFoil(ItemStack var1) {
      return var1.isEnchanted();
   }

   public Rarity getRarity(ItemStack var1) {
      if (!var1.isEnchanted()) {
         return this.rarity;
      } else {
         switch(this.rarity) {
         case COMMON:
         case UNCOMMON:
            return Rarity.RARE;
         case RARE:
            return Rarity.EPIC;
         case EPIC:
         default:
            return this.rarity;
         }
      }
   }

   public boolean isEnchantable(ItemStack var1) {
      return this.getMaxStackSize() == 1 && this.canBeDepleted();
   }

   protected static BlockHitResult getPlayerPOVHitResult(Level var0, Player var1, ClipContext.Fluid var2) {
      float var3 = var1.getXRot();
      float var4 = var1.getYRot();
      Vec3 var5 = var1.getEyePosition();
      float var6 = Mth.cos(-var4 * 0.017453292F - 3.1415927F);
      float var7 = Mth.sin(-var4 * 0.017453292F - 3.1415927F);
      float var8 = -Mth.cos(-var3 * 0.017453292F);
      float var9 = Mth.sin(-var3 * 0.017453292F);
      float var10 = var7 * var8;
      float var12 = var6 * var8;
      double var13 = 5.0D;
      Vec3 var15 = var5.add((double)var10 * 5.0D, (double)var9 * 5.0D, (double)var12 * 5.0D);
      return var0.clip(new ClipContext(var5, var15, ClipContext.Block.OUTLINE, var2, var1));
   }

   public int getEnchantmentValue() {
      return 0;
   }

   public void fillItemCategory(CreativeModeTab var1, NonNullList<ItemStack> var2) {
      if (this.allowdedIn(var1)) {
         var2.add(new ItemStack(this));
      }

   }

   protected boolean allowdedIn(CreativeModeTab var1) {
      CreativeModeTab var2 = this.getItemCategory();
      return var2 != null && (var1 == CreativeModeTab.TAB_SEARCH || var1 == var2);
   }

   @Nullable
   public final CreativeModeTab getItemCategory() {
      return this.category;
   }

   public boolean isValidRepairItem(ItemStack var1, ItemStack var2) {
      return false;
   }

   public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot var1) {
      return ImmutableMultimap.of();
   }

   public boolean useOnRelease(ItemStack var1) {
      return false;
   }

   public ItemStack getDefaultInstance() {
      return new ItemStack(this);
   }

   public boolean isEdible() {
      return this.foodProperties != null;
   }

   @Nullable
   public FoodProperties getFoodProperties() {
      return this.foodProperties;
   }

   public SoundEvent getDrinkingSound() {
      return SoundEvents.GENERIC_DRINK;
   }

   public SoundEvent getEatingSound() {
      return SoundEvents.GENERIC_EAT;
   }

   public boolean isFireResistant() {
      return this.isFireResistant;
   }

   public boolean canBeHurtBy(DamageSource var1) {
      return !this.isFireResistant || !var1.isFire();
   }

   @Nullable
   public SoundEvent getEquipSound() {
      return null;
   }

   public boolean canFitInsideContainerItems() {
      return true;
   }

   public static class Properties {
      int maxStackSize = 64;
      int maxDamage;
      Item craftingRemainingItem;
      CreativeModeTab category;
      Rarity rarity;
      FoodProperties foodProperties;
      boolean isFireResistant;

      public Properties() {
         this.rarity = Rarity.COMMON;
      }

      public Item.Properties food(FoodProperties var1) {
         this.foodProperties = var1;
         return this;
      }

      public Item.Properties stacksTo(int var1) {
         if (this.maxDamage > 0) {
            throw new RuntimeException("Unable to have damage AND stack.");
         } else {
            this.maxStackSize = var1;
            return this;
         }
      }

      public Item.Properties defaultDurability(int var1) {
         return this.maxDamage == 0 ? this.durability(var1) : this;
      }

      public Item.Properties durability(int var1) {
         this.maxDamage = var1;
         this.maxStackSize = 1;
         return this;
      }

      public Item.Properties craftRemainder(Item var1) {
         this.craftingRemainingItem = var1;
         return this;
      }

      public Item.Properties tab(CreativeModeTab var1) {
         this.category = var1;
         return this;
      }

      public Item.Properties rarity(Rarity var1) {
         this.rarity = var1;
         return this;
      }

      public Item.Properties fireResistant() {
         this.isFireResistant = true;
         return this;
      }
   }
}
