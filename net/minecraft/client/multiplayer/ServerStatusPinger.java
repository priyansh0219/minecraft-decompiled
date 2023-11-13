package net.minecraft.client.multiplayer;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.network.protocol.status.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerStatusPinger {
   static final Splitter SPLITTER = Splitter.on('\u0000').limit(6);
   static final Logger LOGGER = LogManager.getLogger();
   private static final Component CANT_CONNECT_MESSAGE;
   private final List<Connection> connections = Collections.synchronizedList(Lists.newArrayList());

   public void pingServer(final ServerData var1, final Runnable var2) throws UnknownHostException {
      ServerAddress var3 = ServerAddress.parseString(var1.ip);
      Optional var4 = ServerNameResolver.DEFAULT.resolveAddress(var3).map(ResolvedServerAddress::asInetSocketAddress);
      if (!var4.isPresent()) {
         this.onPingFailed(ConnectScreen.UNKNOWN_HOST_MESSAGE, var1);
      } else {
         final InetSocketAddress var5 = (InetSocketAddress)var4.get();
         final Connection var6 = Connection.connectToServer(var5, false);
         this.connections.add(var6);
         var1.motd = new TranslatableComponent("multiplayer.status.pinging");
         var1.ping = -1L;
         var1.playerList = null;
         var6.setListener(new ClientStatusPacketListener() {
            private boolean success;
            private boolean receivedPing;
            private long pingStart;

            public void handleStatusResponse(ClientboundStatusResponsePacket var1x) {
               if (this.receivedPing) {
                  var6.disconnect(new TranslatableComponent("multiplayer.status.unrequested"));
               } else {
                  this.receivedPing = true;
                  ServerStatus var2x = var1x.getStatus();
                  if (var2x.getDescription() != null) {
                     var1.motd = var2x.getDescription();
                  } else {
                     var1.motd = TextComponent.EMPTY;
                  }

                  if (var2x.getVersion() != null) {
                     var1.version = new TextComponent(var2x.getVersion().getName());
                     var1.protocol = var2x.getVersion().getProtocol();
                  } else {
                     var1.version = new TranslatableComponent("multiplayer.status.old");
                     var1.protocol = 0;
                  }

                  if (var2x.getPlayers() != null) {
                     var1.status = ServerStatusPinger.formatPlayerCount(var2x.getPlayers().getNumPlayers(), var2x.getPlayers().getMaxPlayers());
                     ArrayList var3 = Lists.newArrayList();
                     if (ArrayUtils.isNotEmpty(var2x.getPlayers().getSample())) {
                        GameProfile[] var4 = var2x.getPlayers().getSample();
                        int var5x = var4.length;

                        for(int var6x = 0; var6x < var5x; ++var6x) {
                           GameProfile var7 = var4[var6x];
                           var3.add(new TextComponent(var7.getName()));
                        }

                        if (var2x.getPlayers().getSample().length < var2x.getPlayers().getNumPlayers()) {
                           var3.add(new TranslatableComponent("multiplayer.status.and_more", new Object[]{var2x.getPlayers().getNumPlayers() - var2x.getPlayers().getSample().length}));
                        }

                        var1.playerList = var3;
                     }
                  } else {
                     var1.status = (new TranslatableComponent("multiplayer.status.unknown")).withStyle(ChatFormatting.DARK_GRAY);
                  }

                  String var8 = null;
                  if (var2x.getFavicon() != null) {
                     String var9 = var2x.getFavicon();
                     if (var9.startsWith("data:image/png;base64,")) {
                        var8 = var9.substring("data:image/png;base64,".length());
                     } else {
                        ServerStatusPinger.LOGGER.error("Invalid server icon (unknown format)");
                     }
                  }

                  if (!Objects.equals(var8, var1.getIconB64())) {
                     var1.setIconB64(var8);
                     var2.run();
                  }

                  this.pingStart = Util.getMillis();
                  var6.send(new ServerboundPingRequestPacket(this.pingStart));
                  this.success = true;
               }
            }

            public void handlePongResponse(ClientboundPongResponsePacket var1x) {
               long var2x = this.pingStart;
               long var4 = Util.getMillis();
               var1.ping = var4 - var2x;
               var6.disconnect(new TranslatableComponent("multiplayer.status.finished"));
            }

            public void onDisconnect(Component var1x) {
               if (!this.success) {
                  ServerStatusPinger.this.onPingFailed(var1x, var1);
                  ServerStatusPinger.this.pingLegacyServer(var5, var1);
               }

            }

            public Connection getConnection() {
               return var6;
            }
         });

         try {
            var6.send(new ClientIntentionPacket(var3.getHost(), var3.getPort(), ConnectionProtocol.STATUS));
            var6.send(new ServerboundStatusRequestPacket());
         } catch (Throwable var8) {
            LOGGER.error(var8);
         }

      }
   }

   void onPingFailed(Component var1, ServerData var2) {
      LOGGER.error("Can't ping {}: {}", var2.ip, var1.getString());
      var2.motd = CANT_CONNECT_MESSAGE;
      var2.status = TextComponent.EMPTY;
   }

   void pingLegacyServer(final InetSocketAddress var1, final ServerData var2) {
      ((Bootstrap)((Bootstrap)((Bootstrap)(new Bootstrap()).group((EventLoopGroup)Connection.NETWORK_WORKER_GROUP.get())).handler(new ChannelInitializer<Channel>() {
         protected void initChannel(Channel var1x) {
            try {
               var1x.config().setOption(ChannelOption.TCP_NODELAY, true);
            } catch (ChannelException var3) {
            }

            var1x.pipeline().addLast(new ChannelHandler[]{new SimpleChannelInboundHandler<ByteBuf>() {
               // $FF: synthetic field
               final <undefinedtype> this$1;

               {
                  this.this$1 = var1;
               }

               public void channelActive(ChannelHandlerContext var1) throws Exception {
                  super.channelActive(var1);
                  ByteBuf var2 = Unpooled.buffer();

                  try {
                     var2.writeByte(254);
                     var2.writeByte(1);
                     var2.writeByte(250);
                     char[] var3 = "MC|PingHost".toCharArray();
                     var2.writeShort(var3.length);
                     char[] var4 = var3;
                     int var5 = var3.length;

                     int var6;
                     char var7;
                     for(var6 = 0; var6 < var5; ++var6) {
                        var7 = var4[var6];
                        var2.writeChar(var7);
                     }

                     var2.writeShort(7 + 2 * this.this$1.val$address.getHostName().length());
                     var2.writeByte(127);
                     var3 = this.this$1.val$address.getHostName().toCharArray();
                     var2.writeShort(var3.length);
                     var4 = var3;
                     var5 = var3.length;

                     for(var6 = 0; var6 < var5; ++var6) {
                        var7 = var4[var6];
                        var2.writeChar(var7);
                     }

                     var2.writeInt(this.this$1.val$address.getPort());
                     var1.channel().writeAndFlush(var2).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                  } finally {
                     var2.release();
                  }
               }

               protected void channelRead0(ChannelHandlerContext var1, ByteBuf var2) {
                  short var3 = var2.readUnsignedByte();
                  if (var3 == 255) {
                     String var4 = new String(var2.readBytes(var2.readShort() * 2).array(), StandardCharsets.UTF_16BE);
                     String[] var5 = (String[])Iterables.toArray(ServerStatusPinger.SPLITTER.split(var4), String.class);
                     if ("§1".equals(var5[0])) {
                        int var6 = Mth.getInt(var5[1], 0);
                        String var7 = var5[2];
                        String var8 = var5[3];
                        int var9 = Mth.getInt(var5[4], -1);
                        int var10 = Mth.getInt(var5[5], -1);
                        this.this$1.val$data.protocol = -1;
                        this.this$1.val$data.version = new TextComponent(var7);
                        this.this$1.val$data.motd = new TextComponent(var8);
                        this.this$1.val$data.status = ServerStatusPinger.formatPlayerCount(var9, var10);
                     }
                  }

                  var1.close();
               }

               public void exceptionCaught(ChannelHandlerContext var1, Throwable var2) {
                  var1.close();
               }

               // $FF: synthetic method
               protected void channelRead0(ChannelHandlerContext var1, Object var2) throws Exception {
                  this.channelRead0(var1, (ByteBuf)var2);
               }
            }});
         }
      })).channel(NioSocketChannel.class)).connect(var1.getAddress(), var1.getPort());
   }

   static Component formatPlayerCount(int var0, int var1) {
      return (new TextComponent(Integer.toString(var0))).append((new TextComponent("/")).withStyle(ChatFormatting.DARK_GRAY)).append(Integer.toString(var1)).withStyle(ChatFormatting.GRAY);
   }

   public void tick() {
      synchronized(this.connections) {
         Iterator var2 = this.connections.iterator();

         while(var2.hasNext()) {
            Connection var3 = (Connection)var2.next();
            if (var3.isConnected()) {
               var3.tick();
            } else {
               var2.remove();
               var3.handleDisconnection();
            }
         }

      }
   }

   public void removeAll() {
      synchronized(this.connections) {
         Iterator var2 = this.connections.iterator();

         while(var2.hasNext()) {
            Connection var3 = (Connection)var2.next();
            if (var3.isConnected()) {
               var2.remove();
               var3.disconnect(new TranslatableComponent("multiplayer.status.cancelled"));
            }
         }

      }
   }

   static {
      CANT_CONNECT_MESSAGE = (new TranslatableComponent("multiplayer.status.cannot_connect")).withStyle(ChatFormatting.DARK_RED);
   }
}
