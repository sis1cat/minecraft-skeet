package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.LlamaDecorLayer;
import net.minecraft.world.entity.EntityType;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorField;

public class ModelAdapterLlamaDecor extends ModelAdapterLlama {
    public ModelAdapterLlamaDecor() {
        this(EntityType.LLAMA, "llama_decor", ModelLayers.LLAMA_DECOR);
    }

    protected ModelAdapterLlamaDecor(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterLlamaDecor modeladapterllamadecor = new ModelAdapterLlamaDecor(this.getEntityType(), "llama_baby_decor", ModelLayers.LLAMA_BABY_DECOR);
        modeladapterllamadecor.setBaby(true);
        modeladapterllamadecor.setAlias(this.getName());
        return modeladapterllamadecor;
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        ReflectorField reflectorfield = this.isBaby() ? Reflector.LlamaDecorLayer_babyModel : Reflector.LlamaDecorLayer_adultModel;
        this.setLayerModel(renderer, LlamaDecorLayer.class, reflectorfield, "LlamaDecorLayer_model", modelBase);
    }
}