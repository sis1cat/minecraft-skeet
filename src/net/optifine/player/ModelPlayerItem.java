package net.optifine.player;

import java.util.function.Function;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class ModelPlayerItem extends Model {
    public ModelPlayerItem(Function<ResourceLocation, RenderType> renderTypeIn) {
        super(ModelPart.makeRoot(), renderTypeIn);
    }
}