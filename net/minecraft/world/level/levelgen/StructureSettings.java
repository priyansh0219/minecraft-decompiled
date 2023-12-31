package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StrongholdConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;

public class StructureSettings {
   public static final Codec<StructureSettings> CODEC = RecordCodecBuilder.create((var0x) -> {
      return var0x.group(StrongholdConfiguration.CODEC.optionalFieldOf("stronghold").forGetter((var0xx) -> {
         return Optional.ofNullable(var0xx.stronghold);
      }), Codec.simpleMap(Registry.STRUCTURE_FEATURE, StructureFeatureConfiguration.CODEC, Registry.STRUCTURE_FEATURE).fieldOf("structures").forGetter((var0xx) -> {
         return var0xx.structureConfig;
      })).apply(var0x, StructureSettings::new);
   });
   public static final ImmutableMap<StructureFeature<?>, StructureFeatureConfiguration> DEFAULTS;
   public static final StrongholdConfiguration DEFAULT_STRONGHOLD;
   private final Map<StructureFeature<?>, StructureFeatureConfiguration> structureConfig;
   @Nullable
   private final StrongholdConfiguration stronghold;

   public StructureSettings(Optional<StrongholdConfiguration> var1, Map<StructureFeature<?>, StructureFeatureConfiguration> var2) {
      this.stronghold = (StrongholdConfiguration)var1.orElse((Object)null);
      this.structureConfig = var2;
   }

   public StructureSettings(boolean var1) {
      this.structureConfig = Maps.newHashMap(DEFAULTS);
      this.stronghold = var1 ? DEFAULT_STRONGHOLD : null;
   }

   public Map<StructureFeature<?>, StructureFeatureConfiguration> structureConfig() {
      return this.structureConfig;
   }

   @Nullable
   public StructureFeatureConfiguration getConfig(StructureFeature<?> var1) {
      return (StructureFeatureConfiguration)this.structureConfig.get(var1);
   }

   @Nullable
   public StrongholdConfiguration stronghold() {
      return this.stronghold;
   }

   static {
      DEFAULTS = ImmutableMap.builder().put(StructureFeature.VILLAGE, new StructureFeatureConfiguration(32, 8, 10387312)).put(StructureFeature.DESERT_PYRAMID, new StructureFeatureConfiguration(32, 8, 14357617)).put(StructureFeature.IGLOO, new StructureFeatureConfiguration(32, 8, 14357618)).put(StructureFeature.JUNGLE_TEMPLE, new StructureFeatureConfiguration(32, 8, 14357619)).put(StructureFeature.SWAMP_HUT, new StructureFeatureConfiguration(32, 8, 14357620)).put(StructureFeature.PILLAGER_OUTPOST, new StructureFeatureConfiguration(32, 8, 165745296)).put(StructureFeature.STRONGHOLD, new StructureFeatureConfiguration(1, 0, 0)).put(StructureFeature.OCEAN_MONUMENT, new StructureFeatureConfiguration(32, 5, 10387313)).put(StructureFeature.END_CITY, new StructureFeatureConfiguration(20, 11, 10387313)).put(StructureFeature.WOODLAND_MANSION, new StructureFeatureConfiguration(80, 20, 10387319)).put(StructureFeature.BURIED_TREASURE, new StructureFeatureConfiguration(1, 0, 0)).put(StructureFeature.MINESHAFT, new StructureFeatureConfiguration(1, 0, 0)).put(StructureFeature.RUINED_PORTAL, new StructureFeatureConfiguration(40, 15, 34222645)).put(StructureFeature.SHIPWRECK, new StructureFeatureConfiguration(24, 4, 165745295)).put(StructureFeature.OCEAN_RUIN, new StructureFeatureConfiguration(20, 8, 14357621)).put(StructureFeature.BASTION_REMNANT, new StructureFeatureConfiguration(27, 4, 30084232)).put(StructureFeature.NETHER_BRIDGE, new StructureFeatureConfiguration(27, 4, 30084232)).put(StructureFeature.NETHER_FOSSIL, new StructureFeatureConfiguration(2, 1, 14357921)).build();
      Iterator var0 = Registry.STRUCTURE_FEATURE.iterator();

      StructureFeature var1;
      do {
         if (!var0.hasNext()) {
            DEFAULT_STRONGHOLD = new StrongholdConfiguration(32, 3, 128);
            return;
         }

         var1 = (StructureFeature)var0.next();
      } while(DEFAULTS.containsKey(var1));

      throw new IllegalStateException("Structure feature without default settings: " + Registry.STRUCTURE_FEATURE.getKey(var1));
   }
}
