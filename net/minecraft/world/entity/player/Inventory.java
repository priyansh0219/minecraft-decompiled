package net.minecraft.world.entity.player;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Nameable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class Inventory implements Container, Nameable {
   public static final int POP_TIME_DURATION = 5;
   public static final int INVENTORY_SIZE = 36;
   private static final int SELECTION_SIZE = 9;
   public static final int SLOT_OFFHAND = 40;
   public static final int NOT_FOUND_INDEX = -1;
   public static final int[] ALL_ARMOR_SLOTS = new int[]{0, 1, 2, 3};
   public static final int[] HELMET_SLOT_ONLY = new int[]{3};
   public final NonNullList<ItemStack> items;
   public final NonNullList<ItemStack> armor;
   public final NonNullList<ItemStack> offhand;
   private final List<NonNullList<ItemStack>> compartments;
   public int selected;
   public final Player player;
   private int timesChanged;

   public Inventory(Player var1) {
      this.items = NonNullList.withSize(36, ItemStack.EMPTY);
      this.armor = NonNullList.withSize(4, ItemStack.EMPTY);
      this.offhand = NonNullList.withSize(1, ItemStack.EMPTY);
      this.compartments = ImmutableList.of(this.items, this.armor, this.offhand);
      this.player = var1;
   }

   public ItemStack getSelected() {
      return isHotbarSlot(this.selected) ? (ItemStack)this.items.get(this.selected) : ItemStack.EMPTY;
   }

   public static int getSelectionSize() {
      return 9;
   }

   private boolean hasRemainingSpaceForItem(ItemStack var1, ItemStack var2) {
      return !var1.isEmpty() && ItemStack.isSameItemSameTags(var1, var2) && var1.isStackable() && var1.getCount() < var1.getMaxStackSize() && var1.getCount() < this.getMaxStackSize();
   }

   public int getFreeSlot() {
      for(int var1 = 0; var1 < this.items.size(); ++var1) {
         if (((ItemStack)this.items.get(var1)).isEmpty()) {
            return var1;
         }
      }

      return -1;
   }

   public void setPickedItem(ItemStack var1) {
      int var2 = this.findSlotMatchingItem(var1);
      if (isHotbarSlot(var2)) {
         this.selected = var2;
      } else {
         if (var2 == -1) {
            this.selected = this.getSuitableHotbarSlot();
            if (!((ItemStack)this.items.get(this.selected)).isEmpty()) {
               int var3 = this.getFreeSlot();
               if (var3 != -1) {
                  this.items.set(var3, (ItemStack)this.items.get(this.selected));
               }
            }

            this.items.set(this.selected, var1);
         } else {
            this.pickSlot(var2);
         }

      }
   }

   public void pickSlot(int var1) {
      this.selected = this.getSuitableHotbarSlot();
      ItemStack var2 = (ItemStack)this.items.get(this.selected);
      this.items.set(this.selected, (ItemStack)this.items.get(var1));
      this.items.set(var1, var2);
   }

   public static boolean isHotbarSlot(int var0) {
      return var0 >= 0 && var0 < 9;
   }

   public int findSlotMatchingItem(ItemStack var1) {
      for(int var2 = 0; var2 < this.items.size(); ++var2) {
         if (!((ItemStack)this.items.get(var2)).isEmpty() && ItemStack.isSameItemSameTags(var1, (ItemStack)this.items.get(var2))) {
            return var2;
         }
      }

      return -1;
   }

   public int findSlotMatchingUnusedItem(ItemStack var1) {
      for(int var2 = 0; var2 < this.items.size(); ++var2) {
         ItemStack var3 = (ItemStack)this.items.get(var2);
         if (!((ItemStack)this.items.get(var2)).isEmpty() && ItemStack.isSameItemSameTags(var1, (ItemStack)this.items.get(var2)) && !((ItemStack)this.items.get(var2)).isDamaged() && !var3.isEnchanted() && !var3.hasCustomHoverName()) {
            return var2;
         }
      }

      return -1;
   }

   public int getSuitableHotbarSlot() {
      int var1;
      int var2;
      for(var1 = 0; var1 < 9; ++var1) {
         var2 = (this.selected + var1) % 9;
         if (((ItemStack)this.items.get(var2)).isEmpty()) {
            return var2;
         }
      }

      for(var1 = 0; var1 < 9; ++var1) {
         var2 = (this.selected + var1) % 9;
         if (!((ItemStack)this.items.get(var2)).isEnchanted()) {
            return var2;
         }
      }

      return this.selected;
   }

   public void swapPaint(double var1) {
      if (var1 > 0.0D) {
         var1 = 1.0D;
      }

      if (var1 < 0.0D) {
         var1 = -1.0D;
      }

      for(this.selected = (int)((double)this.selected - var1); this.selected < 0; this.selected += 9) {
      }

      while(this.selected >= 9) {
         this.selected -= 9;
      }

   }

   public int clearOrCountMatchingItems(Predicate<ItemStack> var1, int var2, Container var3) {
      byte var4 = 0;
      boolean var5 = var2 == 0;
      int var7 = var4 + ContainerHelper.clearOrCountMatchingItems((Container)this, var1, var2 - var4, var5);
      var7 += ContainerHelper.clearOrCountMatchingItems(var3, var1, var2 - var7, var5);
      ItemStack var6 = this.player.containerMenu.getCarried();
      var7 += ContainerHelper.clearOrCountMatchingItems(var6, var1, var2 - var7, var5);
      if (var6.isEmpty()) {
         this.player.containerMenu.setCarried(ItemStack.EMPTY);
      }

      return var7;
   }

   private int addResource(ItemStack var1) {
      int var2 = this.getSlotWithRemainingSpace(var1);
      if (var2 == -1) {
         var2 = this.getFreeSlot();
      }

      return var2 == -1 ? var1.getCount() : this.addResource(var2, var1);
   }

   private int addResource(int var1, ItemStack var2) {
      Item var3 = var2.getItem();
      int var4 = var2.getCount();
      ItemStack var5 = this.getItem(var1);
      if (var5.isEmpty()) {
         var5 = new ItemStack(var3, 0);
         if (var2.hasTag()) {
            var5.setTag(var2.getTag().copy());
         }

         this.setItem(var1, var5);
      }

      int var6 = var4;
      if (var4 > var5.getMaxStackSize() - var5.getCount()) {
         var6 = var5.getMaxStackSize() - var5.getCount();
      }

      if (var6 > this.getMaxStackSize() - var5.getCount()) {
         var6 = this.getMaxStackSize() - var5.getCount();
      }

      if (var6 == 0) {
         return var4;
      } else {
         var4 -= var6;
         var5.grow(var6);
         var5.setPopTime(5);
         return var4;
      }
   }

   public int getSlotWithRemainingSpace(ItemStack var1) {
      if (this.hasRemainingSpaceForItem(this.getItem(this.selected), var1)) {
         return this.selected;
      } else if (this.hasRemainingSpaceForItem(this.getItem(40), var1)) {
         return 40;
      } else {
         for(int var2 = 0; var2 < this.items.size(); ++var2) {
            if (this.hasRemainingSpaceForItem((ItemStack)this.items.get(var2), var1)) {
               return var2;
            }
         }

         return -1;
      }
   }

   public void tick() {
      Iterator var1 = this.compartments.iterator();

      while(var1.hasNext()) {
         NonNullList var2 = (NonNullList)var1.next();

         for(int var3 = 0; var3 < var2.size(); ++var3) {
            if (!((ItemStack)var2.get(var3)).isEmpty()) {
               ((ItemStack)var2.get(var3)).inventoryTick(this.player.level, this.player, var3, this.selected == var3);
            }
         }
      }

   }

   public boolean add(ItemStack var1) {
      return this.add(-1, var1);
   }

   public boolean add(int var1, ItemStack var2) {
      if (var2.isEmpty()) {
         return false;
      } else {
         try {
            if (var2.isDamaged()) {
               if (var1 == -1) {
                  var1 = this.getFreeSlot();
               }

               if (var1 >= 0) {
                  this.items.set(var1, var2.copy());
                  ((ItemStack)this.items.get(var1)).setPopTime(5);
                  var2.setCount(0);
                  return true;
               } else if (this.player.getAbilities().instabuild) {
                  var2.setCount(0);
                  return true;
               } else {
                  return false;
               }
            } else {
               int var3;
               do {
                  var3 = var2.getCount();
                  if (var1 == -1) {
                     var2.setCount(this.addResource(var2));
                  } else {
                     var2.setCount(this.addResource(var1, var2));
                  }
               } while(!var2.isEmpty() && var2.getCount() < var3);

               if (var2.getCount() == var3 && this.player.getAbilities().instabuild) {
                  var2.setCount(0);
                  return true;
               } else {
                  return var2.getCount() < var3;
               }
            }
         } catch (Throwable var6) {
            CrashReport var4 = CrashReport.forThrowable(var6, "Adding item to inventory");
            CrashReportCategory var5 = var4.addCategory("Item being added");
            var5.setDetail("Item ID", (Object)Item.getId(var2.getItem()));
            var5.setDetail("Item data", (Object)var2.getDamageValue());
            var5.setDetail("Item name", () -> {
               return var2.getHoverName().getString();
            });
            throw new ReportedException(var4);
         }
      }
   }

   public void placeItemBackInInventory(ItemStack var1) {
      this.placeItemBackInInventory(var1, true);
   }

   public void placeItemBackInInventory(ItemStack var1, boolean var2) {
      while(true) {
         if (!var1.isEmpty()) {
            int var3 = this.getSlotWithRemainingSpace(var1);
            if (var3 == -1) {
               var3 = this.getFreeSlot();
            }

            if (var3 != -1) {
               int var4 = var1.getMaxStackSize() - this.getItem(var3).getCount();
               if (this.add(var3, var1.split(var4)) && var2 && this.player instanceof ServerPlayer) {
                  ((ServerPlayer)this.player).connection.send(new ClientboundContainerSetSlotPacket(-2, var3, this.getItem(var3)));
               }
               continue;
            }

            this.player.drop(var1, false);
         }

         return;
      }
   }

   public ItemStack removeItem(int var1, int var2) {
      NonNullList var3 = null;

      NonNullList var5;
      for(Iterator var4 = this.compartments.iterator(); var4.hasNext(); var1 -= var5.size()) {
         var5 = (NonNullList)var4.next();
         if (var1 < var5.size()) {
            var3 = var5;
            break;
         }
      }

      return var3 != null && !((ItemStack)var3.get(var1)).isEmpty() ? ContainerHelper.removeItem(var3, var1, var2) : ItemStack.EMPTY;
   }

   public void removeItem(ItemStack var1) {
      Iterator var2 = this.compartments.iterator();

      while(true) {
         while(var2.hasNext()) {
            NonNullList var3 = (NonNullList)var2.next();

            for(int var4 = 0; var4 < var3.size(); ++var4) {
               if (var3.get(var4) == var1) {
                  var3.set(var4, ItemStack.EMPTY);
                  break;
               }
            }
         }

         return;
      }
   }

   public ItemStack removeItemNoUpdate(int var1) {
      NonNullList var2 = null;

      NonNullList var4;
      for(Iterator var3 = this.compartments.iterator(); var3.hasNext(); var1 -= var4.size()) {
         var4 = (NonNullList)var3.next();
         if (var1 < var4.size()) {
            var2 = var4;
            break;
         }
      }

      if (var2 != null && !((ItemStack)var2.get(var1)).isEmpty()) {
         ItemStack var5 = (ItemStack)var2.get(var1);
         var2.set(var1, ItemStack.EMPTY);
         return var5;
      } else {
         return ItemStack.EMPTY;
      }
   }

   public void setItem(int var1, ItemStack var2) {
      NonNullList var3 = null;

      NonNullList var5;
      for(Iterator var4 = this.compartments.iterator(); var4.hasNext(); var1 -= var5.size()) {
         var5 = (NonNullList)var4.next();
         if (var1 < var5.size()) {
            var3 = var5;
            break;
         }
      }

      if (var3 != null) {
         var3.set(var1, var2);
      }

   }

   public float getDestroySpeed(BlockState var1) {
      return ((ItemStack)this.items.get(this.selected)).getDestroySpeed(var1);
   }

   public ListTag save(ListTag var1) {
      int var2;
      CompoundTag var3;
      for(var2 = 0; var2 < this.items.size(); ++var2) {
         if (!((ItemStack)this.items.get(var2)).isEmpty()) {
            var3 = new CompoundTag();
            var3.putByte("Slot", (byte)var2);
            ((ItemStack)this.items.get(var2)).save(var3);
            var1.add(var3);
         }
      }

      for(var2 = 0; var2 < this.armor.size(); ++var2) {
         if (!((ItemStack)this.armor.get(var2)).isEmpty()) {
            var3 = new CompoundTag();
            var3.putByte("Slot", (byte)(var2 + 100));
            ((ItemStack)this.armor.get(var2)).save(var3);
            var1.add(var3);
         }
      }

      for(var2 = 0; var2 < this.offhand.size(); ++var2) {
         if (!((ItemStack)this.offhand.get(var2)).isEmpty()) {
            var3 = new CompoundTag();
            var3.putByte("Slot", (byte)(var2 + 150));
            ((ItemStack)this.offhand.get(var2)).save(var3);
            var1.add(var3);
         }
      }

      return var1;
   }

   public void load(ListTag var1) {
      this.items.clear();
      this.armor.clear();
      this.offhand.clear();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         CompoundTag var3 = var1.getCompound(var2);
         int var4 = var3.getByte("Slot") & 255;
         ItemStack var5 = ItemStack.of(var3);
         if (!var5.isEmpty()) {
            if (var4 >= 0 && var4 < this.items.size()) {
               this.items.set(var4, var5);
            } else if (var4 >= 100 && var4 < this.armor.size() + 100) {
               this.armor.set(var4 - 100, var5);
            } else if (var4 >= 150 && var4 < this.offhand.size() + 150) {
               this.offhand.set(var4 - 150, var5);
            }
         }
      }

   }

   public int getContainerSize() {
      return this.items.size() + this.armor.size() + this.offhand.size();
   }

   public boolean isEmpty() {
      Iterator var1 = this.items.iterator();

      ItemStack var2;
      do {
         if (!var1.hasNext()) {
            var1 = this.armor.iterator();

            do {
               if (!var1.hasNext()) {
                  var1 = this.offhand.iterator();

                  do {
                     if (!var1.hasNext()) {
                        return true;
                     }

                     var2 = (ItemStack)var1.next();
                  } while(var2.isEmpty());

                  return false;
               }

               var2 = (ItemStack)var1.next();
            } while(var2.isEmpty());

            return false;
         }

         var2 = (ItemStack)var1.next();
      } while(var2.isEmpty());

      return false;
   }

   public ItemStack getItem(int var1) {
      NonNullList var2 = null;

      NonNullList var4;
      for(Iterator var3 = this.compartments.iterator(); var3.hasNext(); var1 -= var4.size()) {
         var4 = (NonNullList)var3.next();
         if (var1 < var4.size()) {
            var2 = var4;
            break;
         }
      }

      return var2 == null ? ItemStack.EMPTY : (ItemStack)var2.get(var1);
   }

   public Component getName() {
      return new TranslatableComponent("container.inventory");
   }

   public ItemStack getArmor(int var1) {
      return (ItemStack)this.armor.get(var1);
   }

   public void hurtArmor(DamageSource var1, float var2, int[] var3) {
      if (!(var2 <= 0.0F)) {
         var2 /= 4.0F;
         if (var2 < 1.0F) {
            var2 = 1.0F;
         }

         int[] var4 = var3;
         int var5 = var3.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            int var7 = var4[var6];
            ItemStack var8 = (ItemStack)this.armor.get(var7);
            if ((!var1.isFire() || !var8.getItem().isFireResistant()) && var8.getItem() instanceof ArmorItem) {
               var8.hurtAndBreak((int)var2, this.player, (var1x) -> {
                  var1x.broadcastBreakEvent(EquipmentSlot.byTypeAndIndex(EquipmentSlot.Type.ARMOR, var7));
               });
            }
         }

      }
   }

   public void dropAll() {
      Iterator var1 = this.compartments.iterator();

      while(var1.hasNext()) {
         List var2 = (List)var1.next();

         for(int var3 = 0; var3 < var2.size(); ++var3) {
            ItemStack var4 = (ItemStack)var2.get(var3);
            if (!var4.isEmpty()) {
               this.player.drop(var4, true, false);
               var2.set(var3, ItemStack.EMPTY);
            }
         }
      }

   }

   public void setChanged() {
      ++this.timesChanged;
   }

   public int getTimesChanged() {
      return this.timesChanged;
   }

   public boolean stillValid(Player var1) {
      if (this.player.isRemoved()) {
         return false;
      } else {
         return !(var1.distanceToSqr(this.player) > 64.0D);
      }
   }

   public boolean contains(ItemStack var1) {
      Iterator var2 = this.compartments.iterator();

      while(var2.hasNext()) {
         List var3 = (List)var2.next();
         Iterator var4 = var3.iterator();

         while(var4.hasNext()) {
            ItemStack var5 = (ItemStack)var4.next();
            if (!var5.isEmpty() && var5.sameItem(var1)) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean contains(Tag<Item> var1) {
      Iterator var2 = this.compartments.iterator();

      while(var2.hasNext()) {
         List var3 = (List)var2.next();
         Iterator var4 = var3.iterator();

         while(var4.hasNext()) {
            ItemStack var5 = (ItemStack)var4.next();
            if (!var5.isEmpty() && var5.is(var1)) {
               return true;
            }
         }
      }

      return false;
   }

   public void replaceWith(Inventory var1) {
      for(int var2 = 0; var2 < this.getContainerSize(); ++var2) {
         this.setItem(var2, var1.getItem(var2));
      }

      this.selected = var1.selected;
   }

   public void clearContent() {
      Iterator var1 = this.compartments.iterator();

      while(var1.hasNext()) {
         List var2 = (List)var1.next();
         var2.clear();
      }

   }

   public void fillStackedContents(StackedContents var1) {
      Iterator var2 = this.items.iterator();

      while(var2.hasNext()) {
         ItemStack var3 = (ItemStack)var2.next();
         var1.accountSimpleStack(var3);
      }

   }
}
