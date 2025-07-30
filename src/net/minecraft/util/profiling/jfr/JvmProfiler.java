package net.minecraft.util.profiling.jfr;

import com.mojang.logging.LogUtils;
import java.net.SocketAddress;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.slf4j.Logger;

public interface JvmProfiler {
    JvmProfiler INSTANCE = (JvmProfiler)(Runtime.class.getModule().getLayer().findModule("jdk.jfr").isPresent()
        ? JfrProfiler.getInstance()
        : new JvmProfiler.NoOpProfiler());

    boolean start(Environment pEnvironment);

    Path stop();

    boolean isRunning();

    boolean isAvailable();

    void onServerTick(float pCurrentAverageTickTime);

    void onPacketReceived(ConnectionProtocol pProtocol, PacketType<?> pPacketType, SocketAddress pAddress, int pSize);

    void onPacketSent(ConnectionProtocol pProtocol, PacketType<?> pPacketType, SocketAddress pAddress, int pSize);

    void onRegionFileRead(RegionStorageInfo pRegionStorageInfo, ChunkPos pChunkPos, RegionFileVersion pVersion, int pBytes);

    void onRegionFileWrite(RegionStorageInfo pRegionStorageInfo, ChunkPos pChunkPos, RegionFileVersion pVersion, int pBytes);

    @Nullable
    ProfiledDuration onWorldLoadedStarted();

    @Nullable
    ProfiledDuration onChunkGenerate(ChunkPos pChunkPos, ResourceKey<Level> pLevel, String pName);

    @Nullable
    ProfiledDuration onStructureGenerate(ChunkPos pChunkPos, ResourceKey<Level> pLevel, Holder<Structure> pStructure);

    public static class NoOpProfiler implements JvmProfiler {
        private static final Logger LOGGER = LogUtils.getLogger();
        static final ProfiledDuration noOpCommit = p_378711_ -> {
        };

        @Override
        public boolean start(Environment p_185368_) {
            LOGGER.warn("Attempted to start Flight Recorder, but it's not supported on this JVM");
            return false;
        }

        @Override
        public Path stop() {
            throw new IllegalStateException("Attempted to stop Flight Recorder, but it's not supported on this JVM");
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void onPacketReceived(ConnectionProtocol p_298045_, PacketType<?> p_329330_, SocketAddress p_185365_, int p_185363_) {
        }

        @Override
        public void onPacketSent(ConnectionProtocol p_297220_, PacketType<?> p_336281_, SocketAddress p_185377_, int p_185375_) {
        }

        @Override
        public void onRegionFileRead(RegionStorageInfo p_328378_, ChunkPos p_330600_, RegionFileVersion p_329437_, int p_328234_) {
        }

        @Override
        public void onRegionFileWrite(RegionStorageInfo p_335465_, ChunkPos p_330839_, RegionFileVersion p_333005_, int p_328862_) {
        }

        @Override
        public void onServerTick(float p_185361_) {
        }

        @Override
        public ProfiledDuration onWorldLoadedStarted() {
            return noOpCommit;
        }

        @Nullable
        @Override
        public ProfiledDuration onChunkGenerate(ChunkPos p_185370_, ResourceKey<Level> p_185371_, String p_185372_) {
            return null;
        }

        @Override
        public ProfiledDuration onStructureGenerate(ChunkPos p_375412_, ResourceKey<Level> p_377426_, Holder<Structure> p_376385_) {
            return noOpCommit;
        }
    }
}