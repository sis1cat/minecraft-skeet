package net.minecraft.client.renderer;

import com.darkmagician6.eventapi.EventManager;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import net.optifine.Config;
import net.optifine.GlErrors;
import net.optifine.Lagometer;
import net.optifine.RandomEntities;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.gui.GuiChatOF;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorResolver;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import net.optifine.util.MemoryMonitor;
import net.optifine.util.TimedEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import sisicat.IDefault;
import sisicat.events.GraphicsEvent;
import sisicat.events.World2DGraphics;
import sisicat.main.functions.FunctionsManager;
import sisicat.main.gui.elements.widgets.Widget;
import sisicat.main.utilities.Render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;

public class GameRenderer implements AutoCloseable {
    private static final ResourceLocation BLUR_POST_CHAIN_ID = ResourceLocation.withDefaultNamespace("blur");
    public static final int MAX_BLUR_RADIUS = 10;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEPTH_BUFFER_DEBUG = false;
    public static final float PROJECTION_Z_NEAR = 0.05F;
    private static final float GUI_Z_NEAR = 1000.0F;
    private final Minecraft minecraft;
    private final ResourceManager resourceManager;
    private final RandomSource random = RandomSource.create();
    private float renderDistance;
    public final ItemInHandRenderer itemInHandRenderer;
    public final RenderBuffers renderBuffers;
    private int confusionAnimationTick;
    private float fovModifier;
    private float oldFovModifier;
    private float darkenWorldAmount;
    private float darkenWorldAmountO;
    private boolean renderHand = true;
    private boolean renderBlockOutline = true;
    private long lastScreenshotAttempt;
    private boolean hasWorldScreenshot;
    private long lastActiveTime = Util.getMillis();
    private final LightTexture lightTexture;
    private final OverlayTexture overlayTexture = new OverlayTexture();
    private boolean panoramicMode;
    private float zoom = 1.0F;
    private float zoomX;
    private float zoomY;
    public static final int ITEM_ACTIVATION_ANIMATION_LENGTH = 40;
    @Nullable
    private ItemStack itemActivationItem;
    private int itemActivationTicks;
    private float itemActivationOffX;
    private float itemActivationOffY;
    private final CrossFrameResourcePool resourcePool = new CrossFrameResourcePool(3);
    @Nullable
    private ResourceLocation postEffectId;
    private boolean effectActive;
    private final Camera mainCamera = new Camera();
    private boolean initialized = false;
    private Level updatedWorld = null;
    private int frameCount = 0;
    private float clipDistance = 128.0F;
    private long lastServerTime = 0L;
    private int lastServerTicks = 0;
    private int serverWaitTime = 0;
    private int serverWaitTimeCurrent = 0;
    private float avgServerTimeDiff = 0.0F;
    private float avgServerTickDiff = 0.0F;
    private ResourceLocation[] fxaaShaders = new ResourceLocation[10];
    private boolean guiLoadingVisible = false;

    public GameRenderer(Minecraft pMinecraft, ItemInHandRenderer pItemInHandRenderer, ResourceManager pResourceManager, RenderBuffers pRenderBuffers) {
        this.minecraft = pMinecraft;
        this.resourceManager = pResourceManager;
        this.itemInHandRenderer = pItemInHandRenderer;
        this.lightTexture = new LightTexture(this, pMinecraft);
        this.renderBuffers = pRenderBuffers;
    }

    @Override
    public void close() {
        this.lightTexture.close();
        this.overlayTexture.close();
        this.resourcePool.close();
    }

    public void setRenderHand(boolean pRenderHand) {
        this.renderHand = pRenderHand;
    }

    public void setRenderBlockOutline(boolean pRenderBlockOutline) {
        this.renderBlockOutline = pRenderBlockOutline;
    }

    public void setPanoramicMode(boolean pPanoramicMode) {
        this.panoramicMode = pPanoramicMode;
    }

    public boolean isPanoramicMode() {
        return this.panoramicMode;
    }

    public void clearPostEffect() {
        this.postEffectId = null;
    }

    public void togglePostEffect() {
        this.effectActive = !this.effectActive;
    }

    public void checkEntityPostEffect(@Nullable Entity pEntity) {
        this.postEffectId = null;
        if (pEntity instanceof Creeper) {
            this.setPostEffect(ResourceLocation.withDefaultNamespace("creeper"));
        } else if (pEntity instanceof Spider) {
            this.setPostEffect(ResourceLocation.withDefaultNamespace("spider"));
        } else if (pEntity instanceof EnderMan) {
            this.setPostEffect(ResourceLocation.withDefaultNamespace("invert"));
        } else if (pEntity != null && Reflector.EntitySpectatorShaderManager_get.exists()) {
            ResourceLocation resourcelocation = (ResourceLocation)Reflector.EntitySpectatorShaderManager_get.call(pEntity.getType());
            if (resourcelocation != null) {
                this.setPostEffect(resourcelocation);
            }
        }
    }

    private void setPostEffect(ResourceLocation pPostEffectId) {
        if (Config.isMainFramebufferEnabled()) {
            this.postEffectId = pPostEffectId;
            this.effectActive = true;
        }
    }

    public void processBlurEffect() {
        if (Config.isMainFramebufferEnabled()) {
            RenderSystem.disableDepthTest();
            float f = (float)this.minecraft.options.getMenuBackgroundBlurriness();
            if (!(f < 1.0F)) {
                PostChain postchain = this.minecraft.getShaderManager().getPostChain(BLUR_POST_CHAIN_ID, LevelTargetBundle.MAIN_TARGETS);
                if (postchain != null) {
                    boolean flag = RenderSystem.isBlendEnabled();
                    if (flag) {
                        RenderSystem.disableBlend();
                    }

                    postchain.setUniform("Radius", f);
                    postchain.process(this.minecraft.getMainRenderTarget(), this.resourcePool);
                    if (flag) {
                        RenderSystem.enableBlend();
                    }
                }
            }
        }
    }

    public void preloadUiShader(ResourceProvider pResourceProvider) {
        try {
            this.minecraft.getShaderManager().preloadForStartup(pResourceProvider, CoreShaders.RENDERTYPE_GUI, CoreShaders.RENDERTYPE_GUI_OVERLAY, CoreShaders.POSITION_TEX_COLOR);
        } catch (IOException | ShaderManager.CompilationException shadermanager$compilationexception) {
            throw new RuntimeException("Could not preload shaders for loading UI", shadermanager$compilationexception);
        }
    }

    public void tick() {
        this.tickFov();
        this.lightTexture.tick();
        if (this.minecraft.getCameraEntity() == null) {
            this.minecraft.setCameraEntity(this.minecraft.player);
        }

        this.mainCamera.tick();
        this.itemInHandRenderer.tick();
        this.confusionAnimationTick++;
        if (this.minecraft.level.tickRateManager().runsNormally()) {
            this.minecraft.levelRenderer.tickParticles(this.mainCamera);
            this.darkenWorldAmountO = this.darkenWorldAmount;
            if (this.minecraft.gui.getBossOverlay().shouldDarkenScreen()) {
                this.darkenWorldAmount += 0.05F;
                if (this.darkenWorldAmount > 1.0F) {
                    this.darkenWorldAmount = 1.0F;
                }
            } else if (this.darkenWorldAmount > 0.0F) {
                this.darkenWorldAmount -= 0.0125F;
            }

            if (this.itemActivationTicks > 0) {
                this.itemActivationTicks--;
                if (this.itemActivationTicks == 0) {
                    this.itemActivationItem = null;
                }
            }
        }
    }

    @Nullable
    public ResourceLocation currentPostEffect() {
        return this.postEffectId;
    }

    public void resize(int pWidth, int pHeight) {
        this.resourcePool.clear();
        this.minecraft.levelRenderer.resize(pWidth, pHeight);
    }

    public void pick(float pPartialTicks) {
        Entity entity = this.minecraft.getCameraEntity();
        if (entity != null && this.minecraft.level != null && this.minecraft.player != null) {
            Profiler.get().push("pick");
            double d0 = this.minecraft.player.blockInteractionRange();
            double d1 = this.minecraft.player.entityInteractionRange();
            HitResult hitresult = this.pick(entity, d0, d1, pPartialTicks);
            this.minecraft.hitResult = hitresult;
            this.minecraft.crosshairPickEntity = hitresult instanceof EntityHitResult entityhitresult ? entityhitresult.getEntity() : null;
            Profiler.get().pop();
        }
    }

    private HitResult pick(Entity pEntity, double pBlockInteractionRange, double pEntityInteractionRange, float pPartialTick) {
        double d0 = Math.max(pBlockInteractionRange, pEntityInteractionRange);
        double d1 = Mth.square(d0);
        Vec3 vec3 = pEntity.getEyePosition(pPartialTick);
        HitResult hitresult = pEntity.pick(d0, pPartialTick, false);
        double d2 = hitresult.getLocation().distanceToSqr(vec3);
        if (hitresult.getType() != HitResult.Type.MISS) {
            d1 = d2;
            d0 = Math.sqrt(d2);
        }

        Vec3 vec31 = pEntity.getViewVector(pPartialTick);
        Vec3 vec32 = vec3.add(vec31.x * d0, vec31.y * d0, vec31.z * d0);
        float f = 1.0F;
        AABB aabb = pEntity.getBoundingBox().expandTowards(vec31.scale(d0)).inflate(1.0, 1.0, 1.0);
        EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(pEntity, vec3, vec32, aabb, EntitySelector.CAN_BE_PICKED, d1);
        return entityhitresult != null && entityhitresult.getLocation().distanceToSqr(vec3) < d2
            ? filterHitResult(entityhitresult, vec3, pEntityInteractionRange)
            : filterHitResult(hitresult, vec3, pBlockInteractionRange);
    }

    private static HitResult filterHitResult(HitResult pHitResult, Vec3 pPos, double pBlockInteractionRange) {
        Vec3 vec3 = pHitResult.getLocation();
        if (!vec3.closerThan(pPos, pBlockInteractionRange)) {
            Vec3 vec31 = pHitResult.getLocation();
            Direction direction = Direction.getApproximateNearest(
                vec31.x - pPos.x, vec31.y - pPos.y, vec31.z - pPos.z
            );
            return BlockHitResult.miss(vec31, direction, BlockPos.containing(vec31));
        } else {
            return pHitResult;
        }
    }

    private void tickFov() {
        float f;
        if (this.minecraft.getCameraEntity() instanceof AbstractClientPlayer abstractclientplayer) {
            Options options = this.minecraft.options;
            boolean flag = options.getCameraType().isFirstPerson();
            float f1 = options.fovEffectScale().get().floatValue();
            f = abstractclientplayer.getFieldOfViewModifier(flag, f1);
        } else {
            f = 1.0F;
        }

        this.oldFovModifier = this.fovModifier;
        this.fovModifier = this.fovModifier + (f - this.fovModifier) * 0.5F;
        this.fovModifier = Mth.clamp(this.fovModifier, 0.1F, 1.5F);
    }

    public float getFov(Camera pCamera, float pPartialTick, boolean pUseFovSetting) {
        if (this.panoramicMode) {
            return 90.0F;
        } else {
            float f = 70.0F;
            if (pUseFovSetting) {
                f = (float)this.minecraft.options.fov().get().intValue();
                boolean flag = this.minecraft.getCameraEntity() instanceof AbstractClientPlayer && ((AbstractClientPlayer)this.minecraft.getCameraEntity()).isScoping();
                if (Config.isDynamicFov() || flag) {
                    f *= Mth.lerp(pPartialTick, this.oldFovModifier, this.fovModifier);
                }
            }

            boolean flag1 = false;
            if (this.minecraft.screen == null) {
                flag1 = this.minecraft.options.ofKeyBindZoom.isDown();
            }

            if (flag1) {
                if (!Config.zoomMode) {
                    Config.zoomMode = true;
                    Config.zoomSmoothCamera = this.minecraft.options.smoothCamera;
                    this.minecraft.options.smoothCamera = true;
                    this.minecraft.levelRenderer.needsUpdate();
                }

                if (Config.zoomMode) {
                    f /= 4.0F;
                }
            } else if (Config.zoomMode) {
                Config.zoomMode = false;
                this.minecraft.options.smoothCamera = Config.zoomSmoothCamera;
                this.minecraft.levelRenderer.needsUpdate();
            }

            if (pCamera.getEntity() instanceof LivingEntity livingentity && livingentity.isDeadOrDying()) {
                float f1 = Math.min((float)livingentity.deathTime + pPartialTick, 20.0F);
                f /= (1.0F - 500.0F / (f1 + 500.0F)) * 2.0F + 1.0F;
            }

            FogType fogtype = pCamera.getFluidInCamera();
            if (fogtype == FogType.LAVA || fogtype == FogType.WATER) {
                float f2 = this.minecraft.options.fovEffectScale().get().floatValue();
                f *= Mth.lerp(f2, 1.0F, 0.85714287F);
            }

            if (Reflector.ForgeEventFactoryClient_fireComputeFov.exists()) {
                ViewportEvent.ComputeFov viewportevent$computefov = (ViewportEvent.ComputeFov)Reflector.ForgeEventFactoryClient_fireComputeFov
                    .call(this, pCamera, pPartialTick, f, pUseFovSetting);
                if (viewportevent$computefov != null) {
                    return viewportevent$computefov.getFOV();
                }
            }

            return Math.min(f, 150f);
        }
    }

    private void bobHurt(PoseStack pPoseStack, float pPartialTicks) {
        if (this.minecraft.getCameraEntity() instanceof LivingEntity livingentity && !FunctionsManager.getFunctionByName("Remove hurt effect").isActivated()) {
            float f2 = (float)livingentity.hurtTime - pPartialTicks;
            if (livingentity.isDeadOrDying()) {
                float f = Math.min((float)livingentity.deathTime + pPartialTicks, 20.0F);
                pPoseStack.mulPose(Axis.ZP.rotationDegrees(40.0F - 8000.0F / (f + 200.0F)));
            }

            if (f2 < 0.0F) {
                return;
            }

            f2 /= (float)livingentity.hurtDuration;
            f2 = Mth.sin(f2 * f2 * f2 * f2 * (float) Math.PI);
            float f3 = livingentity.getHurtDir();
            pPoseStack.mulPose(Axis.YP.rotationDegrees(-f3));
            float f1 = (float)((double)(-f2) * 14.0 * this.minecraft.options.damageTiltStrength().get());
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(f1));
            pPoseStack.mulPose(Axis.YP.rotationDegrees(f3));
        }
    }

    private void bobView(PoseStack pPoseStack, float pPartialTicks) {
        if (this.minecraft.getCameraEntity() instanceof AbstractClientPlayer abstractclientplayer) {
            float f2 = abstractclientplayer.walkDist - abstractclientplayer.walkDistO;
            float f = -(abstractclientplayer.walkDist + f2 * pPartialTicks);
            float f1 = Mth.lerp(pPartialTicks, abstractclientplayer.oBob, abstractclientplayer.bob);
            pPoseStack.translate(Mth.sin(f * (float) Math.PI) * f1 * 0.5F, -Math.abs(Mth.cos(f * (float) Math.PI) * f1), 0.0F);
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(f * (float) Math.PI) * f1 * 3.0F));
            pPoseStack.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(f * (float) Math.PI - 0.2F) * f1) * 5.0F));
        }
    }

    public void renderZoomed(float pZoom, float pZoomX, float pZoomY) {
        this.zoom = pZoom;
        this.zoomX = pZoomX;
        this.zoomY = pZoomY;
        this.setRenderBlockOutline(false);
        this.setRenderHand(false);
        this.renderLevel(DeltaTracker.ZERO);
        this.zoom = 1.0F;
    }

    private void renderItemInHand(Camera pCamera, float pPartialTick, Matrix4f pProjectionMatrix) {
        this.renderHand(pCamera, pPartialTick, pProjectionMatrix, true, true, false);
    }

    public void renderHand(
        Camera activeRenderInfoIn, float partialTicks, Matrix4f matrixStackIn, boolean renderItem, boolean renderOverlay, boolean renderTranslucent
    ) {
        if (!this.panoramicMode) {
            Shaders.beginRenderFirstPersonHand(renderTranslucent);
            Matrix4f matrix4f = this.getProjectionMatrix(this.getFov(activeRenderInfoIn, partialTicks, false));
            RenderSystem.setProjectionMatrix(matrix4f, ProjectionType.PERSPECTIVE);
            PoseStack posestack = new PoseStack();
            boolean flag = false;
            if (renderItem) {
                posestack.pushPose();
                posestack.mulPose(matrixStackIn.invert(new Matrix4f()));
                Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
                matrix4fstack.pushMatrix().set(matrixStackIn);
                this.bobHurt(posestack, partialTicks);
                if (this.minecraft.options.bobView().get()) {
                    this.bobView(posestack, partialTicks);
                }

                flag = this.minecraft.getCameraEntity() instanceof LivingEntity && ((LivingEntity)this.minecraft.getCameraEntity()).isSleeping();
                if (this.minecraft.options.getCameraType().isFirstPerson()
                    && !flag
                    && !this.minecraft.options.hideGui
                    && this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
                    this.lightTexture.turnOnLightLayer();
                    if (Config.isShaders()) {
                        ShadersRender.renderItemFP(
                            this.itemInHandRenderer,
                            partialTicks,
                            posestack,
                            this.renderBuffers.bufferSource(),
                            this.minecraft.player,
                            this.minecraft.getEntityRenderDispatcher().getPackedLightCoords(this.minecraft.player, partialTicks),
                            renderTranslucent
                        );
                    } else {
                        this.itemInHandRenderer
                            .renderHandsWithItems(
                                partialTicks,
                                posestack,
                                this.renderBuffers.bufferSource(),
                                this.minecraft.player,
                                this.minecraft.getEntityRenderDispatcher().getPackedLightCoords(this.minecraft.player, partialTicks)
                            );
                    }

                    this.lightTexture.turnOffLightLayer();
                }

                matrix4fstack.popMatrix();
                posestack.popPose();
            }

            Shaders.endRenderFirstPersonHand();
            if (!renderOverlay) {
                return;
            }

            this.lightTexture.turnOffLightLayer();
            if (this.minecraft.options.getCameraType().isFirstPerson() && !flag) {
                MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
                ScreenEffectRenderer.renderScreenEffect(this.minecraft, posestack, multibuffersource$buffersource);
                multibuffersource$buffersource.endBatch();
            }
        }
    }

    public Matrix4f getProjectionMatrix(float pFov) {
        Matrix4f matrix4f = new Matrix4f();
        if (Config.isShaders() && Shaders.isRenderingFirstPersonHand()) {
            Shaders.applyHandDepth(matrix4f);
        }

        this.clipDistance = this.renderDistance + 1024.0F;
        if (this.zoom != 1.0F) {
            matrix4f.translate(this.zoomX, -this.zoomY, 0.0F);
            matrix4f.scale(this.zoom, this.zoom, 1.0F);
        }

        return matrix4f.perspective(
            pFov * (float) (Math.PI / 180.0),
            (float)this.minecraft.getWindow().getWidth() / (float)this.minecraft.getWindow().getHeight(),
            0.05F,
            this.clipDistance
        );
    }

    public float getDepthFar() {
        return this.renderDistance * 4.0F;
    }

    public static float getNightVisionScale(LivingEntity pLivingEntity, float pNanoTime) {
        MobEffectInstance mobeffectinstance = pLivingEntity.getEffect(MobEffects.NIGHT_VISION);
        return !mobeffectinstance.endsWithin(200)
            ? 1.0F
            : 0.7F + Mth.sin(((float)mobeffectinstance.getDuration() - pNanoTime) * (float) Math.PI * 0.2F) * 0.3F;
    }

    private Screen lastScreen = new DummyScreen(Component.literal(""));
    private boolean lockLastScreen = false;

    private static class DummyScreen extends Screen {

        protected DummyScreen(Component pTitle) {
            super(pTitle);
        }
    }

    public void render(DeltaTracker pDeltaTracker, boolean pRenderLevel) {
        this.frameInit();
        if (!this.minecraft.isWindowActive()
            && this.minecraft.options.pauseOnLostFocus
            && (!this.minecraft.options.touchscreen().get() || !this.minecraft.mouseHandler.isRightPressed())) {
            if (Util.getMillis() - this.lastActiveTime > 500L) {
                this.minecraft.pauseGame(false);
            }
        } else {
            this.lastActiveTime = Util.getMillis();
        }

        if (!this.minecraft.noRender) {
            ProfilerFiller profilerfiller = Profiler.get();
            boolean flag = this.minecraft.isGameLoadFinished();
            int i = (int)(this.minecraft.mouseHandler.xpos() * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth());
            int j = (int)(this.minecraft.mouseHandler.ypos() * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight());
            if (flag && pRenderLevel && this.minecraft.level != null && !Config.isReloadingResources()) {
                profilerfiller.push("level");
                this.renderLevel(pDeltaTracker);
                this.tryTakeScreenshotIfNeeded();
                this.minecraft.levelRenderer.doEntityOutline();
                if (this.postEffectId != null && this.effectActive) {
                    RenderSystem.disableBlend();
                    RenderSystem.disableDepthTest();
                    RenderSystem.resetTextureMatrix();
                    PostChain postchain = this.minecraft.getShaderManager().getPostChain(this.postEffectId, LevelTargetBundle.MAIN_TARGETS);
                    if (postchain != null) {
                        postchain.process(this.minecraft.getMainRenderTarget(), this.resourcePool);
                    }

                    RenderSystem.enableTexture();
                }

                this.minecraft.getMainRenderTarget().bindWrite(true);
            } else {
                RenderSystem.viewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
            }
            EventManager.call(new World2DGraphics());
            Window window = this.minecraft.getWindow();
            RenderSystem.clear(256);
            float f = Reflector.ForgeHooksClient_getGuiFarPlane.exists() ? Reflector.ForgeHooksClient_getGuiFarPlane.callFloat() : 21000.0F;
            Matrix4f matrix4f = new Matrix4f()
                .setOrtho(
                    0.0F, (float)((double)window.getWidth() / window.getGuiScale()), (float)((double)window.getHeight() / window.getGuiScale()), 0.0F, 1000.0F, f
                );
            RenderSystem.setProjectionMatrix(matrix4f, ProjectionType.ORTHOGRAPHIC);
            Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
            matrix4fstack.pushMatrix();
            float f1 = Reflector.ForgeHooksClient_getGuiFarPlane.exists() ? 1000.0F - f : -11000.0F;
            matrix4fstack.translation(0.0F, 0.0F, f1);
            Lighting.setupFor3DItems();
            GuiGraphics guigraphics = new GuiGraphics(this.minecraft, this.renderBuffers.bufferSource());
            if (this.lightTexture.isCustom()) {
                this.lightTexture.setAllowed(false);
            }

            if (flag && pRenderLevel && this.minecraft.level != null) {
                profilerfiller.popPush("gui");
                if (!this.minecraft.options.hideGui) {
                    this.renderItemActivationAnimation(guigraphics, pDeltaTracker.getGameTimeDeltaPartialTick(false));
                }

                this.minecraft.gui.render(guigraphics, pDeltaTracker);
                guigraphics.flush();
                RenderSystem.clear(256);
                profilerfiller.pop();
            }

            if (this.guiLoadingVisible != (this.minecraft.getOverlay() != null)) {
                if (this.minecraft.getOverlay() != null) {
                    LoadingOverlay.registerTextures(this.minecraft.getTextureManager());
                    if (this.minecraft.getOverlay() instanceof LoadingOverlay) {
                        LoadingOverlay loadingoverlay = (LoadingOverlay)this.minecraft.getOverlay();
                        loadingoverlay.update();
                    }
                }

                this.guiLoadingVisible = this.minecraft.getOverlay() != null;
            }

            if(lastScreen instanceof ChatScreen chatScreen && FunctionsManager.getFunctionByName("Integrated UI animations").getSettingByName("Integrated UI animations").getSelectedOptionsList().contains("Smooth chat")) {

                lockLastScreen = true;

                if (!chatScreen.isAnimationPassed && chatScreen.closeAnimation) {
                    lastScreen.renderWithTooltip(guigraphics, i, j, pDeltaTracker.getGameTimeDeltaTicks());
                } else if (chatScreen.closeAnimation) {
                    lastScreen = new DummyScreen(Component.literal(""));
                    lockLastScreen = false;
                }

            }
            if (this.minecraft.getOverlay() != null) {
                try {
                    this.minecraft.getOverlay().render(guigraphics, i, j, pDeltaTracker.getGameTimeDeltaTicks());
                } catch (Throwable throwable1) {
                    CrashReport crashreport2 = CrashReport.forThrowable(throwable1, "Rendering overlay");
                    CrashReportCategory crashreportcategory2 = crashreport2.addCategory("Overlay render details");
                    crashreportcategory2.setDetail("Overlay name", () -> this.minecraft.getOverlay().getClass().getCanonicalName());
                    throw new ReportedException(crashreport2);
                }
            } else if (flag && this.minecraft.screen != null) {
                try {
                    if (Config.isCustomEntityModels()) {
                        CustomEntityModels.onRenderScreen(this.minecraft.screen);
                    }

                    if (Reflector.ForgeHooksClient_drawScreen.exists()) {
                        Reflector.callVoid(Reflector.ForgeHooksClient_drawScreen, this.minecraft.screen, guigraphics, i, j, pDeltaTracker.getRealtimeDeltaTicks());
                    } else {

                        this.minecraft.screen.renderWithTooltip(guigraphics, i, j, pDeltaTracker.getGameTimeDeltaTicks());

                        if(!lockLastScreen)
                            this.lastScreen = this.minecraft.screen;

                    }
                } catch (Throwable throwable3) {
                    CrashReport crashreport = CrashReport.forThrowable(throwable3, "Rendering screen");
                    CrashReportCategory crashreportcategory = crashreport.addCategory("Screen render details");
                    crashreportcategory.setDetail("Screen name", () -> this.minecraft.screen.getClass().getCanonicalName());
                    crashreportcategory.setDetail(
                        "Mouse location",
                        () -> String.format(
                                Locale.ROOT,
                                "Scaled: (%d, %d). Absolute: (%f, %f)",
                                i,
                                j,
                                this.minecraft.mouseHandler.xpos(),
                                this.minecraft.mouseHandler.ypos()
                            )
                    );
                    crashreportcategory.setDetail(
                        "Screen size",
                        () -> String.format(
                                Locale.ROOT,
                                "Scaled: (%d, %d). Absolute: (%d, %d). Scale factor of %f",
                                this.minecraft.getWindow().getGuiScaledWidth(),
                                this.minecraft.getWindow().getGuiScaledHeight(),
                                this.minecraft.getWindow().getWidth(),
                                this.minecraft.getWindow().getHeight(),
                                this.minecraft.getWindow().getGuiScale()
                            )
                    );
                    throw new ReportedException(crashreport);
                }

                try {
                    if (this.minecraft.screen != null) {
                        this.minecraft.screen.handleDelayedNarration();
                    }
                } catch (Throwable throwable) {
                    CrashReport crashreport1 = CrashReport.forThrowable(throwable, "Narrating screen");
                    CrashReportCategory crashreportcategory1 = crashreport1.addCategory("Screen details");
                    crashreportcategory1.setDetail("Screen name", () -> this.minecraft.screen.getClass().getCanonicalName());
                    throw new ReportedException(crashreport1);
                }
            }

            if (flag && pRenderLevel && this.minecraft.level != null) {
                this.minecraft.gui.renderSavingIndicator(guigraphics, pDeltaTracker);
            }

            if (flag) {
                try (Zone zone = profilerfiller.zone("toasts")) {
                    this.minecraft.getToastManager().render(guigraphics);
                }
            }

            guigraphics.flush();
            matrix4fstack.popMatrix();
            this.resourcePool.endFrame();
            this.lightTexture.setAllowed(true);
        }

        this.frameFinish();
        this.waitForServerThread();
        MemoryMonitor.update();
        Lagometer.updateLagometer();

        if(Minecraft.getInstance().getOverlay() == null) {

            RenderSystem.disableDepthTest();

            glEnable(GL_BLEND);
            GL30.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

            Render.frameBuffer.bind();

            GL11.glClearColor(0.0f, 0.0f, 0.0f, 0f);
            glClear(GL_COLOR_BUFFER_BIT);

            EventManager.call(new GraphicsEvent());
            Render.drawAll();

            Render.frameBuffer.unbind();

            Render.drawFrameBuffer(new float[]{255, 255, 255, Widget.menuAlpha});

            Render.drawAll();

            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();

        }

    }
    public static void get() {

        ByteBuffer data = BufferUtils.createByteBuffer(1440 * 1440 * 4);

        GL11.glReadPixels(0, 0, 1440, 1440, GL_RGBA, GL_UNSIGNED_BYTE, data);

        data.rewind();

        NativeImage texture = new NativeImage(1440, 1440, false);

        for (int y1 = 0; y1 < 1440; y1++)

            for (int x1 = 0; x1 < 1440; x1++) {
                int i = (x1 + (1440 - y1 - 1) * 1440) * 4;
                int r = data.get(i)     & 0xFF;
                int g = data.get(i + 1) & 0xFF;
                int b = data.get(i + 2) & 0xFF;
                int a = data.get(i + 3) & 0xFF;
                int rgba = (a << 24) | (b << 16) | (g << 8) | r;
                texture.setPixelABGR(x1, y1, rgba);
            }

        try {
            texture.writeToFile(new File("name.png"));
            texture.close();
        }catch (Exception e) {
            e.printStackTrace();
        }

    }
    private void tryTakeScreenshotIfNeeded() {
        if (!this.hasWorldScreenshot && this.minecraft.isLocalServer()) {
            long i = Util.getMillis();
            if (i - this.lastScreenshotAttempt >= 1000L) {
                this.lastScreenshotAttempt = i;
                IntegratedServer integratedserver = this.minecraft.getSingleplayerServer();
                if (integratedserver != null && !integratedserver.isStopped()) {
                    integratedserver.getWorldScreenshotFile().ifPresent(pathIn -> {
                        if (Files.isRegularFile(pathIn)) {
                            this.hasWorldScreenshot = true;
                        } else {
                            this.takeAutoScreenshot(pathIn);
                        }
                    });
                }
            }
        }
    }

    private void takeAutoScreenshot(Path pPath) {
        if (this.minecraft.levelRenderer.countRenderedSections() > 10 && this.minecraft.levelRenderer.hasRenderedAllSections()) {
            NativeImage nativeimage = Screenshot.takeScreenshot(this.minecraft.getMainRenderTarget());
            Util.ioPool().execute(() -> {
                int i = nativeimage.getWidth();
                int j = nativeimage.getHeight();
                int k = 0;
                int l = 0;
                if (i > j) {
                    k = (i - j) / 2;
                    i = j;
                } else {
                    l = (j - i) / 2;
                    j = i;
                }

                try (NativeImage nativeimage1 = new NativeImage(64, 64, false)) {
                    nativeimage.resizeSubRectTo(k, l, i, j, nativeimage1);
                    nativeimage1.writeToFile(pPath);
                } catch (IOException ioexception1) {
                    LOGGER.warn("Couldn't save auto screenshot", (Throwable)ioexception1);
                } finally {
                    nativeimage.close();
                }
            });
        }
    }

    private boolean shouldRenderBlockOutline() {
        if (!this.renderBlockOutline) {
            return false;
        } else {
            Entity entity = this.minecraft.getCameraEntity();
            boolean flag = entity instanceof Player && !this.minecraft.options.hideGui;
            if (flag && !((Player)entity).getAbilities().mayBuild) {
                ItemStack itemstack = ((LivingEntity)entity).getMainHandItem();
                HitResult hitresult = this.minecraft.hitResult;
                if (hitresult != null && hitresult.getType() == HitResult.Type.BLOCK) {
                    BlockPos blockpos = ((BlockHitResult)hitresult).getBlockPos();
                    BlockState blockstate = this.minecraft.level.getBlockState(blockpos);
                    if (this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
                        flag = blockstate.getMenuProvider(this.minecraft.level, blockpos) != null;
                    } else {
                        BlockInWorld blockinworld = new BlockInWorld(this.minecraft.level, blockpos, false);
                        Registry<Block> registry = this.minecraft.level.registryAccess().lookupOrThrow(Registries.BLOCK);
                        flag = !itemstack.isEmpty() && (itemstack.canBreakBlockInAdventureMode(blockinworld) || itemstack.canPlaceOnBlockInAdventureMode(blockinworld));
                    }
                }
            }

            return flag;
        }
    }

    public void renderLevel(DeltaTracker pDeltaTracker) {
        float f = pDeltaTracker.getGameTimeDeltaPartialTick(true);
        this.lightTexture.updateLightTexture(f);
        if (this.minecraft.getCameraEntity() == null) {
            this.minecraft.setCameraEntity(this.minecraft.player);
        }

        this.pick(f);
        if (Config.isShaders()) {
            Shaders.beginRender(this.minecraft, this.mainCamera, f);
        }

        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("center");
        boolean flag = Config.isShaders();
        if (flag) {
            Shaders.beginRenderPass(f);
        }

        boolean flag1 = this.shouldRenderBlockOutline();
        profilerfiller.popPush("camera");
        Camera camera = this.mainCamera;
        Entity entity = (Entity)(this.minecraft.getCameraEntity() == null ? this.minecraft.player : this.minecraft.getCameraEntity());
        float f1 = this.minecraft.level.tickRateManager().isEntityFrozen(entity) ? 1.0F : f;
        camera.setup(this.minecraft.level, entity, !this.minecraft.options.getCameraType().isFirstPerson(), this.minecraft.options.getCameraType().isMirrored(), f1);
        this.renderDistance = (float)(this.minecraft.options.getEffectiveRenderDistance() * 16);
        float f2 = this.getFov(camera, f, true);
        Matrix4f matrix4f = this.getProjectionMatrix(f2);
        Matrix4f matrix4f1 = matrix4f;
        if (Shaders.isEffectsModelView()) {
            matrix4f = new Matrix4f();
        }

        PoseStack posestack = new PoseStack();
        this.bobHurt(posestack, camera.getPartialTickTime());
        if (this.minecraft.options.bobView().get()) {
            this.bobView(posestack, camera.getPartialTickTime());
        }

        matrix4f.mul(posestack.last().pose());
        float f3 = this.minecraft.options.screenEffectScale().get().floatValue();
        float f4 = Mth.lerp(f, this.minecraft.player.oSpinningEffectIntensity, this.minecraft.player.spinningEffectIntensity) * f3 * f3;
        if (f4 > 0.0F) {
            int i = this.minecraft.player.hasEffect(MobEffects.CONFUSION) ? 7 : 20;
            float f5 = 5.0F / (f4 * f4 + 5.0F) - f4 * 0.04F;
            f5 *= f5;
            Vector3f vector3f = new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F);
            float f6 = ((float)this.confusionAnimationTick + f) * (float)i * (float) (Math.PI / 180.0);
            matrix4f.rotate(f6, vector3f);
            matrix4f.scale(1.0F / f5, 1.0F, 1.0F);
            matrix4f.rotate(-f6, vector3f);
        }

        Matrix4f matrix4f3 = matrix4f;
        if (Shaders.isEffectsModelView()) {
            matrix4f = matrix4f1;
        }

        float f7 = Math.max(f2, (float)this.minecraft.options.fov().get().intValue());
        Matrix4f matrix4f4 = this.getProjectionMatrix(f7);
        RenderSystem.setProjectionMatrix(matrix4f, ProjectionType.PERSPECTIVE);
        if (Reflector.ForgeEventFactoryClient_fireComputeCameraAngles.exists()) {
            ViewportEvent.ComputeCameraAngles viewportevent$computecameraangles = (ViewportEvent.ComputeCameraAngles)Reflector.ForgeEventFactoryClient_fireComputeCameraAngles
                .call(this, camera, f);
            camera.setRotation(
                viewportevent$computecameraangles.getYaw(), viewportevent$computecameraangles.getPitch(), viewportevent$computecameraangles.getRoll()
            );
        }

        Quaternionf quaternionf = camera.rotation().conjugate(new Quaternionf());
        Matrix4f matrix4f2 = new Matrix4f().rotation(quaternionf);
        if (Shaders.isEffectsModelView()) {
            matrix4f2 = matrix4f3.mul(matrix4f2);
        }

        this.minecraft.levelRenderer.prepareCullFrustum(camera.getPosition(), matrix4f2, matrix4f4);
        this.minecraft.getMainRenderTarget().bindWrite(true);
        this.minecraft.levelRenderer.renderLevel(this.resourcePool, pDeltaTracker, flag1, camera, this, matrix4f2, matrix4f);
        profilerfiller.popPush("hand");
        if (this.renderHand && !Shaders.isShadowPass) {
            if (flag) {
                ShadersRender.renderHand1(this, matrix4f2, camera, f);
                Shaders.renderCompositeFinal();
            }

            RenderSystem.clear(256);
            if (flag) {
                ShadersRender.renderFPOverlay(this, matrix4f2, camera, f);
            } else {
                this.renderItemInHand(camera, f, matrix4f2);
            }
        }

        if (flag) {
            Shaders.endRender();
        }

        profilerfiller.pop();
    }

    public void resetData() {
        this.itemActivationItem = null;
        this.minecraft.getMapTextureManager().resetData();
        this.mainCamera.reset();
        this.hasWorldScreenshot = false;
    }

    private void waitForServerThread() {
        this.serverWaitTimeCurrent = 0;
        if (!Config.isSmoothWorld() || !Config.isSingleProcessor()) {
            this.lastServerTime = 0L;
            this.lastServerTicks = 0;
        } else if (this.minecraft.isLocalServer()) {
            IntegratedServer integratedserver = this.minecraft.getSingleplayerServer();
            if (integratedserver != null) {
                boolean flag = this.minecraft.isPaused();
                if (!flag && !(this.minecraft.screen instanceof ReceivingLevelScreen)) {
                    if (this.serverWaitTime > 0) {
                        Lagometer.timerServer.start();
                        Config.sleep((long)this.serverWaitTime);
                        Lagometer.timerServer.end();
                        this.serverWaitTimeCurrent = this.serverWaitTime;
                    }

                    long i = System.nanoTime() / 1000000L;
                    if (this.lastServerTime != 0L && this.lastServerTicks != 0) {
                        long j = i - this.lastServerTime;
                        if (j < 0L) {
                            this.lastServerTime = i;
                            j = 0L;
                        }

                        if (j >= 50L) {
                            this.lastServerTime = i;
                            int k = integratedserver.getTickCount();
                            int l = k - this.lastServerTicks;
                            if (l < 0) {
                                this.lastServerTicks = k;
                                l = 0;
                            }

                            if (l < 1 && this.serverWaitTime < 100) {
                                this.serverWaitTime += 2;
                            }

                            if (l > 1 && this.serverWaitTime > 0) {
                                this.serverWaitTime--;
                            }

                            this.lastServerTicks = k;
                        }
                    } else {
                        this.lastServerTime = i;
                        this.lastServerTicks = integratedserver.getTickCount();
                        this.avgServerTickDiff = 1.0F;
                        this.avgServerTimeDiff = 50.0F;
                    }
                } else {
                    if (this.minecraft.screen instanceof ReceivingLevelScreen) {
                        Config.sleep(20L);
                    }

                    this.lastServerTime = 0L;
                    this.lastServerTicks = 0;
                }
            }
        }
    }

    private void frameInit() {
        this.frameCount++;
        Config.frameStart();
        GlErrors.frameStart();
        if (!this.initialized) {
            ReflectorResolver.resolve();
            if (Config.getBitsOs() == 64 && Config.getBitsJre() == 32) {
                Config.setNotify64BitJava(true);
            }

            this.initialized = true;
        }

        Level level = this.minecraft.level;
        if (level != null) {
            if (Config.getNewRelease() != null) {
                String s = "HD_U".replace("HD_U", "HD Ultra").replace("L", "Light");
                String s1 = s + " " + Config.getNewRelease();
                MutableComponent mutablecomponent = Component.literal(
                    I18n.get("of.message.newVersion", "\u00ef\u00bf\u00bdn" + s1 + "\u00ef\u00bf\u00bdr")
                );
                mutablecomponent.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://optifine.net/downloads")));
                this.minecraft.gui.getChat().addMessage(mutablecomponent);
                Config.setNewRelease(null);
            }

            if (Config.isNotify64BitJava()) {
                Config.setNotify64BitJava(false);
                Component component = Component.literal(I18n.get("of.message.java64Bit"));
                this.minecraft.gui.getChat().addMessage(component);
            }
        }

        if (this.updatedWorld != level) {
            RandomEntities.worldChanged(this.updatedWorld, level);
            Config.updateThreadPriorities();
            this.lastServerTime = 0L;
            this.lastServerTicks = 0;
            this.updatedWorld = level;
        }

        if (!this.setFxaaShader(Shaders.configAntialiasingLevel)) {
            Shaders.configAntialiasingLevel = 0;
        }

        if (this.minecraft.screen != null && this.minecraft.screen.getClass() == ChatScreen.class) {
            this.minecraft.setScreen(new GuiChatOF((ChatScreen)this.minecraft.screen));
        }
    }

    private void frameFinish() {
        if (this.minecraft.level != null && Config.isShowGlErrors() && TimedEvent.isActive("CheckGlErrorFrameFinish", 10000L)) {
            int i = GlStateManager._getError();
            if (i != 0 && GlErrors.isEnabled(i)) {
                String s = Config.getGlErrorString(i);
                Component component = Component.literal(I18n.get("of.message.openglError", i, s));
                this.minecraft.gui.getChat().addMessage(component);
            }
        }
    }

    public boolean setFxaaShader(int fxaaLevel) {
        if (!Config.isMainFramebufferEnabled()) {
            return false;
        } else if (this.postEffectId != null && this.postEffectId != this.fxaaShaders[2] && this.postEffectId != this.fxaaShaders[4]) {
            return true;
        } else if (fxaaLevel != 2 && fxaaLevel != 4) {
            if (this.postEffectId == null) {
                return true;
            } else {
                this.postEffectId = null;
                return true;
            }
        } else if (this.postEffectId != null && this.postEffectId == this.fxaaShaders[fxaaLevel]) {
            return true;
        } else if (this.minecraft.level == null) {
            return true;
        } else {
            ResourceLocation resourcelocation = new ResourceLocation("fxaa_of_" + fxaaLevel + "x");
            this.fxaaShaders[fxaaLevel] = resourcelocation;
            this.setPostEffect(resourcelocation);
            return this.effectActive;
        }
    }

    public static float getRenderPartialTicks() {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    public int getFrameCount() {
        return this.frameCount;
    }

    public void displayItemActivation(ItemStack pStack) {
        this.itemActivationItem = pStack;
        this.itemActivationTicks = 40;
        this.itemActivationOffX = this.random.nextFloat() * 2.0F - 1.0F;
        this.itemActivationOffY = this.random.nextFloat() * 2.0F - 1.0F;
    }

    private void renderItemActivationAnimation(GuiGraphics pGuiGraphics, float pPartialTick) {
        if (this.itemActivationItem != null && this.itemActivationTicks > 0) {
            int i = 40 - this.itemActivationTicks;
            float f = ((float)i + pPartialTick) / 40.0F;
            float f1 = f * f;
            float f2 = f * f1;
            float f3 = 10.25F * f2 * f1 - 24.95F * f1 * f1 + 25.5F * f2 - 13.8F * f1 + 4.0F * f;
            float f4 = f3 * (float) Math.PI;
            float f5 = this.itemActivationOffX * (float)(pGuiGraphics.guiWidth() / 4);
            float f6 = this.itemActivationOffY * (float)(pGuiGraphics.guiHeight() / 4);
            PoseStack posestack = pGuiGraphics.pose();
            posestack.pushPose();
            posestack.translate(
                (float)(pGuiGraphics.guiWidth() / 2) + f5 * Mth.abs(Mth.sin(f4 * 2.0F)),
                (float)(pGuiGraphics.guiHeight() / 2) + f6 * Mth.abs(Mth.sin(f4 * 2.0F)),
                -50.0F
            );
            float f7 = 50.0F + 175.0F * Mth.sin(f4);
            posestack.scale(f7, -f7, f7);
            posestack.mulPose(Axis.YP.rotationDegrees(900.0F * Mth.abs(Mth.sin(f4))));
            posestack.mulPose(Axis.XP.rotationDegrees(6.0F * Mth.cos(f * 8.0F)));
            posestack.mulPose(Axis.ZP.rotationDegrees(6.0F * Mth.cos(f * 8.0F)));
            pGuiGraphics.drawSpecial(
                bufferSourceIn -> this.minecraft
                        .getItemRenderer()
                        .renderStatic(
                            this.itemActivationItem, ItemDisplayContext.FIXED, 15728880, OverlayTexture.NO_OVERLAY, posestack, bufferSourceIn, this.minecraft.level, 0
                        )
            );
            posestack.popPose();
        }
    }

    public Minecraft getMinecraft() {
        return this.minecraft;
    }

    public float getDarkenWorldAmount(float pPartialTicks) {
        return Mth.lerp(pPartialTicks, this.darkenWorldAmountO, this.darkenWorldAmount);
    }

    public float getRenderDistance() {
        return this.renderDistance;
    }

    public Camera getMainCamera() {
        return this.mainCamera;
    }

    public LightTexture lightTexture() {
        return this.lightTexture;
    }

    public OverlayTexture overlayTexture() {
        return this.overlayTexture;
    }
}