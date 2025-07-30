package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.optifine.entity.model.anim.IRenderResolver;
import net.optifine.entity.model.anim.RenderResolverEntity;

public class ModelAdapterParrotShoulder extends ModelAdapterVirtual {
    private static Map<String, String> mapParts = makeMapParts();

    public ModelAdapterParrotShoulder() {
        super("parrot_shoulder");
        this.setAlias("parrot");
    }

    @Override
    public Model makeModel() {
        return new ParrotModel(bakeModelLayer(ModelLayers.PARROT));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart) {
        return this.getModelRenderer(model.root(), mapParts.get(modelPart));
    }

    @Override
    public String[] getModelRendererNames() {
        return toArray(mapParts.keySet());
    }

    private static Map<String, String> makeMapParts() {
        return ModelAdapterParrot.makeStaticMapParts();
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, RendererCache rendererCache, int index) {
        final ParrotModel parrotmodel = (ParrotModel)modelBase;
        IEntityRenderer ientityrenderer = new VirtualEntityRenderer() {
            @Override
            public void register() {
                parrotmodel.locationTextureCustom = this.getLocationTextureCustom();
                CustomStaticModels.setParrotModel(parrotmodel);
            }
        };
        return ientityrenderer;
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, ResourceLocation textureLocation) {
        VirtualEntityRenderer virtualentityrenderer = (VirtualEntityRenderer)er;
        virtualentityrenderer.setLocationTextureCustom(textureLocation);
        return true;
    }

    @Override
    public IRenderResolver getRenderResolver() {
        return new RenderResolverEntity();
    }
}