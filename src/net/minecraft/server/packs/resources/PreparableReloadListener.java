package net.minecraft.server.packs.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface PreparableReloadListener {
    CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier pBarrier, ResourceManager pManager, Executor pBackgroundExecutor, Executor pGameExecutor);

    default String getName() {
        return this.getClass().getSimpleName();
    }

    public interface PreparationBarrier {
        <T> CompletableFuture<T> wait(T pBackgroundResult);
    }
}