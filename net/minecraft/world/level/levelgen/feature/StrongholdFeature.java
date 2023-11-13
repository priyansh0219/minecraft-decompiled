package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.NoiseAffectingStructureStart;
import net.minecraft.world.level.levelgen.structure.StrongholdPieces;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class StrongholdFeature extends StructureFeature<NoneFeatureConfiguration> {
   public StrongholdFeature(Codec<NoneFeatureConfiguration> var1) {
      super(var1);
   }

   public StructureFeature.StructureStartFactory<NoneFeatureConfiguration> getStartFactory() {
      return StrongholdFeature.StrongholdStart::new;
   }

   protected boolean isFeatureChunk(ChunkGenerator var1, BiomeSource var2, long var3, WorldgenRandom var5, ChunkPos var6, Biome var7, ChunkPos var8, NoneFeatureConfiguration var9, LevelHeightAccessor var10) {
      return var1.hasStronghold(var6);
   }

   public static class StrongholdStart extends NoiseAffectingStructureStart<NoneFeatureConfiguration> {
      private final long seed;

      public StrongholdStart(StructureFeature<NoneFeatureConfiguration> var1, ChunkPos var2, int var3, long var4) {
         super(var1, var2, var3, var4);
         this.seed = var4;
      }

      public void generatePieces(RegistryAccess var1, ChunkGenerator var2, StructureManager var3, ChunkPos var4, Biome var5, NoneFeatureConfiguration var6, LevelHeightAccessor var7) {
         int var8 = 0;

         StrongholdPieces.StartPiece var9;
         do {
            this.clearPieces();
            this.random.setLargeFeatureSeed(this.seed + (long)(var8++), var4.x, var4.z);
            StrongholdPieces.resetPieces();
            var9 = new StrongholdPieces.StartPiece(this.random, var4.getBlockX(2), var4.getBlockZ(2));
            this.addPiece(var9);
            var9.addChildren(var9, this, this.random);
            List var10 = var9.pendingChildren;

            while(!var10.isEmpty()) {
               int var11 = this.random.nextInt(var10.size());
               StructurePiece var12 = (StructurePiece)var10.remove(var11);
               var12.addChildren(var9, this, this.random);
            }

            this.moveBelowSeaLevel(var2.getSeaLevel(), var2.getMinY(), this.random, 10);
         } while(this.hasNoPieces() || var9.portalRoomPiece == null);

      }
   }
}
