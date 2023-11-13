package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class UnderwaterCaveWorldCarver extends CaveWorldCarver {
   public UnderwaterCaveWorldCarver(Codec<CaveCarverConfiguration> var1) {
      super(var1);
      this.replaceableBlocks = ImmutableSet.of(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DIRT, Blocks.COARSE_DIRT, new Block[]{Blocks.PODZOL, Blocks.GRASS_BLOCK, Blocks.TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA, Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA, Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.MYCELIUM, Blocks.SNOW, Blocks.SAND, Blocks.GRAVEL, Blocks.WATER, Blocks.LAVA, Blocks.OBSIDIAN, Blocks.PACKED_ICE});
   }

   protected boolean hasDisallowedLiquid(ChunkAccess var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      return false;
   }

   protected boolean carveBlock(CarvingContext var1, CaveCarverConfiguration var2, ChunkAccess var3, Function<BlockPos, Biome> var4, BitSet var5, Random var6, BlockPos.MutableBlockPos var7, BlockPos.MutableBlockPos var8, Aquifer var9, MutableBoolean var10) {
      return carveBlock(this, var3, var6, var7, var8, var9);
   }

   protected static boolean carveBlock(WorldCarver<?> var0, ChunkAccess var1, Random var2, BlockPos.MutableBlockPos var3, BlockPos.MutableBlockPos var4, Aquifer var5) {
      if (var5.computeState(WorldCarver.STONE_SOURCE, var3.getX(), var3.getY(), var3.getZ(), Double.NEGATIVE_INFINITY).isAir()) {
         return false;
      } else {
         BlockState var6 = var1.getBlockState(var3);
         if (!var0.canReplaceBlock(var6)) {
            return false;
         } else if (var3.getY() == 10) {
            float var11 = var2.nextFloat();
            if ((double)var11 < 0.25D) {
               var1.setBlockState(var3, Blocks.MAGMA_BLOCK.defaultBlockState(), false);
               var1.getBlockTicks().scheduleTick(var3, Blocks.MAGMA_BLOCK, 0);
            } else {
               var1.setBlockState(var3, Blocks.OBSIDIAN.defaultBlockState(), false);
            }

            return true;
         } else if (var3.getY() < 10) {
            var1.setBlockState(var3, Blocks.LAVA.defaultBlockState(), false);
            return false;
         } else {
            var1.setBlockState(var3, WATER.createLegacyBlock(), false);
            int var7 = var1.getPos().x;
            int var8 = var1.getPos().z;
            UnmodifiableIterator var9 = LiquidBlock.POSSIBLE_FLOW_DIRECTIONS.iterator();

            while(var9.hasNext()) {
               Direction var10 = (Direction)var9.next();
               var4.setWithOffset(var3, (Direction)var10);
               if (SectionPos.blockToSectionCoord(var4.getX()) != var7 || SectionPos.blockToSectionCoord(var4.getZ()) != var8 || var1.getBlockState(var4).isAir()) {
                  var1.getLiquidTicks().scheduleTick(var3, WATER.getType(), 0);
                  break;
               }
            }

            return true;
         }
      }
   }
}
