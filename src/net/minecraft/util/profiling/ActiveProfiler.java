package net.minecraft.util.profiling;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.optifine.Config;
import net.optifine.Lagometer;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorClass;
import net.optifine.reflect.ReflectorField;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

public class ActiveProfiler implements ProfileCollector {
    private static final long WARNING_TIME_NANOS = Duration.ofMillis(100L).toNanos();
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<String> paths = Lists.newArrayList();
    private final LongList startTimes = new LongArrayList();
    private final Map<String, ActiveProfiler.PathEntry> entries = Maps.newHashMap();
    private final IntSupplier getTickTime;
    private final LongSupplier getRealTime;
    private final long startTimeNano;
    private final int startTimeTicks;
    private String path = "";
    private boolean started;
    @Nullable
    private ActiveProfiler.PathEntry currentEntry;
    private final boolean warn;
    private final Set<Pair<String, MetricCategory>> chartedPaths = new ObjectArraySet<>();
    private boolean clientProfiler = false;
    private boolean lagometerActive = false;
    private static final String SCHEDULED_EXECUTABLES = "scheduledExecutables";
    private static final String TICK = "tick";
    private static final String SOUND = "sound";
    private static final int HASH_SCHEDULED_EXECUTABLES = "scheduledExecutables".hashCode();
    private static final int HASH_TICK = "tick".hashCode();
    private static final int HASH_SOUND = "sound".hashCode();
    private static final ReflectorClass MINECRAFT = new ReflectorClass(Minecraft.class);
    private static final ReflectorField Minecraft_timeTracker = new ReflectorField(MINECRAFT, ContinuousProfiler.class);

    public ActiveProfiler(LongSupplier pStartTimeNano, IntSupplier pStartTimeTicks, boolean pWarn) {
        this.startTimeNano = pStartTimeNano.getAsLong();
        this.getRealTime = pStartTimeNano;
        this.startTimeTicks = pStartTimeTicks.getAsInt();
        this.getTickTime = pStartTimeTicks;
        this.warn = pWarn;
    }

    @Override
    public void startTick() {
        ContinuousProfiler continuousprofiler = (ContinuousProfiler)Reflector.getFieldValue(Minecraft.getInstance(), Minecraft_timeTracker);
        this.clientProfiler = continuousprofiler != null && continuousprofiler.getFiller() == this;
        this.lagometerActive = this.clientProfiler && Lagometer.isActive();
        if (this.started) {
            LOGGER.error("Profiler tick already started - missing endTick()?");
        } else {
            this.started = true;
            this.path = "";
            this.paths.clear();
            this.push("root");
        }
    }

    @Override
    public void endTick() {
        if (!this.started) {
            LOGGER.error("Profiler tick already ended - missing startTick()?");
        } else {
            this.pop();
            this.started = false;
            if (!this.path.isEmpty()) {
                LOGGER.error(
                    "Profiler tick ended before path was fully popped (remainder: '{}'). Mismatched push/pop?",
                    LogUtils.defer(() -> ProfileResults.demanglePath(this.path))
                );
            }
        }
    }

    @Override
    public void push(String pName) {
        if (this.lagometerActive) {
            int i = pName.hashCode();
            if (i == HASH_SCHEDULED_EXECUTABLES && pName.equals("scheduledExecutables")) {
                Lagometer.timerScheduledExecutables.start();
            } else if (i == HASH_TICK && pName.equals("tick") && Config.isMinecraftThread()) {
                Lagometer.timerScheduledExecutables.end();
                Lagometer.timerTick.start();
            }
        }

        if (!this.started) {
            LOGGER.error("Cannot push '{}' to profiler if profiler tick hasn't started - missing startTick()?", pName);
        } else {
            if (!this.path.isEmpty()) {
                this.path = this.path + "\u001e";
            }

            this.path = this.path + pName;
            this.paths.add(this.path);
            this.startTimes.add(Util.getNanos());
            this.currentEntry = null;
        }
    }

    @Override
    public void push(Supplier<String> pNameSupplier) {
        this.push(pNameSupplier.get());
    }

    @Override
    public void markForCharting(MetricCategory p_145928_) {
        this.chartedPaths.add(Pair.of(this.path, p_145928_));
    }

    @Override
    public void pop() {
        if (!this.started) {
            LOGGER.error("Cannot pop from profiler if profiler tick hasn't started - missing startTick()?");
        } else if (this.startTimes.isEmpty()) {
            LOGGER.error("Tried to pop one too many times! Mismatched push() and pop()?");
        } else {
            long i = Util.getNanos();
            long j = this.startTimes.removeLong(this.startTimes.size() - 1);
            this.paths.remove(this.paths.size() - 1);
            long k = i - j;
            ActiveProfiler.PathEntry activeprofiler$pathentry = this.getCurrentEntry();
            activeprofiler$pathentry.accumulatedDuration = (activeprofiler$pathentry.accumulatedDuration * 49L + k) / 50L;
            activeprofiler$pathentry.count = 1L;
            activeprofiler$pathentry.accumulatedDuration += k;
            activeprofiler$pathentry.count++;
            activeprofiler$pathentry.maxDuration = Math.max(activeprofiler$pathentry.maxDuration, k);
            activeprofiler$pathentry.minDuration = Math.min(activeprofiler$pathentry.minDuration, k);
            if (this.warn && k > WARNING_TIME_NANOS) {
                LOGGER.warn(
                    "Something's taking too long! '{}' took aprox {} ms",
                    LogUtils.defer(() -> ProfileResults.demanglePath(this.path)),
                    LogUtils.defer(() -> (double)k / 1000000.0)
                );
            }

            this.path = this.paths.isEmpty() ? "" : this.paths.get(this.paths.size() - 1);
            this.currentEntry = null;
        }
    }

    @Override
    public void popPush(String pName) {
        if (this.lagometerActive) {
            int i = pName.hashCode();
            if (i == HASH_SOUND && pName.equals("sound")) {
                Lagometer.timerTick.end();
            }
        }

        this.pop();
        this.push(pName);
    }

    @Override
    public void popPush(Supplier<String> pNameSupplier) {
        this.pop();
        this.push(pNameSupplier);
    }

    private ActiveProfiler.PathEntry getCurrentEntry() {
        if (this.currentEntry == null) {
            this.currentEntry = this.entries.computeIfAbsent(this.path, keyIn -> new ActiveProfiler.PathEntry());
        }

        return this.currentEntry;
    }

    @Override
    public void incrementCounter(String p_185247_, int p_185248_) {
        this.getCurrentEntry().counters.addTo(p_185247_, (long)p_185248_);
    }

    @Override
    public void incrementCounter(Supplier<String> p_185250_, int p_185251_) {
        this.getCurrentEntry().counters.addTo(p_185250_.get(), (long)p_185251_);
    }

    @Override
    public ProfileResults getResults() {
        return new FilledProfileResults(this.entries, this.startTimeNano, this.startTimeTicks, this.getRealTime.getAsLong(), this.getTickTime.getAsInt());
    }

    @Nullable
    @Override
    public ActiveProfiler.PathEntry getEntry(String p_145930_) {
        return this.entries.get(p_145930_);
    }

    @Override
    public Set<Pair<String, MetricCategory>> getChartedPaths() {
        return this.chartedPaths;
    }

    public static class PathEntry implements ProfilerPathEntry {
        long maxDuration = Long.MIN_VALUE;
        long minDuration = Long.MAX_VALUE;
        long accumulatedDuration;
        long count;
        final Object2LongOpenHashMap<String> counters = new Object2LongOpenHashMap<>();

        @Override
        public long getDuration() {
            return this.accumulatedDuration;
        }

        @Override
        public long getMaxDuration() {
            return this.maxDuration;
        }

        @Override
        public long getCount() {
            return this.count;
        }

        @Override
        public Object2LongMap<String> getCounters() {
            return Object2LongMaps.unmodifiable(this.counters);
        }
    }
}