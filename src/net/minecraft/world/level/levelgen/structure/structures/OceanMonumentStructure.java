package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class OceanMonumentStructure extends Structure {
    public static final MapCodec<OceanMonumentStructure> CODEC = simpleCodec(OceanMonumentStructure::new);

    public OceanMonumentStructure(Structure.StructureSettings p_228955_) {
        super(p_228955_);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_228964_) {
        int i = p_228964_.chunkPos().getBlockX(9);
        int j = p_228964_.chunkPos().getBlockZ(9);

        for (Holder<Biome> holder : p_228964_.biomeSource().getBiomesWithin(i, p_228964_.chunkGenerator().getSeaLevel(), j, 29, p_228964_.randomState().sampler())) {
            if (!holder.is(BiomeTags.REQUIRED_OCEAN_MONUMENT_SURROUNDING)) {
                return Optional.empty();
            }
        }

        return onTopOfChunkCenter(p_228964_, Heightmap.Types.OCEAN_FLOOR_WG, p_228967_ -> generatePieces(p_228967_, p_228964_));
    }

    private static StructurePiece createTopPiece(ChunkPos pChunkPos, WorldgenRandom pRandom) {
        int i = pChunkPos.getMinBlockX() - 29;
        int j = pChunkPos.getMinBlockZ() - 29;
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(pRandom);
        return new OceanMonumentPieces.MonumentBuilding(pRandom, i, j, direction);
    }

    private static void generatePieces(StructurePiecesBuilder pBuilder, Structure.GenerationContext pContext) {
        pBuilder.addPiece(createTopPiece(pContext.chunkPos(), pContext.random()));
    }

    public static PiecesContainer regeneratePiecesAfterLoad(ChunkPos pChunkPos, long pSeed, PiecesContainer pPiecesContainer) {
        if (pPiecesContainer.isEmpty()) {
            return pPiecesContainer;
        } else {
            WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
            worldgenrandom.setLargeFeatureSeed(pSeed, pChunkPos.x, pChunkPos.z);
            StructurePiece structurepiece = pPiecesContainer.pieces().get(0);
            BoundingBox boundingbox = structurepiece.getBoundingBox();
            int i = boundingbox.minX();
            int j = boundingbox.minZ();
            Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(worldgenrandom);
            Direction direction1 = Objects.requireNonNullElse(structurepiece.getOrientation(), direction);
            StructurePiece structurepiece1 = new OceanMonumentPieces.MonumentBuilding(worldgenrandom, i, j, direction1);
            StructurePiecesBuilder structurepiecesbuilder = new StructurePiecesBuilder();
            structurepiecesbuilder.addPiece(structurepiece1);
            return structurepiecesbuilder.build();
        }
    }

    @Override
    public StructureType<?> type() {
        return StructureType.OCEAN_MONUMENT;
    }
}