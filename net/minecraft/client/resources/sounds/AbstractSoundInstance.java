package net.minecraft.client.resources.sounds;

import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public abstract class AbstractSoundInstance implements SoundInstance {
   protected Sound sound;
   protected final SoundSource source;
   protected final ResourceLocation location;
   protected float volume;
   protected float pitch;
   protected double x;
   protected double y;
   protected double z;
   protected boolean looping;
   protected int delay;
   protected SoundInstance.Attenuation attenuation;
   protected boolean relative;

   protected AbstractSoundInstance(SoundEvent var1, SoundSource var2) {
      this(var1.getLocation(), var2);
   }

   protected AbstractSoundInstance(ResourceLocation var1, SoundSource var2) {
      this.volume = 1.0F;
      this.pitch = 1.0F;
      this.attenuation = SoundInstance.Attenuation.LINEAR;
      this.location = var1;
      this.source = var2;
   }

   public ResourceLocation getLocation() {
      return this.location;
   }

   public WeighedSoundEvents resolve(SoundManager var1) {
      WeighedSoundEvents var2 = var1.getSoundEvent(this.location);
      if (var2 == null) {
         this.sound = SoundManager.EMPTY_SOUND;
      } else {
         this.sound = var2.getSound();
      }

      return var2;
   }

   public Sound getSound() {
      return this.sound;
   }

   public SoundSource getSource() {
      return this.source;
   }

   public boolean isLooping() {
      return this.looping;
   }

   public int getDelay() {
      return this.delay;
   }

   public float getVolume() {
      return this.volume * this.sound.getVolume();
   }

   public float getPitch() {
      return this.pitch * this.sound.getPitch();
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public SoundInstance.Attenuation getAttenuation() {
      return this.attenuation;
   }

   public boolean isRelative() {
      return this.relative;
   }

   public String toString() {
      return "SoundInstance[" + this.location + "]";
   }
}
