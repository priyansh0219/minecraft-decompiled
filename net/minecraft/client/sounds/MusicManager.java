package net.minecraft.client.sounds;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.Music;
import net.minecraft.util.Mth;

public class MusicManager {
   private static final int STARTING_DELAY = 100;
   private final Random random = new Random();
   private final Minecraft minecraft;
   @Nullable
   private SoundInstance currentMusic;
   private int nextSongDelay = 100;

   public MusicManager(Minecraft var1) {
      this.minecraft = var1;
   }

   public void tick() {
      Music var1 = this.minecraft.getSituationalMusic();
      if (this.currentMusic != null) {
         if (!var1.getEvent().getLocation().equals(this.currentMusic.getLocation()) && var1.replaceCurrentMusic()) {
            this.minecraft.getSoundManager().stop(this.currentMusic);
            this.nextSongDelay = Mth.nextInt(this.random, 0, var1.getMinDelay() / 2);
         }

         if (!this.minecraft.getSoundManager().isActive(this.currentMusic)) {
            this.currentMusic = null;
            this.nextSongDelay = Math.min(this.nextSongDelay, Mth.nextInt(this.random, var1.getMinDelay(), var1.getMaxDelay()));
         }
      }

      this.nextSongDelay = Math.min(this.nextSongDelay, var1.getMaxDelay());
      if (this.currentMusic == null && this.nextSongDelay-- <= 0) {
         this.startPlaying(var1);
      }

   }

   public void startPlaying(Music var1) {
      this.currentMusic = SimpleSoundInstance.forMusic(var1.getEvent());
      if (this.currentMusic.getSound() != SoundManager.EMPTY_SOUND) {
         this.minecraft.getSoundManager().play(this.currentMusic);
      }

      this.nextSongDelay = Integer.MAX_VALUE;
   }

   public void stopPlaying() {
      if (this.currentMusic != null) {
         this.minecraft.getSoundManager().stop(this.currentMusic);
         this.currentMusic = null;
      }

      this.nextSongDelay += 100;
   }

   public boolean isPlayingMusic(Music var1) {
      return this.currentMusic == null ? false : var1.getEvent().getLocation().equals(this.currentMusic.getLocation());
   }
}
