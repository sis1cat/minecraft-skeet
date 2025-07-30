package net.minecraft.world.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SmallFireball extends Fireball {
    public SmallFireball(EntityType<? extends SmallFireball> p_37364_, Level p_37365_) {
        super(p_37364_, p_37365_);
    }

    public SmallFireball(Level pLevel, LivingEntity pOwner, Vec3 pMovement) {
        super(EntityType.SMALL_FIREBALL, pOwner, pMovement, pLevel);
    }

    public SmallFireball(Level pLevel, double pX, double pY, double pZ, Vec3 pMovement) {
        super(EntityType.SMALL_FIREBALL, pX, pY, pZ, pMovement, pLevel);
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);
        if (this.level() instanceof ServerLevel serverlevel) {
            Entity entity1 = pResult.getEntity();
            Entity $$4 = this.getOwner();
            int $$5 = entity1.getRemainingFireTicks();
            entity1.igniteForSeconds(5.0F);
            DamageSource $$6 = this.damageSources().fireball(this, $$4);
            if (!entity1.hurtServer(serverlevel, $$6, 5.0F)) {
                entity1.setRemainingFireTicks($$5);
            } else {
                EnchantmentHelper.doPostAttackEffects(serverlevel, entity1, $$6);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult p_37384_) {
        super.onHitBlock(p_37384_);
        if (this.level() instanceof ServerLevel serverlevel) {
            Entity entity = this.getOwner();
            if (!(entity instanceof Mob) || serverlevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                BlockPos blockpos = p_37384_.getBlockPos().relative(p_37384_.getDirection());
                if (this.level().isEmptyBlock(blockpos)) {
                    this.level().setBlockAndUpdate(blockpos, BaseFireBlock.getState(this.level(), blockpos));
                }
            }
        }
    }

    @Override
    protected void onHit(HitResult pResult) {
        super.onHit(pResult);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }
}