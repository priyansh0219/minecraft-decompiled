package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

public class CreateFlatWorldScreen extends Screen {
   private static final int SLOT_TEX_SIZE = 128;
   private static final int SLOT_BG_SIZE = 18;
   private static final int SLOT_STAT_HEIGHT = 20;
   private static final int SLOT_BG_X = 1;
   private static final int SLOT_BG_Y = 1;
   private static final int SLOT_FG_X = 2;
   private static final int SLOT_FG_Y = 2;
   protected final CreateWorldScreen parent;
   private final Consumer<FlatLevelGeneratorSettings> applySettings;
   FlatLevelGeneratorSettings generator;
   private Component columnType;
   private Component columnHeight;
   private CreateFlatWorldScreen.DetailsList list;
   private Button deleteLayerButton;

   public CreateFlatWorldScreen(CreateWorldScreen var1, Consumer<FlatLevelGeneratorSettings> var2, FlatLevelGeneratorSettings var3) {
      super(new TranslatableComponent("createWorld.customize.flat.title"));
      this.parent = var1;
      this.applySettings = var2;
      this.generator = var3;
   }

   public FlatLevelGeneratorSettings settings() {
      return this.generator;
   }

   public void setConfig(FlatLevelGeneratorSettings var1) {
      this.generator = var1;
   }

   protected void init() {
      this.columnType = new TranslatableComponent("createWorld.customize.flat.tile");
      this.columnHeight = new TranslatableComponent("createWorld.customize.flat.height");
      this.list = new CreateFlatWorldScreen.DetailsList();
      this.addWidget(this.list);
      this.deleteLayerButton = (Button)this.addRenderableWidget(new Button(this.width / 2 - 155, this.height - 52, 150, 20, new TranslatableComponent("createWorld.customize.flat.removeLayer"), (var1) -> {
         if (this.hasValidSelection()) {
            List var2 = this.generator.getLayersInfo();
            int var3 = this.list.children().indexOf(this.list.getSelected());
            int var4 = var2.size() - var3 - 1;
            var2.remove(var4);
            this.list.setSelected(var2.isEmpty() ? null : (CreateFlatWorldScreen.DetailsList.Entry)this.list.children().get(Math.min(var3, var2.size() - 1)));
            this.generator.updateLayers();
            this.list.resetRows();
            this.updateButtonValidity();
         }
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 5, this.height - 52, 150, 20, new TranslatableComponent("createWorld.customize.presets"), (var1) -> {
         this.minecraft.setScreen(new PresetFlatWorldScreen(this));
         this.generator.updateLayers();
         this.updateButtonValidity();
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 155, this.height - 28, 150, 20, CommonComponents.GUI_DONE, (var1) -> {
         this.applySettings.accept(this.generator);
         this.minecraft.setScreen(this.parent);
         this.generator.updateLayers();
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 5, this.height - 28, 150, 20, CommonComponents.GUI_CANCEL, (var1) -> {
         this.minecraft.setScreen(this.parent);
         this.generator.updateLayers();
      }));
      this.generator.updateLayers();
      this.updateButtonValidity();
   }

   void updateButtonValidity() {
      this.deleteLayerButton.active = this.hasValidSelection();
   }

   private boolean hasValidSelection() {
      return this.list.getSelected() != null;
   }

   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   public void render(PoseStack var1, int var2, int var3, float var4) {
      this.renderBackground(var1);
      this.list.render(var1, var2, var3, var4);
      drawCenteredString(var1, this.font, this.title, this.width / 2, 8, 16777215);
      int var5 = this.width / 2 - 92 - 16;
      drawString(var1, this.font, this.columnType, var5, 32, 16777215);
      drawString(var1, this.font, this.columnHeight, var5 + 2 + 213 - this.font.width((FormattedText)this.columnHeight), 32, 16777215);
      super.render(var1, var2, var3, var4);
   }

   class DetailsList extends ObjectSelectionList<CreateFlatWorldScreen.DetailsList.Entry> {
      public DetailsList() {
         super(CreateFlatWorldScreen.this.minecraft, CreateFlatWorldScreen.this.width, CreateFlatWorldScreen.this.height, 43, CreateFlatWorldScreen.this.height - 60, 24);

         for(int var2 = 0; var2 < CreateFlatWorldScreen.this.generator.getLayersInfo().size(); ++var2) {
            this.addEntry(new CreateFlatWorldScreen.DetailsList.Entry());
         }

      }

      public void setSelected(@Nullable CreateFlatWorldScreen.DetailsList.Entry var1) {
         super.setSelected(var1);
         CreateFlatWorldScreen.this.updateButtonValidity();
      }

      protected boolean isFocused() {
         return CreateFlatWorldScreen.this.getFocused() == this;
      }

      protected int getScrollbarPosition() {
         return this.width - 70;
      }

      public void resetRows() {
         int var1 = this.children().indexOf(this.getSelected());
         this.clearEntries();

         for(int var2 = 0; var2 < CreateFlatWorldScreen.this.generator.getLayersInfo().size(); ++var2) {
            this.addEntry(new CreateFlatWorldScreen.DetailsList.Entry());
         }

         List var3 = this.children();
         if (var1 >= 0 && var1 < var3.size()) {
            this.setSelected((CreateFlatWorldScreen.DetailsList.Entry)var3.get(var1));
         }

      }

      private class Entry extends ObjectSelectionList.Entry<CreateFlatWorldScreen.DetailsList.Entry> {
         Entry() {
         }

         public void render(PoseStack var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
            FlatLayerInfo var11 = (FlatLayerInfo)CreateFlatWorldScreen.this.generator.getLayersInfo().get(CreateFlatWorldScreen.this.generator.getLayersInfo().size() - var2 - 1);
            BlockState var12 = var11.getBlockState();
            ItemStack var13 = this.getDisplayItem(var12);
            this.blitSlot(var1, var4, var3, var13);
            CreateFlatWorldScreen.this.font.draw(var1, var13.getHoverName(), (float)(var4 + 18 + 5), (float)(var3 + 3), 16777215);
            TranslatableComponent var14;
            if (var2 == 0) {
               var14 = new TranslatableComponent("createWorld.customize.flat.layer.top", new Object[]{var11.getHeight()});
            } else if (var2 == CreateFlatWorldScreen.this.generator.getLayersInfo().size() - 1) {
               var14 = new TranslatableComponent("createWorld.customize.flat.layer.bottom", new Object[]{var11.getHeight()});
            } else {
               var14 = new TranslatableComponent("createWorld.customize.flat.layer", new Object[]{var11.getHeight()});
            }

            CreateFlatWorldScreen.this.font.draw(var1, (Component)var14, (float)(var4 + 2 + 213 - CreateFlatWorldScreen.this.font.width((FormattedText)var14)), (float)(var3 + 3), 16777215);
         }

         private ItemStack getDisplayItem(BlockState var1) {
            Item var2 = var1.getBlock().asItem();
            if (var2 == Items.AIR) {
               if (var1.is(Blocks.WATER)) {
                  var2 = Items.WATER_BUCKET;
               } else if (var1.is(Blocks.LAVA)) {
                  var2 = Items.LAVA_BUCKET;
               }
            }

            return new ItemStack(var2);
         }

         public Component getNarration() {
            FlatLayerInfo var1 = (FlatLayerInfo)CreateFlatWorldScreen.this.generator.getLayersInfo().get(CreateFlatWorldScreen.this.generator.getLayersInfo().size() - DetailsList.this.children().indexOf(this) - 1);
            ItemStack var2 = this.getDisplayItem(var1.getBlockState());
            return (Component)(!var2.isEmpty() ? new TranslatableComponent("narrator.select", new Object[]{var2.getHoverName()}) : TextComponent.EMPTY);
         }

         public boolean mouseClicked(double var1, double var3, int var5) {
            if (var5 == 0) {
               DetailsList.this.setSelected(this);
               return true;
            } else {
               return false;
            }
         }

         private void blitSlot(PoseStack var1, int var2, int var3, ItemStack var4) {
            this.blitSlotBg(var1, var2 + 1, var3 + 1);
            if (!var4.isEmpty()) {
               CreateFlatWorldScreen.this.itemRenderer.renderGuiItem(var4, var2 + 2, var3 + 2);
            }

         }

         private void blitSlotBg(PoseStack var1, int var2, int var3) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, GuiComponent.STATS_ICON_LOCATION);
            GuiComponent.blit(var1, var2, var3, CreateFlatWorldScreen.this.getBlitOffset(), 0.0F, 0.0F, 18, 18, 128, 128);
         }
      }
   }
}
