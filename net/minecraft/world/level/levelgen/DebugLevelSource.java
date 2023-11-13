package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.RegistryLookupCodec;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;

public class DebugLevelSource extends ChunkGenerator {
   public static final Codec<DebugLevelSource> CODEC;
   private static final int BLOCK_MARGIN = 2;
   private static final List<BlockState> ALL_BLOCKS;
   private static final int GRID_WIDTH;
   private static final int GRID_HEIGHT;
   protected static final BlockState AIR;
   protected static final BlockState BARRIER;
   public static final int HEIGHT = 70;
   public static final int BARRIER_HEIGHT = 60;
   private final Registry<Biome> biomes;

   public DebugLevelSource(Registry<Biome> var1) {
      super(new FixedBiomeSource((Biome)var1.getOrThrow(Biomes.PLAINS)), new StructureSettings(false));
      this.biomes = var1;
   }

   public Registry<Biome> biomes() {
      return this.biomes;
   }

   protected Codec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   public ChunkGenerator withSeed(long var1) {
      return this;
   }

   public void buildSurfaceAndBedrock(WorldGenRegion var1, ChunkAccess var2) {
   }

   public void applyCarvers(long var1, BiomeManager var3, ChunkAccess var4, GenerationStep.Carving var5) {
   }

   public void applyBiomeDecoration(WorldGenRegion var1, StructureFeatureManager var2) {
      BlockPos.MutableBlockPos var3 = new BlockPos.MutableBlockPos();
      ChunkPos var4 = var1.getCenter();

      for(int var5 = 0; var5 < 16; ++var5) {
         for(int var6 = 0; var6 < 16; ++var6) {
            int var7 = SectionPos.sectionToBlockCoord(var4.x, var5);
            int var8 = SectionPos.sectionToBlockCoord(var4.z, var6);
            var1.setBlock(var3.set(var7, 60, var8), BARRIER, 2);
            BlockState var9 = getBlockStateFor(var7, var8);
            if (var9 != null) {
               var1.setBlock(var3.set(var7, 70, var8), var9, 2);
            }
         }
      }

   }

   public CompletableFuture<ChunkAccess> fillFromNoise(Executor var1, StructureFeatureManager var2, ChunkAccess var3) {
      return CompletableFuture.completedFuture(var3);
   }

   public int getBaseHeight(int var1, int var2, Heightmap.Types var3, LevelHeightAccessor var4) {
      return 0;
   }

   public NoiseColumn getBaseColumn(int var1, int var2, LevelHeightAccessor var3) {
      return new NoiseColumn(0, new BlockState[0]);
   }

   public static BlockState getBlockStateFor(int var0, int var1) {
      BlockState var2 = AIR;
      if (var0 > 0 && var1 > 0 && var0 % 2 != 0 && var1 % 2 != 0) {
         var0 /= 2;
         var1 /= 2;
         if (var0 <= GRID_WIDTH && var1 <= GRID_HEIGHT) {
            int var3 = Mth.abs(var0 * GRID_WIDTH + var1);
            if (var3 < ALL_BLOCKS.size()) {
               var2 = (BlockState)ALL_BLOCKS.get(var3);
            }
         }
      }

      return var2;
   }

   static {
      CODEC = RegistryLookupCodec.create(Registry.BIOME_REGISTRY).xmap(DebugLevelSource::new, DebugLevelSource::biomes).stable().codec();
      ALL_BLOCKS = (List)StreamSupport.stream(Registry.BLOCK.spliterator(), false).flatMap((var0) -> {
         return var0.getStateDefinition().getPossibleStates().stream();
      }).collect(Collectors.toList());
      GRID_WIDTH = Mth.ceil(Mth.sqrt((float)ALL_BLOCKS.size()));
      GRID_HEIGHT = Mth.ceil((float)ALL_BLOCKS.size() / (float)GRID_WIDTH);
      AIR = Blocks.AIR.defaultBlockState();
      BARRIER = Blocks.BARRIER.defaultBlockState();
   }
}
