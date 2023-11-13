package net.minecraft.world.level.levelgen.surfacebuilders;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.Codec;
import java.util.Comparator;
import java.util.Random;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

public abstract class NetherCappedSurfaceBuilder extends SurfaceBuilder<SurfaceBuilderBaseConfiguration> {
   private long seed;
   private ImmutableMap<BlockState, PerlinNoise> floorNoises = ImmutableMap.of();
   private ImmutableMap<BlockState, PerlinNoise> ceilingNoises = ImmutableMap.of();
   private PerlinNoise patchNoise;

   public NetherCappedSurfaceBuilder(Codec<SurfaceBuilderBaseConfiguration> var1) {
      super(var1);
   }

   public void apply(Random var1, ChunkAccess var2, Biome var3, int var4, int var5, int var6, double var7, BlockState var9, BlockState var10, int var11, int var12, long var13, SurfaceBuilderBaseConfiguration var15) {
      int var16 = var11 + 1;
      int var17 = var4 & 15;
      int var18 = var5 & 15;
      int var19 = (int)(var7 / 3.0D + 3.0D + var1.nextDouble() * 0.25D);
      int var20 = (int)(var7 / 3.0D + 3.0D + var1.nextDouble() * 0.25D);
      double var21 = 0.03125D;
      boolean var23 = this.patchNoise.getValue((double)var4 * 0.03125D, 109.0D, (double)var5 * 0.03125D) * 75.0D + var1.nextDouble() > 0.0D;
      BlockState var24 = (BlockState)((Entry)this.ceilingNoises.entrySet().stream().max(Comparator.comparing((var3x) -> {
         return ((PerlinNoise)var3x.getValue()).getValue((double)var4, (double)var11, (double)var5);
      })).get()).getKey();
      BlockState var25 = (BlockState)((Entry)this.floorNoises.entrySet().stream().max(Comparator.comparing((var3x) -> {
         return ((PerlinNoise)var3x.getValue()).getValue((double)var4, (double)var11, (double)var5);
      })).get()).getKey();
      BlockPos.MutableBlockPos var26 = new BlockPos.MutableBlockPos();
      BlockState var27 = var2.getBlockState(var26.set(var17, 128, var18));

      for(int var28 = 127; var28 >= var12; --var28) {
         var26.set(var17, var28, var18);
         BlockState var29 = var2.getBlockState(var26);
         int var30;
         if (var27.is(var9.getBlock()) && (var29.isAir() || var29 == var10)) {
            for(var30 = 0; var30 < var19; ++var30) {
               var26.move(Direction.UP);
               if (!var2.getBlockState(var26).is(var9.getBlock())) {
                  break;
               }

               var2.setBlockState(var26, var24, false);
            }

            var26.set(var17, var28, var18);
         }

         if ((var27.isAir() || var27 == var10) && var29.is(var9.getBlock())) {
            for(var30 = 0; var30 < var20 && var2.getBlockState(var26).is(var9.getBlock()); ++var30) {
               if (var23 && var28 >= var16 - 4 && var28 <= var16 + 1) {
                  var2.setBlockState(var26, this.getPatchBlockState(), false);
               } else {
                  var2.setBlockState(var26, var25, false);
               }

               var26.move(Direction.DOWN);
            }
         }

         var27 = var29;
      }

   }

   public void initNoise(long var1) {
      if (this.seed != var1 || this.patchNoise == null || this.floorNoises.isEmpty() || this.ceilingNoises.isEmpty()) {
         this.floorNoises = initPerlinNoises(this.getFloorBlockStates(), var1);
         this.ceilingNoises = initPerlinNoises(this.getCeilingBlockStates(), var1 + (long)this.floorNoises.size());
         this.patchNoise = new PerlinNoise(new WorldgenRandom(var1 + (long)this.floorNoises.size() + (long)this.ceilingNoises.size()), ImmutableList.of(0));
      }

      this.seed = var1;
   }

   private static ImmutableMap<BlockState, PerlinNoise> initPerlinNoises(ImmutableList<BlockState> var0, long var1) {
      Builder var3 = new Builder();

      for(UnmodifiableIterator var4 = var0.iterator(); var4.hasNext(); ++var1) {
         BlockState var5 = (BlockState)var4.next();
         var3.put(var5, new PerlinNoise(new WorldgenRandom(var1), ImmutableList.of(-4)));
      }

      return var3.build();
   }

   protected abstract ImmutableList<BlockState> getFloorBlockStates();

   protected abstract ImmutableList<BlockState> getCeilingBlockStates();

   protected abstract BlockState getPatchBlockState();
}
