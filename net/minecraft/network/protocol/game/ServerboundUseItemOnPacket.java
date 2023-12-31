package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

public class ServerboundUseItemOnPacket implements Packet<ServerGamePacketListener> {
   private final BlockHitResult blockHit;
   private final InteractionHand hand;

   public ServerboundUseItemOnPacket(InteractionHand var1, BlockHitResult var2) {
      this.hand = var1;
      this.blockHit = var2;
   }

   public ServerboundUseItemOnPacket(FriendlyByteBuf var1) {
      this.hand = (InteractionHand)var1.readEnum(InteractionHand.class);
      this.blockHit = var1.readBlockHitResult();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeEnum(this.hand);
      var1.writeBlockHitResult(this.blockHit);
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleUseItemOn(this);
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public BlockHitResult getHitResult() {
      return this.blockHit;
   }
}
