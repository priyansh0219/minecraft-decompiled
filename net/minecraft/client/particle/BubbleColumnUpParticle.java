package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;

public class BubbleColumnUpParticle extends TextureSheetParticle {
   BubbleColumnUpParticle(ClientLevel var1, double var2, double var4, double var6, double var8, double var10, double var12) {
      super(var1, var2, var4, var6);
      this.gravity = -0.125F;
      this.friction = 0.85F;
      this.setSize(0.02F, 0.02F);
      this.quadSize *= this.random.nextFloat() * 0.6F + 0.2F;
      this.xd = var8 * 0.20000000298023224D + (Math.random() * 2.0D - 1.0D) * 0.019999999552965164D;
      this.yd = var10 * 0.20000000298023224D + (Math.random() * 2.0D - 1.0D) * 0.019999999552965164D;
      this.zd = var12 * 0.20000000298023224D + (Math.random() * 2.0D - 1.0D) * 0.019999999552965164D;
      this.lifetime = (int)(40.0D / (Math.random() * 0.8D + 0.2D));
   }

   public void tick() {
      super.tick();
      if (!this.removed && !this.level.getFluidState(new BlockPos(this.x, this.y, this.z)).is(FluidTags.WATER)) {
         this.remove();
      }

   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet var1) {
         this.sprite = var1;
      }

      public Particle createParticle(SimpleParticleType var1, ClientLevel var2, double var3, double var5, double var7, double var9, double var11, double var13) {
         BubbleColumnUpParticle var15 = new BubbleColumnUpParticle(var2, var3, var5, var7, var9, var11, var13);
         var15.pickSprite(this.sprite);
         return var15;
      }
   }
}
