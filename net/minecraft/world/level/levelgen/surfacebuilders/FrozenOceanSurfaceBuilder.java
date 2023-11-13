package net.minecraft.world.level.levelgen.surfacebuilders;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.material.Material;

public class FrozenOceanSurfaceBuilder extends SurfaceBuilder<SurfaceBuilderBaseConfiguration> {
   protected static final BlockState PACKED_ICE;
   protected static final BlockState SNOW_BLOCK;
   private static final BlockState AIR;
   private static final BlockState GRAVEL;
   private static final BlockState ICE;
   private PerlinSimplexNoise icebergNoise;
   private PerlinSimplexNoise icebergRoofNoise;
   private long seed;

   public FrozenOceanSurfaceBuilder(Codec<SurfaceBuilderBaseConfiguration> var1) {
      super(var1);
   }

   public void apply(Random var1, ChunkAccess var2, Biome var3, int var4, int var5, int var6, double var7, BlockState var9, BlockState var10, int var11, int var12, long var13, SurfaceBuilderBaseConfiguration var15) {
      double var16 = 0.0D;
      double var18 = 0.0D;
      BlockPos.MutableBlockPos var20 = new BlockPos.MutableBlockPos();
      float var21 = var3.getTemperature(var20.set(var4, 63, var5));
      double var22 = Math.min(Math.abs(var7), this.icebergNoise.getValue((double)var4 * 0.1D, (double)var5 * 0.1D, false) * 15.0D);
      if (var22 > 1.8D) {
         double var24 = 0.09765625D;
         double var26 = Math.abs(this.icebergRoofNoise.getValue((double)var4 * 0.09765625D, (double)var5 * 0.09765625D, false));
         var16 = var22 * var22 * 1.2D;
         double var28 = Math.ceil(var26 * 40.0D) + 14.0D;
         if (var16 > var28) {
            var16 = var28;
         }

         if (var21 > 0.1F) {
            var16 -= 2.0D;
         }

         if (var16 > 2.0D) {
            var18 = (double)var11 - var16 - 7.0D;
            var16 += (double)var11;
         } else {
            var16 = 0.0D;
         }
      }

      int var38 = var4 & 15;
      int var25 = var5 & 15;
      SurfaceBuilderConfiguration var39 = var3.getGenerationSettings().getSurfaceBuilderConfig();
      BlockState var27 = var39.getUnderMaterial();
      BlockState var40 = var39.getTopMaterial();
      BlockState var29 = var27;
      BlockState var30 = var40;
      int var31 = (int)(var7 / 3.0D + 3.0D + var1.nextDouble() * 0.25D);
      int var32 = -1;
      int var33 = 0;
      int var34 = 2 + var1.nextInt(4);
      int var35 = var11 + 18 + var1.nextInt(10);

      for(int var36 = Math.max(var6, (int)var16 + 1); var36 >= var12; --var36) {
         var20.set(var38, var36, var25);
         if (var2.getBlockState(var20).isAir() && var36 < (int)var16 && var1.nextDouble() > 0.01D) {
            var2.setBlockState(var20, PACKED_ICE, false);
         } else if (var2.getBlockState(var20).getMaterial() == Material.WATER && var36 > (int)var18 && var36 < var11 && var18 != 0.0D && var1.nextDouble() > 0.15D) {
            var2.setBlockState(var20, PACKED_ICE, false);
         }

         BlockState var37 = var2.getBlockState(var20);
         if (var37.isAir()) {
            var32 = -1;
         } else if (!var37.is(var9.getBlock())) {
            if (var37.is(Blocks.PACKED_ICE) && var33 <= var34 && var36 > var35) {
               var2.setBlockState(var20, SNOW_BLOCK, false);
               ++var33;
            }
         } else if (var32 == -1) {
            if (var31 <= 0) {
               var30 = AIR;
               var29 = var9;
            } else if (var36 >= var11 - 4 && var36 <= var11 + 1) {
               var30 = var40;
               var29 = var27;
            }

            if (var36 < var11 && (var30 == null || var30.isAir())) {
               if (var3.getTemperature(var20.set(var4, var36, var5)) < 0.15F) {
                  var30 = ICE;
               } else {
                  var30 = var10;
               }
            }

            var32 = var31;
            if (var36 >= var11 - 1) {
               var2.setBlockState(var20, var30, false);
            } else if (var36 < var11 - 7 - var31) {
               var30 = AIR;
               var29 = var9;
               var2.setBlockState(var20, GRAVEL, false);
            } else {
               var2.setBlockState(var20, var29, false);
            }
         } else if (var32 > 0) {
            --var32;
            var2.setBlockState(var20, var29, false);
            if (var32 == 0 && var29.is(Blocks.SAND) && var31 > 1) {
               var32 = var1.nextInt(4) + Math.max(0, var36 - 63);
               var29 = var29.is(Blocks.RED_SAND) ? Blocks.RED_SANDSTONE.defaultBlockState() : Blocks.SANDSTONE.defaultBlockState();
            }
         }
      }

   }

   public void initNoise(long var1) {
      if (this.seed != var1 || this.icebergNoise == null || this.icebergRoofNoise == null) {
         WorldgenRandom var3 = new WorldgenRandom(var1);
         this.icebergNoise = new PerlinSimplexNoise(var3, IntStream.rangeClosed(-3, 0));
         this.icebergRoofNoise = new PerlinSimplexNoise(var3, ImmutableList.of(0));
      }

      this.seed = var1;
   }

   static {
      PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState();
      SNOW_BLOCK = Blocks.SNOW_BLOCK.defaultBlockState();
      AIR = Blocks.AIR.defaultBlockState();
      GRAVEL = Blocks.GRAVEL.defaultBlockState();
      ICE = Blocks.ICE.defaultBlockState();
   }
}
