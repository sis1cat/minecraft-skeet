package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.LlamaDecorLayer;
import net.minecraft.world.entity.EntityType;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorField;

public class ModelAdapterTraderLlamaDecor extends ModelAdapterTraderLlama {
    public ModelAdapterTraderLlamaDecor() {
        super(EntityType.TRADER_LLAMA, "trader_llama_decor", ModelLayers.LLAMA_DECOR);
    }

    protected ModelAdapterTraderLlamaDecor(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterTraderLlamaDecor modeladaptertraderllamadecor = new ModelAdapterTraderLlamaDecor(
            this.getEntityType(), "trader_llama_baby_decor", ModelLayers.LLAMA_BABY_DECOR
        );
        modeladaptertraderllamadecor.setBaby(true);
        modeladaptertraderllamadecor.setAlias(this.getName());
        return modeladaptertraderllamadecor;
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        ReflectorField reflectorfield = this.isBaby() ? Reflector.LlamaDecorLayer_babyModel : Reflector.LlamaDecorLayer_adultModel;
        this.setLayerModel(renderer, LlamaDecorLayer.class, reflectorfield, "LlamaDecorLayer_model", modelBase);
    }
}