package net.optifine.util;

import com.google.gson.JsonObject;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;

public class MetadataUtils {
    public static TextureMetadataSection parseTextureMetadataSection(JsonObject json) {
        boolean flag = Json.getBoolean(json, "blur", false);
        boolean flag1 = Json.getBoolean(json, "clamp", false);
        return new TextureMetadataSection(flag, flag1);
    }
}