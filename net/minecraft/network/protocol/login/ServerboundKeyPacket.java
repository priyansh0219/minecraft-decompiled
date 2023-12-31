package net.minecraft.network.protocol.login;

import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;

public class ServerboundKeyPacket implements Packet<ServerLoginPacketListener> {
   private final byte[] keybytes;
   private final byte[] nonce;

   public ServerboundKeyPacket(SecretKey var1, PublicKey var2, byte[] var3) throws CryptException {
      this.keybytes = Crypt.encryptUsingKey(var2, var1.getEncoded());
      this.nonce = Crypt.encryptUsingKey(var2, var3);
   }

   public ServerboundKeyPacket(FriendlyByteBuf var1) {
      this.keybytes = var1.readByteArray();
      this.nonce = var1.readByteArray();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeByteArray(this.keybytes);
      var1.writeByteArray(this.nonce);
   }

   public void handle(ServerLoginPacketListener var1) {
      var1.handleKey(this);
   }

   public SecretKey getSecretKey(PrivateKey var1) throws CryptException {
      return Crypt.decryptByteToSecretKey(var1, this.keybytes);
   }

   public byte[] getNonce(PrivateKey var1) throws CryptException {
      return Crypt.decryptUsingKey(var1, this.nonce);
   }
}
