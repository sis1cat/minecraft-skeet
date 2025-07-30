package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.datafixers.Products.P4;
import com.mojang.datafixers.Products.P5;
import com.mojang.datafixers.Products.P9;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

public class ConcentricRingsStructurePlacement extends StructurePlacement {
    public static final MapCodec<ConcentricRingsStructurePlacement> CODEC = RecordCodecBuilder.mapCodec(
        p_204960_ -> codec(p_204960_).apply(p_204960_, ConcentricRingsStructurePlacement::new)
    );
    private final int distance;
    private final int spread;
    private final int count;
    private final HolderSet<Biome> preferredBiomes;

    private static P9<Mu<ConcentricRingsStructurePlacement>, Vec3i, StructurePlacement.FrequencyReductionMethod, Float, Integer, Optional<StructurePlacement.ExclusionZone>, Integer, Integer, Integer, HolderSet<Biome>> codec(
        Instance<ConcentricRingsStructurePlacement> pInstance
    ) {
        P5<Mu<ConcentricRingsStructurePlacement>, Vec3i, StructurePlacement.FrequencyReductionMethod, Float, Integer, Optional<StructurePlacement.ExclusionZone>> p5 = placementCodec(
            pInstance
        );
        P4<Mu<ConcentricRingsStructurePlacement>, Integer, Integer, Integer, HolderSet<Biome>> p4 = pInstance.group(
            Codec.intRange(0, 1023).fieldOf("distance").forGetter(ConcentricRingsStructurePlacement::distance),
            Codec.intRange(0, 1023).fieldOf("spread").forGetter(ConcentricRingsStructurePlacement::spread),
            Codec.intRange(1, 4095).fieldOf("count").forGetter(ConcentricRingsStructurePlacement::count),
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("preferred_biomes").forGetter(ConcentricRingsStructurePlacement::preferredBiomes)
        );
        return new P9<>(p5.t1(), p5.t2(), p5.t3(), p5.t4(), p5.t5(), p4.t1(), p4.t2(), p4.t3(), p4.t4());
    }

    public ConcentricRingsStructurePlacement(
        Vec3i pLocateOffset,
        StructurePlacement.FrequencyReductionMethod pFrequencyReductionMethod,
        float pFrequency,
        int pSalt,
        Optional<StructurePlacement.ExclusionZone> pExclusionZone,
        int pDistance,
        int pSpread,
        int pCount,
        HolderSet<Biome> pPreferredBiomes
    ) {
        super(pLocateOffset, pFrequencyReductionMethod, pFrequency, pSalt, pExclusionZone);
        this.distance = pDistance;
        this.spread = pSpread;
        this.count = pCount;
        this.preferredBiomes = pPreferredBiomes;
    }

    public ConcentricRingsStructurePlacement(int pDistance, int pSpread, int pCount, HolderSet<Biome> pPreferrredBiomes) {
        this(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, 0, Optional.empty(), pDistance, pSpread, pCount, pPreferrredBiomes);
    }

    public int distance() {
        return this.distance;
    }

    public int spread() {
        return this.spread;
    }

    public int count() {
        return this.count;
    }

    public HolderSet<Biome> preferredBiomes() {
        return this.preferredBiomes;
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState p_256631_, int p_256202_, int p_255915_) {
        List<ChunkPos> list = p_256631_.getRingPositionsFor(this);
        return list == null ? false : list.contains(new ChunkPos(p_256202_, p_255915_));
    }

    @Override
    public StructurePlacementType<?> type() {
        return StructurePlacementType.CONCENTRIC_RINGS;
    }
}