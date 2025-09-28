package net.minecraftforge.resource;

import java.io.InputStream;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;

public class DelegatingPackResources extends AbstractPackResources {
    public DelegatingPackResources(PackLocationInfo info) {
        super(info);
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... pathIn) {
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation namespaceIn) {
        return null;
    }

    @Override
    public void listResources(PackType typeIn, String namespaceIn, String pathIn, PackResources.ResourceOutput outputIn) {
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return null;
    }

    @Override
    public void close() {
    }
}