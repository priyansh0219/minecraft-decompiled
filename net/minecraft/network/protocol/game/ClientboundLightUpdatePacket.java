package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class ClientboundLightUpdatePacket implements Packet<ClientGamePacketListener> {
   private final int x;
   private final int z;
   private final BitSet skyYMask;
   private final BitSet blockYMask;
   private final BitSet emptySkyYMask;
   private final BitSet emptyBlockYMask;
   private final List<byte[]> skyUpdates;
   private final List<byte[]> blockUpdates;
   private final boolean trustEdges;

   public ClientboundLightUpdatePacket(ChunkPos var1, LevelLightEngine var2, @Nullable BitSet var3, @Nullable BitSet var4, boolean var5) {
      this.x = var1.x;
      this.z = var1.z;
      this.trustEdges = var5;
      this.skyYMask = new BitSet();
      this.blockYMask = new BitSet();
      this.emptySkyYMask = new BitSet();
      this.emptyBlockYMask = new BitSet();
      this.skyUpdates = Lists.newArrayList();
      this.blockUpdates = Lists.newArrayList();

      for(int var6 = 0; var6 < var2.getLightSectionCount(); ++var6) {
         if (var3 == null || var3.get(var6)) {
            prepareSectionData(var1, var2, LightLayer.SKY, var6, this.skyYMask, this.emptySkyYMask, this.skyUpdates);
         }

         if (var4 == null || var4.get(var6)) {
            prepareSectionData(var1, var2, LightLayer.BLOCK, var6, this.blockYMask, this.emptyBlockYMask, this.blockUpdates);
         }
      }

   }

   private static void prepareSectionData(ChunkPos var0, LevelLightEngine var1, LightLayer var2, int var3, BitSet var4, BitSet var5, List<byte[]> var6) {
      DataLayer var7 = var1.getLayerListener(var2).getDataLayerData(SectionPos.of(var0, var1.getMinLightSection() + var3));
      if (var7 != null) {
         if (var7.isEmpty()) {
            var5.set(var3);
         } else {
            var4.set(var3);
            var6.add((byte[])var7.getData().clone());
         }
      }

   }

   public ClientboundLightUpdatePacket(FriendlyByteBuf var1) {
      this.x = var1.readVarInt();
      this.z = var1.readVarInt();
      this.trustEdges = var1.readBoolean();
      this.skyYMask = var1.readBitSet();
      this.blockYMask = var1.readBitSet();
      this.emptySkyYMask = var1.readBitSet();
      this.emptyBlockYMask = var1.readBitSet();
      this.skyUpdates = var1.readList((var0) -> {
         return var0.readByteArray(2048);
      });
      this.blockUpdates = var1.readList((var0) -> {
         return var0.readByteArray(2048);
      });
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeVarInt(this.x);
      var1.writeVarInt(this.z);
      var1.writeBoolean(this.trustEdges);
      var1.writeBitSet(this.skyYMask);
      var1.writeBitSet(this.blockYMask);
      var1.writeBitSet(this.emptySkyYMask);
      var1.writeBitSet(this.emptyBlockYMask);
      var1.writeCollection(this.skyUpdates, FriendlyByteBuf::writeByteArray);
      var1.writeCollection(this.blockUpdates, FriendlyByteBuf::writeByteArray);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleLightUpdatePacked(this);
   }

   public int getX() {
      return this.x;
   }

   public int getZ() {
      return this.z;
   }

   public BitSet getSkyYMask() {
      return this.skyYMask;
   }

   public BitSet getEmptySkyYMask() {
      return this.emptySkyYMask;
   }

   public List<byte[]> getSkyUpdates() {
      return this.skyUpdates;
   }

   public BitSet getBlockYMask() {
      return this.blockYMask;
   }

   public BitSet getEmptyBlockYMask() {
      return this.emptyBlockYMask;
   }

   public List<byte[]> getBlockUpdates() {
      return this.blockUpdates;
   }

   public boolean getTrustEdges() {
      return this.trustEdges;
   }
}
