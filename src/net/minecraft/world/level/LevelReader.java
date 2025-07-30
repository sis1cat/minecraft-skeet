package net.minecraft.world.level;

import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public interface LevelReader extends BlockAndTintGetter, CollisionGetter, SignalGetter, BiomeManager.NoiseBiomeSource {
    @Nullable
    ChunkAccess getChunk(int pX, int pZ, ChunkStatus pChunkStatus, boolean pRequireChunk);

    @Deprecated
    boolean hasChunk(int pChunkX, int pChunkZ);

    int getHeight(Heightmap.Types pHeightmapType, int pX, int pZ);

    int getSkyDarken();

    BiomeManager getBiomeManager();

    default Holder<Biome> getBiome(BlockPos pPos) {
        return this.getBiomeManager().getBiome(pPos);
    }

    default Stream<BlockState> getBlockStatesIfLoaded(AABB pAabb) {
        int i = Mth.floor(pAabb.minX);
        int j = Mth.floor(pAabb.maxX);
        int k = Mth.floor(pAabb.minY);
        int l = Mth.floor(pAabb.maxY);
        int i1 = Mth.floor(pAabb.minZ);
        int j1 = Mth.floor(pAabb.maxZ);
        return this.hasChunksAt(i, k, i1, j, l, j1) ? this.getBlockStates(pAabb) : Stream.empty();
    }

    @Override
    default int getBlockTint(BlockPos pBlockPos, ColorResolver pColorResolver) {
        return pColorResolver.getColor(this.getBiome(pBlockPos).value(), (double)pBlockPos.getX(), (double)pBlockPos.getZ());
    }

    @Override
    default Holder<Biome> getNoiseBiome(int p_204163_, int p_204164_, int p_204165_) {
        ChunkAccess chunkaccess = this.getChunk(QuartPos.toSection(p_204163_), QuartPos.toSection(p_204165_), ChunkStatus.BIOMES, false);
        return chunkaccess != null ? chunkaccess.getNoiseBiome(p_204163_, p_204164_, p_204165_) : this.getUncachedNoiseBiome(p_204163_, p_204164_, p_204165_);
    }

    Holder<Biome> getUncachedNoiseBiome(int pX, int pY, int pZ);

    boolean isClientSide();

    int getSeaLevel();

    DimensionType dimensionType();

    @Override
    default int getMinY() {
        return this.dimensionType().minY();
    }

    @Override
    default int getHeight() {
        return this.dimensionType().height();
    }

    default BlockPos getHeightmapPos(Heightmap.Types pHeightmapType, BlockPos pPos) {
        return new BlockPos(pPos.getX(), this.getHeight(pHeightmapType, pPos.getX(), pPos.getZ()), pPos.getZ());
    }

    default boolean isEmptyBlock(BlockPos pPos) {
        return this.getBlockState(pPos).isAir();
    }

    default boolean canSeeSkyFromBelowWater(BlockPos pPos) {
        if (pPos.getY() >= this.getSeaLevel()) {
            return this.canSeeSky(pPos);
        } else {
            BlockPos blockpos = new BlockPos(pPos.getX(), this.getSeaLevel(), pPos.getZ());
            if (!this.canSeeSky(blockpos)) {
                return false;
            } else {
                for (BlockPos blockpos1 = blockpos.below(); blockpos1.getY() > pPos.getY(); blockpos1 = blockpos1.below()) {
                    BlockState blockstate = this.getBlockState(blockpos1);
                    if (blockstate.getLightBlock() > 0 && !blockstate.liquid()) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    default float getPathfindingCostFromLightLevels(BlockPos pPos) {
        return this.getLightLevelDependentMagicValue(pPos) - 0.5F;
    }

    @Deprecated
    default float getLightLevelDependentMagicValue(BlockPos pPos) {
        float f = (float)this.getMaxLocalRawBrightness(pPos) / 15.0F;
        float f1 = f / (4.0F - 3.0F * f);
        return Mth.lerp(this.dimensionType().ambientLight(), f1, 1.0F);
    }

    default ChunkAccess getChunk(BlockPos pPos) {
        return this.getChunk(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()));
    }

    default ChunkAccess getChunk(int pChunkX, int pChunkZ) {
        return this.getChunk(pChunkX, pChunkZ, ChunkStatus.FULL, true);
    }

    default ChunkAccess getChunk(int pChunkX, int pChunkZ, ChunkStatus pChunkStatus) {
        return this.getChunk(pChunkX, pChunkZ, pChunkStatus, true);
    }

    @Nullable
    @Override
    default BlockGetter getChunkForCollisions(int pChunkX, int pChunkZ) {
        return this.getChunk(pChunkX, pChunkZ, ChunkStatus.EMPTY, false);
    }

    default boolean isWaterAt(BlockPos pPos) {
        return this.getFluidState(pPos).is(FluidTags.WATER);
    }

    default boolean containsAnyLiquid(AABB pBb) {
        int i = Mth.floor(pBb.minX);
        int j = Mth.ceil(pBb.maxX);
        int k = Mth.floor(pBb.minY);
        int l = Mth.ceil(pBb.maxY);
        int i1 = Mth.floor(pBb.minZ);
        int j1 = Mth.ceil(pBb.maxZ);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int k1 = i; k1 < j; k1++) {
            for (int l1 = k; l1 < l; l1++) {
                for (int i2 = i1; i2 < j1; i2++) {
                    BlockState blockstate = this.getBlockState(blockpos$mutableblockpos.set(k1, l1, i2));
                    if (!blockstate.getFluidState().isEmpty()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    default int getMaxLocalRawBrightness(BlockPos pPos) {
        return this.getMaxLocalRawBrightness(pPos, this.getSkyDarken());
    }

    default int getMaxLocalRawBrightness(BlockPos pPos, int pAmount) {
        return pPos.getX() >= -30000000 && pPos.getZ() >= -30000000 && pPos.getX() < 30000000 && pPos.getZ() < 30000000
            ? this.getRawBrightness(pPos, pAmount)
            : 15;
    }

    @Deprecated
    default boolean hasChunkAt(int pX, int pZ) {
        return this.hasChunk(SectionPos.blockToSectionCoord(pX), SectionPos.blockToSectionCoord(pZ));
    }

    @Deprecated
    default boolean hasChunkAt(BlockPos pPos) {
        return this.hasChunkAt(pPos.getX(), pPos.getZ());
    }

    @Deprecated
    default boolean hasChunksAt(BlockPos pFrom, BlockPos pTo) {
        return this.hasChunksAt(pFrom.getX(), pFrom.getY(), pFrom.getZ(), pTo.getX(), pTo.getY(), pTo.getZ());
    }

    @Deprecated
    default boolean hasChunksAt(int pFromX, int pFromY, int pFromZ, int pToX, int pToY, int pToZ) {
        return pToY >= this.getMinY() && pFromY <= this.getMaxY() ? this.hasChunksAt(pFromX, pFromZ, pToX, pToZ) : false;
    }

    @Deprecated
    default boolean hasChunksAt(int pFromX, int pFromZ, int pToX, int pToZ) {
        int i = SectionPos.blockToSectionCoord(pFromX);
        int j = SectionPos.blockToSectionCoord(pToX);
        int k = SectionPos.blockToSectionCoord(pFromZ);
        int l = SectionPos.blockToSectionCoord(pToZ);

        for (int i1 = i; i1 <= j; i1++) {
            for (int j1 = k; j1 <= l; j1++) {
                if (!this.hasChunk(i1, j1)) {
                    return false;
                }
            }
        }

        return true;
    }

    RegistryAccess registryAccess();

    FeatureFlagSet enabledFeatures();

    default <T> HolderLookup<T> holderLookup(ResourceKey<? extends Registry<? extends T>> pRegistryKey) {
        Registry<T> registry = this.registryAccess().lookupOrThrow(pRegistryKey);
        return registry.filterFeatures(this.enabledFeatures());
    }
}