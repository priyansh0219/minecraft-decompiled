package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V2684 extends NamespacedSchema {
   public V2684(int var1, Schema var2) {
      super(var1, var2);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema var1) {
      Map var2 = super.registerBlockEntities(var1);
      var1.registerSimple(var2, "minecraft:sculk_sensor");
      return var2;
   }
}
