package net.optifine;

import java.util.Comparator;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.util.Mth;

public class ChunkPosComparator implements Comparator<SectionRenderDispatcher.RenderSection> {
    private int chunkPosX;
    private int chunkPosZ;
    private double yawRad;
    private double pitchNorm;

    public ChunkPosComparator(int chunkPosX, int chunkPosZ, double yawRad, double pitchRad) {
        this.chunkPosX = chunkPosX;
        this.chunkPosZ = chunkPosZ;
        this.yawRad = yawRad;
        this.pitchNorm = 1.0 - Mth.clamp(Math.abs(pitchRad) / (Math.PI / 2), 0.0, 1.0);
    }

    public int compare(SectionRenderDispatcher.RenderSection rs1, SectionRenderDispatcher.RenderSection rs2) {
        int i = rs1.getSectionPosition().x();
        int j = rs1.getSectionPosition().z();
        int k = rs2.getSectionPosition().x();
        int l = rs2.getSectionPosition().z();
        int i1 = this.getDistSq(i, j);
        int j1 = this.getDistSq(k, l);
        return i1 - j1;
    }

    private int getDistSq(int cpx, int cpz) {
        int i = cpx - this.chunkPosX;
        int j = cpz - this.chunkPosZ;
        int k = i * i + j * j;
        double d0 = Mth.atan2((double)j, (double)i);
        double d1 = Math.abs(d0 - this.yawRad);
        if (d1 > Math.PI) {
            d1 = (Math.PI * 2) - d1;
        }

        return (int)((double)k * (0.2 + this.pitchNorm * d1));
    }
}