package net.minecraft.data.loot;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class LootTableProvider implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackOutput.PathProvider pathProvider;
    private final Set<ResourceKey<LootTable>> requiredTables;
    private final List<LootTableProvider.SubProviderEntry> subProviders;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public LootTableProvider(
        PackOutput pOutput,
        Set<ResourceKey<LootTable>> pRequiredTables,
        List<LootTableProvider.SubProviderEntry> pSubProviders,
        CompletableFuture<HolderLookup.Provider> pRegistries
    ) {
        this.pathProvider = pOutput.createRegistryElementsPathProvider(Registries.LOOT_TABLE);
        this.subProviders = pSubProviders;
        this.requiredTables = pRequiredTables;
        this.registries = pRegistries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput p_254060_) {
        return this.registries.thenCompose(p_325860_ -> this.run(p_254060_, p_325860_));
    }

    private CompletableFuture<?> run(CachedOutput pOutput, HolderLookup.Provider pProvider) {
        WritableRegistry<LootTable> writableregistry = new MappedRegistry<>(Registries.LOOT_TABLE, Lifecycle.experimental());
        Map<RandomSupport.Seed128bit, ResourceLocation> map = new Object2ObjectOpenHashMap<>();
        this.subProviders.forEach(p_341016_ -> p_341016_.provider().apply(pProvider).generate((p_358218_, p_358219_) -> {
                ResourceLocation resourcelocation = sequenceIdForLootTable(p_358218_);
                ResourceLocation resourcelocation1 = map.put(RandomSequence.seedForKey(resourcelocation), resourcelocation);
                if (resourcelocation1 != null) {
                    Util.logAndPauseIfInIde("Loot table random sequence seed collision on " + resourcelocation1 + " and " + p_358218_.location());
                }

                p_358219_.setRandomSequence(resourcelocation);
                LootTable loottable = p_358219_.setParamSet(p_341016_.paramSet).build();
                writableregistry.register(p_358218_, loottable, RegistrationInfo.BUILT_IN);
            }));
        writableregistry.freeze();
        ProblemReporter.Collector problemreporter$collector = new ProblemReporter.Collector();
        HolderGetter.Provider holdergetter$provider = new RegistryAccess.ImmutableRegistryAccess(List.of(writableregistry)).freeze();
        ValidationContext validationcontext = new ValidationContext(problemreporter$collector, LootContextParamSets.ALL_PARAMS, holdergetter$provider);

        for (ResourceKey<LootTable> resourcekey : Sets.difference(this.requiredTables, writableregistry.registryKeySet())) {
            problemreporter$collector.report("Missing built-in table: " + resourcekey.location());
        }

        writableregistry.listElements()
            .forEach(
                p_358221_ -> p_358221_.value()
                        .validate(
                            validationcontext.setContextKeySet(p_358221_.value().getParamSet())
                                .enterElement("{" + p_358221_.key().location() + "}", p_358221_.key())
                        )
            );
        Multimap<String, String> multimap = problemreporter$collector.get();
        if (!multimap.isEmpty()) {
            multimap.forEach((p_124446_, p_124447_) -> LOGGER.warn("Found validation problem in {}: {}", p_124446_, p_124447_));
            throw new IllegalStateException("Failed to validate loot tables, see logs");
        } else {
            return CompletableFuture.allOf(writableregistry.entrySet().stream().map(p_325852_ -> {
                ResourceKey<LootTable> resourcekey1 = p_325852_.getKey();
                LootTable loottable = p_325852_.getValue();
                Path path = this.pathProvider.json(resourcekey1.location());
                return DataProvider.saveStable(pOutput, pProvider, LootTable.DIRECT_CODEC, loottable, path);
            }).toArray(CompletableFuture[]::new));
        }
    }

    private static ResourceLocation sequenceIdForLootTable(ResourceKey<LootTable> pLootTable) {
        return pLootTable.location();
    }

    @Override
    public final String getName() {
        return "Loot Tables";
    }

    public static record SubProviderEntry(Function<HolderLookup.Provider, LootTableSubProvider> provider, ContextKeySet paramSet) {
    }
}