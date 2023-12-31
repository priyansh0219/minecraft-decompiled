package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.io.IOException;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class PacketEncoder extends MessageToByteEncoder<Packet<?>> {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker MARKER;
   private final PacketFlow flow;

   public PacketEncoder(PacketFlow var1) {
      this.flow = var1;
   }

   protected void encode(ChannelHandlerContext var1, Packet<?> var2, ByteBuf var3) throws Exception {
      ConnectionProtocol var4 = (ConnectionProtocol)var1.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get();
      if (var4 == null) {
         throw new RuntimeException("ConnectionProtocol unknown: " + var2);
      } else {
         Integer var5 = var4.getPacketId(this.flow, var2);
         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(MARKER, "OUT: [{}:{}] {}", var1.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get(), var5, var2.getClass().getName());
         }

         if (var5 == null) {
            throw new IOException("Can't serialize unregistered packet");
         } else {
            FriendlyByteBuf var6 = new FriendlyByteBuf(var3);
            var6.writeVarInt(var5);

            try {
               int var7 = var6.writerIndex();
               var2.write(var6);
               int var8 = var6.writerIndex() - var7;
               if (var8 > 2097152) {
                  throw new IllegalArgumentException("Packet too big (is " + var8 + ", should be less than 2097152): " + var2);
               }
            } catch (Throwable var9) {
               LOGGER.error(var9);
               if (var2.isSkippable()) {
                  throw new SkipPacketException(var9);
               } else {
                  throw var9;
               }
            }
         }
      }
   }

   // $FF: synthetic method
   protected void encode(ChannelHandlerContext var1, Object var2, ByteBuf var3) throws Exception {
      this.encode(var1, (Packet)var2, var3);
   }

   static {
      MARKER = MarkerManager.getMarker("PACKET_SENT", Connection.PACKET_MARKER);
   }
}
