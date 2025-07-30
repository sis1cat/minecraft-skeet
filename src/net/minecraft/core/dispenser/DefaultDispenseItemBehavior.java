package net.minecraft.core.dispenser;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class DefaultDispenseItemBehavior implements DispenseItemBehavior {
    private static final int DEFAULT_ACCURACY = 6;

    @Override
    public final ItemStack dispense(BlockSource p_123391_, ItemStack p_123392_) {
        ItemStack itemstack = this.execute(p_123391_, p_123392_);
        this.playSound(p_123391_);
        this.playAnimation(p_123391_, p_123391_.state().getValue(DispenserBlock.FACING));
        return itemstack;
    }

    protected ItemStack execute(BlockSource pBlockSource, ItemStack pItem) {
        Direction direction = pBlockSource.state().getValue(DispenserBlock.FACING);
        Position position = DispenserBlock.getDispensePosition(pBlockSource);
        ItemStack itemstack = pItem.split(1);
        spawnItem(pBlockSource.level(), itemstack, 6, direction, position);
        return pItem;
    }

    public static void spawnItem(Level pLevel, ItemStack pStack, int pSpeed, Direction pFacing, Position pPosition) {
        double d0 = pPosition.x();
        double d1 = pPosition.y();
        double d2 = pPosition.z();
        if (pFacing.getAxis() == Direction.Axis.Y) {
            d1 -= 0.125;
        } else {
            d1 -= 0.15625;
        }

        ItemEntity itementity = new ItemEntity(pLevel, d0, d1, d2, pStack);
        double d3 = pLevel.random.nextDouble() * 0.1 + 0.2;
        itementity.setDeltaMovement(
            pLevel.random.triangle((double)pFacing.getStepX() * d3, 0.0172275 * (double)pSpeed),
            pLevel.random.triangle(0.2, 0.0172275 * (double)pSpeed),
            pLevel.random.triangle((double)pFacing.getStepZ() * d3, 0.0172275 * (double)pSpeed)
        );
        pLevel.addFreshEntity(itementity);
    }

    protected void playSound(BlockSource pBlockSource) {
        playDefaultSound(pBlockSource);
    }

    protected void playAnimation(BlockSource pBlockSource, Direction pDirection) {
        playDefaultAnimation(pBlockSource, pDirection);
    }

    private static void playDefaultSound(BlockSource pBlockSource) {
        pBlockSource.level().levelEvent(1000, pBlockSource.pos(), 0);
    }

    private static void playDefaultAnimation(BlockSource pBlockSource, Direction pDirection) {
        pBlockSource.level().levelEvent(2000, pBlockSource.pos(), pDirection.get3DDataValue());
    }

    protected ItemStack consumeWithRemainder(BlockSource pBlockSource, ItemStack pStack, ItemStack pRemainder) {
        pStack.shrink(1);
        if (pStack.isEmpty()) {
            return pRemainder;
        } else {
            this.addToInventoryOrDispense(pBlockSource, pRemainder);
            return pStack;
        }
    }

    private void addToInventoryOrDispense(BlockSource pBlockSource, ItemStack pRemainder) {
        ItemStack itemstack = pBlockSource.blockEntity().insertItem(pRemainder);
        if (!itemstack.isEmpty()) {
            Direction direction = pBlockSource.state().getValue(DispenserBlock.FACING);
            spawnItem(pBlockSource.level(), itemstack, 6, direction, DispenserBlock.getDispensePosition(pBlockSource));
            playDefaultSound(pBlockSource);
            playDefaultAnimation(pBlockSource, direction);
        }
    }
}