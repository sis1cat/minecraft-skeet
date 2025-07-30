package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.EndFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class TheEndGatewayBlockEntity extends TheEndPortalBlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SPAWN_TIME = 200;
    private static final int COOLDOWN_TIME = 40;
    private static final int ATTENTION_INTERVAL = 2400;
    private static final int EVENT_COOLDOWN = 1;
    private static final int GATEWAY_HEIGHT_ABOVE_SURFACE = 10;
    private long age;
    private int teleportCooldown;
    @Nullable
    private BlockPos exitPortal;
    private boolean exactTeleport;

    public TheEndGatewayBlockEntity(BlockPos p_155813_, BlockState p_155814_) {
        super(BlockEntityType.END_GATEWAY, p_155813_, p_155814_);
    }

    @Override
    protected void saveAdditional(CompoundTag p_187527_, HolderLookup.Provider p_328092_) {
        super.saveAdditional(p_187527_, p_328092_);
        p_187527_.putLong("Age", this.age);
        if (this.exitPortal != null) {
            p_187527_.put("exit_portal", NbtUtils.writeBlockPos(this.exitPortal));
        }

        if (this.exactTeleport) {
            p_187527_.putBoolean("ExactTeleport", true);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag p_328247_, HolderLookup.Provider p_335607_) {
        super.loadAdditional(p_328247_, p_335607_);
        this.age = p_328247_.getLong("Age");
        NbtUtils.readBlockPos(p_328247_, "exit_portal").filter(Level::isInSpawnableBounds).ifPresent(p_327323_ -> this.exitPortal = p_327323_);
        this.exactTeleport = p_328247_.getBoolean("ExactTeleport");
    }

    public static void beamAnimationTick(Level pLevel, BlockPos pPos, BlockState pState, TheEndGatewayBlockEntity pBlockEntity) {
        pBlockEntity.age++;
        if (pBlockEntity.isCoolingDown()) {
            pBlockEntity.teleportCooldown--;
        }
    }

    public static void portalTick(Level pLevel, BlockPos pPos, BlockState pState, TheEndGatewayBlockEntity pBlockEntity) {
        boolean flag = pBlockEntity.isSpawning();
        boolean flag1 = pBlockEntity.isCoolingDown();
        pBlockEntity.age++;
        if (flag1) {
            pBlockEntity.teleportCooldown--;
        } else if (pBlockEntity.age % 2400L == 0L) {
            triggerCooldown(pLevel, pPos, pState, pBlockEntity);
        }

        if (flag != pBlockEntity.isSpawning() || flag1 != pBlockEntity.isCoolingDown()) {
            setChanged(pLevel, pPos, pState);
        }
    }

    public boolean isSpawning() {
        return this.age < 200L;
    }

    public boolean isCoolingDown() {
        return this.teleportCooldown > 0;
    }

    public float getSpawnPercent(float pPartialTicks) {
        return Mth.clamp(((float)this.age + pPartialTicks) / 200.0F, 0.0F, 1.0F);
    }

    public float getCooldownPercent(float pPartialTicks) {
        return 1.0F - Mth.clamp(((float)this.teleportCooldown - pPartialTicks) / 40.0F, 0.0F, 1.0F);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_332673_) {
        return this.saveCustomOnly(p_332673_);
    }

    public static void triggerCooldown(Level pLevel, BlockPos pPos, BlockState pState, TheEndGatewayBlockEntity pBlockEntity) {
        if (!pLevel.isClientSide) {
            pBlockEntity.teleportCooldown = 40;
            pLevel.blockEvent(pPos, pState.getBlock(), 1, 0);
            setChanged(pLevel, pPos, pState);
        }
    }

    @Override
    public boolean triggerEvent(int pId, int pType) {
        if (pId == 1) {
            this.teleportCooldown = 40;
            return true;
        } else {
            return super.triggerEvent(pId, pType);
        }
    }

    @Nullable
    public Vec3 getPortalPosition(ServerLevel pLevel, BlockPos pPos) {
        if (this.exitPortal == null && pLevel.dimension() == Level.END) {
            BlockPos blockpos = findOrCreateValidTeleportPos(pLevel, pPos);
            blockpos = blockpos.above(10);
            LOGGER.debug("Creating portal at {}", blockpos);
            spawnGatewayPortal(pLevel, blockpos, EndGatewayConfiguration.knownExit(pPos, false));
            this.setExitPosition(blockpos, this.exactTeleport);
        }

        if (this.exitPortal != null) {
            BlockPos blockpos1 = this.exactTeleport ? this.exitPortal : findExitPosition(pLevel, this.exitPortal);
            return blockpos1.getBottomCenter();
        } else {
            return null;
        }
    }

    private static BlockPos findExitPosition(Level pLevel, BlockPos pPos) {
        BlockPos blockpos = findTallestBlock(pLevel, pPos.offset(0, 2, 0), 5, false);
        LOGGER.debug("Best exit position for portal at {} is {}", pPos, blockpos);
        return blockpos.above();
    }

    private static BlockPos findOrCreateValidTeleportPos(ServerLevel pLevel, BlockPos pPos) {
        Vec3 vec3 = findExitPortalXZPosTentative(pLevel, pPos);
        LevelChunk levelchunk = getChunk(pLevel, vec3);
        BlockPos blockpos = findValidSpawnInChunk(levelchunk);
        if (blockpos == null) {
            BlockPos blockpos1 = BlockPos.containing(vec3.x + 0.5, 75.0, vec3.z + 0.5);
            LOGGER.debug("Failed to find a suitable block to teleport to, spawning an island on {}", blockpos1);
            pLevel.registryAccess()
                .lookup(Registries.CONFIGURED_FEATURE)
                .flatMap(p_360496_ -> p_360496_.get(EndFeatures.END_ISLAND))
                .ifPresent(
                    p_256040_ -> p_256040_.value()
                            .place(pLevel, pLevel.getChunkSource().getGenerator(), RandomSource.create(blockpos1.asLong()), blockpos1)
                );
            blockpos = blockpos1;
        } else {
            LOGGER.debug("Found suitable block to teleport to: {}", blockpos);
        }

        return findTallestBlock(pLevel, blockpos, 16, true);
    }

    private static Vec3 findExitPortalXZPosTentative(ServerLevel pLevel, BlockPos pPos) {
        Vec3 vec3 = new Vec3((double)pPos.getX(), 0.0, (double)pPos.getZ()).normalize();
        int i = 1024;
        Vec3 vec31 = vec3.scale(1024.0);

        for (int j = 16; !isChunkEmpty(pLevel, vec31) && j-- > 0; vec31 = vec31.add(vec3.scale(-16.0))) {
            LOGGER.debug("Skipping backwards past nonempty chunk at {}", vec31);
        }

        for (int k = 16; isChunkEmpty(pLevel, vec31) && k-- > 0; vec31 = vec31.add(vec3.scale(16.0))) {
            LOGGER.debug("Skipping forward past empty chunk at {}", vec31);
        }

        LOGGER.debug("Found chunk at {}", vec31);
        return vec31;
    }

    private static boolean isChunkEmpty(ServerLevel pLevel, Vec3 pPos) {
        return getChunk(pLevel, pPos).getHighestFilledSectionIndex() == -1;
    }

    private static BlockPos findTallestBlock(BlockGetter pLevel, BlockPos pPos, int pRadius, boolean pAllowBedrock) {
        BlockPos blockpos = null;

        for (int i = -pRadius; i <= pRadius; i++) {
            for (int j = -pRadius; j <= pRadius; j++) {
                if (i != 0 || j != 0 || pAllowBedrock) {
                    for (int k = pLevel.getMaxY(); k > (blockpos == null ? pLevel.getMinY() : blockpos.getY()); k--) {
                        BlockPos blockpos1 = new BlockPos(pPos.getX() + i, k, pPos.getZ() + j);
                        BlockState blockstate = pLevel.getBlockState(blockpos1);
                        if (blockstate.isCollisionShapeFullBlock(pLevel, blockpos1) && (pAllowBedrock || !blockstate.is(Blocks.BEDROCK))) {
                            blockpos = blockpos1;
                            break;
                        }
                    }
                }
            }
        }

        return blockpos == null ? pPos : blockpos;
    }

    private static LevelChunk getChunk(Level pLevel, Vec3 pPos) {
        return pLevel.getChunk(Mth.floor(pPos.x / 16.0), Mth.floor(pPos.z / 16.0));
    }

    @Nullable
    private static BlockPos findValidSpawnInChunk(LevelChunk pChunk) {
        ChunkPos chunkpos = pChunk.getPos();
        BlockPos blockpos = new BlockPos(chunkpos.getMinBlockX(), 30, chunkpos.getMinBlockZ());
        int i = pChunk.getHighestSectionPosition() + 16 - 1;
        BlockPos blockpos1 = new BlockPos(chunkpos.getMaxBlockX(), i, chunkpos.getMaxBlockZ());
        BlockPos blockpos2 = null;
        double d0 = 0.0;

        for (BlockPos blockpos3 : BlockPos.betweenClosed(blockpos, blockpos1)) {
            BlockState blockstate = pChunk.getBlockState(blockpos3);
            BlockPos blockpos4 = blockpos3.above();
            BlockPos blockpos5 = blockpos3.above(2);
            if (blockstate.is(Blocks.END_STONE)
                && !pChunk.getBlockState(blockpos4).isCollisionShapeFullBlock(pChunk, blockpos4)
                && !pChunk.getBlockState(blockpos5).isCollisionShapeFullBlock(pChunk, blockpos5)) {
                double d1 = blockpos3.distToCenterSqr(0.0, 0.0, 0.0);
                if (blockpos2 == null || d1 < d0) {
                    blockpos2 = blockpos3;
                    d0 = d1;
                }
            }
        }

        return blockpos2;
    }

    private static void spawnGatewayPortal(ServerLevel pLevel, BlockPos pPos, EndGatewayConfiguration pConfig) {
        Feature.END_GATEWAY.place(pConfig, pLevel, pLevel.getChunkSource().getGenerator(), RandomSource.create(), pPos);
    }

    @Override
    public boolean shouldRenderFace(Direction pFace) {
        return Block.shouldRenderFace(this.getBlockState(), this.level.getBlockState(this.getBlockPos().relative(pFace)), pFace);
    }

    public int getParticleAmount() {
        int i = 0;

        for (Direction direction : Direction.values()) {
            i += this.shouldRenderFace(direction) ? 1 : 0;
        }

        return i;
    }

    public void setExitPosition(BlockPos pExitPortal, boolean pExactTeleport) {
        this.exactTeleport = pExactTeleport;
        this.exitPortal = pExitPortal;
        this.setChanged();
    }
}