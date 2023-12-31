package net.minecraft.world.level.levelgen.synth;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleListIterator;
import net.minecraft.world.level.levelgen.RandomSource;

public class NormalNoise {
   private static final double INPUT_FACTOR = 1.0181268882175227D;
   private static final double TARGET_DEVIATION = 0.3333333333333333D;
   private final double valueFactor;
   private final PerlinNoise first;
   private final PerlinNoise second;

   public static NormalNoise create(RandomSource var0, int var1, double... var2) {
      return new NormalNoise(var0, var1, new DoubleArrayList(var2));
   }

   public static NormalNoise create(RandomSource var0, int var1, DoubleList var2) {
      return new NormalNoise(var0, var1, var2);
   }

   private NormalNoise(RandomSource var1, int var2, DoubleList var3) {
      this.first = PerlinNoise.create(var1, var2, var3);
      this.second = PerlinNoise.create(var1, var2, var3);
      int var4 = Integer.MAX_VALUE;
      int var5 = Integer.MIN_VALUE;
      DoubleListIterator var6 = var3.iterator();

      while(var6.hasNext()) {
         int var7 = var6.nextIndex();
         double var8 = var6.nextDouble();
         if (var8 != 0.0D) {
            var4 = Math.min(var4, var7);
            var5 = Math.max(var5, var7);
         }
      }

      this.valueFactor = 0.16666666666666666D / expectedDeviation(var5 - var4);
   }

   private static double expectedDeviation(int var0) {
      return 0.1D * (1.0D + 1.0D / (double)(var0 + 1));
   }

   public double getValue(double var1, double var3, double var5) {
      double var7 = var1 * 1.0181268882175227D;
      double var9 = var3 * 1.0181268882175227D;
      double var11 = var5 * 1.0181268882175227D;
      return (this.first.getValue(var1, var3, var5) + this.second.getValue(var7, var9, var11)) * this.valueFactor;
   }
}
