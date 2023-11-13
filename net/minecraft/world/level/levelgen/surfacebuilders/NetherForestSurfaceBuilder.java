package net.minecraft.world.level.levelgen.surfacebuilders;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

public class NetherForestSurfaceBuilder extends SurfaceBuilder<SurfaceBuilderBaseConfiguration> {
   private static final BlockState AIR;
   protected long seed;
   private PerlinNoise decorationNoise;

   public NetherForestSurfaceBuilder(Codec<SurfaceBuilderBaseConfiguration> var1) {
      super(var1);
   }

   public void apply(Random var1, ChunkAccess var2, Biome var3, int var4, int var5, int var6, double var7, BlockState var9, BlockState var10, int var11, int var12, long var13, SurfaceBuilderBaseConfiguration var15) {
      int var16 = var11;
      int var17 = var4 & 15;
      int var18 = var5 & 15;
      double var19 = this.decorationNoise.getValue((double)var4 * 0.1D, (double)var11, (double)var5 * 0.1D);
      boolean var21 = var19 > 0.15D + var1.nextDouble() * 0.35D;
      double var22 = this.decorationNoise.getValue((double)var4 * 0.1D, 109.0D, (double)var5 * 0.1D);
      boolean var24 = var22 > 0.25D + var1.nextDouble() * 0.9D;
      int var25 = (int)(var7 / 3.0D + 3.0D + var1.nextDouble() * 0.25D);
      BlockPos.MutableBlockPos var26 = new BlockPos.MutableBlockPos();
      int var27 = -1;
      BlockState var28 = var15.getUnderMaterial();

      for(int var29 = 127; var29 >= var12; --var29) {
         var26.set(var17, var29, var18);
         BlockState var30 = var15.getTopMaterial();
         BlockState var31 = var2.getBlockState(var26);
         if (var31.isAir()) {
            var27 = -1;
         } else if (var31.is(var9.getBlock())) {
            if (var27 == -1) {
               boolean var32 = false;
               if (var25 <= 0) {
                  var32 = true;
                  var28 = var15.getUnderMaterial();
               }

               if (var21) {
                  var30 = var15.getUnderMaterial();
               } else if (var24) {
                  var30 = var15.getUnderwaterMaterial();
               }

               if (var29 < var16 && var32) {
                  var30 = var10;
               }

               var27 = var25;
               if (var29 >= var16 - 1) {
                  var2.setBlockState(var26, var30, false);
               } else {
                  var2.setBlockState(var26, var28, false);
               }
            } else if (var27 > 0) {
               --var27;
               var2.setBlockState(var26, var28, false);
            }
         }
      }

   }

   public void initNoise(long var1) {
      if (this.seed != var1 || this.decorationNoise == null) {
         this.decorationNoise = new PerlinNoise(new WorldgenRandom(var1), ImmutableList.of(0));
      }

      this.seed = var1;
   }

   static {
      AIR = Blocks.CAVE_AIR.defaultBlockState();
   }
}
