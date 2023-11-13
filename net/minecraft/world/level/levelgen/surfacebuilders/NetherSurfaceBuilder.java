package net.minecraft.world.level.levelgen.surfacebuilders;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

public class NetherSurfaceBuilder extends SurfaceBuilder<SurfaceBuilderBaseConfiguration> {
   private static final BlockState AIR;
   private static final BlockState GRAVEL;
   private static final BlockState SOUL_SAND;
   protected long seed;
   protected PerlinNoise decorationNoise;

   public NetherSurfaceBuilder(Codec<SurfaceBuilderBaseConfiguration> var1) {
      super(var1);
   }

   public void apply(Random var1, ChunkAccess var2, Biome var3, int var4, int var5, int var6, double var7, BlockState var9, BlockState var10, int var11, int var12, long var13, SurfaceBuilderBaseConfiguration var15) {
      int var16 = var11;
      int var17 = var4 & 15;
      int var18 = var5 & 15;
      double var19 = 0.03125D;
      boolean var21 = this.decorationNoise.getValue((double)var4 * 0.03125D, (double)var5 * 0.03125D, 0.0D) * 75.0D + var1.nextDouble() > 0.0D;
      boolean var22 = this.decorationNoise.getValue((double)var4 * 0.03125D, 109.0D, (double)var5 * 0.03125D) * 75.0D + var1.nextDouble() > 0.0D;
      int var23 = (int)(var7 / 3.0D + 3.0D + var1.nextDouble() * 0.25D);
      BlockPos.MutableBlockPos var24 = new BlockPos.MutableBlockPos();
      int var25 = -1;
      BlockState var26 = var15.getTopMaterial();
      BlockState var27 = var15.getUnderMaterial();

      for(int var28 = 127; var28 >= var12; --var28) {
         var24.set(var17, var28, var18);
         BlockState var29 = var2.getBlockState(var24);
         if (var29.isAir()) {
            var25 = -1;
         } else if (var29.is(var9.getBlock())) {
            if (var25 == -1) {
               boolean var30 = false;
               if (var23 <= 0) {
                  var30 = true;
                  var27 = var15.getUnderMaterial();
               } else if (var28 >= var16 - 4 && var28 <= var16 + 1) {
                  var26 = var15.getTopMaterial();
                  var27 = var15.getUnderMaterial();
                  if (var22) {
                     var26 = GRAVEL;
                     var27 = var15.getUnderMaterial();
                  }

                  if (var21) {
                     var26 = SOUL_SAND;
                     var27 = SOUL_SAND;
                  }
               }

               if (var28 < var16 && var30) {
                  var26 = var10;
               }

               var25 = var23;
               if (var28 >= var16 - 1) {
                  var2.setBlockState(var24, var26, false);
               } else {
                  var2.setBlockState(var24, var27, false);
               }
            } else if (var25 > 0) {
               --var25;
               var2.setBlockState(var24, var27, false);
            }
         }
      }

   }

   public void initNoise(long var1) {
      if (this.seed != var1 || this.decorationNoise == null) {
         this.decorationNoise = new PerlinNoise(new WorldgenRandom(var1), IntStream.rangeClosed(-3, 0));
      }

      this.seed = var1;
   }

   static {
      AIR = Blocks.CAVE_AIR.defaultBlockState();
      GRAVEL = Blocks.GRAVEL.defaultBlockState();
      SOUL_SAND = Blocks.SOUL_SAND.defaultBlockState();
   }
}
