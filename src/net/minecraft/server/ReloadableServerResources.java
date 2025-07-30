package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.util.Unit;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;

public class ReloadableServerResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final CompletableFuture<Unit> DATA_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
    private final ReloadableServerRegistries.Holder fullRegistryHolder;
    private final Commands commands;
    private final RecipeManager recipes;
    private final ServerAdvancementManager advancements;
    private final ServerFunctionLibrary functionLibrary;
    private final List<Registry.PendingTags<?>> postponedTags;

    private ReloadableServerResources(
        LayeredRegistryAccess<RegistryLayer> pRegistryAccess,
        HolderLookup.Provider pRegistries,
        FeatureFlagSet pEnabledFeatures,
        Commands.CommandSelection pCommandSelection,
        List<Registry.PendingTags<?>> pPostponedTags,
        int pFunctionCompilationLevel
    ) {
        this.fullRegistryHolder = new ReloadableServerRegistries.Holder(pRegistryAccess.compositeAccess());
        this.postponedTags = pPostponedTags;
        this.recipes = new RecipeManager(pRegistries);
        this.commands = new Commands(pCommandSelection, CommandBuildContext.simple(pRegistries, pEnabledFeatures));
        this.advancements = new ServerAdvancementManager(pRegistries);
        this.functionLibrary = new ServerFunctionLibrary(pFunctionCompilationLevel, this.commands.getDispatcher());
    }

    public ServerFunctionLibrary getFunctionLibrary() {
        return this.functionLibrary;
    }

    public ReloadableServerRegistries.Holder fullRegistries() {
        return this.fullRegistryHolder;
    }

    public RecipeManager getRecipeManager() {
        return this.recipes;
    }

    public Commands getCommands() {
        return this.commands;
    }

    public ServerAdvancementManager getAdvancements() {
        return this.advancements;
    }

    public List<PreparableReloadListener> listeners() {
        return List.of(this.recipes, this.functionLibrary, this.advancements);
    }

    public static CompletableFuture<ReloadableServerResources> loadResources(
        ResourceManager pResourceManager,
        LayeredRegistryAccess<RegistryLayer> pRegistryAccess,
        List<Registry.PendingTags<?>> pPostponedTags,
        FeatureFlagSet pEnabledFeatures,
        Commands.CommandSelection pCommandSelection,
        int pFunctionCompilationLevel,
        Executor pBackgroundExecutor,
        Executor pGameExecutor
    ) {
        return ReloadableServerRegistries.reload(pRegistryAccess, pPostponedTags, pResourceManager, pBackgroundExecutor)
            .thenCompose(
                p_358539_ -> {
                    ReloadableServerResources reloadableserverresources = new ReloadableServerResources(
                        p_358539_.layers(), p_358539_.lookupWithUpdatedTags(), pEnabledFeatures, pCommandSelection, pPostponedTags, pFunctionCompilationLevel
                    );
                    return SimpleReloadInstance.create(
                            pResourceManager, reloadableserverresources.listeners(), pBackgroundExecutor, pGameExecutor, DATA_RELOAD_INITIAL_TASK, LOGGER.isDebugEnabled()
                        )
                        .done()
                        .thenApply(p_214306_ -> reloadableserverresources);
                }
            );
    }

    public void updateStaticRegistryTags() {
        this.postponedTags.forEach(Registry.PendingTags::apply);
    }
}