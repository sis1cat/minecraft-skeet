package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.client.model.data.ModelData;
import net.optifine.BlockPosM;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.CustomSky;
import net.optifine.DynamicLights;
import net.optifine.EmissiveTextures;
import net.optifine.Lagometer;
import net.optifine.SmartAnimations;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.reflect.Reflector;
import net.optifine.render.ChunkVisibility;
import net.optifine.render.RenderEnv;
import net.optifine.render.RenderStateManager;
import net.optifine.render.RenderUtils;
import net.optifine.render.VboRegion;
import net.optifine.shaders.FrustumDummy;
import net.optifine.shaders.RenderStage;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import net.optifine.util.BiomeUtils;
import net.optifine.util.GpuMemory;
import net.optifine.util.MathUtils;
import net.optifine.util.PairInt;
import net.optifine.util.RandomUtils;
import net.optifine.util.RenderChunkUtils;
import net.optifine.util.SectionUtils;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import sisicat.main.functions.FunctionsManager;
import sisicat.main.utilities.Animation;

public class LevelRenderer implements ResourceManagerReloadListener, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation TRANSPARENCY_POST_CHAIN_ID = ResourceLocation.withDefaultNamespace("transparency");
    private static final ResourceLocation ENTITY_OUTLINE_POST_CHAIN_ID = ResourceLocation.withDefaultNamespace("entity_outline");
    public static final int SECTION_SIZE = 16;
    public static final int HALF_SECTION_SIZE = 8;
    public static final int NEARBY_SECTION_DISTANCE_IN_BLOCKS = 32;
    private static final int MINIMUM_TRANSPARENT_SORT_COUNT = 15;
    private final Minecraft minecraft;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    private final RenderBuffers renderBuffers;
    private final SkyRenderer skyRenderer = new SkyRenderer();
    private final CloudRenderer cloudRenderer = new CloudRenderer();
    private final WorldBorderRenderer worldBorderRenderer = new WorldBorderRenderer();
    private WeatherEffectRenderer weatherEffectRenderer = new WeatherEffectRenderer();
    @Nullable
    protected ClientLevel level;
    private final SectionOcclusionGraph sectionOcclusionGraph = new SectionOcclusionGraph();
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList<>(10000);
    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections = new ObjectArrayList<>(50);
    private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
    @Nullable
    private ViewArea viewArea;
    private int ticks;
    private final Int2ObjectMap<BlockDestructionProgress> destroyingBlocks = new Int2ObjectOpenHashMap<>();
    private final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress = new Long2ObjectOpenHashMap<>();
    @Nullable
    private RenderTarget entityOutlineTarget;
    private final LevelTargetBundle targets = new LevelTargetBundle();
    private int lastCameraSectionX = Integer.MIN_VALUE;
    private int lastCameraSectionY = Integer.MIN_VALUE;
    private int lastCameraSectionZ = Integer.MIN_VALUE;
    private double prevCamX = Double.MIN_VALUE;
    private double prevCamY = Double.MIN_VALUE;
    private double prevCamZ = Double.MIN_VALUE;
    private double prevCamRotX = Double.MIN_VALUE;
    private double prevCamRotY = Double.MIN_VALUE;
    @Nullable
    private SectionRenderDispatcher sectionRenderDispatcher;
    private int lastViewDistance = -1;
    private final List<Entity> visibleEntities = new ArrayList<>();
    private int visibleEntityCount;
    private Frustum cullingFrustum;
    private boolean captureFrustum;
    @Nullable
    private Frustum capturedFrustum;
    @Nullable
    private BlockPos lastTranslucentSortBlockPos;
    private int translucencyResortIterationIndex;
    private Set<SectionRenderDispatcher.RenderSection> chunksToResortTransparency = new LinkedHashSet<>();
    private int countChunksToUpdate = 0;
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> renderInfosTerrain = new ObjectArrayList<>(1024);
    private LongOpenHashSet renderInfosEntities = new LongOpenHashSet(1024);
    private List<SectionRenderDispatcher.RenderSection> renderInfosTileEntities = new ArrayList<>(1024);
    private ObjectArrayList renderInfosTerrainNormal = new ObjectArrayList(1024);
    private LongOpenHashSet renderInfosEntitiesNormal = new LongOpenHashSet(1024);
    private List renderInfosTileEntitiesNormal = new ArrayList(1024);
    private ObjectArrayList renderInfosTerrainShadow = new ObjectArrayList(1024);
    private LongOpenHashSet renderInfosEntitiesShadow = new LongOpenHashSet(1024);
    private List renderInfosTileEntitiesShadow = new ArrayList(1024);
    protected int renderDistance = 0;
    protected int renderDistanceSq = 0;
    protected int renderDistanceXZSq = 0;
    private int countTileEntitiesRendered;
    private RenderEnv renderEnv = new RenderEnv(Blocks.AIR.defaultBlockState(), new BlockPos(0, 0, 0));
    public boolean renderOverlayDamaged = false;
    public boolean renderOverlayEyes = false;
    private boolean firstWorldLoad = false;
    private static int renderEntitiesCounter = 0;
    public int loadVisibleChunksCounter = -1;
    public static MessageSignature loadVisibleChunksMessageId = new MessageSignature(RandomUtils.getRandomBytes(256));
    private static boolean ambientOcclusion = false;
    private Map<String, List<Entity>> mapEntityLists = new HashMap<>();
    private Map<RenderType, Map> mapRegionLayers = new LinkedHashMap<>();
    private int frameId;
    private boolean debugFixTerrainFrustumShadow;

    public LevelRenderer(Minecraft pMinecraft, EntityRenderDispatcher pEntityRenderDispatcher, BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, RenderBuffers pRenderBuffers) {
        this.minecraft = pMinecraft;
        this.entityRenderDispatcher = pEntityRenderDispatcher;
        this.blockEntityRenderDispatcher = pBlockEntityRenderDispatcher;
        this.renderBuffers = pRenderBuffers;
    }

    public void tickParticles(Camera pCamera) {
        this.weatherEffectRenderer.tickRainParticles(this.minecraft.level, pCamera, this.ticks, this.minecraft.options.particles().get());
    }

    @Override
    public void close() {
        if (this.entityOutlineTarget != null) {
            this.entityOutlineTarget.destroyBuffers();
        }

        this.skyRenderer.close();
        this.cloudRenderer.close();
    }

    @Override
    public void onResourceManagerReload(ResourceManager pResourceManager) {
        this.initOutline();
    }

    public void initOutline() {
        if (this.entityOutlineTarget != null) {
            this.entityOutlineTarget.destroyBuffers();
        }

        this.entityOutlineTarget = new TextureTarget(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), true);
        this.entityOutlineTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
    }

    @Nullable
    private PostChain getTransparencyChain() {
        if (!Minecraft.useShaderTransparency()) {
            return null;
        } else {
            PostChain postchain = this.minecraft.getShaderManager().getPostChain(TRANSPARENCY_POST_CHAIN_ID, LevelTargetBundle.SORTING_TARGETS);
            if (postchain == null) {
                this.minecraft.options.graphicsMode().set(GraphicsStatus.FANCY);
                this.minecraft.options.save();
            }

            return postchain;
        }
    }

    public void doEntityOutline() {
        if (this.shouldShowEntityOutlines()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ZERO,
                GlStateManager.DestFactor.ONE
            );
            this.entityOutlineTarget.blitAndBlendToScreen(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    public boolean shouldShowEntityOutlines() {
        return !Config.isShaders() && Config.isMainFramebufferEnabled()
            ? !this.minecraft.gameRenderer.isPanoramicMode() && this.entityOutlineTarget != null && this.minecraft.player != null
            : false;
    }

    public void setLevel(@Nullable ClientLevel pLevel) {
        this.lastCameraSectionX = Integer.MIN_VALUE;
        this.lastCameraSectionY = Integer.MIN_VALUE;
        this.lastCameraSectionZ = Integer.MIN_VALUE;
        this.entityRenderDispatcher.setLevel(pLevel);
        this.level = pLevel;
        if (Config.isDynamicLights()) {
            DynamicLights.clear();
        }

        ChunkVisibility.reset();
        this.renderEnv.reset(null, null);
        BiomeUtils.onWorldChanged(this.level);
        Shaders.checkWorldChanged(this.level);
        if (pLevel != null) {
            this.allChanged();
        } else {
            if (this.viewArea != null) {
                this.viewArea.releaseAllBuffers();
                this.viewArea = null;
            }

            if (this.sectionRenderDispatcher != null) {
                this.sectionRenderDispatcher.dispose();
            }

            this.sectionRenderDispatcher = null;
            this.globalBlockEntities.clear();
            this.sectionOcclusionGraph.waitAndReset(null);
            this.clearVisibleSections();
            this.clearRenderInfos();
        }
    }

    private void clearVisibleSections() {
        this.visibleSections.clear();
        this.nearbyVisibleSections.clear();
    }

    public void allChanged() {
        if (this.level != null) {
            this.level.clearTintCaches();
            if (this.sectionRenderDispatcher == null) {
                this.sectionRenderDispatcher = new SectionRenderDispatcher(
                    this.level, this, Util.backgroundExecutor(), this.renderBuffers, this.minecraft.getBlockRenderer(), this.minecraft.getBlockEntityRenderDispatcher()
                );
            } else {
                this.sectionRenderDispatcher.setLevel(this.level);
            }

            this.cloudRenderer.markForRebuild();
            ItemBlockRenderTypes.setFancy(Config.isTreesFancy());
            ModelBlockRenderer.updateAoLightValue();
            if (Config.isDynamicLights()) {
                DynamicLights.clear();
            }

            SmartAnimations.update();
            ambientOcclusion = Minecraft.useAmbientOcclusion();
            this.lastViewDistance = this.minecraft.options.getEffectiveRenderDistance();
            this.renderDistance = this.lastViewDistance * 16;
            this.renderDistanceSq = this.renderDistance * this.renderDistance;
            double d0 = (double)((this.lastViewDistance + 1) * 16);
            this.renderDistanceXZSq = (int)(d0 * d0);
            if (this.viewArea != null) {
                this.viewArea.releaseAllBuffers();
            }

            GpuMemory.bufferFreed(GpuMemory.getBufferAllocated());
            this.sectionRenderDispatcher.blockUntilClear();
            synchronized (this.globalBlockEntities) {
                this.globalBlockEntities.clear();
            }

            this.viewArea = new ViewArea(this.sectionRenderDispatcher, this.level, this.minecraft.options.getEffectiveRenderDistance(), this);
            this.sectionOcclusionGraph.waitAndReset(this.viewArea);
            this.clearVisibleSections();
            this.clearRenderInfos();
            this.killFrustum();
            Camera camera = this.minecraft.gameRenderer.getMainCamera();
            this.viewArea.repositionCamera(SectionPos.of(camera.getPosition()));
        }
    }

    public void resize(int pWidth, int pHeight) {
        this.needsUpdate();
        if (this.entityOutlineTarget != null) {
            this.entityOutlineTarget.resize(pWidth, pHeight);
        }
    }

    public String getSectionStatistics() {
        int i = this.viewArea.sections.length;
        int j = this.countRenderedSections();
        return String.format(
            Locale.ROOT,
            "C: %d/%d %sD: %d, %s",
            j,
            i,
            this.minecraft.smartCull ? "(s) " : "",
            this.lastViewDistance,
            this.sectionRenderDispatcher == null ? "null" : this.sectionRenderDispatcher.getStats()
        );
    }

    public SectionRenderDispatcher getSectionRenderDispatcher() {
        return this.sectionRenderDispatcher;
    }

    public double getTotalSections() {
        return (double)this.viewArea.sections.length;
    }

    public double getLastViewDistance() {
        return (double)this.lastViewDistance;
    }

    public int countRenderedSections() {
        return this.renderInfosTerrain.size();
    }

    public String getEntityStatistics() {
        return "E: " + this.visibleEntityCount + "/" + this.level.getEntityCount() + ", SD: " + this.level.getServerSimulationDistance() + ", " + Config.getVersionDebug();
    }

    private void setupRender(Camera pCamera, Frustum pFrustum, boolean pHasCapturedFrustum, boolean pIsSpectator) {
        Vec3 vec3 = pCamera.getPosition();
        if (this.minecraft.options.getEffectiveRenderDistance() != this.lastViewDistance) {
            this.allChanged();
        }

        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("camera");
        int i = SectionPos.posToSectionCoord(vec3.x());
        int j = SectionPos.posToSectionCoord(vec3.y());
        int k = SectionPos.posToSectionCoord(vec3.z());
        if (this.lastCameraSectionX != i || this.lastCameraSectionY != j || this.lastCameraSectionZ != k) {
            this.lastCameraSectionX = i;
            this.lastCameraSectionY = j;
            this.lastCameraSectionZ = k;
            this.viewArea.repositionCamera(SectionPos.of(vec3));
        }

        if (Config.isDynamicLights()) {
            DynamicLights.update(this);
        }

        this.sectionRenderDispatcher.setCamera(vec3);
        profilerfiller.popPush("cull");
        double d0 = Math.floor(vec3.x / 8.0);
        double d1 = Math.floor(vec3.y / 8.0);
        double d2 = Math.floor(vec3.z / 8.0);
        if (d0 != this.prevCamX || d1 != this.prevCamY || d2 != this.prevCamZ) {
            this.sectionOcclusionGraph.invalidate();
        }

        this.prevCamX = d0;
        this.prevCamY = d1;
        this.prevCamZ = d2;
        profilerfiller.popPush("update");
        Lagometer.timerVisibility.start();
        if (!pHasCapturedFrustum) {
            boolean flag = this.minecraft.smartCull;
            if (pIsSpectator && this.level.getBlockState(pCamera.getBlockPosition()).isSolidRender()) {
                flag = false;
            }

            profilerfiller.push("section_occlusion_graph");
            this.sectionOcclusionGraph.update(flag, pCamera, pFrustum, this.visibleSections, this.level.getChunkSource().getLoadedEmptySections());
            profilerfiller.pop();
            double d3 = Math.floor((double)(pCamera.getXRot() / 2.0F));
            double d4 = Math.floor((double)(pCamera.getYRot() / 2.0F));
            boolean flag1 = false;
            if (this.sectionOcclusionGraph.consumeFrustumUpdate() || d3 != this.prevCamRotX || d4 != this.prevCamRotY) {
                this.applyFrustum(offsetFrustum(pFrustum));
                this.prevCamRotX = d3;
                this.prevCamRotY = d4;
                flag1 = true;
                ShadersRender.frustumTerrainShadowChanged = true;
            }

            if (this.level.getSectionStorage().resetUpdated() || flag1) {
                this.applyFrustumEntities(pFrustum, -1);
                ShadersRender.frustumEntitiesShadowChanged = true;
            }
        }

        Lagometer.timerVisibility.end();
        profilerfiller.pop();
    }

    public static Frustum offsetFrustum(Frustum pFrustum) {
        return new Frustum(pFrustum).offsetToFullyIncludeCameraCube(8);
    }

    private void applyFrustum(Frustum pFrustum) {
        this.applyFrustum(pFrustum, true, -1);
    }

    public void applyFrustum(Frustum frustumIn, boolean updateRenderInfos, int maxChunkDistance) {
        if (!Minecraft.getInstance().isSameThread()) {
            throw new IllegalStateException("applyFrustum called from wrong thread: " + Thread.currentThread().getName());
        } else {
            Profiler.get().push("apply_frustum");
            if (updateRenderInfos) {
                this.clearVisibleSections();
            }

            this.clearRenderInfosTerrain();
            this.sectionOcclusionGraph.addSectionsInFrustum(frustumIn, this.visibleSections, this.nearbyVisibleSections, updateRenderInfos, maxChunkDistance);
            Profiler.get().pop();
        }
    }

    public void addRecentlyCompiledSection(SectionRenderDispatcher.RenderSection pRenderSection) {
        this.sectionOcclusionGraph.schedulePropagationFrom(pRenderSection);
    }

    public void prepareCullFrustum(Vec3 pCameraPosition, Matrix4f pFrustumMatrix, Matrix4f pProjectionMatrix) {
        this.cullingFrustum = new Frustum(pFrustumMatrix, pProjectionMatrix);
        this.cullingFrustum.prepare(pCameraPosition.x(), pCameraPosition.y(), pCameraPosition.z());
        if (Config.isShaders() && !Shaders.isFrustumCulling()) {
            this.cullingFrustum.disabled = true;
        }
    }

    public void renderLevel(
        GraphicsResourceAllocator pGraphicsResourceAllocator,
        DeltaTracker pDeltaTracker,
        boolean pRenderBlockOutline,
        Camera pCamera,
        GameRenderer pGameRenderer,
        Matrix4f pFrustumMatrix,
        Matrix4f pProjectionMatrix
    ) {
        float f = pDeltaTracker.getGameTimeDeltaPartialTick(false);
        RenderSystem.setShaderGameTime(this.level.getGameTime(), f);
        this.blockEntityRenderDispatcher.prepare(this.level, pCamera, this.minecraft.hitResult);
        this.entityRenderDispatcher.prepare(this.level, pCamera, this.minecraft.crosshairPickEntity);
        final ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.popPush("light_update_queue");
        this.level.pollLightUpdates();
        profilerfiller.popPush("light_updates");
        this.level.getChunkSource().getLightEngine().runLightUpdates();
        Vec3 vec3 = pCamera.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        profilerfiller.popPush("culling");
        boolean flag = this.capturedFrustum != null;
        Frustum frustum = flag ? this.capturedFrustum : this.cullingFrustum;
        Profiler.get().popPush("captureFrustum");
        if (this.captureFrustum) {
            this.capturedFrustum = flag ? new Frustum(pFrustumMatrix, pProjectionMatrix) : frustum;
            this.capturedFrustum.prepare(d0, d1, d2);
            this.captureFrustum = false;
            frustum = this.capturedFrustum;
            frustum.disabled = Config.isShaders() && !Shaders.isFrustumCulling();
            frustum.prepare(vec3.x, vec3.y, vec3.z);
            this.applyFrustum(frustum, false, -1);
            this.applyFrustumEntities(frustum, -1);
        }

        if (this.debugFixTerrainFrustumShadow) {
            this.capturedFrustum = flag ? new Frustum(pFrustumMatrix, pProjectionMatrix) : frustum;
            this.capturedFrustum.prepare(d0, d1, d2);
            this.debugFixTerrainFrustumShadow = false;
            frustum = this.capturedFrustum;
            frustum.prepare(vec3.x, vec3.y, vec3.z);
            ShadersRender.frustumTerrainShadowChanged = true;
            ShadersRender.frustumEntitiesShadowChanged = true;
            ShadersRender.applyFrustumShadow(this, frustum);
        }

        if (Config.isShaders()) {
            Shaders.setViewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
        } else {
            RenderSystem.viewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
        }

        boolean flag1 = Config.isShaders();
        profilerfiller.popPush("fog");
        float f1 = pGameRenderer.getRenderDistance();
        boolean flag2 = this.minecraft.level.effects().isFoggyAt(Mth.floor(d0), Mth.floor(d1)) || this.minecraft.gui.getBossOverlay().shouldCreateWorldFog();
        boolean flag3 = this.minecraft.level.effects().isFoggyAt(Mth.floor(d0), Mth.floor(d1));
        Vector4f vector4f = FogRenderer.computeFogColor(pCamera, f, this.minecraft.level, this.minecraft.options.getEffectiveRenderDistance(), pGameRenderer.getDarkenWorldAmount(f));
        FogParameters fogparameters = FogRenderer.setupFog(pCamera, FogRenderer.FogMode.FOG_TERRAIN, vector4f, f1, flag2, f);
        FogParameters fogparameters1 = FogRenderer.setupFog(pCamera, FogRenderer.FogMode.FOG_SKY, vector4f, f1, flag2, f);
        profilerfiller.popPush("cullEntities");
        boolean flag4 = this.collectVisibleEntities(pCamera, frustum, this.visibleEntities);
        this.visibleEntityCount = this.visibleEntities.size();
        this.checkLoadVisibleChunks(pCamera, frustum, this.minecraft.player.isSpectator());
        this.frameId++;
        profilerfiller.popPush("terrain_setup");
        this.setupRender(pCamera, frustum, flag, this.minecraft.player.isSpectator());
        profilerfiller.popPush("compile_sections");
        this.compileSections(pCamera);
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.mul(pFrustumMatrix);
        FrameGraphBuilder framegraphbuilder = new FrameGraphBuilder();
        this.targets.main = framegraphbuilder.importExternal("main", this.minecraft.getMainRenderTarget());
        int i = this.minecraft.getMainRenderTarget().width;
        int j = this.minecraft.getMainRenderTarget().height;
        RenderTargetDescriptor rendertargetdescriptor = new RenderTargetDescriptor(i, j, true);
        PostChain postchain = this.getTransparencyChain();
        if (postchain != null) {
            this.targets.translucent = framegraphbuilder.createInternal("translucent", rendertargetdescriptor);
            this.targets.itemEntity = framegraphbuilder.createInternal("item_entity", rendertargetdescriptor);
            this.targets.particles = framegraphbuilder.createInternal("particles", rendertargetdescriptor);
            this.targets.weather = framegraphbuilder.createInternal("weather", rendertargetdescriptor);
            this.targets.clouds = framegraphbuilder.createInternal("clouds", rendertargetdescriptor);
        }

        if (this.entityOutlineTarget != null) {
            this.targets.entityOutline = framegraphbuilder.importExternal("entity_outline", this.entityOutlineTarget);
        }

        FramePass framepass = framegraphbuilder.addPass("clear");
        this.targets.main = framepass.readsAndWrites(this.targets.main);
        framepass.executes(() -> {
            RenderSystem.clearColor(vector4f.x, vector4f.y, vector4f.z, 0.0F);
            RenderSystem.clear(16640);
            if (flag1) {
                Shaders.clearRenderBuffer();
                Shaders.setCamera(pFrustumMatrix, pCamera, f);
                Shaders.renderPrepare();
            }
        });
        if (!flag3) {
            if ((Config.isSkyEnabled() || Config.isSunMoonEnabled() || Config.isStarsEnabled()) && !Shaders.isShadowPass) {
                this.addSkyPass(framegraphbuilder, pCamera, f, fogparameters1);
            } else {
                GlStateManager._disableBlend();
            }
        }

        this.addMainPass(framegraphbuilder, frustum, pCamera, pFrustumMatrix, pProjectionMatrix, fogparameters, pRenderBlockOutline, flag4, pDeltaTracker, profilerfiller);
        PostChain postchain1 = this.minecraft.getShaderManager().getPostChain(ENTITY_OUTLINE_POST_CHAIN_ID, LevelTargetBundle.OUTLINE_TARGETS);
        if (flag4 && postchain1 != null) {
            postchain1.addToFrame(framegraphbuilder, i, j, this.targets);
        }

        if (!Shaders.isParticlesBeforeDeferred()) {
            this.addParticlesPass(framegraphbuilder, pCamera, f, fogparameters, frustum);
        }

        CloudStatus cloudstatus = this.minecraft.options.getCloudsType();
        if (cloudstatus != CloudStatus.OFF && !Config.isCloudsOff()) {
            float f2 = this.level.effects().getCloudHeight();
            if (!Float.isNaN(f2)) {
                f2 = (float)((double)f2 + this.minecraft.options.ofCloudsHeight * 128.0);
                float f3 = (float)this.ticks + f;
                int k = this.level.getCloudColor(f);
                this.addCloudsPass(framegraphbuilder, pFrustumMatrix, pProjectionMatrix, cloudstatus, pCamera.getPosition(), f3, k, f2 + 0.33F, fogparameters1);
            }
        }

        this.addWeatherPass(framegraphbuilder, pCamera.getPosition(), f, fogparameters);
        if (postchain != null) {
            postchain.addToFrame(framegraphbuilder, i, j, this.targets);
        }

        this.addLateDebugPass(framegraphbuilder, vec3, fogparameters);
        profilerfiller.popPush("framegraph");
        framegraphbuilder.execute(pGraphicsResourceAllocator, new FrameGraphBuilder.Inspector() {
            @Override
            public void beforeExecutePass(String p_367748_) {
                profilerfiller.push(p_367748_);
            }

            @Override
            public void afterExecutePass(String p_367757_) {
                profilerfiller.pop();
            }
        });
        this.minecraft.getMainRenderTarget().bindWrite(false);
        this.visibleEntities.clear();
        this.targets.clear();
        matrix4fstack.popMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderFog(FogParameters.NO_FOG);
    }

    private void addMainPass(
        FrameGraphBuilder pFrameGraphBuilder,
        Frustum pFrustum,
        Camera pCamera,
        Matrix4f pFrustumMatrix,
        Matrix4f pProjectionMatrix,
        FogParameters pFogParameters,
        boolean pRenderBlockOutline,
        boolean pRenderEntityOutline,
        DeltaTracker pDeltaTracker,
        ProfilerFiller pProfiler
    ) {
        FramePass framepass = pFrameGraphBuilder.addPass("main");
        this.targets.main = framepass.readsAndWrites(this.targets.main);
        if (this.targets.translucent != null) {
            this.targets.translucent = framepass.readsAndWrites(this.targets.translucent);
        }

        if (this.targets.itemEntity != null) {
            this.targets.itemEntity = framepass.readsAndWrites(this.targets.itemEntity);
        }

        if (this.targets.weather != null) {
            this.targets.weather = framepass.readsAndWrites(this.targets.weather);
        }

        if (pRenderEntityOutline && this.targets.entityOutline != null) {
            this.targets.entityOutline = framepass.readsAndWrites(this.targets.entityOutline);
        }

        ResourceHandle<RenderTarget> resourcehandle = this.targets.main;
        ResourceHandle<RenderTarget> resourcehandle1 = this.targets.translucent;
        ResourceHandle<RenderTarget> resourcehandle2 = this.targets.itemEntity;
        ResourceHandle<RenderTarget> resourcehandle3 = this.targets.weather;
        ResourceHandle<RenderTarget> resourcehandle4 = this.targets.entityOutline;
        framepass.executes(() -> {
            RenderSystem.setShaderFog(pFogParameters);
            float f = pDeltaTracker.getGameTimeDeltaPartialTick(false);
            Vec3 vec3 = pCamera.getPosition();
            double d0 = vec3.x();
            double d1 = vec3.y();
            double d2 = vec3.z();
            pProfiler.push("terrain");
            Lagometer.timerTerrain.start();
            if (this.minecraft.options.ofSmoothFps) {
                pProfiler.popPush("finish");
                GL11.glFinish();
                pProfiler.popPush("terrain");
            }

            boolean flag = Config.isShaders();
            this.renderSectionLayer(RenderType.solid(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
            this.minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).setBlurMipmap(false, this.minecraft.options.mipmapLevels().get() > 0);
            this.renderSectionLayer(RenderType.cutoutMipped(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
            this.minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).restoreLastBlurMipmap();
            this.renderSectionLayer(RenderType.cutout(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
            if (flag) {
                ShadersRender.endTerrain();
            }

            if (this.level.effects().constantAmbientLight()) {
                Lighting.setupNetherLevel();
            } else {
                Lighting.setupLevel();
            }

            if (resourcehandle2 != null) {
                resourcehandle2.get().setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                resourcehandle2.get().clear();
                resourcehandle2.get().copyDepthFrom(this.minecraft.getMainRenderTarget());
                resourcehandle.get().bindWrite(false);
            }

            if (resourcehandle3 != null) {
                resourcehandle3.get().setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                resourcehandle3.get().clear();
            }

            if (this.shouldShowEntityOutlines() && resourcehandle4 != null) {
                resourcehandle4.get().setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                resourcehandle4.get().clear();
                resourcehandle.get().bindWrite(false);
            }

            if (flag) {
                Shaders.beginEntities();
            }

            ItemFrameRenderer.updateItemRenderDistance();
            renderEntitiesCounter++;
            this.countTileEntitiesRendered = 0;
            PoseStack posestack = new PoseStack();
            MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
            MultiBufferSource.BufferSource multibuffersource$buffersource1 = this.renderBuffers.crumblingBufferSource();
            pProfiler.popPush("entities");
            if (Config.isFastRender()) {
                RenderStateManager.enableCache();
                multibuffersource$buffersource.enableCache();
            }

            if (Config.isRenderRegions() || Config.isMultiTexture()) {
                RenderSystem.setShader(CoreShaders.POSITION);
            }

            this.renderEntities(posestack, multibuffersource$buffersource, pCamera, pDeltaTracker, this.visibleEntities);
            multibuffersource$buffersource.endLastBatch();
            this.checkPoseStack(posestack);
            if (Config.isFastRender()) {
                multibuffersource$buffersource.flushCache();
                RenderStateManager.flushCache();
            }

            if (flag) {
                Shaders.endEntities();
            }

            if (flag) {
                Shaders.beginBlockEntities();
            }

            SignRenderer.updateTextRenderDistance();
            pProfiler.popPush("blockentities");
            boolean flag1 = this.renderBlockEntities(posestack, multibuffersource$buffersource, multibuffersource$buffersource1, pCamera, f, pFrustum);
            boolean flag2 = flag1 || pRenderBlockOutline;
            multibuffersource$buffersource.endLastBatch();
            this.checkPoseStack(posestack);
            multibuffersource$buffersource.endBatch(RenderType.solid());
            multibuffersource$buffersource.endBatch(RenderType.endPortal());
            multibuffersource$buffersource.endBatch(RenderType.endGateway());
            multibuffersource$buffersource.endBatch(Sheets.solidBlockSheet());
            multibuffersource$buffersource.endBatch(Sheets.cutoutBlockSheet());
            multibuffersource$buffersource.endBatch(Sheets.bedSheet());
            multibuffersource$buffersource.endBatch(Sheets.shulkerBoxSheet());
            multibuffersource$buffersource.endBatch(Sheets.signSheet());
            multibuffersource$buffersource.endBatch(Sheets.hangingSignSheet());
            multibuffersource$buffersource.endBatch(Sheets.chestSheet());
            this.renderBuffers.outlineBufferSource().endOutlineBatch();
            if (Config.isFastRender()) {
                multibuffersource$buffersource.disableCache();
                RenderStateManager.disableCache();
            }

            Lagometer.timerTerrain.end();
            if (flag2) {
                this.renderBlockOutline(pCamera, multibuffersource$buffersource, posestack, false, f);
            }

            if (flag) {
                Shaders.endBlockEntities();
            }

            pProfiler.popPush("debug");
            this.minecraft.debugRenderer.render(posestack, pFrustum, multibuffersource$buffersource, d0, d1, d2);
            multibuffersource$buffersource.endLastBatch();
            if (flag) {
                RenderUtils.finishRenderBuffers();
                ShadersRender.beginDebug();
            }

            this.checkPoseStack(posestack);
            multibuffersource$buffersource.endBatch(Sheets.translucentItemSheet());
            multibuffersource$buffersource.endBatch(Sheets.bannerSheet());
            multibuffersource$buffersource.endBatch(Sheets.shieldSheet());
            multibuffersource$buffersource.endBatch(RenderType.armorEntityGlint());
            multibuffersource$buffersource.endBatch(RenderType.glint());
            multibuffersource$buffersource.endBatch(RenderType.glintTranslucent());
            multibuffersource$buffersource.endBatch(RenderType.entityGlint());
            this.renderOverlayDamaged = true;
            pProfiler.popPush("destroyProgress");
            this.renderBlockDestroyAnimation(posestack, pCamera, multibuffersource$buffersource1);
            multibuffersource$buffersource1.endBatch();
            this.checkPoseStack(posestack);
            multibuffersource$buffersource.endBatch(RenderType.waterMask());
            multibuffersource$buffersource.endBatch();
            this.renderOverlayDamaged = false;
            RenderUtils.flushRenderBuffers();
            renderEntitiesCounter--;
            if (resourcehandle1 != null) {
                resourcehandle1.get().setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                resourcehandle1.get().clear();
                resourcehandle1.get().copyDepthFrom(resourcehandle.get());
            }

            if (flag) {
                multibuffersource$buffersource.endBatch();
                ShadersRender.endDebug();
                Shaders.preRenderHand();
                Matrix4f matrix4f = MathUtils.copy(RenderSystem.getProjectionMatrix());
                ShadersRender.renderHand0(this.minecraft.gameRenderer, pFrustumMatrix, pCamera, f);
                RenderSystem.setProjectionMatrix(matrix4f, RenderSystem.getProjectionType());
                Shaders.preWater();
            }

            if (Shaders.isParticlesBeforeDeferred()) {
                pProfiler.popPush("particles");
                RenderSystem.setShaderFog(pFogParameters);
                Shaders.beginParticles();
                RenderStateShard.PARTICLES_TARGET.setupRenderState();
                this.minecraft.particleEngine.renderParticles(pCamera, f, this.renderBuffers.bufferSource(), pFrustum);
                RenderStateShard.PARTICLES_TARGET.clearRenderState();
                Shaders.endParticles();
            }

            pProfiler.popPush("translucent");
            Lagometer.timerTerrain.start();
            if (flag) {
                Shaders.beginWater();
            }

            this.renderSectionLayer(RenderType.translucent(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
            if (flag) {
                Shaders.endWater();
            }

            Lagometer.timerTerrain.end();
            pProfiler.popPush("string");
            this.renderSectionLayer(RenderType.tripwire(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
            if (flag2) {
                this.renderBlockOutline(pCamera, multibuffersource$buffersource, posestack, true, f);
            }

            multibuffersource$buffersource.endBatch();
            pProfiler.pop();
        });
    }

    private void addParticlesPass(FrameGraphBuilder pFrameGraphBuilder, Camera pCamera, float pPartialTick, FogParameters pFog) {
        this.addParticlesPass(pFrameGraphBuilder, pCamera, pPartialTick, pFog, FrustumDummy.INSTANCE);
    }

    private void addParticlesPass(FrameGraphBuilder graphBuilderIn, Camera cameraIn, float partialTicks, FogParameters fogParamsIn, Frustum frustumIn) {
        FramePass framepass = graphBuilderIn.addPass("particles");
        if (this.targets.particles != null) {
            this.targets.particles = framepass.readsAndWrites(this.targets.particles);
            framepass.reads(this.targets.main);
        } else {
            this.targets.main = framepass.readsAndWrites(this.targets.main);
        }

        ResourceHandle<RenderTarget> resourcehandle = this.targets.main;
        ResourceHandle<RenderTarget> resourcehandle1 = this.targets.particles;
        framepass.executes(() -> {
            RenderSystem.setShaderFog(fogParamsIn);
            if (resourcehandle1 != null) {
                resourcehandle1.get().setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                resourcehandle1.get().clear();
                resourcehandle1.get().copyDepthFrom(resourcehandle.get());
            }

            if (Config.isShaders()) {
                Shaders.beginParticles();
            }

            this.minecraft.particleEngine.renderParticles(cameraIn, partialTicks, this.renderBuffers.bufferSource(), frustumIn);
            if (Config.isShaders()) {
                Shaders.endParticles();
            }
        });
    }

    private void addCloudsPass(
        FrameGraphBuilder pFrameGraphBuilder,
        Matrix4f pFrustumMatrix,
        Matrix4f pProjectionMatrix,
        CloudStatus pCloudStatus,
        Vec3 pCameraPosition,
        float pAgeInTicks,
        int pHeight,
        float pTicks
    ) {
        this.addCloudsPass(pFrameGraphBuilder, pFrustumMatrix, pProjectionMatrix, pCloudStatus, pCameraPosition, pAgeInTicks, pHeight, pTicks, null);
    }

    private void addCloudsPass(
        FrameGraphBuilder builderIn,
        Matrix4f viewIn,
        Matrix4f projectionIn,
        CloudStatus statusIn,
        Vec3 posIn,
        float ticksIn,
        int colorIn,
        float cloudHeightIn,
        FogParameters fogParamsIn
    ) {
        FramePass framepass = builderIn.addPass("clouds");
        if (this.targets.clouds != null) {
            this.targets.clouds = framepass.readsAndWrites(this.targets.clouds);
        } else {
            this.targets.main = framepass.readsAndWrites(this.targets.main);
        }

        ResourceHandle<RenderTarget> resourcehandle = this.targets.clouds;
        framepass.executes(() -> {
            if (resourcehandle != null) {
                resourcehandle.get().setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                resourcehandle.get().clear();
            }

            if (Config.isShaders()) {
                Shaders.beginClouds();
            }

            if (fogParamsIn != null) {
                RenderSystem.setShaderFog(fogParamsIn);
            }

            this.cloudRenderer.render(colorIn, statusIn, cloudHeightIn, viewIn, projectionIn, posIn, ticksIn);
            if (Config.isShaders()) {
                Shaders.endClouds();
            }
        });
    }

    private void addWeatherPass(FrameGraphBuilder pFrameGraphBuilder, Vec3 pCameraPosition, float pPartialTick, FogParameters pFog) {
        int i = this.minecraft.options.getEffectiveRenderDistance() * 16;
        float f = this.minecraft.gameRenderer.getDepthFar();
        FramePass framepass = pFrameGraphBuilder.addPass("weather");
        if (this.targets.weather != null) {
            this.targets.weather = framepass.readsAndWrites(this.targets.weather);
        } else {
            this.targets.main = framepass.readsAndWrites(this.targets.main);
        }

        framepass.executes(() -> {
            RenderSystem.setShaderFog(pFog);
            MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
            if (Config.isShaders()) {
                Shaders.beginWeather();
            }

            this.weatherEffectRenderer.render(this.minecraft.level, multibuffersource$buffersource, this.ticks, pPartialTick, pCameraPosition);
            if (Config.isShaders()) {
                Shaders.endWeather();
            }

            this.worldBorderRenderer.render(this.level.getWorldBorder(), pCameraPosition, (double)i, (double)f);
            multibuffersource$buffersource.endBatch();
        });
    }

    private void addLateDebugPass(FrameGraphBuilder pFrameGraphBuilder, Vec3 pCameraPosition, FogParameters pFog) {
        FramePass framepass = pFrameGraphBuilder.addPass("late_debug");
        this.targets.main = framepass.readsAndWrites(this.targets.main);
        if (this.targets.itemEntity != null) {
            this.targets.itemEntity = framepass.readsAndWrites(this.targets.itemEntity);
        }

        ResourceHandle<RenderTarget> resourcehandle = this.targets.main;
        framepass.executes(() -> {
            RenderSystem.setShaderFog(pFog);
            resourcehandle.get().bindWrite(false);
            PoseStack posestack = new PoseStack();
            MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
            this.minecraft.debugRenderer.renderAfterTranslucents(posestack, multibuffersource$buffersource, pCameraPosition.x, pCameraPosition.y, pCameraPosition.z);
            multibuffersource$buffersource.endLastBatch();
            this.checkPoseStack(posestack);
        });
    }

    private boolean collectVisibleEntities(Camera pCamera, Frustum pFrustum, List<Entity> pOutput) {
        Vec3 vec3 = pCamera.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        boolean flag = false;
        boolean flag1 = this.shouldShowEntityOutlines();
        Entity.setViewScale(Mth.clamp((double)this.minecraft.options.getEffectiveRenderDistance() / 8.0, 1.0, 2.5) * this.minecraft.options.entityDistanceScaling().get());
        int i = this.level.getMinY();
        int j = this.level.getMaxY();

        for (Entity entity : this.level.entitiesForRendering()) {
            if (this.shouldRenderEntity(entity, i, j)) {
                boolean flag2 = entity == this.minecraft.player && !this.minecraft.player.isSpectator();
                if (this.entityRenderDispatcher.shouldRender(entity, pFrustum, d0, d1, d2) || entity.hasIndirectPassenger(this.minecraft.player)) {
                    BlockPos blockpos = entity.blockPosition();
                    if ((this.level.isOutsideBuildHeight(blockpos.getY()) || this.isSectionCompiled(blockpos))
                        && (
                            entity != pCamera.getEntity()
                                || pCamera.isDetached()
                                || pCamera.getEntity() instanceof LivingEntity && ((LivingEntity)pCamera.getEntity()).isSleeping()
                        )
                        && (!(entity instanceof LocalPlayer) || pCamera.getEntity() == entity || flag2)) {
                        String s = entity.getClass().getName();
                        List<Entity> list = this.mapEntityLists.get(s);
                        if (list == null) {
                            list = new ArrayList<>();
                            this.mapEntityLists.put(s, list);
                        }

                        list.add(entity);
                        boolean flag3 = this.minecraft.shouldEntityAppearGlowing(entity);
                        if (Reflector.IForgeEntity_hasCustomOutlineRendering.exists()) {
                            flag3 = flag3 || Reflector.callBoolean(entity, Reflector.IForgeEntity_hasCustomOutlineRendering, this.minecraft.player);
                        }

                        if (flag1 && flag3) {
                            flag = true;
                        }
                    }
                }
            }
        }

        return flag;
    }

    private void renderEntities(PoseStack pPoseStack, MultiBufferSource.BufferSource pBufferSource, Camera pCamera, DeltaTracker pDeltaTracker, List<Entity> pEntities) {
        Vec3 vec3 = pCamera.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        TickRateManager tickratemanager = this.minecraft.level.tickRateManager();
        boolean flag = this.shouldShowEntityOutlines();
        boolean flag1 = Config.isShaders();

        for (List<Entity> list : this.mapEntityLists.values()) {
            for (Entity entity : list) {
                if (entity.tickCount == 0) {
                    entity.xOld = entity.getX();
                    entity.yOld = entity.getY();
                    entity.zOld = entity.getZ();
                }

                MultiBufferSource multibuffersource;
                if (flag && this.minecraft.shouldEntityAppearGlowing(entity)) {
                    OutlineBufferSource outlinebuffersource = this.renderBuffers.outlineBufferSource();
                    multibuffersource = outlinebuffersource;
                    int i = entity.getTeamColor();
                    outlinebuffersource.setColor(ARGB.red(i), ARGB.green(i), ARGB.blue(i), 255);
                } else {
                    multibuffersource = pBufferSource;
                }

                if (flag1) {
                    Shaders.nextEntity(entity);
                }

                float f = pDeltaTracker.getGameTimeDeltaPartialTick(!tickratemanager.isEntityFrozen(entity));
                this.renderEntity(entity, d0, d1, d2, f, pPoseStack, multibuffersource);
            }

            list.clear();
        }
    }

    private void renderBlockEntities(
        PoseStack pPoseStack, MultiBufferSource.BufferSource pBufferSource, MultiBufferSource.BufferSource pCrumblingBufferSource, Camera pCamera, float pPartialTick
    ) {
        this.renderBlockEntities(pPoseStack, pBufferSource, pCrumblingBufferSource, pCamera, pPartialTick, FrustumDummy.INSTANCE);
    }

    private boolean renderBlockEntities(
        PoseStack matrixStackIn,
        MultiBufferSource.BufferSource bufferSourceIn,
        MultiBufferSource.BufferSource crumblingSourceIn,
        Camera cameraIn,
        float partialTicks,
        Frustum frustumIn
    ) {
        boolean flag = false;
        Vec3 vec3 = cameraIn.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        boolean flag1 = Config.isShaders();
        boolean flag2 = Reflector.IForgeBlockEntity_getRenderBoundingBox.exists();

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.renderInfosTileEntities) {
            List<BlockEntity> list = sectionrenderdispatcher$rendersection.getCompiled().getRenderableBlockEntities();
            if (!list.isEmpty()) {
                for (BlockEntity blockentity : list) {
                    if (flag2) {
                        AABB aabb = (AABB)Reflector.call(blockentity, Reflector.IForgeBlockEntity_getRenderBoundingBox);
                        if (aabb != null && !frustumIn.isVisible(aabb)) {
                            continue;
                        }
                    }

                    if (flag1) {
                        Shaders.nextBlockEntity(blockentity);
                    }

                    BlockPos blockpos1 = blockentity.getBlockPos();
                    MultiBufferSource multibuffersource = bufferSourceIn;
                    matrixStackIn.pushPose();
                    matrixStackIn.translate((double)blockpos1.getX() - d0, (double)blockpos1.getY() - d1, (double)blockpos1.getZ() - d2);
                    SortedSet<BlockDestructionProgress> sortedset = this.destructionProgress.get(blockpos1.asLong());
                    if (sortedset != null && !sortedset.isEmpty()) {
                        int i = sortedset.last().getProgress();
                        if (i >= 0) {
                            PoseStack.Pose posestack$pose = matrixStackIn.last();
                            VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(
                                crumblingSourceIn.getBuffer(ModelBakery.DESTROY_TYPES.get(i)), posestack$pose, 1.0F
                            );
                            multibuffersource = renderTypeIn -> {
                                VertexConsumer vertexconsumer1 = bufferSourceIn.getBuffer(renderTypeIn);
                                return renderTypeIn.affectsCrumbling() ? VertexMultiConsumer.create(vertexconsumer, vertexconsumer1) : vertexconsumer1;
                            };
                        }
                    }

                    if (Reflector.IForgeBlockEntity_hasCustomOutlineRendering.exists()
                        && this.shouldShowEntityOutlines()
                        && Reflector.callBoolean(blockentity, Reflector.IForgeBlockEntity_hasCustomOutlineRendering, this.minecraft.player)) {
                        flag = true;
                    }

                    this.blockEntityRenderDispatcher.render(blockentity, partialTicks, matrixStackIn, multibuffersource);
                    matrixStackIn.popPose();
                    this.countTileEntitiesRendered++;
                }
            }
        }

        synchronized (this.globalBlockEntities) {
            for (BlockEntity blockentity1 : this.globalBlockEntities) {
                if (flag2) {
                    AABB aabb1 = (AABB)Reflector.call(blockentity1, Reflector.IForgeBlockEntity_getRenderBoundingBox);
                    if (aabb1 != null && !frustumIn.isVisible(aabb1)) {
                        continue;
                    }
                }

                if (flag1) {
                    Shaders.nextBlockEntity(blockentity1);
                }

                BlockPos blockpos = blockentity1.getBlockPos();
                matrixStackIn.pushPose();
                matrixStackIn.translate((double)blockpos.getX() - d0, (double)blockpos.getY() - d1, (double)blockpos.getZ() - d2);
                if (Reflector.IForgeBlockEntity_hasCustomOutlineRendering.exists()
                    && this.shouldShowEntityOutlines()
                    && Reflector.callBoolean(blockentity1, Reflector.IForgeBlockEntity_hasCustomOutlineRendering, this.minecraft.player)) {
                    flag = true;
                }

                this.blockEntityRenderDispatcher.render(blockentity1, partialTicks, matrixStackIn, bufferSourceIn);
                matrixStackIn.popPose();
                this.countTileEntitiesRendered++;
            }

            return flag;
        }
    }

    private void renderBlockDestroyAnimation(PoseStack pPoseStack, Camera pCamera, MultiBufferSource.BufferSource pBufferSource) {
        Vec3 vec3 = pCamera.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();

        for (Entry<SortedSet<BlockDestructionProgress>> entry : this.destructionProgress.long2ObjectEntrySet()) {
            BlockPos blockpos = BlockPos.of(entry.getLongKey());
            if (!(blockpos.distToCenterSqr(d0, d1, d2) > 1024.0)) {
                SortedSet<BlockDestructionProgress> sortedset = entry.getValue();
                if (sortedset != null && !sortedset.isEmpty()) {
                    int i = sortedset.last().getProgress();
                    pPoseStack.pushPose();
                    pPoseStack.translate((double)blockpos.getX() - d0, (double)blockpos.getY() - d1, (double)blockpos.getZ() - d2);
                    PoseStack.Pose posestack$pose = pPoseStack.last();
                    VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(pBufferSource.getBuffer(ModelBakery.DESTROY_TYPES.get(i)), posestack$pose, 1.0F);
                    ModelData modeldata = this.level.getModelDataManager().getAt(blockpos);
                    if (modeldata == null) {
                        modeldata = ModelData.EMPTY;
                    }

                    this.minecraft
                        .getBlockRenderer()
                        .renderBreakingTexture(this.level.getBlockState(blockpos), blockpos, this.level, pPoseStack, vertexconsumer, modeldata);
                    pPoseStack.popPose();
                }
            }
        }
    }

    private void renderBlockOutline(Camera pCamera, MultiBufferSource.BufferSource pBufferSource, PoseStack pPoseStack, boolean pSort) {
        this.renderBlockOutline(pCamera, pBufferSource, pPoseStack, pSort, 0.0F);
    }

    private void renderBlockOutline(
        Camera cameraIn, MultiBufferSource.BufferSource bufferSourceIn, PoseStack matrixStackIn, boolean translucentIn, float partialTicks
    ) {
        if (this.minecraft.hitResult instanceof BlockHitResult blockhitresult) {
            if (blockhitresult.getType() != HitResult.Type.MISS) {
                BlockPos blockpos = blockhitresult.getBlockPos();
                BlockState blockstate = this.level.getBlockState(blockpos);
                if (Config.isShaders()) {
                    ShadersRender.beginOutline();
                }

                if (!Reflector.callBoolean(
                        Reflector.ForgeHooksClient_onDrawHighlight, this, cameraIn, blockhitresult, partialTicks, matrixStackIn, bufferSourceIn
                    )
                    && !blockstate.isAir()
                    && this.level.getWorldBorder().isWithinBounds(blockpos)) {
                    boolean flag = ItemBlockRenderTypes.getChunkRenderType(blockstate).sortOnUpload();
                    if (flag != translucentIn) {
                        return;
                    }

                    Vec3 vec3 = cameraIn.getPosition();
                    Boolean obool = this.minecraft.options.highContrastBlockOutline().get();
                    if (obool) {
                        VertexConsumer vertexconsumer = bufferSourceIn.getBuffer(RenderType.secondaryBlockOutline());
                        this.renderHitOutline(
                            matrixStackIn, vertexconsumer, cameraIn.getEntity(), vec3.x, vec3.y, vec3.z, blockpos, blockstate, -16777216
                        );
                    }

                    VertexConsumer vertexconsumer1 = bufferSourceIn.getBuffer(RenderType.lines());
                    int i = obool ? -11010079 : ARGB.color(102, -16777216);
                    this.renderHitOutline(matrixStackIn, vertexconsumer1, cameraIn.getEntity(), vec3.x, vec3.y, vec3.z, blockpos, blockstate, i);
                    bufferSourceIn.endLastBatch();
                }

                if (Config.isShaders()) {
                    bufferSourceIn.endBatch(RenderType.lines());
                    ShadersRender.endOutline();
                }
            } else if (this.minecraft.hitResult instanceof EntityHitResult entityhitresult) {
                Reflector.ForgeHooksClient_onDrawHighlight.call(this, cameraIn, entityhitresult, partialTicks, matrixStackIn, bufferSourceIn);
            }
        }
    }

    public void checkPoseStack(PoseStack pPoseStack) {
        if (!pPoseStack.clear()) {
            throw new IllegalStateException("Pose stack not empty");
        }
    }

    public void renderEntity(
        Entity pEntity, double pCamX, double pCamY, double pCamZ, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource
    ) {
        double d0 = Mth.lerp((double)pPartialTick, pEntity.xOld, pEntity.getX());
        double d1 = Mth.lerp((double)pPartialTick, pEntity.yOld, pEntity.getY());
        double d2 = Mth.lerp((double)pPartialTick, pEntity.zOld, pEntity.getZ());
        this.entityRenderDispatcher
            .render(
                pEntity, d0 - pCamX, d1 - pCamY, d2 - pCamZ, pPartialTick, pPoseStack, pBufferSource, this.entityRenderDispatcher.getPackedLightCoords(pEntity, pPartialTick)
            );
    }

    private void scheduleTranslucentSectionResort(Vec3 pCameraPosition) {
        if (!this.visibleSections.isEmpty()) {
            BlockPos blockpos = BlockPos.containing(pCameraPosition);
            boolean flag = !blockpos.equals(this.lastTranslucentSortBlockPos);
            Profiler.get().push("translucent_sort");
            SectionRenderDispatcher.TranslucencyPointOfView sectionrenderdispatcher$translucencypointofview = new SectionRenderDispatcher.TranslucencyPointOfView();

            for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.nearbyVisibleSections) {
                this.scheduleResort(sectionrenderdispatcher$rendersection, sectionrenderdispatcher$translucencypointofview, pCameraPosition, flag, true);
            }

            this.translucencyResortIterationIndex = this.translucencyResortIterationIndex % this.visibleSections.size();
            int i = Math.max(this.visibleSections.size() / 8, 15);

            while (i-- > 0) {
                int j = this.translucencyResortIterationIndex++ % this.visibleSections.size();
                this.scheduleResort(this.visibleSections.get(j), sectionrenderdispatcher$translucencypointofview, pCameraPosition, flag, false);
            }

            this.lastTranslucentSortBlockPos = blockpos;
            Profiler.get().pop();
        }
    }

    private void scheduleResort(
        SectionRenderDispatcher.RenderSection pSection,
        SectionRenderDispatcher.TranslucencyPointOfView pPointOfView,
        Vec3 pCameraPosition,
        boolean pForce,
        boolean pIgnoreAxisAlignment
    ) {
        pPointOfView.set(pCameraPosition, pSection.getSectionNode());
        boolean flag = !pPointOfView.equals(pSection.pointOfView.get());
        boolean flag1 = pForce && (pPointOfView.isAxisAligned() || pIgnoreAxisAlignment);
        if ((flag1 || flag) && !pSection.transparencyResortingScheduled() && pSection.hasTranslucentGeometry()) {
            pSection.resortTransparency(this.sectionRenderDispatcher);
        }
    }

    public HashMap<BlockPos, ChunkAnimation> chunkMap = new LinkedHashMap<>();

    static class ChunkAnimation {

        public Animation animation;
        public float yOffset;

        public ChunkAnimation() {
            animation = new Animation();
            yOffset = Objects.equals(FunctionsManager.getFunctionByName("Chunks appearance animation").getSettingByName("Type").getStringValue(), "Up") ? -8 : 8;
        }

        public void animate() {
            yOffset = animation.interpolate(yOffset, 0, 350d - 300 * ((FunctionsManager.getFunctionByName("Chunks appearance animation").getSettingByName("Speed").getFloatValue() - 1)));
        }

    }

    public void renderSectionLayer(RenderType pRenderType, double pX, double pY, double pZ, Matrix4f pFrustrumMatrix, Matrix4f pProjectionMatrix) {
        RenderSystem.assertOnRenderThread();
        Zone zone = Profiler.get().zone(() -> "render_" + pRenderType.name);
        zone.addText(pRenderType::toString);
        boolean flag = pRenderType != RenderType.translucent();
        boolean flag1 = Config.isShaders();
        ObjectListIterator<SectionRenderDispatcher.RenderSection> objectlistiterator = this.renderInfosTerrain
            .listIterator(flag ? 0 : this.renderInfosTerrain.size());
        pRenderType.setupRenderState();
        CompiledShaderProgram compiledshaderprogram = RenderSystem.getShader();
        if (compiledshaderprogram == null) {
            pRenderType.clearRenderState();
            zone.close();
        } else {
            compiledshaderprogram.setDefaultUniformsFog(VertexFormat.Mode.QUADS, pFrustrumMatrix, pProjectionMatrix, this.minecraft.getWindow());
            compiledshaderprogram.apply();
            Uniform uniform = compiledshaderprogram.MODEL_OFFSET;
            if (flag1) {
                ShadersRender.preRenderChunkLayer(pRenderType);
                Shaders.setModelViewMatrix(pFrustrumMatrix);
                Shaders.setProjectionMatrix(pProjectionMatrix);
                Shaders.setTextureMatrix(RenderSystem.getTextureMatrix());
                Shaders.setColorModulator(RenderSystem.getShaderColor());
            }

            boolean flag2 = SmartAnimations.isActive();
            if (flag1 && Shaders.activeProgramID > 0) {
                uniform = null;
            }

            if (Config.isRenderRegions() && !pRenderType.isNeedsSorting()) {
                int i = Integer.MIN_VALUE;
                int j = Integer.MIN_VALUE;
                VboRegion vboregion2 = null;
                Map<PairInt, Map<VboRegion, List<VertexBuffer>>> map2 = (Map<PairInt, Map<VboRegion, List<VertexBuffer>>>)this.mapRegionLayers
                    .computeIfAbsent(pRenderType, k -> new LinkedHashMap(16));
                Map<VboRegion, List<VertexBuffer>> map = null;
                List<VertexBuffer> list = null;

                while (flag ? objectlistiterator.hasNext() : objectlistiterator.hasPrevious()) {
                    SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = flag
                        ? objectlistiterator.next()
                        : objectlistiterator.previous();
                    if (!sectionrenderdispatcher$rendersection1.getCompiled().isEmpty(pRenderType)) {
                        VertexBuffer vertexbuffer1 = sectionrenderdispatcher$rendersection1.getBuffer(pRenderType);
                        VboRegion vboregion = vertexbuffer1.getVboRegion();
                        if (sectionrenderdispatcher$rendersection1.regionX != i || sectionrenderdispatcher$rendersection1.regionZ != j) {
                            PairInt pairint = PairInt.of(sectionrenderdispatcher$rendersection1.regionX, sectionrenderdispatcher$rendersection1.regionZ);
                            map = map2.computeIfAbsent(pairint, k -> new LinkedHashMap<>(8));
                            i = sectionrenderdispatcher$rendersection1.regionX;
                            j = sectionrenderdispatcher$rendersection1.regionZ;
                            vboregion2 = null;
                        }

                        if (vboregion != vboregion2) {
                            list = map.computeIfAbsent(vboregion, k -> new ArrayList<>());
                            vboregion2 = vboregion;
                        }

                        list.add(vertexbuffer1);
                        if (flag2) {
                            BitSet bitset1 = sectionrenderdispatcher$rendersection1.getCompiled().getAnimatedSprites(pRenderType);
                            if (bitset1 != null) {
                                SmartAnimations.spritesRendered(bitset1);
                            }
                        }
                    }
                }

                for (java.util.Map.Entry<PairInt, Map<VboRegion, List<VertexBuffer>>> entry1 : map2.entrySet()) {
                    PairInt pairint1 = entry1.getKey();
                    Map<VboRegion, List<VertexBuffer>> map1 = entry1.getValue();

                    for (java.util.Map.Entry<VboRegion, List<VertexBuffer>> entry : map1.entrySet()) {
                        VboRegion vboregion1 = entry.getKey();
                        List<VertexBuffer> list1 = entry.getValue();
                        if (!list1.isEmpty()) {
                            for (VertexBuffer vertexbuffer2 : list1) {
                                vertexbuffer2.draw();
                            }

                            this.drawRegion(pairint1.getLeft(), 0, pairint1.getRight(), pX, pY, pZ, vboregion1, uniform, flag1);
                            list1.clear();
                        }
                    }
                }
            } else {
                ArrayList<BlockPos> usedChunks = new ArrayList<>();
                while (flag ? objectlistiterator.hasNext() : objectlistiterator.hasPrevious()) {
                    SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = flag
                        ? objectlistiterator.next()
                        : objectlistiterator.previous();
                    if (!sectionrenderdispatcher$rendersection.getCompiled().isEmpty(pRenderType)) {
                        VertexBuffer vertexbuffer = sectionrenderdispatcher$rendersection.getBuffer(pRenderType);
                        BlockPos blockpos = sectionrenderdispatcher$rendersection.getOrigin();

                        if(chunkMap.containsKey(blockpos))
                            chunkMap.get(blockpos).animate();
                        else chunkMap.put(blockpos, new ChunkAnimation());

                        if (uniform != null) {
                            uniform.set(
                                (float)((double)blockpos.getX() - pX - (double)sectionrenderdispatcher$rendersection.regionDX),
                                (float)((double)blockpos.getY() - pY - (double)sectionrenderdispatcher$rendersection.regionDY) + (FunctionsManager.getFunctionByName("Chunks appearance animation").isActivated() ? chunkMap.get(blockpos).yOffset : 0),
                                (float)((double)blockpos.getZ() - pZ - (double)sectionrenderdispatcher$rendersection.regionDZ)
                            );
                            uniform.upload();
                        }

                        if (flag1) {
                            Shaders.uniform_modelOffset
                                .setValue(
                                    (float)((double)blockpos.getX() - pX - (double)sectionrenderdispatcher$rendersection.regionDX),
                                    (float)((double)blockpos.getY() - pY - (double)sectionrenderdispatcher$rendersection.regionDY) + (FunctionsManager.getFunctionByName("Chunks appearance animation").isActivated() ? chunkMap.get(blockpos).yOffset : 0),
                                    (float)((double)blockpos.getZ() - pZ - (double)sectionrenderdispatcher$rendersection.regionDZ)
                                );
                        }

                        if (flag2) {
                            BitSet bitset = sectionrenderdispatcher$rendersection.getCompiled().getAnimatedSprites(pRenderType);
                            if (bitset != null) {
                                SmartAnimations.spritesRendered(bitset);
                            }
                        }

                        vertexbuffer.bind();
                        vertexbuffer.draw();
                    }
                }

                ArrayList<BlockPos> unusedChunks = new ArrayList<>();

                for(Map.Entry<BlockPos, ChunkAnimation> chunk : chunkMap.entrySet()) {

                    Vec2 cameraPos = new Vec2(
                            (float) Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition().x,

                            (float) Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition().z
                    );

                    Vec2 chunkPos = new Vec2(chunk.getKey().getX(), chunk.getKey().getZ());

                    double distance = Math.sqrt(chunkPos.distanceToSqr(cameraPos));

                    if(
                            distance > Minecraft.getInstance().options.renderDistance().get() * 16 + 33
                    )
                        unusedChunks.add(chunk.getKey());

                }

                for(BlockPos unusedChunk : unusedChunks)
                    chunkMap.remove(unusedChunk);

            }

            if (Config.isMultiTexture()) {
                this.minecraft.getTextureManager().bindTexture(TextureAtlas.LOCATION_BLOCKS);
            }

            if (uniform != null) {
                uniform.set(0.0F, 0.0F, 0.0F);
            }

            if (flag1) {
                Shaders.uniform_modelOffset.setValue(0.0F, 0.0F, 0.0F);
            }

            compiledshaderprogram.clear();
            VertexBuffer.unbind();
            zone.close();
            if (flag1) {
                ShadersRender.postRenderChunkLayer(pRenderType);
            }

            pRenderType.clearRenderState();
        }
    }

    private void drawRegion(int regionX, int regionY, int regionZ, double xIn, double yIn, double zIn, VboRegion vboRegion, Uniform uniform, boolean isShaders) {
        if (uniform != null) {
            uniform.set((float)((double)regionX - xIn), (float)((double)regionY - yIn), (float)((double)regionZ - zIn));
            uniform.upload();
        }

        if (isShaders) {
            Shaders.uniform_modelOffset.setValue((float)((double)regionX - xIn), (float)((double)regionY - yIn), (float)((double)regionZ - zIn));
        }

        vboRegion.finishDraw();
    }

    public void captureFrustum() {
        this.captureFrustum = true;
    }

    public void killFrustum() {
        this.capturedFrustum = null;
    }

    public void tick() {
        if (this.level.tickRateManager().runsNormally()) {
            this.ticks++;
        }

        if (this.ticks % 20 == 0) {
            Iterator<BlockDestructionProgress> iterator = this.destroyingBlocks.values().iterator();

            while (iterator.hasNext()) {
                BlockDestructionProgress blockdestructionprogress = iterator.next();
                int i = blockdestructionprogress.getUpdatedRenderTick();
                if (this.ticks - i > 400) {
                    iterator.remove();
                    this.removeProgress(blockdestructionprogress);
                }
            }
        }

        if (Config.isRenderRegions() && this.ticks % 20 == 0) {
            this.mapRegionLayers.clear();
        }
    }

    private void removeProgress(BlockDestructionProgress pProgress) {
        long i = pProgress.getPos().asLong();
        Set<BlockDestructionProgress> set = this.destructionProgress.get(i);
        set.remove(pProgress);
        if (set.isEmpty()) {
            this.destructionProgress.remove(i);
        }
    }

    private void addSkyPass(FrameGraphBuilder pFrameGraphBuilder, Camera pCamera, float pPartialTick, FogParameters pFog) {
        FogType fogtype = pCamera.getFluidInCamera();
        if (fogtype != FogType.POWDER_SNOW && fogtype != FogType.LAVA && !this.doesMobEffectBlockSky(pCamera)) {
            DimensionSpecialEffects dimensionspecialeffects = this.level.effects();
            DimensionSpecialEffects.SkyType dimensionspecialeffects$skytype = dimensionspecialeffects.skyType();
            if (dimensionspecialeffects$skytype != DimensionSpecialEffects.SkyType.NONE) {
                FramePass framepass = pFrameGraphBuilder.addPass("sky");
                this.targets.main = framepass.readsAndWrites(this.targets.main);
                framepass.executes(
                    () -> {
                        boolean flag = Config.isShaders();
                        if (flag) {
                            Shaders.beginSky();
                        }

                        RenderSystem.setShaderFog(pFog);
                        this.skyRenderer.setRenderParameters(this.level, pPartialTick);
                        if (dimensionspecialeffects$skytype == DimensionSpecialEffects.SkyType.END) {
                            this.skyRenderer.renderEndSky();
                        } else {
                            if (flag) {
                                Shaders.disableTexture2D();
                            }

                            PoseStack posestack = new PoseStack();
                            float f = this.level.getSunAngle(pPartialTick);
                            float f1 = this.level.getTimeOfDay(pPartialTick);
                            float f2 = 1.0F - this.level.getRainLevel(pPartialTick);
                            float f3 = this.level.getStarBrightness(pPartialTick) * f2;
                            int i = dimensionspecialeffects.getSunriseOrSunsetColor(f1);
                            int j = this.level.getMoonPhase();
                            int k = this.level.getSkyColor(this.minecraft.gameRenderer.getMainCamera().getPosition(), pPartialTick);
                            k = CustomColors.getSkyColor(
                                k,
                                this.minecraft.level,
                                this.minecraft.getCameraEntity().getX(),
                                this.minecraft.getCameraEntity().getY() + 1.0,
                                this.minecraft.getCameraEntity().getZ()
                            );
                            float f4 = ARGB.redFloat(k);
                            float f5 = ARGB.greenFloat(k);
                            float f6 = ARGB.blueFloat(k);
                            if (flag) {
                                Shaders.setSkyColor(f4, f5, f6);
                                RenderSystem.setColorToAttribute(true);
                                Shaders.enableFog();
                                Shaders.preSkyList(posestack);
                            }

                            if (Config.isSkyEnabled()) {
                                this.skyRenderer.renderSkyDisc(f4, f5, f6);
                            }

                            if (flag) {
                                Shaders.disableFog();
                            }

                            MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
                            if (dimensionspecialeffects.isSunriseOrSunset(f1) && Config.isSunMoonEnabled()) {
                                if (flag) {
                                    Shaders.disableTexture2D();
                                }

                                if (flag) {
                                    Shaders.setRenderStage(RenderStage.SUNSET);
                                }

                                this.skyRenderer.renderSunriseAndSunset(posestack, multibuffersource$buffersource, f, i);
                            }

                            if (flag) {
                                Shaders.enableTexture2D();
                            }

                            if (CustomSky.isActive()) {
                                multibuffersource$buffersource.flushRenderBuffers();
                            }

                            this.skyRenderer.renderSunMoonAndStars(posestack, multibuffersource$buffersource, f1, j, f2, f3, pFog);
                            multibuffersource$buffersource.endBatch();
                            if (flag) {
                                Shaders.disableTexture2D();
                            }

                            if (this.shouldRenderDarkDisc(pPartialTick)) {
                                if (flag) {
                                    Shaders.setRenderStage(RenderStage.VOID);
                                }

                                this.skyRenderer.renderDarkDisc(posestack);
                            }

                            if (flag) {
                                RenderSystem.setColorToAttribute(false);
                            }
                        }

                        if (flag) {
                            Shaders.endSky();
                        }
                    }
                );
            }
        }
    }

    private boolean shouldRenderDarkDisc(float pPartialTick) {
        return this.minecraft.player.getEyePosition(pPartialTick).y - this.level.getLevelData().getHorizonHeight(this.level) < 0.0;
    }

    private boolean doesMobEffectBlockSky(Camera pCamera) {
        return pCamera.getEntity() instanceof LivingEntity livingentity
            ? livingentity.hasEffect(MobEffects.BLINDNESS) || livingentity.hasEffect(MobEffects.DARKNESS)
            : false;
    }

    private void compileSections(Camera pCamera) {
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("populate_sections_to_compile");
        RenderRegionCache renderregioncache = new RenderRegionCache();
        renderregioncache.compileStarted();
        BlockPos blockpos = pCamera.getBlockPosition();
        List<SectionRenderDispatcher.RenderSection> list = Lists.newArrayList();
        Lagometer.timerChunkUpdate.start();

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.visibleSections) {
            if (sectionrenderdispatcher$rendersection.isDirty() && sectionrenderdispatcher$rendersection.hasAllNeighbors()) {
                if (sectionrenderdispatcher$rendersection.needsBackgroundPriorityUpdate()) {
                    list.add(sectionrenderdispatcher$rendersection);
                } else {
                    boolean flag = false;
                    if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.NEARBY) {
                        BlockPos blockpos1 = sectionrenderdispatcher$rendersection.getOrigin().offset(8, 8, 8);
                        flag = blockpos1.distSqr(blockpos) < 768.0 || sectionrenderdispatcher$rendersection.isDirtyFromPlayer();
                    } else if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.PLAYER_AFFECTED) {
                        flag = sectionrenderdispatcher$rendersection.isDirtyFromPlayer();
                    }

                    if (flag) {
                        profilerfiller.push("build_near_sync");
                        this.sectionRenderDispatcher.rebuildSectionSync(sectionrenderdispatcher$rendersection, renderregioncache);
                        sectionrenderdispatcher$rendersection.setNotDirty();
                        profilerfiller.pop();
                    } else {
                        list.add(sectionrenderdispatcher$rendersection);
                    }
                }
            }
        }

        Lagometer.timerChunkUpdate.end();
        Lagometer.timerChunkUpload.start();
        profilerfiller.popPush("upload");
        this.sectionRenderDispatcher.uploadAllPendingUploads();
        this.viewArea.clearUnusedVbos();
        profilerfiller.popPush("schedule_async_compile");
        if (this.chunksToResortTransparency.size() > 0) {
            Iterator<SectionRenderDispatcher.RenderSection> iterator = this.chunksToResortTransparency.iterator();
            if (iterator.hasNext()) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection2 = iterator.next();
                if (this.sectionRenderDispatcher.updateTransparencyLater(sectionrenderdispatcher$rendersection2)) {
                    iterator.remove();
                }
            }
        }

        double d1 = 0.0;
        int i = Config.getUpdatesPerFrame();
        this.countChunksToUpdate = list.size();

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 : SectionUtils.getSortedDist(list, this.minecraft.player)) {
            boolean flag1 = sectionrenderdispatcher$rendersection1.isChunkRegionEmpty();
            boolean flag2 = sectionrenderdispatcher$rendersection1.needsBackgroundPriorityUpdate();
            if (sectionrenderdispatcher$rendersection1.isDirty()) {
                sectionrenderdispatcher$rendersection1.rebuildSectionAsync(this.sectionRenderDispatcher, renderregioncache);
                sectionrenderdispatcher$rendersection1.setNotDirty();
                if (!flag1 && !flag2) {
                    double d0 = 2.0 * RenderChunkUtils.getRelativeBufferSize(sectionrenderdispatcher$rendersection1);
                    d1 += d0;
                    if (d1 > (double)i) {
                        break;
                    }
                }
            }
        }

        renderregioncache.compileFinished();
        Lagometer.timerChunkUpload.end();
        profilerfiller.pop();
        this.scheduleTranslucentSectionResort(pCamera.getPosition());
    }

    private void renderHitOutline(
        PoseStack pPoseStack,
        VertexConsumer pBuffer,
        Entity pEntity,
        double pCamX,
        double pCamY,
        double pCamZ,
        BlockPos pPos,
        BlockState pState,
        int pColor
    ) {
        if (!Config.isCustomEntityModels() || !CustomEntityModels.isCustomModel(pState)) {
            ShapeRenderer.renderShape(
                pPoseStack,
                pBuffer,
                pState.getShape(this.level, pPos, CollisionContext.of(pEntity)),
                (double)pPos.getX() - pCamX,
                (double)pPos.getY() - pCamY,
                (double)pPos.getZ() - pCamZ,
                pColor
            );
        }
    }

    public void blockChanged(BlockGetter pLevel, BlockPos pPos, BlockState pOldState, BlockState pNewState, int pFlags) {
        this.setBlockDirty(pPos, (pFlags & 8) != 0);
    }

    private void setBlockDirty(BlockPos pPos, boolean pReRenderOnMainThread) {
        for (int i = pPos.getZ() - 1; i <= pPos.getZ() + 1; i++) {
            for (int j = pPos.getX() - 1; j <= pPos.getX() + 1; j++) {
                for (int k = pPos.getY() - 1; k <= pPos.getY() + 1; k++) {
                    this.setSectionDirty(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k), SectionPos.blockToSectionCoord(i), pReRenderOnMainThread);
                }
            }
        }
    }

    public void setBlocksDirty(int pMinX, int pMinY, int pMinZ, int pMaxX, int pMaxY, int pMaxZ) {
        for (int i = pMinZ - 1; i <= pMaxZ + 1; i++) {
            for (int j = pMinX - 1; j <= pMaxX + 1; j++) {
                for (int k = pMinY - 1; k <= pMaxY + 1; k++) {
                    this.setSectionDirty(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k), SectionPos.blockToSectionCoord(i));
                }
            }
        }
    }

    public void setBlockDirty(BlockPos pPos, BlockState pOldState, BlockState pNewState) {
        if (this.minecraft.getModelManager().requiresRender(pOldState, pNewState)) {
            this.setBlocksDirty(
                pPos.getX(), pPos.getY(), pPos.getZ(), pPos.getX(), pPos.getY(), pPos.getZ()
            );
        }
    }

    public void setSectionDirtyWithNeighbors(int pSectionX, int pSectionY, int pSectionZ) {
        this.setSectionRangeDirty(pSectionX - 1, pSectionY - 1, pSectionZ - 1, pSectionX + 1, pSectionY + 1, pSectionZ + 1);
    }

    public void setSectionRangeDirty(int pMinY, int pMinX, int pMinZ, int pMaxY, int pMaxX, int pMaxZ) {
        for (int i = pMinZ; i <= pMaxZ; i++) {
            for (int j = pMinY; j <= pMaxY; j++) {
                for (int k = pMinX; k <= pMaxX; k++) {
                    this.setSectionDirty(j, k, i);
                }
            }
        }
    }

    public void setSectionDirty(int pSectionX, int pSectionY, int pSectionZ) {
        this.setSectionDirty(pSectionX, pSectionY, pSectionZ, false);
    }

    private void setSectionDirty(int pSectionX, int pSectionY, int pSectionZ, boolean pReRenderOnMainThread) {
        this.viewArea.setDirty(pSectionX, pSectionY, pSectionZ, pReRenderOnMainThread);
    }

    public void onSectionBecomingNonEmpty(long pSectionPos) {
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSection(pSectionPos);
        if (sectionrenderdispatcher$rendersection != null) {
            this.sectionOcclusionGraph.schedulePropagationFrom(sectionrenderdispatcher$rendersection);
        }
    }

    public void addParticle(
        ParticleOptions pOptions,
        boolean pForce,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed
    ) {
        this.addParticle(pOptions, pForce, false, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    public void addParticle(
        ParticleOptions pOptions,
        boolean pForce,
        boolean pDecreased,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed
    ) {
        try {
            this.addParticleInternal(pOptions, pForce, pDecreased, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception while adding particle");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being added");
            crashreportcategory.setDetail("ID", BuiltInRegistries.PARTICLE_TYPE.getKey(pOptions.getType()));
            crashreportcategory.setDetail(
                "Parameters", () -> ParticleTypes.CODEC.encodeStart(this.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), pOptions).toString()
            );
            crashreportcategory.setDetail("Position", () -> CrashReportCategory.formatLocation(this.level, pX, pY, pZ));
            throw new ReportedException(crashreport);
        }
    }

    public <T extends ParticleOptions> void addParticle(
        T pOptions, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
    ) {
        this.addParticle(pOptions, pOptions.getType().getOverrideLimiter(), pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    @Nullable
    Particle addParticleInternal(
        ParticleOptions pOptions,
        boolean pForce,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed
    ) {
        return this.addParticleInternal(pOptions, pForce, false, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    @Nullable
    private Particle addParticleInternal(
        ParticleOptions pOptions,
        boolean pForce,
        boolean pDecreased,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed
    ) {
        Camera camera = this.minecraft.gameRenderer.getMainCamera();
        ParticleStatus particlestatus = this.calculateParticleLevel(pDecreased);
        if (pOptions == ParticleTypes.EXPLOSION_EMITTER && !Config.isAnimatedExplosion()) {
            return null;
        } else if (pOptions == ParticleTypes.EXPLOSION && !Config.isAnimatedExplosion()) {
            return null;
        } else if (pOptions == ParticleTypes.POOF && !Config.isAnimatedExplosion()) {
            return null;
        } else if (pOptions == ParticleTypes.UNDERWATER && !Config.isWaterParticles()) {
            return null;
        } else if (pOptions == ParticleTypes.SMOKE && !Config.isAnimatedSmoke()) {
            return null;
        } else if (pOptions == ParticleTypes.LARGE_SMOKE && !Config.isAnimatedSmoke()) {
            return null;
        } else if (pOptions == ParticleTypes.ENTITY_EFFECT && !Config.isPotionParticles()) {
            return null;
        } else if (pOptions == ParticleTypes.EFFECT && !Config.isPotionParticles()) {
            return null;
        } else if (pOptions == ParticleTypes.INSTANT_EFFECT && !Config.isPotionParticles()) {
            return null;
        } else if (pOptions == ParticleTypes.WITCH && !Config.isPotionParticles()) {
            return null;
        } else if (pOptions == ParticleTypes.PORTAL && !Config.isPortalParticles()) {
            return null;
        } else if (pOptions == ParticleTypes.FLAME && !Config.isAnimatedFlame()) {
            return null;
        } else if (pOptions == ParticleTypes.SOUL_FIRE_FLAME && !Config.isAnimatedFlame()) {
            return null;
        } else if (pOptions == ParticleTypes.DUST && !Config.isAnimatedRedstone()) {
            return null;
        } else if (pOptions == ParticleTypes.DRIPPING_WATER && !Config.isDrippingWaterLava()) {
            return null;
        } else if (pOptions == ParticleTypes.DRIPPING_LAVA && !Config.isDrippingWaterLava()) {
            return null;
        } else if (pOptions == ParticleTypes.FIREWORK && !Config.isFireworkParticles()) {
            return null;
        } else {
            if (!pForce) {
                double d0 = 1024.0;
                if (pOptions == ParticleTypes.CRIT) {
                    d0 = 38416.0;
                }

                if (camera.getPosition().distanceToSqr(pX, pY, pZ) > d0) {
                    return null;
                }

                if (particlestatus == ParticleStatus.MINIMAL) {
                    return null;
                }
            }

            Particle particle = this.minecraft.particleEngine.createParticle(pOptions, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
            if (pOptions == ParticleTypes.BUBBLE) {
                CustomColors.updateWaterFX(particle, this.level, pX, pY, pZ, this.renderEnv);
            }

            if (pOptions == ParticleTypes.SPLASH) {
                CustomColors.updateWaterFX(particle, this.level, pX, pY, pZ, this.renderEnv);
            }

            if (pOptions == ParticleTypes.RAIN) {
                CustomColors.updateWaterFX(particle, this.level, pX, pY, pZ, this.renderEnv);
            }

            if (pOptions == ParticleTypes.MYCELIUM) {
                CustomColors.updateMyceliumFX(particle);
            }

            if (pOptions == ParticleTypes.PORTAL) {
                CustomColors.updatePortalFX(particle);
            }

            if (pOptions == ParticleTypes.DUST) {
                CustomColors.updateReddustFX(particle, this.level, pX, pY, pZ);
            }

            if (pOptions == ParticleTypes.LAVA) {
                CustomColors.updateLavaFX(particle);
            }

            return particle;
        }
    }

    private ParticleStatus calculateParticleLevel(boolean pDecreased) {
        ParticleStatus particlestatus = this.minecraft.options.particles().get();
        if (pDecreased && particlestatus == ParticleStatus.MINIMAL && this.level.random.nextInt(10) == 0) {
            particlestatus = ParticleStatus.DECREASED;
        }

        if (particlestatus == ParticleStatus.DECREASED && this.level.random.nextInt(3) == 0) {
            particlestatus = ParticleStatus.MINIMAL;
        }

        return particlestatus;
    }

    public void destroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress) {
        if (pProgress >= 0 && pProgress < 10) {
            BlockDestructionProgress blockdestructionprogress1 = this.destroyingBlocks.get(pBreakerId);
            if (blockdestructionprogress1 != null) {
                this.removeProgress(blockdestructionprogress1);
            }

            if (blockdestructionprogress1 == null
                || blockdestructionprogress1.getPos().getX() != pPos.getX()
                || blockdestructionprogress1.getPos().getY() != pPos.getY()
                || blockdestructionprogress1.getPos().getZ() != pPos.getZ()) {
                blockdestructionprogress1 = new BlockDestructionProgress(pBreakerId, pPos);
                this.destroyingBlocks.put(pBreakerId, blockdestructionprogress1);
            }

            blockdestructionprogress1.setProgress(pProgress);
            blockdestructionprogress1.updateTick(this.ticks);
            this.destructionProgress.computeIfAbsent(blockdestructionprogress1.getPos().asLong(), keyIn -> Sets.newTreeSet()).add(blockdestructionprogress1);
        } else {
            BlockDestructionProgress blockdestructionprogress = this.destroyingBlocks.remove(pBreakerId);
            if (blockdestructionprogress != null) {
                this.removeProgress(blockdestructionprogress);
            }
        }
    }

    public boolean hasRenderedAllSections() {
        return this.sectionRenderDispatcher.isQueueEmpty();
    }

    public void onChunkReadyToRender(ChunkPos pChunkPos) {
        this.sectionOcclusionGraph.onChunkReadyToRender(pChunkPos);
    }

    public void needsUpdate() {
        this.sectionOcclusionGraph.invalidate();
        this.cloudRenderer.markForRebuild();
    }

    public int getCountRenderers() {
        return this.viewArea.sections.length;
    }

    public int getCountEntitiesRendered() {
        return this.visibleEntityCount;
    }

    public int getCountTileEntitiesRendered() {
        return this.countTileEntitiesRendered;
    }

    public int getCountLoadedChunks() {
        if (this.level == null) {
            return 0;
        } else {
            ClientChunkCache clientchunkcache = this.level.getChunkSource();
            return clientchunkcache == null ? 0 : clientchunkcache.getLoadedChunksCount();
        }
    }

    public int getCountChunksToUpdate() {
        return this.countChunksToUpdate;
    }

    public SectionRenderDispatcher.RenderSection getRenderChunk(BlockPos pos) {
        return this.viewArea.getRenderSectionAt(pos);
    }

    public SectionRenderDispatcher.RenderSection getRenderChunk(long pos) {
        return this.viewArea.getRenderSection(pos);
    }

    public ClientLevel getWorld() {
        return this.level;
    }

    private void clearRenderInfos() {
        this.clearRenderInfosTerrain();
        this.clearRenderInfosEntities();
    }

    private void clearRenderInfosTerrain() {
        if (renderEntitiesCounter > 0) {
            this.renderInfosTerrain = new ObjectArrayList<>(this.renderInfosTerrain.size() + 16);
            this.renderInfosTileEntities = new ArrayList<>(this.renderInfosTileEntities.size() + 16);
        } else {
            this.renderInfosTerrain.clear();
            this.renderInfosTileEntities.clear();
        }
    }

    private void clearRenderInfosEntities() {
        if (renderEntitiesCounter > 0) {
            this.renderInfosEntities = new LongOpenHashSet(this.renderInfosEntities.size() + 16);
        } else {
            this.renderInfosEntities.clear();
        }
    }

    public void onPlayerPositionSet() {
        if (this.firstWorldLoad) {
            this.allChanged();
            this.firstWorldLoad = false;
        }
    }

    public void pauseChunkUpdates() {
        if (this.sectionRenderDispatcher != null) {
            this.sectionRenderDispatcher.pauseChunkUpdates();
        }
    }

    public void resumeChunkUpdates() {
        if (this.sectionRenderDispatcher != null) {
            this.sectionRenderDispatcher.resumeChunkUpdates();
        }
    }

    public int getFrameCount() {
        return this.frameId;
    }

    public RenderBuffers getRenderTypeTextures() {
        return this.renderBuffers;
    }

    public LongOpenHashSet getRenderChunksEntities() {
        return this.renderInfosEntities;
    }

    private void addEntitySection(LongOpenHashSet set, EntitySectionStorage storage, BlockPos pos) {
        long i = SectionPos.asLong(pos);
        EntitySection entitysection = storage.getSection(i);
        if (entitysection != null) {
            set.add(i);
        }
    }

    private boolean hasEntitySection(EntitySectionStorage storage, BlockPos pos) {
        long i = SectionPos.asLong(pos);
        EntitySection entitysection = storage.getSection(i);
        return entitysection != null;
    }

    public List<SectionRenderDispatcher.RenderSection> getRenderInfos() {
        return this.visibleSections;
    }

    public List<SectionRenderDispatcher.RenderSection> getRenderInfosTerrain() {
        return this.renderInfosTerrain;
    }

    public List<SectionRenderDispatcher.RenderSection> getRenderInfosTileEntities() {
        return this.renderInfosTileEntities;
    }

    private void checkLoadVisibleChunks(Camera activeRenderInfo, Frustum icamera, boolean spectator) {
        if (this.loadVisibleChunksCounter == 0) {
            this.loadAllVisibleChunks(activeRenderInfo, icamera, spectator);
            this.minecraft.gui.getChat().deleteMessage(loadVisibleChunksMessageId);
        }

        if (this.loadVisibleChunksCounter >= 0) {
            this.loadVisibleChunksCounter--;
        }
    }

    private void loadAllVisibleChunks(Camera activeRenderInfo, Frustum icamera, boolean spectator) {
        int i = this.minecraft.options.ofChunkUpdates;
        boolean flag = this.minecraft.options.ofLazyChunkLoading;

        try {
            this.minecraft.options.ofChunkUpdates = 1000;
            this.minecraft.options.ofLazyChunkLoading = false;
            LevelRenderer levelrenderer = Config.getRenderGlobal();
            int j = levelrenderer.getCountLoadedChunks();
            long k = System.currentTimeMillis();
            Config.dbg("Loading visible chunks");
            long l = System.currentTimeMillis() + 5000L;
            int i1 = 0;
            boolean flag1 = false;

            do {
                flag1 = false;

                for (int j1 = 0; j1 < 100; j1++) {
                    levelrenderer.needsUpdate();
                    levelrenderer.setupRender(activeRenderInfo, icamera, false, spectator);
                    Config.sleep(1L);
                    this.compileSections(activeRenderInfo);
                    if (levelrenderer.getCountChunksToUpdate() > 0) {
                        flag1 = true;
                    }

                    if (!levelrenderer.hasRenderedAllSections()) {
                        flag1 = true;
                    }

                    i1 += levelrenderer.getCountChunksToUpdate();

                    while (!levelrenderer.hasRenderedAllSections()) {
                        int k1 = levelrenderer.getCountChunksToUpdate();
                        this.compileSections(activeRenderInfo);
                        if (k1 == levelrenderer.getCountChunksToUpdate()) {
                            break;
                        }
                    }

                    i1 -= levelrenderer.getCountChunksToUpdate();
                    if (!flag1) {
                        break;
                    }
                }

                if (levelrenderer.getCountLoadedChunks() != j) {
                    flag1 = true;
                    j = levelrenderer.getCountLoadedChunks();
                }

                if (System.currentTimeMillis() > l) {
                    Config.log("Chunks loaded: " + i1);
                    l = System.currentTimeMillis() + 5000L;
                }
            } while (flag1);

            Config.log("Chunks loaded: " + i1);
            Config.log("Finished loading visible chunks");
            SectionRenderDispatcher.renderChunksUpdated = 0;
        } finally {
            this.minecraft.options.ofChunkUpdates = i;
            this.minecraft.options.ofLazyChunkLoading = flag;
        }
    }

    public void applyFrustumEntities(Frustum camera, int maxChunkDistance) {
        this.renderInfosEntities.clear();
        int i = (int)camera.getCameraX() >> 4 << 4;
        int j = (int)camera.getCameraY() >> 4 << 4;
        int k = (int)camera.getCameraZ() >> 4 << 4;
        int l = maxChunkDistance * maxChunkDistance;
        EntitySectionStorage<?> entitysectionstorage = this.level.getSectionStorage();
        BlockPosM blockposm = new BlockPosM();
        LongSet longset = entitysectionstorage.getSectionKeys();
        LongIterator longiterator = longset.iterator();

        while (longiterator.hasNext()) {
            long i1 = longiterator.nextLong();
            blockposm.setXyz(
                SectionPos.sectionToBlockCoord(SectionPos.x(i1)), SectionPos.sectionToBlockCoord(SectionPos.y(i1)), SectionPos.sectionToBlockCoord(SectionPos.z(i1))
            );
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSectionAt(blockposm);
            if (sectionrenderdispatcher$rendersection != null && camera.isVisible(sectionrenderdispatcher$rendersection.getBoundingBox())) {
                if (maxChunkDistance > 0) {
                    BlockPos blockpos = sectionrenderdispatcher$rendersection.getOrigin();
                    int j1 = i - blockpos.getX();
                    int k1 = j - blockpos.getY();
                    int l1 = k - blockpos.getZ();
                    int i2 = j1 * j1 + k1 * k1 + l1 * l1;
                    if (i2 > l) {
                        continue;
                    }
                }

                this.renderInfosEntities.add(i1);
            }
        }
    }

    public void setShadowRenderInfos(boolean shadowInfos) {
        if (shadowInfos) {
            this.renderInfosTerrain = this.renderInfosTerrainShadow;
            this.renderInfosEntities = this.renderInfosEntitiesShadow;
            this.renderInfosTileEntities = this.renderInfosTileEntitiesShadow;
        } else {
            this.renderInfosTerrain = this.renderInfosTerrainNormal;
            this.renderInfosEntities = this.renderInfosEntitiesNormal;
            this.renderInfosTileEntities = this.renderInfosTileEntitiesNormal;
        }
    }

    public int getRenderedChunksShadow() {
        return !Config.isShadersShadows() ? -1 : this.renderInfosTerrainShadow.size();
    }

    public int getCountEntitiesRenderedShadow() {
        return !Config.isShadersShadows() ? -1 : ShadersRender.countEntitiesRenderedShadow;
    }

    public int getCountTileEntitiesRenderedShadow() {
        if (!Config.isShaders()) {
            return -1;
        } else {
            return !Shaders.hasShadowMap ? -1 : ShadersRender.countTileEntitiesRenderedShadow;
        }
    }

    public void captureFrustumShadow() {
        this.debugFixTerrainFrustumShadow = true;
    }

    public boolean isDebugFrustum() {
        return this.capturedFrustum != null;
    }

    public void onChunkRenderNeedsUpdate(SectionRenderDispatcher.RenderSection renderChunk) {
        if (!renderChunk.getCompiled().hasTerrainBlockEntities()) {
            ;
        }
    }

    public boolean needsFrustumUpdate() {
        return this.sectionOcclusionGraph.needsFrustumUpdate();
    }

    public boolean shouldRenderEntity(Entity entity, int minWorldY, int maxWorldY) {
        if (entity instanceof Display) {
            return true;
        } else if (entity == this.minecraft.getCameraEntity()) {
            return true;
        } else {
            if (entity instanceof LivingEntity livingentity && livingentity.getScale() > 1.0F) {
                return true;
            }

            BlockPos blockpos = entity.blockPosition();
            return this.renderInfosEntities.contains(SectionPos.asLong(blockpos)) || blockpos.getY() <= minWorldY || blockpos.getY() >= maxWorldY;
        }
    }

    public Frustum getFrustum() {
        return this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum;
    }

    public int getTicks() {
        return this.ticks;
    }

    public WeatherEffectRenderer getWeatherEffects() {
        return this.weatherEffectRenderer;
    }

    public void setWeatherEffects(WeatherEffectRenderer value) {
        this.weatherEffectRenderer = value;
    }

    public void updateGlobalBlockEntities(Collection<BlockEntity> pBlockEntitiesToRemove, Collection<BlockEntity> pBlockEntitiesToAdd) {
        synchronized (this.globalBlockEntities) {
            this.globalBlockEntities.removeAll(pBlockEntitiesToRemove);
            this.globalBlockEntities.addAll(pBlockEntitiesToAdd);
        }
    }

    public static int getLightColor(BlockAndTintGetter pLevel, BlockPos pPos) {
        return getLightColor(pLevel, pLevel.getBlockState(pPos), pPos);
    }

    public static int getLightColor(BlockAndTintGetter pLevel, BlockState pState, BlockPos pPos) {
        return getPackedLightmapCoords(pLevel, pState, pPos, ambientOcclusion);
    }

    public static int getPackedLightmapCoords(BlockAndTintGetter lightReaderIn, BlockState blockStateIn, BlockPos blockPosIn, boolean ambientOcclusionIn) {
        if (EmissiveTextures.isRenderEmissive() && Config.isMinecraftThread()) {
            return LightTexture.MAX_BRIGHTNESS;
        } else if (blockStateIn.emissiveRendering(lightReaderIn, blockPosIn)) {
            return 15794417;
        } else {
            int i = lightReaderIn.getBrightness(LightLayer.SKY, blockPosIn);
            int j = lightReaderIn.getBrightness(LightLayer.BLOCK, blockPosIn);
            int k = blockStateIn.getLightValue(lightReaderIn, blockPosIn);
            if (j < k) {
                j = k;
            }

            int l = i << 20 | j << 4;
            if (Config.isDynamicLights() && lightReaderIn instanceof BlockGetter && (!ambientOcclusionIn || !blockStateIn.isSolidRender())) {
                l = DynamicLights.getCombinedLight(blockPosIn, l);
            }

            return l;
        }
    }

    public boolean isSectionCompiled(BlockPos pPos) {
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSectionAt(pPos);
        return sectionrenderdispatcher$rendersection != null
            && sectionrenderdispatcher$rendersection.compiled.get() != SectionRenderDispatcher.CompiledSection.UNCOMPILED;
    }

    @Nullable
    public RenderTarget entityOutlineTarget() {
        return this.targets.entityOutline != null ? this.targets.entityOutline.get() : null;
    }

    @Nullable
    public RenderTarget getTranslucentTarget() {
        return this.targets.translucent != null ? this.targets.translucent.get() : null;
    }

    @Nullable
    public RenderTarget getItemEntityTarget() {
        return this.targets.itemEntity != null ? this.targets.itemEntity.get() : null;
    }

    @Nullable
    public RenderTarget getParticlesTarget() {
        return this.targets.particles != null ? this.targets.particles.get() : null;
    }

    @Nullable
    public RenderTarget getWeatherTarget() {
        return this.targets.weather != null ? this.targets.weather.get() : null;
    }

    @Nullable
    public RenderTarget getCloudsTarget() {
        return this.targets.clouds != null ? this.targets.clouds.get() : null;
    }

    @VisibleForDebug
    public ObjectArrayList<SectionRenderDispatcher.RenderSection> getVisibleSections() {
        return this.visibleSections;
    }

    @VisibleForDebug
    public SectionOcclusionGraph getSectionOcclusionGraph() {
        return this.sectionOcclusionGraph;
    }

    @Nullable
    public Frustum getCapturedFrustum() {
        return this.capturedFrustum;
    }

    public CloudRenderer getCloudRenderer() {
        return this.cloudRenderer;
    }
}