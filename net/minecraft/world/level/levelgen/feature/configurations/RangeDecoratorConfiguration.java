package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

public class RangeDecoratorConfiguration implements DecoratorConfiguration, FeatureConfiguration {
   public static final Codec<RangeDecoratorConfiguration> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(HeightProvider.CODEC.fieldOf("height").forGetter((var0x) -> {
         return var0x.height;
      })).apply(var0, RangeDecoratorConfiguration::new);
   });
   public final HeightProvider height;

   public RangeDecoratorConfiguration(HeightProvider var1) {
      this.height = var1;
   }
}
