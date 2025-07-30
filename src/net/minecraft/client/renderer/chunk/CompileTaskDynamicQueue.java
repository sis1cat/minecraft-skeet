package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.ListIterator;
import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CompileTaskDynamicQueue {
    private static final int MAX_RECOMPILE_QUOTA = 2;
    private int recompileQuota = 2;
    private final List<SectionRenderDispatcher.RenderSection.CompileTask> tasks = new ObjectArrayList<>();

    public synchronized void add(SectionRenderDispatcher.RenderSection.CompileTask pTask) {
        this.tasks.add(pTask);
    }

    @Nullable
    public synchronized SectionRenderDispatcher.RenderSection.CompileTask poll(Vec3 pCameraPosition) {
        int i = -1;
        int j = -1;
        double d0 = Double.MAX_VALUE;
        double d1 = Double.MAX_VALUE;
        ListIterator<SectionRenderDispatcher.RenderSection.CompileTask> listiterator = this.tasks.listIterator();

        while (listiterator.hasNext()) {
            int k = listiterator.nextIndex();
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = listiterator.next();
            if (sectionrenderdispatcher$rendersection$compiletask.isCancelled.get()) {
                listiterator.remove();
            } else {
                double d2 = sectionrenderdispatcher$rendersection$compiletask.getOrigin().distToCenterSqr(pCameraPosition);
                if (!sectionrenderdispatcher$rendersection$compiletask.isRecompile() && d2 < d0) {
                    d0 = d2;
                    i = k;
                }

                if (sectionrenderdispatcher$rendersection$compiletask.isRecompile() && d2 < d1) {
                    d1 = d2;
                    j = k;
                }
            }
        }

        boolean flag = j >= 0;
        boolean flag1 = i >= 0;
        if (!flag || flag1 && (this.recompileQuota <= 0 || !(d1 < d0))) {
            this.recompileQuota = 2;
            return this.removeTaskByIndex(i);
        } else {
            this.recompileQuota--;
            return this.removeTaskByIndex(j);
        }
    }

    public int size() {
        return this.tasks.size();
    }

    @Nullable
    private SectionRenderDispatcher.RenderSection.CompileTask removeTaskByIndex(int pIndex) {
        return pIndex >= 0 ? this.tasks.remove(pIndex) : null;
    }

    public synchronized void clear() {
        for (SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask : this.tasks) {
            sectionrenderdispatcher$rendersection$compiletask.cancel();
        }

        this.tasks.clear();
    }
}