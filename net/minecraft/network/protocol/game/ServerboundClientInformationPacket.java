package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;

public class ServerboundClientInformationPacket implements Packet<ServerGamePacketListener> {
   public static final int MAX_LANGUAGE_LENGTH = 16;
   private final String language;
   private final int viewDistance;
   private final ChatVisiblity chatVisibility;
   private final boolean chatColors;
   private final int modelCustomisation;
   private final HumanoidArm mainHand;
   private final boolean textFilteringEnabled;

   public ServerboundClientInformationPacket(String var1, int var2, ChatVisiblity var3, boolean var4, int var5, HumanoidArm var6, boolean var7) {
      this.language = var1;
      this.viewDistance = var2;
      this.chatVisibility = var3;
      this.chatColors = var4;
      this.modelCustomisation = var5;
      this.mainHand = var6;
      this.textFilteringEnabled = var7;
   }

   public ServerboundClientInformationPacket(FriendlyByteBuf var1) {
      this.language = var1.readUtf(16);
      this.viewDistance = var1.readByte();
      this.chatVisibility = (ChatVisiblity)var1.readEnum(ChatVisiblity.class);
      this.chatColors = var1.readBoolean();
      this.modelCustomisation = var1.readUnsignedByte();
      this.mainHand = (HumanoidArm)var1.readEnum(HumanoidArm.class);
      this.textFilteringEnabled = var1.readBoolean();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeUtf(this.language);
      var1.writeByte(this.viewDistance);
      var1.writeEnum(this.chatVisibility);
      var1.writeBoolean(this.chatColors);
      var1.writeByte(this.modelCustomisation);
      var1.writeEnum(this.mainHand);
      var1.writeBoolean(this.textFilteringEnabled);
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleClientInformation(this);
   }

   public String getLanguage() {
      return this.language;
   }

   public int getViewDistance() {
      return this.viewDistance;
   }

   public ChatVisiblity getChatVisibility() {
      return this.chatVisibility;
   }

   public boolean getChatColors() {
      return this.chatColors;
   }

   public int getModelCustomisation() {
      return this.modelCustomisation;
   }

   public HumanoidArm getMainHand() {
      return this.mainHand;
   }

   public boolean isTextFilteringEnabled() {
      return this.textFilteringEnabled;
   }
}