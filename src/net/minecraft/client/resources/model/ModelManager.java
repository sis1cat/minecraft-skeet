package net.minecraft.client.resources.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import java.io.Reader;
import java.io.StringReader;
import java.lang.invoke.StringConcatFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SpecialBlockModelRenderer;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.optifine.Config;
import net.optifine.CustomItems;
import net.optifine.reflect.Reflector;
import net.optifine.util.PathUtils;
import net.optifine.util.TextureUtils;
import org.slf4j.Logger;

public class ModelManager implements PreparableReloadListener, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter MODEL_LISTER = FileToIdConverter.json("models");
    private static final Map<ResourceLocation, ResourceLocation> VANILLA_ATLASES = Map.of(
        Sheets.BANNER_SHEET,
        ResourceLocation.withDefaultNamespace("banner_patterns"),
        Sheets.BED_SHEET,
        ResourceLocation.withDefaultNamespace("beds"),
        Sheets.CHEST_SHEET,
        ResourceLocation.withDefaultNamespace("chests"),
        Sheets.SHIELD_SHEET,
        ResourceLocation.withDefaultNamespace("shield_patterns"),
        Sheets.SIGN_SHEET,
        ResourceLocation.withDefaultNamespace("signs"),
        Sheets.SHULKER_SHEET,
        ResourceLocation.withDefaultNamespace("shulker_boxes"),
        Sheets.ARMOR_TRIMS_SHEET,
        ResourceLocation.withDefaultNamespace("armor_trims"),
        Sheets.DECORATED_POT_SHEET,
        ResourceLocation.withDefaultNamespace("decorated_pot"),
        TextureAtlas.LOCATION_BLOCKS,
        ResourceLocation.withDefaultNamespace("blocks")
    );
    private Map<ModelResourceLocation, BakedModel> bakedBlockStateModels = Map.of();
    private Map<ResourceLocation, ItemModel> bakedItemStackModels = Map.of();
    private Map<ResourceLocation, ClientItem.Properties> itemProperties = Map.of();
    private final AtlasSet atlases;
    private final BlockModelShaper blockModelShaper;
    private final BlockColors blockColors;
    private EntityModelSet entityModelSet = EntityModelSet.EMPTY;
    private SpecialBlockModelRenderer specialBlockModelRenderer = SpecialBlockModelRenderer.EMPTY;
    private int maxMipmapLevels;
    private BakedModel missingModel;
    private ItemModel missingItemModel;
    private Object2IntMap<BlockState> modelGroups = Object2IntMaps.emptyMap();
    private Map<ModelResourceLocation, BakedModel> bakedBlockStateModelsView = Map.of();
    private Map<ResourceLocation, ItemModel> bakedItemStackModelsView = Map.of();
    private ModelBakery modelBakery;

    public ModelManager(TextureManager pTextureManager, BlockColors pBlockColors, int pMaxMipmapLevels) {
        this.blockColors = pBlockColors;
        this.maxMipmapLevels = pMaxMipmapLevels;
        this.blockModelShaper = new BlockModelShaper(this);
        this.atlases = new AtlasSet(VANILLA_ATLASES, pTextureManager);
    }

    public BakedModel getModel(ModelResourceLocation pModelLocation) {
        return this.bakedBlockStateModels.getOrDefault(pModelLocation, this.missingModel);
    }

    public BakedModel getMissingModel() {
        return this.missingModel;
    }

    public ItemModel getItemModel(ResourceLocation pModelLocation) {
        return this.bakedItemStackModels.getOrDefault(pModelLocation, this.missingItemModel);
    }

    public ClientItem.Properties getItemProperties(ResourceLocation pItemId) {
        return this.itemProperties.getOrDefault(pItemId, ClientItem.Properties.DEFAULT);
    }

    public BlockModelShaper getBlockModelShaper() {
        return this.blockModelShaper;
    }

    @Override
    public final CompletableFuture<Void> reload(
        PreparableReloadListener.PreparationBarrier p_249079_, ResourceManager p_251134_, Executor p_250550_, Executor p_249221_
    ) {
        Reflector.GeometryLoaderManager_init.call();
        UnbakedModel unbakedmodel = MissingBlockModel.missingModel();
        CompletableFuture<EntityModelSet> completablefuture = CompletableFuture.supplyAsync(EntityModelSet::vanilla, p_250550_);
        CompletableFuture<SpecialBlockModelRenderer> completablefuture1 = completablefuture.thenApplyAsync(SpecialBlockModelRenderer::vanilla, p_250550_);
        CompletableFuture<Map<ResourceLocation, UnbakedModel>> completablefuture2 = loadBlockModels(p_251134_, p_250550_);
        CompletableFuture<BlockStateModelLoader.LoadedModels> completablefuture3 = BlockStateModelLoader.loadBlockStates(unbakedmodel, p_251134_, p_250550_);
        CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> completablefuture4 = ClientItemInfoLoader.scheduleLoad(p_251134_, p_250550_);
        CompletableFuture<ModelDiscovery> completablefuture5 = CompletableFuture.allOf(completablefuture2, completablefuture3, completablefuture4)
            .thenApplyAsync(voidIn -> discoverModelDependencies(unbakedmodel, completablefuture2.join(), completablefuture3.join(), completablefuture4.join()), p_250550_);
        CompletableFuture<Object2IntMap<BlockState>> completablefuture6 = completablefuture3.thenApplyAsync(
            modelsIn -> buildModelGroups(this.blockColors, modelsIn), p_250550_
        );
        Map<ResourceLocation, CompletableFuture<AtlasSet.StitchResult>> map = this.atlases.scheduleLoad(p_251134_, this.maxMipmapLevels, p_250550_);
        return CompletableFuture.allOf(
                Stream.concat(
                        map.values().stream(),
                        Stream.of(completablefuture5, completablefuture6, completablefuture3, completablefuture4, completablefuture, completablefuture1)
                    )
                    .toArray(CompletableFuture[]::new)
            )
            .thenApplyAsync(
                voidIn -> {
                    Map<ResourceLocation, AtlasSet.StitchResult> map1 = map.entrySet()
                        .stream()
                        .collect(Collectors.toMap(Entry::getKey, entryIn -> entryIn.getValue().join()));
                    ModelDiscovery modeldiscovery = completablefuture5.join();
                    Object2IntMap<BlockState> object2intmap = completablefuture6.join();
                    Set<ResourceLocation> set = modeldiscovery.getUnreferencedModels();
                    if (!set.isEmpty()) {
                        LOGGER.debug("Unreferenced models: \n{}", set.stream().sorted().map(locIn -> "\t" + locIn + "\n").collect(Collectors.joining()));
                    }

                    ModelBakery modelbakery = new ModelBakery(
                        completablefuture.join(),
                        completablefuture3.join().plainModels(),
                        completablefuture4.join().contents(),
                        modeldiscovery.getReferencedModels(),
                        unbakedmodel
                    );
                    return loadModels(Profiler.get(), map1, modelbakery, object2intmap, completablefuture.join(), completablefuture1.join());
                },
                p_250550_
            )
            .thenCompose(stateIn -> stateIn.readyForUpload.thenApply(void2In -> (ModelManager.ReloadState)stateIn))
            .thenCompose(p_249079_::wait)
            .thenAcceptAsync(stateIn -> this.apply(stateIn, Profiler.get()), p_249221_);
    }

    private static CompletableFuture<Map<ResourceLocation, UnbakedModel>> loadBlockModels(ResourceManager pResourceManager, Executor pExecutor) {
        return CompletableFuture.<Map<ResourceLocation, Resource>>supplyAsync(() -> MODEL_LISTER.listMatchingResources(pResourceManager), pExecutor)
            .thenCompose(
                resourcesIn -> {
                    List<CompletableFuture<Pair<ResourceLocation, BlockModel>>> list = new ArrayList<>(resourcesIn.size());
                    TextureUtils.registerCustomModels((Map<ResourceLocation, Resource>)resourcesIn);

                    for (Entry<ResourceLocation, Resource> entry : resourcesIn.entrySet()) {
                        list.add(CompletableFuture.supplyAsync(() -> {
                            ResourceLocation resourcelocation = MODEL_LISTER.fileToId(entry.getKey());

                            try {
                                Pair pair;
                                try (Reader reader = entry.getValue().openAsReader()) {
                                    Reader reader1 = reader;
                                    if (resourcelocation.isOptiFine()) {
                                        String s = Config.readAll(reader);
                                        String s1 = PathUtils.removeLast(resourcelocation.getPath());
                                        s = s.replace("\"./", "\"" + s1 + "/");
                                        reader1 = new StringReader(s);
                                    }

                                    pair = Pair.of(resourcelocation, BlockModel.fromStream(reader1));
                                }

                                return pair;
                            } catch (Exception exception1) {
                                LOGGER.error("Failed to load model {}", entry.getKey(), exception1);
                                return null;
                            }
                        }, pExecutor));
                    }

                    return Util.sequence(list)
                        .thenApply(
                            modelsIn -> modelsIn.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond))
                        );
                }
            );
    }

    private static ModelDiscovery discoverModelDependencies(
        UnbakedModel pMissingModel,
        Map<ResourceLocation, UnbakedModel> pInputModels,
        BlockStateModelLoader.LoadedModels pLoadedModels,
        ClientItemInfoLoader.LoadedClientInfos pLoadedClientInfos
    ) {
        ModelDiscovery modeldiscovery = new ModelDiscovery(pInputModels, pMissingModel);
        pLoadedModels.forResolving().forEach(modeldiscovery::addRoot);
        pLoadedClientInfos.contents().values().forEach(itemIn -> modeldiscovery.addRoot(itemIn.model()));
        modeldiscovery.registerSpecialModels();
        modeldiscovery.discoverDependencies();
        if (Config.isCustomItems()) {
            modeldiscovery.resolveCustomModels();
            CustomItems.collectModelSprites(pInputModels);
        }

        return modeldiscovery;
    }

    private static ModelManager.ReloadState loadModels(
        ProfilerFiller pProfiler,
        final Map<ResourceLocation, AtlasSet.StitchResult> pAtlasPreperations,
        ModelBakery pModelBakery,
        Object2IntMap<BlockState> pModelGroups,
        EntityModelSet pEntityModelSet,
        SpecialBlockModelRenderer pSpecialBlockModelRenderer
    ) {
        pProfiler.push("baking");
        final Multimap<String, Material> multimap = HashMultimap.create();
        final Multimap<String, String> multimap1 = HashMultimap.create();
        final TextureAtlasSprite textureatlassprite = pAtlasPreperations.get(TextureAtlas.LOCATION_BLOCKS).missing();
        ModelBakery.BakingResult modelbakery$bakingresult = pModelBakery.bakeModels(new ModelBakery.TextureGetter() {
            @Override
            public TextureAtlasSprite get(ModelDebugName p_375833_, Material p_375858_) {
                AtlasSet.StitchResult atlasset$stitchresult = pAtlasPreperations.get(p_375858_.atlasLocation());
                TextureAtlasSprite textureatlassprite1 = atlasset$stitchresult.getSprite(p_375858_.texture());
                if (textureatlassprite1 != null) {
                    return textureatlassprite1;
                } else {
                    multimap.put(p_375833_.get(), p_375858_);
                    return atlasset$stitchresult.missing();
                }
            }

            @Override
            public TextureAtlasSprite reportMissingReference(ModelDebugName p_377684_, String p_378821_) {
                multimap1.put(p_377684_.get(), p_378821_);
                return textureatlassprite;
            }
        });
        multimap.asMap()
            .forEach(
                (locIn, materialsIn) -> LOGGER.warn(
                        "Missing textures in model {}: {}",
                        locIn,
                        materialsIn.stream()
                            .sorted(Material.COMPARATOR)
                            .map(materialIn -> materialIn.atlasLocation() + ":" + materialIn.texture())
                            .collect(Collectors.joining(", "))
                    )
            );
        multimap1.asMap()
            .forEach(
                (nameIn, refsIn) -> LOGGER.warn(
                        "Missing texture references in model {}: {}",
                        nameIn,
                        refsIn.stream()
                            .sorted()
//                            .map(strIn -> StringConcatFactory.makeConcatWithConstants<"makeConcatWithConstants","\u0001">(strIn))
                            .collect(Collectors.joining(", "))
                    )
            );
        pProfiler.popPush("forge_modify_baking_result");
        Reflector.ForgeHooksClient_onModifyBakingResult.call(pModelBakery, modelbakery$bakingresult);
        pProfiler.popPush("dispatch");
        Map<BlockState, BakedModel> map = createBlockStateToModelDispatch(modelbakery$bakingresult.blockStateModels(), modelbakery$bakingresult.missingModel());
        CompletableFuture<Void> completablefuture = CompletableFuture.allOf(
            pAtlasPreperations.values().stream().map(AtlasSet.StitchResult::readyForUpload).toArray(CompletableFuture[]::new)
        );
        pProfiler.pop();
        return new ModelManager.ReloadState(modelbakery$bakingresult, pModelGroups, map, pAtlasPreperations, pEntityModelSet, pSpecialBlockModelRenderer, completablefuture, pModelBakery);
    }

    private static Map<BlockState, BakedModel> createBlockStateToModelDispatch(Map<ModelResourceLocation, BakedModel> pBlockStateModels, BakedModel pMissingModel) {
        Map<BlockState, BakedModel> map = new IdentityHashMap<>();

        for (Block block : BuiltInRegistries.BLOCK) {
            block.getStateDefinition().getPossibleStates().forEach(stateIn -> {
                ResourceLocation resourcelocation = stateIn.getBlock().builtInRegistryHolder().key().location();
                ModelResourceLocation modelresourcelocation = BlockModelShaper.stateToModelLocation(resourcelocation, stateIn);
                BakedModel bakedmodel = pBlockStateModels.get(modelresourcelocation);
                if (bakedmodel == null) {
                    LOGGER.warn("Missing model for variant: '{}'", modelresourcelocation);
                    map.putIfAbsent(stateIn, pMissingModel);
                } else {
                    map.put(stateIn, bakedmodel);
                }
            });
        }

        return map;
    }

    private static Object2IntMap<BlockState> buildModelGroups(BlockColors pBlockColors, BlockStateModelLoader.LoadedModels pLoadedModels) {
        return ModelGroupCollector.build(pBlockColors, pLoadedModels);
    }

    private void apply(ModelManager.ReloadState pReloadState, ProfilerFiller pProfiler) {
        pProfiler.push("upload");
        pReloadState.atlasPreparations.values().forEach(AtlasSet.StitchResult::upload);
        ModelBakery.BakingResult modelbakery$bakingresult = pReloadState.bakedModels;
        this.bakedBlockStateModels = modelbakery$bakingresult.blockStateModels();
        this.bakedItemStackModels = modelbakery$bakingresult.itemStackModels();
        this.itemProperties = modelbakery$bakingresult.itemProperties();
        this.modelGroups = pReloadState.modelGroups;
        this.missingModel = modelbakery$bakingresult.missingModel();
        this.missingItemModel = modelbakery$bakingresult.missingItemModel();
        this.bakedBlockStateModelsView = Collections.unmodifiableMap(this.bakedBlockStateModels);
        this.bakedItemStackModelsView = Collections.unmodifiableMap(this.bakedItemStackModels);
        this.modelBakery = pReloadState.modelBakery();
        Reflector.ForgeHooksClient_onModelBake.call(this, this.modelBakery);
        pProfiler.popPush("cache");
        this.blockModelShaper.replaceCache(pReloadState.modelCache);
        this.specialBlockModelRenderer = pReloadState.specialBlockModelRenderer;
        this.entityModelSet = pReloadState.entityModelSet;
        pProfiler.pop();
    }

    public boolean requiresRender(BlockState pOldState, BlockState pNewState) {
        if (pOldState == pNewState) {
            return false;
        } else {
            int i = this.modelGroups.getInt(pOldState);
            if (i != -1) {
                int j = this.modelGroups.getInt(pNewState);
                if (i == j) {
                    FluidState fluidstate = pOldState.getFluidState();
                    FluidState fluidstate1 = pNewState.getFluidState();
                    return fluidstate != fluidstate1;
                }
            }

            return true;
        }
    }

    public TextureAtlas getAtlas(ResourceLocation pLocation) {
        if (this.atlases == null) {
            throw new RuntimeException("getAtlasTexture called too early!");
        } else {
            return this.atlases.getAtlas(pLocation);
        }
    }

    @Override
    public void close() {
        this.atlases.close();
    }

    public void updateMaxMipLevel(int pLevel) {
        this.maxMipmapLevels = pLevel;
    }

    public BakedModel getBakedItemModel(ResourceLocation locationIn) {
        ItemModel itemmodel = this.bakedItemStackModels.get(locationIn);
        return itemmodel instanceof BlockModelWrapper blockmodelwrapper ? blockmodelwrapper.getModel() : this.getMissingModel();
    }

    public Map<ModelResourceLocation, BakedModel> getModels() {
        return this.bakedBlockStateModelsView;
    }

    public Map<ResourceLocation, ItemModel> getItemModels() {
        return this.bakedItemStackModelsView;
    }

    public ModelBakery getModelBakery() {
        return Preconditions.checkNotNull(this.modelBakery, "Attempted to query model bakery before it has been initialized.");
    }

    public Supplier<SpecialBlockModelRenderer> specialBlockModelRenderer() {
        return () -> this.specialBlockModelRenderer;
    }

    public Supplier<EntityModelSet> entityModels() {
        return () -> this.entityModelSet;
    }

    static record ReloadState(
        ModelBakery.BakingResult bakedModels,
        Object2IntMap<BlockState> modelGroups,
        Map<BlockState, BakedModel> modelCache,
        Map<ResourceLocation, AtlasSet.StitchResult> atlasPreparations,
        EntityModelSet entityModelSet,
        SpecialBlockModelRenderer specialBlockModelRenderer,
        CompletableFuture<Void> readyForUpload,
        ModelBakery modelBakery
    ) {
    }
}