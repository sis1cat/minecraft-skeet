package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.FlatLevelGeneratorPresetTags;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPresets;

public class FlatLevelGeneratorPresetTagsProvider extends TagsProvider<FlatLevelGeneratorPreset> {
    public FlatLevelGeneratorPresetTagsProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pProvider) {
        super(pOutput, Registries.FLAT_LEVEL_GENERATOR_PRESET, pProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider p_255741_) {
        this.tag(FlatLevelGeneratorPresetTags.VISIBLE)
            .add(FlatLevelGeneratorPresets.CLASSIC_FLAT)
            .add(FlatLevelGeneratorPresets.TUNNELERS_DREAM)
            .add(FlatLevelGeneratorPresets.WATER_WORLD)
            .add(FlatLevelGeneratorPresets.OVERWORLD)
            .add(FlatLevelGeneratorPresets.SNOWY_KINGDOM)
            .add(FlatLevelGeneratorPresets.BOTTOMLESS_PIT)
            .add(FlatLevelGeneratorPresets.DESERT)
            .add(FlatLevelGeneratorPresets.REDSTONE_READY)
            .add(FlatLevelGeneratorPresets.THE_VOID);
    }
}