package net.minecraft.data;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.SharedConstants;
import net.minecraft.SuppressForbidden;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.advancements.packs.VanillaAdvancementProvider;
import net.minecraft.data.info.BiomeParametersDumpReport;
import net.minecraft.data.info.BlockListReport;
import net.minecraft.data.info.CommandsReport;
import net.minecraft.data.info.DatapackStructureReport;
import net.minecraft.data.info.ItemListReport;
import net.minecraft.data.info.PacketReport;
import net.minecraft.data.info.RegistryDumpReport;
import net.minecraft.data.loot.packs.TradeRebalanceLootTableProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.minecraft.data.registries.TradeRebalanceRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.data.structures.SnbtToNbt;
import net.minecraft.data.structures.StructureUpdater;
import net.minecraft.data.tags.BannerPatternTagsProvider;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.data.tags.CatVariantTagsProvider;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.data.tags.FlatLevelGeneratorPresetTagsProvider;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.data.tags.GameEventTagsProvider;
import net.minecraft.data.tags.InstrumentTagsProvider;
import net.minecraft.data.tags.PaintingVariantTagsProvider;
import net.minecraft.data.tags.PoiTypeTagsProvider;
import net.minecraft.data.tags.StructureTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.data.tags.TradeRebalanceEnchantmentTagsProvider;
import net.minecraft.data.tags.TradeRebalanceStructureTagsProvider;
import net.minecraft.data.tags.VanillaBlockTagsProvider;
import net.minecraft.data.tags.VanillaEnchantmentTagsProvider;
import net.minecraft.data.tags.VanillaItemTagsProvider;
import net.minecraft.data.tags.WorldPresetTagsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.levelgen.structure.Structure;

public class Main {
//    @SuppressForbidden(
//        a = "System.out needed before bootstrap"
//    )
    @DontObfuscate
    public static void main(String[] pArgs) throws IOException {
        SharedConstants.tryDetectVersion();
        OptionParser optionparser = new OptionParser();
        OptionSpec<Void> optionspec = optionparser.accepts("help", "Show the help menu").forHelp();
        OptionSpec<Void> optionspec1 = optionparser.accepts("server", "Include server generators");
        OptionSpec<Void> optionspec2 = optionparser.accepts("dev", "Include development tools");
        OptionSpec<Void> optionspec3 = optionparser.accepts("reports", "Include data reports");
        optionparser.accepts("validate", "Validate inputs");
        OptionSpec<Void> optionspec4 = optionparser.accepts("all", "Include all generators");
        OptionSpec<String> optionspec5 = optionparser.accepts("output", "Output folder").withRequiredArg().defaultsTo("generated");
        OptionSpec<String> optionspec6 = optionparser.accepts("input", "Input folder").withRequiredArg();
        OptionSet optionset = optionparser.parse(pArgs);
        if (!optionset.has(optionspec) && optionset.hasOptions()) {
            Path path = Paths.get(optionspec5.value(optionset));
            boolean flag = optionset.has(optionspec4);
            boolean flag1 = flag || optionset.has(optionspec1);
            boolean flag2 = flag || optionset.has(optionspec2);
            boolean flag3 = flag || optionset.has(optionspec3);
            Collection<Path> collection = optionset.valuesOf(optionspec6).stream().map(p_129659_ -> Paths.get(p_129659_)).toList();
            DataGenerator datagenerator = new DataGenerator(path, SharedConstants.getCurrentVersion(), true);
            addServerProviders(datagenerator, collection, flag1, flag2, flag3);
            datagenerator.run();
        } else {
            optionparser.printHelpOn(System.out);
        }
    }

    private static <T extends DataProvider> DataProvider.Factory<T> bindRegistries(
        BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, T> pTagProviderFactory, CompletableFuture<HolderLookup.Provider> pLookupProvider
    ) {
        return p_255476_ -> pTagProviderFactory.apply(p_255476_, pLookupProvider);
    }

    public static void addServerProviders(DataGenerator pDataGenerator, Collection<Path> pPaths, boolean pServer, boolean pDev, boolean pReports) {
        DataGenerator.PackGenerator datagenerator$packgenerator = pDataGenerator.getVanillaPack(pServer);
        datagenerator$packgenerator.addProvider(p_253388_ -> new SnbtToNbt(p_253388_, pPaths).addFilter(new StructureUpdater()));
        CompletableFuture<HolderLookup.Provider> completablefuture1 = CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor());
        DataGenerator.PackGenerator datagenerator$packgenerator1 = pDataGenerator.getVanillaPack(pServer);
        datagenerator$packgenerator1.addProvider(bindRegistries(RegistriesDatapackGenerator::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(VanillaAdvancementProvider::create, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(VanillaLootTableProvider::create, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(VanillaRecipeProvider.Runner::new, completablefuture1));
        TagsProvider<Block> tagsprovider = datagenerator$packgenerator1.addProvider(bindRegistries(VanillaBlockTagsProvider::new, completablefuture1));
        TagsProvider<Item> tagsprovider1 = datagenerator$packgenerator1.addProvider(
            p_274753_ -> new VanillaItemTagsProvider(p_274753_, completablefuture1, tagsprovider.contentsGetter())
        );
        TagsProvider<Biome> tagsprovider2 = datagenerator$packgenerator1.addProvider(bindRegistries(BiomeTagsProvider::new, completablefuture1));
        TagsProvider<BannerPattern> tagsprovider3 = datagenerator$packgenerator1.addProvider(bindRegistries(BannerPatternTagsProvider::new, completablefuture1));
        TagsProvider<Structure> tagsprovider4 = datagenerator$packgenerator1.addProvider(bindRegistries(StructureTagsProvider::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(CatVariantTagsProvider::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(DamageTypeTagsProvider::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(EntityTypeTagsProvider::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(FlatLevelGeneratorPresetTagsProvider::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(FluidTagsProvider::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(GameEventTagsProvider::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(InstrumentTagsProvider::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(PaintingVariantTagsProvider::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(PoiTypeTagsProvider::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(WorldPresetTagsProvider::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(VanillaEnchantmentTagsProvider::new, completablefuture1));
        datagenerator$packgenerator1 = pDataGenerator.getVanillaPack(pDev);
        datagenerator$packgenerator1.addProvider(p_253386_ -> new NbtToSnbt(p_253386_, pPaths));
        datagenerator$packgenerator1 = pDataGenerator.getVanillaPack(pReports);
        datagenerator$packgenerator1.addProvider(bindRegistries(BiomeParametersDumpReport::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(ItemListReport::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(BlockListReport::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(bindRegistries(CommandsReport::new, completablefuture1));
        datagenerator$packgenerator1.addProvider(RegistryDumpReport::new);
        datagenerator$packgenerator1.addProvider(PacketReport::new);
        datagenerator$packgenerator1.addProvider(DatapackStructureReport::new);
        CompletableFuture<RegistrySetBuilder.PatchedRegistries> completablefuture2 = TradeRebalanceRegistries.createLookup(completablefuture1);
        CompletableFuture<HolderLookup.Provider> completablefuture = completablefuture2.thenApply(RegistrySetBuilder.PatchedRegistries::patches);
        DataGenerator.PackGenerator datagenerator$packgenerator2 = pDataGenerator.getBuiltinDatapack(pServer, "trade_rebalance");
        datagenerator$packgenerator2.addProvider(bindRegistries(RegistriesDatapackGenerator::new, completablefuture));
        datagenerator$packgenerator2.addProvider(
            p_296336_ -> PackMetadataGenerator.forFeaturePack(
                    p_296336_, Component.translatable("dataPack.trade_rebalance.description"), FeatureFlagSet.of(FeatureFlags.TRADE_REBALANCE)
                )
        );
        datagenerator$packgenerator2.addProvider(bindRegistries(TradeRebalanceLootTableProvider::create, completablefuture1));
        datagenerator$packgenerator2.addProvider(bindRegistries(TradeRebalanceStructureTagsProvider::new, completablefuture1));
        datagenerator$packgenerator2.addProvider(bindRegistries(TradeRebalanceEnchantmentTagsProvider::new, completablefuture1));
        datagenerator$packgenerator1 = pDataGenerator.getBuiltinDatapack(pServer, "redstone_experiments");
        datagenerator$packgenerator1.addProvider(
            p_358165_ -> PackMetadataGenerator.forFeaturePack(
                    p_358165_, Component.translatable("dataPack.redstone_experiments.description"), FeatureFlagSet.of(FeatureFlags.REDSTONE_EXPERIMENTS)
                )
        );
        datagenerator$packgenerator1 = pDataGenerator.getBuiltinDatapack(pServer, "minecart_improvements");
        datagenerator$packgenerator1.addProvider(
            p_358177_ -> PackMetadataGenerator.forFeaturePack(
                    p_358177_, Component.translatable("dataPack.minecart_improvements.description"), FeatureFlagSet.of(FeatureFlags.MINECART_IMPROVEMENTS)
                )
        );
    }
}