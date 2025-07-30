package net.optifine.entity.model;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterHangingSign extends ModelAdapterBlockEntity {
    private WoodType woodType;
    private HangingSignRenderer.AttachmentType attachmentType;
    private Map<String, String> mapParts;

    public ModelAdapterHangingSign(WoodType woodType, HangingSignRenderer.AttachmentType attachmentType) {
        super(BlockEntityType.HANGING_SIGN, woodType.name() + "_hanging_sign_" + attachmentType.getSerializedName());
        this.setAlias("hanging_sign_" + attachmentType.getSerializedName());
        this.setAlias("hanging_sign");
        this.woodType = woodType;
        this.attachmentType = attachmentType;
        this.mapParts = makeMapParts(attachmentType);
    }

    @Override
    public Model makeModel() {
        return HangingSignRenderer.createSignModel(Minecraft.getInstance().getEntityModels(), this.woodType, this.attachmentType);
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
        List<String> list = new ArrayList<>(List.of("board", "plank", "chains", "chain_left1", "chain_left2", "chain_right1", "chain_right2", "chains_v"));
        list.removeAll(this.mapParts.keySet());
        return toArray(list);
    }

    private static Map<String, String> makeMapParts(HangingSignRenderer.AttachmentType attType) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("board", "board");
        if (attType == HangingSignRenderer.AttachmentType.WALL) {
            map.put("plank", "plank");
        }

        if (attType == HangingSignRenderer.AttachmentType.WALL || attType == HangingSignRenderer.AttachmentType.CEILING) {
            map.put("chains", "normalChains");
            map.put("chain_left1", "chainL1");
            map.put("chain_left2", "chainL2");
            map.put("chain_right1", "chainR1");
            map.put("chain_right2", "chainR2");
        }

        if (attType == HangingSignRenderer.AttachmentType.CEILING_MIDDLE) {
            map.put("chains_v", "vChains");
        }

        map.put("root", "root");
        return map;
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, RendererCache rendererCache, int index) {
        BlockEntityRenderer blockentityrenderer = rendererCache.get(BlockEntityType.HANGING_SIGN, index, () -> new HangingSignRenderer(this.getContext()));
        if (!(blockentityrenderer instanceof HangingSignRenderer)) {
            return null;
        } else if (!Reflector.TileEntityHangingSignRenderer_hangingSignModels.exists()) {
            Config.warn("Field not found: HangingSignRenderer.hangingSignModels");
            return null;
        } else {
            Map<HangingSignRenderer.ModelKey, Model> map = (Map<HangingSignRenderer.ModelKey, Model>)Reflector.getFieldValue(
                blockentityrenderer, Reflector.TileEntityHangingSignRenderer_hangingSignModels
            );
            if (map == null) {
                Config.warn("Field not found: HangingSignRenderer.hangingSignModels");
                return null;
            } else {
                if (map instanceof ImmutableMap) {
                    map = new HashMap<>(map);
                    Reflector.TileEntityHangingSignRenderer_hangingSignModels.setValue(blockentityrenderer, map);
                }

                HangingSignRenderer.ModelKey hangingsignrenderer$modelkey = new HangingSignRenderer.ModelKey(this.woodType, this.attachmentType);
                map.put(hangingsignrenderer$modelkey, modelBase);
                return blockentityrenderer;
            }
        }
    }
}