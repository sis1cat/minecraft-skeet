package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.chunk.StructureAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public class StructureManager {
    private final LevelAccessor level;
    private final WorldOptions worldOptions;
    private final StructureCheck structureCheck;

    public StructureManager(LevelAccessor pLevel, WorldOptions pWorldOptions, StructureCheck pStructureCheck) {
        this.level = pLevel;
        this.worldOptions = pWorldOptions;
        this.structureCheck = pStructureCheck;
    }

    public StructureManager forWorldGenRegion(WorldGenRegion pRegion) {
        if (pRegion.getLevel() != this.level) {
            throw new IllegalStateException("Using invalid structure manager (source level: " + pRegion.getLevel() + ", region: " + pRegion);
        } else {
            return new StructureManager(pRegion, this.worldOptions, this.structureCheck);
        }
    }

    public List<StructureStart> startsForStructure(ChunkPos pChunkPos, Predicate<Structure> pStructurePredicate) {
        Map<Structure, LongSet> map = this.level.getChunk(pChunkPos.x, pChunkPos.z, ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
        Builder<StructureStart> builder = ImmutableList.builder();

        for (Entry<Structure, LongSet> entry : map.entrySet()) {
            Structure structure = entry.getKey();
            if (pStructurePredicate.test(structure)) {
                this.fillStartsForStructure(structure, entry.getValue(), builder::add);
            }
        }

        return builder.build();
    }

    public List<StructureStart> startsForStructure(SectionPos pSectionPos, Structure pStructure) {
        LongSet longset = this.level.getChunk(pSectionPos.x(), pSectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES).getReferencesForStructure(pStructure);
        Builder<StructureStart> builder = ImmutableList.builder();
        this.fillStartsForStructure(pStructure, longset, builder::add);
        return builder.build();
    }

    public void fillStartsForStructure(Structure pStructure, LongSet pStructureRefs, Consumer<StructureStart> pStartConsumer) {
        for (long i : pStructureRefs) {
            SectionPos sectionpos = SectionPos.of(new ChunkPos(i), this.level.getMinSectionY());
            StructureStart structurestart = this.getStartForStructure(
                sectionpos, pStructure, this.level.getChunk(sectionpos.x(), sectionpos.z(), ChunkStatus.STRUCTURE_STARTS)
            );
            if (structurestart != null && structurestart.isValid()) {
                pStartConsumer.accept(structurestart);
            }
        }
    }

    @Nullable
    public StructureStart getStartForStructure(SectionPos pSectionPos, Structure pStructure, StructureAccess pStructureAccess) {
        return pStructureAccess.getStartForStructure(pStructure);
    }

    public void setStartForStructure(SectionPos pSectionPos, Structure pStructure, StructureStart pStructureStart, StructureAccess pStructureAccess) {
        pStructureAccess.setStartForStructure(pStructure, pStructureStart);
    }

    public void addReferenceForStructure(SectionPos pSectionPos, Structure pStructure, long pReference, StructureAccess pStructureAccess) {
        pStructureAccess.addReferenceForStructure(pStructure, pReference);
    }

    public boolean shouldGenerateStructures() {
        return this.worldOptions.generateStructures();
    }

    public StructureStart getStructureAt(BlockPos pPos, Structure pStructure) {
        for (StructureStart structurestart : this.startsForStructure(SectionPos.of(pPos), pStructure)) {
            if (structurestart.getBoundingBox().isInside(pPos)) {
                return structurestart;
            }
        }

        return StructureStart.INVALID_START;
    }

    public StructureStart getStructureWithPieceAt(BlockPos pPos, TagKey<Structure> pStructureTag) {
        return this.getStructureWithPieceAt(pPos, p_327244_ -> p_327244_.is(pStructureTag));
    }

    public StructureStart getStructureWithPieceAt(BlockPos pPos, HolderSet<Structure> pStructures) {
        return this.getStructureWithPieceAt(pPos, pStructures::contains);
    }

    public StructureStart getStructureWithPieceAt(BlockPos pPos, Predicate<Holder<Structure>> pPredicate) {
        Registry<Structure> registry = this.registryAccess().lookupOrThrow(Registries.STRUCTURE);

        for (StructureStart structurestart : this.startsForStructure(
            new ChunkPos(pPos), p_359959_ -> registry.get(registry.getId(p_359959_)).map(pPredicate::test).orElse(false)
        )) {
            if (this.structureHasPieceAt(pPos, structurestart)) {
                return structurestart;
            }
        }

        return StructureStart.INVALID_START;
    }

    public StructureStart getStructureWithPieceAt(BlockPos pPos, Structure pStructure) {
        for (StructureStart structurestart : this.startsForStructure(SectionPos.of(pPos), pStructure)) {
            if (this.structureHasPieceAt(pPos, structurestart)) {
                return structurestart;
            }
        }

        return StructureStart.INVALID_START;
    }

    public boolean structureHasPieceAt(BlockPos pPos, StructureStart pStructureStart) {
        for (StructurePiece structurepiece : pStructureStart.getPieces()) {
            if (structurepiece.getBoundingBox().isInside(pPos)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasAnyStructureAt(BlockPos pPos) {
        SectionPos sectionpos = SectionPos.of(pPos);
        return this.level.getChunk(sectionpos.x(), sectionpos.z(), ChunkStatus.STRUCTURE_REFERENCES).hasAnyStructureReferences();
    }

    public Map<Structure, LongSet> getAllStructuresAt(BlockPos pPos) {
        SectionPos sectionpos = SectionPos.of(pPos);
        return this.level.getChunk(sectionpos.x(), sectionpos.z(), ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
    }

    public StructureCheckResult checkStructurePresence(ChunkPos pChunkPos, Structure pStructure, StructurePlacement pPlacement, boolean pSkipKnownStructures) {
        return this.structureCheck.checkStart(pChunkPos, pStructure, pPlacement, pSkipKnownStructures);
    }

    public void addReference(StructureStart pStructureStart) {
        pStructureStart.addReference();
        this.structureCheck.incrementReference(pStructureStart.getChunkPos(), pStructureStart.getStructure());
    }

    public RegistryAccess registryAccess() {
        return this.level.registryAccess();
    }
}