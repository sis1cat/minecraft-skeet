package net.minecraft.world.level.chunk.status;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.VisibleForTesting;

public class ChunkStatus {
    public static final int MAX_STRUCTURE_DISTANCE = 8;
    private static final EnumSet<Heightmap.Types> WORLDGEN_HEIGHTMAPS = EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG);
    public static final EnumSet<Heightmap.Types> FINAL_HEIGHTMAPS = EnumSet.of(
        Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE, Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES
    );
    public static final ChunkStatus EMPTY = register("empty", null, WORLDGEN_HEIGHTMAPS, ChunkType.PROTOCHUNK);
    public static final ChunkStatus STRUCTURE_STARTS = register("structure_starts", EMPTY, WORLDGEN_HEIGHTMAPS, ChunkType.PROTOCHUNK);
    public static final ChunkStatus STRUCTURE_REFERENCES = register("structure_references", STRUCTURE_STARTS, WORLDGEN_HEIGHTMAPS, ChunkType.PROTOCHUNK);
    public static final ChunkStatus BIOMES = register("biomes", STRUCTURE_REFERENCES, WORLDGEN_HEIGHTMAPS, ChunkType.PROTOCHUNK);
    public static final ChunkStatus NOISE = register("noise", BIOMES, WORLDGEN_HEIGHTMAPS, ChunkType.PROTOCHUNK);
    public static final ChunkStatus SURFACE = register("surface", NOISE, WORLDGEN_HEIGHTMAPS, ChunkType.PROTOCHUNK);
    public static final ChunkStatus CARVERS = register("carvers", SURFACE, FINAL_HEIGHTMAPS, ChunkType.PROTOCHUNK);
    public static final ChunkStatus FEATURES = register("features", CARVERS, FINAL_HEIGHTMAPS, ChunkType.PROTOCHUNK);
    public static final ChunkStatus INITIALIZE_LIGHT = register("initialize_light", FEATURES, FINAL_HEIGHTMAPS, ChunkType.PROTOCHUNK);
    public static final ChunkStatus LIGHT = register("light", INITIALIZE_LIGHT, FINAL_HEIGHTMAPS, ChunkType.PROTOCHUNK);
    public static final ChunkStatus SPAWN = register("spawn", LIGHT, FINAL_HEIGHTMAPS, ChunkType.PROTOCHUNK);
    public static final ChunkStatus FULL = register("full", SPAWN, FINAL_HEIGHTMAPS, ChunkType.LEVELCHUNK);
    private final int index;
    private final ChunkStatus parent;
    private final ChunkType chunkType;
    private final EnumSet<Heightmap.Types> heightmapsAfter;

    private static ChunkStatus register(String pName, @Nullable ChunkStatus pParent, EnumSet<Heightmap.Types> pHeightmapsAfter, ChunkType pChunkType) {
        return Registry.register(BuiltInRegistries.CHUNK_STATUS, pName, new ChunkStatus(pParent, pHeightmapsAfter, pChunkType));
    }

    public static List<ChunkStatus> getStatusList() {
        List<ChunkStatus> list = Lists.newArrayList();

        ChunkStatus chunkstatus;
        for (chunkstatus = FULL; chunkstatus.getParent() != chunkstatus; chunkstatus = chunkstatus.getParent()) {
            list.add(chunkstatus);
        }

        list.add(chunkstatus);
        Collections.reverse(list);
        return list;
    }

    @VisibleForTesting
    protected ChunkStatus(@Nullable ChunkStatus pParent, EnumSet<Heightmap.Types> pHeightmapsAfter, ChunkType pChunkType) {
        this.parent = pParent == null ? this : pParent;
        this.chunkType = pChunkType;
        this.heightmapsAfter = pHeightmapsAfter;
        this.index = pParent == null ? 0 : pParent.getIndex() + 1;
    }

    public int getIndex() {
        return this.index;
    }

    public ChunkStatus getParent() {
        return this.parent;
    }

    public ChunkType getChunkType() {
        return this.chunkType;
    }

    public static ChunkStatus byName(String pName) {
        return BuiltInRegistries.CHUNK_STATUS.getValue(ResourceLocation.tryParse(pName));
    }

    public EnumSet<Heightmap.Types> heightmapsAfter() {
        return this.heightmapsAfter;
    }

    public boolean isOrAfter(ChunkStatus pChunkStatus) {
        return this.getIndex() >= pChunkStatus.getIndex();
    }

    public boolean isAfter(ChunkStatus pChunkStatus) {
        return this.getIndex() > pChunkStatus.getIndex();
    }

    public boolean isOrBefore(ChunkStatus pChunkStatus) {
        return this.getIndex() <= pChunkStatus.getIndex();
    }

    public boolean isBefore(ChunkStatus pChunkStatus) {
        return this.getIndex() < pChunkStatus.getIndex();
    }

    public static ChunkStatus max(ChunkStatus pFirst, ChunkStatus pSecond) {
        return pFirst.isAfter(pSecond) ? pFirst : pSecond;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public String getName() {
        return BuiltInRegistries.CHUNK_STATUS.getKey(this).toString();
    }
}