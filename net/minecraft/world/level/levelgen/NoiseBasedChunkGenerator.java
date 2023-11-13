package net.minecraft.world.level.levelgen;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.levelgen.synth.SurfaceNoise;

public final class NoiseBasedChunkGenerator extends ChunkGenerator {
   public static final Codec<NoiseBasedChunkGenerator> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter((var0x) -> {
         return var0x.biomeSource;
      }), Codec.LONG.fieldOf("seed").stable().forGetter((var0x) -> {
         return var0x.seed;
      }), NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter((var0x) -> {
         return var0x.settings;
      })).apply(var0, var0.stable(NoiseBasedChunkGenerator::new));
   });
   private static final BlockState AIR;
   private static final BlockState[] EMPTY_COLUMN;
   private final int cellHeight;
   private final int cellWidth;
   final int cellCountX;
   final int cellCountY;
   final int cellCountZ;
   private final SurfaceNoise surfaceNoise;
   private final NormalNoise barrierNoise;
   private final NormalNoise waterLevelNoise;
   private final NormalNoise lavaNoise;
   protected final BlockState defaultBlock;
   protected final BlockState defaultFluid;
   private final long seed;
   protected final Supplier<NoiseGeneratorSettings> settings;
   private final int height;
   private final NoiseSampler sampler;
   private final BaseStoneSource baseStoneSource;
   final OreVeinifier oreVeinifier;
   final NoodleCavifier noodleCavifier;

   public NoiseBasedChunkGenerator(BiomeSource var1, long var2, Supplier<NoiseGeneratorSettings> var4) {
      this(var1, var1, var2, var4);
   }

   private NoiseBasedChunkGenerator(BiomeSource var1, BiomeSource var2, long var3, Supplier<NoiseGeneratorSettings> var5) {
      super(var1, var2, ((NoiseGeneratorSettings)var5.get()).structureSettings(), var3);
      this.seed = var3;
      NoiseGeneratorSettings var6 = (NoiseGeneratorSettings)var5.get();
      this.settings = var5;
      NoiseSettings var7 = var6.noiseSettings();
      this.height = var7.height();
      this.cellHeight = QuartPos.toBlock(var7.noiseSizeVertical());
      this.cellWidth = QuartPos.toBlock(var7.noiseSizeHorizontal());
      this.defaultBlock = var6.getDefaultBlock();
      this.defaultFluid = var6.getDefaultFluid();
      this.cellCountX = 16 / this.cellWidth;
      this.cellCountY = var7.height() / this.cellHeight;
      this.cellCountZ = 16 / this.cellWidth;
      WorldgenRandom var8 = new WorldgenRandom(var3);
      BlendedNoise var9 = new BlendedNoise(var8);
      this.surfaceNoise = (SurfaceNoise)(var7.useSimplexSurfaceNoise() ? new PerlinSimplexNoise(var8, IntStream.rangeClosed(-3, 0)) : new PerlinNoise(var8, IntStream.rangeClosed(-3, 0)));
      var8.consumeCount(2620);
      PerlinNoise var10 = new PerlinNoise(var8, IntStream.rangeClosed(-15, 0));
      SimplexNoise var11;
      if (var7.islandNoiseOverride()) {
         WorldgenRandom var12 = new WorldgenRandom(var3);
         var12.consumeCount(17292);
         var11 = new SimplexNoise(var12);
      } else {
         var11 = null;
      }

      this.barrierNoise = NormalNoise.create(new SimpleRandomSource(var8.nextLong()), -3, (double[])(1.0D));
      this.waterLevelNoise = NormalNoise.create(new SimpleRandomSource(var8.nextLong()), -3, (double[])(1.0D, 0.0D, 2.0D));
      this.lavaNoise = NormalNoise.create(new SimpleRandomSource(var8.nextLong()), -1, (double[])(1.0D, 0.0D));
      Object var13;
      if (var6.isNoiseCavesEnabled()) {
         var13 = new Cavifier(var8, var7.minY() / this.cellHeight);
      } else {
         var13 = NoiseModifier.PASSTHROUGH;
      }

      this.sampler = new NoiseSampler(var1, this.cellWidth, this.cellHeight, this.cellCountY, var7, var9, var11, var10, (NoiseModifier)var13);
      this.baseStoneSource = new DepthBasedReplacingBaseStoneSource(var3, this.defaultBlock, Blocks.DEEPSLATE.defaultBlockState(), var6);
      this.oreVeinifier = new OreVeinifier(var3, this.defaultBlock, this.cellWidth, this.cellHeight, var6.noiseSettings().minY());
      this.noodleCavifier = new NoodleCavifier(var3);
   }

   private boolean isAquifersEnabled() {
      return ((NoiseGeneratorSettings)this.settings.get()).isAquifersEnabled();
   }

   protected Codec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   public ChunkGenerator withSeed(long var1) {
      return new NoiseBasedChunkGenerator(this.biomeSource.withSeed(var1), var1, this.settings);
   }

   public boolean stable(long var1, ResourceKey<NoiseGeneratorSettings> var3) {
      return this.seed == var1 && ((NoiseGeneratorSettings)this.settings.get()).stable(var3);
   }

   private double[] makeAndFillNoiseColumn(int var1, int var2, int var3, int var4) {
      double[] var5 = new double[var4 + 1];
      this.fillNoiseColumn(var5, var1, var2, var3, var4);
      return var5;
   }

   private void fillNoiseColumn(double[] var1, int var2, int var3, int var4, int var5) {
      NoiseSettings var6 = ((NoiseGeneratorSettings)this.settings.get()).noiseSettings();
      this.sampler.fillNoiseColumn(var1, var2, var3, var6, this.getSeaLevel(), var4, var5);
   }

   public int getBaseHeight(int var1, int var2, Heightmap.Types var3, LevelHeightAccessor var4) {
      int var5 = Math.max(((NoiseGeneratorSettings)this.settings.get()).noiseSettings().minY(), var4.getMinBuildHeight());
      int var6 = Math.min(((NoiseGeneratorSettings)this.settings.get()).noiseSettings().minY() + ((NoiseGeneratorSettings)this.settings.get()).noiseSettings().height(), var4.getMaxBuildHeight());
      int var7 = Mth.intFloorDiv(var5, this.cellHeight);
      int var8 = Mth.intFloorDiv(var6 - var5, this.cellHeight);
      return var8 <= 0 ? var4.getMinBuildHeight() : this.iterateNoiseColumn(var1, var2, (BlockState[])null, var3.isOpaque(), var7, var8).orElse(var4.getMinBuildHeight());
   }

   public NoiseColumn getBaseColumn(int var1, int var2, LevelHeightAccessor var3) {
      int var4 = Math.max(((NoiseGeneratorSettings)this.settings.get()).noiseSettings().minY(), var3.getMinBuildHeight());
      int var5 = Math.min(((NoiseGeneratorSettings)this.settings.get()).noiseSettings().minY() + ((NoiseGeneratorSettings)this.settings.get()).noiseSettings().height(), var3.getMaxBuildHeight());
      int var6 = Mth.intFloorDiv(var4, this.cellHeight);
      int var7 = Mth.intFloorDiv(var5 - var4, this.cellHeight);
      if (var7 <= 0) {
         return new NoiseColumn(var4, EMPTY_COLUMN);
      } else {
         BlockState[] var8 = new BlockState[var7 * this.cellHeight];
         this.iterateNoiseColumn(var1, var2, var8, (Predicate)null, var6, var7);
         return new NoiseColumn(var4, var8);
      }
   }

   public BaseStoneSource getBaseStoneSource() {
      return this.baseStoneSource;
   }

   private OptionalInt iterateNoiseColumn(int var1, int var2, @Nullable BlockState[] var3, @Nullable Predicate<BlockState> var4, int var5, int var6) {
      int var7 = SectionPos.blockToSectionCoord(var1);
      int var8 = SectionPos.blockToSectionCoord(var2);
      int var9 = Math.floorDiv(var1, this.cellWidth);
      int var10 = Math.floorDiv(var2, this.cellWidth);
      int var11 = Math.floorMod(var1, this.cellWidth);
      int var12 = Math.floorMod(var2, this.cellWidth);
      double var13 = (double)var11 / (double)this.cellWidth;
      double var15 = (double)var12 / (double)this.cellWidth;
      double[][] var17 = new double[][]{this.makeAndFillNoiseColumn(var9, var10, var5, var6), this.makeAndFillNoiseColumn(var9, var10 + 1, var5, var6), this.makeAndFillNoiseColumn(var9 + 1, var10, var5, var6), this.makeAndFillNoiseColumn(var9 + 1, var10 + 1, var5, var6)};
      Aquifer var18 = this.getAquifer(var5, var6, new ChunkPos(var7, var8));

      for(int var19 = var6 - 1; var19 >= 0; --var19) {
         double var20 = var17[0][var19];
         double var22 = var17[1][var19];
         double var24 = var17[2][var19];
         double var26 = var17[3][var19];
         double var28 = var17[0][var19 + 1];
         double var30 = var17[1][var19 + 1];
         double var32 = var17[2][var19 + 1];
         double var34 = var17[3][var19 + 1];

         for(int var36 = this.cellHeight - 1; var36 >= 0; --var36) {
            double var37 = (double)var36 / (double)this.cellHeight;
            double var39 = Mth.lerp3(var37, var13, var15, var20, var28, var24, var32, var22, var30, var26, var34);
            int var41 = var19 * this.cellHeight + var36;
            int var42 = var41 + var5 * this.cellHeight;
            BlockState var43 = this.updateNoiseAndGenerateBaseState(Beardifier.NO_BEARDS, var18, this.baseStoneSource, NoiseModifier.PASSTHROUGH, var1, var42, var2, var39);
            if (var3 != null) {
               var3[var41] = var43;
            }

            if (var4 != null && var4.test(var43)) {
               return OptionalInt.of(var42 + 1);
            }
         }
      }

      return OptionalInt.empty();
   }

   private Aquifer getAquifer(int var1, int var2, ChunkPos var3) {
      return !this.isAquifersEnabled() ? Aquifer.createDisabled(this.getSeaLevel(), this.defaultFluid) : Aquifer.create(var3, this.barrierNoise, this.waterLevelNoise, this.lavaNoise, (NoiseGeneratorSettings)this.settings.get(), this.sampler, var1 * this.cellHeight, var2 * this.cellHeight);
   }

   protected BlockState updateNoiseAndGenerateBaseState(Beardifier var1, Aquifer var2, BaseStoneSource var3, NoiseModifier var4, int var5, int var6, int var7, double var8) {
      double var10 = Mth.clamp(var8 / 200.0D, -1.0D, 1.0D);
      var10 = var10 / 2.0D - var10 * var10 * var10 / 24.0D;
      var10 = var4.modifyNoise(var10, var5, var6, var7);
      var10 += var1.beardifyOrBury(var5, var6, var7);
      return var2.computeState(var3, var5, var6, var7, var10);
   }

   public void buildSurfaceAndBedrock(WorldGenRegion var1, ChunkAccess var2) {
      ChunkPos var3 = var2.getPos();
      int var4 = var3.x;
      int var5 = var3.z;
      WorldgenRandom var6 = new WorldgenRandom();
      var6.setBaseChunkSeed(var4, var5);
      ChunkPos var7 = var2.getPos();
      int var8 = var7.getMinBlockX();
      int var9 = var7.getMinBlockZ();
      double var10 = 0.0625D;
      BlockPos.MutableBlockPos var12 = new BlockPos.MutableBlockPos();

      for(int var13 = 0; var13 < 16; ++var13) {
         for(int var14 = 0; var14 < 16; ++var14) {
            int var15 = var8 + var13;
            int var16 = var9 + var14;
            int var17 = var2.getHeight(Heightmap.Types.WORLD_SURFACE_WG, var13, var14) + 1;
            double var18 = this.surfaceNoise.getSurfaceNoiseValue((double)var15 * 0.0625D, (double)var16 * 0.0625D, 0.0625D, (double)var13 * 0.0625D) * 15.0D;
            int var20 = ((NoiseGeneratorSettings)this.settings.get()).getMinSurfaceLevel();
            var1.getBiome(var12.set(var8 + var13, var17, var9 + var14)).buildSurfaceAt(var6, var2, var15, var16, var17, var18, this.defaultBlock, this.defaultFluid, this.getSeaLevel(), var20, var1.getSeed());
         }
      }

      this.setBedrock(var2, var6);
   }

   private void setBedrock(ChunkAccess var1, Random var2) {
      BlockPos.MutableBlockPos var3 = new BlockPos.MutableBlockPos();
      int var4 = var1.getPos().getMinBlockX();
      int var5 = var1.getPos().getMinBlockZ();
      NoiseGeneratorSettings var6 = (NoiseGeneratorSettings)this.settings.get();
      int var7 = var6.noiseSettings().minY();
      int var8 = var7 + var6.getBedrockFloorPosition();
      int var9 = this.height - 1 + var7 - var6.getBedrockRoofPosition();
      boolean var10 = true;
      int var11 = var1.getMinBuildHeight();
      int var12 = var1.getMaxBuildHeight();
      boolean var13 = var9 + 5 - 1 >= var11 && var9 < var12;
      boolean var14 = var8 + 5 - 1 >= var11 && var8 < var12;
      if (var13 || var14) {
         Iterator var15 = BlockPos.betweenClosed(var4, 0, var5, var4 + 15, 0, var5 + 15).iterator();

         while(true) {
            BlockPos var16;
            int var17;
            do {
               if (!var15.hasNext()) {
                  return;
               }

               var16 = (BlockPos)var15.next();
               if (var13) {
                  for(var17 = 0; var17 < 5; ++var17) {
                     if (var17 <= var2.nextInt(5)) {
                        var1.setBlockState(var3.set(var16.getX(), var9 - var17, var16.getZ()), Blocks.BEDROCK.defaultBlockState(), false);
                     }
                  }
               }
            } while(!var14);

            for(var17 = 4; var17 >= 0; --var17) {
               if (var17 <= var2.nextInt(5)) {
                  var1.setBlockState(var3.set(var16.getX(), var8 + var17, var16.getZ()), Blocks.BEDROCK.defaultBlockState(), false);
               }
            }
         }
      }
   }

   public CompletableFuture<ChunkAccess> fillFromNoise(Executor var1, StructureFeatureManager var2, ChunkAccess var3) {
      NoiseSettings var4 = ((NoiseGeneratorSettings)this.settings.get()).noiseSettings();
      int var5 = Math.max(var4.minY(), var3.getMinBuildHeight());
      int var6 = Math.min(var4.minY() + var4.height(), var3.getMaxBuildHeight());
      int var7 = Mth.intFloorDiv(var5, this.cellHeight);
      int var8 = Mth.intFloorDiv(var6 - var5, this.cellHeight);
      if (var8 <= 0) {
         return CompletableFuture.completedFuture(var3);
      } else {
         int var9 = var3.getSectionIndex(var8 * this.cellHeight - 1 + var5);
         int var10 = var3.getSectionIndex(var5);
         return CompletableFuture.supplyAsync(() -> {
            HashSet var7x = Sets.newHashSet();
            boolean var15 = false;

            ChunkAccess var17;
            try {
               var15 = true;
               int var8x = var9;

               while(true) {
                  if (var8x < var10) {
                     var17 = this.doFill(var2, var3, var7, var8);
                     var15 = false;
                     break;
                  }

                  LevelChunkSection var9x = var3.getOrCreateSection(var8x);
                  var9x.acquire();
                  var7x.add(var9x);
                  --var8x;
               }
            } finally {
               if (var15) {
                  Iterator var12 = var7x.iterator();

                  while(var12.hasNext()) {
                     LevelChunkSection var13 = (LevelChunkSection)var12.next();
                     var13.release();
                  }

               }
            }

            Iterator var18 = var7x.iterator();

            while(var18.hasNext()) {
               LevelChunkSection var10x = (LevelChunkSection)var18.next();
               var10x.release();
            }

            return var17;
         }, Util.backgroundExecutor());
      }
   }

   private ChunkAccess doFill(StructureFeatureManager var1, ChunkAccess var2, int var3, int var4) {
      Heightmap var5 = var2.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
      Heightmap var6 = var2.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
      ChunkPos var7 = var2.getPos();
      int var8 = var7.getMinBlockX();
      int var9 = var7.getMinBlockZ();
      Beardifier var10 = new Beardifier(var1, var2);
      Aquifer var11 = this.getAquifer(var3, var4, var7);
      NoiseInterpolator var12 = new NoiseInterpolator(this.cellCountX, var4, this.cellCountZ, var7, var3, this::fillNoiseColumn);
      ArrayList var13 = Lists.newArrayList(new NoiseInterpolator[]{var12});
      Objects.requireNonNull(var13);
      Consumer var14 = var13::add;
      DoubleFunction var15 = this.createBaseStoneSource(var3, var7, var14);
      DoubleFunction var16 = this.createCaveNoiseModifier(var3, var7, var14);
      var13.forEach(NoiseInterpolator::initializeForFirstCellX);
      BlockPos.MutableBlockPos var17 = new BlockPos.MutableBlockPos();

      for(int var18 = 0; var18 < this.cellCountX; ++var18) {
         var13.forEach((var1x) -> {
            var1x.advanceCellX(var18);
         });

         for(int var20 = 0; var20 < this.cellCountZ; ++var20) {
            LevelChunkSection var21 = var2.getOrCreateSection(var2.getSectionsCount() - 1);

            for(int var22 = var4 - 1; var22 >= 0; --var22) {
               var13.forEach((var2x) -> {
                  var2x.selectCellYZ(var22, var20);
               });

               for(int var25 = this.cellHeight - 1; var25 >= 0; --var25) {
                  int var26 = (var3 + var22) * this.cellHeight + var25;
                  int var27 = var26 & 15;
                  int var28 = var2.getSectionIndex(var26);
                  if (var2.getSectionIndex(var21.bottomBlockY()) != var28) {
                     var21 = var2.getOrCreateSection(var28);
                  }

                  double var29 = (double)var25 / (double)this.cellHeight;
                  var13.forEach((var2x) -> {
                     var2x.updateForY(var29);
                  });

                  for(int var31 = 0; var31 < this.cellWidth; ++var31) {
                     int var32 = var8 + var18 * this.cellWidth + var31;
                     int var33 = var32 & 15;
                     double var34 = (double)var31 / (double)this.cellWidth;
                     var13.forEach((var2x) -> {
                        var2x.updateForX(var34);
                     });

                     for(int var36 = 0; var36 < this.cellWidth; ++var36) {
                        int var37 = var9 + var20 * this.cellWidth + var36;
                        int var38 = var37 & 15;
                        double var39 = (double)var36 / (double)this.cellWidth;
                        double var41 = var12.calculateValue(var39);
                        BlockState var43 = this.updateNoiseAndGenerateBaseState(var10, var11, (BaseStoneSource)var15.apply(var39), (NoiseModifier)var16.apply(var39), var32, var26, var37, var41);
                        if (var43 != AIR) {
                           if (var43.getLightEmission() != 0 && var2 instanceof ProtoChunk) {
                              var17.set(var32, var26, var37);
                              ((ProtoChunk)var2).addLight(var17);
                           }

                           var21.setBlockState(var33, var27, var38, var43, false);
                           var5.update(var33, var26, var38, var43);
                           var6.update(var33, var26, var38, var43);
                           if (var11.shouldScheduleFluidUpdate() && !var43.getFluidState().isEmpty()) {
                              var17.set(var32, var26, var37);
                              var2.getLiquidTicks().scheduleTick(var17, var43.getFluidState().getType(), 0);
                           }
                        }
                     }
                  }
               }
            }
         }

         var13.forEach(NoiseInterpolator::swapSlices);
      }

      return var2;
   }

   private DoubleFunction<NoiseModifier> createCaveNoiseModifier(int var1, ChunkPos var2, Consumer<NoiseInterpolator> var3) {
      if (!((NoiseGeneratorSettings)this.settings.get()).isNoodleCavesEnabled()) {
         return (var0) -> {
            return NoiseModifier.PASSTHROUGH;
         };
      } else {
         NoiseBasedChunkGenerator.NoodleCaveNoiseModifier var4 = new NoiseBasedChunkGenerator.NoodleCaveNoiseModifier(var2, var1);
         var4.listInterpolators(var3);
         Objects.requireNonNull(var4);
         return var4::prepare;
      }
   }

   private DoubleFunction<BaseStoneSource> createBaseStoneSource(int var1, ChunkPos var2, Consumer<NoiseInterpolator> var3) {
      if (!((NoiseGeneratorSettings)this.settings.get()).isOreVeinsEnabled()) {
         return (var1x) -> {
            return this.baseStoneSource;
         };
      } else {
         NoiseBasedChunkGenerator.OreVeinNoiseSource var4 = new NoiseBasedChunkGenerator.OreVeinNoiseSource(var2, var1, this.seed + 1L);
         var4.listInterpolators(var3);
         BaseStoneSource var5 = (var2x, var3x, var4x) -> {
            BlockState var5 = var4.getBaseBlock(var2x, var3x, var4x);
            return var5 != this.defaultBlock ? var5 : this.baseStoneSource.getBaseBlock(var2x, var3x, var4x);
         };
         return (var2x) -> {
            var4.prepare(var2x);
            return var5;
         };
      }
   }

   protected Aquifer createAquifer(ChunkAccess var1) {
      ChunkPos var2 = var1.getPos();
      int var3 = Math.max(((NoiseGeneratorSettings)this.settings.get()).noiseSettings().minY(), var1.getMinBuildHeight());
      int var4 = Mth.intFloorDiv(var3, this.cellHeight);
      return this.getAquifer(var4, this.cellCountY, var2);
   }

   public int getGenDepth() {
      return this.height;
   }

   public int getSeaLevel() {
      return ((NoiseGeneratorSettings)this.settings.get()).seaLevel();
   }

   public int getMinY() {
      return ((NoiseGeneratorSettings)this.settings.get()).noiseSettings().minY();
   }

   public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Biome var1, StructureFeatureManager var2, MobCategory var3, BlockPos var4) {
      if (var2.getStructureAt(var4, true, StructureFeature.SWAMP_HUT).isValid()) {
         if (var3 == MobCategory.MONSTER) {
            return StructureFeature.SWAMP_HUT.getSpecialEnemies();
         }

         if (var3 == MobCategory.CREATURE) {
            return StructureFeature.SWAMP_HUT.getSpecialAnimals();
         }
      }

      if (var3 == MobCategory.MONSTER) {
         if (var2.getStructureAt(var4, false, StructureFeature.PILLAGER_OUTPOST).isValid()) {
            return StructureFeature.PILLAGER_OUTPOST.getSpecialEnemies();
         }

         if (var2.getStructureAt(var4, false, StructureFeature.OCEAN_MONUMENT).isValid()) {
            return StructureFeature.OCEAN_MONUMENT.getSpecialEnemies();
         }

         if (var2.getStructureAt(var4, true, StructureFeature.NETHER_BRIDGE).isValid()) {
            return StructureFeature.NETHER_BRIDGE.getSpecialEnemies();
         }
      }

      return var3 == MobCategory.UNDERGROUND_WATER_CREATURE && var2.getStructureAt(var4, false, StructureFeature.OCEAN_MONUMENT).isValid() ? StructureFeature.OCEAN_MONUMENT.getSpecialUndergroundWaterAnimals() : super.getMobsAt(var1, var2, var3, var4);
   }

   public void spawnOriginalMobs(WorldGenRegion var1) {
      if (!((NoiseGeneratorSettings)this.settings.get()).disableMobGeneration()) {
         ChunkPos var2 = var1.getCenter();
         Biome var3 = var1.getBiome(var2.getWorldPosition());
         WorldgenRandom var4 = new WorldgenRandom();
         var4.setDecorationSeed(var1.getSeed(), var2.getMinBlockX(), var2.getMinBlockZ());
         NaturalSpawner.spawnMobsForChunkGeneration(var1, var3, var2, var4);
      }
   }

   static {
      AIR = Blocks.AIR.defaultBlockState();
      EMPTY_COLUMN = new BlockState[0];
   }

   class NoodleCaveNoiseModifier implements NoiseModifier {
      private final NoiseInterpolator toggle;
      private final NoiseInterpolator thickness;
      private final NoiseInterpolator ridgeA;
      private final NoiseInterpolator ridgeB;
      private double factorZ;

      public NoodleCaveNoiseModifier(ChunkPos var2, int var3) {
         int var10003 = NoiseBasedChunkGenerator.this.cellCountX;
         int var10004 = NoiseBasedChunkGenerator.this.cellCountY;
         int var10005 = NoiseBasedChunkGenerator.this.cellCountZ;
         NoodleCavifier var10008 = NoiseBasedChunkGenerator.this.noodleCavifier;
         Objects.requireNonNull(var10008);
         this.toggle = new NoiseInterpolator(var10003, var10004, var10005, var2, var3, var10008::fillToggleNoiseColumn);
         var10003 = NoiseBasedChunkGenerator.this.cellCountX;
         var10004 = NoiseBasedChunkGenerator.this.cellCountY;
         var10005 = NoiseBasedChunkGenerator.this.cellCountZ;
         var10008 = NoiseBasedChunkGenerator.this.noodleCavifier;
         Objects.requireNonNull(var10008);
         this.thickness = new NoiseInterpolator(var10003, var10004, var10005, var2, var3, var10008::fillThicknessNoiseColumn);
         var10003 = NoiseBasedChunkGenerator.this.cellCountX;
         var10004 = NoiseBasedChunkGenerator.this.cellCountY;
         var10005 = NoiseBasedChunkGenerator.this.cellCountZ;
         var10008 = NoiseBasedChunkGenerator.this.noodleCavifier;
         Objects.requireNonNull(var10008);
         this.ridgeA = new NoiseInterpolator(var10003, var10004, var10005, var2, var3, var10008::fillRidgeANoiseColumn);
         var10003 = NoiseBasedChunkGenerator.this.cellCountX;
         var10004 = NoiseBasedChunkGenerator.this.cellCountY;
         var10005 = NoiseBasedChunkGenerator.this.cellCountZ;
         var10008 = NoiseBasedChunkGenerator.this.noodleCavifier;
         Objects.requireNonNull(var10008);
         this.ridgeB = new NoiseInterpolator(var10003, var10004, var10005, var2, var3, var10008::fillRidgeBNoiseColumn);
      }

      public NoiseModifier prepare(double var1) {
         this.factorZ = var1;
         return this;
      }

      public double modifyNoise(double var1, int var3, int var4, int var5) {
         double var6 = this.toggle.calculateValue(this.factorZ);
         double var8 = this.thickness.calculateValue(this.factorZ);
         double var10 = this.ridgeA.calculateValue(this.factorZ);
         double var12 = this.ridgeB.calculateValue(this.factorZ);
         return NoiseBasedChunkGenerator.this.noodleCavifier.noodleCavify(var1, var3, var4, var5, var6, var8, var10, var12, NoiseBasedChunkGenerator.this.getMinY());
      }

      public void listInterpolators(Consumer<NoiseInterpolator> var1) {
         var1.accept(this.toggle);
         var1.accept(this.thickness);
         var1.accept(this.ridgeA);
         var1.accept(this.ridgeB);
      }
   }

   private class OreVeinNoiseSource implements BaseStoneSource {
      private final NoiseInterpolator veininess;
      private final NoiseInterpolator veinA;
      private final NoiseInterpolator veinB;
      private double factorZ;
      private final long seed;
      private final WorldgenRandom random = new WorldgenRandom();

      public OreVeinNoiseSource(ChunkPos var2, int var3, long var4) {
         int var10003 = NoiseBasedChunkGenerator.this.cellCountX;
         int var10004 = NoiseBasedChunkGenerator.this.cellCountY;
         int var10005 = NoiseBasedChunkGenerator.this.cellCountZ;
         OreVeinifier var10008 = NoiseBasedChunkGenerator.this.oreVeinifier;
         Objects.requireNonNull(var10008);
         this.veininess = new NoiseInterpolator(var10003, var10004, var10005, var2, var3, var10008::fillVeininessNoiseColumn);
         var10003 = NoiseBasedChunkGenerator.this.cellCountX;
         var10004 = NoiseBasedChunkGenerator.this.cellCountY;
         var10005 = NoiseBasedChunkGenerator.this.cellCountZ;
         var10008 = NoiseBasedChunkGenerator.this.oreVeinifier;
         Objects.requireNonNull(var10008);
         this.veinA = new NoiseInterpolator(var10003, var10004, var10005, var2, var3, var10008::fillNoiseColumnA);
         var10003 = NoiseBasedChunkGenerator.this.cellCountX;
         var10004 = NoiseBasedChunkGenerator.this.cellCountY;
         var10005 = NoiseBasedChunkGenerator.this.cellCountZ;
         var10008 = NoiseBasedChunkGenerator.this.oreVeinifier;
         Objects.requireNonNull(var10008);
         this.veinB = new NoiseInterpolator(var10003, var10004, var10005, var2, var3, var10008::fillNoiseColumnB);
         this.seed = var4;
      }

      public void listInterpolators(Consumer<NoiseInterpolator> var1) {
         var1.accept(this.veininess);
         var1.accept(this.veinA);
         var1.accept(this.veinB);
      }

      public void prepare(double var1) {
         this.factorZ = var1;
      }

      public BlockState getBaseBlock(int var1, int var2, int var3) {
         double var4 = this.veininess.calculateValue(this.factorZ);
         double var6 = this.veinA.calculateValue(this.factorZ);
         double var8 = this.veinB.calculateValue(this.factorZ);
         this.random.setBaseStoneSeed(this.seed, var1, var2, var3);
         return NoiseBasedChunkGenerator.this.oreVeinifier.oreVeinify(this.random, var1, var2, var3, var4, var6, var8);
      }
   }
}
