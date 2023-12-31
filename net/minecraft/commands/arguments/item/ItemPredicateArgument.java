package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemPredicateArgument implements ArgumentType<ItemPredicateArgument.Result> {
   private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick", "#stick{foo=bar}");
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType((var0) -> {
      return new TranslatableComponent("arguments.item.tag.unknown", new Object[]{var0});
   });

   public static ItemPredicateArgument itemPredicate() {
      return new ItemPredicateArgument();
   }

   public ItemPredicateArgument.Result parse(StringReader var1) throws CommandSyntaxException {
      ItemParser var2 = (new ItemParser(var1, true)).parse();
      if (var2.getItem() != null) {
         ItemPredicateArgument.ItemPredicate var4 = new ItemPredicateArgument.ItemPredicate(var2.getItem(), var2.getNbt());
         return (var1x) -> {
            return var4;
         };
      } else {
         ResourceLocation var3 = var2.getTag();
         return (var2x) -> {
            Tag var3x = ((CommandSourceStack)var2x.getSource()).getServer().getTags().getTagOrThrow(Registry.ITEM_REGISTRY, var3, (var0) -> {
               return ERROR_UNKNOWN_TAG.create(var0.toString());
            });
            return new ItemPredicateArgument.TagPredicate(var3x, var2.getNbt());
         };
      }
   }

   public static Predicate<ItemStack> getItemPredicate(CommandContext<CommandSourceStack> var0, String var1) throws CommandSyntaxException {
      return ((ItemPredicateArgument.Result)var0.getArgument(var1, ItemPredicateArgument.Result.class)).create(var0);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> var1, SuggestionsBuilder var2) {
      StringReader var3 = new StringReader(var2.getInput());
      var3.setCursor(var2.getStart());
      ItemParser var4 = new ItemParser(var3, true);

      try {
         var4.parse();
      } catch (CommandSyntaxException var6) {
      }

      return var4.fillSuggestions(var2, ItemTags.getAllTags());
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   // $FF: synthetic method
   public Object parse(StringReader var1) throws CommandSyntaxException {
      return this.parse(var1);
   }

   private static class ItemPredicate implements Predicate<ItemStack> {
      private final Item item;
      @Nullable
      private final CompoundTag nbt;

      public ItemPredicate(Item var1, @Nullable CompoundTag var2) {
         this.item = var1;
         this.nbt = var2;
      }

      public boolean test(ItemStack var1) {
         return var1.is(this.item) && NbtUtils.compareNbt(this.nbt, var1.getTag(), true);
      }

      // $FF: synthetic method
      public boolean test(Object var1) {
         return this.test((ItemStack)var1);
      }
   }

   public interface Result {
      Predicate<ItemStack> create(CommandContext<CommandSourceStack> var1) throws CommandSyntaxException;
   }

   static class TagPredicate implements Predicate<ItemStack> {
      private final Tag<Item> tag;
      @Nullable
      private final CompoundTag nbt;

      public TagPredicate(Tag<Item> var1, @Nullable CompoundTag var2) {
         this.tag = var1;
         this.nbt = var2;
      }

      public boolean test(ItemStack var1) {
         return var1.is(this.tag) && NbtUtils.compareNbt(this.nbt, var1.getTag(), true);
      }

      // $FF: synthetic method
      public boolean test(Object var1) {
         return this.test((ItemStack)var1);
      }
   }
}
