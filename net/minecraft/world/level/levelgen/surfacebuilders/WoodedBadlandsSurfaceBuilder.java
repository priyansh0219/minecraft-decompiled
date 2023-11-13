package net.minecraft.world.level.levelgen.surfacebuilders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

public class WoodedBadlandsSurfaceBuilder extends BadlandsSurfaceBuilder {
   private static final BlockState WHITE_TERRACOTTA;
   private static final BlockState ORANGE_TERRACOTTA;
   private static final BlockState TERRACOTTA;

   public WoodedBadlandsSurfaceBuilder(Codec<SurfaceBuilderBaseConfiguration> var1) {
      super(var1);
   }

   public void apply(Random var1, ChunkAccess var2, Biome var3, int var4, int var5, int var6, double var7, BlockState var9, BlockState var10, int var11, int var12, long var13, SurfaceBuilderBaseConfiguration var15) {
      int var16 = var4 & 15;
      int var17 = var5 & 15;
      BlockState var18 = WHITE_TERRACOTTA;
      SurfaceBuilderConfiguration var19 = var3.getGenerationSettings().getSurfaceBuilderConfig();
      BlockState var20 = var19.getUnderMaterial();
      BlockState var21 = var19.getTopMaterial();
      BlockState var22 = var20;
      int var23 = (int)(var7 / 3.0D + 3.0D + var1.nextDouble() * 0.25D);
      boolean var24 = Math.cos(var7 / 3.0D * 3.141592653589793D) > 0.0D;
      int var25 = -1;
      boolean var26 = false;
      int var27 = 0;
      BlockPos.MutableBlockPos var28 = new BlockPos.MutableBlockPos();

      for(int var29 = var6; var29 >= var12; --var29) {
         if (var27 < 15) {
            var28.set(var16, var29, var17);
            BlockState var30 = var2.getBlockState(var28);
            if (var30.isAir()) {
               var25 = -1;
            } else if (var30.is(var9.getBlock())) {
               if (var25 == -1) {
                  var26 = false;
                  if (var23 <= 0) {
                     var18 = Blocks.AIR.defaultBlockState();
                     var22 = var9;
                  } else if (var29 >= var11 - 4 && var29 <= var11 + 1) {
                     var18 = WHITE_TERRACOTTA;
                     var22 = var20;
                  }

                  if (var29 < var11 && (var18 == null || var18.isAir())) {
                     var18 = var10;
                  }

                  var25 = var23 + Math.max(0, var29 - var11);
                  if (var29 >= var11 - 1) {
                     if (var29 > 86 + var23 * 2) {
                        if (var24) {
                           var2.setBlockState(var28, Blocks.COARSE_DIRT.defaultBlockState(), false);
                        } else {
                           var2.setBlockState(var28, Blocks.GRASS_BLOCK.defaultBlockState(), false);
                        }
                     } else if (var29 > var11 + 3 + var23) {
                        BlockState var31;
                        if (var29 >= 64 && var29 <= 127) {
                           if (var24) {
                              var31 = TERRACOTTA;
                           } else {
                              var31 = this.getBand(var4, var29, var5);
                           }
                        } else {
                           var31 = ORANGE_TERRACOTTA;
                        }

                        var2.setBlockState(var28, var31, false);
                     } else {
                        var2.setBlockState(var28, var21, false);
                        var26 = true;
                     }
                  } else {
                     var2.setBlockState(var28, var22, false);
                     if (var22 == WHITE_TERRACOTTA) {
                        var2.setBlockState(var28, ORANGE_TERRACOTTA, false);
                     }
                  }
               } else if (var25 > 0) {
                  --var25;
                  if (var26) {
                     var2.setBlockState(var28, ORANGE_TERRACOTTA, false);
                  } else {
                     var2.setBlockState(var28, this.getBand(var4, var29, var5), false);
                  }
               }

               ++var27;
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
