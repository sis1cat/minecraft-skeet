package net.optifine.entity.model;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class Renderers {
    private final Map<EntityType, EntityRenderer> entityRenderers = new HashMap<>();
    private final Map<BlockEntityType, BlockEntityRenderer> blockEntityRenderers = new HashMap<>();
    private final Map<SkullBlock.Type, SkullModelBase> skullModels = new HashMap<>();
    private BookModel bookModel;
    private ParrotModel parrotModel;

    public static Renderers collectRenderers() {
        Renderers renderers = new Renderers();
        renderers.entityRenderers.putAll(getEntityRenderMap());
        renderers.blockEntityRenderers.putAll(getBlockEntityRenderMap());
        renderers.skullModels.putAll(getSkullModelMap());
        renderers.bookModel = CustomStaticModels.getBookModel();
        renderers.parrotModel = CustomStaticModels.getParrotModel();
        return renderers;
    }

    public void restoreRenderers() {
        new Renderers();
        getEntityRenderMap().clear();
        getEntityRenderMap().putAll(this.entityRenderers);
        getBlockEntityRenderMap().clear();
        getBlockEntityRenderMap().putAll(this.blockEntityRenderers);
        getSkullModelMap().clear();
        getSkullModelMap().putAll(this.skullModels);
        CustomStaticModels.setBookModel(this.bookModel);
        CustomStaticModels.setParrotModel(this.parrotModel);
    }

    private static Map getEntityRenderMap() {
        return RendererUtils.getEntityRenderMap();
    }

    public static Map<BlockEntityType, BlockEntityRenderer> getBlockEntityRenderMap() {
        return RendererUtils.getBlockEntityRenderMap();
    }

    public static Map<SkullBlock.Type, SkullModelBase> getSkullModelMap() {
        return RendererUtils.getSkullModelMap();
    }
}
