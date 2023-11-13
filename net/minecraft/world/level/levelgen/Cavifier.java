package net.minecraft.world.level.levelgen;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.NoiseUtils;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class Cavifier implements NoiseModifier {
   private final int minCellY;
   private final NormalNoise layerNoiseSource;
   private final NormalNoise pillarNoiseSource;
   private final NormalNoise pillarRarenessModulator;
   private final NormalNoise pillarThicknessModulator;
   private final NormalNoise spaghetti2dNoiseSource;
   private final NormalNoise spaghetti2dElevationModulator;
   private final NormalNoise spaghetti2dRarityModulator;
   private final NormalNoise spaghetti2dThicknessModulator;
   private final NormalNoise spaghetti3dNoiseSource1;
   private final NormalNoise spaghetti3dNoiseSource2;
   private final NormalNoise spaghetti3dRarityModulator;
   private final NormalNoise spaghetti3dThicknessModulator;
   private final NormalNoise spaghettiRoughnessNoise;
   private final NormalNoise spaghettiRoughnessModulator;
   private final NormalNoise caveEntranceNoiseSource;
   private final NormalNoise cheeseNoiseSource;
   private static final int CHEESE_NOISE_RANGE = 128;
   private static final int SURFACE_DENSITY_THRESHOLD = 170;

   public Cavifier(RandomSource var1, int var2) {
      this.minCellY = var2;
      this.pillarNoiseSource = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -7, (double[])(1.0D, 1.0D));
      this.pillarRarenessModulator = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -8, (double[])(1.0D));
      this.pillarThicknessModulator = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -8, (double[])(1.0D));
      this.spaghetti2dNoiseSource = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -7, (double[])(1.0D));
      this.spaghetti2dElevationModulator = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -8, (double[])(1.0D));
      this.spaghetti2dRarityModulator = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -11, (double[])(1.0D));
      this.spaghetti2dThicknessModulator = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -11, (double[])(1.0D));
      this.spaghetti3dNoiseSource1 = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -7, (double[])(1.0D));
      this.spaghetti3dNoiseSource2 = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -7, (double[])(1.0D));
      this.spaghetti3dRarityModulator = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -11, (double[])(1.0D));
      this.spaghetti3dThicknessModulator = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -8, (double[])(1.0D));
      this.spaghettiRoughnessNoise = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -5, (double[])(1.0D));
      this.spaghettiRoughnessModulator = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -8, (double[])(1.0D));
      this.caveEntranceNoiseSource = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -8, (double[])(1.0D, 1.0D, 1.0D));
      this.layerNoiseSource = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -8, (double[])(1.0D));
      this.cheeseNoiseSource = NormalNoise.create(new SimpleRandomSource(var1.nextLong()), -8, (double[])(0.5D, 1.0D, 2.0D, 1.0D, 2.0D, 1.0D, 0.0D, 2.0D, 0.0D));
   }

   public double modifyNoise(double var1, int var3, int var4, int var5) {
      boolean var6 = var1 < 170.0D;
      double var7 = this.spaghettiRoughness(var5, var3, var4);
      double var9 = this.getSpaghetti3d(var5, var3, var4);
      if (var6) {
         return Math.min(var1, (var9 + var7) * 128.0D * 5.0D);
      } else {
         double var11 = this.cheeseNoiseSource.getValue((double)var5, (double)var3 / 1.5D, (double)var4);
         double var13 = Mth.clamp(var11 + 0.25D, -1.0D, 1.0D);
         double var15 = (double)((float)(30 - var3) / 8.0F);
         double var17 = var13 + Mth.clampedLerp(0.5D, 0.0D, var15);
         double var19 = this.getLayerizedCaverns(var5, var3, var4);
         double var21 = this.getSpaghetti2d(var5, var3, var4);
         double var23 = var17 + var19;
         double var25 = Math.min(var23, Math.min(var9, var21) + var7);
         double var27 = Math.max(var25, this.getPillars(var5, var3, var4));
         return 128.0D * Mth.clamp(var27, -1.0D, 1.0D);
      }
   }

   private double addEntrances(double var1, int var3, int var4, int var5) {
      double var6 = this.caveEntranceNoiseSource.getValue((double)(var3 * 2), (double)var4, (double)(var5 * 2));
      var6 = NoiseUtils.biasTowardsExtreme(var6, 1.0D);
      boolean var8 = false;
      double var9 = (double)(var4 - 0) / 40.0D;
      var6 += Mth.clampedLerp(0.5D, var1, var9);
      double var11 = 3.0D;
      var6 = 4.0D * var6 + 3.0D;
      return Math.min(var1, var6);
   }

   private double getPillars(int var1, int var2, int var3) {
      double var4 = 0.0D;
      double var6 = 2.0D;
      double var8 = NoiseUtils.sampleNoiseAndMapToRange(this.pillarRarenessModulator, (double)var1, (double)var2, (double)var3, 0.0D, 2.0D);
      double var10 = 0.0D;
      double var12 = 1.1D;
      double var14 = NoiseUtils.sampleNoiseAndMapToRange(this.pillarThicknessModulator, (double)var1, (double)var2, (double)var3, 0.0D, 1.1D);
      var14 = Math.pow(var14, 3.0D);
      double var16 = 25.0D;
      double var18 = 0.3D;
      double var20 = this.pillarNoiseSource.getValue((double)var1 * 25.0D, (double)var2 * 0.3D, (double)var3 * 25.0D);
      var20 = var14 * (var20 * 2.0D - var8);
      return var20 > 0.03D ? var20 : Double.NEGATIVE_INFINITY;
   }

   private double getLayerizedCaverns(int var1, int var2, int var3) {
      double var4 = this.layerNoiseSource.getValue((double)var1, (double)(var2 * 8), (double)var3);
      return Mth.square(var4) * 4.0D;
   }

   private double getSpaghetti3d(int var1, int var2, int var3) {
      double var4 = this.spaghetti3dRarityModulator.getValue((double)(var1 * 2), (double)var2, (double)(var3 * 2));
      double var6 = Cavifier.QuantizedSpaghettiRarity.getSpaghettiRarity3D(var4);
      double var8 = 0.065D;
      double var10 = 0.088D;
      double var12 = NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti3dThicknessModulator, (double)var1, (double)var2, (double)var3, 0.065D, 0.088D);
      double var14 = sampleWithRarity(this.spaghetti3dNoiseSource1, (double)var1, (double)var2, (double)var3, var6);
      double var16 = Math.abs(var6 * var14) - var12;
      double var18 = sampleWithRarity(this.spaghetti3dNoiseSource2, (double)var1, (double)var2, (double)var3, var6);
      double var20 = Math.abs(var6 * var18) - var12;
      return clampToUnit(Math.max(var16, var20));
   }

   private double getSpaghetti2d(int var1, int var2, int var3) {
      double var4 = this.spaghetti2dRarityModulator.getValue((double)(var1 * 2), (double)var2, (double)(var3 * 2));
      double var6 = Cavifier.QuantizedSpaghettiRarity.getSphaghettiRarity2D(var4);
      double var8 = 0.6D;
      double var10 = 1.3D;
      double var12 = NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti2dThicknessModulator, (double)(var1 * 2), (double)var2, (double)(var3 * 2), 0.6D, 1.3D);
      double var14 = sampleWithRarity(this.spaghetti2dNoiseSource, (double)var1, (double)var2, (double)var3, var6);
      double var16 = 0.083D;
      double var18 = Math.abs(var6 * var14) - 0.083D * var12;
      int var20 = this.minCellY;
      boolean var21 = true;
      double var22 = NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti2dElevationModulator, (double)var1, 0.0D, (double)var3, (double)var20, 8.0D);
      double var24 = Math.abs(var22 - (double)var2 / 8.0D) - 1.0D * var12;
      var24 = var24 * var24 * var24;
      return clampToUnit(Math.max(var24, var18));
   }

   private double spaghettiRoughness(int var1, int var2, int var3) {
      double var4 = NoiseUtils.sampleNoiseAndMapToRange(this.spaghettiRoughnessModulator, (double)var1, (double)var2, (double)var3, 0.0D, 0.1D);
      return (0.4D - Math.abs(this.spaghettiRoughnessNoise.getValue((double)var1, (double)var2, (double)var3))) * var4;
   }

   private static double clampToUnit(double var0) {
      return Mth.clamp(var0, -1.0D, 1.0D);
   }

   private static double sampleWithRarity(NormalNoise var0, double var1, double var3, double var5, double var7) {
      return var0.getValue(var1 / var7, var3 / var7, var5 / var7);
   }

   private static final class QuantizedSpaghettiRarity {
      static double getSphaghettiRarity2D(double var0) {
         if (var0 < -0.75D) {
            return 0.5D;
         } else if (var0 < -0.5D) {
            return 0.75D;
         } else if (var0 < 0.5D) {
            return 1.0D;
         } else {
            return var0 < 0.75D ? 2.0D : 3.0D;
         }
      }

      static double getSpaghettiRarity3D(double var0) {
         if (var0 < -0.5D) {
            return 0.75D;
         } else if (var0 < 0.0D) {
            return 1.0D;
         } else {
            return var0 < 0.5D ? 1.5D : 2.0D;
         }
      }
   }
}
