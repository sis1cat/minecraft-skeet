package net.minecraft.client.resources;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.BuiltInMetadata;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.VanillaPackResourcesBuilder;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientPackSource extends BuiltInPackSource {
    private static final PackMetadataSection VERSION_METADATA_SECTION = new PackMetadataSection(
        Component.translatable("resourcePack.vanilla.description"), SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES), Optional.empty()
    );
    private static final BuiltInMetadata BUILT_IN_METADATA = BuiltInMetadata.of(PackMetadataSection.TYPE, VERSION_METADATA_SECTION);
    public static final String HIGH_CONTRAST_PACK = "high_contrast";
    private static final Map<String, Component> SPECIAL_PACK_NAMES = Map.of(
        "programmer_art", Component.translatable("resourcePack.programmer_art.name"), "high_contrast", Component.translatable("resourcePack.high_contrast.name")
    );
    private static final PackLocationInfo VANILLA_PACK_INFO = new PackLocationInfo(
        "vanilla", Component.translatable("resourcePack.vanilla.name"), PackSource.BUILT_IN, Optional.of(CORE_PACK_INFO)
    );
    private static final PackSelectionConfig VANILLA_SELECTION_CONFIG = new PackSelectionConfig(true, Pack.Position.BOTTOM, false);
    private static final PackSelectionConfig BUILT_IN_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);
    private static final ResourceLocation PACKS_DIR = ResourceLocation.withDefaultNamespace("resourcepacks");
    @Nullable
    private final Path externalAssetDir;

    public ClientPackSource(Path pAssetIndex, DirectoryValidator pValidator) {
        super(PackType.CLIENT_RESOURCES, createVanillaPackSource(pAssetIndex), PACKS_DIR, pValidator);
        this.externalAssetDir = this.findExplodedAssetPacks(pAssetIndex);
    }

    private static PackLocationInfo createBuiltInPackLocation(String pId, Component pTitle) {
        return new PackLocationInfo(pId, pTitle, PackSource.BUILT_IN, Optional.of(KnownPack.vanilla(pId)));
    }

    @Nullable
    private Path findExplodedAssetPacks(Path pAssetIndex) {
        if (SharedConstants.IS_RUNNING_IN_IDE && pAssetIndex.getFileSystem() == FileSystems.getDefault()) {
            Path path = pAssetIndex.getParent().resolve("resourcepacks");
            if (Files.isDirectory(path)) {
                return path;
            }
        }

        return null;
    }

    private static VanillaPackResources createVanillaPackSource(Path pAssetIndex) {
        VanillaPackResourcesBuilder vanillapackresourcesbuilder = new VanillaPackResourcesBuilder().setMetadata(BUILT_IN_METADATA).exposeNamespace("minecraft", "realms");
        return vanillapackresourcesbuilder.applyDevelopmentConfig().pushJarResources().pushAssetPath(PackType.CLIENT_RESOURCES, pAssetIndex).build(VANILLA_PACK_INFO);
    }

    @Override
    protected Component getPackTitle(String p_250421_) {
        Component component = SPECIAL_PACK_NAMES.get(p_250421_);
        return (Component)(component != null ? component : Component.literal(p_250421_));
    }

    @Nullable
    @Override
    protected Pack createVanillaPack(PackResources p_250048_) {
        return Pack.readMetaAndCreate(VANILLA_PACK_INFO, fixedResources(p_250048_), PackType.CLIENT_RESOURCES, VANILLA_SELECTION_CONFIG);
    }

    @Nullable
    @Override
    protected Pack createBuiltinPack(String p_250992_, Pack.ResourcesSupplier p_250814_, Component p_249835_) {
        return Pack.readMetaAndCreate(createBuiltInPackLocation(p_250992_, p_249835_), p_250814_, PackType.CLIENT_RESOURCES, BUILT_IN_SELECTION_CONFIG);
    }

    @Override
    protected void populatePackList(BiConsumer<String, Function<String, Pack>> p_249851_) {
        super.populatePackList(p_249851_);
        if (this.externalAssetDir != null) {
            this.discoverPacksInPath(this.externalAssetDir, p_249851_);
        }
    }
}