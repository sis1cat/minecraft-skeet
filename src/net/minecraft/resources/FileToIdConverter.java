package net.minecraft.resources;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.optifine.util.StrUtils;

public class FileToIdConverter {
    private final String prefix;
    private final String extension;

    public FileToIdConverter(String pPrefix, String pExtenstion) {
        this.prefix = pPrefix;
        this.extension = pExtenstion;
    }

    public static FileToIdConverter json(String pName) {
        return new FileToIdConverter(pName, ".json");
    }

    public static FileToIdConverter registry(ResourceKey<? extends Registry<?>> pRegistryKey) {
        return json(Registries.elementsDirPath(pRegistryKey));
    }

    public ResourceLocation idToFile(ResourceLocation pId) {
        return pId.withPath(this.prefix + "/" + pId.getPath() + this.extension);
    }

    public ResourceLocation fileToId(ResourceLocation pFile) {
        if (!pFile.getPath().startsWith(this.prefix)) {
            return pFile.withPath(StrUtils.removeSuffix(pFile.getPath(), this.extension));
        } else {
            String s = pFile.getPath();
            return pFile.withPath(s.substring(this.prefix.length() + 1, s.length() - this.extension.length()));
        }
    }

    public Map<ResourceLocation, Resource> listMatchingResources(ResourceManager pResourceManager) {
        return pResourceManager.listResources(this.prefix, locIn -> locIn.getPath().endsWith(this.extension));
    }

    public Map<ResourceLocation, List<Resource>> listMatchingResourceStacks(ResourceManager pResourceManager) {
        return pResourceManager.listResourceStacks(this.prefix, locIn -> locIn.getPath().endsWith(this.extension));
    }

    public boolean matches(ResourceLocation loc) {
        String s = loc.getPath();
        return s.startsWith(this.prefix) && s.endsWith(this.extension);
    }
}