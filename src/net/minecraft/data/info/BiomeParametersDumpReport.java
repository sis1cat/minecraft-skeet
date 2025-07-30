package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import org.slf4j.Logger;

public class BiomeParametersDumpReport implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path topPath;
    private final CompletableFuture<HolderLookup.Provider> registries;
    private static final MapCodec<ResourceKey<Biome>> ENTRY_CODEC = ResourceKey.codec(Registries.BIOME).fieldOf("biome");
    private static final Codec<Climate.ParameterList<ResourceKey<Biome>>> CODEC = Climate.ParameterList.codec(ENTRY_CODEC).fieldOf("biomes").codec();

    public BiomeParametersDumpReport(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries) {
        this.topPath = pOutput.getOutputFolder(PackOutput.Target.REPORTS).resolve("biome_parameters");
        this.registries = pRegistries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput p_254091_) {
        return this.registries
            .thenCompose(
                p_325848_ -> {
                    DynamicOps<JsonElement> dynamicops = p_325848_.createSerializationContext(JsonOps.INSTANCE);
                    List<CompletableFuture<?>> list = new ArrayList<>();
                    MultiNoiseBiomeSourceParameterList.knownPresets()
                        .forEach(
                            (p_274759_, p_274760_) -> list.add(
                                    dumpValue(
                                        this.createPath(p_274759_.id()),
                                        p_254091_,
                                        dynamicops,
                                        CODEC,
                                        (Climate.ParameterList<ResourceKey<Biome>>)p_274760_
                                    )
                                )
                        );
                    return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
                }
            );
    }

    private static <E> CompletableFuture<?> dumpValue(
        Path pPath, CachedOutput pOutput, DynamicOps<JsonElement> pOps, Encoder<E> pEncoder, E pValue
    ) {
        Optional<JsonElement> optional = pEncoder.encodeStart(pOps, pValue)
            .resultOrPartial(p_236195_ -> LOGGER.error("Couldn't serialize element {}: {}", pPath, p_236195_));
        return optional.isPresent() ? DataProvider.saveStable(pOutput, optional.get(), pPath) : CompletableFuture.completedFuture(null);
    }

    private Path createPath(ResourceLocation pLocation) {
        return this.topPath.resolve(pLocation.getNamespace()).resolve(pLocation.getPath() + ".json");
    }

    @Override
    public final String getName() {
        return "Biome Parameters";
    }
}