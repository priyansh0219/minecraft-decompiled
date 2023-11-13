package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.item.ItemStack;

public class ServerboundEditBookPacket implements Packet<ServerGamePacketListener> {
   private final ItemStack book;
   private final boolean signing;
   private final int slot;

   public ServerboundEditBookPacket(ItemStack var1, boolean var2, int var3) {
      this.book = var1.copy();
      this.signing = var2;
      this.slot = var3;
   }

   public ServerboundEditBookPacket(FriendlyByteBuf var1) {
      this.book = var1.readItem();
      this.signing = var1.readBoolean();
      this.slot = var1.readVarInt();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeItem(this.book);
      var1.writeBoolean(this.signing);
      var1.writeVarInt(this.slot);
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleEditBook(this);
   }

   public ItemStack getBook() {
      return this.book;
   }

   public boolean isSigning() {
      return this.signing;
   }

   public int getSlot() {
      return this.slot;
   }
}
