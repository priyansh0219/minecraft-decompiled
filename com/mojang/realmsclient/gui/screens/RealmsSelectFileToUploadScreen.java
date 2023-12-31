package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.realms.RealmsLabel;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RealmsSelectFileToUploadScreen extends RealmsScreen {
   private static final Logger LOGGER = LogManager.getLogger();
   static final Component WORLD_TEXT = new TranslatableComponent("selectWorld.world");
   static final Component REQUIRES_CONVERSION_TEXT = new TranslatableComponent("selectWorld.conversion");
   static final Component HARDCORE_TEXT;
   static final Component CHEATS_TEXT;
   private static final DateFormat DATE_FORMAT;
   private final RealmsResetWorldScreen lastScreen;
   private final long worldId;
   private final int slotId;
   Button uploadButton;
   List<LevelSummary> levelList = Lists.newArrayList();
   int selectedWorld = -1;
   RealmsSelectFileToUploadScreen.WorldSelectionList worldSelectionList;
   private final Runnable callback;

   public RealmsSelectFileToUploadScreen(long var1, int var3, RealmsResetWorldScreen var4, Runnable var5) {
      super(new TranslatableComponent("mco.upload.select.world.title"));
      this.lastScreen = var4;
      this.worldId = var1;
      this.slotId = var3;
      this.callback = var5;
   }

   private void loadLevelList() throws Exception {
      this.levelList = (List)this.minecraft.getLevelSource().getLevelList().stream().sorted((var0, var1x) -> {
         if (var0.getLastPlayed() < var1x.getLastPlayed()) {
            return 1;
         } else {
            return var0.getLastPlayed() > var1x.getLastPlayed() ? -1 : var0.getLevelId().compareTo(var1x.getLevelId());
         }
      }).collect(Collectors.toList());
      Iterator var1 = this.levelList.iterator();

      while(var1.hasNext()) {
         LevelSummary var2 = (LevelSummary)var1.next();
         this.worldSelectionList.addEntry(var2);
      }

   }

   public void init() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      this.worldSelectionList = new RealmsSelectFileToUploadScreen.WorldSelectionList();

      try {
         this.loadLevelList();
      } catch (Exception var2) {
         LOGGER.error("Couldn't load level list", var2);
         this.minecraft.setScreen(new RealmsGenericErrorScreen(new TextComponent("Unable to load worlds"), Component.nullToEmpty(var2.getMessage()), this.lastScreen));
         return;
      }

      this.addWidget(this.worldSelectionList);
      this.uploadButton = (Button)this.addRenderableWidget(new Button(this.width / 2 - 154, this.height - 32, 153, 20, new TranslatableComponent("mco.upload.button.name"), (var1) -> {
         this.upload();
      }));
      this.uploadButton.active = this.selectedWorld >= 0 && this.selectedWorld < this.levelList.size();
      this.addRenderableWidget(new Button(this.width / 2 + 6, this.height - 32, 153, 20, CommonComponents.GUI_BACK, (var1) -> {
         this.minecraft.setScreen(this.lastScreen);
      }));
      this.addLabel(new RealmsLabel(new TranslatableComponent("mco.upload.select.world.subtitle"), this.width / 2, row(-1), 10526880));
      if (this.levelList.isEmpty()) {
         this.addLabel(new RealmsLabel(new TranslatableComponent("mco.upload.select.world.none"), this.width / 2, this.height / 2 - 20, 16777215));
      }

   }

   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(this.getTitle(), this.createLabelNarration());
   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   private void upload() {
      if (this.selectedWorld != -1 && !((LevelSummary)this.levelList.get(this.selectedWorld)).isHardcore()) {
         LevelSummary var1 = (LevelSummary)this.levelList.get(this.selectedWorld);
         this.minecraft.setScreen(new RealmsUploadScreen(this.worldId, this.slotId, this.lastScreen, var1, this.callback));
      }

   }

   public void render(PoseStack var1, int var2, int var3, float var4) {
      this.renderBackground(var1);
      this.worldSelectionList.render(var1, var2, var3, var4);
      drawCenteredString(var1, this.font, this.title, this.width / 2, 13, 16777215);
      super.render(var1, var2, var3, var4);
   }

   public boolean keyPressed(int var1, int var2, int var3) {
      if (var1 == 256) {
         this.minecraft.setScreen(this.lastScreen);
         return true;
      } else {
         return super.keyPressed(var1, var2, var3);
      }
   }

   static Component gameModeName(LevelSummary var0) {
      return var0.getGameMode().getLongDisplayName();
   }

   static String formatLastPlayed(LevelSummary var0) {
      return DATE_FORMAT.format(new Date(var0.getLastPlayed()));
   }

   static {
      HARDCORE_TEXT = (new TranslatableComponent("mco.upload.hardcore")).withStyle(ChatFormatting.DARK_RED);
      CHEATS_TEXT = new TranslatableComponent("selectWorld.cheats");
      DATE_FORMAT = new SimpleDateFormat();
   }

   private class WorldSelectionList extends RealmsObjectSelectionList<RealmsSelectFileToUploadScreen.Entry> {
      public WorldSelectionList() {
         super(RealmsSelectFileToUploadScreen.this.width, RealmsSelectFileToUploadScreen.this.height, RealmsSelectFileToUploadScreen.row(0), RealmsSelectFileToUploadScreen.this.height - 40, 36);
      }

      public void addEntry(LevelSummary var1) {
         this.addEntry(RealmsSelectFileToUploadScreen.this.new Entry(var1));
      }

      public int getMaxPosition() {
         return RealmsSelectFileToUploadScreen.this.levelList.size() * 36;
      }

      public boolean isFocused() {
         return RealmsSelectFileToUploadScreen.this.getFocused() == this;
      }

      public void renderBackground(PoseStack var1) {
         RealmsSelectFileToUploadScreen.this.renderBackground(var1);
      }

      public void setSelected(@Nullable RealmsSelectFileToUploadScreen.Entry var1) {
         super.setSelected(var1);
         RealmsSelectFileToUploadScreen.this.selectedWorld = this.children().indexOf(var1);
         RealmsSelectFileToUploadScreen.this.uploadButton.active = RealmsSelectFileToUploadScreen.this.selectedWorld >= 0 && RealmsSelectFileToUploadScreen.this.selectedWorld < this.getItemCount() && !((LevelSummary)RealmsSelectFileToUploadScreen.this.levelList.get(RealmsSelectFileToUploadScreen.this.selectedWorld)).isHardcore();
      }
   }

   class Entry extends ObjectSelectionList.Entry<RealmsSelectFileToUploadScreen.Entry> {
      private final LevelSummary levelSummary;
      private final String name;
      private final String id;
      private final Component info;

      public Entry(LevelSummary var2) {
         this.levelSummary = var2;
         this.name = var2.getLevelName();
         String var10001 = var2.getLevelId();
         this.id = var10001 + " (" + RealmsSelectFileToUploadScreen.formatLastPlayed(var2) + ")";
         if (var2.isRequiresConversion()) {
            this.info = RealmsSelectFileToUploadScreen.REQUIRES_CONVERSION_TEXT;
         } else {
            Object var3;
            if (var2.isHardcore()) {
               var3 = RealmsSelectFileToUploadScreen.HARDCORE_TEXT;
            } else {
               var3 = RealmsSelectFileToUploadScreen.gameModeName(var2);
            }

            if (var2.hasCheats()) {
               var3 = ((Component)var3).copy().append(", ").append(RealmsSelectFileToUploadScreen.CHEATS_TEXT);
            }

            this.info = (Component)var3;
         }

      }

      public void render(PoseStack var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         this.renderItem(var1, var2, var4, var3);
      }

      public boolean mouseClicked(double var1, double var3, int var5) {
         RealmsSelectFileToUploadScreen.this.worldSelectionList.selectItem(RealmsSelectFileToUploadScreen.this.levelList.indexOf(this.levelSummary));
         return true;
      }

      protected void renderItem(PoseStack var1, int var2, int var3, int var4) {
         String var5;
         if (this.name.isEmpty()) {
            var5 = RealmsSelectFileToUploadScreen.WORLD_TEXT + " " + (var2 + 1);
         } else {
            var5 = this.name;
         }

         RealmsSelectFileToUploadScreen.this.font.draw(var1, var5, (float)(var3 + 2), (float)(var4 + 1), 16777215);
         RealmsSelectFileToUploadScreen.this.font.draw(var1, this.id, (float)(var3 + 2), (float)(var4 + 12), 8421504);
         RealmsSelectFileToUploadScreen.this.font.draw(var1, this.info, (float)(var3 + 2), (float)(var4 + 12 + 10), 8421504);
      }

      public Component getNarration() {
         Component var1 = CommonComponents.joinLines(new TextComponent(this.levelSummary.getLevelName()), new TextComponent(RealmsSelectFileToUploadScreen.formatLastPlayed(this.levelSummary)), RealmsSelectFileToUploadScreen.gameModeName(this.levelSummary));
         return new TranslatableComponent("narrator.select", new Object[]{var1});
      }
   }
}
