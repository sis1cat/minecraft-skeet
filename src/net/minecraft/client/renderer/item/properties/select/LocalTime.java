package net.minecraft.client.renderer.item.properties.select;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LocalTime implements SelectItemModelProperty<String> {
    public static final String ROOT_LOCALE = "";
    private static final long UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1L);
    private static final Codec<TimeZone> TIME_ZONE_CODEC = Codec.STRING.comapFlatMap(p_376253_ -> {
        TimeZone timezone = TimeZone.getTimeZone(p_376253_);
        return timezone.equals(TimeZone.UNKNOWN_ZONE) ? DataResult.error(() -> "Unknown timezone: " + p_376253_) : DataResult.success(timezone);
    }, TimeZone::getID);
    private static final MapCodec<LocalTime.Data> DATA_MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_378756_ -> p_378756_.group(
                    Codec.STRING.fieldOf("pattern").forGetter(p_378804_ -> p_378804_.format),
                    Codec.STRING.optionalFieldOf("locale", "").forGetter(p_375710_ -> p_375710_.localeId),
                    TIME_ZONE_CODEC.optionalFieldOf("time_zone").forGetter(p_375894_ -> p_375894_.timeZone)
                )
                .apply(p_378756_, LocalTime.Data::new)
    );
    public static final SelectItemModelProperty.Type<LocalTime, String> TYPE = SelectItemModelProperty.Type.create(
        DATA_MAP_CODEC.flatXmap(LocalTime::create, p_377409_ -> DataResult.success(p_377409_.data)), Codec.STRING
    );
    private final LocalTime.Data data;
    private final DateFormat parsedFormat;
    private long nextUpdateTimeMs;
    private String lastResult = "";

    private LocalTime(LocalTime.Data pData, DateFormat pParsedFormat) {
        this.data = pData;
        this.parsedFormat = pParsedFormat;
    }

    public static LocalTime create(String pFormat, String pLocaleId, Optional<TimeZone> pTimeZone) {
        return create(new LocalTime.Data(pFormat, pLocaleId, pTimeZone))
            .getOrThrow(p_376916_ -> new IllegalStateException("Failed to validate format: " + p_376916_));
    }

    private static DataResult<LocalTime> create(LocalTime.Data pData) {
        ULocale ulocale = new ULocale(pData.localeId);
        Calendar calendar = pData.timeZone
            .<Calendar>map(p_377754_ -> Calendar.getInstance(p_377754_, ulocale))
            .orElseGet(() -> Calendar.getInstance(ulocale));
        SimpleDateFormat simpledateformat = new SimpleDateFormat(pData.format, ulocale);
        simpledateformat.setCalendar(calendar);

        try {
            simpledateformat.format(new Date());
        } catch (Exception exception) {
            return DataResult.error(() -> "Invalid time format '" + simpledateformat + "': " + exception.getMessage());
        }

        return DataResult.success(new LocalTime(pData, simpledateformat));
    }

    @Nullable
    public String get(ItemStack p_378462_, @Nullable ClientLevel p_377341_, @Nullable LivingEntity p_377996_, int p_376733_, ItemDisplayContext p_377284_) {
        long i = Util.getMillis();
        if (i > this.nextUpdateTimeMs) {
            this.lastResult = this.update();
            this.nextUpdateTimeMs = i + UPDATE_INTERVAL_MS;
        }

        return this.lastResult;
    }

    private String update() {
        return this.parsedFormat.format(new Date());
    }

    @Override
    public SelectItemModelProperty.Type<LocalTime, String> type() {
        return TYPE;
    }

    @OnlyIn(Dist.CLIENT)
    static record Data(String format, String localeId, Optional<TimeZone> timeZone) {
    }
}