package net.minecraft.client.sounds;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SoundManager extends SimplePreparableReloadListener<SoundManager.Preparations> {
   public static final Sound EMPTY_SOUND;
   static final Logger LOGGER;
   private static final String SOUNDS_PATH = "sounds.json";
   private static final Gson GSON;
   private static final TypeToken<Map<String, SoundEventRegistration>> SOUND_EVENT_REGISTRATION_TYPE;
   private final Map<ResourceLocation, WeighedSoundEvents> registry = Maps.newHashMap();
   private final SoundEngine soundEngine;

   public SoundManager(ResourceManager var1, Options var2) {
      this.soundEngine = new SoundEngine(this, var2, var1);
   }

   protected SoundManager.Preparations prepare(ResourceManager var1, ProfilerFiller var2) {
      SoundManager.Preparations var3 = new SoundManager.Preparations();
      var2.startTick();

      for(Iterator var4 = var1.getNamespaces().iterator(); var4.hasNext(); var2.pop()) {
         String var5 = (String)var4.next();
         var2.push(var5);

         try {
            List var6 = var1.getResources(new ResourceLocation(var5, "sounds.json"));

            for(Iterator var7 = var6.iterator(); var7.hasNext(); var2.pop()) {
               Resource var8 = (Resource)var7.next();
               var2.push(var8.getSourceName());

               try {
                  InputStream var9 = var8.getInputStream();

                  try {
                     InputStreamReader var10 = new InputStreamReader(var9, StandardCharsets.UTF_8);

                     try {
                        var2.push("parse");
                        Map var11 = (Map)GsonHelper.fromJson(GSON, (Reader)var10, (TypeToken)SOUND_EVENT_REGISTRATION_TYPE);
                        var2.popPush("register");
                        Iterator var12 = var11.entrySet().iterator();

                        while(var12.hasNext()) {
                           Entry var13 = (Entry)var12.next();
                           var3.handleRegistration(new ResourceLocation(var5, (String)var13.getKey()), (SoundEventRegistration)var13.getValue(), var1);
                        }

                        var2.pop();
                     } catch (Throwable var16) {
                        try {
                           var10.close();
                        } catch (Throwable var15) {
                           var16.addSuppressed(var15);
                        }

                        throw var16;
                     }

                     var10.close();
                  } catch (Throwable var17) {
                     if (var9 != null) {
                        try {
                           var9.close();
                        } catch (Throwable var14) {
                           var17.addSuppressed(var14);
                        }
                     }

                     throw var17;
                  }

                  if (var9 != null) {
                     var9.close();
                  }
               } catch (RuntimeException var18) {
                  LOGGER.warn("Invalid {} in resourcepack: '{}'", "sounds.json", var8.getSourceName(), var18);
               }
            }
         } catch (IOException var19) {
         }
      }

      var2.endTick();
      return var3;
   }

   protected void apply(SoundManager.Preparations var1, ResourceManager var2, ProfilerFiller var3) {
      var1.apply(this.registry, this.soundEngine);
      Iterator var4;
      ResourceLocation var5;
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         var4 = this.registry.keySet().iterator();

         while(var4.hasNext()) {
            var5 = (ResourceLocation)var4.next();
            WeighedSoundEvents var6 = (WeighedSoundEvents)this.registry.get(var5);
            if (var6.getSubtitle() instanceof TranslatableComponent) {
               String var7 = ((TranslatableComponent)var6.getSubtitle()).getKey();
               if (!I18n.exists(var7) && Registry.SOUND_EVENT.containsKey(var5)) {
                  LOGGER.error("Missing subtitle {} for sound event: {}", var7, var5);
               }
            }
         }
      }

      if (LOGGER.isDebugEnabled()) {
         var4 = this.registry.keySet().iterator();

         while(var4.hasNext()) {
            var5 = (ResourceLocation)var4.next();
            if (!Registry.SOUND_EVENT.containsKey(var5)) {
               LOGGER.debug("Not having sound event for: {}", var5);
            }
         }
      }

      this.soundEngine.reload();
   }

   static boolean validateSoundResource(Sound var0, ResourceLocation var1, ResourceManager var2) {
      ResourceLocation var3 = var0.getPath();
      if (!var2.hasResource(var3)) {
         LOGGER.warn("File {} does not exist, cannot add it to event {}", var3, var1);
         return false;
      } else {
         return true;
      }
   }

   @Nullable
   public WeighedSoundEvents getSoundEvent(ResourceLocation var1) {
      return (WeighedSoundEvents)this.registry.get(var1);
   }

   public Collection<ResourceLocation> getAvailableSounds() {
      return this.registry.keySet();
   }

   public void queueTickingSound(TickableSoundInstance var1) {
      this.soundEngine.queueTickingSound(var1);
   }

   public void play(SoundInstance var1) {
      this.soundEngine.play(var1);
   }

   public void playDelayed(SoundInstance var1, int var2) {
      this.soundEngine.playDelayed(var1, var2);
   }

   public void updateSource(Camera var1) {
      this.soundEngine.updateSource(var1);
   }

   public void pause() {
      this.soundEngine.pause();
   }

   public void stop() {
      this.soundEngine.stopAll();
   }

   public void destroy() {
      this.soundEngine.destroy();
   }

   public void tick(boolean var1) {
      this.soundEngine.tick(var1);
   }

   public void resume() {
      this.soundEngine.resume();
   }

   public void updateSourceVolume(SoundSource var1, float var2) {
      if (var1 == SoundSource.MASTER && var2 <= 0.0F) {
         this.stop();
      }

      this.soundEngine.updateCategoryVolume(var1, var2);
   }

   public void stop(SoundInstance var1) {
      this.soundEngine.stop(var1);
   }

   public boolean isActive(SoundInstance var1) {
      return this.soundEngine.isActive(var1);
   }

   public void addListener(SoundEventListener var1) {
      this.soundEngine.addEventListener(var1);
   }

   public void removeListener(SoundEventListener var1) {
      this.soundEngine.removeEventListener(var1);
   }

   public void stop(@Nullable ResourceLocation var1, @Nullable SoundSource var2) {
      this.soundEngine.stop(var1, var2);
   }

   public String getDebugString() {
      return this.soundEngine.getDebugString();
   }

   // $FF: synthetic method
   protected Object prepare(ResourceManager var1, ProfilerFiller var2) {
      return this.prepare(var1, var2);
   }

   static {
      EMPTY_SOUND = new Sound("meta:missing_sound", 1.0F, 1.0F, 1, Sound.Type.FILE, false, false, 16);
      LOGGER = LogManager.getLogger();
      GSON = (new GsonBuilder()).registerTypeHierarchyAdapter(Component.class, new Component.Serializer()).registerTypeAdapter(SoundEventRegistration.class, new SoundEventRegistrationSerializer()).create();
      SOUND_EVENT_REGISTRATION_TYPE = new TypeToken<Map<String, SoundEventRegistration>>() {
      };
   }

   protected static class Preparations {
      final Map<ResourceLocation, WeighedSoundEvents> registry = Maps.newHashMap();

      void handleRegistration(ResourceLocation var1, SoundEventRegistration var2, ResourceManager var3) {
         WeighedSoundEvents var4 = (WeighedSoundEvents)this.registry.get(var1);
         boolean var5 = var4 == null;
         if (var5 || var2.isReplace()) {
            if (!var5) {
               SoundManager.LOGGER.debug("Replaced sound event location {}", var1);
            }

            var4 = new WeighedSoundEvents(var1, var2.getSubtitle());
            this.registry.put(var1, var4);
         }

         Iterator var6 = var2.getSounds().iterator();

         while(var6.hasNext()) {
            Sound var7 = (Sound)var6.next();
            ResourceLocation var8 = var7.getLocation();
            Object var9;
            switch(var7.getType()) {
            case FILE:
               if (!SoundManager.validateSoundResource(var7, var1, var3)) {
                  continue;
               }

               var9 = var7;
               break;
            case SOUND_EVENT:
               var9 = new Weighted<Sound>(var8, var7) {
                  // $FF: synthetic field
                  final ResourceLocation val$soundLocation;
                  // $FF: synthetic field
                  final Sound val$sound;
                  // $FF: synthetic field
                  final SoundManager.Preparations this$0;

                  {
                     this.this$0 = var1;
                     this.val$soundLocation = var2;
                     this.val$sound = var3;
                  }

                  public int getWeight() {
                     WeighedSoundEvents var1 = (WeighedSoundEvents)this.this$0.registry.get(this.val$soundLocation);
                     return var1 == null ? 0 : var1.getWeight();
                  }

                  public Sound getSound() {
                     WeighedSoundEvents var1 = (WeighedSoundEvents)this.this$0.registry.get(this.val$soundLocation);
                     if (var1 == null) {
                        return SoundManager.EMPTY_SOUND;
                     } else {
                        Sound var2 = var1.getSound();
                        return new Sound(var2.getLocation().toString(), var2.getVolume() * this.val$sound.getVolume(), var2.getPitch() * this.val$sound.getPitch(), this.val$sound.getWeight(), Sound.Type.FILE, var2.shouldStream() || this.val$sound.shouldStream(), var2.shouldPreload(), var2.getAttenuationDistance());
                     }
                  }

                  public void preloadIfRequired(SoundEngine var1) {
                     WeighedSoundEvents var2 = (WeighedSoundEvents)this.this$0.registry.get(this.val$soundLocation);
                     if (var2 != null) {
                        var2.preloadIfRequired(var1);
                     }
                  }

                  // $FF: synthetic method
                  public Object getSound() {
                     return this.getSound();
                  }
               };
               break;
            default:
               throw new IllegalStateException("Unknown SoundEventRegistration type: " + var7.getType());
            }

            var4.addSound((Weighted)var9);
         }

      }

      public void apply(Map<ResourceLocation, WeighedSoundEvents> var1, SoundEngine var2) {
         var1.clear();
         Iterator var3 = this.registry.entrySet().iterator();

         while(var3.hasNext()) {
            Entry var4 = (Entry)var3.next();
            var1.put((ResourceLocation)var4.getKey(), (WeighedSoundEvents)var4.getValue());
            ((WeighedSoundEvents)var4.getValue()).preloadIfRequired(var2);
         }

      }
   }
}
