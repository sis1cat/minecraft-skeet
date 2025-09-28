package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterBoatPatch extends ModelAdapterEntity {
    public ModelAdapterBoatPatch(EntityType entityType, String prefix, ModelLayerLocation modelLayer) {
        this(entityType, prefix + "_boat_patch", modelLayer, new String[]{"boat_patch"});
    }

    protected ModelAdapterBoatPatch(EntityType entityType, String name, ModelLayerLocation modelLayer, String[] aliases) {
        super(entityType, name, modelLayer);
        this.setAliases(aliases);
    }

    @Override
    public Model makeModel(ModelPart root) {
        root = bakeModelLayer(ModelLayers.BOAT_WATER_PATCH);
        return new Model.Simple(root, locIn -> RenderType.waterMask());
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("water_patch", "water_patch");
        map.put("root", "root");
        return map;
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, RendererCache rendererCache, int index) {
        BoatRenderer boatrenderer = new BoatRenderer(this.getEntityContext(), this.getModelLayer());
        BoatRenderer boatrenderer1 = (BoatRenderer)rendererCache.get(this.getEntityType(), index, () -> boatrenderer);
        if (!Reflector.RenderBoat_patchModel.exists()) {
            Config.warn("Field not found: RenderBoat.patchModel");
            return null;
        } else {
            Reflector.RenderBoat_patchModel.setValue(boatrenderer1, modelBase);
            return boatrenderer1;
        }
    }
}