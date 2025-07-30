package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.phys.Vec3;
import net.optifine.BlockPosM;
import net.optifine.Vec3M;
import org.slf4j.Logger;

public class SectionOcclusionGraph {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int MINIMUM_ADVANCED_CULLING_DISTANCE = 60;
    private static final double CEILED_SECTION_DIAGONAL = Math.ceil(Math.sqrt(3.0) * 16.0);
    private boolean needsFullUpdate = true;
    @Nullable
    private Future<?> fullUpdateTask;
    @Nullable
    private ViewArea viewArea;
    private final AtomicReference<SectionOcclusionGraph.GraphState> currentGraph = new AtomicReference<>();
    private final AtomicReference<SectionOcclusionGraph.GraphEvents> nextGraphEvents = new AtomicReference<>();
    private final AtomicBoolean needsFrustumUpdate = new AtomicBoolean(false);
    private LevelRenderer levelRenderer;

    public void waitAndReset(@Nullable ViewArea pViewArea) {
        if (this.fullUpdateTask != null) {
            try {
                this.fullUpdateTask.get();
                this.fullUpdateTask = null;
            } catch (Exception exception) {
                LOGGER.warn("Full update failed", (Throwable)exception);
            }
        }

        this.viewArea = pViewArea;
        this.levelRenderer = Minecraft.getInstance().levelRenderer;
        if (pViewArea != null) {
            this.currentGraph.set(new SectionOcclusionGraph.GraphState(pViewArea));
            this.invalidate();
        } else {
            this.currentGraph.set(null);
        }
    }

    public void invalidate() {
        this.needsFullUpdate = true;
    }

    public void addSectionsInFrustum(Frustum pFrustum, List<SectionRenderDispatcher.RenderSection> pVisibleSections, List<SectionRenderDispatcher.RenderSection> pNearbyVisibleSections) {
        this.addSectionsInFrustum(pFrustum, pVisibleSections, pNearbyVisibleSections, true, -1);
    }

    public void addSectionsInFrustum(
        Frustum frustumIn,
        List<SectionRenderDispatcher.RenderSection> sectionsIn,
        List<SectionRenderDispatcher.RenderSection> sectionsNearIn,
        boolean updateSections,
        int maxChunkDistance
    ) {
        List<SectionRenderDispatcher.RenderSection> list = this.levelRenderer.getRenderInfosTerrain();
        List<SectionRenderDispatcher.RenderSection> list1 = this.levelRenderer.getRenderInfosTileEntities();
        int i = (int)frustumIn.getCameraX() >> 4 << 4;
        int j = (int)frustumIn.getCameraY() >> 4 << 4;
        int k = (int)frustumIn.getCameraZ() >> 4 << 4;
        int l = maxChunkDistance * maxChunkDistance;
        this.currentGraph.get().storage().sectionTree.visitNodes((nodeIn, skipFrustumIn, levelIn, nearIn) -> {
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = nodeIn.getSection();
            if (sectionrenderdispatcher$rendersection != null) {
                if (maxChunkDistance > 0) {
                    BlockPos blockpos = sectionrenderdispatcher$rendersection.getOrigin();
                    int i1 = i - blockpos.getX();
                    int j1 = j - blockpos.getY();
                    int k1 = k - blockpos.getZ();
                    int l1 = i1 * i1 + j1 * j1 + k1 * k1;
                    if (l1 > l) {
                        return;
                    }
                }

                if (updateSections) {
                    sectionsIn.add(sectionrenderdispatcher$rendersection);
                }

                if (nearIn) {
                    sectionsNearIn.add(sectionrenderdispatcher$rendersection);
                }

                SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = sectionrenderdispatcher$rendersection.getCompiled();
                if (!sectionrenderdispatcher$compiledsection.isEmpty()) {
                    list.add(sectionrenderdispatcher$rendersection);
                }

                if (!sectionrenderdispatcher$compiledsection.getRenderableBlockEntities().isEmpty()) {
                    list1.add(sectionrenderdispatcher$rendersection);
                }
            }
        }, frustumIn, 32);
    }

    public boolean consumeFrustumUpdate() {
        return this.needsFrustumUpdate.compareAndSet(true, false);
    }

    public void onChunkReadyToRender(ChunkPos pChunkPos) {
        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents = this.nextGraphEvents.get();
        if (sectionocclusiongraph$graphevents != null) {
            this.addNeighbors(sectionocclusiongraph$graphevents, pChunkPos);
        }

        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents1 = this.currentGraph.get().events;
        if (sectionocclusiongraph$graphevents1 != sectionocclusiongraph$graphevents) {
            this.addNeighbors(sectionocclusiongraph$graphevents1, pChunkPos);
        }
    }

    public void schedulePropagationFrom(SectionRenderDispatcher.RenderSection pSection) {
        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents = this.nextGraphEvents.get();
        if (sectionocclusiongraph$graphevents != null) {
            sectionocclusiongraph$graphevents.sectionsToPropagateFrom.add(pSection);
        }

        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents1 = this.currentGraph.get().events;
        if (sectionocclusiongraph$graphevents1 != sectionocclusiongraph$graphevents) {
            sectionocclusiongraph$graphevents1.sectionsToPropagateFrom.add(pSection);
        }

        if (pSection.getCompiled().hasTerrainBlockEntities()) {
            this.needsFrustumUpdate.set(true);
        }
    }

    public void update(
        boolean pSmartCull, Camera pCamera, Frustum pFrustum, List<SectionRenderDispatcher.RenderSection> pVisibleSections, LongOpenHashSet pLoadedEmptySections
    ) {
        Vec3 vec3 = pCamera.getPosition();
        if (this.needsFullUpdate && (this.fullUpdateTask == null || this.fullUpdateTask.isDone())) {
            this.scheduleFullUpdate(pSmartCull, pCamera, vec3, pLoadedEmptySections);
        }

        this.runPartialUpdate(pSmartCull, pFrustum, pVisibleSections, vec3, pLoadedEmptySections);
    }

    private void scheduleFullUpdate(boolean pSmartCull, Camera pCamera, Vec3 pCameraPosition, LongOpenHashSet pLoadedEmptySections) {
        this.needsFullUpdate = false;
        LongOpenHashSet longopenhashset = pLoadedEmptySections.clone();
        this.fullUpdateTask = CompletableFuture.runAsync(() -> {
            SectionOcclusionGraph.GraphState sectionocclusiongraph$graphstate = new SectionOcclusionGraph.GraphState(this.viewArea);
            this.nextGraphEvents.set(sectionocclusiongraph$graphstate.events);
            Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();
            this.initializeQueueForFullUpdate(pCamera, queue);
            queue.forEach(nodeIn -> sectionocclusiongraph$graphstate.storage.sectionToNodeMap.put(nodeIn.section, nodeIn));
            this.runUpdates(sectionocclusiongraph$graphstate.storage, pCameraPosition, queue, pSmartCull, sectionIn -> {
            }, longopenhashset);
            this.currentGraph.set(sectionocclusiongraph$graphstate);
            this.nextGraphEvents.set(null);
            this.needsFrustumUpdate.set(true);
        }, Util.backgroundExecutor());
    }

    private void runPartialUpdate(
        boolean pSmartCull, Frustum pFrustum, List<SectionRenderDispatcher.RenderSection> pVisibleSections, Vec3 pCameraPosition, LongOpenHashSet pLoadedEmptySections
    ) {
        SectionOcclusionGraph.GraphState sectionocclusiongraph$graphstate = this.currentGraph.get();
        this.queueSectionsWithNewNeighbors(sectionocclusiongraph$graphstate);
        if (!sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.isEmpty()) {
            Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();

            while (!sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.isEmpty()) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.poll();
                SectionOcclusionGraph.Node sectionocclusiongraph$node = sectionocclusiongraph$graphstate.storage
                    .sectionToNodeMap
                    .get(sectionrenderdispatcher$rendersection);
                if (sectionocclusiongraph$node != null && sectionocclusiongraph$node.section == sectionrenderdispatcher$rendersection) {
                    queue.add(sectionocclusiongraph$node);
                }
            }

            List<SectionRenderDispatcher.RenderSection> list1 = this.levelRenderer.getRenderInfos();
            List<SectionRenderDispatcher.RenderSection> list2 = this.levelRenderer.getRenderInfosTerrain();
            List<SectionRenderDispatcher.RenderSection> list = this.levelRenderer.getRenderInfosTileEntities();
            Frustum frustum = LevelRenderer.offsetFrustum(pFrustum);
            Consumer<SectionRenderDispatcher.RenderSection> consumer = sectionIn -> {
                if (frustum.isVisible(sectionIn.getBoundingBox())) {
                    this.needsFrustumUpdate.set(true);
                    if (sectionIn == list1) {
                        SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = sectionIn.compiled.get();
                        if (!sectionrenderdispatcher$compiledsection.isEmpty()) {
                            list2.add(sectionIn);
                        }

                        if (!sectionrenderdispatcher$compiledsection.getRenderableBlockEntities().isEmpty()) {
                            list.add(sectionIn);
                        }
                    }
                }
            };
            this.runUpdates(sectionocclusiongraph$graphstate.storage, pCameraPosition, queue, pSmartCull, consumer, pLoadedEmptySections);
        }
    }

    private void queueSectionsWithNewNeighbors(SectionOcclusionGraph.GraphState pGraphState) {
        LongIterator longiterator = pGraphState.events.chunksWhichReceivedNeighbors.iterator();

        while (longiterator.hasNext()) {
            long i = longiterator.nextLong();
            List<SectionRenderDispatcher.RenderSection> list = pGraphState.storage.chunksWaitingForNeighbors.get(i);
            if (list != null && list.get(0).hasAllNeighbors()) {
                pGraphState.events.sectionsToPropagateFrom.addAll(list);
                pGraphState.storage.chunksWaitingForNeighbors.remove(i);
            }
        }

        pGraphState.events.chunksWhichReceivedNeighbors.clear();
    }

    private void addNeighbors(SectionOcclusionGraph.GraphEvents pGraphEvents, ChunkPos pChunkPos) {
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x - 1, pChunkPos.z));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x, pChunkPos.z - 1));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x + 1, pChunkPos.z));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x, pChunkPos.z + 1));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x - 1, pChunkPos.z - 1));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x - 1, pChunkPos.z + 1));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x + 1, pChunkPos.z - 1));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x + 1, pChunkPos.z + 1));
    }

    private void initializeQueueForFullUpdate(Camera pCamera, Queue<SectionOcclusionGraph.Node> pNodeQueue) {
        BlockPos blockpos = pCamera.getBlockPosition();
        long i = SectionPos.asLong(blockpos);
        int j = SectionPos.y(i);
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSection(i);
        if (sectionrenderdispatcher$rendersection == null) {
            LevelHeightAccessor levelheightaccessor = this.viewArea.getLevelHeightAccessor();
            boolean flag = j < levelheightaccessor.getMinSectionY();
            int k = flag ? levelheightaccessor.getMinSectionY() : levelheightaccessor.getMaxSectionY();
            int l = this.viewArea.getViewDistance();
            List<SectionOcclusionGraph.Node> list = Lists.newArrayList();
            int i1 = SectionPos.x(i);
            int j1 = SectionPos.z(i);

            for (int k1 = -l; k1 <= l; k1++) {
                for (int l1 = -l; l1 <= l; l1++) {
                    SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = this.viewArea
                        .getRenderSection(SectionPos.asLong(k1 + i1, k, l1 + j1));
                    if (sectionrenderdispatcher$rendersection1 != null && this.isInViewDistance(i, sectionrenderdispatcher$rendersection1.getSectionNode())) {
                        Direction direction = flag ? Direction.UP : Direction.DOWN;
                        SectionOcclusionGraph.Node sectionocclusiongraph$node = sectionrenderdispatcher$rendersection1.getRenderInfo(direction, 0);
                        sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, direction);
                        if (k1 > 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.EAST);
                        } else if (k1 < 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.WEST);
                        }

                        if (l1 > 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.SOUTH);
                        } else if (l1 < 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.NORTH);
                        }

                        list.add(sectionocclusiongraph$node);
                    }
                }
            }

            list.sort(Comparator.comparingDouble(nodeIn -> blockpos.distSqr(nodeIn.section.getOrigin().offset(8, 8, 8))));
            pNodeQueue.addAll(list);
        } else {
            pNodeQueue.add(sectionrenderdispatcher$rendersection.getRenderInfo(null, 0));
        }
    }

    private void runUpdates(
        SectionOcclusionGraph.GraphStorage pStorage,
        Vec3 pCameraPosition,
        Queue<SectionOcclusionGraph.Node> pQueue,
        boolean pSmartCull,
        Consumer<SectionRenderDispatcher.RenderSection> pVisibleSectionConsumer,
        LongOpenHashSet pLoadedEmptySection
    ) {
        int i = 16;
        BlockPos blockpos = new BlockPos(
            Mth.floor(pCameraPosition.x / 16.0) * 16, Mth.floor(pCameraPosition.y / 16.0) * 16, Mth.floor(pCameraPosition.z / 16.0) * 16
        );
        long j = SectionPos.asLong(blockpos);
        BlockPos blockpos1 = blockpos.offset(8, 8, 8);

        while (!pQueue.isEmpty()) {
            SectionOcclusionGraph.Node sectionocclusiongraph$node = pQueue.poll();
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = sectionocclusiongraph$node.section;
            if (!pLoadedEmptySection.contains(sectionocclusiongraph$node.section.getSectionNode())) {
                if (pStorage.sectionTree.add(sectionocclusiongraph$node.section)) {
                    pVisibleSectionConsumer.accept(sectionocclusiongraph$node.section);
                }
            } else {
                sectionocclusiongraph$node.section
                    .compiled
                    .compareAndSet(SectionRenderDispatcher.CompiledSection.UNCOMPILED, SectionRenderDispatcher.CompiledSection.EMPTY);
            }

            boolean flag = Math.abs(sectionrenderdispatcher$rendersection.getOrigin().getX() - blockpos.getX()) > 60
                || Math.abs(sectionrenderdispatcher$rendersection.getOrigin().getY() - blockpos.getY()) > 60
                || Math.abs(sectionrenderdispatcher$rendersection.getOrigin().getZ() - blockpos.getZ()) > 60;

            for (Direction direction : DIRECTIONS) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = this.getRelativeFrom(
                    j, sectionrenderdispatcher$rendersection, direction
                );
                if (sectionrenderdispatcher$rendersection1 != null && (!pSmartCull || !sectionocclusiongraph$node.hasDirection(direction.getOpposite()))) {
                    if (pSmartCull && sectionocclusiongraph$node.hasSourceDirections()) {
                        SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = sectionrenderdispatcher$rendersection.getCompiled();
                        boolean flag1 = false;

                        for (int k = 0; k < DIRECTIONS.length; k++) {
                            if (sectionocclusiongraph$node.hasSourceDirection(k)
                                && sectionrenderdispatcher$compiledsection.facesCanSeeEachother(DIRECTIONS[k].getOpposite(), direction)) {
                                flag1 = true;
                                break;
                            }
                        }

                        if (!flag1) {
                            continue;
                        }
                    }

                    if (pSmartCull && flag) {
                        BlockPos blockpos2 = sectionrenderdispatcher$rendersection1.getOrigin();
                        int i1 = (
                                direction.getAxis() == Direction.Axis.X
                                    ? blockpos1.getX() > blockpos2.getX()
                                    : blockpos1.getX() < blockpos2.getX()
                            )
                            ? 16
                            : 0;
                        int j1 = (
                                direction.getAxis() == Direction.Axis.Y
                                    ? blockpos1.getY() > blockpos2.getY()
                                    : blockpos1.getY() < blockpos2.getY()
                            )
                            ? 16
                            : 0;
                        int l = (
                                direction.getAxis() == Direction.Axis.Z
                                    ? blockpos1.getZ() > blockpos2.getZ()
                                    : blockpos1.getZ() < blockpos2.getZ()
                            )
                            ? 16
                            : 0;
                        Vec3M vec3m = pStorage.vec3M1
                            .set((double)(blockpos2.getX() + i1), (double)(blockpos2.getY() + j1), (double)(blockpos2.getZ() + l));
                        Vec3M vec3m1 = pStorage.vec3M2.set(pCameraPosition).subtract(vec3m).normalize().scale(CEILED_SECTION_DIAGONAL);
                        boolean flag2 = true;

                        while (pStorage.vec3M3.set(pCameraPosition).subtract(vec3m).lengthSqr() > 3600.0) {
                            vec3m = vec3m.add(vec3m1);
                            LevelHeightAccessor levelheightaccessor = this.viewArea.getLevelHeightAccessor();
                            if (vec3m.y > (double)levelheightaccessor.getMaxY() || vec3m.y < (double)levelheightaccessor.getMinY()) {
                                break;
                            }

                            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection2 = this.viewArea
                                .getRenderSectionAt(pStorage.blockPosM1.setXyz(vec3m.x, vec3m.y, vec3m.z));
                            if (sectionrenderdispatcher$rendersection2 == null || pStorage.sectionToNodeMap.get(sectionrenderdispatcher$rendersection2) == null
                                )
                             {
                                flag2 = false;
                                break;
                            }
                        }

                        if (!flag2) {
                            continue;
                        }
                    }

                    SectionOcclusionGraph.Node sectionocclusiongraph$node1 = pStorage.sectionToNodeMap.get(sectionrenderdispatcher$rendersection1);
                    if (sectionocclusiongraph$node1 != null) {
                        sectionocclusiongraph$node1.addSourceDirection(direction);
                    } else {
                        SectionOcclusionGraph.Node sectionocclusiongraph$node2 = sectionrenderdispatcher$rendersection1.getRenderInfo(
                            direction, sectionocclusiongraph$node.step + 1
                        );
                        sectionocclusiongraph$node2.setDirections(sectionocclusiongraph$node.directions, direction);
                        if (sectionrenderdispatcher$rendersection1.hasAllNeighbors()) {
                            pQueue.add(sectionocclusiongraph$node2);
                            pStorage.sectionToNodeMap.put(sectionrenderdispatcher$rendersection1, sectionocclusiongraph$node2);
                        } else if (this.isInViewDistance(j, sectionrenderdispatcher$rendersection1.getSectionNode())) {
                            pStorage.sectionToNodeMap.put(sectionrenderdispatcher$rendersection1, sectionocclusiongraph$node2);
                            pStorage.chunksWaitingForNeighbors
                                .computeIfAbsent(ChunkPos.asLong(sectionrenderdispatcher$rendersection1.getOrigin()), posLongIn -> new ArrayList<>())
                                .add(sectionrenderdispatcher$rendersection1);
                        }
                    }
                }
            }
        }
    }

    private boolean isInViewDistance(long pCenterPos, long pPos) {
        return ChunkTrackingView.isInViewDistance(
            SectionPos.x(pCenterPos),
            SectionPos.z(pCenterPos),
            this.viewArea.getViewDistance(),
            SectionPos.x(pPos),
            SectionPos.z(pPos)
        );
    }

    @Nullable
    private SectionRenderDispatcher.RenderSection getRelativeFrom(long pSectionPos, SectionRenderDispatcher.RenderSection pSection, Direction pDirection) {
        long i = pSection.getNeighborSectionNode(pDirection);
        int j = SectionPos.sectionToBlockCoord(SectionPos.y(pSectionPos));
        int k = SectionPos.sectionToBlockCoord(SectionPos.y(i));
        ClientLevel clientlevel = this.levelRenderer.level;
        if (k >= clientlevel.getMinY() && k < clientlevel.getMaxY()) {
            if (Mth.abs(j - k) > this.levelRenderer.renderDistance) {
                return null;
            } else {
                int l = SectionPos.sectionToBlockCoord(SectionPos.x(pSectionPos));
                int i1 = SectionPos.sectionToBlockCoord(SectionPos.z(pSectionPos));
                int j1 = SectionPos.sectionToBlockCoord(SectionPos.x(i));
                int k1 = SectionPos.sectionToBlockCoord(SectionPos.z(i));
                int l1 = l - j1;
                int i2 = i1 - k1;
                int j2 = l1 * l1 + i2 * i2;
                return j2 > this.levelRenderer.renderDistanceXZSq ? null : this.viewArea.getRenderSection(i);
            }
        } else {
            return null;
        }
    }

    @Nullable
    @VisibleForDebug
    public SectionOcclusionGraph.Node getNode(SectionRenderDispatcher.RenderSection pSection) {
        return this.currentGraph.get().storage.sectionToNodeMap.get(pSection);
    }

    public Octree getOctree() {
        return this.currentGraph.get().storage.sectionTree;
    }

    public boolean needsFrustumUpdate() {
        return this.needsFrustumUpdate.get();
    }

    public void setNeedsFrustumUpdate(boolean val) {
        this.needsFrustumUpdate.set(val);
    }

    static record GraphEvents(LongSet chunksWhichReceivedNeighbors, BlockingQueue<SectionRenderDispatcher.RenderSection> sectionsToPropagateFrom) {
        GraphEvents() {
            this(new LongOpenHashSet(), new LinkedBlockingQueue<>());
        }
    }

    static record GraphState(SectionOcclusionGraph.GraphStorage storage, SectionOcclusionGraph.GraphEvents events) {
        GraphState(ViewArea pViewArea) {
            this(new SectionOcclusionGraph.GraphStorage(pViewArea), new SectionOcclusionGraph.GraphEvents());
        }
    }

    static class GraphStorage {
        public final SectionOcclusionGraph.SectionToNodeMap sectionToNodeMap;
        public final Octree sectionTree;
        public final Long2ObjectMap<List<SectionRenderDispatcher.RenderSection>> chunksWaitingForNeighbors;
        public final Vec3M vec3M1 = new Vec3M(0.0, 0.0, 0.0);
        public final Vec3M vec3M2 = new Vec3M(0.0, 0.0, 0.0);
        public final Vec3M vec3M3 = new Vec3M(0.0, 0.0, 0.0);
        public final BlockPosM blockPosM1 = new BlockPosM();

        public GraphStorage(ViewArea pViewArea) {
            this.sectionToNodeMap = new SectionOcclusionGraph.SectionToNodeMap(pViewArea.sections.length);
            this.sectionTree = new Octree(pViewArea.getCameraSectionPos(), pViewArea.getViewDistance(), pViewArea.sectionGridSizeY, pViewArea.level.getMinY());
            this.chunksWaitingForNeighbors = new Long2ObjectOpenHashMap<>();
        }

        @Override
        public String toString() {
            return "sectionToNode: " + this.sectionToNodeMap + ", sectionTree: " + this.sectionTree + ", sectionsWaiting: " + this.chunksWaitingForNeighbors;
        }
    }

    @VisibleForDebug
    public static class Node {
        @VisibleForDebug
        public final SectionRenderDispatcher.RenderSection section;
        private int sourceDirections;
        int directions;
        @VisibleForDebug
        public int step;

        public Node(SectionRenderDispatcher.RenderSection pSection, @Nullable Direction pSourceDirection, int pStep) {
            this.section = pSection;
            if (pSourceDirection != null) {
                this.addSourceDirection(pSourceDirection);
            }

            this.step = pStep;
        }

        void setDirections(int directionsIn, Direction directionIn) {
            this.directions = this.directions | directionsIn | 1 << directionIn.ordinal();
        }

        public void initialize(Direction facingIn, int counter) {
            this.sourceDirections = facingIn != null ? 1 << facingIn.ordinal() : 0;
            this.directions = 0;
            this.step = counter;
        }

        @Override
        public String toString() {
            return this.section.getOrigin() + "";
        }

        boolean hasDirection(Direction pDirection) {
            return (this.directions & 1 << pDirection.ordinal()) > 0;
        }

        void addSourceDirection(Direction pSourceDirection) {
            this.sourceDirections = (byte)(this.sourceDirections | this.sourceDirections | 1 << pSourceDirection.ordinal());
        }

        @VisibleForDebug
        public boolean hasSourceDirection(int pDirection) {
            return (this.sourceDirections & 1 << pDirection) > 0;
        }

        boolean hasSourceDirections() {
            return this.sourceDirections != 0;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(this.section.getSectionNode());
        }

        @Override
        public boolean equals(Object pOther) {
            return pOther instanceof SectionOcclusionGraph.Node sectionocclusiongraph$node
                ? this.section.getSectionNode() == sectionocclusiongraph$node.section.getSectionNode()
                : false;
        }
    }

    static class SectionToNodeMap {
        private final SectionOcclusionGraph.Node[] nodes;

        SectionToNodeMap(int pSize) {
            this.nodes = new SectionOcclusionGraph.Node[pSize];
        }

        public void put(SectionRenderDispatcher.RenderSection pSection, SectionOcclusionGraph.Node pNode) {
            this.nodes[pSection.index] = pNode;
        }

        @Nullable
        public SectionOcclusionGraph.Node get(SectionRenderDispatcher.RenderSection pSection) {
            int i = pSection.index;
            return i >= 0 && i < this.nodes.length ? this.nodes[i] : null;
        }

        @Override
        public String toString() {
            StringBuilder stringbuilder = new StringBuilder();
            int i = 0;

            for (int j = 0; j < this.nodes.length; j++) {
                SectionOcclusionGraph.Node sectionocclusiongraph$node = this.nodes[j];
                if (sectionocclusiongraph$node != null) {
                    if (!stringbuilder.isEmpty()) {
                        stringbuilder.append(", ");
                    }

                    stringbuilder.append(j + ":" + sectionocclusiongraph$node);
                    if (i++ > 100) {
                        stringbuilder.append(", ...");
                        break;
                    }
                }
            }

            return "[" + stringbuilder.toString() + "]";
        }
    }
}