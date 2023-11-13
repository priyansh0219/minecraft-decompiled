package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.BaseStoneSource;
import net.minecraft.world.level.levelgen.SingleBaseStoneSource;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.mutable.MutableBoolean;

public abstract class WorldCarver<C extends CarverConfiguration> {
   public static final WorldCarver<CaveCarverConfiguration> CAVE;
   public static final WorldCarver<CaveCarverConfiguration> NETHER_CAVE;
   public static final WorldCarver<CanyonCarverConfiguration> CANYON;
   public static final WorldCarver<CanyonCarverConfiguration> UNDERWATER_CANYON;
   public static final WorldCarver<CaveCarverConfiguration> UNDERWATER_CAVE;
   protected static final BaseStoneSource STONE_SOURCE;
   protected static final BlockState AIR;
   protected static final BlockState CAVE_AIR;
   protected static final FluidState WATER;
   protected static final FluidState LAVA;
   protected Set<Block> replaceableBlocks;
   protected Set<Fluid> liquids;
   private final Codec<ConfiguredWorldCarver<C>> configuredCodec;

   private static <C extends CarverConfiguration, F extends WorldCarver<C>> F register(String var0, F var1) {
      return (WorldCarver)Registry.register(Registry.CARVER, (String)var0, var1);
   }

   public WorldCarver(Codec<C> var1) {
      this.replaceableBlocks = ImmutableSet.of(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DIRT, Blocks.COARSE_DIRT, new Block[]{Blocks.PODZOL, Blocks.GRASS_BLOCK, Blocks.TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA, Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA, Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.MYCELIUM, Blocks.SNOW, Blocks.PACKED_ICE, Blocks.DEEPSLATE, Blocks.TUFF, Blocks.GRANITE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.RAW_COPPER_BLOCK});
      this.liquids = ImmutableSet.of(Fluids.WATER);
      this.configuredCodec = var1.fieldOf("config").xmap(this::configured, ConfiguredWorldCarver::config).codec();
   }

   public ConfiguredWorldCarver<C> configured(C var1) {
      return new ConfiguredWorldCarver(this, var1);
   }

   public Codec<ConfiguredWorldCarver<C>> configuredCodec() {
      return this.configuredCodec;
   }

   public int getRange() {
      return 4;
   }

   protected boolean carveEllipsoid(CarvingContext var1, C var2, ChunkAccess var3, Function<BlockPos, Biome> var4, long var5, Aquifer var7, double var8, double var10, double var12, double var14, double var16, BitSet var18, WorldCarver.CarveSkipChecker var19) {
      ChunkPos var20 = var3.getPos();
      int var21 = var20.x;
      int var22 = var20.z;
      Random var23 = new Random(var5 + (long)var21 + (long)var22);
      double var24 = (double)var20.getMiddleBlockX();
      double var26 = (double)var20.getMiddleBlockZ();
      double var28 = 16.0D + var14 * 2.0D;
      if (!(Math.abs(var8 - var24) > var28) && !(Math.abs(var12 - var26) > var28)) {
         int var30 = var20.getMinBlockX();
         int var31 = var20.getMinBlockZ();
         int var32 = Math.max(Mth.floor(var8 - var14) - var30 - 1, 0);
         int var33 = Math.min(Mth.floor(var8 + var14) - var30, 15);
         int var34 = Math.max(Mth.floor(var10 - var16) - 1, var1.getMinGenY() + 1);
         int var35 = Math.min(Mth.floor(var10 + var16) + 1, var1.getMinGenY() + var1.getGenDepth() - 8);
         int var36 = Math.max(Mth.floor(var12 - var14) - var31 - 1, 0);
         int var37 = Math.min(Mth.floor(var12 + var14) - var31, 15);
         if (!var2.aquifersEnabled && this.hasDisallowedLiquid(var3, var32, var33, var34, var35, var36, var37)) {
            return false;
         } else {
            boolean var38 = false;
            BlockPos.MutableBlockPos var39 = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos var40 = new BlockPos.MutableBlockPos();

            for(int var41 = var32; var41 <= var33; ++var41) {
               int var42 = var20.getBlockX(var41);
               double var43 = ((double)var42 + 0.5D - var8) / var14;

               for(int var45 = var36; var45 <= var37; ++var45) {
                  int var46 = var20.getBlockZ(var45);
                  double var47 = ((double)var46 + 0.5D - var12) / var14;
                  if (!(var43 * var43 + var47 * var47 >= 1.0D)) {
                     MutableBoolean var49 = new MutableBoolean(false);

                     for(int var50 = var35; var50 > var34; --var50) {
                        double var51 = ((double)var50 - 0.5D - var10) / var16;
                        if (!var19.shouldSkip(var1, var43, var51, var47, var50)) {
                           int var53 = var50 - var1.getMinGenY();
                           int var54 = var41 | var45 << 4 | var53 << 8;
                           if (!var18.get(var54) || isDebugEnabled(var2)) {
                              var18.set(var54);
                              var39.set(var42, var50, var46);
                              var38 |= this.carveBlock(var1, var2, var3, var4, var18, var23, var39, var40, var7, var49);
                           }
                        }
                     }
                  }
               }
            }

            return var38;
         }
      } else {
         return false;
      }
   }

   protected boolean carveBlock(CarvingContext var1, C var2, ChunkAccess var3, Function<BlockPos, Biome> var4, BitSet var5, Random var6, BlockPos.MutableBlockPos var7, BlockPos.MutableBlockPos var8, Aquifer var9, MutableBoolean var10) {
      BlockState var11 = var3.getBlockState(var7);
      BlockState var12 = var3.getBlockState(var8.setWithOffset(var7, (Direction)Direction.UP));
      if (var11.is(Blocks.GRASS_BLOCK) || var11.is(Blocks.MYCELIUM)) {
         var10.setTrue();
      }

      if (!this.canReplaceBlock(var11, var12) && !isDebugEnabled(var2)) {
         return false;
      } else {
         BlockState var13 = this.getCarveState(var1, var2, var7, var9);
         if (var13 == null) {
            return false;
         } else {
            var3.setBlockState(var7, var13, false);
            if (var10.isTrue()) {
               var8.setWithOffset(var7, (Direction)Direction.DOWN);
               if (var3.getBlockState(var8).is(Blocks.DIRT)) {
                  var3.setBlockState(var8, ((Biome)var4.apply(var7)).getGenerationSettings().getSurfaceBuilderConfig().getTopMaterial(), false);
               }
            }

            return true;
         }
      }
   }

   @Nullable
   private BlockState getCarveState(CarvingContext var1, C var2, BlockPos var3, Aquifer var4) {
      if (var3.getY() <= var2.lavaLevel.resolveY(var1)) {
         return LAVA.createLegacyBlock();
      } else if (!var2.aquifersEnabled) {
         return isDebugEnabled(var2) ? getDebugState(var2, AIR) : AIR;
      } else {
         BlockState var5 = var4.computeState(STONE_SOURCE, var3.getX(), var3.getY(), var3.getZ(), 0.0D);
         if (var5 == Blocks.STONE.defaultBlockState()) {
            return isDebugEnabled(var2) ? var2.debugSettings.getBarrierState() : null;
         } else {
            return isDebugEnabled(var2) ? getDebugState(var2, var5) : var5;
         }
      }
   }

   private static BlockState getDebugState(CarverConfiguration var0, BlockState var1) {
      if (var1.is(Blocks.AIR)) {
         return var0.debugSettings.getAirState();
      } else if (var1.is(Blocks.WATER)) {
         BlockState var2 = var0.debugSettings.getWaterState();
         return var2.hasProperty(BlockStateProperties.WATERLOGGED) ? (BlockState)var2.setValue(BlockStateProperties.WATERLOGGED, true) : var2;
      } else {
         return var1.is(Blocks.LAVA) ? var0.debugSettings.getLavaState() : var1;
      }
   }

   public abstract boolean carve(CarvingContext var1, C var2, ChunkAccess var3, Function<BlockPos, Biome> var4, Random var5, Aquifer var6, ChunkPos var7, BitSet var8);

   public abstract boolean isStartChunk(C var1, Random var2);

   protected boolean canReplaceBlock(BlockState var1) {
      return this.replaceableBlocks.contains(var1.getBlock());
   }

   protected boolean canReplaceBlock(BlockState var1, BlockState var2) {
      return this.canReplaceBlock(var1) || (var1.is(Blocks.SAND) || var1.is(Blocks.GRAVEL)) && !var2.getFluidState().is(FluidTags.WATER);
   }

   protected boolean hasDisallowedLiquid(ChunkAccess var1, int var2, int var3, int var4, int var5, int var6, int var7) {
      ChunkPos var8 = var1.getPos();
      int var9 = var8.getMinBlockX();
      int var10 = var8.getMinBlockZ();
      BlockPos.MutableBlockPos var11 = new BlockPos.MutableBlockPos();

      for(int var12 = var2; var12 <= var3; ++var12) {
         for(int var13 = var6; var13 <= var7; ++var13) {
            for(int var14 = var4 - 1; var14 <= var5 + 1; ++var14) {
               var11.set(var9 + var12, var14, var10 + var13);
               if (this.liquids.contains(var1.getFluidState(var11).getType())) {
                  return true;
               }

               if (var14 != var5 + 1 && !isEdge(var12, var13, var2, var3, var6, var7)) {
                  var14 = var5;
               }
            }
         }
      }

      return false;
   }

   private static boolean isEdge(int var0, int var1, int var2, int var3, int var4, int var5) {
      return var0 == var2 || var0 == var3 || var1 == var4 || var1 == var5;
   }

   protected static boolean canReach(ChunkPos var0, double var1, double var3, int var5, int var6, float var7) {
      double var8 = (double)var0.getMiddleBlockX();
      double var10 = (double)var0.getMiddleBlockZ();
      double var12 = var1 - var8;
      double var14 = var3 - var10;
      double var16 = (double)(var6 - var5);
      double var18 = (double)(var7 + 2.0F + 16.0F);
      return var12 * var12 + var14 * var14 - var16 * var16 <= var18 * var18;
   }

   private static boolean isDebugEnabled(CarverConfiguration var0) {
      return var0.debugSettings.isDebugMode();
   }

   static {
      CAVE = register("cave", new CaveWorldCarver(CaveCarverConfiguration.CODEC));
      NETHER_CAVE = register("nether_cave", new NetherWorldCarver(CaveCarverConfiguration.CODEC));
      CANYON = register("canyon", new CanyonWorldCarver(CanyonCarverConfiguration.CODEC));
      UNDERWATER_CANYON = register("underwater_canyon", new UnderwaterCanyonWorldCarver(CanyonCarverConfiguration.CODEC));
      UNDERWATER_CAVE = register("underwater_cave", new UnderwaterCaveWorldCarver(CaveCarverConfiguration.CODEC));
      STONE_SOURCE = new SingleBaseStoneSource(Blocks.STONE.defaultBlockState());
      AIR = Blocks.AIR.defaultBlockState();
      CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
      WATER = Fluids.WATER.defaultFluidState();
      LAVA = Fluids.LAVA.defaultFluidState();
   }

   public interface CarveSkipChecker {
      boolean shouldSkip(CarvingContext var1, double var2, double var4, double var6, int var8);
   }
}
