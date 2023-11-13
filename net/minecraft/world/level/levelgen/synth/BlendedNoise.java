package net.minecraft.world.level.levelgen.synth;

import java.util.stream.IntStream;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.RandomSource;

public class BlendedNoise {
   private final PerlinNoise minLimitNoise;
   private final PerlinNoise maxLimitNoise;
   private final PerlinNoise mainNoise;

   public BlendedNoise(PerlinNoise var1, PerlinNoise var2, PerlinNoise var3) {
      this.minLimitNoise = var1;
      this.maxLimitNoise = var2;
      this.mainNoise = var3;
   }

   public BlendedNoise(RandomSource var1) {
      this(new PerlinNoise(var1, IntStream.rangeClosed(-15, 0)), new PerlinNoise(var1, IntStream.rangeClosed(-15, 0)), new PerlinNoise(var1, IntStream.rangeClosed(-7, 0)));
   }

   public double sampleAndClampNoise(int var1, int var2, int var3, double var4, double var6, double var8, double var10) {
      double var12 = 0.0D;
      double var14 = 0.0D;
      double var16 = 0.0D;
      boolean var18 = true;
      double var19 = 1.0D;

      for(int var21 = 0; var21 < 8; ++var21) {
         ImprovedNoise var22 = this.mainNoise.getOctaveNoise(var21);
         if (var22 != null) {
            var16 += var22.noise(PerlinNoise.wrap((double)var1 * var8 * var19), PerlinNoise.wrap((double)var2 * var10 * var19), PerlinNoise.wrap((double)var3 * var8 * var19), var10 * var19, (double)var2 * var10 * var19) / var19;
         }

         var19 /= 2.0D;
      }

      double var35 = (var16 / 10.0D + 1.0D) / 2.0D;
      boolean var23 = var35 >= 1.0D;
      boolean var24 = var35 <= 0.0D;
      var19 = 1.0D;

      for(int var25 = 0; var25 < 16; ++var25) {
         double var26 = PerlinNoise.wrap((double)var1 * var4 * var19);
         double var28 = PerlinNoise.wrap((double)var2 * var6 * var19);
         double var30 = PerlinNoise.wrap((double)var3 * var4 * var19);
         double var32 = var6 * var19;
         ImprovedNoise var34;
         if (!var23) {
            var34 = this.minLimitNoise.getOctaveNoise(var25);
            if (var34 != null) {
               var12 += var34.noise(var26, var28, var30, var32, (double)var2 * var32) / var19;
            }
         }

         if (!var24) {
            var34 = this.maxLimitNoise.getOctaveNoise(var25);
            if (var34 != null) {
               var14 += var34.noise(var26, var28, var30, var32, (double)var2 * var32) / var19;
            }
         }

         var19 /= 2.0D;
      }

      return Mth.clampedLerp(var12 / 512.0D, var14 / 512.0D, var35);
   }
}
