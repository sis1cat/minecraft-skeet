package net.optifine.util;

import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.optifine.ChunkPosComparator;
import net.optifine.SectionPosComparator;

public class SectionUtils {
    public static Collection<SectionRenderDispatcher.RenderSection> getSortedDist(List<SectionRenderDispatcher.RenderSection> listSections, LocalPlayer player) {
        if (listSections.size() <= 1) {
            return listSections;
        } else {
            SectionPos sectionpos = SectionPos.of(player.blockPosition());
            SectionPosComparator sectionposcomparator = new SectionPosComparator(sectionpos);
            PriorityQueue<SectionRenderDispatcher.RenderSection> priorityqueue = new PriorityQueue<>(listSections.size(), sectionposcomparator);

            for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : listSections) {
                priorityqueue.add(sectionrenderdispatcher$rendersection);
            }

            return priorityqueue;
        }
    }

    public static Collection<SectionRenderDispatcher.RenderSection> getSortedDistDir(
        List<SectionRenderDispatcher.RenderSection> listSections, LocalPlayer player
    ) {
        if (listSections.size() <= 1) {
            return listSections;
        } else {
            float f = player.getYRot() + 90.0F;

            while (f <= -180.0F) {
                f += 360.0F;
            }

            while (f > 180.0F) {
                f -= 360.0F;
            }

            double d0 = (double)f * (Math.PI / 180.0);
            double d1 = (double)player.getXRot();
            double d2 = d1 * (Math.PI / 180.0);
            ChunkPos chunkpos = player.chunkPosition();
            ChunkPosComparator chunkposcomparator = new ChunkPosComparator(chunkpos.x, chunkpos.z, d0, d2);
            PriorityQueue<SectionRenderDispatcher.RenderSection> priorityqueue = new PriorityQueue<>(listSections.size(), chunkposcomparator);

            for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : listSections) {
                priorityqueue.add(sectionrenderdispatcher$rendersection);
            }

            return priorityqueue;
        }
    }
}