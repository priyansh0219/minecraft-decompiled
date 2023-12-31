package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.GameRules;

public class EditGameRulesScreen extends Screen {
   private final Consumer<Optional<GameRules>> exitCallback;
   private EditGameRulesScreen.RuleList rules;
   private final Set<EditGameRulesScreen.RuleEntry> invalidEntries = Sets.newHashSet();
   private Button doneButton;
   @Nullable
   private List<FormattedCharSequence> tooltip;
   private final GameRules gameRules;

   public EditGameRulesScreen(GameRules var1, Consumer<Optional<GameRules>> var2) {
      super(new TranslatableComponent("editGamerule.title"));
      this.gameRules = var1;
      this.exitCallback = var2;
   }

   protected void init() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      super.init();
      this.rules = new EditGameRulesScreen.RuleList(this.gameRules);
      this.addWidget(this.rules);
      this.addRenderableWidget(new Button(this.width / 2 - 155 + 160, this.height - 29, 150, 20, CommonComponents.GUI_CANCEL, (var1) -> {
         this.exitCallback.accept(Optional.empty());
      }));
      this.doneButton = (Button)this.addRenderableWidget(new Button(this.width / 2 - 155, this.height - 29, 150, 20, CommonComponents.GUI_DONE, (var1) -> {
         this.exitCallback.accept(Optional.of(this.gameRules));
      }));
   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   public void onClose() {
      this.exitCallback.accept(Optional.empty());
   }

   public void render(PoseStack var1, int var2, int var3, float var4) {
      this.tooltip = null;
      this.rules.render(var1, var2, var3, var4);
      drawCenteredString(var1, this.font, this.title, this.width / 2, 20, 16777215);
      super.render(var1, var2, var3, var4);
      if (this.tooltip != null) {
         this.renderTooltip(var1, this.tooltip, var2, var3);
      }

   }

   void setTooltip(@Nullable List<FormattedCharSequence> var1) {
      this.tooltip = var1;
   }

   private void updateDoneButton() {
      this.doneButton.active = this.invalidEntries.isEmpty();
   }

   void markInvalid(EditGameRulesScreen.RuleEntry var1) {
      this.invalidEntries.add(var1);
      this.updateDoneButton();
   }

   void clearInvalid(EditGameRulesScreen.RuleEntry var1) {
      this.invalidEntries.remove(var1);
      this.updateDoneButton();
   }

   // $FF: synthetic method
   static Font access$700(EditGameRulesScreen var0) {
      return var0.font;
   }

   public class RuleList extends ContainerObjectSelectionList<EditGameRulesScreen.RuleEntry> {
      public RuleList(GameRules var2) {
         super(EditGameRulesScreen.this.minecraft, EditGameRulesScreen.this.width, EditGameRulesScreen.this.height, 43, EditGameRulesScreen.this.height - 32, 24);
         HashMap var3 = Maps.newHashMap();
         GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor(EditGameRulesScreen.this, var2, var3) {
            // $FF: synthetic field
            final EditGameRulesScreen val$this$0;
            // $FF: synthetic field
            final GameRules val$gameRules;
            // $FF: synthetic field
            final Map val$entries;
            // $FF: synthetic field
            final EditGameRulesScreen.RuleList this$1;

            {
               this.this$1 = var1;
               this.val$this$0 = var2;
               this.val$gameRules = var3;
               this.val$entries = var4;
            }

            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> var1, GameRules.Type<GameRules.BooleanValue> var2) {
               this.addEntry(var1, (var1, var2, var3, var4) -> {
                  return this.this$1.this$0.new BooleanRuleEntry(var1, var2, var3, var4);
               });
            }

            public void visitInteger(GameRules.Key<GameRules.IntegerValue> var1, GameRules.Type<GameRules.IntegerValue> var2) {
               this.addEntry(var1, (var1, var2, var3, var4) -> {
                  return this.this$1.this$0.new IntegerRuleEntry(var1, var2, var3, var4);
               });
            }

            private <T extends GameRules.Value<T>> void addEntry(GameRules.Key<T> var1, EditGameRulesScreen.EntryFactory<T> var2) {
               TranslatableComponent var3 = new TranslatableComponent(var1.getDescriptionId());
               MutableComponent var4 = (new TextComponent(var1.getId())).withStyle(ChatFormatting.YELLOW);
               GameRules.Value var5 = this.val$gameRules.getRule(var1);
               String var6 = var5.serialize();
               MutableComponent var7 = (new TranslatableComponent("editGamerule.default", new Object[]{new TextComponent(var6)})).withStyle(ChatFormatting.GRAY);
               String var8 = var1.getDescriptionId() + ".description";
               ImmutableList var9;
               String var10;
               if (I18n.exists(var8)) {
                  Builder var11 = ImmutableList.builder().add(var4.getVisualOrderText());
                  TranslatableComponent var12 = new TranslatableComponent(var8);
                  List var10000 = EditGameRulesScreen.access$700(this.this$1.this$0).split(var12, 150);
                  Objects.requireNonNull(var11);
                  var10000.forEach(var11::add);
                  var9 = var11.add(var7.getVisualOrderText()).build();
                  String var13 = var12.getString();
                  var10 = var13 + "\n" + var7.getString();
               } else {
                  var9 = ImmutableList.of(var4.getVisualOrderText(), var7.getVisualOrderText());
                  var10 = var7.getString();
               }

               ((Map)this.val$entries.computeIfAbsent(var1.getCategory(), (var0) -> {
                  return Maps.newHashMap();
               })).put(var1, var2.create(var3, var9, var10, var5));
            }

            // $FF: synthetic method
            private static Map lambda$addEntry$2(GameRules.Category var0) {
               return Maps.newHashMap();
            }

            // $FF: synthetic method
            private EditGameRulesScreen.RuleEntry lambda$visitInteger$1(Component var1, List var2, String var3, GameRules.IntegerValue var4) {
               return this.this$1.this$0.new IntegerRuleEntry(var1, var2, var3, var4);
            }

            // $FF: synthetic method
            private EditGameRulesScreen.RuleEntry lambda$visitBoolean$0(Component var1, List var2, String var3, GameRules.BooleanValue var4) {
               return this.this$1.this$0.new BooleanRuleEntry(var1, var2, var3, var4);
            }
         });
         var3.entrySet().stream().sorted(Entry.comparingByKey()).forEach((var1x) -> {
            this.addEntry(EditGameRulesScreen.this.new CategoryRuleEntry((new TranslatableComponent(((GameRules.Category)var1x.getKey()).getDescriptionId())).withStyle(new ChatFormatting[]{ChatFormatting.BOLD, ChatFormatting.YELLOW})));
            ((Map)var1x.getValue()).entrySet().stream().sorted(Entry.comparingByKey(Comparator.comparing(GameRules.Key::getId))).forEach((var1) -> {
               this.addEntry((EditGameRulesScreen.RuleEntry)var1.getValue());
            });
         });
      }

      public void render(PoseStack var1, int var2, int var3, float var4) {
         super.render(var1, var2, var3, var4);
         EditGameRulesScreen.RuleEntry var5 = (EditGameRulesScreen.RuleEntry)this.getHovered();
         if (var5 != null) {
            EditGameRulesScreen.this.setTooltip(var5.tooltip);
         }

      }
   }

   public class IntegerRuleEntry extends EditGameRulesScreen.GameRuleEntry {
      private final EditBox input;

      public IntegerRuleEntry(Component var2, List<FormattedCharSequence> var3, String var4, GameRules.IntegerValue var5) {
         super(var3, var2);
         this.input = new EditBox(EditGameRulesScreen.this.minecraft.font, 10, 5, 42, 20, var2.copy().append("\n").append(var4).append("\n"));
         this.input.setValue(Integer.toString(var5.get()));
         this.input.setResponder((var2x) -> {
            if (var5.tryDeserialize(var2x)) {
               this.input.setTextColor(14737632);
               EditGameRulesScreen.this.clearInvalid(this);
            } else {
               this.input.setTextColor(16711680);
               EditGameRulesScreen.this.markInvalid(this);
            }

         });
         this.children.add(this.input);
      }

      public void render(PoseStack var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         this.renderLabel(var1, var3, var4);
         this.input.x = var4 + var5 - 44;
         this.input.y = var3;
         this.input.render(var1, var7, var8, var10);
      }
   }

   public class BooleanRuleEntry extends EditGameRulesScreen.GameRuleEntry {
      private final CycleButton<Boolean> checkbox;

      public BooleanRuleEntry(Component var2, List<FormattedCharSequence> var3, String var4, GameRules.BooleanValue var5) {
         super(var3, var2);
         this.checkbox = CycleButton.onOffBuilder(var5.get()).displayOnlyValue().withCustomNarration((var1x) -> {
            return var1x.createDefaultNarrationMessage().append("\n").append(var4);
         }).create(10, 5, 44, 20, var2, (var1x, var2x) -> {
            var5.set(var2x, (MinecraftServer)null);
         });
         this.children.add(this.checkbox);
      }

      public void render(PoseStack var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         this.renderLabel(var1, var3, var4);
         this.checkbox.x = var4 + var5 - 45;
         this.checkbox.y = var3;
         this.checkbox.render(var1, var7, var8, var10);
      }
   }

   public abstract class GameRuleEntry extends EditGameRulesScreen.RuleEntry {
      private final List<FormattedCharSequence> label;
      protected final List<AbstractWidget> children = Lists.newArrayList();

      public GameRuleEntry(@Nullable List<FormattedCharSequence> var2, Component var3) {
         super(var2);
         this.label = EditGameRulesScreen.this.minecraft.font.split(var3, 175);
      }

      public List<? extends GuiEventListener> children() {
         return this.children;
      }

      public List<? extends NarratableEntry> narratables() {
         return this.children;
      }

      protected void renderLabel(PoseStack var1, int var2, int var3) {
         if (this.label.size() == 1) {
            EditGameRulesScreen.this.minecraft.font.draw(var1, (FormattedCharSequence)this.label.get(0), (float)var3, (float)(var2 + 5), 16777215);
         } else if (this.label.size() >= 2) {
            EditGameRulesScreen.this.minecraft.font.draw(var1, (FormattedCharSequence)this.label.get(0), (float)var3, (float)var2, 16777215);
            EditGameRulesScreen.this.minecraft.font.draw(var1, (FormattedCharSequence)this.label.get(1), (float)var3, (float)(var2 + 10), 16777215);
         }

      }
   }

   @FunctionalInterface
   interface EntryFactory<T extends GameRules.Value<T>> {
      EditGameRulesScreen.RuleEntry create(Component var1, List<FormattedCharSequence> var2, String var3, T var4);
   }

   public class CategoryRuleEntry extends EditGameRulesScreen.RuleEntry {
      final Component label;

      public CategoryRuleEntry(Component var2) {
         super((List)null);
         this.label = var2;
      }

      public void render(PoseStack var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         GuiComponent.drawCenteredString(var1, EditGameRulesScreen.this.minecraft.font, this.label, var4 + var5 / 2, var3 + 5, 16777215);
      }

      public List<? extends GuiEventListener> children() {
         return ImmutableList.of();
      }

      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(new NarratableEntry() {
            // $FF: synthetic field
            final EditGameRulesScreen.CategoryRuleEntry this$1;

            {
               this.this$1 = var1;
            }

            public NarratableEntry.NarrationPriority narrationPriority() {
               return NarratableEntry.NarrationPriority.HOVERED;
            }

            public void updateNarration(NarrationElementOutput var1) {
               var1.add(NarratedElementType.TITLE, this.this$1.label);
            }
         });
      }
   }

   public abstract class RuleEntry extends ContainerObjectSelectionList.Entry<EditGameRulesScreen.RuleEntry> {
      @Nullable
      final List<FormattedCharSequence> tooltip;

      public RuleEntry(@Nullable List<FormattedCharSequence> var2) {
         this.tooltip = var2;
      }
   }
}
