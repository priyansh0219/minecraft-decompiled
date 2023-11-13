package net.minecraft.world.level.levelgen.synth;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.List;
import java.util.function.LongFunction;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public class PerlinNoise implements SurfaceNoise {
   private static final int ROUND_OFF = 33554432;
   private final ImprovedNoise[] noiseLevels;
   private final DoubleList amplitudes;
   private final double lowestFreqValueFactor;
   private final double lowestFreqInputFactor;

   public PerlinNoise(RandomSource var1, IntStream var2) {
      this(var1, (List)var2.boxed().collect(ImmutableList.toImmutableList()));
   }

   public PerlinNoise(RandomSource var1, List<Integer> var2) {
      this(var1, (IntSortedSet)(new IntRBTreeSet(var2)));
   }

   public static PerlinNoise create(RandomSource var0, int var1, double... var2) {
      return create(var0, var1, (DoubleList)(new DoubleArrayList(var2)));
   }

   public static PerlinNoise create(RandomSource var0, int var1, DoubleList var2) {
      return new PerlinNoise(var0, Pair.of(var1, var2));
   }

   private static Pair<Integer, DoubleList> makeAmplitudes(IntSortedSet var0) {
      if (var0.isEmpty()) {
         throw new IllegalArgumentException("Need some octaves!");
      } else {
         int var1 = -var0.firstInt();
         int var2 = var0.lastInt();
         int var3 = var1 + var2 + 1;
         if (var3 < 1) {
            throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
         } else {
            DoubleArrayList var4 = new DoubleArrayList(new double[var3]);
            IntBidirectionalIterator var5 = var0.iterator();

            while(var5.hasNext()) {
               int var6 = var5.nextInt();
               var4.set(var6 + var1, 1.0D);
            }

            return Pair.of(-var1, var4);
         }
      }
   }

   private PerlinNoise(RandomSource var1, IntSortedSet var2) {
      this(var1, var2, WorldgenRandom::new);
   }

   private PerlinNoise(RandomSource var1, IntSortedSet var2, LongFunction<RandomSource> var3) {
      this(var1, makeAmplitudes(var2), var3);
   }

   protected PerlinNoise(RandomSource var1, Pair<Integer, DoubleList> var2) {
      this(var1, var2, WorldgenRandom::new);
   }

   protected PerlinNoise(RandomSource var1, Pair<Integer, DoubleList> var2, LongFunction<RandomSource> var3) {
      int var4 = (Integer)var2.getFirst();
      this.amplitudes = (DoubleList)var2.getSecond();
      ImprovedNoise var5 = new ImprovedNoise(var1);
      int var6 = this.amplitudes.size();
      int var7 = -var4;
      this.noiseLevels = new ImprovedNoise[var6];
      if (var7 >= 0 && var7 < var6) {
         double var8 = this.amplitudes.getDouble(var7);
         if (var8 != 0.0D) {
            this.noiseLevels[var7] = var5;
         }
      }

      for(int var11 = var7 - 1; var11 >= 0; --var11) {
         if (var11 < var6) {
            double var9 = this.amplitudes.getDouble(var11);
            if (var9 != 0.0D) {
               this.noiseLevels[var11] = new ImprovedNoise(var1);
            } else {
               skipOctave(var1);
            }
         } else {
            skipOctave(var1);
         }
      }

      if (var7 < var6 - 1) {
         throw new IllegalArgumentException("Positive octaves are temporarily disabled");
      } else {
         this.lowestFreqInputFactor = Math.pow(2.0D, (double)(-var7));
         this.lowestFreqValueFactor = Math.pow(2.0D, (double)(var6 - 1)) / (Math.pow(2.0D, (double)var6) - 1.0D);
      }
   }

   private static void skipOctave(RandomSource var0) {
      var0.consumeCount(262);
   }

   public double getValue(double var1, double var3, double var5) {
      return this.getValue(var1, var3, var5, 0.0D, 0.0D, false);
   }

   @Deprecated
   public double getValue(double var1, double var3, double var5, double var7, double var9, boolean var11) {
      double var12 = 0.0D;
      double var14 = this.lowestFreqInputFactor;
      double var16 = this.lowestFreqValueFactor;

      for(int var18 = 0; var18 < this.noiseLevels.length; ++var18) {
         ImprovedNoise var19 = this.noiseLevels[var18];
         if (var19 != null) {
            double var20 = var19.noise(wrap(var1 * var14), var11 ? -var19.yo : wrap(var3 * var14), wrap(var5 * var14), var7 * var14, var9 * var14);
            var12 += this.amplitudes.getDouble(var18) * var20 * var16;
         }

         var14 *= 2.0D;
         var16 /= 2.0D;
      }

      return var12;
   }

   @Nullable
   public ImprovedNoise getOctaveNoise(int var1) {
      return this.noiseLevels[this.noiseLevels.length - 1 - var1];
   }

   public static double wrap(double var0) {
      return var0 - (double)Mth.lfloor(var0 / 3.3554432E7D + 0.5D) * 3.3554432E7D;
   }

   public double getSurfaceNoiseValue(double var1, double var3, double var5, double var7) {
      return this.getValue(var1, var3, 0.0D, var5, var7, false);
   }
}
