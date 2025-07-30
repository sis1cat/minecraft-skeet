package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.HitResult;
import net.optifine.EmissiveTextures;
import net.optifine.entity.model.CustomEntityModels;

public class BlockEntityRenderDispatcher implements ResourceManagerReloadListener {
    private Map<BlockEntityType<?>, BlockEntityRenderer<?>> renderers = ImmutableMap.of();
    private final Font font;
    private final Supplier<EntityModelSet> entityModelSet;
    public Level level;
    public Camera camera;
    public HitResult cameraHitResult;
    private final BlockRenderDispatcher blockRenderDispatcher;
    private final ItemModelResolver itemModelResolver;
    private final ItemRenderer itemRenderer;
    private final EntityRenderDispatcher entityRenderer;
    public static BlockEntity tileEntityRendered;
    private BlockEntityRendererProvider.Context context;

    public BlockEntityRenderDispatcher(
        Font pFont,
        Supplier<EntityModelSet> pEntityModelSet,
        BlockRenderDispatcher pBlockRenderDispatcher,
        ItemModelResolver pItemModelResolver,
        ItemRenderer pItemRenderer,
        EntityRenderDispatcher pEntityRenderer
    ) {
        this.itemRenderer = pItemRenderer;
        this.itemModelResolver = pItemModelResolver;
        this.entityRenderer = pEntityRenderer;
        this.font = pFont;
        this.entityModelSet = pEntityModelSet;
        this.blockRenderDispatcher = pBlockRenderDispatcher;
    }

    @Nullable
    public <E extends BlockEntity> BlockEntityRenderer<E> getRenderer(E pBlockEntity) {
        return (BlockEntityRenderer<E>)this.renderers.get(pBlockEntity.getType());
    }

    public void prepare(Level pLevel, Camera pCamera, HitResult pCameraHitResult) {
        if (this.level != pLevel) {
            this.setLevel(pLevel);
        }

        this.camera = pCamera;
        this.cameraHitResult = pCameraHitResult;
    }

    public <E extends BlockEntity> void render(E pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource) {
        BlockEntityRenderer<E> blockentityrenderer = this.getRenderer(pBlockEntity);
        if (blockentityrenderer != null
            && pBlockEntity.hasLevel()
            && pBlockEntity.getType().isValid(pBlockEntity.getBlockState())
            && blockentityrenderer.shouldRender(pBlockEntity, this.camera.getPosition())) {
            try {
                setupAndRender(blockentityrenderer, pBlockEntity, pPartialTick, pPoseStack, pBufferSource);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering Block Entity");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Block Entity Details");
                pBlockEntity.fillCrashReportCategory(crashreportcategory);
                throw new ReportedException(crashreport);
            }
        }
    }

    private static <T extends BlockEntity> void setupAndRender(
        BlockEntityRenderer<T> pRenderer, T pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource
    ) {
        Level level = pBlockEntity.getLevel();
        int i;
        if (level != null) {
            i = LevelRenderer.getLightColor(level, pBlockEntity.getBlockPos());
        } else {
            i = 15728880;
        }

        BlockEntity blockentity = tileEntityRendered;
        tileEntityRendered = pBlockEntity;
        pRenderer = CustomEntityModels.getBlockEntityRenderer(pBlockEntity, pRenderer);
        if (EmissiveTextures.isActive()) {
            EmissiveTextures.beginRender();
        }

        pRenderer.render(pBlockEntity, pPartialTick, pPoseStack, pBufferSource, i, OverlayTexture.NO_OVERLAY);
        if (EmissiveTextures.isActive()) {
            if (EmissiveTextures.hasEmissive()) {
                EmissiveTextures.beginRenderEmissive();
                pRenderer.render(pBlockEntity, pPartialTick, pPoseStack, pBufferSource, LightTexture.MAX_BRIGHTNESS, OverlayTexture.NO_OVERLAY);
                EmissiveTextures.endRenderEmissive();
            }

            EmissiveTextures.endRender();
        }

        tileEntityRendered = blockentity;
    }

    public void setLevel(@Nullable Level pLevel) {
        this.level = pLevel;
        if (pLevel == null) {
            this.camera = null;
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager p_173563_) {
        BlockEntityRendererProvider.Context blockentityrendererprovider$context = new BlockEntityRendererProvider.Context(
            this, this.blockRenderDispatcher, this.itemModelResolver, this.itemRenderer, this.entityRenderer, this.entityModelSet.get(), this.font
        );
        this.context = blockentityrendererprovider$context;
        this.renderers = BlockEntityRenderers.createEntityRenderers(blockentityrendererprovider$context);
    }

    public BlockEntityRenderer getRenderer(BlockEntityType type) {
        return this.renderers.get(type);
    }

    public BlockEntityRendererProvider.Context getContext() {
        return this.context;
    }

    public Map getBlockEntityRenderMap() {
        if (this.renderers instanceof ImmutableMap) {
            this.renderers = new HashMap<>(this.renderers);
        }

        return this.renderers;
    }
}
