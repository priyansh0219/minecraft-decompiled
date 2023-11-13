package net.minecraft.world.level.levelgen;

public interface RandomSource {
   void setSeed(long var1);

   int nextInt();

   int nextInt(int var1);

   long nextLong();

   boolean nextBoolean();

   float nextFloat();

   double nextDouble();

   double nextGaussian();

   default void consumeCount(int var1) {
      for(int var2 = 0; var2 < var1; ++var2) {
         this.nextInt();
      }

   }
}
