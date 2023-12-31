package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratorConfiguration;

public class CaveDecoratorConfiguration implements DecoratorConfiguration {
   public static final Codec<CaveDecoratorConfiguration> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(CaveSurface.CODEC.fieldOf("surface").forGetter((var0x) -> {
         return var0x.surface;
      }), Codec.INT.fieldOf("floor_to_ceiling_search_range").forGetter((var0x) -> {
         return var0x.floorToCeilingSearchRange;
      })).apply(var0, CaveDecoratorConfiguration::new);
   });
   public final CaveSurface surface;
   public final int floorToCeilingSearchRange;

   public CaveDecoratorConfiguration(CaveSurface var1, int var2) {
      this.surface = var1;
      this.floorToCeilingSearchRange = var2;
   }
}
