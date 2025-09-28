package net.minecraftforge.client.extensions;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

public interface IForgeBakedModel {
    private BakedModel getBakedModel() {
        return (BakedModel)this;
    }

    default List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
        throw new UnsupportedOperationException();
    }

    default boolean useAmbientOcclusion(BlockState state) {
        return this.getBakedModel().useAmbientOcclusion();
    }

    default boolean useAmbientOcclusion(BlockState state, RenderType renderType) {
        return this.getBakedModel().useAmbientOcclusion();
    }

    default BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
        throw new UnsupportedOperationException();
    }

    default ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        throw new UnsupportedOperationException();
    }

    default TextureAtlasSprite getParticleIcon(ModelData data) {
        throw new UnsupportedOperationException();
    }

    default ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        throw new UnsupportedOperationException();
    }
}