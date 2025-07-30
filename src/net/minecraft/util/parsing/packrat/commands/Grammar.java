package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.Dictionary;
import net.minecraft.util.parsing.packrat.ErrorCollector;
import net.minecraft.util.parsing.packrat.ErrorEntry;
import net.minecraft.util.parsing.packrat.ParseState;

public record Grammar<T>(Dictionary<StringReader> rules, Atom<T> top) {
    public Optional<T> parse(ParseState<StringReader> pParseState) {
        return pParseState.parseTopRule(this.top);
    }

    public T parseForCommands(StringReader pReader) throws CommandSyntaxException {
        ErrorCollector.LongestOnly<StringReader> longestonly = new ErrorCollector.LongestOnly<>();
        StringReaderParserState stringreaderparserstate = new StringReaderParserState(this.rules(), longestonly, pReader);
        Optional<T> optional = this.parse(stringreaderparserstate);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            List<Exception> list = longestonly.entries().stream().<Exception>mapMulti((p_331304_, p_328464_) -> {
                if (p_331304_.reason() instanceof Exception exception1) {
                    p_328464_.accept(exception1);
                }
            }).toList();

            for (Exception exception : list) {
                if (exception instanceof CommandSyntaxException commandsyntaxexception) {
                    throw commandsyntaxexception;
                }
            }

            if (list.size() == 1 && list.get(0) instanceof RuntimeException runtimeexception) {
                throw runtimeexception;
            } else {
                throw new IllegalStateException(
                    "Failed to parse: " + longestonly.entries().stream().map(ErrorEntry::toString).collect(Collectors.joining(", "))
                );
            }
        }
    }

    public CompletableFuture<Suggestions> parseForSuggestions(SuggestionsBuilder pBuilder) {
        StringReader stringreader = new StringReader(pBuilder.getInput());
        stringreader.setCursor(pBuilder.getStart());
        ErrorCollector.LongestOnly<StringReader> longestonly = new ErrorCollector.LongestOnly<>();
        StringReaderParserState stringreaderparserstate = new StringReaderParserState(this.rules(), longestonly, stringreader);
        this.parse(stringreaderparserstate);
        List<ErrorEntry<StringReader>> list = longestonly.entries();
        if (list.isEmpty()) {
            return pBuilder.buildFuture();
        } else {
            SuggestionsBuilder suggestionsbuilder = pBuilder.createOffset(longestonly.cursor());

            for (ErrorEntry<StringReader> errorentry : list) {
                if (errorentry.suggestions() instanceof ResourceSuggestion resourcesuggestion) {
                    SharedSuggestionProvider.suggestResource(resourcesuggestion.possibleResources(), suggestionsbuilder);
                } else {
                    SharedSuggestionProvider.suggest(errorentry.suggestions().possibleValues(stringreaderparserstate), suggestionsbuilder);
                }
            }

            return suggestionsbuilder.buildFuture();
        }
    }
}