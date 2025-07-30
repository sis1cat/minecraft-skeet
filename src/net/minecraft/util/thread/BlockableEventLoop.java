package net.minecraft.util.thread;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.CheckReturnValue;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.MetricsRegistry;
import net.minecraft.util.profiling.metrics.ProfilerMeasured;
import net.optifine.Config;
import net.optifine.util.PacketRunnable;
import org.slf4j.Logger;

public abstract class BlockableEventLoop<R extends Runnable> implements ProfilerMeasured, TaskScheduler<R>, Executor {
    public static final long BLOCK_TIME_NANOS = 100000L;
    private final String name;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Queue<R> pendingRunnables = Queues.newConcurrentLinkedQueue();
    private int blockingCount;

    protected BlockableEventLoop(String pName) {
        this.name = pName;
        MetricsRegistry.INSTANCE.add(this);
        if (this instanceof Minecraft minecraft) {
            Config.initMinecraft(minecraft);
        }
    }

    protected abstract boolean shouldRun(R pRunnable);

    public boolean isSameThread() {
        return Thread.currentThread() == this.getRunningThread();
    }

    protected abstract Thread getRunningThread();

    protected boolean scheduleExecutables() {
        return !this.isSameThread();
    }

    public int getPendingTasksCount() {
        return this.pendingRunnables.size();
    }

    @Override
    public String name() {
        return this.name;
    }

    public <V> CompletableFuture<V> submit(Supplier<V> pSupplier) {
        return this.scheduleExecutables() ? CompletableFuture.supplyAsync(pSupplier, this) : CompletableFuture.completedFuture(pSupplier.get());
    }

    private CompletableFuture<Void> submitAsync(Runnable pTask) {
        return CompletableFuture.supplyAsync(() -> {
            pTask.run();
            return null;
        }, this);
    }

    @CheckReturnValue
    public CompletableFuture<Void> submit(Runnable pTask) {
        if (this.scheduleExecutables()) {
            return this.submitAsync(pTask);
        } else {
            pTask.run();
            return CompletableFuture.completedFuture(null);
        }
    }

    public void executeBlocking(Runnable pTask) {
        if (!this.isSameThread()) {
            this.submitAsync(pTask).join();
        } else {
            pTask.run();
        }
    }

    @Override
    public void schedule(R p_18712_) {
        this.pendingRunnables.add(p_18712_);
        LockSupport.unpark(this.getRunningThread());
    }

    @Override
    public void execute(Runnable pTask) {
        if (this.scheduleExecutables()) {
            this.schedule(this.wrapRunnable(pTask));
        } else {
            pTask.run();
        }
    }

    public void executeIfPossible(Runnable pTask) {
        this.execute(pTask);
    }

    protected void dropAllTasks() {
        this.pendingRunnables.clear();
    }

    protected void runAllTasks() {
        int i = Integer.MAX_VALUE;
        if (Config.isLazyChunkLoading() && this == Minecraft.getInstance()) {
            i = this.getTaskCount();
        }

        while (this.pollTask()) {
            if (--i <= 0) {
                break;
            }
        }
    }

    public boolean pollTask() {
        R r = this.pendingRunnables.peek();
        if (r == null) {
            return false;
        } else if (this.blockingCount == 0 && !this.shouldRun(r)) {
            return false;
        } else {
            this.doRunTask(this.pendingRunnables.remove());
            return true;
        }
    }

    public void managedBlock(BooleanSupplier pIsDone) {
        this.blockingCount++;

        try {
            while (!pIsDone.getAsBoolean()) {
                if (!this.pollTask()) {
                    this.waitForTasks();
                }
            }
        } finally {
            this.blockingCount--;
        }
    }

    protected void waitForTasks() {
        Thread.yield();
        LockSupport.parkNanos("waiting for tasks", 100000L);
    }

    protected void doRunTask(R pTask) {
        try (Zone zone = TracyClient.beginZone("Task", SharedConstants.IS_RUNNING_IN_IDE)) {
            pTask.run();
        } catch (Exception exception1) {
            LOGGER.error(LogUtils.FATAL_MARKER, "Error executing task on {}", this.name(), exception1);
            if (isNonRecoverable(exception1)) {
                throw exception1;
            }
        }
    }

    @Override
    public List<MetricSampler> profiledMetrics() {
        return ImmutableList.of(MetricSampler.create(this.name + "-pending-tasks", MetricCategory.EVENT_LOOPS, this::getPendingTasksCount));
    }

    public static boolean isNonRecoverable(Throwable pError) {
        return pError instanceof ReportedException reportedexception
            ? isNonRecoverable(reportedexception.getCause())
            : pError instanceof OutOfMemoryError || pError instanceof StackOverflowError;
    }

    private int getTaskCount() {
        if (this.pendingRunnables.isEmpty()) {
            return 0;
        } else {
            R[] ar = (R[])this.pendingRunnables.toArray(new Runnable[this.pendingRunnables.size()]);
            double d0 = this.getChunkUpdateWeight(ar);
            if (d0 < 5.0) {
                return Integer.MAX_VALUE;
            } else {
                int i = ar.length;
                int j = Math.max(Config.getFpsAverage(), 1);
                double d1 = (double)(i * 10 / j);
                return this.getCount(ar, d1);
            }
        }
    }

    private int getCount(R[] rs, double maxWeight) {
        double d0 = 0.0;

        for (int i = 0; i < rs.length; i++) {
            R r = rs[i];
            d0 += this.getChunkUpdateWeight(r);
            if (d0 > maxWeight) {
                return i + 1;
            }
        }

        return rs.length;
    }

    private double getChunkUpdateWeight(R[] rs) {
        double d0 = 0.0;

        for (int i = 0; i < rs.length; i++) {
            R r = rs[i];
            d0 += this.getChunkUpdateWeight(r);
        }

        return d0;
    }

    private double getChunkUpdateWeight(Runnable r) {
        if (r instanceof PacketRunnable packetrunnable) {
            Packet packet = packetrunnable.getPacket();
            if (packet instanceof ClientboundLevelChunkWithLightPacket) {
                return 1.0;
            }

            if (packet instanceof ClientboundLightUpdatePacket) {
                return 0.2;
            }

            if (packet instanceof ClientboundForgetLevelChunkPacket) {
                return 2.6;
            }
        }

        return 0.0;
    }
}