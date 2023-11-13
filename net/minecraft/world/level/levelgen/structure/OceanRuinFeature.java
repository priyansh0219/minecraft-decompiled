package net.minecraft.world.level.levelgen.structure;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OceanRuinConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class OceanRuinFeature extends StructureFeature<OceanRuinConfiguration> {
   public OceanRuinFeature(Codec<OceanRuinConfiguration> var1) {
      super(var1);
   }

   public StructureFeature.StructureStartFactory<OceanRuinConfiguration> getStartFactory() {
      return OceanRuinFeature.OceanRuinStart::new;
   }

   public static enum Type implements StringRepresentable {
      WARM("warm"),
      COLD("cold");

      public static final Codec<OceanRuinFeature.Type> CODEC = StringRepresentable.fromEnum(OceanRuinFeature.Type::values, OceanRuinFeature.Type::byName);
      private static final Map<String, OceanRuinFeature.Type> BY_NAME = (Map)Arrays.stream(values()).collect(Collectors.toMap(OceanRuinFeature.Type::getName, (var0) -> {
         return var0;
      }));
      private final String name;

      private Type(String var3) {
         this.name = var3;
      }

      public String getName() {
         return this.name;
      }

      @Nullable
      public static OceanRuinFeature.Type byName(String var0) {
         return (OceanRuinFeature.Type)BY_NAME.get(var0);
      }

      public String getSerializedName() {
         return this.name;
      }

      // $FF: synthetic method
      private static OceanRuinFeature.Type[] $values() {
         return new OceanRuinFeature.Type[]{WARM, COLD};
      }
   }

   public static class OceanRuinStart extends StructureStart<OceanRuinConfiguration> {
      public OceanRuinStart(StructureFeature<OceanRuinConfiguration> var1, ChunkPos var2, int var3, long var4) {
         super(var1, var2, var3, var4);
      }

      public void generatePieces(RegistryAccess var1, ChunkGenerator var2, StructureManager var3, ChunkPos var4, Biome var5, OceanRuinConfiguration var6, LevelHeightAccessor var7) {
         BlockPos var8 = new BlockPos(var4.getMinBlockX(), 90, var4.getMinBlockZ());
         Rotation var9 = Rotation.getRandom(this.random);
         OceanRuinPieces.addPieces(var3, var8, var9, this, this.random, var6);
      }
   }
}
