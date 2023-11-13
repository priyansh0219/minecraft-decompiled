package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

public class CarverConfiguration extends ProbabilityFeatureConfiguration {
   public static final MapCodec<CarverConfiguration> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter((var0x) -> {
         return var0x.probability;
      }), HeightProvider.CODEC.fieldOf("y").forGetter((var0x) -> {
         return var0x.y;
      }), FloatProvider.CODEC.fieldOf("yScale").forGetter((var0x) -> {
         return var0x.yScale;
      }), VerticalAnchor.CODEC.fieldOf("lava_level").forGetter((var0x) -> {
         return var0x.lavaLevel;
      }), Codec.BOOL.fieldOf("aquifers_enabled").forGetter((var0x) -> {
         return var0x.aquifersEnabled;
      }), CarverDebugSettings.CODEC.optionalFieldOf("debug_settings", CarverDebugSettings.DEFAULT).forGetter((var0x) -> {
         return var0x.debugSettings;
      })).apply(var0, CarverConfiguration::new);
   });
   public final HeightProvider y;
   public final FloatProvider yScale;
   public final VerticalAnchor lavaLevel;
   public final boolean aquifersEnabled;
   public final CarverDebugSettings debugSettings;

   public CarverConfiguration(float var1, HeightProvider var2, FloatProvider var3, VerticalAnchor var4, boolean var5, CarverDebugSettings var6) {
      super(var1);
      this.y = var2;
      this.yScale = var3;
      this.lavaLevel = var4;
      this.aquifersEnabled = var5;
      this.debugSettings = var6;
   }
}
