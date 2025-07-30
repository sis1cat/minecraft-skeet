package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class TextureSlots {
    public static final TextureSlots EMPTY = new TextureSlots(Map.of());
    private static final char REFERENCE_CHAR = '#';
    private final Map<String, Material> resolvedValues;

    public TextureSlots(Map<String, Material> pResolvedValues) {
        this.resolvedValues = pResolvedValues;
    }

    @Nullable
    public Material getMaterial(String pName) {
        if (isTextureReference(pName)) {
            pName = pName.substring(1);
        }

        return this.resolvedValues.get(pName);
    }

    private static boolean isTextureReference(String pName) {
        return pName.charAt(0) == '#';
    }

    public static TextureSlots.Data parseTextureMap(JsonObject pJson, ResourceLocation pAtlas) {
        TextureSlots.Data.Builder textureslots$data$builder = new TextureSlots.Data.Builder();

        for (Entry<String, JsonElement> entry : pJson.entrySet()) {
            parseEntry(pAtlas, entry.getKey(), entry.getValue().getAsString(), textureslots$data$builder);
        }

        return textureslots$data$builder.build();
    }

    private static void parseEntry(ResourceLocation pAtlas, String pName, String pMaterial, TextureSlots.Data.Builder pBuilder) {
        if (isTextureReference(pMaterial)) {
            pBuilder.addReference(pName, pMaterial.substring(1));
        } else {
            ResourceLocation resourcelocation = ResourceLocation.tryParse(pMaterial);
            if (resourcelocation == null) {
                throw new JsonParseException(pMaterial + " is not valid resource location");
            }

            pBuilder.addTexture(pName, new Material(pAtlas, resourcelocation));
        }
    }

    public Map<String, Material> getResolvedValues() {
        return this.resolvedValues;
    }

    @Override
    public String toString() {
        return this.resolvedValues + "";
    }

    public static record Data(Map<String, TextureSlots.SlotContents> values) {
        public static final TextureSlots.Data EMPTY = new TextureSlots.Data(Map.of());

        public static class Builder {
            private final Map<String, TextureSlots.SlotContents> textureMap = new HashMap<>();

            public TextureSlots.Data.Builder addReference(String pName, String pMaterial) {
                this.textureMap.put(pName, new TextureSlots.Reference(pMaterial));
                return this;
            }

            public TextureSlots.Data.Builder addTexture(String pName, Material pMaterial) {
                this.textureMap.put(pName, new TextureSlots.Value(pMaterial));
                return this;
            }

            public TextureSlots.Data build() {
                return this.textureMap.isEmpty() ? TextureSlots.Data.EMPTY : new TextureSlots.Data(Map.copyOf(this.textureMap));
            }
        }
    }

    static record Reference(String target) implements TextureSlots.SlotContents {
    }

    public static class Resolver {
        private static final Logger LOGGER = LogUtils.getLogger();
        private final List<TextureSlots.Data> entries = new ArrayList<>();

        public TextureSlots.Resolver addLast(TextureSlots.Data pData) {
            this.entries.addLast(pData);
            return this;
        }

        public TextureSlots.Resolver addFirst(TextureSlots.Data pData) {
            this.entries.addFirst(pData);
            return this;
        }

        public TextureSlots resolve(ModelDebugName pName) {
            if (this.entries.isEmpty()) {
                return TextureSlots.EMPTY;
            } else {
                Object2ObjectMap<String, Material> object2objectmap = new Object2ObjectArrayMap<>();
                Object2ObjectMap<String, TextureSlots.Reference> object2objectmap1 = new Object2ObjectArrayMap<>();

                for (TextureSlots.Data textureslots$data : Lists.reverse(this.entries)) {
                    textureslots$data.values.forEach((keyIn, slotIn) -> {
                        Objects.requireNonNull(slotIn);
                        Objects.requireNonNull(slotIn);
                        switch (slotIn) {
                            case TextureSlots.Value textureslots$value:
                                object2objectmap1.remove(keyIn);
                                object2objectmap.put(keyIn, textureslots$value.material());
                                break;
                            case TextureSlots.Reference textureslots$reference1:
                                object2objectmap.remove(keyIn);
                                object2objectmap1.put(keyIn, textureslots$reference1);
                                break;
                            default:
                                throw new MatchException(null, null);
                        }
                    });
                }

                if (object2objectmap1.isEmpty()) {
                    return new TextureSlots(object2objectmap);
                } else {
                    boolean flag = true;

                    while (flag) {
                        flag = false;
                        ObjectIterator<it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry<String, TextureSlots.Reference>> objectiterator = Object2ObjectMaps.fastIterator(
                            object2objectmap1
                        );

                        while (objectiterator.hasNext()) {
                            it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry<String, TextureSlots.Reference> entry = objectiterator.next();
                            Material material = object2objectmap.get(entry.getValue().target);
                            if (material != null) {
                                object2objectmap.put(entry.getKey(), material);
                                objectiterator.remove();
                                flag = true;
                            }
                        }
                    }

                    if (!object2objectmap1.isEmpty()) {
                        Map<String, Material> map = object2objectmap;
                        Map<String, TextureSlots.Reference> map1 = object2objectmap1;

                        for (String s : object2objectmap1.keySet()) {
                            TextureSlots.Reference textureslots$reference = map1.get(s);
                            if (!map.isEmpty() || map1.size() != 1 || !s.equals("particle") || !textureslots$reference.target.equals("layer0")) {
                                LOGGER.warn("Unresolved texture reference in {}, {}", pName.get(), "#" + s + " -> #" + textureslots$reference.target);
                            }
                        }
                    }

                    return new TextureSlots(object2objectmap);
                }
            }
        }
    }

    public sealed interface SlotContents permits TextureSlots.Value, TextureSlots.Reference {
    }

    static record Value(Material material) implements TextureSlots.SlotContents {
    }
}