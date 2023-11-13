package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;

public class TreeConfiguration implements FeatureConfiguration {
   public static final Codec<TreeConfiguration> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(BlockStateProvider.CODEC.fieldOf("trunk_provider").forGetter((var0x) -> {
         return var0x.trunkProvider;
      }), TrunkPlacer.CODEC.fieldOf("trunk_placer").forGetter((var0x) -> {
         return var0x.trunkPlacer;
      }), BlockStateProvider.CODEC.fieldOf("foliage_provider").forGetter((var0x) -> {
         return var0x.foliageProvider;
      }), BlockStateProvider.CODEC.fieldOf("sapling_provider").forGetter((var0x) -> {
         return var0x.saplingProvider;
      }), FoliagePlacer.CODEC.fieldOf("foliage_placer").forGetter((var0x) -> {
         return var0x.foliagePlacer;
      }), BlockStateProvider.CODEC.fieldOf("dirt_provider").forGetter((var0x) -> {
         return var0x.dirtProvider;
      }), FeatureSize.CODEC.fieldOf("minimum_size").forGetter((var0x) -> {
         return var0x.minimumSize;
      }), TreeDecorator.CODEC.listOf().fieldOf("decorators").forGetter((var0x) -> {
         return var0x.decorators;
      }), Codec.BOOL.fieldOf("ignore_vines").orElse(false).forGetter((var0x) -> {
         return var0x.ignoreVines;
      }), Codec.BOOL.fieldOf("force_dirt").orElse(false).forGetter((var0x) -> {
         return var0x.forceDirt;
      })).apply(var0, TreeConfiguration::new);
   });
   public final BlockStateProvider trunkProvider;
   public final BlockStateProvider dirtProvider;
   public final TrunkPlacer trunkPlacer;
   public final BlockStateProvider foliageProvider;
   public final BlockStateProvider saplingProvider;
   public final FoliagePlacer foliagePlacer;
   public final FeatureSize minimumSize;
   public final List<TreeDecorator> decorators;
   public final boolean ignoreVines;
   public final boolean forceDirt;

   protected TreeConfiguration(BlockStateProvider var1, TrunkPlacer var2, BlockStateProvider var3, BlockStateProvider var4, FoliagePlacer var5, BlockStateProvider var6, FeatureSize var7, List<TreeDecorator> var8, boolean var9, boolean var10) {
      this.trunkProvider = var1;
      this.trunkPlacer = var2;
      this.foliageProvider = var3;
      this.foliagePlacer = var5;
      this.dirtProvider = var6;
      this.saplingProvider = var4;
      this.minimumSize = var7;
      this.decorators = var8;
      this.ignoreVines = var9;
      this.forceDirt = var10;
   }

   public TreeConfiguration withDecorators(List<TreeDecorator> var1) {
      return new TreeConfiguration(this.trunkProvider, this.trunkPlacer, this.foliageProvider, this.saplingProvider, this.foliagePlacer, this.dirtProvider, this.minimumSize, var1, this.ignoreVines, this.forceDirt);
   }

   public static class TreeConfigurationBuilder {
      public final BlockStateProvider trunkProvider;
      private final TrunkPlacer trunkPlacer;
      public final BlockStateProvider foliageProvider;
      public final BlockStateProvider saplingProvider;
      private final FoliagePlacer foliagePlacer;
      private BlockStateProvider dirtProvider;
      private final FeatureSize minimumSize;
      private List<TreeDecorator> decorators = ImmutableList.of();
      private boolean ignoreVines;
      private boolean forceDirt;

      public TreeConfigurationBuilder(BlockStateProvider var1, TrunkPlacer var2, BlockStateProvider var3, BlockStateProvider var4, FoliagePlacer var5, FeatureSize var6) {
         this.trunkProvider = var1;
         this.trunkPlacer = var2;
         this.foliageProvider = var3;
         this.saplingProvider = var4;
         this.dirtProvider = new SimpleStateProvider(Blocks.DIRT.defaultBlockState());
         this.foliagePlacer = var5;
         this.minimumSize = var6;
      }

      public TreeConfiguration.TreeConfigurationBuilder dirt(BlockStateProvider var1) {
         this.dirtProvider = var1;
         return this;
      }

      public TreeConfiguration.TreeConfigurationBuilder decorators(List<TreeDecorator> var1) {
         this.decorators = var1;
         return this;
      }

      public TreeConfiguration.TreeConfigurationBuilder ignoreVines() {
         this.ignoreVines = true;
         return this;
      }

      public TreeConfiguration.TreeConfigurationBuilder forceDirt() {
         this.forceDirt = true;
         return this;
      }

      public TreeConfiguration build() {
         return new TreeConfiguration(this.trunkProvider, this.trunkPlacer, this.foliageProvider, this.saplingProvider, this.foliagePlacer, this.dirtProvider, this.minimumSize, this.decorators, this.ignoreVines, this.forceDirt);
      }
   }
}
