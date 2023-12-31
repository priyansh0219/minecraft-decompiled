package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Queues;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.OfflineSocialInteractions;
import com.mojang.authlib.minecraft.SocialInteractionsService;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.GlDebug;
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Function4;
import com.mojang.math.Matrix4f;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.FileUtil;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DatapackLoadFailureScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.OutOfMemoryScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.profiling.ClientMetricsSamplersProvider;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.VirtualScreen;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.client.resources.FoliageColorReloadListener;
import net.minecraft.client.resources.GrassColorReloadListener;
import net.minecraft.client.resources.LegacyPackResourcesAdapter;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.client.resources.PackResourcesAdapterV4;
import net.minecraft.client.resources.PaintingTextureManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.searchtree.MutableSearchTree;
import net.minecraft.client.searchtree.ReloadableIdSearchTree;
import net.minecraft.client.searchtree.ReloadableSearchTree;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.KeybindComponent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.RegistryWriteOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerResources;
import net.minecraft.server.level.progress.ProcessorChunkProgressListener;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadableResourceManager;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FileZipper;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Unit;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.ContinuousProfiler;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.ResultField;
import net.minecraft.util.profiling.SingleTickProfiler;
import net.minecraft.util.profiling.metrics.profiling.ActiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.InactiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorder;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Snooper;
import net.minecraft.world.SnooperPopulator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public class Minecraft extends ReentrantBlockableEventLoop<Runnable> implements SnooperPopulator, WindowEventHandler {
   private static Minecraft instance;
   private static final Logger LOGGER = LogManager.getLogger();
   public static final boolean ON_OSX;
   private static final int MAX_TICKS_PER_UPDATE = 10;
   public static final ResourceLocation DEFAULT_FONT;
   public static final ResourceLocation UNIFORM_FONT;
   public static final ResourceLocation ALT_FONT;
   private static final CompletableFuture<Unit> RESOURCE_RELOAD_INITIAL_TASK;
   private static final Component SOCIAL_INTERACTIONS_NOT_AVAILABLE;
   public static final String UPDATE_DRIVERS_ADVICE = "Please make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).";
   private final File resourcePackDirectory;
   private final PropertyMap profileProperties;
   private final TextureManager textureManager;
   private final DataFixer fixerUpper;
   private final VirtualScreen virtualScreen;
   private final Window window;
   private final Timer timer = new Timer(20.0F, 0L);
   private final Snooper snooper = new Snooper("client", this, Util.getMillis());
   private final RenderBuffers renderBuffers;
   public final LevelRenderer levelRenderer;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final ItemRenderer itemRenderer;
   private final ItemInHandRenderer itemInHandRenderer;
   public final ParticleEngine particleEngine;
   private final SearchRegistry searchRegistry = new SearchRegistry();
   private final User user;
   public final Font font;
   public final GameRenderer gameRenderer;
   public final DebugRenderer debugRenderer;
   private final AtomicReference<StoringChunkProgressListener> progressListener = new AtomicReference();
   public final Gui gui;
   public final Options options;
   private final HotbarManager hotbarManager;
   public final MouseHandler mouseHandler;
   public final KeyboardHandler keyboardHandler;
   public final File gameDirectory;
   private final String launchedVersion;
   private final String versionType;
   private final Proxy proxy;
   private final LevelStorageSource levelSource;
   public final FrameTimer frameTimer = new FrameTimer();
   private final boolean is64bit;
   private final boolean demo;
   private final boolean allowsMultiplayer;
   private final boolean allowsChat;
   private final ReloadableResourceManager resourceManager;
   private final ClientPackSource clientPackSource;
   private final PackRepository resourcePackRepository;
   private final LanguageManager languageManager;
   private final BlockColors blockColors;
   private final ItemColors itemColors;
   private final RenderTarget mainRenderTarget;
   private final SoundManager soundManager;
   private final MusicManager musicManager;
   private final FontManager fontManager;
   private final SplashManager splashManager;
   private final GpuWarnlistManager gpuWarnlistManager;
   private final MinecraftSessionService minecraftSessionService;
   private final SocialInteractionsService socialInteractionsService;
   private final SkinManager skinManager;
   private final ModelManager modelManager;
   private final BlockRenderDispatcher blockRenderer;
   private final PaintingTextureManager paintingTextures;
   private final MobEffectTextureManager mobEffectTextures;
   private final ToastComponent toast;
   private final Game game = new Game(this);
   private final Tutorial tutorial;
   private final PlayerSocialManager playerSocialManager;
   private final EntityModelSet entityModels;
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   public static byte[] reserve;
   @Nullable
   public MultiPlayerGameMode gameMode;
   @Nullable
   public ClientLevel level;
   @Nullable
   public LocalPlayer player;
   @Nullable
   private IntegratedServer singleplayerServer;
   @Nullable
   private ServerData currentServer;
   @Nullable
   private Connection pendingConnection;
   private boolean isLocalServer;
   @Nullable
   public Entity cameraEntity;
   @Nullable
   public Entity crosshairPickEntity;
   @Nullable
   public HitResult hitResult;
   private int rightClickDelay;
   protected int missTime;
   private boolean pause;
   private float pausePartialTick;
   private long lastNanoTime = Util.getNanos();
   private long lastTime;
   private int frames;
   public boolean noRender;
   @Nullable
   public Screen screen;
   @Nullable
   private Overlay overlay;
   private boolean connectedToRealms;
   private Thread gameThread;
   private volatile boolean running = true;
   @Nullable
   private CrashReport delayedCrash;
   private static int fps;
   public String fpsString = "";
   public boolean wireframe;
   public boolean chunkPath;
   public boolean chunkVisibility;
   public boolean smartCull = true;
   private boolean windowActive;
   private final Queue<Runnable> progressTasks = Queues.newConcurrentLinkedQueue();
   @Nullable
   private CompletableFuture<Void> pendingReload;
   @Nullable
   private TutorialToast socialInteractionsToast;
   private ProfilerFiller profiler;
   private int fpsPieRenderTicks;
   private final ContinuousProfiler fpsPieProfiler;
   @Nullable
   private ProfileResults fpsPieResults;
   private MetricsRecorder metricsRecorder;
   private final ResourceLoadStateTracker reloadStateTracker;
   private String debugPath;

   public Minecraft(GameConfig var1) {
      super("Client");
      this.profiler = InactiveProfiler.INSTANCE;
      this.fpsPieProfiler = new ContinuousProfiler(Util.timeSource, () -> {
         return this.fpsPieRenderTicks;
      });
      this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
      this.reloadStateTracker = new ResourceLoadStateTracker();
      this.debugPath = "root";
      instance = this;
      this.gameDirectory = var1.location.gameDirectory;
      File var2 = var1.location.assetDirectory;
      this.resourcePackDirectory = var1.location.resourcePackDirectory;
      this.launchedVersion = var1.game.launchVersion;
      this.versionType = var1.game.versionType;
      this.profileProperties = var1.user.profileProperties;
      this.clientPackSource = new ClientPackSource(new File(this.gameDirectory, "server-resource-packs"), var1.location.getAssetIndex());
      this.resourcePackRepository = new PackRepository(Minecraft::createClientPackAdapter, new RepositorySource[]{this.clientPackSource, new FolderRepositorySource(this.resourcePackDirectory, PackSource.DEFAULT)});
      this.proxy = var1.user.proxy;
      YggdrasilAuthenticationService var3 = new YggdrasilAuthenticationService(this.proxy);
      this.minecraftSessionService = var3.createMinecraftSessionService();
      this.socialInteractionsService = this.createSocialInteractions(var3, var1);
      this.user = var1.user.user;
      LOGGER.info("Setting user: {}", this.user.getName());
      LOGGER.debug("(Session ID is {})", this.user.getSessionId());
      this.demo = var1.game.demo;
      this.allowsMultiplayer = !var1.game.disableMultiplayer;
      this.allowsChat = !var1.game.disableChat;
      this.is64bit = checkIs64Bit();
      this.singleplayerServer = null;
      String var4;
      int var5;
      if (this.allowsMultiplayer() && var1.server.hostname != null) {
         var4 = var1.server.hostname;
         var5 = var1.server.port;
      } else {
         var4 = null;
         var5 = 0;
      }

      KeybindComponent.setKeyResolver(KeyMapping::createNameSupplier);
      this.fixerUpper = DataFixers.getDataFixer();
      this.toast = new ToastComponent(this);
      this.gameThread = Thread.currentThread();
      this.options = new Options(this, this.gameDirectory);
      this.tutorial = new Tutorial(this, this.options);
      this.hotbarManager = new HotbarManager(this.gameDirectory, this.fixerUpper);
      LOGGER.info("Backend library: {}", RenderSystem.getBackendDescription());
      DisplayData var6;
      if (this.options.overrideHeight > 0 && this.options.overrideWidth > 0) {
         var6 = new DisplayData(this.options.overrideWidth, this.options.overrideHeight, var1.display.fullscreenWidth, var1.display.fullscreenHeight, var1.display.isFullscreen);
      } else {
         var6 = var1.display;
      }

      Util.timeSource = RenderSystem.initBackendSystem();
      this.virtualScreen = new VirtualScreen(this);
      this.window = this.virtualScreen.newWindow(var6, this.options.fullscreenVideoModeString, this.createTitle());
      this.setWindowActive(true);

      try {
         InputStream var7 = this.getClientPackSource().getVanillaPack().getResource(PackType.CLIENT_RESOURCES, new ResourceLocation("icons/icon_16x16.png"));
         InputStream var8 = this.getClientPackSource().getVanillaPack().getResource(PackType.CLIENT_RESOURCES, new ResourceLocation("icons/icon_32x32.png"));
         this.window.setIcon(var7, var8);
      } catch (IOException var9) {
         LOGGER.error("Couldn't set icon", var9);
      }

      this.window.setFramerateLimit(this.options.framerateLimit);
      this.mouseHandler = new MouseHandler(this);
      this.mouseHandler.setup(this.window.getWindow());
      this.keyboardHandler = new KeyboardHandler(this);
      this.keyboardHandler.setup(this.window.getWindow());
      RenderSystem.initRenderer(this.options.glDebugVerbosity, false);
      this.mainRenderTarget = new MainTarget(this.window.getWidth(), this.window.getHeight());
      this.mainRenderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      this.mainRenderTarget.clear(ON_OSX);
      this.resourceManager = new SimpleReloadableResourceManager(PackType.CLIENT_RESOURCES);
      this.resourcePackRepository.reload();
      this.options.loadSelectedResourcePacks(this.resourcePackRepository);
      this.languageManager = new LanguageManager(this.options.languageCode);
      this.resourceManager.registerReloadListener(this.languageManager);
      this.textureManager = new TextureManager(this.resourceManager);
      this.resourceManager.registerReloadListener(this.textureManager);
      this.skinManager = new SkinManager(this.textureManager, new File(var2, "skins"), this.minecraftSessionService);
      this.levelSource = new LevelStorageSource(this.gameDirectory.toPath().resolve("saves"), this.gameDirectory.toPath().resolve("backups"), this.fixerUpper);
      this.soundManager = new SoundManager(this.resourceManager, this.options);
      this.resourceManager.registerReloadListener(this.soundManager);
      this.splashManager = new SplashManager(this.user);
      this.resourceManager.registerReloadListener(this.splashManager);
      this.musicManager = new MusicManager(this);
      this.fontManager = new FontManager(this.textureManager);
      this.font = this.fontManager.createFont();
      this.resourceManager.registerReloadListener(this.fontManager.getReloadListener());
      this.selectMainFont(this.isEnforceUnicode());
      this.resourceManager.registerReloadListener(new GrassColorReloadListener());
      this.resourceManager.registerReloadListener(new FoliageColorReloadListener());
      this.window.setErrorSection("Startup");
      RenderSystem.setupDefaultState(0, 0, this.window.getWidth(), this.window.getHeight());
      this.window.setErrorSection("Post startup");
      this.blockColors = BlockColors.createDefault();
      this.itemColors = ItemColors.createDefault(this.blockColors);
      this.modelManager = new ModelManager(this.textureManager, this.blockColors, this.options.mipmapLevels);
      this.resourceManager.registerReloadListener(this.modelManager);
      this.entityModels = new EntityModelSet();
      this.resourceManager.registerReloadListener(this.entityModels);
      this.blockEntityRenderDispatcher = new BlockEntityRenderDispatcher(this.font, this.entityModels, this::getBlockRenderer);
      this.resourceManager.registerReloadListener(this.blockEntityRenderDispatcher);
      BlockEntityWithoutLevelRenderer var10 = new BlockEntityWithoutLevelRenderer(this.blockEntityRenderDispatcher, this.entityModels);
      this.resourceManager.registerReloadListener(var10);
      this.itemRenderer = new ItemRenderer(this.textureManager, this.modelManager, this.itemColors, var10);
      this.entityRenderDispatcher = new EntityRenderDispatcher(this.textureManager, this.itemRenderer, this.font, this.options, this.entityModels);
      this.resourceManager.registerReloadListener(this.entityRenderDispatcher);
      this.itemInHandRenderer = new ItemInHandRenderer(this);
      this.resourceManager.registerReloadListener(this.itemRenderer);
      this.renderBuffers = new RenderBuffers();
      this.gameRenderer = new GameRenderer(this, this.resourceManager, this.renderBuffers);
      this.resourceManager.registerReloadListener(this.gameRenderer);
      this.playerSocialManager = new PlayerSocialManager(this, this.socialInteractionsService);
      this.blockRenderer = new BlockRenderDispatcher(this.modelManager.getBlockModelShaper(), var10, this.blockColors);
      this.resourceManager.registerReloadListener(this.blockRenderer);
      this.levelRenderer = new LevelRenderer(this, this.renderBuffers);
      this.resourceManager.registerReloadListener(this.levelRenderer);
      this.createSearchTrees();
      this.resourceManager.registerReloadListener(this.searchRegistry);
      this.particleEngine = new ParticleEngine(this.level, this.textureManager);
      this.resourceManager.registerReloadListener(this.particleEngine);
      this.paintingTextures = new PaintingTextureManager(this.textureManager);
      this.resourceManager.registerReloadListener(this.paintingTextures);
      this.mobEffectTextures = new MobEffectTextureManager(this.textureManager);
      this.resourceManager.registerReloadListener(this.mobEffectTextures);
      this.gpuWarnlistManager = new GpuWarnlistManager();
      this.resourceManager.registerReloadListener(this.gpuWarnlistManager);
      this.gui = new Gui(this);
      this.debugRenderer = new DebugRenderer(this);
      RenderSystem.setErrorCallback(this::onFullscreenError);
      if (this.mainRenderTarget.width == this.window.getWidth() && this.mainRenderTarget.height == this.window.getHeight()) {
         if (this.options.fullscreen && !this.window.isFullscreen()) {
            this.window.toggleFullScreen();
            this.options.fullscreen = this.window.isFullscreen();
         }
      } else {
         int var10002 = this.window.getWidth();
         StringBuilder var11 = new StringBuilder("Recovering from unsupported resolution (" + var10002 + "x" + this.window.getHeight() + ").\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).");
         if (GlDebug.isDebugEnabled()) {
            var11.append("\n\nReported GL debug messages:\n").append(String.join("\n", GlDebug.getLastOpenGlDebugMessages()));
         }

         this.window.setWindowed(this.mainRenderTarget.width, this.mainRenderTarget.height);
         TinyFileDialogs.tinyfd_messageBox("Minecraft", var11.toString(), "ok", "error", false);
      }

      this.window.updateVsync(this.options.enableVsync);
      this.window.updateRawMouseInput(this.options.rawMouseInput);
      this.window.setDefaultErrorCallback();
      this.resizeDisplay();
      if (var4 != null) {
         ConnectScreen.startConnecting(new TitleScreen(), this, new ServerAddress(var4, var5), (ServerData)null);
      } else {
         this.setScreen(new TitleScreen(true));
      }

      this.gameRenderer.preloadUiShader(this.getClientPackSource().getVanillaPack());
      LoadingOverlay.registerTextures(this);
      List var12 = this.resourcePackRepository.openAllSelected();
      this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.INITIAL, var12);
      this.setOverlay(new LoadingOverlay(this, this.resourceManager.createReload(Util.backgroundExecutor(), this, RESOURCE_RELOAD_INITIAL_TASK, var12), (var1x) -> {
         Util.ifElse(var1x, this::rollbackResourcePacks, () -> {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
               this.selfTest();
            }

            this.reloadStateTracker.finishReload();
         });
      }, false));
   }

   public void updateTitle() {
      this.window.setTitle(this.createTitle());
   }

   private String createTitle() {
      StringBuilder var1 = new StringBuilder("Minecraft");
      if (this.isProbablyModded()) {
         var1.append("*");
      }

      var1.append(" ");
      var1.append(SharedConstants.getCurrentVersion().getName());
      ClientPacketListener var2 = this.getConnection();
      if (var2 != null && var2.getConnection().isConnected()) {
         var1.append(" - ");
         if (this.singleplayerServer != null && !this.singleplayerServer.isPublished()) {
            var1.append(I18n.get("title.singleplayer"));
         } else if (this.isConnectedToRealms()) {
            var1.append(I18n.get("title.multiplayer.realms"));
         } else if (this.singleplayerServer == null && (this.currentServer == null || !this.currentServer.isLan())) {
            var1.append(I18n.get("title.multiplayer.other"));
         } else {
            var1.append(I18n.get("title.multiplayer.lan"));
         }
      }

      return var1.toString();
   }

   private SocialInteractionsService createSocialInteractions(YggdrasilAuthenticationService var1, GameConfig var2) {
      try {
         return var1.createSocialInteractionsService(var2.user.user.getAccessToken());
      } catch (AuthenticationException var4) {
         LOGGER.error("Failed to verify authentication", var4);
         return new OfflineSocialInteractions();
      }
   }

   public boolean isProbablyModded() {
      return !"vanilla".equals(ClientBrandRetriever.getClientModName()) || Minecraft.class.getSigners() == null;
   }

   private void rollbackResourcePacks(Throwable var1) {
      if (this.resourcePackRepository.getSelectedIds().size() > 1) {
         TextComponent var2;
         if (var1 instanceof SimpleReloadableResourceManager.ResourcePackLoadingFailure) {
            var2 = new TextComponent(((SimpleReloadableResourceManager.ResourcePackLoadingFailure)var1).getPack().getName());
         } else {
            var2 = null;
         }

         this.clearResourcePacksOnError(var1, var2);
      } else {
         Util.throwAsRuntime(var1);
      }

   }

   public void clearResourcePacksOnError(Throwable var1, @Nullable Component var2) {
      LOGGER.info("Caught error loading resourcepacks, removing all selected resourcepacks", var1);
      this.reloadStateTracker.startRecovery(var1);
      this.resourcePackRepository.setSelected(Collections.emptyList());
      this.options.resourcePacks.clear();
      this.options.incompatibleResourcePacks.clear();
      this.options.save();
      this.reloadResourcePacks(true).thenRun(() -> {
         ToastComponent var2x = this.getToasts();
         SystemToast.addOrUpdate(var2x, SystemToast.SystemToastIds.PACK_LOAD_FAILURE, new TranslatableComponent("resourcePack.load_fail"), var2);
      });
   }

   public void run() {
      this.gameThread = Thread.currentThread();

      try {
         boolean var1 = false;

         while(this.running) {
            if (this.delayedCrash != null) {
               crash(this.delayedCrash);
               return;
            }

            try {
               SingleTickProfiler var7 = SingleTickProfiler.createTickProfiler("Renderer");
               boolean var3 = this.shouldRenderFpsPie();
               this.profiler = this.constructProfiler(var3, var7);
               this.profiler.startTick();
               this.metricsRecorder.startTick();
               this.runTick(!var1);
               this.metricsRecorder.endTick();
               this.profiler.endTick();
               this.finishProfilers(var3, var7);
            } catch (OutOfMemoryError var4) {
               if (var1) {
                  throw var4;
               }

               this.emergencySave();
               this.setScreen(new OutOfMemoryScreen());
               System.gc();
               LOGGER.fatal("Out of memory", var4);
               var1 = true;
            }
         }
      } catch (ReportedException var5) {
         this.fillReport(var5.getReport());
         this.emergencySave();
         LOGGER.fatal("Reported exception thrown!", var5);
         crash(var5.getReport());
      } catch (Throwable var6) {
         CrashReport var2 = this.fillReport(new CrashReport("Unexpected error", var6));
         LOGGER.fatal("Unreported exception thrown!", var6);
         this.emergencySave();
         crash(var2);
      }

   }

   void selectMainFont(boolean var1) {
      this.fontManager.setRenames(var1 ? ImmutableMap.of(DEFAULT_FONT, UNIFORM_FONT) : ImmutableMap.of());
   }

   private void createSearchTrees() {
      ReloadableSearchTree var1 = new ReloadableSearchTree((var0) -> {
         return var0.getTooltipLines((Player)null, TooltipFlag.Default.NORMAL).stream().map((var0x) -> {
            return ChatFormatting.stripFormatting(var0x.getString()).trim();
         }).filter((var0x) -> {
            return !var0x.isEmpty();
         });
      }, (var0) -> {
         return Stream.of(Registry.ITEM.getKey(var0.getItem()));
      });
      ReloadableIdSearchTree var2 = new ReloadableIdSearchTree((var0) -> {
         return ItemTags.getAllTags().getMatchingTags(var0.getItem()).stream();
      });
      NonNullList var3 = NonNullList.create();
      Iterator var4 = Registry.ITEM.iterator();

      while(var4.hasNext()) {
         Item var5 = (Item)var4.next();
         var5.fillItemCategory(CreativeModeTab.TAB_SEARCH, var3);
      }

      var3.forEach((var2x) -> {
         var1.add(var2x);
         var2.add(var2x);
      });
      ReloadableSearchTree var6 = new ReloadableSearchTree((var0) -> {
         return var0.getRecipes().stream().flatMap((var0x) -> {
            return var0x.getResultItem().getTooltipLines((Player)null, TooltipFlag.Default.NORMAL).stream();
         }).map((var0x) -> {
            return ChatFormatting.stripFormatting(var0x.getString()).trim();
         }).filter((var0x) -> {
            return !var0x.isEmpty();
         });
      }, (var0) -> {
         return var0.getRecipes().stream().map((var0x) -> {
            return Registry.ITEM.getKey(var0x.getResultItem().getItem());
         });
      });
      this.searchRegistry.register(SearchRegistry.CREATIVE_NAMES, var1);
      this.searchRegistry.register(SearchRegistry.CREATIVE_TAGS, var2);
      this.searchRegistry.register(SearchRegistry.RECIPE_COLLECTIONS, var6);
   }

   private void onFullscreenError(int var1, long var2) {
      this.options.enableVsync = false;
      this.options.save();
   }

   private static boolean checkIs64Bit() {
      String[] var0 = new String[]{"sun.arch.data.model", "com.ibm.vm.bitmode", "os.arch"};
      String[] var1 = var0;
      int var2 = var0.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         String var4 = var1[var3];
         String var5 = System.getProperty(var4);
         if (var5 != null && var5.contains("64")) {
            return true;
         }
      }

      return false;
   }

   public RenderTarget getMainRenderTarget() {
      return this.mainRenderTarget;
   }

   public String getLaunchedVersion() {
      return this.launchedVersion;
   }

   public String getVersionType() {
      return this.versionType;
   }

   public void delayCrash(CrashReport var1) {
      this.delayedCrash = var1;
   }

   public static void crash(CrashReport var0) {
      File var1 = new File(getInstance().gameDirectory, "crash-reports");
      SimpleDateFormat var10003 = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
      Date var10004 = new Date();
      File var2 = new File(var1, "crash-" + var10003.format(var10004) + "-client.txt");
      Bootstrap.realStdoutPrintln(var0.getFriendlyReport());
      if (var0.getSaveFile() != null) {
         Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + var0.getSaveFile());
         System.exit(-1);
      } else if (var0.saveToFile(var2)) {
         Bootstrap.realStdoutPrintln("#@!@# Game crashed! Crash report saved to: #@!@# " + var2.getAbsolutePath());
         System.exit(-1);
      } else {
         Bootstrap.realStdoutPrintln("#@?@# Game crashed! Crash report could not be saved. #@?@#");
         System.exit(-2);
      }

   }

   public boolean isEnforceUnicode() {
      return this.options.forceUnicodeFont;
   }

   public CompletableFuture<Void> reloadResourcePacks() {
      return this.reloadResourcePacks(false);
   }

   private CompletableFuture<Void> reloadResourcePacks(boolean var1) {
      if (this.pendingReload != null) {
         return this.pendingReload;
      } else {
         CompletableFuture var2 = new CompletableFuture();
         if (!var1 && this.overlay instanceof LoadingOverlay) {
            this.pendingReload = var2;
            return var2;
         } else {
            this.resourcePackRepository.reload();
            List var3 = this.resourcePackRepository.openAllSelected();
            if (!var1) {
               this.reloadStateTracker.startReload(ResourceLoadStateTracker.ReloadReason.MANUAL, var3);
            }

            this.setOverlay(new LoadingOverlay(this, this.resourceManager.createReload(Util.backgroundExecutor(), this, RESOURCE_RELOAD_INITIAL_TASK, var3), (var2x) -> {
               Util.ifElse(var2x, this::rollbackResourcePacks, () -> {
                  this.levelRenderer.allChanged();
                  this.reloadStateTracker.finishReload();
                  var2.complete((Object)null);
               });
            }, true));
            return var2;
         }
      }
   }

   private void selfTest() {
      boolean var1 = false;
      BlockModelShaper var2 = this.getBlockRenderer().getBlockModelShaper();
      BakedModel var3 = var2.getModelManager().getMissingModel();
      Iterator var4 = Registry.BLOCK.iterator();

      while(var4.hasNext()) {
         Block var5 = (Block)var4.next();
         UnmodifiableIterator var6 = var5.getStateDefinition().getPossibleStates().iterator();

         while(var6.hasNext()) {
            BlockState var7 = (BlockState)var6.next();
            if (var7.getRenderShape() == RenderShape.MODEL) {
               BakedModel var8 = var2.getBlockModel(var7);
               if (var8 == var3) {
                  LOGGER.debug("Missing model for: {}", var7);
                  var1 = true;
               }
            }
         }
      }

      TextureAtlasSprite var12 = var3.getParticleIcon();
      Iterator var13 = Registry.BLOCK.iterator();

      while(var13.hasNext()) {
         Block var15 = (Block)var13.next();
         UnmodifiableIterator var17 = var15.getStateDefinition().getPossibleStates().iterator();

         while(var17.hasNext()) {
            BlockState var19 = (BlockState)var17.next();
            TextureAtlasSprite var9 = var2.getParticleIcon(var19);
            if (!var19.isAir() && var9 == var12) {
               LOGGER.debug("Missing particle icon for: {}", var19);
               var1 = true;
            }
         }
      }

      NonNullList var14 = NonNullList.create();
      Iterator var16 = Registry.ITEM.iterator();

      while(var16.hasNext()) {
         Item var18 = (Item)var16.next();
         var14.clear();
         var18.fillItemCategory(CreativeModeTab.TAB_SEARCH, var14);
         Iterator var20 = var14.iterator();

         while(var20.hasNext()) {
            ItemStack var21 = (ItemStack)var20.next();
            String var10 = var21.getDescriptionId();
            String var11 = (new TranslatableComponent(var10)).getString();
            if (var11.toLowerCase(Locale.ROOT).equals(var18.getDescriptionId())) {
               LOGGER.debug("Missing translation for: {} {} {}", var21, var10, var21.getItem());
            }
         }
      }

      var1 |= MenuScreens.selfTest();
      var1 |= EntityRenderers.validateRegistrations();
      if (var1) {
         throw new IllegalStateException("Your game data is foobar, fix the errors above!");
      }
   }

   public LevelStorageSource getLevelSource() {
      return this.levelSource;
   }

   private void openChatScreen(String var1) {
      Minecraft.ChatStatus var2 = this.getChatStatus();
      if (!var2.isChatAllowed(this.isLocalServer())) {
         this.gui.setOverlayMessage(var2.getMessage(), false);
      } else {
         this.setScreen(new ChatScreen(var1));
      }

   }

   public void setScreen(@Nullable Screen var1) {
      if (SharedConstants.IS_RUNNING_IN_IDE && Thread.currentThread() != this.gameThread) {
         LOGGER.error("setScreen called from non-game thread");
      }

      if (this.screen != null) {
         this.screen.removed();
      }

      if (var1 == null && this.level == null) {
         var1 = new TitleScreen();
      } else if (var1 == null && this.player.isDeadOrDying()) {
         if (this.player.shouldShowDeathScreen()) {
            var1 = new DeathScreen((Component)null, this.level.getLevelData().isHardcore());
         } else {
            this.player.respawn();
         }
      }

      this.screen = (Screen)var1;
      BufferUploader.reset();
      if (var1 != null) {
         this.mouseHandler.releaseMouse();
         KeyMapping.releaseAll();
         ((Screen)var1).init(this, this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
         this.noRender = false;
      } else {
         this.soundManager.resume();
         this.mouseHandler.grabMouse();
      }

      this.updateTitle();
   }

   public void setOverlay(@Nullable Overlay var1) {
      this.overlay = var1;
   }

   public void destroy() {
      try {
         LOGGER.info("Stopping!");

         try {
            NarratorChatListener.INSTANCE.destroy();
         } catch (Throwable var7) {
         }

         try {
            if (this.level != null) {
               this.level.disconnect();
            }

            this.clearLevel();
         } catch (Throwable var6) {
         }

         if (this.screen != null) {
            this.screen.removed();
         }

         this.close();
      } finally {
         Util.timeSource = System::nanoTime;
         if (this.delayedCrash == null) {
            System.exit(0);
         }

      }

   }

   public void close() {
      try {
         this.modelManager.close();
         this.fontManager.close();
         this.gameRenderer.close();
         this.levelRenderer.close();
         this.soundManager.destroy();
         this.resourcePackRepository.close();
         this.particleEngine.close();
         this.mobEffectTextures.close();
         this.paintingTextures.close();
         this.textureManager.close();
         this.resourceManager.close();
         Util.shutdownExecutors();
      } catch (Throwable var5) {
         LOGGER.error("Shutdown failure!", var5);
         throw var5;
      } finally {
         this.virtualScreen.close();
         this.window.close();
      }

   }

   private void runTick(boolean var1) {
      this.window.setErrorSection("Pre render");
      long var2 = Util.getNanos();
      if (this.window.shouldClose()) {
         this.stop();
      }

      if (this.pendingReload != null && !(this.overlay instanceof LoadingOverlay)) {
         CompletableFuture var4 = this.pendingReload;
         this.pendingReload = null;
         this.reloadResourcePacks().thenRun(() -> {
            var4.complete((Object)null);
         });
      }

      Runnable var10;
      while((var10 = (Runnable)this.progressTasks.poll()) != null) {
         var10.run();
      }

      int var6;
      if (var1) {
         int var5 = this.timer.advanceTime(Util.getMillis());
         this.profiler.push("scheduledExecutables");
         this.runAllTasks();
         this.profiler.pop();
         this.profiler.push("tick");

         for(var6 = 0; var6 < Math.min(10, var5); ++var6) {
            this.profiler.incrementCounter("clientTick");
            this.tick();
         }

         this.profiler.pop();
      }

      this.mouseHandler.turnPlayer();
      this.window.setErrorSection("Render");
      this.profiler.push("sound");
      this.soundManager.updateSource(this.gameRenderer.getMainCamera());
      this.profiler.pop();
      this.profiler.push("render");
      PoseStack var11 = RenderSystem.getModelViewStack();
      var11.pushPose();
      RenderSystem.applyModelViewMatrix();
      RenderSystem.clear(16640, ON_OSX);
      this.mainRenderTarget.bindWrite(true);
      FogRenderer.setupNoFog();
      this.profiler.push("display");
      RenderSystem.enableTexture();
      RenderSystem.enableCull();
      this.profiler.pop();
      if (!this.noRender) {
         this.profiler.popPush("gameRenderer");
         this.gameRenderer.render(this.pause ? this.pausePartialTick : this.timer.partialTick, var2, var1);
         this.profiler.popPush("toasts");
         this.toast.render(new PoseStack());
         this.profiler.pop();
      }

      if (this.fpsPieResults != null) {
         this.profiler.push("fpsPie");
         this.renderFpsMeter(new PoseStack(), this.fpsPieResults);
         this.profiler.pop();
      }

      this.profiler.push("blit");
      this.mainRenderTarget.unbindWrite();
      var11.popPose();
      var11.pushPose();
      RenderSystem.applyModelViewMatrix();
      this.mainRenderTarget.blitToScreen(this.window.getWidth(), this.window.getHeight());
      var11.popPose();
      RenderSystem.applyModelViewMatrix();
      this.profiler.popPush("updateDisplay");
      this.window.updateDisplay();
      var6 = this.getFramerateLimit();
      if ((double)var6 < Option.FRAMERATE_LIMIT.getMaxValue()) {
         RenderSystem.limitDisplayFPS(var6);
      }

      this.profiler.popPush("yield");
      Thread.yield();
      this.profiler.pop();
      this.window.setErrorSection("Post render");
      ++this.frames;
      boolean var7 = this.hasSingleplayerServer() && (this.screen != null && this.screen.isPauseScreen() || this.overlay != null && this.overlay.isPauseScreen()) && !this.singleplayerServer.isPublished();
      if (this.pause != var7) {
         if (this.pause) {
            this.pausePartialTick = this.timer.partialTick;
         } else {
            this.timer.partialTick = this.pausePartialTick;
         }

         this.pause = var7;
      }

      long var8 = Util.getNanos();
      this.frameTimer.logFrameDuration(var8 - this.lastNanoTime);
      this.lastNanoTime = var8;
      this.profiler.push("fpsUpdate");

      while(Util.getMillis() >= this.lastTime + 1000L) {
         fps = this.frames;
         this.fpsString = String.format("%d fps T: %s%s%s%s B: %d", fps, (double)this.options.framerateLimit == Option.FRAMERATE_LIMIT.getMaxValue() ? "inf" : this.options.framerateLimit, this.options.enableVsync ? " vsync" : "", this.options.graphicsMode.toString(), this.options.renderClouds == CloudStatus.OFF ? "" : (this.options.renderClouds == CloudStatus.FAST ? " fast-clouds" : " fancy-clouds"), this.options.biomeBlendRadius);
         this.lastTime += 1000L;
         this.frames = 0;
         this.snooper.prepare();
         if (!this.snooper.isStarted()) {
            this.snooper.start();
         }
      }

      this.profiler.pop();
   }

   private boolean shouldRenderFpsPie() {
      return this.options.renderDebug && this.options.renderDebugCharts && !this.options.hideGui;
   }

   private ProfilerFiller constructProfiler(boolean var1, @Nullable SingleTickProfiler var2) {
      if (!var1 && !this.metricsRecorder.isRecording()) {
         return (ProfilerFiller)(var2 == null ? InactiveProfiler.INSTANCE : var2.startTick());
      } else if (var1) {
         if (!this.fpsPieProfiler.isEnabled()) {
            this.fpsPieRenderTicks = 0;
            this.fpsPieProfiler.enable();
         }

         ++this.fpsPieRenderTicks;
         ProfilerFiller var3 = this.metricsRecorder.isRecording() ? ProfilerFiller.tee(this.fpsPieProfiler.getFiller(), this.metricsRecorder.getProfiler()) : this.fpsPieProfiler.getFiller();
         return SingleTickProfiler.decorateFiller(var3, var2);
      } else {
         if (this.fpsPieProfiler.isEnabled()) {
            this.fpsPieProfiler.disable();
         }

         return SingleTickProfiler.decorateFiller(this.metricsRecorder.getProfiler(), var2);
      }
   }

   private void finishProfilers(boolean var1, @Nullable SingleTickProfiler var2) {
      if (var2 != null) {
         var2.endTick();
      }

      if (var1) {
         this.fpsPieResults = this.fpsPieProfiler.getResults();
      } else {
         this.fpsPieResults = null;
      }

      this.profiler = this.fpsPieProfiler.getFiller();
   }

   public void resizeDisplay() {
      int var1 = this.window.calculateScale(this.options.guiScale, this.isEnforceUnicode());
      this.window.setGuiScale((double)var1);
      if (this.screen != null) {
         this.screen.resize(this, this.window.getGuiScaledWidth(), this.window.getGuiScaledHeight());
      }

      RenderTarget var2 = this.getMainRenderTarget();
      var2.resize(this.window.getWidth(), this.window.getHeight(), ON_OSX);
      this.gameRenderer.resize(this.window.getWidth(), this.window.getHeight());
      this.mouseHandler.setIgnoreFirstMove();
   }

   public void cursorEntered() {
      this.mouseHandler.cursorEntered();
   }

   private int getFramerateLimit() {
      return this.level != null || this.screen == null && this.overlay == null ? this.window.getFramerateLimit() : 60;
   }

   public void emergencySave() {
      try {
         reserve = new byte[0];
         this.levelRenderer.clear();
      } catch (Throwable var3) {
      }

      try {
         System.gc();
         if (this.isLocalServer && this.singleplayerServer != null) {
            this.singleplayerServer.halt(true);
         }

         this.clearLevel(new GenericDirtMessageScreen(new TranslatableComponent("menu.savingLevel")));
      } catch (Throwable var2) {
      }

      System.gc();
   }

   public boolean debugClientMetricsStart(Consumer<TranslatableComponent> var1) {
      if (this.metricsRecorder.isRecording()) {
         this.debugClientMetricsStop();
         return false;
      } else {
         Consumer var2 = (var2x) -> {
            int var3 = var2x.getTickDuration();
            double var4 = (double)var2x.getNanoDuration() / (double)TimeUtil.NANOSECONDS_PER_SECOND;
            this.execute(() -> {
               var1.accept(new TranslatableComponent("commands.debug.stopped", new Object[]{String.format(Locale.ROOT, "%.2f", var4), var3, String.format(Locale.ROOT, "%.2f", (double)var3 / var4)}));
            });
         };
         Consumer var3 = (var2x) -> {
            MutableComponent var3 = (new TextComponent(var2x.toString())).withStyle(ChatFormatting.UNDERLINE).withStyle((var1x) -> {
               return var1x.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, var2x.toFile().getParent()));
            });
            this.execute(() -> {
               var1.accept(new TranslatableComponent("debug.profiling.stop", new Object[]{var3}));
            });
         };
         SystemReport var4 = fillSystemReport(new SystemReport(), this, this.languageManager, this.launchedVersion, this.options);
         Consumer var5 = (var3x) -> {
            Path var4x = this.archiveProfilingReport(var4, var3x);
            var3.accept(var4x);
         };
         Consumer var6;
         if (this.singleplayerServer == null) {
            var6 = (var1x) -> {
               var5.accept(ImmutableList.of(var1x));
            };
         } else {
            this.singleplayerServer.fillSystemReport(var4);
            CompletableFuture var7 = new CompletableFuture();
            CompletableFuture var8 = new CompletableFuture();
            CompletableFuture.allOf(var7, var8).thenRunAsync(() -> {
               var5.accept(ImmutableList.of((Path)var7.join(), (Path)var8.join()));
            }, Util.ioPool());
            IntegratedServer var10000 = this.singleplayerServer;
            Consumer var10001 = (var0) -> {
            };
            Objects.requireNonNull(var8);
            var10000.startRecordingMetrics(var10001, var8::complete);
            Objects.requireNonNull(var7);
            var6 = var7::complete;
         }

         this.metricsRecorder = ActiveMetricsRecorder.createStarted(new ClientMetricsSamplersProvider(Util.timeSource, this.levelRenderer), Util.timeSource, Util.ioPool(), new MetricsPersister("client"), (var2x) -> {
            this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
            var2.accept(var2x);
         }, var6);
         return true;
      }
   }

   private void debugClientMetricsStop() {
      this.metricsRecorder.end();
      if (this.singleplayerServer != null) {
         this.singleplayerServer.finishRecordingMetrics();
      }

   }

   private Path archiveProfilingReport(SystemReport var1, List<Path> var2) {
      String var4;
      if (this.isLocalServer()) {
         var4 = this.getSingleplayerServer().getWorldData().getLevelName();
      } else {
         var4 = this.getCurrentServer().name;
      }

      Path var3;
      try {
         String var5 = String.format("%s-%s-%s", (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()), var4, SharedConstants.getCurrentVersion().getId());
         String var6 = FileUtil.findAvailableName(MetricsPersister.PROFILING_RESULTS_DIR, var5, ".zip");
         var3 = MetricsPersister.PROFILING_RESULTS_DIR.resolve(var6);
      } catch (IOException var23) {
         throw new UncheckedIOException(var23);
      }

      boolean var18 = false;

      try {
         var18 = true;
         FileZipper var25 = new FileZipper(var3);

         try {
            var25.add(Paths.get("system.txt"), var1.toLineSeparatedString());
            var25.add(Paths.get("client").resolve(this.options.getFile().getName()), this.options.dumpOptionsForReport());
            Objects.requireNonNull(var25);
            var2.forEach(var25::add);
         } catch (Throwable var22) {
            try {
               var25.close();
            } catch (Throwable var20) {
               var22.addSuppressed(var20);
            }

            throw var22;
         }

         var25.close();
         var18 = false;
      } finally {
         if (var18) {
            Iterator var9 = var2.iterator();

            while(var9.hasNext()) {
               Path var10 = (Path)var9.next();

               try {
                  FileUtils.forceDelete(var10.toFile());
               } catch (IOException var19) {
                  LOGGER.warn("Failed to delete temporary profiling result {}", var10, var19);
               }
            }

         }
      }

      Iterator var26 = var2.iterator();

      while(var26.hasNext()) {
         Path var27 = (Path)var26.next();

         try {
            FileUtils.forceDelete(var27.toFile());
         } catch (IOException var21) {
            LOGGER.warn("Failed to delete temporary profiling result {}", var27, var21);
         }
      }

      return var3;
   }

   public void debugFpsMeterKeyPress(int var1) {
      if (this.fpsPieResults != null) {
         List var2 = this.fpsPieResults.getTimes(this.debugPath);
         if (!var2.isEmpty()) {
            ResultField var3 = (ResultField)var2.remove(0);
            if (var1 == 0) {
               if (!var3.name.isEmpty()) {
                  int var4 = this.debugPath.lastIndexOf(30);
                  if (var4 >= 0) {
                     this.debugPath = this.debugPath.substring(0, var4);
                  }
               }
            } else {
               --var1;
               if (var1 < var2.size() && !"unspecified".equals(((ResultField)var2.get(var1)).name)) {
                  if (!this.debugPath.isEmpty()) {
                     this.debugPath = this.debugPath + "\u001e";
                  }

                  String var10001 = this.debugPath;
                  this.debugPath = var10001 + ((ResultField)var2.get(var1)).name;
               }
            }

         }
      }
   }

   private void renderFpsMeter(PoseStack var1, ProfileResults var2) {
      List var3 = var2.getTimes(this.debugPath);
      ResultField var4 = (ResultField)var3.remove(0);
      RenderSystem.clear(256, ON_OSX);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Matrix4f var5 = Matrix4f.orthographic(0.0F, (float)this.window.getWidth(), 0.0F, (float)this.window.getHeight(), 1000.0F, 3000.0F);
      RenderSystem.setProjectionMatrix(var5);
      PoseStack var6 = RenderSystem.getModelViewStack();
      var6.setIdentity();
      var6.translate(0.0D, 0.0D, -2000.0D);
      RenderSystem.applyModelViewMatrix();
      RenderSystem.lineWidth(1.0F);
      RenderSystem.disableTexture();
      Tesselator var7 = Tesselator.getInstance();
      BufferBuilder var8 = var7.getBuilder();
      boolean var9 = true;
      int var10 = this.window.getWidth() - 160 - 10;
      int var11 = this.window.getHeight() - 320;
      RenderSystem.enableBlend();
      var8.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      var8.vertex((double)((float)var10 - 176.0F), (double)((float)var11 - 96.0F - 16.0F), 0.0D).color(200, 0, 0, 0).endVertex();
      var8.vertex((double)((float)var10 - 176.0F), (double)(var11 + 320), 0.0D).color(200, 0, 0, 0).endVertex();
      var8.vertex((double)((float)var10 + 176.0F), (double)(var11 + 320), 0.0D).color(200, 0, 0, 0).endVertex();
      var8.vertex((double)((float)var10 + 176.0F), (double)((float)var11 - 96.0F - 16.0F), 0.0D).color(200, 0, 0, 0).endVertex();
      var7.end();
      RenderSystem.disableBlend();
      double var12 = 0.0D;

      ResultField var15;
      int var17;
      for(Iterator var14 = var3.iterator(); var14.hasNext(); var12 += var15.percentage) {
         var15 = (ResultField)var14.next();
         int var16 = Mth.floor(var15.percentage / 4.0D) + 1;
         var8.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
         var17 = var15.getColor();
         int var18 = var17 >> 16 & 255;
         int var19 = var17 >> 8 & 255;
         int var20 = var17 & 255;
         var8.vertex((double)var10, (double)var11, 0.0D).color(var18, var19, var20, 255).endVertex();

         int var21;
         float var22;
         float var23;
         float var24;
         for(var21 = var16; var21 >= 0; --var21) {
            var22 = (float)((var12 + var15.percentage * (double)var21 / (double)var16) * 6.2831854820251465D / 100.0D);
            var23 = Mth.sin(var22) * 160.0F;
            var24 = Mth.cos(var22) * 160.0F * 0.5F;
            var8.vertex((double)((float)var10 + var23), (double)((float)var11 - var24), 0.0D).color(var18, var19, var20, 255).endVertex();
         }

         var7.end();
         var8.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

         for(var21 = var16; var21 >= 0; --var21) {
            var22 = (float)((var12 + var15.percentage * (double)var21 / (double)var16) * 6.2831854820251465D / 100.0D);
            var23 = Mth.sin(var22) * 160.0F;
            var24 = Mth.cos(var22) * 160.0F * 0.5F;
            if (!(var24 > 0.0F)) {
               var8.vertex((double)((float)var10 + var23), (double)((float)var11 - var24), 0.0D).color(var18 >> 1, var19 >> 1, var20 >> 1, 255).endVertex();
               var8.vertex((double)((float)var10 + var23), (double)((float)var11 - var24 + 10.0F), 0.0D).color(var18 >> 1, var19 >> 1, var20 >> 1, 255).endVertex();
            }
         }

         var7.end();
      }

      DecimalFormat var25 = new DecimalFormat("##0.00");
      var25.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
      RenderSystem.enableTexture();
      String var26 = ProfileResults.demanglePath(var4.name);
      String var28 = "";
      if (!"unspecified".equals(var26)) {
         var28 = var28 + "[0] ";
      }

      if (var26.isEmpty()) {
         var28 = var28 + "ROOT ";
      } else {
         var28 = var28 + var26 + " ";
      }

      var17 = 16777215;
      this.font.drawShadow(var1, var28, (float)(var10 - 160), (float)(var11 - 80 - 16), 16777215);
      String var10000 = var25.format(var4.globalPercentage);
      var28 = var10000 + "%";
      this.font.drawShadow(var1, var28, (float)(var10 + 160 - this.font.width(var28)), (float)(var11 - 80 - 16), 16777215);

      for(int var27 = 0; var27 < var3.size(); ++var27) {
         ResultField var31 = (ResultField)var3.get(var27);
         StringBuilder var29 = new StringBuilder();
         if ("unspecified".equals(var31.name)) {
            var29.append("[?] ");
         } else {
            var29.append("[").append(var27 + 1).append("] ");
         }

         String var30 = var29.append(var31.name).toString();
         this.font.drawShadow(var1, var30, (float)(var10 - 160), (float)(var11 + 80 + var27 * 8 + 20), var31.getColor());
         var10000 = var25.format(var31.percentage);
         var30 = var10000 + "%";
         this.font.drawShadow(var1, var30, (float)(var10 + 160 - 50 - this.font.width(var30)), (float)(var11 + 80 + var27 * 8 + 20), var31.getColor());
         var10000 = var25.format(var31.globalPercentage);
         var30 = var10000 + "%";
         this.font.drawShadow(var1, var30, (float)(var10 + 160 - this.font.width(var30)), (float)(var11 + 80 + var27 * 8 + 20), var31.getColor());
      }

   }

   public void stop() {
      this.running = false;
   }

   public boolean isRunning() {
      return this.running;
   }

   public void pauseGame(boolean var1) {
      if (this.screen == null) {
         boolean var2 = this.hasSingleplayerServer() && !this.singleplayerServer.isPublished();
         if (var2) {
            this.setScreen(new PauseScreen(!var1));
            this.soundManager.pause();
         } else {
            this.setScreen(new PauseScreen(true));
         }

      }
   }

   private void continueAttack(boolean var1) {
      if (!var1) {
         this.missTime = 0;
      }

      if (this.missTime <= 0 && !this.player.isUsingItem()) {
         if (var1 && this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult var2 = (BlockHitResult)this.hitResult;
            BlockPos var3 = var2.getBlockPos();
            if (!this.level.getBlockState(var3).isAir()) {
               Direction var4 = var2.getDirection();
               if (this.gameMode.continueDestroyBlock(var3, var4)) {
                  this.particleEngine.crack(var3, var4);
                  this.player.swing(InteractionHand.MAIN_HAND);
               }
            }

         } else {
            this.gameMode.stopDestroyBlock();
         }
      }
   }

   private void startAttack() {
      if (this.missTime <= 0) {
         if (this.hitResult == null) {
            LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
            if (this.gameMode.hasMissTime()) {
               this.missTime = 10;
            }

         } else if (!this.player.isHandsBusy()) {
            switch(this.hitResult.getType()) {
            case ENTITY:
               this.gameMode.attack(this.player, ((EntityHitResult)this.hitResult).getEntity());
               break;
            case BLOCK:
               BlockHitResult var1 = (BlockHitResult)this.hitResult;
               BlockPos var2 = var1.getBlockPos();
               if (!this.level.getBlockState(var2).isAir()) {
                  this.gameMode.startDestroyBlock(var2, var1.getDirection());
                  break;
               }
            case MISS:
               if (this.gameMode.hasMissTime()) {
                  this.missTime = 10;
               }

               this.player.resetAttackStrengthTicker();
            }

            this.player.swing(InteractionHand.MAIN_HAND);
         }
      }
   }

   private void startUseItem() {
      if (!this.gameMode.isDestroying()) {
         this.rightClickDelay = 4;
         if (!this.player.isHandsBusy()) {
            if (this.hitResult == null) {
               LOGGER.warn("Null returned as 'hitResult', this shouldn't happen!");
            }

            InteractionHand[] var1 = InteractionHand.values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
               InteractionHand var4 = var1[var3];
               ItemStack var5 = this.player.getItemInHand(var4);
               if (this.hitResult != null) {
                  switch(this.hitResult.getType()) {
                  case ENTITY:
                     EntityHitResult var6 = (EntityHitResult)this.hitResult;
                     Entity var7 = var6.getEntity();
                     InteractionResult var8 = this.gameMode.interactAt(this.player, var7, var6, var4);
                     if (!var8.consumesAction()) {
                        var8 = this.gameMode.interact(this.player, var7, var4);
                     }

                     if (var8.consumesAction()) {
                        if (var8.shouldSwing()) {
                           this.player.swing(var4);
                        }

                        return;
                     }
                     break;
                  case BLOCK:
                     BlockHitResult var9 = (BlockHitResult)this.hitResult;
                     int var10 = var5.getCount();
                     InteractionResult var11 = this.gameMode.useItemOn(this.player, this.level, var4, var9);
                     if (var11.consumesAction()) {
                        if (var11.shouldSwing()) {
                           this.player.swing(var4);
                           if (!var5.isEmpty() && (var5.getCount() != var10 || this.gameMode.hasInfiniteItems())) {
                              this.gameRenderer.itemInHandRenderer.itemUsed(var4);
                           }
                        }

                        return;
                     }

                     if (var11 == InteractionResult.FAIL) {
                        return;
                     }
                  }
               }

               if (!var5.isEmpty()) {
                  InteractionResult var12 = this.gameMode.useItem(this.player, this.level, var4);
                  if (var12.consumesAction()) {
                     if (var12.shouldSwing()) {
                        this.player.swing(var4);
                     }

                     this.gameRenderer.itemInHandRenderer.itemUsed(var4);
                     return;
                  }
               }
            }

         }
      }
   }

   public MusicManager getMusicManager() {
      return this.musicManager;
   }

   public void tick() {
      if (this.rightClickDelay > 0) {
         --this.rightClickDelay;
      }

      this.profiler.push("gui");
      if (!this.pause) {
         this.gui.tick();
      }

      this.profiler.pop();
      this.gameRenderer.pick(1.0F);
      this.tutorial.onLookAt(this.level, this.hitResult);
      this.profiler.push("gameMode");
      if (!this.pause && this.level != null) {
         this.gameMode.tick();
      }

      this.profiler.popPush("textures");
      if (this.level != null) {
         this.textureManager.tick();
      }

      if (this.screen == null && this.player != null) {
         if (this.player.isDeadOrDying() && !(this.screen instanceof DeathScreen)) {
            this.setScreen((Screen)null);
         } else if (this.player.isSleeping() && this.level != null) {
            this.setScreen(new InBedChatScreen());
         }
      } else if (this.screen != null && this.screen instanceof InBedChatScreen && !this.player.isSleeping()) {
         this.setScreen((Screen)null);
      }

      if (this.screen != null) {
         this.missTime = 10000;
      }

      if (this.screen != null) {
         Screen.wrapScreenError(() -> {
            this.screen.tick();
         }, "Ticking screen", this.screen.getClass().getCanonicalName());
      }

      if (!this.options.renderDebug) {
         this.gui.clearCache();
      }

      if (this.overlay == null && (this.screen == null || this.screen.passEvents)) {
         this.profiler.popPush("Keybindings");
         this.handleKeybinds();
         if (this.missTime > 0) {
            --this.missTime;
         }
      }

      if (this.level != null) {
         this.profiler.popPush("gameRenderer");
         if (!this.pause) {
            this.gameRenderer.tick();
         }

         this.profiler.popPush("levelRenderer");
         if (!this.pause) {
            this.levelRenderer.tick();
         }

         this.profiler.popPush("level");
         if (!this.pause) {
            if (this.level.getSkyFlashTime() > 0) {
               this.level.setSkyFlashTime(this.level.getSkyFlashTime() - 1);
            }

            this.level.tickEntities();
         }
      } else if (this.gameRenderer.currentEffect() != null) {
         this.gameRenderer.shutdownEffect();
      }

      if (!this.pause) {
         this.musicManager.tick();
      }

      this.soundManager.tick(this.pause);
      if (this.level != null) {
         if (!this.pause) {
            if (!this.options.joinedFirstServer && this.isMultiplayerServer()) {
               TranslatableComponent var1 = new TranslatableComponent("tutorial.socialInteractions.title");
               TranslatableComponent var2 = new TranslatableComponent("tutorial.socialInteractions.description", new Object[]{Tutorial.key("socialInteractions")});
               this.socialInteractionsToast = new TutorialToast(TutorialToast.Icons.SOCIAL_INTERACTIONS, var1, var2, true);
               this.tutorial.addTimedToast(this.socialInteractionsToast, 160);
               this.options.joinedFirstServer = true;
               this.options.save();
            }

            this.tutorial.tick();

            try {
               this.level.tick(() -> {
                  return true;
               });
            } catch (Throwable var4) {
               CrashReport var5 = CrashReport.forThrowable(var4, "Exception in world tick");
               if (this.level == null) {
                  CrashReportCategory var3 = var5.addCategory("Affected level");
                  var3.setDetail("Problem", (Object)"Level is null!");
               } else {
                  this.level.fillReportDetails(var5);
               }

               throw new ReportedException(var5);
            }
         }

         this.profiler.popPush("animateTick");
         if (!this.pause && this.level != null) {
            this.level.animateTick(this.player.getBlockX(), this.player.getBlockY(), this.player.getBlockZ());
         }

         this.profiler.popPush("particles");
         if (!this.pause) {
            this.particleEngine.tick();
         }
      } else if (this.pendingConnection != null) {
         this.profiler.popPush("pendingConnection");
         this.pendingConnection.tick();
      }

      this.profiler.popPush("keyboard");
      this.keyboardHandler.tick();
      this.profiler.pop();
   }

   private boolean isMultiplayerServer() {
      return !this.isLocalServer || this.singleplayerServer != null && this.singleplayerServer.isPublished();
   }

   private void handleKeybinds() {
      for(; this.options.keyTogglePerspective.consumeClick(); this.levelRenderer.needsUpdate()) {
         CameraType var1 = this.options.getCameraType();
         this.options.setCameraType(this.options.getCameraType().cycle());
         if (var1.isFirstPerson() != this.options.getCameraType().isFirstPerson()) {
            this.gameRenderer.checkEntityPostEffect(this.options.getCameraType().isFirstPerson() ? this.getCameraEntity() : null);
         }
      }

      while(this.options.keySmoothCamera.consumeClick()) {
         this.options.smoothCamera = !this.options.smoothCamera;
      }

      for(int var4 = 0; var4 < 9; ++var4) {
         boolean var2 = this.options.keySaveHotbarActivator.isDown();
         boolean var3 = this.options.keyLoadHotbarActivator.isDown();
         if (this.options.keyHotbarSlots[var4].consumeClick()) {
            if (this.player.isSpectator()) {
               this.gui.getSpectatorGui().onHotbarSelected(var4);
            } else if (!this.player.isCreative() || this.screen != null || !var3 && !var2) {
               this.player.getInventory().selected = var4;
            } else {
               CreativeModeInventoryScreen.handleHotbarLoadOrSave(this, var4, var3, var2);
            }
         }
      }

      while(this.options.keySocialInteractions.consumeClick()) {
         if (!this.isMultiplayerServer()) {
            this.player.displayClientMessage(SOCIAL_INTERACTIONS_NOT_AVAILABLE, true);
            NarratorChatListener.INSTANCE.sayNow(SOCIAL_INTERACTIONS_NOT_AVAILABLE);
         } else {
            if (this.socialInteractionsToast != null) {
               this.tutorial.removeTimedToast(this.socialInteractionsToast);
               this.socialInteractionsToast = null;
            }

            this.setScreen(new SocialInteractionsScreen());
         }
      }

      while(this.options.keyInventory.consumeClick()) {
         if (this.gameMode.isServerControlledInventory()) {
            this.player.sendOpenInventory();
         } else {
            this.tutorial.onOpenInventory();
            this.setScreen(new InventoryScreen(this.player));
         }
      }

      while(this.options.keyAdvancements.consumeClick()) {
         this.setScreen(new AdvancementsScreen(this.player.connection.getAdvancements()));
      }

      while(this.options.keySwapOffhand.consumeClick()) {
         if (!this.player.isSpectator()) {
            this.getConnection().send((Packet)(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN)));
         }
      }

      while(this.options.keyDrop.consumeClick()) {
         if (!this.player.isSpectator() && this.player.drop(Screen.hasControlDown())) {
            this.player.swing(InteractionHand.MAIN_HAND);
         }
      }

      while(this.options.keyChat.consumeClick()) {
         this.openChatScreen("");
      }

      if (this.screen == null && this.overlay == null && this.options.keyCommand.consumeClick()) {
         this.openChatScreen("/");
      }

      if (this.player.isUsingItem()) {
         if (!this.options.keyUse.isDown()) {
            this.gameMode.releaseUsingItem(this.player);
         }

         label115:
         while(true) {
            if (!this.options.keyAttack.consumeClick()) {
               while(this.options.keyUse.consumeClick()) {
               }

               while(true) {
                  if (this.options.keyPickItem.consumeClick()) {
                     continue;
                  }
                  break label115;
               }
            }
         }
      } else {
         while(this.options.keyAttack.consumeClick()) {
            this.startAttack();
         }

         while(this.options.keyUse.consumeClick()) {
            this.startUseItem();
         }

         while(this.options.keyPickItem.consumeClick()) {
            this.pickBlock();
         }
      }

      if (this.options.keyUse.isDown() && this.rightClickDelay == 0 && !this.player.isUsingItem()) {
         this.startUseItem();
      }

      this.continueAttack(this.screen == null && this.options.keyAttack.isDown() && this.mouseHandler.isMouseGrabbed());
   }

   public static DataPackConfig loadDataPacks(LevelStorageSource.LevelStorageAccess var0) {
      MinecraftServer.convertFromRegionFormatIfNeeded(var0);
      DataPackConfig var1 = var0.getDataPacks();
      if (var1 == null) {
         throw new IllegalStateException("Failed to load data pack config");
      } else {
         return var1;
      }
   }

   public static WorldData loadWorldData(LevelStorageSource.LevelStorageAccess var0, RegistryAccess.RegistryHolder var1, ResourceManager var2, DataPackConfig var3) {
      RegistryReadOps var4 = RegistryReadOps.createAndLoad(NbtOps.INSTANCE, (ResourceManager)var2, var1);
      WorldData var5 = var0.getDataTag(var4, var3);
      if (var5 == null) {
         throw new IllegalStateException("Failed to load world");
      } else {
         return var5;
      }
   }

   public void loadLevel(String var1) {
      this.doLoadLevel(var1, RegistryAccess.builtin(), Minecraft::loadDataPacks, Minecraft::loadWorldData, false, Minecraft.ExperimentalDialogType.BACKUP);
   }

   public void createLevel(String var1, LevelSettings var2, RegistryAccess.RegistryHolder var3, WorldGenSettings var4) {
      this.doLoadLevel(var1, var3, (var1x) -> {
         return var2.getDataPackConfig();
      }, (var3x, var4x, var5, var6) -> {
         RegistryWriteOps var7 = RegistryWriteOps.create(JsonOps.INSTANCE, var3);
         RegistryReadOps var8 = RegistryReadOps.createAndLoad(JsonOps.INSTANCE, (ResourceManager)var5, var3);
         DataResult var9 = WorldGenSettings.CODEC.encodeStart(var7, var4).setLifecycle(Lifecycle.stable()).flatMap((var1) -> {
            return WorldGenSettings.CODEC.parse(var8, var1);
         });
         Logger var10002 = LOGGER;
         Objects.requireNonNull(var10002);
         WorldGenSettings var10 = (WorldGenSettings)var9.resultOrPartial(Util.prefix("Error reading worldgen settings after loading data packs: ", var10002::error)).orElse(var4);
         return new PrimaryLevelData(var2, var10, var9.lifecycle());
      }, false, Minecraft.ExperimentalDialogType.CREATE);
   }

   private void doLoadLevel(String var1, RegistryAccess.RegistryHolder var2, Function<LevelStorageSource.LevelStorageAccess, DataPackConfig> var3, Function4<LevelStorageSource.LevelStorageAccess, RegistryAccess.RegistryHolder, ResourceManager, DataPackConfig, WorldData> var4, boolean var5, Minecraft.ExperimentalDialogType var6) {
      LevelStorageSource.LevelStorageAccess var7;
      try {
         var7 = this.levelSource.createAccess(var1);
      } catch (IOException var21) {
         LOGGER.warn("Failed to read level {} data", var1, var21);
         SystemToast.onWorldAccessFailure(this, var1);
         this.setScreen((Screen)null);
         return;
      }

      Minecraft.ServerStem var8;
      try {
         var8 = this.makeServerStem(var2, var3, var4, var5, var7);
      } catch (Exception var20) {
         LOGGER.warn("Failed to load datapacks, can't proceed with server load", var20);
         this.setScreen(new DatapackLoadFailureScreen(() -> {
            this.doLoadLevel(var1, var2, var3, var4, true, var6);
         }));

         try {
            var7.close();
         } catch (IOException var16) {
            LOGGER.warn("Failed to unlock access to level {}", var1, var16);
         }

         return;
      }

      WorldData var9 = var8.worldData();
      boolean var10 = var9.worldGenSettings().isOldCustomizedWorld();
      boolean var11 = var9.worldGenSettingsLifecycle() != Lifecycle.stable();
      if (var6 == Minecraft.ExperimentalDialogType.NONE || !var10 && !var11) {
         this.clearLevel();
         this.progressListener.set((Object)null);

         try {
            var7.saveDataTag(var2, var9);
            var8.serverResources().updateGlobals();
            YggdrasilAuthenticationService var12 = new YggdrasilAuthenticationService(this.proxy);
            MinecraftSessionService var23 = var12.createMinecraftSessionService();
            GameProfileRepository var25 = var12.createProfileRepository();
            GameProfileCache var15 = new GameProfileCache(var25, new File(this.gameDirectory, MinecraftServer.USERID_CACHE_FILE.getName()));
            var15.setExecutor(this);
            SkullBlockEntity.setProfileCache(var15);
            SkullBlockEntity.setSessionService(var23);
            GameProfileCache.setUsesAuthentication(false);
            this.singleplayerServer = (IntegratedServer)MinecraftServer.spin((var8x) -> {
               return new IntegratedServer(var8x, this, var2, var7, var8.packRepository(), var8.serverResources(), var9, var23, var25, var15, (var1) -> {
                  StoringChunkProgressListener var2 = new StoringChunkProgressListener(var1 + 0);
                  this.progressListener.set(var2);
                  Queue var10001 = this.progressTasks;
                  Objects.requireNonNull(var10001);
                  return ProcessorChunkProgressListener.createStarted(var2, var10001::add);
               });
            });
            this.isLocalServer = true;
         } catch (Throwable var19) {
            CrashReport var13 = CrashReport.forThrowable(var19, "Starting integrated server");
            CrashReportCategory var14 = var13.addCategory("Starting integrated server");
            var14.setDetail("Level ID", (Object)var1);
            var14.setDetail("Level Name", (Object)var9.getLevelName());
            throw new ReportedException(var13);
         }

         while(this.progressListener.get() == null) {
            Thread.yield();
         }

         LevelLoadingScreen var22 = new LevelLoadingScreen((StoringChunkProgressListener)this.progressListener.get());
         this.setScreen(var22);
         this.profiler.push("waitForServer");

         while(!this.singleplayerServer.isReady()) {
            var22.tick();
            this.runTick(false);

            try {
               Thread.sleep(16L);
            } catch (InterruptedException var18) {
            }

            if (this.delayedCrash != null) {
               crash(this.delayedCrash);
               return;
            }
         }

         this.profiler.pop();
         SocketAddress var24 = this.singleplayerServer.getConnection().startMemoryChannel();
         Connection var26 = Connection.connectToLocalServer(var24);
         var26.setListener(new ClientHandshakePacketListenerImpl(var26, this, (Screen)null, (var0) -> {
         }));
         var26.send(new ClientIntentionPacket(var24.toString(), 0, ConnectionProtocol.LOGIN));
         var26.send(new ServerboundHelloPacket(this.getUser().getGameProfile()));
         this.pendingConnection = var26;
      } else {
         this.displayExperimentalConfirmationDialog(var6, var1, var10, () -> {
            this.doLoadLevel(var1, var2, var3, var4, var5, Minecraft.ExperimentalDialogType.NONE);
         });
         var8.close();

         try {
            var7.close();
         } catch (IOException var17) {
            LOGGER.warn("Failed to unlock access to level {}", var1, var17);
         }

      }
   }

   private void displayExperimentalConfirmationDialog(Minecraft.ExperimentalDialogType var1, String var2, boolean var3, Runnable var4) {
      if (var1 == Minecraft.ExperimentalDialogType.BACKUP) {
         TranslatableComponent var5;
         TranslatableComponent var6;
         if (var3) {
            var5 = new TranslatableComponent("selectWorld.backupQuestion.customized");
            var6 = new TranslatableComponent("selectWorld.backupWarning.customized");
         } else {
            var5 = new TranslatableComponent("selectWorld.backupQuestion.experimental");
            var6 = new TranslatableComponent("selectWorld.backupWarning.experimental");
         }

         this.setScreen(new BackupConfirmScreen((Screen)null, (var3x, var4x) -> {
            if (var3x) {
               EditWorldScreen.makeBackupAndShowToast(this.levelSource, var2);
            }

            var4.run();
         }, var5, var6, false));
      } else {
         this.setScreen(new ConfirmScreen((var3x) -> {
            if (var3x) {
               var4.run();
            } else {
               this.setScreen((Screen)null);

               try {
                  LevelStorageSource.LevelStorageAccess var4x = this.levelSource.createAccess(var2);

                  try {
                     var4x.deleteLevel();
                  } catch (Throwable var8) {
                     if (var4x != null) {
                        try {
                           var4x.close();
                        } catch (Throwable var7) {
                           var8.addSuppressed(var7);
                        }
                     }

                     throw var8;
                  }

                  if (var4x != null) {
                     var4x.close();
                  }
               } catch (IOException var9) {
                  SystemToast.onWorldDeleteFailure(this, var2);
                  LOGGER.error("Failed to delete world {}", var2, var9);
               }
            }

         }, new TranslatableComponent("selectWorld.backupQuestion.experimental"), new TranslatableComponent("selectWorld.backupWarning.experimental"), CommonComponents.GUI_PROCEED, CommonComponents.GUI_CANCEL));
      }

   }

   public Minecraft.ServerStem makeServerStem(RegistryAccess.RegistryHolder var1, Function<LevelStorageSource.LevelStorageAccess, DataPackConfig> var2, Function4<LevelStorageSource.LevelStorageAccess, RegistryAccess.RegistryHolder, ResourceManager, DataPackConfig, WorldData> var3, boolean var4, LevelStorageSource.LevelStorageAccess var5) throws InterruptedException, ExecutionException {
      DataPackConfig var6 = (DataPackConfig)var2.apply(var5);
      PackRepository var7 = new PackRepository(PackType.SERVER_DATA, new RepositorySource[]{new ServerPacksSource(), new FolderRepositorySource(var5.getLevelPath(LevelResource.DATAPACK_DIR).toFile(), PackSource.WORLD)});

      try {
         DataPackConfig var8 = MinecraftServer.configurePackRepository(var7, var6, var4);
         CompletableFuture var9 = ServerResources.loadResources(var7.openAllSelected(), var1, Commands.CommandSelection.INTEGRATED, 2, Util.backgroundExecutor(), this);
         Objects.requireNonNull(var9);
         this.managedBlock(var9::isDone);
         ServerResources var10 = (ServerResources)var9.get();
         WorldData var11 = (WorldData)var3.apply(var5, var1, var10.getResourceManager(), var8);
         return new Minecraft.ServerStem(var7, var10, var11);
      } catch (ExecutionException | InterruptedException var12) {
         var7.close();
         throw var12;
      }
   }

   public void setLevel(ClientLevel var1) {
      ProgressScreen var2 = new ProgressScreen(true);
      var2.progressStartNoAbort(new TranslatableComponent("connect.joining"));
      this.updateScreenAndTick(var2);
      this.level = var1;
      this.updateLevelInEngines(var1);
      if (!this.isLocalServer) {
         YggdrasilAuthenticationService var3 = new YggdrasilAuthenticationService(this.proxy);
         MinecraftSessionService var4 = var3.createMinecraftSessionService();
         GameProfileRepository var5 = var3.createProfileRepository();
         GameProfileCache var6 = new GameProfileCache(var5, new File(this.gameDirectory, MinecraftServer.USERID_CACHE_FILE.getName()));
         var6.setExecutor(this);
         SkullBlockEntity.setProfileCache(var6);
         SkullBlockEntity.setSessionService(var4);
         GameProfileCache.setUsesAuthentication(false);
      }

   }

   public void clearLevel() {
      this.clearLevel(new ProgressScreen(true));
   }

   public void clearLevel(Screen var1) {
      ClientPacketListener var2 = this.getConnection();
      if (var2 != null) {
         this.dropAllTasks();
         var2.cleanup();
      }

      IntegratedServer var3 = this.singleplayerServer;
      this.singleplayerServer = null;
      this.gameRenderer.resetData();
      this.gameMode = null;
      NarratorChatListener.INSTANCE.clear();
      this.updateScreenAndTick(var1);
      if (this.level != null) {
         if (var3 != null) {
            this.profiler.push("waitForServer");

            while(!var3.isShutdown()) {
               this.runTick(false);
            }

            this.profiler.pop();
         }

         this.clientPackSource.clearServerPack();
         this.gui.onDisconnected();
         this.currentServer = null;
         this.isLocalServer = false;
         this.game.onLeaveGameSession();
      }

      this.level = null;
      this.updateLevelInEngines((ClientLevel)null);
      this.player = null;
   }

   private void updateScreenAndTick(Screen var1) {
      this.profiler.push("forcedTick");
      this.soundManager.stop();
      this.cameraEntity = null;
      this.pendingConnection = null;
      this.setScreen(var1);
      this.runTick(false);
      this.profiler.pop();
   }

   public void forceSetScreen(Screen var1) {
      this.profiler.push("forcedTick");
      this.setScreen(var1);
      this.runTick(false);
      this.profiler.pop();
   }

   private void updateLevelInEngines(@Nullable ClientLevel var1) {
      this.levelRenderer.setLevel(var1);
      this.particleEngine.setLevel(var1);
      this.blockEntityRenderDispatcher.setLevel(var1);
      this.updateTitle();
   }

   public boolean allowsMultiplayer() {
      return this.allowsMultiplayer && this.socialInteractionsService.serversAllowed();
   }

   public boolean allowsRealms() {
      return this.socialInteractionsService.realmsAllowed();
   }

   public boolean isBlocked(UUID var1) {
      if (this.getChatStatus().isChatAllowed(false)) {
         return this.playerSocialManager.shouldHideMessageFrom(var1);
      } else {
         return (this.player == null || !var1.equals(this.player.getUUID())) && !var1.equals(Util.NIL_UUID);
      }
   }

   public Minecraft.ChatStatus getChatStatus() {
      if (this.options.chatVisibility == ChatVisiblity.HIDDEN) {
         return Minecraft.ChatStatus.DISABLED_BY_OPTIONS;
      } else if (!this.allowsChat) {
         return Minecraft.ChatStatus.DISABLED_BY_LAUNCHER;
      } else {
         return !this.socialInteractionsService.chatAllowed() ? Minecraft.ChatStatus.DISABLED_BY_PROFILE : Minecraft.ChatStatus.ENABLED;
      }
   }

   public final boolean isDemo() {
      return this.demo;
   }

   @Nullable
   public ClientPacketListener getConnection() {
      return this.player == null ? null : this.player.connection;
   }

   public static boolean renderNames() {
      return !instance.options.hideGui;
   }

   public static boolean useFancyGraphics() {
      return instance.options.graphicsMode.getId() >= GraphicsStatus.FANCY.getId();
   }

   public static boolean useShaderTransparency() {
      return !instance.gameRenderer.isPanoramicMode() && instance.options.graphicsMode.getId() >= GraphicsStatus.FABULOUS.getId();
   }

   public static boolean useAmbientOcclusion() {
      return instance.options.ambientOcclusion != AmbientOcclusionStatus.OFF;
   }

   private void pickBlock() {
      if (this.hitResult != null && this.hitResult.getType() != HitResult.Type.MISS) {
         boolean var1 = this.player.getAbilities().instabuild;
         BlockEntity var2 = null;
         HitResult.Type var4 = this.hitResult.getType();
         ItemStack var3;
         if (var4 == HitResult.Type.BLOCK) {
            BlockPos var8 = ((BlockHitResult)this.hitResult).getBlockPos();
            BlockState var6 = this.level.getBlockState(var8);
            if (var6.isAir()) {
               return;
            }

            Block var7 = var6.getBlock();
            var3 = var7.getCloneItemStack(this.level, var8, var6);
            if (var3.isEmpty()) {
               return;
            }

            if (var1 && Screen.hasControlDown() && var6.hasBlockEntity()) {
               var2 = this.level.getBlockEntity(var8);
            }
         } else {
            if (var4 != HitResult.Type.ENTITY || !var1) {
               return;
            }

            Entity var5 = ((EntityHitResult)this.hitResult).getEntity();
            var3 = var5.getPickResult();
            if (var3 == null) {
               return;
            }
         }

         if (var3.isEmpty()) {
            String var10 = "";
            if (var4 == HitResult.Type.BLOCK) {
               var10 = Registry.BLOCK.getKey(this.level.getBlockState(((BlockHitResult)this.hitResult).getBlockPos()).getBlock()).toString();
            } else if (var4 == HitResult.Type.ENTITY) {
               var10 = Registry.ENTITY_TYPE.getKey(((EntityHitResult)this.hitResult).getEntity().getType()).toString();
            }

            LOGGER.warn("Picking on: [{}] {} gave null item", var4, var10);
         } else {
            Inventory var9 = this.player.getInventory();
            if (var2 != null) {
               this.addCustomNbtData(var3, var2);
            }

            int var11 = var9.findSlotMatchingItem(var3);
            if (var1) {
               var9.setPickedItem(var3);
               this.gameMode.handleCreativeModeItemAdd(this.player.getItemInHand(InteractionHand.MAIN_HAND), 36 + var9.selected);
            } else if (var11 != -1) {
               if (Inventory.isHotbarSlot(var11)) {
                  var9.selected = var11;
               } else {
                  this.gameMode.handlePickItem(var11);
               }
            }

         }
      }
   }

   private ItemStack addCustomNbtData(ItemStack var1, BlockEntity var2) {
      CompoundTag var3 = var2.save(new CompoundTag());
      CompoundTag var4;
      if (var1.getItem() instanceof PlayerHeadItem && var3.contains("SkullOwner")) {
         var4 = var3.getCompound("SkullOwner");
         var1.getOrCreateTag().put("SkullOwner", var4);
         return var1;
      } else {
         var1.addTagElement("BlockEntityTag", var3);
         var4 = new CompoundTag();
         ListTag var5 = new ListTag();
         var5.add(StringTag.valueOf("\"(+NBT)\""));
         var4.put("Lore", var5);
         var1.addTagElement("display", var4);
         return var1;
      }
   }

   public CrashReport fillReport(CrashReport var1) {
      SystemReport var2 = var1.getSystemReport();
      fillSystemReport(var2, this, this.languageManager, this.launchedVersion, this.options);
      if (this.level != null) {
         this.level.fillReportDetails(var1);
      }

      if (this.singleplayerServer != null) {
         this.singleplayerServer.fillSystemReport(var2);
      }

      this.reloadStateTracker.fillCrashReport(var1);
      return var1;
   }

   public static void fillReport(@Nullable Minecraft var0, @Nullable LanguageManager var1, String var2, @Nullable Options var3, CrashReport var4) {
      SystemReport var5 = var4.getSystemReport();
      fillSystemReport(var5, var0, var1, var2, var3);
   }

   private static SystemReport fillSystemReport(SystemReport var0, @Nullable Minecraft var1, @Nullable LanguageManager var2, String var3, Options var4) {
      var0.setDetail("Launched Version", () -> {
         return var3;
      });
      var0.setDetail("Backend library", RenderSystem::getBackendDescription);
      var0.setDetail("Backend API", RenderSystem::getApiDescription);
      var0.setDetail("Window size", () -> {
         return var1 != null ? var1.window.getWidth() + "x" + var1.window.getHeight() : "<not initialized>";
      });
      var0.setDetail("GL Caps", RenderSystem::getCapsString);
      var0.setDetail("GL debug messages", () -> {
         return GlDebug.isDebugEnabled() ? String.join("\n", GlDebug.getLastOpenGlDebugMessages()) : "<disabled>";
      });
      var0.setDetail("Using VBOs", () -> {
         return "Yes";
      });
      var0.setDetail("Is Modded", () -> {
         String var0 = ClientBrandRetriever.getClientModName();
         if (!"vanilla".equals(var0)) {
            return "Definitely; Client brand changed to '" + var0 + "'";
         } else {
            return Minecraft.class.getSigners() == null ? "Very likely; Jar signature invalidated" : "Probably not. Jar signature remains and client brand is untouched.";
         }
      });
      var0.setDetail("Type", "Client (map_client.txt)");
      if (var4 != null) {
         if (instance != null) {
            String var5 = instance.getGpuWarnlistManager().getAllWarnings();
            if (var5 != null) {
               var0.setDetail("GPU Warnings", var5);
            }
         }

         var0.setDetail("Graphics mode", var4.graphicsMode.toString());
         var0.setDetail("Resource Packs", () -> {
            StringBuilder var1 = new StringBuilder();
            Iterator var2 = var4.resourcePacks.iterator();

            while(var2.hasNext()) {
               String var3 = (String)var2.next();
               if (var1.length() > 0) {
                  var1.append(", ");
               }

               var1.append(var3);
               if (var4.incompatibleResourcePacks.contains(var3)) {
                  var1.append(" (incompatible)");
               }
            }

            return var1.toString();
         });
      }

      if (var2 != null) {
         var0.setDetail("Current Language", () -> {
            return var2.getSelected().toString();
         });
      }

      var0.setDetail("CPU", GlUtil::getCpuInfo);
      return var0;
   }

   public static Minecraft getInstance() {
      return instance;
   }

   public CompletableFuture<Void> delayTextureReload() {
      return this.submit(this::reloadResourcePacks).thenCompose((var0) -> {
         return var0;
      });
   }

   public void populateSnooper(Snooper var1) {
      var1.setDynamicData("fps", fps);
      var1.setDynamicData("vsync_enabled", this.options.enableVsync);
      var1.setDynamicData("display_frequency", this.window.getRefreshRate());
      var1.setDynamicData("display_type", this.window.isFullscreen() ? "fullscreen" : "windowed");
      var1.setDynamicData("run_time", (Util.getMillis() - var1.getStartupTime()) / 60L * 1000L);
      var1.setDynamicData("current_action", this.getCurrentSnooperAction());
      var1.setDynamicData("language", this.options.languageCode == null ? "en_us" : this.options.languageCode);
      String var2 = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? "little" : "big";
      var1.setDynamicData("endianness", var2);
      var1.setDynamicData("subtitles", this.options.showSubtitles);
      var1.setDynamicData("touch", this.options.touchscreen ? "touch" : "mouse");
      int var3 = 0;
      Iterator var4 = this.resourcePackRepository.getSelectedPacks().iterator();

      while(var4.hasNext()) {
         Pack var5 = (Pack)var4.next();
         if (!var5.isRequired() && !var5.isFixedPosition()) {
            int var10001 = var3++;
            var1.setDynamicData("resource_pack[" + var10001 + "]", var5.getId());
         }
      }

      var1.setDynamicData("resource_packs", var3);
      if (this.singleplayerServer != null) {
         var1.setDynamicData("snooper_partner", this.singleplayerServer.getSnooper().getToken());
      }

   }

   private String getCurrentSnooperAction() {
      if (this.singleplayerServer != null) {
         return this.singleplayerServer.isPublished() ? "hosting_lan" : "singleplayer";
      } else if (this.currentServer != null) {
         return this.currentServer.isLan() ? "playing_lan" : "multiplayer";
      } else {
         return "out_of_game";
      }
   }

   public void populateSnooperInitial(Snooper var1) {
      var1.setFixedData("client_brand", ClientBrandRetriever.getClientModName());
      var1.setFixedData("launched_version", this.launchedVersion);
      populateSnooperWithOpenGL(var1);
      var1.setFixedData("gl_max_texture_size", RenderSystem.maxSupportedTextureSize());
      GameProfile var2 = this.user.getGameProfile();
      if (var2.getId() != null) {
         var1.setFixedData("uuid", Hashing.sha1().hashBytes(var2.getId().toString().getBytes(Charsets.ISO_8859_1)).toString());
      }

   }

   private static void populateSnooperWithOpenGL(Snooper var0) {
      Objects.requireNonNull(var0);
      GlUtil.populateSnooperWithOpenGL(var0::setFixedData);
   }

   public boolean isSnooperEnabled() {
      return this.options.snooperEnabled;
   }

   public void setCurrentServer(@Nullable ServerData var1) {
      this.currentServer = var1;
   }

   @Nullable
   public ServerData getCurrentServer() {
      return this.currentServer;
   }

   public boolean isLocalServer() {
      return this.isLocalServer;
   }

   public boolean hasSingleplayerServer() {
      return this.isLocalServer && this.singleplayerServer != null;
   }

   @Nullable
   public IntegratedServer getSingleplayerServer() {
      return this.singleplayerServer;
   }

   public Snooper getSnooper() {
      return this.snooper;
   }

   public User getUser() {
      return this.user;
   }

   public PropertyMap getProfileProperties() {
      if (this.profileProperties.isEmpty()) {
         GameProfile var1 = this.getMinecraftSessionService().fillProfileProperties(this.user.getGameProfile(), false);
         this.profileProperties.putAll(var1.getProperties());
      }

      return this.profileProperties;
   }

   public Proxy getProxy() {
      return this.proxy;
   }

   public TextureManager getTextureManager() {
      return this.textureManager;
   }

   public ResourceManager getResourceManager() {
      return this.resourceManager;
   }

   public PackRepository getResourcePackRepository() {
      return this.resourcePackRepository;
   }

   public ClientPackSource getClientPackSource() {
      return this.clientPackSource;
   }

   public File getResourcePackDirectory() {
      return this.resourcePackDirectory;
   }

   public LanguageManager getLanguageManager() {
      return this.languageManager;
   }

   public Function<ResourceLocation, TextureAtlasSprite> getTextureAtlas(ResourceLocation var1) {
      TextureAtlas var10000 = this.modelManager.getAtlas(var1);
      Objects.requireNonNull(var10000);
      return var10000::getSprite;
   }

   public boolean is64Bit() {
      return this.is64bit;
   }

   public boolean isPaused() {
      return this.pause;
   }

   public GpuWarnlistManager getGpuWarnlistManager() {
      return this.gpuWarnlistManager;
   }

   public SoundManager getSoundManager() {
      return this.soundManager;
   }

   public Music getSituationalMusic() {
      if (this.screen instanceof WinScreen) {
         return Musics.CREDITS;
      } else if (this.player != null) {
         if (this.player.level.dimension() == Level.END) {
            return this.gui.getBossOverlay().shouldPlayMusic() ? Musics.END_BOSS : Musics.END;
         } else {
            Biome.BiomeCategory var1 = this.player.level.getBiome(this.player.blockPosition()).getBiomeCategory();
            if (this.musicManager.isPlayingMusic(Musics.UNDER_WATER) || this.player.isUnderWater() && (var1 == Biome.BiomeCategory.OCEAN || var1 == Biome.BiomeCategory.RIVER)) {
               return Musics.UNDER_WATER;
            } else {
               return this.player.level.dimension() != Level.NETHER && this.player.getAbilities().instabuild && this.player.getAbilities().mayfly ? Musics.CREATIVE : (Music)this.level.getBiomeManager().getNoiseBiomeAtPosition(this.player.blockPosition()).getBackgroundMusic().orElse(Musics.GAME);
            }
         }
      } else {
         return Musics.MENU;
      }
   }

   public MinecraftSessionService getMinecraftSessionService() {
      return this.minecraftSessionService;
   }

   public SkinManager getSkinManager() {
      return this.skinManager;
   }

   @Nullable
   public Entity getCameraEntity() {
      return this.cameraEntity;
   }

   public void setCameraEntity(Entity var1) {
      this.cameraEntity = var1;
      this.gameRenderer.checkEntityPostEffect(var1);
   }

   public boolean shouldEntityAppearGlowing(Entity var1) {
      return var1.isCurrentlyGlowing() || this.player != null && this.player.isSpectator() && this.options.keySpectatorOutlines.isDown() && var1.getType() == EntityType.PLAYER;
   }

   protected Thread getRunningThread() {
      return this.gameThread;
   }

   protected Runnable wrapRunnable(Runnable var1) {
      return var1;
   }

   protected boolean shouldRun(Runnable var1) {
      return true;
   }

   public BlockRenderDispatcher getBlockRenderer() {
      return this.blockRenderer;
   }

   public EntityRenderDispatcher getEntityRenderDispatcher() {
      return this.entityRenderDispatcher;
   }

   public BlockEntityRenderDispatcher getBlockEntityRenderDispatcher() {
      return this.blockEntityRenderDispatcher;
   }

   public ItemRenderer getItemRenderer() {
      return this.itemRenderer;
   }

   public ItemInHandRenderer getItemInHandRenderer() {
      return this.itemInHandRenderer;
   }

   public <T> MutableSearchTree<T> getSearchTree(SearchRegistry.Key<T> var1) {
      return this.searchRegistry.getTree(var1);
   }

   public FrameTimer getFrameTimer() {
      return this.frameTimer;
   }

   public boolean isConnectedToRealms() {
      return this.connectedToRealms;
   }

   public void setConnectedToRealms(boolean var1) {
      this.connectedToRealms = var1;
   }

   public DataFixer getFixerUpper() {
      return this.fixerUpper;
   }

   public float getFrameTime() {
      return this.timer.partialTick;
   }

   public float getDeltaFrameTime() {
      return this.timer.tickDelta;
   }

   public BlockColors getBlockColors() {
      return this.blockColors;
   }

   public boolean showOnlyReducedInfo() {
      return this.player != null && this.player.isReducedDebugInfo() || this.options.reducedDebugInfo;
   }

   public ToastComponent getToasts() {
      return this.toast;
   }

   public Tutorial getTutorial() {
      return this.tutorial;
   }

   public boolean isWindowActive() {
      return this.windowActive;
   }

   public HotbarManager getHotbarManager() {
      return this.hotbarManager;
   }

   public ModelManager getModelManager() {
      return this.modelManager;
   }

   public PaintingTextureManager getPaintingTextures() {
      return this.paintingTextures;
   }

   public MobEffectTextureManager getMobEffectTextures() {
      return this.mobEffectTextures;
   }

   public void setWindowActive(boolean var1) {
      this.windowActive = var1;
   }

   public Component grabPanoramixScreenshot(File var1, int var2, int var3) {
      int var4 = this.window.getWidth();
      int var5 = this.window.getHeight();
      TextureTarget var6 = new TextureTarget(var2, var3, true, ON_OSX);
      float var7 = this.player.getXRot();
      float var8 = this.player.getYRot();
      float var9 = this.player.xRotO;
      float var10 = this.player.yRotO;
      this.gameRenderer.setRenderBlockOutline(false);

      TranslatableComponent var12;
      try {
         this.gameRenderer.setPanoramicMode(true);
         this.levelRenderer.graphicsChanged();
         this.window.setWidth(var2);
         this.window.setHeight(var3);

         for(int var11 = 0; var11 < 6; ++var11) {
            switch(var11) {
            case 0:
               this.player.setYRot(var8);
               this.player.setXRot(0.0F);
               break;
            case 1:
               this.player.setYRot((var8 + 90.0F) % 360.0F);
               this.player.setXRot(0.0F);
               break;
            case 2:
               this.player.setYRot((var8 + 180.0F) % 360.0F);
               this.player.setXRot(0.0F);
               break;
            case 3:
               this.player.setYRot((var8 - 90.0F) % 360.0F);
               this.player.setXRot(0.0F);
               break;
            case 4:
               this.player.setYRot(var8);
               this.player.setXRot(-90.0F);
               break;
            case 5:
            default:
               this.player.setYRot(var8);
               this.player.setXRot(90.0F);
            }

            this.player.yRotO = this.player.getYRot();
            this.player.xRotO = this.player.getXRot();
            var6.bindWrite(true);
            this.gameRenderer.renderLevel(1.0F, 0L, new PoseStack());

            try {
               Thread.sleep(10L);
            } catch (InterruptedException var17) {
            }

            Screenshot.grab(var1, "panorama_" + var11 + ".png", var2, var3, var6, (var0) -> {
            });
         }

         MutableComponent var20 = (new TextComponent(var1.getName())).withStyle(ChatFormatting.UNDERLINE).withStyle((var1x) -> {
            return var1x.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, var1.getAbsolutePath()));
         });
         var12 = new TranslatableComponent("screenshot.success", new Object[]{var20});
         return var12;
      } catch (Exception var18) {
         LOGGER.error("Couldn't save image", var18);
         var12 = new TranslatableComponent("screenshot.failure", new Object[]{var18.getMessage()});
      } finally {
         this.player.setXRot(var7);
         this.player.setYRot(var8);
         this.player.xRotO = var9;
         this.player.yRotO = var10;
         this.gameRenderer.setRenderBlockOutline(true);
         this.window.setWidth(var4);
         this.window.setHeight(var5);
         var6.destroyBuffers();
         this.gameRenderer.setPanoramicMode(false);
         this.levelRenderer.graphicsChanged();
         this.getMainRenderTarget().bindWrite(true);
      }

      return var12;
   }

   private Component grabHugeScreenshot(File var1, int var2, int var3, int var4, int var5) {
      try {
         ByteBuffer var6 = GlUtil.allocateMemory(var2 * var3 * 3);
         Screenshot var7 = new Screenshot(var1, var4, var5, var3);
         float var8 = (float)var4 / (float)var2;
         float var9 = (float)var5 / (float)var3;
         float var10 = var8 > var9 ? var8 : var9;

         for(int var11 = (var5 - 1) / var3 * var3; var11 >= 0; var11 -= var3) {
            for(int var12 = 0; var12 < var4; var12 += var2) {
               RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
               float var13 = (float)(var4 - var2) / 2.0F * 2.0F - (float)(var12 * 2);
               float var14 = (float)(var5 - var3) / 2.0F * 2.0F - (float)(var11 * 2);
               var13 /= (float)var2;
               var14 /= (float)var3;
               this.gameRenderer.renderZoomed(var10, var13, var14);
               var6.clear();
               RenderSystem.pixelStore(3333, 1);
               RenderSystem.pixelStore(3317, 1);
               RenderSystem.readPixels(0, 0, var2, var3, 32992, 5121, var6);
               var7.addRegion(var6, var12, var11, var2, var3);
            }

            var7.saveRow();
         }

         File var16 = var7.close();
         GlUtil.freeMemory(var6);
         MutableComponent var17 = (new TextComponent(var16.getName())).withStyle(ChatFormatting.UNDERLINE).withStyle((var1x) -> {
            return var1x.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, var16.getAbsolutePath()));
         });
         return new TranslatableComponent("screenshot.success", new Object[]{var17});
      } catch (Exception var15) {
         LOGGER.warn("Couldn't save screenshot", var15);
         return new TranslatableComponent("screenshot.failure", new Object[]{var15.getMessage()});
      }
   }

   public ProfilerFiller getProfiler() {
      return this.profiler;
   }

   public Game getGame() {
      return this.game;
   }

   @Nullable
   public StoringChunkProgressListener getProgressListener() {
      return (StoringChunkProgressListener)this.progressListener.get();
   }

   public SplashManager getSplashManager() {
      return this.splashManager;
   }

   @Nullable
   public Overlay getOverlay() {
      return this.overlay;
   }

   public PlayerSocialManager getPlayerSocialManager() {
      return this.playerSocialManager;
   }

   public boolean renderOnThread() {
      return false;
   }

   public Window getWindow() {
      return this.window;
   }

   public RenderBuffers renderBuffers() {
      return this.renderBuffers;
   }

   private static Pack createClientPackAdapter(String var0, Component var1, boolean var2, Supplier<PackResources> var3, PackMetadataSection var4, Pack.Position var5, PackSource var6) {
      int var7 = var4.getPackFormat();
      Supplier var8 = var3;
      if (var7 <= 3) {
         var8 = adaptV3(var3);
      }

      if (var7 <= 4) {
         var8 = adaptV4(var8);
      }

      return new Pack(var0, var1, var2, var8, var4, PackType.CLIENT_RESOURCES, var5, var6);
   }

   private static Supplier<PackResources> adaptV3(Supplier<PackResources> var0) {
      return () -> {
         return new LegacyPackResourcesAdapter((PackResources)var0.get(), LegacyPackResourcesAdapter.V3);
      };
   }

   private static Supplier<PackResources> adaptV4(Supplier<PackResources> var0) {
      return () -> {
         return new PackResourcesAdapterV4((PackResources)var0.get());
      };
   }

   public void updateMaxMipLevel(int var1) {
      this.modelManager.updateMaxMipLevel(var1);
   }

   public EntityModelSet getEntityModels() {
      return this.entityModels;
   }

   public boolean isTextFilteringEnabled() {
      return false;
   }

   static {
      ON_OSX = Util.getPlatform() == Util.OS.OSX;
      DEFAULT_FONT = new ResourceLocation("default");
      UNIFORM_FONT = new ResourceLocation("uniform");
      ALT_FONT = new ResourceLocation("alt");
      RESOURCE_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
      SOCIAL_INTERACTIONS_NOT_AVAILABLE = new TranslatableComponent("multiplayer.socialInteractions.not_available");
      reserve = new byte[10485760];
   }

   public static enum ChatStatus {
      ENABLED(TextComponent.EMPTY) {
         public boolean isChatAllowed(boolean var1) {
            return true;
         }
      },
      DISABLED_BY_OPTIONS((new TranslatableComponent("chat.disabled.options")).withStyle(ChatFormatting.RED)) {
         public boolean isChatAllowed(boolean var1) {
            return false;
         }
      },
      DISABLED_BY_LAUNCHER((new TranslatableComponent("chat.disabled.launcher")).withStyle(ChatFormatting.RED)) {
         public boolean isChatAllowed(boolean var1) {
            return var1;
         }
      },
      DISABLED_BY_PROFILE((new TranslatableComponent("chat.disabled.profile")).withStyle(ChatFormatting.RED)) {
         public boolean isChatAllowed(boolean var1) {
            return var1;
         }
      };

      private final Component message;

      ChatStatus(Component var3) {
         this.message = var3;
      }

      public Component getMessage() {
         return this.message;
      }

      public abstract boolean isChatAllowed(boolean var1);

      // $FF: synthetic method
      private static Minecraft.ChatStatus[] $values() {
         return new Minecraft.ChatStatus[]{ENABLED, DISABLED_BY_OPTIONS, DISABLED_BY_LAUNCHER, DISABLED_BY_PROFILE};
      }
   }

   static enum ExperimentalDialogType {
      NONE,
      CREATE,
      BACKUP;

      // $FF: synthetic method
      private static Minecraft.ExperimentalDialogType[] $values() {
         return new Minecraft.ExperimentalDialogType[]{NONE, CREATE, BACKUP};
      }
   }

   public static final class ServerStem implements AutoCloseable {
      private final PackRepository packRepository;
      private final ServerResources serverResources;
      private final WorldData worldData;

      ServerStem(PackRepository var1, ServerResources var2, WorldData var3) {
         this.packRepository = var1;
         this.serverResources = var2;
         this.worldData = var3;
      }

      public PackRepository packRepository() {
         return this.packRepository;
      }

      public ServerResources serverResources() {
         return this.serverResources;
      }

      public WorldData worldData() {
         return this.worldData;
      }

      public void close() {
         this.packRepository.close();
         this.serverResources.close();
      }
   }
}
