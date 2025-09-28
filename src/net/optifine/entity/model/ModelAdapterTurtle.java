package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.TurtleModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TurtleRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterTurtle extends ModelAdapterQuadruped {
    public ModelAdapterTurtle() {
        super(EntityType.TURTLE, "turtle", ModelLayers.TURTLE);
    }

    protected ModelAdapterTurtle(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterTurtle modeladapterturtle = new ModelAdapterTurtle(this.getEntityType(), "turtle_baby", ModelLayers.TURTLE_BABY);
        modeladapterturtle.setBaby(true);
        modeladapterturtle.setAlias(this.getName());
        return modeladapterturtle;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new TurtleModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = super.makeMapParts();
        map.put("body2", "egg_belly");
        return map;
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new TurtleRenderer(context);
    }
}