package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratorConfiguration;

public class CarvingMaskDecoratorConfiguration implements DecoratorConfiguration {
   public static final Codec<CarvingMaskDecoratorConfiguration> CODEC;
   protected final GenerationStep.Carving step;

   public CarvingMaskDecoratorConfiguration(GenerationStep.Carving var1) {
      this.step = var1;
   }

   static {
      CODEC = GenerationStep.Carving.CODEC.fieldOf("step").xmap(CarvingMaskDecoratorConfiguration::new, (var0) -> {
         return var0.step;
      }).codec();
   }
}
