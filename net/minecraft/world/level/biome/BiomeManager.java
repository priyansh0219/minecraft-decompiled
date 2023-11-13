package net.minecraft.world.level.biome;

import com.google.common.hash.Hashing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

public class BiomeManager {
   static final int CHUNK_CENTER_QUART = QuartPos.fromBlock(8);
   private final BiomeManager.NoiseBiomeSource noiseBiomeSource;
   private final long biomeZoomSeed;
   private final BiomeZoomer zoomer;

   public BiomeManager(BiomeManager.NoiseBiomeSource var1, long var2, BiomeZoomer var4) {
      this.noiseBiomeSource = var1;
      this.biomeZoomSeed = var2;
      this.zoomer = var4;
   }

   public static long obfuscateSeed(long var0) {
      return Hashing.sha256().hashLong(var0).asLong();
   }

   public BiomeManager withDifferentSource(BiomeSource var1) {
      return new BiomeManager(var1, this.biomeZoomSeed, this.zoomer);
   }

   public Biome getBiome(BlockPos var1) {
      return this.zoomer.getBiome(this.biomeZoomSeed, var1.getX(), var1.getY(), var1.getZ(), this.noiseBiomeSource);
   }

   public Biome getNoiseBiomeAtPosition(double var1, double var3, double var5) {
      int var7 = QuartPos.fromBlock(Mth.floor(var1));
      int var8 = QuartPos.fromBlock(Mth.floor(var3));
      int var9 = QuartPos.fromBlock(Mth.floor(var5));
      return this.getNoiseBiomeAtQuart(var7, var8, var9);
   }

   public Biome getNoiseBiomeAtPosition(BlockPos var1) {
      int var2 = QuartPos.fromBlock(var1.getX());
      int var3 = QuartPos.fromBlock(var1.getY());
      int var4 = QuartPos.fromBlock(var1.getZ());
      return this.getNoiseBiomeAtQuart(var2, var3, var4);
   }

   public Biome getNoiseBiomeAtQuart(int var1, int var2, int var3) {
      return this.noiseBiomeSource.getNoiseBiome(var1, var2, var3);
   }

   public Biome getPrimaryBiomeAtChunk(ChunkPos var1) {
      return this.noiseBiomeSource.getPrimaryBiome(var1);
   }

   public interface NoiseBiomeSource {
      Biome getNoiseBiome(int var1, int var2, int var3);

      default Biome getPrimaryBiome(ChunkPos var1) {
         return this.getNoiseBiome(QuartPos.fromSection(var1.x) + BiomeManager.CHUNK_CENTER_QUART, 0, QuartPos.fromSection(var1.z) + BiomeManager.CHUNK_CENTER_QUART);
      }
   }
}
