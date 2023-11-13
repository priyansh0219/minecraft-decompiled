package net.minecraft.world.phys.shapes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class Shapes {
   public static final double EPSILON = 1.0E-7D;
   public static final double BIG_EPSILON = 1.0E-6D;
   private static final VoxelShape BLOCK = (VoxelShape)Util.make(() -> {
      BitSetDiscreteVoxelShape var0 = new BitSetDiscreteVoxelShape(1, 1, 1);
      var0.fill(0, 0, 0);
      return new CubeVoxelShape(var0);
   });
   public static final VoxelShape INFINITY = box(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
   private static final VoxelShape EMPTY = new ArrayVoxelShape(new BitSetDiscreteVoxelShape(0, 0, 0), new DoubleArrayList(new double[]{0.0D}), new DoubleArrayList(new double[]{0.0D}), new DoubleArrayList(new double[]{0.0D}));

   public static VoxelShape empty() {
      return EMPTY;
   }

   public static VoxelShape block() {
      return BLOCK;
   }

   public static VoxelShape box(double var0, double var2, double var4, double var6, double var8, double var10) {
      if (!(var0 > var6) && !(var2 > var8) && !(var4 > var10)) {
         return create(var0, var2, var4, var6, var8, var10);
      } else {
         throw new IllegalArgumentException("The min values need to be smaller or equals to the max values");
      }
   }

   public static VoxelShape create(double var0, double var2, double var4, double var6, double var8, double var10) {
      if (!(var6 - var0 < 1.0E-7D) && !(var8 - var2 < 1.0E-7D) && !(var10 - var4 < 1.0E-7D)) {
         int var12 = findBits(var0, var6);
         int var13 = findBits(var2, var8);
         int var14 = findBits(var4, var10);
         if (var12 >= 0 && var13 >= 0 && var14 >= 0) {
            if (var12 == 0 && var13 == 0 && var14 == 0) {
               return block();
            } else {
               int var15 = 1 << var12;
               int var16 = 1 << var13;
               int var17 = 1 << var14;
               BitSetDiscreteVoxelShape var18 = BitSetDiscreteVoxelShape.withFilledBounds(var15, var16, var17, (int)Math.round(var0 * (double)var15), (int)Math.round(var2 * (double)var16), (int)Math.round(var4 * (double)var17), (int)Math.round(var6 * (double)var15), (int)Math.round(var8 * (double)var16), (int)Math.round(var10 * (double)var17));
               return new CubeVoxelShape(var18);
            }
         } else {
            return new ArrayVoxelShape(BLOCK.shape, DoubleArrayList.wrap(new double[]{var0, var6}), DoubleArrayList.wrap(new double[]{var2, var8}), DoubleArrayList.wrap(new double[]{var4, var10}));
         }
      } else {
         return empty();
      }
   }

   public static VoxelShape create(AABB var0) {
      return create(var0.minX, var0.minY, var0.minZ, var0.maxX, var0.maxY, var0.maxZ);
   }

   @VisibleForTesting
   protected static int findBits(double var0, double var2) {
      if (!(var0 < -1.0E-7D) && !(var2 > 1.0000001D)) {
         for(int var4 = 0; var4 <= 3; ++var4) {
            int var5 = 1 << var4;
            double var6 = var0 * (double)var5;
            double var8 = var2 * (double)var5;
            boolean var10 = Math.abs(var6 - (double)Math.round(var6)) < 1.0E-7D * (double)var5;
            boolean var11 = Math.abs(var8 - (double)Math.round(var8)) < 1.0E-7D * (double)var5;
            if (var10 && var11) {
               return var4;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   protected static long lcm(int var0, int var1) {
      return (long)var0 * (long)(var1 / IntMath.gcd(var0, var1));
   }

   public static VoxelShape or(VoxelShape var0, VoxelShape var1) {
      return join(var0, var1, BooleanOp.OR);
   }

   public static VoxelShape or(VoxelShape var0, VoxelShape... var1) {
      return (VoxelShape)Arrays.stream(var1).reduce(var0, Shapes::or);
   }

   public static VoxelShape join(VoxelShape var0, VoxelShape var1, BooleanOp var2) {
      return joinUnoptimized(var0, var1, var2).optimize();
   }

   public static VoxelShape joinUnoptimized(VoxelShape var0, VoxelShape var1, BooleanOp var2) {
      if (var2.apply(false, false)) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
      } else if (var0 == var1) {
         return var2.apply(true, true) ? var0 : empty();
      } else {
         boolean var3 = var2.apply(true, false);
         boolean var4 = var2.apply(false, true);
         if (var0.isEmpty()) {
            return var4 ? var1 : empty();
         } else if (var1.isEmpty()) {
            return var3 ? var0 : empty();
         } else {
            IndexMerger var5 = createIndexMerger(1, var0.getCoords(Direction.Axis.X), var1.getCoords(Direction.Axis.X), var3, var4);
            IndexMerger var6 = createIndexMerger(var5.size() - 1, var0.getCoords(Direction.Axis.Y), var1.getCoords(Direction.Axis.Y), var3, var4);
            IndexMerger var7 = createIndexMerger((var5.size() - 1) * (var6.size() - 1), var0.getCoords(Direction.Axis.Z), var1.getCoords(Direction.Axis.Z), var3, var4);
            BitSetDiscreteVoxelShape var8 = BitSetDiscreteVoxelShape.join(var0.shape, var1.shape, var5, var6, var7, var2);
            return (VoxelShape)(var5 instanceof DiscreteCubeMerger && var6 instanceof DiscreteCubeMerger && var7 instanceof DiscreteCubeMerger ? new CubeVoxelShape(var8) : new ArrayVoxelShape(var8, var5.getList(), var6.getList(), var7.getList()));
         }
      }
   }

   public static boolean joinIsNotEmpty(VoxelShape var0, VoxelShape var1, BooleanOp var2) {
      if (var2.apply(false, false)) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
      } else {
         boolean var3 = var0.isEmpty();
         boolean var4 = var1.isEmpty();
         if (!var3 && !var4) {
            if (var0 == var1) {
               return var2.apply(true, true);
            } else {
               boolean var5 = var2.apply(true, false);
               boolean var6 = var2.apply(false, true);
               Direction.Axis[] var7 = AxisCycle.AXIS_VALUES;
               int var8 = var7.length;

               for(int var9 = 0; var9 < var8; ++var9) {
                  Direction.Axis var10 = var7[var9];
                  if (var0.max(var10) < var1.min(var10) - 1.0E-7D) {
                     return var5 || var6;
                  }

                  if (var1.max(var10) < var0.min(var10) - 1.0E-7D) {
                     return var5 || var6;
                  }
               }

               IndexMerger var11 = createIndexMerger(1, var0.getCoords(Direction.Axis.X), var1.getCoords(Direction.Axis.X), var5, var6);
               IndexMerger var12 = createIndexMerger(var11.size() - 1, var0.getCoords(Direction.Axis.Y), var1.getCoords(Direction.Axis.Y), var5, var6);
               IndexMerger var13 = createIndexMerger((var11.size() - 1) * (var12.size() - 1), var0.getCoords(Direction.Axis.Z), var1.getCoords(Direction.Axis.Z), var5, var6);
               return joinIsNotEmpty(var11, var12, var13, var0.shape, var1.shape, var2);
            }
         } else {
            return var2.apply(!var3, !var4);
         }
      }
   }

   private static boolean joinIsNotEmpty(IndexMerger var0, IndexMerger var1, IndexMerger var2, DiscreteVoxelShape var3, DiscreteVoxelShape var4, BooleanOp var5) {
      return !var0.forMergedIndexes((var5x, var6, var7) -> {
         return var1.forMergedIndexes((var6x, var7x, var8) -> {
            return var2.forMergedIndexes((var7, var8x, var9) -> {
               return !var5.apply(var3.isFullWide(var5x, var6x, var7), var4.isFullWide(var6, var7x, var8x));
            });
         });
      });
   }

   public static double collide(Direction.Axis var0, AABB var1, Stream<VoxelShape> var2, double var3) {
      for(Iterator var5 = var2.iterator(); var5.hasNext(); var3 = ((VoxelShape)var5.next()).collide(var0, var1, var3)) {
         if (Math.abs(var3) < 1.0E-7D) {
            return 0.0D;
         }
      }

      return var3;
   }

   public static double collide(Direction.Axis var0, AABB var1, LevelReader var2, double var3, CollisionContext var5, Stream<VoxelShape> var6) {
      return collide(var1, var2, var3, var5, AxisCycle.between(var0, Direction.Axis.Z), var6);
   }

   private static double collide(AABB var0, LevelReader var1, double var2, CollisionContext var4, AxisCycle var5, Stream<VoxelShape> var6) {
      if (!(var0.getXsize() < 1.0E-6D) && !(var0.getYsize() < 1.0E-6D) && !(var0.getZsize() < 1.0E-6D)) {
         if (Math.abs(var2) < 1.0E-7D) {
            return 0.0D;
         } else {
            AxisCycle var7 = var5.inverse();
            Direction.Axis var8 = var7.cycle(Direction.Axis.X);
            Direction.Axis var9 = var7.cycle(Direction.Axis.Y);
            Direction.Axis var10 = var7.cycle(Direction.Axis.Z);
            BlockPos.MutableBlockPos var11 = new BlockPos.MutableBlockPos();
            int var12 = Mth.floor(var0.min(var8) - 1.0E-7D) - 1;
            int var13 = Mth.floor(var0.max(var8) + 1.0E-7D) + 1;
            int var14 = Mth.floor(var0.min(var9) - 1.0E-7D) - 1;
            int var15 = Mth.floor(var0.max(var9) + 1.0E-7D) + 1;
            double var16 = var0.min(var10) - 1.0E-7D;
            double var18 = var0.max(var10) + 1.0E-7D;
            boolean var20 = var2 > 0.0D;
            int var21 = var20 ? Mth.floor(var0.max(var10) - 1.0E-7D) - 1 : Mth.floor(var0.min(var10) + 1.0E-7D) + 1;
            int var22 = lastC(var2, var16, var18);
            int var23 = var20 ? 1 : -1;
            int var24 = var21;

            while(true) {
               if (var20) {
                  if (var24 > var22) {
                     break;
                  }
               } else if (var24 < var22) {
                  break;
               }

               for(int var25 = var12; var25 <= var13; ++var25) {
                  for(int var26 = var14; var26 <= var15; ++var26) {
                     int var27 = 0;
                     if (var25 == var12 || var25 == var13) {
                        ++var27;
                     }

                     if (var26 == var14 || var26 == var15) {
                        ++var27;
                     }

                     if (var24 == var21 || var24 == var22) {
                        ++var27;
                     }

                     if (var27 < 3) {
                        var11.set(var7, var25, var26, var24);
                        BlockState var28 = var1.getBlockState(var11);
                        if ((var27 != 1 || var28.hasLargeCollisionShape()) && (var27 != 2 || var28.is(Blocks.MOVING_PISTON))) {
                           var2 = var28.getCollisionShape(var1, var11, var4).collide(var10, var0.move((double)(-var11.getX()), (double)(-var11.getY()), (double)(-var11.getZ())), var2);
                           if (Math.abs(var2) < 1.0E-7D) {
                              return 0.0D;
                           }

                           var22 = lastC(var2, var16, var18);
                        }
                     }
                  }
               }

               var24 += var23;
            }

            double[] var29 = new double[]{var2};
            var6.forEach((var3) -> {
               var29[0] = var3.collide(var10, var0, var29[0]);
            });
            return var29[0];
         }
      } else {
         return var2;
      }
   }

   private static int lastC(double var0, double var2, double var4) {
      return var0 > 0.0D ? Mth.floor(var4 + var0) + 1 : Mth.floor(var2 + var0) - 1;
   }

   public static boolean blockOccudes(VoxelShape var0, VoxelShape var1, Direction var2) {
      if (var0 == block() && var1 == block()) {
         return true;
      } else if (var1.isEmpty()) {
         return false;
      } else {
         Direction.Axis var3 = var2.getAxis();
         Direction.AxisDirection var4 = var2.getAxisDirection();
         VoxelShape var5 = var4 == Direction.AxisDirection.POSITIVE ? var0 : var1;
         VoxelShape var6 = var4 == Direction.AxisDirection.POSITIVE ? var1 : var0;
         BooleanOp var7 = var4 == Direction.AxisDirection.POSITIVE ? BooleanOp.ONLY_FIRST : BooleanOp.ONLY_SECOND;
         return DoubleMath.fuzzyEquals(var5.max(var3), 1.0D, 1.0E-7D) && DoubleMath.fuzzyEquals(var6.min(var3), 0.0D, 1.0E-7D) && !joinIsNotEmpty(new SliceShape(var5, var3, var5.shape.getSize(var3) - 1), new SliceShape(var6, var3, 0), var7);
      }
   }

   public static VoxelShape getFaceShape(VoxelShape var0, Direction var1) {
      if (var0 == block()) {
         return block();
      } else {
         Direction.Axis var4 = var1.getAxis();
         boolean var2;
         int var3;
         if (var1.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            var2 = DoubleMath.fuzzyEquals(var0.max(var4), 1.0D, 1.0E-7D);
            var3 = var0.shape.getSize(var4) - 1;
         } else {
            var2 = DoubleMath.fuzzyEquals(var0.min(var4), 0.0D, 1.0E-7D);
            var3 = 0;
         }

         return (VoxelShape)(!var2 ? empty() : new SliceShape(var0, var4, var3));
      }
   }

   public static boolean mergedFaceOccludes(VoxelShape var0, VoxelShape var1, Direction var2) {
      if (var0 != block() && var1 != block()) {
         Direction.Axis var3 = var2.getAxis();
         Direction.AxisDirection var4 = var2.getAxisDirection();
         VoxelShape var5 = var4 == Direction.AxisDirection.POSITIVE ? var0 : var1;
         VoxelShape var6 = var4 == Direction.AxisDirection.POSITIVE ? var1 : var0;
         if (!DoubleMath.fuzzyEquals(var5.max(var3), 1.0D, 1.0E-7D)) {
            var5 = empty();
         }

         if (!DoubleMath.fuzzyEquals(var6.min(var3), 0.0D, 1.0E-7D)) {
            var6 = empty();
         }

         return !joinIsNotEmpty(block(), joinUnoptimized(new SliceShape(var5, var3, var5.shape.getSize(var3) - 1), new SliceShape(var6, var3, 0), BooleanOp.OR), BooleanOp.ONLY_FIRST);
      } else {
         return true;
      }
   }

   public static boolean faceShapeOccludes(VoxelShape var0, VoxelShape var1) {
      if (var0 != block() && var1 != block()) {
         if (var0.isEmpty() && var1.isEmpty()) {
            return false;
         } else {
            return !joinIsNotEmpty(block(), joinUnoptimized(var0, var1, BooleanOp.OR), BooleanOp.ONLY_FIRST);
         }
      } else {
         return true;
      }
   }

   @VisibleForTesting
   protected static IndexMerger createIndexMerger(int var0, DoubleList var1, DoubleList var2, boolean var3, boolean var4) {
      int var5 = var1.size() - 1;
      int var6 = var2.size() - 1;
      if (var1 instanceof CubePointRange && var2 instanceof CubePointRange) {
         long var7 = lcm(var5, var6);
         if ((long)var0 * var7 <= 256L) {
            return new DiscreteCubeMerger(var5, var6);
         }
      }

      if (var1.getDouble(var5) < var2.getDouble(0) - 1.0E-7D) {
         return new NonOverlappingMerger(var1, var2, false);
      } else if (var2.getDouble(var6) < var1.getDouble(0) - 1.0E-7D) {
         return new NonOverlappingMerger(var2, var1, true);
      } else {
         return (IndexMerger)(var5 == var6 && Objects.equals(var1, var2) ? new IdenticalMerger(var1) : new IndirectMerger(var1, var2, var3, var4));
      }
   }

   public interface DoubleLineConsumer {
      void consume(double var1, double var3, double var5, double var7, double var9, double var11);
   }
}
