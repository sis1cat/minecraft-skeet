package net.minecraft.commands;

import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.gametest.framework.TestCommand;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.AdvancementCommands;
import net.minecraft.server.commands.AttributeCommand;
import net.minecraft.server.commands.BanIpCommands;
import net.minecraft.server.commands.BanListCommands;
import net.minecraft.server.commands.BanPlayerCommands;
import net.minecraft.server.commands.BossBarCommands;
import net.minecraft.server.commands.ClearInventoryCommands;
import net.minecraft.server.commands.CloneCommands;
import net.minecraft.server.commands.DamageCommand;
import net.minecraft.server.commands.DataPackCommand;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.commands.DebugCommand;
import net.minecraft.server.commands.DebugConfigCommand;
import net.minecraft.server.commands.DebugMobSpawningCommand;
import net.minecraft.server.commands.DebugPathCommand;
import net.minecraft.server.commands.DefaultGameModeCommands;
import net.minecraft.server.commands.DifficultyCommand;
import net.minecraft.server.commands.EffectCommands;
import net.minecraft.server.commands.EmoteCommands;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.server.commands.ExperienceCommand;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.commands.FillCommand;
import net.minecraft.server.commands.ForceLoadCommand;
import net.minecraft.server.commands.FunctionCommand;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.server.commands.GameRuleCommand;
import net.minecraft.server.commands.GiveCommand;
import net.minecraft.server.commands.HelpCommand;
import net.minecraft.server.commands.ItemCommands;
import net.minecraft.server.commands.JfrCommand;
import net.minecraft.server.commands.KickCommand;
import net.minecraft.server.commands.KillCommand;
import net.minecraft.server.commands.ListPlayersCommand;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.server.commands.LootCommand;
import net.minecraft.server.commands.MsgCommand;
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.commands.PardonCommand;
import net.minecraft.server.commands.PardonIpCommand;
import net.minecraft.server.commands.ParticleCommand;
import net.minecraft.server.commands.PerfCommand;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.server.commands.PlaySoundCommand;
import net.minecraft.server.commands.PublishCommand;
import net.minecraft.server.commands.RaidCommand;
import net.minecraft.server.commands.RandomCommand;
import net.minecraft.server.commands.RecipeCommand;
import net.minecraft.server.commands.ReloadCommand;
import net.minecraft.server.commands.ReturnCommand;
import net.minecraft.server.commands.RideCommand;
import net.minecraft.server.commands.RotateCommand;
import net.minecraft.server.commands.SaveAllCommand;
import net.minecraft.server.commands.SaveOffCommand;
import net.minecraft.server.commands.SaveOnCommand;
import net.minecraft.server.commands.SayCommand;
import net.minecraft.server.commands.ScheduleCommand;
import net.minecraft.server.commands.ScoreboardCommand;
import net.minecraft.server.commands.SeedCommand;
import net.minecraft.server.commands.ServerPackCommand;
import net.minecraft.server.commands.SetBlockCommand;
import net.minecraft.server.commands.SetPlayerIdleTimeoutCommand;
import net.minecraft.server.commands.SetSpawnCommand;
import net.minecraft.server.commands.SetWorldSpawnCommand;
import net.minecraft.server.commands.SpawnArmorTrimsCommand;
import net.minecraft.server.commands.SpectateCommand;
import net.minecraft.server.commands.SpreadPlayersCommand;
import net.minecraft.server.commands.StopCommand;
import net.minecraft.server.commands.StopSoundCommand;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.commands.TagCommand;
import net.minecraft.server.commands.TeamCommand;
import net.minecraft.server.commands.TeamMsgCommand;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.commands.TellRawCommand;
import net.minecraft.server.commands.TickCommand;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.server.commands.TitleCommand;
import net.minecraft.server.commands.TransferCommand;
import net.minecraft.server.commands.TriggerCommand;
import net.minecraft.server.commands.WardenSpawnTrackerCommand;
import net.minecraft.server.commands.WeatherCommand;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.commands.WorldBorderCommand;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;

public class Commands {
    private static final ThreadLocal<ExecutionContext<CommandSourceStack>> CURRENT_EXECUTION_CONTEXT = new ThreadLocal<>();
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int LEVEL_ALL = 0;
    public static final int LEVEL_MODERATORS = 1;
    public static final int LEVEL_GAMEMASTERS = 2;
    public static final int LEVEL_ADMINS = 3;
    public static final int LEVEL_OWNERS = 4;
    private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

    public Commands(Commands.CommandSelection pSelection, CommandBuildContext pContext) {
        AdvancementCommands.register(this.dispatcher);
        AttributeCommand.register(this.dispatcher, pContext);
        ExecuteCommand.register(this.dispatcher, pContext);
        BossBarCommands.register(this.dispatcher, pContext);
        ClearInventoryCommands.register(this.dispatcher, pContext);
        CloneCommands.register(this.dispatcher, pContext);
        DamageCommand.register(this.dispatcher, pContext);
        DataCommands.register(this.dispatcher);
        DataPackCommand.register(this.dispatcher);
        DebugCommand.register(this.dispatcher);
        DefaultGameModeCommands.register(this.dispatcher);
        DifficultyCommand.register(this.dispatcher);
        EffectCommands.register(this.dispatcher, pContext);
        EmoteCommands.register(this.dispatcher);
        EnchantCommand.register(this.dispatcher, pContext);
        ExperienceCommand.register(this.dispatcher);
        FillCommand.register(this.dispatcher, pContext);
        FillBiomeCommand.register(this.dispatcher, pContext);
        ForceLoadCommand.register(this.dispatcher);
        FunctionCommand.register(this.dispatcher);
        GameModeCommand.register(this.dispatcher);
        GameRuleCommand.register(this.dispatcher, pContext);
        GiveCommand.register(this.dispatcher, pContext);
        HelpCommand.register(this.dispatcher);
        ItemCommands.register(this.dispatcher, pContext);
        KickCommand.register(this.dispatcher);
        KillCommand.register(this.dispatcher);
        ListPlayersCommand.register(this.dispatcher);
        LocateCommand.register(this.dispatcher, pContext);
        LootCommand.register(this.dispatcher, pContext);
        MsgCommand.register(this.dispatcher);
        ParticleCommand.register(this.dispatcher, pContext);
        PlaceCommand.register(this.dispatcher);
        PlaySoundCommand.register(this.dispatcher);
        RandomCommand.register(this.dispatcher);
        ReloadCommand.register(this.dispatcher);
        RecipeCommand.register(this.dispatcher);
        ReturnCommand.register(this.dispatcher);
        RideCommand.register(this.dispatcher);
        RotateCommand.register(this.dispatcher);
        SayCommand.register(this.dispatcher);
        ScheduleCommand.register(this.dispatcher);
        ScoreboardCommand.register(this.dispatcher, pContext);
        SeedCommand.register(this.dispatcher, pSelection != Commands.CommandSelection.INTEGRATED);
        SetBlockCommand.register(this.dispatcher, pContext);
        SetSpawnCommand.register(this.dispatcher);
        SetWorldSpawnCommand.register(this.dispatcher);
        SpectateCommand.register(this.dispatcher);
        SpreadPlayersCommand.register(this.dispatcher);
        StopSoundCommand.register(this.dispatcher);
        SummonCommand.register(this.dispatcher, pContext);
        TagCommand.register(this.dispatcher);
        TeamCommand.register(this.dispatcher, pContext);
        TeamMsgCommand.register(this.dispatcher);
        TeleportCommand.register(this.dispatcher);
        TellRawCommand.register(this.dispatcher, pContext);
        TickCommand.register(this.dispatcher);
        TimeCommand.register(this.dispatcher);
        TitleCommand.register(this.dispatcher, pContext);
        TriggerCommand.register(this.dispatcher);
        WeatherCommand.register(this.dispatcher);
        WorldBorderCommand.register(this.dispatcher);
        if (JvmProfiler.INSTANCE.isAvailable()) {
            JfrCommand.register(this.dispatcher);
        }

        if (SharedConstants.IS_RUNNING_IN_IDE) {
            TestCommand.register(this.dispatcher);
            RaidCommand.register(this.dispatcher, pContext);
            DebugPathCommand.register(this.dispatcher);
            DebugMobSpawningCommand.register(this.dispatcher);
            WardenSpawnTrackerCommand.register(this.dispatcher);
            SpawnArmorTrimsCommand.register(this.dispatcher);
            ServerPackCommand.register(this.dispatcher);
            if (pSelection.includeDedicated) {
                DebugConfigCommand.register(this.dispatcher);
            }
        }

        if (pSelection.includeDedicated) {
            BanIpCommands.register(this.dispatcher);
            BanListCommands.register(this.dispatcher);
            BanPlayerCommands.register(this.dispatcher);
            DeOpCommands.register(this.dispatcher);
            OpCommand.register(this.dispatcher);
            PardonCommand.register(this.dispatcher);
            PardonIpCommand.register(this.dispatcher);
            PerfCommand.register(this.dispatcher);
            SaveAllCommand.register(this.dispatcher);
            SaveOffCommand.register(this.dispatcher);
            SaveOnCommand.register(this.dispatcher);
            SetPlayerIdleTimeoutCommand.register(this.dispatcher);
            StopCommand.register(this.dispatcher);
            TransferCommand.register(this.dispatcher);
            WhitelistCommand.register(this.dispatcher);
        }

        if (pSelection.includeIntegrated) {
            PublishCommand.register(this.dispatcher);
        }

        this.dispatcher.setConsumer(ExecutionCommandSource.resultConsumer());
    }

    public static <S> ParseResults<S> mapSource(ParseResults<S> pParseResults, UnaryOperator<S> pMapper) {
        CommandContextBuilder<S> commandcontextbuilder = pParseResults.getContext();
        CommandContextBuilder<S> commandcontextbuilder1 = commandcontextbuilder.withSource(pMapper.apply(commandcontextbuilder.getSource()));
        return new ParseResults<>(commandcontextbuilder1, pParseResults.getReader(), pParseResults.getExceptions());
    }

    public void performPrefixedCommand(CommandSourceStack pSource, String pCommand) {
        pCommand = pCommand.startsWith("/") ? pCommand.substring(1) : pCommand;
        this.performCommand(this.dispatcher.parse(pCommand, pSource), pCommand);
    }

    public void performCommand(ParseResults<CommandSourceStack> pParseResults, String pCommand) {
        CommandSourceStack commandsourcestack = pParseResults.getContext().getSource();
        Profiler.get().push(() -> "/" + pCommand);
        ContextChain<CommandSourceStack> contextchain = finishParsing(pParseResults, pCommand, commandsourcestack);

        try {
            if (contextchain != null) {
                executeCommandInContext(
                    commandsourcestack,
                    p_308343_ -> ExecutionContext.queueInitialCommandExecution(p_308343_, pCommand, contextchain, commandsourcestack, CommandResultCallback.EMPTY)
                );
            }
        } catch (Exception exception) {
            MutableComponent mutablecomponent = Component.literal(exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error("Command exception: /{}", pCommand, exception);
                StackTraceElement[] astacktraceelement = exception.getStackTrace();

                for (int i = 0; i < Math.min(astacktraceelement.length, 3); i++) {
                    mutablecomponent.append("\n\n")
                        .append(astacktraceelement[i].getMethodName())
                        .append("\n ")
                        .append(astacktraceelement[i].getFileName())
                        .append(":")
                        .append(String.valueOf(astacktraceelement[i].getLineNumber()));
                }
            }

            commandsourcestack.sendFailure(
                Component.translatable("command.failed").withStyle(p_82137_ -> p_82137_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, mutablecomponent)))
            );
            if (SharedConstants.IS_RUNNING_IN_IDE) {
                commandsourcestack.sendFailure(Component.literal(Util.describeError(exception)));
                LOGGER.error("'/{}' threw an exception", pCommand, exception);
            }
        } finally {
            Profiler.get().pop();
        }
    }

    @Nullable
    private static ContextChain<CommandSourceStack> finishParsing(ParseResults<CommandSourceStack> pParseResults, String pCommand, CommandSourceStack pSource) {
        try {
            validateParseResults(pParseResults);
            return ContextChain.tryFlatten(pParseResults.getContext().build(pCommand))
                .orElseThrow(() -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(pParseResults.getReader()));
        } catch (CommandSyntaxException commandsyntaxexception) {
            pSource.sendFailure(ComponentUtils.fromMessage(commandsyntaxexception.getRawMessage()));
            if (commandsyntaxexception.getInput() != null && commandsyntaxexception.getCursor() >= 0) {
                int i = Math.min(commandsyntaxexception.getInput().length(), commandsyntaxexception.getCursor());
                MutableComponent mutablecomponent = Component.empty()
                    .withStyle(ChatFormatting.GRAY)
                    .withStyle(p_82134_ -> p_82134_.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + pCommand)));
                if (i > 10) {
                    mutablecomponent.append(CommonComponents.ELLIPSIS);
                }

                mutablecomponent.append(commandsyntaxexception.getInput().substring(Math.max(0, i - 10), i));
                if (i < commandsyntaxexception.getInput().length()) {
                    Component component = Component.literal(commandsyntaxexception.getInput().substring(i))
                        .withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE);
                    mutablecomponent.append(component);
                }

                mutablecomponent.append(Component.translatable("command.context.here").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
                pSource.sendFailure(mutablecomponent);
            }

            return null;
        }
    }

    public static void executeCommandInContext(CommandSourceStack pSource, Consumer<ExecutionContext<CommandSourceStack>> pContextConsumer) {
        MinecraftServer minecraftserver = pSource.getServer();
        ExecutionContext<CommandSourceStack> executioncontext = CURRENT_EXECUTION_CONTEXT.get();
        boolean flag = executioncontext == null;
        if (flag) {
            int i = Math.max(1, minecraftserver.getGameRules().getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH));
            int j = minecraftserver.getGameRules().getInt(GameRules.RULE_MAX_COMMAND_FORK_COUNT);

            try (ExecutionContext<CommandSourceStack> executioncontext1 = new ExecutionContext<>(i, j, Profiler.get())) {
                CURRENT_EXECUTION_CONTEXT.set(executioncontext1);
                pContextConsumer.accept(executioncontext1);
                executioncontext1.runCommandQueue();
            } finally {
                CURRENT_EXECUTION_CONTEXT.set(null);
            }
        } else {
            pContextConsumer.accept(executioncontext);
        }
    }

    public void sendCommands(ServerPlayer pPlayer) {
        Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> map = Maps.newHashMap();
        RootCommandNode<SharedSuggestionProvider> rootcommandnode = new RootCommandNode<>();
        map.put(this.dispatcher.getRoot(), rootcommandnode);
        this.fillUsableCommands(this.dispatcher.getRoot(), rootcommandnode, pPlayer.createCommandSourceStack(), map);
        pPlayer.connection.send(new ClientboundCommandsPacket(rootcommandnode));
    }

    private void fillUsableCommands(
        CommandNode<CommandSourceStack> pRootCommandSource,
        CommandNode<SharedSuggestionProvider> pRootSuggestion,
        CommandSourceStack pSource,
        Map<CommandNode<CommandSourceStack>, CommandNode<SharedSuggestionProvider>> pCommandNodeToSuggestionNode
    ) {
        for (CommandNode<CommandSourceStack> commandnode : pRootCommandSource.getChildren()) {
            if (commandnode.canUse(pSource)) {
                ArgumentBuilder<SharedSuggestionProvider, ?> argumentbuilder = (ArgumentBuilder)commandnode.createBuilder();
                argumentbuilder.requires(p_82126_ -> true);
                if (argumentbuilder.getCommand() != null) {
                    argumentbuilder.executes(p_82102_ -> 0);
                }

                if (argumentbuilder instanceof RequiredArgumentBuilder) {
                    RequiredArgumentBuilder<SharedSuggestionProvider, ?> requiredargumentbuilder = (RequiredArgumentBuilder<SharedSuggestionProvider, ?>)argumentbuilder;
                    if (requiredargumentbuilder.getSuggestionsProvider() != null) {
                        requiredargumentbuilder.suggests(SuggestionProviders.safelySwap(requiredargumentbuilder.getSuggestionsProvider()));
                    }
                }

                if (argumentbuilder.getRedirect() != null) {
                    argumentbuilder.redirect(pCommandNodeToSuggestionNode.get(argumentbuilder.getRedirect()));
                }

                CommandNode<SharedSuggestionProvider> commandnode1 = argumentbuilder.build();
                pCommandNodeToSuggestionNode.put(commandnode, commandnode1);
                pRootSuggestion.addChild(commandnode1);
                if (!commandnode.getChildren().isEmpty()) {
                    this.fillUsableCommands(commandnode, commandnode1, pSource, pCommandNodeToSuggestionNode);
                }
            }
        }
    }

    public static LiteralArgumentBuilder<CommandSourceStack> literal(String pName) {
        return LiteralArgumentBuilder.literal(pName);
    }

    public static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(String pName, ArgumentType<T> pType) {
        return RequiredArgumentBuilder.argument(pName, pType);
    }

    public static Predicate<String> createValidator(Commands.ParseFunction pParser) {
        return p_82124_ -> {
            try {
                pParser.parse(new StringReader(p_82124_));
                return true;
            } catch (CommandSyntaxException commandsyntaxexception) {
                return false;
            }
        };
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() {
        return this.dispatcher;
    }

    public static <S> void validateParseResults(ParseResults<S> pParseResults) throws CommandSyntaxException {
        CommandSyntaxException commandsyntaxexception = getParseException(pParseResults);
        if (commandsyntaxexception != null) {
            throw commandsyntaxexception;
        }
    }

    @Nullable
    public static <S> CommandSyntaxException getParseException(ParseResults<S> pResult) {
        if (!pResult.getReader().canRead()) {
            return null;
        } else if (pResult.getExceptions().size() == 1) {
            return pResult.getExceptions().values().iterator().next();
        } else {
            return pResult.getContext().getRange().isEmpty()
                ? CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().createWithContext(pResult.getReader())
                : CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(pResult.getReader());
        }
    }

    public static CommandBuildContext createValidationContext(final HolderLookup.Provider pProvider) {
        return new CommandBuildContext() {
            @Override
            public FeatureFlagSet enabledFeatures() {
                return FeatureFlags.REGISTRY.allFlags();
            }

            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
                return pProvider.listRegistryKeys();
            }

            @Override
            public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_334781_) {
                return pProvider.lookup(p_334781_).map(this::createLookup);
            }

            private <T> HolderLookup.RegistryLookup.Delegate<T> createLookup(final HolderLookup.RegistryLookup<T> p_328045_) {
                return new HolderLookup.RegistryLookup.Delegate<T>() {
                    @Override
                    public HolderLookup.RegistryLookup<T> parent() {
                        return p_328045_;
                    }

                    @Override
                    public Optional<HolderSet.Named<T>> get(TagKey<T> p_255936_) {
                        return Optional.of(this.getOrThrow(p_255936_));
                    }

                    @Override
                    public HolderSet.Named<T> getOrThrow(TagKey<T> p_255953_) {
                        Optional<HolderSet.Named<T>> optional = this.parent().get(p_255953_);
                        return optional.orElseGet(() -> HolderSet.emptyNamed(this.parent(), p_255953_));
                    }
                };
            }
        };
    }

    public static void validate() {
        CommandBuildContext commandbuildcontext = createValidationContext(VanillaRegistries.createLookup());
        CommandDispatcher<CommandSourceStack> commanddispatcher = new Commands(Commands.CommandSelection.ALL, commandbuildcontext).getDispatcher();
        RootCommandNode<CommandSourceStack> rootcommandnode = commanddispatcher.getRoot();
        commanddispatcher.findAmbiguities(
            (p_230947_, p_230948_, p_230949_, p_230950_) -> LOGGER.warn(
                    "Ambiguity between arguments {} and {} with inputs: {}",
                    commanddispatcher.getPath(p_230948_),
                    commanddispatcher.getPath(p_230949_),
                    p_230950_
                )
        );
        Set<ArgumentType<?>> set = ArgumentUtils.findUsedArgumentTypes(rootcommandnode);
        Set<ArgumentType<?>> set1 = set.stream().filter(p_325582_ -> !ArgumentTypeInfos.isClassRecognized(p_325582_.getClass())).collect(Collectors.toSet());
        if (!set1.isEmpty()) {
            LOGGER.warn(
                "Missing type registration for following arguments:\n {}", set1.stream().map(p_325583_ -> "\t" + p_325583_).collect(Collectors.joining(",\n"))
            );
            throw new IllegalStateException("Unregistered argument types");
        }
    }

    public static enum CommandSelection {
        ALL(true, true),
        DEDICATED(false, true),
        INTEGRATED(true, false);

        final boolean includeIntegrated;
        final boolean includeDedicated;

        private CommandSelection(final boolean pIncludeIntegrated, final boolean pIncludeDedicated) {
            this.includeIntegrated = pIncludeIntegrated;
            this.includeDedicated = pIncludeDedicated;
        }
    }

    @FunctionalInterface
    public interface ParseFunction {
        void parse(StringReader pInput) throws CommandSyntaxException;
    }
}