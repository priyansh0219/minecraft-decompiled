package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

public enum CaveSurface implements StringRepresentable {
   CEILING(Direction.UP, 1, "ceiling"),
   FLOOR(Direction.DOWN, -1, "floor");

   public static final Codec<CaveSurface> CODEC = StringRepresentable.fromEnum(CaveSurface::values, CaveSurface::byName);
   private final Direction direction;
   private final int y;
   private final String id;
   private static final CaveSurface[] VALUES = values();

   private CaveSurface(Direction var3, int var4, String var5) {
      this.direction = var3;
      this.y = var4;
      this.id = var5;
   }

   public Direction getDirection() {
      return this.direction;
   }

   public int getY() {
      return this.y;
   }

   public static CaveSurface byName(String var0) {
      CaveSurface[] var1 = VALUES;
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         CaveSurface var4 = var1[var3];
         if (var4.getSerializedName().equals(var0)) {
            return var4;
         }
      }

      throw new IllegalArgumentException("Unknown Surface type: " + var0);
   }

   public String getSerializedName() {
      return this.id;
   }

   // $FF: synthetic method
   private static CaveSurface[] $values() {
      return new CaveSurface[]{CEILING, FLOOR};
   }
}
