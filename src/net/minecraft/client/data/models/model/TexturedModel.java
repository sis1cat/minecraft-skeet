package net.minecraft.client.data.models.model;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TexturedModel {
    public static final TexturedModel.Provider CUBE = createDefault(TextureMapping::cube, ModelTemplates.CUBE_ALL);
    public static final TexturedModel.Provider CUBE_INNER_FACES = createDefault(TextureMapping::cube, ModelTemplates.CUBE_ALL_INNER_FACES);
    public static final TexturedModel.Provider CUBE_MIRRORED = createDefault(TextureMapping::cube, ModelTemplates.CUBE_MIRRORED_ALL);
    public static final TexturedModel.Provider COLUMN = createDefault(TextureMapping::column, ModelTemplates.CUBE_COLUMN);
    public static final TexturedModel.Provider COLUMN_HORIZONTAL = createDefault(TextureMapping::column, ModelTemplates.CUBE_COLUMN_HORIZONTAL);
    public static final TexturedModel.Provider CUBE_TOP_BOTTOM = createDefault(TextureMapping::cubeBottomTop, ModelTemplates.CUBE_BOTTOM_TOP);
    public static final TexturedModel.Provider CUBE_TOP = createDefault(TextureMapping::cubeTop, ModelTemplates.CUBE_TOP);
    public static final TexturedModel.Provider ORIENTABLE_ONLY_TOP = createDefault(TextureMapping::orientableCubeOnlyTop, ModelTemplates.CUBE_ORIENTABLE);
    public static final TexturedModel.Provider ORIENTABLE = createDefault(TextureMapping::orientableCube, ModelTemplates.CUBE_ORIENTABLE_TOP_BOTTOM);
    public static final TexturedModel.Provider CARPET = createDefault(TextureMapping::wool, ModelTemplates.CARPET);
    public static final TexturedModel.Provider MOSSY_CARPET_SIDE = createDefault(TextureMapping::side, ModelTemplates.MOSSY_CARPET_SIDE);
    public static final TexturedModel.Provider FLOWERBED_1 = createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_1);
    public static final TexturedModel.Provider FLOWERBED_2 = createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_2);
    public static final TexturedModel.Provider FLOWERBED_3 = createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_3);
    public static final TexturedModel.Provider FLOWERBED_4 = createDefault(TextureMapping::flowerbed, ModelTemplates.FLOWERBED_4);
    public static final TexturedModel.Provider GLAZED_TERRACOTTA = createDefault(TextureMapping::pattern, ModelTemplates.GLAZED_TERRACOTTA);
    public static final TexturedModel.Provider CORAL_FAN = createDefault(TextureMapping::fan, ModelTemplates.CORAL_FAN);
    public static final TexturedModel.Provider ANVIL = createDefault(TextureMapping::top, ModelTemplates.ANVIL);
    public static final TexturedModel.Provider LEAVES = createDefault(TextureMapping::cube, ModelTemplates.LEAVES);
    public static final TexturedModel.Provider LANTERN = createDefault(TextureMapping::lantern, ModelTemplates.LANTERN);
    public static final TexturedModel.Provider HANGING_LANTERN = createDefault(TextureMapping::lantern, ModelTemplates.HANGING_LANTERN);
    public static final TexturedModel.Provider SEAGRASS = createDefault(TextureMapping::defaultTexture, ModelTemplates.SEAGRASS);
    public static final TexturedModel.Provider COLUMN_ALT = createDefault(TextureMapping::logColumn, ModelTemplates.CUBE_COLUMN);
    public static final TexturedModel.Provider COLUMN_HORIZONTAL_ALT = createDefault(TextureMapping::logColumn, ModelTemplates.CUBE_COLUMN_HORIZONTAL);
    public static final TexturedModel.Provider TOP_BOTTOM_WITH_WALL = createDefault(TextureMapping::cubeBottomTopWithWall, ModelTemplates.CUBE_BOTTOM_TOP);
    public static final TexturedModel.Provider COLUMN_WITH_WALL = createDefault(TextureMapping::columnWithWall, ModelTemplates.CUBE_COLUMN);
    private final TextureMapping mapping;
    private final ModelTemplate template;

    private TexturedModel(TextureMapping pMapping, ModelTemplate pTemplate) {
        this.mapping = pMapping;
        this.template = pTemplate;
    }

    public ModelTemplate getTemplate() {
        return this.template;
    }

    public TextureMapping getMapping() {
        return this.mapping;
    }

    public TexturedModel updateTextures(Consumer<TextureMapping> pUpdater) {
        pUpdater.accept(this.mapping);
        return this;
    }

    public ResourceLocation create(Block pBlock, BiConsumer<ResourceLocation, ModelInstance> pOutput) {
        return this.template.create(pBlock, this.mapping, pOutput);
    }

    public ResourceLocation createWithSuffix(Block pBlock, String pSuffix, BiConsumer<ResourceLocation, ModelInstance> pOutput) {
        return this.template.createWithSuffix(pBlock, pSuffix, this.mapping, pOutput);
    }

    private static TexturedModel.Provider createDefault(Function<Block, TextureMapping> pTextureMappingGetter, ModelTemplate pTemplate) {
        return p_376579_ -> new TexturedModel(pTextureMappingGetter.apply(p_376579_), pTemplate);
    }

    public static TexturedModel createAllSame(ResourceLocation pLocation) {
        return new TexturedModel(TextureMapping.cube(pLocation), ModelTemplates.CUBE_ALL);
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface Provider {
        TexturedModel get(Block pBlock);

        default ResourceLocation create(Block pBlock, BiConsumer<ResourceLocation, ModelInstance> pOutput) {
            return this.get(pBlock).create(pBlock, pOutput);
        }

        default ResourceLocation createWithSuffix(Block pBlock, String pSuffix, BiConsumer<ResourceLocation, ModelInstance> pOutput) {
            return this.get(pBlock).createWithSuffix(pBlock, pSuffix, pOutput);
        }

        default TexturedModel.Provider updateTexture(Consumer<TextureMapping> pUpdater) {
            return p_378681_ -> this.get(p_378681_).updateTextures(pUpdater);
        }
    }
}