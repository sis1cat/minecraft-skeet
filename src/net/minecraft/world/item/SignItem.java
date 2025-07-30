package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SignItem extends StandingAndWallBlockItem {
    public SignItem(Block pStandingBlock, Block pWallBlock, Item.Properties pProperties) {
        super(pStandingBlock, pWallBlock, Direction.DOWN, pProperties);
    }

    public SignItem(Item.Properties pProperties, Block pStandingBlock, Block pWallBlock, Direction pAttachmentDirection) {
        super(pStandingBlock, pWallBlock, pAttachmentDirection, pProperties);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pPos, Level pLevel, @Nullable Player pPlayer, ItemStack pStack, BlockState pState) {
        boolean flag = super.updateCustomBlockEntityTag(pPos, pLevel, pPlayer, pStack, pState);
        if (!pLevel.isClientSide
            && !flag
            && pPlayer != null
            && pLevel.getBlockEntity(pPos) instanceof SignBlockEntity signblockentity
            && pLevel.getBlockState(pPos).getBlock() instanceof SignBlock signblock) {
            signblock.openTextEdit(pPlayer, signblockentity, true);
        }

        return flag;
    }
}