package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProvider;

public class CopyNbtFunction extends LootItemConditionalFunction {
   final NbtProvider source;
   final List<CopyNbtFunction.CopyOperation> operations;

   CopyNbtFunction(LootItemCondition[] var1, NbtProvider var2, List<CopyNbtFunction.CopyOperation> var3) {
      super(var1);
      this.source = var2;
      this.operations = ImmutableList.copyOf(var3);
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.COPY_NBT;
   }

   static NbtPathArgument.NbtPath compileNbtPath(String var0) {
      try {
         return (new NbtPathArgument()).parse(new StringReader(var0));
      } catch (CommandSyntaxException var2) {
         throw new IllegalArgumentException("Failed to parse path " + var0, var2);
      }
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.source.getReferencedContextParams();
   }

   public ItemStack run(ItemStack var1, LootContext var2) {
      Tag var3 = this.source.get(var2);
      if (var3 != null) {
         this.operations.forEach((var2x) -> {
            Objects.requireNonNull(var1);
            var2x.apply(var1::getOrCreateTag, var3);
         });
      }

      return var1;
   }

   public static CopyNbtFunction.Builder copyData(NbtProvider var0) {
      return new CopyNbtFunction.Builder(var0);
   }

   public static CopyNbtFunction.Builder copyData(LootContext.EntityTarget var0) {
      return new CopyNbtFunction.Builder(ContextNbtProvider.forContextEntity(var0));
   }

   public static class Builder extends LootItemConditionalFunction.Builder<CopyNbtFunction.Builder> {
      private final NbtProvider source;
      private final List<CopyNbtFunction.CopyOperation> ops = Lists.newArrayList();

      Builder(NbtProvider var1) {
         this.source = var1;
      }

      public CopyNbtFunction.Builder copy(String var1, String var2, CopyNbtFunction.MergeStrategy var3) {
         this.ops.add(new CopyNbtFunction.CopyOperation(var1, var2, var3));
         return this;
      }

      public CopyNbtFunction.Builder copy(String var1, String var2) {
         return this.copy(var1, var2, CopyNbtFunction.MergeStrategy.REPLACE);
      }

      protected CopyNbtFunction.Builder getThis() {
         return this;
      }

      public LootItemFunction build() {
         return new CopyNbtFunction(this.getConditions(), this.source, this.ops);
      }

      // $FF: synthetic method
      protected LootItemConditionalFunction.Builder getThis() {
         return this.getThis();
      }
   }

   private static class CopyOperation {
      private final String sourcePathText;
      private final NbtPathArgument.NbtPath sourcePath;
      private final String targetPathText;
      private final NbtPathArgument.NbtPath targetPath;
      private final CopyNbtFunction.MergeStrategy op;

      CopyOperation(String var1, String var2, CopyNbtFunction.MergeStrategy var3) {
         this.sourcePathText = var1;
         this.sourcePath = CopyNbtFunction.compileNbtPath(var1);
         this.targetPathText = var2;
         this.targetPath = CopyNbtFunction.compileNbtPath(var2);
         this.op = var3;
      }

      public void apply(Supplier<Tag> var1, Tag var2) {
         try {
            List var3 = this.sourcePath.get(var2);
            if (!var3.isEmpty()) {
               this.op.merge((Tag)var1.get(), this.targetPath, var3);
            }
         } catch (CommandSyntaxException var4) {
         }

      }

      public JsonObject toJson() {
         JsonObject var1 = new JsonObject();
         var1.addProperty("source", this.sourcePathText);
         var1.addProperty("target", this.targetPathText);
         var1.addProperty("op", this.op.name);
         return var1;
      }

      public static CopyNbtFunction.CopyOperation fromJson(JsonObject var0) {
         String var1 = GsonHelper.getAsString(var0, "source");
         String var2 = GsonHelper.getAsString(var0, "target");
         CopyNbtFunction.MergeStrategy var3 = CopyNbtFunction.MergeStrategy.getByName(GsonHelper.getAsString(var0, "op"));
         return new CopyNbtFunction.CopyOperation(var1, var2, var3);
      }
   }

   public static class Serializer extends LootItemConditionalFunction.Serializer<CopyNbtFunction> {
      public void serialize(JsonObject var1, CopyNbtFunction var2, JsonSerializationContext var3) {
         super.serialize(var1, (LootItemConditionalFunction)var2, var3);
         var1.add("source", var3.serialize(var2.source));
         JsonArray var4 = new JsonArray();
         Stream var10000 = var2.operations.stream().map(CopyNbtFunction.CopyOperation::toJson);
         Objects.requireNonNull(var4);
         var10000.forEach(var4::add);
         var1.add("ops", var4);
      }

      public CopyNbtFunction deserialize(JsonObject var1, JsonDeserializationContext var2, LootItemCondition[] var3) {
         NbtProvider var4 = (NbtProvider)GsonHelper.getAsObject(var1, "source", var2, NbtProvider.class);
         ArrayList var5 = Lists.newArrayList();
         JsonArray var6 = GsonHelper.getAsJsonArray(var1, "ops");
         Iterator var7 = var6.iterator();

         while(var7.hasNext()) {
            JsonElement var8 = (JsonElement)var7.next();
            JsonObject var9 = GsonHelper.convertToJsonObject(var8, "op");
            var5.add(CopyNbtFunction.CopyOperation.fromJson(var9));
         }

         return new CopyNbtFunction(var3, var4, var5);
      }

      // $FF: synthetic method
      public LootItemConditionalFunction deserialize(JsonObject var1, JsonDeserializationContext var2, LootItemCondition[] var3) {
         return this.deserialize(var1, var2, var3);
      }
   }

   public static enum MergeStrategy {
      REPLACE("replace") {
         public void merge(Tag var1, NbtPathArgument.NbtPath var2, List<Tag> var3) throws CommandSyntaxException {
            Tag var10002 = (Tag)Iterables.getLast(var3);
            Objects.requireNonNull(var10002);
            var2.set(var1, var10002::copy);
         }
      },
      APPEND("append") {
         public void merge(Tag var1, NbtPathArgument.NbtPath var2, List<Tag> var3) throws CommandSyntaxException {
            List var4 = var2.getOrCreate(var1, ListTag::new);
            var4.forEach((var1) -> {
               if (var1 instanceof ListTag) {
                  var0.forEach((var1) -> {
                     ((ListTag)var0).add(var1.copy());
                  });
               }

            });
         }

         // $FF: synthetic method
         private static void lambda$merge$1(List var0, Tag var1) {
            if (var1 instanceof ListTag) {
               var0.forEach((var1) -> {
                  ((ListTag)var0).add(var1.copy());
               });
            }

         }

         // $FF: synthetic method
         private static void lambda$merge$0(Tag var0, Tag var1) {
            ((ListTag)var0).add(var1.copy());
         }
      },
      MERGE("merge") {
         public void merge(Tag var1, NbtPathArgument.NbtPath var2, List<Tag> var3) throws CommandSyntaxException {
            List var4 = var2.getOrCreate(var1, CompoundTag::new);
            var4.forEach((var1) -> {
               if (var1 instanceof CompoundTag) {
                  var0.forEach((var1) -> {
                     if (var1 instanceof CompoundTag) {
                        ((CompoundTag)var0).merge((CompoundTag)var1);
                     }

                  });
               }

            });
         }

         // $FF: synthetic method
         private static void lambda$merge$1(List var0, Tag var1) {
            if (var1 instanceof CompoundTag) {
               var0.forEach((var1) -> {
                  if (var1 instanceof CompoundTag) {
                     ((CompoundTag)var0).merge((CompoundTag)var1);
                  }

               });
            }

         }

         // $FF: synthetic method
         private static void lambda$merge$0(Tag var0, Tag var1) {
            if (var1 instanceof CompoundTag) {
               ((CompoundTag)var0).merge((CompoundTag)var1);
            }

         }
      };

      final String name;

      public abstract void merge(Tag var1, NbtPathArgument.NbtPath var2, List<Tag> var3) throws CommandSyntaxException;

      MergeStrategy(String var3) {
         this.name = var3;
      }

      public static CopyNbtFunction.MergeStrategy getByName(String var0) {
         CopyNbtFunction.MergeStrategy[] var1 = values();
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            CopyNbtFunction.MergeStrategy var4 = var1[var3];
            if (var4.name.equals(var0)) {
               return var4;
            }
         }

         throw new IllegalArgumentException("Invalid merge strategy" + var0);
      }

      // $FF: synthetic method
      private static CopyNbtFunction.MergeStrategy[] $values() {
         return new CopyNbtFunction.MergeStrategy[]{REPLACE, APPEND, MERGE};
      }
   }
}
