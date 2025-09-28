package sisicat.main.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.phys.Vec3;

public class PlayerPredictions {

    private static Vec3 predictOnStopControl(LivingEntity entity) {
        Vec3 delta = entity.getDeltaMovement();

        BlockPos blockPos = entity.getBlockPosBelowThatAffectsMyMovement();
        float blockFriction = entity.onGround()
                ? entity.level().getBlockState(blockPos).getBlock().getFriction()
                : 1.0F;

        float f1 = blockFriction * 0.91F;

        float frictionFactor = f1;
        float verticalFriction = (entity instanceof FlyingAnimal) ? f1 : 0.98F;

        double nextX = delta.x * frictionFactor * verticalFriction;
        double nextZ = delta.z * frictionFactor * verticalFriction;

        double nextY;

        if (entity.onGround()) {
            nextY = 0.0;
        } else {
            double d0 = delta.y;

            MobEffectInstance levitation = entity.getEffect(MobEffects.LEVITATION);
            if (levitation != null) {
                d0 += (0.05 * (levitation.getAmplifier() + 1) - delta.y) * 0.2;
            } else if (!entity.level().isClientSide || entity.level().hasChunkAt(blockPos)) {
                d0 -= entity.getEffectiveGravity();
            } else if (entity.getY() > entity.level().getMinY()) {
                d0 = -0.1;
            } else {
                d0 = 0.0;
            }

            nextY = d0 * verticalFriction;
        }

        float blockSpeedFactor = entity.getBlockSpeedFactor();

        return new Vec3(nextX * blockSpeedFactor, nextY, nextZ * blockSpeedFactor);
    }

    public static double predictNextYDelta(LivingEntity entity) {

        Vec3 delta = entity.getDeltaMovement();

        BlockPos blockPos = entity.getBlockPosBelowThatAffectsMyMovement();
        float blockFriction = entity.onGround()
                ? entity.level().getBlockState(blockPos).getBlock().getFriction()
                : 1.0F;

        float f1 = blockFriction * 0.91F;

        float verticalFriction = (entity instanceof FlyingAnimal) ? f1 : 0.98F;

        double nextY;

        if (entity.onGround()) {
            nextY = 0.0;
        } else {
            double d0 = delta.y;

            MobEffectInstance levitation = entity.getEffect(MobEffects.LEVITATION);
            if (levitation != null) {
                d0 += (0.05 * (levitation.getAmplifier() + 1) - delta.y) * 0.2;
            } else if (!entity.level().isClientSide || entity.level().hasChunkAt(blockPos)) {
                d0 -= entity.getEffectiveGravity();
            } else if (entity.getY() > entity.level().getMinY()) {
                d0 = -0.1;
            } else {
                d0 = 0.0;
            }

            nextY = d0 * verticalFriction;
        }

        return nextY;

    }

    public static Vec3 predictNextPosition(LivingEntity livingEntity) {

        final Vec3 deltaFrame1 = predictOnStopControl(livingEntity);
        final Vec3 deltaFrame2 = livingEntity.getDeltaMovement();

        final Vec3 middleFrame = new Vec3(
                deltaFrame1.x + (deltaFrame2.x - deltaFrame1.x) / 2,
                deltaFrame1.y + (deltaFrame2.y - deltaFrame1.y) / 2,
                deltaFrame1.z + (deltaFrame2.z - deltaFrame1.z) / 2
        );

        return new Vec3(
                livingEntity.getX() + middleFrame.x,
                livingEntity.getY() + middleFrame.y,
                livingEntity.getZ() + middleFrame.z
        );

    }

}
