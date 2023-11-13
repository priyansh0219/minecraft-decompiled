package net.minecraft.world.level.levelgen.structure;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RangeDecoratorConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class NetherFossilFeature extends StructureFeature<RangeDecoratorConfiguration> {
   public NetherFossilFeature(Codec<RangeDecoratorConfiguration> var1) {
      super(var1);
   }

   public StructureFeature.StructureStartFactory<RangeDecoratorConfiguration> getStartFactory() {
      return NetherFossilFeature.FeatureStart::new;
   }

   public static class FeatureStart extends NoiseAffectingStructureStart<RangeDecoratorConfiguration> {
      public FeatureStart(StructureFeature<RangeDecoratorConfiguration> var1, ChunkPos var2, int var3, long var4) {
         super(var1, var2, var3, var4);
      }

      public void generatePieces(RegistryAccess var1, ChunkGenerator var2, StructureManager var3, ChunkPos var4, Biome var5, RangeDecoratorConfiguration var6, LevelHeightAccessor var7) {
         int var8 = var4.getMinBlockX() + this.random.nextInt(16);
         int var9 = var4.getMinBlockZ() + this.random.nextInt(16);
         int var10 = var2.getSeaLevel();
         WorldGenerationContext var11 = new WorldGenerationContext(var2) {
            // $FF: synthetic field
            final ChunkGenerator val$chunkGenerator;
            // $FF: synthetic field
            final NetherFossilFeature.FeatureStart this$0;

            {
               this.this$0 = var1;
               this.val$chunkGenerator = var2;
            }

            public int getMinGenY() {
               return this.val$chunkGenerator.getMinY();
            }

            public int getGenDepth() {
               return this.val$chunkGenerator.getGenDepth();
            }
         };
         int var12 = var6.height.sample(this.random, var11);
         NoiseColumn var13 = var2.getBaseColumn(var8, var9, var7);

         for(BlockPos.MutableBlockPos var14 = new BlockPos.MutableBlockPos(var8, var12, var9); var12 > var10; --var12) {
            BlockState var15 = var13.getBlockState(var14);
            var14.move(Direction.DOWN);
            BlockState var16 = var13.getBlockState(var14);
            if (var15.isAir() && (var16.is(Blocks.SOUL_SAND) || var16.isFaceSturdy(EmptyBlockGetter.INSTANCE, var14, Direction.UP))) {
               break;
            }
         }

         if (var12 > var10) {
            NetherFossilPieces.addPieces(var3, this, this.random, new BlockPos(var8, var12, var9));
         }
      }
   }
}
