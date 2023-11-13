package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.ShipwreckConfiguration;
import net.minecraft.world.level.levelgen.structure.ShipwreckPieces;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class ShipwreckFeature extends StructureFeature<ShipwreckConfiguration> {
   public ShipwreckFeature(Codec<ShipwreckConfiguration> var1) {
      super(var1);
   }

   public StructureFeature.StructureStartFactory<ShipwreckConfiguration> getStartFactory() {
      return ShipwreckFeature.FeatureStart::new;
   }

   public static class FeatureStart extends StructureStart<ShipwreckConfiguration> {
      public FeatureStart(StructureFeature<ShipwreckConfiguration> var1, ChunkPos var2, int var3, long var4) {
         super(var1, var2, var3, var4);
      }

      public void generatePieces(RegistryAccess var1, ChunkGenerator var2, StructureManager var3, ChunkPos var4, Biome var5, ShipwreckConfiguration var6, LevelHeightAccessor var7) {
         Rotation var8 = Rotation.getRandom(this.random);
         BlockPos var9 = new BlockPos(var4.getMinBlockX(), 90, var4.getMinBlockZ());
         ShipwreckPieces.addPieces(var3, var9, var8, this, this.random, var6);
      }
   }
}
