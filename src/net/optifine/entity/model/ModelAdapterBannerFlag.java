package net.optifine.entity.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.BannerFlagModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.optifine.reflect.Reflector;

public class ModelAdapterBannerFlag extends ModelAdapterBlockEntity {
    private boolean standing;
    private Map<String, String> mapParts = this.makeMapParts();

    public ModelAdapterBannerFlag(boolean standing) {
        super(BlockEntityType.BANNER, "banner_flag" + (standing ? "" : "_wall"));
        this.standing = standing;
        if (!standing) {
            this.setAliases(new String[]{"banner_flag", "banner"});
        } else {
            this.setAlias("banner");
        }
    }

    @Override
    public Model makeModel() {
        return new BannerFlagModel(bakeModelLayer(this.standing ? ModelLayers.STANDING_BANNER_FLAG : ModelLayers.WALL_BANNER_FLAG));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart) {
        return this.getModelRenderer(model.root(), this.mapParts.get(modelPart));
    }

    @Override
    public String[] getModelRendererNames() {
        return toArray(this.mapParts.keySet());
    }

    @Override
    public String[] getIgnoredModelRendererNames() {
        List<String> list = new ArrayList<>(ModelAdapterBannerBase.getAllBannerPartNames());
        list.removeAll(this.mapParts.keySet());
        return toArray(list);
    }

    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("slate", "flag");
        map.put("root", "root");
        return map;
    }

    @Override
    public IEntityRenderer makeEntityRender(Model model, RendererCache rendererCache, int index) {
        BlockEntityRenderer blockentityrenderer = rendererCache.get(BlockEntityType.BANNER, index, () -> new BannerRenderer(this.getContext()));
        BannerFlagModel bannerflagmodel = (BannerFlagModel)model;
        if (!Reflector.TileEntityBannerRenderer_bannerFlagModels.exists()) {
            throw new IllegalArgumentException("Field not found: BannerRenderer.bannerFlagModels");
        } else {
            int i = this.standing ? 0 : 1;
            Reflector.TileEntityBannerRenderer_bannerFlagModels.setValue(blockentityrenderer, i, bannerflagmodel);
            return blockentityrenderer;
        }
    }
}