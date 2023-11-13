package net.minecraft.world.level.levelgen.placement;

import java.util.BitSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class DecorationContext implements WorldGenerationContext {
   private final WorldGenLevel level;
   private final ChunkGenerator generator;

   public DecorationContext(WorldGenLevel var1, ChunkGenerator var2) {
      this.level = var1;
      this.generator = var2;
   }

   public int getHeight(Heightmap.Types var1, int var2, int var3) {
      return this.level.getHeight(var1, var2, var3);
   }

   public int getMinGenY() {
      return this.generator.getMinY();
   }

   public int getGenDepth() {
      return this.generator.getGenDepth();
   }

   public BitSet getCarvingMask(ChunkPos var1, GenerationStep.Carving var2) {
      return ((ProtoChunk)this.level.getChunk(var1.x, var1.z)).getOrCreateCarvingMask(var2);
   }

   public BlockState getBlockState(BlockPos var1) {
      return this.level.getBlockState(var1);
   }

   public int getMinBuildHeight() {
      return this.level.getMinBuildHeight();
   }

   public WorldGenLevel getLevel() {
      return this.level;
   }
}
