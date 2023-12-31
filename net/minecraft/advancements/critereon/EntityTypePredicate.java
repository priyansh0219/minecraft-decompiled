package net.minecraft.advancements.critereon;

import com.google.common.base.Joiner;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.SerializationTags;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagContainer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;

public abstract class EntityTypePredicate {
   public static final EntityTypePredicate ANY = new EntityTypePredicate() {
      public boolean matches(EntityType<?> var1) {
         return true;
      }

      public JsonElement serializeToJson() {
         return JsonNull.INSTANCE;
      }
   };
   private static final Joiner COMMA_JOINER = Joiner.on(", ");

   public abstract boolean matches(EntityType<?> var1);

   public abstract JsonElement serializeToJson();

   public static EntityTypePredicate fromJson(@Nullable JsonElement var0) {
      if (var0 != null && !var0.isJsonNull()) {
         String var1 = GsonHelper.convertToString(var0, "type");
         ResourceLocation var2;
         if (var1.startsWith("#")) {
            var2 = new ResourceLocation(var1.substring(1));
            return new EntityTypePredicate.TagPredicate(SerializationTags.getInstance().getTagOrThrow(Registry.ENTITY_TYPE_REGISTRY, var2, (var0x) -> {
               return new JsonSyntaxException("Unknown entity tag '" + var0x + "'");
            }));
         } else {
            var2 = new ResourceLocation(var1);
            EntityType var3 = (EntityType)Registry.ENTITY_TYPE.getOptional(var2).orElseThrow(() -> {
               return new JsonSyntaxException("Unknown entity type '" + var2 + "', valid types are: " + COMMA_JOINER.join(Registry.ENTITY_TYPE.keySet()));
            });
            return new EntityTypePredicate.TypePredicate(var3);
         }
      } else {
         return ANY;
      }
   }

   public static EntityTypePredicate of(EntityType<?> var0) {
      return new EntityTypePredicate.TypePredicate(var0);
   }

   public static EntityTypePredicate of(Tag<EntityType<?>> var0) {
      return new EntityTypePredicate.TagPredicate(var0);
   }

   static class TagPredicate extends EntityTypePredicate {
      private final Tag<EntityType<?>> tag;

      public TagPredicate(Tag<EntityType<?>> var1) {
         this.tag = var1;
      }

      public boolean matches(EntityType<?> var1) {
         return var1.is(this.tag);
      }

      public JsonElement serializeToJson() {
         TagContainer var10002 = SerializationTags.getInstance();
         ResourceKey var10003 = Registry.ENTITY_TYPE_REGISTRY;
         Tag var10004 = this.tag;
         return new JsonPrimitive("#" + var10002.getIdOrThrow(var10003, var10004, () -> {
            return new IllegalStateException("Unknown entity type tag");
         }));
      }
   }

   private static class TypePredicate extends EntityTypePredicate {
      private final EntityType<?> type;

      public TypePredicate(EntityType<?> var1) {
         this.type = var1;
      }

      public boolean matches(EntityType<?> var1) {
         return this.type == var1;
      }

      public JsonElement serializeToJson() {
         return new JsonPrimitive(Registry.ENTITY_TYPE.getKey(this.type).toString());
      }
   }
}
