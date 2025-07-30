package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class JukeboxBlock extends BaseEntityBlock {
    public static final MapCodec<JukeboxBlock> CODEC = simpleCodec(JukeboxBlock::new);
    public static final BooleanProperty HAS_RECORD = BlockStateProperties.HAS_RECORD;

    @Override
    public MapCodec<JukeboxBlock> codec() {
        return CODEC;
    }

    protected JukeboxBlock(BlockBehaviour.Properties p_54257_) {
        super(p_54257_);
        this.registerDefaultState(this.stateDefinition.any().setValue(HAS_RECORD, Boolean.valueOf(false)));
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        CustomData customdata = pStack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (customdata.contains("RecordItem")) {
            pLevel.setBlock(pPos, pState.setValue(HAS_RECORD, Boolean.valueOf(true)), 2);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_54281_, Level p_54282_, BlockPos p_54283_, Player p_54284_, BlockHitResult p_54286_) {
        if (p_54281_.getValue(HAS_RECORD) && p_54282_.getBlockEntity(p_54283_) instanceof JukeboxBlockEntity jukeboxblockentity) {
            jukeboxblockentity.popOutTheItem();
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_345342_, BlockState p_343906_, Level p_342356_, BlockPos p_342905_, Player p_343973_, InteractionHand p_345093_, BlockHitResult p_345506_
    ) {
        if (p_343906_.getValue(HAS_RECORD)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        } else {
            ItemStack itemstack = p_343973_.getItemInHand(p_345093_);
            InteractionResult interactionresult = JukeboxPlayable.tryInsertIntoJukebox(p_342356_, p_342905_, itemstack, p_343973_);
            return (InteractionResult)(!interactionresult.consumesAction() ? InteractionResult.TRY_WITH_EMPTY_HAND : interactionresult);
        }
    }

    @Override
    protected void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            if (pLevel.getBlockEntity(pPos) instanceof JukeboxBlockEntity jukeboxblockentity) {
                jukeboxblockentity.popOutTheItem();
            }

            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_153451_, BlockState p_153452_) {
        return new JukeboxBlockEntity(p_153451_, p_153452_);
    }

    @Override
    public boolean isSignalSource(BlockState p_273404_) {
        return true;
    }

    @Override
    public int getSignal(BlockState p_272942_, BlockGetter p_273232_, BlockPos p_273524_, Direction p_272902_) {
        if (p_273232_.getBlockEntity(p_273524_) instanceof JukeboxBlockEntity jukeboxblockentity && jukeboxblockentity.getSongPlayer().isPlaying()) {
            return 15;
        }

        return 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
        return pLevel.getBlockEntity(pPos) instanceof JukeboxBlockEntity jukeboxblockentity ? jukeboxblockentity.getComparatorOutput() : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(HAS_RECORD);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_239682_, BlockState p_239683_, BlockEntityType<T> p_239684_) {
        return p_239683_.getValue(HAS_RECORD) ? createTickerHelper(p_239684_, BlockEntityType.JUKEBOX, JukeboxBlockEntity::tick) : null;
    }
}