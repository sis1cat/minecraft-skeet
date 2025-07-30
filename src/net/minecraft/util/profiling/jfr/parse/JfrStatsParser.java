package net.minecraft.util.profiling.jfr.parse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import net.minecraft.util.profiling.jfr.stats.ChunkGenStat;
import net.minecraft.util.profiling.jfr.stats.ChunkIdentification;
import net.minecraft.util.profiling.jfr.stats.CpuLoadStat;
import net.minecraft.util.profiling.jfr.stats.FileIOStat;
import net.minecraft.util.profiling.jfr.stats.GcHeapStat;
import net.minecraft.util.profiling.jfr.stats.IoSummary;
import net.minecraft.util.profiling.jfr.stats.PacketIdentification;
import net.minecraft.util.profiling.jfr.stats.StructureGenStat;
import net.minecraft.util.profiling.jfr.stats.ThreadAllocationStat;
import net.minecraft.util.profiling.jfr.stats.TickTimeStat;

public class JfrStatsParser {
    private Instant recordingStarted = Instant.EPOCH;
    private Instant recordingEnded = Instant.EPOCH;
    private final List<ChunkGenStat> chunkGenStats = Lists.newArrayList();
    private final List<StructureGenStat> structureGenStats = Lists.newArrayList();
    private final List<CpuLoadStat> cpuLoadStat = Lists.newArrayList();
    private final Map<PacketIdentification, JfrStatsParser.MutableCountAndSize> receivedPackets = Maps.newHashMap();
    private final Map<PacketIdentification, JfrStatsParser.MutableCountAndSize> sentPackets = Maps.newHashMap();
    private final Map<ChunkIdentification, JfrStatsParser.MutableCountAndSize> readChunks = Maps.newHashMap();
    private final Map<ChunkIdentification, JfrStatsParser.MutableCountAndSize> writtenChunks = Maps.newHashMap();
    private final List<FileIOStat> fileWrites = Lists.newArrayList();
    private final List<FileIOStat> fileReads = Lists.newArrayList();
    private int garbageCollections;
    private Duration gcTotalDuration = Duration.ZERO;
    private final List<GcHeapStat> gcHeapStats = Lists.newArrayList();
    private final List<ThreadAllocationStat> threadAllocationStats = Lists.newArrayList();
    private final List<TickTimeStat> tickTimes = Lists.newArrayList();
    @Nullable
    private Duration worldCreationDuration = null;

    private JfrStatsParser(Stream<RecordedEvent> pEvents) {
        this.capture(pEvents);
    }

    public static JfrStatsResult parse(Path pFile) {
        try {
            JfrStatsResult jfrstatsresult;
            try (final RecordingFile recordingfile = new RecordingFile(pFile)) {
                Iterator<RecordedEvent> iterator = new Iterator<RecordedEvent>() {
                    @Override
                    public boolean hasNext() {
                        return recordingfile.hasMoreEvents();
                    }

                    public RecordedEvent next() {
                        if (!this.hasNext()) {
                            throw new NoSuchElementException();
                        } else {
                            try {
                                return recordingfile.readEvent();
                            } catch (IOException ioexception1) {
                                throw new UncheckedIOException(ioexception1);
                            }
                        }
                    }
                };
                Stream<RecordedEvent> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 1297), false);
                jfrstatsresult = new JfrStatsParser(stream).results();
            }

            return jfrstatsresult;
        } catch (IOException ioexception) {
            throw new UncheckedIOException(ioexception);
        }
    }

    private JfrStatsResult results() {
        Duration duration = Duration.between(this.recordingStarted, this.recordingEnded);
        return new JfrStatsResult(
            this.recordingStarted,
            this.recordingEnded,
            duration,
            this.worldCreationDuration,
            this.tickTimes,
            this.cpuLoadStat,
            GcHeapStat.summary(duration, this.gcHeapStats, this.gcTotalDuration, this.garbageCollections),
            ThreadAllocationStat.summary(this.threadAllocationStats),
            collectIoStats(duration, this.receivedPackets),
            collectIoStats(duration, this.sentPackets),
            collectIoStats(duration, this.writtenChunks),
            collectIoStats(duration, this.readChunks),
            FileIOStat.summary(duration, this.fileWrites),
            FileIOStat.summary(duration, this.fileReads),
            this.chunkGenStats,
            this.structureGenStats
        );
    }

    private void capture(Stream<RecordedEvent> pEvents) {
        pEvents.forEach(p_374914_ -> {
            if (p_374914_.getEndTime().isAfter(this.recordingEnded) || this.recordingEnded.equals(Instant.EPOCH)) {
                this.recordingEnded = p_374914_.getEndTime();
            }

            if (p_374914_.getStartTime().isBefore(this.recordingStarted) || this.recordingStarted.equals(Instant.EPOCH)) {
                this.recordingStarted = p_374914_.getStartTime();
            }

            String s = p_374914_.getEventType().getName();
            switch (s) {
                case "minecraft.ChunkGeneration":
                    this.chunkGenStats.add(ChunkGenStat.from(p_374914_));
                    break;
                case "minecraft.StructureGeneration":
                    this.structureGenStats.add(StructureGenStat.from(p_374914_));
                    break;
                case "minecraft.LoadWorld":
                    this.worldCreationDuration = p_374914_.getDuration();
                    break;
                case "minecraft.ServerTickTime":
                    this.tickTimes.add(TickTimeStat.from(p_374914_));
                    break;
                case "minecraft.PacketReceived":
                    this.incrementPacket(p_374914_, p_374914_.getInt("bytes"), this.receivedPackets);
                    break;
                case "minecraft.PacketSent":
                    this.incrementPacket(p_374914_, p_374914_.getInt("bytes"), this.sentPackets);
                    break;
                case "minecraft.ChunkRegionRead":
                    this.incrementChunk(p_374914_, p_374914_.getInt("bytes"), this.readChunks);
                    break;
                case "minecraft.ChunkRegionWrite":
                    this.incrementChunk(p_374914_, p_374914_.getInt("bytes"), this.writtenChunks);
                    break;
                case "jdk.ThreadAllocationStatistics":
                    this.threadAllocationStats.add(ThreadAllocationStat.from(p_374914_));
                    break;
                case "jdk.GCHeapSummary":
                    this.gcHeapStats.add(GcHeapStat.from(p_374914_));
                    break;
                case "jdk.CPULoad":
                    this.cpuLoadStat.add(CpuLoadStat.from(p_374914_));
                    break;
                case "jdk.FileWrite":
                    this.appendFileIO(p_374914_, this.fileWrites, "bytesWritten");
                    break;
                case "jdk.FileRead":
                    this.appendFileIO(p_374914_, this.fileReads, "bytesRead");
                    break;
                case "jdk.GarbageCollection":
                    this.garbageCollections++;
                    this.gcTotalDuration = this.gcTotalDuration.plus(p_374914_.getDuration());
            }
        });
    }

    private void incrementPacket(RecordedEvent pEvent, int pIncrement, Map<PacketIdentification, JfrStatsParser.MutableCountAndSize> pPackets) {
        pPackets.computeIfAbsent(PacketIdentification.from(pEvent), p_326728_ -> new JfrStatsParser.MutableCountAndSize()).increment(pIncrement);
    }

    private void incrementChunk(RecordedEvent pEvent, int pIncrement, Map<ChunkIdentification, JfrStatsParser.MutableCountAndSize> pChunks) {
        pChunks.computeIfAbsent(ChunkIdentification.from(pEvent), p_332913_ -> new JfrStatsParser.MutableCountAndSize()).increment(pIncrement);
    }

    private void appendFileIO(RecordedEvent pEvent, List<FileIOStat> pStats, String pId) {
        pStats.add(new FileIOStat(pEvent.getDuration(), pEvent.getString("path"), pEvent.getLong(pId)));
    }

    private static <T> IoSummary<T> collectIoStats(Duration pRecordingDuration, Map<T, JfrStatsParser.MutableCountAndSize> pEntries) {
        List<Pair<T, IoSummary.CountAndSize>> list = pEntries.entrySet()
            .stream()
            .map(p_326729_ -> Pair.of(p_326729_.getKey(), p_326729_.getValue().toCountAndSize()))
            .toList();
        return new IoSummary<>(pRecordingDuration, list);
    }

    public static final class MutableCountAndSize {
        private long count;
        private long totalSize;

        public void increment(int pIncrement) {
            this.totalSize += (long)pIncrement;
            this.count++;
        }

        public IoSummary.CountAndSize toCountAndSize() {
            return new IoSummary.CountAndSize(this.count, this.totalSize);
        }
    }
}