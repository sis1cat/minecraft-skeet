package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.text.DecimalFormat;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import org.apache.commons.lang3.mutable.MutableObject;

public final class NoiseBasedChunkGenerator extends ChunkGenerator {
    public static final MapCodec<NoiseBasedChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
        p_255585_ -> p_255585_.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(p_255584_ -> p_255584_.biomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(p_224278_ -> p_224278_.settings)
                )
                .apply(p_255585_, p_255585_.stable(NoiseBasedChunkGenerator::new))
    );
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private final Holder<NoiseGeneratorSettings> settings;
    private final Supplier<Aquifer.FluidPicker> globalFluidPicker;

    public NoiseBasedChunkGenerator(BiomeSource pBiomeSource, Holder<NoiseGeneratorSettings> pSettings) {
        super(pBiomeSource);
        this.settings = pSettings;
        this.globalFluidPicker = Suppliers.memoize(() -> createFluidPicker(pSettings.value()));
    }

    private static Aquifer.FluidPicker createFluidPicker(NoiseGeneratorSettings pSettings) {
        Aquifer.FluidStatus aquifer$fluidstatus = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        int i = pSettings.seaLevel();
        Aquifer.FluidStatus aquifer$fluidstatus1 = new Aquifer.FluidStatus(i, pSettings.defaultFluid());
        Aquifer.FluidStatus aquifer$fluidstatus2 = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());
        return (p_224274_, p_224275_, p_224276_) -> p_224275_ < Math.min(-54, i) ? aquifer$fluidstatus : aquifer$fluidstatus1;
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(RandomState p_224299_, Blender p_224300_, StructureManager p_224301_, ChunkAccess p_224302_) {
        return CompletableFuture.supplyAsync(() -> {
            this.doCreateBiomes(p_224300_, p_224299_, p_224301_, p_224302_);
            return p_224302_;
        }, Util.backgroundExecutor().forName("init_biomes"));
    }

    private void doCreateBiomes(Blender pBlender, RandomState pRandom, StructureManager pStructureManager, ChunkAccess pChunk) {
        NoiseChunk noisechunk = pChunk.getOrCreateNoiseChunk(p_224340_ -> this.createNoiseChunk(p_224340_, pStructureManager, pBlender, pRandom));
        BiomeResolver biomeresolver = BelowZeroRetrogen.getBiomeResolver(pBlender.getBiomeResolver(this.biomeSource), pChunk);
        pChunk.fillBiomesFromNoise(biomeresolver, noisechunk.cachedClimateSampler(pRandom.router(), this.settings.value().spawnTarget()));
    }

    private NoiseChunk createNoiseChunk(ChunkAccess pChunk, StructureManager pStructureManager, Blender pBlender, RandomState pRandom) {
        return NoiseChunk.forChunk(
            pChunk, pRandom, Beardifier.forStructuresInChunk(pStructureManager, pChunk.getPos()), this.settings.value(), this.globalFluidPicker.get(), pBlender
        );
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    public Holder<NoiseGeneratorSettings> generatorSettings() {
        return this.settings;
    }

    public boolean stable(ResourceKey<NoiseGeneratorSettings> pSettings) {
        return this.settings.is(pSettings);
    }

    @Override
    public int getBaseHeight(int p_224216_, int p_224217_, Heightmap.Types p_224218_, LevelHeightAccessor p_224219_, RandomState p_224220_) {
        return this.iterateNoiseColumn(p_224219_, p_224220_, p_224216_, p_224217_, null, p_224218_.isOpaque()).orElse(p_224219_.getMinY());
    }

    @Override
    public NoiseColumn getBaseColumn(int p_224211_, int p_224212_, LevelHeightAccessor p_224213_, RandomState p_224214_) {
        MutableObject<NoiseColumn> mutableobject = new MutableObject<>();
        this.iterateNoiseColumn(p_224213_, p_224214_, p_224211_, p_224212_, mutableobject, null);
        return mutableobject.getValue();
    }

    @Override
    public void addDebugScreenInfo(List<String> p_224304_, RandomState p_224305_, BlockPos p_224306_) {
        DecimalFormat decimalformat = new DecimalFormat("0.000");
        NoiseRouter noiserouter = p_224305_.router();
        DensityFunction.SinglePointContext densityfunction$singlepointcontext = new DensityFunction.SinglePointContext(
            p_224306_.getX(), p_224306_.getY(), p_224306_.getZ()
        );
        double d0 = noiserouter.ridges().compute(densityfunction$singlepointcontext);
        p_224304_.add(
            "NoiseRouter T: "
                + decimalformat.format(noiserouter.temperature().compute(densityfunction$singlepointcontext))
                + " V: "
                + decimalformat.format(noiserouter.vegetation().compute(densityfunction$singlepointcontext))
                + " C: "
                + decimalformat.format(noiserouter.continents().compute(densityfunction$singlepointcontext))
                + " E: "
                + decimalformat.format(noiserouter.erosion().compute(densityfunction$singlepointcontext))
                + " D: "
                + decimalformat.format(noiserouter.depth().compute(densityfunction$singlepointcontext))
                + " W: "
                + decimalformat.format(d0)
                + " PV: "
                + decimalformat.format((double)NoiseRouterData.peaksAndValleys((float)d0))
                + " AS: "
                + decimalformat.format(noiserouter.initialDensityWithoutJaggedness().compute(densityfunction$singlepointcontext))
                + " N: "
                + decimalformat.format(noiserouter.finalDensity().compute(densityfunction$singlepointcontext))
        );
    }

    private OptionalInt iterateNoiseColumn(
        LevelHeightAccessor pLevel,
        RandomState pRandom,
        int pX,
        int pZ,
        @Nullable MutableObject<NoiseColumn> pColumn,
        @Nullable Predicate<BlockState> pStoppingState
    ) {
        NoiseSettings noisesettings = this.settings.value().noiseSettings().clampToHeightAccessor(pLevel);
        int i = noisesettings.getCellHeight();
        int j = noisesettings.minY();
        int k = Mth.floorDiv(j, i);
        int l = Mth.floorDiv(noisesettings.height(), i);
        if (l <= 0) {
            return OptionalInt.empty();
        } else {
            BlockState[] ablockstate;
            if (pColumn == null) {
                ablockstate = null;
            } else {
                ablockstate = new BlockState[noisesettings.height()];
                pColumn.setValue(new NoiseColumn(j, ablockstate));
            }

            int i1 = noisesettings.getCellWidth();
            int j1 = Math.floorDiv(pX, i1);
            int k1 = Math.floorDiv(pZ, i1);
            int l1 = Math.floorMod(pX, i1);
            int i2 = Math.floorMod(pZ, i1);
            int j2 = j1 * i1;
            int k2 = k1 * i1;
            double d0 = (double)l1 / (double)i1;
            double d1 = (double)i2 / (double)i1;
            NoiseChunk noisechunk = new NoiseChunk(
                1,
                pRandom,
                j2,
                k2,
                noisesettings,
                DensityFunctions.BeardifierMarker.INSTANCE,
                this.settings.value(),
                this.globalFluidPicker.get(),
                Blender.empty()
            );
            noisechunk.initializeForFirstCellX();
            noisechunk.advanceCellX(0);

            for (int l2 = l - 1; l2 >= 0; l2--) {
                noisechunk.selectCellYZ(l2, 0);

                for (int i3 = i - 1; i3 >= 0; i3--) {
                    int j3 = (k + l2) * i + i3;
                    double d2 = (double)i3 / (double)i;
                    noisechunk.updateForY(j3, d2);
                    noisechunk.updateForX(pX, d0);
                    noisechunk.updateForZ(pZ, d1);
                    BlockState blockstate = noisechunk.getInterpolatedState();
                    BlockState blockstate1 = blockstate == null ? this.settings.value().defaultBlock() : blockstate;
                    if (ablockstate != null) {
                        int k3 = l2 * i + i3;
                        ablockstate[k3] = blockstate1;
                    }

                    if (pStoppingState != null && pStoppingState.test(blockstate1)) {
                        noisechunk.stopInterpolation();
                        return OptionalInt.of(j3 + 1);
                    }
                }
            }

            noisechunk.stopInterpolation();
            return OptionalInt.empty();
        }
    }

    @Override
    public void buildSurface(WorldGenRegion p_224232_, StructureManager p_224233_, RandomState p_224234_, ChunkAccess p_224235_) {
        if (!SharedConstants.debugVoidTerrain(p_224235_.getPos())) {
            WorldGenerationContext worldgenerationcontext = new WorldGenerationContext(this, p_224232_);
            this.buildSurface(
                p_224235_,
                worldgenerationcontext,
                p_224234_,
                p_224233_,
                p_224232_.getBiomeManager(),
                p_224232_.registryAccess().lookupOrThrow(Registries.BIOME),
                Blender.of(p_224232_)
            );
        }
    }

    @VisibleForTesting
    public void buildSurface(
        ChunkAccess pChunk,
        WorldGenerationContext pContext,
        RandomState pRandom,
        StructureManager pStructureManager,
        BiomeManager pBiomeManager,
        Registry<Biome> pBiomes,
        Blender pBlender
    ) {
        NoiseChunk noisechunk = pChunk.getOrCreateNoiseChunk(p_224321_ -> this.createNoiseChunk(p_224321_, pStructureManager, pBlender, pRandom));
        NoiseGeneratorSettings noisegeneratorsettings = this.settings.value();
        pRandom.surfaceSystem()
            .buildSurface(
                pRandom, pBiomeManager, pBiomes, noisegeneratorsettings.useLegacyRandomSource(), pContext, pChunk, noisechunk, noisegeneratorsettings.surfaceRule()
            );
    }

    @Override
    public void applyCarvers(
        WorldGenRegion p_224224_, long p_224225_, RandomState p_224226_, BiomeManager p_224227_, StructureManager p_224228_, ChunkAccess p_224229_
    ) {
        BiomeManager biomemanager = p_224227_.withDifferentSource(
            (p_255581_, p_255582_, p_255583_) -> this.biomeSource.getNoiseBiome(p_255581_, p_255582_, p_255583_, p_224226_.sampler())
        );
        WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
        int i = 8;
        ChunkPos chunkpos = p_224229_.getPos();
        NoiseChunk noisechunk = p_224229_.getOrCreateNoiseChunk(p_224250_ -> this.createNoiseChunk(p_224250_, p_224228_, Blender.of(p_224224_), p_224226_));
        Aquifer aquifer = noisechunk.aquifer();
        CarvingContext carvingcontext = new CarvingContext(
            this, p_224224_.registryAccess(), p_224229_.getHeightAccessorForGeneration(), noisechunk, p_224226_, this.settings.value().surfaceRule()
        );
        CarvingMask carvingmask = ((ProtoChunk)p_224229_).getOrCreateCarvingMask();

        for (int j = -8; j <= 8; j++) {
            for (int k = -8; k <= 8; k++) {
                ChunkPos chunkpos1 = new ChunkPos(chunkpos.x + j, chunkpos.z + k);
                ChunkAccess chunkaccess = p_224224_.getChunk(chunkpos1.x, chunkpos1.z);
                BiomeGenerationSettings biomegenerationsettings = chunkaccess.carverBiome(
                    () -> this.getBiomeGenerationSettings(
                            this.biomeSource
                                .getNoiseBiome(QuartPos.fromBlock(chunkpos1.getMinBlockX()), 0, QuartPos.fromBlock(chunkpos1.getMinBlockZ()), p_224226_.sampler())
                        )
                );
                Iterable<Holder<ConfiguredWorldCarver<?>>> iterable = biomegenerationsettings.getCarvers();
                int l = 0;

                for (Holder<ConfiguredWorldCarver<?>> holder : iterable) {
                    ConfiguredWorldCarver<?> configuredworldcarver = holder.value();
                    worldgenrandom.setLargeFeatureSeed(p_224225_ + (long)l, chunkpos1.x, chunkpos1.z);
                    if (configuredworldcarver.isStartChunk(worldgenrandom)) {
                        configuredworldcarver.carve(carvingcontext, p_224229_, biomemanager::getBiome, worldgenrandom, aquifer, chunkpos1, carvingmask);
                    }

                    l++;
                }
            }
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender p_224313_, RandomState p_224314_, StructureManager p_224315_, ChunkAccess p_224316_) {
        NoiseSettings noisesettings = this.settings.value().noiseSettings().clampToHeightAccessor(p_224316_.getHeightAccessorForGeneration());
        int i = noisesettings.minY();
        int j = Mth.floorDiv(i, noisesettings.getCellHeight());
        int k = Mth.floorDiv(noisesettings.height(), noisesettings.getCellHeight());
        return k <= 0 ? CompletableFuture.completedFuture(p_224316_) : CompletableFuture.supplyAsync(() -> {
            int l = p_224316_.getSectionIndex(k * noisesettings.getCellHeight() - 1 + i);
            int i1 = p_224316_.getSectionIndex(i);
            Set<LevelChunkSection> set = Sets.newHashSet();

            for (int j1 = l; j1 >= i1; j1--) {
                LevelChunkSection levelchunksection = p_224316_.getSection(j1);
                levelchunksection.acquire();
                set.add(levelchunksection);
            }

            ChunkAccess chunkaccess;
            try {
                chunkaccess = this.doFill(p_224313_, p_224315_, p_224314_, p_224316_, j, k);
            } finally {
                for (LevelChunkSection levelchunksection1 : set) {
                    levelchunksection1.release();
                }
            }

            return chunkaccess;
        }, Util.backgroundExecutor().forName("wgen_fill_noise"));
    }

    private ChunkAccess doFill(Blender pBlender, StructureManager pStructureManager, RandomState pRandom, ChunkAccess pChunk, int pMinCellY, int pCellCountY) {
        NoiseChunk noisechunk = pChunk.getOrCreateNoiseChunk(p_224255_ -> this.createNoiseChunk(p_224255_, pStructureManager, pBlender, pRandom));
        Heightmap heightmap = pChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap heightmap1 = pChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkpos = pChunk.getPos();
        int i = chunkpos.getMinBlockX();
        int j = chunkpos.getMinBlockZ();
        Aquifer aquifer = noisechunk.aquifer();
        noisechunk.initializeForFirstCellX();
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        int k = noisechunk.cellWidth();
        int l = noisechunk.cellHeight();
        int i1 = 16 / k;
        int j1 = 16 / k;

        for (int k1 = 0; k1 < i1; k1++) {
            noisechunk.advanceCellX(k1);

            for (int l1 = 0; l1 < j1; l1++) {
                int i2 = pChunk.getSectionsCount() - 1;
                LevelChunkSection levelchunksection = pChunk.getSection(i2);

                for (int j2 = pCellCountY - 1; j2 >= 0; j2--) {
                    noisechunk.selectCellYZ(j2, l1);

                    for (int k2 = l - 1; k2 >= 0; k2--) {
                        int l2 = (pMinCellY + j2) * l + k2;
                        int i3 = l2 & 15;
                        int j3 = pChunk.getSectionIndex(l2);
                        if (i2 != j3) {
                            i2 = j3;
                            levelchunksection = pChunk.getSection(j3);
                        }

                        double d0 = (double)k2 / (double)l;
                        noisechunk.updateForY(l2, d0);

                        for (int k3 = 0; k3 < k; k3++) {
                            int l3 = i + k1 * k + k3;
                            int i4 = l3 & 15;
                            double d1 = (double)k3 / (double)k;
                            noisechunk.updateForX(l3, d1);

                            for (int j4 = 0; j4 < k; j4++) {
                                int k4 = j + l1 * k + j4;
                                int l4 = k4 & 15;
                                double d2 = (double)j4 / (double)k;
                                noisechunk.updateForZ(k4, d2);
                                BlockState blockstate = noisechunk.getInterpolatedState();
                                if (blockstate == null) {
                                    blockstate = this.settings.value().defaultBlock();
                                }

                                blockstate = this.debugPreliminarySurfaceLevel(noisechunk, l3, l2, k4, blockstate);
                                if (blockstate != AIR && !SharedConstants.debugVoidTerrain(pChunk.getPos())) {
                                    levelchunksection.setBlockState(i4, i3, l4, blockstate, false);
                                    heightmap.update(i4, l2, l4, blockstate);
                                    heightmap1.update(i4, l2, l4, blockstate);
                                    if (aquifer.shouldScheduleFluidUpdate() && !blockstate.getFluidState().isEmpty()) {
                                        blockpos$mutableblockpos.set(l3, l2, k4);
                                        pChunk.markPosForPostprocessing(blockpos$mutableblockpos);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            noisechunk.swapSlices();
        }

        noisechunk.stopInterpolation();
        return pChunk;
    }

    private BlockState debugPreliminarySurfaceLevel(NoiseChunk pChunk, int pX, int pY, int pZ, BlockState pState) {
        return pState;
    }

    @Override
    public int getGenDepth() {
        return this.settings.value().noiseSettings().height();
    }

    @Override
    public int getSeaLevel() {
        return this.settings.value().seaLevel();
    }

    @Override
    public int getMinY() {
        return this.settings.value().noiseSettings().minY();
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion p_64379_) {
        if (!this.settings.value().disableMobGeneration()) {
            ChunkPos chunkpos = p_64379_.getCenter();
            Holder<Biome> holder = p_64379_.getBiome(chunkpos.getWorldPosition().atY(p_64379_.getMaxY()));
            WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
            worldgenrandom.setDecorationSeed(p_64379_.getSeed(), chunkpos.getMinBlockX(), chunkpos.getMinBlockZ());
            NaturalSpawner.spawnMobsForChunkGeneration(p_64379_, holder, chunkpos, worldgenrandom);
        }
    }
}