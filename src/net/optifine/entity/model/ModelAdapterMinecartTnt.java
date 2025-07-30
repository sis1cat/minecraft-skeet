package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.world.entity.EntityType;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterMinecartTnt extends ModelAdapterMinecart {
    public ModelAdapterMinecartTnt() {
        super(EntityType.TNT_MINECART, "tnt_minecart", ModelLayers.TNT_MINECART);
        this.setAlias("minecart");
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, RendererCache rendererCache, int index) {
        TntMinecartRenderer tntminecartrenderer = new TntMinecartRenderer(this.getContext());
        if (!Reflector.RenderMinecart_modelMinecart.exists()) {
            Config.warn("Field not found: RenderMinecart.modelMinecart");
            return null;
        } else {
            Reflector.setFieldValue(tntminecartrenderer, Reflector.RenderMinecart_modelMinecart, modelBase);
            return tntminecartrenderer;
        }
    }
}