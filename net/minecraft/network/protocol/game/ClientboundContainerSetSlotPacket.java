package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.item.ItemStack;

public class ClientboundContainerSetSlotPacket implements Packet<ClientGamePacketListener> {
   public static final int CARRIED_ITEM = -1;
   public static final int PLAYER_INVENTORY = -2;
   private final int containerId;
   private final int slot;
   private final ItemStack itemStack;

   public ClientboundContainerSetSlotPacket(int var1, int var2, ItemStack var3) {
      this.containerId = var1;
      this.slot = var2;
      this.itemStack = var3.copy();
   }

   public ClientboundContainerSetSlotPacket(FriendlyByteBuf var1) {
      this.containerId = var1.readByte();
      this.slot = var1.readShort();
      this.itemStack = var1.readItem();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeByte(this.containerId);
      var1.writeShort(this.slot);
      var1.writeItem(this.itemStack);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleContainerSetSlot(this);
   }

   public int getContainerId() {
      return this.containerId;
   }

   public int getSlot() {
      return this.slot;
   }

   public ItemStack getItem() {
      return this.itemStack;
   }
}
