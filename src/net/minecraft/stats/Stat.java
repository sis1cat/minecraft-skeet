package net.minecraft.stats;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class Stat<T> extends ObjectiveCriteria {
    public static final StreamCodec<RegistryFriendlyByteBuf, Stat<?>> STREAM_CODEC = ByteBufCodecs.registry(Registries.STAT_TYPE)
        .dispatch(Stat::getType, StatType::streamCodec);
    private final StatFormatter formatter;
    private final T value;
    private final StatType<T> type;

    protected Stat(StatType<T> pType, T pValue, StatFormatter pFormatter) {
        super(buildName(pType, pValue));
        this.type = pType;
        this.formatter = pFormatter;
        this.value = pValue;
    }

    public static <T> String buildName(StatType<T> pType, T pValue) {
        return locationToKey(BuiltInRegistries.STAT_TYPE.getKey(pType)) + ":" + locationToKey(pType.getRegistry().getKey(pValue));
    }

    private static <T> String locationToKey(@Nullable ResourceLocation pLocation) {
        return pLocation.toString().replace(':', '.');
    }

    public StatType<T> getType() {
        return this.type;
    }

    public T getValue() {
        return this.value;
    }

    public String format(int pValue) {
        return this.formatter.format(pValue);
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther || pOther instanceof Stat && Objects.equals(this.getName(), ((Stat)pOther).getName());
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    @Override
    public String toString() {
        return "Stat{name=" + this.getName() + ", formatter=" + this.formatter + "}";
    }
}