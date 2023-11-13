package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.world.level.dimension.DimensionType;

public class NoiseSettings {
   public static final Codec<NoiseSettings> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("min_y").forGetter(NoiseSettings::minY), Codec.intRange(0, DimensionType.Y_SIZE).fieldOf("height").forGetter(NoiseSettings::height), NoiseSamplingSettings.CODEC.fieldOf("sampling").forGetter(NoiseSettings::noiseSamplingSettings), NoiseSlideSettings.CODEC.fieldOf("top_slide").forGetter(NoiseSettings::topSlideSettings), NoiseSlideSettings.CODEC.fieldOf("bottom_slide").forGetter(NoiseSettings::bottomSlideSettings), Codec.intRange(1, 4).fieldOf("size_horizontal").forGetter(NoiseSettings::noiseSizeHorizontal), Codec.intRange(1, 4).fieldOf("size_vertical").forGetter(NoiseSettings::noiseSizeVertical), Codec.DOUBLE.fieldOf("density_factor").forGetter(NoiseSettings::densityFactor), Codec.DOUBLE.fieldOf("density_offset").forGetter(NoiseSettings::densityOffset), Codec.BOOL.fieldOf("simplex_surface_noise").forGetter(NoiseSettings::useSimplexSurfaceNoise), Codec.BOOL.optionalFieldOf("random_density_offset", false, Lifecycle.experimental()).forGetter(NoiseSettings::randomDensityOffset), Codec.BOOL.optionalFieldOf("island_noise_override", false, Lifecycle.experimental()).forGetter(NoiseSettings::islandNoiseOverride), Codec.BOOL.optionalFieldOf("amplified", false, Lifecycle.experimental()).forGetter(NoiseSettings::isAmplified)).apply(var0, NoiseSettings::new);
   }).comapFlatMap(NoiseSettings::guardY, Function.identity());
   private final int minY;
   private final int height;
   private final NoiseSamplingSettings noiseSamplingSettings;
   private final NoiseSlideSettings topSlideSettings;
   private final NoiseSlideSettings bottomSlideSettings;
   private final int noiseSizeHorizontal;
   private final int noiseSizeVertical;
   private final double densityFactor;
   private final double densityOffset;
   private final boolean useSimplexSurfaceNoise;
   private final boolean randomDensityOffset;
   private final boolean islandNoiseOverride;
   private final boolean isAmplified;

   private static DataResult<NoiseSettings> guardY(NoiseSettings var0) {
      if (var0.minY() + var0.height() > DimensionType.MAX_Y + 1) {
         return DataResult.error("min_y + height cannot be higher than: " + (DimensionType.MAX_Y + 1));
      } else if (var0.height() % 16 != 0) {
         return DataResult.error("height has to be a multiple of 16");
      } else {
         return var0.minY() % 16 != 0 ? DataResult.error("min_y has to be a multiple of 16") : DataResult.success(var0);
      }
   }

   private NoiseSettings(int var1, int var2, NoiseSamplingSettings var3, NoiseSlideSettings var4, NoiseSlideSettings var5, int var6, int var7, double var8, double var10, boolean var12, boolean var13, boolean var14, boolean var15) {
      this.minY = var1;
      this.height = var2;
      this.noiseSamplingSettings = var3;
      this.topSlideSettings = var4;
      this.bottomSlideSettings = var5;
      this.noiseSizeHorizontal = var6;
      this.noiseSizeVertical = var7;
      this.densityFactor = var8;
      this.densityOffset = var10;
      this.useSimplexSurfaceNoise = var12;
      this.randomDensityOffset = var13;
      this.islandNoiseOverride = var14;
      this.isAmplified = var15;
   }

   public static NoiseSettings create(int var0, int var1, NoiseSamplingSettings var2, NoiseSlideSettings var3, NoiseSlideSettings var4, int var5, int var6, double var7, double var9, boolean var11, boolean var12, boolean var13, boolean var14) {
      NoiseSettings var15 = new NoiseSettings(var0, var1, var2, var3, var4, var5, var6, var7, var9, var11, var12, var13, var14);
      guardY(var15).error().ifPresent((var0x) -> {
         throw new IllegalStateException(var0x.message());
      });
      return var15;
   }

   public int minY() {
      return this.minY;
   }

   public int height() {
      return this.height;
   }

   public NoiseSamplingSettings noiseSamplingSettings() {
      return this.noiseSamplingSettings;
   }

   public NoiseSlideSettings topSlideSettings() {
      return this.topSlideSettings;
   }

   public NoiseSlideSettings bottomSlideSettings() {
      return this.bottomSlideSettings;
   }

   public int noiseSizeHorizontal() {
      return this.noiseSizeHorizontal;
   }

   public int noiseSizeVertical() {
      return this.noiseSizeVertical;
   }

   public double densityFactor() {
      return this.densityFactor;
   }

   public double densityOffset() {
      return this.densityOffset;
   }

   @Deprecated
   public boolean useSimplexSurfaceNoise() {
      return this.useSimplexSurfaceNoise;
   }

   @Deprecated
   public boolean randomDensityOffset() {
      return this.randomDensityOffset;
   }

   @Deprecated
   public boolean islandNoiseOverride() {
      return this.islandNoiseOverride;
   }

   @Deprecated
   public boolean isAmplified() {
      return this.isAmplified;
   }
}
