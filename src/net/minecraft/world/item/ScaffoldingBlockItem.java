package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ScaffoldingBlockItem extends BlockItem {
    public ScaffoldingBlockItem(Block p_43060_, Item.Properties p_43061_) {
        super(p_43060_, p_43061_);
    }

    @Nullable
    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext pContext) {
        BlockPos blockpos = pContext.getClickedPos();
        Level level = pContext.getLevel();
        BlockState blockstate = level.getBlockState(blockpos);
        Block block = this.getBlock();
        if (!blockstate.is(block)) {
            return ScaffoldingBlock.getDistance(level, blockpos) == 7 ? null : pContext;
        } else {
            Direction direction;
            if (pContext.isSecondaryUseActive()) {
                direction = pContext.isInside() ? pContext.getClickedFace().getOpposite() : pContext.getClickedFace();
            } else {
                direction = pContext.getClickedFace() == Direction.UP ? pContext.getHorizontalDirection() : Direction.UP;
            }

            int i = 0;
            BlockPos.MutableBlockPos blockpos$mutableblockpos = blockpos.mutable().move(direction);

            while (i < 7) {
                if (!level.isClientSide && !level.isInWorldBounds(blockpos$mutableblockpos)) {
                    Player player = pContext.getPlayer();
                    int j = level.getMaxY();
                    if (player instanceof ServerPlayer && blockpos$mutableblockpos.getY() > j) {
                        ((ServerPlayer)player).sendSystemMessage(Component.translatable("build.tooHigh", j).withStyle(ChatFormatting.RED), true);
                    }
                    break;
                }

                blockstate = level.getBlockState(blockpos$mutableblockpos);
                if (!blockstate.is(this.getBlock())) {
                    if (blockstate.canBeReplaced(pContext)) {
                        return BlockPlaceContext.at(pContext, blockpos$mutableblockpos, direction);
                    }
                    break;
                }

                blockpos$mutableblockpos.move(direction);
                if (direction.getAxis().isHorizontal()) {
                    i++;
                }
            }

            return null;
        }
    }

    @Override
    protected boolean mustSurvive() {
        return false;
    }
}