package net.minecraft.world.level.levelgen.structure;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public abstract class NoiseAffectingStructureStart<C extends FeatureConfiguration> extends StructureStart<C> {
   public NoiseAffectingStructureStart(StructureFeature<C> var1, ChunkPos var2, int var3, long var4) {
      super(var1, var2, var3, var4);
   }

   protected BoundingBox createBoundingBox() {
      return super.createBoundingBox().inflate(12);
   }
}
