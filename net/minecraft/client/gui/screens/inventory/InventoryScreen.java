package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;

public class InventoryScreen extends EffectRenderingInventoryScreen<InventoryMenu> implements RecipeUpdateListener {
   private static final ResourceLocation RECIPE_BUTTON_LOCATION = new ResourceLocation("textures/gui/recipe_button.png");
   private float xMouse;
   private float yMouse;
   private final RecipeBookComponent recipeBookComponent = new RecipeBookComponent();
   private boolean recipeBookComponentInitialized;
   private boolean widthTooNarrow;
   private boolean buttonClicked;

   public InventoryScreen(Player var1) {
      super(var1.inventoryMenu, var1.getInventory(), new TranslatableComponent("container.crafting"));
      this.passEvents = true;
      this.titleLabelX = 97;
   }

   public void tick() {
      if (this.minecraft.gameMode.hasInfiniteItems()) {
         this.minecraft.setScreen(new CreativeModeInventoryScreen(this.minecraft.player));
      } else {
         this.recipeBookComponent.tick();
      }
   }

   protected void init() {
      if (this.minecraft.gameMode.hasInfiniteItems()) {
         this.minecraft.setScreen(new CreativeModeInventoryScreen(this.minecraft.player));
      } else {
         super.init();
         this.widthTooNarrow = this.width < 379;
         this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, (RecipeBookMenu)this.menu);
         this.recipeBookComponentInitialized = true;
         this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
         this.addRenderableWidget(new ImageButton(this.leftPos + 104, this.height / 2 - 22, 20, 18, 0, 0, 19, RECIPE_BUTTON_LOCATION, (var1) -> {
            this.recipeBookComponent.toggleVisibility();
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            ((ImageButton)var1).setPosition(this.leftPos + 104, this.height / 2 - 22);
            this.buttonClicked = true;
         }));
         this.addWidget(this.recipeBookComponent);
         this.setInitialFocus(this.recipeBookComponent);
      }
   }

   protected void renderLabels(PoseStack var1, int var2, int var3) {
      this.font.draw(var1, this.title, (float)this.titleLabelX, (float)this.titleLabelY, 4210752);
   }

   public void render(PoseStack var1, int var2, int var3, float var4) {
      this.renderBackground(var1);
      this.doRenderEffects = !this.recipeBookComponent.isVisible();
      if (this.recipeBookComponent.isVisible() && this.widthTooNarrow) {
         this.renderBg(var1, var4, var2, var3);
         this.recipeBookComponent.render(var1, var2, var3, var4);
      } else {
         this.recipeBookComponent.render(var1, var2, var3, var4);
         super.render(var1, var2, var3, var4);
         this.recipeBookComponent.renderGhostRecipe(var1, this.leftPos, this.topPos, false, var4);
      }

      this.renderTooltip(var1, var2, var3);
      this.recipeBookComponent.renderTooltip(var1, this.leftPos, this.topPos, var2, var3);
      this.xMouse = (float)var2;
      this.yMouse = (float)var3;
   }

   protected void renderBg(PoseStack var1, float var2, int var3, int var4) {
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, INVENTORY_LOCATION);
      int var5 = this.leftPos;
      int var6 = this.topPos;
      this.blit(var1, var5, var6, 0, 0, this.imageWidth, this.imageHeight);
      renderEntityInInventory(var5 + 51, var6 + 75, 30, (float)(var5 + 51) - this.xMouse, (float)(var6 + 75 - 50) - this.yMouse, this.minecraft.player);
   }

   public static void renderEntityInInventory(int var0, int var1, int var2, float var3, float var4, LivingEntity var5) {
      float var6 = (float)Math.atan((double)(var3 / 40.0F));
      float var7 = (float)Math.atan((double)(var4 / 40.0F));
      PoseStack var8 = RenderSystem.getModelViewStack();
      var8.pushPose();
      var8.translate((double)var0, (double)var1, 1050.0D);
      var8.scale(1.0F, 1.0F, -1.0F);
      RenderSystem.applyModelViewMatrix();
      PoseStack var9 = new PoseStack();
      var9.translate(0.0D, 0.0D, 1000.0D);
      var9.scale((float)var2, (float)var2, (float)var2);
      Quaternion var10 = Vector3f.ZP.rotationDegrees(180.0F);
      Quaternion var11 = Vector3f.XP.rotationDegrees(var7 * 20.0F);
      var10.mul(var11);
      var9.mulPose(var10);
      float var12 = var5.yBodyRot;
      float var13 = var5.getYRot();
      float var14 = var5.getXRot();
      float var15 = var5.yHeadRotO;
      float var16 = var5.yHeadRot;
      var5.yBodyRot = 180.0F + var6 * 20.0F;
      var5.setYRot(180.0F + var6 * 40.0F);
      var5.setXRot(-var7 * 20.0F);
      var5.yHeadRot = var5.getYRot();
      var5.yHeadRotO = var5.getYRot();
      Lighting.setupForEntityInInventory();
      EntityRenderDispatcher var17 = Minecraft.getInstance().getEntityRenderDispatcher();
      var11.conj();
      var17.overrideCameraOrientation(var11);
      var17.setRenderShadow(false);
      MultiBufferSource.BufferSource var18 = Minecraft.getInstance().renderBuffers().bufferSource();
      RenderSystem.runAsFancy(() -> {
         var17.render(var5, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, var9, var18, 15728880);
      });
      var18.endBatch();
      var17.setRenderShadow(true);
      var5.yBodyRot = var12;
      var5.setYRot(var13);
      var5.setXRot(var14);
      var5.yHeadRotO = var15;
      var5.yHeadRot = var16;
      var8.popPose();
      RenderSystem.applyModelViewMatrix();
      Lighting.setupFor3DItems();
   }

   protected boolean isHovering(int var1, int var2, int var3, int var4, double var5, double var7) {
      return (!this.widthTooNarrow || !this.recipeBookComponent.isVisible()) && super.isHovering(var1, var2, var3, var4, var5, var7);
   }

   public boolean mouseClicked(double var1, double var3, int var5) {
      if (this.recipeBookComponent.mouseClicked(var1, var3, var5)) {
         this.setFocused(this.recipeBookComponent);
         return true;
      } else {
         return this.widthTooNarrow && this.recipeBookComponent.isVisible() ? false : super.mouseClicked(var1, var3, var5);
      }
   }

   public boolean mouseReleased(double var1, double var3, int var5) {
      if (this.buttonClicked) {
         this.buttonClicked = false;
         return true;
      } else {
         return super.mouseReleased(var1, var3, var5);
      }
   }

   protected boolean hasClickedOutside(double var1, double var3, int var5, int var6, int var7) {
      boolean var8 = var1 < (double)var5 || var3 < (double)var6 || var1 >= (double)(var5 + this.imageWidth) || var3 >= (double)(var6 + this.imageHeight);
      return this.recipeBookComponent.hasClickedOutside(var1, var3, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, var7) && var8;
   }

   protected void slotClicked(Slot var1, int var2, int var3, ClickType var4) {
      super.slotClicked(var1, var2, var3, var4);
      this.recipeBookComponent.slotClicked(var1);
   }

   public void recipesUpdated() {
      this.recipeBookComponent.recipesUpdated();
   }

   public void removed() {
      if (this.recipeBookComponentInitialized) {
         this.recipeBookComponent.removed();
      }

      super.removed();
   }

   public RecipeBookComponent getRecipeBookComponent() {
      return this.recipeBookComponent;
   }
}
