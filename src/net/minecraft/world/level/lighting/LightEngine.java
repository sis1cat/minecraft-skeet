package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class LightEngine<M extends DataLayerStorageMap<M>, S extends LayerLightSectionStorage<M>> implements LayerLightEventListener {
    public static final int MAX_LEVEL = 15;
    protected static final int MIN_OPACITY = 1;
    protected static final long PULL_LIGHT_IN_ENTRY = LightEngine.QueueEntry.decreaseAllDirections(1);
    private static final int MIN_QUEUE_SIZE = 512;
    protected static final Direction[] PROPAGATION_DIRECTIONS = Direction.values();
    protected final LightChunkGetter chunkSource;
    protected final S storage;
    private final LongOpenHashSet blockNodesToCheck = new LongOpenHashSet(512, 0.5F);
    private final LongArrayFIFOQueue decreaseQueue = new LongArrayFIFOQueue();
    private final LongArrayFIFOQueue increaseQueue = new LongArrayFIFOQueue();
    private static final int CACHE_SIZE = 2;
    private final long[] lastChunkPos = new long[2];
    private final LightChunk[] lastChunk = new LightChunk[2];

    protected LightEngine(LightChunkGetter pChunkSource, S pStorage) {
        this.chunkSource = pChunkSource;
        this.storage = pStorage;
        this.clearChunkCache();
    }

    public static boolean hasDifferentLightProperties(BlockState pState1, BlockState pState2) {
        return pState2 == pState1
            ? false
            : pState2.getLightBlock() != pState1.getLightBlock() || pState2.getLightEmission() != pState1.getLightEmission() || pState2.useShapeForLightOcclusion() || pState1.useShapeForLightOcclusion();
    }

    public static int getLightBlockInto(BlockState pState1, BlockState pState2, Direction pDirection, int pDefaultReturnValue) {
        boolean flag = isEmptyShape(pState1);
        boolean flag1 = isEmptyShape(pState2);
        if (flag && flag1) {
            return pDefaultReturnValue;
        } else {
            VoxelShape voxelshape = flag ? Shapes.empty() : pState1.getOcclusionShape();
            VoxelShape voxelshape1 = flag1 ? Shapes.empty() : pState2.getOcclusionShape();
            return Shapes.mergedFaceOccludes(voxelshape, voxelshape1, pDirection) ? 16 : pDefaultReturnValue;
        }
    }

    public static VoxelShape getOcclusionShape(BlockState pState, Direction pDirection) {
        return isEmptyShape(pState) ? Shapes.empty() : pState.getFaceOcclusionShape(pDirection);
    }

    protected static boolean isEmptyShape(BlockState pState) {
        return !pState.canOcclude() || !pState.useShapeForLightOcclusion();
    }

    protected BlockState getState(BlockPos pPos) {
        int i = SectionPos.blockToSectionCoord(pPos.getX());
        int j = SectionPos.blockToSectionCoord(pPos.getZ());
        LightChunk lightchunk = this.getChunk(i, j);
        return lightchunk == null ? Blocks.BEDROCK.defaultBlockState() : lightchunk.getBlockState(pPos);
    }

    protected int getOpacity(BlockState pState) {
        return Math.max(1, pState.getLightBlock());
    }

    protected boolean shapeOccludes(BlockState pState1, BlockState pState2, Direction pDirection) {
        VoxelShape voxelshape = getOcclusionShape(pState1, pDirection);
        VoxelShape voxelshape1 = getOcclusionShape(pState2, pDirection.getOpposite());
        return Shapes.faceShapeOccludes(voxelshape, voxelshape1);
    }

    @Nullable
    protected LightChunk getChunk(int pX, int pZ) {
        long i = ChunkPos.asLong(pX, pZ);

        for (int j = 0; j < 2; j++) {
            if (i == this.lastChunkPos[j]) {
                return this.lastChunk[j];
            }
        }

        LightChunk lightchunk = this.chunkSource.getChunkForLighting(pX, pZ);

        for (int k = 1; k > 0; k--) {
            this.lastChunkPos[k] = this.lastChunkPos[k - 1];
            this.lastChunk[k] = this.lastChunk[k - 1];
        }

        this.lastChunkPos[0] = i;
        this.lastChunk[0] = lightchunk;
        return lightchunk;
    }

    private void clearChunkCache() {
        Arrays.fill(this.lastChunkPos, ChunkPos.INVALID_CHUNK_POS);
        Arrays.fill(this.lastChunk, null);
    }

    @Override
    public void checkBlock(BlockPos p_285352_) {
        this.blockNodesToCheck.add(p_285352_.asLong());
    }

    public void queueSectionData(long pSectionPos, @Nullable DataLayer pData) {
        this.storage.queueSectionData(pSectionPos, pData);
    }

    public void retainData(ChunkPos pChunkPos, boolean pRetainData) {
        this.storage.retainData(SectionPos.getZeroNode(pChunkPos.x, pChunkPos.z), pRetainData);
    }

    @Override
    public void updateSectionStatus(SectionPos p_285167_, boolean p_284934_) {
        this.storage.updateSectionStatus(p_285167_.asLong(), p_284934_);
    }

    @Override
    public void setLightEnabled(ChunkPos p_285116_, boolean p_285522_) {
        this.storage.setLightEnabled(SectionPos.getZeroNode(p_285116_.x, p_285116_.z), p_285522_);
    }

    @Override
    public int runLightUpdates() {
        LongIterator longiterator = this.blockNodesToCheck.iterator();

        while (longiterator.hasNext()) {
            this.checkNode(longiterator.nextLong());
        }

        this.blockNodesToCheck.clear();
        this.blockNodesToCheck.trim(512);
        int i = 0;
        i += this.propagateDecreases();
        i += this.propagateIncreases();
        this.clearChunkCache();
        this.storage.markNewInconsistencies(this);
        this.storage.swapSectionMap();
        return i;
    }

    private int propagateIncreases() {
        int i;
        for (i = 0; !this.increaseQueue.isEmpty(); i++) {
            long j = this.increaseQueue.dequeueLong();
            long k = this.increaseQueue.dequeueLong();
            int l = this.storage.getStoredLevel(j);
            int i1 = LightEngine.QueueEntry.getFromLevel(k);
            if (LightEngine.QueueEntry.isIncreaseFromEmission(k) && l < i1) {
                this.storage.setStoredLevel(j, i1);
                l = i1;
            }

            if (l == i1) {
                this.propagateIncrease(j, k, l);
            }
        }

        return i;
    }

    private int propagateDecreases() {
        int i;
        for (i = 0; !this.decreaseQueue.isEmpty(); i++) {
            long j = this.decreaseQueue.dequeueLong();
            long k = this.decreaseQueue.dequeueLong();
            this.propagateDecrease(j, k);
        }

        return i;
    }

    protected void enqueueDecrease(long pPackedPos1, long pPackedPos2) {
        this.decreaseQueue.enqueue(pPackedPos1);
        this.decreaseQueue.enqueue(pPackedPos2);
    }

    protected void enqueueIncrease(long pPackedPos1, long pPackedPos2) {
        this.increaseQueue.enqueue(pPackedPos1);
        this.increaseQueue.enqueue(pPackedPos2);
    }

    @Override
    public boolean hasLightWork() {
        return this.storage.hasInconsistencies() || !this.blockNodesToCheck.isEmpty() || !this.decreaseQueue.isEmpty() || !this.increaseQueue.isEmpty();
    }

    @Nullable
    @Override
    public DataLayer getDataLayerData(SectionPos p_285093_) {
        return this.storage.getDataLayerData(p_285093_.asLong());
    }

    @Override
    public int getLightValue(BlockPos p_285149_) {
        return this.storage.getLightValue(p_285149_.asLong());
    }

    public String getDebugData(long pSectionPos) {
        return this.getDebugSectionType(pSectionPos).display();
    }

    public LayerLightSectionStorage.SectionType getDebugSectionType(long pSectionPos) {
        return this.storage.getDebugSectionType(pSectionPos);
    }

    protected abstract void checkNode(long pPackedPos);

    protected abstract void propagateIncrease(long pPackedPos, long pQueueEntry, int pLightLevel);

    protected abstract void propagateDecrease(long pPackedPos, long pLightLevel);

    public static class QueueEntry {
        private static final int FROM_LEVEL_BITS = 4;
        private static final int DIRECTION_BITS = 6;
        private static final long LEVEL_MASK = 15L;
        private static final long DIRECTIONS_MASK = 1008L;
        private static final long FLAG_FROM_EMPTY_SHAPE = 1024L;
        private static final long FLAG_INCREASE_FROM_EMISSION = 2048L;

        public static long decreaseSkipOneDirection(int pLevel, Direction pDirection) {
            long i = withoutDirection(1008L, pDirection);
            return withLevel(i, pLevel);
        }

        public static long decreaseAllDirections(int pLevel) {
            return withLevel(1008L, pLevel);
        }

        public static long increaseLightFromEmission(int pLevel, boolean pFromEmptyShape) {
            long i = 1008L;
            i |= 2048L;
            if (pFromEmptyShape) {
                i |= 1024L;
            }

            return withLevel(i, pLevel);
        }

        public static long increaseSkipOneDirection(int pLevel, boolean pFromEmptyShape, Direction pDirection) {
            long i = withoutDirection(1008L, pDirection);
            if (pFromEmptyShape) {
                i |= 1024L;
            }

            return withLevel(i, pLevel);
        }

        public static long increaseOnlyOneDirection(int pLevel, boolean pFromEmptyShape, Direction pDirection) {
            long i = 0L;
            if (pFromEmptyShape) {
                i |= 1024L;
            }

            i = withDirection(i, pDirection);
            return withLevel(i, pLevel);
        }

        public static long increaseSkySourceInDirections(boolean pDown, boolean pNorth, boolean pSouth, boolean pWest, boolean pEast) {
            long i = withLevel(0L, 15);
            if (pDown) {
                i = withDirection(i, Direction.DOWN);
            }

            if (pNorth) {
                i = withDirection(i, Direction.NORTH);
            }

            if (pSouth) {
                i = withDirection(i, Direction.SOUTH);
            }

            if (pWest) {
                i = withDirection(i, Direction.WEST);
            }

            if (pEast) {
                i = withDirection(i, Direction.EAST);
            }

            return i;
        }

        public static int getFromLevel(long pEntry) {
            return (int)(pEntry & 15L);
        }

        public static boolean isFromEmptyShape(long pEntry) {
            return (pEntry & 1024L) != 0L;
        }

        public static boolean isIncreaseFromEmission(long pEntry) {
            return (pEntry & 2048L) != 0L;
        }

        public static boolean shouldPropagateInDirection(long pEntry, Direction pDirection) {
            return (pEntry & 1L << pDirection.ordinal() + 4) != 0L;
        }

        private static long withLevel(long pEntry, int pLevel) {
            return pEntry & -16L | (long)pLevel & 15L;
        }

        private static long withDirection(long pEntry, Direction pDirection) {
            return pEntry | 1L << pDirection.ordinal() + 4;
        }

        private static long withoutDirection(long pEntry, Direction pDirection) {
            return pEntry & ~(1L << pDirection.ordinal() + 4);
        }
    }
}