package net.minecraft.world.level.newbiome.context;

import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.world.level.levelgen.SimpleRandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.newbiome.area.Area;
import net.minecraft.world.level.newbiome.area.LazyArea;
import net.minecraft.world.level.newbiome.layer.traits.PixelTransformer;

public class LazyAreaContext implements BigContext<LazyArea> {
   private static final int MAX_CACHE = 1024;
   private final Long2IntLinkedOpenHashMap cache;
   private final int maxCache;
   private final ImprovedNoise biomeNoise;
   private final long seed;
   private long rval;

   public LazyAreaContext(int var1, long var2, long var4) {
      this.seed = mixSeed(var2, var4);
      this.biomeNoise = new ImprovedNoise(new SimpleRandomSource(var2));
      this.cache = new Long2IntLinkedOpenHashMap(16, 0.25F);
      this.cache.defaultReturnValue(Integer.MIN_VALUE);
      this.maxCache = var1;
   }

   public LazyArea createResult(PixelTransformer var1) {
      return new LazyArea(this.cache, this.maxCache, var1);
   }

   public LazyArea createResult(PixelTransformer var1, LazyArea var2) {
      return new LazyArea(this.cache, Math.min(1024, var2.getMaxCache() * 4), var1);
   }

   public LazyArea createResult(PixelTransformer var1, LazyArea var2, LazyArea var3) {
      return new LazyArea(this.cache, Math.min(1024, Math.max(var2.getMaxCache(), var3.getMaxCache()) * 4), var1);
   }

   public void initRandom(long var1, long var3) {
      long var5 = this.seed;
      var5 = LinearCongruentialGenerator.next(var5, var1);
      var5 = LinearCongruentialGenerator.next(var5, var3);
      var5 = LinearCongruentialGenerator.next(var5, var1);
      var5 = LinearCongruentialGenerator.next(var5, var3);
      this.rval = var5;
   }

   public int nextRandom(int var1) {
      int var2 = Math.floorMod(this.rval >> 24, var1);
      this.rval = LinearCongruentialGenerator.next(this.rval, this.seed);
      return var2;
   }

   public ImprovedNoise getBiomeNoise() {
      return this.biomeNoise;
   }

   private static long mixSeed(long var0, long var2) {
      long var4 = LinearCongruentialGenerator.next(var2, var2);
      var4 = LinearCongruentialGenerator.next(var4, var2);
      var4 = LinearCongruentialGenerator.next(var4, var2);
      long var6 = LinearCongruentialGenerator.next(var0, var4);
      var6 = LinearCongruentialGenerator.next(var6, var4);
      var6 = LinearCongruentialGenerator.next(var6, var4);
      return var6;
   }

   // $FF: synthetic method
   public Area createResult(PixelTransformer var1) {
      return this.createResult(var1);
   }
}
