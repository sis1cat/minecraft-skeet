package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class AlterGroundDecorator extends TreeDecorator {
    public static final MapCodec<AlterGroundDecorator> CODEC = BlockStateProvider.CODEC
        .fieldOf("provider")
        .xmap(AlterGroundDecorator::new, p_69327_ -> p_69327_.provider);
    private final BlockStateProvider provider;

    public AlterGroundDecorator(BlockStateProvider pProvider) {
        this.provider = pProvider;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.ALTER_GROUND;
    }

    @Override
    public void place(TreeDecorator.Context p_225969_) {
        List<BlockPos> list = Lists.newArrayList();
        List<BlockPos> list1 = p_225969_.roots();
        List<BlockPos> list2 = p_225969_.logs();
        if (list1.isEmpty()) {
            list.addAll(list2);
        } else if (!list2.isEmpty() && list1.get(0).getY() == list2.get(0).getY()) {
            list.addAll(list2);
            list.addAll(list1);
        } else {
            list.addAll(list1);
        }

        if (!list.isEmpty()) {
            int i = list.get(0).getY();
            list.stream().filter(p_69310_ -> p_69310_.getY() == i).forEach(p_225978_ -> {
                this.placeCircle(p_225969_, p_225978_.west().north());
                this.placeCircle(p_225969_, p_225978_.east(2).north());
                this.placeCircle(p_225969_, p_225978_.west().south(2));
                this.placeCircle(p_225969_, p_225978_.east(2).south(2));

                for (int j = 0; j < 5; j++) {
                    int k = p_225969_.random().nextInt(64);
                    int l = k % 8;
                    int i1 = k / 8;
                    if (l == 0 || l == 7 || i1 == 0 || i1 == 7) {
                        this.placeCircle(p_225969_, p_225978_.offset(-3 + l, 0, -3 + i1));
                    }
                }
            });
        }
    }

    private void placeCircle(TreeDecorator.Context pContext, BlockPos pPos) {
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i) != 2 || Math.abs(j) != 2) {
                    this.placeBlockAt(pContext, pPos.offset(i, 0, j));
                }
            }
        }
    }

    private void placeBlockAt(TreeDecorator.Context pContext, BlockPos pPos) {
        for (int i = 2; i >= -3; i--) {
            BlockPos blockpos = pPos.above(i);
            if (Feature.isGrassOrDirt(pContext.level(), blockpos)) {
                pContext.setBlock(blockpos, this.provider.getState(pContext.random(), pPos));
                break;
            }

            if (!pContext.isAir(blockpos) && i < 0) {
                break;
            }
        }
    }
}