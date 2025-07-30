package net.minecraft.world.level;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class ChunkPos {
    public static final Codec<ChunkPos> CODEC = Codec.INT_STREAM
        .<ChunkPos>comapFlatMap(
            intStreamIn -> Util.fixedSize(intStreamIn, 2).map(intsIn -> new ChunkPos(intsIn[0], intsIn[1])),
            posIn -> IntStream.of(posIn.x, posIn.z)
        )
        .stable();
    public static final StreamCodec<ByteBuf, ChunkPos> STREAM_CODEC = new StreamCodec<ByteBuf, ChunkPos>() {
        public ChunkPos decode(ByteBuf p_366719_) {
            return FriendlyByteBuf.readChunkPos(p_366719_);
        }

        public void encode(ByteBuf p_365664_, ChunkPos p_370100_) {
            FriendlyByteBuf.writeChunkPos(p_365664_, p_370100_);
        }
    };
    private static final int SAFETY_MARGIN = 1056;
    public static final long INVALID_CHUNK_POS = asLong(1875066, 1875066);
    private static final int SAFETY_MARGIN_CHUNKS = (32 + ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FULL).accumulatedDependencies().size() + 1) * 2;
    public static final int MAX_COORDINATE_VALUE = SectionPos.blockToSectionCoord(BlockPos.MAX_HORIZONTAL_COORDINATE) - SAFETY_MARGIN_CHUNKS;
    public static final ChunkPos ZERO = new ChunkPos(0, 0);
    private static final long COORD_BITS = 32L;
    private static final long COORD_MASK = 4294967295L;
    private static final int REGION_BITS = 5;
    public static final int REGION_SIZE = 32;
    private static final int REGION_MASK = 31;
    public static final int REGION_MAX_INDEX = 31;
    public final int x;
    public final int z;
    private static final int HASH_A = 1664525;
    private static final int HASH_C = 1013904223;
    private static final int HASH_Z_XOR = -559038737;
    private int cachedHashCode = 0;

    public ChunkPos(int pX, int pY) {
        this.x = pX;
        this.z = pY;
    }

    public ChunkPos(BlockPos pPos) {
        this.x = SectionPos.blockToSectionCoord(pPos.getX());
        this.z = SectionPos.blockToSectionCoord(pPos.getZ());
    }

    public ChunkPos(long pPackedPos) {
        this.x = (int)pPackedPos;
        this.z = (int)(pPackedPos >> 32);
    }

    public static ChunkPos minFromRegion(int pChunkX, int pChunkZ) {
        return new ChunkPos(pChunkX << 5, pChunkZ << 5);
    }

    public static ChunkPos maxFromRegion(int pChunkX, int pChunkZ) {
        return new ChunkPos((pChunkX << 5) + 31, (pChunkZ << 5) + 31);
    }

    public long toLong() {
        return asLong(this.x, this.z);
    }

    public static long asLong(int pX, int pZ) {
        return (long)pX & 4294967295L | ((long)pZ & 4294967295L) << 32;
    }

    public static long asLong(BlockPos pPos) {
        return asLong(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()));
    }

    public static int getX(long pChunkAsLong) {
        return (int)(pChunkAsLong & 4294967295L);
    }

    public static int getZ(long pChunkAsLong) {
        return (int)(pChunkAsLong >>> 32 & 4294967295L);
    }

    @Override
    public int hashCode() {
        if (this.cachedHashCode != 0) {
            return this.cachedHashCode;
        } else {
            this.cachedHashCode = hash(this.x, this.z);
            return this.cachedHashCode;
        }
    }

    public static int hash(int pX, int pZ) {
        int i = 1664525 * pX + 1013904223;
        int j = 1664525 * (pZ ^ -559038737) + 1013904223;
        return i ^ j;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return pOther instanceof ChunkPos chunkpos ? this.x == chunkpos.x && this.z == chunkpos.z : false;
        }
    }

    public int getMiddleBlockX() {
        return this.getBlockX(8);
    }

    public int getMiddleBlockZ() {
        return this.getBlockZ(8);
    }

    public int getMinBlockX() {
        return SectionPos.sectionToBlockCoord(this.x);
    }

    public int getMinBlockZ() {
        return SectionPos.sectionToBlockCoord(this.z);
    }

    public int getMaxBlockX() {
        return this.getBlockX(15);
    }

    public int getMaxBlockZ() {
        return this.getBlockZ(15);
    }

    public int getRegionX() {
        return this.x >> 5;
    }

    public int getRegionZ() {
        return this.z >> 5;
    }

    public int getRegionLocalX() {
        return this.x & 31;
    }

    public int getRegionLocalZ() {
        return this.z & 31;
    }

    public BlockPos getBlockAt(int pXSection, int pY, int pZSection) {
        return new BlockPos(this.getBlockX(pXSection), pY, this.getBlockZ(pZSection));
    }

    public int getBlockX(int pX) {
        return SectionPos.sectionToBlockCoord(this.x, pX);
    }

    public int getBlockZ(int pZ) {
        return SectionPos.sectionToBlockCoord(this.z, pZ);
    }

    public BlockPos getMiddleBlockPosition(int pY) {
        return new BlockPos(this.getMiddleBlockX(), pY, this.getMiddleBlockZ());
    }

    @Override
    public String toString() {
        return "[" + this.x + ", " + this.z + "]";
    }

    public BlockPos getWorldPosition() {
        return new BlockPos(this.getMinBlockX(), 0, this.getMinBlockZ());
    }

    public int getChessboardDistance(ChunkPos pChunkPos) {
        return this.getChessboardDistance(pChunkPos.x, pChunkPos.z);
    }

    public int getChessboardDistance(int pX, int pZ) {
        return Math.max(Math.abs(this.x - pX), Math.abs(this.z - pZ));
    }

    public int distanceSquared(ChunkPos pChunkPos) {
        return this.distanceSquared(pChunkPos.x, pChunkPos.z);
    }

    public int distanceSquared(long pPackedPos) {
        return this.distanceSquared(getX(pPackedPos), getZ(pPackedPos));
    }

    private int distanceSquared(int pX, int pZ) {
        int i = pX - this.x;
        int j = pZ - this.z;
        return i * i + j * j;
    }

    public static Stream<ChunkPos> rangeClosed(ChunkPos pCenter, int pRadius) {
        return rangeClosed(
            new ChunkPos(pCenter.x - pRadius, pCenter.z - pRadius), new ChunkPos(pCenter.x + pRadius, pCenter.z + pRadius)
        );
    }

    public static Stream<ChunkPos> rangeClosed(final ChunkPos pStart, final ChunkPos pEnd) {
        int i = Math.abs(pStart.x - pEnd.x) + 1;
        int j = Math.abs(pStart.z - pEnd.z) + 1;
        final int k = pStart.x < pEnd.x ? 1 : -1;
        final int l = pStart.z < pEnd.z ? 1 : -1;
        return StreamSupport.stream(new AbstractSpliterator<ChunkPos>((long)(i * j), 64) {
            @Nullable
            private ChunkPos pos;

            @Override
            public boolean tryAdvance(Consumer<? super ChunkPos> p_45630_) {
                if (this.pos == null) {
                    this.pos = pStart;
                } else {
                    int i1 = this.pos.x;
                    int j1 = this.pos.z;
                    if (i1 == pEnd.x) {
                        if (j1 == pEnd.z) {
                            return false;
                        }

                        this.pos = new ChunkPos(pStart.x, j1 + l);
                    } else {
                        this.pos = new ChunkPos(i1 + k, j1);
                    }
                }

                p_45630_.accept(this.pos);
                return true;
            }
        }, false);
    }
}