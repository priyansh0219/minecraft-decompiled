package net.minecraft.world.level.block.state.properties;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.StringRepresentable;

public enum StructureMode implements StringRepresentable {
   SAVE("save"),
   LOAD("load"),
   CORNER("corner"),
   DATA("data");

   private final String name;
   private final Component displayName;

   private StructureMode(String var3) {
      this.name = var3;
      this.displayName = new TranslatableComponent("structure_block.mode_info." + var3);
   }

   public String getSerializedName() {
      return this.name;
   }

   public Component getDisplayName() {
      return this.displayName;
   }

   // $FF: synthetic method
   private static StructureMode[] $values() {
      return new StructureMode[]{SAVE, LOAD, CORNER, DATA};
   }
}
