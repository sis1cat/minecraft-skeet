package net.optifine.entity.model;

import java.util.List;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.optifine.reflect.ReflectorField;

public abstract class ModelAdapterLiving extends ModelAdapterEntity {
    public ModelAdapterLiving(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public final IEntityRenderer makeEntityRender(Model modelBase, RendererCache rendererCache, int index) {
        LivingEntityRenderer livingentityrenderer = (LivingEntityRenderer)rendererCache.get(
            this.getEntityType(), index, () -> this.makeLivingRenderer(this.getContext())
        );
        this.modifyLivingRenderer(livingentityrenderer, modelBase);
        return livingentityrenderer;
    }

    protected abstract LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context var1);

    protected void modifyLivingRenderer(LivingEntityRenderer renderer, Model modelBase) {
        if (!(modelBase instanceof EntityModel)) {
            throw new IllegalArgumentException("Not an EntityModel: " + modelBase);
        } else {
            renderer.model = ((EntityModel)modelBase);
        }
    }

    public List<RenderLayer> getRenderLayers(LivingEntityRenderer renderer, Class cls) {
        return renderer.getLayers(cls);
    }

    protected void setLayerModel(LivingEntityRenderer renderer, Class layerClass, ReflectorField modelField, String fieldName, Model model) {
        if (!modelField.exists()) {
            throw new IllegalArgumentException("Field not found: " + fieldName);
        } else {
            for (RenderLayer renderlayer : this.getRenderLayers(renderer, layerClass)) {
                modelField.setValue(renderlayer, model);
            }
        }
    }
}
