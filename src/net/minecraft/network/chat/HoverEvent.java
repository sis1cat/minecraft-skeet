package net.minecraft.network.chat;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class HoverEvent {
    public static final Codec<HoverEvent> CODEC = Codec.withAlternative(
            HoverEvent.TypedHoverEvent.CODEC.codec(), HoverEvent.TypedHoverEvent.LEGACY_CODEC.codec()
        )
        .xmap(HoverEvent::new, p_326065_ -> p_326065_.event);
    private final HoverEvent.TypedHoverEvent<?> event;

    public <T> HoverEvent(HoverEvent.Action<T> pAction, T pValue) {
        this(new HoverEvent.TypedHoverEvent<>(pAction, pValue));
    }

    private HoverEvent(HoverEvent.TypedHoverEvent<?> pEvent) {
        this.event = pEvent;
    }

    public HoverEvent.Action<?> getAction() {
        return this.event.action;
    }

    @Nullable
    public <T> T getValue(HoverEvent.Action<T> pActionType) {
        return this.event.action == pActionType ? pActionType.cast(this.event.value) : null;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return pOther != null && this.getClass() == pOther.getClass() ? ((HoverEvent)pOther).event.equals(this.event) : false;
        }
    }

    @Override
    public String toString() {
        return this.event.toString();
    }

    @Override
    public int hashCode() {
        return this.event.hashCode();
    }

    public static class Action<T> implements StringRepresentable {
        public static final HoverEvent.Action<Component> SHOW_TEXT = new HoverEvent.Action<>(
            "show_text", true, ComponentSerialization.CODEC, (p_326066_, p_326067_) -> DataResult.success(p_326066_)
        );
        public static final HoverEvent.Action<HoverEvent.ItemStackInfo> SHOW_ITEM = new HoverEvent.Action<>(
            "show_item", true, HoverEvent.ItemStackInfo.CODEC, HoverEvent.ItemStackInfo::legacyCreate
        );
        public static final HoverEvent.Action<HoverEvent.EntityTooltipInfo> SHOW_ENTITY = new HoverEvent.Action<>(
            "show_entity", true, HoverEvent.EntityTooltipInfo.CODEC, HoverEvent.EntityTooltipInfo::legacyCreate
        );
        public static final Codec<HoverEvent.Action<?>> UNSAFE_CODEC = StringRepresentable.fromValues(
            () -> new HoverEvent.Action[]{SHOW_TEXT, SHOW_ITEM, SHOW_ENTITY}
        );
        public static final Codec<HoverEvent.Action<?>> CODEC = UNSAFE_CODEC.validate(HoverEvent.Action::filterForSerialization);
        private final String name;
        private final boolean allowFromServer;
        final MapCodec<HoverEvent.TypedHoverEvent<T>> codec;
        final MapCodec<HoverEvent.TypedHoverEvent<T>> legacyCodec;

        public Action(String pName, boolean pAllowFromServer, Codec<T> pCodec, final HoverEvent.LegacyConverter<T> pLegacyConverter) {
            this.name = pName;
            this.allowFromServer = pAllowFromServer;
            this.codec = pCodec.xmap(p_308563_ -> new HoverEvent.TypedHoverEvent<>(this, (T)p_308563_), p_308564_ -> p_308564_.value)
                .fieldOf("contents");
            this.legacyCodec = (new Codec<HoverEvent.TypedHoverEvent<T>>() {
                @Override
                public <D> DataResult<Pair<HoverEvent.TypedHoverEvent<T>, D>> decode(DynamicOps<D> p_333314_, D p_328005_) {
                    return ComponentSerialization.CODEC.decode(p_333314_, p_328005_).flatMap(p_332506_ -> {
                        DataResult<T> dataresult;
                        if (p_333314_ instanceof RegistryOps<D> registryops) {
                            dataresult = pLegacyConverter.parse(p_332506_.getFirst(), registryops);
                        } else {
                            dataresult = pLegacyConverter.parse(p_332506_.getFirst(), null);
                        }

                        return dataresult.map(p_335904_ -> Pair.of(new HoverEvent.TypedHoverEvent<>(Action.this, (T)p_335904_), (D)p_332506_.getSecond()));
                    });
                }

                public <D> DataResult<D> encode(HoverEvent.TypedHoverEvent<T> p_328207_, DynamicOps<D> p_331514_, D p_330476_) {
                    return DataResult.error(() -> "Can't encode in legacy format");
                }
            }).fieldOf("value");
        }

        public boolean isAllowedFromServer() {
            return this.allowFromServer;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        T cast(Object pParameter) {
            return (T)pParameter;
        }

        @Override
        public String toString() {
            return "<action " + this.name + ">";
        }

        private static DataResult<HoverEvent.Action<?>> filterForSerialization(@Nullable HoverEvent.Action<?> pAction) {
            if (pAction == null) {
                return DataResult.error(() -> "Unknown action");
            } else {
                return !pAction.isAllowedFromServer() ? DataResult.error(() -> "Action not allowed: " + pAction) : DataResult.success(pAction, Lifecycle.stable());
            }
        }
    }

    public static class EntityTooltipInfo {
        public static final Codec<HoverEvent.EntityTooltipInfo> CODEC = RecordCodecBuilder.create(
            p_358478_ -> p_358478_.group(
                        BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(p_309982_ -> p_309982_.type),
                        UUIDUtil.LENIENT_CODEC.fieldOf("id").forGetter(p_312795_ -> p_312795_.id),
                        ComponentSerialization.CODEC.lenientOptionalFieldOf("name").forGetter(p_310270_ -> p_310270_.name)
                    )
                    .apply(p_358478_, HoverEvent.EntityTooltipInfo::new)
        );
        public final EntityType<?> type;
        public final UUID id;
        public final Optional<Component> name;
        @Nullable
        private List<Component> linesCache;

        public EntityTooltipInfo(EntityType<?> pType, UUID pId, @Nullable Component pName) {
            this(pType, pId, Optional.ofNullable(pName));
        }

        public EntityTooltipInfo(EntityType<?> pType, UUID pId, Optional<Component> pName) {
            this.type = pType;
            this.id = pId;
            this.name = pName;
        }

        public static DataResult<HoverEvent.EntityTooltipInfo> legacyCreate(Component pName, @Nullable RegistryOps<?> pOps) {
            try {
                CompoundTag compoundtag = TagParser.parseTag(pName.getString());
                DynamicOps<JsonElement> dynamicops = (DynamicOps<JsonElement>)(pOps != null ? pOps.withParent(JsonOps.INSTANCE) : JsonOps.INSTANCE);
                DataResult<Component> dataresult = ComponentSerialization.CODEC.parse(dynamicops, JsonParser.parseString(compoundtag.getString("name")));
                EntityType<?> entitytype = BuiltInRegistries.ENTITY_TYPE.getValue(ResourceLocation.parse(compoundtag.getString("type")));
                UUID uuid = UUID.fromString(compoundtag.getString("id"));
                return dataresult.map(p_326072_ -> new HoverEvent.EntityTooltipInfo(entitytype, uuid, p_326072_));
            } catch (Exception exception) {
                return DataResult.error(() -> "Failed to parse tooltip: " + exception.getMessage());
            }
        }

        public List<Component> getTooltipLines() {
            if (this.linesCache == null) {
                this.linesCache = new ArrayList<>();
                this.name.ifPresent(this.linesCache::add);
                this.linesCache.add(Component.translatable("gui.entity_tooltip.type", this.type.getDescription()));
                this.linesCache.add(Component.literal(this.id.toString()));
            }

            return this.linesCache;
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) {
                return true;
            } else if (pOther != null && this.getClass() == pOther.getClass()) {
                HoverEvent.EntityTooltipInfo hoverevent$entitytooltipinfo = (HoverEvent.EntityTooltipInfo)pOther;
                return this.type.equals(hoverevent$entitytooltipinfo.type)
                    && this.id.equals(hoverevent$entitytooltipinfo.id)
                    && this.name.equals(hoverevent$entitytooltipinfo.name);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int i = this.type.hashCode();
            i = 31 * i + this.id.hashCode();
            return 31 * i + this.name.hashCode();
        }
    }

    public static class ItemStackInfo {
        public static final Codec<HoverEvent.ItemStackInfo> FULL_CODEC = ItemStack.CODEC
            .xmap(HoverEvent.ItemStackInfo::new, HoverEvent.ItemStackInfo::getItemStack);
        private static final Codec<HoverEvent.ItemStackInfo> SIMPLE_CODEC = ItemStack.SIMPLE_ITEM_CODEC
            .xmap(HoverEvent.ItemStackInfo::new, HoverEvent.ItemStackInfo::getItemStack);
        public static final Codec<HoverEvent.ItemStackInfo> CODEC = Codec.withAlternative(FULL_CODEC, SIMPLE_CODEC);
        private final Holder<Item> item;
        private final int count;
        private final DataComponentPatch components;
        @Nullable
        private ItemStack itemStack;

        ItemStackInfo(Holder<Item> pItem, int pCount, DataComponentPatch pComponents) {
            this.item = pItem;
            this.count = pCount;
            this.components = pComponents;
        }

        public ItemStackInfo(ItemStack pStack) {
            this(pStack.getItemHolder(), pStack.getCount(), pStack.getComponentsPatch());
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) {
                return true;
            } else if (pOther != null && this.getClass() == pOther.getClass()) {
                HoverEvent.ItemStackInfo hoverevent$itemstackinfo = (HoverEvent.ItemStackInfo)pOther;
                return this.count == hoverevent$itemstackinfo.count
                    && this.item.equals(hoverevent$itemstackinfo.item)
                    && this.components.equals(hoverevent$itemstackinfo.components);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int i = this.item.hashCode();
            i = 31 * i + this.count;
            return 31 * i + this.components.hashCode();
        }

        public ItemStack getItemStack() {
            if (this.itemStack == null) {
                this.itemStack = new ItemStack(this.item, this.count, this.components);
            }

            return this.itemStack;
        }

        private static DataResult<HoverEvent.ItemStackInfo> legacyCreate(Component pName, @Nullable RegistryOps<?> pOps) {
            try {
                CompoundTag compoundtag = TagParser.parseTag(pName.getString());
                DynamicOps<Tag> dynamicops = (DynamicOps<Tag>)(pOps != null ? pOps.withParent(NbtOps.INSTANCE) : NbtOps.INSTANCE);
                return ItemStack.CODEC.parse(dynamicops, compoundtag).map(HoverEvent.ItemStackInfo::new);
            } catch (CommandSyntaxException commandsyntaxexception) {
                return DataResult.error(() -> "Failed to parse item tag: " + commandsyntaxexception.getMessage());
            }
        }
    }

    public interface LegacyConverter<T> {
        DataResult<T> parse(Component pName, @Nullable RegistryOps<?> pOps);
    }

    static record TypedHoverEvent<T>(HoverEvent.Action<T> action, T value) {
        public static final MapCodec<HoverEvent.TypedHoverEvent<?>> CODEC = HoverEvent.Action.CODEC
            .dispatchMap("action", HoverEvent.TypedHoverEvent::action, p_326074_ -> p_326074_.codec);
        public static final MapCodec<HoverEvent.TypedHoverEvent<?>> LEGACY_CODEC = HoverEvent.Action.CODEC
            .dispatchMap("action", HoverEvent.TypedHoverEvent::action, p_326073_ -> p_326073_.legacyCodec);
    }
}