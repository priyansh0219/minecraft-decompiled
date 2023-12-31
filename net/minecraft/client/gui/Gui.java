package net.minecraft.client.gui;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3f;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.chat.ChatListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.chat.OverlayChatListener;
import net.minecraft.client.gui.chat.StandardChatListener;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.client.gui.components.spectator.SpectatorGui;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.StringDecomposer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import org.apache.commons.lang3.StringUtils;

public class Gui extends GuiComponent {
   private static final ResourceLocation VIGNETTE_LOCATION = new ResourceLocation("textures/misc/vignette.png");
   private static final ResourceLocation WIDGETS_LOCATION = new ResourceLocation("textures/gui/widgets.png");
   private static final ResourceLocation PUMPKIN_BLUR_LOCATION = new ResourceLocation("textures/misc/pumpkinblur.png");
   private static final ResourceLocation SPYGLASS_SCOPE_LOCATION = new ResourceLocation("textures/misc/spyglass_scope.png");
   private static final ResourceLocation POWDER_SNOW_OUTLINE_LOCATION = new ResourceLocation("textures/misc/powder_snow_outline.png");
   private static final Component DEMO_EXPIRED_TEXT = new TranslatableComponent("demo.demoExpired");
   private static final int COLOR_WHITE = 16777215;
   private static final float MIN_CROSSHAIR_ATTACK_SPEED = 5.0F;
   private static final int NUM_HEARTS_PER_ROW = 10;
   private static final int LINE_HEIGHT = 10;
   private static final String SPACER = ": ";
   private static final float PORTAL_OVERLAY_ALPHA_MIN = 0.2F;
   private static final int HEART_SIZE = 9;
   private static final int HEART_SEPARATION = 8;
   private final Random random = new Random();
   private final Minecraft minecraft;
   private final ItemRenderer itemRenderer;
   private final ChatComponent chat;
   private int tickCount;
   @Nullable
   private Component overlayMessageString;
   private int overlayMessageTime;
   private boolean animateOverlayMessageColor;
   public float vignetteBrightness = 1.0F;
   private int toolHighlightTimer;
   private ItemStack lastToolHighlight;
   private final DebugScreenOverlay debugScreen;
   private final SubtitleOverlay subtitleOverlay;
   private final SpectatorGui spectatorGui;
   private final PlayerTabOverlay tabList;
   private final BossHealthOverlay bossOverlay;
   private int titleTime;
   @Nullable
   private Component title;
   @Nullable
   private Component subtitle;
   private int titleFadeInTime;
   private int titleStayTime;
   private int titleFadeOutTime;
   private int lastHealth;
   private int displayHealth;
   private long lastHealthTime;
   private long healthBlinkTime;
   private int screenWidth;
   private int screenHeight;
   private final Map<ChatType, List<ChatListener>> chatListeners;
   private float scopeScale;

   public Gui(Minecraft var1) {
      this.lastToolHighlight = ItemStack.EMPTY;
      this.chatListeners = Maps.newHashMap();
      this.minecraft = var1;
      this.itemRenderer = var1.getItemRenderer();
      this.debugScreen = new DebugScreenOverlay(var1);
      this.spectatorGui = new SpectatorGui(var1);
      this.chat = new ChatComponent(var1);
      this.tabList = new PlayerTabOverlay(var1, this);
      this.bossOverlay = new BossHealthOverlay(var1);
      this.subtitleOverlay = new SubtitleOverlay(var1);
      ChatType[] var2 = ChatType.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         ChatType var5 = var2[var4];
         this.chatListeners.put(var5, Lists.newArrayList());
      }

      NarratorChatListener var6 = NarratorChatListener.INSTANCE;
      ((List)this.chatListeners.get(ChatType.CHAT)).add(new StandardChatListener(var1));
      ((List)this.chatListeners.get(ChatType.CHAT)).add(var6);
      ((List)this.chatListeners.get(ChatType.SYSTEM)).add(new StandardChatListener(var1));
      ((List)this.chatListeners.get(ChatType.SYSTEM)).add(var6);
      ((List)this.chatListeners.get(ChatType.GAME_INFO)).add(new OverlayChatListener(var1));
      this.resetTitleTimes();
   }

   public void resetTitleTimes() {
      this.titleFadeInTime = 10;
      this.titleStayTime = 70;
      this.titleFadeOutTime = 20;
   }

   public void render(PoseStack var1, float var2) {
      this.screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
      this.screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
      Font var3 = this.getFont();
      RenderSystem.enableBlend();
      if (Minecraft.useFancyGraphics()) {
         this.renderVignette(this.minecraft.getCameraEntity());
      } else {
         RenderSystem.enableDepthTest();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.defaultBlendFunc();
      }

      float var4 = this.minecraft.getDeltaFrameTime();
      this.scopeScale = Mth.lerp(0.5F * var4, this.scopeScale, 1.125F);
      if (this.minecraft.options.getCameraType().isFirstPerson()) {
         if (this.minecraft.player.isScoping()) {
            this.renderSpyglassOverlay(this.scopeScale);
         } else {
            this.scopeScale = 0.5F;
            ItemStack var5 = this.minecraft.player.getInventory().getArmor(3);
            if (var5.is(Blocks.CARVED_PUMPKIN.asItem())) {
               this.renderTextureOverlay(PUMPKIN_BLUR_LOCATION, 1.0F);
            }
         }
      }

      if (this.minecraft.player.getTicksFrozen() > 0) {
         this.renderTextureOverlay(POWDER_SNOW_OUTLINE_LOCATION, this.minecraft.player.getPercentFrozen());
      }

      float var11 = Mth.lerp(var2, this.minecraft.player.oPortalTime, this.minecraft.player.portalTime);
      if (var11 > 0.0F && !this.minecraft.player.hasEffect(MobEffects.CONFUSION)) {
         this.renderPortalOverlay(var11);
      }

      if (this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
         this.spectatorGui.renderHotbar(var1, var2);
      } else if (!this.minecraft.options.hideGui) {
         this.renderHotbar(var2, var1);
      }

      if (!this.minecraft.options.hideGui) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderTexture(0, GUI_ICONS_LOCATION);
         RenderSystem.enableBlend();
         this.renderCrosshair(var1);
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.defaultBlendFunc();
         this.minecraft.getProfiler().push("bossHealth");
         this.bossOverlay.render(var1);
         this.minecraft.getProfiler().pop();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShaderTexture(0, GUI_ICONS_LOCATION);
         if (this.minecraft.gameMode.canHurtPlayer()) {
            this.renderPlayerHealth(var1);
         }

         this.renderVehicleHealth(var1);
         RenderSystem.disableBlend();
         int var6 = this.screenWidth / 2 - 91;
         if (this.minecraft.player.isRidingJumpable()) {
            this.renderJumpMeter(var1, var6);
         } else if (this.minecraft.gameMode.hasExperience()) {
            this.renderExperienceBar(var1, var6);
         }

         if (this.minecraft.options.heldItemTooltips && this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            this.renderSelectedItemName(var1);
         } else if (this.minecraft.player.isSpectator()) {
            this.spectatorGui.renderTooltip(var1);
         }
      }

      int var8;
      float var12;
      if (this.minecraft.player.getSleepTimer() > 0) {
         this.minecraft.getProfiler().push("sleep");
         RenderSystem.disableDepthTest();
         var12 = (float)this.minecraft.player.getSleepTimer();
         float var7 = var12 / 100.0F;
         if (var7 > 1.0F) {
            var7 = 1.0F - (var12 - 100.0F) / 10.0F;
         }

         var8 = (int)(220.0F * var7) << 24 | 1052704;
         fill(var1, 0, 0, this.screenWidth, this.screenHeight, var8);
         RenderSystem.enableDepthTest();
         this.minecraft.getProfiler().pop();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }

      if (this.minecraft.isDemo()) {
         this.renderDemoOverlay(var1);
      }

      this.renderEffects(var1);
      if (this.minecraft.options.renderDebug) {
         this.debugScreen.render(var1);
      }

      if (!this.minecraft.options.hideGui) {
         int var9;
         int var10;
         int var14;
         if (this.overlayMessageString != null && this.overlayMessageTime > 0) {
            this.minecraft.getProfiler().push("overlayMessage");
            var12 = (float)this.overlayMessageTime - var2;
            var14 = (int)(var12 * 255.0F / 20.0F);
            if (var14 > 255) {
               var14 = 255;
            }

            if (var14 > 8) {
               var1.pushPose();
               var1.translate((double)(this.screenWidth / 2), (double)(this.screenHeight - 68), 0.0D);
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               var8 = 16777215;
               if (this.animateOverlayMessageColor) {
                  var8 = Mth.hsvToRgb(var12 / 50.0F, 0.7F, 0.6F) & 16777215;
               }

               var9 = var14 << 24 & -16777216;
               var10 = var3.width((FormattedText)this.overlayMessageString);
               this.drawBackdrop(var1, var3, -4, var10, 16777215 | var9);
               var3.draw(var1, this.overlayMessageString, (float)(-var10 / 2), -4.0F, var8 | var9);
               RenderSystem.disableBlend();
               var1.popPose();
            }

            this.minecraft.getProfiler().pop();
         }

         if (this.title != null && this.titleTime > 0) {
            this.minecraft.getProfiler().push("titleAndSubtitle");
            var12 = (float)this.titleTime - var2;
            var14 = 255;
            if (this.titleTime > this.titleFadeOutTime + this.titleStayTime) {
               float var15 = (float)(this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime) - var12;
               var14 = (int)(var15 * 255.0F / (float)this.titleFadeInTime);
            }

            if (this.titleTime <= this.titleFadeOutTime) {
               var14 = (int)(var12 * 255.0F / (float)this.titleFadeOutTime);
            }

            var14 = Mth.clamp((int)var14, (int)0, (int)255);
            if (var14 > 8) {
               var1.pushPose();
               var1.translate((double)(this.screenWidth / 2), (double)(this.screenHeight / 2), 0.0D);
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               var1.pushPose();
               var1.scale(4.0F, 4.0F, 4.0F);
               var8 = var14 << 24 & -16777216;
               var9 = var3.width((FormattedText)this.title);
               this.drawBackdrop(var1, var3, -10, var9, 16777215 | var8);
               var3.drawShadow(var1, this.title, (float)(-var9 / 2), -10.0F, 16777215 | var8);
               var1.popPose();
               if (this.subtitle != null) {
                  var1.pushPose();
                  var1.scale(2.0F, 2.0F, 2.0F);
                  var10 = var3.width((FormattedText)this.subtitle);
                  this.drawBackdrop(var1, var3, 5, var10, 16777215 | var8);
                  var3.drawShadow(var1, this.subtitle, (float)(-var10 / 2), 5.0F, 16777215 | var8);
                  var1.popPose();
               }

               RenderSystem.disableBlend();
               var1.popPose();
            }

            this.minecraft.getProfiler().pop();
         }

         this.subtitleOverlay.render(var1);
         Scoreboard var13 = this.minecraft.level.getScoreboard();
         Objective var18 = null;
         PlayerTeam var16 = var13.getPlayersTeam(this.minecraft.player.getScoreboardName());
         if (var16 != null) {
            var9 = var16.getColor().getId();
            if (var9 >= 0) {
               var18 = var13.getDisplayObjective(3 + var9);
            }
         }

         Objective var17 = var18 != null ? var18 : var13.getDisplayObjective(1);
         if (var17 != null) {
            this.displayScoreboardSidebar(var1, var17);
         }

         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         var1.pushPose();
         var1.translate(0.0D, (double)(this.screenHeight - 48), 0.0D);
         this.minecraft.getProfiler().push("chat");
         this.chat.render(var1, this.tickCount);
         this.minecraft.getProfiler().pop();
         var1.popPose();
         var17 = var13.getDisplayObjective(0);
         if (!this.minecraft.options.keyPlayerList.isDown() || this.minecraft.isLocalServer() && this.minecraft.player.connection.getOnlinePlayers().size() <= 1 && var17 == null) {
            this.tabList.setVisible(false);
         } else {
            this.tabList.setVisible(true);
            this.tabList.render(var1, this.screenWidth, var13, var17);
         }
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void drawBackdrop(PoseStack var1, Font var2, int var3, int var4, int var5) {
      int var6 = this.minecraft.options.getBackgroundColor(0.0F);
      if (var6 != 0) {
         int var7 = -var4 / 2;
         int var10001 = var7 - 2;
         int var10002 = var3 - 2;
         int var10003 = var7 + var4 + 2;
         Objects.requireNonNull(var2);
         fill(var1, var10001, var10002, var10003, var3 + 9 + 2, FastColor.ARGB32.multiply(var6, var5));
      }

   }

   private void renderCrosshair(PoseStack var1) {
      Options var2 = this.minecraft.options;
      if (var2.getCameraType().isFirstPerson()) {
         if (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR || this.canRenderCrosshairForSpectator(this.minecraft.hitResult)) {
            if (var2.renderDebug && !var2.hideGui && !this.minecraft.player.isReducedDebugInfo() && !var2.reducedDebugInfo) {
               Camera var9 = this.minecraft.gameRenderer.getMainCamera();
               PoseStack var10 = RenderSystem.getModelViewStack();
               var10.pushPose();
               var10.translate((double)(this.screenWidth / 2), (double)(this.screenHeight / 2), (double)this.getBlitOffset());
               var10.mulPose(Vector3f.XN.rotationDegrees(var9.getXRot()));
               var10.mulPose(Vector3f.YP.rotationDegrees(var9.getYRot()));
               var10.scale(-1.0F, -1.0F, -1.0F);
               RenderSystem.applyModelViewMatrix();
               RenderSystem.renderCrosshair(10);
               var10.popPose();
               RenderSystem.applyModelViewMatrix();
            } else {
               RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
               boolean var3 = true;
               this.blit(var1, (this.screenWidth - 15) / 2, (this.screenHeight - 15) / 2, 0, 0, 15, 15);
               if (this.minecraft.options.attackIndicator == AttackIndicatorStatus.CROSSHAIR) {
                  float var4 = this.minecraft.player.getAttackStrengthScale(0.0F);
                  boolean var5 = false;
                  if (this.minecraft.crosshairPickEntity != null && this.minecraft.crosshairPickEntity instanceof LivingEntity && var4 >= 1.0F) {
                     var5 = this.minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0F;
                     var5 &= this.minecraft.crosshairPickEntity.isAlive();
                  }

                  int var6 = this.screenHeight / 2 - 7 + 16;
                  int var7 = this.screenWidth / 2 - 8;
                  if (var5) {
                     this.blit(var1, var7, var6, 68, 94, 16, 16);
                  } else if (var4 < 1.0F) {
                     int var8 = (int)(var4 * 17.0F);
                     this.blit(var1, var7, var6, 36, 94, 16, 4);
                     this.blit(var1, var7, var6, 52, 94, var8, 4);
                  }
               }
            }

         }
      }
   }

   private boolean canRenderCrosshairForSpectator(HitResult var1) {
      if (var1 == null) {
         return false;
      } else if (var1.getType() == HitResult.Type.ENTITY) {
         return ((EntityHitResult)var1).getEntity() instanceof MenuProvider;
      } else if (var1.getType() == HitResult.Type.BLOCK) {
         BlockPos var2 = ((BlockHitResult)var1).getBlockPos();
         ClientLevel var3 = this.minecraft.level;
         return var3.getBlockState(var2).getMenuProvider(var3, var2) != null;
      } else {
         return false;
      }
   }

   protected void renderEffects(PoseStack var1) {
      Collection var2 = this.minecraft.player.getActiveEffects();
      if (!var2.isEmpty()) {
         RenderSystem.enableBlend();
         int var3 = 0;
         int var4 = 0;
         MobEffectTextureManager var5 = this.minecraft.getMobEffectTextures();
         ArrayList var6 = Lists.newArrayListWithExpectedSize(var2.size());
         RenderSystem.setShaderTexture(0, AbstractContainerScreen.INVENTORY_LOCATION);
         Iterator var7 = Ordering.natural().reverse().sortedCopy(var2).iterator();

         while(var7.hasNext()) {
            MobEffectInstance var8 = (MobEffectInstance)var7.next();
            MobEffect var9 = var8.getEffect();
            if (var8.showIcon()) {
               int var10 = this.screenWidth;
               int var11 = 1;
               if (this.minecraft.isDemo()) {
                  var11 += 15;
               }

               if (var9.isBeneficial()) {
                  ++var3;
                  var10 -= 25 * var3;
               } else {
                  ++var4;
                  var10 -= 25 * var4;
                  var11 += 26;
               }

               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               float var12 = 1.0F;
               if (var8.isAmbient()) {
                  this.blit(var1, var10, var11, 165, 166, 24, 24);
               } else {
                  this.blit(var1, var10, var11, 141, 166, 24, 24);
                  if (var8.getDuration() <= 200) {
                     int var13 = 10 - var8.getDuration() / 20;
                     var12 = Mth.clamp((float)var8.getDuration() / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F) + Mth.cos((float)var8.getDuration() * 3.1415927F / 5.0F) * Mth.clamp((float)var13 / 10.0F * 0.25F, 0.0F, 0.25F);
                  }
               }

               TextureAtlasSprite var17 = var5.get(var9);
               var6.add(() -> {
                  RenderSystem.setShaderTexture(0, var17.atlas().location());
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, var12);
                  blit(var1, var10 + 3, var11 + 3, this.getBlitOffset(), 18, 18, var17);
               });
            }
         }

         var6.forEach(Runnable::run);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void renderHotbar(float var1, PoseStack var2) {
      Player var3 = this.getCameraPlayer();
      if (var3 != null) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
         ItemStack var4 = var3.getOffhandItem();
         HumanoidArm var5 = var3.getMainArm().getOpposite();
         int var6 = this.screenWidth / 2;
         int var7 = this.getBlitOffset();
         boolean var8 = true;
         boolean var9 = true;
         this.setBlitOffset(-90);
         this.blit(var2, var6 - 91, this.screenHeight - 22, 0, 0, 182, 22);
         this.blit(var2, var6 - 91 - 1 + var3.getInventory().selected * 20, this.screenHeight - 22 - 1, 0, 22, 24, 22);
         if (!var4.isEmpty()) {
            if (var5 == HumanoidArm.LEFT) {
               this.blit(var2, var6 - 91 - 29, this.screenHeight - 23, 24, 22, 29, 24);
            } else {
               this.blit(var2, var6 + 91, this.screenHeight - 23, 53, 22, 29, 24);
            }
         }

         this.setBlitOffset(var7);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         int var10 = 1;

         int var11;
         int var12;
         int var13;
         for(var11 = 0; var11 < 9; ++var11) {
            var12 = var6 - 90 + var11 * 20 + 2;
            var13 = this.screenHeight - 16 - 3;
            this.renderSlot(var12, var13, var1, var3, (ItemStack)var3.getInventory().items.get(var11), var10++);
         }

         if (!var4.isEmpty()) {
            var11 = this.screenHeight - 16 - 3;
            if (var5 == HumanoidArm.LEFT) {
               this.renderSlot(var6 - 91 - 26, var11, var1, var3, var4, var10++);
            } else {
               this.renderSlot(var6 + 91 + 10, var11, var1, var3, var4, var10++);
            }
         }

         if (this.minecraft.options.attackIndicator == AttackIndicatorStatus.HOTBAR) {
            float var15 = this.minecraft.player.getAttackStrengthScale(0.0F);
            if (var15 < 1.0F) {
               var12 = this.screenHeight - 20;
               var13 = var6 + 91 + 6;
               if (var5 == HumanoidArm.RIGHT) {
                  var13 = var6 - 91 - 22;
               }

               RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
               int var14 = (int)(var15 * 19.0F);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               this.blit(var2, var13, var12, 0, 94, 18, 18);
               this.blit(var2, var13, var12 + 18 - var14, 18, 112 - var14, 18, var14);
            }
         }

         RenderSystem.disableBlend();
      }
   }

   public void renderJumpMeter(PoseStack var1, int var2) {
      this.minecraft.getProfiler().push("jumpBar");
      RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
      float var3 = this.minecraft.player.getJumpRidingScale();
      boolean var4 = true;
      int var5 = (int)(var3 * 183.0F);
      int var6 = this.screenHeight - 32 + 3;
      this.blit(var1, var2, var6, 0, 84, 182, 5);
      if (var5 > 0) {
         this.blit(var1, var2, var6, 0, 89, var5, 5);
      }

      this.minecraft.getProfiler().pop();
   }

   public void renderExperienceBar(PoseStack var1, int var2) {
      this.minecraft.getProfiler().push("expBar");
      RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
      int var3 = this.minecraft.player.getXpNeededForNextLevel();
      int var5;
      int var6;
      if (var3 > 0) {
         boolean var4 = true;
         var5 = (int)(this.minecraft.player.experienceProgress * 183.0F);
         var6 = this.screenHeight - 32 + 3;
         this.blit(var1, var2, var6, 0, 64, 182, 5);
         if (var5 > 0) {
            this.blit(var1, var2, var6, 0, 69, var5, 5);
         }
      }

      this.minecraft.getProfiler().pop();
      if (this.minecraft.player.experienceLevel > 0) {
         this.minecraft.getProfiler().push("expLevel");
         String var7 = this.minecraft.player.experienceLevel.makeConcatWithConstants<invokedynamic>(this.minecraft.player.experienceLevel);
         var5 = (this.screenWidth - this.getFont().width(var7)) / 2;
         var6 = this.screenHeight - 31 - 4;
         this.getFont().draw(var1, (String)var7, (float)(var5 + 1), (float)var6, 0);
         this.getFont().draw(var1, (String)var7, (float)(var5 - 1), (float)var6, 0);
         this.getFont().draw(var1, (String)var7, (float)var5, (float)(var6 + 1), 0);
         this.getFont().draw(var1, (String)var7, (float)var5, (float)(var6 - 1), 0);
         this.getFont().draw(var1, var7, (float)var5, (float)var6, 8453920);
         this.minecraft.getProfiler().pop();
      }

   }

   public void renderSelectedItemName(PoseStack var1) {
      this.minecraft.getProfiler().push("selectedItemName");
      if (this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty()) {
         MutableComponent var2 = (new TextComponent("")).append(this.lastToolHighlight.getHoverName()).withStyle(this.lastToolHighlight.getRarity().color);
         if (this.lastToolHighlight.hasCustomHoverName()) {
            var2.withStyle(ChatFormatting.ITALIC);
         }

         int var3 = this.getFont().width((FormattedText)var2);
         int var4 = (this.screenWidth - var3) / 2;
         int var5 = this.screenHeight - 59;
         if (!this.minecraft.gameMode.canHurtPlayer()) {
            var5 += 14;
         }

         int var6 = (int)((float)this.toolHighlightTimer * 256.0F / 10.0F);
         if (var6 > 255) {
            var6 = 255;
         }

         if (var6 > 0) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            int var10001 = var4 - 2;
            int var10002 = var5 - 2;
            int var10003 = var4 + var3 + 2;
            Objects.requireNonNull(this.getFont());
            fill(var1, var10001, var10002, var10003, var5 + 9 + 2, this.minecraft.options.getBackgroundColor(0));
            this.getFont().drawShadow(var1, (Component)var2, (float)var4, (float)var5, 16777215 + (var6 << 24));
            RenderSystem.disableBlend();
         }
      }

      this.minecraft.getProfiler().pop();
   }

   public void renderDemoOverlay(PoseStack var1) {
      this.minecraft.getProfiler().push("demo");
      Object var2;
      if (this.minecraft.level.getGameTime() >= 120500L) {
         var2 = DEMO_EXPIRED_TEXT;
      } else {
         var2 = new TranslatableComponent("demo.remainingTime", new Object[]{StringUtil.formatTickDuration((int)(120500L - this.minecraft.level.getGameTime()))});
      }

      int var3 = this.getFont().width((FormattedText)var2);
      this.getFont().drawShadow(var1, (Component)var2, (float)(this.screenWidth - var3 - 10), 5.0F, 16777215);
      this.minecraft.getProfiler().pop();
   }

   private void displayScoreboardSidebar(PoseStack var1, Objective var2) {
      Scoreboard var3 = var2.getScoreboard();
      Collection var4 = var3.getPlayerScores(var2);
      List var5 = (List)var4.stream().filter((var0) -> {
         return var0.getOwner() != null && !var0.getOwner().startsWith("#");
      }).collect(Collectors.toList());
      Object var26;
      if (var5.size() > 15) {
         var26 = Lists.newArrayList(Iterables.skip(var5, var4.size() - 15));
      } else {
         var26 = var5;
      }

      ArrayList var6 = Lists.newArrayListWithCapacity(((Collection)var26).size());
      Component var7 = var2.getDisplayName();
      int var8 = this.getFont().width((FormattedText)var7);
      int var9 = var8;
      int var10 = this.getFont().width(": ");

      Score var12;
      MutableComponent var14;
      for(Iterator var11 = ((Collection)var26).iterator(); var11.hasNext(); var9 = Math.max(var9, this.getFont().width((FormattedText)var14) + var10 + this.getFont().width(Integer.toString(var12.getScore())))) {
         var12 = (Score)var11.next();
         PlayerTeam var13 = var3.getPlayersTeam(var12.getOwner());
         var14 = PlayerTeam.formatNameForTeam(var13, new TextComponent(var12.getOwner()));
         var6.add(Pair.of(var12, var14));
      }

      int var10000 = ((Collection)var26).size();
      Objects.requireNonNull(this.getFont());
      int var27 = var10000 * 9;
      int var28 = this.screenHeight / 2 + var27 / 3;
      boolean var29 = true;
      int var30 = this.screenWidth - var9 - 3;
      int var15 = 0;
      int var16 = this.minecraft.options.getBackgroundColor(0.3F);
      int var17 = this.minecraft.options.getBackgroundColor(0.4F);
      Iterator var18 = var6.iterator();

      while(var18.hasNext()) {
         Pair var19 = (Pair)var18.next();
         ++var15;
         Score var20 = (Score)var19.getFirst();
         Component var21 = (Component)var19.getSecond();
         ChatFormatting var31 = ChatFormatting.RED;
         String var22 = var31 + var20.getScore();
         Objects.requireNonNull(this.getFont());
         int var24 = var28 - var15 * 9;
         int var25 = this.screenWidth - 3 + 2;
         int var10001 = var30 - 2;
         Objects.requireNonNull(this.getFont());
         fill(var1, var10001, var24, var25, var24 + 9, var16);
         this.getFont().draw(var1, (Component)var21, (float)var30, (float)var24, -1);
         this.getFont().draw(var1, (String)var22, (float)(var25 - this.getFont().width(var22)), (float)var24, -1);
         if (var15 == ((Collection)var26).size()) {
            var10001 = var30 - 2;
            Objects.requireNonNull(this.getFont());
            fill(var1, var10001, var24 - 9 - 1, var25, var24 - 1, var17);
            fill(var1, var30 - 2, var24 - 1, var25, var24, var16);
            Font var32 = this.getFont();
            float var10003 = (float)(var30 + var9 / 2 - var8 / 2);
            Objects.requireNonNull(this.getFont());
            var32.draw(var1, (Component)var7, var10003, (float)(var24 - 9), -1);
         }
      }

   }

   private Player getCameraPlayer() {
      return !(this.minecraft.getCameraEntity() instanceof Player) ? null : (Player)this.minecraft.getCameraEntity();
   }

   private LivingEntity getPlayerVehicleWithHealth() {
      Player var1 = this.getCameraPlayer();
      if (var1 != null) {
         Entity var2 = var1.getVehicle();
         if (var2 == null) {
            return null;
         }

         if (var2 instanceof LivingEntity) {
            return (LivingEntity)var2;
         }
      }

      return null;
   }

   private int getVehicleMaxHearts(LivingEntity var1) {
      if (var1 != null && var1.showVehicleHealth()) {
         float var2 = var1.getMaxHealth();
         int var3 = (int)(var2 + 0.5F) / 2;
         if (var3 > 30) {
            var3 = 30;
         }

         return var3;
      } else {
         return 0;
      }
   }

   private int getVisibleVehicleHeartRows(int var1) {
      return (int)Math.ceil((double)var1 / 10.0D);
   }

   private void renderPlayerHealth(PoseStack var1) {
      Player var2 = this.getCameraPlayer();
      if (var2 != null) {
         int var3 = Mth.ceil(var2.getHealth());
         boolean var4 = this.healthBlinkTime > (long)this.tickCount && (this.healthBlinkTime - (long)this.tickCount) / 3L % 2L == 1L;
         long var5 = Util.getMillis();
         if (var3 < this.lastHealth && var2.invulnerableTime > 0) {
            this.lastHealthTime = var5;
            this.healthBlinkTime = (long)(this.tickCount + 20);
         } else if (var3 > this.lastHealth && var2.invulnerableTime > 0) {
            this.lastHealthTime = var5;
            this.healthBlinkTime = (long)(this.tickCount + 10);
         }

         if (var5 - this.lastHealthTime > 1000L) {
            this.lastHealth = var3;
            this.displayHealth = var3;
            this.lastHealthTime = var5;
         }

         this.lastHealth = var3;
         int var7 = this.displayHealth;
         this.random.setSeed((long)(this.tickCount * 312871));
         FoodData var8 = var2.getFoodData();
         int var9 = var8.getFoodLevel();
         int var10 = this.screenWidth / 2 - 91;
         int var11 = this.screenWidth / 2 + 91;
         int var12 = this.screenHeight - 39;
         float var13 = Math.max((float)var2.getAttributeValue(Attributes.MAX_HEALTH), (float)Math.max(var7, var3));
         int var14 = Mth.ceil(var2.getAbsorptionAmount());
         int var15 = Mth.ceil((var13 + (float)var14) / 2.0F / 10.0F);
         int var16 = Math.max(10 - (var15 - 2), 3);
         int var17 = var12 - (var15 - 1) * var16 - 10;
         int var18 = var12 - 10;
         int var19 = var2.getArmorValue();
         int var20 = -1;
         if (var2.hasEffect(MobEffects.REGENERATION)) {
            var20 = this.tickCount % Mth.ceil(var13 + 5.0F);
         }

         this.minecraft.getProfiler().push("armor");

         int var22;
         for(int var21 = 0; var21 < 10; ++var21) {
            if (var19 > 0) {
               var22 = var10 + var21 * 8;
               if (var21 * 2 + 1 < var19) {
                  this.blit(var1, var22, var17, 34, 9, 9, 9);
               }

               if (var21 * 2 + 1 == var19) {
                  this.blit(var1, var22, var17, 25, 9, 9, 9);
               }

               if (var21 * 2 + 1 > var19) {
                  this.blit(var1, var22, var17, 16, 9, 9, 9);
               }
            }
         }

         this.minecraft.getProfiler().popPush("health");
         this.renderHearts(var1, var2, var10, var12, var16, var20, var13, var3, var7, var14, var4);
         LivingEntity var29 = this.getPlayerVehicleWithHealth();
         var22 = this.getVehicleMaxHearts(var29);
         int var23;
         int var24;
         int var25;
         int var27;
         if (var22 == 0) {
            this.minecraft.getProfiler().popPush("food");

            for(var23 = 0; var23 < 10; ++var23) {
               var24 = var12;
               var25 = 16;
               byte var26 = 0;
               if (var2.hasEffect(MobEffects.HUNGER)) {
                  var25 += 36;
                  var26 = 13;
               }

               if (var2.getFoodData().getSaturationLevel() <= 0.0F && this.tickCount % (var9 * 3 + 1) == 0) {
                  var24 = var12 + (this.random.nextInt(3) - 1);
               }

               var27 = var11 - var23 * 8 - 9;
               this.blit(var1, var27, var24, 16 + var26 * 9, 27, 9, 9);
               if (var23 * 2 + 1 < var9) {
                  this.blit(var1, var27, var24, var25 + 36, 27, 9, 9);
               }

               if (var23 * 2 + 1 == var9) {
                  this.blit(var1, var27, var24, var25 + 45, 27, 9, 9);
               }
            }

            var18 -= 10;
         }

         this.minecraft.getProfiler().popPush("air");
         var23 = var2.getMaxAirSupply();
         var24 = Math.min(var2.getAirSupply(), var23);
         if (var2.isEyeInFluid(FluidTags.WATER) || var24 < var23) {
            var25 = this.getVisibleVehicleHeartRows(var22) - 1;
            var18 -= var25 * 10;
            int var30 = Mth.ceil((double)(var24 - 2) * 10.0D / (double)var23);
            var27 = Mth.ceil((double)var24 * 10.0D / (double)var23) - var30;

            for(int var28 = 0; var28 < var30 + var27; ++var28) {
               if (var28 < var30) {
                  this.blit(var1, var11 - var28 * 8 - 9, var18, 16, 18, 9, 9);
               } else {
                  this.blit(var1, var11 - var28 * 8 - 9, var18, 25, 18, 9, 9);
               }
            }
         }

         this.minecraft.getProfiler().pop();
      }
   }

   private void renderHearts(PoseStack var1, Player var2, int var3, int var4, int var5, int var6, float var7, int var8, int var9, int var10, boolean var11) {
      Gui.HeartType var12 = Gui.HeartType.forPlayer(var2);
      int var13 = 9 * (var2.level.getLevelData().isHardcore() ? 5 : 0);
      int var14 = Mth.ceil((double)var7 / 2.0D);
      int var15 = Mth.ceil((double)var10 / 2.0D);
      int var16 = var14 * 2;

      for(int var17 = var14 + var15 - 1; var17 >= 0; --var17) {
         int var18 = var17 / 10;
         int var19 = var17 % 10;
         int var20 = var3 + var19 * 8;
         int var21 = var4 - var18 * var5;
         if (var8 + var10 <= 4) {
            var21 += this.random.nextInt(2);
         }

         if (var17 < var14 && var17 == var6) {
            var21 -= 2;
         }

         this.renderHeart(var1, Gui.HeartType.CONTAINER, var20, var21, var13, var11, false);
         int var22 = var17 * 2;
         boolean var23 = var17 >= var14;
         if (var23) {
            int var24 = var22 - var16;
            if (var24 < var10) {
               boolean var25 = var24 + 1 == var10;
               this.renderHeart(var1, var12 == Gui.HeartType.WITHERED ? var12 : Gui.HeartType.ABSORBING, var20, var21, var13, false, var25);
            }
         }

         boolean var26;
         if (var11 && var22 < var9) {
            var26 = var22 + 1 == var9;
            this.renderHeart(var1, var12, var20, var21, var13, true, var26);
         }

         if (var22 < var8) {
            var26 = var22 + 1 == var8;
            this.renderHeart(var1, var12, var20, var21, var13, false, var26);
         }
      }

   }

   private void renderHeart(PoseStack var1, Gui.HeartType var2, int var3, int var4, int var5, boolean var6, boolean var7) {
      this.blit(var1, var3, var4, var2.getX(var7, var6), var5, 9, 9);
   }

   private void renderVehicleHealth(PoseStack var1) {
      LivingEntity var2 = this.getPlayerVehicleWithHealth();
      if (var2 != null) {
         int var3 = this.getVehicleMaxHearts(var2);
         if (var3 != 0) {
            int var4 = (int)Math.ceil((double)var2.getHealth());
            this.minecraft.getProfiler().popPush("mountHealth");
            int var5 = this.screenHeight - 39;
            int var6 = this.screenWidth / 2 + 91;
            int var7 = var5;
            int var8 = 0;

            for(boolean var9 = false; var3 > 0; var8 += 20) {
               int var10 = Math.min(var3, 10);
               var3 -= var10;

               for(int var11 = 0; var11 < var10; ++var11) {
                  boolean var12 = true;
                  byte var13 = 0;
                  int var14 = var6 - var11 * 8 - 9;
                  this.blit(var1, var14, var7, 52 + var13 * 9, 9, 9, 9);
                  if (var11 * 2 + 1 + var8 < var4) {
                     this.blit(var1, var14, var7, 88, 9, 9, 9);
                  }

                  if (var11 * 2 + 1 + var8 == var4) {
                     this.blit(var1, var14, var7, 97, 9, 9, 9);
                  }
               }

               var7 -= 10;
            }

         }
      }
   }

   private void renderTextureOverlay(ResourceLocation var1, float var2) {
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, var2);
      RenderSystem.setShaderTexture(0, var1);
      Tesselator var3 = Tesselator.getInstance();
      BufferBuilder var4 = var3.getBuilder();
      var4.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      var4.vertex(0.0D, (double)this.screenHeight, -90.0D).uv(0.0F, 1.0F).endVertex();
      var4.vertex((double)this.screenWidth, (double)this.screenHeight, -90.0D).uv(1.0F, 1.0F).endVertex();
      var4.vertex((double)this.screenWidth, 0.0D, -90.0D).uv(1.0F, 0.0F).endVertex();
      var4.vertex(0.0D, 0.0D, -90.0D).uv(0.0F, 0.0F).endVertex();
      var3.end();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderSpyglassOverlay(float var1) {
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, SPYGLASS_SCOPE_LOCATION);
      Tesselator var2 = Tesselator.getInstance();
      BufferBuilder var3 = var2.getBuilder();
      float var4 = (float)Math.min(this.screenWidth, this.screenHeight);
      float var6 = Math.min((float)this.screenWidth / var4, (float)this.screenHeight / var4) * var1;
      float var7 = var4 * var6;
      float var8 = var4 * var6;
      float var9 = ((float)this.screenWidth - var7) / 2.0F;
      float var10 = ((float)this.screenHeight - var8) / 2.0F;
      float var11 = var9 + var7;
      float var12 = var10 + var8;
      var3.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      var3.vertex((double)var9, (double)var12, -90.0D).uv(0.0F, 1.0F).endVertex();
      var3.vertex((double)var11, (double)var12, -90.0D).uv(1.0F, 1.0F).endVertex();
      var3.vertex((double)var11, (double)var10, -90.0D).uv(1.0F, 0.0F).endVertex();
      var3.vertex((double)var9, (double)var10, -90.0D).uv(0.0F, 0.0F).endVertex();
      var2.end();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.disableTexture();
      var3.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      var3.vertex(0.0D, (double)this.screenHeight, -90.0D).color(0, 0, 0, 255).endVertex();
      var3.vertex((double)this.screenWidth, (double)this.screenHeight, -90.0D).color(0, 0, 0, 255).endVertex();
      var3.vertex((double)this.screenWidth, (double)var12, -90.0D).color(0, 0, 0, 255).endVertex();
      var3.vertex(0.0D, (double)var12, -90.0D).color(0, 0, 0, 255).endVertex();
      var3.vertex(0.0D, (double)var10, -90.0D).color(0, 0, 0, 255).endVertex();
      var3.vertex((double)this.screenWidth, (double)var10, -90.0D).color(0, 0, 0, 255).endVertex();
      var3.vertex((double)this.screenWidth, 0.0D, -90.0D).color(0, 0, 0, 255).endVertex();
      var3.vertex(0.0D, 0.0D, -90.0D).color(0, 0, 0, 255).endVertex();
      var3.vertex(0.0D, (double)var12, -90.0D).color(0, 0, 0, 255).endVertex();
      var3.vertex((double)var9, (double)var12, -90.0D).color(0, 0, 0, 255).endVertex();
      var3.vertex((double)var9, (double)var10, -90.0D).color(0, 0, 0, 255).endVertex();
      var3.vertex(0.0D, (double)var10, -90.0D).color(0, 0, 0, 255).endVertex();
      var3.vertex((double)var11, (double)var12, -90.0D).color(0, 0, 0, 255).endVertex();
      var3.vertex((double)this.screenWidth, (double)var12, -90.0D).color(0, 0, 0, 255).endVertex();
      var3.vertex((double)this.screenWidth, (double)var10, -90.0D).color(0, 0, 0, 255).endVertex();
      var3.vertex((double)var11, (double)var10, -90.0D).color(0, 0, 0, 255).endVertex();
      var2.end();
      RenderSystem.enableTexture();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void updateVignetteBrightness(Entity var1) {
      if (var1 != null) {
         float var2 = Mth.clamp(1.0F - var1.getBrightness(), 0.0F, 1.0F);
         this.vignetteBrightness = (float)((double)this.vignetteBrightness + (double)(var2 - this.vignetteBrightness) * 0.01D);
      }
   }

   private void renderVignette(Entity var1) {
      WorldBorder var2 = this.minecraft.level.getWorldBorder();
      float var3 = (float)var2.getDistanceToBorder(var1);
      double var4 = Math.min(var2.getLerpSpeed() * (double)var2.getWarningTime() * 1000.0D, Math.abs(var2.getLerpTarget() - var2.getSize()));
      double var6 = Math.max((double)var2.getWarningBlocks(), var4);
      if ((double)var3 < var6) {
         var3 = 1.0F - (float)((double)var3 / var6);
      } else {
         var3 = 0.0F;
      }

      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
      if (var3 > 0.0F) {
         var3 = Mth.clamp(var3, 0.0F, 1.0F);
         RenderSystem.setShaderColor(0.0F, var3, var3, 1.0F);
      } else {
         float var8 = this.vignetteBrightness;
         var8 = Mth.clamp(var8, 0.0F, 1.0F);
         RenderSystem.setShaderColor(var8, var8, var8, 1.0F);
      }

      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, VIGNETTE_LOCATION);
      Tesselator var10 = Tesselator.getInstance();
      BufferBuilder var9 = var10.getBuilder();
      var9.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      var9.vertex(0.0D, (double)this.screenHeight, -90.0D).uv(0.0F, 1.0F).endVertex();
      var9.vertex((double)this.screenWidth, (double)this.screenHeight, -90.0D).uv(1.0F, 1.0F).endVertex();
      var9.vertex((double)this.screenWidth, 0.0D, -90.0D).uv(1.0F, 0.0F).endVertex();
      var9.vertex(0.0D, 0.0D, -90.0D).uv(0.0F, 0.0F).endVertex();
      var10.end();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
   }

   private void renderPortalOverlay(float var1) {
      if (var1 < 1.0F) {
         var1 *= var1;
         var1 *= var1;
         var1 = var1 * 0.8F + 0.2F;
      }

      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, var1);
      RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      TextureAtlasSprite var2 = this.minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
      float var3 = var2.getU0();
      float var4 = var2.getV0();
      float var5 = var2.getU1();
      float var6 = var2.getV1();
      Tesselator var7 = Tesselator.getInstance();
      BufferBuilder var8 = var7.getBuilder();
      var8.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      var8.vertex(0.0D, (double)this.screenHeight, -90.0D).uv(var3, var6).endVertex();
      var8.vertex((double)this.screenWidth, (double)this.screenHeight, -90.0D).uv(var5, var6).endVertex();
      var8.vertex((double)this.screenWidth, 0.0D, -90.0D).uv(var5, var4).endVertex();
      var8.vertex(0.0D, 0.0D, -90.0D).uv(var3, var4).endVertex();
      var7.end();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderSlot(int var1, int var2, float var3, Player var4, ItemStack var5, int var6) {
      if (!var5.isEmpty()) {
         PoseStack var7 = RenderSystem.getModelViewStack();
         float var8 = (float)var5.getPopTime() - var3;
         if (var8 > 0.0F) {
            float var9 = 1.0F + var8 / 5.0F;
            var7.pushPose();
            var7.translate((double)(var1 + 8), (double)(var2 + 12), 0.0D);
            var7.scale(1.0F / var9, (var9 + 1.0F) / 2.0F, 1.0F);
            var7.translate((double)(-(var1 + 8)), (double)(-(var2 + 12)), 0.0D);
            RenderSystem.applyModelViewMatrix();
         }

         this.itemRenderer.renderAndDecorateItem(var4, var5, var1, var2, var6);
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         if (var8 > 0.0F) {
            var7.popPose();
            RenderSystem.applyModelViewMatrix();
         }

         this.itemRenderer.renderGuiItemDecorations(this.minecraft.font, var5, var1, var2);
      }
   }

   public void tick() {
      if (this.overlayMessageTime > 0) {
         --this.overlayMessageTime;
      }

      if (this.titleTime > 0) {
         --this.titleTime;
         if (this.titleTime <= 0) {
            this.title = null;
            this.subtitle = null;
         }
      }

      ++this.tickCount;
      Entity var1 = this.minecraft.getCameraEntity();
      if (var1 != null) {
         this.updateVignetteBrightness(var1);
      }

      if (this.minecraft.player != null) {
         ItemStack var2 = this.minecraft.player.getInventory().getSelected();
         if (var2.isEmpty()) {
            this.toolHighlightTimer = 0;
         } else if (!this.lastToolHighlight.isEmpty() && var2.is(this.lastToolHighlight.getItem()) && var2.getHoverName().equals(this.lastToolHighlight.getHoverName())) {
            if (this.toolHighlightTimer > 0) {
               --this.toolHighlightTimer;
            }
         } else {
            this.toolHighlightTimer = 40;
         }

         this.lastToolHighlight = var2;
      }

   }

   public void setNowPlaying(Component var1) {
      this.setOverlayMessage(new TranslatableComponent("record.nowPlaying", new Object[]{var1}), true);
   }

   public void setOverlayMessage(Component var1, boolean var2) {
      this.overlayMessageString = var1;
      this.overlayMessageTime = 60;
      this.animateOverlayMessageColor = var2;
   }

   public void setTimes(int var1, int var2, int var3) {
      if (var1 >= 0) {
         this.titleFadeInTime = var1;
      }

      if (var2 >= 0) {
         this.titleStayTime = var2;
      }

      if (var3 >= 0) {
         this.titleFadeOutTime = var3;
      }

      if (this.titleTime > 0) {
         this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
      }

   }

   public void setSubtitle(Component var1) {
      this.subtitle = var1;
   }

   public void setTitle(Component var1) {
      this.title = var1;
      this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
   }

   public void clear() {
      this.title = null;
      this.subtitle = null;
      this.titleTime = 0;
   }

   public UUID guessChatUUID(Component var1) {
      String var2 = StringDecomposer.getPlainText(var1);
      String var3 = StringUtils.substringBetween(var2, "<", ">");
      return var3 == null ? Util.NIL_UUID : this.minecraft.getPlayerSocialManager().getDiscoveredUUID(var3);
   }

   public void handleChat(ChatType var1, Component var2, UUID var3) {
      if (!this.minecraft.isBlocked(var3)) {
         if (!this.minecraft.options.hideMatchedNames || !this.minecraft.isBlocked(this.guessChatUUID(var2))) {
            Iterator var4 = ((List)this.chatListeners.get(var1)).iterator();

            while(var4.hasNext()) {
               ChatListener var5 = (ChatListener)var4.next();
               var5.handle(var1, var2, var3);
            }

         }
      }
   }

   public ChatComponent getChat() {
      return this.chat;
   }

   public int getGuiTicks() {
      return this.tickCount;
   }

   public Font getFont() {
      return this.minecraft.font;
   }

   public SpectatorGui getSpectatorGui() {
      return this.spectatorGui;
   }

   public PlayerTabOverlay getTabList() {
      return this.tabList;
   }

   public void onDisconnected() {
      this.tabList.reset();
      this.bossOverlay.reset();
      this.minecraft.getToasts().clear();
      this.minecraft.options.renderDebug = false;
      this.chat.clearMessages(true);
   }

   public BossHealthOverlay getBossOverlay() {
      return this.bossOverlay;
   }

   public void clearCache() {
      this.debugScreen.clearChunkCache();
   }

   private static enum HeartType {
      CONTAINER(0, false),
      NORMAL(2, true),
      POISIONED(4, true),
      WITHERED(6, true),
      ABSORBING(8, false),
      FROZEN(9, false);

      private final int index;
      private final boolean canBlink;

      private HeartType(int var3, boolean var4) {
         this.index = var3;
         this.canBlink = var4;
      }

      public int getX(boolean var1, boolean var2) {
         int var3;
         if (this == CONTAINER) {
            var3 = var2 ? 1 : 0;
         } else {
            int var4 = var1 ? 1 : 0;
            int var5 = this.canBlink && var2 ? 2 : 0;
            var3 = var4 + var5;
         }

         return 16 + (this.index * 2 + var3) * 9;
      }

      static Gui.HeartType forPlayer(Player var0) {
         Gui.HeartType var1;
         if (var0.hasEffect(MobEffects.POISON)) {
            var1 = POISIONED;
         } else if (var0.hasEffect(MobEffects.WITHER)) {
            var1 = WITHERED;
         } else if (var0.isFullyFrozen()) {
            var1 = FROZEN;
         } else {
            var1 = NORMAL;
         }

         return var1;
      }

      // $FF: synthetic method
      private static Gui.HeartType[] $values() {
         return new Gui.HeartType[]{CONTAINER, NORMAL, POISIONED, WITHERED, ABSORBING, FROZEN};
      }
   }
}
