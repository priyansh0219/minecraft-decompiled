package net.minecraft.world.level.levelgen.surfacebuilders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

public class ErodedBadlandsSurfaceBuilder extends BadlandsSurfaceBuilder {
   private static final BlockState WHITE_TERRACOTTA;
   private static final BlockState ORANGE_TERRACOTTA;
   private static final BlockState TERRACOTTA;

   public ErodedBadlandsSurfaceBuilder(Codec<SurfaceBuilderBaseConfiguration> var1) {
      super(var1);
   }

   public void apply(Random var1, ChunkAccess var2, Biome var3, int var4, int var5, int var6, double var7, BlockState var9, BlockState var10, int var11, int var12, long var13, SurfaceBuilderBaseConfiguration var15) {
      double var16 = 0.0D;
      double var18 = Math.min(Math.abs(var7), this.pillarNoise.getValue((double)var4 * 0.25D, (double)var5 * 0.25D, false) * 15.0D);
      if (var18 > 0.0D) {
         double var20 = 0.001953125D;
         double var22 = Math.abs(this.pillarRoofNoise.getValue((double)var4 * 0.001953125D, (double)var5 * 0.001953125D, false));
         var16 = var18 * var18 * 2.5D;
         double var24 = Math.ceil(var22 * 50.0D) + 14.0D;
         if (var16 > var24) {
            var16 = var24;
         }

         var16 += 64.0D;
      }

      int var35 = var4 & 15;
      int var21 = var5 & 15;
      BlockState var36 = WHITE_TERRACOTTA;
      SurfaceBuilderConfiguration var23 = var3.getGenerationSettings().getSurfaceBuilderConfig();
      BlockState var37 = var23.getUnderMaterial();
      BlockState var25 = var23.getTopMaterial();
      BlockState var26 = var37;
      int var27 = (int)(var7 / 3.0D + 3.0D + var1.nextDouble() * 0.25D);
      boolean var28 = Math.cos(var7 / 3.0D * 3.141592653589793D) > 0.0D;
      int var29 = -1;
      boolean var30 = false;
      BlockPos.MutableBlockPos var31 = new BlockPos.MutableBlockPos();

      for(int var32 = Math.max(var6, (int)var16 + 1); var32 >= var12; --var32) {
         var31.set(var35, var32, var21);
         if (var2.getBlockState(var31).isAir() && var32 < (int)var16) {
            var2.setBlockState(var31, var9, false);
         }

         BlockState var33 = var2.getBlockState(var31);
         if (var33.isAir()) {
            var29 = -1;
         } else if (var33.is(var9.getBlock())) {
            if (var29 == -1) {
               var30 = false;
               if (var27 <= 0) {
                  var36 = Blocks.AIR.defaultBlockState();
                  var26 = var9;
               } else if (var32 >= var11 - 4 && var32 <= var11 + 1) {
                  var36 = WHITE_TERRACOTTA;
                  var26 = var37;
               }

               if (var32 < var11 && (var36 == null || var36.isAir())) {
                  var36 = var10;
               }

               var29 = var27 + Math.max(0, var32 - var11);
               if (var32 >= var11 - 1) {
                  if (var32 <= var11 + 3 + var27) {
                     var2.setBlockState(var31, var25, false);
                     var30 = true;
                  } else {
                     BlockState var34;
                     if (var32 >= 64 && var32 <= 127) {
                        if (var28) {
                           var34 = TERRACOTTA;
                        } else {
                           var34 = this.getBand(var4, var32, var5);
                        }
                     } else {
                        var34 = ORANGE_TERRACOTTA;
                     }

                     var2.setBlockState(var31, var34, false);
                  }
               } else {
                  var2.setBlockState(var31, var26, false);
                  if (var26.is(Blocks.WHITE_TERRACOTTA) || var26.is(Blocks.ORANGE_TERRACOTTA) || var26.is(Blocks.MAGENTA_TERRACOTTA) || var26.is(Blocks.LIGHT_BLUE_TERRACOTTA) || var26.is(Blocks.YELLOW_TERRACOTTA) || var26.is(Blocks.LIME_TERRACOTTA) || var26.is(Blocks.PINK_TERRACOTTA) || var26.is(Blocks.GRAY_TERRACOTTA) || var26.is(Blocks.LIGHT_GRAY_TERRACOTTA) || var26.is(Blocks.CYAN_TERRACOTTA) || var26.is(Blocks.PURPLE_TERRACOTTA) || var26.is(Blocks.BLUE_TERRACOTTA) || var26.is(Blocks.BROWN_TERRACOTTA) || var26.is(Blocks.GREEN_TERRACOTTA) || var26.is(Blocks.RED_TERRACOTTA) || var26.is(Blocks.BLACK_TERRACOTTA)) {
                     var2.setBlockState(var31, ORANGE_TERRACOTTA, false);
                  }
               }
            } else if (var29 > 0) {
               --var29;
               if (var30) {
                  var2.setBlockState(var31, ORANGE_TERRACOTTA, false);
               } else {
                  var2.setBlockState(var31, this.getBand(var4, var32, var5), false);
               }
            }
         }
      }

   }

   static {
      WHITE_TERRACOTTA = Blocks.WHITE_TERRACOTTA.defaultBlockState();
      ORANGE_TERRACOTTA = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
      TERRACOTTA = Blocks.TERRACOTTA.defaultBlockState();
   }
}
