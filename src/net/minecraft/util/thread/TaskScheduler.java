package net.minecraft.util.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public interface TaskScheduler<R extends Runnable> extends AutoCloseable {
    String name();

    void schedule(R pTask);

    @Override
    default void close() {
    }

    R wrapRunnable(Runnable pRunnable);

    default <Source> CompletableFuture<Source> scheduleWithResult(Consumer<CompletableFuture<Source>> pResultConsumer) {
        CompletableFuture<Source> completablefuture = new CompletableFuture<>();
        this.schedule(this.wrapRunnable(() -> pResultConsumer.accept(completablefuture)));
        return completablefuture;
    }

    static TaskScheduler<Runnable> wrapExecutor(final String pName, final Executor pExecutor) {
        return new TaskScheduler<Runnable>() {
            @Override
            public String name() {
                return pName;
            }

            @Override
            public void schedule(Runnable p_361412_) {
                pExecutor.execute(p_361412_);
            }

            @Override
            public Runnable wrapRunnable(Runnable p_367104_) {
                return p_367104_;
            }

            @Override
            public String toString() {
                return pName;
            }
        };
    }
}