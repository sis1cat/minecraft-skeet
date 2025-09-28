package net.optifine;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class DynamicLight {
    private Entity entity = null;
    private double offsetY = 0.0;
    private double lastPosX = -2.1474836E9F;
    private double lastPosY = -2.1474836E9F;
    private double lastPosZ = -2.1474836E9F;
    private int lastLightLevel = 0;
    private long timeCheckMs = 0L;
    private Set<BlockPos> setLitChunkPos = new HashSet<>();

    public DynamicLight(Entity entity) {
        this.entity = entity;
        this.offsetY = (double)entity.getEyeHeight();
    }

    public void update(LevelRenderer renderGlobal) {
        if (Config.isDynamicLightsFast()) {
            long i = System.currentTimeMillis();
            if (i < this.timeCheckMs + 500L) {
                return;
            }

            this.timeCheckMs = i;
        }

        double d6 = this.entity.getX() - 0.5;
        double d0 = this.entity.getY() - 0.5 + this.offsetY;
        double d1 = this.entity.getZ() - 0.5;
        int j = DynamicLights.getLightLevel(this.entity);
        double d2 = d6 - this.lastPosX;
        double d3 = d0 - this.lastPosY;
        double d4 = d1 - this.lastPosZ;
        double d5 = 0.1;
        if (!(Math.abs(d2) <= d5) || !(Math.abs(d3) <= d5) || !(Math.abs(d4) <= d5) || this.lastLightLevel != j) {
            this.lastPosX = d6;
            this.lastPosY = d0;
            this.lastPosZ = d1;
            this.lastLightLevel = j;
            Set<BlockPos> set = new HashSet<>();
            if (j > 0) {
                Direction direction = (Mth.floor(d6) & 15) >= 8 ? Direction.EAST : Direction.WEST;
                Direction direction1 = (Mth.floor(d0) & 15) >= 8 ? Direction.UP : Direction.DOWN;
                Direction direction2 = (Mth.floor(d1) & 15) >= 8 ? Direction.SOUTH : Direction.NORTH;
                long k = this.getChunkPos(d6, d0, d1);
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = renderGlobal.getRenderChunk(k);
                long l = this.getChunkPos(sectionrenderdispatcher$rendersection, k, direction);
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = renderGlobal.getRenderChunk(l);
                long i1 = this.getChunkPos(sectionrenderdispatcher$rendersection, k, direction2);
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection2 = renderGlobal.getRenderChunk(i1);
                long j1 = this.getChunkPos(sectionrenderdispatcher$rendersection1, l, direction2);
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection3 = renderGlobal.getRenderChunk(j1);
                long k1 = this.getChunkPos(sectionrenderdispatcher$rendersection, k, direction1);
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection4 = renderGlobal.getRenderChunk(k1);
                long l1 = this.getChunkPos(sectionrenderdispatcher$rendersection4, k1, direction);
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection5 = renderGlobal.getRenderChunk(l1);
                long i2 = this.getChunkPos(sectionrenderdispatcher$rendersection4, k1, direction2);
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection6 = renderGlobal.getRenderChunk(i2);
                long j2 = this.getChunkPos(sectionrenderdispatcher$rendersection5, l1, direction2);
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection7 = renderGlobal.getRenderChunk(j2);
                this.updateChunkLight(sectionrenderdispatcher$rendersection, this.setLitChunkPos, set);
                this.updateChunkLight(sectionrenderdispatcher$rendersection1, this.setLitChunkPos, set);
                this.updateChunkLight(sectionrenderdispatcher$rendersection2, this.setLitChunkPos, set);
                this.updateChunkLight(sectionrenderdispatcher$rendersection3, this.setLitChunkPos, set);
                this.updateChunkLight(sectionrenderdispatcher$rendersection4, this.setLitChunkPos, set);
                this.updateChunkLight(sectionrenderdispatcher$rendersection5, this.setLitChunkPos, set);
                this.updateChunkLight(sectionrenderdispatcher$rendersection6, this.setLitChunkPos, set);
                this.updateChunkLight(sectionrenderdispatcher$rendersection7, this.setLitChunkPos, set);
            }

            this.updateLitChunks(renderGlobal);
            this.setLitChunkPos = set;
        }
    }

    private long getChunkPos(double x, double y, double z) {
        int i = SectionPos.blockToSectionCoord(Mth.floor(x));
        int j = SectionPos.blockToSectionCoord(Mth.floor(y));
        int k = SectionPos.blockToSectionCoord(Mth.floor(z));
        return SectionPos.asLong(i, j, k);
    }

    private long getChunkPos(SectionRenderDispatcher.RenderSection renderChunk, long pos, Direction facing) {
        int i = SectionPos.x(pos);
        int j = SectionPos.y(pos);
        int k = SectionPos.z(pos);
        i += facing.getStepX();
        j += facing.getStepY();
        k += facing.getStepZ();
        return SectionPos.asLong(i, j, k);
    }

    private void updateChunkLight(SectionRenderDispatcher.RenderSection renderChunk, Set<BlockPos> setPrevPos, Set<BlockPos> setNewPos) {
        if (renderChunk != null) {
            SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = renderChunk.getCompiled();
            if (sectionrenderdispatcher$compiledsection != null && !sectionrenderdispatcher$compiledsection.isEmpty()) {
                renderChunk.setDirty(false);
                renderChunk.setNeedsBackgroundPriorityUpdate(true);
            }

            BlockPos blockpos = renderChunk.getOrigin().immutable();
            if (setPrevPos != null) {
                setPrevPos.remove(blockpos);
            }

            if (setNewPos != null) {
                setNewPos.add(blockpos);
            }
        }
    }

    public void updateLitChunks(LevelRenderer renderGlobal) {
        for (BlockPos blockpos : this.setLitChunkPos) {
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = renderGlobal.getRenderChunk(blockpos);
            this.updateChunkLight(sectionrenderdispatcher$rendersection, null, null);
        }
    }

    public Entity getEntity() {
        return this.entity;
    }

    public double getLastPosX() {
        return this.lastPosX;
    }

    public double getLastPosY() {
        return this.lastPosY;
    }

    public double getLastPosZ() {
        return this.lastPosZ;
    }

    public int getLastLightLevel() {
        return this.lastLightLevel;
    }

    public double getOffsetY() {
        return this.offsetY;
    }

    @Override
    public String toString() {
        return "Entity: " + this.entity + ", offsetY: " + this.offsetY;
    }
}