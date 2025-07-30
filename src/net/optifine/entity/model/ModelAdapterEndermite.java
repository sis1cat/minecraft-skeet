package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.EndermiteModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EndermiteRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterEndermite extends ModelAdapterEntity {
    public ModelAdapterEndermite() {
        super(EntityType.ENDERMITE, "endermite", ModelLayers.ENDERMITE);
    }

    @Override
    public Model makeModel(ModelPart root) {
        return new EndermiteModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body1", "segment0");
        map.put("body2", "segment1");
        map.put("body3", "segment2");
        map.put("body4", "segment3");
        map.put("root", "root");
        return map;
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, RendererCache rendererCache, int index) {
        EndermiteRenderer endermiterenderer = new EndermiteRenderer(this.getContext());
        endermiterenderer.model = (EndermiteModel)modelBase;
        return endermiterenderer;
    }
}