package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;

public class ExplorationMapFunction extends LootItemConditionalFunction {
    public static final TagKey<Structure> DEFAULT_DESTINATION = StructureTags.ON_TREASURE_MAPS;
    public static final Holder<MapDecorationType> DEFAULT_DECORATION = MapDecorationTypes.WOODLAND_MANSION;
    public static final byte DEFAULT_ZOOM = 2;
    public static final int DEFAULT_SEARCH_RADIUS = 50;
    public static final boolean DEFAULT_SKIP_EXISTING = true;
    public static final MapCodec<ExplorationMapFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_298090_ -> commonFields(p_298090_)
                .and(
                    p_298090_.group(
                        TagKey.codec(Registries.STRUCTURE).optionalFieldOf("destination", DEFAULT_DESTINATION).forGetter(p_299700_ -> p_299700_.destination),
                        MapDecorationType.CODEC.optionalFieldOf("decoration", DEFAULT_DECORATION).forGetter(p_327569_ -> p_327569_.mapDecoration),
                        Codec.BYTE.optionalFieldOf("zoom", Byte.valueOf((byte)2)).forGetter(p_299686_ -> p_299686_.zoom),
                        Codec.INT.optionalFieldOf("search_radius", Integer.valueOf(50)).forGetter(p_300245_ -> p_300245_.searchRadius),
                        Codec.BOOL.optionalFieldOf("skip_existing_chunks", Boolean.valueOf(true)).forGetter(p_299770_ -> p_299770_.skipKnownStructures)
                    )
                )
                .apply(p_298090_, ExplorationMapFunction::new)
    );
    private final TagKey<Structure> destination;
    private final Holder<MapDecorationType> mapDecoration;
    private final byte zoom;
    private final int searchRadius;
    private final boolean skipKnownStructures;

    ExplorationMapFunction(
        List<LootItemCondition> pConditons, TagKey<Structure> pDestination, Holder<MapDecorationType> pMapDecoration, byte pZoom, int pSearchRadius, boolean pSkipKnownStructures
    ) {
        super(pConditons);
        this.destination = pDestination;
        this.mapDecoration = pMapDecoration;
        this.zoom = pZoom;
        this.searchRadius = pSearchRadius;
        this.skipKnownStructures = pSkipKnownStructures;
    }

    @Override
    public LootItemFunctionType<ExplorationMapFunction> getType() {
        return LootItemFunctions.EXPLORATION_MAP;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.ORIGIN);
    }

    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        if (!pStack.is(Items.MAP)) {
            return pStack;
        } else {
            Vec3 vec3 = pContext.getOptionalParameter(LootContextParams.ORIGIN);
            if (vec3 != null) {
                ServerLevel serverlevel = pContext.getLevel();
                BlockPos blockpos = serverlevel.findNearestMapStructure(this.destination, BlockPos.containing(vec3), this.searchRadius, this.skipKnownStructures);
                if (blockpos != null) {
                    ItemStack itemstack = MapItem.create(serverlevel, blockpos.getX(), blockpos.getZ(), this.zoom, true, true);
                    MapItem.renderBiomePreviewMap(serverlevel, itemstack);
                    MapItemSavedData.addTargetDecoration(itemstack, blockpos, "+", this.mapDecoration);
                    return itemstack;
                }
            }

            return pStack;
        }
    }

    public static ExplorationMapFunction.Builder makeExplorationMap() {
        return new ExplorationMapFunction.Builder();
    }

    public static class Builder extends LootItemConditionalFunction.Builder<ExplorationMapFunction.Builder> {
        private TagKey<Structure> destination = ExplorationMapFunction.DEFAULT_DESTINATION;
        private Holder<MapDecorationType> mapDecoration = ExplorationMapFunction.DEFAULT_DECORATION;
        private byte zoom = 2;
        private int searchRadius = 50;
        private boolean skipKnownStructures = true;

        protected ExplorationMapFunction.Builder getThis() {
            return this;
        }

        public ExplorationMapFunction.Builder setDestination(TagKey<Structure> pDestination) {
            this.destination = pDestination;
            return this;
        }

        public ExplorationMapFunction.Builder setMapDecoration(Holder<MapDecorationType> pMapDecoration) {
            this.mapDecoration = pMapDecoration;
            return this;
        }

        public ExplorationMapFunction.Builder setZoom(byte pZoom) {
            this.zoom = pZoom;
            return this;
        }

        public ExplorationMapFunction.Builder setSearchRadius(int pSearchRadius) {
            this.searchRadius = pSearchRadius;
            return this;
        }

        public ExplorationMapFunction.Builder setSkipKnownStructures(boolean pSkipKnownStructures) {
            this.skipKnownStructures = pSkipKnownStructures;
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new ExplorationMapFunction(this.getConditions(), this.destination, this.mapDecoration, this.zoom, this.searchRadius, this.skipKnownStructures);
        }
    }
}