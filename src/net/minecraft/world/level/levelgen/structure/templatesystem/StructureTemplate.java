package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.IdMapper;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public class StructureTemplate {
    public static final String PALETTE_TAG = "palette";
    public static final String PALETTE_LIST_TAG = "palettes";
    public static final String ENTITIES_TAG = "entities";
    public static final String BLOCKS_TAG = "blocks";
    public static final String BLOCK_TAG_POS = "pos";
    public static final String BLOCK_TAG_STATE = "state";
    public static final String BLOCK_TAG_NBT = "nbt";
    public static final String ENTITY_TAG_POS = "pos";
    public static final String ENTITY_TAG_BLOCKPOS = "blockPos";
    public static final String ENTITY_TAG_NBT = "nbt";
    public static final String SIZE_TAG = "size";
    private final List<StructureTemplate.Palette> palettes = Lists.newArrayList();
    private final List<StructureTemplate.StructureEntityInfo> entityInfoList = Lists.newArrayList();
    private Vec3i size = Vec3i.ZERO;
    private String author = "?";

    public Vec3i getSize() {
        return this.size;
    }

    public void setAuthor(String pAuthor) {
        this.author = pAuthor;
    }

    public String getAuthor() {
        return this.author;
    }

    public void fillFromWorld(Level pLevel, BlockPos pPos, Vec3i pSize, boolean pWithEntities, @Nullable Block pToIgnore) {
        if (pSize.getX() >= 1 && pSize.getY() >= 1 && pSize.getZ() >= 1) {
            BlockPos blockpos = pPos.offset(pSize).offset(-1, -1, -1);
            List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
            List<StructureTemplate.StructureBlockInfo> list1 = Lists.newArrayList();
            List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();
            BlockPos blockpos1 = new BlockPos(
                Math.min(pPos.getX(), blockpos.getX()),
                Math.min(pPos.getY(), blockpos.getY()),
                Math.min(pPos.getZ(), blockpos.getZ())
            );
            BlockPos blockpos2 = new BlockPos(
                Math.max(pPos.getX(), blockpos.getX()),
                Math.max(pPos.getY(), blockpos.getY()),
                Math.max(pPos.getZ(), blockpos.getZ())
            );
            this.size = pSize;

            for (BlockPos blockpos3 : BlockPos.betweenClosed(blockpos1, blockpos2)) {
                BlockPos blockpos4 = blockpos3.subtract(blockpos1);
                BlockState blockstate = pLevel.getBlockState(blockpos3);
                if (pToIgnore == null || !blockstate.is(pToIgnore)) {
                    BlockEntity blockentity = pLevel.getBlockEntity(blockpos3);
                    StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo;
                    if (blockentity != null) {
                        structuretemplate$structureblockinfo = new StructureTemplate.StructureBlockInfo(
                            blockpos4, blockstate, blockentity.saveWithId(pLevel.registryAccess())
                        );
                    } else {
                        structuretemplate$structureblockinfo = new StructureTemplate.StructureBlockInfo(blockpos4, blockstate, null);
                    }

                    addToLists(structuretemplate$structureblockinfo, list, list1, list2);
                }
            }

            List<StructureTemplate.StructureBlockInfo> list3 = buildInfoList(list, list1, list2);
            this.palettes.clear();
            this.palettes.add(new StructureTemplate.Palette(list3));
            if (pWithEntities) {
                this.fillEntityList(pLevel, blockpos1, blockpos2);
            } else {
                this.entityInfoList.clear();
            }
        }
    }

    private static void addToLists(
        StructureTemplate.StructureBlockInfo pBlockInfo,
        List<StructureTemplate.StructureBlockInfo> pNormalBlocks,
        List<StructureTemplate.StructureBlockInfo> pBlocksWithNbt,
        List<StructureTemplate.StructureBlockInfo> pBlocksWithSpecialShape
    ) {
        if (pBlockInfo.nbt != null) {
            pBlocksWithNbt.add(pBlockInfo);
        } else if (!pBlockInfo.state.getBlock().hasDynamicShape() && pBlockInfo.state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
            pNormalBlocks.add(pBlockInfo);
        } else {
            pBlocksWithSpecialShape.add(pBlockInfo);
        }
    }

    private static List<StructureTemplate.StructureBlockInfo> buildInfoList(
        List<StructureTemplate.StructureBlockInfo> pNormalBlocks,
        List<StructureTemplate.StructureBlockInfo> pBlocksWithNbt,
        List<StructureTemplate.StructureBlockInfo> pBlocksWithSpecialShape
    ) {
        Comparator<StructureTemplate.StructureBlockInfo> comparator = Comparator.<StructureTemplate.StructureBlockInfo>comparingInt(
                p_74641_ -> p_74641_.pos.getY()
            )
            .thenComparingInt(p_74637_ -> p_74637_.pos.getX())
            .thenComparingInt(p_74572_ -> p_74572_.pos.getZ());
        pNormalBlocks.sort(comparator);
        pBlocksWithSpecialShape.sort(comparator);
        pBlocksWithNbt.sort(comparator);
        List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
        list.addAll(pNormalBlocks);
        list.addAll(pBlocksWithSpecialShape);
        list.addAll(pBlocksWithNbt);
        return list;
    }

    private void fillEntityList(Level pLevel, BlockPos pStartPos, BlockPos pEndPos) {
        List<Entity> list = pLevel.getEntitiesOfClass(Entity.class, AABB.encapsulatingFullBlocks(pStartPos, pEndPos), p_74499_ -> !(p_74499_ instanceof Player));
        this.entityInfoList.clear();

        for (Entity entity : list) {
            Vec3 vec3 = new Vec3(
                entity.getX() - (double)pStartPos.getX(),
                entity.getY() - (double)pStartPos.getY(),
                entity.getZ() - (double)pStartPos.getZ()
            );
            CompoundTag compoundtag = new CompoundTag();
            entity.save(compoundtag);
            BlockPos blockpos;
            if (entity instanceof Painting) {
                blockpos = ((Painting)entity).getPos().subtract(pStartPos);
            } else {
                blockpos = BlockPos.containing(vec3);
            }

            this.entityInfoList.add(new StructureTemplate.StructureEntityInfo(vec3, blockpos, compoundtag.copy()));
        }
    }

    public List<StructureTemplate.StructureBlockInfo> filterBlocks(BlockPos pPos, StructurePlaceSettings pSettings, Block pBlock) {
        return this.filterBlocks(pPos, pSettings, pBlock, true);
    }

    public List<StructureTemplate.JigsawBlockInfo> getJigsaws(BlockPos pPos, Rotation pRotation) {
        if (this.palettes.isEmpty()) {
            return new ArrayList<>();
        } else {
            StructurePlaceSettings structureplacesettings = new StructurePlaceSettings().setRotation(pRotation);
            List<StructureTemplate.JigsawBlockInfo> list = structureplacesettings.getRandomPalette(this.palettes, pPos).jigsaws();
            List<StructureTemplate.JigsawBlockInfo> list1 = new ArrayList<>(list.size());

            for (StructureTemplate.JigsawBlockInfo structuretemplate$jigsawblockinfo : list) {
                StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo = structuretemplate$jigsawblockinfo.info;
                list1.add(
                    structuretemplate$jigsawblockinfo.withInfo(
                        new StructureTemplate.StructureBlockInfo(
                            calculateRelativePosition(structureplacesettings, structuretemplate$structureblockinfo.pos()).offset(pPos),
                            structuretemplate$structureblockinfo.state.rotate(structureplacesettings.getRotation()),
                            structuretemplate$structureblockinfo.nbt
                        )
                    )
                );
            }

            return list1;
        }
    }

    public ObjectArrayList<StructureTemplate.StructureBlockInfo> filterBlocks(
        BlockPos pPos, StructurePlaceSettings pSettings, Block pBlock, boolean pRelativePosition
    ) {
        ObjectArrayList<StructureTemplate.StructureBlockInfo> objectarraylist = new ObjectArrayList<>();
        BoundingBox boundingbox = pSettings.getBoundingBox();
        if (this.palettes.isEmpty()) {
            return objectarraylist;
        } else {
            for (StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo : pSettings.getRandomPalette(this.palettes, pPos).blocks(pBlock)) {
                BlockPos blockpos = pRelativePosition
                    ? calculateRelativePosition(pSettings, structuretemplate$structureblockinfo.pos).offset(pPos)
                    : structuretemplate$structureblockinfo.pos;
                if (boundingbox == null || boundingbox.isInside(blockpos)) {
                    objectarraylist.add(
                        new StructureTemplate.StructureBlockInfo(
                            blockpos,
                            structuretemplate$structureblockinfo.state.rotate(pSettings.getRotation()),
                            structuretemplate$structureblockinfo.nbt
                        )
                    );
                }
            }

            return objectarraylist;
        }
    }

    public BlockPos calculateConnectedPosition(StructurePlaceSettings pDecorator, BlockPos pStart, StructurePlaceSettings pSettings, BlockPos pEnd) {
        BlockPos blockpos = calculateRelativePosition(pDecorator, pStart);
        BlockPos blockpos1 = calculateRelativePosition(pSettings, pEnd);
        return blockpos.subtract(blockpos1);
    }

    public static BlockPos calculateRelativePosition(StructurePlaceSettings pDecorator, BlockPos pPos) {
        return transform(pPos, pDecorator.getMirror(), pDecorator.getRotation(), pDecorator.getRotationPivot());
    }

    public boolean placeInWorld(
        ServerLevelAccessor pServerLevel, BlockPos pOffset, BlockPos pPos, StructurePlaceSettings pSettings, RandomSource pRandom, int pFlags
    ) {
        if (this.palettes.isEmpty()) {
            return false;
        } else {
            List<StructureTemplate.StructureBlockInfo> list = pSettings.getRandomPalette(this.palettes, pOffset).blocks();
            if ((!list.isEmpty() || !pSettings.isIgnoreEntities() && !this.entityInfoList.isEmpty())
                && this.size.getX() >= 1
                && this.size.getY() >= 1
                && this.size.getZ() >= 1) {
                BoundingBox boundingbox = pSettings.getBoundingBox();
                List<BlockPos> list1 = Lists.newArrayListWithCapacity(pSettings.shouldApplyWaterlogging() ? list.size() : 0);
                List<BlockPos> list2 = Lists.newArrayListWithCapacity(pSettings.shouldApplyWaterlogging() ? list.size() : 0);
                List<Pair<BlockPos, CompoundTag>> list3 = Lists.newArrayListWithCapacity(list.size());
                int i = Integer.MAX_VALUE;
                int j = Integer.MAX_VALUE;
                int k = Integer.MAX_VALUE;
                int l = Integer.MIN_VALUE;
                int i1 = Integer.MIN_VALUE;
                int j1 = Integer.MIN_VALUE;

                for (StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo : processBlockInfos(pServerLevel, pOffset, pPos, pSettings, list)) {
                    BlockPos blockpos = structuretemplate$structureblockinfo.pos;
                    if (boundingbox == null || boundingbox.isInside(blockpos)) {
                        FluidState fluidstate = pSettings.shouldApplyWaterlogging() ? pServerLevel.getFluidState(blockpos) : null;
                        BlockState blockstate = structuretemplate$structureblockinfo.state.mirror(pSettings.getMirror()).rotate(pSettings.getRotation());
                        if (structuretemplate$structureblockinfo.nbt != null) {
                            BlockEntity blockentity = pServerLevel.getBlockEntity(blockpos);
                            Clearable.tryClear(blockentity);
                            pServerLevel.setBlock(blockpos, Blocks.BARRIER.defaultBlockState(), 20);
                        }

                        if (pServerLevel.setBlock(blockpos, blockstate, pFlags)) {
                            i = Math.min(i, blockpos.getX());
                            j = Math.min(j, blockpos.getY());
                            k = Math.min(k, blockpos.getZ());
                            l = Math.max(l, blockpos.getX());
                            i1 = Math.max(i1, blockpos.getY());
                            j1 = Math.max(j1, blockpos.getZ());
                            list3.add(Pair.of(blockpos, structuretemplate$structureblockinfo.nbt));
                            if (structuretemplate$structureblockinfo.nbt != null) {
                                BlockEntity blockentity1 = pServerLevel.getBlockEntity(blockpos);
                                if (blockentity1 != null) {
                                    if (blockentity1 instanceof RandomizableContainer) {
                                        structuretemplate$structureblockinfo.nbt.putLong("LootTableSeed", pRandom.nextLong());
                                    }

                                    blockentity1.loadWithComponents(structuretemplate$structureblockinfo.nbt, pServerLevel.registryAccess());
                                }
                            }

                            if (fluidstate != null) {
                                if (blockstate.getFluidState().isSource()) {
                                    list2.add(blockpos);
                                } else if (blockstate.getBlock() instanceof LiquidBlockContainer) {
                                    ((LiquidBlockContainer)blockstate.getBlock()).placeLiquid(pServerLevel, blockpos, blockstate, fluidstate);
                                    if (!fluidstate.isSource()) {
                                        list1.add(blockpos);
                                    }
                                }
                            }
                        }
                    }
                }

                boolean flag = true;
                Direction[] adirection = new Direction[]{Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

                while (flag && !list1.isEmpty()) {
                    flag = false;
                    Iterator<BlockPos> iterator = list1.iterator();

                    while (iterator.hasNext()) {
                        BlockPos blockpos3 = iterator.next();
                        FluidState fluidstate2 = pServerLevel.getFluidState(blockpos3);

                        for (int i2 = 0; i2 < adirection.length && !fluidstate2.isSource(); i2++) {
                            BlockPos blockpos1 = blockpos3.relative(adirection[i2]);
                            FluidState fluidstate1 = pServerLevel.getFluidState(blockpos1);
                            if (fluidstate1.isSource() && !list2.contains(blockpos1)) {
                                fluidstate2 = fluidstate1;
                            }
                        }

                        if (fluidstate2.isSource()) {
                            BlockState blockstate1 = pServerLevel.getBlockState(blockpos3);
                            Block block = blockstate1.getBlock();
                            if (block instanceof LiquidBlockContainer) {
                                ((LiquidBlockContainer)block).placeLiquid(pServerLevel, blockpos3, blockstate1, fluidstate2);
                                flag = true;
                                iterator.remove();
                            }
                        }
                    }
                }

                if (i <= l) {
                    if (!pSettings.getKnownShape()) {
                        DiscreteVoxelShape discretevoxelshape = new BitSetDiscreteVoxelShape(l - i + 1, i1 - j + 1, j1 - k + 1);
                        int k1 = i;
                        int l1 = j;
                        int j2 = k;

                        for (Pair<BlockPos, CompoundTag> pair1 : list3) {
                            BlockPos blockpos2 = pair1.getFirst();
                            discretevoxelshape.fill(blockpos2.getX() - k1, blockpos2.getY() - l1, blockpos2.getZ() - j2);
                        }

                        updateShapeAtEdge(pServerLevel, pFlags, discretevoxelshape, k1, l1, j2);
                    }

                    for (Pair<BlockPos, CompoundTag> pair : list3) {
                        BlockPos blockpos4 = pair.getFirst();
                        if (!pSettings.getKnownShape()) {
                            BlockState blockstate2 = pServerLevel.getBlockState(blockpos4);
                            BlockState blockstate3 = Block.updateFromNeighbourShapes(blockstate2, pServerLevel, blockpos4);
                            if (blockstate2 != blockstate3) {
                                pServerLevel.setBlock(blockpos4, blockstate3, pFlags & -2 | 16);
                            }

                            pServerLevel.blockUpdated(blockpos4, blockstate3.getBlock());
                        }

                        if (pair.getSecond() != null) {
                            BlockEntity blockentity2 = pServerLevel.getBlockEntity(blockpos4);
                            if (blockentity2 != null) {
                                blockentity2.setChanged();
                            }
                        }
                    }
                }

                if (!pSettings.isIgnoreEntities()) {
                    this.placeEntities(pServerLevel, pOffset, pSettings.getMirror(), pSettings.getRotation(), pSettings.getRotationPivot(), boundingbox, pSettings.shouldFinalizeEntities());
                }

                return true;
            } else {
                return false;
            }
        }
    }

    public static void updateShapeAtEdge(LevelAccessor pLevel, int pFlags, DiscreteVoxelShape pShape, BlockPos pPos) {
        updateShapeAtEdge(pLevel, pFlags, pShape, pPos.getX(), pPos.getY(), pPos.getZ());
    }

    public static void updateShapeAtEdge(LevelAccessor pLevel, int pFlags, DiscreteVoxelShape pShape, int pX, int pY, int pZ) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos blockpos$mutableblockpos1 = new BlockPos.MutableBlockPos();
        pShape.forAllFaces(
            (p_360634_, p_360635_, p_360636_, p_360637_) -> {
                blockpos$mutableblockpos.set(pX + p_360635_, pY + p_360636_, pZ + p_360637_);
                blockpos$mutableblockpos1.setWithOffset(blockpos$mutableblockpos, p_360634_);
                BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
                BlockState blockstate1 = pLevel.getBlockState(blockpos$mutableblockpos1);
                BlockState blockstate2 = blockstate.updateShape(
                    pLevel, pLevel, blockpos$mutableblockpos, p_360634_, blockpos$mutableblockpos1, blockstate1, pLevel.getRandom()
                );
                if (blockstate != blockstate2) {
                    pLevel.setBlock(blockpos$mutableblockpos, blockstate2, pFlags & -2);
                }

                BlockState blockstate3 = blockstate1.updateShape(
                    pLevel, pLevel, blockpos$mutableblockpos1, p_360634_.getOpposite(), blockpos$mutableblockpos, blockstate2, pLevel.getRandom()
                );
                if (blockstate1 != blockstate3) {
                    pLevel.setBlock(blockpos$mutableblockpos1, blockstate3, pFlags & -2);
                }
            }
        );
    }

    public static List<StructureTemplate.StructureBlockInfo> processBlockInfos(
        ServerLevelAccessor pServerLevel,
        BlockPos pOffset,
        BlockPos pPos,
        StructurePlaceSettings pSettings,
        List<StructureTemplate.StructureBlockInfo> pBlockInfos
    ) {
        List<StructureTemplate.StructureBlockInfo> list = new ArrayList<>();
        List<StructureTemplate.StructureBlockInfo> list1 = new ArrayList<>();

        for (StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo : pBlockInfos) {
            BlockPos blockpos = calculateRelativePosition(pSettings, structuretemplate$structureblockinfo.pos).offset(pOffset);
            StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo1 = new StructureTemplate.StructureBlockInfo(
                blockpos,
                structuretemplate$structureblockinfo.state,
                structuretemplate$structureblockinfo.nbt != null ? structuretemplate$structureblockinfo.nbt.copy() : null
            );
            Iterator<StructureProcessor> iterator = pSettings.getProcessors().iterator();

            while (structuretemplate$structureblockinfo1 != null && iterator.hasNext()) {
                structuretemplate$structureblockinfo1 = iterator.next()
                    .processBlock(pServerLevel, pOffset, pPos, structuretemplate$structureblockinfo, structuretemplate$structureblockinfo1, pSettings);
            }

            if (structuretemplate$structureblockinfo1 != null) {
                list1.add(structuretemplate$structureblockinfo1);
                list.add(structuretemplate$structureblockinfo);
            }
        }

        for (StructureProcessor structureprocessor : pSettings.getProcessors()) {
            list1 = structureprocessor.finalizeProcessing(pServerLevel, pOffset, pPos, list, list1, pSettings);
        }

        return list1;
    }

    private void placeEntities(
        ServerLevelAccessor pServerLevel,
        BlockPos pPos,
        Mirror pMirror,
        Rotation pRotation,
        BlockPos pOffset,
        @Nullable BoundingBox pBoundingBox,
        boolean pWithEntities
    ) {
        for (StructureTemplate.StructureEntityInfo structuretemplate$structureentityinfo : this.entityInfoList) {
            BlockPos blockpos = transform(structuretemplate$structureentityinfo.blockPos, pMirror, pRotation, pOffset).offset(pPos);
            if (pBoundingBox == null || pBoundingBox.isInside(blockpos)) {
                CompoundTag compoundtag = structuretemplate$structureentityinfo.nbt.copy();
                Vec3 vec3 = transform(structuretemplate$structureentityinfo.pos, pMirror, pRotation, pOffset);
                Vec3 vec31 = vec3.add((double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ());
                ListTag listtag = new ListTag();
                listtag.add(DoubleTag.valueOf(vec31.x));
                listtag.add(DoubleTag.valueOf(vec31.y));
                listtag.add(DoubleTag.valueOf(vec31.z));
                compoundtag.put("Pos", listtag);
                compoundtag.remove("UUID");
                createEntityIgnoreException(pServerLevel, compoundtag).ifPresent(p_275190_ -> {
                    float f = p_275190_.rotate(pRotation);
                    f += p_275190_.mirror(pMirror) - p_275190_.getYRot();
                    p_275190_.moveTo(vec31.x, vec31.y, vec31.z, f, p_275190_.getXRot());
                    if (pWithEntities && p_275190_ instanceof Mob) {
                        ((Mob)p_275190_).finalizeSpawn(pServerLevel, pServerLevel.getCurrentDifficultyAt(BlockPos.containing(vec31)), EntitySpawnReason.STRUCTURE, null);
                    }

                    pServerLevel.addFreshEntityWithPassengers(p_275190_);
                });
            }
        }
    }

    private static Optional<Entity> createEntityIgnoreException(ServerLevelAccessor pLevel, CompoundTag pTag) {
        try {
            return EntityType.create(pTag, pLevel.getLevel(), EntitySpawnReason.STRUCTURE);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public Vec3i getSize(Rotation pRotation) {
        switch (pRotation) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                return new Vec3i(this.size.getZ(), this.size.getY(), this.size.getX());
            default:
                return this.size;
        }
    }

    public static BlockPos transform(BlockPos pTargetPos, Mirror pMirror, Rotation pRotation, BlockPos pOffset) {
        int i = pTargetPos.getX();
        int j = pTargetPos.getY();
        int k = pTargetPos.getZ();
        boolean flag = true;
        switch (pMirror) {
            case LEFT_RIGHT:
                k = -k;
                break;
            case FRONT_BACK:
                i = -i;
                break;
            default:
                flag = false;
        }

        int l = pOffset.getX();
        int i1 = pOffset.getZ();
        switch (pRotation) {
            case COUNTERCLOCKWISE_90:
                return new BlockPos(l - i1 + k, j, l + i1 - i);
            case CLOCKWISE_90:
                return new BlockPos(l + i1 - k, j, i1 - l + i);
            case CLOCKWISE_180:
                return new BlockPos(l + l - i, j, i1 + i1 - k);
            default:
                return flag ? new BlockPos(i, j, k) : pTargetPos;
        }
    }

    public static Vec3 transform(Vec3 pTarget, Mirror pMirror, Rotation pRotation, BlockPos pCenterOffset) {
        double d0 = pTarget.x;
        double d1 = pTarget.y;
        double d2 = pTarget.z;
        boolean flag = true;
        switch (pMirror) {
            case LEFT_RIGHT:
                d2 = 1.0 - d2;
                break;
            case FRONT_BACK:
                d0 = 1.0 - d0;
                break;
            default:
                flag = false;
        }

        int i = pCenterOffset.getX();
        int j = pCenterOffset.getZ();
        switch (pRotation) {
            case COUNTERCLOCKWISE_90:
                return new Vec3((double)(i - j) + d2, d1, (double)(i + j + 1) - d0);
            case CLOCKWISE_90:
                return new Vec3((double)(i + j + 1) - d2, d1, (double)(j - i) + d0);
            case CLOCKWISE_180:
                return new Vec3((double)(i + i + 1) - d0, d1, (double)(j + j + 1) - d2);
            default:
                return flag ? new Vec3(d0, d1, d2) : pTarget;
        }
    }

    public BlockPos getZeroPositionWithTransform(BlockPos pTargetPos, Mirror pMirror, Rotation pRotation) {
        return getZeroPositionWithTransform(pTargetPos, pMirror, pRotation, this.getSize().getX(), this.getSize().getZ());
    }

    public static BlockPos getZeroPositionWithTransform(BlockPos pPos, Mirror pMirror, Rotation pRotation, int pSizeX, int pSizeZ) {
        pSizeX--;
        pSizeZ--;
        int i = pMirror == Mirror.FRONT_BACK ? pSizeX : 0;
        int j = pMirror == Mirror.LEFT_RIGHT ? pSizeZ : 0;
        BlockPos blockpos = pPos;
        switch (pRotation) {
            case COUNTERCLOCKWISE_90:
                blockpos = pPos.offset(j, 0, pSizeX - i);
                break;
            case CLOCKWISE_90:
                blockpos = pPos.offset(pSizeZ - j, 0, i);
                break;
            case CLOCKWISE_180:
                blockpos = pPos.offset(pSizeX - i, 0, pSizeZ - j);
                break;
            case NONE:
                blockpos = pPos.offset(i, 0, j);
        }

        return blockpos;
    }

    public BoundingBox getBoundingBox(StructurePlaceSettings pSettings, BlockPos pStartPos) {
        return this.getBoundingBox(pStartPos, pSettings.getRotation(), pSettings.getRotationPivot(), pSettings.getMirror());
    }

    public BoundingBox getBoundingBox(BlockPos pStartPos, Rotation pRotation, BlockPos pPivotPos, Mirror pMirror) {
        return getBoundingBox(pStartPos, pRotation, pPivotPos, pMirror, this.size);
    }

    @VisibleForTesting
    protected static BoundingBox getBoundingBox(BlockPos pStartPos, Rotation pRotation, BlockPos pPivotPos, Mirror pMirror, Vec3i pSize) {
        Vec3i vec3i = pSize.offset(-1, -1, -1);
        BlockPos blockpos = transform(BlockPos.ZERO, pMirror, pRotation, pPivotPos);
        BlockPos blockpos1 = transform(BlockPos.ZERO.offset(vec3i), pMirror, pRotation, pPivotPos);
        return BoundingBox.fromCorners(blockpos, blockpos1).move(pStartPos);
    }

    public CompoundTag save(CompoundTag pTag) {
        if (this.palettes.isEmpty()) {
            pTag.put("blocks", new ListTag());
            pTag.put("palette", new ListTag());
        } else {
            List<StructureTemplate.SimplePalette> list = Lists.newArrayList();
            StructureTemplate.SimplePalette structuretemplate$simplepalette = new StructureTemplate.SimplePalette();
            list.add(structuretemplate$simplepalette);

            for (int i = 1; i < this.palettes.size(); i++) {
                list.add(new StructureTemplate.SimplePalette());
            }

            ListTag listtag1 = new ListTag();
            List<StructureTemplate.StructureBlockInfo> list1 = this.palettes.get(0).blocks();

            for (int j = 0; j < list1.size(); j++) {
                StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo = list1.get(j);
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.put(
                    "pos",
                    this.newIntegerList(
                        structuretemplate$structureblockinfo.pos.getX(),
                        structuretemplate$structureblockinfo.pos.getY(),
                        structuretemplate$structureblockinfo.pos.getZ()
                    )
                );
                int k = structuretemplate$simplepalette.idFor(structuretemplate$structureblockinfo.state);
                compoundtag.putInt("state", k);
                if (structuretemplate$structureblockinfo.nbt != null) {
                    compoundtag.put("nbt", structuretemplate$structureblockinfo.nbt);
                }

                listtag1.add(compoundtag);

                for (int l = 1; l < this.palettes.size(); l++) {
                    StructureTemplate.SimplePalette structuretemplate$simplepalette1 = list.get(l);
                    structuretemplate$simplepalette1.addMapping(this.palettes.get(l).blocks().get(j).state, k);
                }
            }

            pTag.put("blocks", listtag1);
            if (list.size() == 1) {
                ListTag listtag2 = new ListTag();

                for (BlockState blockstate : structuretemplate$simplepalette) {
                    listtag2.add(NbtUtils.writeBlockState(blockstate));
                }

                pTag.put("palette", listtag2);
            } else {
                ListTag listtag3 = new ListTag();

                for (StructureTemplate.SimplePalette structuretemplate$simplepalette2 : list) {
                    ListTag listtag4 = new ListTag();

                    for (BlockState blockstate1 : structuretemplate$simplepalette2) {
                        listtag4.add(NbtUtils.writeBlockState(blockstate1));
                    }

                    listtag3.add(listtag4);
                }

                pTag.put("palettes", listtag3);
            }
        }

        ListTag listtag = new ListTag();

        for (StructureTemplate.StructureEntityInfo structuretemplate$structureentityinfo : this.entityInfoList) {
            CompoundTag compoundtag1 = new CompoundTag();
            compoundtag1.put(
                "pos",
                this.newDoubleList(
                    structuretemplate$structureentityinfo.pos.x,
                    structuretemplate$structureentityinfo.pos.y,
                    structuretemplate$structureentityinfo.pos.z
                )
            );
            compoundtag1.put(
                "blockPos",
                this.newIntegerList(
                    structuretemplate$structureentityinfo.blockPos.getX(),
                    structuretemplate$structureentityinfo.blockPos.getY(),
                    structuretemplate$structureentityinfo.blockPos.getZ()
                )
            );
            if (structuretemplate$structureentityinfo.nbt != null) {
                compoundtag1.put("nbt", structuretemplate$structureentityinfo.nbt);
            }

            listtag.add(compoundtag1);
        }

        pTag.put("entities", listtag);
        pTag.put("size", this.newIntegerList(this.size.getX(), this.size.getY(), this.size.getZ()));
        return NbtUtils.addCurrentDataVersion(pTag);
    }

    public void load(HolderGetter<Block> pBlockGetter, CompoundTag pTag) {
        this.palettes.clear();
        this.entityInfoList.clear();
        ListTag listtag = pTag.getList("size", 3);
        this.size = new Vec3i(listtag.getInt(0), listtag.getInt(1), listtag.getInt(2));
        ListTag listtag1 = pTag.getList("blocks", 10);
        if (pTag.contains("palettes", 9)) {
            ListTag listtag2 = pTag.getList("palettes", 9);

            for (int i = 0; i < listtag2.size(); i++) {
                this.loadPalette(pBlockGetter, listtag2.getList(i), listtag1);
            }
        } else {
            this.loadPalette(pBlockGetter, pTag.getList("palette", 10), listtag1);
        }

        ListTag listtag5 = pTag.getList("entities", 10);

        for (int j = 0; j < listtag5.size(); j++) {
            CompoundTag compoundtag = listtag5.getCompound(j);
            ListTag listtag3 = compoundtag.getList("pos", 6);
            Vec3 vec3 = new Vec3(listtag3.getDouble(0), listtag3.getDouble(1), listtag3.getDouble(2));
            ListTag listtag4 = compoundtag.getList("blockPos", 3);
            BlockPos blockpos = new BlockPos(listtag4.getInt(0), listtag4.getInt(1), listtag4.getInt(2));
            if (compoundtag.contains("nbt")) {
                CompoundTag compoundtag1 = compoundtag.getCompound("nbt");
                this.entityInfoList.add(new StructureTemplate.StructureEntityInfo(vec3, blockpos, compoundtag1));
            }
        }
    }

    private void loadPalette(HolderGetter<Block> pBlockGetter, ListTag pPaletteTag, ListTag pBlocksTag) {
        StructureTemplate.SimplePalette structuretemplate$simplepalette = new StructureTemplate.SimplePalette();

        for (int i = 0; i < pPaletteTag.size(); i++) {
            structuretemplate$simplepalette.addMapping(NbtUtils.readBlockState(pBlockGetter, pPaletteTag.getCompound(i)), i);
        }

        List<StructureTemplate.StructureBlockInfo> list2 = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> list = Lists.newArrayList();
        List<StructureTemplate.StructureBlockInfo> list1 = Lists.newArrayList();

        for (int j = 0; j < pBlocksTag.size(); j++) {
            CompoundTag compoundtag = pBlocksTag.getCompound(j);
            ListTag listtag = compoundtag.getList("pos", 3);
            BlockPos blockpos = new BlockPos(listtag.getInt(0), listtag.getInt(1), listtag.getInt(2));
            BlockState blockstate = structuretemplate$simplepalette.stateFor(compoundtag.getInt("state"));
            CompoundTag compoundtag1;
            if (compoundtag.contains("nbt")) {
                compoundtag1 = compoundtag.getCompound("nbt");
            } else {
                compoundtag1 = null;
            }

            StructureTemplate.StructureBlockInfo structuretemplate$structureblockinfo = new StructureTemplate.StructureBlockInfo(
                blockpos, blockstate, compoundtag1
            );
            addToLists(structuretemplate$structureblockinfo, list2, list, list1);
        }

        List<StructureTemplate.StructureBlockInfo> list3 = buildInfoList(list2, list, list1);
        this.palettes.add(new StructureTemplate.Palette(list3));
    }

    private ListTag newIntegerList(int... pValues) {
        ListTag listtag = new ListTag();

        for (int i : pValues) {
            listtag.add(IntTag.valueOf(i));
        }

        return listtag;
    }

    private ListTag newDoubleList(double... pValues) {
        ListTag listtag = new ListTag();

        for (double d0 : pValues) {
            listtag.add(DoubleTag.valueOf(d0));
        }

        return listtag;
    }

    public static JigsawBlockEntity.JointType getJointType(CompoundTag pTag, BlockState pState) {
        return JigsawBlockEntity.JointType.CODEC
            .byName(
                pTag.getString("joint"),
                () -> JigsawBlock.getFrontFacing(pState).getAxis().isHorizontal() ? JigsawBlockEntity.JointType.ALIGNED : JigsawBlockEntity.JointType.ROLLABLE
            );
    }

    public static record JigsawBlockInfo(
        StructureTemplate.StructureBlockInfo info,
        JigsawBlockEntity.JointType jointType,
        ResourceLocation name,
        ResourceLocation pool,
        ResourceLocation target,
        int placementPriority,
        int selectionPriority
    ) {
        public static StructureTemplate.JigsawBlockInfo of(StructureTemplate.StructureBlockInfo pStructureBlockInfo) {
            CompoundTag compoundtag = Objects.requireNonNull(pStructureBlockInfo.nbt(), () -> pStructureBlockInfo + " nbt was null");
            return new StructureTemplate.JigsawBlockInfo(
                pStructureBlockInfo,
                StructureTemplate.getJointType(compoundtag, pStructureBlockInfo.state()),
                ResourceLocation.parse(compoundtag.getString("name")),
                ResourceLocation.parse(compoundtag.getString("pool")),
                ResourceLocation.parse(compoundtag.getString("target")),
                compoundtag.getInt("placement_priority"),
                compoundtag.getInt("selection_priority")
            );
        }

        @Override
        public String toString() {
            return String.format(
                Locale.ROOT,
                "<JigsawBlockInfo | %s | %s | name: %s | pool: %s | target: %s | placement: %d | selection: %d | %s>",
                this.info.pos,
                this.info.state,
                this.name,
                this.pool,
                this.target,
                this.placementPriority,
                this.selectionPriority,
                this.info.nbt
            );
        }

        public StructureTemplate.JigsawBlockInfo withInfo(StructureTemplate.StructureBlockInfo pInfo) {
            return new StructureTemplate.JigsawBlockInfo(
                pInfo, this.jointType, this.name, this.pool, this.target, this.placementPriority, this.selectionPriority
            );
        }
    }

    public static final class Palette {
        private final List<StructureTemplate.StructureBlockInfo> blocks;
        private final Map<Block, List<StructureTemplate.StructureBlockInfo>> cache = Maps.newHashMap();
        @Nullable
        private List<StructureTemplate.JigsawBlockInfo> cachedJigsaws;

        Palette(List<StructureTemplate.StructureBlockInfo> pBlocks) {
            this.blocks = pBlocks;
        }

        public List<StructureTemplate.JigsawBlockInfo> jigsaws() {
            if (this.cachedJigsaws == null) {
                this.cachedJigsaws = this.blocks(Blocks.JIGSAW).stream().map(StructureTemplate.JigsawBlockInfo::of).toList();
            }

            return this.cachedJigsaws;
        }

        public List<StructureTemplate.StructureBlockInfo> blocks() {
            return this.blocks;
        }

        public List<StructureTemplate.StructureBlockInfo> blocks(Block pBlock) {
            return this.cache
                .computeIfAbsent(
                    pBlock, p_74659_ -> this.blocks.stream().filter(p_163818_ -> p_163818_.state.is(p_74659_)).collect(Collectors.toList())
                );
        }
    }

    static class SimplePalette implements Iterable<BlockState> {
        public static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.defaultBlockState();
        private final IdMapper<BlockState> ids = new IdMapper<>(16);
        private int lastId;

        public int idFor(BlockState pState) {
            int i = this.ids.getId(pState);
            if (i == -1) {
                i = this.lastId++;
                this.ids.addMapping(pState, i);
            }

            return i;
        }

        @Nullable
        public BlockState stateFor(int pId) {
            BlockState blockstate = this.ids.byId(pId);
            return blockstate == null ? DEFAULT_BLOCK_STATE : blockstate;
        }

        @Override
        public Iterator<BlockState> iterator() {
            return this.ids.iterator();
        }

        public void addMapping(BlockState pState, int pId) {
            this.ids.addMapping(pState, pId);
        }
    }

    public static record StructureBlockInfo(BlockPos pos, BlockState state, @Nullable CompoundTag nbt) {
        @Override
        public String toString() {
            return String.format(Locale.ROOT, "<StructureBlockInfo | %s | %s | %s>", this.pos, this.state, this.nbt);
        }
    }

    public static class StructureEntityInfo {
        public final Vec3 pos;
        public final BlockPos blockPos;
        public final CompoundTag nbt;

        public StructureEntityInfo(Vec3 pPos, BlockPos pBlockPos, CompoundTag pNbt) {
            this.pos = pPos;
            this.blockPos = pBlockPos;
            this.nbt = pNbt;
        }
    }
}