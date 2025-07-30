package net.minecraft.server.packs;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult.Error;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public abstract class AbstractPackResources implements PackResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackLocationInfo location;

    protected AbstractPackResources(PackLocationInfo pLocation) {
        this.location = pLocation;
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionType<T> p_375504_) throws IOException {
        IoSupplier<InputStream> iosupplier = this.getRootResource(new String[]{"pack.mcmeta"});
        if (iosupplier == null) {
            return null;
        } else {
            Object object;
            try (InputStream inputstream = iosupplier.get()) {
                object = getMetadataFromStream(p_375504_, inputstream);
            }

            return (T)object;
        }
    }

    @Nullable
    public static <T> T getMetadataFromStream(MetadataSectionType<T> pType, InputStream pStream) {
        JsonObject jsonobject;
        try (BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(pStream, StandardCharsets.UTF_8))) {
            jsonobject = GsonHelper.parse(bufferedreader);
        } catch (Exception exception) {
            LOGGER.error("Couldn't load {} metadata", pType.name(), exception);
            return null;
        }

        return !jsonobject.has(pType.name())
            ? null
            : pType.codec()
                .parse(JsonOps.INSTANCE, jsonobject.get(pType.name()))
                .ifError(p_377647_ -> LOGGER.error("Couldn't load {} metadata: {}", pType.name(), p_377647_))
                .result()
                .orElse(null);
    }

    @Override
    public PackLocationInfo location() {
        return this.location;
    }
}