package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class CartographyTableBlock extends Block {
    public static final MapCodec<CartographyTableBlock> CODEC = simpleCodec(CartographyTableBlock::new);
    private static final Component CONTAINER_TITLE = Component.translatable("container.cartography_table");

    @Override
    public MapCodec<CartographyTableBlock> codec() {
        return CODEC;
    }

    protected CartographyTableBlock(BlockBehaviour.Properties p_51349_) {
        super(p_51349_);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_51357_, Level p_51358_, BlockPos p_51359_, Player p_51360_, BlockHitResult p_51362_) {
        if (!p_51358_.isClientSide) {
            p_51360_.openMenu(p_51357_.getMenuProvider(p_51358_, p_51359_));
            p_51360_.awardStat(Stats.INTERACT_WITH_CARTOGRAPHY_TABLE);
        }

        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState pState, Level pLevel, BlockPos pPos) {
        return new SimpleMenuProvider(
            (p_51353_, p_51354_, p_51355_) -> new CartographyTableMenu(p_51353_, p_51354_, ContainerLevelAccess.create(pLevel, pPos)), CONTAINER_TITLE
        );
    }
}