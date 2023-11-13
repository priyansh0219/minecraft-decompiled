package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

public enum DoubleBlockHalf implements StringRepresentable {
   UPPER,
   LOWER;

   public String toString() {
      return this.getSerializedName();
   }

   public String getSerializedName() {
      return this == UPPER ? "upper" : "lower";
   }

   // $FF: synthetic method
   private static DoubleBlockHalf[] $values() {
      return new DoubleBlockHalf[]{UPPER, LOWER};
   }
}
