package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.optifine.entity.model.anim.IRenderResolver;
import net.optifine.entity.model.anim.RenderResolverEntity;

public class ModelAdapterTridentHand extends ModelAdapterVirtual {
    private static Map<String, String> mapParts = makeMapParts();

    public ModelAdapterTridentHand() {
        super("trident_hand");
        this.setAlias("trident");
    }

    @Override
    public Model makeModel() {
        return new TridentModel(bakeModelLayer(ModelLayers.TRIDENT));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart) {
        return this.getModelRenderer(model.root(), mapParts.get(modelPart));
    }

    @Override
    public String[] getModelRendererNames() {
        return toArray(mapParts.keySet());
    }

    public static Map<String, String> makeMapParts() {
        return ModelAdapterTrident.makeStaticMapParts();
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, RendererCache rendererCache, int index) {
        final TridentModel tridentmodel = (TridentModel)modelBase;
        return new VirtualEntityRenderer() {
            @Override
            public void register() {
                tridentmodel.locationTextureCustom = this.getLocationTextureCustom();
                CustomStaticModels.setTridentModel(tridentmodel);
            }
        };
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