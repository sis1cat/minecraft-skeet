package net.optifine.entity.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.WoodType;

public class ModelAdapterSign extends ModelAdapterBlockEntity {
    private WoodType woodType;
    private boolean standing;
    private Map<String, String> mapParts;

    public ModelAdapterSign(WoodType woodType, boolean standing) {
        super(BlockEntityType.SIGN, woodType.name() + (standing ? "_sign" : "_wall_sign"));
        this.setAliases(standing ? new String[]{"sign"} : new String[]{"wall_sign", "sign"});
        this.woodType = woodType;
        this.standing = standing;
        this.mapParts = this.makeMapParts();
    }

    @Override
    public Model makeModel() {
        return SignRenderer.createSignModel(Minecraft.getInstance().getEntityModels(), this.woodType, this.standing);
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
        List<String> list = new ArrayList<>(List.of("board", "stick"));
        list.removeAll(this.mapParts.keySet());
        return toArray(list);
    }

    private Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("board", "sign");
        if (this.standing) {
            map.put("stick", "stick");
        }

        map.put("root", "root");
        return map;
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, RendererCache rendererCache, int index) {
        SignRenderer signrenderer = (SignRenderer)rendererCache.get(BlockEntityType.SIGN, index, () -> new SignRenderer(this.getContext()));
        signrenderer.setSignModel(this.woodType, modelBase, this.standing);
        return signrenderer;
    }
}