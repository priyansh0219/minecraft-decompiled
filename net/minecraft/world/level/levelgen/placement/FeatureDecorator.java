package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.feature.configurations.CountConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratorConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.HeightmapConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoiseDependantDecoratorConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneDecoratorConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RangeDecoratorConfiguration;
import net.minecraft.world.level.levelgen.placement.nether.CountMultiLayerDecorator;

public abstract class FeatureDecorator<DC extends DecoratorConfiguration> {
   public static final FeatureDecorator<NoneDecoratorConfiguration> NOPE;
   public static final FeatureDecorator<DecoratedDecoratorConfiguration> DECORATED;
   public static final FeatureDecorator<CarvingMaskDecoratorConfiguration> CARVING_MASK;
   public static final FeatureDecorator<CountConfiguration> COUNT_MULTILAYER;
   public static final FeatureDecorator<NoneDecoratorConfiguration> SQUARE;
   public static final FeatureDecorator<NoneDecoratorConfiguration> DARK_OAK_TREE;
   public static final FeatureDecorator<NoneDecoratorConfiguration> ICEBERG;
   public static final FeatureDecorator<ChanceDecoratorConfiguration> CHANCE;
   public static final FeatureDecorator<CountConfiguration> COUNT;
   public static final FeatureDecorator<NoiseDependantDecoratorConfiguration> COUNT_NOISE;
   public static final FeatureDecorator<NoiseCountFactorDecoratorConfiguration> COUNT_NOISE_BIASED;
   public static final FeatureDecorator<FrequencyWithExtraChanceDecoratorConfiguration> COUNT_EXTRA;
   public static final FeatureDecorator<ChanceDecoratorConfiguration> LAVA_LAKE;
   public static final FeatureDecorator<HeightmapConfiguration> HEIGHTMAP;
   public static final FeatureDecorator<HeightmapConfiguration> HEIGHTMAP_SPREAD_DOUBLE;
   public static final FeatureDecorator<WaterDepthThresholdConfiguration> WATER_DEPTH_THRESHOLD;
   public static final FeatureDecorator<CaveDecoratorConfiguration> CAVE_SURFACE;
   public static final FeatureDecorator<RangeDecoratorConfiguration> RANGE;
   public static final FeatureDecorator<NoneDecoratorConfiguration> SPREAD_32_ABOVE;
   public static final FeatureDecorator<NoneDecoratorConfiguration> END_GATEWAY;
   private final Codec<ConfiguredDecorator<DC>> configuredCodec;

   private static <T extends DecoratorConfiguration, G extends FeatureDecorator<T>> G register(String var0, G var1) {
      return (FeatureDecorator)Registry.register(Registry.DECORATOR, (String)var0, var1);
   }

   public FeatureDecorator(Codec<DC> var1) {
      this.configuredCodec = var1.fieldOf("config").xmap((var1x) -> {
         return new ConfiguredDecorator(this, var1x);
      }, ConfiguredDecorator::config).codec();
   }

   public ConfiguredDecorator<DC> configured(DC var1) {
      return new ConfiguredDecorator(this, var1);
   }

   public Codec<ConfiguredDecorator<DC>> configuredCodec() {
      return this.configuredCodec;
   }

   public abstract Stream<BlockPos> getPositions(DecorationContext var1, Random var2, DC var3, BlockPos var4);

   public String toString() {
      String var10000 = this.getClass().getSimpleName();
      return var10000 + "@" + Integer.toHexString(this.hashCode());
   }

   static {
      NOPE = register("nope", new NopePlacementDecorator(NoneDecoratorConfiguration.CODEC));
      DECORATED = register("decorated", new DecoratedDecorator(DecoratedDecoratorConfiguration.CODEC));
      CARVING_MASK = register("carving_mask", new CarvingMaskDecorator(CarvingMaskDecoratorConfiguration.CODEC));
      COUNT_MULTILAYER = register("count_multilayer", new CountMultiLayerDecorator(CountConfiguration.CODEC));
      SQUARE = register("square", new SquareDecorator(NoneDecoratorConfiguration.CODEC));
      DARK_OAK_TREE = register("dark_oak_tree", new DarkOakTreePlacementDecorator(NoneDecoratorConfiguration.CODEC));
      ICEBERG = register("iceberg", new IcebergPlacementDecorator(NoneDecoratorConfiguration.CODEC));
      CHANCE = register("chance", new ChanceDecorator(ChanceDecoratorConfiguration.CODEC));
      COUNT = register("count", new CountDecorator(CountConfiguration.CODEC));
      COUNT_NOISE = register("count_noise", new CountNoiseDecorator(NoiseDependantDecoratorConfiguration.CODEC));
      COUNT_NOISE_BIASED = register("count_noise_biased", new NoiseBasedDecorator(NoiseCountFactorDecoratorConfiguration.CODEC));
      COUNT_EXTRA = register("count_extra", new CountWithExtraChanceDecorator(FrequencyWithExtraChanceDecoratorConfiguration.CODEC));
      LAVA_LAKE = register("lava_lake", new LakeLavaPlacementDecorator(ChanceDecoratorConfiguration.CODEC));
      HEIGHTMAP = register("heightmap", new HeightmapDecorator(HeightmapConfiguration.CODEC));
      HEIGHTMAP_SPREAD_DOUBLE = register("heightmap_spread_double", new HeightmapDoubleDecorator(HeightmapConfiguration.CODEC));
      WATER_DEPTH_THRESHOLD = register("water_depth_threshold", new WaterDepthThresholdDecorator(WaterDepthThresholdConfiguration.CODEC));
      CAVE_SURFACE = register("cave_surface", new CaveSurfaceDecorator(CaveDecoratorConfiguration.CODEC));
      RANGE = register("range", new RangeDecorator(RangeDecoratorConfiguration.CODEC));
      SPREAD_32_ABOVE = register("spread_32_above", new Spread32Decorator(NoneDecoratorConfiguration.CODEC));
      END_GATEWAY = register("end_gateway", new EndGatewayPlacementDecorator(NoneDecoratorConfiguration.CODEC));
   }
}
