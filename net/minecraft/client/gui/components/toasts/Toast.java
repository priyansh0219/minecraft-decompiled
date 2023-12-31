package net.minecraft.client.gui.components.toasts;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public interface Toast {
   ResourceLocation TEXTURE = new ResourceLocation("textures/gui/toasts.png");
   Object NO_TOKEN = new Object();

   Toast.Visibility render(PoseStack var1, ToastComponent var2, long var3);

   default Object getToken() {
      return NO_TOKEN;
   }

   default int width() {
      return 160;
   }

   default int height() {
      return 32;
   }

   public static enum Visibility {
      SHOW(SoundEvents.UI_TOAST_IN),
      HIDE(SoundEvents.UI_TOAST_OUT);

      private final SoundEvent soundEvent;

      private Visibility(SoundEvent var3) {
         this.soundEvent = var3;
      }

      public void playSound(SoundManager var1) {
         var1.play(SimpleSoundInstance.forUI(this.soundEvent, 1.0F, 1.0F));
      }

      // $FF: synthetic method
      private static Toast.Visibility[] $values() {
         return new Toast.Visibility[]{SHOW, HIDE};
      }
   }
}
