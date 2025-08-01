package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPresets;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class CreateWorldScreen extends Screen {
    private static final int GROUP_BOTTOM = 1;
    private static final int TAB_COLUMN_WIDTH = 210;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TEMP_WORLD_PREFIX = "mcworld-";
    static final Component GAME_MODEL_LABEL = Component.translatable("selectWorld.gameMode");
    static final Component NAME_LABEL = Component.translatable("selectWorld.enterName");
    static final Component EXPERIMENTS_LABEL = Component.translatable("selectWorld.experiments");
    static final Component ALLOW_COMMANDS_INFO = Component.translatable("selectWorld.allowCommands.info");
    private static final Component PREPARING_WORLD_DATA = Component.translatable("createWorld.preparing");
    private static final int HORIZONTAL_BUTTON_SPACING = 10;
    private static final int VERTICAL_BUTTON_SPACING = 8;
    public static final ResourceLocation TAB_HEADER_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/tab_header_background.png");
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    final WorldCreationUiState uiState;
    private final TabManager tabManager = new TabManager(p_374587_ -> {
        AbstractWidget abstractwidget = this.addRenderableWidget(p_374587_);
    }, p_325424_ -> this.removeWidget(p_325424_));
    private boolean recreated;
    private final DirectoryValidator packValidator;
    private final CreateWorldCallback createWorldCallback;
    @Nullable
    private final Screen lastScreen;
    @Nullable
    private Path tempDataPackDir;
    @Nullable
    private PackRepository tempDataPackRepository;
    @Nullable
    private TabNavigationBar tabNavigationBar;

    public static void openFresh(Minecraft pMinecraft, @Nullable Screen pLastScreen) {
        openFresh(pMinecraft, pLastScreen, (p_357709_, p_357710_, p_357711_, p_357712_) -> p_357709_.createNewWorld(p_357710_, p_357711_));
    }

    public static void openFresh(Minecraft pMinecraft, @Nullable Screen pLastScreen, CreateWorldCallback pCallback) {
        WorldCreationContextMapper worldcreationcontextmapper = (p_357732_, p_357733_, p_357734_) -> new WorldCreationContext(
                p_357734_.worldGenSettings(), p_357733_, p_357732_, p_357734_.dataConfiguration()
            );
        Function<WorldLoader.DataLoadContext, WorldGenSettings> function = p_357697_ -> new WorldGenSettings(
                WorldOptions.defaultWithRandomSeed(), WorldPresets.createNormalWorldDimensions(p_357697_.datapackWorldgen())
            );
        openCreateWorldScreen(pMinecraft, pLastScreen, function, worldcreationcontextmapper, WorldPresets.NORMAL, pCallback);
    }

    public static void testWorld(Minecraft pMinecraft, @Nullable Screen pLastScreen) {
        WorldCreationContextMapper worldcreationcontextmapper = (p_357698_, p_357699_, p_357700_) -> new WorldCreationContext(
                p_357700_.worldGenSettings().options(),
                p_357700_.worldGenSettings().dimensions(),
                p_357699_,
                p_357698_,
                p_357700_.dataConfiguration(),
                new InitialWorldCreationOptions(
                    WorldCreationUiState.SelectedGameMode.CREATIVE,
                    Set.of(GameRules.RULE_DAYLIGHT, GameRules.RULE_WEATHER_CYCLE, GameRules.RULE_DOMOBSPAWNING),
                    FlatLevelGeneratorPresets.REDSTONE_READY
                )
            );
        Function<WorldLoader.DataLoadContext, WorldGenSettings> function = p_357731_ -> new WorldGenSettings(
                WorldOptions.testWorldWithRandomSeed(), WorldPresets.createFlatWorldDimensions(p_357731_.datapackWorldgen())
            );
        openCreateWorldScreen(
            pMinecraft,
            pLastScreen,
            function,
            worldcreationcontextmapper,
            WorldPresets.FLAT,
            (p_357719_, p_357720_, p_357721_, p_357722_) -> p_357719_.createNewWorld(p_357720_, p_357721_)
        );
    }

    private static void openCreateWorldScreen(
        Minecraft pMinecraft,
        @Nullable Screen pLastScreen,
        Function<WorldLoader.DataLoadContext, WorldGenSettings> pWorldGenSettingsGetter,
        WorldCreationContextMapper pCreationContextMapper,
        ResourceKey<WorldPreset> pPreset,
        CreateWorldCallback pCreateWorldCallback
    ) {
        queueLoadScreen(pMinecraft, PREPARING_WORLD_DATA);
        PackRepository packrepository = new PackRepository(new ServerPacksSource(pMinecraft.directoryValidator()));
        WorldLoader.InitConfig worldloader$initconfig = createDefaultLoadConfig(packrepository, WorldDataConfiguration.DEFAULT);
        CompletableFuture<WorldCreationContext> completablefuture = WorldLoader.load(
            worldloader$initconfig,
            p_357718_ -> new WorldLoader.DataLoadOutput<>(new DataPackReloadCookie(pWorldGenSettingsGetter.apply(p_357718_), p_357718_.dataConfiguration()), p_357718_.datapackDimensions()),
            (p_357705_, p_357706_, p_357707_, p_357708_) -> {
                p_357705_.close();
                return pCreationContextMapper.apply(p_357706_, p_357707_, p_357708_);
            },
            Util.backgroundExecutor(),
            pMinecraft
        );
        pMinecraft.managedBlock(completablefuture::isDone);
        pMinecraft.setScreen(new CreateWorldScreen(pMinecraft, pLastScreen, completablefuture.join(), Optional.of(pPreset), OptionalLong.empty(), pCreateWorldCallback));
    }

    public static CreateWorldScreen createFromExisting(
        Minecraft pMinecraft, @Nullable Screen pLastScreen, LevelSettings pLevelSettings, WorldCreationContext pContext, @Nullable Path pTempDataPackDir
    ) {
        CreateWorldScreen createworldscreen = new CreateWorldScreen(
            pMinecraft,
            pLastScreen,
            pContext,
            WorldPresets.fromSettings(pContext.selectedDimensions()),
            OptionalLong.of(pContext.options().seed()),
            (p_357713_, p_357714_, p_357715_, p_357716_) -> p_357713_.createNewWorld(p_357714_, p_357715_)
        );
        createworldscreen.recreated = true;
        createworldscreen.uiState.setName(pLevelSettings.levelName());
        createworldscreen.uiState.setAllowCommands(pLevelSettings.allowCommands());
        createworldscreen.uiState.setDifficulty(pLevelSettings.difficulty());
        createworldscreen.uiState.getGameRules().assignFrom(pLevelSettings.gameRules(), null);
        if (pLevelSettings.hardcore()) {
            createworldscreen.uiState.setGameMode(WorldCreationUiState.SelectedGameMode.HARDCORE);
        } else if (pLevelSettings.gameType().isSurvival()) {
            createworldscreen.uiState.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
        } else if (pLevelSettings.gameType().isCreative()) {
            createworldscreen.uiState.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
        }

        createworldscreen.tempDataPackDir = pTempDataPackDir;
        return createworldscreen;
    }

    private CreateWorldScreen(
        Minecraft pMinecraft,
        @Nullable Screen pLastScreen,
        WorldCreationContext pContext,
        Optional<ResourceKey<WorldPreset>> pPreset,
        OptionalLong pSeed,
        CreateWorldCallback pCreateWorldCallback
    ) {
        super(Component.translatable("selectWorld.create"));
        this.lastScreen = pLastScreen;
        this.packValidator = pMinecraft.directoryValidator();
        this.createWorldCallback = pCreateWorldCallback;
        this.uiState = new WorldCreationUiState(pMinecraft.getLevelSource().getBaseDir(), pContext, pPreset, pSeed);
    }

    public WorldCreationUiState getUiState() {
        return this.uiState;
    }

    @Override
    protected void init() {
        this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width)
            .addTabs(new CreateWorldScreen.GameTab(), new CreateWorldScreen.WorldTab(), new CreateWorldScreen.MoreTab())
            .build();
        this.addRenderableWidget(this.tabNavigationBar);
        LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        linearlayout.addChild(Button.builder(Component.translatable("selectWorld.create"), p_232938_ -> this.onCreate()).build());
        linearlayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_232903_ -> this.popScreen()).build());
        this.layout.visitWidgets(p_374585_ -> {
            p_374585_.setTabOrderGroup(1);
            this.addRenderableWidget(p_374585_);
        });
        this.tabNavigationBar.selectTab(0, false);
        this.uiState.onChanged();
        this.repositionElements();
    }

    @Override
    protected void setInitialFocus() {
    }

    @Override
    public void repositionElements() {
        if (this.tabNavigationBar != null) {
            this.tabNavigationBar.setWidth(this.width);
            this.tabNavigationBar.arrangeElements();
            int i = this.tabNavigationBar.getRectangle().bottom();
            ScreenRectangle screenrectangle = new ScreenRectangle(0, i, this.width, this.height - this.layout.getFooterHeight() - i);
            this.tabManager.setTabArea(screenrectangle);
            this.layout.setHeaderHeight(i);
            this.layout.arrangeElements();
        }
    }

    private static void queueLoadScreen(Minecraft pMinecraft, Component pTitle) {
        pMinecraft.forceSetScreen(new GenericMessageScreen(pTitle));
    }

    private void onCreate() {
        WorldCreationContext worldcreationcontext = this.uiState.getSettings();
        WorldDimensions.Complete worlddimensions$complete = worldcreationcontext.selectedDimensions().bake(worldcreationcontext.datapackDimensions());
        LayeredRegistryAccess<RegistryLayer> layeredregistryaccess = worldcreationcontext.worldgenRegistries()
            .replaceFrom(RegistryLayer.DIMENSIONS, worlddimensions$complete.dimensionsRegistryAccess());
        Lifecycle lifecycle = FeatureFlags.isExperimental(worldcreationcontext.dataConfiguration().enabledFeatures()) ? Lifecycle.experimental() : Lifecycle.stable();
        Lifecycle lifecycle1 = layeredregistryaccess.compositeAccess().allRegistriesLifecycle();
        Lifecycle lifecycle2 = lifecycle1.add(lifecycle);
        boolean flag = !this.recreated && lifecycle1 == Lifecycle.stable();
        LevelSettings levelsettings = this.createLevelSettings(worlddimensions$complete.specialWorldProperty() == PrimaryLevelData.SpecialWorldProperty.DEBUG);
        PrimaryLevelData primaryleveldata = new PrimaryLevelData(
            levelsettings, this.uiState.getSettings().options(), worlddimensions$complete.specialWorldProperty(), lifecycle2
        );
        WorldOpenFlows.confirmWorldCreation(this.minecraft, this, lifecycle2, () -> this.createWorldAndCleanup(layeredregistryaccess, primaryleveldata), flag);
    }

    private void createWorldAndCleanup(LayeredRegistryAccess<RegistryLayer> pRegistryAccess, PrimaryLevelData pLevelData) {
        boolean flag = this.createWorldCallback.create(this, pRegistryAccess, pLevelData, this.tempDataPackDir);
        this.removeTempDataPackDir();
        if (!flag) {
            this.popScreen();
        }
    }

    private boolean createNewWorld(LayeredRegistryAccess<RegistryLayer> pRegistryAccess, WorldData pWorldData) {
        String s = this.uiState.getTargetFolder();
        WorldCreationContext worldcreationcontext = this.uiState.getSettings();
        queueLoadScreen(this.minecraft, PREPARING_WORLD_DATA);
        Optional<LevelStorageSource.LevelStorageAccess> optional = createNewWorldDirectory(this.minecraft, s, this.tempDataPackDir);
        if (optional.isEmpty()) {
            SystemToast.onPackCopyFailure(this.minecraft, s);
            return false;
        } else {
            this.minecraft.createWorldOpenFlows().createLevelFromExistingSettings(optional.get(), worldcreationcontext.dataPackResources(), pRegistryAccess, pWorldData);
            return true;
        }
    }

    private LevelSettings createLevelSettings(boolean pDebug) {
        String s = this.uiState.getName().trim();
        if (pDebug) {
            GameRules gamerules = new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures());
            gamerules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
            return new LevelSettings(s, GameType.SPECTATOR, false, Difficulty.PEACEFUL, true, gamerules, WorldDataConfiguration.DEFAULT);
        } else {
            return new LevelSettings(
                s,
                this.uiState.getGameMode().gameType,
                this.uiState.isHardcore(),
                this.uiState.getDifficulty(),
                this.uiState.isAllowCommands(),
                this.uiState.getGameRules(),
                this.uiState.getSettings().dataConfiguration()
            );
        }
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (this.tabNavigationBar.keyPressed(pKeyCode)) {
            return true;
        } else if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        } else if (pKeyCode != 257 && pKeyCode != 335) {
            return false;
        } else {
            this.onCreate();
            return true;
        }
    }

    @Override
    public void onClose() {
        this.popScreen();
    }

    public void popScreen() {
        this.minecraft.setScreen(this.lastScreen);
        this.removeTempDataPackDir();
    }

    @Override
    public void render(GuiGraphics p_282137_, int p_283640_, int p_281243_, float p_282743_) {
        super.render(p_282137_, p_283640_, p_281243_, p_282743_);
        p_282137_.blit(RenderType::guiTextured, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
    }

    @Override
    protected void renderMenuBackground(GuiGraphics p_334471_) {
        p_334471_.blit(RenderType::guiTextured, TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16);
        this.renderMenuBackground(p_334471_, 0, this.layout.getHeaderHeight(), this.width, this.height);
    }

    @Nullable
    private Path getOrCreateTempDataPackDir() {
        if (this.tempDataPackDir == null) {
            try {
                this.tempDataPackDir = Files.createTempDirectory("mcworld-");
            } catch (IOException ioexception) {
                LOGGER.warn("Failed to create temporary dir", (Throwable)ioexception);
                SystemToast.onPackCopyFailure(this.minecraft, this.uiState.getTargetFolder());
                this.popScreen();
            }
        }

        return this.tempDataPackDir;
    }

    void openExperimentsScreen(WorldDataConfiguration pWorldDataConfiguration) {
        Pair<Path, PackRepository> pair = this.getDataPackSelectionSettings(pWorldDataConfiguration);
        if (pair != null) {
            this.minecraft.setScreen(new ExperimentsScreen(this, pair.getSecond(), p_269636_ -> this.tryApplyNewDataPacks(p_269636_, false, this::openExperimentsScreen)));
        }
    }

    void openDataPackSelectionScreen(WorldDataConfiguration pWorldDataConfiguration) {
        Pair<Path, PackRepository> pair = this.getDataPackSelectionSettings(pWorldDataConfiguration);
        if (pair != null) {
            this.minecraft
                .setScreen(
                    new PackSelectionScreen(
                        pair.getSecond(), p_269637_ -> this.tryApplyNewDataPacks(p_269637_, true, this::openDataPackSelectionScreen), pair.getFirst(), Component.translatable("dataPack.title")
                    )
                );
        }
    }

    private void tryApplyNewDataPacks(PackRepository pPackRepository, boolean pShouldConfirm, Consumer<WorldDataConfiguration> pCallback) {
        List<String> list = ImmutableList.copyOf(pPackRepository.getSelectedIds());
        List<String> list1 = pPackRepository.getAvailableIds().stream().filter(p_232927_ -> !list.contains(p_232927_)).collect(ImmutableList.toImmutableList());
        WorldDataConfiguration worlddataconfiguration = new WorldDataConfiguration(
            new DataPackConfig(list, list1), this.uiState.getSettings().dataConfiguration().enabledFeatures()
        );
        if (this.uiState.tryUpdateDataConfiguration(worlddataconfiguration)) {
            this.minecraft.setScreen(this);
        } else {
            FeatureFlagSet featureflagset = pPackRepository.getRequestedFeatureFlags();
            if (FeatureFlags.isExperimental(featureflagset) && pShouldConfirm) {
                this.minecraft.setScreen(new ConfirmExperimentalFeaturesScreen(pPackRepository.getSelectedPacks(), p_269635_ -> {
                    if (p_269635_) {
                        this.applyNewPackConfig(pPackRepository, worlddataconfiguration, pCallback);
                    } else {
                        pCallback.accept(this.uiState.getSettings().dataConfiguration());
                    }
                }));
            } else {
                this.applyNewPackConfig(pPackRepository, worlddataconfiguration, pCallback);
            }
        }
    }

    private void applyNewPackConfig(PackRepository pPackRepository, WorldDataConfiguration pWorldDataConfiguration, Consumer<WorldDataConfiguration> pCallback) {
        this.minecraft.forceSetScreen(new GenericMessageScreen(Component.translatable("dataPack.validation.working")));
        WorldLoader.InitConfig worldloader$initconfig = createDefaultLoadConfig(pPackRepository, pWorldDataConfiguration);
        WorldLoader.<DataPackReloadCookie, WorldCreationContext>load(
                worldloader$initconfig,
                p_325422_ -> {
                    if (p_325422_.datapackWorldgen().lookupOrThrow(Registries.WORLD_PRESET).listElements().findAny().isEmpty()) {
                        throw new IllegalStateException("Needs at least one world preset to continue");
                    } else if (p_325422_.datapackWorldgen().lookupOrThrow(Registries.BIOME).listElements().findAny().isEmpty()) {
                        throw new IllegalStateException("Needs at least one biome continue");
                    } else {
                        WorldCreationContext worldcreationcontext = this.uiState.getSettings();
                        DynamicOps<JsonElement> dynamicops = worldcreationcontext.worldgenLoadContext().createSerializationContext(JsonOps.INSTANCE);
                        DataResult<JsonElement> dataresult = WorldGenSettings.encode(
                                dynamicops, worldcreationcontext.options(), worldcreationcontext.selectedDimensions()
                            )
                            .setLifecycle(Lifecycle.stable());
                        DynamicOps<JsonElement> dynamicops1 = p_325422_.datapackWorldgen().createSerializationContext(JsonOps.INSTANCE);
                        WorldGenSettings worldgensettings = dataresult.<WorldGenSettings>flatMap(
                                p_232895_ -> WorldGenSettings.CODEC.parse(dynamicops1, p_232895_)
                            )
                            .getOrThrow(p_325420_ -> new IllegalStateException("Error parsing worldgen settings after loading data packs: " + p_325420_));
                        return new WorldLoader.DataLoadOutput<>(new DataPackReloadCookie(worldgensettings, p_325422_.dataConfiguration()), p_325422_.datapackDimensions());
                    }
                },
                (p_357727_, p_357728_, p_357729_, p_357730_) -> {
                    p_357727_.close();
                    return new WorldCreationContext(p_357730_.worldGenSettings(), p_357729_, p_357728_, p_357730_.dataConfiguration());
                },
                Util.backgroundExecutor(),
                this.minecraft
            )
            .thenApply(p_340824_ -> {
                p_340824_.validate();
                return (WorldCreationContext)p_340824_;
            })
            .thenAcceptAsync(this.uiState::setSettings, this.minecraft)
            .handleAsync(
                (p_280900_, p_280901_) -> {
                    if (p_280901_ != null) {
                        LOGGER.warn("Failed to validate datapack", p_280901_);
                        this.minecraft
                            .setScreen(
                                new ConfirmScreen(
                                    p_269627_ -> {
                                        if (p_269627_) {
                                            pCallback.accept(this.uiState.getSettings().dataConfiguration());
                                        } else {
                                            pCallback.accept(WorldDataConfiguration.DEFAULT);
                                        }
                                    },
                                    Component.translatable("dataPack.validation.failed"),
                                    CommonComponents.EMPTY,
                                    Component.translatable("dataPack.validation.back"),
                                    Component.translatable("dataPack.validation.reset")
                                )
                            );
                    } else {
                        this.minecraft.setScreen(this);
                    }

                    return null;
                },
                this.minecraft
            );
    }

    private static WorldLoader.InitConfig createDefaultLoadConfig(PackRepository pPackRepository, WorldDataConfiguration pInitialDataConfig) {
        WorldLoader.PackConfig worldloader$packconfig = new WorldLoader.PackConfig(pPackRepository, pInitialDataConfig, false, true);
        return new WorldLoader.InitConfig(worldloader$packconfig, Commands.CommandSelection.INTEGRATED, 2);
    }

    private void removeTempDataPackDir() {
        if (this.tempDataPackDir != null && Files.exists(this.tempDataPackDir)) {
            try (Stream<Path> stream = Files.walk(this.tempDataPackDir)) {
                stream.sorted(Comparator.reverseOrder()).forEach(p_232942_ -> {
                    try {
                        Files.delete(p_232942_);
                    } catch (IOException ioexception1) {
                        LOGGER.warn("Failed to remove temporary file {}", p_232942_, ioexception1);
                    }
                });
            } catch (IOException ioexception) {
                LOGGER.warn("Failed to list temporary dir {}", this.tempDataPackDir);
            }
        }

        this.tempDataPackDir = null;
    }

    private static void copyBetweenDirs(Path pFromDir, Path pToDir, Path pFilePath) {
        try {
            Util.copyBetweenDirs(pFromDir, pToDir, pFilePath);
        } catch (IOException ioexception) {
            LOGGER.warn("Failed to copy datapack file from {} to {}", pFilePath, pToDir);
            throw new UncheckedIOException(ioexception);
        }
    }

    private static Optional<LevelStorageSource.LevelStorageAccess> createNewWorldDirectory(Minecraft pMinecraft, String pSaveName, @Nullable Path pTempDataPackDir) {
        try {
            LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = pMinecraft.getLevelSource().createAccess(pSaveName);
            if (pTempDataPackDir == null) {
                return Optional.of(levelstoragesource$levelstorageaccess);
            }

            try {
                Optional optional;
                try (Stream<Path> stream = Files.walk(pTempDataPackDir)) {
                    Path path = levelstoragesource$levelstorageaccess.getLevelPath(LevelResource.DATAPACK_DIR);
                    FileUtil.createDirectoriesSafe(path);
                    stream.filter(p_232924_ -> !p_232924_.equals(pTempDataPackDir)).forEach(p_357703_ -> copyBetweenDirs(pTempDataPackDir, path, p_357703_));
                    optional = Optional.of(levelstoragesource$levelstorageaccess);
                }

                return optional;
            } catch (UncheckedIOException | IOException ioexception) {
                LOGGER.warn("Failed to copy datapacks to world {}", pSaveName, ioexception);
                levelstoragesource$levelstorageaccess.close();
            }
        } catch (UncheckedIOException | IOException ioexception1) {
            LOGGER.warn("Failed to create access for {}", pSaveName, ioexception1);
        }

        return Optional.empty();
    }

    @Nullable
    public static Path createTempDataPackDirFromExistingWorld(Path pDatapackDir, Minecraft pMinecraft) {
        MutableObject<Path> mutableobject = new MutableObject<>();

        try (Stream<Path> stream = Files.walk(pDatapackDir)) {
            stream.filter(p_357726_ -> !p_357726_.equals(pDatapackDir)).forEach(p_232933_ -> {
                Path path = mutableobject.getValue();
                if (path == null) {
                    try {
                        path = Files.createTempDirectory("mcworld-");
                    } catch (IOException ioexception1) {
                        LOGGER.warn("Failed to create temporary dir");
                        throw new UncheckedIOException(ioexception1);
                    }

                    mutableobject.setValue(path);
                }

                copyBetweenDirs(pDatapackDir, path, p_232933_);
            });
        } catch (UncheckedIOException | IOException ioexception) {
            LOGGER.warn("Failed to copy datapacks from world {}", pDatapackDir, ioexception);
            SystemToast.onPackCopyFailure(pMinecraft, pDatapackDir.toString());
            return null;
        }

        return mutableobject.getValue();
    }

    @Nullable
    private Pair<Path, PackRepository> getDataPackSelectionSettings(WorldDataConfiguration pWorldDataConfiguration) {
        Path path = this.getOrCreateTempDataPackDir();
        if (path != null) {
            if (this.tempDataPackRepository == null) {
                this.tempDataPackRepository = ServerPacksSource.createPackRepository(path, this.packValidator);
                this.tempDataPackRepository.reload();
            }

            this.tempDataPackRepository.setSelected(pWorldDataConfiguration.dataPacks().getEnabled());
            return Pair.of(path, this.tempDataPackRepository);
        } else {
            return null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    class GameTab extends GridLayoutTab {
        private static final Component TITLE = Component.translatable("createWorld.tab.game.title");
        private static final Component ALLOW_COMMANDS = Component.translatable("selectWorld.allowCommands");
        private final EditBox nameEdit;

        GameTab() {
            super(TITLE);
            GridLayout.RowHelper gridlayout$rowhelper = this.layout.rowSpacing(8).createRowHelper(1);
            LayoutSettings layoutsettings = gridlayout$rowhelper.newCellSettings();
            this.nameEdit = new EditBox(CreateWorldScreen.this.font, 208, 20, Component.translatable("selectWorld.enterName"));
            this.nameEdit.setValue(CreateWorldScreen.this.uiState.getName());
            this.nameEdit.setResponder(CreateWorldScreen.this.uiState::setName);
            CreateWorldScreen.this.uiState
                .addListener(
                    p_275871_ -> this.nameEdit
                            .setTooltip(
                                Tooltip.create(
                                    Component.translatable("selectWorld.targetFolder", Component.literal(p_275871_.getTargetFolder()).withStyle(ChatFormatting.ITALIC))
                                )
                            )
                );
            CreateWorldScreen.this.setInitialFocus(this.nameEdit);
            gridlayout$rowhelper.addChild(
                CommonLayouts.labeledElement(CreateWorldScreen.this.font, this.nameEdit, CreateWorldScreen.NAME_LABEL),
                gridlayout$rowhelper.newCellSettings().alignHorizontallyCenter()
            );
            CycleButton<WorldCreationUiState.SelectedGameMode> cyclebutton = gridlayout$rowhelper.addChild(
                CycleButton.<WorldCreationUiState.SelectedGameMode>builder(p_268080_ -> p_268080_.displayName)
                    .withValues(
                        WorldCreationUiState.SelectedGameMode.SURVIVAL,
                        WorldCreationUiState.SelectedGameMode.HARDCORE,
                        WorldCreationUiState.SelectedGameMode.CREATIVE
                    )
                    .create(0, 0, 210, 20, CreateWorldScreen.GAME_MODEL_LABEL, (p_268266_, p_268208_) -> CreateWorldScreen.this.uiState.setGameMode(p_268208_)),
                layoutsettings
            );
            CreateWorldScreen.this.uiState.addListener(p_280907_ -> {
                cyclebutton.setValue(p_280907_.getGameMode());
                cyclebutton.active = !p_280907_.isDebug();
                cyclebutton.setTooltip(Tooltip.create(p_280907_.getGameMode().getInfo()));
            });
            CycleButton<Difficulty> cyclebutton1 = gridlayout$rowhelper.addChild(
                CycleButton.builder(Difficulty::getDisplayName)
                    .withValues(Difficulty.values())
                    .create(
                        0,
                        0,
                        210,
                        20,
                        Component.translatable("options.difficulty"),
                        (p_267962_, p_268338_) -> CreateWorldScreen.this.uiState.setDifficulty(p_268338_)
                    ),
                layoutsettings
            );
            CreateWorldScreen.this.uiState.addListener(p_280905_ -> {
                cyclebutton1.setValue(CreateWorldScreen.this.uiState.getDifficulty());
                cyclebutton1.active = !CreateWorldScreen.this.uiState.isHardcore();
                cyclebutton1.setTooltip(Tooltip.create(CreateWorldScreen.this.uiState.getDifficulty().getInfo()));
            });
            CycleButton<Boolean> cyclebutton2 = gridlayout$rowhelper.addChild(
                CycleButton.onOffBuilder()
                    .withTooltip(p_325425_ -> Tooltip.create(CreateWorldScreen.ALLOW_COMMANDS_INFO))
                    .create(0, 0, 210, 20, ALLOW_COMMANDS, (p_325426_, p_325427_) -> CreateWorldScreen.this.uiState.setAllowCommands(p_325427_))
            );
            CreateWorldScreen.this.uiState.addListener(p_325429_ -> {
                cyclebutton2.setValue(CreateWorldScreen.this.uiState.isAllowCommands());
                cyclebutton2.active = !CreateWorldScreen.this.uiState.isDebug() && !CreateWorldScreen.this.uiState.isHardcore();
            });
            if (!SharedConstants.getCurrentVersion().isStable()) {
                gridlayout$rowhelper.addChild(
                    Button.builder(
                            CreateWorldScreen.EXPERIMENTS_LABEL,
                            p_269641_ -> CreateWorldScreen.this.openExperimentsScreen(CreateWorldScreen.this.uiState.getSettings().dataConfiguration())
                        )
                        .width(210)
                        .build()
                );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    class MoreTab extends GridLayoutTab {
        private static final Component TITLE = Component.translatable("createWorld.tab.more.title");
        private static final Component GAME_RULES_LABEL = Component.translatable("selectWorld.gameRules");
        private static final Component DATA_PACKS_LABEL = Component.translatable("selectWorld.dataPacks");

        MoreTab() {
            super(TITLE);
            GridLayout.RowHelper gridlayout$rowhelper = this.layout.rowSpacing(8).createRowHelper(1);
            gridlayout$rowhelper.addChild(Button.builder(GAME_RULES_LABEL, p_268028_ -> this.openGameRulesScreen()).width(210).build());
            gridlayout$rowhelper.addChild(
                Button.builder(
                        CreateWorldScreen.EXPERIMENTS_LABEL, p_269642_ -> CreateWorldScreen.this.openExperimentsScreen(CreateWorldScreen.this.uiState.getSettings().dataConfiguration())
                    )
                    .width(210)
                    .build()
            );
            gridlayout$rowhelper.addChild(
                Button.builder(DATA_PACKS_LABEL, p_268345_ -> CreateWorldScreen.this.openDataPackSelectionScreen(CreateWorldScreen.this.uiState.getSettings().dataConfiguration()))
                    .width(210)
                    .build()
            );
        }

        private void openGameRulesScreen() {
            CreateWorldScreen.this.minecraft
                .setScreen(
                    new EditGameRulesScreen(
                        CreateWorldScreen.this.uiState.getGameRules().copy(CreateWorldScreen.this.uiState.getSettings().dataConfiguration().enabledFeatures()),
                        p_268107_ -> {
                            CreateWorldScreen.this.minecraft.setScreen(CreateWorldScreen.this);
                            p_268107_.ifPresent(CreateWorldScreen.this.uiState::setGameRules);
                        }
                    )
                );
        }
    }

    @OnlyIn(Dist.CLIENT)
    class WorldTab extends GridLayoutTab {
        private static final Component TITLE = Component.translatable("createWorld.tab.world.title");
        private static final Component AMPLIFIED_HELP_TEXT = Component.translatable("generator.minecraft.amplified.info");
        private static final Component GENERATE_STRUCTURES = Component.translatable("selectWorld.mapFeatures");
        private static final Component GENERATE_STRUCTURES_INFO = Component.translatable("selectWorld.mapFeatures.info");
        private static final Component BONUS_CHEST = Component.translatable("selectWorld.bonusItems");
        private static final Component SEED_LABEL = Component.translatable("selectWorld.enterSeed");
        static final Component SEED_EMPTY_HINT = Component.translatable("selectWorld.seedInfo").withStyle(ChatFormatting.DARK_GRAY);
        private static final int WORLD_TAB_WIDTH = 310;
        private final EditBox seedEdit;
        private final Button customizeTypeButton;

        WorldTab() {
            super(TITLE);
            GridLayout.RowHelper gridlayout$rowhelper = this.layout.columnSpacing(10).rowSpacing(8).createRowHelper(2);
            CycleButton<WorldCreationUiState.WorldTypeEntry> cyclebutton = gridlayout$rowhelper.addChild(
                CycleButton.builder(WorldCreationUiState.WorldTypeEntry::describePreset)
                    .withValues(this.createWorldTypeValueSupplier())
                    .withCustomNarration(CreateWorldScreen.WorldTab::createTypeButtonNarration)
                    .create(
                        0,
                        0,
                        150,
                        20,
                        Component.translatable("selectWorld.mapType"),
                        (p_268242_, p_267954_) -> CreateWorldScreen.this.uiState.setWorldType(p_267954_)
                    )
            );
            cyclebutton.setValue(CreateWorldScreen.this.uiState.getWorldType());
            CreateWorldScreen.this.uiState.addListener(p_280909_ -> {
                WorldCreationUiState.WorldTypeEntry worldcreationuistate$worldtypeentry = p_280909_.getWorldType();
                cyclebutton.setValue(worldcreationuistate$worldtypeentry);
                if (worldcreationuistate$worldtypeentry.isAmplified()) {
                    cyclebutton.setTooltip(Tooltip.create(AMPLIFIED_HELP_TEXT));
                } else {
                    cyclebutton.setTooltip(null);
                }

                cyclebutton.active = CreateWorldScreen.this.uiState.getWorldType().preset() != null;
            });
            this.customizeTypeButton = gridlayout$rowhelper.addChild(
                Button.builder(Component.translatable("selectWorld.customizeType"), p_268355_ -> this.openPresetEditor()).build()
            );
            CreateWorldScreen.this.uiState.addListener(p_280910_ -> this.customizeTypeButton.active = !p_280910_.isDebug() && p_280910_.getPresetEditor() != null);
            this.seedEdit = new EditBox(CreateWorldScreen.this.font, 308, 20, Component.translatable("selectWorld.enterSeed")) {
                @Override
                protected MutableComponent createNarrationMessage() {
                    return super.createNarrationMessage().append(CommonComponents.NARRATION_SEPARATOR).append(CreateWorldScreen.WorldTab.SEED_EMPTY_HINT);
                }
            };
            this.seedEdit.setHint(SEED_EMPTY_HINT);
            this.seedEdit.setValue(CreateWorldScreen.this.uiState.getSeed());
            this.seedEdit.setResponder(p_268342_ -> CreateWorldScreen.this.uiState.setSeed(this.seedEdit.getValue()));
            gridlayout$rowhelper.addChild(CommonLayouts.labeledElement(CreateWorldScreen.this.font, this.seedEdit, SEED_LABEL), 2);
            SwitchGrid.Builder switchgrid$builder = SwitchGrid.builder(310);
            switchgrid$builder.addSwitch(GENERATE_STRUCTURES, CreateWorldScreen.this.uiState::isGenerateStructures, CreateWorldScreen.this.uiState::setGenerateStructures)
                .withIsActiveCondition(() -> !CreateWorldScreen.this.uiState.isDebug())
                .withInfo(GENERATE_STRUCTURES_INFO);
            switchgrid$builder.addSwitch(BONUS_CHEST, CreateWorldScreen.this.uiState::isBonusChest, CreateWorldScreen.this.uiState::setBonusChest)
                .withIsActiveCondition(() -> !CreateWorldScreen.this.uiState.isHardcore() && !CreateWorldScreen.this.uiState.isDebug());
            SwitchGrid switchgrid = switchgrid$builder.build();
            gridlayout$rowhelper.addChild(switchgrid.layout(), 2);
            CreateWorldScreen.this.uiState.addListener(p_268209_ -> switchgrid.refreshStates());
        }

        private void openPresetEditor() {
            PresetEditor preseteditor = CreateWorldScreen.this.uiState.getPresetEditor();
            if (preseteditor != null) {
                CreateWorldScreen.this.minecraft.setScreen(preseteditor.createEditScreen(CreateWorldScreen.this, CreateWorldScreen.this.uiState.getSettings()));
            }
        }

        private CycleButton.ValueListSupplier<WorldCreationUiState.WorldTypeEntry> createWorldTypeValueSupplier() {
            return new CycleButton.ValueListSupplier<WorldCreationUiState.WorldTypeEntry>() {
                @Override
                public List<WorldCreationUiState.WorldTypeEntry> getSelectedList() {
                    return CycleButton.DEFAULT_ALT_LIST_SELECTOR.getAsBoolean() ? CreateWorldScreen.this.uiState.getAltPresetList() : CreateWorldScreen.this.uiState.getNormalPresetList();
                }

                @Override
                public List<WorldCreationUiState.WorldTypeEntry> getDefaultList() {
                    return CreateWorldScreen.this.uiState.getNormalPresetList();
                }
            };
        }

        private static MutableComponent createTypeButtonNarration(CycleButton<WorldCreationUiState.WorldTypeEntry> pButton) {
            return pButton.getValue().isAmplified() ? CommonComponents.joinForNarration(pButton.createDefaultNarrationMessage(), AMPLIFIED_HELP_TEXT) : pButton.createDefaultNarrationMessage();
        }
    }
}