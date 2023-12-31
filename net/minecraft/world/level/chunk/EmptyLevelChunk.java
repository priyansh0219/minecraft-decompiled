package net.minecraft.world.level.chunk;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.biome.Biomes;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class EmptyLevelChunk extends LevelChunk {
   public EmptyLevelChunk(Level var1, ChunkPos var2) {
      super((Level)var1, (ChunkPos)var2, (ChunkBiomeContainer)(new EmptyLevelChunk.EmptyChunkBiomeContainer(var1)));
   }

   public BlockState getBlockState(BlockPos var1) {
      return Blocks.VOID_AIR.defaultBlockState();
   }

   @Nullable
   public BlockState setBlockState(BlockPos var1, BlockState var2, boolean var3) {
      return null;
   }

   public FluidState getFluidState(BlockPos var1) {
      return Fluids.EMPTY.defaultFluidState();
   }

   public int getLightEmission(BlockPos var1) {
      return 0;
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos var1, LevelChunk.EntityCreationType var2) {
      return null;
   }

   public void addAndRegisterBlockEntity(BlockEntity var1) {
   }

   public void setBlockEntity(BlockEntity var1) {
   }

   public void removeBlockEntity(BlockPos var1) {
   }

   public void markUnsaved() {
   }

   public boolean isEmpty() {
      return true;
   }

   public boolean isYSpaceEmpty(int var1, int var2) {
      return true;
   }

   public ChunkHolder.FullChunkStatus getFullStatus() {
      return ChunkHolder.FullChunkStatus.BORDER;
   }

   static class EmptyChunkBiomeContainer extends ChunkBiomeContainer {
      private static final Biome[] EMPTY_BIOMES = new Biome[0];

      public EmptyChunkBiomeContainer(Level var1) {
         super(var1.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), var1, (Biome[])EMPTY_BIOMES);
      }

      public int[] writeBiomes() {
         throw new UnsupportedOperationException("Can not write biomes of an empty chunk");
      }

      public Biome getNoiseBiome(int var1, int var2, int var3) {
         return Biomes.PLAINS;
      }
   }
}
