package net.minecraft.client.renderer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class ItemInHandRenderer {
   private static final RenderType MAP_BACKGROUND = RenderType.text(new ResourceLocation("textures/map/map_background.png"));
   private static final RenderType MAP_BACKGROUND_CHECKERBOARD = RenderType.text(new ResourceLocation("textures/map/map_background_checkerboard.png"));
   private static final float ITEM_SWING_X_POS_SCALE = -0.4F;
   private static final float ITEM_SWING_Y_POS_SCALE = 0.2F;
   private static final float ITEM_SWING_Z_POS_SCALE = -0.2F;
   private static final float ITEM_HEIGHT_SCALE = -0.6F;
   private static final float ITEM_POS_X = 0.56F;
   private static final float ITEM_POS_Y = -0.52F;
   private static final float ITEM_POS_Z = -0.72F;
   private static final float ITEM_PRESWING_ROT_Y = 45.0F;
   private static final float ITEM_SWING_X_ROT_AMOUNT = -80.0F;
   private static final float ITEM_SWING_Y_ROT_AMOUNT = -20.0F;
   private static final float ITEM_SWING_Z_ROT_AMOUNT = -20.0F;
   private static final float EAT_JIGGLE_X_ROT_AMOUNT = 10.0F;
   private static final float EAT_JIGGLE_Y_ROT_AMOUNT = 90.0F;
   private static final float EAT_JIGGLE_Z_ROT_AMOUNT = 30.0F;
   private static final float EAT_JIGGLE_X_POS_SCALE = 0.6F;
   private static final float EAT_JIGGLE_Y_POS_SCALE = -0.5F;
   private static final float EAT_JIGGLE_Z_POS_SCALE = 0.0F;
   private static final double EAT_JIGGLE_EXPONENT = 27.0D;
   private static final float EAT_EXTRA_JIGGLE_CUTOFF = 0.8F;
   private static final float EAT_EXTRA_JIGGLE_SCALE = 0.1F;
   private static final float ARM_SWING_X_POS_SCALE = -0.3F;
   private static final float ARM_SWING_Y_POS_SCALE = 0.4F;
   private static final float ARM_SWING_Z_POS_SCALE = -0.4F;
   private static final float ARM_SWING_Y_ROT_AMOUNT = 70.0F;
   private static final float ARM_SWING_Z_ROT_AMOUNT = -20.0F;
   private static final float ARM_HEIGHT_SCALE = -0.6F;
   private static final float ARM_POS_SCALE = 0.8F;
   private static final float ARM_POS_X = 0.8F;
   private static final float ARM_POS_Y = -0.75F;
   private static final float ARM_POS_Z = -0.9F;
   private static final float ARM_PRESWING_ROT_Y = 45.0F;
   private static final float ARM_PREROTATION_X_OFFSET = -1.0F;
   private static final float ARM_PREROTATION_Y_OFFSET = 3.6F;
   private static final float ARM_PREROTATION_Z_OFFSET = 3.5F;
   private static final float ARM_POSTROTATION_X_OFFSET = 5.6F;
   private static final int ARM_ROT_X = 200;
   private static final int ARM_ROT_Y = -135;
   private static final int ARM_ROT_Z = 120;
   private static final float MAP_SWING_X_POS_SCALE = -0.4F;
   private static final float MAP_SWING_Z_POS_SCALE = -0.2F;
   private static final float MAP_HANDS_POS_X = 0.0F;
   private static final float MAP_HANDS_POS_Y = 0.04F;
   private static final float MAP_HANDS_POS_Z = -0.72F;
   private static final float MAP_HANDS_HEIGHT_SCALE = -1.2F;
   private static final float MAP_HANDS_TILT_SCALE = -0.5F;
   private static final float MAP_PLAYER_PITCH_SCALE = 45.0F;
   private static final float MAP_HANDS_Z_ROT_AMOUNT = -85.0F;
   private static final float MAPHAND_X_ROT_AMOUNT = 45.0F;
   private static final float MAPHAND_Y_ROT_AMOUNT = 92.0F;
   private static final float MAPHAND_Z_ROT_AMOUNT = -41.0F;
   private static final float MAP_HAND_X_POS = 0.3F;
   private static final float MAP_HAND_Y_POS = -1.1F;
   private static final float MAP_HAND_Z_POS = 0.45F;
   private static final float MAP_SWING_X_ROT_AMOUNT = 20.0F;
   private static final float MAP_PRE_ROT_SCALE = 0.38F;
   private static final float MAP_GLOBAL_X_POS = -0.5F;
   private static final float MAP_GLOBAL_Y_POS = -0.5F;
   private static final float MAP_GLOBAL_Z_POS = 0.0F;
   private static final float MAP_FINAL_SCALE = 0.0078125F;
   private static final int MAP_BORDER = 7;
   private static final int MAP_HEIGHT = 128;
   private static final int MAP_WIDTH = 128;
   private static final float BOW_CHARGE_X_POS_SCALE = 0.0F;
   private static final float BOW_CHARGE_Y_POS_SCALE = 0.0F;
   private static final float BOW_CHARGE_Z_POS_SCALE = 0.04F;
   private static final float BOW_CHARGE_SHAKE_X_SCALE = 0.0F;
   private static final float BOW_CHARGE_SHAKE_Y_SCALE = 0.004F;
   private static final float BOW_CHARGE_SHAKE_Z_SCALE = 0.0F;
   private static final float BOW_CHARGE_Z_SCALE = 0.2F;
   private static final float BOW_MIN_SHAKE_CHARGE = 0.1F;
   private final Minecraft minecraft;
   private ItemStack mainHandItem;
   private ItemStack offHandItem;
   private float mainHandHeight;
   private float oMainHandHeight;
   private float offHandHeight;
   private float oOffHandHeight;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final ItemRenderer itemRenderer;

   public ItemInHandRenderer(Minecraft var1) {
      this.mainHandItem = ItemStack.EMPTY;
      this.offHandItem = ItemStack.EMPTY;
      this.minecraft = var1;
      this.entityRenderDispatcher = var1.getEntityRenderDispatcher();
      this.itemRenderer = var1.getItemRenderer();
   }

   public void renderItem(LivingEntity var1, ItemStack var2, ItemTransforms.TransformType var3, boolean var4, PoseStack var5, MultiBufferSource var6, int var7) {
      if (!var2.isEmpty()) {
         this.itemRenderer.renderStatic(var1, var2, var3, var4, var5, var6, var1.level, var7, OverlayTexture.NO_OVERLAY, var1.getId() + var3.ordinal());
      }
   }

   private float calculateMapTilt(float var1) {
      float var2 = 1.0F - var1 / 45.0F + 0.1F;
      var2 = Mth.clamp(var2, 0.0F, 1.0F);
      var2 = -Mth.cos(var2 * 3.1415927F) * 0.5F + 0.5F;
      return var2;
   }

   private void renderMapHand(PoseStack var1, MultiBufferSource var2, int var3, HumanoidArm var4) {
      RenderSystem.setShaderTexture(0, this.minecraft.player.getSkinTextureLocation());
      PlayerRenderer var5 = (PlayerRenderer)this.entityRenderDispatcher.getRenderer(this.minecraft.player);
      var1.pushPose();
      float var6 = var4 == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      var1.mulPose(Vector3f.YP.rotationDegrees(92.0F));
      var1.mulPose(Vector3f.XP.rotationDegrees(45.0F));
      var1.mulPose(Vector3f.ZP.rotationDegrees(var6 * -41.0F));
      var1.translate((double)(var6 * 0.3F), -1.100000023841858D, 0.44999998807907104D);
      if (var4 == HumanoidArm.RIGHT) {
         var5.renderRightHand(var1, var2, var3, this.minecraft.player);
      } else {
         var5.renderLeftHand(var1, var2, var3, this.minecraft.player);
      }

      var1.popPose();
   }

   private void renderOneHandedMap(PoseStack var1, MultiBufferSource var2, int var3, float var4, HumanoidArm var5, float var6, ItemStack var7) {
      float var8 = var5 == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      var1.translate((double)(var8 * 0.125F), -0.125D, 0.0D);
      if (!this.minecraft.player.isInvisible()) {
         var1.pushPose();
         var1.mulPose(Vector3f.ZP.rotationDegrees(var8 * 10.0F));
         this.renderPlayerArm(var1, var2, var3, var4, var6, var5);
         var1.popPose();
      }

      var1.pushPose();
      var1.translate((double)(var8 * 0.51F), (double)(-0.08F + var4 * -1.2F), -0.75D);
      float var9 = Mth.sqrt(var6);
      float var10 = Mth.sin(var9 * 3.1415927F);
      float var11 = -0.5F * var10;
      float var12 = 0.4F * Mth.sin(var9 * 6.2831855F);
      float var13 = -0.3F * Mth.sin(var6 * 3.1415927F);
      var1.translate((double)(var8 * var11), (double)(var12 - 0.3F * var10), (double)var13);
      var1.mulPose(Vector3f.XP.rotationDegrees(var10 * -45.0F));
      var1.mulPose(Vector3f.YP.rotationDegrees(var8 * var10 * -30.0F));
      this.renderMap(var1, var2, var3, var7);
      var1.popPose();
   }

   private void renderTwoHandedMap(PoseStack var1, MultiBufferSource var2, int var3, float var4, float var5, float var6) {
      float var7 = Mth.sqrt(var6);
      float var8 = -0.2F * Mth.sin(var6 * 3.1415927F);
      float var9 = -0.4F * Mth.sin(var7 * 3.1415927F);
      var1.translate(0.0D, (double)(-var8 / 2.0F), (double)var9);
      float var10 = this.calculateMapTilt(var4);
      var1.translate(0.0D, (double)(0.04F + var5 * -1.2F + var10 * -0.5F), -0.7200000286102295D);
      var1.mulPose(Vector3f.XP.rotationDegrees(var10 * -85.0F));
      if (!this.minecraft.player.isInvisible()) {
         var1.pushPose();
         var1.mulPose(Vector3f.YP.rotationDegrees(90.0F));
         this.renderMapHand(var1, var2, var3, HumanoidArm.RIGHT);
         this.renderMapHand(var1, var2, var3, HumanoidArm.LEFT);
         var1.popPose();
      }

      float var11 = Mth.sin(var7 * 3.1415927F);
      var1.mulPose(Vector3f.XP.rotationDegrees(var11 * 20.0F));
      var1.scale(2.0F, 2.0F, 2.0F);
      this.renderMap(var1, var2, var3, this.mainHandItem);
   }

   private void renderMap(PoseStack var1, MultiBufferSource var2, int var3, ItemStack var4) {
      var1.mulPose(Vector3f.YP.rotationDegrees(180.0F));
      var1.mulPose(Vector3f.ZP.rotationDegrees(180.0F));
      var1.scale(0.38F, 0.38F, 0.38F);
      var1.translate(-0.5D, -0.5D, 0.0D);
      var1.scale(0.0078125F, 0.0078125F, 0.0078125F);
      Integer var5 = MapItem.getMapId(var4);
      MapItemSavedData var6 = MapItem.getSavedData((Integer)var5, this.minecraft.level);
      VertexConsumer var7 = var2.getBuffer(var6 == null ? MAP_BACKGROUND : MAP_BACKGROUND_CHECKERBOARD);
      Matrix4f var8 = var1.last().pose();
      var7.vertex(var8, -7.0F, 135.0F, 0.0F).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(var3).endVertex();
      var7.vertex(var8, 135.0F, 135.0F, 0.0F).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(var3).endVertex();
      var7.vertex(var8, 135.0F, -7.0F, 0.0F).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(var3).endVertex();
      var7.vertex(var8, -7.0F, -7.0F, 0.0F).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(var3).endVertex();
      if (var6 != null) {
         this.minecraft.gameRenderer.getMapRenderer().render(var1, var2, var5, var6, false, var3);
      }

   }

   private void renderPlayerArm(PoseStack var1, MultiBufferSource var2, int var3, float var4, float var5, HumanoidArm var6) {
      boolean var7 = var6 != HumanoidArm.LEFT;
      float var8 = var7 ? 1.0F : -1.0F;
      float var9 = Mth.sqrt(var5);
      float var10 = -0.3F * Mth.sin(var9 * 3.1415927F);
      float var11 = 0.4F * Mth.sin(var9 * 6.2831855F);
      float var12 = -0.4F * Mth.sin(var5 * 3.1415927F);
      var1.translate((double)(var8 * (var10 + 0.64000005F)), (double)(var11 + -0.6F + var4 * -0.6F), (double)(var12 + -0.71999997F));
      var1.mulPose(Vector3f.YP.rotationDegrees(var8 * 45.0F));
      float var13 = Mth.sin(var5 * var5 * 3.1415927F);
      float var14 = Mth.sin(var9 * 3.1415927F);
      var1.mulPose(Vector3f.YP.rotationDegrees(var8 * var14 * 70.0F));
      var1.mulPose(Vector3f.ZP.rotationDegrees(var8 * var13 * -20.0F));
      LocalPlayer var15 = this.minecraft.player;
      RenderSystem.setShaderTexture(0, var15.getSkinTextureLocation());
      var1.translate((double)(var8 * -1.0F), 3.5999999046325684D, 3.5D);
      var1.mulPose(Vector3f.ZP.rotationDegrees(var8 * 120.0F));
      var1.mulPose(Vector3f.XP.rotationDegrees(200.0F));
      var1.mulPose(Vector3f.YP.rotationDegrees(var8 * -135.0F));
      var1.translate((double)(var8 * 5.6F), 0.0D, 0.0D);
      PlayerRenderer var16 = (PlayerRenderer)this.entityRenderDispatcher.getRenderer(var15);
      if (var7) {
         var16.renderRightHand(var1, var2, var3, var15);
      } else {
         var16.renderLeftHand(var1, var2, var3, var15);
      }

   }

   private void applyEatTransform(PoseStack var1, float var2, HumanoidArm var3, ItemStack var4) {
      float var5 = (float)this.minecraft.player.getUseItemRemainingTicks() - var2 + 1.0F;
      float var6 = var5 / (float)var4.getUseDuration();
      float var7;
      if (var6 < 0.8F) {
         var7 = Mth.abs(Mth.cos(var5 / 4.0F * 3.1415927F) * 0.1F);
         var1.translate(0.0D, (double)var7, 0.0D);
      }

      var7 = 1.0F - (float)Math.pow((double)var6, 27.0D);
      int var8 = var3 == HumanoidArm.RIGHT ? 1 : -1;
      var1.translate((double)(var7 * 0.6F * (float)var8), (double)(var7 * -0.5F), (double)(var7 * 0.0F));
      var1.mulPose(Vector3f.YP.rotationDegrees((float)var8 * var7 * 90.0F));
      var1.mulPose(Vector3f.XP.rotationDegrees(var7 * 10.0F));
      var1.mulPose(Vector3f.ZP.rotationDegrees((float)var8 * var7 * 30.0F));
   }

   private void applyItemArmAttackTransform(PoseStack var1, HumanoidArm var2, float var3) {
      int var4 = var2 == HumanoidArm.RIGHT ? 1 : -1;
      float var5 = Mth.sin(var3 * var3 * 3.1415927F);
      var1.mulPose(Vector3f.YP.rotationDegrees((float)var4 * (45.0F + var5 * -20.0F)));
      float var6 = Mth.sin(Mth.sqrt(var3) * 3.1415927F);
      var1.mulPose(Vector3f.ZP.rotationDegrees((float)var4 * var6 * -20.0F));
      var1.mulPose(Vector3f.XP.rotationDegrees(var6 * -80.0F));
      var1.mulPose(Vector3f.YP.rotationDegrees((float)var4 * -45.0F));
   }

   private void applyItemArmTransform(PoseStack var1, HumanoidArm var2, float var3) {
      int var4 = var2 == HumanoidArm.RIGHT ? 1 : -1;
      var1.translate((double)((float)var4 * 0.56F), (double)(-0.52F + var3 * -0.6F), -0.7200000286102295D);
   }

   public void renderHandsWithItems(float var1, PoseStack var2, MultiBufferSource.BufferSource var3, LocalPlayer var4, int var5) {
      float var6 = var4.getAttackAnim(var1);
      InteractionHand var7 = (InteractionHand)MoreObjects.firstNonNull(var4.swingingArm, InteractionHand.MAIN_HAND);
      float var8 = Mth.lerp(var1, var4.xRotO, var4.getXRot());
      ItemInHandRenderer.HandRenderSelection var9 = evaluateWhichHandsToRender(var4);
      float var10 = Mth.lerp(var1, var4.xBobO, var4.xBob);
      float var11 = Mth.lerp(var1, var4.yBobO, var4.yBob);
      var2.mulPose(Vector3f.XP.rotationDegrees((var4.getViewXRot(var1) - var10) * 0.1F));
      var2.mulPose(Vector3f.YP.rotationDegrees((var4.getViewYRot(var1) - var11) * 0.1F));
      float var12;
      float var13;
      if (var9.renderMainHand) {
         var12 = var7 == InteractionHand.MAIN_HAND ? var6 : 0.0F;
         var13 = 1.0F - Mth.lerp(var1, this.oMainHandHeight, this.mainHandHeight);
         this.renderArmWithItem(var4, var1, var8, InteractionHand.MAIN_HAND, var12, this.mainHandItem, var13, var2, var3, var5);
      }

      if (var9.renderOffHand) {
         var12 = var7 == InteractionHand.OFF_HAND ? var6 : 0.0F;
         var13 = 1.0F - Mth.lerp(var1, this.oOffHandHeight, this.offHandHeight);
         this.renderArmWithItem(var4, var1, var8, InteractionHand.OFF_HAND, var12, this.offHandItem, var13, var2, var3, var5);
      }

      var3.endBatch();
   }

   @VisibleForTesting
   static ItemInHandRenderer.HandRenderSelection evaluateWhichHandsToRender(LocalPlayer var0) {
      ItemStack var1 = var0.getMainHandItem();
      ItemStack var2 = var0.getOffhandItem();
      boolean var3 = var1.is(Items.BOW) || var2.is(Items.BOW);
      boolean var4 = var1.is(Items.CROSSBOW) || var2.is(Items.CROSSBOW);
      if (!var3 && !var4) {
         return ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
      } else if (var0.isUsingItem()) {
         return selectionUsingItemWhileHoldingBowLike(var0);
      } else {
         return isChargedCrossbow(var1) ? ItemInHandRenderer.HandRenderSelection.RENDER_MAIN_HAND_ONLY : ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
      }
   }

   private static ItemInHandRenderer.HandRenderSelection selectionUsingItemWhileHoldingBowLike(LocalPlayer var0) {
      ItemStack var1 = var0.getUseItem();
      InteractionHand var2 = var0.getUsedItemHand();
      if (!var1.is(Items.BOW) && !var1.is(Items.CROSSBOW)) {
         return var2 == InteractionHand.MAIN_HAND && isChargedCrossbow(var0.getOffhandItem()) ? ItemInHandRenderer.HandRenderSelection.RENDER_MAIN_HAND_ONLY : ItemInHandRenderer.HandRenderSelection.RENDER_BOTH_HANDS;
      } else {
         return ItemInHandRenderer.HandRenderSelection.onlyForHand(var2);
      }
   }

   private static boolean isChargedCrossbow(ItemStack var0) {
      return var0.is(Items.CROSSBOW) && CrossbowItem.isCharged(var0);
   }

   private void renderArmWithItem(AbstractClientPlayer var1, float var2, float var3, InteractionHand var4, float var5, ItemStack var6, float var7, PoseStack var8, MultiBufferSource var9, int var10) {
      if (!var1.isScoping()) {
         boolean var11 = var4 == InteractionHand.MAIN_HAND;
         HumanoidArm var12 = var11 ? var1.getMainArm() : var1.getMainArm().getOpposite();
         var8.pushPose();
         if (var6.isEmpty()) {
            if (var11 && !var1.isInvisible()) {
               this.renderPlayerArm(var8, var9, var10, var7, var5, var12);
            }
         } else if (var6.is(Items.FILLED_MAP)) {
            if (var11 && this.offHandItem.isEmpty()) {
               this.renderTwoHandedMap(var8, var9, var10, var3, var7, var5);
            } else {
               this.renderOneHandedMap(var8, var9, var10, var7, var12, var5, var6);
            }
         } else {
            boolean var13;
            float var16;
            float var17;
            float var18;
            float var19;
            if (var6.is(Items.CROSSBOW)) {
               var13 = CrossbowItem.isCharged(var6);
               boolean var14 = var12 == HumanoidArm.RIGHT;
               int var15 = var14 ? 1 : -1;
               if (var1.isUsingItem() && var1.getUseItemRemainingTicks() > 0 && var1.getUsedItemHand() == var4) {
                  this.applyItemArmTransform(var8, var12, var7);
                  var8.translate((double)((float)var15 * -0.4785682F), -0.0943870022892952D, 0.05731530860066414D);
                  var8.mulPose(Vector3f.XP.rotationDegrees(-11.935F));
                  var8.mulPose(Vector3f.YP.rotationDegrees((float)var15 * 65.3F));
                  var8.mulPose(Vector3f.ZP.rotationDegrees((float)var15 * -9.785F));
                  var16 = (float)var6.getUseDuration() - ((float)this.minecraft.player.getUseItemRemainingTicks() - var2 + 1.0F);
                  var17 = var16 / (float)CrossbowItem.getChargeDuration(var6);
                  if (var17 > 1.0F) {
                     var17 = 1.0F;
                  }

                  if (var17 > 0.1F) {
                     var18 = Mth.sin((var16 - 0.1F) * 1.3F);
                     var19 = var17 - 0.1F;
                     float var20 = var18 * var19;
                     var8.translate((double)(var20 * 0.0F), (double)(var20 * 0.004F), (double)(var20 * 0.0F));
                  }

                  var8.translate((double)(var17 * 0.0F), (double)(var17 * 0.0F), (double)(var17 * 0.04F));
                  var8.scale(1.0F, 1.0F, 1.0F + var17 * 0.2F);
                  var8.mulPose(Vector3f.YN.rotationDegrees((float)var15 * 45.0F));
               } else {
                  var16 = -0.4F * Mth.sin(Mth.sqrt(var5) * 3.1415927F);
                  var17 = 0.2F * Mth.sin(Mth.sqrt(var5) * 6.2831855F);
                  var18 = -0.2F * Mth.sin(var5 * 3.1415927F);
                  var8.translate((double)((float)var15 * var16), (double)var17, (double)var18);
                  this.applyItemArmTransform(var8, var12, var7);
                  this.applyItemArmAttackTransform(var8, var12, var5);
                  if (var13 && var5 < 0.001F && var11) {
                     var8.translate((double)((float)var15 * -0.641864F), 0.0D, 0.0D);
                     var8.mulPose(Vector3f.YP.rotationDegrees((float)var15 * 10.0F));
                  }
               }

               this.renderItem(var1, var6, var14 ? ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND, !var14, var8, var9, var10);
            } else {
               var13 = var12 == HumanoidArm.RIGHT;
               int var21;
               float var23;
               if (var1.isUsingItem() && var1.getUseItemRemainingTicks() > 0 && var1.getUsedItemHand() == var4) {
                  var21 = var13 ? 1 : -1;
                  switch(var6.getUseAnimation()) {
                  case NONE:
                     this.applyItemArmTransform(var8, var12, var7);
                     break;
                  case EAT:
                  case DRINK:
                     this.applyEatTransform(var8, var2, var12, var6);
                     this.applyItemArmTransform(var8, var12, var7);
                     break;
                  case BLOCK:
                     this.applyItemArmTransform(var8, var12, var7);
                     break;
                  case BOW:
                     this.applyItemArmTransform(var8, var12, var7);
                     var8.translate((double)((float)var21 * -0.2785682F), 0.18344387412071228D, 0.15731531381607056D);
                     var8.mulPose(Vector3f.XP.rotationDegrees(-13.935F));
                     var8.mulPose(Vector3f.YP.rotationDegrees((float)var21 * 35.3F));
                     var8.mulPose(Vector3f.ZP.rotationDegrees((float)var21 * -9.785F));
                     var23 = (float)var6.getUseDuration() - ((float)this.minecraft.player.getUseItemRemainingTicks() - var2 + 1.0F);
                     var16 = var23 / 20.0F;
                     var16 = (var16 * var16 + var16 * 2.0F) / 3.0F;
                     if (var16 > 1.0F) {
                        var16 = 1.0F;
                     }

                     if (var16 > 0.1F) {
                        var17 = Mth.sin((var23 - 0.1F) * 1.3F);
                        var18 = var16 - 0.1F;
                        var19 = var17 * var18;
                        var8.translate((double)(var19 * 0.0F), (double)(var19 * 0.004F), (double)(var19 * 0.0F));
                     }

                     var8.translate((double)(var16 * 0.0F), (double)(var16 * 0.0F), (double)(var16 * 0.04F));
                     var8.scale(1.0F, 1.0F, 1.0F + var16 * 0.2F);
                     var8.mulPose(Vector3f.YN.rotationDegrees((float)var21 * 45.0F));
                     break;
                  case SPEAR:
                     this.applyItemArmTransform(var8, var12, var7);
                     var8.translate((double)((float)var21 * -0.5F), 0.699999988079071D, 0.10000000149011612D);
                     var8.mulPose(Vector3f.XP.rotationDegrees(-55.0F));
                     var8.mulPose(Vector3f.YP.rotationDegrees((float)var21 * 35.3F));
                     var8.mulPose(Vector3f.ZP.rotationDegrees((float)var21 * -9.785F));
                     var23 = (float)var6.getUseDuration() - ((float)this.minecraft.player.getUseItemRemainingTicks() - var2 + 1.0F);
                     var16 = var23 / 10.0F;
                     if (var16 > 1.0F) {
                        var16 = 1.0F;
                     }

                     if (var16 > 0.1F) {
                        var17 = Mth.sin((var23 - 0.1F) * 1.3F);
                        var18 = var16 - 0.1F;
                        var19 = var17 * var18;
                        var8.translate((double)(var19 * 0.0F), (double)(var19 * 0.004F), (double)(var19 * 0.0F));
                     }

                     var8.translate(0.0D, 0.0D, (double)(var16 * 0.2F));
                     var8.scale(1.0F, 1.0F, 1.0F + var16 * 0.2F);
                     var8.mulPose(Vector3f.YN.rotationDegrees((float)var21 * 45.0F));
                  }
               } else if (var1.isAutoSpinAttack()) {
                  this.applyItemArmTransform(var8, var12, var7);
                  var21 = var13 ? 1 : -1;
                  var8.translate((double)((float)var21 * -0.4F), 0.800000011920929D, 0.30000001192092896D);
                  var8.mulPose(Vector3f.YP.rotationDegrees((float)var21 * 65.0F));
                  var8.mulPose(Vector3f.ZP.rotationDegrees((float)var21 * -85.0F));
               } else {
                  float var22 = -0.4F * Mth.sin(Mth.sqrt(var5) * 3.1415927F);
                  var23 = 0.2F * Mth.sin(Mth.sqrt(var5) * 6.2831855F);
                  var16 = -0.2F * Mth.sin(var5 * 3.1415927F);
                  int var24 = var13 ? 1 : -1;
                  var8.translate((double)((float)var24 * var22), (double)var23, (double)var16);
                  this.applyItemArmTransform(var8, var12, var7);
                  this.applyItemArmAttackTransform(var8, var12, var5);
               }

               this.renderItem(var1, var6, var13 ? ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND, !var13, var8, var9, var10);
            }
         }

         var8.popPose();
      }
   }

   public void tick() {
      this.oMainHandHeight = this.mainHandHeight;
      this.oOffHandHeight = this.offHandHeight;
      LocalPlayer var1 = this.minecraft.player;
      ItemStack var2 = var1.getMainHandItem();
      ItemStack var3 = var1.getOffhandItem();
      if (ItemStack.matches(this.mainHandItem, var2)) {
         this.mainHandItem = var2;
      }

      if (ItemStack.matches(this.offHandItem, var3)) {
         this.offHandItem = var3;
      }

      if (var1.isHandsBusy()) {
         this.mainHandHeight = Mth.clamp(this.mainHandHeight - 0.4F, 0.0F, 1.0F);
         this.offHandHeight = Mth.clamp(this.offHandHeight - 0.4F, 0.0F, 1.0F);
      } else {
         float var4 = var1.getAttackStrengthScale(1.0F);
         this.mainHandHeight += Mth.clamp((this.mainHandItem == var2 ? var4 * var4 * var4 : 0.0F) - this.mainHandHeight, -0.4F, 0.4F);
         this.offHandHeight += Mth.clamp((float)(this.offHandItem == var3 ? 1 : 0) - this.offHandHeight, -0.4F, 0.4F);
      }

      if (this.mainHandHeight < 0.1F) {
         this.mainHandItem = var2;
      }

      if (this.offHandHeight < 0.1F) {
         this.offHandItem = var3;
      }

   }

   public void itemUsed(InteractionHand var1) {
      if (var1 == InteractionHand.MAIN_HAND) {
         this.mainHandHeight = 0.0F;
      } else {
         this.offHandHeight = 0.0F;
      }

   }

   @VisibleForTesting
   static enum HandRenderSelection {
      RENDER_BOTH_HANDS(true, true),
      RENDER_MAIN_HAND_ONLY(true, false),
      RENDER_OFF_HAND_ONLY(false, true);

      final boolean renderMainHand;
      final boolean renderOffHand;

      private HandRenderSelection(boolean var3, boolean var4) {
         this.renderMainHand = var3;
         this.renderOffHand = var4;
      }

      public static ItemInHandRenderer.HandRenderSelection onlyForHand(InteractionHand var0) {
         return var0 == InteractionHand.MAIN_HAND ? RENDER_MAIN_HAND_ONLY : RENDER_OFF_HAND_ONLY;
      }

      // $FF: synthetic method
      private static ItemInHandRenderer.HandRenderSelection[] $values() {
         return new ItemInHandRenderer.HandRenderSelection[]{RENDER_BOTH_HANDS, RENDER_MAIN_HAND_ONLY, RENDER_OFF_HAND_ONLY};
      }
   }
}
