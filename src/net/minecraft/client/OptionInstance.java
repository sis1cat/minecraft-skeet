package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractOptionSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.OptionEnum;
import net.optifine.config.FloatOptions;
import net.optifine.config.SliderPercentageOptionOF;
import net.optifine.config.SliderableValueSetInt;
import net.optifine.gui.IOptionControl;
import org.slf4j.Logger;

public class OptionInstance<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final OptionInstance.Enum<Boolean> BOOLEAN_VALUES = new OptionInstance.Enum<>(ImmutableList.of(Boolean.TRUE, Boolean.FALSE), Codec.BOOL);
    public static final OptionInstance.CaptionBasedToString<Boolean> BOOLEAN_TO_STRING = (compIn, boolIn) -> boolIn
            ? CommonComponents.OPTION_ON
            : CommonComponents.OPTION_OFF;
    private final OptionInstance.TooltipSupplier<T> tooltip;
    final Function<T, Component> toString;
    private final OptionInstance.ValueSet<T> values;
    private final Codec<T> codec;
    private final T initialValue;
    private final Consumer<T> onValueUpdate;
    final Component caption;
    T value;
    private String resourceKey;
    public static final Map<String, OptionInstance> OPTIONS_BY_KEY = new LinkedHashMap<>();

    public static OptionInstance<Boolean> createBoolean(String pKey, boolean pInitialValue, Consumer<Boolean> pOnValueUpdate) {
        return createBoolean(pKey, noTooltip(), pInitialValue, pOnValueUpdate);
    }

    public static OptionInstance<Boolean> createBoolean(String pKey, boolean pInitialValue) {
        return createBoolean(pKey, noTooltip(), pInitialValue, onUpdateIn -> {
        });
    }

    public static OptionInstance<Boolean> createBoolean(String pCaption, OptionInstance.TooltipSupplier<Boolean> pTooltip, boolean pInitialValue) {
        return createBoolean(pCaption, pTooltip, pInitialValue, onUpdateIn -> {
        });
    }

    public static OptionInstance<Boolean> createBoolean(
        String pCaption, OptionInstance.TooltipSupplier<Boolean> pTooltip, boolean pInitialValue, Consumer<Boolean> pOnValueUpdate
    ) {
        return createBoolean(pCaption, pTooltip, BOOLEAN_TO_STRING, pInitialValue, pOnValueUpdate);
    }

    public static OptionInstance<Boolean> createBoolean(
        String pCaption,
        OptionInstance.TooltipSupplier<Boolean> pTooltip,
        OptionInstance.CaptionBasedToString<Boolean> pValueStringifier,
        boolean pInitialValue,
        Consumer<Boolean> pOnValueUpdate
    ) {
        return new OptionInstance<>(pCaption, pTooltip, pValueStringifier, BOOLEAN_VALUES, pInitialValue, pOnValueUpdate);
    }

    public OptionInstance(
        String pCaption,
        OptionInstance.TooltipSupplier<T> pTooltip,
        OptionInstance.CaptionBasedToString<T> pValueStringifier,
        OptionInstance.ValueSet<T> pValues,
        T pInitialValue,
        Consumer<T> pOnValueUpdate
    ) {
        this(pCaption, pTooltip, pValueStringifier, pValues, pValues.codec(), pInitialValue, pOnValueUpdate);
    }

    public OptionInstance(
        String pCaption,
        OptionInstance.TooltipSupplier<T> pTooltip,
        OptionInstance.CaptionBasedToString<T> pValueStringifier,
        OptionInstance.ValueSet<T> pValues,
        Codec<T> pCodec,
        T pInitialValue,
        Consumer<T> pOnValueUpdate
    ) {
        this.caption = Component.translatable(pCaption);
        this.tooltip = pTooltip;
        this.toString = objIn -> pValueStringifier.toString(this.caption, objIn);
        this.values = pValues;
        this.codec = pCodec;
        this.initialValue = pInitialValue;
        this.onValueUpdate = pOnValueUpdate;
        this.value = this.initialValue;
        this.resourceKey = pCaption;
        OPTIONS_BY_KEY.put(this.resourceKey, this);
    }

    public static <T> OptionInstance.TooltipSupplier<T> noTooltip() {
        return objIn -> null;
    }

    public static <T> OptionInstance.TooltipSupplier<T> cachedConstantTooltip(Component pMessage) {
        return objIn -> Tooltip.create(pMessage);
    }

    public static <T extends OptionEnum> OptionInstance.CaptionBasedToString<T> forOptionEnum() {
        return (compIn, objIn) -> objIn.getCaption();
    }

    public AbstractWidget createButton(Options pOptions) {
        return this.createButton(pOptions, 0, 0, 150);
    }

    public AbstractWidget createButton(Options pOptions, int pX, int pY, int pWidth) {
        return this.createButton(pOptions, pX, pY, pWidth, objIn -> {
        });
    }

    public AbstractWidget createButton(Options pOptions, int pX, int pY, int pWidth, Consumer<T> pOnValueChanged) {
        return this.values.createButton(this.tooltip, pOptions, pX, pY, pWidth, pOnValueChanged).apply(this);
    }

    public T get() {
        if (this instanceof SliderPercentageOptionOF sliderpercentageoptionof) {
            if (this.value instanceof Integer) {
                return (T)(Integer)(int)sliderpercentageoptionof.getOptionValue();
            }

            if (this.value instanceof Double) {
                return (T)(Double)sliderpercentageoptionof.getOptionValue();
            }
        }

        return this.value;
    }

    public Codec<T> codec() {
        return this.codec;
    }

    @Override
    public String toString() {
        return this.caption.getString();
    }

    public void set(T pValue) {
        T t = this.values.validateValue(pValue).orElseGet(() -> {
            LOGGER.error("Illegal option value " + pValue + " for " + this.caption);
            return this.initialValue;
        });
        if (!Minecraft.getInstance().isRunning()) {
            this.value = t;
        } else if (!Objects.equals(this.value, t)) {
            this.value = t;
            this.onValueUpdate.accept(this.value);
        }
    }

    public OptionInstance.ValueSet<T> values() {
        return this.values;
    }

    public String getResourceKey() {
        return this.resourceKey;
    }

    public Component getCaption() {
        return this.caption;
    }

    public T getMinValue() {
        OptionInstance.IntRangeBase optioninstance$intrangebase = this.getIntRangeBase();
        if (optioninstance$intrangebase != null) {
            return (T)(Integer)optioninstance$intrangebase.minInclusive();
        } else {
            throw new IllegalArgumentException("Min value not supported: " + this.getResourceKey());
        }
    }

    public T getMaxValue() {
        OptionInstance.IntRangeBase optioninstance$intrangebase = this.getIntRangeBase();
        if (optioninstance$intrangebase != null) {
            return (T)(Integer)optioninstance$intrangebase.maxInclusive();
        } else {
            throw new IllegalArgumentException("Max value not supported: " + this.getResourceKey());
        }
    }

    public OptionInstance.IntRangeBase getIntRangeBase() {
        if (this.values instanceof OptionInstance.IntRangeBase) {
            return (OptionInstance.IntRangeBase)this.values;
        } else {
            return this.values instanceof SliderableValueSetInt ? ((SliderableValueSetInt)this.values).getIntRange() : null;
        }
    }

    public boolean isProgressOption() {
        return this.values instanceof OptionInstance.SliderableValueSet;
    }

    public static record AltEnum<T>(
        List<T> values, List<T> altValues, BooleanSupplier altCondition, OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter, Codec<T> codec
    ) implements OptionInstance.CycleableValueSet<T> {
        @Override
        public CycleButton.ValueListSupplier<T> valueListSupplier() {
            return CycleButton.ValueListSupplier.create(this.altCondition, this.values, this.altValues);
        }

        @Override
        public Optional<T> validateValue(T p_231570_) {
            return (this.altCondition.getAsBoolean() ? this.altValues : this.values).contains(p_231570_) ? Optional.of(p_231570_) : Optional.empty();
        }

        public OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter() {
            return this.valueSetter;
        }

        @Override
        public Codec<T> codec() {
            return this.codec;
        }
    }

    public interface CaptionBasedToString<T> {
        Component toString(Component pCaption, T pValue);
    }

    public static record ClampingLazyMaxIntRange(int minInclusive, IntSupplier maxSupplier, int encodableMaxInclusive)
        implements OptionInstance.IntRangeBase,
        OptionInstance.SliderableOrCyclableValueSet<Integer> {
        public Optional<Integer> validateValue(Integer p_231590_) {
            return Optional.of(Mth.clamp(p_231590_, this.minInclusive(), this.maxInclusive()));
        }

        @Override
        public int maxInclusive() {
            return this.maxSupplier.getAsInt();
        }

        @Override
        public Codec<Integer> codec() {
            return Codec.INT
                .validate(
                    valIn -> {
                        int i = this.encodableMaxInclusive + 1;
                        return valIn.compareTo(this.minInclusive) >= 0 && valIn.compareTo(i) <= 0
                            ? DataResult.success(valIn)
                            : DataResult.error(() -> "Value " + valIn + " outside of range [" + this.minInclusive + ":" + i + "]", valIn);
                    }
                );
        }

        @Override
        public boolean createCycleButton() {
            return true;
        }

        @Override
        public CycleButton.ValueListSupplier<Integer> valueListSupplier() {
            return CycleButton.ValueListSupplier.create(IntStream.range(this.minInclusive, this.maxInclusive() + 1).boxed().toList());
        }

        @Override
        public int minInclusive() {
            return this.minInclusive;
        }
    }

    interface CycleableValueSet<T> extends OptionInstance.ValueSet<T> {
        CycleButton.ValueListSupplier<T> valueListSupplier();

        default OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter() {
            return OptionInstance::set;
        }

        @Override
        default Function<OptionInstance<T>, AbstractWidget> createButton(
            OptionInstance.TooltipSupplier<T> p_261801_, Options p_261824_, int p_261649_, int p_262114_, int p_261536_, Consumer<T> p_261642_
        ) {
            return optionIn -> CycleButton.builder(optionIn.toString)
                    .withValues(this.valueListSupplier())
                    .withTooltip(p_261801_)
                    .withInitialValue(optionIn.value)
                    .create(p_261649_, p_262114_, p_261536_, 20, optionIn.caption, (btnIn, valIn) -> {
                        this.valueSetter().set(optionIn, valIn);
                        p_261824_.save();
                        p_261642_.accept(valIn);
                    });
        }

        public interface ValueSetter<T> {
            void set(OptionInstance<T> pInstance, T pValue);
        }
    }

    public static record Enum<T>(List<T> values, Codec<T> codec) implements OptionInstance.CycleableValueSet<T> {
        @Override
        public Optional<T> validateValue(T p_231632_) {
            return this.values.contains(p_231632_) ? Optional.of(p_231632_) : Optional.empty();
        }

        @Override
        public CycleButton.ValueListSupplier<T> valueListSupplier() {
            return CycleButton.ValueListSupplier.create(this.values);
        }

        @Override
        public Codec<T> codec() {
            return this.codec;
        }
    }

    public static record IntRange(int minInclusive, int maxInclusive, boolean applyValueImmediately) implements OptionInstance.IntRangeBase {
        public IntRange(int minInclusive, int maxInclusive, boolean applyValueImmediately) {
            this.minInclusive = minInclusive;
            this.maxInclusive = maxInclusive;
            this.applyValueImmediately = true;
        }

        public IntRange(int pMinInclusive, int pMaxInclusive) {
            this(pMinInclusive, pMaxInclusive, true);
        }

        public Optional<Integer> validateValue(Integer p_231645_) {
            return p_231645_.compareTo(this.minInclusive()) >= 0 && p_231645_.compareTo(this.maxInclusive()) <= 0 ? Optional.of(p_231645_) : Optional.empty();
        }

        @Override
        public Codec<Integer> codec() {
            return Codec.intRange(this.minInclusive, this.maxInclusive + 1);
        }

        @Override
        public int minInclusive() {
            return this.minInclusive;
        }

        @Override
        public int maxInclusive() {
            return this.maxInclusive;
        }

        @Override
        public boolean applyValueImmediately() {
            return this.applyValueImmediately;
        }
    }

    public interface IntRangeBase extends OptionInstance.SliderableValueSet<Integer> {
        int minInclusive();

        int maxInclusive();

        default double toSliderValue(Integer p_231663_) {
            if (p_231663_ == this.minInclusive()) {
                return 0.0;
            } else {
                return p_231663_ == this.maxInclusive()
                    ? 1.0
                    : Mth.map((double)p_231663_.intValue() + 0.5, (double)this.minInclusive(), (double)this.maxInclusive() + 1.0, 0.0, 1.0);
            }
        }

        default Integer fromSliderValue(double p_231656_) {
            if (p_231656_ >= 1.0) {
                p_231656_ = 0.99999F;
            }

            return Mth.floor(Mth.map(p_231656_, 0.0, 1.0, (double)this.minInclusive(), (double)this.maxInclusive() + 1.0));
        }

        default <R> OptionInstance.SliderableValueSet<R> xmap(final IntFunction<? extends R> pTo, final ToIntFunction<? super R> pFrom) {
            return new SliderableValueSetInt<R>() {
                @Override
                public Optional<R> validateValue(R p_231674_) {
                    return IntRangeBase.this.validateValue(Integer.valueOf(pFrom.applyAsInt(p_231674_))).map(pTo::apply);
                }

                @Override
                public double toSliderValue(R p_231678_) {
                    return IntRangeBase.this.toSliderValue(pFrom.applyAsInt(p_231678_));
                }

                @Override
                public R fromSliderValue(double p_231676_) {
                    return (R)pTo.apply(IntRangeBase.this.fromSliderValue(p_231676_));
                }

                @Override
                public Codec<R> codec() {
                    return IntRangeBase.this.codec().xmap(pTo::apply, pFrom::applyAsInt);
                }

                @Override
                public OptionInstance.IntRangeBase getIntRange() {
                    return IntRangeBase.this;
                }
            };
        }
    }

    public static record LazyEnum<T>(Supplier<List<T>> values, Function<T, Optional<T>> validateValue, Codec<T> codec)
        implements OptionInstance.CycleableValueSet<T> {
        @Override
        public Optional<T> validateValue(T p_231689_) {
            return this.validateValue.apply(p_231689_);
        }

        @Override
        public CycleButton.ValueListSupplier<T> valueListSupplier() {
            return CycleButton.ValueListSupplier.create(this.values.get());
        }

        @Override
        public Codec<T> codec() {
            return this.codec;
        }
    }

    public static final class OptionInstanceSliderButton<N> extends AbstractOptionSliderButton implements IOptionControl {
        private final OptionInstance<N> instance;
        private final OptionInstance.SliderableValueSet<N> values;
        private final OptionInstance.TooltipSupplier<N> tooltipSupplier;
        private final Consumer<N> onValueChanged;
        @Nullable
        private Long delayedApplyAt;
        private final boolean applyValueImmediately;
        private boolean supportAdjusting;
        private boolean adjusting;

        OptionInstanceSliderButton(
            Options pOptions,
            int pX,
            int pY,
            int pWidth,
            int pHeight,
            OptionInstance<N> pInstance,
            OptionInstance.SliderableValueSet<N> pValues,
            OptionInstance.TooltipSupplier<N> pTooltipSupplier,
            Consumer<N> pOnValueChanged,
            boolean pApplyValueImmediately
        ) {
            super(pOptions, pX, pY, pWidth, pHeight, pValues.toSliderValue(pInstance.get()));
            this.instance = pInstance;
            this.values = pValues;
            this.tooltipSupplier = pTooltipSupplier;
            this.onValueChanged = pOnValueChanged;
            this.applyValueImmediately = pApplyValueImmediately;
            this.updateMessage();
            this.supportAdjusting = FloatOptions.supportAdjusting(this.instance);
            this.adjusting = false;
        }

        @Override
        protected void updateMessage() {
            if (this.adjusting) {
                double d0 = ((Number)this.values.fromSliderValue(this.value)).doubleValue();
                Component component1 = FloatOptions.getTextComponent(this.instance, d0);
                if (component1 != null) {
                    this.setMessage(component1);
                }
            } else {
                if (this.instance instanceof SliderPercentageOptionOF sliderpercentageoptionof) {
                    Component component = sliderpercentageoptionof.getOptionText();
                    if (component != null) {
                        this.setMessage(component);
                    }
                } else {
                    this.setMessage(this.instance.toString.apply(this.values.fromSliderValue(this.value)));
                }

                this.setTooltip(this.tooltipSupplier.apply(this.values.fromSliderValue(this.value)));
            }
        }

        @Override
        protected void applyValue() {
            if (!this.adjusting) {
                N n = this.instance.get();
                N n1 = this.values.fromSliderValue(this.value);
                if (!n1.equals(n)) {
                    if (this.instance instanceof SliderPercentageOptionOF sliderpercentageoptionof) {
                        sliderpercentageoptionof.setOptionValue(((Number)n1).doubleValue());
                    }

                    if (this.applyValueImmediately) {
                        this.applyUnsavedValue();
                    } else {
                        this.delayedApplyAt = Util.getMillis() + 600L;
                    }
                }
            }
        }

        public void applyUnsavedValue() {
            N n = this.values.fromSliderValue(this.value);
            if (!Objects.equals(n, this.instance.get())) {
                this.instance.set(n);
                this.onValueChanged.accept(this.instance.get());
            }
        }

        @Override
        public void renderWidget(GuiGraphics p_332467_, int p_329907_, int p_334179_, float p_329288_) {
            super.renderWidget(p_332467_, p_329907_, p_334179_, p_329288_);
            if (this.delayedApplyAt != null && Util.getMillis() >= this.delayedApplyAt) {
                this.delayedApplyAt = null;
                this.applyUnsavedValue();
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (this.supportAdjusting) {
                this.adjusting = true;
            }

            super.onClick(mouseX, mouseY);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double mouseDX, double mouseDY) {
            if (this.supportAdjusting) {
                this.adjusting = true;
            }

            super.onDrag(mouseX, mouseY, mouseDX, mouseDY);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            if (this.adjusting) {
                this.adjusting = false;
                this.applyValue();
                this.updateMessage();
            }

            super.onRelease(mouseX, mouseY);
        }

        @Override
        public OptionInstance getControlOption() {
            return this.instance;
        }
    }

    interface SliderableOrCyclableValueSet<T> extends OptionInstance.CycleableValueSet<T>, OptionInstance.SliderableValueSet<T> {
        boolean createCycleButton();

        @Override
        default Function<OptionInstance<T>, AbstractWidget> createButton(
            OptionInstance.TooltipSupplier<T> p_261786_, Options p_262030_, int p_261940_, int p_262149_, int p_261495_, Consumer<T> p_261881_
        ) {
            return this.createCycleButton()
                ? OptionInstance.CycleableValueSet.super.createButton(p_261786_, p_262030_, p_261940_, p_262149_, p_261495_, p_261881_)
                : OptionInstance.SliderableValueSet.super.createButton(p_261786_, p_262030_, p_261940_, p_262149_, p_261495_, p_261881_);
        }
    }

    public interface SliderableValueSet<T> extends OptionInstance.ValueSet<T> {
        double toSliderValue(T pValue);

        T fromSliderValue(double pValue);

        default boolean applyValueImmediately() {
            return true;
        }

        @Override
        default Function<OptionInstance<T>, AbstractWidget> createButton(
            OptionInstance.TooltipSupplier<T> p_261993_, Options p_262177_, int p_261706_, int p_261683_, int p_261573_, Consumer<T> p_261969_
        ) {
            return optionIn -> new OptionInstance.OptionInstanceSliderButton<>(
                    p_262177_, p_261706_, p_261683_, p_261573_, 20, optionIn, this, p_261993_, p_261969_, this.applyValueImmediately()
                );
        }
    }

    @FunctionalInterface
    public interface TooltipSupplier<T> {
        @Nullable
        Tooltip apply(T pValue);
    }

    public static enum UnitDouble implements OptionInstance.SliderableValueSet<Double> {
        INSTANCE;

        public Optional<Double> validateValue(Double p_231747_) {
            return p_231747_ >= 0.0 && p_231747_ <= 1.0 ? Optional.of(p_231747_) : Optional.empty();
        }

        public double toSliderValue(Double p_231756_) {
            return p_231756_;
        }

        public Double fromSliderValue(double p_231741_) {
            return p_231741_;
        }

        public <R> OptionInstance.SliderableValueSet<R> xmap(final DoubleFunction<? extends R> pEncoder, final ToDoubleFunction<? super R> pDecoder) {
            return new OptionInstance.SliderableValueSet<R>() {
                @Override
                public Optional<R> validateValue(R p_231773_) {
                    return UnitDouble.this.validateValue(pDecoder.applyAsDouble(p_231773_)).map(pEncoder::apply);
                }

                @Override
                public double toSliderValue(R p_231777_) {
                    return UnitDouble.this.toSliderValue(pDecoder.applyAsDouble(p_231777_));
                }

                @Override
                public R fromSliderValue(double p_231775_) {
                    return (R)pEncoder.apply(UnitDouble.this.fromSliderValue(p_231775_));
                }

                @Override
                public Codec<R> codec() {
                    return UnitDouble.this.codec().xmap(pEncoder::apply, pDecoder::applyAsDouble);
                }
            };
        }

        @Override
        public Codec<Double> codec() {
            return Codec.withAlternative(Codec.doubleRange(0.0, 1.0), Codec.BOOL, flagIn -> flagIn ? 1.0 : 0.0);
        }
    }

    interface ValueSet<T> {
        Function<OptionInstance<T>, AbstractWidget> createButton(
            OptionInstance.TooltipSupplier<T> pTooltipSupplier, Options pOptions, int pX, int pY, int pWidth, Consumer<T> pOnValueChanged
        );

        Optional<T> validateValue(T pValue);

        Codec<T> codec();
    }
}
