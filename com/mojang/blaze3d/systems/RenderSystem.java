package com.mojang.blaze3d.systems;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallbackI;

@DontObfuscate
public class RenderSystem {
   static final Logger LOGGER = LogManager.getLogger();
   private static final ConcurrentLinkedQueue<RenderCall> recordingQueue = Queues.newConcurrentLinkedQueue();
   private static final Tesselator RENDER_THREAD_TESSELATOR = new Tesselator();
   private static final int MINIMUM_ATLAS_TEXTURE_SIZE = 1024;
   private static boolean isReplayingQueue;
   @Nullable
   private static Thread gameThread;
   @Nullable
   private static Thread renderThread;
   private static int MAX_SUPPORTED_TEXTURE_SIZE = -1;
   private static boolean isInInit;
   private static double lastDrawTime = Double.MIN_VALUE;
   private static final RenderSystem.AutoStorageIndexBuffer sharedSequential = new RenderSystem.AutoStorageIndexBuffer(1, 1, IntConsumer::accept);
   private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialQuad = new RenderSystem.AutoStorageIndexBuffer(4, 6, (var0, var1) -> {
      var0.accept(var1 + 0);
      var0.accept(var1 + 1);
      var0.accept(var1 + 2);
      var0.accept(var1 + 2);
      var0.accept(var1 + 3);
      var0.accept(var1 + 0);
   });
   private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialLines = new RenderSystem.AutoStorageIndexBuffer(4, 6, (var0, var1) -> {
      var0.accept(var1 + 0);
      var0.accept(var1 + 1);
      var0.accept(var1 + 2);
      var0.accept(var1 + 3);
      var0.accept(var1 + 2);
      var0.accept(var1 + 1);
   });
   private static Matrix4f projectionMatrix = new Matrix4f();
   private static Matrix4f savedProjectionMatrix = new Matrix4f();
   private static PoseStack modelViewStack = new PoseStack();
   private static Matrix4f modelViewMatrix = new Matrix4f();
   private static Matrix4f textureMatrix = new Matrix4f();
   private static final int[] shaderTextures = new int[12];
   private static final float[] shaderColor = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
   private static float shaderFogStart;
   private static float shaderFogEnd = 1.0F;
   private static final float[] shaderFogColor = new float[]{0.0F, 0.0F, 0.0F, 0.0F};
   private static final Vector3f[] shaderLightDirections = new Vector3f[2];
   private static float shaderGameTime;
   private static float shaderLineWidth = 1.0F;
   @Nullable
   private static ShaderInstance shader;

   public static void initRenderThread() {
      if (renderThread == null && gameThread != Thread.currentThread()) {
         renderThread = Thread.currentThread();
      } else {
         throw new IllegalStateException("Could not initialize render thread");
      }
   }

   public static boolean isOnRenderThread() {
      return Thread.currentThread() == renderThread;
   }

   public static boolean isOnRenderThreadOrInit() {
      return isInInit || isOnRenderThread();
   }

   public static void initGameThread(boolean var0) {
      boolean var1 = renderThread == Thread.currentThread();
      if (gameThread == null && renderThread != null && var1 != var0) {
         gameThread = Thread.currentThread();
      } else {
         throw new IllegalStateException("Could not initialize tick thread");
      }
   }

   public static boolean isOnGameThread() {
      return true;
   }

   public static boolean isOnGameThreadOrInit() {
      return isInInit || isOnGameThread();
   }

   public static void assertThread(Supplier<Boolean> var0) {
      if (!(Boolean)var0.get()) {
         throw new IllegalStateException("Rendersystem called from wrong thread");
      }
   }

   public static boolean isInInitPhase() {
      return true;
   }

   public static void recordRenderCall(RenderCall var0) {
      recordingQueue.add(var0);
   }

   public static void flipFrame(long var0) {
      GLFW.glfwPollEvents();
      replayQueue();
      Tesselator.getInstance().getBuilder().clear();
      GLFW.glfwSwapBuffers(var0);
      GLFW.glfwPollEvents();
   }

   public static void replayQueue() {
      isReplayingQueue = true;

      while(!recordingQueue.isEmpty()) {
         RenderCall var0 = (RenderCall)recordingQueue.poll();
         var0.execute();
      }

      isReplayingQueue = false;
   }

   public static void limitDisplayFPS(int var0) {
      double var1 = lastDrawTime + 1.0D / (double)var0;

      double var3;
      for(var3 = GLFW.glfwGetTime(); var3 < var1; var3 = GLFW.glfwGetTime()) {
         GLFW.glfwWaitEventsTimeout(var1 - var3);
      }

      lastDrawTime = var3;
   }

   public static void disableDepthTest() {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._disableDepthTest();
   }

   public static void enableDepthTest() {
      assertThread(RenderSystem::isOnGameThreadOrInit);
      GlStateManager._enableDepthTest();
   }

   public static void enableScissor(int var0, int var1, int var2, int var3) {
      assertThread(RenderSystem::isOnGameThreadOrInit);
      GlStateManager._enableScissorTest();
      GlStateManager._scissorBox(var0, var1, var2, var3);
   }

   public static void disableScissor() {
      assertThread(RenderSystem::isOnGameThreadOrInit);
      GlStateManager._disableScissorTest();
   }

   public static void depthFunc(int var0) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._depthFunc(var0);
   }

   public static void depthMask(boolean var0) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._depthMask(var0);
   }

   public static void enableBlend() {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._enableBlend();
   }

   public static void disableBlend() {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._disableBlend();
   }

   public static void blendFunc(GlStateManager.SourceFactor var0, GlStateManager.DestFactor var1) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._blendFunc(var0.value, var1.value);
   }

   public static void blendFunc(int var0, int var1) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._blendFunc(var0, var1);
   }

   public static void blendFuncSeparate(GlStateManager.SourceFactor var0, GlStateManager.DestFactor var1, GlStateManager.SourceFactor var2, GlStateManager.DestFactor var3) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._blendFuncSeparate(var0.value, var1.value, var2.value, var3.value);
   }

   public static void blendFuncSeparate(int var0, int var1, int var2, int var3) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._blendFuncSeparate(var0, var1, var2, var3);
   }

   public static void blendEquation(int var0) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._blendEquation(var0);
   }

   public static void enableCull() {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._enableCull();
   }

   public static void disableCull() {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._disableCull();
   }

   public static void polygonMode(int var0, int var1) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._polygonMode(var0, var1);
   }

   public static void enablePolygonOffset() {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._enablePolygonOffset();
   }

   public static void disablePolygonOffset() {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._disablePolygonOffset();
   }

   public static void polygonOffset(float var0, float var1) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._polygonOffset(var0, var1);
   }

   public static void enableColorLogicOp() {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._enableColorLogicOp();
   }

   public static void disableColorLogicOp() {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._disableColorLogicOp();
   }

   public static void logicOp(GlStateManager.LogicOp var0) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._logicOp(var0.value);
   }

   public static void activeTexture(int var0) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._activeTexture(var0);
   }

   public static void enableTexture() {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._enableTexture();
   }

   public static void disableTexture() {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._disableTexture();
   }

   public static void texParameter(int var0, int var1, int var2) {
      GlStateManager._texParameter(var0, var1, var2);
   }

   public static void deleteTexture(int var0) {
      assertThread(RenderSystem::isOnGameThreadOrInit);
      GlStateManager._deleteTexture(var0);
   }

   public static void bindTextureForSetup(int var0) {
      bindTexture(var0);
   }

   public static void bindTexture(int var0) {
      GlStateManager._bindTexture(var0);
   }

   public static void viewport(int var0, int var1, int var2, int var3) {
      assertThread(RenderSystem::isOnGameThreadOrInit);
      GlStateManager._viewport(var0, var1, var2, var3);
   }

   public static void colorMask(boolean var0, boolean var1, boolean var2, boolean var3) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._colorMask(var0, var1, var2, var3);
   }

   public static void stencilFunc(int var0, int var1, int var2) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._stencilFunc(var0, var1, var2);
   }

   public static void stencilMask(int var0) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._stencilMask(var0);
   }

   public static void stencilOp(int var0, int var1, int var2) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._stencilOp(var0, var1, var2);
   }

   public static void clearDepth(double var0) {
      assertThread(RenderSystem::isOnGameThreadOrInit);
      GlStateManager._clearDepth(var0);
   }

   public static void clearColor(float var0, float var1, float var2, float var3) {
      assertThread(RenderSystem::isOnGameThreadOrInit);
      GlStateManager._clearColor(var0, var1, var2, var3);
   }

   public static void clearStencil(int var0) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._clearStencil(var0);
   }

   public static void clear(int var0, boolean var1) {
      assertThread(RenderSystem::isOnGameThreadOrInit);
      GlStateManager._clear(var0, var1);
   }

   public static void setShaderFogStart(float var0) {
      assertThread(RenderSystem::isOnGameThread);
      _setShaderFogStart(var0);
   }

   private static void _setShaderFogStart(float var0) {
      shaderFogStart = var0;
   }

   public static float getShaderFogStart() {
      assertThread(RenderSystem::isOnRenderThread);
      return shaderFogStart;
   }

   public static void setShaderFogEnd(float var0) {
      assertThread(RenderSystem::isOnGameThread);
      _setShaderFogEnd(var0);
   }

   private static void _setShaderFogEnd(float var0) {
      shaderFogEnd = var0;
   }

   public static float getShaderFogEnd() {
      assertThread(RenderSystem::isOnRenderThread);
      return shaderFogEnd;
   }

   public static void setShaderFogColor(float var0, float var1, float var2, float var3) {
      assertThread(RenderSystem::isOnGameThread);
      _setShaderFogColor(var0, var1, var2, var3);
   }

   public static void setShaderFogColor(float var0, float var1, float var2) {
      setShaderFogColor(var0, var1, var2, 1.0F);
   }

   private static void _setShaderFogColor(float var0, float var1, float var2, float var3) {
      shaderFogColor[0] = var0;
      shaderFogColor[1] = var1;
      shaderFogColor[2] = var2;
      shaderFogColor[3] = var3;
   }

   public static float[] getShaderFogColor() {
      assertThread(RenderSystem::isOnRenderThread);
      return shaderFogColor;
   }

   public static void setShaderLights(Vector3f var0, Vector3f var1) {
      assertThread(RenderSystem::isOnGameThread);
      _setShaderLights(var0, var1);
   }

   public static void _setShaderLights(Vector3f var0, Vector3f var1) {
      shaderLightDirections[0] = var0;
      shaderLightDirections[1] = var1;
   }

   public static void setupShaderLights(ShaderInstance var0) {
      assertThread(RenderSystem::isOnRenderThread);
      if (var0.LIGHT0_DIRECTION != null) {
         var0.LIGHT0_DIRECTION.set(shaderLightDirections[0]);
      }

      if (var0.LIGHT1_DIRECTION != null) {
         var0.LIGHT1_DIRECTION.set(shaderLightDirections[1]);
      }

   }

   public static void setShaderColor(float var0, float var1, float var2, float var3) {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> {
            _setShaderColor(var0, var1, var2, var3);
         });
      } else {
         _setShaderColor(var0, var1, var2, var3);
      }

   }

   private static void _setShaderColor(float var0, float var1, float var2, float var3) {
      shaderColor[0] = var0;
      shaderColor[1] = var1;
      shaderColor[2] = var2;
      shaderColor[3] = var3;
   }

   public static float[] getShaderColor() {
      assertThread(RenderSystem::isOnRenderThread);
      return shaderColor;
   }

   public static void drawElements(int var0, int var1, int var2) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._drawElements(var0, var1, var2, 0L);
   }

   public static void lineWidth(float var0) {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> {
            shaderLineWidth = var0;
         });
      } else {
         shaderLineWidth = var0;
      }

   }

   public static float getShaderLineWidth() {
      assertThread(RenderSystem::isOnRenderThread);
      return shaderLineWidth;
   }

   public static void pixelStore(int var0, int var1) {
      assertThread(RenderSystem::isOnGameThreadOrInit);
      GlStateManager._pixelStore(var0, var1);
   }

   public static void readPixels(int var0, int var1, int var2, int var3, int var4, int var5, ByteBuffer var6) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._readPixels(var0, var1, var2, var3, var4, var5, var6);
   }

   public static void getString(int var0, Consumer<String> var1) {
      assertThread(RenderSystem::isOnGameThread);
      var1.accept(GlStateManager._getString(var0));
   }

   public static String getBackendDescription() {
      assertThread(RenderSystem::isInInitPhase);
      return String.format("LWJGL version %s", GLX._getLWJGLVersion());
   }

   public static String getApiDescription() {
      assertThread(RenderSystem::isInInitPhase);
      return GLX.getOpenGLVersionString();
   }

   public static LongSupplier initBackendSystem() {
      assertThread(RenderSystem::isInInitPhase);
      return GLX._initGlfw();
   }

   public static void initRenderer(int var0, boolean var1) {
      assertThread(RenderSystem::isInInitPhase);
      GLX._init(var0, var1);
   }

   public static void setErrorCallback(GLFWErrorCallbackI var0) {
      assertThread(RenderSystem::isInInitPhase);
      GLX._setGlfwErrorCallback(var0);
   }

   public static void renderCrosshair(int var0) {
      assertThread(RenderSystem::isOnGameThread);
      GLX._renderCrosshair(var0, true, true, true);
   }

   public static String getCapsString() {
      assertThread(RenderSystem::isOnGameThread);
      return "Using framebuffer using OpenGL 3.2";
   }

   public static void setupDefaultState(int var0, int var1, int var2, int var3) {
      assertThread(RenderSystem::isInInitPhase);
      GlStateManager._enableTexture();
      GlStateManager._clearDepth(1.0D);
      GlStateManager._enableDepthTest();
      GlStateManager._depthFunc(515);
      projectionMatrix.setIdentity();
      savedProjectionMatrix.setIdentity();
      modelViewMatrix.setIdentity();
      textureMatrix.setIdentity();
      GlStateManager._viewport(var0, var1, var2, var3);
   }

   public static int maxSupportedTextureSize() {
      if (MAX_SUPPORTED_TEXTURE_SIZE == -1) {
         assertThread(RenderSystem::isOnRenderThreadOrInit);
         int var0 = GlStateManager._getInteger(3379);

         for(int var1 = Math.max(32768, var0); var1 >= 1024; var1 >>= 1) {
            GlStateManager._texImage2D(32868, 0, 6408, var1, var1, 0, 6408, 5121, (IntBuffer)null);
            int var2 = GlStateManager._getTexLevelParameter(32868, 0, 4096);
            if (var2 != 0) {
               MAX_SUPPORTED_TEXTURE_SIZE = var1;
               return var1;
            }
         }

         MAX_SUPPORTED_TEXTURE_SIZE = Math.max(var0, 1024);
         LOGGER.info("Failed to determine maximum texture size by probing, trying GL_MAX_TEXTURE_SIZE = {}", MAX_SUPPORTED_TEXTURE_SIZE);
      }

      return MAX_SUPPORTED_TEXTURE_SIZE;
   }

   public static void glBindBuffer(int var0, IntSupplier var1) {
      GlStateManager._glBindBuffer(var0, var1.getAsInt());
   }

   public static void glBindVertexArray(Supplier<Integer> var0) {
      GlStateManager._glBindVertexArray((Integer)var0.get());
   }

   public static void glBufferData(int var0, ByteBuffer var1, int var2) {
      assertThread(RenderSystem::isOnRenderThreadOrInit);
      GlStateManager._glBufferData(var0, var1, var2);
   }

   public static void glDeleteBuffers(int var0) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._glDeleteBuffers(var0);
   }

   public static void glDeleteVertexArrays(int var0) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._glDeleteVertexArrays(var0);
   }

   public static void glUniform1i(int var0, int var1) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._glUniform1i(var0, var1);
   }

   public static void glUniform1(int var0, IntBuffer var1) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._glUniform1(var0, var1);
   }

   public static void glUniform2(int var0, IntBuffer var1) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._glUniform2(var0, var1);
   }

   public static void glUniform3(int var0, IntBuffer var1) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._glUniform3(var0, var1);
   }

   public static void glUniform4(int var0, IntBuffer var1) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._glUniform4(var0, var1);
   }

   public static void glUniform1(int var0, FloatBuffer var1) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._glUniform1(var0, var1);
   }

   public static void glUniform2(int var0, FloatBuffer var1) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._glUniform2(var0, var1);
   }

   public static void glUniform3(int var0, FloatBuffer var1) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._glUniform3(var0, var1);
   }

   public static void glUniform4(int var0, FloatBuffer var1) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._glUniform4(var0, var1);
   }

   public static void glUniformMatrix2(int var0, boolean var1, FloatBuffer var2) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._glUniformMatrix2(var0, var1, var2);
   }

   public static void glUniformMatrix3(int var0, boolean var1, FloatBuffer var2) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._glUniformMatrix3(var0, var1, var2);
   }

   public static void glUniformMatrix4(int var0, boolean var1, FloatBuffer var2) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager._glUniformMatrix4(var0, var1, var2);
   }

   public static void setupOverlayColor(IntSupplier var0, int var1) {
      assertThread(RenderSystem::isOnGameThread);
      int var2 = var0.getAsInt();
      setShaderTexture(1, var2);
   }

   public static void teardownOverlayColor() {
      assertThread(RenderSystem::isOnGameThread);
      setShaderTexture(1, 0);
   }

   public static void setupLevelDiffuseLighting(Vector3f var0, Vector3f var1, Matrix4f var2) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager.setupLevelDiffuseLighting(var0, var1, var2);
   }

   public static void setupGuiFlatDiffuseLighting(Vector3f var0, Vector3f var1) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager.setupGuiFlatDiffuseLighting(var0, var1);
   }

   public static void setupGui3DDiffuseLighting(Vector3f var0, Vector3f var1) {
      assertThread(RenderSystem::isOnGameThread);
      GlStateManager.setupGui3DDiffuseLighting(var0, var1);
   }

   public static void beginInitialization() {
      isInInit = true;
   }

   public static void finishInitialization() {
      isInInit = false;
      if (!recordingQueue.isEmpty()) {
         replayQueue();
      }

      if (!recordingQueue.isEmpty()) {
         throw new IllegalStateException("Recorded to render queue during initialization");
      }
   }

   public static void glGenBuffers(Consumer<Integer> var0) {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> {
            var0.accept(GlStateManager._glGenBuffers());
         });
      } else {
         var0.accept(GlStateManager._glGenBuffers());
      }

   }

   public static void glGenVertexArrays(Consumer<Integer> var0) {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> {
            var0.accept(GlStateManager._glGenVertexArrays());
         });
      } else {
         var0.accept(GlStateManager._glGenVertexArrays());
      }

   }

   public static Tesselator renderThreadTesselator() {
      assertThread(RenderSystem::isOnRenderThread);
      return RENDER_THREAD_TESSELATOR;
   }

   public static void defaultBlendFunc() {
      blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
   }

   @Deprecated
   public static void runAsFancy(Runnable var0) {
      boolean var1 = Minecraft.useShaderTransparency();
      if (!var1) {
         var0.run();
      } else {
         Options var2 = Minecraft.getInstance().options;
         GraphicsStatus var3 = var2.graphicsMode;
         var2.graphicsMode = GraphicsStatus.FANCY;
         var0.run();
         var2.graphicsMode = var3;
      }
   }

   public static void setShader(Supplier<ShaderInstance> var0) {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> {
            shader = (ShaderInstance)var0.get();
         });
      } else {
         shader = (ShaderInstance)var0.get();
      }

   }

   @Nullable
   public static ShaderInstance getShader() {
      assertThread(RenderSystem::isOnRenderThread);
      return shader;
   }

   public static int getTextureId(int var0) {
      return GlStateManager._getTextureId(var0);
   }

   public static void setShaderTexture(int var0, ResourceLocation var1) {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> {
            _setShaderTexture(var0, var1);
         });
      } else {
         _setShaderTexture(var0, var1);
      }

   }

   public static void _setShaderTexture(int var0, ResourceLocation var1) {
      if (var0 >= 0 && var0 < shaderTextures.length) {
         TextureManager var2 = Minecraft.getInstance().getTextureManager();
         AbstractTexture var3 = var2.getTexture(var1);
         shaderTextures[var0] = var3.getId();
      }

   }

   public static void setShaderTexture(int var0, int var1) {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> {
            _setShaderTexture(var0, var1);
         });
      } else {
         _setShaderTexture(var0, var1);
      }

   }

   public static void _setShaderTexture(int var0, int var1) {
      if (var0 >= 0 && var0 < shaderTextures.length) {
         shaderTextures[var0] = var1;
      }

   }

   public static int getShaderTexture(int var0) {
      assertThread(RenderSystem::isOnRenderThread);
      return var0 >= 0 && var0 < shaderTextures.length ? shaderTextures[var0] : 0;
   }

   public static void setProjectionMatrix(Matrix4f var0) {
      Matrix4f var1 = var0.copy();
      if (!isOnRenderThread()) {
         recordRenderCall(() -> {
            projectionMatrix = var1;
         });
      } else {
         projectionMatrix = var1;
      }

   }

   public static void setTextureMatrix(Matrix4f var0) {
      Matrix4f var1 = var0.copy();
      if (!isOnRenderThread()) {
         recordRenderCall(() -> {
            textureMatrix = var1;
         });
      } else {
         textureMatrix = var1;
      }

   }

   public static void resetTextureMatrix() {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> {
            textureMatrix.setIdentity();
         });
      } else {
         textureMatrix.setIdentity();
      }

   }

   public static void applyModelViewMatrix() {
      Matrix4f var0 = modelViewStack.last().pose().copy();
      if (!isOnRenderThread()) {
         recordRenderCall(() -> {
            modelViewMatrix = var0;
         });
      } else {
         modelViewMatrix = var0;
      }

   }

   public static void backupProjectionMatrix() {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> {
            _backupProjectionMatrix();
         });
      } else {
         _backupProjectionMatrix();
      }

   }

   private static void _backupProjectionMatrix() {
      savedProjectionMatrix = projectionMatrix;
   }

   public static void restoreProjectionMatrix() {
      if (!isOnRenderThread()) {
         recordRenderCall(() -> {
            _restoreProjectionMatrix();
         });
      } else {
         _restoreProjectionMatrix();
      }

   }

   private static void _restoreProjectionMatrix() {
      projectionMatrix = savedProjectionMatrix;
   }

   public static Matrix4f getProjectionMatrix() {
      assertThread(RenderSystem::isOnRenderThread);
      return projectionMatrix;
   }

   public static Matrix4f getModelViewMatrix() {
      assertThread(RenderSystem::isOnRenderThread);
      return modelViewMatrix;
   }

   public static PoseStack getModelViewStack() {
      return modelViewStack;
   }

   public static Matrix4f getTextureMatrix() {
      assertThread(RenderSystem::isOnRenderThread);
      return textureMatrix;
   }

   public static RenderSystem.AutoStorageIndexBuffer getSequentialBuffer(VertexFormat.Mode var0, int var1) {
      assertThread(RenderSystem::isOnRenderThread);
      RenderSystem.AutoStorageIndexBuffer var2;
      if (var0 == VertexFormat.Mode.QUADS) {
         var2 = sharedSequentialQuad;
      } else if (var0 == VertexFormat.Mode.LINES) {
         var2 = sharedSequentialLines;
      } else {
         var2 = sharedSequential;
      }

      var2.ensureStorage(var1);
      return var2;
   }

   public static void setShaderGameTime(long var0, float var2) {
      float var3 = ((float)var0 + var2) / 24000.0F;
      if (!isOnRenderThread()) {
         recordRenderCall(() -> {
            shaderGameTime = var3;
         });
      } else {
         shaderGameTime = var3;
      }

   }

   public static float getShaderGameTime() {
      assertThread(RenderSystem::isOnRenderThread);
      return shaderGameTime;
   }

   // $FF: synthetic method
   private static void lambda$setupGui3DDiffuseLighting$57(Vector3f var0, Vector3f var1) {
      GlStateManager.setupGui3DDiffuseLighting(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$setupGuiFlatDiffuseLighting$56(Vector3f var0, Vector3f var1) {
      GlStateManager.setupGuiFlatDiffuseLighting(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$setupLevelDiffuseLighting$55(Vector3f var0, Vector3f var1, Matrix4f var2) {
      GlStateManager.setupLevelDiffuseLighting(var0, var1, var2);
   }

   // $FF: synthetic method
   private static void lambda$teardownOverlayColor$54() {
      setShaderTexture(1, 0);
   }

   // $FF: synthetic method
   private static void lambda$setupOverlayColor$53(IntSupplier var0) {
      int var1 = var0.getAsInt();
      setShaderTexture(1, var1);
   }

   // $FF: synthetic method
   private static void lambda$glUniformMatrix4$52(int var0, boolean var1, FloatBuffer var2) {
      GlStateManager._glUniformMatrix4(var0, var1, var2);
   }

   // $FF: synthetic method
   private static void lambda$glUniformMatrix3$51(int var0, boolean var1, FloatBuffer var2) {
      GlStateManager._glUniformMatrix3(var0, var1, var2);
   }

   // $FF: synthetic method
   private static void lambda$glUniformMatrix2$50(int var0, boolean var1, FloatBuffer var2) {
      GlStateManager._glUniformMatrix2(var0, var1, var2);
   }

   // $FF: synthetic method
   private static void lambda$glUniform4$49(int var0, FloatBuffer var1) {
      GlStateManager._glUniform4(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$glUniform3$48(int var0, FloatBuffer var1) {
      GlStateManager._glUniform3(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$glUniform2$47(int var0, FloatBuffer var1) {
      GlStateManager._glUniform2(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$glUniform1$46(int var0, FloatBuffer var1) {
      GlStateManager._glUniform1(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$glUniform4$45(int var0, IntBuffer var1) {
      GlStateManager._glUniform4(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$glUniform3$44(int var0, IntBuffer var1) {
      GlStateManager._glUniform3(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$glUniform2$43(int var0, IntBuffer var1) {
      GlStateManager._glUniform2(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$glUniform1$42(int var0, IntBuffer var1) {
      GlStateManager._glUniform1(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$glUniform1i$41(int var0, int var1) {
      GlStateManager._glUniform1i(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$glDeleteVertexArrays$40(int var0) {
      GlStateManager._glDeleteVertexArrays(var0);
   }

   // $FF: synthetic method
   private static void lambda$glDeleteBuffers$39(int var0) {
      GlStateManager._glDeleteBuffers(var0);
   }

   // $FF: synthetic method
   private static void lambda$glBindVertexArray$38(Supplier var0) {
      GlStateManager._glBindVertexArray((Integer)var0.get());
   }

   // $FF: synthetic method
   private static void lambda$glBindBuffer$37(int var0, IntSupplier var1) {
      GlStateManager._glBindBuffer(var0, var1.getAsInt());
   }

   // $FF: synthetic method
   private static void lambda$renderCrosshair$36(int var0) {
      GLX._renderCrosshair(var0, true, true, true);
   }

   // $FF: synthetic method
   private static void lambda$getString$35(int var0, Consumer var1) {
      String var2 = GlStateManager._getString(var0);
      var1.accept(var2);
   }

   // $FF: synthetic method
   private static void lambda$readPixels$34(int var0, int var1, int var2, int var3, int var4, int var5, ByteBuffer var6) {
      GlStateManager._readPixels(var0, var1, var2, var3, var4, var5, var6);
   }

   // $FF: synthetic method
   private static void lambda$pixelStore$33(int var0, int var1) {
      GlStateManager._pixelStore(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$drawElements$31(int var0, int var1, int var2) {
      GlStateManager._drawElements(var0, var1, var2, 0L);
   }

   // $FF: synthetic method
   private static void lambda$setShaderLights$29(Vector3f var0, Vector3f var1) {
      _setShaderLights(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$setShaderFogColor$28(float var0, float var1, float var2, float var3) {
      _setShaderFogColor(var0, var1, var2, var3);
   }

   // $FF: synthetic method
   private static void lambda$setShaderFogEnd$27(float var0) {
      _setShaderFogEnd(var0);
   }

   // $FF: synthetic method
   private static void lambda$setShaderFogStart$26(float var0) {
      _setShaderFogStart(var0);
   }

   // $FF: synthetic method
   private static void lambda$clear$25(int var0, boolean var1) {
      GlStateManager._clear(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$clearStencil$24(int var0) {
      GlStateManager._clearStencil(var0);
   }

   // $FF: synthetic method
   private static void lambda$clearColor$23(float var0, float var1, float var2, float var3) {
      GlStateManager._clearColor(var0, var1, var2, var3);
   }

   // $FF: synthetic method
   private static void lambda$clearDepth$22(double var0) {
      GlStateManager._clearDepth(var0);
   }

   // $FF: synthetic method
   private static void lambda$stencilOp$21(int var0, int var1, int var2) {
      GlStateManager._stencilOp(var0, var1, var2);
   }

   // $FF: synthetic method
   private static void lambda$stencilMask$20(int var0) {
      GlStateManager._stencilMask(var0);
   }

   // $FF: synthetic method
   private static void lambda$stencilFunc$19(int var0, int var1, int var2) {
      GlStateManager._stencilFunc(var0, var1, var2);
   }

   // $FF: synthetic method
   private static void lambda$colorMask$18(boolean var0, boolean var1, boolean var2, boolean var3) {
      GlStateManager._colorMask(var0, var1, var2, var3);
   }

   // $FF: synthetic method
   private static void lambda$viewport$17(int var0, int var1, int var2, int var3) {
      GlStateManager._viewport(var0, var1, var2, var3);
   }

   // $FF: synthetic method
   private static void lambda$bindTexture$16(int var0) {
      GlStateManager._bindTexture(var0);
   }

   // $FF: synthetic method
   private static void lambda$deleteTexture$15(int var0) {
      GlStateManager._deleteTexture(var0);
   }

   // $FF: synthetic method
   private static void lambda$texParameter$14(int var0, int var1, int var2) {
      GlStateManager._texParameter(var0, var1, var2);
   }

   // $FF: synthetic method
   private static void lambda$activeTexture$13(int var0) {
      GlStateManager._activeTexture(var0);
   }

   // $FF: synthetic method
   private static void lambda$logicOp$12(GlStateManager.LogicOp var0) {
      GlStateManager._logicOp(var0.value);
   }

   // $FF: synthetic method
   private static void lambda$polygonOffset$11(float var0, float var1) {
      GlStateManager._polygonOffset(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$polygonMode$10(int var0, int var1) {
      GlStateManager._polygonMode(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$blendEquation$9(int var0) {
      GlStateManager._blendEquation(var0);
   }

   // $FF: synthetic method
   private static void lambda$blendFuncSeparate$8(int var0, int var1, int var2, int var3) {
      GlStateManager._blendFuncSeparate(var0, var1, var2, var3);
   }

   // $FF: synthetic method
   private static void lambda$blendFuncSeparate$7(GlStateManager.SourceFactor var0, GlStateManager.DestFactor var1, GlStateManager.SourceFactor var2, GlStateManager.DestFactor var3) {
      GlStateManager._blendFuncSeparate(var0.value, var1.value, var2.value, var3.value);
   }

   // $FF: synthetic method
   private static void lambda$blendFunc$6(int var0, int var1) {
      GlStateManager._blendFunc(var0, var1);
   }

   // $FF: synthetic method
   private static void lambda$blendFunc$5(GlStateManager.SourceFactor var0, GlStateManager.DestFactor var1) {
      GlStateManager._blendFunc(var0.value, var1.value);
   }

   // $FF: synthetic method
   private static void lambda$depthMask$4(boolean var0) {
      GlStateManager._depthMask(var0);
   }

   // $FF: synthetic method
   private static void lambda$depthFunc$3(int var0) {
      GlStateManager._depthFunc(var0);
   }

   // $FF: synthetic method
   private static void lambda$enableScissor$2(int var0, int var1, int var2, int var3) {
      GlStateManager._enableScissorTest();
      GlStateManager._scissorBox(var0, var1, var2, var3);
   }

   static {
      projectionMatrix.setIdentity();
      savedProjectionMatrix.setIdentity();
      modelViewMatrix.setIdentity();
      textureMatrix.setIdentity();
   }

   public static final class AutoStorageIndexBuffer {
      private final int vertexStride;
      private final int indexStride;
      private final RenderSystem.AutoStorageIndexBuffer.IndexGenerator generator;
      private int name;
      private VertexFormat.IndexType type;
      private int indexCount;

      AutoStorageIndexBuffer(int var1, int var2, RenderSystem.AutoStorageIndexBuffer.IndexGenerator var3) {
         this.type = VertexFormat.IndexType.BYTE;
         this.vertexStride = var1;
         this.indexStride = var2;
         this.generator = var3;
      }

      void ensureStorage(int var1) {
         if (var1 > this.indexCount) {
            RenderSystem.LOGGER.debug("Growing IndexBuffer: Old limit {}, new limit {}.", this.indexCount, var1);
            if (this.name == 0) {
               this.name = GlStateManager._glGenBuffers();
            }

            VertexFormat.IndexType var2 = VertexFormat.IndexType.least(var1);
            int var3 = Mth.roundToward(var1 * var2.bytes, 4);
            GlStateManager._glBindBuffer(34963, this.name);
            GlStateManager._glBufferData(34963, (long)var3, 35048);
            ByteBuffer var4 = GlStateManager._glMapBuffer(34963, 35001);
            if (var4 == null) {
               throw new RuntimeException("Failed to map GL buffer");
            } else {
               this.type = var2;
               it.unimi.dsi.fastutil.ints.IntConsumer var5 = this.intConsumer(var4);

               for(int var6 = 0; var6 < var1; var6 += this.indexStride) {
                  this.generator.accept(var5, var6 * this.vertexStride / this.indexStride);
               }

               GlStateManager._glUnmapBuffer(34963);
               GlStateManager._glBindBuffer(34963, 0);
               this.indexCount = var1;
               BufferUploader.invalidateElementArrayBufferBinding();
            }
         }
      }

      private it.unimi.dsi.fastutil.ints.IntConsumer intConsumer(ByteBuffer var1) {
         switch(this.type) {
         case BYTE:
            return (var1x) -> {
               var1.put((byte)var1x);
            };
         case SHORT:
            return (var1x) -> {
               var1.putShort((short)var1x);
            };
         case INT:
         default:
            Objects.requireNonNull(var1);
            return var1::putInt;
         }
      }

      public int name() {
         return this.name;
      }

      public VertexFormat.IndexType type() {
         return this.type;
      }

      interface IndexGenerator {
         void accept(it.unimi.dsi.fastutil.ints.IntConsumer var1, int var2);
      }
   }
}
