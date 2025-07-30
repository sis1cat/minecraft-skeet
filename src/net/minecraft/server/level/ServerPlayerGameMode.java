package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ServerPlayerGameMode {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected ServerLevel level;
    protected final ServerPlayer player;
    private GameType gameModeForPlayer = GameType.DEFAULT_MODE;
    @Nullable
    private GameType previousGameModeForPlayer;
    private boolean isDestroyingBlock;
    private int destroyProgressStart;
    private BlockPos destroyPos = BlockPos.ZERO;
    private int gameTicks;
    private boolean hasDelayedDestroy;
    private BlockPos delayedDestroyPos = BlockPos.ZERO;
    private int delayedTickStart;
    private int lastSentState = -1;

    public ServerPlayerGameMode(ServerPlayer pPlayer) {
        this.player = pPlayer;
        this.level = pPlayer.serverLevel();
    }

    public boolean changeGameModeForPlayer(GameType pGameModeForPlayer) {
        if (pGameModeForPlayer == this.gameModeForPlayer) {
            return false;
        } else {
            this.setGameModeForPlayer(pGameModeForPlayer, this.previousGameModeForPlayer);
            this.player.onUpdateAbilities();
            this.player
                .server
                .getPlayerList()
                .broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, this.player));
            this.level.updateSleepingPlayerList();
            if (pGameModeForPlayer == GameType.CREATIVE) {
                this.player.resetCurrentImpulseContext();
            }

            return true;
        }
    }

    protected void setGameModeForPlayer(GameType pGameModeForPlayer, @Nullable GameType pPreviousGameModeForPlayer) {
        this.previousGameModeForPlayer = pPreviousGameModeForPlayer;
        this.gameModeForPlayer = pGameModeForPlayer;
        pGameModeForPlayer.updatePlayerAbilities(this.player.getAbilities());
    }

    public GameType getGameModeForPlayer() {
        return this.gameModeForPlayer;
    }

    @Nullable
    public GameType getPreviousGameModeForPlayer() {
        return this.previousGameModeForPlayer;
    }

    public boolean isSurvival() {
        return this.gameModeForPlayer.isSurvival();
    }

    public boolean isCreative() {
        return this.gameModeForPlayer.isCreative();
    }

    public void tick() {
        this.gameTicks++;
        if (this.hasDelayedDestroy) {
            BlockState blockstate = this.level.getBlockState(this.delayedDestroyPos);
            if (blockstate.isAir()) {
                this.hasDelayedDestroy = false;
            } else {
                float f = this.incrementDestroyProgress(blockstate, this.delayedDestroyPos, this.delayedTickStart);
                if (f >= 1.0F) {
                    this.hasDelayedDestroy = false;
                    this.destroyBlock(this.delayedDestroyPos);
                }
            }
        } else if (this.isDestroyingBlock) {
            BlockState blockstate1 = this.level.getBlockState(this.destroyPos);
            if (blockstate1.isAir()) {
                this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
                this.lastSentState = -1;
                this.isDestroyingBlock = false;
            } else {
                this.incrementDestroyProgress(blockstate1, this.destroyPos, this.destroyProgressStart);
            }
        }
    }

    private float incrementDestroyProgress(BlockState pState, BlockPos pPos, int pStartTick) {
        int i = this.gameTicks - pStartTick;
        float f = pState.getDestroyProgress(this.player, this.player.level(), pPos) * (float)(i + 1);
        int j = (int)(f * 10.0F);
        if (j != this.lastSentState) {
            this.level.destroyBlockProgress(this.player.getId(), pPos, j);
            this.lastSentState = j;
        }

        return f;
    }

    private void debugLogging(BlockPos pPos, boolean pTerminate, int pSequence, String pMessage) {
    }

    public void handleBlockBreakAction(BlockPos pPos, ServerboundPlayerActionPacket.Action pAction, Direction pFace, int pMaxBuildHeight, int pSequence) {
        if (!this.player.canInteractWithBlock(pPos, 1.0)) {
            this.debugLogging(pPos, false, pSequence, "too far");
        } else if (pPos.getY() > pMaxBuildHeight) {
            this.player.connection.send(new ClientboundBlockUpdatePacket(pPos, this.level.getBlockState(pPos)));
            this.debugLogging(pPos, false, pSequence, "too high");
        } else {
            if (pAction == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                if (!this.level.mayInteract(this.player, pPos)) {
                    this.player.connection.send(new ClientboundBlockUpdatePacket(pPos, this.level.getBlockState(pPos)));
                    this.debugLogging(pPos, false, pSequence, "may not interact");
                    return;
                }

                if (this.isCreative()) {
                    this.destroyAndAck(pPos, pSequence, "creative destroy");
                    return;
                }

                if (this.player.blockActionRestricted(this.level, pPos, this.gameModeForPlayer)) {
                    this.player.connection.send(new ClientboundBlockUpdatePacket(pPos, this.level.getBlockState(pPos)));
                    this.debugLogging(pPos, false, pSequence, "block action restricted");
                    return;
                }

                this.destroyProgressStart = this.gameTicks;
                float f = 1.0F;
                BlockState blockstate = this.level.getBlockState(pPos);
                if (!blockstate.isAir()) {
                    EnchantmentHelper.onHitBlock(
                        this.level,
                        this.player.getMainHandItem(),
                        this.player,
                        this.player,
                        EquipmentSlot.MAINHAND,
                        Vec3.atCenterOf(pPos),
                        blockstate,
                        p_343810_ -> this.player.onEquippedItemBroken(p_343810_, EquipmentSlot.MAINHAND)
                    );
                    blockstate.attack(this.level, pPos, this.player);
                    f = blockstate.getDestroyProgress(this.player, this.player.level(), pPos);
                }

                if (!blockstate.isAir() && f >= 1.0F) {
                    this.destroyAndAck(pPos, pSequence, "insta mine");
                } else {
                    if (this.isDestroyingBlock) {
                        this.player.connection.send(new ClientboundBlockUpdatePacket(this.destroyPos, this.level.getBlockState(this.destroyPos)));
                        this.debugLogging(pPos, false, pSequence, "abort destroying since another started (client insta mine, server disagreed)");
                    }

                    this.isDestroyingBlock = true;
                    this.destroyPos = pPos.immutable();
                    int i = (int)(f * 10.0F);
                    this.level.destroyBlockProgress(this.player.getId(), pPos, i);
                    this.debugLogging(pPos, true, pSequence, "actual start of destroying");
                    this.lastSentState = i;
                }
            } else if (pAction == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                if (pPos.equals(this.destroyPos)) {
                    int j = this.gameTicks - this.destroyProgressStart;
                    BlockState blockstate1 = this.level.getBlockState(pPos);
                    if (!blockstate1.isAir()) {
                        float f1 = blockstate1.getDestroyProgress(this.player, this.player.level(), pPos) * (float)(j + 1);
                        if (f1 >= 0.7F) {
                            this.isDestroyingBlock = false;
                            this.level.destroyBlockProgress(this.player.getId(), pPos, -1);
                            this.destroyAndAck(pPos, pSequence, "destroyed");
                            return;
                        }

                        if (!this.hasDelayedDestroy) {
                            this.isDestroyingBlock = false;
                            this.hasDelayedDestroy = true;
                            this.delayedDestroyPos = pPos;
                            this.delayedTickStart = this.destroyProgressStart;
                        }
                    }
                }

                this.debugLogging(pPos, true, pSequence, "stopped destroying");
            } else if (pAction == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
                this.isDestroyingBlock = false;
                if (!Objects.equals(this.destroyPos, pPos)) {
                    LOGGER.warn("Mismatch in destroy block pos: {} {}", this.destroyPos, pPos);
                    this.level.destroyBlockProgress(this.player.getId(), this.destroyPos, -1);
                    this.debugLogging(pPos, true, pSequence, "aborted mismatched destroying");
                }

                this.level.destroyBlockProgress(this.player.getId(), pPos, -1);
                this.debugLogging(pPos, true, pSequence, "aborted destroying");
            }
        }
    }

    public void destroyAndAck(BlockPos pPos, int pSequence, String pMessage) {
        if (this.destroyBlock(pPos)) {
            this.debugLogging(pPos, true, pSequence, pMessage);
        } else {
            this.player.connection.send(new ClientboundBlockUpdatePacket(pPos, this.level.getBlockState(pPos)));
            this.debugLogging(pPos, false, pSequence, pMessage);
        }
    }

    public boolean destroyBlock(BlockPos pPos) {
        BlockState blockstate1 = this.level.getBlockState(pPos);
        if (!this.player.getMainHandItem().getItem().canAttackBlock(blockstate1, this.level, pPos, this.player)) {
            return false;
        } else {
            BlockEntity blockentity = this.level.getBlockEntity(pPos);
            Block block = blockstate1.getBlock();
            if (block instanceof GameMasterBlock && !this.player.canUseGameMasterBlocks()) {
                this.level.sendBlockUpdated(pPos, blockstate1, blockstate1, 3);
                return false;
            } else if (this.player.blockActionRestricted(this.level, pPos, this.gameModeForPlayer)) {
                return false;
            } else {
                BlockState blockstate = block.playerWillDestroy(this.level, pPos, blockstate1, this.player);
                boolean flag1 = this.level.removeBlock(pPos, false);
                if (flag1) {
                    block.destroy(this.level, pPos, blockstate);
                }

                if (this.isCreative()) {
                    return true;
                } else {
                    ItemStack itemstack = this.player.getMainHandItem();
                    ItemStack itemstack1 = itemstack.copy();
                    boolean flag = this.player.hasCorrectToolForDrops(blockstate);
                    itemstack.mineBlock(this.level, blockstate, pPos, this.player);
                    if (flag1 && flag) {
                        block.playerDestroy(this.level, this.player, pPos, blockstate, blockentity, itemstack1);
                    }

                    return true;
                }
            }
        }
    }

    public InteractionResult useItem(ServerPlayer pPlayer, Level pLevel, ItemStack pStack, InteractionHand pHand) {
        if (this.gameModeForPlayer == GameType.SPECTATOR) {
            return InteractionResult.PASS;
        } else if (pPlayer.getCooldowns().isOnCooldown(pStack)) {
            return InteractionResult.PASS;
        } else {
            int i = pStack.getCount();
            int j = pStack.getDamageValue();
            InteractionResult interactionresult = pStack.use(pLevel, pPlayer, pHand);
            ItemStack itemstack;
            if (interactionresult instanceof InteractionResult.Success interactionresult$success) {
                itemstack = Objects.requireNonNullElse(interactionresult$success.heldItemTransformedTo(), pPlayer.getItemInHand(pHand));
            } else {
                itemstack = pPlayer.getItemInHand(pHand);
            }

            if (itemstack == pStack && itemstack.getCount() == i && itemstack.getUseDuration(pPlayer) <= 0 && itemstack.getDamageValue() == j) {
                return interactionresult;
            } else if (interactionresult instanceof InteractionResult.Fail && itemstack.getUseDuration(pPlayer) > 0 && !pPlayer.isUsingItem()) {
                return interactionresult;
            } else {
                if (pStack != itemstack) {
                    pPlayer.setItemInHand(pHand, itemstack);
                }

                if (itemstack.isEmpty()) {
                    pPlayer.setItemInHand(pHand, ItemStack.EMPTY);
                }

                if (!pPlayer.isUsingItem()) {
                    pPlayer.inventoryMenu.sendAllDataToRemote();
                }

                return interactionresult;
            }
        }
    }

    public InteractionResult useItemOn(ServerPlayer pPlayer, Level pLevel, ItemStack pStack, InteractionHand pHand, BlockHitResult pHitResult) {
        BlockPos blockpos = pHitResult.getBlockPos();
        BlockState blockstate = pLevel.getBlockState(blockpos);
        if (!blockstate.getBlock().isEnabled(pLevel.enabledFeatures())) {
            return InteractionResult.FAIL;
        } else if (this.gameModeForPlayer == GameType.SPECTATOR) {
            MenuProvider menuprovider = blockstate.getMenuProvider(pLevel, blockpos);
            if (menuprovider != null) {
                pPlayer.openMenu(menuprovider);
                return InteractionResult.CONSUME;
            } else {
                return InteractionResult.PASS;
            }
        } else {
            boolean flag = !pPlayer.getMainHandItem().isEmpty() || !pPlayer.getOffhandItem().isEmpty();
            boolean flag1 = pPlayer.isSecondaryUseActive() && flag;
            ItemStack itemstack = pStack.copy();
            if (!flag1) {
                InteractionResult interactionresult = blockstate.useItemOn(pPlayer.getItemInHand(pHand), pLevel, pPlayer, pHand, pHitResult);
                if (interactionresult.consumesAction()) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(pPlayer, blockpos, itemstack);
                    return interactionresult;
                }

                if (interactionresult instanceof InteractionResult.TryEmptyHandInteraction && pHand == InteractionHand.MAIN_HAND) {
                    InteractionResult interactionresult1 = blockstate.useWithoutItem(pLevel, pPlayer, pHitResult);
                    if (interactionresult1.consumesAction()) {
                        CriteriaTriggers.DEFAULT_BLOCK_USE.trigger(pPlayer, blockpos);
                        return interactionresult1;
                    }
                }
            }

            if (!pStack.isEmpty() && !pPlayer.getCooldowns().isOnCooldown(pStack)) {
                UseOnContext useoncontext = new UseOnContext(pPlayer, pHand, pHitResult);
                InteractionResult interactionresult2;
                if (this.isCreative()) {
                    int i = pStack.getCount();
                    interactionresult2 = pStack.useOn(useoncontext);
                    pStack.setCount(i);
                } else {
                    interactionresult2 = pStack.useOn(useoncontext);
                }

                if (interactionresult2.consumesAction()) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(pPlayer, blockpos, itemstack);
                }

                return interactionresult2;
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    public void setLevel(ServerLevel pServerLevel) {
        this.level = pServerLevel;
    }
}