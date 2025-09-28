package net.optifine;

import java.util.Comparator;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;

public class SectionPosComparator implements Comparator<SectionRenderDispatcher.RenderSection> {
    private int chunkPosX;
    private int chunkPosY;
    private int chunkPosZ;

    public SectionPosComparator(SectionPos sectionPos) {
        this.chunkPosX = sectionPos.x();
        this.chunkPosY = sectionPos.y();
        this.chunkPosZ = sectionPos.z();
    }

    public int compare(SectionRenderDispatcher.RenderSection rs1, SectionRenderDispatcher.RenderSection rs2) {
        SectionPos sectionpos = rs1.getSectionPosition();
        SectionPos sectionpos1 = rs2.getSectionPosition();
        int i = this.getDistSq(sectionpos);
        int j = this.getDistSq(sectionpos1);
        return i - j;
    }

    public int getDistSq(SectionPos sp) {
        int i = sp.x() - this.chunkPosX;
        int j = sp.y() - this.chunkPosY;
        int k = sp.z() - this.chunkPosZ;
        return i * i + j * j + k * k;
    }
}