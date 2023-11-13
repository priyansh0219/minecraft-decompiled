package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

public class ClientboundLevelChunkPacket implements Packet<ClientGamePacketListener> {
   public static final int TWO_MEGABYTES = 2097152;
   private final int x;
   private final int z;
   private final BitSet availableSections;
   private final CompoundTag heightmaps;
   private final int[] biomes;
   private final byte[] buffer;
   private final List<CompoundTag> blockEntitiesTags;

   public ClientboundLevelChunkPacket(LevelChunk var1) {
      ChunkPos var2 = var1.getPos();
      this.x = var2.x;
      this.z = var2.z;
      this.heightmaps = new CompoundTag();
      Iterator var3 = var1.getHeightmaps().iterator();

      Entry var4;
      while(var3.hasNext()) {
         var4 = (Entry)var3.next();
         if (((Heightmap.Types)var4.getKey()).sendToClient()) {
            this.heightmaps.put(((Heightmap.Types)var4.getKey()).getSerializationKey(), new LongArrayTag(((Heightmap)var4.getValue()).getRawData()));
         }
      }

      this.biomes = var1.getBiomes().writeBiomes();
      this.buffer = new byte[this.calculateChunkSize(var1)];
      this.availableSections = this.extractChunkData(new FriendlyByteBuf(this.getWriteBuffer()), var1);
      this.blockEntitiesTags = Lists.newArrayList();
      var3 = var1.getBlockEntities().entrySet().iterator();

      while(var3.hasNext()) {
         var4 = (Entry)var3.next();
         BlockEntity var5 = (BlockEntity)var4.getValue();
         CompoundTag var6 = var5.getUpdateTag();
         this.blockEntitiesTags.add(var6);
      }

   }

   public ClientboundLevelChunkPacket(FriendlyByteBuf var1) {
      this.x = var1.readInt();
      this.z = var1.readInt();
      this.availableSections = var1.readBitSet();
      this.heightmaps = var1.readNbt();
      if (this.heightmaps == null) {
         throw new RuntimeException("Can't read heightmap in packet for [" + this.x + ", " + this.z + "]");
      } else {
         this.biomes = var1.readVarIntArray(ChunkBiomeContainer.MAX_SIZE);
         int var2 = var1.readVarInt();
         if (var2 > 2097152) {
            throw new RuntimeException("Chunk Packet trying to allocate too much memory on read.");
         } else {
            this.buffer = new byte[var2];
            var1.readBytes(this.buffer);
            this.blockEntitiesTags = var1.readList(FriendlyByteBuf::readNbt);
         }
      }
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeInt(this.x);
      var1.writeInt(this.z);
      var1.writeBitSet(this.availableSections);
      var1.writeNbt(this.heightmaps);
      var1.writeVarIntArray(this.biomes);
      var1.writeVarInt(this.buffer.length);
      var1.writeBytes(this.buffer);
      var1.writeCollection(this.blockEntitiesTags, FriendlyByteBuf::writeNbt);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleLevelChunk(this);
   }

   public FriendlyByteBuf getReadBuffer() {
      return new FriendlyByteBuf(Unpooled.wrappedBuffer(this.buffer));
   }

   private ByteBuf getWriteBuffer() {
      ByteBuf var1 = Unpooled.wrappedBuffer(this.buffer);
      var1.writerIndex(0);
      return var1;
   }

   public BitSet extractChunkData(FriendlyByteBuf var1, LevelChunk var2) {
      BitSet var3 = new BitSet();
      LevelChunkSection[] var4 = var2.getSections();
      int var5 = 0;

      for(int var6 = var4.length; var5 < var6; ++var5) {
         LevelChunkSection var7 = var4[var5];
         if (var7 != LevelChunk.EMPTY_SECTION && !var7.isEmpty()) {
            var3.set(var5);
            var7.write(var1);
         }
      }

      return var3;
   }

   protected int calculateChunkSize(LevelChunk var1) {
      int var2 = 0;
      LevelChunkSection[] var3 = var1.getSections();
      int var4 = 0;

      for(int var5 = var3.length; var4 < var5; ++var4) {
         LevelChunkSection var6 = var3[var4];
         if (var6 != LevelChunk.EMPTY_SECTION && !var6.isEmpty()) {
            var2 += var6.getSerializedSize();
         }
      }

      return var2;
   }

   public int getX() {
      return this.x;
   }

   public int getZ() {
      return this.z;
   }

   public BitSet getAvailableSections() {
      return this.availableSections;
   }

   public CompoundTag getHeightmaps() {
      return this.heightmaps;
   }

   public List<CompoundTag> getBlockEntitiesTags() {
      return this.blockEntitiesTags;
   }

   public int[] getBiomes() {
      return this.biomes;
   }
}
