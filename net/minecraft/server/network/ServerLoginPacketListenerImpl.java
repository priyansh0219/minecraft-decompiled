package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;
import net.minecraft.network.protocol.login.ServerboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerLoginPacketListenerImpl implements ServerLoginPacketListener {
   private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
   static final Logger LOGGER = LogManager.getLogger();
   private static final int MAX_TICKS_BEFORE_LOGIN = 600;
   private static final Random RANDOM = new Random();
   private final byte[] nonce = new byte[4];
   final MinecraftServer server;
   public final Connection connection;
   ServerLoginPacketListenerImpl.State state;
   private int tick;
   @Nullable
   GameProfile gameProfile;
   private final String serverId;
   @Nullable
   private ServerPlayer delayedAcceptPlayer;

   public ServerLoginPacketListenerImpl(MinecraftServer var1, Connection var2) {
      this.state = ServerLoginPacketListenerImpl.State.HELLO;
      this.serverId = "";
      this.server = var1;
      this.connection = var2;
      RANDOM.nextBytes(this.nonce);
   }

   public void tick() {
      if (this.state == ServerLoginPacketListenerImpl.State.READY_TO_ACCEPT) {
         this.handleAcceptedLogin();
      } else if (this.state == ServerLoginPacketListenerImpl.State.DELAY_ACCEPT) {
         ServerPlayer var1 = this.server.getPlayerList().getPlayer(this.gameProfile.getId());
         if (var1 == null) {
            this.state = ServerLoginPacketListenerImpl.State.READY_TO_ACCEPT;
            this.placeNewPlayer(this.delayedAcceptPlayer);
            this.delayedAcceptPlayer = null;
         }
      }

      if (this.tick++ == 600) {
         this.disconnect(new TranslatableComponent("multiplayer.disconnect.slow_login"));
      }

   }

   public Connection getConnection() {
      return this.connection;
   }

   public void disconnect(Component var1) {
      try {
         LOGGER.info("Disconnecting {}: {}", this.getUserName(), var1.getString());
         this.connection.send(new ClientboundLoginDisconnectPacket(var1));
         this.connection.disconnect(var1);
      } catch (Exception var3) {
         LOGGER.error("Error whilst disconnecting player", var3);
      }

   }

   public void handleAcceptedLogin() {
      if (!this.gameProfile.isComplete()) {
         this.gameProfile = this.createFakeProfile(this.gameProfile);
      }

      Component var1 = this.server.getPlayerList().canPlayerLogin(this.connection.getRemoteAddress(), this.gameProfile);
      if (var1 != null) {
         this.disconnect(var1);
      } else {
         this.state = ServerLoginPacketListenerImpl.State.ACCEPTED;
         if (this.server.getCompressionThreshold() >= 0 && !this.connection.isMemoryConnection()) {
            this.connection.send(new ClientboundLoginCompressionPacket(this.server.getCompressionThreshold()), (var1x) -> {
               this.connection.setupCompression(this.server.getCompressionThreshold());
            });
         }

         this.connection.send(new ClientboundGameProfilePacket(this.gameProfile));
         ServerPlayer var2 = this.server.getPlayerList().getPlayer(this.gameProfile.getId());

         try {
            ServerPlayer var3 = this.server.getPlayerList().getPlayerForLogin(this.gameProfile);
            if (var2 != null) {
               this.state = ServerLoginPacketListenerImpl.State.DELAY_ACCEPT;
               this.delayedAcceptPlayer = var3;
            } else {
               this.placeNewPlayer(var3);
            }
         } catch (Exception var5) {
            TranslatableComponent var4 = new TranslatableComponent("multiplayer.disconnect.invalid_player_data");
            this.connection.send(new ClientboundDisconnectPacket(var4));
            this.connection.disconnect(var4);
         }
      }

   }

   private void placeNewPlayer(ServerPlayer var1) {
      this.server.getPlayerList().placeNewPlayer(this.connection, var1);
   }

   public void onDisconnect(Component var1) {
      LOGGER.info("{} lost connection: {}", this.getUserName(), var1.getString());
   }

   public String getUserName() {
      if (this.gameProfile != null) {
         GameProfile var10000 = this.gameProfile;
         return var10000 + " (" + this.connection.getRemoteAddress() + ")";
      } else {
         return String.valueOf(this.connection.getRemoteAddress());
      }
   }

   public void handleHello(ServerboundHelloPacket var1) {
      Validate.validState(this.state == ServerLoginPacketListenerImpl.State.HELLO, "Unexpected hello packet", new Object[0]);
      this.gameProfile = var1.getGameProfile();
      if (this.server.usesAuthentication() && !this.connection.isMemoryConnection()) {
         this.state = ServerLoginPacketListenerImpl.State.KEY;
         this.connection.send(new ClientboundHelloPacket("", this.server.getKeyPair().getPublic().getEncoded(), this.nonce));
      } else {
         this.state = ServerLoginPacketListenerImpl.State.READY_TO_ACCEPT;
      }

   }

   public void handleKey(ServerboundKeyPacket var1) {
      Validate.validState(this.state == ServerLoginPacketListenerImpl.State.KEY, "Unexpected key packet", new Object[0]);
      PrivateKey var2 = this.server.getKeyPair().getPrivate();

      final String var3;
      try {
         if (!Arrays.equals(this.nonce, var1.getNonce(var2))) {
            throw new IllegalStateException("Protocol error");
         }

         SecretKey var4 = var1.getSecretKey(var2);
         Cipher var5 = Crypt.getCipher(2, var4);
         Cipher var6 = Crypt.getCipher(1, var4);
         var3 = (new BigInteger(Crypt.digestData("", this.server.getKeyPair().getPublic(), var4))).toString(16);
         this.state = ServerLoginPacketListenerImpl.State.AUTHENTICATING;
         this.connection.setEncryptionKey(var5, var6);
      } catch (CryptException var7) {
         throw new IllegalStateException("Protocol error", var7);
      }

      Thread var8 = new Thread("User Authenticator #" + UNIQUE_THREAD_ID.incrementAndGet()) {
         public void run() {
            GameProfile var1 = ServerLoginPacketListenerImpl.this.gameProfile;

            try {
               ServerLoginPacketListenerImpl.this.gameProfile = ServerLoginPacketListenerImpl.this.server.getSessionService().hasJoinedServer(new GameProfile((UUID)null, var1.getName()), var3, this.getAddress());
               if (ServerLoginPacketListenerImpl.this.gameProfile != null) {
                  ServerLoginPacketListenerImpl.LOGGER.info("UUID of player {} is {}", ServerLoginPacketListenerImpl.this.gameProfile.getName(), ServerLoginPacketListenerImpl.this.gameProfile.getId());
                  ServerLoginPacketListenerImpl.this.state = ServerLoginPacketListenerImpl.State.READY_TO_ACCEPT;
               } else if (ServerLoginPacketListenerImpl.this.server.isSingleplayer()) {
                  ServerLoginPacketListenerImpl.LOGGER.warn("Failed to verify username but will let them in anyway!");
                  ServerLoginPacketListenerImpl.this.gameProfile = ServerLoginPacketListenerImpl.this.createFakeProfile(var1);
                  ServerLoginPacketListenerImpl.this.state = ServerLoginPacketListenerImpl.State.READY_TO_ACCEPT;
               } else {
                  ServerLoginPacketListenerImpl.this.disconnect(new TranslatableComponent("multiplayer.disconnect.unverified_username"));
                  ServerLoginPacketListenerImpl.LOGGER.error("Username '{}' tried to join with an invalid session", var1.getName());
               }
            } catch (AuthenticationUnavailableException var3x) {
               if (ServerLoginPacketListenerImpl.this.server.isSingleplayer()) {
                  ServerLoginPacketListenerImpl.LOGGER.warn("Authentication servers are down but will let them in anyway!");
                  ServerLoginPacketListenerImpl.this.gameProfile = ServerLoginPacketListenerImpl.this.createFakeProfile(var1);
                  ServerLoginPacketListenerImpl.this.state = ServerLoginPacketListenerImpl.State.READY_TO_ACCEPT;
               } else {
                  ServerLoginPacketListenerImpl.this.disconnect(new TranslatableComponent("multiplayer.disconnect.authservers_down"));
                  ServerLoginPacketListenerImpl.LOGGER.error("Couldn't verify username because servers are unavailable");
               }
            }

         }

         @Nullable
         private InetAddress getAddress() {
            SocketAddress var1 = ServerLoginPacketListenerImpl.this.connection.getRemoteAddress();
            return ServerLoginPacketListenerImpl.this.server.getPreventProxyConnections() && var1 instanceof InetSocketAddress ? ((InetSocketAddress)var1).getAddress() : null;
         }
      };
      var8.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
      var8.start();
   }

   public void handleCustomQueryPacket(ServerboundCustomQueryPacket var1) {
      this.disconnect(new TranslatableComponent("multiplayer.disconnect.unexpected_query_response"));
   }

   protected GameProfile createFakeProfile(GameProfile var1) {
      UUID var2 = Player.createPlayerUUID(var1.getName());
      return new GameProfile(var2, var1.getName());
   }

   private static enum State {
      HELLO,
      KEY,
      AUTHENTICATING,
      NEGOTIATING,
      READY_TO_ACCEPT,
      DELAY_ACCEPT,
      ACCEPTED;

      // $FF: synthetic method
      private static ServerLoginPacketListenerImpl.State[] $values() {
         return new ServerLoginPacketListenerImpl.State[]{HELLO, KEY, AUTHENTICATING, NEGOTIATING, READY_TO_ACCEPT, DELAY_ACCEPT, ACCEPTED};
      }
   }
}
