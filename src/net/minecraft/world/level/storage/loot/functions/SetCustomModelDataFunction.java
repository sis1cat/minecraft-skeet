package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetCustomModelDataFunction extends LootItemConditionalFunction {
    private static final Codec<NumberProvider> COLOR_PROVIDER_CODEC = Codec.withAlternative(NumberProviders.CODEC, ExtraCodecs.RGB_COLOR_CODEC, value -> new ConstantValue(value));
    public static final MapCodec<SetCustomModelDataFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_375377_ -> commonFields(p_375377_)
                .and(
                    p_375377_.group(
                        ListOperation.StandAlone.codec(NumberProviders.CODEC, Integer.MAX_VALUE)
                            .optionalFieldOf("floats")
                            .forGetter(p_375379_ -> p_375379_.floats),
                        ListOperation.StandAlone.codec(Codec.BOOL, Integer.MAX_VALUE).optionalFieldOf("flags").forGetter(p_375382_ -> p_375382_.flags),
                        ListOperation.StandAlone.codec(Codec.STRING, Integer.MAX_VALUE)
                            .optionalFieldOf("strings")
                            .forGetter(p_375372_ -> p_375372_.strings),
                        ListOperation.StandAlone.codec(COLOR_PROVIDER_CODEC, Integer.MAX_VALUE).optionalFieldOf("colors").forGetter(p_375380_ -> p_375380_.colors)
                    )
                )
                .apply(p_375377_, SetCustomModelDataFunction::new)
    );
    private final Optional<ListOperation.StandAlone<NumberProvider>> floats;
    private final Optional<ListOperation.StandAlone<Boolean>> flags;
    private final Optional<ListOperation.StandAlone<String>> strings;
    private final Optional<ListOperation.StandAlone<NumberProvider>> colors;

    public SetCustomModelDataFunction(
        List<LootItemCondition> pConditions,
        Optional<ListOperation.StandAlone<NumberProvider>> pFloats,
        Optional<ListOperation.StandAlone<Boolean>> pFlags,
        Optional<ListOperation.StandAlone<String>> pStrings,
        Optional<ListOperation.StandAlone<NumberProvider>> pColors
    ) {
        super(pConditions);
        this.floats = pFloats;
        this.flags = pFlags;
        this.strings = pStrings;
        this.colors = pColors;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Stream.concat(this.floats.stream(), this.colors.stream())
            .flatMap(p_375378_ -> p_375378_.value().stream())
            .flatMap(p_375381_ -> p_375381_.getReferencedContextParams().stream())
            .collect(Collectors.toSet());
    }

    @Override
    public LootItemFunctionType<SetCustomModelDataFunction> getType() {
        return LootItemFunctions.SET_CUSTOM_MODEL_DATA;
    }

    private static <T> List<T> apply(Optional<ListOperation.StandAlone<T>> pValue, List<T> pList) {
        return pValue.<List<T>>map(p_375376_ -> p_375376_.apply(pList)).orElse(pList);
    }

    private static <T, E> List<E> apply(Optional<ListOperation.StandAlone<T>> pValue, List<E> pList, Function<T, E> pConverter) {
        return pValue.<List<E>>map(p_375385_ -> {
            List<E> list = p_375385_.value().stream().map(pConverter).toList();
            return p_375385_.operation().apply(pList, list);
        }).orElse(pList);
    }

    @Override
    public ItemStack run(ItemStack p_328099_, LootContext p_333702_) {
        CustomModelData custommodeldata = p_328099_.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
        p_328099_.set(
            DataComponents.CUSTOM_MODEL_DATA,
            new CustomModelData(
                apply(this.floats, custommodeldata.floats(), p_375387_ -> p_375387_.getFloat(p_333702_)),
                apply(this.flags, custommodeldata.flags()),
                apply(this.strings, custommodeldata.strings()),
                apply(this.colors, custommodeldata.colors(), p_375374_ -> p_375374_.getInt(p_333702_))
            )
        );
        return p_328099_;
    }
}