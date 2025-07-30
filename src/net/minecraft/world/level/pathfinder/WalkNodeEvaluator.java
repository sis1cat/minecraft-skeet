package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WalkNodeEvaluator extends NodeEvaluator {
    public static final double SPACE_BETWEEN_WALL_POSTS = 0.5;
    private static final double DEFAULT_MOB_JUMP_HEIGHT = 1.125;
    private final Long2ObjectMap<PathType> pathTypesByPosCacheByMob = new Long2ObjectOpenHashMap<>();
    private final Object2BooleanMap<AABB> collisionCache = new Object2BooleanOpenHashMap<>();
    private final Node[] reusableNeighbors = new Node[Direction.Plane.HORIZONTAL.length()];

    @Override
    public void prepare(PathNavigationRegion p_77620_, Mob p_77621_) {
        super.prepare(p_77620_, p_77621_);
        p_77621_.onPathfindingStart();
    }

    @Override
    public void done() {
        this.mob.onPathfindingDone();
        this.pathTypesByPosCacheByMob.clear();
        this.collisionCache.clear();
        super.done();
    }

    @Override
    public Node getStart() {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        int i = this.mob.getBlockY();
        BlockState blockstate = this.currentContext.getBlockState(blockpos$mutableblockpos.set(this.mob.getX(), (double)i, this.mob.getZ()));
        if (!this.mob.canStandOnFluid(blockstate.getFluidState())) {
            if (this.canFloat() && this.mob.isInWater()) {
                while (true) {
                    if (!blockstate.is(Blocks.WATER) && blockstate.getFluidState() != Fluids.WATER.getSource(false)) {
                        i--;
                        break;
                    }

                    blockstate = this.currentContext
                        .getBlockState(blockpos$mutableblockpos.set(this.mob.getX(), (double)(++i), this.mob.getZ()));
                }
            } else if (this.mob.onGround()) {
                i = Mth.floor(this.mob.getY() + 0.5);
            } else {
                blockpos$mutableblockpos.set(this.mob.getX(), this.mob.getY() + 1.0, this.mob.getZ());

                while (blockpos$mutableblockpos.getY() > this.currentContext.level().getMinY()) {
                    i = blockpos$mutableblockpos.getY();
                    blockpos$mutableblockpos.setY(blockpos$mutableblockpos.getY() - 1);
                    BlockState blockstate1 = this.currentContext.getBlockState(blockpos$mutableblockpos);
                    if (!blockstate1.isAir() && !blockstate1.isPathfindable(PathComputationType.LAND)) {
                        break;
                    }
                }
            }
        } else {
            while (this.mob.canStandOnFluid(blockstate.getFluidState())) {
                blockstate = this.currentContext.getBlockState(blockpos$mutableblockpos.set(this.mob.getX(), (double)(++i), this.mob.getZ()));
            }

            i--;
        }

        BlockPos blockpos = this.mob.blockPosition();
        if (!this.canStartAt(blockpos$mutableblockpos.set(blockpos.getX(), i, blockpos.getZ()))) {
            AABB aabb = this.mob.getBoundingBox();
            if (this.canStartAt(blockpos$mutableblockpos.set(aabb.minX, (double)i, aabb.minZ))
                || this.canStartAt(blockpos$mutableblockpos.set(aabb.minX, (double)i, aabb.maxZ))
                || this.canStartAt(blockpos$mutableblockpos.set(aabb.maxX, (double)i, aabb.minZ))
                || this.canStartAt(blockpos$mutableblockpos.set(aabb.maxX, (double)i, aabb.maxZ))) {
                return this.getStartNode(blockpos$mutableblockpos);
            }
        }

        return this.getStartNode(new BlockPos(blockpos.getX(), i, blockpos.getZ()));
    }

    protected Node getStartNode(BlockPos pPos) {
        Node node = this.getNode(pPos);
        node.type = this.getCachedPathType(node.x, node.y, node.z);
        node.costMalus = this.mob.getPathfindingMalus(node.type);
        return node;
    }

    protected boolean canStartAt(BlockPos pPos) {
        PathType pathtype = this.getCachedPathType(pPos.getX(), pPos.getY(), pPos.getZ());
        return pathtype != PathType.OPEN && this.mob.getPathfindingMalus(pathtype) >= 0.0F;
    }

    @Override
    public Target getTarget(double p_334058_, double p_329070_, double p_328068_) {
        return this.getTargetNodeAt(p_334058_, p_329070_, p_328068_);
    }

    @Override
    public int getNeighbors(Node[] p_77640_, Node p_77641_) {
        int i = 0;
        int j = 0;
        PathType pathtype = this.getCachedPathType(p_77641_.x, p_77641_.y + 1, p_77641_.z);
        PathType pathtype1 = this.getCachedPathType(p_77641_.x, p_77641_.y, p_77641_.z);
        if (this.mob.getPathfindingMalus(pathtype) >= 0.0F && pathtype1 != PathType.STICKY_HONEY) {
            j = Mth.floor(Math.max(1.0F, this.mob.maxUpStep()));
        }

        double d0 = this.getFloorLevel(new BlockPos(p_77641_.x, p_77641_.y, p_77641_.z));

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Node node = this.findAcceptedNode(
                p_77641_.x + direction.getStepX(), p_77641_.y, p_77641_.z + direction.getStepZ(), j, d0, direction, pathtype1
            );
            this.reusableNeighbors[direction.get2DDataValue()] = node;
            if (this.isNeighborValid(node, p_77641_)) {
                p_77640_[i++] = node;
            }
        }

        for (Direction direction1 : Direction.Plane.HORIZONTAL) {
            Direction direction2 = direction1.getClockWise();
            if (this.isDiagonalValid(p_77641_, this.reusableNeighbors[direction1.get2DDataValue()], this.reusableNeighbors[direction2.get2DDataValue()])) {
                Node node1 = this.findAcceptedNode(
                    p_77641_.x + direction1.getStepX() + direction2.getStepX(),
                    p_77641_.y,
                    p_77641_.z + direction1.getStepZ() + direction2.getStepZ(),
                    j,
                    d0,
                    direction1,
                    pathtype1
                );
                if (this.isDiagonalValid(node1)) {
                    p_77640_[i++] = node1;
                }
            }
        }

        return i;
    }

    protected boolean isNeighborValid(@Nullable Node pNeighbor, Node pNode) {
        return pNeighbor != null && !pNeighbor.closed && (pNeighbor.costMalus >= 0.0F || pNode.costMalus < 0.0F);
    }

    protected boolean isDiagonalValid(Node pRoot, @Nullable Node pXNode, @Nullable Node pZNode) {
        if (pZNode == null || pXNode == null || pZNode.y > pRoot.y || pXNode.y > pRoot.y) {
            return false;
        } else if (pXNode.type != PathType.WALKABLE_DOOR && pZNode.type != PathType.WALKABLE_DOOR) {
            boolean flag = pZNode.type == PathType.FENCE && pXNode.type == PathType.FENCE && (double)this.mob.getBbWidth() < 0.5;
            return (pZNode.y < pRoot.y || pZNode.costMalus >= 0.0F || flag)
                && (pXNode.y < pRoot.y || pXNode.costMalus >= 0.0F || flag);
        } else {
            return false;
        }
    }

    protected boolean isDiagonalValid(@Nullable Node pNode) {
        if (pNode == null || pNode.closed) {
            return false;
        } else {
            return pNode.type == PathType.WALKABLE_DOOR ? false : pNode.costMalus >= 0.0F;
        }
    }

    private static boolean doesBlockHavePartialCollision(PathType pPathType) {
        return pPathType == PathType.FENCE || pPathType == PathType.DOOR_WOOD_CLOSED || pPathType == PathType.DOOR_IRON_CLOSED;
    }

    private boolean canReachWithoutCollision(Node pNode) {
        AABB aabb = this.mob.getBoundingBox();
        Vec3 vec3 = new Vec3(
            (double)pNode.x - this.mob.getX() + aabb.getXsize() / 2.0,
            (double)pNode.y - this.mob.getY() + aabb.getYsize() / 2.0,
            (double)pNode.z - this.mob.getZ() + aabb.getZsize() / 2.0
        );
        int i = Mth.ceil(vec3.length() / aabb.getSize());
        vec3 = vec3.scale((double)(1.0F / (float)i));

        for (int j = 1; j <= i; j++) {
            aabb = aabb.move(vec3);
            if (this.hasCollisions(aabb)) {
                return false;
            }
        }

        return true;
    }

    protected double getFloorLevel(BlockPos pPos) {
        BlockGetter blockgetter = this.currentContext.level();
        return (this.canFloat() || this.isAmphibious()) && blockgetter.getFluidState(pPos).is(FluidTags.WATER)
            ? (double)pPos.getY() + 0.5
            : getFloorLevel(blockgetter, pPos);
    }

    public static double getFloorLevel(BlockGetter pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.below();
        VoxelShape voxelshape = pLevel.getBlockState(blockpos).getCollisionShape(pLevel, blockpos);
        return (double)blockpos.getY() + (voxelshape.isEmpty() ? 0.0 : voxelshape.max(Direction.Axis.Y));
    }

    protected boolean isAmphibious() {
        return false;
    }

    @Nullable
    protected Node findAcceptedNode(int pX, int pY, int pZ, int pVerticalDeltaLimit, double pNodeFloorLevel, Direction pDirection, PathType pPathType) {
        Node node = null;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        double d0 = this.getFloorLevel(blockpos$mutableblockpos.set(pX, pY, pZ));
        if (d0 - pNodeFloorLevel > this.getMobJumpHeight()) {
            return null;
        } else {
            PathType pathtype = this.getCachedPathType(pX, pY, pZ);
            float f = this.mob.getPathfindingMalus(pathtype);
            if (f >= 0.0F) {
                node = this.getNodeAndUpdateCostToMax(pX, pY, pZ, pathtype, f);
            }

            if (doesBlockHavePartialCollision(pPathType) && node != null && node.costMalus >= 0.0F && !this.canReachWithoutCollision(node)) {
                node = null;
            }

            if (pathtype != PathType.WALKABLE && (!this.isAmphibious() || pathtype != PathType.WATER)) {
                if ((node == null || node.costMalus < 0.0F)
                    && pVerticalDeltaLimit > 0
                    && (pathtype != PathType.FENCE || this.canWalkOverFences())
                    && pathtype != PathType.UNPASSABLE_RAIL
                    && pathtype != PathType.TRAPDOOR
                    && pathtype != PathType.POWDER_SNOW) {
                    node = this.tryJumpOn(pX, pY, pZ, pVerticalDeltaLimit, pNodeFloorLevel, pDirection, pPathType, blockpos$mutableblockpos);
                } else if (!this.isAmphibious() && pathtype == PathType.WATER && !this.canFloat()) {
                    node = this.tryFindFirstNonWaterBelow(pX, pY, pZ, node);
                } else if (pathtype == PathType.OPEN) {
                    node = this.tryFindFirstGroundNodeBelow(pX, pY, pZ);
                } else if (doesBlockHavePartialCollision(pathtype) && node == null) {
                    node = this.getClosedNode(pX, pY, pZ, pathtype);
                }

                return node;
            } else {
                return node;
            }
        }
    }

    private double getMobJumpHeight() {
        return Math.max(1.125, (double)this.mob.maxUpStep());
    }

    private Node getNodeAndUpdateCostToMax(int pX, int pY, int pZ, PathType pPathType, float pMalus) {
        Node node = this.getNode(pX, pY, pZ);
        node.type = pPathType;
        node.costMalus = Math.max(node.costMalus, pMalus);
        return node;
    }

    private Node getBlockedNode(int pX, int pY, int pZ) {
        Node node = this.getNode(pX, pY, pZ);
        node.type = PathType.BLOCKED;
        node.costMalus = -1.0F;
        return node;
    }

    private Node getClosedNode(int pX, int pY, int pZ, PathType pPathType) {
        Node node = this.getNode(pX, pY, pZ);
        node.closed = true;
        node.type = pPathType;
        node.costMalus = pPathType.getMalus();
        return node;
    }

    @Nullable
    private Node tryJumpOn(
        int pX,
        int pY,
        int pZ,
        int pVerticalDeltaLimit,
        double pNodeFloorLevel,
        Direction pDirection,
        PathType pPathType,
        BlockPos.MutableBlockPos pPos
    ) {
        Node node = this.findAcceptedNode(pX, pY + 1, pZ, pVerticalDeltaLimit - 1, pNodeFloorLevel, pDirection, pPathType);
        if (node == null) {
            return null;
        } else if (this.mob.getBbWidth() >= 1.0F) {
            return node;
        } else if (node.type != PathType.OPEN && node.type != PathType.WALKABLE) {
            return node;
        } else {
            double d0 = (double)(pX - pDirection.getStepX()) + 0.5;
            double d1 = (double)(pZ - pDirection.getStepZ()) + 0.5;
            double d2 = (double)this.mob.getBbWidth() / 2.0;
            AABB aabb = new AABB(
                d0 - d2,
                this.getFloorLevel(pPos.set(d0, (double)(pY + 1), d1)) + 0.001,
                d1 - d2,
                d0 + d2,
                (double)this.mob.getBbHeight()
                    + this.getFloorLevel(pPos.set((double)node.x, (double)node.y, (double)node.z))
                    - 0.002,
                d1 + d2
            );
            return this.hasCollisions(aabb) ? null : node;
        }
    }

    @Nullable
    private Node tryFindFirstNonWaterBelow(int pX, int pY, int pZ, @Nullable Node pNode) {
        pY--;

        while (pY > this.mob.level().getMinY()) {
            PathType pathtype = this.getCachedPathType(pX, pY, pZ);
            if (pathtype != PathType.WATER) {
                return pNode;
            }

            pNode = this.getNodeAndUpdateCostToMax(pX, pY, pZ, pathtype, this.mob.getPathfindingMalus(pathtype));
            pY--;
        }

        return pNode;
    }

    private Node tryFindFirstGroundNodeBelow(int pX, int pY, int pZ) {
        for (int i = pY - 1; i >= this.mob.level().getMinY(); i--) {
            if (pY - i > this.mob.getMaxFallDistance()) {
                return this.getBlockedNode(pX, i, pZ);
            }

            PathType pathtype = this.getCachedPathType(pX, i, pZ);
            float f = this.mob.getPathfindingMalus(pathtype);
            if (pathtype != PathType.OPEN) {
                if (f >= 0.0F) {
                    return this.getNodeAndUpdateCostToMax(pX, i, pZ, pathtype, f);
                }

                return this.getBlockedNode(pX, i, pZ);
            }
        }

        return this.getBlockedNode(pX, pY, pZ);
    }

    private boolean hasCollisions(AABB pBoundingBox) {
        return this.collisionCache.computeIfAbsent(pBoundingBox, p_327517_ -> !this.currentContext.level().noCollision(this.mob, pBoundingBox));
    }

    protected PathType getCachedPathType(int pX, int pY, int pZ) {
        return this.pathTypesByPosCacheByMob
            .computeIfAbsent(
                BlockPos.asLong(pX, pY, pZ),
                p_327521_ -> this.getPathTypeOfMob(this.currentContext, pX, pY, pZ, this.mob)
            );
    }

    @Override
    public PathType getPathTypeOfMob(PathfindingContext p_336212_, int p_330284_, int p_332224_, int p_335362_, Mob p_327680_) {
        Set<PathType> set = this.getPathTypeWithinMobBB(p_336212_, p_330284_, p_332224_, p_335362_);
        if (set.contains(PathType.FENCE)) {
            return PathType.FENCE;
        } else if (set.contains(PathType.UNPASSABLE_RAIL)) {
            return PathType.UNPASSABLE_RAIL;
        } else {
            PathType pathtype = PathType.BLOCKED;

            for (PathType pathtype1 : set) {
                if (p_327680_.getPathfindingMalus(pathtype1) < 0.0F) {
                    return pathtype1;
                }

                if (p_327680_.getPathfindingMalus(pathtype1) >= p_327680_.getPathfindingMalus(pathtype)) {
                    pathtype = pathtype1;
                }
            }

            return this.entityWidth <= 1
                    && pathtype != PathType.OPEN
                    && p_327680_.getPathfindingMalus(pathtype) == 0.0F
                    && this.getPathType(p_336212_, p_330284_, p_332224_, p_335362_) == PathType.OPEN
                ? PathType.OPEN
                : pathtype;
        }
    }

    public Set<PathType> getPathTypeWithinMobBB(PathfindingContext pContext, int pX, int pY, int pZ) {
        EnumSet<PathType> enumset = EnumSet.noneOf(PathType.class);

        for (int i = 0; i < this.entityWidth; i++) {
            for (int j = 0; j < this.entityHeight; j++) {
                for (int k = 0; k < this.entityDepth; k++) {
                    int l = i + pX;
                    int i1 = j + pY;
                    int j1 = k + pZ;
                    PathType pathtype = this.getPathType(pContext, l, i1, j1);
                    BlockPos blockpos = this.mob.blockPosition();
                    boolean flag = this.canPassDoors();
                    if (pathtype == PathType.DOOR_WOOD_CLOSED && this.canOpenDoors() && flag) {
                        pathtype = PathType.WALKABLE_DOOR;
                    }

                    if (pathtype == PathType.DOOR_OPEN && !flag) {
                        pathtype = PathType.BLOCKED;
                    }

                    if (pathtype == PathType.RAIL
                        && this.getPathType(pContext, blockpos.getX(), blockpos.getY(), blockpos.getZ()) != PathType.RAIL
                        && this.getPathType(pContext, blockpos.getX(), blockpos.getY() - 1, blockpos.getZ()) != PathType.RAIL) {
                        pathtype = PathType.UNPASSABLE_RAIL;
                    }

                    enumset.add(pathtype);
                }
            }
        }

        return enumset;
    }

    @Override
    public PathType getPathType(PathfindingContext p_333098_, int p_327758_, int p_329863_, int p_328680_) {
        return getPathTypeStatic(p_333098_, new BlockPos.MutableBlockPos(p_327758_, p_329863_, p_328680_));
    }

    public static PathType getPathTypeStatic(Mob pMob, BlockPos pPos) {
        return getPathTypeStatic(new PathfindingContext(pMob.level(), pMob), pPos.mutable());
    }

    public static PathType getPathTypeStatic(PathfindingContext pContext, BlockPos.MutableBlockPos pPos) {
        int i = pPos.getX();
        int j = pPos.getY();
        int k = pPos.getZ();
        PathType pathtype = pContext.getPathTypeFromState(i, j, k);
        if (pathtype == PathType.OPEN && j >= pContext.level().getMinY() + 1) {
            return switch (pContext.getPathTypeFromState(i, j - 1, k)) {
                case OPEN, WATER, LAVA, WALKABLE -> PathType.OPEN;
                case DAMAGE_FIRE -> PathType.DAMAGE_FIRE;
                case DAMAGE_OTHER -> PathType.DAMAGE_OTHER;
                case STICKY_HONEY -> PathType.STICKY_HONEY;
                case POWDER_SNOW -> PathType.DANGER_POWDER_SNOW;
                case DAMAGE_CAUTIOUS -> PathType.DAMAGE_CAUTIOUS;
                case TRAPDOOR -> PathType.DANGER_TRAPDOOR;
                default -> checkNeighbourBlocks(pContext, i, j, k, PathType.WALKABLE);
            };
        } else {
            return pathtype;
        }
    }

    public static PathType checkNeighbourBlocks(PathfindingContext pContext, int pX, int pY, int pZ, PathType pPathType) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    if (i != 0 || k != 0) {
                        PathType pathtype = pContext.getPathTypeFromState(pX + i, pY + j, pZ + k);
                        if (pathtype == PathType.DAMAGE_OTHER) {
                            return PathType.DANGER_OTHER;
                        }

                        if (pathtype == PathType.DAMAGE_FIRE || pathtype == PathType.LAVA) {
                            return PathType.DANGER_FIRE;
                        }

                        if (pathtype == PathType.WATER) {
                            return PathType.WATER_BORDER;
                        }

                        if (pathtype == PathType.DAMAGE_CAUTIOUS) {
                            return PathType.DAMAGE_CAUTIOUS;
                        }
                    }
                }
            }
        }

        return pPathType;
    }

    protected static PathType getPathTypeFromState(BlockGetter pLevel, BlockPos pPos) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        Block block = blockstate.getBlock();
        if (blockstate.isAir()) {
            return PathType.OPEN;
        } else if (blockstate.is(BlockTags.TRAPDOORS) || blockstate.is(Blocks.LILY_PAD) || blockstate.is(Blocks.BIG_DRIPLEAF)) {
            return PathType.TRAPDOOR;
        } else if (blockstate.is(Blocks.POWDER_SNOW)) {
            return PathType.POWDER_SNOW;
        } else if (blockstate.is(Blocks.CACTUS) || blockstate.is(Blocks.SWEET_BERRY_BUSH)) {
            return PathType.DAMAGE_OTHER;
        } else if (blockstate.is(Blocks.HONEY_BLOCK)) {
            return PathType.STICKY_HONEY;
        } else if (blockstate.is(Blocks.COCOA)) {
            return PathType.COCOA;
        } else if (!blockstate.is(Blocks.WITHER_ROSE) && !blockstate.is(Blocks.POINTED_DRIPSTONE)) {
            FluidState fluidstate = blockstate.getFluidState();
            if (fluidstate.is(FluidTags.LAVA)) {
                return PathType.LAVA;
            } else if (isBurningBlock(blockstate)) {
                return PathType.DAMAGE_FIRE;
            } else if (block instanceof DoorBlock doorblock) {
                if (blockstate.getValue(DoorBlock.OPEN)) {
                    return PathType.DOOR_OPEN;
                } else {
                    return doorblock.type().canOpenByHand() ? PathType.DOOR_WOOD_CLOSED : PathType.DOOR_IRON_CLOSED;
                }
            } else if (block instanceof BaseRailBlock) {
                return PathType.RAIL;
            } else if (block instanceof LeavesBlock) {
                return PathType.LEAVES;
            } else if (!blockstate.is(BlockTags.FENCES)
                && !blockstate.is(BlockTags.WALLS)
                && (!(block instanceof FenceGateBlock) || blockstate.getValue(FenceGateBlock.OPEN))) {
                if (!blockstate.isPathfindable(PathComputationType.LAND)) {
                    return PathType.BLOCKED;
                } else {
                    return fluidstate.is(FluidTags.WATER) ? PathType.WATER : PathType.OPEN;
                }
            } else {
                return PathType.FENCE;
            }
        } else {
            return PathType.DAMAGE_CAUTIOUS;
        }
    }
}