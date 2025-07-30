package net.minecraft.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.primitives.UnsignedBytes;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.Codec.ResultFunction;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.codecs.BaseMapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.HolderSet;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ExtraCodecs {
    public static final Codec<JsonElement> JSON = converter(JsonOps.INSTANCE);
    public static final Codec<Object> JAVA = converter(JavaOps.INSTANCE);
    public static final Codec<Vector3f> VECTOR3F = Codec.FLOAT
        .listOf()
        .comapFlatMap(
            p_326507_ -> Util.fixedSize((List<Float>)p_326507_, 3).map(p_253489_ -> new Vector3f(p_253489_.get(0), p_253489_.get(1), p_253489_.get(2))),
            p_269787_ -> List.of(p_269787_.x(), p_269787_.y(), p_269787_.z())
        );
    public static final Codec<Vector4f> VECTOR4F = Codec.FLOAT
        .listOf()
        .comapFlatMap(
            p_326501_ -> Util.fixedSize((List<Float>)p_326501_, 4)
                    .map(p_326509_ -> new Vector4f(p_326509_.get(0), p_326509_.get(1), p_326509_.get(2), p_326509_.get(3))),
            p_326511_ -> List.of(p_326511_.x(), p_326511_.y(), p_326511_.z(), p_326511_.w())
        );
    public static final Codec<Quaternionf> QUATERNIONF_COMPONENTS = Codec.FLOAT
        .listOf()
        .comapFlatMap(
            p_326503_ -> Util.fixedSize((List<Float>)p_326503_, 4)
                    .map(p_341245_ -> new Quaternionf(p_341245_.get(0), p_341245_.get(1), p_341245_.get(2), p_341245_.get(3)).normalize()),
            p_269780_ -> List.of(p_269780_.x, p_269780_.y, p_269780_.z, p_269780_.w)
        );
    public static final Codec<AxisAngle4f> AXISANGLE4F = RecordCodecBuilder.create(
        p_269774_ -> p_269774_.group(
                    Codec.FLOAT.fieldOf("angle").forGetter(p_269776_ -> p_269776_.angle),
                    VECTOR3F.fieldOf("axis").forGetter(p_269778_ -> new Vector3f(p_269778_.x, p_269778_.y, p_269778_.z))
                )
                .apply(p_269774_, AxisAngle4f::new)
    );
    public static final Codec<Quaternionf> QUATERNIONF = Codec.withAlternative(QUATERNIONF_COMPONENTS, AXISANGLE4F.xmap(Quaternionf::new, AxisAngle4f::new));
    public static final Codec<Matrix4f> MATRIX4F = Codec.FLOAT
        .listOf()
        .comapFlatMap(p_326510_ -> Util.fixedSize((List<Float>)p_326510_, 16).map(p_269777_ -> {
                Matrix4f matrix4f = new Matrix4f();

                for (int i = 0; i < p_269777_.size(); i++) {
                    matrix4f.setRowColumn(i >> 2, i & 3, p_269777_.get(i));
                }

                return matrix4f.determineProperties();
            }), p_269775_ -> {
            FloatList floatlist = new FloatArrayList(16);

            for (int i = 0; i < 16; i++) {
                floatlist.add(p_269775_.getRowColumn(i >> 2, i & 3));
            }

            return floatlist;
        });
    public static final Codec<Integer> RGB_COLOR_CODEC = Codec.withAlternative(
        Codec.INT, VECTOR3F, p_358806_ -> ARGB.colorFromFloat(1.0F, p_358806_.x(), p_358806_.y(), p_358806_.z())
    );
    public static final Codec<Integer> ARGB_COLOR_CODEC = Codec.withAlternative(
        Codec.INT, VECTOR4F, p_358801_ -> ARGB.colorFromFloat(p_358801_.w(), p_358801_.x(), p_358801_.y(), p_358801_.z())
    );
    public static final Codec<Integer> UNSIGNED_BYTE = Codec.BYTE
        .flatComapMap(
            UnsignedBytes::toInt,
            p_326500_ -> p_326500_ > 255
                    ? DataResult.error(() -> "Unsigned byte was too large: " + p_326500_ + " > 255")
                    : DataResult.success(p_326500_.byteValue())
        );
    public static final Codec<Integer> NON_NEGATIVE_INT = intRangeWithMessage(0, Integer.MAX_VALUE, p_275703_ -> "Value must be non-negative: " + p_275703_);
    public static final Codec<Integer> POSITIVE_INT = intRangeWithMessage(1, Integer.MAX_VALUE, p_274847_ -> "Value must be positive: " + p_274847_);
    public static final Codec<Float> NON_NEGATIVE_FLOAT = floatRangeMinInclusiveWithMessage(0.0F, Float.MAX_VALUE, p_274876_ -> "Value must be non-negative: " + p_274876_);
    public static final Codec<Float> POSITIVE_FLOAT = floatRangeMinExclusiveWithMessage(0.0F, Float.MAX_VALUE, p_368583_ -> "Value must be positive: " + p_368583_);
    public static final Codec<Pattern> PATTERN = Codec.STRING.comapFlatMap(p_274857_ -> {
        try {
            return DataResult.success(Pattern.compile(p_274857_));
        } catch (PatternSyntaxException patternsyntaxexception) {
            return DataResult.error(() -> "Invalid regex pattern '" + p_274857_ + "': " + patternsyntaxexception.getMessage());
        }
    }, Pattern::pattern);
    public static final Codec<Instant> INSTANT_ISO8601 = temporalCodec(DateTimeFormatter.ISO_INSTANT).xmap(Instant::from, Function.identity());
    public static final Codec<byte[]> BASE64_STRING = Codec.STRING.comapFlatMap(p_274852_ -> {
        try {
            return DataResult.success(Base64.getDecoder().decode(p_274852_));
        } catch (IllegalArgumentException illegalargumentexception) {
            return DataResult.error(() -> "Malformed base64 string");
        }
    }, p_216180_ -> Base64.getEncoder().encodeToString(p_216180_));
    public static final Codec<String> ESCAPED_STRING = Codec.STRING
        .comapFlatMap(p_296617_ -> DataResult.success(StringEscapeUtils.unescapeJava(p_296617_)), StringEscapeUtils::escapeJava);
    public static final Codec<ExtraCodecs.TagOrElementLocation> TAG_OR_ELEMENT_ID = Codec.STRING
        .comapFlatMap(
            p_326502_ -> p_326502_.startsWith("#")
                    ? ResourceLocation.read(p_326502_.substring(1)).map(p_216182_ -> new ExtraCodecs.TagOrElementLocation(p_216182_, true))
                    : ResourceLocation.read(p_326502_).map(p_216165_ -> new ExtraCodecs.TagOrElementLocation(p_216165_, false)),
            ExtraCodecs.TagOrElementLocation::decoratedId
        );
    public static final Function<Optional<Long>, OptionalLong> toOptionalLong = p_216176_ -> p_216176_.map(OptionalLong::of).orElseGet(OptionalLong::empty);
    public static final Function<OptionalLong, Optional<Long>> fromOptionalLong = p_216178_ -> p_216178_.isPresent()
            ? Optional.of(p_216178_.getAsLong())
            : Optional.empty();
    public static final Codec<BitSet> BIT_SET = Codec.LONG_STREAM
        .xmap(p_253514_ -> BitSet.valueOf(p_253514_.toArray()), p_253493_ -> Arrays.stream(p_253493_.toLongArray()));
    private static final Codec<Property> PROPERTY = RecordCodecBuilder.create(
        p_326504_ -> p_326504_.group(
                    Codec.STRING.fieldOf("name").forGetter(Property::name),
                    Codec.STRING.fieldOf("value").forGetter(Property::value),
                    Codec.STRING.lenientOptionalFieldOf("signature").forGetter(p_296611_ -> Optional.ofNullable(p_296611_.signature()))
                )
                .apply(p_326504_, (p_253494_, p_253495_, p_253496_) -> new Property(p_253494_, p_253495_, p_253496_.orElse(null)))
    );
    public static final Codec<PropertyMap> PROPERTY_MAP = Codec.either(Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()), PROPERTY.listOf())
        .xmap(p_253515_ -> {
            PropertyMap propertymap = new PropertyMap();
            p_253515_.ifLeft(p_253506_ -> p_253506_.forEach((p_253500_, p_253501_) -> {
                    for (String s : p_253501_) {
                        propertymap.put(p_253500_, new Property(p_253500_, s));
                    }
                })).ifRight(p_296607_ -> {
                for (Property property : p_296607_) {
                    propertymap.put(property.name(), property);
                }
            });
            return propertymap;
        }, p_253504_ -> Either.right(p_253504_.values().stream().toList()));
    public static final Codec<String> PLAYER_NAME = Codec.string(0, 16)
        .validate(
            p_326493_ -> StringUtil.isValidPlayerName(p_326493_)
                    ? DataResult.success(p_326493_)
                    : DataResult.error(() -> "Player name contained disallowed characters: '" + p_326493_ + "'")
        );
    private static final MapCodec<GameProfile> GAME_PROFILE_WITHOUT_PROPERTIES = RecordCodecBuilder.mapCodec(
        p_326508_ -> p_326508_.group(UUIDUtil.AUTHLIB_CODEC.fieldOf("id").forGetter(GameProfile::getId), PLAYER_NAME.fieldOf("name").forGetter(GameProfile::getName))
                .apply(p_326508_, GameProfile::new)
    );
    public static final Codec<GameProfile> GAME_PROFILE = RecordCodecBuilder.create(
        p_326512_ -> p_326512_.group(
                    GAME_PROFILE_WITHOUT_PROPERTIES.forGetter(Function.identity()),
                    PROPERTY_MAP.lenientOptionalFieldOf("properties", new PropertyMap()).forGetter(GameProfile::getProperties)
                )
                .apply(p_326512_, (p_253518_, p_253519_) -> {
                    p_253519_.forEach((p_253511_, p_253512_) -> p_253518_.getProperties().put(p_253511_, p_253512_));
                    return p_253518_;
                })
    );
    public static final Codec<String> NON_EMPTY_STRING = Codec.STRING
        .validate(p_274858_ -> p_274858_.isEmpty() ? DataResult.error(() -> "Expected non-empty string") : DataResult.success(p_274858_));
    public static final Codec<Integer> CODEPOINT = Codec.STRING.comapFlatMap(p_284688_ -> {
        int[] aint = p_284688_.codePoints().toArray();
        return aint.length != 1 ? DataResult.error(() -> "Expected one codepoint, got: " + p_284688_) : DataResult.success(aint[0]);
    }, Character::toString);
    public static final Codec<String> RESOURCE_PATH_CODEC = Codec.STRING
        .validate(
            p_296613_ -> !ResourceLocation.isValidPath(p_296613_)
                    ? DataResult.error(() -> "Invalid string to use as a resource path element: " + p_296613_)
                    : DataResult.success(p_296613_)
        );

    public static <T> Codec<T> converter(DynamicOps<T> pOps) {
        return Codec.PASSTHROUGH.xmap(p_308959_ -> p_308959_.convert(pOps).getValue(), p_308962_ -> new Dynamic<>(pOps, (T)p_308962_));
    }

    public static <P, I> Codec<I> intervalCodec(
        Codec<P> pCodec, String pMinFieldName, String pMaxFieldName, BiFunction<P, P, DataResult<I>> pFactory, Function<I, P> pMinGetter, Function<I, P> pMaxGetter
    ) {
        Codec<I> codec = Codec.list(pCodec).comapFlatMap(p_326514_ -> Util.fixedSize((List<P>)p_326514_, 2).flatMap(p_184445_ -> {
                P p = p_184445_.get(0);
                P p1 = p_184445_.get(1);
                return pFactory.apply(p, p1);
            }), p_184459_ -> ImmutableList.of(pMinGetter.apply((I)p_184459_), pMaxGetter.apply((I)p_184459_)));
        Codec<I> codec1 = RecordCodecBuilder.<Pair<P, P>>create(
                p_184360_ -> p_184360_.group(pCodec.fieldOf(pMinFieldName).forGetter(Pair::getFirst), pCodec.fieldOf(pMaxFieldName).forGetter(Pair::getSecond))
                        .apply(p_184360_, Pair::of)
            )
            .comapFlatMap(
                p_184392_ -> pFactory.apply((P)p_184392_.getFirst(), (P)p_184392_.getSecond()),
                p_184449_ -> Pair.of(pMinGetter.apply((I)p_184449_), pMaxGetter.apply((I)p_184449_))
            );
        Codec<I> codec2 = Codec.withAlternative(codec, codec1);
        return Codec.either(pCodec, codec2)
            .comapFlatMap(p_184389_ -> p_184389_.map(p_184395_ -> pFactory.apply((P)p_184395_, (P)p_184395_), DataResult::success), p_184411_ -> {
                P p = pMinGetter.apply((I)p_184411_);
                P p1 = pMaxGetter.apply((I)p_184411_);
                return Objects.equals(p, p1) ? Either.left(p) : Either.right((I)p_184411_);
            });
    }

    public static <A> ResultFunction<A> orElsePartial(final A pValue) {
        return new ResultFunction<A>() {
            @Override
            public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> p_184466_, T p_184467_, DataResult<Pair<A, T>> p_184468_) {
                MutableObject<String> mutableobject = new MutableObject<>();
                Optional<Pair<A, T>> optional = p_184468_.resultOrPartial(mutableobject::setValue);
                return optional.isPresent()
                    ? p_184468_
                    : DataResult.error(() -> "(" + mutableobject.getValue() + " -> using default)", Pair.of(pValue, p_184467_));
            }

            @Override
            public <T> DataResult<T> coApply(DynamicOps<T> p_184470_, A p_184471_, DataResult<T> p_184472_) {
                return p_184472_;
            }

            @Override
            public String toString() {
                return "OrElsePartial[" + pValue + "]";
            }
        };
    }

    public static <E> Codec<E> idResolverCodec(ToIntFunction<E> pEncoder, IntFunction<E> pDecoder, int pNotFoundValue) {
        return Codec.INT
            .flatXmap(
                p_184414_ -> Optional.ofNullable(pDecoder.apply(p_184414_))
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "Unknown element id: " + p_184414_)),
                p_274850_ -> {
                    int i = pEncoder.applyAsInt((E)p_274850_);
                    return i == pNotFoundValue ? DataResult.error(() -> "Element with unknown id: " + p_274850_) : DataResult.success(i);
                }
            );
    }

    public static <I, E> Codec<E> idResolverCodec(Codec<I> pIdCodec, Function<I, E> pIdToValue, Function<E, I> pValueToId) {
        return pIdCodec.flatXmap(p_374893_ -> {
            E e = pIdToValue.apply((I)p_374893_);
            return e == null ? DataResult.error(() -> "Unknown element id: " + p_374893_) : DataResult.success(e);
        }, p_374900_ -> {
            I i = pValueToId.apply((E)p_374900_);
            return i == null ? DataResult.error(() -> "Element with unknown id: " + p_374900_) : DataResult.success(i);
        });
    }

    public static <E> Codec<E> orCompressed(final Codec<E> pFirst, final Codec<E> pSecond) {
        return new Codec<E>() {
            @Override
            public <T> DataResult<T> encode(E p_184483_, DynamicOps<T> p_184484_, T p_184485_) {
                return p_184484_.compressMaps() ? pSecond.encode(p_184483_, p_184484_, p_184485_) : pFirst.encode(p_184483_, p_184484_, p_184485_);
            }

            @Override
            public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> p_184480_, T p_184481_) {
                return p_184480_.compressMaps() ? pSecond.decode(p_184480_, p_184481_) : pFirst.decode(p_184480_, p_184481_);
            }

            @Override
            public String toString() {
                return pFirst + " orCompressed " + pSecond;
            }
        };
    }

    public static <E> MapCodec<E> orCompressed(final MapCodec<E> pFirst, final MapCodec<E> pSecond) {
        return new MapCodec<E>() {
            @Override
            public <T> RecordBuilder<T> encode(E p_310450_, DynamicOps<T> p_312581_, RecordBuilder<T> p_310094_) {
                return p_312581_.compressMaps() ? pSecond.encode(p_310450_, p_312581_, p_310094_) : pFirst.encode(p_310450_, p_312581_, p_310094_);
            }

            @Override
            public <T> DataResult<E> decode(DynamicOps<T> p_312833_, MapLike<T> p_309452_) {
                return p_312833_.compressMaps() ? pSecond.decode(p_312833_, p_309452_) : pFirst.decode(p_312833_, p_309452_);
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> p_311885_) {
                return pSecond.keys(p_311885_);
            }

            @Override
            public String toString() {
                return pFirst + " orCompressed " + pSecond;
            }
        };
    }

    public static <E> Codec<E> overrideLifecycle(Codec<E> pCodec, final Function<E, Lifecycle> pApplyLifecycle, final Function<E, Lifecycle> pCoApplyLifecycle) {
        return pCodec.mapResult(new ResultFunction<E>() {
            @Override
            public <T> DataResult<Pair<E, T>> apply(DynamicOps<T> p_310634_, T p_310147_, DataResult<Pair<E, T>> p_311056_) {
                return p_311056_.result().map(p_326518_ -> p_311056_.setLifecycle(pApplyLifecycle.apply(p_326518_.getFirst()))).orElse(p_311056_);
            }

            @Override
            public <T> DataResult<T> coApply(DynamicOps<T> p_311101_, E p_309590_, DataResult<T> p_309495_) {
                return p_309495_.setLifecycle(pCoApplyLifecycle.apply(p_309590_));
            }

            @Override
            public String toString() {
                return "WithLifecycle[" + pApplyLifecycle + " " + pCoApplyLifecycle + "]";
            }
        });
    }

    public static <E> Codec<E> overrideLifecycle(Codec<E> pCodec, Function<E, Lifecycle> pLifecycleGetter) {
        return overrideLifecycle(pCodec, pLifecycleGetter, pLifecycleGetter);
    }

    public static <K, V> ExtraCodecs.StrictUnboundedMapCodec<K, V> strictUnboundedMap(Codec<K> pKey, Codec<V> pValue) {
        return new ExtraCodecs.StrictUnboundedMapCodec<>(pKey, pValue);
    }

    public static <E> Codec<List<E>> compactListCodec(Codec<E> pElementCodec) {
        return compactListCodec(pElementCodec, pElementCodec.listOf());
    }

    public static <E> Codec<List<E>> compactListCodec(Codec<E> pElementCodec, Codec<List<E>> pListCodec) {
        return Codec.either(pListCodec, pElementCodec)
            .xmap(
                p_374901_ -> p_374901_.map(p_374891_ -> p_374891_, List::of),
                p_374897_ -> p_374897_.size() == 1 ? Either.right(p_374897_.getFirst()) : Either.left((List<E>)p_374897_)
            );
    }

    private static Codec<Integer> intRangeWithMessage(int pMin, int pMax, Function<Integer, String> pErrorMessage) {
        return Codec.INT
            .validate(
                p_274889_ -> p_274889_.compareTo(pMin) >= 0 && p_274889_.compareTo(pMax) <= 0
                        ? DataResult.success(p_274889_)
                        : DataResult.error(() -> pErrorMessage.apply(p_274889_))
            );
    }

    public static Codec<Integer> intRange(int pMin, int pMax) {
        return intRangeWithMessage(pMin, pMax, p_269784_ -> "Value must be within range [" + pMin + ";" + pMax + "]: " + p_269784_);
    }

    private static Codec<Float> floatRangeMinInclusiveWithMessage(float pMin, float pMax, Function<Float, String> pErrorMessage) {
        return Codec.FLOAT
            .validate(
                p_358800_ -> p_358800_.compareTo(pMin) >= 0 && p_358800_.compareTo(pMax) <= 0
                        ? DataResult.success(p_358800_)
                        : DataResult.error(() -> pErrorMessage.apply(p_358800_))
            );
    }

    private static Codec<Float> floatRangeMinExclusiveWithMessage(float pMin, float pMax, Function<Float, String> pErrorMessage) {
        return Codec.FLOAT
            .validate(
                p_274865_ -> p_274865_.compareTo(pMin) > 0 && p_274865_.compareTo(pMax) <= 0
                        ? DataResult.success(p_274865_)
                        : DataResult.error(() -> pErrorMessage.apply(p_274865_))
            );
    }

    public static Codec<Float> floatRange(float pMin, float pMax) {
        return floatRangeMinInclusiveWithMessage(pMin, pMax, p_374896_ -> "Value must be within range [" + pMin + ";" + pMax + "]: " + p_374896_);
    }

    public static <T> Codec<List<T>> nonEmptyList(Codec<List<T>> pCodec) {
        return pCodec.validate(p_274853_ -> p_274853_.isEmpty() ? DataResult.error(() -> "List must have contents") : DataResult.success(p_274853_));
    }

    public static <T> Codec<HolderSet<T>> nonEmptyHolderSet(Codec<HolderSet<T>> pCodec) {
        return pCodec.validate(
            p_274860_ -> p_274860_.unwrap().right().filter(List::isEmpty).isPresent()
                    ? DataResult.error(() -> "List must have contents")
                    : DataResult.success(p_274860_)
        );
    }

    public static <M extends Map<?, ?>> Codec<M> nonEmptyMap(Codec<M> pMapCodec) {
        return pMapCodec.validate(p_358805_ -> p_358805_.isEmpty() ? DataResult.error(() -> "Map must have contents") : DataResult.success(p_358805_));
    }

    public static <E> MapCodec<E> retrieveContext(final Function<DynamicOps<?>, DataResult<E>> pRetriever) {
        class ContextRetrievalCodec extends MapCodec<E> {
            @Override
            public <T> RecordBuilder<T> encode(E p_203993_, DynamicOps<T> p_203994_, RecordBuilder<T> p_203995_) {
                return p_203995_;
            }

            @Override
            public <T> DataResult<E> decode(DynamicOps<T> p_203990_, MapLike<T> p_203991_) {
                return pRetriever.apply(p_203990_);
            }

            @Override
            public String toString() {
                return "ContextRetrievalCodec[" + pRetriever + "]";
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> p_203997_) {
                return Stream.empty();
            }
        }

        return new ContextRetrievalCodec();
    }

    public static <E, L extends Collection<E>, T> Function<L, DataResult<L>> ensureHomogenous(Function<E, T> pTypeGetter) {
        return p_203980_ -> {
            Iterator<E> iterator = p_203980_.iterator();
            if (iterator.hasNext()) {
                T t = pTypeGetter.apply(iterator.next());

                while (iterator.hasNext()) {
                    E e = iterator.next();
                    T t1 = pTypeGetter.apply(e);
                    if (t1 != t) {
                        return DataResult.error(() -> "Mixed type list: element " + e + " had type " + t1 + ", but list is of type " + t);
                    }
                }
            }

            return DataResult.success(p_203980_, Lifecycle.stable());
        };
    }

    public static <A> Codec<A> catchDecoderException(final Codec<A> pCodec) {
        return Codec.of(pCodec, new Decoder<A>() {
            @Override
            public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> p_309963_, T p_309877_) {
                try {
                    return pCodec.decode(p_309963_, p_309877_);
                } catch (Exception exception) {
                    return DataResult.error(() -> "Caught exception decoding " + p_309877_ + ": " + exception.getMessage());
                }
            }
        });
    }

    public static Codec<TemporalAccessor> temporalCodec(DateTimeFormatter pDateTimeFormatter) {
        return Codec.STRING.comapFlatMap(p_296605_ -> {
            try {
                return DataResult.success(pDateTimeFormatter.parse(p_296605_));
            } catch (Exception exception) {
                return DataResult.error(exception::getMessage);
            }
        }, pDateTimeFormatter::format);
    }

    public static MapCodec<OptionalLong> asOptionalLong(MapCodec<Optional<Long>> pCodec) {
        return pCodec.xmap(toOptionalLong, fromOptionalLong);
    }

    public static <K, V> Codec<Map<K, V>> sizeLimitedMap(Codec<Map<K, V>> pMapCodec, int pMaxSize) {
        return pMapCodec.validate(
            p_326506_ -> p_326506_.size() > pMaxSize
                    ? DataResult.error(() -> "Map is too long: " + p_326506_.size() + ", expected range [0-" + pMaxSize + "]")
                    : DataResult.success(p_326506_)
        );
    }

    public static <T> Codec<Object2BooleanMap<T>> object2BooleanMap(Codec<T> pCodec) {
        return Codec.unboundedMap(pCodec, Codec.BOOL).xmap(Object2BooleanOpenHashMap::new, Object2ObjectOpenHashMap::new);
    }

    @Deprecated
    public static <K, V> MapCodec<V> dispatchOptionalValue(
        final String pKey1,
        final String pKey2,
        final Codec<K> pCodec,
        final Function<? super V, ? extends K> pKeyGetter,
        final Function<? super K, ? extends Codec<? extends V>> pCodecGetter
    ) {
        return new MapCodec<V>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> p_310901_) {
                return Stream.of(p_310901_.createString(pKey1), p_310901_.createString(pKey2));
            }

            @Override
            public <T> DataResult<V> decode(DynamicOps<T> p_310472_, MapLike<T> p_310342_) {
                T t = p_310342_.get(pKey1);
                return t == null
                    ? DataResult.error(() -> "Missing \"" + pKey1 + "\" in: " + p_310342_)
                    : pCodec.decode(p_310472_, t).flatMap(p_326527_ -> {
                        T t1 = Objects.requireNonNullElseGet(p_310342_.get(pKey2), p_310472_::emptyMap);
                        return pCodecGetter.apply(p_326527_.getFirst()).decode(p_310472_, t1).map(Pair::getFirst);
                    });
            }

            @Override
            public <T> RecordBuilder<T> encode(V p_309380_, DynamicOps<T> p_311460_, RecordBuilder<T> p_311592_) {
                K k = (K)pKeyGetter.apply(p_309380_);
                p_311592_.add(pKey1, pCodec.encodeStart(p_311460_, k));
                DataResult<T> dataresult = this.encode((Codec)pCodecGetter.apply(k), p_309380_, p_311460_);
                if (dataresult.result().isEmpty() || !Objects.equals(dataresult.result().get(), p_311460_.emptyMap())) {
                    p_311592_.add(pKey2, dataresult);
                }

                return p_311592_;
            }

            private <T, V2 extends V> DataResult<T> encode(Codec<V2> p_313212_, V p_310224_, DynamicOps<T> p_311229_) {
                return p_313212_.encodeStart(p_311229_, (V2)p_310224_);
            }
        };
    }

    public static <A> Codec<Optional<A>> optionalEmptyMap(final Codec<A> pCodec) {
        return new Codec<Optional<A>>() {
            @Override
            public <T> DataResult<Pair<Optional<A>, T>> decode(DynamicOps<T> p_331677_, T p_335846_) {
                return isEmptyMap(p_331677_, p_335846_)
                    ? DataResult.success(Pair.of(Optional.empty(), p_335846_))
                    : pCodec.decode(p_331677_, p_335846_).map(p_333523_ -> p_333523_.mapFirst(Optional::of));
            }

            private static <T> boolean isEmptyMap(DynamicOps<T> p_336166_, T p_333395_) {
                Optional<MapLike<T>> optional = p_336166_.getMap(p_333395_).result();
                return optional.isPresent() && optional.get().entries().findAny().isEmpty();
            }

            public <T> DataResult<T> encode(Optional<A> p_332665_, DynamicOps<T> p_329533_, T p_335687_) {
                return p_332665_.isEmpty() ? DataResult.success(p_329533_.emptyMap()) : pCodec.encode(p_332665_.get(), p_329533_, p_335687_);
            }
        };
    }

    public static class LateBoundIdMapper<I, V> {
        private final BiMap<I, V> idToValue = HashBiMap.create();

        public Codec<V> codec(Codec<I> pIdCodec) {
            BiMap<V, I> bimap = this.idToValue.inverse();
            return ExtraCodecs.idResolverCodec(pIdCodec, this.idToValue::get, bimap::get);
        }

        public ExtraCodecs.LateBoundIdMapper<I, V> put(I pId, V pValue) {
            Objects.requireNonNull(pValue, () -> "Value for " + pId + " is null");
            this.idToValue.put(pId, pValue);
            return this;
        }
    }

    public static record StrictUnboundedMapCodec<K, V>(Codec<K> keyCodec, Codec<V> elementCodec) implements Codec<Map<K, V>>, BaseMapCodec<K, V> {
        @Override
        public <T> DataResult<Map<K, V>> decode(DynamicOps<T> pOps, MapLike<T> pInput) {
            Builder<K, V> builder = ImmutableMap.builder();

            for (Pair<T, T> pair : pInput.entries().toList()) {
                DataResult<K> dataresult = this.keyCodec().parse(pOps, pair.getFirst());
                DataResult<V> dataresult1 = this.elementCodec().parse(pOps, pair.getSecond());
                DataResult<Pair<K, V>> dataresult2 = dataresult.apply2stable(Pair::of, dataresult1);
                Optional<Error<Pair<K, V>>> optional = dataresult2.error();
                if (optional.isPresent()) {
                    String s = optional.get().message();
                    return DataResult.error(() -> dataresult.result().isPresent() ? "Map entry '" + dataresult.result().get() + "' : " + s : s);
                }

                if (!dataresult2.result().isPresent()) {
                    return DataResult.error(() -> "Empty or invalid map contents are not allowed");
                }

                Pair<K, V> pair1 = dataresult2.result().get();
                builder.put(pair1.getFirst(), pair1.getSecond());
            }

            Map<K, V> map = builder.build();
            return DataResult.success(map);
        }

        @Override
        public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> pOps, T pInput) {
            return pOps.getMap(pInput)
                .setLifecycle(Lifecycle.stable())
                .flatMap(p_297301_ -> this.decode(pOps, (MapLike<T>)p_297301_))
                .map(p_300226_ -> Pair.of((Map<K, V>)p_300226_, pInput));
        }

        public <T> DataResult<T> encode(Map<K, V> pInput, DynamicOps<T> pOps, T pValue) {
            return this.encode(pInput, pOps, pOps.mapBuilder()).build(pValue);
        }

        @Override
        public String toString() {
            return "StrictUnboundedMapCodec[" + this.keyCodec + " -> " + this.elementCodec + "]";
        }

        @Override
        public Codec<K> keyCodec() {
            return this.keyCodec;
        }

        @Override
        public Codec<V> elementCodec() {
            return this.elementCodec;
        }
    }

    public static record TagOrElementLocation(ResourceLocation id, boolean tag) {
        @Override
        public String toString() {
            return this.decoratedId();
        }

        private String decoratedId() {
            return this.tag ? "#" + this.id : this.id.toString();
        }
    }
}