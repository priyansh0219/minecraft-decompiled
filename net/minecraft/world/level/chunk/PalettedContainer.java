package net.minecraft.world.level.chunk;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.IdMapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.BitStorage;
import net.minecraft.util.DebugBuffer;
import net.minecraft.util.Mth;
import net.minecraft.util.ThreadingDetector;

public class PalettedContainer<T> implements PaletteResize<T> {
   private static final int SIZE = 4096;
   public static final int GLOBAL_PALETTE_BITS = 9;
   public static final int MIN_PALETTE_SIZE = 4;
   private final Palette<T> globalPalette;
   private final PaletteResize<T> dummyPaletteResize = (var0, var1x) -> {
      return 0;
   };
   private final IdMapper<T> registry;
   private final Function<CompoundTag, T> reader;
   private final Function<T, CompoundTag> writer;
   private final T defaultValue;
   protected BitStorage storage;
   private Palette<T> palette;
   private int bits;
   private final Semaphore lock = new Semaphore(1);
   @Nullable
   private final DebugBuffer<Pair<Thread, StackTraceElement[]>> traces = null;

   public void acquire() {
      if (this.traces != null) {
         Thread var1 = Thread.currentThread();
         this.traces.push(Pair.of(var1, var1.getStackTrace()));
      }

      ThreadingDetector.checkAndLock(this.lock, this.traces, "PalettedContainer");
   }

   public void release() {
      this.lock.release();
   }

   public PalettedContainer(Palette<T> var1, IdMapper<T> var2, Function<CompoundTag, T> var3, Function<T, CompoundTag> var4, T var5) {
      this.globalPalette = var1;
      this.registry = var2;
      this.reader = var3;
      this.writer = var4;
      this.defaultValue = var5;
      this.setBits(4);
   }

   private static int getIndex(int var0, int var1, int var2) {
      return var1 << 8 | var2 << 4 | var0;
   }

   private void setBits(int var1) {
      if (var1 != this.bits) {
         this.bits = var1;
         if (this.bits <= 4) {
            this.bits = 4;
            this.palette = new LinearPalette(this.registry, this.bits, this, this.reader);
         } else if (this.bits < 9) {
            this.palette = new HashMapPalette(this.registry, this.bits, this, this.reader, this.writer);
         } else {
            this.palette = this.globalPalette;
            this.bits = Mth.ceillog2(this.registry.size());
         }

         this.palette.idFor(this.defaultValue);
         this.storage = new BitStorage(this.bits, 4096);
      }
   }

   public int onResize(int var1, T var2) {
      BitStorage var3 = this.storage;
      Palette var4 = this.palette;
      this.setBits(var1);

      for(int var5 = 0; var5 < var3.getSize(); ++var5) {
         Object var6 = var4.valueFor(var3.get(var5));
         if (var6 != null) {
            this.set(var5, var6);
         }
      }

      return this.palette.idFor(var2);
   }

   public T getAndSet(int var1, int var2, int var3, T var4) {
      Object var6;
      try {
         this.acquire();
         Object var5 = this.getAndSet(getIndex(var1, var2, var3), var4);
         var6 = var5;
      } finally {
         this.release();
      }

      return var6;
   }

   public T getAndSetUnchecked(int var1, int var2, int var3, T var4) {
      return this.getAndSet(getIndex(var1, var2, var3), var4);
   }

   private T getAndSet(int var1, T var2) {
      int var3 = this.palette.idFor(var2);
      int var4 = this.storage.getAndSet(var1, var3);
      Object var5 = this.palette.valueFor(var4);
      return var5 == null ? this.defaultValue : var5;
   }

   public void set(int var1, int var2, int var3, T var4) {
      try {
         this.acquire();
         this.set(getIndex(var1, var2, var3), var4);
      } finally {
         this.release();
      }

   }

   private void set(int var1, T var2) {
      int var3 = this.palette.idFor(var2);
      this.storage.set(var1, var3);
   }

   public T get(int var1, int var2, int var3) {
      return this.get(getIndex(var1, var2, var3));
   }

   protected T get(int var1) {
      Object var2 = this.palette.valueFor(this.storage.get(var1));
      return var2 == null ? this.defaultValue : var2;
   }

   public void read(FriendlyByteBuf var1) {
      try {
         this.acquire();
         byte var2 = var1.readByte();
         if (this.bits != var2) {
            this.setBits(var2);
         }

         this.palette.read(var1);
         var1.readLongArray(this.storage.getRaw());
      } finally {
         this.release();
      }

   }

   public void write(FriendlyByteBuf var1) {
      try {
         this.acquire();
         var1.writeByte(this.bits);
         this.palette.write(var1);
         var1.writeLongArray(this.storage.getRaw());
      } finally {
         this.release();
      }

   }

   public void read(ListTag var1, long[] var2) {
      try {
         this.acquire();
         int var3 = Math.max(4, Mth.ceillog2(var1.size()));
         if (var3 != this.bits) {
            this.setBits(var3);
         }

         this.palette.read(var1);
         int var4 = var2.length * 64 / 4096;
         if (this.palette == this.globalPalette) {
            HashMapPalette var5 = new HashMapPalette(this.registry, var3, this.dummyPaletteResize, this.reader, this.writer);
            var5.read(var1);
            BitStorage var6 = new BitStorage(var3, 4096, var2);

            for(int var7 = 0; var7 < 4096; ++var7) {
               this.storage.set(var7, this.globalPalette.idFor(var5.valueFor(var6.get(var7))));
            }
         } else if (var4 == this.bits) {
            System.arraycopy(var2, 0, this.storage.getRaw(), 0, var2.length);
         } else {
            BitStorage var11 = new BitStorage(var4, 4096, var2);

            for(int var12 = 0; var12 < 4096; ++var12) {
               this.storage.set(var12, var11.get(var12));
            }
         }
      } finally {
         this.release();
      }

   }

   public void write(CompoundTag var1, String var2, String var3) {
      try {
         this.acquire();
         HashMapPalette var4 = new HashMapPalette(this.registry, this.bits, this.dummyPaletteResize, this.reader, this.writer);
         Object var5 = this.defaultValue;
         int var6 = var4.idFor(this.defaultValue);
         int[] var7 = new int[4096];

         for(int var8 = 0; var8 < 4096; ++var8) {
            Object var9 = this.get(var8);
            if (var9 != var5) {
               var5 = var9;
               var6 = var4.idFor(var9);
            }

            var7[var8] = var6;
         }

         ListTag var15 = new ListTag();
         var4.write(var15);
         var1.put(var2, var15);
         int var16 = Math.max(4, Mth.ceillog2(var15.size()));
         BitStorage var10 = new BitStorage(var16, 4096);

         for(int var11 = 0; var11 < var7.length; ++var11) {
            var10.set(var11, var7[var11]);
         }

         var1.putLongArray(var3, var10.getRaw());
      } finally {
         this.release();
      }
   }

   public int getSerializedSize() {
      return 1 + this.palette.getSerializedSize() + FriendlyByteBuf.getVarIntSize(this.storage.getSize()) + this.storage.getRaw().length * 8;
   }

   public boolean maybeHas(Predicate<T> var1) {
      return this.palette.maybeHas(var1);
   }

   public void count(PalettedContainer.CountConsumer<T> var1) {
      Int2IntOpenHashMap var2 = new Int2IntOpenHashMap();
      this.storage.getAll((var1x) -> {
         var2.put(var1x, var2.get(var1x) + 1);
      });
      var2.int2IntEntrySet().forEach((var2x) -> {
         var1.accept(this.palette.valueFor(var2x.getIntKey()), var2x.getIntValue());
      });
   }

   @FunctionalInterface
   public interface CountConsumer<T> {
      void accept(T var1, int var2);
   }
}
