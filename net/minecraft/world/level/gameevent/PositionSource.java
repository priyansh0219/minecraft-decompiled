package net.minecraft.world.level.gameevent;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;

public interface PositionSource {
   Codec<PositionSource> CODEC = Registry.POSITION_SOURCE_TYPE.dispatch(PositionSource::getType, PositionSourceType::codec);

   Optional<BlockPos> getPosition(Level var1);

   PositionSourceType<?> getType();
}
