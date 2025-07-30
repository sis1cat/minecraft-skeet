package net.minecraft.server.level;

import javax.annotation.Nullable;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;

public class ChunkLevel {
    private static final int FULL_CHUNK_LEVEL = 33;
    private static final int BLOCK_TICKING_LEVEL = 32;
    private static final int ENTITY_TICKING_LEVEL = 31;
    private static final ChunkStep FULL_CHUNK_STEP = ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FULL);
    public static final int RADIUS_AROUND_FULL_CHUNK = FULL_CHUNK_STEP.accumulatedDependencies().getRadius();
    public static final int MAX_LEVEL = 33 + RADIUS_AROUND_FULL_CHUNK + 32;

    @Nullable
    public static ChunkStatus generationStatus(int pLevel) {
        return getStatusAroundFullChunk(pLevel - 33, null);
    }

    @Nullable
    public static ChunkStatus getStatusAroundFullChunk(int pDistance, @Nullable ChunkStatus pChunkStatus) {
        if (pDistance > RADIUS_AROUND_FULL_CHUNK) {
            return pChunkStatus;
        } else {
            return pDistance <= 0 ? ChunkStatus.FULL : FULL_CHUNK_STEP.accumulatedDependencies().get(pDistance);
        }
    }

    public static ChunkStatus getStatusAroundFullChunk(int pDistance) {
        return getStatusAroundFullChunk(pDistance, ChunkStatus.EMPTY);
    }

    public static int byStatus(ChunkStatus pStatus) {
        return 33 + FULL_CHUNK_STEP.getAccumulatedRadiusOf(pStatus);
    }

    public static FullChunkStatus fullStatus(int pLevel) {
        if (pLevel <= 31) {
            return FullChunkStatus.ENTITY_TICKING;
        } else if (pLevel <= 32) {
            return FullChunkStatus.BLOCK_TICKING;
        } else {
            return pLevel <= 33 ? FullChunkStatus.FULL : FullChunkStatus.INACCESSIBLE;
        }
    }

    public static int byStatus(FullChunkStatus pStatus) {
        return switch (pStatus) {
            case INACCESSIBLE -> MAX_LEVEL;
            case FULL -> 33;
            case BLOCK_TICKING -> 32;
            case ENTITY_TICKING -> 31;
        };
    }

    public static boolean isEntityTicking(int pLevel) {
        return pLevel <= 31;
    }

    public static boolean isBlockTicking(int pLevel) {
        return pLevel <= 32;
    }

    public static boolean isLoaded(int pLevel) {
        return pLevel <= MAX_LEVEL;
    }
}