package net.minecraft.server;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class ServerFunctionManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation TICK_FUNCTION_TAG = ResourceLocation.withDefaultNamespace("tick");
    private static final ResourceLocation LOAD_FUNCTION_TAG = ResourceLocation.withDefaultNamespace("load");
    private final MinecraftServer server;
    private List<CommandFunction<CommandSourceStack>> ticking = ImmutableList.of();
    private boolean postReload;
    private ServerFunctionLibrary library;

    public ServerFunctionManager(MinecraftServer pServer, ServerFunctionLibrary pLibrary) {
        this.server = pServer;
        this.library = pLibrary;
        this.postReload(pLibrary);
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() {
        return this.server.getCommands().getDispatcher();
    }

    public void tick() {
        if (this.server.tickRateManager().runsNormally()) {
            if (this.postReload) {
                this.postReload = false;
                Collection<CommandFunction<CommandSourceStack>> collection = this.library.getTag(LOAD_FUNCTION_TAG);
                this.executeTagFunctions(collection, LOAD_FUNCTION_TAG);
            }

            this.executeTagFunctions(this.ticking, TICK_FUNCTION_TAG);
        }
    }

    private void executeTagFunctions(Collection<CommandFunction<CommandSourceStack>> pFunctionObjects, ResourceLocation pIdentifier) {
        Profiler.get().push(pIdentifier::toString);

        for (CommandFunction<CommandSourceStack> commandfunction : pFunctionObjects) {
            this.execute(commandfunction, this.getGameLoopSender());
        }

        Profiler.get().pop();
    }

    public void execute(CommandFunction<CommandSourceStack> pFunction, CommandSourceStack pSource) {
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push(() -> "function " + pFunction.id());

        try {
            InstantiatedFunction<CommandSourceStack> instantiatedfunction = pFunction.instantiate(null, this.getDispatcher());
            Commands.executeCommandInContext(pSource, p_311172_ -> ExecutionContext.queueInitialFunctionCall(p_311172_, instantiatedfunction, pSource, CommandResultCallback.EMPTY));
        } catch (FunctionInstantiationException functioninstantiationexception) {
        } catch (Exception exception) {
            LOGGER.warn("Failed to execute function {}", pFunction.id(), exception);
        } finally {
            profilerfiller.pop();
        }
    }

    public void replaceLibrary(ServerFunctionLibrary pReloader) {
        this.library = pReloader;
        this.postReload(pReloader);
    }

    private void postReload(ServerFunctionLibrary pReloader) {
        this.ticking = List.copyOf(pReloader.getTag(TICK_FUNCTION_TAG));
        this.postReload = true;
    }

    public CommandSourceStack getGameLoopSender() {
        return this.server.createCommandSourceStack().withPermission(2).withSuppressedOutput();
    }

    public Optional<CommandFunction<CommandSourceStack>> get(ResourceLocation pFunction) {
        return this.library.getFunction(pFunction);
    }

    public List<CommandFunction<CommandSourceStack>> getTag(ResourceLocation pTag) {
        return this.library.getTag(pTag);
    }

    public Iterable<ResourceLocation> getFunctionNames() {
        return this.library.getFunctions().keySet();
    }

    public Iterable<ResourceLocation> getTagNames() {
        return this.library.getAvailableTags();
    }
}