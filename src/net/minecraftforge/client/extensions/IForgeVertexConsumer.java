package net.minecraftforge.client.extensions;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;

public interface IForgeVertexConsumer {
    default void putBulkData(
        PoseStack.Pose pose,
        BakedQuad bakedQuad,
        float red,
        float green,
        float blue,
        float alpha,
        int packedLight,
        int packedOverlay,
        boolean readExistingColor
    ) {
        throw new UnsupportedOperationException();
    }
}