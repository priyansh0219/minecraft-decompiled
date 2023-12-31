package net.minecraft.client.gui.chat;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;

public class StandardChatListener implements ChatListener {
   private final Minecraft minecraft;

   public StandardChatListener(Minecraft var1) {
      this.minecraft = var1;
   }

   public void handle(ChatType var1, Component var2, UUID var3) {
      if (var1 != ChatType.CHAT) {
         this.minecraft.gui.getChat().addMessage(var2);
      } else {
         this.minecraft.gui.getChat().enqueueMessage(var2);
      }

   }
}
