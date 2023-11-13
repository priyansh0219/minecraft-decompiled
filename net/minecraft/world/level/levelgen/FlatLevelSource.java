package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

public class FlatLevelSource extends ChunkGenerator {
   public static final Codec<FlatLevelSource> CODEC;
   private final FlatLevelGeneratorSettings settings;

   public FlatLevelSource(FlatLevelGeneratorSettings var1) {
      super(new FixedBiomeSource(var1.getBiomeFromSettings()), new FixedBiomeSource(var1.getBiome()), var1.structureSettings(), 0L);
      this.settings = var1;
   }

   protected Codec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   public ChunkGenerator withSeed(long var1) {
      return this;
   }

   public FlatLevelGeneratorSettings settings() {
      return this.settings;
   }

   public void buildSurfaceAndBedrock(WorldGenRegion var1, ChunkAccess var2) {
   }

   public int getSpawnHeight(LevelHeightAccessor var1) {
      return var1.getMinBuildHeight() + Math.min(var1.getHeight(), this.settings.getLayers().size());
   }

   public CompletableFuture<ChunkAccess> fillFromNoise(Executor var1, StructureFeatureManager var2, ChunkAccess var3) {
      List var4 = this.settings.getLayers();
      BlockPos.MutableBlockPos var5 = new BlockPos.MutableBlockPos();
      Heightmap var6 = var3.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
      Heightmap var7 = var3.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

      for(int var8 = 0; var8 < Math.min(var3.getHeight(), var4.size()); ++var8) {
         BlockState var9 = (BlockState)var4.get(var8);
         if (var9 != null) {
            int var10 = var3.getMinBuildHeight() + var8;

            for(int var11 = 0; var11 < 16; ++var11) {
               for(int var12 = 0; var12 < 16; ++var12) {
                  var3.setBlockState(var5.set(var11, var10, var12), var9, false);
                  var6.update(var11, var10, var12, var9);
                  var7.update(var11, var10, var12, var9);
               }
            }
         }
      }

      return CompletableFuture.completedFuture(var3);
   }

   public int getBaseHeight(int var1, int var2, Heightmap.Types var3, LevelHeightAccessor var4) {
      List var5 = this.settings.getLayers();

      for(int var6 = Math.min(var5.size(), var4.getMaxBuildHeight()) - 1; var6 >= 0; --var6) {
         BlockState var7 = (BlockState)var5.get(var6);
         if (var7 != null && var3.isOpaque().test(var7)) {
            return var4.getMinBuildHeight() + var6 + 1;
         }
      }

      return var4.getMinBuildHeight();
   }

   public NoiseColumn getBaseColumn(int var1, int var2, LevelHeightAccessor var3) {
      return new NoiseColumn(var3.getMinBuildHeight(), (BlockState[])this.settings.getLayers().stream().limit((long)var3.getHeight()).map((var0) -> {
         return var0 == null ? Blocks.AIR.defaultBlockState() : var0;
      }).toArray((var0) -> {
         return new BlockState[var0];
      }));
   }

   static {
      CODEC = FlatLevelGeneratorSettings.CODEC.fieldOf("settings").xmap(FlatLevelSource::new, FlatLevelSource::settings).codec();
   }
}
