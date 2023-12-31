package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;

public class ShufflingList<U> {
   protected final List<ShufflingList.WeightedEntry<U>> entries;
   private final Random random = new Random();

   public ShufflingList() {
      this.entries = Lists.newArrayList();
   }

   private ShufflingList(List<ShufflingList.WeightedEntry<U>> var1) {
      this.entries = Lists.newArrayList(var1);
   }

   public static <U> Codec<ShufflingList<U>> codec(Codec<U> var0) {
      return ShufflingList.WeightedEntry.codec(var0).listOf().xmap(ShufflingList::new, (var0x) -> {
         return var0x.entries;
      });
   }

   public ShufflingList<U> add(U var1, int var2) {
      this.entries.add(new ShufflingList.WeightedEntry(var1, var2));
      return this;
   }

   public ShufflingList<U> shuffle() {
      this.entries.forEach((var1) -> {
         var1.setRandom(this.random.nextFloat());
      });
      this.entries.sort(Comparator.comparingDouble(ShufflingList.WeightedEntry::getRandWeight));
      return this;
   }

   public Stream<U> stream() {
      return this.entries.stream().map(ShufflingList.WeightedEntry::getData);
   }

   public String toString() {
      return "ShufflingList[" + this.entries + "]";
   }

   public static class WeightedEntry<T> {
      final T data;
      final int weight;
      private double randWeight;

      WeightedEntry(T var1, int var2) {
         this.weight = var2;
         this.data = var1;
      }

      private double getRandWeight() {
         return this.randWeight;
      }

      void setRandom(float var1) {
         this.randWeight = -Math.pow((double)var1, (double)(1.0F / (float)this.weight));
      }

      public T getData() {
         return this.data;
      }

      public int getWeight() {
         return this.weight;
      }

      public String toString() {
         return this.weight + ":" + this.data;
      }

      public static <E> Codec<ShufflingList.WeightedEntry<E>> codec(Codec<E> var0) {
         return new Codec<ShufflingList.WeightedEntry<E>>() {
            // $FF: synthetic field
            final Codec val$elementCodec;

            {
               this.val$elementCodec = var1;
            }

            public <T> DataResult<Pair<ShufflingList.WeightedEntry<E>, T>> decode(DynamicOps<T> var1, T var2) {
               Dynamic var3 = new Dynamic(var1, var2);
               OptionalDynamic var10000 = var3.get("data");
               Codec var10001 = this.val$elementCodec;
               Objects.requireNonNull(var10001);
               return var10000.flatMap(var10001::parse).map((var1) -> {
                  return new ShufflingList.WeightedEntry(var1, var0.get("weight").asInt(1));
               }).map((var1) -> {
                  return Pair.of(var1, var0.empty());
               });
            }

            public <T> DataResult<T> encode(ShufflingList.WeightedEntry<E> var1, DynamicOps<T> var2, T var3) {
               return var2.mapBuilder().add("weight", var2.createInt(var1.weight)).add("data", this.val$elementCodec.encodeStart(var2, var1.data)).build(var3);
            }

            // $FF: synthetic method
            public DataResult encode(Object var1, DynamicOps var2, Object var3) {
               return this.encode((ShufflingList.WeightedEntry)var1, var2, var3);
            }

            // $FF: synthetic method
            private static Pair lambda$decode$1(DynamicOps var0, ShufflingList.WeightedEntry var1) {
               return Pair.of(var1, var0.empty());
            }

            // $FF: synthetic method
            private static ShufflingList.WeightedEntry lambda$decode$0(Dynamic var0, Object var1) {
               return new ShufflingList.WeightedEntry(var1, var0.get("weight").asInt(1));
            }
         };
      }
   }
}
