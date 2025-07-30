package net.minecraft.client.data.models.blockstates;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface Condition extends Supplier<JsonElement> {
    void validate(StateDefinition<?, ?> pStateDefinition);

    static Condition.TerminalCondition condition() {
        return new Condition.TerminalCondition();
    }

    static Condition and(Condition... pConditions) {
        return new Condition.CompositeCondition(Condition.Operation.AND, Arrays.asList(pConditions));
    }

    static Condition or(Condition... pConditions) {
        return new Condition.CompositeCondition(Condition.Operation.OR, Arrays.asList(pConditions));
    }

    @OnlyIn(Dist.CLIENT)
    public static class CompositeCondition implements Condition {
        private final Condition.Operation operation;
        private final List<Condition> subconditions;

        CompositeCondition(Condition.Operation pOperation, List<Condition> pSubconditions) {
            this.operation = pOperation;
            this.subconditions = pSubconditions;
        }

        @Override
        public void validate(StateDefinition<?, ?> p_375975_) {
            this.subconditions.forEach(p_377907_ -> p_377907_.validate(p_375975_));
        }

        public JsonElement get() {
            JsonArray jsonarray = new JsonArray();
            this.subconditions.stream().map(Supplier::get).forEach(jsonarray::add);
            JsonObject jsonobject = new JsonObject();
            jsonobject.add(this.operation.id, jsonarray);
            return jsonobject;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Operation {
        AND("AND"),
        OR("OR");

        final String id;

        private Operation(final String pId) {
            this.id = pId;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class TerminalCondition implements Condition {
        private final Map<Property<?>, String> terms = Maps.newHashMap();

        private static <T extends Comparable<T>> String joinValues(Property<T> pProperty, Stream<T> pValues) {
            return pValues.map(pProperty::getName).collect(Collectors.joining("|"));
        }

        private static <T extends Comparable<T>> String getTerm(Property<T> pProperty, T pStartValue, T[] pOtherValues) {
            return joinValues(pProperty, Stream.concat(Stream.of(pStartValue), Stream.of(pOtherValues)));
        }

        private <T extends Comparable<T>> void putValue(Property<T> pProperty, String pValue) {
            String s = this.terms.put(pProperty, pValue);
            if (s != null) {
                throw new IllegalStateException("Tried to replace " + pProperty + " value from " + s + " to " + pValue);
            }
        }

        public final <T extends Comparable<T>> Condition.TerminalCondition term(Property<T> pProperty, T pValue) {
            this.putValue(pProperty, pProperty.getName(pValue));
            return this;
        }

        @SafeVarargs
        public final <T extends Comparable<T>> Condition.TerminalCondition term(Property<T> pProperty, T pStartValue, T... pOtherValues) {
            this.putValue(pProperty, getTerm(pProperty, pStartValue, pOtherValues));
            return this;
        }

        public final <T extends Comparable<T>> Condition.TerminalCondition negatedTerm(Property<T> pProperty, T pValue) {
            this.putValue(pProperty, "!" + pProperty.getName(pValue));
            return this;
        }

        @SafeVarargs
        public final <T extends Comparable<T>> Condition.TerminalCondition negatedTerm(Property<T> pProperty, T pStartValue, T... pOtherValues) {
            this.putValue(pProperty, "!" + getTerm(pProperty, pStartValue, pOtherValues));
            return this;
        }

        public JsonElement get() {
            JsonObject jsonobject = new JsonObject();
            this.terms.forEach((p_375818_, p_378345_) -> jsonobject.addProperty(p_375818_.getName(), p_378345_));
            return jsonobject;
        }

        @Override
        public void validate(StateDefinition<?, ?> p_378182_) {
            List<Property<?>> list = this.terms
                .keySet()
                .stream()
                .filter(p_375684_ -> p_378182_.getProperty(p_375684_.getName()) != p_375684_)
                .collect(Collectors.toList());
            if (!list.isEmpty()) {
                throw new IllegalStateException("Properties " + list + " are missing from " + p_378182_);
            }
        }
    }
}