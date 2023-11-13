package net.minecraft.world.level.biome;

import net.minecraft.core.QuartPos;

public enum NearestNeighborBiomeZoomer implements BiomeZoomer {
   INSTANCE;

   public Biome getBiome(long var1, int var3, int var4, int var5, BiomeManager.NoiseBiomeSource var6) {
      return var6.getNoiseBiome(QuartPos.fromBlock(var3), QuartPos.fromBlock(var4), QuartPos.fromBlock(var5));
   }

   // $FF: synthetic method
   private static NearestNeighborBiomeZoomer[] $values() {
      return new NearestNeighborBiomeZoomer[]{INSTANCE};
   }
}
