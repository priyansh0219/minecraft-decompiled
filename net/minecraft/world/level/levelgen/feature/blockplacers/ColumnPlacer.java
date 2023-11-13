package net.minecraft.world.level.levelgen.feature.blockplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public class ColumnPlacer extends BlockPlacer {
   public static final Codec<ColumnPlacer> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(IntProvider.NON_NEGATIVE_CODEC.fieldOf("size").forGetter((var0x) -> {
         return var0x.size;
      })).apply(var0, ColumnPlacer::new);
   });
   private final IntProvider size;

   public ColumnPlacer(IntProvider var1) {
      this.size = var1;
   }

   protected BlockPlacerType<?> type() {
      return BlockPlacerType.COLUMN_PLACER;
   }

   public void place(LevelAccessor var1, BlockPos var2, BlockState var3, Random var4) {
      BlockPos.MutableBlockPos var5 = var2.mutable();
      int var6 = this.size.sample(var4);

      for(int var7 = 0; var7 < var6; ++var7) {
         var1.setBlock(var5, var3, 2);
         var5.move(Direction.UP);
      }

   }
}
