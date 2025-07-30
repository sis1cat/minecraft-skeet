package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PistonHeadRenderer implements BlockEntityRenderer<PistonMovingBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public PistonHeadRenderer(BlockEntityRendererProvider.Context pContext) {
        this.blockRenderer = pContext.getBlockRenderDispatcher();
    }

    public void render(PistonMovingBlockEntity p_112452_, float p_112453_, PoseStack p_112454_, MultiBufferSource p_112455_, int p_112456_, int p_112457_) {
        Level level = p_112452_.getLevel();
        if (level != null) {
            BlockPos blockpos = p_112452_.getBlockPos().relative(p_112452_.getMovementDirection().getOpposite());
            BlockState blockstate = p_112452_.getMovedState();
            if (!blockstate.isAir()) {
                ModelBlockRenderer.enableCaching();
                p_112454_.pushPose();
                p_112454_.translate(p_112452_.getXOff(p_112453_), p_112452_.getYOff(p_112453_), p_112452_.getZOff(p_112453_));
                if (blockstate.is(Blocks.PISTON_HEAD) && p_112452_.getProgress(p_112453_) <= 4.0F) {
                    blockstate = blockstate.setValue(PistonHeadBlock.SHORT, Boolean.valueOf(p_112452_.getProgress(p_112453_) <= 0.5F));
                    this.renderBlock(blockpos, blockstate, p_112454_, p_112455_, level, false, p_112457_);
                } else if (p_112452_.isSourcePiston() && !p_112452_.isExtending()) {
                    PistonType pistontype = blockstate.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT;
                    BlockState blockstate1 = Blocks.PISTON_HEAD
                        .defaultBlockState()
                        .setValue(PistonHeadBlock.TYPE, pistontype)
                        .setValue(PistonHeadBlock.FACING, blockstate.getValue(PistonBaseBlock.FACING));
                    blockstate1 = blockstate1.setValue(PistonHeadBlock.SHORT, Boolean.valueOf(p_112452_.getProgress(p_112453_) >= 0.5F));
                    this.renderBlock(blockpos, blockstate1, p_112454_, p_112455_, level, false, p_112457_);
                    BlockPos blockpos1 = blockpos.relative(p_112452_.getMovementDirection());
                    p_112454_.popPose();
                    p_112454_.pushPose();
                    blockstate = blockstate.setValue(PistonBaseBlock.EXTENDED, Boolean.valueOf(true));
                    this.renderBlock(blockpos1, blockstate, p_112454_, p_112455_, level, true, p_112457_);
                } else {
                    this.renderBlock(blockpos, blockstate, p_112454_, p_112455_, level, false, p_112457_);
                }

                p_112454_.popPose();
                ModelBlockRenderer.clearCache();
            }
        }
    }

    private void renderBlock(
        BlockPos pPos, BlockState pState, PoseStack pPoseStack, MultiBufferSource pBufferSource, Level pLevel, boolean pExtended, int pPackedOverlay
    ) {
        RenderType rendertype = ItemBlockRenderTypes.getMovingBlockRenderType(pState);
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(rendertype);
        this.blockRenderer
            .getModelRenderer()
            .tesselateBlock(
                pLevel,
                this.blockRenderer.getBlockModel(pState),
                pState,
                pPos,
                pPoseStack,
                vertexconsumer,
                pExtended,
                RandomSource.create(),
                pState.getSeed(pPos),
                pPackedOverlay
            );
    }

    @Override
    public int getViewDistance() {
        return 68;
    }
}