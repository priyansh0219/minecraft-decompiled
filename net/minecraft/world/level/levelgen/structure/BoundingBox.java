package net.minecraft.world.level.levelgen.structure;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BoundingBox {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final Codec<BoundingBox> CODEC;
   private int minX;
   private int minY;
   private int minZ;
   private int maxX;
   private int maxY;
   private int maxZ;

   public BoundingBox(BlockPos var1) {
      this(var1.getX(), var1.getY(), var1.getZ(), var1.getX(), var1.getY(), var1.getZ());
   }

   public BoundingBox(int var1, int var2, int var3, int var4, int var5, int var6) {
      this.minX = var1;
      this.minY = var2;
      this.minZ = var3;
      this.maxX = var4;
      this.maxY = var5;
      this.maxZ = var6;
      if (var4 < var1 || var5 < var2 || var6 < var3) {
         String var7 = "Invalid bounding box data, inverted bounds for: " + this;
         if (SharedConstants.IS_RUNNING_IN_IDE) {
            throw new IllegalStateException(var7);
         }

         LOGGER.error(var7);
         this.minX = Math.min(var1, var4);
         this.minY = Math.min(var2, var5);
         this.minZ = Math.min(var3, var6);
         this.maxX = Math.max(var1, var4);
         this.maxY = Math.max(var2, var5);
         this.maxZ = Math.max(var3, var6);
      }

   }

   public static BoundingBox fromCorners(Vec3i var0, Vec3i var1) {
      return new BoundingBox(Math.min(var0.getX(), var1.getX()), Math.min(var0.getY(), var1.getY()), Math.min(var0.getZ(), var1.getZ()), Math.max(var0.getX(), var1.getX()), Math.max(var0.getY(), var1.getY()), Math.max(var0.getZ(), var1.getZ()));
   }

   public static BoundingBox infinite() {
      return new BoundingBox(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
   }

   public static BoundingBox orientBox(int var0, int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, Direction var9) {
      switch(var9) {
      case SOUTH:
      default:
         return new BoundingBox(var0 + var3, var1 + var4, var2 + var5, var0 + var6 - 1 + var3, var1 + var7 - 1 + var4, var2 + var8 - 1 + var5);
      case NORTH:
         return new BoundingBox(var0 + var3, var1 + var4, var2 - var8 + 1 + var5, var0 + var6 - 1 + var3, var1 + var7 - 1 + var4, var2 + var5);
      case WEST:
         return new BoundingBox(var0 - var8 + 1 + var5, var1 + var4, var2 + var3, var0 + var5, var1 + var7 - 1 + var4, var2 + var6 - 1 + var3);
      case EAST:
         return new BoundingBox(var0 + var5, var1 + var4, var2 + var3, var0 + var8 - 1 + var5, var1 + var7 - 1 + var4, var2 + var6 - 1 + var3);
      }
   }

   public boolean intersects(BoundingBox var1) {
      return this.maxX >= var1.minX && this.minX <= var1.maxX && this.maxZ >= var1.minZ && this.minZ <= var1.maxZ && this.maxY >= var1.minY && this.minY <= var1.maxY;
   }

   public boolean intersects(int var1, int var2, int var3, int var4) {
      return this.maxX >= var1 && this.minX <= var3 && this.maxZ >= var2 && this.minZ <= var4;
   }

   public static Optional<BoundingBox> encapsulatingPositions(Iterable<BlockPos> var0) {
      Iterator var1 = var0.iterator();
      if (!var1.hasNext()) {
         return Optional.empty();
      } else {
         BoundingBox var2 = new BoundingBox((BlockPos)var1.next());
         Objects.requireNonNull(var2);
         var1.forEachRemaining(var2::encapsulate);
         return Optional.of(var2);
      }
   }

   public static Optional<BoundingBox> encapsulatingBoxes(Iterable<BoundingBox> var0) {
      Iterator var1 = var0.iterator();
      if (!var1.hasNext()) {
         return Optional.empty();
      } else {
         BoundingBox var2 = (BoundingBox)var1.next();
         BoundingBox var3 = new BoundingBox(var2.minX, var2.minY, var2.minZ, var2.maxX, var2.maxY, var2.maxZ);
         Objects.requireNonNull(var3);
         var1.forEachRemaining(var3::encapsulate);
         return Optional.of(var3);
      }
   }

   public BoundingBox encapsulate(BoundingBox var1) {
      this.minX = Math.min(this.minX, var1.minX);
      this.minY = Math.min(this.minY, var1.minY);
      this.minZ = Math.min(this.minZ, var1.minZ);
      this.maxX = Math.max(this.maxX, var1.maxX);
      this.maxY = Math.max(this.maxY, var1.maxY);
      this.maxZ = Math.max(this.maxZ, var1.maxZ);
      return this;
   }

   public BoundingBox encapsulate(BlockPos var1) {
      this.minX = Math.min(this.minX, var1.getX());
      this.minY = Math.min(this.minY, var1.getY());
      this.minZ = Math.min(this.minZ, var1.getZ());
      this.maxX = Math.max(this.maxX, var1.getX());
      this.maxY = Math.max(this.maxY, var1.getY());
      this.maxZ = Math.max(this.maxZ, var1.getZ());
      return this;
   }

   public BoundingBox inflate(int var1) {
      this.minX -= var1;
      this.minY -= var1;
      this.minZ -= var1;
      this.maxX += var1;
      this.maxY += var1;
      this.maxZ += var1;
      return this;
   }

   public BoundingBox move(int var1, int var2, int var3) {
      this.minX += var1;
      this.minY += var2;
      this.minZ += var3;
      this.maxX += var1;
      this.maxY += var2;
      this.maxZ += var3;
      return this;
   }

   public BoundingBox move(Vec3i var1) {
      return this.move(var1.getX(), var1.getY(), var1.getZ());
   }

   public BoundingBox moved(int var1, int var2, int var3) {
      return new BoundingBox(this.minX + var1, this.minY + var2, this.minZ + var3, this.maxX + var1, this.maxY + var2, this.maxZ + var3);
   }

   public boolean isInside(Vec3i var1) {
      return var1.getX() >= this.minX && var1.getX() <= this.maxX && var1.getZ() >= this.minZ && var1.getZ() <= this.maxZ && var1.getY() >= this.minY && var1.getY() <= this.maxY;
   }

   public Vec3i getLength() {
      return new Vec3i(this.maxX - this.minX, this.maxY - this.minY, this.maxZ - this.minZ);
   }

   public int getXSpan() {
      return this.maxX - this.minX + 1;
   }

   public int getYSpan() {
      return this.maxY - this.minY + 1;
   }

   public int getZSpan() {
      return this.maxZ - this.minZ + 1;
   }

   public BlockPos getCenter() {
      return new BlockPos(this.minX + (this.maxX - this.minX + 1) / 2, this.minY + (this.maxY - this.minY + 1) / 2, this.minZ + (this.maxZ - this.minZ + 1) / 2);
   }

   public void forAllCorners(Consumer<BlockPos> var1) {
      BlockPos.MutableBlockPos var2 = new BlockPos.MutableBlockPos();
      var1.accept(var2.set(this.maxX, this.maxY, this.maxZ));
      var1.accept(var2.set(this.minX, this.maxY, this.maxZ));
      var1.accept(var2.set(this.maxX, this.minY, this.maxZ));
      var1.accept(var2.set(this.minX, this.minY, this.maxZ));
      var1.accept(var2.set(this.maxX, this.maxY, this.minZ));
      var1.accept(var2.set(this.minX, this.maxY, this.minZ));
      var1.accept(var2.set(this.maxX, this.minY, this.minZ));
      var1.accept(var2.set(this.minX, this.minY, this.minZ));
   }

   public String toString() {
      return MoreObjects.toStringHelper(this).add("minX", this.minX).add("minY", this.minY).add("minZ", this.minZ).add("maxX", this.maxX).add("maxY", this.maxY).add("maxZ", this.maxZ).toString();
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (!(var1 instanceof BoundingBox)) {
         return false;
      } else {
         BoundingBox var2 = (BoundingBox)var1;
         return this.minX == var2.minX && this.minY == var2.minY && this.minZ == var2.minZ && this.maxX == var2.maxX && this.maxY == var2.maxY && this.maxZ == var2.maxZ;
      }
   }

   public int hashCode() {
      return Objects.hash(new Object[]{this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ});
   }

   public int minX() {
      return this.minX;
   }

   public int minY() {
      return this.minY;
   }

   public int minZ() {
      return this.minZ;
   }

   public int maxX() {
      return this.maxX;
   }

   public int maxY() {
      return this.maxY;
   }

   public int maxZ() {
      return this.maxZ;
   }

   static {
      CODEC = Codec.INT_STREAM.comapFlatMap((var0) -> {
         return Util.fixedSize((IntStream)var0, 6).map((var0x) -> {
            return new BoundingBox(var0x[0], var0x[1], var0x[2], var0x[3], var0x[4], var0x[5]);
         });
      }, (var0) -> {
         return IntStream.of(new int[]{var0.minX, var0.minY, var0.minZ, var0.maxX, var0.maxY, var0.maxZ});
      }).stable();
   }
}
