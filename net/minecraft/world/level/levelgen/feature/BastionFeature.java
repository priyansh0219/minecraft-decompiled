package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;

public class BastionFeature extends JigsawFeature {
   private static final int BASTION_SPAWN_HEIGHT = 33;

   public BastionFeature(Codec<JigsawConfiguration> var1) {
      super(var1, 33, false, false);
   }

   protected boolean isFeatureChunk(ChunkGenerator var1, BiomeSource var2, long var3, WorldgenRandom var5, ChunkPos var6, Biome var7, ChunkPos var8, JigsawConfiguration var9, LevelHeightAccessor var10) {
      return var5.nextInt(5) >= 2;
   }
}
