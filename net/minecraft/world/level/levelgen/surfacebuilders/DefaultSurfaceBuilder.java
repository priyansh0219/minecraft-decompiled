package net.minecraft.world.level.levelgen.surfacebuilders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

public class DefaultSurfaceBuilder extends SurfaceBuilder<SurfaceBuilderBaseConfiguration> {
   public DefaultSurfaceBuilder(Codec<SurfaceBuilderBaseConfiguration> var1) {
      super(var1);
   }

   public void apply(Random var1, ChunkAccess var2, Biome var3, int var4, int var5, int var6, double var7, BlockState var9, BlockState var10, int var11, int var12, long var13, SurfaceBuilderBaseConfiguration var15) {
      this.apply(var1, var2, var3, var4, var5, var6, var7, var9, var10, var15.getTopMaterial(), var15.getUnderMaterial(), var15.getUnderwaterMaterial(), var11, var12);
   }

   protected void apply(Random var1, ChunkAccess var2, Biome var3, int var4, int var5, int var6, double var7, BlockState var9, BlockState var10, BlockState var11, BlockState var12, BlockState var13, int var14, int var15) {
      BlockPos.MutableBlockPos var16 = new BlockPos.MutableBlockPos();
      int var17 = (int)(var7 / 3.0D + 3.0D + var1.nextDouble() * 0.25D);
      int var19;
      BlockState var21;
      if (var17 == 0) {
         boolean var18 = false;

         for(var19 = var6; var19 >= var15; --var19) {
            var16.set(var4, var19, var5);
            BlockState var20 = var2.getBlockState(var16);
            if (var20.isAir()) {
               var18 = false;
            } else if (var20.is(var9.getBlock())) {
               if (!var18) {
                  if (var19 >= var14) {
                     var21 = Blocks.AIR.defaultBlockState();
                  } else if (var19 == var14 - 1) {
                     var21 = var3.getTemperature(var16) < 0.15F ? Blocks.ICE.defaultBlockState() : var10;
                  } else if (var19 >= var14 - (7 + var17)) {
                     var21 = var9;
                  } else {
                     var21 = var13;
                  }

                  var2.setBlockState(var16, var21, false);
               }

               var18 = true;
            }
         }
      } else {
         BlockState var23 = var12;
         var19 = -1;

         for(int var24 = var6; var24 >= var15; --var24) {
            var16.set(var4, var24, var5);
            var21 = var2.getBlockState(var16);
            if (var21.isAir()) {
               var19 = -1;
            } else if (var21.is(var9.getBlock())) {
               if (var19 == -1) {
                  var19 = var17;
                  BlockState var22;
                  if (var24 >= var14 + 2) {
                     var22 = var11;
                  } else if (var24 >= var14 - 1) {
                     var23 = var12;
                     var22 = var11;
                  } else if (var24 >= var14 - 4) {
                     var23 = var12;
                     var22 = var12;
                  } else if (var24 >= var14 - (7 + var17)) {
                     var22 = var23;
                  } else {
                     var23 = var9;
                     var22 = var13;
                  }

                  var2.setBlockState(var16, var22, false);
               } else if (var19 > 0) {
                  --var19;
                  var2.setBlockState(var16, var23, false);
                  if (var19 == 0 && var23.is(Blocks.SAND) && var17 > 1) {
                     var19 = var1.nextInt(4) + Math.max(0, var24 - var14);
                     var23 = var23.is(Blocks.RED_SAND) ? Blocks.RED_SANDSTONE.defaultBlockState() : Blocks.SANDSTONE.defaultBlockState();
                  }
               }
            }
         }
      }

   }
}
