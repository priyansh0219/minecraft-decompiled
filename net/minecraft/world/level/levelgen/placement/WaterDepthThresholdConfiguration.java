package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratorConfiguration;

public class WaterDepthThresholdConfiguration implements DecoratorConfiguration {
   public static final Codec<WaterDepthThresholdConfiguration> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(Codec.INT.fieldOf("max_water_depth").forGetter((var0x) -> {
         return var0x.maxWaterDepth;
      })).apply(var0, WaterDepthThresholdConfiguration::new);
   });
   public final int maxWaterDepth;

   public WaterDepthThresholdConfiguration(int var1) {
      this.maxWaterDepth = var1;
   }
}
