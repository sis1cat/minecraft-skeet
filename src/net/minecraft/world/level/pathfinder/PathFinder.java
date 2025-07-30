package net.minecraft.world.level.pathfinder;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;

public class PathFinder {
    private static final float FUDGING = 1.5F;
    private final Node[] neighbors = new Node[32];
    private int maxVisitedNodes;
    private final NodeEvaluator nodeEvaluator;
    private static final boolean DEBUG = false;
    private final BinaryHeap openSet = new BinaryHeap();

    public PathFinder(NodeEvaluator pNodeEvaluator, int pMaxVisitedNodes) {
        this.nodeEvaluator = pNodeEvaluator;
        this.maxVisitedNodes = pMaxVisitedNodes;
    }

    public void setMaxVisitedNodes(int pMaxVisitedNodes) {
        this.maxVisitedNodes = pMaxVisitedNodes;
    }

    @Nullable
    public Path findPath(PathNavigationRegion pRegion, Mob pMob, Set<BlockPos> pTargetPositions, float pMaxRange, int pAccuracy, float pSearchDepthMultiplier) {
        this.openSet.clear();
        this.nodeEvaluator.prepare(pRegion, pMob);
        Node node = this.nodeEvaluator.getStart();
        if (node == null) {
            return null;
        } else {
            Map<Target, BlockPos> map = pTargetPositions.stream()
                .collect(
                    Collectors.toMap(
                        p_327511_ -> this.nodeEvaluator.getTarget((double)p_327511_.getX(), (double)p_327511_.getY(), (double)p_327511_.getZ()),
                        Function.identity()
                    )
                );
            Path path = this.findPath(node, map, pMaxRange, pAccuracy, pSearchDepthMultiplier);
            this.nodeEvaluator.done();
            return path;
        }
    }

    @Nullable
    private Path findPath(Node pNode, Map<Target, BlockPos> pTargetPositions, float pMaxRange, int pAccuracy, float pSearchDepthMultiplier) {
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("find_path");
        profilerfiller.markForCharting(MetricCategory.PATH_FINDING);
        Set<Target> set = pTargetPositions.keySet();
        pNode.g = 0.0F;
        pNode.h = this.getBestH(pNode, set);
        pNode.f = pNode.h;
        this.openSet.clear();
        this.openSet.insert(pNode);
        Set<Node> set1 = ImmutableSet.of();
        int i = 0;
        Set<Target> set2 = Sets.newHashSetWithExpectedSize(set.size());
        int j = (int)((float)this.maxVisitedNodes * pSearchDepthMultiplier);

        while (!this.openSet.isEmpty()) {
            if (++i >= j) {
                break;
            }

            Node node = this.openSet.pop();
            node.closed = true;

            for (Target target : set) {
                if (node.distanceManhattan(target) <= (float)pAccuracy) {
                    target.setReached();
                    set2.add(target);
                }
            }

            if (!set2.isEmpty()) {
                break;
            }

            if (!(node.distanceTo(pNode) >= pMaxRange)) {
                int k = this.nodeEvaluator.getNeighbors(this.neighbors, node);

                for (int l = 0; l < k; l++) {
                    Node node1 = this.neighbors[l];
                    float f = this.distance(node, node1);
                    node1.walkedDistance = node.walkedDistance + f;
                    float f1 = node.g + f + node1.costMalus;
                    if (node1.walkedDistance < pMaxRange && (!node1.inOpenSet() || f1 < node1.g)) {
                        node1.cameFrom = node;
                        node1.g = f1;
                        node1.h = this.getBestH(node1, set) * 1.5F;
                        if (node1.inOpenSet()) {
                            this.openSet.changeCost(node1, node1.g + node1.h);
                        } else {
                            node1.f = node1.g + node1.h;
                            this.openSet.insert(node1);
                        }
                    }
                }
            }
        }

        Optional<Path> optional = !set2.isEmpty()
            ? set2.stream().map(p_77454_ -> this.reconstructPath(p_77454_.getBestNode(), pTargetPositions.get(p_77454_), true)).min(Comparator.comparingInt(Path::getNodeCount))
            : set.stream()
                .map(p_77451_ -> this.reconstructPath(p_77451_.getBestNode(), pTargetPositions.get(p_77451_), false))
                .min(Comparator.comparingDouble(Path::getDistToTarget).thenComparingInt(Path::getNodeCount));
        profilerfiller.pop();
        return optional.isEmpty() ? null : optional.get();
    }

    protected float distance(Node pFirst, Node pSecond) {
        return pFirst.distanceTo(pSecond);
    }

    private float getBestH(Node pNode, Set<Target> pTargets) {
        float f = Float.MAX_VALUE;

        for (Target target : pTargets) {
            float f1 = pNode.distanceTo(target);
            target.updateBest(f1, pNode);
            f = Math.min(f1, f);
        }

        return f;
    }

    private Path reconstructPath(Node pPoint, BlockPos pTargetPos, boolean pReachesTarget) {
        List<Node> list = Lists.newArrayList();
        Node node = pPoint;
        list.add(0, pPoint);

        while (node.cameFrom != null) {
            node = node.cameFrom;
            list.add(0, node);
        }

        return new Path(list, pTargetPos, pReachesTarget);
    }
}