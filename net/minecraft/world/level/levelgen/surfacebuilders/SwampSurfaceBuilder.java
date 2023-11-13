package net.minecraft.world.level.levelgen.surfacebuilders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

public class SwampSurfaceBuilder extends SurfaceBuilder<SurfaceBuilderBaseConfiguration> {
   public SwampSurfaceBuilder(Codec<SurfaceBuilderBaseConfiguration> var1) {
      super(var1);
   }

   public void apply(Random var1, ChunkAccess var2, Biome var3, int var4, int var5, int var6, double var7, BlockState var9, BlockState var10, int var11, int var12, long var13, SurfaceBuilderBaseConfiguration var15) {
      double var16 = Biome.BIOME_INFO_NOISE.getValue((double)var4 * 0.25D, (double)var5 * 0.25D, false);
      if (var16 > 0.0D) {
         int var18 = var4 & 15;
         int var19 = var5 & 15;
         BlockPos.MutableBlockPos var20 = new BlockPos.MutableBlockPos();

         for(int var21 = var6; var21 >= var12; --var21) {
            var20.set(var18, var21, var19);
            if (!var2.getBlockState(var20).isAir()) {
               if (var21 == 62 && !var2.getBlockState(var20).is(var10.getBlock())) {
                  var2.setBlockState(var20, var10, false);
               }
               break;
            }
         }
      }

      SurfaceBuilder.DEFAULT.apply(var1, var2, var3, var4, var5, var6, var7, var9, var10, var11, var12, var13, var15);
   }
}
