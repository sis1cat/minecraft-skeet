package net.minecraft.client.renderer.block.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockModel implements UnbakedModel {
    @VisibleForTesting
    static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(BlockModel.class, new BlockModel.Deserializer())
        .registerTypeAdapter(BlockElement.class, new BlockElement.Deserializer())
        .registerTypeAdapter(BlockElementFace.class, new BlockElementFace.Deserializer())
        .registerTypeAdapter(BlockFaceUV.class, new BlockFaceUV.Deserializer())
        .registerTypeAdapter(ItemTransform.class, new ItemTransform.Deserializer())
        .registerTypeAdapter(ItemTransforms.class, new ItemTransforms.Deserializer())
        .create();
    private final List<BlockElement> elements;
    @Nullable
    private final UnbakedModel.GuiLight guiLight;
    @Nullable
    private final Boolean hasAmbientOcclusion;
    @Nullable
    private final ItemTransforms transforms;
    @VisibleForTesting
    private final TextureSlots.Data textureSlots;
    @Nullable
    private UnbakedModel parent;
    @Nullable
    private final ResourceLocation parentLocation;

    public static BlockModel fromStream(Reader pReader) {
        return GsonHelper.fromJson(GSON, pReader, BlockModel.class);
    }

    public BlockModel(
        @Nullable ResourceLocation pParentLocation,
        List<BlockElement> pElements,
        TextureSlots.Data pTextureSlots,
        @Nullable Boolean pHasAmbientOcclusion,
        @Nullable UnbakedModel.GuiLight pGuiLight,
        @Nullable ItemTransforms pTransforms
    ) {
        this.elements = pElements;
        this.hasAmbientOcclusion = pHasAmbientOcclusion;
        this.guiLight = pGuiLight;
        this.textureSlots = pTextureSlots;
        this.parentLocation = pParentLocation;
        this.transforms = pTransforms;
    }

    @Nullable
    @Override
    public Boolean getAmbientOcclusion() {
        return this.hasAmbientOcclusion;
    }

    @Nullable
    @Override
    public UnbakedModel.GuiLight getGuiLight() {
        return this.guiLight;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver p_377823_) {
        if (this.parentLocation != null) {
            this.parent = p_377823_.resolve(this.parentLocation);
        }
    }

    @Nullable
    @Override
    public UnbakedModel getParent() {
        return this.parent;
    }

    @Override
    public TextureSlots.Data getTextureSlots() {
        return this.textureSlots;
    }

    @Nullable
    @Override
    public ItemTransforms getTransforms() {
        return this.transforms;
    }

    @Override
    public BakedModel bake(
        TextureSlots p_378598_, ModelBaker p_378458_, ModelState p_111453_, boolean p_111455_, boolean p_377435_, ItemTransforms p_378085_
    ) {
        return this.elements.isEmpty() && this.parent != null
            ? this.parent.bake(p_378598_, p_378458_, p_111453_, p_111455_, p_377435_, p_378085_)
            : SimpleBakedModel.bakeElements(this.elements, p_378598_, p_378458_.sprites(), p_111453_, p_111455_, p_377435_, true, p_378085_);
    }

    @Nullable
    @VisibleForTesting
    List<BlockElement> getElements() {
        return this.elements;
    }

    @Nullable
    @VisibleForTesting
    ResourceLocation getParentLocation() {
        return this.parentLocation;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Deserializer implements JsonDeserializer<BlockModel> {
        public BlockModel deserialize(JsonElement pJson, Type pType, JsonDeserializationContext pContext) throws JsonParseException {
            JsonObject jsonobject = pJson.getAsJsonObject();
            List<BlockElement> list = this.getElements(pContext, jsonobject);
            String s = this.getParentName(jsonobject);
            TextureSlots.Data textureslots$data = this.getTextureMap(jsonobject);
            Boolean obool = this.getAmbientOcclusion(jsonobject);
            ItemTransforms itemtransforms = null;
            if (jsonobject.has("display")) {
                JsonObject jsonobject1 = GsonHelper.getAsJsonObject(jsonobject, "display");
                itemtransforms = pContext.deserialize(jsonobject1, ItemTransforms.class);
            }

            UnbakedModel.GuiLight unbakedmodel$guilight = null;
            if (jsonobject.has("gui_light")) {
                unbakedmodel$guilight = UnbakedModel.GuiLight.getByName(GsonHelper.getAsString(jsonobject, "gui_light"));
            }

            ResourceLocation resourcelocation = s.isEmpty() ? null : ResourceLocation.parse(s);
            return new BlockModel(resourcelocation, list, textureslots$data, obool, unbakedmodel$guilight, itemtransforms);
        }

        private TextureSlots.Data getTextureMap(JsonObject pJson) {
            if (pJson.has("textures")) {
                JsonObject jsonobject = GsonHelper.getAsJsonObject(pJson, "textures");
                return TextureSlots.parseTextureMap(jsonobject, TextureAtlas.LOCATION_BLOCKS);
            } else {
                return TextureSlots.Data.EMPTY;
            }
        }

        private String getParentName(JsonObject pJson) {
            return GsonHelper.getAsString(pJson, "parent", "");
        }

        @Nullable
        protected Boolean getAmbientOcclusion(JsonObject pJson) {
            return pJson.has("ambientocclusion") ? GsonHelper.getAsBoolean(pJson, "ambientocclusion") : null;
        }

        protected List<BlockElement> getElements(JsonDeserializationContext pContext, JsonObject pJson) {
            if (!pJson.has("elements")) {
                return List.of();
            } else {
                List<BlockElement> list = new ArrayList<>();

                for (JsonElement jsonelement : GsonHelper.getAsJsonArray(pJson, "elements")) {
                    list.add(pContext.deserialize(jsonelement, BlockElement.class));
                }

                return list;
            }
        }
    }
}