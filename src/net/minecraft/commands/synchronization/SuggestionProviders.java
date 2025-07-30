package net.minecraft.commands.synchronization;

import com.google.common.collect.Maps;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class SuggestionProviders {
    private static final Map<ResourceLocation, SuggestionProvider<SharedSuggestionProvider>> PROVIDERS_BY_NAME = Maps.newHashMap();
    private static final ResourceLocation DEFAULT_NAME = ResourceLocation.withDefaultNamespace("ask_server");
    public static final SuggestionProvider<SharedSuggestionProvider> ASK_SERVER = register(
        DEFAULT_NAME, (p_121673_, p_121674_) -> p_121673_.getSource().customSuggestion(p_121673_)
    );
    public static final SuggestionProvider<CommandSourceStack> AVAILABLE_SOUNDS = register(
        ResourceLocation.withDefaultNamespace("available_sounds"), (p_121667_, p_121668_) -> SharedSuggestionProvider.suggestResource(p_121667_.getSource().getAvailableSounds(), p_121668_)
    );
    public static final SuggestionProvider<CommandSourceStack> SUMMONABLE_ENTITIES = register(
        ResourceLocation.withDefaultNamespace("summonable_entities"),
        (p_358078_, p_358079_) -> SharedSuggestionProvider.suggestResource(
                BuiltInRegistries.ENTITY_TYPE.stream().filter(p_247987_ -> p_247987_.isEnabled(p_358078_.getSource().enabledFeatures()) && p_247987_.canSummon()),
                p_358079_,
                EntityType::getKey,
                p_212436_ -> Component.translatable(Util.makeDescriptionId("entity", EntityType.getKey(p_212436_)))
            )
    );

    public static <S extends SharedSuggestionProvider> SuggestionProvider<S> register(
        ResourceLocation pName, SuggestionProvider<SharedSuggestionProvider> pProvider
    ) {
        if (PROVIDERS_BY_NAME.containsKey(pName)) {
            throw new IllegalArgumentException("A command suggestion provider is already registered with the name " + pName);
        } else {
            PROVIDERS_BY_NAME.put(pName, pProvider);
            return (SuggestionProvider)new Wrapper(pName, pProvider);
        }
    }

    public static SuggestionProvider<SharedSuggestionProvider> getProvider(ResourceLocation pName) {
        return PROVIDERS_BY_NAME.getOrDefault(pName, ASK_SERVER);
    }

    public static ResourceLocation getName(SuggestionProvider<SharedSuggestionProvider> pProvider) {
        return pProvider instanceof SuggestionProviders.Wrapper ? ((SuggestionProviders.Wrapper)pProvider).name : DEFAULT_NAME;
    }

    public static SuggestionProvider<SharedSuggestionProvider> safelySwap(SuggestionProvider<SharedSuggestionProvider> pProvider) {
        return pProvider instanceof SuggestionProviders.Wrapper ? pProvider : ASK_SERVER;
    }

    protected static class Wrapper implements SuggestionProvider<SharedSuggestionProvider> {
        private final SuggestionProvider<SharedSuggestionProvider> delegate;
        final ResourceLocation name;

        public Wrapper(ResourceLocation pName, SuggestionProvider<SharedSuggestionProvider> pDelegate) {
            this.delegate = pDelegate;
            this.name = pName;
        }

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<SharedSuggestionProvider> pContext, SuggestionsBuilder pBuilder) throws CommandSyntaxException {
            return this.delegate.getSuggestions(pContext, pBuilder);
        }
    }
}