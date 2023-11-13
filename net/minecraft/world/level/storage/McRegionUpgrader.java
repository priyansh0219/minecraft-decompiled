package net.minecraft.world.level.storage;

import com.google.common.collect.Lists;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.OverworldBiomeSource;
import net.minecraft.world.level.chunk.storage.OldChunkStorage;
import net.minecraft.world.level.chunk.storage.RegionFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class McRegionUpgrader {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String MCREGION_EXTENSION = ".mcr";

   static boolean convertLevel(LevelStorageSource.LevelStorageAccess var0, ProgressListener var1) {
      var1.progressStagePercentage(0);
      ArrayList var2 = Lists.newArrayList();
      ArrayList var3 = Lists.newArrayList();
      ArrayList var4 = Lists.newArrayList();
      File var5 = var0.getDimensionPath(Level.OVERWORLD);
      File var6 = var0.getDimensionPath(Level.NETHER);
      File var7 = var0.getDimensionPath(Level.END);
      LOGGER.info("Scanning folders...");
      addRegionFiles(var5, var2);
      if (var6.exists()) {
         addRegionFiles(var6, var3);
      }

      if (var7.exists()) {
         addRegionFiles(var7, var4);
      }

      int var8 = var2.size() + var3.size() + var4.size();
      LOGGER.info("Total conversion count is {}", var8);
      RegistryAccess.RegistryHolder var9 = RegistryAccess.builtin();
      RegistryReadOps var10 = RegistryReadOps.createAndLoad(NbtOps.INSTANCE, (ResourceManager)ResourceManager.Empty.INSTANCE, var9);
      WorldData var11 = var0.getDataTag(var10, DataPackConfig.DEFAULT);
      long var12 = var11 != null ? var11.worldGenSettings().seed() : 0L;
      Registry var15 = var9.registryOrThrow(Registry.BIOME_REGISTRY);
      Object var14;
      if (var11 != null && var11.worldGenSettings().isFlatWorld()) {
         var14 = new FixedBiomeSource((Biome)var15.getOrThrow(Biomes.PLAINS));
      } else {
         var14 = new OverworldBiomeSource(var12, false, false, var15);
      }

      convertRegions(var9, new File(var5, "region"), var2, (BiomeSource)var14, 0, var8, var1);
      convertRegions(var9, new File(var6, "region"), var3, new FixedBiomeSource((Biome)var15.getOrThrow(Biomes.NETHER_WASTES)), var2.size(), var8, var1);
      convertRegions(var9, new File(var7, "region"), var4, new FixedBiomeSource((Biome)var15.getOrThrow(Biomes.THE_END)), var2.size() + var3.size(), var8, var1);
      makeMcrLevelDatBackup(var0);
      var0.saveDataTag(var9, var11);
      return true;
   }

   private static void makeMcrLevelDatBackup(LevelStorageSource.LevelStorageAccess var0) {
      File var1 = var0.getLevelPath(LevelResource.LEVEL_DATA_FILE).toFile();
      if (!var1.exists()) {
         LOGGER.warn("Unable to create level.dat_mcr backup");
      } else {
         File var2 = new File(var1.getParent(), "level.dat_mcr");
         if (!var1.renameTo(var2)) {
            LOGGER.warn("Unable to create level.dat_mcr backup");
         }

      }
   }

   private static void convertRegions(RegistryAccess.RegistryHolder var0, File var1, Iterable<File> var2, BiomeSource var3, int var4, int var5, ProgressListener var6) {
      Iterator var7 = var2.iterator();

      while(var7.hasNext()) {
         File var8 = (File)var7.next();
         convertRegion(var0, var1, var8, var3, var4, var5, var6);
         ++var4;
         int var9 = (int)Math.round(100.0D * (double)var4 / (double)var5);
         var6.progressStagePercentage(var9);
      }

   }

   private static void convertRegion(RegistryAccess.RegistryHolder var0, File var1, File var2, BiomeSource var3, int var4, int var5, ProgressListener var6) {
      String var7 = var2.getName();

      try {
         RegionFile var8 = new RegionFile(var2, var1, true);

         try {
            String var10005 = var7.substring(0, var7.length() - ".mcr".length());
            RegionFile var9 = new RegionFile(new File(var1, var10005 + ".mca"), var1, true);

            try {
               for(int var10 = 0; var10 < 32; ++var10) {
                  int var11;
                  for(var11 = 0; var11 < 32; ++var11) {
                     ChunkPos var12 = new ChunkPos(var10, var11);
                     if (var8.hasChunk(var12) && !var9.hasChunk(var12)) {
                        CompoundTag var13;
                        try {
                           label141: {
                              DataInputStream var14 = var8.getChunkDataInputStream(var12);

                              label142: {
                                 try {
                                    if (var14 == null) {
                                       LOGGER.warn("Failed to fetch input stream for chunk {}", var12);
                                       break label142;
                                    }

                                    var13 = NbtIo.read((DataInput)var14);
                                 } catch (Throwable var26) {
                                    if (var14 != null) {
                                       try {
                                          var14.close();
                                       } catch (Throwable var24) {
                                          var26.addSuppressed(var24);
                                       }
                                    }

                                    throw var26;
                                 }

                                 if (var14 != null) {
                                    var14.close();
                                 }
                                 break label141;
                              }

                              if (var14 != null) {
                                 var14.close();
                              }
                              continue;
                           }
                        } catch (IOException var27) {
                           LOGGER.warn("Failed to read data for chunk {}", var12, var27);
                           continue;
                        }

                        CompoundTag var32 = var13.getCompound("Level");
                        OldChunkStorage.OldLevelChunk var15 = OldChunkStorage.load(var32);
                        CompoundTag var16 = new CompoundTag();
                        CompoundTag var17 = new CompoundTag();
                        var16.put("Level", var17);
                        OldChunkStorage.convertToAnvilFormat(var0, var15, var17, var3);
                        DataOutputStream var18 = var9.getChunkDataOutputStream(var12);

                        try {
                           NbtIo.write(var16, (DataOutput)var18);
                        } catch (Throwable var25) {
                           if (var18 != null) {
                              try {
                                 var18.close();
                              } catch (Throwable var23) {
                                 var25.addSuppressed(var23);
                              }
                           }

                           throw var25;
                        }

                        if (var18 != null) {
                           var18.close();
                        }
                     }
                  }

                  var11 = (int)Math.round(100.0D * (double)(var4 * 1024) / (double)(var5 * 1024));
                  int var31 = (int)Math.round(100.0D * (double)((var10 + 1) * 32 + var4 * 1024) / (double)(var5 * 1024));
                  if (var31 > var11) {
                     var6.progressStagePercentage(var31);
                  }
               }
            } catch (Throwable var28) {
               try {
                  var9.close();
               } catch (Throwable var22) {
                  var28.addSuppressed(var22);
               }

               throw var28;
            }

            var9.close();
         } catch (Throwable var29) {
            try {
               var8.close();
            } catch (Throwable var21) {
               var29.addSuppressed(var21);
            }

            throw var29;
         }

         var8.close();
      } catch (IOException var30) {
         LOGGER.error("Failed to upgrade region file {}", var2, var30);
      }

   }

   private static void addRegionFiles(File var0, Collection<File> var1) {
      File var2 = new File(var0, "region");
      File[] var3 = var2.listFiles((var0x, var1x) -> {
         return var1x.endsWith(".mcr");
      });
      if (var3 != null) {
         Collections.addAll(var1, var3);
      }

   }
}
