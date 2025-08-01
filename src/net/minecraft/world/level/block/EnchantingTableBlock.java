package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EnchantingTableBlock extends BaseEntityBlock {
    public static final MapCodec<EnchantingTableBlock> CODEC = simpleCodec(EnchantingTableBlock::new);
    protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);
    public static final List<BlockPos> BOOKSHELF_OFFSETS = BlockPos.betweenClosedStream(-2, 0, -2, 2, 1, 2)
        .filter(p_328998_ -> Math.abs(p_328998_.getX()) == 2 || Math.abs(p_328998_.getZ()) == 2)
        .map(BlockPos::immutable)
        .toList();

    @Override
    public MapCodec<EnchantingTableBlock> codec() {
        return CODEC;
    }

    protected EnchantingTableBlock(BlockBehaviour.Properties p_333403_) {
        super(p_333403_);
    }

    public static boolean isValidBookShelf(Level pLevel, BlockPos pEnchantingTablePos, BlockPos pBookshelfPos) {
        return pLevel.getBlockState(pEnchantingTablePos.offset(pBookshelfPos)).is(BlockTags.ENCHANTMENT_POWER_PROVIDER)
            && pLevel.getBlockState(pEnchantingTablePos.offset(pBookshelfPos.getX() / 2, pBookshelfPos.getY(), pBookshelfPos.getZ() / 2)).is(BlockTags.ENCHANTMENT_POWER_TRANSMITTER);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState p_329621_) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState p_329467_, BlockGetter p_330223_, BlockPos p_327750_, CollisionContext p_332344_) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState p_334651_, Level p_328070_, BlockPos p_335832_, RandomSource p_335180_) {
        super.animateTick(p_334651_, p_328070_, p_335832_, p_335180_);

        for (BlockPos blockpos : BOOKSHELF_OFFSETS) {
            if (p_335180_.nextInt(16) == 0 && isValidBookShelf(p_328070_, p_335832_, blockpos)) {
                p_328070_.addParticle(
                    ParticleTypes.ENCHANT,
                    (double)p_335832_.getX() + 0.5,
                    (double)p_335832_.getY() + 2.0,
                    (double)p_335832_.getZ() + 0.5,
                    (double)((float)blockpos.getX() + p_335180_.nextFloat()) - 0.5,
                    (double)((float)blockpos.getY() - p_335180_.nextFloat() - 1.0F),
                    (double)((float)blockpos.getZ() + p_335180_.nextFloat()) - 0.5
                );
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_329775_, BlockState p_330999_) {
        return new EnchantingTableBlockEntity(p_329775_, p_330999_);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_335666_, BlockState p_330579_, BlockEntityType<T> p_332523_) {
        return p_335666_.isClientSide ? createTickerHelper(p_332523_, BlockEntityType.ENCHANTING_TABLE, EnchantingTableBlockEntity::bookAnimationTick) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_335615_, Level p_335039_, BlockPos p_331142_, Player p_334809_, BlockHitResult p_334503_) {
        if (!p_335039_.isClientSide) {
            p_334809_.openMenu(p_335615_.getMenuProvider(p_335039_, p_331142_));
        }

        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState p_335872_, Level p_334298_, BlockPos p_336351_) {
        BlockEntity blockentity = p_334298_.getBlockEntity(p_336351_);
        if (blockentity instanceof EnchantingTableBlockEntity) {
            Component component = ((Nameable)blockentity).getDisplayName();
            return new SimpleMenuProvider(
                (p_328554_, p_332165_, p_330050_) -> new EnchantmentMenu(p_328554_, p_332165_, ContainerLevelAccess.create(p_334298_, p_336351_)), component
            );
        } else {
            return null;
        }
    }

    @Override
    protected boolean isPathfindable(BlockState p_335176_, PathComputationType p_334574_) {
        return false;
    }
}