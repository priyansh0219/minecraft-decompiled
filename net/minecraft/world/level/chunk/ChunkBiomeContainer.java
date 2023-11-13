package net.minecraft.world.level.chunk;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkBiomeContainer implements BiomeManager.NoiseBiomeSource {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int WIDTH_BITS = Mth.ceillog2(16) - 2;
   private static final int HORIZONTAL_MASK;
   public static final int MAX_SIZE;
   private final IdMap<Biome> biomeRegistry;
   private final Biome[] biomes;
   private final int quartMinY;
   private final int quartHeight;

   protected ChunkBiomeContainer(IdMap<Biome> var1, LevelHeightAccessor var2, Biome[] var3) {
      this.biomeRegistry = var1;
      this.biomes = var3;
      this.quartMinY = QuartPos.fromBlock(var2.getMinBuildHeight());
      this.quartHeight = QuartPos.fromBlock(var2.getHeight()) - 1;
   }

   public ChunkBiomeContainer(IdMap<Biome> var1, LevelHeightAccessor var2, int[] var3) {
      this(var1, var2, new Biome[var3.length]);
      int var4 = -1;

      for(int var5 = 0; var5 < this.biomes.length; ++var5) {
         int var6 = var3[var5];
         Biome var7 = (Biome)var1.byId(var6);
         if (var7 == null) {
            if (var4 == -1) {
               var4 = var5;
            }

            this.biomes[var5] = (Biome)var1.byId(0);
         } else {
            this.biomes[var5] = var7;
         }
      }

      if (var4 != -1) {
         LOGGER.warn("Invalid biome data received, starting from {}: {}", var4, Arrays.toString(var3));
      }

   }

   public ChunkBiomeContainer(IdMap<Biome> var1, LevelHeightAccessor var2, ChunkPos var3, BiomeSource var4) {
      this(var1, var2, var3, var4, (int[])null);
   }

   public ChunkBiomeContainer(IdMap<Biome> var1, LevelHeightAccessor var2, ChunkPos var3, BiomeSource var4, @Nullable int[] var5) {
      this(var1, var2, new Biome[(1 << WIDTH_BITS + WIDTH_BITS) * ceilDiv(var2.getHeight(), 4)]);
      int var6 = QuartPos.fromBlock(var3.getMinBlockX());
      int var7 = this.quartMinY;
      int var8 = QuartPos.fromBlock(var3.getMinBlockZ());

      for(int var9 = 0; var9 < this.biomes.length; ++var9) {
         if (var5 != null && var9 < var5.length) {
            this.biomes[var9] = (Biome)var1.byId(var5[var9]);
         }

         if (this.biomes[var9] == null) {
            this.biomes[var9] = generateBiomeForIndex(var4, var6, var7, var8, var9);
         }
      }

   }

   private static int ceilDiv(int var0, int var1) {
      return (var0 + var1 - 1) / var1;
   }

   private static Biome generateBiomeForIndex(BiomeSource var0, int var1, int var2, int var3, int var4) {
      int var5 = var4 & HORIZONTAL_MASK;
      int var6 = var4 >> WIDTH_BITS + WIDTH_BITS;
      int var7 = var4 >> WIDTH_BITS & HORIZONTAL_MASK;
      return var0.getNoiseBiome(var1 + var5, var2 + var6, var3 + var7);
   }

   public int[] writeBiomes() {
      int[] var1 = new int[this.biomes.length];

      for(int var2 = 0; var2 < this.biomes.length; ++var2) {
         var1[var2] = this.biomeRegistry.getId(this.biomes[var2]);
      }

      return var1;
   }

   public Biome getNoiseBiome(int var1, int var2, int var3) {
      int var4 = var1 & HORIZONTAL_MASK;
      int var5 = Mth.clamp((int)(var2 - this.quartMinY), (int)0, (int)this.quartHeight);
      int var6 = var3 & HORIZONTAL_MASK;
      return this.biomes[var5 << WIDTH_BITS + WIDTH_BITS | var6 << WIDTH_BITS | var4];
   }

   static {
      HORIZONTAL_MASK = (1 << WIDTH_BITS) - 1;
      MAX_SIZE = 1 << WIDTH_BITS + WIDTH_BITS + DimensionType.BITS_FOR_Y - 2;
   }
}
