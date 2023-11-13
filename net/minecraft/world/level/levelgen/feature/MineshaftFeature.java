package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.MineshaftConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.MineShaftPieces;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class MineshaftFeature extends StructureFeature<MineshaftConfiguration> {
   public MineshaftFeature(Codec<MineshaftConfiguration> var1) {
      super(var1);
   }

   protected boolean isFeatureChunk(ChunkGenerator var1, BiomeSource var2, long var3, WorldgenRandom var5, ChunkPos var6, Biome var7, ChunkPos var8, MineshaftConfiguration var9, LevelHeightAccessor var10) {
      var5.setLargeFeatureSeed(var3, var6.x, var6.z);
      double var11 = (double)var9.probability;
      return var5.nextDouble() < var11;
   }

   public StructureFeature.StructureStartFactory<MineshaftConfiguration> getStartFactory() {
      return MineshaftFeature.MineShaftStart::new;
   }

   public static class MineShaftStart extends StructureStart<MineshaftConfiguration> {
      public MineShaftStart(StructureFeature<MineshaftConfiguration> var1, ChunkPos var2, int var3, long var4) {
         super(var1, var2, var3, var4);
      }

      public void generatePieces(RegistryAccess var1, ChunkGenerator var2, StructureManager var3, ChunkPos var4, Biome var5, MineshaftConfiguration var6, LevelHeightAccessor var7) {
         MineShaftPieces.MineShaftRoom var8 = new MineShaftPieces.MineShaftRoom(0, this.random, var4.getBlockX(2), var4.getBlockZ(2), var6.type);
         this.addPiece(var8);
         var8.addChildren(var8, this, this.random);
         if (var6.type == MineshaftFeature.Type.MESA) {
            boolean var9 = true;
            BoundingBox var10 = this.getBoundingBox();
            int var11 = var2.getSeaLevel() - var10.maxY() + var10.getYSpan() / 2 - -5;
            this.offsetPiecesVertically(var11);
         } else {
            this.moveBelowSeaLevel(var2.getSeaLevel(), var2.getMinY(), this.random, 10);
         }

      }
   }

   public static enum Type implements StringRepresentable {
      NORMAL("normal", Blocks.OAK_LOG, Blocks.OAK_PLANKS, Blocks.OAK_FENCE),
      MESA("mesa", Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_FENCE);

      public static final Codec<MineshaftFeature.Type> CODEC = StringRepresentable.fromEnum(MineshaftFeature.Type::values, MineshaftFeature.Type::byName);
      private static final Map<String, MineshaftFeature.Type> BY_NAME = (Map)Arrays.stream(values()).collect(Collectors.toMap(MineshaftFeature.Type::getName, (var0) -> {
         return var0;
      }));
      private final String name;
      private final BlockState woodState;
      private final BlockState planksState;
      private final BlockState fenceState;

      private Type(String var3, Block var4, Block var5, Block var6) {
         this.name = var3;
         this.woodState = var4.defaultBlockState();
         this.planksState = var5.defaultBlockState();
         this.fenceState = var6.defaultBlockState();
      }

      public String getName() {
         return this.name;
      }

      private static MineshaftFeature.Type byName(String var0) {
         return (MineshaftFeature.Type)BY_NAME.get(var0);
      }

      public static MineshaftFeature.Type byId(int var0) {
         return var0 >= 0 && var0 < values().length ? values()[var0] : NORMAL;
      }

      public BlockState getWoodState() {
         return this.woodState;
      }

      public BlockState getPlanksState() {
         return this.planksState;
      }

      public BlockState getFenceState() {
         return this.fenceState;
      }

      public String getSerializedName() {
         return this.name;
      }

      // $FF: synthetic method
      private static MineshaftFeature.Type[] $values() {
         return new MineshaftFeature.Type[]{NORMAL, MESA};
      }
   }
}
