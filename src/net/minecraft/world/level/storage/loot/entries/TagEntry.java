package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class TagEntry extends LootPoolSingletonContainer {
    public static final MapCodec<TagEntry> CODEC = RecordCodecBuilder.mapCodec(
        p_297046_ -> p_297046_.group(
                    TagKey.codec(Registries.ITEM).fieldOf("name").forGetter(p_297052_ -> p_297052_.tag),
                    Codec.BOOL.fieldOf("expand").forGetter(p_297045_ -> p_297045_.expand)
                )
                .and(singletonFields(p_297046_))
                .apply(p_297046_, TagEntry::new)
    );
    private final TagKey<Item> tag;
    private final boolean expand;

    private TagEntry(
        TagKey<Item> pTag, boolean pExpand, int pWeight, int pQuality, List<LootItemCondition> pConditions, List<LootItemFunction> pFunctions
    ) {
        super(pWeight, pQuality, pConditions, pFunctions);
        this.tag = pTag;
        this.expand = pExpand;
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.TAG;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> p_79854_, LootContext p_79855_) {
        BuiltInRegistries.ITEM.getTagOrEmpty(this.tag).forEach(p_205094_ -> p_79854_.accept(new ItemStack((Holder<Item>)p_205094_)));
    }

    private boolean expandTag(LootContext pContext, Consumer<LootPoolEntry> pGeneratorConsumer) {
        if (!this.canRun(pContext)) {
            return false;
        } else {
            for (final Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(this.tag)) {
                pGeneratorConsumer.accept(new LootPoolSingletonContainer.EntryBase() {
                    @Override
                    public void createItemStack(Consumer<ItemStack> p_79869_, LootContext p_79870_) {
                        p_79869_.accept(new ItemStack(holder));
                    }
                });
            }

            return true;
        }
    }

    @Override
    public boolean expand(LootContext p_79861_, Consumer<LootPoolEntry> p_79862_) {
        return this.expand ? this.expandTag(p_79861_, p_79862_) : super.expand(p_79861_, p_79862_);
    }

    public static LootPoolSingletonContainer.Builder<?> tagContents(TagKey<Item> pTag) {
        return simpleBuilder((p_297054_, p_297055_, p_297056_, p_297057_) -> new TagEntry(pTag, false, p_297054_, p_297055_, p_297056_, p_297057_));
    }

    public static LootPoolSingletonContainer.Builder<?> expandTag(TagKey<Item> pTag) {
        return simpleBuilder((p_297048_, p_297049_, p_297050_, p_297051_) -> new TagEntry(pTag, true, p_297048_, p_297049_, p_297050_, p_297051_));
    }
}