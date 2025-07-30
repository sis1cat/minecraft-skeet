package net.minecraft.world.entity.projectile;

import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ProjectileUtil {
    private static final float DEFAULT_ENTITY_HIT_RESULT_MARGIN = 0.3F;

    public static HitResult getHitResultOnMoveVector(Entity pProjectile, Predicate<Entity> pFilter) {
        Vec3 vec3 = pProjectile.getDeltaMovement();
        Level level = pProjectile.level();
        Vec3 vec31 = pProjectile.position();
        return getHitResult(vec31, pProjectile, pFilter, vec3, level, 0.3F, ClipContext.Block.COLLIDER);
    }

    public static HitResult getHitResultOnMoveVector(Entity pProjectile, Predicate<Entity> pFilter, ClipContext.Block pClipContext) {
        Vec3 vec3 = pProjectile.getDeltaMovement();
        Level level = pProjectile.level();
        Vec3 vec31 = pProjectile.position();
        return getHitResult(vec31, pProjectile, pFilter, vec3, level, 0.3F, pClipContext);
    }

    public static HitResult getHitResultOnViewVector(Entity pProjectile, Predicate<Entity> pFilter, double pScale) {
        Vec3 vec3 = pProjectile.getViewVector(0.0F).scale(pScale);
        Level level = pProjectile.level();
        Vec3 vec31 = pProjectile.getEyePosition();
        return getHitResult(vec31, pProjectile, pFilter, vec3, level, 0.0F, ClipContext.Block.COLLIDER);
    }

    private static HitResult getHitResult(
        Vec3 pPos, Entity pProjectile, Predicate<Entity> pFilter, Vec3 pDeltaMovement, Level pLevel, float pMargin, ClipContext.Block pClipContext
    ) {
        Vec3 vec3 = pPos.add(pDeltaMovement);
        HitResult hitresult = pLevel.clipIncludingBorder(new ClipContext(pPos, vec3, pClipContext, ClipContext.Fluid.NONE, pProjectile));
        if (hitresult.getType() != HitResult.Type.MISS) {
            vec3 = hitresult.getLocation();
        }

        HitResult hitresult1 = getEntityHitResult(pLevel, pProjectile, pPos, vec3, pProjectile.getBoundingBox().expandTowards(pDeltaMovement).inflate(1.0), pFilter, pMargin);
        if (hitresult1 != null) {
            hitresult = hitresult1;
        }

        return hitresult;
    }

    @Nullable
    public static EntityHitResult getEntityHitResult(Entity pShooter, Vec3 pStartVec, Vec3 pEndVec, AABB pBoundingBox, Predicate<Entity> pFilter, double pDistance) {
        Level level = pShooter.level();
        double d0 = pDistance;
        Entity entity = null;
        Vec3 vec3 = null;

        for (Entity entity1 : level.getEntities(pShooter, pBoundingBox, pFilter)) {
            AABB aabb = entity1.getBoundingBox().inflate((double)entity1.getPickRadius());
            Optional<Vec3> optional = aabb.clip(pStartVec, pEndVec);
            if (aabb.contains(pStartVec)) {
                if (d0 >= 0.0) {
                    entity = entity1;
                    vec3 = optional.orElse(pStartVec);
                    d0 = 0.0;
                }
            } else if (optional.isPresent()) {
                Vec3 vec31 = optional.get();
                double d1 = pStartVec.distanceToSqr(vec31);
                if (d1 < d0 || d0 == 0.0) {
                    if (entity1.getRootVehicle() == pShooter.getRootVehicle()) {
                        if (d0 == 0.0) {
                            entity = entity1;
                            vec3 = vec31;
                        }
                    } else {
                        entity = entity1;
                        vec3 = vec31;
                        d0 = d1;
                    }
                }
            }
        }

        return entity == null ? null : new EntityHitResult(entity, vec3);
    }

    @Nullable
    public static EntityHitResult getEntityHitResult(Level pLevel, Entity pProjectile, Vec3 pStartVec, Vec3 pEndVec, AABB pBoundingBox, Predicate<Entity> pFilter) {
        return getEntityHitResult(pLevel, pProjectile, pStartVec, pEndVec, pBoundingBox, pFilter, 0.3F);
    }

    @Nullable
    public static EntityHitResult getEntityHitResult(
        Level pLevel, Entity pProjectile, Vec3 pStartVec, Vec3 pEndVec, AABB pBoundingBox, Predicate<Entity> pFilter, float pInflationAmount
    ) {
        double d0 = Double.MAX_VALUE;
        Optional<Vec3> optional = Optional.empty();
        Entity entity = null;

        for (Entity entity1 : pLevel.getEntities(pProjectile, pBoundingBox, pFilter)) {
            AABB aabb = entity1.getBoundingBox().inflate((double)pInflationAmount);
            Optional<Vec3> optional1 = aabb.clip(pStartVec, pEndVec);
            if (optional1.isPresent()) {
                double d1 = pStartVec.distanceToSqr(optional1.get());
                if (d1 < d0) {
                    entity = entity1;
                    d0 = d1;
                    optional = optional1;
                }
            }
        }

        return entity == null ? null : new EntityHitResult(entity, optional.get());
    }

    public static void rotateTowardsMovement(Entity pProjectile, float pRotationSpeed) {
        Vec3 vec3 = pProjectile.getDeltaMovement();
        if (vec3.lengthSqr() != 0.0) {
            double d0 = vec3.horizontalDistance();
            pProjectile.setYRot((float)(Mth.atan2(vec3.z, vec3.x) * 180.0F / (float)Math.PI) + 90.0F);
            pProjectile.setXRot((float)(Mth.atan2(d0, vec3.y) * 180.0F / (float)Math.PI) - 90.0F);

            while (pProjectile.getXRot() - pProjectile.xRotO < -180.0F) {
                pProjectile.xRotO -= 360.0F;
            }

            while (pProjectile.getXRot() - pProjectile.xRotO >= 180.0F) {
                pProjectile.xRotO += 360.0F;
            }

            while (pProjectile.getYRot() - pProjectile.yRotO < -180.0F) {
                pProjectile.yRotO -= 360.0F;
            }

            while (pProjectile.getYRot() - pProjectile.yRotO >= 180.0F) {
                pProjectile.yRotO += 360.0F;
            }

            pProjectile.setXRot(Mth.lerp(pRotationSpeed, pProjectile.xRotO, pProjectile.getXRot()));
            pProjectile.setYRot(Mth.lerp(pRotationSpeed, pProjectile.yRotO, pProjectile.getYRot()));
        }
    }

    public static InteractionHand getWeaponHoldingHand(LivingEntity pShooter, Item pWeapon) {
        return pShooter.getMainHandItem().is(pWeapon) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public static AbstractArrow getMobArrow(LivingEntity pShooter, ItemStack pArrow, float pVelocity, @Nullable ItemStack pWeapon) {
        ArrowItem arrowitem = (ArrowItem)(pArrow.getItem() instanceof ArrowItem ? pArrow.getItem() : Items.ARROW);
        AbstractArrow abstractarrow = arrowitem.createArrow(pShooter.level(), pArrow, pShooter, pWeapon);
        abstractarrow.setBaseDamageFromMob(pVelocity);
        return abstractarrow;
    }
}