package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Time extends NeedleDirectionHelper implements RangeSelectItemModelProperty {
    public static final MapCodec<Time> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_378406_ -> p_378406_.group(
                    Codec.BOOL.optionalFieldOf("wobble", Boolean.valueOf(true)).forGetter(NeedleDirectionHelper::wobble),
                    Time.TimeSource.CODEC.fieldOf("source").forGetter(p_377822_ -> p_377822_.source)
                )
                .apply(p_378406_, Time::new)
    );
    private final Time.TimeSource source;
    private final RandomSource randomSource = RandomSource.create();
    private final NeedleDirectionHelper.Wobbler wobbler;

    public Time(boolean pWobble, Time.TimeSource pSource) {
        super(pWobble);
        this.source = pSource;
        this.wobbler = this.newWobbler(0.9F);
    }

    @Override
    protected float calculate(ItemStack p_378177_, ClientLevel p_378594_, int p_378007_, Entity p_377614_) {
        float f = this.source.get(p_378594_, p_378177_, p_377614_, this.randomSource);
        long i = p_378594_.getGameTime();
        if (this.wobbler.shouldUpdate(i)) {
            this.wobbler.update(i, f);
        }

        return this.wobbler.rotation();
    }

    @Override
    public MapCodec<Time> type() {
        return MAP_CODEC;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum TimeSource implements StringRepresentable {
        RANDOM("random") {
            @Override
            public float get(ClientLevel p_378410_, ItemStack p_375483_, Entity p_377937_, RandomSource p_376828_) {
                return p_376828_.nextFloat();
            }
        },
        DAYTIME("daytime") {
            @Override
            public float get(ClientLevel p_376912_, ItemStack p_377893_, Entity p_377289_, RandomSource p_375920_) {
                return p_376912_.getTimeOfDay(1.0F);
            }
        },
        MOON_PHASE("moon_phase") {
            @Override
            public float get(ClientLevel p_377401_, ItemStack p_376961_, Entity p_378153_, RandomSource p_377203_) {
                return (float)p_377401_.getMoonPhase() / 8.0F;
            }
        };

        public static final Codec<Time.TimeSource> CODEC = StringRepresentable.fromEnum(Time.TimeSource::values);
        private final String name;

        TimeSource(final String pName) {
            this.name = pName;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        abstract float get(ClientLevel pLevel, ItemStack pStack, Entity pEntity, RandomSource pRandom);
    }
}