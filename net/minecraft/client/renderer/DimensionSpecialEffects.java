package net.minecraft.client.renderer;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

public abstract class DimensionSpecialEffects {
   private static final Object2ObjectMap<ResourceLocation, DimensionSpecialEffects> EFFECTS = (Object2ObjectMap)Util.make(new Object2ObjectArrayMap(), (var0) -> {
      DimensionSpecialEffects.OverworldEffects var1 = new DimensionSpecialEffects.OverworldEffects();
      var0.defaultReturnValue(var1);
      var0.put(DimensionType.OVERWORLD_EFFECTS, var1);
      var0.put(DimensionType.NETHER_EFFECTS, new DimensionSpecialEffects.NetherEffects());
      var0.put(DimensionType.END_EFFECTS, new DimensionSpecialEffects.EndEffects());
   });
   private final float[] sunriseCol = new float[4];
   private final float cloudLevel;
   private final boolean hasGround;
   private final DimensionSpecialEffects.SkyType skyType;
   private final boolean forceBrightLightmap;
   private final boolean constantAmbientLight;

   public DimensionSpecialEffects(float var1, boolean var2, DimensionSpecialEffects.SkyType var3, boolean var4, boolean var5) {
      this.cloudLevel = var1;
      this.hasGround = var2;
      this.skyType = var3;
      this.forceBrightLightmap = var4;
      this.constantAmbientLight = var5;
   }

   public static DimensionSpecialEffects forType(DimensionType var0) {
      return (DimensionSpecialEffects)EFFECTS.get(var0.effectsLocation());
   }

   @Nullable
   public float[] getSunriseColor(float var1, float var2) {
      float var3 = 0.4F;
      float var4 = Mth.cos(var1 * 6.2831855F) - 0.0F;
      float var5 = -0.0F;
      if (var4 >= -0.4F && var4 <= 0.4F) {
         float var6 = (var4 - -0.0F) / 0.4F * 0.5F + 0.5F;
         float var7 = 1.0F - (1.0F - Mth.sin(var6 * 3.1415927F)) * 0.99F;
         var7 *= var7;
         this.sunriseCol[0] = var6 * 0.3F + 0.7F;
         this.sunriseCol[1] = var6 * var6 * 0.7F + 0.2F;
         this.sunriseCol[2] = var6 * var6 * 0.0F + 0.2F;
         this.sunriseCol[3] = var7;
         return this.sunriseCol;
      } else {
         return null;
      }
   }

   public float getCloudHeight() {
      return this.cloudLevel;
   }

   public boolean hasGround() {
      return this.hasGround;
   }

   public abstract Vec3 getBrightnessDependentFogColor(Vec3 var1, float var2);

   public abstract boolean isFoggyAt(int var1, int var2);

   public DimensionSpecialEffects.SkyType skyType() {
      return this.skyType;
   }

   public boolean forceBrightLightmap() {
      return this.forceBrightLightmap;
   }

   public boolean constantAmbientLight() {
      return this.constantAmbientLight;
   }

   public static enum SkyType {
      NONE,
      NORMAL,
      END;

      // $FF: synthetic method
      private static DimensionSpecialEffects.SkyType[] $values() {
         return new DimensionSpecialEffects.SkyType[]{NONE, NORMAL, END};
      }
   }

   public static class OverworldEffects extends DimensionSpecialEffects {
      public static final int CLOUD_LEVEL = 128;

      public OverworldEffects() {
         super(128.0F, true, DimensionSpecialEffects.SkyType.NORMAL, false, false);
      }

      public Vec3 getBrightnessDependentFogColor(Vec3 var1, float var2) {
         return var1.multiply((double)(var2 * 0.94F + 0.06F), (double)(var2 * 0.94F + 0.06F), (double)(var2 * 0.91F + 0.09F));
      }

      public boolean isFoggyAt(int var1, int var2) {
         return false;
      }
   }

   public static class NetherEffects extends DimensionSpecialEffects {
      public NetherEffects() {
         super(Float.NaN, true, DimensionSpecialEffects.SkyType.NONE, false, true);
      }

      public Vec3 getBrightnessDependentFogColor(Vec3 var1, float var2) {
         return var1;
      }

      public boolean isFoggyAt(int var1, int var2) {
         return true;
      }
   }

   public static class EndEffects extends DimensionSpecialEffects {
      public EndEffects() {
         super(Float.NaN, false, DimensionSpecialEffects.SkyType.END, true, false);
      }

      public Vec3 getBrightnessDependentFogColor(Vec3 var1, float var2) {
         return var1.scale(0.15000000596046448D);
      }

      public boolean isFoggyAt(int var1, int var2) {
         return false;
      }

      @Nullable
      public float[] getSunriseColor(float var1, float var2) {
         return null;
      }
   }
}
