package net.minecraft.server.packs;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VanillaPackResources implements PackResources, ResourceProvider {
   public static Path generatedDir;
   private static final Logger LOGGER = LogManager.getLogger();
   public static Class<?> clientObject;
   private static final Map<PackType, FileSystem> JAR_FILESYSTEM_BY_TYPE = (Map)Util.make(Maps.newHashMap(), (var0) -> {
      Class var1 = VanillaPackResources.class;
      synchronized(VanillaPackResources.class) {
         PackType[] var2 = PackType.values();
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            PackType var5 = var2[var4];
            URL var6 = VanillaPackResources.class.getResource("/" + var5.getDirectory() + "/.mcassetsroot");

            try {
               URI var7 = var6.toURI();
               if ("jar".equals(var7.getScheme())) {
                  FileSystem var8;
                  try {
                     var8 = FileSystems.getFileSystem(var7);
                  } catch (Exception var11) {
                     var8 = FileSystems.newFileSystem(var7, Collections.emptyMap());
                  }

                  var0.put(var5, var8);
               }
            } catch (IOException | URISyntaxException var12) {
               LOGGER.error("Couldn't get a list of all vanilla resources", var12);
            }
         }

      }
   });
   public final PackMetadataSection packMetadata;
   public final Set<String> namespaces;

   public VanillaPackResources(PackMetadataSection var1, String... var2) {
      this.packMetadata = var1;
      this.namespaces = ImmutableSet.copyOf(var2);
   }

   public InputStream getRootResource(String var1) throws IOException {
      if (!var1.contains("/") && !var1.contains("\\")) {
         if (generatedDir != null) {
            Path var2 = generatedDir.resolve(var1);
            if (Files.exists(var2, new LinkOption[0])) {
               return Files.newInputStream(var2);
            }
         }

         return this.getResourceAsStream(var1);
      } else {
         throw new IllegalArgumentException("Root resources can only be filenames, not paths (no / allowed!)");
      }
   }

   public InputStream getResource(PackType var1, ResourceLocation var2) throws IOException {
      InputStream var3 = this.getResourceAsStream(var1, var2);
      if (var3 != null) {
         return var3;
      } else {
         throw new FileNotFoundException(var2.getPath());
      }
   }

   public Collection<ResourceLocation> getResources(PackType var1, String var2, String var3, int var4, Predicate<String> var5) {
      HashSet var6 = Sets.newHashSet();
      URI var8;
      if (generatedDir != null) {
         try {
            getResources(var6, var4, var2, generatedDir.resolve(var1.getDirectory()), var3, var5);
         } catch (IOException var15) {
         }

         if (var1 == PackType.CLIENT_RESOURCES) {
            Enumeration var7 = null;

            try {
               var7 = clientObject.getClassLoader().getResources(var1.getDirectory() + "/");
            } catch (IOException var14) {
            }

            while(var7 != null && var7.hasMoreElements()) {
               try {
                  var8 = ((URL)var7.nextElement()).toURI();
                  if ("file".equals(var8.getScheme())) {
                     getResources(var6, var4, var2, Paths.get(var8), var3, var5);
                  }
               } catch (IOException | URISyntaxException var13) {
               }
            }
         }
      }

      try {
         URL var16 = VanillaPackResources.class.getResource("/" + var1.getDirectory() + "/.mcassetsroot");
         if (var16 == null) {
            LOGGER.error("Couldn't find .mcassetsroot, cannot load vanilla resources");
            return var6;
         }

         var8 = var16.toURI();
         if ("file".equals(var8.getScheme())) {
            URL var9 = new URL(var16.toString().substring(0, var16.toString().length() - ".mcassetsroot".length()));
            Path var10 = Paths.get(var9.toURI());
            getResources(var6, var4, var2, var10, var3, var5);
         } else if ("jar".equals(var8.getScheme())) {
            Path var17 = ((FileSystem)JAR_FILESYSTEM_BY_TYPE.get(var1)).getPath("/" + var1.getDirectory());
            getResources(var6, var4, "minecraft", var17, var3, var5);
         } else {
            LOGGER.error("Unsupported scheme {} trying to list vanilla resources (NYI?)", var8);
         }
      } catch (NoSuchFileException | FileNotFoundException var11) {
      } catch (IOException | URISyntaxException var12) {
         LOGGER.error("Couldn't get a list of all vanilla resources", var12);
      }

      return var6;
   }

   private static void getResources(Collection<ResourceLocation> var0, int var1, String var2, Path var3, String var4, Predicate<String> var5) throws IOException {
      Path var6 = var3.resolve(var2);
      Stream var7 = Files.walk(var6.resolve(var4), var1, new FileVisitOption[0]);

      try {
         Stream var10000 = var7.filter((var1x) -> {
            return !var1x.endsWith(".mcmeta") && Files.isRegularFile(var1x, new LinkOption[0]) && var5.test(var1x.getFileName().toString());
         }).map((var2x) -> {
            return new ResourceLocation(var2, var6.relativize(var2x).toString().replaceAll("\\\\", "/"));
         });
         Objects.requireNonNull(var0);
         var10000.forEach(var0::add);
      } catch (Throwable var11) {
         if (var7 != null) {
            try {
               var7.close();
            } catch (Throwable var10) {
               var11.addSuppressed(var10);
            }
         }

         throw var11;
      }

      if (var7 != null) {
         var7.close();
      }

   }

   @Nullable
   protected InputStream getResourceAsStream(PackType var1, ResourceLocation var2) {
      String var3 = createPath(var1, var2);
      if (generatedDir != null) {
         Path var10000 = generatedDir;
         String var10001 = var1.getDirectory();
         Path var4 = var10000.resolve(var10001 + "/" + var2.getNamespace() + "/" + var2.getPath());
         if (Files.exists(var4, new LinkOption[0])) {
            try {
               return Files.newInputStream(var4);
            } catch (IOException var7) {
            }
         }
      }

      try {
         URL var8 = VanillaPackResources.class.getResource(var3);
         return isResourceUrlValid(var3, var8) ? var8.openStream() : null;
      } catch (IOException var6) {
         return VanillaPackResources.class.getResourceAsStream(var3);
      }
   }

   private static String createPath(PackType var0, ResourceLocation var1) {
      String var10000 = var0.getDirectory();
      return "/" + var10000 + "/" + var1.getNamespace() + "/" + var1.getPath();
   }

   private static boolean isResourceUrlValid(String var0, @Nullable URL var1) throws IOException {
      return var1 != null && (var1.getProtocol().equals("jar") || FolderPackResources.validatePath(new File(var1.getFile()), var0));
   }

   @Nullable
   protected InputStream getResourceAsStream(String var1) {
      return VanillaPackResources.class.getResourceAsStream("/" + var1);
   }

   public boolean hasResource(PackType var1, ResourceLocation var2) {
      String var3 = createPath(var1, var2);
      if (generatedDir != null) {
         Path var10000 = generatedDir;
         String var10001 = var1.getDirectory();
         Path var4 = var10000.resolve(var10001 + "/" + var2.getNamespace() + "/" + var2.getPath());
         if (Files.exists(var4, new LinkOption[0])) {
            return true;
         }
      }

      try {
         URL var6 = VanillaPackResources.class.getResource(var3);
         return isResourceUrlValid(var3, var6);
      } catch (IOException var5) {
         return false;
      }
   }

   public Set<String> getNamespaces(PackType var1) {
      return this.namespaces;
   }

   @Nullable
   public <T> T getMetadataSection(MetadataSectionSerializer<T> var1) throws IOException {
      try {
         InputStream var2 = this.getRootResource("pack.mcmeta");

         label51: {
            Object var4;
            try {
               if (var2 == null) {
                  break label51;
               }

               Object var3 = AbstractPackResources.getMetadataFromStream(var1, var2);
               if (var3 == null) {
                  break label51;
               }

               var4 = var3;
            } catch (Throwable var6) {
               if (var2 != null) {
                  try {
                     var2.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (var2 != null) {
               var2.close();
            }

            return var4;
         }

         if (var2 != null) {
            var2.close();
         }
      } catch (FileNotFoundException | RuntimeException var7) {
      }

      return var1 == PackMetadataSection.SERIALIZER ? this.packMetadata : null;
   }

   public String getName() {
      return "Default";
   }

   public void close() {
   }

   public Resource getResource(final ResourceLocation var1) throws IOException {
      return new Resource() {
         @Nullable
         InputStream inputStream;

         public void close() throws IOException {
            if (this.inputStream != null) {
               this.inputStream.close();
            }

         }

         public ResourceLocation getLocation() {
            return var1;
         }

         public InputStream getInputStream() {
            try {
               this.inputStream = VanillaPackResources.this.getResource(PackType.CLIENT_RESOURCES, var1);
            } catch (IOException var2) {
               throw new UncheckedIOException("Could not get client resource from vanilla pack", var2);
            }

            return this.inputStream;
         }

         public boolean hasMetadata() {
            return false;
         }

         @Nullable
         public <T> T getMetadata(MetadataSectionSerializer<T> var1x) {
            return null;
         }

         public String getSourceName() {
            return var1.toString();
         }
      };
   }
}
