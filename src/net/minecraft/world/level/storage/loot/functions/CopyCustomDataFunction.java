package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProvider;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProviders;
import org.apache.commons.lang3.mutable.MutableObject;

public class CopyCustomDataFunction extends LootItemConditionalFunction {
    public static final MapCodec<CopyCustomDataFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_334162_ -> commonFields(p_334162_)
                .and(
                    p_334162_.group(
                        NbtProviders.CODEC.fieldOf("source").forGetter(p_330558_ -> p_330558_.source),
                        CopyCustomDataFunction.CopyOperation.CODEC.listOf().fieldOf("ops").forGetter(p_327675_ -> p_327675_.operations)
                    )
                )
                .apply(p_334162_, CopyCustomDataFunction::new)
    );
    private final NbtProvider source;
    private final List<CopyCustomDataFunction.CopyOperation> operations;

    CopyCustomDataFunction(List<LootItemCondition> pConditions, NbtProvider pSource, List<CopyCustomDataFunction.CopyOperation> pOperations) {
        super(pConditions);
        this.source = pSource;
        this.operations = List.copyOf(pOperations);
    }

    @Override
    public LootItemFunctionType<CopyCustomDataFunction> getType() {
        return LootItemFunctions.COPY_CUSTOM_DATA;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return this.source.getReferencedContextParams();
    }

    @Override
    public ItemStack run(ItemStack p_333117_, LootContext p_334578_) {
        Tag tag = this.source.get(p_334578_);
        if (tag == null) {
            return p_333117_;
        } else {
            MutableObject<CompoundTag> mutableobject = new MutableObject<>();
            Supplier<Tag> supplier = () -> {
                if (mutableobject.getValue() == null) {
                    mutableobject.setValue(p_333117_.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
                }

                return mutableobject.getValue();
            };
            this.operations.forEach(p_329887_ -> p_329887_.apply(supplier, tag));
            CompoundTag compoundtag = mutableobject.getValue();
            if (compoundtag != null) {
                CustomData.set(DataComponents.CUSTOM_DATA, p_333117_, compoundtag);
            }

            return p_333117_;
        }
    }

    @Deprecated
    public static CopyCustomDataFunction.Builder copyData(NbtProvider pSource) {
        return new CopyCustomDataFunction.Builder(pSource);
    }

    public static CopyCustomDataFunction.Builder copyData(LootContext.EntityTarget pTarget) {
        return new CopyCustomDataFunction.Builder(ContextNbtProvider.forContextEntity(pTarget));
    }

    public static class Builder extends LootItemConditionalFunction.Builder<CopyCustomDataFunction.Builder> {
        private final NbtProvider source;
        private final List<CopyCustomDataFunction.CopyOperation> ops = Lists.newArrayList();

        Builder(NbtProvider pSource) {
            this.source = pSource;
        }

        public CopyCustomDataFunction.Builder copy(String pSourceKey, String pDestinationKey, CopyCustomDataFunction.MergeStrategy pMergeStrategy) {
            try {
                this.ops
                    .add(
                        new CopyCustomDataFunction.CopyOperation(
                            NbtPathArgument.NbtPath.of(pSourceKey), NbtPathArgument.NbtPath.of(pDestinationKey), pMergeStrategy
                        )
                    );
                return this;
            } catch (CommandSyntaxException commandsyntaxexception) {
                throw new IllegalArgumentException(commandsyntaxexception);
            }
        }

        public CopyCustomDataFunction.Builder copy(String pSourceKey, String pDestinationKey) {
            return this.copy(pSourceKey, pDestinationKey, CopyCustomDataFunction.MergeStrategy.REPLACE);
        }

        protected CopyCustomDataFunction.Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new CopyCustomDataFunction(this.getConditions(), this.source, this.ops);
        }
    }

    static record CopyOperation(NbtPathArgument.NbtPath sourcePath, NbtPathArgument.NbtPath targetPath, CopyCustomDataFunction.MergeStrategy op) {
        public static final Codec<CopyCustomDataFunction.CopyOperation> CODEC = RecordCodecBuilder.create(
            p_333172_ -> p_333172_.group(
                        NbtPathArgument.NbtPath.CODEC.fieldOf("source").forGetter(CopyCustomDataFunction.CopyOperation::sourcePath),
                        NbtPathArgument.NbtPath.CODEC.fieldOf("target").forGetter(CopyCustomDataFunction.CopyOperation::targetPath),
                        CopyCustomDataFunction.MergeStrategy.CODEC.fieldOf("op").forGetter(CopyCustomDataFunction.CopyOperation::op)
                    )
                    .apply(p_333172_, CopyCustomDataFunction.CopyOperation::new)
        );

        public void apply(Supplier<Tag> pSourceTag, Tag pTag) {
            try {
                List<Tag> list = this.sourcePath.get(pTag);
                if (!list.isEmpty()) {
                    this.op.merge(pSourceTag.get(), this.targetPath, list);
                }
            } catch (CommandSyntaxException commandsyntaxexception) {
            }
        }
    }

    public static enum MergeStrategy implements StringRepresentable {
        REPLACE("replace") {
            @Override
            public void merge(Tag p_327968_, NbtPathArgument.NbtPath p_329545_, List<Tag> p_330977_) throws CommandSyntaxException {
                p_329545_.set(p_327968_, Iterables.getLast(p_330977_));
            }
        },
        APPEND("append") {
            @Override
            public void merge(Tag p_334866_, NbtPathArgument.NbtPath p_330111_, List<Tag> p_331184_) throws CommandSyntaxException {
                List<Tag> list = p_330111_.getOrCreate(p_334866_, ListTag::new);
                list.forEach(p_328852_ -> {
                    if (p_328852_ instanceof ListTag) {
                        p_331184_.forEach(p_333613_ -> ((ListTag)p_328852_).add(p_333613_.copy()));
                    }
                });
            }
        },
        MERGE("merge") {
            @Override
            public void merge(Tag p_330874_, NbtPathArgument.NbtPath p_329263_, List<Tag> p_336007_) throws CommandSyntaxException {
                List<Tag> list = p_329263_.getOrCreate(p_330874_, CompoundTag::new);
                list.forEach(p_328276_ -> {
                    if (p_328276_ instanceof CompoundTag) {
                        p_336007_.forEach(p_330167_ -> {
                            if (p_330167_ instanceof CompoundTag) {
                                ((CompoundTag)p_328276_).merge((CompoundTag)p_330167_);
                            }
                        });
                    }
                });
            }
        };

        public static final Codec<CopyCustomDataFunction.MergeStrategy> CODEC = StringRepresentable.fromEnum(CopyCustomDataFunction.MergeStrategy::values);
        private final String name;

        public abstract void merge(Tag pTag, NbtPathArgument.NbtPath pPath, List<Tag> pCurrentData) throws CommandSyntaxException;

        MergeStrategy(final String pName) {
            this.name = pName;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}