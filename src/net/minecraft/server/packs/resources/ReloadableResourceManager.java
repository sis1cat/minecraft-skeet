package net.minecraft.server.packs.resources;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.Unit;
import net.optifine.util.TextureUtils;
import org.slf4j.Logger;

public class ReloadableResourceManager implements ResourceManager, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private CloseableResourceManager resources;
    private final List<PreparableReloadListener> listeners = Lists.newArrayList();
    private final PackType type;

    public ReloadableResourceManager(PackType pType) {
        this.type = pType;
        this.resources = new MultiPackResourceManager(pType, List.of());
    }

    @Override
    public void close() {
        this.resources.close();
    }

    public void registerReloadListener(PreparableReloadListener pListener) {
        this.listeners.add(pListener);
    }

    public ReloadInstance createReload(Executor pBackgroundExecutor, Executor pGameExecutor, CompletableFuture<Unit> pWaitingFor, List<PackResources> pResourcePacks) {
        LOGGER.info("Reloading ResourceManager: {}", LogUtils.defer(() -> pResourcePacks.stream().map(PackResources::packId).collect(Collectors.joining(", "))));
        this.resources.close();
        this.resources = new MultiPackResourceManager(this.type, pResourcePacks);
        if (Minecraft.getInstance().getResourceManager() == this) {
            TextureUtils.resourcesPreReload(this);
        }

        return SimpleReloadInstance.create(this.resources, this.listeners, pBackgroundExecutor, pGameExecutor, pWaitingFor, LOGGER.isDebugEnabled());
    }

    @Override
    public Optional<Resource> getResource(ResourceLocation p_215494_) {
        return this.resources.getResource(p_215494_);
    }

    @Override
    public Set<String> getNamespaces() {
        return this.resources.getNamespaces();
    }

    @Override
    public List<Resource> getResourceStack(ResourceLocation p_215486_) {
        return this.resources.getResourceStack(p_215486_);
    }

    @Override
    public Map<ResourceLocation, Resource> listResources(String p_215488_, Predicate<ResourceLocation> p_215489_) {
        return this.resources.listResources(p_215488_, p_215489_);
    }

    @Override
    public Map<ResourceLocation, List<Resource>> listResourceStacks(String p_215491_, Predicate<ResourceLocation> p_215492_) {
        return this.resources.listResourceStacks(p_215491_, p_215492_);
    }

    @Override
    public Stream<PackResources> listPacks() {
        return this.resources.listPacks();
    }

    public void registerReloadListenerIfNotPresent(PreparableReloadListener listener) {
        if (!this.listeners.contains(listener)) {
            this.registerReloadListener(listener);
        }
    }
}