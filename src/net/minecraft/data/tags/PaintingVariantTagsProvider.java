package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.decoration.PaintingVariants;

public class PaintingVariantTagsProvider extends TagsProvider<PaintingVariant> {
    public PaintingVariantTagsProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pProvider) {
        super(pOutput, Registries.PAINTING_VARIANT, pProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider p_256017_) {
        this.tag(PaintingVariantTags.PLACEABLE)
            .add(
                PaintingVariants.KEBAB,
                PaintingVariants.AZTEC,
                PaintingVariants.ALBAN,
                PaintingVariants.AZTEC2,
                PaintingVariants.BOMB,
                PaintingVariants.PLANT,
                PaintingVariants.WASTELAND,
                PaintingVariants.POOL,
                PaintingVariants.COURBET,
                PaintingVariants.SEA,
                PaintingVariants.SUNSET,
                PaintingVariants.CREEBET,
                PaintingVariants.WANDERER,
                PaintingVariants.GRAHAM,
                PaintingVariants.MATCH,
                PaintingVariants.BUST,
                PaintingVariants.STAGE,
                PaintingVariants.VOID,
                PaintingVariants.SKULL_AND_ROSES,
                PaintingVariants.WITHER,
                PaintingVariants.FIGHTERS,
                PaintingVariants.POINTER,
                PaintingVariants.PIGSCENE,
                PaintingVariants.BURNING_SKULL,
                PaintingVariants.SKELETON,
                PaintingVariants.DONKEY_KONG,
                PaintingVariants.BAROQUE,
                PaintingVariants.HUMBLE,
                PaintingVariants.MEDITATIVE,
                PaintingVariants.PRAIRIE_RIDE,
                PaintingVariants.UNPACKED,
                PaintingVariants.BACKYARD,
                PaintingVariants.BOUQUET,
                PaintingVariants.CAVEBIRD,
                PaintingVariants.CHANGING,
                PaintingVariants.COTAN,
                PaintingVariants.ENDBOSS,
                PaintingVariants.FERN,
                PaintingVariants.FINDING,
                PaintingVariants.LOWMIST,
                PaintingVariants.ORB,
                PaintingVariants.OWLEMONS,
                PaintingVariants.PASSAGE,
                PaintingVariants.POND,
                PaintingVariants.SUNFLOWERS,
                PaintingVariants.TIDES
            );
    }
}