package net.minecraft.client.resources.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.data.ModelData;

public class SimpleBakedModel implements BakedModel {
    public static final String PARTICLE_TEXTURE_REFERENCE = "particle";
    private final List<BakedQuad> unculledFaces;
    private final Map<Direction, List<BakedQuad>> culledFaces;
    private final boolean hasAmbientOcclusion;
    private final boolean isGui3d;
    private final boolean usesBlockLight;
    private final TextureAtlasSprite particleIcon;
    private final ItemTransforms transforms;
    private ResourceLocation modelLocation;
    protected final ChunkRenderTypeSet blockRenderTypes;

    public SimpleBakedModel(
        List<BakedQuad> pUnculledFaces,
        Map<Direction, List<BakedQuad>> pCulledFaces,
        boolean pHasAmbientOcclusion,
        boolean pUseBlockLight,
        boolean pIsGui3d,
        TextureAtlasSprite pParticleIcon,
        ItemTransforms pTransforms
    ) {
        this(pUnculledFaces, pCulledFaces, pHasAmbientOcclusion, pUseBlockLight, pIsGui3d, pParticleIcon, pTransforms, RenderTypeGroup.EMPTY);
    }

    public SimpleBakedModel(
        List<BakedQuad> generalQuadsIn,
        Map<Direction, List<BakedQuad>> faceQuadsIn,
        boolean aoIn,
        boolean blockLightIn,
        boolean gui3dIn,
        TextureAtlasSprite textureIn,
        ItemTransforms transformsIn,
        RenderTypeGroup renderTypes
    ) {
        this.unculledFaces = generalQuadsIn;
        this.culledFaces = faceQuadsIn;
        this.hasAmbientOcclusion = aoIn;
        this.isGui3d = gui3dIn;
        this.usesBlockLight = blockLightIn;
        this.particleIcon = textureIn;
        this.transforms = transformsIn;
        this.blockRenderTypes = !renderTypes.isEmpty() ? ChunkRenderTypeSet.of(renderTypes.block()) : null;
    }

    public static BakedModel bakeElements(
        List<BlockElement> pElements,
        TextureSlots pTextureSlots,
        SpriteGetter pSpriteGetter,
        ModelState pModelState,
        boolean pHasAmbientOcclusion,
        boolean pUseBlockLight,
        boolean pIsGui3d,
        ItemTransforms pTransforms
    ) {
        return bakeElements(pElements, pTextureSlots, pSpriteGetter, pModelState, pHasAmbientOcclusion, pUseBlockLight, pIsGui3d, pTransforms, null);
    }

    public static BakedModel bakeElements(
        List<BlockElement> elementsIn,
        TextureSlots slotsIn,
        SpriteGetter spritesIn,
        ModelState modelStateIn,
        boolean aoIn,
        boolean blockLightIn,
        boolean guiLightIn,
        ItemTransforms transformsIn,
        RenderTypeGroup renderType
    ) {
        TextureAtlasSprite textureatlassprite = findSprite(spritesIn, slotsIn, "particle");
        SimpleBakedModel.Builder simplebakedmodel$builder = new SimpleBakedModel.Builder(aoIn, blockLightIn, guiLightIn, transformsIn)
            .particle(textureatlassprite);

        for (BlockElement blockelement : elementsIn) {
            for (Direction direction : blockelement.faces.keySet()) {
                BlockElementFace blockelementface = blockelement.faces.get(direction);
                TextureAtlasSprite textureatlassprite1 = findSprite(spritesIn, slotsIn, blockelementface.texture());
                if (blockelementface.cullForDirection() == null) {
                    simplebakedmodel$builder.addUnculledFace(bakeFace(blockelement, blockelementface, textureatlassprite1, direction, modelStateIn));
                } else {
                    simplebakedmodel$builder.addCulledFace(
                        Direction.rotate(modelStateIn.getRotation().getMatrix(), blockelementface.cullForDirection()),
                        bakeFace(blockelement, blockelementface, textureatlassprite1, direction, modelStateIn)
                    );
                }
            }
        }

        if (renderType != null) {
            simplebakedmodel$builder.renderTypes(renderType);
        }

        return simplebakedmodel$builder.build();
    }

    private static BakedQuad bakeFace(
        BlockElement pElement, BlockElementFace pFace, TextureAtlasSprite pSprite, Direction pFacing, ModelState pTransform
    ) {
        return FaceBakery.bakeQuad(
            pElement.from, pElement.to, pFace, pSprite, pFacing, pTransform, pElement.rotation, pElement.shade, pElement.lightEmission
        );
    }

    private static TextureAtlasSprite findSprite(SpriteGetter pSpriteGetter, TextureSlots pTextureSlots, String pMaterial) {
        Material material = pTextureSlots.getMaterial(pMaterial);
        return material != null ? pSpriteGetter.get(material) : pSpriteGetter.reportMissingReference(pMaterial);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState p_235054_, @Nullable Direction p_235055_, RandomSource p_235056_) {
        return p_235055_ == null ? this.unculledFaces : this.culledFaces.get(p_235055_);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.hasAmbientOcclusion;
    }

    @Override
    public boolean isGui3d() {
        return this.isGui3d;
    }

    @Override
    public boolean usesBlockLight() {
        return this.usesBlockLight;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.particleIcon;
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.transforms;
    }

    public ResourceLocation getModelLocation() {
        return this.modelLocation;
    }

    public void setModelLocation(ResourceLocation modelLocation) {
        this.modelLocation = modelLocation;
    }

    @Override
    public String toString() {
        return this.modelLocation + "";
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return this.blockRenderTypes != null ? this.blockRenderTypes : BakedModel.super.getRenderTypes(state, rand, data);
    }

    public static class Builder {
        private final ImmutableList.Builder<BakedQuad> unculledFaces = ImmutableList.builder();
        private final EnumMap<Direction, ImmutableList.Builder<BakedQuad>> culledFaces = Maps.newEnumMap(Direction.class);
        private final boolean hasAmbientOcclusion;
        @Nullable
        private TextureAtlasSprite particleIcon;
        private final boolean usesBlockLight;
        private final boolean isGui3d;
        private final ItemTransforms transforms;
        private RenderTypeGroup renderTypes = RenderTypeGroup.EMPTY;

        public Builder(boolean pHasAmbientOcclusion, boolean pUseBlockLight, boolean pIsGui3d, ItemTransforms pTransforms) {
            this.hasAmbientOcclusion = pHasAmbientOcclusion;
            this.usesBlockLight = pUseBlockLight;
            this.isGui3d = pIsGui3d;
            this.transforms = pTransforms;

            for (Direction direction : Direction.values()) {
                this.culledFaces.put(direction, ImmutableList.builder());
            }
        }

        public SimpleBakedModel.Builder addCulledFace(Direction pFacing, BakedQuad pQuad) {
            this.culledFaces.get(pFacing).add(pQuad);
            return this;
        }

        public SimpleBakedModel.Builder addUnculledFace(BakedQuad pQuad) {
            this.unculledFaces.add(pQuad);
            return this;
        }

        public SimpleBakedModel.Builder particle(TextureAtlasSprite pParticleIcon) {
            this.particleIcon = pParticleIcon;
            return this;
        }

        public SimpleBakedModel.Builder item() {
            return this;
        }

        public SimpleBakedModel.Builder renderTypes(RenderTypeGroup renderTypes) {
            this.renderTypes = renderTypes;
            return this;
        }

        public BakedModel build() {
            if (this.particleIcon == null) {
                throw new RuntimeException("Missing particle!");
            } else {
                Map<Direction, List<BakedQuad>> map = Maps.transformValues(this.culledFaces, ImmutableList.Builder::build);
                return new SimpleBakedModel(
                    this.unculledFaces.build(),
                    new EnumMap<>(map),
                    this.hasAmbientOcclusion,
                    this.usesBlockLight,
                    this.isGui3d,
                    this.particleIcon,
                    this.transforms,
                    this.renderTypes
                );
            }
        }
    }
}