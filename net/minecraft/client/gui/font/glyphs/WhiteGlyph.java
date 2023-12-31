package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.RawGlyph;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;

public enum WhiteGlyph implements RawGlyph {
   INSTANCE;

   private static final int WIDTH = 5;
   private static final int HEIGHT = 8;
   private static final NativeImage IMAGE_DATA = (NativeImage)Util.make(new NativeImage(NativeImage.Format.RGBA, 5, 8, false), (var0) -> {
      for(int var1 = 0; var1 < 8; ++var1) {
         for(int var2 = 0; var2 < 5; ++var2) {
            boolean var10000;
            if (var2 != 0 && var2 + 1 != 5 && var1 != 0 && var1 + 1 != 8) {
               var10000 = false;
            } else {
               var10000 = true;
            }

            var0.setPixelRGBA(var2, var1, -1);
         }
      }

      var0.untrack();
   });

   public int getPixelWidth() {
      return 5;
   }

   public int getPixelHeight() {
      return 8;
   }

   public float getAdvance() {
      return 6.0F;
   }

   public float getOversample() {
      return 1.0F;
   }

   public void upload(int var1, int var2) {
      IMAGE_DATA.upload(0, var1, var2, false);
   }

   public boolean isColored() {
      return true;
   }

   // $FF: synthetic method
   private static WhiteGlyph[] $values() {
      return new WhiteGlyph[]{INSTANCE};
   }
}
