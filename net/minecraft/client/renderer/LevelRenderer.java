package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3d;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Option;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LevelRenderer implements ResourceManagerReloadListener, AutoCloseable {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final int CHUNK_SIZE = 16;
   public static final int MAX_CHUNKS_WIDTH = 66;
   public static final int MAX_CHUNKS_AREA = 4356;
   private static final float SKY_DISC_RADIUS = 512.0F;
   private static final int MIN_FOG_DISTANCE = 32;
   private static final int RAIN_RADIUS = 10;
   private static final int RAIN_DIAMETER = 21;
   private static final int TRANSPARENT_SORT_COUNT = 15;
   private static final ResourceLocation MOON_LOCATION = new ResourceLocation("textures/environment/moon_phases.png");
   private static final ResourceLocation SUN_LOCATION = new ResourceLocation("textures/environment/sun.png");
   private static final ResourceLocation CLOUDS_LOCATION = new ResourceLocation("textures/environment/clouds.png");
   private static final ResourceLocation END_SKY_LOCATION = new ResourceLocation("textures/environment/end_sky.png");
   private static final ResourceLocation FORCEFIELD_LOCATION = new ResourceLocation("textures/misc/forcefield.png");
   private static final ResourceLocation RAIN_LOCATION = new ResourceLocation("textures/environment/rain.png");
   private static final ResourceLocation SNOW_LOCATION = new ResourceLocation("textures/environment/snow.png");
   public static final Direction[] DIRECTIONS = Direction.values();
   private final Minecraft minecraft;
   private final TextureManager textureManager;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   private final RenderBuffers renderBuffers;
   private ClientLevel level;
   private Set<ChunkRenderDispatcher.RenderChunk> chunksToCompile = Sets.newLinkedHashSet();
   private final ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunks = new ObjectArrayList();
   private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
   private ViewArea viewArea;
   private LevelRenderer.RenderInfoMap renderInfoMap;
   @Nullable
   private VertexBuffer starBuffer;
   @Nullable
   private VertexBuffer skyBuffer;
   @Nullable
   private VertexBuffer darkBuffer;
   private boolean generateClouds = true;
   @Nullable
   private VertexBuffer cloudBuffer;
   private final RunningTrimmedMean frameTimes = new RunningTrimmedMean(100);
   private int ticks;
   private final Int2ObjectMap<BlockDestructionProgress> destroyingBlocks = new Int2ObjectOpenHashMap();
   private final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress = new Long2ObjectOpenHashMap();
   private final Map<BlockPos, SoundInstance> playingRecords = Maps.newHashMap();
   @Nullable
   private RenderTarget entityTarget;
   @Nullable
   private PostChain entityEffect;
   @Nullable
   private RenderTarget translucentTarget;
   @Nullable
   private RenderTarget itemEntityTarget;
   @Nullable
   private RenderTarget particlesTarget;
   @Nullable
   private RenderTarget weatherTarget;
   @Nullable
   private RenderTarget cloudsTarget;
   @Nullable
   private PostChain transparencyChain;
   private double lastCameraX = Double.MIN_VALUE;
   private double lastCameraY = Double.MIN_VALUE;
   private double lastCameraZ = Double.MIN_VALUE;
   private int lastCameraChunkX = Integer.MIN_VALUE;
   private int lastCameraChunkY = Integer.MIN_VALUE;
   private int lastCameraChunkZ = Integer.MIN_VALUE;
   private double prevCamX = Double.MIN_VALUE;
   private double prevCamY = Double.MIN_VALUE;
   private double prevCamZ = Double.MIN_VALUE;
   private double prevCamRotX = Double.MIN_VALUE;
   private double prevCamRotY = Double.MIN_VALUE;
   private int prevCloudX = Integer.MIN_VALUE;
   private int prevCloudY = Integer.MIN_VALUE;
   private int prevCloudZ = Integer.MIN_VALUE;
   private Vec3 prevCloudColor;
   private CloudStatus prevCloudsType;
   private ChunkRenderDispatcher chunkRenderDispatcher;
   private int lastViewDistance;
   private int renderedEntities;
   private int culledEntities;
   private Frustum cullingFrustum;
   private boolean captureFrustum;
   @Nullable
   private Frustum capturedFrustum;
   private final Vector4f[] frustumPoints;
   private final Vector3d frustumPos;
   private double xTransparentOld;
   private double yTransparentOld;
   private double zTransparentOld;
   private boolean needsUpdate;
   private int frameId;
   private int rainSoundTime;
   private final float[] rainSizeX;
   private final float[] rainSizeZ;

   public LevelRenderer(Minecraft var1, RenderBuffers var2) {
      this.prevCloudColor = Vec3.ZERO;
      this.lastViewDistance = -1;
      this.frustumPoints = new Vector4f[8];
      this.frustumPos = new Vector3d(0.0D, 0.0D, 0.0D);
      this.needsUpdate = true;
      this.rainSizeX = new float[1024];
      this.rainSizeZ = new float[1024];
      this.minecraft = var1;
      this.entityRenderDispatcher = var1.getEntityRenderDispatcher();
      this.blockEntityRenderDispatcher = var1.getBlockEntityRenderDispatcher();
      this.renderBuffers = var2;
      this.textureManager = var1.getTextureManager();

      for(int var3 = 0; var3 < 32; ++var3) {
         for(int var4 = 0; var4 < 32; ++var4) {
            float var5 = (float)(var4 - 16);
            float var6 = (float)(var3 - 16);
            float var7 = Mth.sqrt(var5 * var5 + var6 * var6);
            this.rainSizeX[var3 << 5 | var4] = -var6 / var7;
            this.rainSizeZ[var3 << 5 | var4] = var5 / var7;
         }
      }

      this.createStars();
      this.createLightSky();
      this.createDarkSky();
   }

   private void renderSnowAndRain(LightTexture var1, float var2, double var3, double var5, double var7) {
      float var9 = this.minecraft.level.getRainLevel(var2);
      if (!(var9 <= 0.0F)) {
         var1.turnOnLightLayer();
         ClientLevel var10 = this.minecraft.level;
         int var11 = Mth.floor(var3);
         int var12 = Mth.floor(var5);
         int var13 = Mth.floor(var7);
         Tesselator var14 = Tesselator.getInstance();
         BufferBuilder var15 = var14.getBuilder();
         RenderSystem.disableCull();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableDepthTest();
         byte var16 = 5;
         if (Minecraft.useFancyGraphics()) {
            var16 = 10;
         }

         RenderSystem.depthMask(Minecraft.useShaderTransparency());
         byte var17 = -1;
         float var18 = (float)this.ticks + var2;
         RenderSystem.setShader(GameRenderer::getParticleShader);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         BlockPos.MutableBlockPos var19 = new BlockPos.MutableBlockPos();

         for(int var20 = var13 - var16; var20 <= var13 + var16; ++var20) {
            for(int var21 = var11 - var16; var21 <= var11 + var16; ++var21) {
               int var22 = (var20 - var13 + 16) * 32 + var21 - var11 + 16;
               double var23 = (double)this.rainSizeX[var22] * 0.5D;
               double var25 = (double)this.rainSizeZ[var22] * 0.5D;
               var19.set(var21, 0, var20);
               Biome var27 = var10.getBiome(var19);
               if (var27.getPrecipitation() != Biome.Precipitation.NONE) {
                  int var28 = var10.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, var19).getY();
                  int var29 = var12 - var16;
                  int var30 = var12 + var16;
                  if (var29 < var28) {
                     var29 = var28;
                  }

                  if (var30 < var28) {
                     var30 = var28;
                  }

                  int var31 = var28;
                  if (var28 < var12) {
                     var31 = var12;
                  }

                  if (var29 != var30) {
                     Random var32 = new Random((long)(var21 * var21 * 3121 + var21 * 45238971 ^ var20 * var20 * 418711 + var20 * 13761));
                     var19.set(var21, var29, var20);
                     float var33 = var27.getTemperature(var19);
                     float var35;
                     float var41;
                     if (var33 >= 0.15F) {
                        if (var17 != 0) {
                           if (var17 >= 0) {
                              var14.end();
                           }

                           var17 = 0;
                           RenderSystem.setShaderTexture(0, RAIN_LOCATION);
                           var15.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                        }

                        int var34 = this.ticks + var21 * var21 * 3121 + var21 * 45238971 + var20 * var20 * 418711 + var20 * 13761 & 31;
                        var35 = -((float)var34 + var2) / 32.0F * (3.0F + var32.nextFloat());
                        double var36 = (double)var21 + 0.5D - var3;
                        double var38 = (double)var20 + 0.5D - var7;
                        float var40 = (float)Math.sqrt(var36 * var36 + var38 * var38) / (float)var16;
                        var41 = ((1.0F - var40 * var40) * 0.5F + 0.5F) * var9;
                        var19.set(var21, var31, var20);
                        int var42 = getLightColor(var10, var19);
                        var15.vertex((double)var21 - var3 - var23 + 0.5D, (double)var30 - var5, (double)var20 - var7 - var25 + 0.5D).uv(0.0F, (float)var29 * 0.25F + var35).color(1.0F, 1.0F, 1.0F, var41).uv2(var42).endVertex();
                        var15.vertex((double)var21 - var3 + var23 + 0.5D, (double)var30 - var5, (double)var20 - var7 + var25 + 0.5D).uv(1.0F, (float)var29 * 0.25F + var35).color(1.0F, 1.0F, 1.0F, var41).uv2(var42).endVertex();
                        var15.vertex((double)var21 - var3 + var23 + 0.5D, (double)var29 - var5, (double)var20 - var7 + var25 + 0.5D).uv(1.0F, (float)var30 * 0.25F + var35).color(1.0F, 1.0F, 1.0F, var41).uv2(var42).endVertex();
                        var15.vertex((double)var21 - var3 - var23 + 0.5D, (double)var29 - var5, (double)var20 - var7 - var25 + 0.5D).uv(0.0F, (float)var30 * 0.25F + var35).color(1.0F, 1.0F, 1.0F, var41).uv2(var42).endVertex();
                     } else {
                        if (var17 != 1) {
                           if (var17 >= 0) {
                              var14.end();
                           }

                           var17 = 1;
                           RenderSystem.setShaderTexture(0, SNOW_LOCATION);
                           var15.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                        }

                        float var49 = -((float)(this.ticks & 511) + var2) / 512.0F;
                        var35 = (float)(var32.nextDouble() + (double)var18 * 0.01D * (double)((float)var32.nextGaussian()));
                        float var50 = (float)(var32.nextDouble() + (double)(var18 * (float)var32.nextGaussian()) * 0.001D);
                        double var37 = (double)var21 + 0.5D - var3;
                        double var39 = (double)var20 + 0.5D - var7;
                        var41 = (float)Math.sqrt(var37 * var37 + var39 * var39) / (float)var16;
                        float var48 = ((1.0F - var41 * var41) * 0.3F + 0.5F) * var9;
                        var19.set(var21, var31, var20);
                        int var43 = getLightColor(var10, var19);
                        int var44 = var43 >> 16 & '\uffff';
                        int var45 = var43 & '\uffff';
                        int var46 = (var44 * 3 + 240) / 4;
                        int var47 = (var45 * 3 + 240) / 4;
                        var15.vertex((double)var21 - var3 - var23 + 0.5D, (double)var30 - var5, (double)var20 - var7 - var25 + 0.5D).uv(0.0F + var35, (float)var29 * 0.25F + var49 + var50).color(1.0F, 1.0F, 1.0F, var48).uv2(var47, var46).endVertex();
                        var15.vertex((double)var21 - var3 + var23 + 0.5D, (double)var30 - var5, (double)var20 - var7 + var25 + 0.5D).uv(1.0F + var35, (float)var29 * 0.25F + var49 + var50).color(1.0F, 1.0F, 1.0F, var48).uv2(var47, var46).endVertex();
                        var15.vertex((double)var21 - var3 + var23 + 0.5D, (double)var29 - var5, (double)var20 - var7 + var25 + 0.5D).uv(1.0F + var35, (float)var30 * 0.25F + var49 + var50).color(1.0F, 1.0F, 1.0F, var48).uv2(var47, var46).endVertex();
                        var15.vertex((double)var21 - var3 - var23 + 0.5D, (double)var29 - var5, (double)var20 - var7 - var25 + 0.5D).uv(0.0F + var35, (float)var30 * 0.25F + var49 + var50).color(1.0F, 1.0F, 1.0F, var48).uv2(var47, var46).endVertex();
                     }
                  }
               }
            }
         }

         if (var17 >= 0) {
            var14.end();
         }

         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         var1.turnOffLightLayer();
      }
   }

   public void tickRain(Camera var1) {
      float var2 = this.minecraft.level.getRainLevel(1.0F) / (Minecraft.useFancyGraphics() ? 1.0F : 2.0F);
      if (!(var2 <= 0.0F)) {
         Random var3 = new Random((long)this.ticks * 312987231L);
         ClientLevel var4 = this.minecraft.level;
         BlockPos var5 = new BlockPos(var1.getPosition());
         BlockPos var6 = null;
         int var7 = (int)(100.0F * var2 * var2) / (this.minecraft.options.particles == ParticleStatus.DECREASED ? 2 : 1);

         for(int var8 = 0; var8 < var7; ++var8) {
            int var9 = var3.nextInt(21) - 10;
            int var10 = var3.nextInt(21) - 10;
            BlockPos var11 = var4.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, var5.offset(var9, 0, var10)).below();
            Biome var12 = var4.getBiome(var11);
            if (var11.getY() > var4.getMinBuildHeight() && var11.getY() <= var5.getY() + 10 && var11.getY() >= var5.getY() - 10 && var12.getPrecipitation() == Biome.Precipitation.RAIN && var12.getTemperature(var11) >= 0.15F) {
               var6 = var11;
               if (this.minecraft.options.particles == ParticleStatus.MINIMAL) {
                  break;
               }

               double var13 = var3.nextDouble();
               double var15 = var3.nextDouble();
               BlockState var17 = var4.getBlockState(var11);
               FluidState var18 = var4.getFluidState(var11);
               VoxelShape var19 = var17.getCollisionShape(var4, var11);
               double var20 = var19.max(Direction.Axis.Y, var13, var15);
               double var22 = (double)var18.getHeight(var4, var11);
               double var24 = Math.max(var20, var22);
               SimpleParticleType var26 = !var18.is(FluidTags.LAVA) && !var17.is(Blocks.MAGMA_BLOCK) && !CampfireBlock.isLitCampfire(var17) ? ParticleTypes.RAIN : ParticleTypes.SMOKE;
               this.minecraft.level.addParticle(var26, (double)var11.getX() + var13, (double)var11.getY() + var24, (double)var11.getZ() + var15, 0.0D, 0.0D, 0.0D);
            }
         }

         if (var6 != null && var3.nextInt(3) < this.rainSoundTime++) {
            this.rainSoundTime = 0;
            if (var6.getY() > var5.getY() + 1 && var4.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, var5).getY() > Mth.floor((float)var5.getY())) {
               this.minecraft.level.playLocalSound(var6, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F, 0.5F, false);
            } else {
               this.minecraft.level.playLocalSound(var6, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F, 1.0F, false);
            }
         }

      }
   }

   public void close() {
      if (this.entityEffect != null) {
         this.entityEffect.close();
      }

      if (this.transparencyChain != null) {
         this.transparencyChain.close();
      }

   }

   public void onResourceManagerReload(ResourceManager var1) {
      this.initOutline();
      if (Minecraft.useShaderTransparency()) {
         this.initTransparency();
      }

   }

   public void initOutline() {
      if (this.entityEffect != null) {
         this.entityEffect.close();
      }

      ResourceLocation var1 = new ResourceLocation("shaders/post/entity_outline.json");

      try {
         this.entityEffect = new PostChain(this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getMainRenderTarget(), var1);
         this.entityEffect.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
         this.entityTarget = this.entityEffect.getTempTarget("final");
      } catch (IOException var3) {
         LOGGER.warn("Failed to load shader: {}", var1, var3);
         this.entityEffect = null;
         this.entityTarget = null;
      } catch (JsonSyntaxException var4) {
         LOGGER.warn("Failed to parse shader: {}", var1, var4);
         this.entityEffect = null;
         this.entityTarget = null;
      }

   }

   private void initTransparency() {
      this.deinitTransparency();
      ResourceLocation var1 = new ResourceLocation("shaders/post/transparency.json");

      try {
         PostChain var2 = new PostChain(this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getMainRenderTarget(), var1);
         var2.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
         RenderTarget var10 = var2.getTempTarget("translucent");
         RenderTarget var11 = var2.getTempTarget("itemEntity");
         RenderTarget var12 = var2.getTempTarget("particles");
         RenderTarget var14 = var2.getTempTarget("weather");
         RenderTarget var7 = var2.getTempTarget("clouds");
         this.transparencyChain = var2;
         this.translucentTarget = var10;
         this.itemEntityTarget = var11;
         this.particlesTarget = var12;
         this.weatherTarget = var14;
         this.cloudsTarget = var7;
      } catch (Exception var9) {
         String var3 = var9 instanceof JsonSyntaxException ? "parse" : "load";
         String var4 = "Failed to " + var3 + " shader: " + var1;
         LevelRenderer.TransparencyShaderException var5 = new LevelRenderer.TransparencyShaderException(var4, var9);
         if (this.minecraft.getResourcePackRepository().getSelectedIds().size() > 1) {
            TextComponent var6;
            try {
               var6 = new TextComponent(this.minecraft.getResourceManager().getResource(var1).getSourceName());
            } catch (IOException var8) {
               var6 = null;
            }

            this.minecraft.options.graphicsMode = GraphicsStatus.FANCY;
            this.minecraft.clearResourcePacksOnError(var5, var6);
         } else {
            CrashReport var13 = this.minecraft.fillReport(new CrashReport(var4, var5));
            this.minecraft.options.graphicsMode = GraphicsStatus.FANCY;
            this.minecraft.options.save();
            LOGGER.fatal(var4, var5);
            this.minecraft.emergencySave();
            Minecraft.crash(var13);
         }
      }

   }

   private void deinitTransparency() {
      if (this.transparencyChain != null) {
         this.transparencyChain.close();
         this.translucentTarget.destroyBuffers();
         this.itemEntityTarget.destroyBuffers();
         this.particlesTarget.destroyBuffers();
         this.weatherTarget.destroyBuffers();
         this.cloudsTarget.destroyBuffers();
         this.transparencyChain = null;
         this.translucentTarget = null;
         this.itemEntityTarget = null;
         this.particlesTarget = null;
         this.weatherTarget = null;
         this.cloudsTarget = null;
      }

   }

   public void doEntityOutline() {
      if (this.shouldShowEntityOutlines()) {
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
         this.entityTarget.blitToScreen(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), false);
         RenderSystem.disableBlend();
      }

   }

   protected boolean shouldShowEntityOutlines() {
      return !this.minecraft.gameRenderer.isPanoramicMode() && this.entityTarget != null && this.entityEffect != null && this.minecraft.player != null;
   }

   private void createDarkSky() {
      Tesselator var1 = Tesselator.getInstance();
      BufferBuilder var2 = var1.getBuilder();
      if (this.darkBuffer != null) {
         this.darkBuffer.close();
      }

      this.darkBuffer = new VertexBuffer();
      buildSkyDisc(var2, -16.0F);
      this.darkBuffer.upload(var2);
   }

   private void createLightSky() {
      Tesselator var1 = Tesselator.getInstance();
      BufferBuilder var2 = var1.getBuilder();
      if (this.skyBuffer != null) {
         this.skyBuffer.close();
      }

      this.skyBuffer = new VertexBuffer();
      buildSkyDisc(var2, 16.0F);
      this.skyBuffer.upload(var2);
   }

   private static void buildSkyDisc(BufferBuilder var0, float var1) {
      float var2 = Math.signum(var1) * 512.0F;
      float var3 = 512.0F;
      RenderSystem.setShader(GameRenderer::getPositionShader);
      var0.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
      var0.vertex(0.0D, (double)var1, 0.0D).endVertex();

      for(int var4 = -180; var4 <= 180; var4 += 45) {
         var0.vertex((double)(var2 * Mth.cos((float)var4 * 0.017453292F)), (double)var1, (double)(512.0F * Mth.sin((float)var4 * 0.017453292F))).endVertex();
      }

      var0.end();
   }

   private void createStars() {
      Tesselator var1 = Tesselator.getInstance();
      BufferBuilder var2 = var1.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      if (this.starBuffer != null) {
         this.starBuffer.close();
      }

      this.starBuffer = new VertexBuffer();
      this.drawStars(var2);
      var2.end();
      this.starBuffer.upload(var2);
   }

   private void drawStars(BufferBuilder var1) {
      Random var2 = new Random(10842L);
      var1.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

      for(int var3 = 0; var3 < 1500; ++var3) {
         double var4 = (double)(var2.nextFloat() * 2.0F - 1.0F);
         double var6 = (double)(var2.nextFloat() * 2.0F - 1.0F);
         double var8 = (double)(var2.nextFloat() * 2.0F - 1.0F);
         double var10 = (double)(0.15F + var2.nextFloat() * 0.1F);
         double var12 = var4 * var4 + var6 * var6 + var8 * var8;
         if (var12 < 1.0D && var12 > 0.01D) {
            var12 = 1.0D / Math.sqrt(var12);
            var4 *= var12;
            var6 *= var12;
            var8 *= var12;
            double var14 = var4 * 100.0D;
            double var16 = var6 * 100.0D;
            double var18 = var8 * 100.0D;
            double var20 = Math.atan2(var4, var8);
            double var22 = Math.sin(var20);
            double var24 = Math.cos(var20);
            double var26 = Math.atan2(Math.sqrt(var4 * var4 + var8 * var8), var6);
            double var28 = Math.sin(var26);
            double var30 = Math.cos(var26);
            double var32 = var2.nextDouble() * 3.141592653589793D * 2.0D;
            double var34 = Math.sin(var32);
            double var36 = Math.cos(var32);

            for(int var38 = 0; var38 < 4; ++var38) {
               double var39 = 0.0D;
               double var41 = (double)((var38 & 2) - 1) * var10;
               double var43 = (double)((var38 + 1 & 2) - 1) * var10;
               double var45 = 0.0D;
               double var47 = var41 * var36 - var43 * var34;
               double var49 = var43 * var36 + var41 * var34;
               double var53 = var47 * var28 + 0.0D * var30;
               double var55 = 0.0D * var28 - var47 * var30;
               double var57 = var55 * var22 - var49 * var24;
               double var61 = var49 * var22 + var55 * var24;
               var1.vertex(var14 + var57, var16 + var53, var18 + var61).endVertex();
            }
         }
      }

   }

   public void setLevel(@Nullable ClientLevel var1) {
      this.lastCameraX = Double.MIN_VALUE;
      this.lastCameraY = Double.MIN_VALUE;
      this.lastCameraZ = Double.MIN_VALUE;
      this.lastCameraChunkX = Integer.MIN_VALUE;
      this.lastCameraChunkY = Integer.MIN_VALUE;
      this.lastCameraChunkZ = Integer.MIN_VALUE;
      this.entityRenderDispatcher.setLevel(var1);
      this.level = var1;
      if (var1 != null) {
         this.renderChunks.ensureCapacity(4356 * var1.getSectionsCount());
         this.allChanged();
      } else {
         this.chunksToCompile.clear();
         this.renderChunks.clear();
         if (this.viewArea != null) {
            this.viewArea.releaseAllBuffers();
            this.viewArea = null;
         }

         if (this.chunkRenderDispatcher != null) {
            this.chunkRenderDispatcher.dispose();
         }

         this.chunkRenderDispatcher = null;
         this.globalBlockEntities.clear();
      }

   }

   public void graphicsChanged() {
      if (Minecraft.useShaderTransparency()) {
         this.initTransparency();
      } else {
         this.deinitTransparency();
      }

   }

   public void allChanged() {
      if (this.level != null) {
         this.graphicsChanged();
         this.level.clearTintCaches();
         if (this.chunkRenderDispatcher == null) {
            this.chunkRenderDispatcher = new ChunkRenderDispatcher(this.level, this, Util.backgroundExecutor(), this.minecraft.is64Bit(), this.renderBuffers.fixedBufferPack());
         } else {
            this.chunkRenderDispatcher.setLevel(this.level);
         }

         this.needsUpdate = true;
         this.generateClouds = true;
         ItemBlockRenderTypes.setFancy(Minecraft.useFancyGraphics());
         this.lastViewDistance = this.minecraft.options.renderDistance;
         if (this.viewArea != null) {
            this.viewArea.releaseAllBuffers();
         }

         this.resetChunksToCompile();
         synchronized(this.globalBlockEntities) {
            this.globalBlockEntities.clear();
         }

         this.viewArea = new ViewArea(this.chunkRenderDispatcher, this.level, this.minecraft.options.renderDistance, this);
         this.renderInfoMap = new LevelRenderer.RenderInfoMap(this.viewArea.chunks.length);
         if (this.level != null) {
            Entity var1 = this.minecraft.getCameraEntity();
            if (var1 != null) {
               this.viewArea.repositionCamera(var1.getX(), var1.getZ());
            }
         }

      }
   }

   protected void resetChunksToCompile() {
      this.chunksToCompile.clear();
      this.chunkRenderDispatcher.blockUntilClear();
   }

   public void resize(int var1, int var2) {
      this.needsUpdate();
      if (this.entityEffect != null) {
         this.entityEffect.resize(var1, var2);
      }

      if (this.transparencyChain != null) {
         this.transparencyChain.resize(var1, var2);
      }

   }

   public String getChunkStatistics() {
      int var1 = this.viewArea.chunks.length;
      int var2 = this.countRenderedChunks();
      return String.format("C: %d/%d %sD: %d, %s", var2, var1, this.minecraft.smartCull ? "(s) " : "", this.lastViewDistance, this.chunkRenderDispatcher == null ? "null" : this.chunkRenderDispatcher.getStats());
   }

   public ChunkRenderDispatcher getChunkRenderDispatcher() {
      return this.chunkRenderDispatcher;
   }

   public double getTotalChunks() {
      return (double)this.viewArea.chunks.length;
   }

   public double getLastViewDistance() {
      return (double)this.lastViewDistance;
   }

   public int countRenderedChunks() {
      int var1 = 0;
      ObjectListIterator var2 = this.renderChunks.iterator();

      while(var2.hasNext()) {
         LevelRenderer.RenderChunkInfo var3 = (LevelRenderer.RenderChunkInfo)var2.next();
         if (!var3.chunk.getCompiledChunk().hasNoRenderableLayers()) {
            ++var1;
         }
      }

      return var1;
   }

   public String getEntityStatistics() {
      int var10000 = this.renderedEntities;
      return "E: " + var10000 + "/" + this.level.getEntityCount() + ", B: " + this.culledEntities;
   }

   private void setupRender(Camera var1, Frustum var2, boolean var3, int var4, boolean var5) {
      Vec3 var6 = var1.getPosition();
      if (this.minecraft.options.renderDistance != this.lastViewDistance) {
         this.allChanged();
      }

      this.level.getProfiler().push("camera");
      double var7 = this.minecraft.player.getX();
      double var9 = this.minecraft.player.getY();
      double var11 = this.minecraft.player.getZ();
      double var13 = var7 - this.lastCameraX;
      double var15 = var9 - this.lastCameraY;
      double var17 = var11 - this.lastCameraZ;
      int var19 = SectionPos.posToSectionCoord(var7);
      int var20 = SectionPos.posToSectionCoord(var9);
      int var21 = SectionPos.posToSectionCoord(var11);
      if (this.lastCameraChunkX != var19 || this.lastCameraChunkY != var20 || this.lastCameraChunkZ != var21 || var13 * var13 + var15 * var15 + var17 * var17 > 16.0D) {
         this.lastCameraX = var7;
         this.lastCameraY = var9;
         this.lastCameraZ = var11;
         this.lastCameraChunkX = var19;
         this.lastCameraChunkY = var20;
         this.lastCameraChunkZ = var21;
         this.viewArea.repositionCamera(var7, var11);
      }

      this.chunkRenderDispatcher.setCamera(var6);
      this.level.getProfiler().popPush("cull");
      this.minecraft.getProfiler().popPush("culling");
      BlockPos var22 = var1.getBlockPosition();
      ChunkRenderDispatcher.RenderChunk var23 = this.viewArea.getRenderChunkAt(var22);
      boolean var24 = true;
      BlockPos var25 = new BlockPos(Mth.floor(var6.x / 16.0D) * 16, Mth.floor(var6.y / 16.0D) * 16, Mth.floor(var6.z / 16.0D) * 16);
      float var26 = var1.getXRot();
      float var27 = var1.getYRot();
      this.needsUpdate = this.needsUpdate || !this.chunksToCompile.isEmpty() || var6.x != this.prevCamX || var6.y != this.prevCamY || var6.z != this.prevCamZ || (double)var26 != this.prevCamRotX || (double)var27 != this.prevCamRotY;
      this.prevCamX = var6.x;
      this.prevCamY = var6.y;
      this.prevCamZ = var6.z;
      this.prevCamRotX = (double)var26;
      this.prevCamRotY = (double)var27;
      this.minecraft.getProfiler().popPush("update");
      if (!var3 && this.needsUpdate) {
         this.needsUpdate = false;
         this.updateRenderChunks(var2, var4, var5, var6, var22, var23, 16, var25);
      }

      this.minecraft.getProfiler().popPush("rebuildNear");
      Set var28 = this.chunksToCompile;
      this.chunksToCompile = Sets.newLinkedHashSet();
      ObjectListIterator var29 = this.renderChunks.iterator();

      while(true) {
         while(true) {
            ChunkRenderDispatcher.RenderChunk var31;
            do {
               if (!var29.hasNext()) {
                  this.chunksToCompile.addAll(var28);
                  this.minecraft.getProfiler().pop();
                  return;
               }

               LevelRenderer.RenderChunkInfo var30 = (LevelRenderer.RenderChunkInfo)var29.next();
               var31 = var30.chunk;
            } while(!var31.isDirty() && !var28.contains(var31));

            this.needsUpdate = true;
            BlockPos var32 = var31.getOrigin().offset(8, 8, 8);
            boolean var33 = var32.distSqr(var22) < 768.0D;
            if (!var31.isDirtyFromPlayer() && !var33) {
               this.chunksToCompile.add(var31);
            } else {
               this.minecraft.getProfiler().push("build near");
               this.chunkRenderDispatcher.rebuildChunkSync(var31);
               var31.setNotDirty();
               this.minecraft.getProfiler().pop();
            }
         }
      }
   }

   private void updateRenderChunks(Frustum var1, int var2, boolean var3, Vec3 var4, BlockPos var5, ChunkRenderDispatcher.RenderChunk var6, int var7, BlockPos var8) {
      this.renderChunks.clear();
      ArrayDeque var9 = Queues.newArrayDeque();
      Entity.setViewScale(Mth.clamp((double)this.minecraft.options.renderDistance / 8.0D, 1.0D, 2.5D) * (double)this.minecraft.options.entityDistanceScaling);
      boolean var10 = this.minecraft.smartCull;
      int var11;
      int var15;
      int var16;
      if (var6 == null) {
         var11 = var5.getY() > this.level.getMinBuildHeight() ? this.level.getMaxBuildHeight() - 8 : this.level.getMinBuildHeight() + 8;
         int var12 = Mth.floor(var4.x / (double)var7) * var7;
         int var13 = Mth.floor(var4.z / (double)var7) * var7;
         ArrayList var14 = Lists.newArrayList();

         for(var15 = -this.lastViewDistance; var15 <= this.lastViewDistance; ++var15) {
            for(var16 = -this.lastViewDistance; var16 <= this.lastViewDistance; ++var16) {
               ChunkRenderDispatcher.RenderChunk var17 = this.viewArea.getRenderChunkAt(new BlockPos(var12 + SectionPos.sectionToBlockCoord(var15, 8), var11, var13 + SectionPos.sectionToBlockCoord(var16, 8)));
               if (var17 != null && var1.isVisible(var17.bb)) {
                  var17.setFrame(var2);
                  var14.add(new LevelRenderer.RenderChunkInfo(var17, (Direction)null, 0));
               }
            }
         }

         var14.sort(Comparator.comparingDouble((var1x) -> {
            return var5.distSqr(var1x.chunk.getOrigin().offset(8, 8, 8));
         }));
         var9.addAll(var14);
      } else {
         if (var3 && this.level.getBlockState(var5).isSolidRender(this.level, var5)) {
            var10 = false;
         }

         var6.setFrame(var2);
         var9.add(new LevelRenderer.RenderChunkInfo(var6, (Direction)null, 0));
      }

      this.minecraft.getProfiler().push("iteration");
      var11 = this.minecraft.options.renderDistance;
      this.renderInfoMap.clear();

      while(!var9.isEmpty()) {
         LevelRenderer.RenderChunkInfo var22 = (LevelRenderer.RenderChunkInfo)var9.poll();
         ChunkRenderDispatcher.RenderChunk var23 = var22.chunk;
         this.renderChunks.add(var22);
         Direction[] var24 = DIRECTIONS;
         var15 = var24.length;

         for(var16 = 0; var16 < var15; ++var16) {
            Direction var25 = var24[var16];
            ChunkRenderDispatcher.RenderChunk var18 = this.getRelativeFrom(var8, var23, var25);
            if (!var10 || !var22.hasDirection(var25.getOpposite())) {
               if (var10 && var22.hasSourceDirections()) {
                  ChunkRenderDispatcher.CompiledChunk var19 = var23.getCompiledChunk();
                  boolean var20 = false;

                  for(int var21 = 0; var21 < DIRECTIONS.length; ++var21) {
                     if (var22.hasSourceDirection(var21) && var19.facesCanSeeEachother(DIRECTIONS[var21].getOpposite(), var25)) {
                        var20 = true;
                        break;
                     }
                  }

                  if (!var20) {
                     continue;
                  }
               }

               if (var18 != null && var18.hasAllNeighbors()) {
                  LevelRenderer.RenderChunkInfo var26;
                  if (!var18.setFrame(var2)) {
                     var26 = this.renderInfoMap.get(var18);
                     if (var26 != null) {
                        var26.addSourceDirection(var25);
                     }
                  } else if (var1.isVisible(var18.bb)) {
                     var26 = new LevelRenderer.RenderChunkInfo(var18, var25, var22.step + 1);
                     var26.setDirections(var22.directions, var25);
                     var9.add(var26);
                     this.renderInfoMap.put(var18, var26);
                  }
               }
            }
         }
      }

      this.minecraft.getProfiler().pop();
   }

   @Nullable
   private ChunkRenderDispatcher.RenderChunk getRelativeFrom(BlockPos var1, ChunkRenderDispatcher.RenderChunk var2, Direction var3) {
      BlockPos var4 = var2.getRelativeOrigin(var3);
      if (Mth.abs(var1.getX() - var4.getX()) > this.lastViewDistance * 16) {
         return null;
      } else if (var4.getY() >= this.level.getMinBuildHeight() && var4.getY() < this.level.getMaxBuildHeight()) {
         return Mth.abs(var1.getZ() - var4.getZ()) > this.lastViewDistance * 16 ? null : this.viewArea.getRenderChunkAt(var4);
      } else {
         return null;
      }
   }

   private void captureFrustum(Matrix4f var1, Matrix4f var2, double var3, double var5, double var7, Frustum var9) {
      this.capturedFrustum = var9;
      Matrix4f var10 = var2.copy();
      var10.multiply(var1);
      var10.invert();
      this.frustumPos.x = var3;
      this.frustumPos.y = var5;
      this.frustumPos.z = var7;
      this.frustumPoints[0] = new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F);
      this.frustumPoints[1] = new Vector4f(1.0F, -1.0F, -1.0F, 1.0F);
      this.frustumPoints[2] = new Vector4f(1.0F, 1.0F, -1.0F, 1.0F);
      this.frustumPoints[3] = new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F);
      this.frustumPoints[4] = new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F);
      this.frustumPoints[5] = new Vector4f(1.0F, -1.0F, 1.0F, 1.0F);
      this.frustumPoints[6] = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.frustumPoints[7] = new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F);

      for(int var11 = 0; var11 < 8; ++var11) {
         this.frustumPoints[var11].transform(var10);
         this.frustumPoints[var11].perspectiveDivide();
      }

   }

   public void prepareCullFrustum(PoseStack var1, Vec3 var2, Matrix4f var3) {
      Matrix4f var4 = var1.last().pose();
      double var5 = var2.x();
      double var7 = var2.y();
      double var9 = var2.z();
      this.cullingFrustum = new Frustum(var4, var3);
      this.cullingFrustum.prepare(var5, var7, var9);
   }

   public void renderLevel(PoseStack var1, float var2, long var3, boolean var5, Camera var6, GameRenderer var7, LightTexture var8, Matrix4f var9) {
      RenderSystem.setShaderGameTime(this.level.getGameTime(), var2);
      this.blockEntityRenderDispatcher.prepare(this.level, var6, this.minecraft.hitResult);
      this.entityRenderDispatcher.prepare(this.level, var6, this.minecraft.crosshairPickEntity);
      ProfilerFiller var10 = this.level.getProfiler();
      var10.popPush("light_updates");
      this.minecraft.level.getChunkSource().getLightEngine().runUpdates(Integer.MAX_VALUE, true, true);
      Vec3 var11 = var6.getPosition();
      double var12 = var11.x();
      double var14 = var11.y();
      double var16 = var11.z();
      Matrix4f var18 = var1.last().pose();
      var10.popPush("culling");
      boolean var19 = this.capturedFrustum != null;
      Frustum var20;
      if (var19) {
         var20 = this.capturedFrustum;
         var20.prepare(this.frustumPos.x, this.frustumPos.y, this.frustumPos.z);
      } else {
         var20 = this.cullingFrustum;
      }

      this.minecraft.getProfiler().popPush("captureFrustum");
      if (this.captureFrustum) {
         this.captureFrustum(var18, var9, var11.x, var11.y, var11.z, var19 ? new Frustum(var18, var9) : var20);
         this.captureFrustum = false;
      }

      var10.popPush("clear");
      FogRenderer.setupColor(var6, var2, this.minecraft.level, this.minecraft.options.renderDistance, var7.getDarkenWorldAmount(var2));
      FogRenderer.levelFogColor();
      RenderSystem.clear(16640, Minecraft.ON_OSX);
      float var21 = var7.getRenderDistance();
      boolean var22 = this.minecraft.level.effects().isFoggyAt(Mth.floor(var12), Mth.floor(var14)) || this.minecraft.gui.getBossOverlay().shouldCreateWorldFog();
      if (this.minecraft.options.renderDistance >= 4) {
         var10.popPush("sky");
         RenderSystem.setShader(GameRenderer::getPositionShader);
         this.renderSky(var1, var9, var2, () -> {
            FogRenderer.setupFog(var6, FogRenderer.FogMode.FOG_SKY, var21, var22);
         });
      }

      var10.popPush("fog");
      FogRenderer.setupFog(var6, FogRenderer.FogMode.FOG_TERRAIN, Math.max(var21 - 16.0F, 32.0F), var22);
      var10.popPush("terrain_setup");
      this.setupRender(var6, var20, var19, this.frameId++, this.minecraft.player.isSpectator());
      var10.popPush("updatechunks");
      boolean var23 = true;
      int var24 = this.minecraft.options.framerateLimit;
      long var25 = 33333333L;
      long var27;
      if ((double)var24 == Option.FRAMERATE_LIMIT.getMaxValue()) {
         var27 = 0L;
      } else {
         var27 = (long)(1000000000 / var24);
      }

      long var29 = Util.getNanos() - var3;
      long var31 = this.frameTimes.registerValueAndGetMean(var29);
      long var33 = var31 * 3L / 2L;
      long var35 = Mth.clamp(var33, var27, 33333333L);
      this.compileChunksUntil(var3 + var35);
      var10.popPush("terrain");
      this.renderChunkLayer(RenderType.solid(), var1, var12, var14, var16, var9);
      this.renderChunkLayer(RenderType.cutoutMipped(), var1, var12, var14, var16, var9);
      this.renderChunkLayer(RenderType.cutout(), var1, var12, var14, var16, var9);
      if (this.level.effects().constantAmbientLight()) {
         Lighting.setupNetherLevel(var1.last().pose());
      } else {
         Lighting.setupLevel(var1.last().pose());
      }

      var10.popPush("entities");
      this.renderedEntities = 0;
      this.culledEntities = 0;
      if (this.itemEntityTarget != null) {
         this.itemEntityTarget.clear(Minecraft.ON_OSX);
         this.itemEntityTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
         this.minecraft.getMainRenderTarget().bindWrite(false);
      }

      if (this.weatherTarget != null) {
         this.weatherTarget.clear(Minecraft.ON_OSX);
      }

      if (this.shouldShowEntityOutlines()) {
         this.entityTarget.clear(Minecraft.ON_OSX);
         this.minecraft.getMainRenderTarget().bindWrite(false);
      }

      boolean var37 = false;
      MultiBufferSource.BufferSource var38 = this.renderBuffers.bufferSource();
      Iterator var39 = this.level.entitiesForRendering().iterator();

      while(true) {
         Entity var40;
         int var47;
         do {
            do {
               do {
                  if (!var39.hasNext()) {
                     var38.endLastBatch();
                     this.checkPoseStack(var1);
                     var38.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
                     var38.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
                     var38.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
                     var38.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));
                     var10.popPush("blockentities");
                     ObjectListIterator var53 = this.renderChunks.iterator();

                     while(true) {
                        List var61;
                        do {
                           if (!var53.hasNext()) {
                              synchronized(this.globalBlockEntities) {
                                 Iterator var57 = this.globalBlockEntities.iterator();

                                 while(true) {
                                    if (!var57.hasNext()) {
                                       break;
                                    }

                                    BlockEntity var63 = (BlockEntity)var57.next();
                                    BlockPos var64 = var63.getBlockPos();
                                    var1.pushPose();
                                    var1.translate((double)var64.getX() - var12, (double)var64.getY() - var14, (double)var64.getZ() - var16);
                                    this.blockEntityRenderDispatcher.render(var63, var2, var1, var38);
                                    var1.popPose();
                                 }
                              }

                              this.checkPoseStack(var1);
                              var38.endBatch(RenderType.solid());
                              var38.endBatch(RenderType.endPortal());
                              var38.endBatch(RenderType.endGateway());
                              var38.endBatch(Sheets.solidBlockSheet());
                              var38.endBatch(Sheets.cutoutBlockSheet());
                              var38.endBatch(Sheets.bedSheet());
                              var38.endBatch(Sheets.shulkerBoxSheet());
                              var38.endBatch(Sheets.signSheet());
                              var38.endBatch(Sheets.chestSheet());
                              this.renderBuffers.outlineBufferSource().endOutlineBatch();
                              if (var37) {
                                 this.entityEffect.process(var2);
                                 this.minecraft.getMainRenderTarget().bindWrite(false);
                              }

                              var10.popPush("destroyProgress");
                              ObjectIterator var54 = this.destructionProgress.long2ObjectEntrySet().iterator();

                              while(var54.hasNext()) {
                                 Entry var58 = (Entry)var54.next();
                                 BlockPos var65 = BlockPos.of(var58.getLongKey());
                                 double var66 = (double)var65.getX() - var12;
                                 double var71 = (double)var65.getY() - var14;
                                 double var74 = (double)var65.getZ() - var16;
                                 if (!(var66 * var66 + var71 * var71 + var74 * var74 > 1024.0D)) {
                                    SortedSet var75 = (SortedSet)var58.getValue();
                                    if (var75 != null && !var75.isEmpty()) {
                                       int var76 = ((BlockDestructionProgress)var75.last()).getProgress();
                                       var1.pushPose();
                                       var1.translate((double)var65.getX() - var12, (double)var65.getY() - var14, (double)var65.getZ() - var16);
                                       PoseStack.Pose var50 = var1.last();
                                       SheetedDecalTextureGenerator var51 = new SheetedDecalTextureGenerator(this.renderBuffers.crumblingBufferSource().getBuffer((RenderType)ModelBakery.DESTROY_TYPES.get(var76)), var50.pose(), var50.normal());
                                       this.minecraft.getBlockRenderer().renderBreakingTexture(this.level.getBlockState(var65), var65, this.level, var1, var51);
                                       var1.popPose();
                                    }
                                 }
                              }

                              this.checkPoseStack(var1);
                              HitResult var55 = this.minecraft.hitResult;
                              if (var5 && var55 != null && var55.getType() == HitResult.Type.BLOCK) {
                                 var10.popPush("outline");
                                 BlockPos var59 = ((BlockHitResult)var55).getBlockPos();
                                 BlockState var67 = this.level.getBlockState(var59);
                                 if (!var67.isAir() && this.level.getWorldBorder().isWithinBounds(var59)) {
                                    VertexConsumer var69 = var38.getBuffer(RenderType.lines());
                                    this.renderHitOutline(var1, var69, var6.getEntity(), var12, var14, var16, var59, var67);
                                 }
                              }

                              PoseStack var60 = RenderSystem.getModelViewStack();
                              var60.pushPose();
                              var60.mulPoseMatrix(var1.last().pose());
                              RenderSystem.applyModelViewMatrix();
                              this.minecraft.debugRenderer.render(var1, var38, var12, var14, var16);
                              var60.popPose();
                              RenderSystem.applyModelViewMatrix();
                              var38.endBatch(Sheets.translucentCullBlockSheet());
                              var38.endBatch(Sheets.bannerSheet());
                              var38.endBatch(Sheets.shieldSheet());
                              var38.endBatch(RenderType.armorGlint());
                              var38.endBatch(RenderType.armorEntityGlint());
                              var38.endBatch(RenderType.glint());
                              var38.endBatch(RenderType.glintDirect());
                              var38.endBatch(RenderType.glintTranslucent());
                              var38.endBatch(RenderType.entityGlint());
                              var38.endBatch(RenderType.entityGlintDirect());
                              var38.endBatch(RenderType.waterMask());
                              this.renderBuffers.crumblingBufferSource().endBatch();
                              if (this.transparencyChain != null) {
                                 var38.endBatch(RenderType.lines());
                                 var38.endBatch();
                                 this.translucentTarget.clear(Minecraft.ON_OSX);
                                 this.translucentTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
                                 var10.popPush("translucent");
                                 this.renderChunkLayer(RenderType.translucent(), var1, var12, var14, var16, var9);
                                 var10.popPush("string");
                                 this.renderChunkLayer(RenderType.tripwire(), var1, var12, var14, var16, var9);
                                 this.particlesTarget.clear(Minecraft.ON_OSX);
                                 this.particlesTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
                                 RenderStateShard.PARTICLES_TARGET.setupRenderState();
                                 var10.popPush("particles");
                                 this.minecraft.particleEngine.render(var1, var38, var8, var6, var2);
                                 RenderStateShard.PARTICLES_TARGET.clearRenderState();
                              } else {
                                 var10.popPush("translucent");
                                 if (this.translucentTarget != null) {
                                    this.translucentTarget.clear(Minecraft.ON_OSX);
                                 }

                                 this.renderChunkLayer(RenderType.translucent(), var1, var12, var14, var16, var9);
                                 var38.endBatch(RenderType.lines());
                                 var38.endBatch();
                                 var10.popPush("string");
                                 this.renderChunkLayer(RenderType.tripwire(), var1, var12, var14, var16, var9);
                                 var10.popPush("particles");
                                 this.minecraft.particleEngine.render(var1, var38, var8, var6, var2);
                              }

                              var60.pushPose();
                              var60.mulPoseMatrix(var1.last().pose());
                              RenderSystem.applyModelViewMatrix();
                              if (this.minecraft.options.getCloudsType() != CloudStatus.OFF) {
                                 if (this.transparencyChain != null) {
                                    this.cloudsTarget.clear(Minecraft.ON_OSX);
                                    RenderStateShard.CLOUDS_TARGET.setupRenderState();
                                    var10.popPush("clouds");
                                    this.renderClouds(var1, var9, var2, var12, var14, var16);
                                    RenderStateShard.CLOUDS_TARGET.clearRenderState();
                                 } else {
                                    var10.popPush("clouds");
                                    RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
                                    this.renderClouds(var1, var9, var2, var12, var14, var16);
                                 }
                              }

                              if (this.transparencyChain != null) {
                                 RenderStateShard.WEATHER_TARGET.setupRenderState();
                                 var10.popPush("weather");
                                 this.renderSnowAndRain(var8, var2, var12, var14, var16);
                                 this.renderWorldBorder(var6);
                                 RenderStateShard.WEATHER_TARGET.clearRenderState();
                                 this.transparencyChain.process(var2);
                                 this.minecraft.getMainRenderTarget().bindWrite(false);
                              } else {
                                 RenderSystem.depthMask(false);
                                 var10.popPush("weather");
                                 this.renderSnowAndRain(var8, var2, var12, var14, var16);
                                 this.renderWorldBorder(var6);
                                 RenderSystem.depthMask(true);
                              }

                              this.renderDebug(var6);
                              RenderSystem.depthMask(true);
                              RenderSystem.disableBlend();
                              var60.popPose();
                              RenderSystem.applyModelViewMatrix();
                              FogRenderer.setupNoFog();
                              return;
                           }

                           LevelRenderer.RenderChunkInfo var56 = (LevelRenderer.RenderChunkInfo)var53.next();
                           var61 = var56.chunk.getCompiledChunk().getRenderableBlockEntities();
                        } while(var61.isEmpty());

                        Iterator var62 = var61.iterator();

                        while(var62.hasNext()) {
                           BlockEntity var68 = (BlockEntity)var62.next();
                           BlockPos var70 = var68.getBlockPos();
                           Object var72 = var38;
                           var1.pushPose();
                           var1.translate((double)var70.getX() - var12, (double)var70.getY() - var14, (double)var70.getZ() - var16);
                           SortedSet var73 = (SortedSet)this.destructionProgress.get(var70.asLong());
                           if (var73 != null && !var73.isEmpty()) {
                              var47 = ((BlockDestructionProgress)var73.last()).getProgress();
                              if (var47 >= 0) {
                                 PoseStack.Pose var48 = var1.last();
                                 SheetedDecalTextureGenerator var49 = new SheetedDecalTextureGenerator(this.renderBuffers.crumblingBufferSource().getBuffer((RenderType)ModelBakery.DESTROY_TYPES.get(var47)), var48.pose(), var48.normal());
                                 var72 = (var2x) -> {
                                    VertexConsumer var3 = var38.getBuffer(var2x);
                                    return var2x.affectsCrumbling() ? VertexMultiConsumer.create(var49, var3) : var3;
                                 };
                              }
                           }

                           this.blockEntityRenderDispatcher.render(var68, var2, var1, (MultiBufferSource)var72);
                           var1.popPose();
                        }
                     }
                  }

                  var40 = (Entity)var39.next();
               } while(!this.entityRenderDispatcher.shouldRender(var40, var20, var12, var14, var16) && !var40.hasIndirectPassenger(this.minecraft.player));
            } while(var40 == var6.getEntity() && !var6.isDetached() && (!(var6.getEntity() instanceof LivingEntity) || !((LivingEntity)var6.getEntity()).isSleeping()));
         } while(var40 instanceof LocalPlayer && var6.getEntity() != var40);

         ++this.renderedEntities;
         if (var40.tickCount == 0) {
            var40.xOld = var40.getX();
            var40.yOld = var40.getY();
            var40.zOld = var40.getZ();
         }

         Object var41;
         if (this.shouldShowEntityOutlines() && this.minecraft.shouldEntityAppearGlowing(var40)) {
            var37 = true;
            OutlineBufferSource var42 = this.renderBuffers.outlineBufferSource();
            var41 = var42;
            int var43 = var40.getTeamColor();
            boolean var44 = true;
            int var45 = var43 >> 16 & 255;
            int var46 = var43 >> 8 & 255;
            var47 = var43 & 255;
            var42.setColor(var45, var46, var47, 255);
         } else {
            var41 = var38;
         }

         this.renderEntity(var40, var12, var14, var16, var2, var1, (MultiBufferSource)var41);
      }
   }

   private void checkPoseStack(PoseStack var1) {
      if (!var1.clear()) {
         throw new IllegalStateException("Pose stack not empty");
      }
   }

   private void renderEntity(Entity var1, double var2, double var4, double var6, float var8, PoseStack var9, MultiBufferSource var10) {
      double var11 = Mth.lerp((double)var8, var1.xOld, var1.getX());
      double var13 = Mth.lerp((double)var8, var1.yOld, var1.getY());
      double var15 = Mth.lerp((double)var8, var1.zOld, var1.getZ());
      float var17 = Mth.lerp(var8, var1.yRotO, var1.getYRot());
      this.entityRenderDispatcher.render(var1, var11 - var2, var13 - var4, var15 - var6, var17, var8, var9, var10, this.entityRenderDispatcher.getPackedLightCoords(var1, var8));
   }

   private void renderChunkLayer(RenderType var1, PoseStack var2, double var3, double var5, double var7, Matrix4f var9) {
      RenderSystem.assertThread(RenderSystem::isOnRenderThread);
      var1.setupRenderState();
      if (var1 == RenderType.translucent()) {
         this.minecraft.getProfiler().push("translucent_sort");
         double var10 = var3 - this.xTransparentOld;
         double var12 = var5 - this.yTransparentOld;
         double var14 = var7 - this.zTransparentOld;
         if (var10 * var10 + var12 * var12 + var14 * var14 > 1.0D) {
            this.xTransparentOld = var3;
            this.yTransparentOld = var5;
            this.zTransparentOld = var7;
            int var16 = 0;
            ObjectListIterator var17 = this.renderChunks.iterator();

            while(var17.hasNext()) {
               LevelRenderer.RenderChunkInfo var18 = (LevelRenderer.RenderChunkInfo)var17.next();
               if (var16 < 15 && var18.chunk.resortTransparency(var1, this.chunkRenderDispatcher)) {
                  ++var16;
               }
            }
         }

         this.minecraft.getProfiler().pop();
      }

      this.minecraft.getProfiler().push("filterempty");
      this.minecraft.getProfiler().popPush(() -> {
         return "render_" + var1;
      });
      boolean var20 = var1 != RenderType.translucent();
      ObjectListIterator var11 = this.renderChunks.listIterator(var20 ? 0 : this.renderChunks.size());
      VertexFormat var21 = var1.format();
      ShaderInstance var13 = RenderSystem.getShader();
      BufferUploader.reset();

      for(int var22 = 0; var22 < 12; ++var22) {
         int var15 = RenderSystem.getShaderTexture(var22);
         var13.setSampler("Sampler" + var22, var15);
      }

      if (var13.MODEL_VIEW_MATRIX != null) {
         var13.MODEL_VIEW_MATRIX.set(var2.last().pose());
      }

      if (var13.PROJECTION_MATRIX != null) {
         var13.PROJECTION_MATRIX.set(var9);
      }

      if (var13.COLOR_MODULATOR != null) {
         var13.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
      }

      if (var13.FOG_START != null) {
         var13.FOG_START.set(RenderSystem.getShaderFogStart());
      }

      if (var13.FOG_END != null) {
         var13.FOG_END.set(RenderSystem.getShaderFogEnd());
      }

      if (var13.FOG_COLOR != null) {
         var13.FOG_COLOR.set(RenderSystem.getShaderFogColor());
      }

      if (var13.TEXTURE_MATRIX != null) {
         var13.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
      }

      if (var13.GAME_TIME != null) {
         var13.GAME_TIME.set(RenderSystem.getShaderGameTime());
      }

      RenderSystem.setupShaderLights(var13);
      var13.apply();
      Uniform var23 = var13.CHUNK_OFFSET;
      boolean var24 = false;

      while(true) {
         if (var20) {
            if (!var11.hasNext()) {
               break;
            }
         } else if (!var11.hasPrevious()) {
            break;
         }

         LevelRenderer.RenderChunkInfo var25 = var20 ? (LevelRenderer.RenderChunkInfo)var11.next() : (LevelRenderer.RenderChunkInfo)var11.previous();
         ChunkRenderDispatcher.RenderChunk var26 = var25.chunk;
         if (!var26.getCompiledChunk().isEmpty(var1)) {
            VertexBuffer var27 = var26.getBuffer(var1);
            BlockPos var19 = var26.getOrigin();
            if (var23 != null) {
               var23.set((float)((double)var19.getX() - var3), (float)((double)var19.getY() - var5), (float)((double)var19.getZ() - var7));
               var23.upload();
            }

            var27.drawChunkLayer();
            var24 = true;
         }
      }

      if (var23 != null) {
         var23.set(Vector3f.ZERO);
      }

      var13.clear();
      if (var24) {
         var21.clearBufferState();
      }

      VertexBuffer.unbind();
      VertexBuffer.unbindVertexArray();
      this.minecraft.getProfiler().pop();
      var1.clearRenderState();
   }

   private void renderDebug(Camera var1) {
      Tesselator var2 = Tesselator.getInstance();
      BufferBuilder var3 = var2.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      if (this.minecraft.chunkPath || this.minecraft.chunkVisibility) {
         double var4 = var1.getPosition().x();
         double var6 = var1.getPosition().y();
         double var8 = var1.getPosition().z();
         RenderSystem.depthMask(true);
         RenderSystem.disableCull();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableTexture();
         ObjectListIterator var10 = this.renderChunks.iterator();

         while(var10.hasNext()) {
            LevelRenderer.RenderChunkInfo var11 = (LevelRenderer.RenderChunkInfo)var10.next();
            ChunkRenderDispatcher.RenderChunk var12 = var11.chunk;
            BlockPos var13 = var12.getOrigin();
            PoseStack var14 = RenderSystem.getModelViewStack();
            var14.pushPose();
            var14.translate((double)var13.getX() - var4, (double)var13.getY() - var6, (double)var13.getZ() - var8);
            RenderSystem.applyModelViewMatrix();
            int var15;
            int var17;
            int var18;
            if (this.minecraft.chunkPath) {
               var3.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
               RenderSystem.lineWidth(10.0F);
               var15 = var11.step == 0 ? 0 : Mth.hsvToRgb((float)var11.step / 50.0F, 0.9F, 0.9F);
               int var16 = var15 >> 16 & 255;
               var17 = var15 >> 8 & 255;
               var18 = var15 & 255;

               for(int var19 = 0; var19 < DIRECTIONS.length; ++var19) {
                  if (var11.hasSourceDirection(var19)) {
                     Direction var20 = DIRECTIONS[var19];
                     var3.vertex(8.0D, 8.0D, 8.0D).color(var16, var17, var18, 255).endVertex();
                     var3.vertex((double)(8 - 16 * var20.getStepX()), (double)(8 - 16 * var20.getStepY()), (double)(8 - 16 * var20.getStepZ())).color(var16, var17, var18, 255).endVertex();
                  }
               }

               var2.end();
               RenderSystem.lineWidth(1.0F);
            }

            if (this.minecraft.chunkVisibility && !var12.getCompiledChunk().hasNoRenderableLayers()) {
               var3.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
               RenderSystem.lineWidth(10.0F);
               var15 = 0;
               Direction[] var26 = DIRECTIONS;
               var17 = var26.length;
               var18 = 0;

               while(true) {
                  if (var18 >= var17) {
                     var2.end();
                     RenderSystem.lineWidth(1.0F);
                     if (var15 > 0) {
                        var3.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                        float var27 = 0.5F;
                        float var28 = 0.2F;
                        var3.vertex(0.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(15.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(15.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(0.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(0.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(15.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(15.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(0.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(0.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(0.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(0.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(0.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(15.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(15.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(15.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(15.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(0.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(15.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(15.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(0.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(0.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(15.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(15.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var3.vertex(0.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                        var2.end();
                     }
                     break;
                  }

                  Direction var29 = var26[var18];
                  Direction[] var30 = DIRECTIONS;
                  int var21 = var30.length;

                  for(int var22 = 0; var22 < var21; ++var22) {
                     Direction var23 = var30[var22];
                     boolean var24 = var12.getCompiledChunk().facesCanSeeEachother(var29, var23);
                     if (!var24) {
                        ++var15;
                        var3.vertex((double)(8 + 8 * var29.getStepX()), (double)(8 + 8 * var29.getStepY()), (double)(8 + 8 * var29.getStepZ())).color(1, 0, 0, 1).endVertex();
                        var3.vertex((double)(8 + 8 * var23.getStepX()), (double)(8 + 8 * var23.getStepY()), (double)(8 + 8 * var23.getStepZ())).color(1, 0, 0, 1).endVertex();
                     }
                  }

                  ++var18;
               }
            }

            var14.popPose();
            RenderSystem.applyModelViewMatrix();
         }

         RenderSystem.depthMask(true);
         RenderSystem.disableBlend();
         RenderSystem.enableCull();
         RenderSystem.enableTexture();
      }

      if (this.capturedFrustum != null) {
         RenderSystem.disableCull();
         RenderSystem.disableTexture();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.lineWidth(10.0F);
         PoseStack var25 = RenderSystem.getModelViewStack();
         var25.pushPose();
         var25.translate((double)((float)(this.frustumPos.x - var1.getPosition().x)), (double)((float)(this.frustumPos.y - var1.getPosition().y)), (double)((float)(this.frustumPos.z - var1.getPosition().z)));
         RenderSystem.applyModelViewMatrix();
         RenderSystem.depthMask(true);
         var3.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         this.addFrustumQuad(var3, 0, 1, 2, 3, 0, 1, 1);
         this.addFrustumQuad(var3, 4, 5, 6, 7, 1, 0, 0);
         this.addFrustumQuad(var3, 0, 1, 5, 4, 1, 1, 0);
         this.addFrustumQuad(var3, 2, 3, 7, 6, 0, 0, 1);
         this.addFrustumQuad(var3, 0, 4, 7, 3, 0, 1, 0);
         this.addFrustumQuad(var3, 1, 5, 6, 2, 1, 0, 1);
         var2.end();
         RenderSystem.depthMask(false);
         RenderSystem.setShader(GameRenderer::getPositionShader);
         var3.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         this.addFrustumVertex(var3, 0);
         this.addFrustumVertex(var3, 1);
         this.addFrustumVertex(var3, 1);
         this.addFrustumVertex(var3, 2);
         this.addFrustumVertex(var3, 2);
         this.addFrustumVertex(var3, 3);
         this.addFrustumVertex(var3, 3);
         this.addFrustumVertex(var3, 0);
         this.addFrustumVertex(var3, 4);
         this.addFrustumVertex(var3, 5);
         this.addFrustumVertex(var3, 5);
         this.addFrustumVertex(var3, 6);
         this.addFrustumVertex(var3, 6);
         this.addFrustumVertex(var3, 7);
         this.addFrustumVertex(var3, 7);
         this.addFrustumVertex(var3, 4);
         this.addFrustumVertex(var3, 0);
         this.addFrustumVertex(var3, 4);
         this.addFrustumVertex(var3, 1);
         this.addFrustumVertex(var3, 5);
         this.addFrustumVertex(var3, 2);
         this.addFrustumVertex(var3, 6);
         this.addFrustumVertex(var3, 3);
         this.addFrustumVertex(var3, 7);
         var2.end();
         var25.popPose();
         RenderSystem.applyModelViewMatrix();
         RenderSystem.depthMask(true);
         RenderSystem.disableBlend();
         RenderSystem.enableCull();
         RenderSystem.enableTexture();
         RenderSystem.lineWidth(1.0F);
      }

   }

   private void addFrustumVertex(VertexConsumer var1, int var2) {
      var1.vertex((double)this.frustumPoints[var2].x(), (double)this.frustumPoints[var2].y(), (double)this.frustumPoints[var2].z()).endVertex();
   }

   private void addFrustumQuad(VertexConsumer var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8) {
      float var9 = 0.25F;
      var1.vertex((double)this.frustumPoints[var2].x(), (double)this.frustumPoints[var2].y(), (double)this.frustumPoints[var2].z()).color((float)var6, (float)var7, (float)var8, 0.25F).endVertex();
      var1.vertex((double)this.frustumPoints[var3].x(), (double)this.frustumPoints[var3].y(), (double)this.frustumPoints[var3].z()).color((float)var6, (float)var7, (float)var8, 0.25F).endVertex();
      var1.vertex((double)this.frustumPoints[var4].x(), (double)this.frustumPoints[var4].y(), (double)this.frustumPoints[var4].z()).color((float)var6, (float)var7, (float)var8, 0.25F).endVertex();
      var1.vertex((double)this.frustumPoints[var5].x(), (double)this.frustumPoints[var5].y(), (double)this.frustumPoints[var5].z()).color((float)var6, (float)var7, (float)var8, 0.25F).endVertex();
   }

   public void captureFrustum() {
      this.captureFrustum = true;
   }

   public void killFrustum() {
      this.capturedFrustum = null;
   }

   public void tick() {
      ++this.ticks;
      if (this.ticks % 20 == 0) {
         ObjectIterator var1 = this.destroyingBlocks.values().iterator();

         while(var1.hasNext()) {
            BlockDestructionProgress var2 = (BlockDestructionProgress)var1.next();
            int var3 = var2.getUpdatedRenderTick();
            if (this.ticks - var3 > 400) {
               var1.remove();
               this.removeProgress(var2);
            }
         }

      }
   }

   private void removeProgress(BlockDestructionProgress var1) {
      long var2 = var1.getPos().asLong();
      Set var4 = (Set)this.destructionProgress.get(var2);
      var4.remove(var1);
      if (var4.isEmpty()) {
         this.destructionProgress.remove(var2);
      }

   }

   private void renderEndSky(PoseStack var1) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderTexture(0, END_SKY_LOCATION);
      Tesselator var2 = Tesselator.getInstance();
      BufferBuilder var3 = var2.getBuilder();

      for(int var4 = 0; var4 < 6; ++var4) {
         var1.pushPose();
         if (var4 == 1) {
            var1.mulPose(Vector3f.XP.rotationDegrees(90.0F));
         }

         if (var4 == 2) {
            var1.mulPose(Vector3f.XP.rotationDegrees(-90.0F));
         }

         if (var4 == 3) {
            var1.mulPose(Vector3f.XP.rotationDegrees(180.0F));
         }

         if (var4 == 4) {
            var1.mulPose(Vector3f.ZP.rotationDegrees(90.0F));
         }

         if (var4 == 5) {
            var1.mulPose(Vector3f.ZP.rotationDegrees(-90.0F));
         }

         Matrix4f var5 = var1.last().pose();
         var3.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
         var3.vertex(var5, -100.0F, -100.0F, -100.0F).uv(0.0F, 0.0F).color(40, 40, 40, 255).endVertex();
         var3.vertex(var5, -100.0F, -100.0F, 100.0F).uv(0.0F, 16.0F).color(40, 40, 40, 255).endVertex();
         var3.vertex(var5, 100.0F, -100.0F, 100.0F).uv(16.0F, 16.0F).color(40, 40, 40, 255).endVertex();
         var3.vertex(var5, 100.0F, -100.0F, -100.0F).uv(16.0F, 0.0F).color(40, 40, 40, 255).endVertex();
         var2.end();
         var1.popPose();
      }

      RenderSystem.depthMask(true);
      RenderSystem.enableTexture();
      RenderSystem.disableBlend();
   }

   public void renderSky(PoseStack var1, Matrix4f var2, float var3, Runnable var4) {
      var4.run();
      if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.END) {
         this.renderEndSky(var1);
      } else if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL) {
         RenderSystem.disableTexture();
         Vec3 var5 = this.level.getSkyColor(this.minecraft.gameRenderer.getMainCamera().getPosition(), var3);
         float var6 = (float)var5.x;
         float var7 = (float)var5.y;
         float var8 = (float)var5.z;
         FogRenderer.levelFogColor();
         BufferBuilder var9 = Tesselator.getInstance().getBuilder();
         RenderSystem.depthMask(false);
         RenderSystem.setShaderColor(var6, var7, var8, 1.0F);
         ShaderInstance var10 = RenderSystem.getShader();
         this.skyBuffer.drawWithShader(var1.last().pose(), var2, var10);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         float[] var11 = this.level.effects().getSunriseColor(this.level.getTimeOfDay(var3), var3);
         float var12;
         float var14;
         float var19;
         float var20;
         float var21;
         if (var11 != null) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.disableTexture();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            var1.pushPose();
            var1.mulPose(Vector3f.XP.rotationDegrees(90.0F));
            var12 = Mth.sin(this.level.getSunAngle(var3)) < 0.0F ? 180.0F : 0.0F;
            var1.mulPose(Vector3f.ZP.rotationDegrees(var12));
            var1.mulPose(Vector3f.ZP.rotationDegrees(90.0F));
            float var13 = var11[0];
            var14 = var11[1];
            float var15 = var11[2];
            Matrix4f var16 = var1.last().pose();
            var9.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            var9.vertex(var16, 0.0F, 100.0F, 0.0F).color(var13, var14, var15, var11[3]).endVertex();
            boolean var17 = true;

            for(int var18 = 0; var18 <= 16; ++var18) {
               var19 = (float)var18 * 6.2831855F / 16.0F;
               var20 = Mth.sin(var19);
               var21 = Mth.cos(var19);
               var9.vertex(var16, var20 * 120.0F, var21 * 120.0F, -var21 * 40.0F * var11[3]).color(var11[0], var11[1], var11[2], 0.0F).endVertex();
            }

            var9.end();
            BufferUploader.end(var9);
            var1.popPose();
         }

         RenderSystem.enableTexture();
         RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
         var1.pushPose();
         var12 = 1.0F - this.level.getRainLevel(var3);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, var12);
         var1.mulPose(Vector3f.YP.rotationDegrees(-90.0F));
         var1.mulPose(Vector3f.XP.rotationDegrees(this.level.getTimeOfDay(var3) * 360.0F));
         Matrix4f var24 = var1.last().pose();
         var14 = 30.0F;
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderTexture(0, SUN_LOCATION);
         var9.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         var9.vertex(var24, -var14, 100.0F, -var14).uv(0.0F, 0.0F).endVertex();
         var9.vertex(var24, var14, 100.0F, -var14).uv(1.0F, 0.0F).endVertex();
         var9.vertex(var24, var14, 100.0F, var14).uv(1.0F, 1.0F).endVertex();
         var9.vertex(var24, -var14, 100.0F, var14).uv(0.0F, 1.0F).endVertex();
         var9.end();
         BufferUploader.end(var9);
         var14 = 20.0F;
         RenderSystem.setShaderTexture(0, MOON_LOCATION);
         int var25 = this.level.getMoonPhase();
         int var26 = var25 % 4;
         int var27 = var25 / 4 % 2;
         float var28 = (float)(var26 + 0) / 4.0F;
         var19 = (float)(var27 + 0) / 2.0F;
         var20 = (float)(var26 + 1) / 4.0F;
         var21 = (float)(var27 + 1) / 2.0F;
         var9.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         var9.vertex(var24, -var14, -100.0F, var14).uv(var20, var21).endVertex();
         var9.vertex(var24, var14, -100.0F, var14).uv(var28, var21).endVertex();
         var9.vertex(var24, var14, -100.0F, -var14).uv(var28, var19).endVertex();
         var9.vertex(var24, -var14, -100.0F, -var14).uv(var20, var19).endVertex();
         var9.end();
         BufferUploader.end(var9);
         RenderSystem.disableTexture();
         float var22 = this.level.getStarBrightness(var3) * var12;
         if (var22 > 0.0F) {
            RenderSystem.setShaderColor(var22, var22, var22, var22);
            FogRenderer.setupNoFog();
            this.starBuffer.drawWithShader(var1.last().pose(), var2, GameRenderer.getPositionShader());
            var4.run();
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
         var1.popPose();
         RenderSystem.disableTexture();
         RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
         double var23 = this.minecraft.player.getEyePosition(var3).y - this.level.getLevelData().getHorizonHeight(this.level);
         if (var23 < 0.0D) {
            var1.pushPose();
            var1.translate(0.0D, 12.0D, 0.0D);
            this.darkBuffer.drawWithShader(var1.last().pose(), var2, var10);
            var1.popPose();
         }

         if (this.level.effects().hasGround()) {
            RenderSystem.setShaderColor(var6 * 0.2F + 0.04F, var7 * 0.2F + 0.04F, var8 * 0.6F + 0.1F, 1.0F);
         } else {
            RenderSystem.setShaderColor(var6, var7, var8, 1.0F);
         }

         RenderSystem.enableTexture();
         RenderSystem.depthMask(true);
      }
   }

   public void renderClouds(PoseStack var1, Matrix4f var2, float var3, double var4, double var6, double var8) {
      float var10 = this.level.effects().getCloudHeight();
      if (!Float.isNaN(var10)) {
         RenderSystem.disableCull();
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.depthMask(true);
         float var11 = 12.0F;
         float var12 = 4.0F;
         double var13 = 2.0E-4D;
         double var15 = (double)(((float)this.ticks + var3) * 0.03F);
         double var17 = (var4 + var15) / 12.0D;
         double var19 = (double)(var10 - (float)var6 + 0.33F);
         double var21 = var8 / 12.0D + 0.33000001311302185D;
         var17 -= (double)(Mth.floor(var17 / 2048.0D) * 2048);
         var21 -= (double)(Mth.floor(var21 / 2048.0D) * 2048);
         float var23 = (float)(var17 - (double)Mth.floor(var17));
         float var24 = (float)(var19 / 4.0D - (double)Mth.floor(var19 / 4.0D)) * 4.0F;
         float var25 = (float)(var21 - (double)Mth.floor(var21));
         Vec3 var26 = this.level.getCloudColor(var3);
         int var27 = (int)Math.floor(var17);
         int var28 = (int)Math.floor(var19 / 4.0D);
         int var29 = (int)Math.floor(var21);
         if (var27 != this.prevCloudX || var28 != this.prevCloudY || var29 != this.prevCloudZ || this.minecraft.options.getCloudsType() != this.prevCloudsType || this.prevCloudColor.distanceToSqr(var26) > 2.0E-4D) {
            this.prevCloudX = var27;
            this.prevCloudY = var28;
            this.prevCloudZ = var29;
            this.prevCloudColor = var26;
            this.prevCloudsType = this.minecraft.options.getCloudsType();
            this.generateClouds = true;
         }

         if (this.generateClouds) {
            this.generateClouds = false;
            BufferBuilder var30 = Tesselator.getInstance().getBuilder();
            if (this.cloudBuffer != null) {
               this.cloudBuffer.close();
            }

            this.cloudBuffer = new VertexBuffer();
            this.buildClouds(var30, var17, var19, var21, var26);
            var30.end();
            this.cloudBuffer.upload(var30);
         }

         RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
         RenderSystem.setShaderTexture(0, CLOUDS_LOCATION);
         FogRenderer.levelFogColor();
         var1.pushPose();
         var1.scale(12.0F, 1.0F, 12.0F);
         var1.translate((double)(-var23), (double)var24, (double)(-var25));
         if (this.cloudBuffer != null) {
            int var33 = this.prevCloudsType == CloudStatus.FANCY ? 0 : 1;

            for(int var31 = var33; var31 < 2; ++var31) {
               if (var31 == 0) {
                  RenderSystem.colorMask(false, false, false, false);
               } else {
                  RenderSystem.colorMask(true, true, true, true);
               }

               ShaderInstance var32 = RenderSystem.getShader();
               this.cloudBuffer.drawWithShader(var1.last().pose(), var2, var32);
            }
         }

         var1.popPose();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   private void buildClouds(BufferBuilder var1, double var2, double var4, double var6, Vec3 var8) {
      float var9 = 4.0F;
      float var10 = 0.00390625F;
      boolean var11 = true;
      boolean var12 = true;
      float var13 = 9.765625E-4F;
      float var14 = (float)Mth.floor(var2) * 0.00390625F;
      float var15 = (float)Mth.floor(var6) * 0.00390625F;
      float var16 = (float)var8.x;
      float var17 = (float)var8.y;
      float var18 = (float)var8.z;
      float var19 = var16 * 0.9F;
      float var20 = var17 * 0.9F;
      float var21 = var18 * 0.9F;
      float var22 = var16 * 0.7F;
      float var23 = var17 * 0.7F;
      float var24 = var18 * 0.7F;
      float var25 = var16 * 0.8F;
      float var26 = var17 * 0.8F;
      float var27 = var18 * 0.8F;
      RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
      var1.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
      float var28 = (float)Math.floor(var4 / 4.0D) * 4.0F;
      if (this.prevCloudsType == CloudStatus.FANCY) {
         for(int var29 = -3; var29 <= 4; ++var29) {
            for(int var30 = -3; var30 <= 4; ++var30) {
               float var31 = (float)(var29 * 8);
               float var32 = (float)(var30 * 8);
               if (var28 > -5.0F) {
                  var1.vertex((double)(var31 + 0.0F), (double)(var28 + 0.0F), (double)(var32 + 8.0F)).uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15).color(var22, var23, var24, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
                  var1.vertex((double)(var31 + 8.0F), (double)(var28 + 0.0F), (double)(var32 + 8.0F)).uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15).color(var22, var23, var24, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
                  var1.vertex((double)(var31 + 8.0F), (double)(var28 + 0.0F), (double)(var32 + 0.0F)).uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15).color(var22, var23, var24, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
                  var1.vertex((double)(var31 + 0.0F), (double)(var28 + 0.0F), (double)(var32 + 0.0F)).uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15).color(var22, var23, var24, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
               }

               if (var28 <= 5.0F) {
                  var1.vertex((double)(var31 + 0.0F), (double)(var28 + 4.0F - 9.765625E-4F), (double)(var32 + 8.0F)).uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15).color(var16, var17, var18, 0.8F).normal(0.0F, 1.0F, 0.0F).endVertex();
                  var1.vertex((double)(var31 + 8.0F), (double)(var28 + 4.0F - 9.765625E-4F), (double)(var32 + 8.0F)).uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15).color(var16, var17, var18, 0.8F).normal(0.0F, 1.0F, 0.0F).endVertex();
                  var1.vertex((double)(var31 + 8.0F), (double)(var28 + 4.0F - 9.765625E-4F), (double)(var32 + 0.0F)).uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15).color(var16, var17, var18, 0.8F).normal(0.0F, 1.0F, 0.0F).endVertex();
                  var1.vertex((double)(var31 + 0.0F), (double)(var28 + 4.0F - 9.765625E-4F), (double)(var32 + 0.0F)).uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15).color(var16, var17, var18, 0.8F).normal(0.0F, 1.0F, 0.0F).endVertex();
               }

               int var33;
               if (var29 > -1) {
                  for(var33 = 0; var33 < 8; ++var33) {
                     var1.vertex((double)(var31 + (float)var33 + 0.0F), (double)(var28 + 0.0F), (double)(var32 + 8.0F)).uv((var31 + (float)var33 + 0.5F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15).color(var19, var20, var21, 0.8F).normal(-1.0F, 0.0F, 0.0F).endVertex();
                     var1.vertex((double)(var31 + (float)var33 + 0.0F), (double)(var28 + 4.0F), (double)(var32 + 8.0F)).uv((var31 + (float)var33 + 0.5F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15).color(var19, var20, var21, 0.8F).normal(-1.0F, 0.0F, 0.0F).endVertex();
                     var1.vertex((double)(var31 + (float)var33 + 0.0F), (double)(var28 + 4.0F), (double)(var32 + 0.0F)).uv((var31 + (float)var33 + 0.5F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15).color(var19, var20, var21, 0.8F).normal(-1.0F, 0.0F, 0.0F).endVertex();
                     var1.vertex((double)(var31 + (float)var33 + 0.0F), (double)(var28 + 0.0F), (double)(var32 + 0.0F)).uv((var31 + (float)var33 + 0.5F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15).color(var19, var20, var21, 0.8F).normal(-1.0F, 0.0F, 0.0F).endVertex();
                  }
               }

               if (var29 <= 1) {
                  for(var33 = 0; var33 < 8; ++var33) {
                     var1.vertex((double)(var31 + (float)var33 + 1.0F - 9.765625E-4F), (double)(var28 + 0.0F), (double)(var32 + 8.0F)).uv((var31 + (float)var33 + 0.5F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15).color(var19, var20, var21, 0.8F).normal(1.0F, 0.0F, 0.0F).endVertex();
                     var1.vertex((double)(var31 + (float)var33 + 1.0F - 9.765625E-4F), (double)(var28 + 4.0F), (double)(var32 + 8.0F)).uv((var31 + (float)var33 + 0.5F) * 0.00390625F + var14, (var32 + 8.0F) * 0.00390625F + var15).color(var19, var20, var21, 0.8F).normal(1.0F, 0.0F, 0.0F).endVertex();
                     var1.vertex((double)(var31 + (float)var33 + 1.0F - 9.765625E-4F), (double)(var28 + 4.0F), (double)(var32 + 0.0F)).uv((var31 + (float)var33 + 0.5F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15).color(var19, var20, var21, 0.8F).normal(1.0F, 0.0F, 0.0F).endVertex();
                     var1.vertex((double)(var31 + (float)var33 + 1.0F - 9.765625E-4F), (double)(var28 + 0.0F), (double)(var32 + 0.0F)).uv((var31 + (float)var33 + 0.5F) * 0.00390625F + var14, (var32 + 0.0F) * 0.00390625F + var15).color(var19, var20, var21, 0.8F).normal(1.0F, 0.0F, 0.0F).endVertex();
                  }
               }

               if (var30 > -1) {
                  for(var33 = 0; var33 < 8; ++var33) {
                     var1.vertex((double)(var31 + 0.0F), (double)(var28 + 4.0F), (double)(var32 + (float)var33 + 0.0F)).uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + (float)var33 + 0.5F) * 0.00390625F + var15).color(var25, var26, var27, 0.8F).normal(0.0F, 0.0F, -1.0F).endVertex();
                     var1.vertex((double)(var31 + 8.0F), (double)(var28 + 4.0F), (double)(var32 + (float)var33 + 0.0F)).uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + (float)var33 + 0.5F) * 0.00390625F + var15).color(var25, var26, var27, 0.8F).normal(0.0F, 0.0F, -1.0F).endVertex();
                     var1.vertex((double)(var31 + 8.0F), (double)(var28 + 0.0F), (double)(var32 + (float)var33 + 0.0F)).uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + (float)var33 + 0.5F) * 0.00390625F + var15).color(var25, var26, var27, 0.8F).normal(0.0F, 0.0F, -1.0F).endVertex();
                     var1.vertex((double)(var31 + 0.0F), (double)(var28 + 0.0F), (double)(var32 + (float)var33 + 0.0F)).uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + (float)var33 + 0.5F) * 0.00390625F + var15).color(var25, var26, var27, 0.8F).normal(0.0F, 0.0F, -1.0F).endVertex();
                  }
               }

               if (var30 <= 1) {
                  for(var33 = 0; var33 < 8; ++var33) {
                     var1.vertex((double)(var31 + 0.0F), (double)(var28 + 4.0F), (double)(var32 + (float)var33 + 1.0F - 9.765625E-4F)).uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + (float)var33 + 0.5F) * 0.00390625F + var15).color(var25, var26, var27, 0.8F).normal(0.0F, 0.0F, 1.0F).endVertex();
                     var1.vertex((double)(var31 + 8.0F), (double)(var28 + 4.0F), (double)(var32 + (float)var33 + 1.0F - 9.765625E-4F)).uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + (float)var33 + 0.5F) * 0.00390625F + var15).color(var25, var26, var27, 0.8F).normal(0.0F, 0.0F, 1.0F).endVertex();
                     var1.vertex((double)(var31 + 8.0F), (double)(var28 + 0.0F), (double)(var32 + (float)var33 + 1.0F - 9.765625E-4F)).uv((var31 + 8.0F) * 0.00390625F + var14, (var32 + (float)var33 + 0.5F) * 0.00390625F + var15).color(var25, var26, var27, 0.8F).normal(0.0F, 0.0F, 1.0F).endVertex();
                     var1.vertex((double)(var31 + 0.0F), (double)(var28 + 0.0F), (double)(var32 + (float)var33 + 1.0F - 9.765625E-4F)).uv((var31 + 0.0F) * 0.00390625F + var14, (var32 + (float)var33 + 0.5F) * 0.00390625F + var15).color(var25, var26, var27, 0.8F).normal(0.0F, 0.0F, 1.0F).endVertex();
                  }
               }
            }
         }
      } else {
         boolean var34 = true;
         boolean var35 = true;

         for(int var36 = -32; var36 < 32; var36 += 32) {
            for(int var37 = -32; var37 < 32; var37 += 32) {
               var1.vertex((double)(var36 + 0), (double)var28, (double)(var37 + 32)).uv((float)(var36 + 0) * 0.00390625F + var14, (float)(var37 + 32) * 0.00390625F + var15).color(var16, var17, var18, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
               var1.vertex((double)(var36 + 32), (double)var28, (double)(var37 + 32)).uv((float)(var36 + 32) * 0.00390625F + var14, (float)(var37 + 32) * 0.00390625F + var15).color(var16, var17, var18, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
               var1.vertex((double)(var36 + 32), (double)var28, (double)(var37 + 0)).uv((float)(var36 + 32) * 0.00390625F + var14, (float)(var37 + 0) * 0.00390625F + var15).color(var16, var17, var18, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
               var1.vertex((double)(var36 + 0), (double)var28, (double)(var37 + 0)).uv((float)(var36 + 0) * 0.00390625F + var14, (float)(var37 + 0) * 0.00390625F + var15).color(var16, var17, var18, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
            }
         }
      }

   }

   private void compileChunksUntil(long var1) {
      this.needsUpdate |= this.chunkRenderDispatcher.uploadAllPendingUploads();
      long var3 = Util.getNanos();
      int var5 = 0;
      if (!this.chunksToCompile.isEmpty()) {
         Iterator var6 = this.chunksToCompile.iterator();

         while(var6.hasNext()) {
            ChunkRenderDispatcher.RenderChunk var7 = (ChunkRenderDispatcher.RenderChunk)var6.next();
            if (var7.isDirtyFromPlayer()) {
               this.chunkRenderDispatcher.rebuildChunkSync(var7);
            } else {
               var7.rebuildChunkAsync(this.chunkRenderDispatcher);
            }

            var7.setNotDirty();
            var6.remove();
            ++var5;
            long var8 = Util.getNanos();
            long var10 = var8 - var3;
            long var12 = var10 / (long)var5;
            long var14 = var1 - var8;
            if (var14 < var12) {
               break;
            }
         }
      }

   }

   private void renderWorldBorder(Camera var1) {
      BufferBuilder var2 = Tesselator.getInstance().getBuilder();
      WorldBorder var3 = this.level.getWorldBorder();
      double var4 = (double)(this.minecraft.options.renderDistance * 16);
      if (!(var1.getPosition().x < var3.getMaxX() - var4) || !(var1.getPosition().x > var3.getMinX() + var4) || !(var1.getPosition().z < var3.getMaxZ() - var4) || !(var1.getPosition().z > var3.getMinZ() + var4)) {
         double var6 = 1.0D - var3.getDistanceToBorder(var1.getPosition().x, var1.getPosition().z) / var4;
         var6 = Math.pow(var6, 4.0D);
         var6 = Mth.clamp(var6, 0.0D, 1.0D);
         double var8 = var1.getPosition().x;
         double var10 = var1.getPosition().z;
         double var12 = (double)this.minecraft.gameRenderer.getDepthFar();
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
         RenderSystem.setShaderTexture(0, FORCEFIELD_LOCATION);
         RenderSystem.depthMask(Minecraft.useShaderTransparency());
         PoseStack var14 = RenderSystem.getModelViewStack();
         var14.pushPose();
         RenderSystem.applyModelViewMatrix();
         int var15 = var3.getStatus().getColor();
         float var16 = (float)(var15 >> 16 & 255) / 255.0F;
         float var17 = (float)(var15 >> 8 & 255) / 255.0F;
         float var18 = (float)(var15 & 255) / 255.0F;
         RenderSystem.setShaderColor(var16, var17, var18, (float)var6);
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.polygonOffset(-3.0F, -3.0F);
         RenderSystem.enablePolygonOffset();
         RenderSystem.disableCull();
         float var19 = (float)(Util.getMillis() % 3000L) / 3000.0F;
         float var20 = 0.0F;
         float var21 = 0.0F;
         float var22 = (float)(var12 - Mth.frac(var1.getPosition().y));
         var2.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         double var23 = Math.max((double)Mth.floor(var10 - var4), var3.getMinZ());
         double var25 = Math.min((double)Mth.ceil(var10 + var4), var3.getMaxZ());
         float var32;
         float var27;
         double var28;
         double var30;
         if (var8 > var3.getMaxX() - var4) {
            var27 = 0.0F;

            for(var28 = var23; var28 < var25; var27 += 0.5F) {
               var30 = Math.min(1.0D, var25 - var28);
               var32 = (float)var30 * 0.5F;
               var2.vertex(var3.getMaxX() - var8, -var12, var28 - var10).uv(var19 - var27, var19 + var22).endVertex();
               var2.vertex(var3.getMaxX() - var8, -var12, var28 + var30 - var10).uv(var19 - (var32 + var27), var19 + var22).endVertex();
               var2.vertex(var3.getMaxX() - var8, var12, var28 + var30 - var10).uv(var19 - (var32 + var27), var19 + 0.0F).endVertex();
               var2.vertex(var3.getMaxX() - var8, var12, var28 - var10).uv(var19 - var27, var19 + 0.0F).endVertex();
               ++var28;
            }
         }

         if (var8 < var3.getMinX() + var4) {
            var27 = 0.0F;

            for(var28 = var23; var28 < var25; var27 += 0.5F) {
               var30 = Math.min(1.0D, var25 - var28);
               var32 = (float)var30 * 0.5F;
               var2.vertex(var3.getMinX() - var8, -var12, var28 - var10).uv(var19 + var27, var19 + var22).endVertex();
               var2.vertex(var3.getMinX() - var8, -var12, var28 + var30 - var10).uv(var19 + var32 + var27, var19 + var22).endVertex();
               var2.vertex(var3.getMinX() - var8, var12, var28 + var30 - var10).uv(var19 + var32 + var27, var19 + 0.0F).endVertex();
               var2.vertex(var3.getMinX() - var8, var12, var28 - var10).uv(var19 + var27, var19 + 0.0F).endVertex();
               ++var28;
            }
         }

         var23 = Math.max((double)Mth.floor(var8 - var4), var3.getMinX());
         var25 = Math.min((double)Mth.ceil(var8 + var4), var3.getMaxX());
         if (var10 > var3.getMaxZ() - var4) {
            var27 = 0.0F;

            for(var28 = var23; var28 < var25; var27 += 0.5F) {
               var30 = Math.min(1.0D, var25 - var28);
               var32 = (float)var30 * 0.5F;
               var2.vertex(var28 - var8, -var12, var3.getMaxZ() - var10).uv(var19 + var27, var19 + var22).endVertex();
               var2.vertex(var28 + var30 - var8, -var12, var3.getMaxZ() - var10).uv(var19 + var32 + var27, var19 + var22).endVertex();
               var2.vertex(var28 + var30 - var8, var12, var3.getMaxZ() - var10).uv(var19 + var32 + var27, var19 + 0.0F).endVertex();
               var2.vertex(var28 - var8, var12, var3.getMaxZ() - var10).uv(var19 + var27, var19 + 0.0F).endVertex();
               ++var28;
            }
         }

         if (var10 < var3.getMinZ() + var4) {
            var27 = 0.0F;

            for(var28 = var23; var28 < var25; var27 += 0.5F) {
               var30 = Math.min(1.0D, var25 - var28);
               var32 = (float)var30 * 0.5F;
               var2.vertex(var28 - var8, -var12, var3.getMinZ() - var10).uv(var19 - var27, var19 + var22).endVertex();
               var2.vertex(var28 + var30 - var8, -var12, var3.getMinZ() - var10).uv(var19 - (var32 + var27), var19 + var22).endVertex();
               var2.vertex(var28 + var30 - var8, var12, var3.getMinZ() - var10).uv(var19 - (var32 + var27), var19 + 0.0F).endVertex();
               var2.vertex(var28 - var8, var12, var3.getMinZ() - var10).uv(var19 - var27, var19 + 0.0F).endVertex();
               ++var28;
            }
         }

         var2.end();
         BufferUploader.end(var2);
         RenderSystem.enableCull();
         RenderSystem.polygonOffset(0.0F, 0.0F);
         RenderSystem.disablePolygonOffset();
         RenderSystem.disableBlend();
         var14.popPose();
         RenderSystem.applyModelViewMatrix();
         RenderSystem.depthMask(true);
      }
   }

   private void renderHitOutline(PoseStack var1, VertexConsumer var2, Entity var3, double var4, double var6, double var8, BlockPos var10, BlockState var11) {
      renderShape(var1, var2, var11.getShape(this.level, var10, CollisionContext.of(var3)), (double)var10.getX() - var4, (double)var10.getY() - var6, (double)var10.getZ() - var8, 0.0F, 0.0F, 0.0F, 0.4F);
   }

   public static void renderVoxelShape(PoseStack var0, VertexConsumer var1, VoxelShape var2, double var3, double var5, double var7, float var9, float var10, float var11, float var12) {
      List var13 = var2.toAabbs();
      int var14 = Mth.ceil((double)var13.size() / 3.0D);

      for(int var15 = 0; var15 < var13.size(); ++var15) {
         AABB var16 = (AABB)var13.get(var15);
         float var17 = ((float)var15 % (float)var14 + 1.0F) / (float)var14;
         float var18 = (float)(var15 / var14);
         float var19 = var17 * (float)(var18 == 0.0F ? 1 : 0);
         float var20 = var17 * (float)(var18 == 1.0F ? 1 : 0);
         float var21 = var17 * (float)(var18 == 2.0F ? 1 : 0);
         renderShape(var0, var1, Shapes.create(var16.move(0.0D, 0.0D, 0.0D)), var3, var5, var7, var19, var20, var21, 1.0F);
      }

   }

   private static void renderShape(PoseStack var0, VertexConsumer var1, VoxelShape var2, double var3, double var5, double var7, float var9, float var10, float var11, float var12) {
      PoseStack.Pose var13 = var0.last();
      var2.forAllEdges((var12x, var14, var16, var18, var20, var22) -> {
         float var24 = (float)(var18 - var12x);
         float var25 = (float)(var20 - var14);
         float var26 = (float)(var22 - var16);
         float var27 = Mth.sqrt(var24 * var24 + var25 * var25 + var26 * var26);
         var24 /= var27;
         var25 /= var27;
         var26 /= var27;
         var1.vertex(var13.pose(), (float)(var12x + var3), (float)(var14 + var5), (float)(var16 + var7)).color(var9, var10, var11, var12).normal(var13.normal(), var24, var25, var26).endVertex();
         var1.vertex(var13.pose(), (float)(var18 + var3), (float)(var20 + var5), (float)(var22 + var7)).color(var9, var10, var11, var12).normal(var13.normal(), var24, var25, var26).endVertex();
      });
   }

   public static void renderLineBox(VertexConsumer var0, double var1, double var3, double var5, double var7, double var9, double var11, float var13, float var14, float var15, float var16) {
      renderLineBox(new PoseStack(), var0, var1, var3, var5, var7, var9, var11, var13, var14, var15, var16, var13, var14, var15);
   }

   public static void renderLineBox(PoseStack var0, VertexConsumer var1, AABB var2, float var3, float var4, float var5, float var6) {
      renderLineBox(var0, var1, var2.minX, var2.minY, var2.minZ, var2.maxX, var2.maxY, var2.maxZ, var3, var4, var5, var6, var3, var4, var5);
   }

   public static void renderLineBox(PoseStack var0, VertexConsumer var1, double var2, double var4, double var6, double var8, double var10, double var12, float var14, float var15, float var16, float var17) {
      renderLineBox(var0, var1, var2, var4, var6, var8, var10, var12, var14, var15, var16, var17, var14, var15, var16);
   }

   public static void renderLineBox(PoseStack var0, VertexConsumer var1, double var2, double var4, double var6, double var8, double var10, double var12, float var14, float var15, float var16, float var17, float var18, float var19, float var20) {
      Matrix4f var21 = var0.last().pose();
      Matrix3f var22 = var0.last().normal();
      float var23 = (float)var2;
      float var24 = (float)var4;
      float var25 = (float)var6;
      float var26 = (float)var8;
      float var27 = (float)var10;
      float var28 = (float)var12;
      var1.vertex(var21, var23, var24, var25).color(var14, var19, var20, var17).normal(var22, 1.0F, 0.0F, 0.0F).endVertex();
      var1.vertex(var21, var26, var24, var25).color(var14, var19, var20, var17).normal(var22, 1.0F, 0.0F, 0.0F).endVertex();
      var1.vertex(var21, var23, var24, var25).color(var18, var15, var20, var17).normal(var22, 0.0F, 1.0F, 0.0F).endVertex();
      var1.vertex(var21, var23, var27, var25).color(var18, var15, var20, var17).normal(var22, 0.0F, 1.0F, 0.0F).endVertex();
      var1.vertex(var21, var23, var24, var25).color(var18, var19, var16, var17).normal(var22, 0.0F, 0.0F, 1.0F).endVertex();
      var1.vertex(var21, var23, var24, var28).color(var18, var19, var16, var17).normal(var22, 0.0F, 0.0F, 1.0F).endVertex();
      var1.vertex(var21, var26, var24, var25).color(var14, var15, var16, var17).normal(var22, 0.0F, 1.0F, 0.0F).endVertex();
      var1.vertex(var21, var26, var27, var25).color(var14, var15, var16, var17).normal(var22, 0.0F, 1.0F, 0.0F).endVertex();
      var1.vertex(var21, var26, var27, var25).color(var14, var15, var16, var17).normal(var22, -1.0F, 0.0F, 0.0F).endVertex();
      var1.vertex(var21, var23, var27, var25).color(var14, var15, var16, var17).normal(var22, -1.0F, 0.0F, 0.0F).endVertex();
      var1.vertex(var21, var23, var27, var25).color(var14, var15, var16, var17).normal(var22, 0.0F, 0.0F, 1.0F).endVertex();
      var1.vertex(var21, var23, var27, var28).color(var14, var15, var16, var17).normal(var22, 0.0F, 0.0F, 1.0F).endVertex();
      var1.vertex(var21, var23, var27, var28).color(var14, var15, var16, var17).normal(var22, 0.0F, -1.0F, 0.0F).endVertex();
      var1.vertex(var21, var23, var24, var28).color(var14, var15, var16, var17).normal(var22, 0.0F, -1.0F, 0.0F).endVertex();
      var1.vertex(var21, var23, var24, var28).color(var14, var15, var16, var17).normal(var22, 1.0F, 0.0F, 0.0F).endVertex();
      var1.vertex(var21, var26, var24, var28).color(var14, var15, var16, var17).normal(var22, 1.0F, 0.0F, 0.0F).endVertex();
      var1.vertex(var21, var26, var24, var28).color(var14, var15, var16, var17).normal(var22, 0.0F, 0.0F, -1.0F).endVertex();
      var1.vertex(var21, var26, var24, var25).color(var14, var15, var16, var17).normal(var22, 0.0F, 0.0F, -1.0F).endVertex();
      var1.vertex(var21, var23, var27, var28).color(var14, var15, var16, var17).normal(var22, 1.0F, 0.0F, 0.0F).endVertex();
      var1.vertex(var21, var26, var27, var28).color(var14, var15, var16, var17).normal(var22, 1.0F, 0.0F, 0.0F).endVertex();
      var1.vertex(var21, var26, var24, var28).color(var14, var15, var16, var17).normal(var22, 0.0F, 1.0F, 0.0F).endVertex();
      var1.vertex(var21, var26, var27, var28).color(var14, var15, var16, var17).normal(var22, 0.0F, 1.0F, 0.0F).endVertex();
      var1.vertex(var21, var26, var27, var25).color(var14, var15, var16, var17).normal(var22, 0.0F, 0.0F, 1.0F).endVertex();
      var1.vertex(var21, var26, var27, var28).color(var14, var15, var16, var17).normal(var22, 0.0F, 0.0F, 1.0F).endVertex();
   }

   public static void addChainedFilledBoxVertices(BufferBuilder var0, double var1, double var3, double var5, double var7, double var9, double var11, float var13, float var14, float var15, float var16) {
      var0.vertex(var1, var3, var5).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var1, var3, var5).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var1, var3, var5).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var1, var3, var11).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var1, var9, var5).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var1, var9, var11).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var1, var9, var11).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var1, var3, var11).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var7, var9, var11).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var7, var3, var11).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var7, var3, var11).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var7, var3, var5).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var7, var9, var11).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var7, var9, var5).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var7, var9, var5).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var7, var3, var5).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var1, var9, var5).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var1, var3, var5).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var1, var3, var5).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var7, var3, var5).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var1, var3, var11).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var7, var3, var11).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var7, var3, var11).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var1, var9, var5).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var1, var9, var5).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var1, var9, var11).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var7, var9, var5).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var7, var9, var11).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var7, var9, var11).color(var13, var14, var15, var16).endVertex();
      var0.vertex(var7, var9, var11).color(var13, var14, var15, var16).endVertex();
   }

   public void blockChanged(BlockGetter var1, BlockPos var2, BlockState var3, BlockState var4, int var5) {
      this.setBlockDirty(var2, (var5 & 8) != 0);
   }

   private void setBlockDirty(BlockPos var1, boolean var2) {
      for(int var3 = var1.getZ() - 1; var3 <= var1.getZ() + 1; ++var3) {
         for(int var4 = var1.getX() - 1; var4 <= var1.getX() + 1; ++var4) {
            for(int var5 = var1.getY() - 1; var5 <= var1.getY() + 1; ++var5) {
               this.setSectionDirty(SectionPos.blockToSectionCoord(var4), SectionPos.blockToSectionCoord(var5), SectionPos.blockToSectionCoord(var3), var2);
            }
         }
      }

   }

   public void setBlocksDirty(int var1, int var2, int var3, int var4, int var5, int var6) {
      for(int var7 = var3 - 1; var7 <= var6 + 1; ++var7) {
         for(int var8 = var1 - 1; var8 <= var4 + 1; ++var8) {
            for(int var9 = var2 - 1; var9 <= var5 + 1; ++var9) {
               this.setSectionDirty(SectionPos.blockToSectionCoord(var8), SectionPos.blockToSectionCoord(var9), SectionPos.blockToSectionCoord(var7));
            }
         }
      }

   }

   public void setBlockDirty(BlockPos var1, BlockState var2, BlockState var3) {
      if (this.minecraft.getModelManager().requiresRender(var2, var3)) {
         this.setBlocksDirty(var1.getX(), var1.getY(), var1.getZ(), var1.getX(), var1.getY(), var1.getZ());
      }

   }

   public void setSectionDirtyWithNeighbors(int var1, int var2, int var3) {
      for(int var4 = var3 - 1; var4 <= var3 + 1; ++var4) {
         for(int var5 = var1 - 1; var5 <= var1 + 1; ++var5) {
            for(int var6 = var2 - 1; var6 <= var2 + 1; ++var6) {
               this.setSectionDirty(var5, var6, var4);
            }
         }
      }

   }

   public void setSectionDirty(int var1, int var2, int var3) {
      this.setSectionDirty(var1, var2, var3, false);
   }

   private void setSectionDirty(int var1, int var2, int var3, boolean var4) {
      this.viewArea.setDirty(var1, var2, var3, var4);
   }

   public void playStreamingMusic(@Nullable SoundEvent var1, BlockPos var2) {
      SoundInstance var3 = (SoundInstance)this.playingRecords.get(var2);
      if (var3 != null) {
         this.minecraft.getSoundManager().stop(var3);
         this.playingRecords.remove(var2);
      }

      if (var1 != null) {
         RecordItem var4 = RecordItem.getBySound(var1);
         if (var4 != null) {
            this.minecraft.gui.setNowPlaying(var4.getDisplayName());
         }

         SimpleSoundInstance var5 = SimpleSoundInstance.forRecord(var1, (double)var2.getX(), (double)var2.getY(), (double)var2.getZ());
         this.playingRecords.put(var2, var5);
         this.minecraft.getSoundManager().play(var5);
      }

      this.notifyNearbyEntities(this.level, var2, var1 != null);
   }

   private void notifyNearbyEntities(Level var1, BlockPos var2, boolean var3) {
      List var4 = var1.getEntitiesOfClass(LivingEntity.class, (new AABB(var2)).inflate(3.0D));
      Iterator var5 = var4.iterator();

      while(var5.hasNext()) {
         LivingEntity var6 = (LivingEntity)var5.next();
         var6.setRecordPlayingNearby(var2, var3);
      }

   }

   public void addParticle(ParticleOptions var1, boolean var2, double var3, double var5, double var7, double var9, double var11, double var13) {
      this.addParticle(var1, var2, false, var3, var5, var7, var9, var11, var13);
   }

   public void addParticle(ParticleOptions var1, boolean var2, boolean var3, double var4, double var6, double var8, double var10, double var12, double var14) {
      try {
         this.addParticleInternal(var1, var2, var3, var4, var6, var8, var10, var12, var14);
      } catch (Throwable var19) {
         CrashReport var17 = CrashReport.forThrowable(var19, "Exception while adding particle");
         CrashReportCategory var18 = var17.addCategory("Particle being added");
         var18.setDetail("ID", (Object)Registry.PARTICLE_TYPE.getKey(var1.getType()));
         var18.setDetail("Parameters", (Object)var1.writeToString());
         var18.setDetail("Position", () -> {
            return CrashReportCategory.formatLocation(this.level, var4, var6, var8);
         });
         throw new ReportedException(var17);
      }
   }

   private <T extends ParticleOptions> void addParticle(T var1, double var2, double var4, double var6, double var8, double var10, double var12) {
      this.addParticle(var1, var1.getType().getOverrideLimiter(), var2, var4, var6, var8, var10, var12);
   }

   @Nullable
   private Particle addParticleInternal(ParticleOptions var1, boolean var2, double var3, double var5, double var7, double var9, double var11, double var13) {
      return this.addParticleInternal(var1, var2, false, var3, var5, var7, var9, var11, var13);
   }

   @Nullable
   private Particle addParticleInternal(ParticleOptions var1, boolean var2, boolean var3, double var4, double var6, double var8, double var10, double var12, double var14) {
      Camera var16 = this.minecraft.gameRenderer.getMainCamera();
      if (this.minecraft != null && var16.isInitialized() && this.minecraft.particleEngine != null) {
         ParticleStatus var17 = this.calculateParticleLevel(var3);
         if (var2) {
            return this.minecraft.particleEngine.createParticle(var1, var4, var6, var8, var10, var12, var14);
         } else if (var16.getPosition().distanceToSqr(var4, var6, var8) > 1024.0D) {
            return null;
         } else {
            return var17 == ParticleStatus.MINIMAL ? null : this.minecraft.particleEngine.createParticle(var1, var4, var6, var8, var10, var12, var14);
         }
      } else {
         return null;
      }
   }

   private ParticleStatus calculateParticleLevel(boolean var1) {
      ParticleStatus var2 = this.minecraft.options.particles;
      if (var1 && var2 == ParticleStatus.MINIMAL && this.level.random.nextInt(10) == 0) {
         var2 = ParticleStatus.DECREASED;
      }

      if (var2 == ParticleStatus.DECREASED && this.level.random.nextInt(3) == 0) {
         var2 = ParticleStatus.MINIMAL;
      }

      return var2;
   }

   public void clear() {
   }

   public void globalLevelEvent(int var1, BlockPos var2, int var3) {
      switch(var1) {
      case 1023:
      case 1028:
      case 1038:
         Camera var4 = this.minecraft.gameRenderer.getMainCamera();
         if (var4.isInitialized()) {
            double var5 = (double)var2.getX() - var4.getPosition().x;
            double var7 = (double)var2.getY() - var4.getPosition().y;
            double var9 = (double)var2.getZ() - var4.getPosition().z;
            double var11 = Math.sqrt(var5 * var5 + var7 * var7 + var9 * var9);
            double var13 = var4.getPosition().x;
            double var15 = var4.getPosition().y;
            double var17 = var4.getPosition().z;
            if (var11 > 0.0D) {
               var13 += var5 / var11 * 2.0D;
               var15 += var7 / var11 * 2.0D;
               var17 += var9 / var11 * 2.0D;
            }

            if (var1 == 1023) {
               this.level.playLocalSound(var13, var15, var17, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
            } else if (var1 == 1038) {
               this.level.playLocalSound(var13, var15, var17, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
            } else {
               this.level.playLocalSound(var13, var15, var17, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 5.0F, 1.0F, false);
            }
         }
      default:
      }
   }

   public void levelEvent(Player var1, int var2, BlockPos var3, int var4) {
      Random var5 = this.level.random;
      int var7;
      float var8;
      float var9;
      double var10;
      double var12;
      double var14;
      double var34;
      switch(var2) {
      case 1000:
         this.level.playLocalSound(var3, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 1001:
         this.level.playLocalSound(var3, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.2F, false);
         break;
      case 1002:
         this.level.playLocalSound(var3, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0F, 1.2F, false);
         break;
      case 1003:
         this.level.playLocalSound(var3, SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
         break;
      case 1004:
         this.level.playLocalSound(var3, SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
         break;
      case 1005:
         this.level.playLocalSound(var3, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, var5.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1006:
         this.level.playLocalSound(var3, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, var5.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1007:
         this.level.playLocalSound(var3, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, var5.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1008:
         this.level.playLocalSound(var3, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1.0F, var5.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1009:
         if (var4 == 0) {
            this.level.playLocalSound(var3, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (var5.nextFloat() - var5.nextFloat()) * 0.8F, false);
         } else if (var4 == 1) {
            this.level.playLocalSound(var3, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.7F, 1.6F + (var5.nextFloat() - var5.nextFloat()) * 0.4F, false);
         }
         break;
      case 1010:
         if (Item.byId(var4) instanceof RecordItem) {
            this.playStreamingMusic(((RecordItem)Item.byId(var4)).getSound(), var3);
         } else {
            this.playStreamingMusic((SoundEvent)null, var3);
         }
         break;
      case 1011:
         this.level.playLocalSound(var3, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, var5.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1012:
         this.level.playLocalSound(var3, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, var5.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1013:
         this.level.playLocalSound(var3, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, var5.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1014:
         this.level.playLocalSound(var3, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0F, var5.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1015:
         this.level.playLocalSound(var3, SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 10.0F, (var5.nextFloat() - var5.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1016:
         this.level.playLocalSound(var3, SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 10.0F, (var5.nextFloat() - var5.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1017:
         this.level.playLocalSound(var3, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 10.0F, (var5.nextFloat() - var5.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1018:
         this.level.playLocalSound(var3, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, (var5.nextFloat() - var5.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1019:
         this.level.playLocalSound(var3, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (var5.nextFloat() - var5.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1020:
         this.level.playLocalSound(var3, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 2.0F, (var5.nextFloat() - var5.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1021:
         this.level.playLocalSound(var3, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (var5.nextFloat() - var5.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1022:
         this.level.playLocalSound(var3, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, (var5.nextFloat() - var5.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1024:
         this.level.playLocalSound(var3, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2.0F, (var5.nextFloat() - var5.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1025:
         this.level.playLocalSound(var3, SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 0.05F, (var5.nextFloat() - var5.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1026:
         this.level.playLocalSound(var3, SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 2.0F, (var5.nextFloat() - var5.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1027:
         this.level.playLocalSound(var3, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.NEUTRAL, 2.0F, (var5.nextFloat() - var5.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1029:
         this.level.playLocalSound(var3, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, var5.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1030:
         this.level.playLocalSound(var3, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, var5.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1031:
         this.level.playLocalSound(var3, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1032:
         this.minecraft.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRAVEL, var5.nextFloat() * 0.4F + 0.8F, 0.25F));
         break;
      case 1033:
         this.level.playLocalSound(var3, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 1034:
         this.level.playLocalSound(var3, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 1035:
         this.level.playLocalSound(var3, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 1036:
         this.level.playLocalSound(var3, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, var5.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1037:
         this.level.playLocalSound(var3, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, var5.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1039:
         this.level.playLocalSound(var3, SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1040:
         this.level.playLocalSound(var3, SoundEvents.ZOMBIE_CONVERTED_TO_DROWNED, SoundSource.NEUTRAL, 2.0F, (var5.nextFloat() - var5.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1041:
         this.level.playLocalSound(var3, SoundEvents.HUSK_CONVERTED_TO_ZOMBIE, SoundSource.NEUTRAL, 2.0F, (var5.nextFloat() - var5.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1042:
         this.level.playLocalSound(var3, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1043:
         this.level.playLocalSound(var3, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1044:
         this.level.playLocalSound(var3, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1045:
         this.level.playLocalSound(var3, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1046:
         this.level.playLocalSound(var3, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1047:
         this.level.playLocalSound(var3, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1048:
         this.level.playLocalSound(var3, SoundEvents.SKELETON_CONVERTED_TO_STRAY, SoundSource.NEUTRAL, 2.0F, (var5.nextFloat() - var5.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1500:
         ComposterBlock.handleFill(this.level, var3, var4 > 0);
         break;
      case 1501:
         this.level.playLocalSound(var3, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (var5.nextFloat() - var5.nextFloat()) * 0.8F, false);

         for(var7 = 0; var7 < 8; ++var7) {
            this.level.addParticle(ParticleTypes.LARGE_SMOKE, (double)var3.getX() + var5.nextDouble(), (double)var3.getY() + 1.2D, (double)var3.getZ() + var5.nextDouble(), 0.0D, 0.0D, 0.0D);
         }

         return;
      case 1502:
         this.level.playLocalSound(var3, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.5F, 2.6F + (var5.nextFloat() - var5.nextFloat()) * 0.8F, false);

         for(var7 = 0; var7 < 5; ++var7) {
            var34 = (double)var3.getX() + var5.nextDouble() * 0.6D + 0.2D;
            var10 = (double)var3.getY() + var5.nextDouble() * 0.6D + 0.2D;
            var12 = (double)var3.getZ() + var5.nextDouble() * 0.6D + 0.2D;
            this.level.addParticle(ParticleTypes.SMOKE, var34, var10, var12, 0.0D, 0.0D, 0.0D);
         }

         return;
      case 1503:
         this.level.playLocalSound(var3, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);

         for(var7 = 0; var7 < 16; ++var7) {
            var34 = (double)var3.getX() + (5.0D + var5.nextDouble() * 6.0D) / 16.0D;
            var10 = (double)var3.getY() + 0.8125D;
            var12 = (double)var3.getZ() + (5.0D + var5.nextDouble() * 6.0D) / 16.0D;
            this.level.addParticle(ParticleTypes.SMOKE, var34, var10, var12, 0.0D, 0.0D, 0.0D);
         }

         return;
      case 1504:
         PointedDripstoneBlock.spawnDripParticle(this.level, var3, this.level.getBlockState(var3));
         break;
      case 1505:
         BoneMealItem.addGrowthParticles(this.level, var3, var4);
         this.level.playLocalSound(var3, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 2000:
         Direction var33 = Direction.from3DDataValue(var4);
         var7 = var33.getStepX();
         int var35 = var33.getStepY();
         int var36 = var33.getStepZ();
         var10 = (double)var3.getX() + (double)var7 * 0.6D + 0.5D;
         var12 = (double)var3.getY() + (double)var35 * 0.6D + 0.5D;
         var14 = (double)var3.getZ() + (double)var36 * 0.6D + 0.5D;

         for(int var42 = 0; var42 < 10; ++var42) {
            double var17 = var5.nextDouble() * 0.2D + 0.01D;
            double var19 = var10 + (double)var7 * 0.01D + (var5.nextDouble() - 0.5D) * (double)var36 * 0.5D;
            double var21 = var12 + (double)var35 * 0.01D + (var5.nextDouble() - 0.5D) * (double)var35 * 0.5D;
            double var43 = var14 + (double)var36 * 0.01D + (var5.nextDouble() - 0.5D) * (double)var7 * 0.5D;
            double var25 = (double)var7 * var17 + var5.nextGaussian() * 0.01D;
            double var27 = (double)var35 * var17 + var5.nextGaussian() * 0.01D;
            double var29 = (double)var36 * var17 + var5.nextGaussian() * 0.01D;
            this.addParticle(ParticleTypes.SMOKE, var19, var21, var43, var25, var27, var29);
         }

         return;
      case 2001:
         BlockState var32 = Block.stateById(var4);
         if (!var32.isAir()) {
            SoundType var38 = var32.getSoundType();
            this.level.playLocalSound(var3, var38.getBreakSound(), SoundSource.BLOCKS, (var38.getVolume() + 1.0F) / 2.0F, var38.getPitch() * 0.8F, false);
         }

         this.level.addDestroyBlockEffect(var3, var32);
         break;
      case 2002:
      case 2007:
         Vec3 var31 = Vec3.atBottomCenterOf(var3);

         for(var7 = 0; var7 < 8; ++var7) {
            this.addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SPLASH_POTION)), var31.x, var31.y, var31.z, var5.nextGaussian() * 0.15D, var5.nextDouble() * 0.2D, var5.nextGaussian() * 0.15D);
         }

         float var37 = (float)(var4 >> 16 & 255) / 255.0F;
         var8 = (float)(var4 >> 8 & 255) / 255.0F;
         var9 = (float)(var4 >> 0 & 255) / 255.0F;
         SimpleParticleType var39 = var2 == 2007 ? ParticleTypes.INSTANT_EFFECT : ParticleTypes.EFFECT;

         for(int var11 = 0; var11 < 100; ++var11) {
            var12 = var5.nextDouble() * 4.0D;
            var14 = var5.nextDouble() * 3.141592653589793D * 2.0D;
            double var41 = Math.cos(var14) * var12;
            double var18 = 0.01D + var5.nextDouble() * 0.5D;
            double var20 = Math.sin(var14) * var12;
            Particle var22 = this.addParticleInternal(var39, var39.getType().getOverrideLimiter(), var31.x + var41 * 0.1D, var31.y + 0.3D, var31.z + var20 * 0.1D, var41, var18, var20);
            if (var22 != null) {
               float var23 = 0.75F + var5.nextFloat() * 0.25F;
               var22.setColor(var37 * var23, var8 * var23, var9 * var23);
               var22.setPower((float)var12);
            }
         }

         this.level.playLocalSound(var3, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, var5.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 2003:
         double var6 = (double)var3.getX() + 0.5D;
         var34 = (double)var3.getY();
         var10 = (double)var3.getZ() + 0.5D;

         for(int var40 = 0; var40 < 8; ++var40) {
            this.addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ENDER_EYE)), var6, var34, var10, var5.nextGaussian() * 0.15D, var5.nextDouble() * 0.2D, var5.nextGaussian() * 0.15D);
         }

         for(var12 = 0.0D; var12 < 6.283185307179586D; var12 += 0.15707963267948966D) {
            this.addParticle(ParticleTypes.PORTAL, var6 + Math.cos(var12) * 5.0D, var34 - 0.4D, var10 + Math.sin(var12) * 5.0D, Math.cos(var12) * -5.0D, 0.0D, Math.sin(var12) * -5.0D);
            this.addParticle(ParticleTypes.PORTAL, var6 + Math.cos(var12) * 5.0D, var34 - 0.4D, var10 + Math.sin(var12) * 5.0D, Math.cos(var12) * -7.0D, 0.0D, Math.sin(var12) * -7.0D);
         }

         return;
      case 2004:
         for(var7 = 0; var7 < 20; ++var7) {
            var34 = (double)var3.getX() + 0.5D + (var5.nextDouble() - 0.5D) * 2.0D;
            var10 = (double)var3.getY() + 0.5D + (var5.nextDouble() - 0.5D) * 2.0D;
            var12 = (double)var3.getZ() + 0.5D + (var5.nextDouble() - 0.5D) * 2.0D;
            this.level.addParticle(ParticleTypes.SMOKE, var34, var10, var12, 0.0D, 0.0D, 0.0D);
            this.level.addParticle(ParticleTypes.FLAME, var34, var10, var12, 0.0D, 0.0D, 0.0D);
         }

         return;
      case 2005:
         BoneMealItem.addGrowthParticles(this.level, var3, var4);
         break;
      case 2006:
         for(var7 = 0; var7 < 200; ++var7) {
            var8 = var5.nextFloat() * 4.0F;
            var9 = var5.nextFloat() * 6.2831855F;
            var10 = (double)(Mth.cos(var9) * var8);
            var12 = 0.01D + var5.nextDouble() * 0.5D;
            var14 = (double)(Mth.sin(var9) * var8);
            Particle var16 = this.addParticleInternal(ParticleTypes.DRAGON_BREATH, false, (double)var3.getX() + var10 * 0.1D, (double)var3.getY() + 0.3D, (double)var3.getZ() + var14 * 0.1D, var10, var12, var14);
            if (var16 != null) {
               var16.setPower(var8);
            }
         }

         if (var4 == 1) {
            this.level.playLocalSound(var3, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 1.0F, var5.nextFloat() * 0.1F + 0.9F, false);
         }
         break;
      case 2008:
         this.level.addParticle(ParticleTypes.EXPLOSION, (double)var3.getX() + 0.5D, (double)var3.getY() + 0.5D, (double)var3.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
         break;
      case 2009:
         for(var7 = 0; var7 < 8; ++var7) {
            this.level.addParticle(ParticleTypes.CLOUD, (double)var3.getX() + var5.nextDouble(), (double)var3.getY() + 1.2D, (double)var3.getZ() + var5.nextDouble(), 0.0D, 0.0D, 0.0D);
         }

         return;
      case 3000:
         this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, true, (double)var3.getX() + 0.5D, (double)var3.getY() + 0.5D, (double)var3.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
         this.level.playLocalSound(var3, SoundEvents.END_GATEWAY_SPAWN, SoundSource.BLOCKS, 10.0F, (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F, false);
         break;
      case 3001:
         this.level.playLocalSound(var3, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 64.0F, 0.8F + this.level.random.nextFloat() * 0.3F, false);
         break;
      case 3002:
         if (var4 >= 0 && var4 < Direction.Axis.VALUES.length) {
            ParticleUtils.spawnParticlesAlongAxis(Direction.Axis.VALUES[var4], this.level, var3, 0.125D, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(10, 19));
         } else {
            ParticleUtils.spawnParticlesOnBlockFaces(this.level, var3, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(3, 5));
         }
         break;
      case 3003:
         ParticleUtils.spawnParticlesOnBlockFaces(this.level, var3, ParticleTypes.WAX_ON, UniformInt.of(3, 5));
         this.level.playLocalSound(var3, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 3004:
         ParticleUtils.spawnParticlesOnBlockFaces(this.level, var3, ParticleTypes.WAX_OFF, UniformInt.of(3, 5));
         break;
      case 3005:
         ParticleUtils.spawnParticlesOnBlockFaces(this.level, var3, ParticleTypes.SCRAPE, UniformInt.of(3, 5));
      }

   }

   public void destroyBlockProgress(int var1, BlockPos var2, int var3) {
      BlockDestructionProgress var4;
      if (var3 >= 0 && var3 < 10) {
         var4 = (BlockDestructionProgress)this.destroyingBlocks.get(var1);
         if (var4 != null) {
            this.removeProgress(var4);
         }

         if (var4 == null || var4.getPos().getX() != var2.getX() || var4.getPos().getY() != var2.getY() || var4.getPos().getZ() != var2.getZ()) {
            var4 = new BlockDestructionProgress(var1, var2);
            this.destroyingBlocks.put(var1, var4);
         }

         var4.setProgress(var3);
         var4.updateTick(this.ticks);
         ((SortedSet)this.destructionProgress.computeIfAbsent(var4.getPos().asLong(), (var0) -> {
            return Sets.newTreeSet();
         })).add(var4);
      } else {
         var4 = (BlockDestructionProgress)this.destroyingBlocks.remove(var1);
         if (var4 != null) {
            this.removeProgress(var4);
         }
      }

   }

   public boolean hasRenderedAllChunks() {
      return this.chunksToCompile.isEmpty() && this.chunkRenderDispatcher.isQueueEmpty();
   }

   public void needsUpdate() {
      this.needsUpdate = true;
      this.generateClouds = true;
   }

   public void updateGlobalBlockEntities(Collection<BlockEntity> var1, Collection<BlockEntity> var2) {
      synchronized(this.globalBlockEntities) {
         this.globalBlockEntities.removeAll(var1);
         this.globalBlockEntities.addAll(var2);
      }
   }

   public static int getLightColor(BlockAndTintGetter var0, BlockPos var1) {
      return getLightColor(var0, var0.getBlockState(var1), var1);
   }

   public static int getLightColor(BlockAndTintGetter var0, BlockState var1, BlockPos var2) {
      if (var1.emissiveRendering(var0, var2)) {
         return 15728880;
      } else {
         int var3 = var0.getBrightness(LightLayer.SKY, var2);
         int var4 = var0.getBrightness(LightLayer.BLOCK, var2);
         int var5 = var1.getLightEmission();
         if (var4 < var5) {
            var4 = var5;
         }

         return var3 << 20 | var4 << 4;
      }
   }

   @Nullable
   public RenderTarget entityTarget() {
      return this.entityTarget;
   }

   @Nullable
   public RenderTarget getTranslucentTarget() {
      return this.translucentTarget;
   }

   @Nullable
   public RenderTarget getItemEntityTarget() {
      return this.itemEntityTarget;
   }

   @Nullable
   public RenderTarget getParticlesTarget() {
      return this.particlesTarget;
   }

   @Nullable
   public RenderTarget getWeatherTarget() {
      return this.weatherTarget;
   }

   @Nullable
   public RenderTarget getCloudsTarget() {
      return this.cloudsTarget;
   }

   public static class TransparencyShaderException extends RuntimeException {
      public TransparencyShaderException(String var1, Throwable var2) {
         super(var1, var2);
      }
   }

   private static class RenderInfoMap {
      private final LevelRenderer.RenderChunkInfo[] infos;
      private final LevelRenderer.RenderChunkInfo[] blank;

      RenderInfoMap(int var1) {
         this.infos = new LevelRenderer.RenderChunkInfo[var1];
         this.blank = new LevelRenderer.RenderChunkInfo[var1];
      }

      void clear() {
         System.arraycopy(this.blank, 0, this.infos, 0, this.infos.length);
      }

      public void put(ChunkRenderDispatcher.RenderChunk var1, LevelRenderer.RenderChunkInfo var2) {
         this.infos[var1.index] = var2;
      }

      public LevelRenderer.RenderChunkInfo get(ChunkRenderDispatcher.RenderChunk var1) {
         return this.infos[var1.index];
      }
   }

   private static class RenderChunkInfo {
      final ChunkRenderDispatcher.RenderChunk chunk;
      private byte sourceDirections;
      byte directions;
      final int step;

      RenderChunkInfo(ChunkRenderDispatcher.RenderChunk var1, @Nullable Direction var2, int var3) {
         this.chunk = var1;
         if (var2 != null) {
            this.addSourceDirection(var2);
         }

         this.step = var3;
      }

      public void setDirections(byte var1, Direction var2) {
         this.directions = (byte)(this.directions | var1 | 1 << var2.ordinal());
      }

      public boolean hasDirection(Direction var1) {
         return (this.directions & 1 << var1.ordinal()) > 0;
      }

      public void addSourceDirection(Direction var1) {
         this.sourceDirections = (byte)(this.sourceDirections | this.sourceDirections | 1 << var1.ordinal());
      }

      public boolean hasSourceDirection(int var1) {
         return (this.sourceDirections & 1 << var1) > 0;
      }

      public boolean hasSourceDirections() {
         return this.sourceDirections != 0;
      }
   }
}
