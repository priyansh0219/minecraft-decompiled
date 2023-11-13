package net.minecraft.util;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class ExtraCodecs {
   public static final Codec<Integer> NON_NEGATIVE_INT = intRangeWithMessage(0, Integer.MAX_VALUE, (var0) -> {
      return "Value must be non-negative: " + var0;
   });
   public static final Codec<Integer> POSITIVE_INT = intRangeWithMessage(1, Integer.MAX_VALUE, (var0) -> {
      return "Value must be positive: " + var0;
   });

   public static <F, S> Codec<Either<F, S>> xor(Codec<F> var0, Codec<S> var1) {
      return new ExtraCodecs.XorCodec(var0, var1);
   }

   private static <N extends Number & Comparable<N>> Function<N, DataResult<N>> checkRangeWithMessage(N var0, N var1, Function<N, String> var2) {
      return (var3) -> {
         return ((Comparable)var3).compareTo(var0) >= 0 && ((Comparable)var3).compareTo(var1) <= 0 ? DataResult.success(var3) : DataResult.error((String)var2.apply(var3));
      };
   }

   private static Codec<Integer> intRangeWithMessage(int var0, int var1, Function<Integer, String> var2) {
      Function var3 = checkRangeWithMessage(var0, var1, var2);
      return Codec.INT.flatXmap(var3, var3);
   }

   public static <T> Function<List<T>, DataResult<List<T>>> nonEmptyListCheck() {
      return (var0) -> {
         return var0.isEmpty() ? DataResult.error("List must have contents") : DataResult.success(var0);
      };
   }

   public static <T> Codec<List<T>> nonEmptyList(Codec<List<T>> var0) {
      return var0.flatXmap(nonEmptyListCheck(), nonEmptyListCheck());
   }

   public static <T> Function<List<Supplier<T>>, DataResult<List<Supplier<T>>>> nonNullSupplierListCheck() {
      return (var0) -> {
         ArrayList var1 = Lists.newArrayList();

         for(int var2 = 0; var2 < var0.size(); ++var2) {
            Supplier var3 = (Supplier)var0.get(var2);

            try {
               if (var3.get() == null) {
                  var1.add("Missing value [" + var2 + "] : " + var3);
               }
            } catch (Exception var5) {
               var1.add("Invalid value [" + var2 + "]: " + var3 + ", message: " + var5.getMessage());
            }
         }

         return !var1.isEmpty() ? DataResult.error(String.join("; ", var1)) : DataResult.success(var0, Lifecycle.stable());
      };
   }

   public static <T> Function<Supplier<T>, DataResult<Supplier<T>>> nonNullSupplierCheck() {
      return (var0) -> {
         try {
            if (var0.get() == null) {
               return DataResult.error("Missing value: " + var0);
            }
         } catch (Exception var2) {
            return DataResult.error("Invalid value: " + var0 + ", message: " + var2.getMessage());
         }

         return DataResult.success(var0, Lifecycle.stable());
      };
   }

   private static final class XorCodec<F, S> implements Codec<Either<F, S>> {
      private final Codec<F> first;
      private final Codec<S> second;

      public XorCodec(Codec<F> var1, Codec<S> var2) {
         this.first = var1;
         this.second = var2;
      }

      public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> var1, T var2) {
         DataResult var3 = this.first.decode(var1, var2).map((var0) -> {
            return var0.mapFirst(Either::left);
         });
         DataResult var4 = this.second.decode(var1, var2).map((var0) -> {
            return var0.mapFirst(Either::right);
         });
         Optional var5 = var3.result();
         Optional var6 = var4.result();
         if (var5.isPresent() && var6.isPresent()) {
            return DataResult.error("Both alternatives read successfully, can not pick the correct one; first: " + var5.get() + " second: " + var6.get(), (Pair)var5.get());
         } else {
            return var5.isPresent() ? var3 : var4;
         }
      }

      public <T> DataResult<T> encode(Either<F, S> var1, DynamicOps<T> var2, T var3) {
         return (DataResult)var1.map((var3x) -> {
            return this.first.encode(var3x, var2, var3);
         }, (var3x) -> {
            return this.second.encode(var3x, var2, var3);
         });
      }

      public boolean equals(Object var1) {
         if (this == var1) {
            return true;
         } else if (var1 != null && this.getClass() == var1.getClass()) {
            ExtraCodecs.XorCodec var2 = (ExtraCodecs.XorCodec)var1;
            return Objects.equals(this.first, var2.first) && Objects.equals(this.second, var2.second);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return Objects.hash(new Object[]{this.first, this.second});
      }

      public String toString() {
         return "XorCodec[" + this.first + ", " + this.second + "]";
      }

      // $FF: synthetic method
      public DataResult encode(Object var1, DynamicOps var2, Object var3) {
         return this.encode((Either)var1, var2, var3);
      }
   }
}
