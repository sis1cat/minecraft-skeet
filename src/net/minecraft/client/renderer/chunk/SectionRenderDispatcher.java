package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexSorting;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.TracingExecutor;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.optifine.Config;
import net.optifine.override.ChunkCacheOF;
import net.optifine.render.AabbFrame;
import net.optifine.render.ChunkLayerMap;
import net.optifine.render.ChunkLayerSet;
import net.optifine.render.ICamera;
import net.optifine.util.ChunkUtils;

public class SectionRenderDispatcher {
    private final CompileTaskDynamicQueue compileQueue = new CompileTaskDynamicQueue();
    private final Queue<Runnable> toUpload = Queues.newConcurrentLinkedQueue();
    final SectionBufferBuilderPack fixedBuffers;
    private final SectionBufferBuilderPool bufferPool;
    private volatile int toBatchCount;
    private volatile boolean closed;
    private final ConsecutiveExecutor consecutiveExecutor;
    private final TracingExecutor executor;
    ClientLevel level;
    final LevelRenderer renderer;
    private Vec3 camera = Vec3.ZERO;
    final SectionCompiler sectionCompiler;
    private int countRenderBuilders;
    private List<SectionBufferBuilderPack> listPausedBuilders = new ArrayList<>();
    public static final RenderType[] BLOCK_RENDER_LAYERS = RenderType.chunkBufferLayers().toArray(new RenderType[0]);
    public static int renderChunksUpdated;

    public SectionRenderDispatcher(
        ClientLevel pLevel,
        LevelRenderer pRenderer,
        TracingExecutor pExecutor,
        RenderBuffers pBuffer,
        BlockRenderDispatcher pBlockRenderer,
        BlockEntityRenderDispatcher pBlockEntityRenderer
    ) {
        this.level = pLevel;
        this.renderer = pRenderer;
        this.fixedBuffers = pBuffer.fixedBufferPack();
        this.bufferPool = pBuffer.sectionBufferPool();
        this.countRenderBuilders = this.bufferPool.getFreeBufferCount();
        this.executor = pExecutor;
        this.consecutiveExecutor = new ConsecutiveExecutor(pExecutor, "Section Renderer");
        this.consecutiveExecutor.schedule(this::runTask);
        this.sectionCompiler = new SectionCompiler(pBlockRenderer, pBlockEntityRenderer);
        this.sectionCompiler.sectionRenderDispatcher = this;
    }

    public void setLevel(ClientLevel pLevel) {
        this.level = pLevel;
    }

    private void runTask() {
        if (!this.closed && !this.bufferPool.isEmpty()) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.compileQueue.poll(this.getCameraPosition());
            if (sectionrenderdispatcher$rendersection$compiletask != null) {
                SectionBufferBuilderPack sectionbufferbuilderpack = Objects.requireNonNull(this.bufferPool.acquire());
                if (sectionbufferbuilderpack == null) {
                    this.compileQueue.add(sectionrenderdispatcher$rendersection$compiletask);
                    return;
                }

                this.toBatchCount = this.compileQueue.size();
                CompletableFuture.<CompletableFuture<SectionRenderDispatcher.SectionTaskResult>>supplyAsync(
                        () -> sectionrenderdispatcher$rendersection$compiletask.doTask(sectionbufferbuilderpack),
                        this.executor.forName(sectionrenderdispatcher$rendersection$compiletask.name())
                    )
                    .thenCompose(resultIn -> (CompletionStage<SectionRenderDispatcher.SectionTaskResult>)resultIn)
                    .whenComplete((taskResultIn, throwableIn) -> {
                        if (throwableIn != null) {
                            Minecraft.getInstance().delayCrash(CrashReport.forThrowable(throwableIn, "Batching sections"));
                        } else {
                            sectionrenderdispatcher$rendersection$compiletask.isCompleted.set(true);
                            this.consecutiveExecutor.schedule(() -> {
                                if (taskResultIn == SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL) {
                                    sectionbufferbuilderpack.clearAll();
                                } else {
                                    sectionbufferbuilderpack.discardAll();
                                }

                                this.bufferPool.release(sectionbufferbuilderpack);
                                this.runTask();
                            });
                        }
                    });
            }
        }
    }

    public String getStats() {
        return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.toBatchCount, this.toUpload.size(), this.bufferPool.getFreeBufferCount());
    }

    public int getToBatchCount() {
        return this.toBatchCount;
    }

    public int getToUpload() {
        return this.toUpload.size();
    }

    public int getFreeBufferCount() {
        return this.bufferPool.getFreeBufferCount();
    }

    public void setCamera(Vec3 pCamera) {
        this.camera = pCamera;
    }

    public Vec3 getCameraPosition() {
        return this.camera;
    }

    public void uploadAllPendingUploads() {
        Runnable runnable;
        while ((runnable = this.toUpload.poll()) != null) {
            runnable.run();
        }
    }

    public void rebuildSectionSync(SectionRenderDispatcher.RenderSection pSection, RenderRegionCache pRegionCache) {
        pSection.compileSync(pRegionCache);
    }

    public void blockUntilClear() {
        this.clearBatchQueue();
    }

    public void schedule(SectionRenderDispatcher.RenderSection.CompileTask pTask) {
        if (!this.closed) {
            this.consecutiveExecutor.schedule(() -> {
                if (!this.closed) {
                    this.compileQueue.add(pTask);
                    this.toBatchCount = this.compileQueue.size();
                    this.runTask();
                }
            });
        }
    }

    public CompletableFuture<Void> uploadSectionLayer(MeshData pMeshData, VertexBuffer pVertexBuffer) {
        return this.closed ? CompletableFuture.completedFuture(null) : CompletableFuture.runAsync(() -> {
            if (pVertexBuffer.isInvalid()) {
                pMeshData.close();
            } else {
                try (Zone zone = Profiler.get().zone("Upload Section Layer")) {
                    pVertexBuffer.bind();
                    pVertexBuffer.upload(pMeshData);
                    VertexBuffer.unbind();
                }
            }
        }, this.toUpload::add);
    }

    public CompletableFuture<Void> uploadSectionIndexBuffer(ByteBufferBuilder.Result pResult, VertexBuffer pVertexBuffer) {
        return this.closed ? CompletableFuture.completedFuture(null) : CompletableFuture.runAsync(() -> {
            if (pVertexBuffer.isInvalid()) {
                pResult.close();
            } else {
                try (Zone zone = Profiler.get().zone("Upload Section Indices")) {
                    pVertexBuffer.bind();
                    pVertexBuffer.uploadIndexBuffer(pResult);
                    VertexBuffer.unbind();
                }
            }
        }, this.toUpload::add);
    }

    private void clearBatchQueue() {
        this.compileQueue.clear();
        this.toBatchCount = 0;
    }

    public boolean isQueueEmpty() {
        return this.toBatchCount == 0 && this.toUpload.isEmpty();
    }

    public void dispose() {
        this.closed = true;
        this.clearBatchQueue();
        this.uploadAllPendingUploads();
    }

    public void pauseChunkUpdates() {
        long i = System.currentTimeMillis();
        if (this.listPausedBuilders.size() <= 0) {
            while (this.listPausedBuilders.size() != this.countRenderBuilders) {
                this.uploadAllPendingUploads();
                SectionBufferBuilderPack sectionbufferbuilderpack = this.bufferPool.acquire();
                if (sectionbufferbuilderpack != null) {
                    this.listPausedBuilders.add(sectionbufferbuilderpack);
                }

                if (System.currentTimeMillis() > i + 1000L) {
                    break;
                }
            }
        }
    }

    public void resumeChunkUpdates() {
        for (SectionBufferBuilderPack sectionbufferbuilderpack : this.listPausedBuilders) {
            this.bufferPool.release(sectionbufferbuilderpack);
        }

        this.listPausedBuilders.clear();
    }

    public boolean updateChunkNow(SectionRenderDispatcher.RenderSection renderChunk, RenderRegionCache regionCacheIn) {
        this.rebuildSectionSync(renderChunk, regionCacheIn);
        return true;
    }

    public boolean updateChunkLater(SectionRenderDispatcher.RenderSection renderChunk, RenderRegionCache regionCacheIn) {
        if (this.bufferPool.isEmpty()) {
            return false;
        } else {
            renderChunk.rebuildSectionAsync(this, regionCacheIn);
            return true;
        }
    }

    public boolean updateTransparencyLater(SectionRenderDispatcher.RenderSection renderChunk) {
        if (this.bufferPool.isEmpty()) {
            return false;
        } else {
            renderChunk.resortTransparency(this);
            return true;
        }
    }

    public void addUploadTask(Runnable r) {
        if (r != null) {
            this.toUpload.add(r);
        }
    }

    public static class CompiledSection {
        public static final SectionRenderDispatcher.CompiledSection UNCOMPILED = new SectionRenderDispatcher.CompiledSection() {
            @Override
            public boolean facesCanSeeEachother(Direction p_301280_, Direction p_299155_) {
                return false;
            }

            public void setAnimatedSprites(RenderType layer, BitSet animatedSprites) {
                throw new UnsupportedOperationException();
            }
        };
        public static final SectionRenderDispatcher.CompiledSection EMPTY = new SectionRenderDispatcher.CompiledSection() {
            @Override
            public boolean facesCanSeeEachother(Direction p_343413_, Direction p_342431_) {
                return true;
            }
        };
        final Set<RenderType> hasBlocks = new ChunkLayerSet();
        final List<BlockEntity> renderableBlockEntities = Lists.newArrayList();
        VisibilitySet visibilitySet = new VisibilitySet();
        @Nullable
        MeshData.SortState transparencyState;
        private BitSet[] animatedSprites = new BitSet[RenderType.CHUNK_RENDER_TYPES.length];

        public boolean hasRenderableLayers() {
            return !this.hasBlocks.isEmpty();
        }

        public boolean isEmpty(RenderType pRenderType) {
            return !this.hasBlocks.contains(pRenderType);
        }

        public List<BlockEntity> getRenderableBlockEntities() {
            return this.renderableBlockEntities;
        }

        public boolean facesCanSeeEachother(Direction pFace1, Direction pFace2) {
            return this.visibilitySet.visibilityBetween(pFace1, pFace2);
        }

        public BitSet getAnimatedSprites(RenderType layer) {
            return this.animatedSprites[layer.ordinal()];
        }

        public void setAnimatedSprites(BitSet[] animatedSprites) {
            this.animatedSprites = animatedSprites;
        }

        public boolean isLayerUsed(RenderType renderTypeIn) {
            return this.hasBlocks.contains(renderTypeIn);
        }

        public void setLayerUsed(RenderType renderTypeIn) {
            this.hasBlocks.add(renderTypeIn);
        }

        public boolean hasTerrainBlockEntities() {
            return !this.isEmpty() || !this.getRenderableBlockEntities().isEmpty();
        }

        public boolean isEmpty() {
            return this.hasBlocks.isEmpty();
        }

        public Set<RenderType> getLayersUsed() {
            return this.hasBlocks;
        }
    }

    public class RenderSection {
        public static final int SIZE = 16;
        public final int index;
        public final AtomicReference<SectionRenderDispatcher.CompiledSection> compiled = new AtomicReference<>(
            SectionRenderDispatcher.CompiledSection.UNCOMPILED
        );
        public final AtomicReference<SectionRenderDispatcher.TranslucencyPointOfView> pointOfView = new AtomicReference<>(null);
        @Nullable
        private SectionRenderDispatcher.RenderSection.RebuildTask lastRebuildTask;
        @Nullable
        private SectionRenderDispatcher.RenderSection.ResortTransparencyTask lastResortTransparencyTask;
        private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
        private final ChunkLayerMap<VertexBuffer> buffers = new ChunkLayerMap<>(rtIn -> new VertexBuffer(BufferUsage.STATIC_WRITE));
        private AABB bb;
        private boolean dirty = true;
        long sectionNode = SectionPos.asLong(-1, -1, -1);
        final BlockPos.MutableBlockPos origin = new BlockPos.MutableBlockPos(-1, -1, -1);
        private boolean playerChanged;
        private boolean playerUpdate = false;
        private boolean needsBackgroundPriorityUpdate;
        private boolean renderRegions = Config.isRenderRegions();
        public int regionX;
        public int regionZ;
        public int regionDX;
        public int regionDY;
        public int regionDZ;
        private final SectionRenderDispatcher.RenderSection[] renderChunksOfset16 = new SectionRenderDispatcher.RenderSection[6];
        private boolean renderChunksOffset16Updated = false;
        private LevelChunk chunk;
        private SectionOcclusionGraph.Node renderInfo = new SectionOcclusionGraph.Node(this, null, 0);
        public AabbFrame boundingBoxParent;
        private SectionPos sectionPosition;

        public RenderSection(final int pIndex, final long pSectionNode) {
            this.index = pIndex;
            this.setSectionNode(pSectionNode);
        }

        private boolean doesChunkExistAt(long pPos) {
            ChunkAccess chunkaccess = SectionRenderDispatcher.this.level
                .getChunk(SectionPos.x(pPos), SectionPos.z(pPos), ChunkStatus.FULL, false);
            return chunkaccess != null && SectionRenderDispatcher.this.level.getLightEngine().lightOnInColumn(SectionPos.getZeroNode(pPos));
        }

        public boolean hasAllNeighbors() {
            int i = 24;
            return !(this.getDistToPlayerSqr() > 576.0) ? true : this.doesChunkExistAt(this.sectionNode);
        }

        public AABB getBoundingBox() {
            return this.bb;
        }

        public VertexBuffer getBuffer(RenderType pRenderType) {
            return this.buffers.get(pRenderType);
        }

        public void setSectionNode(long pSectionNode) {
            this.reset();
            this.sectionNode = pSectionNode;
            int i = SectionPos.sectionToBlockCoord(SectionPos.x(pSectionNode));
            int j = SectionPos.sectionToBlockCoord(SectionPos.y(pSectionNode));
            int k = SectionPos.sectionToBlockCoord(SectionPos.z(pSectionNode));
            this.origin.set(i, j, k);
            this.sectionPosition = SectionPos.of(this.origin);
            if (this.renderRegions) {
                int l = 8;
                this.regionX = i >> l << l;
                this.regionZ = k >> l << l;
                this.regionDX = i - this.regionX;
                this.regionDY = j;
                this.regionDZ = k - this.regionZ;
            }

            this.bb = new AABB((double)i, (double)j, (double)k, (double)(i + 16), (double)(j + 16), (double)(k + 16));
            this.renderChunksOffset16Updated = false;
            this.chunk = null;
            this.boundingBoxParent = null;
        }

        protected double getDistToPlayerSqr() {
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            double d0 = this.bb.minX + 8.0 - camera.getPosition().x;
            double d1 = this.bb.minY + 8.0 - camera.getPosition().y;
            double d2 = this.bb.minZ + 8.0 - camera.getPosition().z;
            return d0 * d0 + d1 * d1 + d2 * d2;
        }

        public SectionRenderDispatcher.CompiledSection getCompiled() {
            return this.compiled.get();
        }

        private void reset() {
            this.cancelTasks();
            this.compiled.set(SectionRenderDispatcher.CompiledSection.UNCOMPILED);
            this.pointOfView.set(null);
            this.dirty = true;
        }

        public void releaseBuffers() {
            this.reset();
            this.buffers.values().forEach(VertexBuffer::close);
        }

        public BlockPos getOrigin() {
            return this.origin;
        }

        public long getSectionNode() {
            return this.sectionNode;
        }

        public void setDirty(boolean pPlayerChanged) {
            boolean flag = this.dirty;
            this.dirty = true;
            this.playerChanged = pPlayerChanged | (flag && this.playerChanged);
            if (this.isWorldPlayerUpdate()) {
                this.playerUpdate = true;
            }

            if (!flag) {
                SectionRenderDispatcher.this.renderer.onChunkRenderNeedsUpdate(this);
            }
        }

        public void setNotDirty() {
            this.dirty = false;
            this.playerChanged = false;
            this.playerUpdate = false;
            this.needsBackgroundPriorityUpdate = false;
        }

        public boolean isDirty() {
            return this.dirty;
        }

        public boolean isDirtyFromPlayer() {
            return this.dirty && this.playerChanged;
        }

        public long getNeighborSectionNode(Direction pDirection) {
            return SectionPos.offset(this.sectionNode, pDirection);
        }

        public void resortTransparency(SectionRenderDispatcher pDispatcher) {
            this.lastResortTransparencyTask = new SectionRenderDispatcher.RenderSection.ResortTransparencyTask(this.getCompiled());
            pDispatcher.schedule(this.lastResortTransparencyTask);
        }

        public boolean hasTranslucentGeometry() {
            return this.getCompiled().hasBlocks.contains(RenderType.translucent());
        }

        public boolean transparencyResortingScheduled() {
            return this.lastResortTransparencyTask != null && !this.lastResortTransparencyTask.isCompleted.get();
        }

        protected void cancelTasks() {
            if (this.lastRebuildTask != null) {
                this.lastRebuildTask.cancel();
                this.lastRebuildTask = null;
            }

            if (this.lastResortTransparencyTask != null) {
                this.lastResortTransparencyTask.cancel();
                this.lastResortTransparencyTask = null;
            }
        }

        public SectionRenderDispatcher.RenderSection.CompileTask createCompileTask(RenderRegionCache pRegionCache) {
            this.cancelTasks();
            RenderChunkRegion renderchunkregion = pRegionCache.createRegion(SectionRenderDispatcher.this.level, SectionPos.of(this.sectionNode));
            boolean flag = this.compiled.get() != SectionRenderDispatcher.CompiledSection.UNCOMPILED;
            this.lastRebuildTask = new SectionRenderDispatcher.RenderSection.RebuildTask(this, renderchunkregion, flag, pRegionCache);
            return this.lastRebuildTask;
        }

        public void rebuildSectionAsync(SectionRenderDispatcher pSectionRenderDispatcher, RenderRegionCache pRegionCache) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(pRegionCache);
            pSectionRenderDispatcher.schedule(sectionrenderdispatcher$rendersection$compiletask);
        }

        void updateGlobalBlockEntities(Collection<BlockEntity> pBlockEntities) {
            Set<BlockEntity> set = Sets.newHashSet(pBlockEntities);
            Set<BlockEntity> set1;
            synchronized (this.globalBlockEntities) {
                set1 = Sets.newHashSet(this.globalBlockEntities);
                set.removeAll(this.globalBlockEntities);
                set1.removeAll(pBlockEntities);
                this.globalBlockEntities.clear();
                this.globalBlockEntities.addAll(pBlockEntities);
            }

            SectionRenderDispatcher.this.renderer.updateGlobalBlockEntities(set1, set);
        }

        public void compileSync(RenderRegionCache pRegionCache) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(pRegionCache);
            sectionrenderdispatcher$rendersection$compiletask.doTask(SectionRenderDispatcher.this.fixedBuffers);
        }

        void setCompiled(SectionRenderDispatcher.CompiledSection pCompiled) {
            this.compiled.set(pCompiled);
            SectionRenderDispatcher.this.renderer.addRecentlyCompiledSection(this);
        }

        VertexSorting createVertexSorting() {
            Vec3 vec3 = SectionRenderDispatcher.this.getCameraPosition();
            return VertexSorting.byDistance(
                (float)this.regionDX + (float)(vec3.x - (double)this.origin.getX()),
                (float)this.regionDY + (float)(vec3.y - (double)this.origin.getY()),
                (float)this.regionDZ + (float)(vec3.z - (double)this.origin.getZ())
            );
        }

        private boolean isWorldPlayerUpdate() {
            if (SectionRenderDispatcher.this.level instanceof ClientLevel) {
                ClientLevel clientlevel = SectionRenderDispatcher.this.level;
                return clientlevel.isPlayerUpdate();
            } else {
                return false;
            }
        }

        public boolean isPlayerUpdate() {
            return this.playerUpdate;
        }

        public void setNeedsBackgroundPriorityUpdate(boolean needsBackgroundPriorityUpdate) {
            this.needsBackgroundPriorityUpdate = needsBackgroundPriorityUpdate;
        }

        public boolean needsBackgroundPriorityUpdate() {
            return this.needsBackgroundPriorityUpdate;
        }

        public LevelChunk getChunk() {
            return this.getChunk(this.origin);
        }

        private LevelChunk getChunk(BlockPos posIn) {
            LevelChunk levelchunk = this.chunk;
            if (levelchunk != null && ChunkUtils.isLoaded(levelchunk)) {
                return levelchunk;
            } else {
                levelchunk = SectionRenderDispatcher.this.level.getChunkAt(posIn);
                this.chunk = levelchunk;
                return levelchunk;
            }
        }

        public boolean isChunkRegionEmpty() {
            return this.isChunkRegionEmpty(this.origin);
        }

        private boolean isChunkRegionEmpty(BlockPos posIn) {
            int i = posIn.getY();
            int j = i + 15;
            return this.getChunk(posIn).isYSpaceEmpty(i, j);
        }

        public SectionOcclusionGraph.Node getRenderInfo() {
            return this.renderInfo;
        }

        public SectionOcclusionGraph.Node getRenderInfo(Direction dirIn, int counterIn) {
            this.renderInfo.initialize(dirIn, counterIn);
            return this.renderInfo;
        }

        public boolean isBoundingBoxInFrustum(ICamera camera, int frameCount) {
            return this.getBoundingBoxParent().isBoundingBoxInFrustumFully(camera, frameCount) ? true : camera.isBoundingBoxInFrustum(this.bb);
        }

        public AabbFrame getBoundingBoxParent() {
            if (this.boundingBoxParent == null) {
                BlockPos blockpos = this.getOrigin();
                int i = blockpos.getX();
                int j = blockpos.getY();
                int k = blockpos.getZ();
                int l = 5;
                int i1 = i >> l << l;
                int j1 = j >> l << l;
                int k1 = k >> l << l;
                if (i1 != i || j1 != j || k1 != k) {
                    AabbFrame aabbframe = SectionRenderDispatcher.this.renderer.getRenderChunk(new BlockPos(i1, j1, k1)).getBoundingBoxParent();
                    if (aabbframe != null && aabbframe.minX == (double)i1 && aabbframe.minY == (double)j1 && aabbframe.minZ == (double)k1) {
                        this.boundingBoxParent = aabbframe;
                    }
                }

                if (this.boundingBoxParent == null) {
                    int l1 = 1 << l;
                    this.boundingBoxParent = new AabbFrame((double)i1, (double)j1, (double)k1, (double)(i1 + l1), (double)(j1 + l1), (double)(k1 + l1));
                }
            }

            return this.boundingBoxParent;
        }

        public ClientLevel getWorld() {
            return SectionRenderDispatcher.this.level;
        }

        public SectionPos getSectionPosition() {
            return this.sectionPosition;
        }

        @Override
        public String toString() {
            return "pos: " + this.getOrigin();
        }

        public abstract class CompileTask {
            protected final AtomicBoolean isCancelled = new AtomicBoolean(false);
            protected final AtomicBoolean isCompleted = new AtomicBoolean(false);
            protected final boolean isRecompile;

            public CompileTask(final boolean pIsRecompile) {
                this.isRecompile = pIsRecompile;
            }

            public abstract CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack pSectionBufferBuilderPack);

            public abstract void cancel();

            protected abstract String name();

            public boolean isRecompile() {
                return this.isRecompile;
            }

            public BlockPos getOrigin() {
                return RenderSection.this.origin;
            }
        }

        class RebuildTask extends SectionRenderDispatcher.RenderSection.CompileTask {
            @Nullable
            protected volatile RenderChunkRegion region;
            private RenderRegionCache renderRegionCache;

            public RebuildTask(final SectionRenderDispatcher.RenderSection p_298793_, final RenderChunkRegion pRegion, final boolean pIsRecompile) {
                this(p_298793_, pRegion, pIsRecompile, null);
            }

            public RebuildTask(
                final SectionRenderDispatcher.RenderSection this$1,
                final RenderChunkRegion renderCacheIn,
                final boolean highPriorityIn,
                RenderRegionCache renderRegionCacheIn
            ) {
                super(highPriorityIn);
//                this.this$1 = this$1;
                this.region = renderCacheIn;
                this.renderRegionCache = renderRegionCacheIn;
                if (this.renderRegionCache != null) {
                    this.renderRegionCache.compileStarted();
                }
            }

            @Override
            protected String name() {
                return "rend_chk_rebuild";
            }

            @Override
            public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack p_299595_) {
                CompletableFuture completablefuture;
                try {
                    if (this.isCancelled.get()) {
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                    }

                    RenderChunkRegion renderchunkregion = this.region;
                    this.region = null;
                    if (renderchunkregion == null) {
                        RenderSection.this.setCompiled(SectionRenderDispatcher.CompiledSection.EMPTY);
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL);
                    }

                    SectionPos sectionpos = SectionPos.of(RenderSection.this.origin);
                    if (this.isCancelled.get()) {
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                    }

                    SectionCompiler.Results sectioncompiler$results;
                    try (Zone zone = Profiler.get().zone("Compile Section")) {
                        ChunkCacheOF chunkcacheof = renderchunkregion.makeChunkCacheOF();
                        sectioncompiler$results = SectionRenderDispatcher.this.sectionCompiler
                            .compile(
                                sectionpos, chunkcacheof, RenderSection.this.createVertexSorting(), p_299595_, RenderSection.this.regionDX, RenderSection.this.regionDY, RenderSection.this.regionDZ
                            );
                    }

                    SectionRenderDispatcher.TranslucencyPointOfView sectionrenderdispatcher$translucencypointofview = SectionRenderDispatcher.TranslucencyPointOfView.of(
                        SectionRenderDispatcher.this.getCameraPosition(), RenderSection.this.sectionNode
                    );
                    RenderSection.this.updateGlobalBlockEntities(sectioncompiler$results.globalBlockEntities);
                    if (!this.isCancelled.get()) {
                        SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = new SectionRenderDispatcher.CompiledSection();
                        sectionrenderdispatcher$compiledsection.visibilitySet = sectioncompiler$results.visibilitySet;
                        sectionrenderdispatcher$compiledsection.renderableBlockEntities.addAll(sectioncompiler$results.blockEntities);
                        sectionrenderdispatcher$compiledsection.transparencyState = sectioncompiler$results.transparencyState;
                        sectionrenderdispatcher$compiledsection.setAnimatedSprites(sectioncompiler$results.animatedSprites);
                        List<CompletableFuture<Void>> list = new ArrayList<>(sectioncompiler$results.renderedLayers.size());
                        sectioncompiler$results.renderedLayers.forEach((renderTypeIn, bufferIn) -> {
                            list.add(SectionRenderDispatcher.this.uploadSectionLayer(bufferIn, RenderSection.this.getBuffer(renderTypeIn)));
                            sectionrenderdispatcher$compiledsection.hasBlocks.add(renderTypeIn);
                        });
                        return Util.sequenceFailFast(list).handle((voidIn, throwableIn) -> {
                            if (throwableIn != null && !(throwableIn instanceof CancellationException) && !(throwableIn instanceof InterruptedException)) {
                                Minecraft.getInstance().delayCrash(CrashReport.forThrowable(throwableIn, "Rendering section"));
                            }

                            if (this.isCancelled.get()) {
                                return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                            } else {
                                RenderSection.this.setCompiled(sectionrenderdispatcher$compiledsection);
                                RenderSection.this.pointOfView.set(sectionrenderdispatcher$translucencypointofview);
                                return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                            }
                        });
                    }

                    sectioncompiler$results.release();
                    completablefuture = CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                } finally {
                    if (this.renderRegionCache != null) {
                        this.renderRegionCache.compileFinished();
                    }
                }

                return completablefuture;
            }

            @Override
            public void cancel() {
                this.region = null;
                if (this.isCancelled.compareAndSet(false, true)) {
                    RenderSection.this.setDirty(false);
                }
            }
        }

        class ResortTransparencyTask extends SectionRenderDispatcher.RenderSection.CompileTask {
            private final SectionRenderDispatcher.CompiledSection compiledSection;

            public ResortTransparencyTask(final SectionRenderDispatcher.CompiledSection pCompiledSection) {
                super(true);
                this.compiledSection = pCompiledSection;
            }

            @Override
            protected String name() {
                return "rend_chk_sort";
            }

            @Override
            public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack p_297366_) {
                if (this.isCancelled.get()) {
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                } else {
                    MeshData.SortState meshdata$sortstate = this.compiledSection.transparencyState;
                    if (meshdata$sortstate != null && !this.compiledSection.isEmpty(RenderType.translucent())) {
                        VertexSorting vertexsorting = RenderSection.this.createVertexSorting();
                        SectionRenderDispatcher.TranslucencyPointOfView sectionrenderdispatcher$translucencypointofview = SectionRenderDispatcher.TranslucencyPointOfView.of(
                            SectionRenderDispatcher.this.getCameraPosition(), RenderSection.this.sectionNode
                        );
                        if (sectionrenderdispatcher$translucencypointofview.equals(RenderSection.this.pointOfView.get())
                            && !sectionrenderdispatcher$translucencypointofview.isAxisAligned()) {
                            return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                        } else {
                            ByteBufferBuilder.Result bytebufferbuilder$result = meshdata$sortstate.buildSortedIndexBuffer(
                                p_297366_.buffer(RenderType.translucent()), vertexsorting
                            );
                            if (bytebufferbuilder$result == null) {
                                return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                            } else if (this.isCancelled.get()) {
                                bytebufferbuilder$result.close();
                                return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                            } else {
                                CompletableFuture<SectionRenderDispatcher.SectionTaskResult> completablefuture = SectionRenderDispatcher.this.uploadSectionIndexBuffer(
                                        bytebufferbuilder$result, RenderSection.this.getBuffer(RenderType.translucent())
                                    )
                                    .thenApply(voidIn -> SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                                return completablefuture.handle(
                                    (taskResultIn, throwableIn) -> {
                                        if (throwableIn != null
                                            && !(throwableIn instanceof CancellationException)
                                            && !(throwableIn instanceof InterruptedException)) {
                                            Minecraft.getInstance().delayCrash(CrashReport.forThrowable(throwableIn, "Rendering section"));
                                        }

                                        if (this.isCancelled.get()) {
                                            return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                                        } else {
                                            RenderSection.this.pointOfView.set(sectionrenderdispatcher$translucencypointofview);
                                            return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                                        }
                                    }
                                );
                            }
                        }
                    } else {
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                    }
                }
            }

            @Override
            public void cancel() {
                this.isCancelled.set(true);
            }
        }
    }

    static enum SectionTaskResult {
        SUCCESSFUL,
        CANCELLED;
    }

    public static final class TranslucencyPointOfView {
        private int x;
        private int y;
        private int z;

        public static SectionRenderDispatcher.TranslucencyPointOfView of(Vec3 pCameraPosition, long pSectionNode) {
            return new SectionRenderDispatcher.TranslucencyPointOfView().set(pCameraPosition, pSectionNode);
        }

        public SectionRenderDispatcher.TranslucencyPointOfView set(Vec3 pCameraPosition, long pSectionNode) {
            this.x = getCoordinate(pCameraPosition.x(), SectionPos.x(pSectionNode));
            this.y = getCoordinate(pCameraPosition.y(), SectionPos.y(pSectionNode));
            this.z = getCoordinate(pCameraPosition.z(), SectionPos.z(pSectionNode));
            return this;
        }

        private static int getCoordinate(double pCameraCoord, int pSectionCoord) {
            int i = SectionPos.blockToSectionCoord(pCameraCoord) - pSectionCoord;
            return Mth.clamp(i, -1, 1);
        }

        public boolean isAxisAligned() {
            return this.x == 0 || this.y == 0 || this.z == 0;
        }

        @Override
        public boolean equals(Object pOther) {
            if (pOther == this) {
                return true;
            } else {
                return pOther instanceof SectionRenderDispatcher.TranslucencyPointOfView sectionrenderdispatcher$translucencypointofview
                    ? this.x == sectionrenderdispatcher$translucencypointofview.x
                        && this.y == sectionrenderdispatcher$translucencypointofview.y
                        && this.z == sectionrenderdispatcher$translucencypointofview.z
                    : false;
            }
        }
    }
}
