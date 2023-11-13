package net.minecraft.world.level.levelgen.surfacebuilders;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

public class BadlandsSurfaceBuilder extends SurfaceBuilder<SurfaceBuilderBaseConfiguration> {
   protected static final int MAX_CLAY_DEPTH = 15;
   private static final BlockState WHITE_TERRACOTTA;
   private static final BlockState ORANGE_TERRACOTTA;
   private static final BlockState TERRACOTTA;
   private static final BlockState YELLOW_TERRACOTTA;
   private static final BlockState BROWN_TERRACOTTA;
   private static final BlockState RED_TERRACOTTA;
   private static final BlockState LIGHT_GRAY_TERRACOTTA;
   protected BlockState[] clayBands;
   protected long seed;
   protected PerlinSimplexNoise pillarNoise;
   protected PerlinSimplexNoise pillarRoofNoise;
   protected PerlinSimplexNoise clayBandsOffsetNoise;

   public BadlandsSurfaceBuilder(Codec<SurfaceBuilderBaseConfiguration> var1) {
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
                     if (var29 > var11 + 3 + var23) {
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
                     if (var22.is(Blocks.WHITE_TERRACOTTA) || var22.is(Blocks.ORANGE_TERRACOTTA) || var22.is(Blocks.MAGENTA_TERRACOTTA) || var22.is(Blocks.LIGHT_BLUE_TERRACOTTA) || var22.is(Blocks.YELLOW_TERRACOTTA) || var22.is(Blocks.LIME_TERRACOTTA) || var22.is(Blocks.PINK_TERRACOTTA) || var22.is(Blocks.GRAY_TERRACOTTA) || var22.is(Blocks.LIGHT_GRAY_TERRACOTTA) || var22.is(Blocks.CYAN_TERRACOTTA) || var22.is(Blocks.PURPLE_TERRACOTTA) || var22.is(Blocks.BLUE_TERRACOTTA) || var22.is(Blocks.BROWN_TERRACOTTA) || var22.is(Blocks.GREEN_TERRACOTTA) || var22.is(Blocks.RED_TERRACOTTA) || var22.is(Blocks.BLACK_TERRACOTTA)) {
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

   public void initNoise(long var1) {
      if (this.seed != var1 || this.clayBands == null) {
         this.generateBands(var1);
      }

      if (this.seed != var1 || this.pillarNoise == null || this.pillarRoofNoise == null) {
         WorldgenRandom var3 = new WorldgenRandom(var1);
         this.pillarNoise = new PerlinSimplexNoise(var3, IntStream.rangeClosed(-3, 0));
         this.pillarRoofNoise = new PerlinSimplexNoise(var3, ImmutableList.of(0));
      }

      this.seed = var1;
   }

   protected void generateBands(long var1) {
      this.clayBands = new BlockState[64];
      Arrays.fill(this.clayBands, TERRACOTTA);
      WorldgenRandom var3 = new WorldgenRandom(var1);
      this.clayBandsOffsetNoise = new PerlinSimplexNoise(var3, ImmutableList.of(0));

      int var4;
      for(var4 = 0; var4 < 64; ++var4) {
         var4 += var3.nextInt(5) + 1;
         if (var4 < 64) {
            this.clayBands[var4] = ORANGE_TERRACOTTA;
         }
      }

      var4 = var3.nextInt(4) + 2;

      int var5;
      int var6;
      int var7;
      int var8;
      for(var5 = 0; var5 < var4; ++var5) {
         var6 = var3.nextInt(3) + 1;
         var7 = var3.nextInt(64);

         for(var8 = 0; var7 + var8 < 64 && var8 < var6; ++var8) {
            this.clayBands[var7 + var8] = YELLOW_TERRACOTTA;
         }
      }

      var5 = var3.nextInt(4) + 2;

      int var9;
      for(var6 = 0; var6 < var5; ++var6) {
         var7 = var3.nextInt(3) + 2;
         var8 = var3.nextInt(64);

         for(var9 = 0; var8 + var9 < 64 && var9 < var7; ++var9) {
            this.clayBands[var8 + var9] = BROWN_TERRACOTTA;
         }
      }

      var6 = var3.nextInt(4) + 2;

      for(var7 = 0; var7 < var6; ++var7) {
         var8 = var3.nextInt(3) + 1;
         var9 = var3.nextInt(64);

         for(int var10 = 0; var9 + var10 < 64 && var10 < var8; ++var10) {
            this.clayBands[var9 + var10] = RED_TERRACOTTA;
         }
      }

      var7 = var3.nextInt(3) + 3;
      var8 = 0;

      for(var9 = 0; var9 < var7; ++var9) {
         boolean var12 = true;
         var8 += var3.nextInt(16) + 4;

         for(int var11 = 0; var8 + var11 < 64 && var11 < 1; ++var11) {
            this.clayBands[var8 + var11] = WHITE_TERRACOTTA;
            if (var8 + var11 > 1 && var3.nextBoolean()) {
               this.clayBands[var8 + var11 - 1] = LIGHT_GRAY_TERRACOTTA;
            }

            if (var8 + var11 < 63 && var3.nextBoolean()) {
               this.clayBands[var8 + var11 + 1] = LIGHT_GRAY_TERRACOTTA;
            }
         }
      }

   }

   protected BlockState getBand(int var1, int var2, int var3) {
      int var4 = (int)Math.round(this.clayBandsOffsetNoise.getValue((double)var1 / 512.0D, (double)var3 / 512.0D, false) * 2.0D);
      return this.clayBands[(var2 + var4 + 64) % 64];
   }

   static {
      WHITE_TERRACOTTA = Blocks.WHITE_TERRACOTTA.defaultBlockState();
      ORANGE_TERRACOTTA = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
      TERRACOTTA = Blocks.TERRACOTTA.defaultBlockState();
      YELLOW_TERRACOTTA = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
      BROWN_TERRACOTTA = Blocks.BROWN_TERRACOTTA.defaultBlockState();
      RED_TERRACOTTA = Blocks.RED_TERRACOTTA.defaultBlockState();
      LIGHT_GRAY_TERRACOTTA = Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState();
   }
}
