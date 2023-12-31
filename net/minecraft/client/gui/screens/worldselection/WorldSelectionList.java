package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldSelectionList extends ObjectSelectionList<WorldSelectionList.WorldListEntry> {
   static final Logger LOGGER = LogManager.getLogger();
   static final DateFormat DATE_FORMAT = new SimpleDateFormat();
   static final ResourceLocation ICON_MISSING = new ResourceLocation("textures/misc/unknown_server.png");
   static final ResourceLocation ICON_OVERLAY_LOCATION = new ResourceLocation("textures/gui/world_selection.png");
   static final Component FROM_NEWER_TOOLTIP_1;
   static final Component FROM_NEWER_TOOLTIP_2;
   static final Component SNAPSHOT_TOOLTIP_1;
   static final Component SNAPSHOT_TOOLTIP_2;
   static final Component WORLD_LOCKED_TOOLTIP;
   static final Component WORLD_PRE_WORLDHEIGHT_TOOLTIP;
   private final SelectWorldScreen screen;
   @Nullable
   private List<LevelSummary> cachedList;

   public WorldSelectionList(SelectWorldScreen var1, Minecraft var2, int var3, int var4, int var5, int var6, int var7, Supplier<String> var8, @Nullable WorldSelectionList var9) {
      super(var2, var3, var4, var5, var6, var7);
      this.screen = var1;
      if (var9 != null) {
         this.cachedList = var9.cachedList;
      }

      this.refreshList(var8, false);
   }

   public void refreshList(Supplier<String> var1, boolean var2) {
      this.clearEntries();
      LevelStorageSource var3 = this.minecraft.getLevelSource();
      if (this.cachedList == null || var2) {
         try {
            this.cachedList = var3.getLevelList();
         } catch (LevelStorageException var7) {
            LOGGER.error("Couldn't load level list", var7);
            this.minecraft.setScreen(new ErrorScreen(new TranslatableComponent("selectWorld.unable_to_load"), new TextComponent(var7.getMessage())));
            return;
         }

         Collections.sort(this.cachedList);
      }

      if (this.cachedList.isEmpty()) {
         this.minecraft.setScreen(CreateWorldScreen.create((Screen)null));
      } else {
         String var4 = ((String)var1.get()).toLowerCase(Locale.ROOT);
         Iterator var5 = this.cachedList.iterator();

         while(true) {
            LevelSummary var6;
            do {
               if (!var5.hasNext()) {
                  return;
               }

               var6 = (LevelSummary)var5.next();
            } while(!var6.getLevelName().toLowerCase(Locale.ROOT).contains(var4) && !var6.getLevelId().toLowerCase(Locale.ROOT).contains(var4));

            this.addEntry(new WorldSelectionList.WorldListEntry(this, var6));
         }
      }
   }

   protected int getScrollbarPosition() {
      return super.getScrollbarPosition() + 20;
   }

   public int getRowWidth() {
      return super.getRowWidth() + 50;
   }

   protected boolean isFocused() {
      return this.screen.getFocused() == this;
   }

   public void setSelected(@Nullable WorldSelectionList.WorldListEntry var1) {
      super.setSelected(var1);
      this.screen.updateButtonStatus(var1 != null && !var1.summary.isDisabled());
   }

   protected void moveSelection(AbstractSelectionList.SelectionDirection var1) {
      this.moveSelection(var1, (var0) -> {
         return !var0.summary.isDisabled();
      });
   }

   public Optional<WorldSelectionList.WorldListEntry> getSelectedOpt() {
      return Optional.ofNullable((WorldSelectionList.WorldListEntry)this.getSelected());
   }

   public SelectWorldScreen getScreen() {
      return this.screen;
   }

   static {
      FROM_NEWER_TOOLTIP_1 = (new TranslatableComponent("selectWorld.tooltip.fromNewerVersion1")).withStyle(ChatFormatting.RED);
      FROM_NEWER_TOOLTIP_2 = (new TranslatableComponent("selectWorld.tooltip.fromNewerVersion2")).withStyle(ChatFormatting.RED);
      SNAPSHOT_TOOLTIP_1 = (new TranslatableComponent("selectWorld.tooltip.snapshot1")).withStyle(ChatFormatting.GOLD);
      SNAPSHOT_TOOLTIP_2 = (new TranslatableComponent("selectWorld.tooltip.snapshot2")).withStyle(ChatFormatting.GOLD);
      WORLD_LOCKED_TOOLTIP = (new TranslatableComponent("selectWorld.locked")).withStyle(ChatFormatting.RED);
      WORLD_PRE_WORLDHEIGHT_TOOLTIP = (new TranslatableComponent("selectWorld.pre_worldheight")).withStyle(ChatFormatting.RED);
   }

   public final class WorldListEntry extends ObjectSelectionList.Entry<WorldSelectionList.WorldListEntry> implements AutoCloseable {
      private static final int ICON_WIDTH = 32;
      private static final int ICON_HEIGHT = 32;
      private static final int ICON_OVERLAY_X_JOIN = 0;
      private static final int ICON_OVERLAY_X_JOIN_WITH_NOTIFY = 32;
      private static final int ICON_OVERLAY_X_WARNING = 64;
      private static final int ICON_OVERLAY_X_ERROR = 96;
      private static final int ICON_OVERLAY_Y_UNSELECTED = 0;
      private static final int ICON_OVERLAY_Y_SELECTED = 32;
      private final Minecraft minecraft;
      private final SelectWorldScreen screen;
      final LevelSummary summary;
      private final ResourceLocation iconLocation;
      private File iconFile;
      @Nullable
      private final DynamicTexture icon;
      private long lastClickTime;

      public WorldListEntry(WorldSelectionList var2, LevelSummary var3) {
         this.screen = var2.getScreen();
         this.summary = var3;
         this.minecraft = Minecraft.getInstance();
         String var4 = var3.getLevelId();
         String var10004 = Util.sanitizeName(var4, ResourceLocation::validPathChar);
         this.iconLocation = new ResourceLocation("minecraft", "worlds/" + var10004 + "/" + Hashing.sha1().hashUnencodedChars(var4) + "/icon");
         this.iconFile = var3.getIcon();
         if (!this.iconFile.isFile()) {
            this.iconFile = null;
         }

         this.icon = this.loadServerIcon();
      }

      public Component getNarration() {
         TranslatableComponent var1 = new TranslatableComponent("narrator.select.world", new Object[]{this.summary.getLevelName(), new Date(this.summary.getLastPlayed()), this.summary.isHardcore() ? new TranslatableComponent("gameMode.hardcore") : new TranslatableComponent("gameMode." + this.summary.getGameMode().getName()), this.summary.hasCheats() ? new TranslatableComponent("selectWorld.cheats") : TextComponent.EMPTY, this.summary.getWorldVersionName()});
         Object var2;
         if (this.summary.isLocked()) {
            var2 = CommonComponents.joinForNarration(var1, WorldSelectionList.WORLD_LOCKED_TOOLTIP);
         } else if (this.summary.isIncompatibleWorldHeight()) {
            var2 = CommonComponents.joinForNarration(var1, WorldSelectionList.WORLD_PRE_WORLDHEIGHT_TOOLTIP);
         } else {
            var2 = var1;
         }

         return new TranslatableComponent("narrator.select", new Object[]{var2});
      }

      public void render(PoseStack var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         String var11 = this.summary.getLevelName();
         String var10000 = this.summary.getLevelId();
         String var12 = var10000 + " (" + WorldSelectionList.DATE_FORMAT.format(new Date(this.summary.getLastPlayed())) + ")";
         if (StringUtils.isEmpty(var11)) {
            var10000 = I18n.get("selectWorld.world");
            var11 = var10000 + " " + (var2 + 1);
         }

         Component var13 = this.summary.getInfo();
         this.minecraft.font.draw(var1, var11, (float)(var4 + 32 + 3), (float)(var3 + 1), 16777215);
         Font var17 = this.minecraft.font;
         float var10003 = (float)(var4 + 32 + 3);
         Objects.requireNonNull(this.minecraft.font);
         var17.draw(var1, var12, var10003, (float)(var3 + 9 + 3), 8421504);
         var17 = this.minecraft.font;
         var10003 = (float)(var4 + 32 + 3);
         Objects.requireNonNull(this.minecraft.font);
         int var10004 = var3 + 9;
         Objects.requireNonNull(this.minecraft.font);
         var17.draw(var1, var13, var10003, (float)(var10004 + 9 + 3), 8421504);
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShaderTexture(0, this.icon != null ? this.iconLocation : WorldSelectionList.ICON_MISSING);
         RenderSystem.enableBlend();
         GuiComponent.blit(var1, var4, var3, 0.0F, 0.0F, 32, 32, 32, 32);
         RenderSystem.disableBlend();
         if (this.minecraft.options.touchscreen || var9) {
            RenderSystem.setShaderTexture(0, WorldSelectionList.ICON_OVERLAY_LOCATION);
            GuiComponent.fill(var1, var4, var3, var4 + 32, var3 + 32, -1601138544);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            int var14 = var7 - var4;
            boolean var15 = var14 < 32;
            int var16 = var15 ? 32 : 0;
            if (this.summary.isLocked()) {
               GuiComponent.blit(var1, var4, var3, 96.0F, (float)var16, 32, 32, 256, 256);
               if (var15) {
                  this.screen.setToolTip(this.minecraft.font.split(WorldSelectionList.WORLD_LOCKED_TOOLTIP, 175));
               }
            } else if (this.summary.isIncompatibleWorldHeight()) {
               GuiComponent.blit(var1, var4, var3, 96.0F, 32.0F, 32, 32, 256, 256);
               if (var15) {
                  this.screen.setToolTip(this.minecraft.font.split(WorldSelectionList.WORLD_PRE_WORLDHEIGHT_TOOLTIP, 175));
               }
            } else if (this.summary.markVersionInList()) {
               GuiComponent.blit(var1, var4, var3, 32.0F, (float)var16, 32, 32, 256, 256);
               if (this.summary.askToOpenWorld()) {
                  GuiComponent.blit(var1, var4, var3, 96.0F, (float)var16, 32, 32, 256, 256);
                  if (var15) {
                     this.screen.setToolTip(ImmutableList.of(WorldSelectionList.FROM_NEWER_TOOLTIP_1.getVisualOrderText(), WorldSelectionList.FROM_NEWER_TOOLTIP_2.getVisualOrderText()));
                  }
               } else if (!SharedConstants.getCurrentVersion().isStable()) {
                  GuiComponent.blit(var1, var4, var3, 64.0F, (float)var16, 32, 32, 256, 256);
                  if (var15) {
                     this.screen.setToolTip(ImmutableList.of(WorldSelectionList.SNAPSHOT_TOOLTIP_1.getVisualOrderText(), WorldSelectionList.SNAPSHOT_TOOLTIP_2.getVisualOrderText()));
                  }
               }
            } else {
               GuiComponent.blit(var1, var4, var3, 0.0F, (float)var16, 32, 32, 256, 256);
            }
         }

      }

      public boolean mouseClicked(double var1, double var3, int var5) {
         if (this.summary.isDisabled()) {
            return true;
         } else {
            WorldSelectionList.this.setSelected(this);
            this.screen.updateButtonStatus(WorldSelectionList.this.getSelectedOpt().isPresent());
            if (var1 - (double)WorldSelectionList.this.getRowLeft() <= 32.0D) {
               this.joinWorld();
               return true;
            } else if (Util.getMillis() - this.lastClickTime < 250L) {
               this.joinWorld();
               return true;
            } else {
               this.lastClickTime = Util.getMillis();
               return false;
            }
         }
      }

      public void joinWorld() {
         if (!this.summary.isDisabled()) {
            LevelSummary.BackupStatus var1 = this.summary.backupStatus();
            if (var1.shouldBackup()) {
               String var2 = "selectWorld.backupQuestion." + var1.getTranslationKey();
               String var3 = "selectWorld.backupWarning." + var1.getTranslationKey();
               TranslatableComponent var4 = new TranslatableComponent(var2);
               if (var1.isSevere()) {
                  var4.withStyle(ChatFormatting.BOLD, ChatFormatting.RED);
               }

               TranslatableComponent var5 = new TranslatableComponent(var3, new Object[]{this.summary.getWorldVersionName(), SharedConstants.getCurrentVersion().getName()});
               this.minecraft.setScreen(new BackupConfirmScreen(this.screen, (var1x, var2x) -> {
                  if (var1x) {
                     String var3 = this.summary.getLevelId();

                     try {
                        LevelStorageSource.LevelStorageAccess var4 = this.minecraft.getLevelSource().createAccess(var3);

                        try {
                           EditWorldScreen.makeBackupAndShowToast(var4);
                        } catch (Throwable var8) {
                           if (var4 != null) {
                              try {
                                 var4.close();
                              } catch (Throwable var7) {
                                 var8.addSuppressed(var7);
                              }
                           }

                           throw var8;
                        }

                        if (var4 != null) {
                           var4.close();
                        }
                     } catch (IOException var9) {
                        SystemToast.onWorldAccessFailure(this.minecraft, var3);
                        WorldSelectionList.LOGGER.error("Failed to backup level {}", var3, var9);
                     }
                  }

                  this.loadWorld();
               }, var4, var5, false));
            } else if (this.summary.askToOpenWorld()) {
               this.minecraft.setScreen(new ConfirmScreen((var1x) -> {
                  if (var1x) {
                     try {
                        this.loadWorld();
                     } catch (Exception var3) {
                        WorldSelectionList.LOGGER.error("Failure to open 'future world'", var3);
                        this.minecraft.setScreen(new AlertScreen(() -> {
                           this.minecraft.setScreen(this.screen);
                        }, new TranslatableComponent("selectWorld.futureworld.error.title"), new TranslatableComponent("selectWorld.futureworld.error.text")));
                     }
                  } else {
                     this.minecraft.setScreen(this.screen);
                  }

               }, new TranslatableComponent("selectWorld.versionQuestion"), new TranslatableComponent("selectWorld.versionWarning", new Object[]{this.summary.getWorldVersionName()}), new TranslatableComponent("selectWorld.versionJoinButton"), CommonComponents.GUI_CANCEL));
            } else {
               this.loadWorld();
            }

         }
      }

      public void deleteWorld() {
         this.minecraft.setScreen(new ConfirmScreen((var1) -> {
            if (var1) {
               this.minecraft.setScreen(new ProgressScreen(true));
               this.doDeleteWorld();
            }

            this.minecraft.setScreen(this.screen);
         }, new TranslatableComponent("selectWorld.deleteQuestion"), new TranslatableComponent("selectWorld.deleteWarning", new Object[]{this.summary.getLevelName()}), new TranslatableComponent("selectWorld.deleteButton"), CommonComponents.GUI_CANCEL));
      }

      public void doDeleteWorld() {
         LevelStorageSource var1 = this.minecraft.getLevelSource();
         String var2 = this.summary.getLevelId();

         try {
            LevelStorageSource.LevelStorageAccess var3 = var1.createAccess(var2);

            try {
               var3.deleteLevel();
            } catch (Throwable var7) {
               if (var3 != null) {
                  try {
                     var3.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (var3 != null) {
               var3.close();
            }
         } catch (IOException var8) {
            SystemToast.onWorldDeleteFailure(this.minecraft, var2);
            WorldSelectionList.LOGGER.error("Failed to delete world {}", var2, var8);
         }

         WorldSelectionList.this.refreshList(() -> {
            return this.screen.searchBox.getValue();
         }, true);
      }

      public void editWorld() {
         String var1 = this.summary.getLevelId();

         try {
            LevelStorageSource.LevelStorageAccess var2 = this.minecraft.getLevelSource().createAccess(var1);
            this.minecraft.setScreen(new EditWorldScreen((var3x) -> {
               try {
                  var2.close();
               } catch (IOException var5) {
                  WorldSelectionList.LOGGER.error("Failed to unlock level {}", var1, var5);
               }

               if (var3x) {
                  WorldSelectionList.this.refreshList(() -> {
                     return this.screen.searchBox.getValue();
                  }, true);
               }

               this.minecraft.setScreen(this.screen);
            }, var2));
         } catch (IOException var3) {
            SystemToast.onWorldAccessFailure(this.minecraft, var1);
            WorldSelectionList.LOGGER.error("Failed to access level {}", var1, var3);
            WorldSelectionList.this.refreshList(() -> {
               return this.screen.searchBox.getValue();
            }, true);
         }

      }

      public void recreateWorld() {
         this.queueLoadScreen();
         RegistryAccess.RegistryHolder var1 = RegistryAccess.builtin();

         try {
            LevelStorageSource.LevelStorageAccess var2 = this.minecraft.getLevelSource().createAccess(this.summary.getLevelId());

            try {
               Minecraft.ServerStem var3 = this.minecraft.makeServerStem(var1, Minecraft::loadDataPacks, Minecraft::loadWorldData, false, var2);

               try {
                  LevelSettings var4 = var3.worldData().getLevelSettings();
                  DataPackConfig var5 = var4.getDataPackConfig();
                  WorldGenSettings var6 = var3.worldData().worldGenSettings();
                  Path var7 = CreateWorldScreen.createTempDataPackDirFromExistingWorld(var2.getLevelPath(LevelResource.DATAPACK_DIR), this.minecraft);
                  if (var6.isOldCustomizedWorld()) {
                     this.minecraft.setScreen(new ConfirmScreen((var6x) -> {
                        this.minecraft.setScreen((Screen)(var6x ? new CreateWorldScreen(this.screen, var4, var6, var7, var5, var1) : this.screen));
                     }, new TranslatableComponent("selectWorld.recreate.customized.title"), new TranslatableComponent("selectWorld.recreate.customized.text"), CommonComponents.GUI_PROCEED, CommonComponents.GUI_CANCEL));
                  } else {
                     this.minecraft.setScreen(new CreateWorldScreen(this.screen, var4, var6, var7, var5, var1));
                  }
               } catch (Throwable var10) {
                  if (var3 != null) {
                     try {
                        var3.close();
                     } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                     }
                  }

                  throw var10;
               }

               if (var3 != null) {
                  var3.close();
               }
            } catch (Throwable var11) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var8) {
                     var11.addSuppressed(var8);
                  }
               }

               throw var11;
            }

            if (var2 != null) {
               var2.close();
            }
         } catch (Exception var12) {
            WorldSelectionList.LOGGER.error("Unable to recreate world", var12);
            this.minecraft.setScreen(new AlertScreen(() -> {
               this.minecraft.setScreen(this.screen);
            }, new TranslatableComponent("selectWorld.recreate.error.title"), new TranslatableComponent("selectWorld.recreate.error.text")));
         }

      }

      private void loadWorld() {
         this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         if (this.minecraft.getLevelSource().levelExists(this.summary.getLevelId())) {
            this.queueLoadScreen();
            this.minecraft.loadLevel(this.summary.getLevelId());
         }

      }

      private void queueLoadScreen() {
         this.minecraft.forceSetScreen(new GenericDirtMessageScreen(new TranslatableComponent("selectWorld.data_read")));
      }

      @Nullable
      private DynamicTexture loadServerIcon() {
         boolean var1 = this.iconFile != null && this.iconFile.isFile();
         if (var1) {
            try {
               FileInputStream var2 = new FileInputStream(this.iconFile);

               DynamicTexture var5;
               try {
                  NativeImage var3 = NativeImage.read((InputStream)var2);
                  Validate.validState(var3.getWidth() == 64, "Must be 64 pixels wide", new Object[0]);
                  Validate.validState(var3.getHeight() == 64, "Must be 64 pixels high", new Object[0]);
                  DynamicTexture var4 = new DynamicTexture(var3);
                  this.minecraft.getTextureManager().register((ResourceLocation)this.iconLocation, (AbstractTexture)var4);
                  var5 = var4;
               } catch (Throwable var7) {
                  try {
                     var2.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }

                  throw var7;
               }

               var2.close();
               return var5;
            } catch (Throwable var8) {
               WorldSelectionList.LOGGER.error("Invalid icon for world {}", this.summary.getLevelId(), var8);
               this.iconFile = null;
               return null;
            }
         } else {
            this.minecraft.getTextureManager().release(this.iconLocation);
            return null;
         }
      }

      public void close() {
         if (this.icon != null) {
            this.icon.close();
         }

      }

      public String getLevelName() {
         return this.summary.getLevelName();
      }
   }
}
