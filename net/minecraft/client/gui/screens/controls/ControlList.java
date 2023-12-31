package net.minecraft.client.gui.screens.controls;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.commons.lang3.ArrayUtils;

public class ControlList extends ContainerObjectSelectionList<ControlList.Entry> {
   final ControlsScreen controlsScreen;
   int maxNameWidth;

   public ControlList(ControlsScreen var1, Minecraft var2) {
      super(var2, var1.width + 45, var1.height, 43, var1.height - 32, 20);
      this.controlsScreen = var1;
      KeyMapping[] var3 = (KeyMapping[])ArrayUtils.clone(var2.options.keyMappings);
      Arrays.sort(var3);
      String var4 = null;
      KeyMapping[] var5 = var3;
      int var6 = var3.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         KeyMapping var8 = var5[var7];
         String var9 = var8.getCategory();
         if (!var9.equals(var4)) {
            var4 = var9;
            this.addEntry(new ControlList.CategoryEntry(new TranslatableComponent(var9)));
         }

         TranslatableComponent var10 = new TranslatableComponent(var8.getName());
         int var11 = var2.font.width((FormattedText)var10);
         if (var11 > this.maxNameWidth) {
            this.maxNameWidth = var11;
         }

         this.addEntry(new ControlList.KeyEntry(var8, var10));
      }

   }

   protected int getScrollbarPosition() {
      return super.getScrollbarPosition() + 15;
   }

   public int getRowWidth() {
      return super.getRowWidth() + 32;
   }

   public class CategoryEntry extends ControlList.Entry {
      final Component name;
      private final int width;

      public CategoryEntry(Component var2) {
         this.name = var2;
         this.width = ControlList.this.minecraft.font.width((FormattedText)this.name);
      }

      public void render(PoseStack var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         Font var10000 = ControlList.this.minecraft.font;
         Component var10002 = this.name;
         float var10003 = (float)(ControlList.this.minecraft.screen.width / 2 - this.width / 2);
         int var10004 = var3 + var6;
         Objects.requireNonNull(ControlList.this.minecraft.font);
         var10000.draw(var1, var10002, var10003, (float)(var10004 - 9 - 1), 16777215);
      }

      public boolean changeFocus(boolean var1) {
         return false;
      }

      public List<? extends GuiEventListener> children() {
         return Collections.emptyList();
      }

      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(new NarratableEntry() {
            // $FF: synthetic field
            final ControlList.CategoryEntry this$1;

            {
               this.this$1 = var1;
            }

            public NarratableEntry.NarrationPriority narrationPriority() {
               return NarratableEntry.NarrationPriority.HOVERED;
            }

            public void updateNarration(NarrationElementOutput var1) {
               var1.add(NarratedElementType.TITLE, this.this$1.name);
            }
         });
      }
   }

   public class KeyEntry extends ControlList.Entry {
      private final KeyMapping key;
      private final Component name;
      private final Button changeButton;
      private final Button resetButton;

      KeyEntry(KeyMapping var2, Component var3) {
         this.key = var2;
         this.name = var3;
         this.changeButton = new Button(0, 0, 75, 20, var3, (var2x) -> {
            ControlList.this.controlsScreen.selectedKey = var2;
         }, ControlList.this, var2, var3) {
            // $FF: synthetic field
            final ControlList val$this$0;
            // $FF: synthetic field
            final KeyMapping val$key;
            // $FF: synthetic field
            final Component val$name;
            // $FF: synthetic field
            final ControlList.KeyEntry this$1;

            {
               super(var2, var3, var4, var5, var6, var7);
               this.this$1 = var1;
               this.val$this$0 = var8;
               this.val$key = var9;
               this.val$name = var10;
            }

            protected MutableComponent createNarrationMessage() {
               return this.val$key.isUnbound() ? new TranslatableComponent("narrator.controls.unbound", new Object[]{this.val$name}) : new TranslatableComponent("narrator.controls.bound", new Object[]{this.val$name, super.createNarrationMessage()});
            }
         };
         this.resetButton = new Button(0, 0, 50, 20, new TranslatableComponent("controls.reset"), (var2x) -> {
            ControlList.this.minecraft.options.setKey(var2, var2.getDefaultKey());
            KeyMapping.resetMapping();
         }, ControlList.this, var3) {
            // $FF: synthetic field
            final ControlList val$this$0;
            // $FF: synthetic field
            final Component val$name;
            // $FF: synthetic field
            final ControlList.KeyEntry this$1;

            {
               super(var2, var3, var4, var5, var6, var7);
               this.this$1 = var1;
               this.val$this$0 = var8;
               this.val$name = var9;
            }

            protected MutableComponent createNarrationMessage() {
               return new TranslatableComponent("narrator.controls.reset", new Object[]{this.val$name});
            }
         };
      }

      public void render(PoseStack var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         boolean var11 = ControlList.this.controlsScreen.selectedKey == this.key;
         Font var10000 = ControlList.this.minecraft.font;
         Component var10002 = this.name;
         float var10003 = (float)(var4 + 90 - ControlList.this.maxNameWidth);
         int var10004 = var3 + var6 / 2;
         Objects.requireNonNull(ControlList.this.minecraft.font);
         var10000.draw(var1, var10002, var10003, (float)(var10004 - 9 / 2), 16777215);
         this.resetButton.x = var4 + 190;
         this.resetButton.y = var3;
         this.resetButton.active = !this.key.isDefault();
         this.resetButton.render(var1, var7, var8, var10);
         this.changeButton.x = var4 + 105;
         this.changeButton.y = var3;
         this.changeButton.setMessage(this.key.getTranslatedKeyMessage());
         boolean var12 = false;
         if (!this.key.isUnbound()) {
            KeyMapping[] var13 = ControlList.this.minecraft.options.keyMappings;
            int var14 = var13.length;

            for(int var15 = 0; var15 < var14; ++var15) {
               KeyMapping var16 = var13[var15];
               if (var16 != this.key && this.key.same(var16)) {
                  var12 = true;
                  break;
               }
            }
         }

         if (var11) {
            this.changeButton.setMessage((new TextComponent("> ")).append(this.changeButton.getMessage().copy().withStyle(ChatFormatting.YELLOW)).append(" <").withStyle(ChatFormatting.YELLOW));
         } else if (var12) {
            this.changeButton.setMessage(this.changeButton.getMessage().copy().withStyle(ChatFormatting.RED));
         }

         this.changeButton.render(var1, var7, var8, var10);
      }

      public List<? extends GuiEventListener> children() {
         return ImmutableList.of(this.changeButton, this.resetButton);
      }

      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(this.changeButton, this.resetButton);
      }

      public boolean mouseClicked(double var1, double var3, int var5) {
         if (this.changeButton.mouseClicked(var1, var3, var5)) {
            return true;
         } else {
            return this.resetButton.mouseClicked(var1, var3, var5);
         }
      }

      public boolean mouseReleased(double var1, double var3, int var5) {
         return this.changeButton.mouseReleased(var1, var3, var5) || this.resetButton.mouseReleased(var1, var3, var5);
      }
   }

   public abstract static class Entry extends ContainerObjectSelectionList.Entry<ControlList.Entry> {
   }
}
