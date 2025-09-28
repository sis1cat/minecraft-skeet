package net.minecraft.client.renderer;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.optifine.Config;
import net.optifine.render.ClearVertexBuffersTask;
import net.optifine.render.VboRegion;

public class ViewArea {
    protected final LevelRenderer levelRenderer;
    protected final Level level;
    protected int sectionGridSizeY;
    protected int sectionGridSizeX;
    protected int sectionGridSizeZ;
    private int viewDistance;
    private SectionPos cameraSectionPos;
    public SectionRenderDispatcher.RenderSection[] sections;
    private Map<ChunkPos, VboRegion[]> mapVboRegions = new HashMap<>();
    private int lastCleanIndex = 0;

    public ViewArea(SectionRenderDispatcher pSectionRenderDispatcher, Level pLevel, int pViewDistance, LevelRenderer pLevelRenderer) {
        this.levelRenderer = pLevelRenderer;
        this.level = pLevel;
        this.setViewDistance(pViewDistance);
        this.createSections(pSectionRenderDispatcher);
        this.cameraSectionPos = SectionPos.of(this.viewDistance + 1, 0, this.viewDistance + 1);
    }

    protected void createSections(SectionRenderDispatcher pSectionRenderDispatcher) {
        if (!Minecraft.getInstance().isSameThread()) {
            throw new IllegalStateException("createSections called from wrong thread: " + Thread.currentThread().getName());
        } else {
            int i = this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ;
            this.sections = new SectionRenderDispatcher.RenderSection[i];

            for (int j = 0; j < this.sectionGridSizeX; j++) {
                for (int k = 0; k < this.sectionGridSizeY; k++) {
                    for (int l = 0; l < this.sectionGridSizeZ; l++) {
                        int i1 = this.getSectionIndex(j, k, l);
                        this.sections[i1] = pSectionRenderDispatcher.new RenderSection(i1, SectionPos.asLong(j, k + this.level.getMinSectionY(), l));
                        if (Config.isRenderRegions()) {
                            this.updateVboRegion(this.sections[i1]);
                        }
                    }
                }
            }
        }
    }

    public void releaseAllBuffers() {
        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.sections) {
            sectionrenderdispatcher$rendersection.releaseBuffers();
        }

        this.deleteVboRegions();
    }

    private int getSectionIndex(int pX, int pY, int pZ) {
        return (pZ * this.sectionGridSizeY + pY) * this.sectionGridSizeX + pX;
    }

    protected void setViewDistance(int pRenderDistanceChunks) {
        int i = pRenderDistanceChunks * 2 + 1;
        this.sectionGridSizeX = i;
        this.sectionGridSizeY = this.level.getSectionsCount();
        this.sectionGridSizeZ = i;
        this.viewDistance = pRenderDistanceChunks;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public LevelHeightAccessor getLevelHeightAccessor() {
        return this.level;
    }

    public void repositionCamera(SectionPos pNewSectionPos) {
        for (int i = 0; i < this.sectionGridSizeX; i++) {
            int j = pNewSectionPos.x() - this.viewDistance;
            int k = j + Math.floorMod(i - j, this.sectionGridSizeX);

            for (int l = 0; l < this.sectionGridSizeZ; l++) {
                int i1 = pNewSectionPos.z() - this.viewDistance;
                int j1 = i1 + Math.floorMod(l - i1, this.sectionGridSizeZ);

                for (int k1 = 0; k1 < this.sectionGridSizeY; k1++) {
                    int l1 = this.level.getMinSectionY() + k1;
                    SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.sections[this.getSectionIndex(i, k1, l)];
                    long i2 = sectionrenderdispatcher$rendersection.getSectionNode();
                    if (i2 != SectionPos.asLong(k, l1, j1)) {
                        sectionrenderdispatcher$rendersection.setSectionNode(SectionPos.asLong(k, l1, j1));
                    }
                }
            }
        }

        this.cameraSectionPos = pNewSectionPos;
        this.levelRenderer.getSectionOcclusionGraph().invalidate();
    }

    public SectionPos getCameraSectionPos() {
        return this.cameraSectionPos;
    }

    public void setDirty(int pSectionX, int pSectionY, int pSectionZ, boolean pReRenderOnMainThread) {
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.getRenderSection(pSectionX, pSectionY, pSectionZ);
        if (sectionrenderdispatcher$rendersection != null) {
            sectionrenderdispatcher$rendersection.setDirty(pReRenderOnMainThread);
        }
    }

    @Nullable
    public SectionRenderDispatcher.RenderSection getRenderSectionAt(BlockPos pPos) {
        return this.getRenderSection(SectionPos.asLong(pPos));
    }

    @Nullable
    protected SectionRenderDispatcher.RenderSection getRenderSection(long pSectionPos) {
        int i = SectionPos.x(pSectionPos);
        int j = SectionPos.y(pSectionPos);
        int k = SectionPos.z(pSectionPos);
        return this.getRenderSection(i, j, k);
    }

    @Nullable
    private SectionRenderDispatcher.RenderSection getRenderSection(int pX, int pY, int pZ) {
        if (!this.containsSection(pX, pY, pZ)) {
            return null;
        } else {
            int i = pY - this.level.getMinSectionY();
            int j = Math.floorMod(pX, this.sectionGridSizeX);
            int k = Math.floorMod(pZ, this.sectionGridSizeZ);
            return this.sections[this.getSectionIndex(j, i, k)];
        }
    }

    private boolean containsSection(int pX, int pY, int pZ) {
        if (pY >= this.level.getMinSectionY() && pY <= this.level.getMaxSectionY()) {
            return pX >= this.cameraSectionPos.x() - this.viewDistance && pX <= this.cameraSectionPos.x() + this.viewDistance
                ? pZ >= this.cameraSectionPos.z() - this.viewDistance && pZ <= this.cameraSectionPos.z() + this.viewDistance
                : false;
        } else {
            return false;
        }
    }

    private void updateVboRegion(SectionRenderDispatcher.RenderSection renderChunk) {
        BlockPos blockpos = renderChunk.getOrigin();
        int i = blockpos.getX() >> 8 << 8;
        int j = blockpos.getZ() >> 8 << 8;
        ChunkPos chunkpos = new ChunkPos(i, j);
        RenderType[] arendertype = RenderType.CHUNK_RENDER_TYPES;
        VboRegion[] avboregion = this.mapVboRegions.get(chunkpos);
        if (avboregion == null) {
            avboregion = new VboRegion[arendertype.length];

            for (int k = 0; k < arendertype.length; k++) {
                if (!arendertype[k].isNeedsSorting()) {
                    avboregion[k] = new VboRegion(arendertype[k]);
                }
            }

            this.mapVboRegions.put(chunkpos, avboregion);
        }

        for (int l = 0; l < arendertype.length; l++) {
            RenderType rendertype = arendertype[l];
            VboRegion vboregion = avboregion[l];
            renderChunk.getBuffer(rendertype).setVboRegion(vboregion);
        }
    }

    public void deleteVboRegions() {
        for (ChunkPos chunkpos : this.mapVboRegions.keySet()) {
            VboRegion[] avboregion = this.mapVboRegions.get(chunkpos);

            for (int i = 0; i < avboregion.length; i++) {
                VboRegion vboregion = avboregion[i];
                if (vboregion != null) {
                    vboregion.deleteGlBuffers();
                }

                avboregion[i] = null;
            }
        }

        this.mapVboRegions.clear();
    }

    public int getHighestUsedChunkIndex(int chunkX, int minChunkIndex, int chunkZ) {
        chunkX = Mth.positiveModulo(chunkX, this.sectionGridSizeX);
        minChunkIndex = Mth.clamp(minChunkIndex, 0, this.sectionGridSizeY);
        chunkZ = Mth.positiveModulo(chunkZ, this.sectionGridSizeZ);

        for (int i = this.sectionGridSizeY - 1; i >= minChunkIndex; i--) {
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.sections[this.getSectionIndex(chunkX, i, chunkZ)];
            if (!sectionrenderdispatcher$rendersection.getCompiled().isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    public void clearUnusedVbos() {
        int i = Config.limit(Config.getFpsAverage(), 1, 1000);
        int j = Config.limit(this.sections.length / (10 * i), 3, 100);
        int k = Config.limit(j / 3, 1, 3);
        int l = 0;
        int i1 = Config.limit(this.lastCleanIndex, 0, this.sections.length - 1);

        for (int j1 = Math.min(i1 + j, this.sections.length); i1 < j1 && l < k; i1++) {
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.sections[i1];
            ClearVertexBuffersTask clearvertexbufferstask = ClearVertexBuffersTask.make(
                sectionrenderdispatcher$rendersection.getCompiled().getLayersUsed(), sectionrenderdispatcher$rendersection
            );
            if (clearvertexbufferstask != null) {
                Minecraft.getInstance().levelRenderer.getSectionRenderDispatcher().addUploadTask(clearvertexbufferstask);
                l++;
            }
        }

        if (i1 >= this.sections.length) {
            i1 = 0;
        }

        this.lastCleanIndex = i1;
    }
}