package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.ScoreHolder;

public class ScoreHolderArgument implements ArgumentType<ScoreHolderArgument.Result> {
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_SCORE_HOLDERS = (p_108221_, p_108222_) -> {
        StringReader stringreader = new StringReader(p_108222_.getInput());
        stringreader.setCursor(p_108222_.getStart());
        EntitySelectorParser entityselectorparser = new EntitySelectorParser(stringreader, EntitySelectorParser.allowSelectors(p_108221_.getSource()));

        try {
            entityselectorparser.parse();
        } catch (CommandSyntaxException commandsyntaxexception) {
        }

        return entityselectorparser.fillSuggestions(p_108222_, p_171606_ -> SharedSuggestionProvider.suggest(p_108221_.getSource().getOnlinePlayerNames(), p_171606_));
    };
    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "*", "@e");
    private static final SimpleCommandExceptionType ERROR_NO_RESULTS = new SimpleCommandExceptionType(Component.translatable("argument.scoreHolder.empty"));
    final boolean multiple;

    public ScoreHolderArgument(boolean pMultiple) {
        this.multiple = pMultiple;
    }

    public static ScoreHolder getName(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        return getNames(pContext, pName).iterator().next();
    }

    public static Collection<ScoreHolder> getNames(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        return getNames(pContext, pName, Collections::emptyList);
    }

    public static Collection<ScoreHolder> getNamesWithDefaultWildcard(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        return getNames(pContext, pName, pContext.getSource().getServer().getScoreboard()::getTrackedPlayers);
    }

    public static Collection<ScoreHolder> getNames(CommandContext<CommandSourceStack> pContext, String pName, Supplier<Collection<ScoreHolder>> pObjectives) throws CommandSyntaxException {
        Collection<ScoreHolder> collection = pContext.getArgument(pName, ScoreHolderArgument.Result.class).getNames(pContext.getSource(), pObjectives);
        if (collection.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
        } else {
            return collection;
        }
    }

    public static ScoreHolderArgument scoreHolder() {
        return new ScoreHolderArgument(false);
    }

    public static ScoreHolderArgument scoreHolders() {
        return new ScoreHolderArgument(true);
    }

    public ScoreHolderArgument.Result parse(StringReader pReader) throws CommandSyntaxException {
        return this.parse(pReader, true);
    }

    public <S> ScoreHolderArgument.Result parse(StringReader pReader, S pSuggestionProvider) throws CommandSyntaxException {
        return this.parse(pReader, EntitySelectorParser.allowSelectors(pSuggestionProvider));
    }

    private ScoreHolderArgument.Result parse(StringReader pReader, boolean pAllowSelectors) throws CommandSyntaxException {
        if (pReader.canRead() && pReader.peek() == '@') {
            EntitySelectorParser entityselectorparser = new EntitySelectorParser(pReader, pAllowSelectors);
            EntitySelector entityselector = entityselectorparser.parse();
            if (!this.multiple && entityselector.getMaxResults() > 1) {
                throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.createWithContext(pReader);
            } else {
                return new ScoreHolderArgument.SelectorResult(entityselector);
            }
        } else {
            int i = pReader.getCursor();

            while (pReader.canRead() && pReader.peek() != ' ') {
                pReader.skip();
            }

            String s = pReader.getString().substring(i, pReader.getCursor());
            if (s.equals("*")) {
                return (p_108231_, p_108232_) -> {
                    Collection<ScoreHolder> collection = p_108232_.get();
                    if (collection.isEmpty()) {
                        throw ERROR_NO_RESULTS.create();
                    } else {
                        return collection;
                    }
                };
            } else {
                List<ScoreHolder> list = List.of(ScoreHolder.forNameOnly(s));
                if (s.startsWith("#")) {
                    return (p_108237_, p_108238_) -> list;
                } else {
                    try {
                        UUID uuid = UUID.fromString(s);
                        return (p_308383_, p_308384_) -> {
                            MinecraftServer minecraftserver = p_308383_.getServer();
                            ScoreHolder scoreholder = null;
                            List<ScoreHolder> list1 = null;

                            for (ServerLevel serverlevel : minecraftserver.getAllLevels()) {
                                Entity entity = serverlevel.getEntity(uuid);
                                if (entity != null) {
                                    if (scoreholder == null) {
                                        scoreholder = entity;
                                    } else {
                                        if (list1 == null) {
                                            list1 = new ArrayList<>();
                                            list1.add(scoreholder);
                                        }

                                        list1.add(entity);
                                    }
                                }
                            }

                            if (list1 != null) {
                                return list1;
                            } else {
                                return scoreholder != null ? List.of(scoreholder) : list;
                            }
                        };
                    } catch (IllegalArgumentException illegalargumentexception) {
                        return (p_308379_, p_308380_) -> {
                            MinecraftServer minecraftserver = p_308379_.getServer();
                            ServerPlayer serverplayer = minecraftserver.getPlayerList().getPlayerByName(s);
                            return serverplayer != null ? List.of(serverplayer) : list;
                        };
                    }
                }
            }
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info implements ArgumentTypeInfo<ScoreHolderArgument, ScoreHolderArgument.Info.Template> {
        private static final byte FLAG_MULTIPLE = 1;

        public void serializeToNetwork(ScoreHolderArgument.Info.Template p_233469_, FriendlyByteBuf p_233470_) {
            int i = 0;
            if (p_233469_.multiple) {
                i |= 1;
            }

            p_233470_.writeByte(i);
        }

        public ScoreHolderArgument.Info.Template deserializeFromNetwork(FriendlyByteBuf p_233480_) {
            byte b0 = p_233480_.readByte();
            boolean flag = (b0 & 1) != 0;
            return new ScoreHolderArgument.Info.Template(flag);
        }

        public void serializeToJson(ScoreHolderArgument.Info.Template p_233466_, JsonObject p_233467_) {
            p_233467_.addProperty("amount", p_233466_.multiple ? "multiple" : "single");
        }

        public ScoreHolderArgument.Info.Template unpack(ScoreHolderArgument p_233472_) {
            return new ScoreHolderArgument.Info.Template(p_233472_.multiple);
        }

        public final class Template implements ArgumentTypeInfo.Template<ScoreHolderArgument> {
            final boolean multiple;

            Template(final boolean pMultiple) {
                this.multiple = pMultiple;
            }

            public ScoreHolderArgument instantiate(CommandBuildContext p_233490_) {
                return new ScoreHolderArgument(this.multiple);
            }

            @Override
            public ArgumentTypeInfo<ScoreHolderArgument, ?> type() {
                return Info.this;
            }
        }
    }

    @FunctionalInterface
    public interface Result {
        Collection<ScoreHolder> getNames(CommandSourceStack pSource, Supplier<Collection<ScoreHolder>> pObjectives) throws CommandSyntaxException;
    }

    public static class SelectorResult implements ScoreHolderArgument.Result {
        private final EntitySelector selector;

        public SelectorResult(EntitySelector pSelector) {
            this.selector = pSelector;
        }

        @Override
        public Collection<ScoreHolder> getNames(CommandSourceStack p_108258_, Supplier<Collection<ScoreHolder>> p_108259_) throws CommandSyntaxException {
            List<? extends Entity> list = this.selector.findEntities(p_108258_);
            if (list.isEmpty()) {
                throw EntityArgument.NO_ENTITIES_FOUND.create();
            } else {
                return List.copyOf(list);
            }
        }
    }
}