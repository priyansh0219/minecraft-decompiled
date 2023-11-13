package net.minecraft.world.level;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface CollisionGetter extends BlockGetter {
   WorldBorder getWorldBorder();

   @Nullable
   BlockGetter getChunkForCollisions(int var1, int var2);

   default boolean isUnobstructed(@Nullable Entity var1, VoxelShape var2) {
      return true;
   }

   default boolean isUnobstructed(BlockState var1, BlockPos var2, CollisionContext var3) {
      VoxelShape var4 = var1.getCollisionShape(this, var2, var3);
      return var4.isEmpty() || this.isUnobstructed((Entity)null, var4.move((double)var2.getX(), (double)var2.getY(), (double)var2.getZ()));
   }

   default boolean isUnobstructed(Entity var1) {
      return this.isUnobstructed(var1, Shapes.create(var1.getBoundingBox()));
   }

   default boolean noCollision(AABB var1) {
      return this.noCollision((Entity)null, var1, (var0) -> {
         return true;
      });
   }

   default boolean noCollision(Entity var1) {
      return this.noCollision(var1, var1.getBoundingBox(), (var0) -> {
         return true;
      });
   }

   default boolean noCollision(Entity var1, AABB var2) {
      return this.noCollision(var1, var2, (var0) -> {
         return true;
      });
   }

   default boolean noCollision(@Nullable Entity var1, AABB var2, Predicate<Entity> var3) {
      return this.getCollisions(var1, var2, var3).allMatch(VoxelShape::isEmpty);
   }

   Stream<VoxelShape> getEntityCollisions(@Nullable Entity var1, AABB var2, Predicate<Entity> var3);

   default Stream<VoxelShape> getCollisions(@Nullable Entity var1, AABB var2, Predicate<Entity> var3) {
      return Stream.concat(this.getBlockCollisions(var1, var2), this.getEntityCollisions(var1, var2, var3));
   }

   default Stream<VoxelShape> getBlockCollisions(@Nullable Entity var1, AABB var2) {
      return StreamSupport.stream(new CollisionSpliterator(this, var1, var2), false);
   }

   default boolean hasBlockCollision(@Nullable Entity var1, AABB var2, BiPredicate<BlockState, BlockPos> var3) {
      return !this.getBlockCollisions(var1, var2, var3).allMatch(VoxelShape::isEmpty);
   }

   default Stream<VoxelShape> getBlockCollisions(@Nullable Entity var1, AABB var2, BiPredicate<BlockState, BlockPos> var3) {
      return StreamSupport.stream(new CollisionSpliterator(this, var1, var2, var3), false);
   }

   default Optional<Vec3> findFreePosition(@Nullable Entity var1, VoxelShape var2, Vec3 var3, double var4, double var6, double var8) {
      if (var2.isEmpty()) {
         return Optional.empty();
      } else {
         AABB var10 = var2.bounds().inflate(var4, var6, var8);
         VoxelShape var11 = (VoxelShape)this.getBlockCollisions(var1, var10).flatMap((var0) -> {
            return var0.toAabbs().stream();
         }).map((var6x) -> {
            return var6x.inflate(var4 / 2.0D, var6 / 2.0D, var8 / 2.0D);
         }).map(Shapes::create).reduce(Shapes.empty(), Shapes::or);
         VoxelShape var12 = Shapes.join(var2, var11, BooleanOp.ONLY_FIRST);
         return var12.closestPointTo(var3);
      }
   }
}
