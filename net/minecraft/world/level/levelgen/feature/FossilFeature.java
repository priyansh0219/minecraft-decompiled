package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.mutable.MutableInt;

public class FossilFeature extends Feature<FossilFeatureConfiguration> {
   public FossilFeature(Codec<FossilFeatureConfiguration> var1) {
      super(var1);
   }

   public boolean place(FeaturePlaceContext<FossilFeatureConfiguration> var1) {
      Random var2 = var1.random();
      WorldGenLevel var3 = var1.level();
      BlockPos var4 = var1.origin();
      Rotation var5 = Rotation.getRandom(var2);
      FossilFeatureConfiguration var6 = (FossilFeatureConfiguration)var1.config();
      int var7 = var2.nextInt(var6.fossilStructures.size());
      StructureManager var8 = var3.getLevel().getServer().getStructureManager();
      StructureTemplate var9 = var8.getOrCreate((ResourceLocation)var6.fossilStructures.get(var7));
      StructureTemplate var10 = var8.getOrCreate((ResourceLocation)var6.overlayStructures.get(var7));
      ChunkPos var11 = new ChunkPos(var4);
      BoundingBox var12 = new BoundingBox(var11.getMinBlockX(), var3.getMinBuildHeight(), var11.getMinBlockZ(), var11.getMaxBlockX(), var3.getMaxBuildHeight(), var11.getMaxBlockZ());
      StructurePlaceSettings var13 = (new StructurePlaceSettings()).setRotation(var5).setBoundingBox(var12).setRandom(var2);
      Vec3i var14 = var9.getSize(var5);
      int var15 = var2.nextInt(16 - var14.getX());
      int var16 = var2.nextInt(16 - var14.getZ());
      int var17 = var3.getMaxBuildHeight();

      int var18;
      for(var18 = 0; var18 < var14.getX(); ++var18) {
         for(int var19 = 0; var19 < var14.getZ(); ++var19) {
            var17 = Math.min(var17, var3.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, var4.getX() + var18 + var15, var4.getZ() + var19 + var16));
         }
      }

      var18 = Mth.clamp(var4.getY(), var3.getMinBuildHeight(), var17 - 15);
      BlockPos var20 = var9.getZeroPositionWithTransform(var4.offset(var15, 0, var16).atY(var18), Mirror.NONE, var5);
      if (countEmptyCorners(var3, var9.getBoundingBox(var13, var20)) > var6.maxEmptyCornersAllowed) {
         return false;
      } else {
         var13.clearProcessors();
         ((StructureProcessorList)var6.fossilProcessors.get()).list().forEach((var1x) -> {
            var13.addProcessor(var1x);
         });
         var9.placeInWorld(var3, var20, var20, var13, var2, 4);
         var13.clearProcessors();
         ((StructureProcessorList)var6.overlayProcessors.get()).list().forEach((var1x) -> {
            var13.addProcessor(var1x);
         });
         var10.placeInWorld(var3, var20, var20, var13, var2, 4);
         return true;
      }
   }

   private static int countEmptyCorners(WorldGenLevel var0, BoundingBox var1) {
      MutableInt var2 = new MutableInt(0);
      var1.forAllCorners((var2x) -> {
         BlockState var3 = var0.getBlockState(var2x);
         if (var3.isAir() || var3.is(Blocks.LAVA) || var3.is(Blocks.WATER)) {
            var2.add(1);
         }

      });
      return var2.getValue();
   }
}
