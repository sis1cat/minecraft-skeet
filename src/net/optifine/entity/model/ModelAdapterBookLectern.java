package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.LecternRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterBookLectern extends ModelAdapterBook {
    public ModelAdapterBookLectern() {
        super(BlockEntityType.LECTERN, "lectern_book");
        this.setAliases(new String[]{"enchanting_book", "book"});
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, RendererCache rendererCache, int index) {
        BlockEntityRenderer blockentityrenderer = rendererCache.get(BlockEntityType.LECTERN, index, () -> new LecternRenderer(this.getContext()));
        if (!(blockentityrenderer instanceof LecternRenderer)) {
            return null;
        } else if (!Reflector.TileEntityLecternRenderer_modelBook.exists()) {
            Config.warn("Field not found: TileEntityLecternRenderer.modelBook");
            return null;
        } else {
            Reflector.setFieldValue(blockentityrenderer, Reflector.TileEntityLecternRenderer_modelBook, modelBase);
            return blockentityrenderer;
        }
    }
}