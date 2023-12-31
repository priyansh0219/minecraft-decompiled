package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class RotatedBlockProvider extends BlockStateProvider {
   public static final Codec<RotatedBlockProvider> CODEC;
   private final Block block;

   public RotatedBlockProvider(Block var1) {
      this.block = var1;
   }

   protected BlockStateProviderType<?> type() {
      return BlockStateProviderType.ROTATED_BLOCK_PROVIDER;
   }

   public BlockState getState(Random var1, BlockPos var2) {
      Direction.Axis var3 = Direction.Axis.getRandom(var1);
      return (BlockState)this.block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, var3);
   }

   static {
      CODEC = BlockState.CODEC.fieldOf("state").xmap(BlockBehaviour.BlockStateBase::getBlock, Block::defaultBlockState).xmap(RotatedBlockProvider::new, (var0) -> {
         return var0.block;
      }).codec();
   }
}
