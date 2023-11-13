package net.minecraft.resources;

import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.DataResult.PartialResult;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegistryReadOps<T> extends DelegatingOps<T> {
   static final Logger LOGGER = LogManager.getLogger();
   private static final String JSON = ".json";
   private final RegistryReadOps.ResourceAccess resources;
   private final RegistryAccess registryAccess;
   private final Map<ResourceKey<? extends Registry<?>>, RegistryReadOps.ReadCache<?>> readCache;
   private final RegistryReadOps<JsonElement> jsonOps;

   public static <T> RegistryReadOps<T> createAndLoad(DynamicOps<T> var0, ResourceManager var1, RegistryAccess var2) {
      return createAndLoad(var0, RegistryReadOps.ResourceAccess.forResourceManager(var1), var2);
   }

   public static <T> RegistryReadOps<T> createAndLoad(DynamicOps<T> var0, RegistryReadOps.ResourceAccess var1, RegistryAccess var2) {
      RegistryReadOps var3 = new RegistryReadOps(var0, var1, var2, Maps.newIdentityHashMap());
      RegistryAccess.load(var2, var3);
      return var3;
   }

   public static <T> RegistryReadOps<T> create(DynamicOps<T> var0, ResourceManager var1, RegistryAccess var2) {
      return create(var0, RegistryReadOps.ResourceAccess.forResourceManager(var1), var2);
   }

   public static <T> RegistryReadOps<T> create(DynamicOps<T> var0, RegistryReadOps.ResourceAccess var1, RegistryAccess var2) {
      return new RegistryReadOps(var0, var1, var2, Maps.newIdentityHashMap());
   }

   private RegistryReadOps(DynamicOps<T> var1, RegistryReadOps.ResourceAccess var2, RegistryAccess var3, IdentityHashMap<ResourceKey<? extends Registry<?>>, RegistryReadOps.ReadCache<?>> var4) {
      super(var1);
      this.resources = var2;
      this.registryAccess = var3;
      this.readCache = var4;
      this.jsonOps = var1 == JsonOps.INSTANCE ? this : new RegistryReadOps(JsonOps.INSTANCE, var2, var3, var4);
   }

   protected <E> DataResult<Pair<Supplier<E>, T>> decodeElement(T var1, ResourceKey<? extends Registry<E>> var2, Codec<E> var3, boolean var4) {
      Optional var5 = this.registryAccess.ownedRegistry(var2);
      if (!var5.isPresent()) {
         return DataResult.error("Unknown registry: " + var2);
      } else {
         WritableRegistry var6 = (WritableRegistry)var5.get();
         DataResult var7 = ResourceLocation.CODEC.decode(this.delegate, var1);
         if (!var7.result().isPresent()) {
            return !var4 ? DataResult.error("Inline definitions not allowed here") : var3.decode(this, var1).map((var0) -> {
               return var0.mapFirst((var0x) -> {
                  return () -> {
                     return var0x;
                  };
               });
            });
         } else {
            Pair var8 = (Pair)var7.result().get();
            ResourceLocation var9 = (ResourceLocation)var8.getFirst();
            return this.readAndRegisterElement(var2, var6, var3, var9).map((var1x) -> {
               return Pair.of(var1x, var8.getSecond());
            });
         }
      }
   }

   public <E> DataResult<MappedRegistry<E>> decodeElements(MappedRegistry<E> var1, ResourceKey<? extends Registry<E>> var2, Codec<E> var3) {
      Collection var4 = this.resources.listResources(var2);
      DataResult var5 = DataResult.success(var1, Lifecycle.stable());
      String var6 = var2.location().getPath() + "/";
      Iterator var7 = var4.iterator();

      while(var7.hasNext()) {
         ResourceLocation var8 = (ResourceLocation)var7.next();
         String var9 = var8.getPath();
         if (!var9.endsWith(".json")) {
            LOGGER.warn("Skipping resource {} since it is not a json file", var8);
         } else if (!var9.startsWith(var6)) {
            LOGGER.warn("Skipping resource {} since it does not have a registry name prefix", var8);
         } else {
            String var10 = var9.substring(var6.length(), var9.length() - ".json".length());
            ResourceLocation var11 = new ResourceLocation(var8.getNamespace(), var10);
            var5 = var5.flatMap((var4x) -> {
               return this.readAndRegisterElement(var2, var4x, var3, var11).map((var1) -> {
                  return var4x;
               });
            });
         }
      }

      return var5.setPartial(var1);
   }

   private <E> DataResult<Supplier<E>> readAndRegisterElement(ResourceKey<? extends Registry<E>> var1, final WritableRegistry<E> var2, Codec<E> var3, ResourceLocation var4) {
      final ResourceKey var5 = ResourceKey.create(var1, var4);
      RegistryReadOps.ReadCache var6 = this.readCache(var1);
      DataResult var7 = (DataResult)var6.values.get(var5);
      if (var7 != null) {
         return var7;
      } else {
         com.google.common.base.Supplier var8 = Suppliers.memoize(() -> {
            Object var2x = var2.get(var5);
            if (var2x == null) {
               throw new RuntimeException("Error during recursive registry parsing, element resolved too early: " + var5);
            } else {
               return var2x;
            }
         });
         var6.values.put(var5, DataResult.success(var8));
         Optional var9 = this.resources.parseElement(this.jsonOps, var1, var5, var3);
         DataResult var10;
         if (!var9.isPresent()) {
            var10 = DataResult.success(new Supplier<E>() {
               public E get() {
                  return var2.get(var5);
               }

               public String toString() {
                  return var5.toString();
               }
            }, Lifecycle.stable());
         } else {
            DataResult var11 = (DataResult)var9.get();
            Optional var12 = var11.result();
            if (var12.isPresent()) {
               Pair var13 = (Pair)var12.get();
               var2.registerOrOverride((OptionalInt)var13.getSecond(), var5, var13.getFirst(), var11.lifecycle());
            }

            var10 = var11.map((var2x) -> {
               return () -> {
                  return var2.get(var5);
               };
            });
         }

         var6.values.put(var5, var10);
         return var10;
      }
   }

   private <E> RegistryReadOps.ReadCache<E> readCache(ResourceKey<? extends Registry<E>> var1) {
      return (RegistryReadOps.ReadCache)this.readCache.computeIfAbsent(var1, (var0) -> {
         return new RegistryReadOps.ReadCache();
      });
   }

   protected <E> DataResult<Registry<E>> registry(ResourceKey<? extends Registry<E>> var1) {
      return (DataResult)this.registryAccess.ownedRegistry(var1).map((var0) -> {
         return DataResult.success(var0, var0.elementsLifecycle());
      }).orElseGet(() -> {
         return DataResult.error("Unknown registry: " + var1);
      });
   }

   public interface ResourceAccess {
      Collection<ResourceLocation> listResources(ResourceKey<? extends Registry<?>> var1);

      <E> Optional<DataResult<Pair<E, OptionalInt>>> parseElement(DynamicOps<JsonElement> var1, ResourceKey<? extends Registry<E>> var2, ResourceKey<E> var3, Decoder<E> var4);

      static RegistryReadOps.ResourceAccess forResourceManager(ResourceManager var0) {
         return new RegistryReadOps.ResourceAccess() {
            // $FF: synthetic field
            final ResourceManager val$manager;

            {
               this.val$manager = var1;
            }

            public Collection<ResourceLocation> listResources(ResourceKey<? extends Registry<?>> var1) {
               return this.val$manager.listResources(var1.location().getPath(), (var0) -> {
                  return var0.endsWith(".json");
               });
            }

            public <E> Optional<DataResult<Pair<E, OptionalInt>>> parseElement(DynamicOps<JsonElement> var1, ResourceKey<? extends Registry<E>> var2, ResourceKey<E> var3, Decoder<E> var4) {
               ResourceLocation var5 = var3.location();
               String var10002 = var5.getNamespace();
               String var10003 = var2.location().getPath();
               ResourceLocation var6 = new ResourceLocation(var10002, var10003 + "/" + var5.getPath() + ".json");
               if (!this.val$manager.hasResource(var6)) {
                  return Optional.empty();
               } else {
                  try {
                     Resource var7 = this.val$manager.getResource(var6);

                     Optional var11;
                     try {
                        InputStreamReader var8 = new InputStreamReader(var7.getInputStream(), StandardCharsets.UTF_8);

                        try {
                           JsonParser var9 = new JsonParser();
                           JsonElement var10 = var9.parse(var8);
                           var11 = Optional.of(var4.parse(var1, var10).map((var0) -> {
                              return Pair.of(var0, OptionalInt.empty());
                           }));
                        } catch (Throwable var14) {
                           try {
                              var8.close();
                           } catch (Throwable var13) {
                              var14.addSuppressed(var13);
                           }

                           throw var14;
                        }

                        var8.close();
                     } catch (Throwable var15) {
                        if (var7 != null) {
                           try {
                              var7.close();
                           } catch (Throwable var12) {
                              var15.addSuppressed(var12);
                           }
                        }

                        throw var15;
                     }

                     if (var7 != null) {
                        var7.close();
                     }

                     return var11;
                  } catch (JsonIOException | JsonSyntaxException | IOException var16) {
                     return Optional.of(DataResult.error("Failed to parse " + var6 + " file: " + var16.getMessage()));
                  }
               }
            }

            public String toString() {
               return "ResourceAccess[" + this.val$manager + "]";
            }

            // $FF: synthetic method
            private static Pair lambda$parseElement$1(Object var0) {
               return Pair.of(var0, OptionalInt.empty());
            }

            // $FF: synthetic method
            private static boolean lambda$listResources$0(String var0) {
               return var0.endsWith(".json");
            }
         };
      }

      public static final class MemoryMap implements RegistryReadOps.ResourceAccess {
         private final Map<ResourceKey<?>, JsonElement> data = Maps.newIdentityHashMap();
         private final Object2IntMap<ResourceKey<?>> ids = new Object2IntOpenCustomHashMap(Util.identityStrategy());
         private final Map<ResourceKey<?>, Lifecycle> lifecycles = Maps.newIdentityHashMap();

         public <E> void add(RegistryAccess.RegistryHolder var1, ResourceKey<E> var2, Encoder<E> var3, int var4, E var5, Lifecycle var6) {
            DataResult var7 = var3.encodeStart(RegistryWriteOps.create(JsonOps.INSTANCE, var1), var5);
            Optional var8 = var7.error();
            if (var8.isPresent()) {
               RegistryReadOps.LOGGER.error("Error adding element: {}", ((PartialResult)var8.get()).message());
            } else {
               this.data.put(var2, (JsonElement)var7.result().get());
               this.ids.put(var2, var4);
               this.lifecycles.put(var2, var6);
            }
         }

         public Collection<ResourceLocation> listResources(ResourceKey<? extends Registry<?>> var1) {
            return (Collection)this.data.keySet().stream().filter((var1x) -> {
               return var1x.isFor(var1);
            }).map((var1x) -> {
               String var10002 = var1x.location().getNamespace();
               String var10003 = var1.location().getPath();
               return new ResourceLocation(var10002, var10003 + "/" + var1x.location().getPath() + ".json");
            }).collect(Collectors.toList());
         }

         public <E> Optional<DataResult<Pair<E, OptionalInt>>> parseElement(DynamicOps<JsonElement> var1, ResourceKey<? extends Registry<E>> var2, ResourceKey<E> var3, Decoder<E> var4) {
            JsonElement var5 = (JsonElement)this.data.get(var3);
            return var5 == null ? Optional.of(DataResult.error("Unknown element: " + var3)) : Optional.of(var4.parse(var1, var5).setLifecycle((Lifecycle)this.lifecycles.get(var3)).map((var2x) -> {
               return Pair.of(var2x, OptionalInt.of(this.ids.getInt(var3)));
            }));
         }
      }
   }

   static final class ReadCache<E> {
      final Map<ResourceKey<E>, DataResult<Supplier<E>>> values = Maps.newIdentityHashMap();
   }
}
