package net.minecraft.commands.arguments;

import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class EntityArgument implements ArgumentType<EntitySelector> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "@e", "@e[type=foo]", "dd12be42-52a9-4a91-a8a1-11c01849e498");
    public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_ENTITY = new SimpleCommandExceptionType(Component.translatable("argument.entity.toomany"));
    public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_PLAYER = new SimpleCommandExceptionType(Component.translatable("argument.player.toomany"));
    public static final SimpleCommandExceptionType ERROR_ONLY_PLAYERS_ALLOWED = new SimpleCommandExceptionType(Component.translatable("argument.player.entities"));
    public static final SimpleCommandExceptionType NO_ENTITIES_FOUND = new SimpleCommandExceptionType(Component.translatable("argument.entity.notfound.entity"));
    public static final SimpleCommandExceptionType NO_PLAYERS_FOUND = new SimpleCommandExceptionType(Component.translatable("argument.entity.notfound.player"));
    public static final SimpleCommandExceptionType ERROR_SELECTORS_NOT_ALLOWED = new SimpleCommandExceptionType(Component.translatable("argument.entity.selector.not_allowed"));
    final boolean single;
    final boolean playersOnly;

    protected EntityArgument(boolean pSingle, boolean pPlayersOnly) {
        this.single = pSingle;
        this.playersOnly = pPlayersOnly;
    }

    public static EntityArgument entity() {
        return new EntityArgument(true, false);
    }

    public static Entity getEntity(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        return pContext.getArgument(pName, EntitySelector.class).findSingleEntity(pContext.getSource());
    }

    public static EntityArgument entities() {
        return new EntityArgument(false, false);
    }

    public static Collection<? extends Entity> getEntities(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        Collection<? extends Entity> collection = getOptionalEntities(pContext, pName);
        if (collection.isEmpty()) {
            throw NO_ENTITIES_FOUND.create();
        } else {
            return collection;
        }
    }

    public static Collection<? extends Entity> getOptionalEntities(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        return pContext.getArgument(pName, EntitySelector.class).findEntities(pContext.getSource());
    }

    public static Collection<ServerPlayer> getOptionalPlayers(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        return pContext.getArgument(pName, EntitySelector.class).findPlayers(pContext.getSource());
    }

    public static EntityArgument player() {
        return new EntityArgument(true, true);
    }

    public static ServerPlayer getPlayer(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        return pContext.getArgument(pName, EntitySelector.class).findSinglePlayer(pContext.getSource());
    }

    public static EntityArgument players() {
        return new EntityArgument(false, true);
    }

    public static Collection<ServerPlayer> getPlayers(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        List<ServerPlayer> list = pContext.getArgument(pName, EntitySelector.class).findPlayers(pContext.getSource());
        if (list.isEmpty()) {
            throw NO_PLAYERS_FOUND.create();
        } else {
            return list;
        }
    }

    public EntitySelector parse(StringReader pReader) throws CommandSyntaxException {
        return this.parse(pReader, true);
    }

    public <S> EntitySelector parse(StringReader p_345548_, S p_345559_) throws CommandSyntaxException {
        return this.parse(p_345548_, EntitySelectorParser.allowSelectors(p_345559_));
    }

    private EntitySelector parse(StringReader pReader, boolean pAllowSelectors) throws CommandSyntaxException {
        int i = 0;
        EntitySelectorParser entityselectorparser = new EntitySelectorParser(pReader, pAllowSelectors);
        EntitySelector entityselector = entityselectorparser.parse();
        if (entityselector.getMaxResults() > 1 && this.single) {
            if (this.playersOnly) {
                pReader.setCursor(0);
                throw ERROR_NOT_SINGLE_PLAYER.createWithContext(pReader);
            } else {
                pReader.setCursor(0);
                throw ERROR_NOT_SINGLE_ENTITY.createWithContext(pReader);
            }
        } else if (entityselector.includesEntities() && this.playersOnly && !entityselector.isSelfSelector()) {
            pReader.setCursor(0);
            throw ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(pReader);
        } else {
            return entityselector;
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
        if (pContext.getSource() instanceof SharedSuggestionProvider sharedsuggestionprovider) {
            StringReader stringreader = new StringReader(pBuilder.getInput());
            stringreader.setCursor(pBuilder.getStart());
            EntitySelectorParser entityselectorparser = new EntitySelectorParser(stringreader, EntitySelectorParser.allowSelectors(sharedsuggestionprovider));

            try {
                entityselectorparser.parse();
            } catch (CommandSyntaxException commandsyntaxexception) {
            }

            return entityselectorparser.fillSuggestions(pBuilder, p_91457_ -> {
                Collection<String> collection = sharedsuggestionprovider.getOnlinePlayerNames();
                Iterable<String> iterable = (Iterable<String>)(this.playersOnly ? collection : Iterables.concat(collection, sharedsuggestionprovider.getSelectedEntities()));
                SharedSuggestionProvider.suggest(iterable, p_91457_);
            });
        } else {
            return Suggestions.empty();
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info implements ArgumentTypeInfo<EntityArgument, EntityArgument.Info.Template> {
        private static final byte FLAG_SINGLE = 1;
        private static final byte FLAG_PLAYERS_ONLY = 2;

        public void serializeToNetwork(EntityArgument.Info.Template p_231271_, FriendlyByteBuf p_231272_) {
            int i = 0;
            if (p_231271_.single) {
                i |= 1;
            }

            if (p_231271_.playersOnly) {
                i |= 2;
            }

            p_231272_.writeByte(i);
        }

        public EntityArgument.Info.Template deserializeFromNetwork(FriendlyByteBuf p_231282_) {
            byte b0 = p_231282_.readByte();
            return new EntityArgument.Info.Template((b0 & 1) != 0, (b0 & 2) != 0);
        }

        public void serializeToJson(EntityArgument.Info.Template p_231268_, JsonObject p_231269_) {
            p_231269_.addProperty("amount", p_231268_.single ? "single" : "multiple");
            p_231269_.addProperty("type", p_231268_.playersOnly ? "players" : "entities");
        }

        public EntityArgument.Info.Template unpack(EntityArgument p_231274_) {
            return new EntityArgument.Info.Template(p_231274_.single, p_231274_.playersOnly);
        }

        public final class Template implements ArgumentTypeInfo.Template<EntityArgument> {
            final boolean single;
            final boolean playersOnly;

            Template(final boolean pSingle, final boolean pPlayersOnly) {
                this.single = pSingle;
                this.playersOnly = pPlayersOnly;
            }

            public EntityArgument instantiate(CommandBuildContext p_231294_) {
                return new EntityArgument(this.single, this.playersOnly);
            }

            @Override
            public ArgumentTypeInfo<EntityArgument, ?> type() {
                return Info.this;
            }
        }
    }
}