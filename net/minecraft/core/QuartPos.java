package net.minecraft.core;

public final class QuartPos {
   public static final int BITS = 2;
   public static final int SIZE = 4;
   private static final int SECTION_TO_QUARTS_BITS = 2;

   private QuartPos() {
   }

   public static int fromBlock(int var0) {
      return var0 >> 2;
   }

   public static int toBlock(int var0) {
      return var0 << 2;
   }

   public static int fromSection(int var0) {
      return var0 << 2;
   }

   public static int toSection(int var0) {
      return var0 >> 2;
   }
}
