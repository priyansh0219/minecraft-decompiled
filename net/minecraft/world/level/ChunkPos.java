package net.minecraft.world.level;

import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public class ChunkPos {
   public static final long INVALID_CHUNK_POS = asLong(1875016, 1875016);
   private static final long COORD_BITS = 32L;
   private static final long COORD_MASK = 4294967295L;
   private static final int REGION_BITS = 5;
   private static final int REGION_MASK = 31;
   public final int x;
   public final int z;
   private static final int HASH_A = 1664525;
   private static final int HASH_C = 1013904223;
   private static final int HASH_Z_XOR = -559038737;

   public ChunkPos(int var1, int var2) {
      this.x = var1;
      this.z = var2;
   }

   public ChunkPos(BlockPos var1) {
      this.x = SectionPos.blockToSectionCoord(var1.getX());
      this.z = SectionPos.blockToSectionCoord(var1.getZ());
   }

   public ChunkPos(long var1) {
      this.x = (int)var1;
      this.z = (int)(var1 >> 32);
   }

   public long toLong() {
      return asLong(this.x, this.z);
   }

   public static long asLong(int var0, int var1) {
      return (long)var0 & 4294967295L | ((long)var1 & 4294967295L) << 32;
   }

   public static long asLong(BlockPos var0) {
      return asLong(SectionPos.blockToSectionCoord(var0.getX()), SectionPos.blockToSectionCoord(var0.getZ()));
   }

   public static int getX(long var0) {
      return (int)(var0 & 4294967295L);
   }

   public static int getZ(long var0) {
      return (int)(var0 >>> 32 & 4294967295L);
   }

   public int hashCode() {
      int var1 = 1664525 * this.x + 1013904223;
      int var2 = 1664525 * (this.z ^ -559038737) + 1013904223;
      return var1 ^ var2;
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (!(var1 instanceof ChunkPos)) {
         return false;
      } else {
         ChunkPos var2 = (ChunkPos)var1;
         return this.x == var2.x && this.z == var2.z;
      }
   }

   public int getMiddleBlockX() {
      return this.getBlockX(8);
   }

   public int getMiddleBlockZ() {
      return this.getBlockZ(8);
   }

   public int getMinBlockX() {
      return SectionPos.sectionToBlockCoord(this.x);
   }

   public int getMinBlockZ() {
      return SectionPos.sectionToBlockCoord(this.z);
   }

   public int getMaxBlockX() {
      return this.getBlockX(15);
   }

   public int getMaxBlockZ() {
      return this.getBlockZ(15);
   }

   public int getRegionX() {
      return this.x >> 5;
   }

   public int getRegionZ() {
      return this.z >> 5;
   }

   public int getRegionLocalX() {
      return this.x & 31;
   }

   public int getRegionLocalZ() {
      return this.z & 31;
   }

   public BlockPos getBlockAt(int var1, int var2, int var3) {
      return new BlockPos(this.getBlockX(var1), var2, this.getBlockZ(var3));
   }

   public int getBlockX(int var1) {
      return SectionPos.sectionToBlockCoord(this.x, var1);
   }

   public int getBlockZ(int var1) {
      return SectionPos.sectionToBlockCoord(this.z, var1);
   }

   public BlockPos getMiddleBlockPosition(int var1) {
      return new BlockPos(this.getMiddleBlockX(), var1, this.getMiddleBlockZ());
   }

   public String toString() {
      return "[" + this.x + ", " + this.z + "]";
   }

   public BlockPos getWorldPosition() {
      return new BlockPos(this.getMinBlockX(), 0, this.getMinBlockZ());
   }

   public int getChessboardDistance(ChunkPos var1) {
      return Math.max(Math.abs(this.x - var1.x), Math.abs(this.z - var1.z));
   }

   public static Stream<ChunkPos> rangeClosed(ChunkPos var0, int var1) {
      return rangeClosed(new ChunkPos(var0.x - var1, var0.z - var1), new ChunkPos(var0.x + var1, var0.z + var1));
   }

   public static Stream<ChunkPos> rangeClosed(final ChunkPos var0, final ChunkPos var1) {
      int var2 = Math.abs(var0.x - var1.x) + 1;
      int var3 = Math.abs(var0.z - var1.z) + 1;
      final int var4 = var0.x < var1.x ? 1 : -1;
      final int var5 = var0.z < var1.z ? 1 : -1;
      return StreamSupport.stream(new AbstractSpliterator<ChunkPos>((long)(var2 * var3), 64) {
         @Nullable
         private ChunkPos pos;

         public boolean tryAdvance(Consumer<? super ChunkPos> var1x) {
            if (this.pos == null) {
               this.pos = var0;
            } else {
               int var2 = this.pos.x;
               int var3 = this.pos.z;
               if (var2 == var1.x) {
                  if (var3 == var1.z) {
                     return false;
                  }

                  this.pos = new ChunkPos(var0.x, var3 + var5);
               } else {
                  this.pos = new ChunkPos(var2 + var4, var3);
               }
            }

            var1x.accept(this.pos);
            return true;
         }
      }, false);
   }
}
