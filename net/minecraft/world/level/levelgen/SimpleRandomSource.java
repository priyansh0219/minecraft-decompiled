package net.minecraft.world.level.levelgen;

import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.DebugBuffer;
import net.minecraft.util.Mth;
import net.minecraft.util.ThreadingDetector;

public class SimpleRandomSource implements RandomSource {
   private static final int MODULUS_BITS = 48;
   private static final long MODULUS_MASK = 281474976710655L;
   private static final long MULTIPLIER = 25214903917L;
   private static final long INCREMENT = 11L;
   private static final float FLOAT_MULTIPLIER = 5.9604645E-8F;
   private static final double DOUBLE_MULTIPLIER = 1.1102230246251565E-16D;
   private final AtomicLong seed = new AtomicLong();
   private double nextNextGaussian;
   private boolean haveNextNextGaussian;

   public SimpleRandomSource(long var1) {
      this.setSeed(var1);
   }

   public void setSeed(long var1) {
      if (!this.seed.compareAndSet(this.seed.get(), (var1 ^ 25214903917L) & 281474976710655L)) {
         throw ThreadingDetector.makeThreadingException("SimpleRandomSource", (DebugBuffer)null);
      }
   }

   private int next(int var1) {
      long var2 = this.seed.get();
      long var4 = var2 * 25214903917L + 11L & 281474976710655L;
      if (!this.seed.compareAndSet(var2, var4)) {
         throw ThreadingDetector.makeThreadingException("SimpleRandomSource", (DebugBuffer)null);
      } else {
         return (int)(var4 >> 48 - var1);
      }
   }

   public int nextInt() {
      return this.next(32);
   }

   public int nextInt(int var1) {
      if (var1 <= 0) {
         throw new IllegalArgumentException("Bound must be positive");
      } else if ((var1 & var1 - 1) == 0) {
         return (int)((long)var1 * (long)this.next(31) >> 31);
      } else {
         int var2;
         int var3;
         do {
            var2 = this.next(31);
            var3 = var2 % var1;
         } while(var2 - var3 + (var1 - 1) < 0);

         return var3;
      }
   }

   public long nextLong() {
      int var1 = this.next(32);
      int var2 = this.next(32);
      long var3 = (long)var1 << 32;
      return var3 + (long)var2;
   }

   public boolean nextBoolean() {
      return this.next(1) != 0;
   }

   public float nextFloat() {
      return (float)this.next(24) * 5.9604645E-8F;
   }

   public double nextDouble() {
      int var1 = this.next(26);
      int var2 = this.next(27);
      long var3 = ((long)var1 << 27) + (long)var2;
      return (double)var3 * 1.1102230246251565E-16D;
   }

   public double nextGaussian() {
      if (this.haveNextNextGaussian) {
         this.haveNextNextGaussian = false;
         return this.nextNextGaussian;
      } else {
         double var1;
         double var3;
         double var5;
         do {
            do {
               var1 = 2.0D * this.nextDouble() - 1.0D;
               var3 = 2.0D * this.nextDouble() - 1.0D;
               var5 = Mth.square(var1) + Mth.square(var3);
            } while(var5 >= 1.0D);
         } while(var5 == 0.0D);

         double var7 = Math.sqrt(-2.0D * Math.log(var5) / var5);
         this.nextNextGaussian = var3 * var7;
         this.haveNextNextGaussian = true;
         return var1 * var7;
      }
   }
}
