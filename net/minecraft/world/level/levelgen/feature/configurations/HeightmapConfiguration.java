package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.Heightmap;

public class HeightmapConfiguration implements DecoratorConfiguration {
   public static final Codec<HeightmapConfiguration> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(Heightmap.Types.CODEC.fieldOf("heightmap").forGetter((var0x) -> {
         return var0x.heightmap;
      })).apply(var0, HeightmapConfiguration::new);
   });
   public final Heightmap.Types heightmap;

   public HeightmapConfiguration(Heightmap.Types var1) {
      this.heightmap = var1;
   }
}
