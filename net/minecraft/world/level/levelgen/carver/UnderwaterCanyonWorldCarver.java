package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class UnderwaterCanyonWorldCarver extends CanyonWorldCarver {
   public UnderwaterCanyonWorldCarver(Codec<CanyonCarverConfiguration> var1) {
      super(var1);
      this.replaceableBlocks = ImmutableSet.of(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DIRT, Blocks.COARSE_DIRT, new Block[]{Blocks.PODZOL, Blocks.GRASS_BLOCK, Blocks.TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA, Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA, Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.MYCELIUM, Blocks.SNOW, Blocks.SAND, Blocks.GRAVEL, Blocks.WATER, Blocks.LAVA, Blocks.OBSIDIAN, Blocks.AIR, Blocks.CAVE_AIR});
   }

   protected boolean hasDisallowedLiquid(ChunkAccess var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      return false;
   }

   protected boolean carveBlock(CarvingContext var1, CanyonCarverConfiguration var2, ChunkAccess var3, Function<BlockPos, Biome> var4, BitSet var5, Random var6, BlockPos.MutableBlockPos var7, BlockPos.MutableBlockPos var8, Aquifer var9, MutableBoolean var10) {
      return UnderwaterCaveWorldCarver.carveBlock(this, var3, var6, var7, var8, var9);
   }
}
