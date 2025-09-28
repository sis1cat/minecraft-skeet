package net.optifine.entity.model;

import java.util.function.Function;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class CustomEntityModel extends Model {
    public CustomEntityModel(ModelPart root, Function<ResourceLocation, RenderType> renderTypeIn) {
        super(root, renderTypeIn);
    }
}