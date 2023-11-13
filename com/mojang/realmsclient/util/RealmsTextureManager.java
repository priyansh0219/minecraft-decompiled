package com.mojang.realmsclient.util;

import com.google.common.collect.Maps;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.util.UUIDTypeAdapter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RealmsTextureManager {
   private static final Map<String, RealmsTextureManager.RealmsTexture> TEXTURES = Maps.newHashMap();
   static final Map<String, Boolean> SKIN_FETCH_STATUS = Maps.newHashMap();
   static final Map<String, String> FETCHED_SKINS = Maps.newHashMap();
   static final Logger LOGGER = LogManager.getLogger();
   private static final ResourceLocation TEMPLATE_ICON_LOCATION = new ResourceLocation("textures/gui/presets/isles.png");

   public static void bindWorldTemplate(String var0, @Nullable String var1) {
      if (var1 == null) {
         RenderSystem.setShaderTexture(0, TEMPLATE_ICON_LOCATION);
      } else {
         int var2 = getTextureId(var0, var1);
         RenderSystem.setShaderTexture(0, var2);
      }
   }

   public static void withBoundFace(String var0, Runnable var1) {
      bindFace(var0);
      var1.run();
   }

   private static void bindDefaultFace(UUID var0) {
      RenderSystem.setShaderTexture(0, DefaultPlayerSkin.getDefaultSkin(var0));
   }

   private static void bindFace(final String var0) {
      UUID var1 = UUIDTypeAdapter.fromString(var0);
      int var3;
      if (TEXTURES.containsKey(var0)) {
         var3 = ((RealmsTextureManager.RealmsTexture)TEXTURES.get(var0)).textureId;
         RenderSystem.setShaderTexture(0, var3);
      } else if (SKIN_FETCH_STATUS.containsKey(var0)) {
         if (!(Boolean)SKIN_FETCH_STATUS.get(var0)) {
            bindDefaultFace(var1);
         } else if (FETCHED_SKINS.containsKey(var0)) {
            var3 = getTextureId(var0, (String)FETCHED_SKINS.get(var0));
            RenderSystem.setShaderTexture(0, var3);
         } else {
            bindDefaultFace(var1);
         }

      } else {
         SKIN_FETCH_STATUS.put(var0, false);
         bindDefaultFace(var1);
         Thread var2 = new Thread("Realms Texture Downloader") {
            public void run() {
               Map var1 = RealmsUtil.getTextures(var0);
               if (var1.containsKey(Type.SKIN)) {
                  MinecraftProfileTexture var2 = (MinecraftProfileTexture)var1.get(Type.SKIN);
                  String var3 = var2.getUrl();
                  HttpURLConnection var4 = null;
                  RealmsTextureManager.LOGGER.debug("Downloading http texture from {}", var3);

                  try {
                     try {
                        var4 = (HttpURLConnection)(new URL(var3)).openConnection(Minecraft.getInstance().getProxy());
                        var4.setDoInput(true);
                        var4.setDoOutput(false);
                        var4.connect();
                        if (var4.getResponseCode() / 100 != 2) {
                           RealmsTextureManager.SKIN_FETCH_STATUS.remove(var0);
                           return;
                        }

                        BufferedImage var5;
                        try {
                           var5 = ImageIO.read(var4.getInputStream());
                        } catch (Exception var17) {
                           RealmsTextureManager.SKIN_FETCH_STATUS.remove(var0);
                           return;
                        } finally {
                           IOUtils.closeQuietly(var4.getInputStream());
                        }

                        var5 = (new SkinProcessor()).process(var5);
                        ByteArrayOutputStream var6 = new ByteArrayOutputStream();
                        ImageIO.write(var5, "png", var6);
                        RealmsTextureManager.FETCHED_SKINS.put(var0, (new Base64()).encodeToString(var6.toByteArray()));
                        RealmsTextureManager.SKIN_FETCH_STATUS.put(var0, true);
                     } catch (Exception var19) {
                        RealmsTextureManager.LOGGER.error("Couldn't download http texture", var19);
                        RealmsTextureManager.SKIN_FETCH_STATUS.remove(var0);
                     }

                  } finally {
                     if (var4 != null) {
                        var4.disconnect();
                     }

                  }
               } else {
                  RealmsTextureManager.SKIN_FETCH_STATUS.put(var0, true);
               }
            }
         };
         var2.setDaemon(true);
         var2.start();
      }
   }

   private static int getTextureId(String var0, String var1) {
      RealmsTextureManager.RealmsTexture var2 = (RealmsTextureManager.RealmsTexture)TEXTURES.get(var0);
      if (var2 != null && var2.image.equals(var1)) {
         return var2.textureId;
      } else {
         int var3;
         if (var2 != null) {
            var3 = var2.textureId;
         } else {
            var3 = GlStateManager._genTexture();
         }

         IntBuffer var4 = null;
         int var5 = 0;
         int var6 = 0;

         try {
            ByteArrayInputStream var8 = new ByteArrayInputStream((new Base64()).decode(var1));

            BufferedImage var7;
            try {
               var7 = ImageIO.read(var8);
            } finally {
               IOUtils.closeQuietly(var8);
            }

            var5 = var7.getWidth();
            var6 = var7.getHeight();
            int[] var9 = new int[var5 * var6];
            var7.getRGB(0, 0, var5, var6, var9, 0, var5);
            var4 = ByteBuffer.allocateDirect(4 * var5 * var6).order(ByteOrder.nativeOrder()).asIntBuffer();
            var4.put(var9);
            var4.flip();
         } catch (IOException var13) {
            var13.printStackTrace();
         }

         RenderSystem.activeTexture(33984);
         RenderSystem.bindTextureForSetup(var3);
         TextureUtil.initTexture(var4, var5, var6);
         TEXTURES.put(var0, new RealmsTextureManager.RealmsTexture(var1, var3));
         return var3;
      }
   }

   public static class RealmsTexture {
      final String image;
      final int textureId;

      public RealmsTexture(String var1, int var2) {
         this.image = var1;
         this.textureId = var2;
      }
   }
}
