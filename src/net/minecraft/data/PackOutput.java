package net.minecraft.data;

import java.nio.file.Path;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class PackOutput {
    private final Path outputFolder;

    public PackOutput(Path pOutputFolder) {
        this.outputFolder = pOutputFolder;
    }

    public Path getOutputFolder() {
        return this.outputFolder;
    }

    public Path getOutputFolder(PackOutput.Target pTarget) {
        return this.getOutputFolder().resolve(pTarget.directory);
    }

    public PackOutput.PathProvider createPathProvider(PackOutput.Target pTarget, String pKind) {
        return new PackOutput.PathProvider(this, pTarget, pKind);
    }

    public PackOutput.PathProvider createRegistryElementsPathProvider(ResourceKey<? extends Registry<?>> pRegistryKey) {
        return this.createPathProvider(PackOutput.Target.DATA_PACK, Registries.elementsDirPath(pRegistryKey));
    }

    public PackOutput.PathProvider createRegistryTagsPathProvider(ResourceKey<? extends Registry<?>> pRegistryKey) {
        return this.createPathProvider(PackOutput.Target.DATA_PACK, Registries.tagsDirPath(pRegistryKey));
    }

    public static class PathProvider {
        private final Path root;
        private final String kind;

        PathProvider(PackOutput pOutput, PackOutput.Target pTarget, String pKind) {
            this.root = pOutput.getOutputFolder(pTarget);
            this.kind = pKind;
        }

        public Path file(ResourceLocation pLocation, String pExtension) {
            return this.root.resolve(pLocation.getNamespace()).resolve(this.kind).resolve(pLocation.getPath() + "." + pExtension);
        }

        public Path json(ResourceLocation pLocation) {
            return this.root.resolve(pLocation.getNamespace()).resolve(this.kind).resolve(pLocation.getPath() + ".json");
        }

        public Path json(ResourceKey<?> pKey) {
            return this.root.resolve(pKey.location().getNamespace()).resolve(this.kind).resolve(pKey.location().getPath() + ".json");
        }
    }

    public static enum Target {
        DATA_PACK("data"),
        RESOURCE_PACK("assets"),
        REPORTS("reports");

        final String directory;

        private Target(final String pDirectory) {
            this.directory = pDirectory;
        }
    }
}