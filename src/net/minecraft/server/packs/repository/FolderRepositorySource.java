package net.minecraft.server.packs.repository;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.linkfs.LinkFileSystem;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.slf4j.Logger;

public class FolderRepositorySource implements RepositorySource {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final PackSelectionConfig DISCOVERED_PACK_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);
    private final Path folder;
    private final PackType packType;
    private final PackSource packSource;
    private final DirectoryValidator validator;

    public FolderRepositorySource(Path pFolder, PackType pPackType, PackSource pPackSource, DirectoryValidator pValidator) {
        this.folder = pFolder;
        this.packType = pPackType;
        this.packSource = pPackSource;
        this.validator = pValidator;
    }

    private static String nameFromPath(Path pPath) {
        return pPath.getFileName().toString();
    }

    @Override
    public void loadPacks(Consumer<Pack> p_250965_) {
        try {
            FileUtil.createDirectoriesSafe(this.folder);
            discoverPacks(this.folder, this.validator, (p_326474_, p_326475_) -> {
                PackLocationInfo packlocationinfo = this.createDiscoveredFilePackInfo(p_326474_);
                Pack pack = Pack.readMetaAndCreate(packlocationinfo, p_326475_, this.packType, DISCOVERED_PACK_SELECTION_CONFIG);
                if (pack != null) {
                    p_250965_.accept(pack);
                }
            });
        } catch (IOException ioexception) {
            LOGGER.warn("Failed to list packs in {}", this.folder, ioexception);
        }
    }

    private PackLocationInfo createDiscoveredFilePackInfo(Path pPath) {
        String s = nameFromPath(pPath);
        return new PackLocationInfo("file/" + s, Component.literal(s), this.packSource, Optional.empty());
    }

    public static void discoverPacks(Path pFolder, DirectoryValidator pValidator, BiConsumer<Path, Pack.ResourcesSupplier> pOutput) throws IOException {
        FolderRepositorySource.FolderPackDetector folderrepositorysource$folderpackdetector = new FolderRepositorySource.FolderPackDetector(pValidator);

        try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(pFolder)) {
            for (Path path : directorystream) {
                try {
                    List<ForbiddenSymlinkInfo> list = new ArrayList<>();
                    Pack.ResourcesSupplier pack$resourcessupplier = folderrepositorysource$folderpackdetector.detectPackResources(path, list);
                    if (!list.isEmpty()) {
                        LOGGER.warn("Ignoring potential pack entry: {}", ContentValidationException.getMessage(path, list));
                    } else if (pack$resourcessupplier != null) {
                        pOutput.accept(path, pack$resourcessupplier);
                    } else {
                        LOGGER.info("Found non-pack entry '{}', ignoring", path);
                    }
                } catch (IOException ioexception) {
                    LOGGER.warn("Failed to read properties of '{}', ignoring", path, ioexception);
                }
            }
        }
    }

    static class FolderPackDetector extends PackDetector<Pack.ResourcesSupplier> {
        protected FolderPackDetector(DirectoryValidator p_301187_) {
            super(p_301187_);
        }

        @Nullable
        protected Pack.ResourcesSupplier createZipPack(Path p_299114_) {
            FileSystem filesystem = p_299114_.getFileSystem();
            if (filesystem != FileSystems.getDefault() && !(filesystem instanceof LinkFileSystem)) {
                FolderRepositorySource.LOGGER.info("Can't open pack archive at {}", p_299114_);
                return null;
            } else {
                return new FilePackResources.FileResourcesSupplier(p_299114_);
            }
        }

        protected Pack.ResourcesSupplier createDirectoryPack(Path p_300765_) {
            return new PathPackResources.PathResourcesSupplier(p_300765_);
        }
    }
}