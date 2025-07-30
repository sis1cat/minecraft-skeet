package net.minecraft.data;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public interface DataProvider {
    ToIntFunction<String> FIXED_ORDER_FIELDS = Util.make(new Object2IntOpenHashMap<>(), p_236070_ -> {
        p_236070_.put("type", 0);
        p_236070_.put("parent", 1);
        p_236070_.defaultReturnValue(2);
    });
    Comparator<String> KEY_COMPARATOR = Comparator.comparingInt(FIXED_ORDER_FIELDS).thenComparing(p_236077_ -> (String)p_236077_);
    Logger LOGGER = LogUtils.getLogger();

    CompletableFuture<?> run(CachedOutput pOutput);

    String getName();

    static <T> CompletableFuture<?> saveAll(CachedOutput pOutput, Codec<T> pCodec, PackOutput.PathProvider pPathProvider, Map<ResourceLocation, T> pEntries) {
        return saveAll(pOutput, pCodec, pPathProvider::json, pEntries);
    }

    static <T, E> CompletableFuture<?> saveAll(CachedOutput pOutput, Codec<E> pCodec, Function<T, Path> pPathGetter, Map<T, E> pEntries) {
        return saveAll(pOutput, p_374749_ -> pCodec.encodeStart(JsonOps.INSTANCE, (E)p_374749_).getOrThrow(), pPathGetter, pEntries);
    }

    static <T, E> CompletableFuture<?> saveAll(CachedOutput pOutput, Function<E, JsonElement> pSerializer, Function<T, Path> pPathGetter, Map<T, E> pEntries) {
        return CompletableFuture.allOf(pEntries.entrySet().stream().map(p_374753_ -> {
            Path path = pPathGetter.apply(p_374753_.getKey());
            JsonElement jsonelement = pSerializer.apply(p_374753_.getValue());
            return saveStable(pOutput, jsonelement, path);
        }).toArray(CompletableFuture[]::new));
    }

    static <T> CompletableFuture<?> saveStable(CachedOutput pOutput, HolderLookup.Provider pRegistries, Codec<T> pCodec, T pValue, Path pPath) {
        RegistryOps<JsonElement> registryops = pRegistries.createSerializationContext(JsonOps.INSTANCE);
        return saveStable(pOutput, registryops, pCodec, pValue, pPath);
    }

    static <T> CompletableFuture<?> saveStable(CachedOutput pOutput, Codec<T> pCodec, T pValue, Path pPath) {
        return saveStable(pOutput, JsonOps.INSTANCE, pCodec, pValue, pPath);
    }

    private static <T> CompletableFuture<?> saveStable(
        CachedOutput pOutput, DynamicOps<JsonElement> pOps, Codec<T> pCodec, T pValue, Path pPath
    ) {
        JsonElement jsonelement = pCodec.encodeStart(pOps, pValue).getOrThrow();
        return saveStable(pOutput, jsonelement, pPath);
    }

    static CompletableFuture<?> saveStable(CachedOutput pOutput, JsonElement pJson, Path pPath) {
        return CompletableFuture.runAsync(() -> {
            try {
                ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
                HashingOutputStream hashingoutputstream = new HashingOutputStream(Hashing.sha1(), bytearrayoutputstream);

                try (JsonWriter jsonwriter = new JsonWriter(new OutputStreamWriter(hashingoutputstream, StandardCharsets.UTF_8))) {
                    jsonwriter.setSerializeNulls(false);
                    jsonwriter.setIndent("  ");
                    GsonHelper.writeValue(jsonwriter, pJson, KEY_COMPARATOR);
                }

                pOutput.writeIfNeeded(pPath, bytearrayoutputstream.toByteArray(), hashingoutputstream.hash());
            } catch (IOException ioexception) {
                LOGGER.error("Failed to save file to {}", pPath, ioexception);
            }
        }, Util.backgroundExecutor().forName("saveStable"));
    }

    @FunctionalInterface
    public interface Factory<T extends DataProvider> {
        T create(PackOutput pOutput);
    }
}