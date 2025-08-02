package sisicat.main.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

import net.minecraft.world.phys.shapes.VoxelShape;
import sisicat.IDefault;

import java.util.Optional;
import java.util.function.Predicate;

public class RayTrace implements IDefault {

    public static Vec3 getRotationVector(float yaw, float pitch) {

        float yawRadians = -yaw * ((float) Math.PI / 180) - (float) Math.PI;
        float pitchRadians = -pitch * ((float) Math.PI / 180);

        float cosYaw = Mth.cos(yawRadians);
        float sinYaw = Mth.sin(yawRadians);
        float cosPitch = -Mth.cos(pitchRadians);
        float sinPitch = Mth.sin(pitchRadians);

        return new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);

    }

     private static HitResult pick(float yaw, float pitch, double distance, boolean ignoreWithoutColliderBlocks) {

        Vec3 playerEyePosition = mc.getCameraEntity().getEyePosition();
        Vec3 rotationVector = getRotationVector(yaw, pitch);
        Vec3 stretchedVector = playerEyePosition.add(rotationVector.x * distance, rotationVector.y * distance, rotationVector.z * distance);

        return mc.level.clip(new ClipContext(playerEyePosition, stretchedVector, ignoreWithoutColliderBlocks ? ClipContext.Block.COLLIDER : ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.cameraEntity));

    }

    public static Vec2 getRotationForDifferences(double xDifference, double yDifference, double zDifference) {
        return new Vec2(
                (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(zDifference, xDifference)) - 90),
                (float) Mth.wrapDegrees(Math.toDegrees(-Math.atan2(yDifference, Math.hypot(xDifference, zDifference))))
        );
    }

    public static HitResult getHitResultByDifferences(double xDifference, double yDifference, double zDifference, float distance, int ignoreBlocks) {

        final Vec2 rotation = getRotationForDifferences(xDifference, yDifference, zDifference);
        return RayTrace.getHitResult(rotation.x, rotation.y, distance, ignoreBlocks);

    }

    public static EntityHitResult getEntityHitResult(Entity pShooter, Vec3 pStartVec, Vec3 pEndVec, AABB pBoundingBox, Predicate<Entity> pFilter, double pDistance) {
        Level level = pShooter.level();
        double d0 = pDistance;
        Entity entity = null;
        Vec3 vec3 = null;

        for (Entity entity1 : level.getEntities(pShooter, pBoundingBox, pFilter)) {
            AABB aabb = entity1.getBoundingBox().inflate(1.0D);
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

    public static HitResult getHitResult(float yaw, float pitch, double distance, int mode) {

        HitResult hitResult;

        Entity cameraEntity = mc.getCameraEntity();
        Vec3 cameraEyePosition = cameraEntity.getEyePosition();

        hitResult = pick(yaw, pitch, distance, mode == 0);

        double distanceToPickedBlock = hitResult.getLocation().distanceToSqr(cameraEyePosition);

        Vec3 rotationVector = getRotationVector(yaw, pitch);
        Vec3 stretchedVector = cameraEyePosition.add(rotationVector.x * distance, rotationVector.y * distance, rotationVector.z * distance);

        AABB aabb = cameraEntity.getBoundingBox().expandTowards(rotationVector.scale(distance)).inflate(1.0D, 1.0D, 1.0D);

        EntityHitResult entityhitresult =
                getEntityHitResult(
                        cameraEntity, cameraEyePosition, stretchedVector, aabb,
                        (entity) -> !entity.isSpectator() && entity.isPickable(), mode == 1 ? 6 : distanceToPickedBlock
                );

        if (entityhitresult != null) {

            Vec3 entityLocation = entityhitresult.getLocation();
            double distanceToPickedEntity = cameraEyePosition.distanceToSqr(entityLocation);

            if (distanceToPickedEntity < distanceToPickedBlock || mode == 1)
                hitResult = entityhitresult;

        }

        return hitResult;

    }

    public static BlockHitResult getBlockOnAngle(float yaw, float pitch, double distance) {

        Vec3 playerEyePosition = mc.player.getEyePosition();
        Vec3 directoryVector = getRotationVector(yaw, pitch);
        Vec3 stretchedVector = playerEyePosition.add(directoryVector.x * distance, directoryVector.y * distance, directoryVector.z * distance);

        BlockHitResult blockHitResult = mc.level.clip(new ClipContext(playerEyePosition, stretchedVector, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));

        return blockHitResult;

    }

    public static BlockHitResult getBlockUnderHitbox(double distance) {

        AABB boundingBox = mc.player.getBoundingBox();

        Vec3 cornerPos1 = new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
        Vec3 cornerPos2 = new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
        Vec3 cornerPos3 = new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.minZ);
        Vec3 cornerPos4 = new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.maxZ);

        Vec3 directoryVector = getRotationVector(0, 90);

        double muldvdx = directoryVector.x * distance;
        double muldvdy = directoryVector.y * distance;
        double muldvdz = directoryVector.z * distance;

        Vec3 stretchedVector1 = cornerPos1.add(muldvdx, muldvdy, muldvdz);
        Vec3 stretchedVector2 = cornerPos2.add(muldvdx, muldvdy, muldvdz);
        Vec3 stretchedVector3 = cornerPos3.add(muldvdx, muldvdy, muldvdz);
        Vec3 stretchedVector4 = cornerPos4.add(muldvdx, muldvdy, muldvdz);

        BlockHitResult[] blockHitResults = new BlockHitResult[]{
                mc.level.clip(new ClipContext(cornerPos1, stretchedVector1, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)),
                mc.level.clip(new ClipContext(cornerPos2, stretchedVector2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)),
                mc.level.clip(new ClipContext(cornerPos3, stretchedVector3, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)),
                mc.level.clip(new ClipContext(cornerPos4, stretchedVector4, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player))
        };

        for(BlockHitResult blockHitResult : blockHitResults)
            if(blockHitResult.getType() != HitResult.Type.MISS)
                return blockHitResult;

        return blockHitResults[0];

    }

    public static BlockHitResult getBlockAboveHitbox(double distance) {

        AABB boundingBox = mc.player.getBoundingBox();

        Vec3 cornerPos1 = new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.minZ);
        Vec3 cornerPos2 = new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
        Vec3 cornerPos3 = new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
        Vec3 cornerPos4 = new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);

        Vec3 directoryVector = getRotationVector(0, -90);

        double muldvdx = directoryVector.x * distance;
        double muldvdy = directoryVector.y * distance;
        double muldvdz = directoryVector.z * distance;

        Vec3 stretchedVector1 = cornerPos1.add(muldvdx, muldvdy, muldvdz);
        Vec3 stretchedVector2 = cornerPos2.add(muldvdx, muldvdy, muldvdz);
        Vec3 stretchedVector3 = cornerPos3.add(muldvdx, muldvdy, muldvdz);
        Vec3 stretchedVector4 = cornerPos4.add(muldvdx, muldvdy, muldvdz);

        BlockHitResult[] blockHitResults = new BlockHitResult[]{
                mc.level.clip(new ClipContext(cornerPos1, stretchedVector1, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)),
                mc.level.clip(new ClipContext(cornerPos2, stretchedVector2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)),
                mc.level.clip(new ClipContext(cornerPos3, stretchedVector3, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)),
                mc.level.clip(new ClipContext(cornerPos4, stretchedVector4, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player))
        };

        for(BlockHitResult blockHitResult : blockHitResults)
            if(blockHitResult.getType() != HitResult.Type.MISS)
                return blockHitResult;

        return blockHitResults[0];

    }

    public static BlockHitResult getBlockUnderHitbox(double minY, double distance) {

        AABB boundingBox = mc.player.getBoundingBox();

        Vec3 cornerPos1 = new Vec3(boundingBox.minX, minY, boundingBox.minZ);
        Vec3 cornerPos2 = new Vec3(boundingBox.maxX, minY, boundingBox.maxZ);
        Vec3 cornerPos3 = new Vec3(boundingBox.maxX, minY, boundingBox.minZ);
        Vec3 cornerPos4 = new Vec3(boundingBox.minX, minY, boundingBox.maxZ);

        Vec3 directoryVector = getRotationVector(0, 90);

        double muldvdx = directoryVector.x * distance;
        double muldvdy = directoryVector.y * distance;
        double muldvdz = directoryVector.z * distance;

        Vec3 stretchedVector1 = cornerPos1.add(muldvdx, muldvdy, muldvdz);
        Vec3 stretchedVector2 = cornerPos2.add(muldvdx, muldvdy, muldvdz);
        Vec3 stretchedVector3 = cornerPos3.add(muldvdx, muldvdy, muldvdz);
        Vec3 stretchedVector4 = cornerPos4.add(muldvdx, muldvdy, muldvdz);

        BlockHitResult[] blockHitResults = new BlockHitResult[]{
                mc.level.clip(new ClipContext(cornerPos1, stretchedVector1, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)),
                mc.level.clip(new ClipContext(cornerPos2, stretchedVector2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)),
                mc.level.clip(new ClipContext(cornerPos3, stretchedVector3, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)),
                mc.level.clip(new ClipContext(cornerPos4, stretchedVector4, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player))
        };

        for(BlockHitResult blockHitResult : blockHitResults)
            if(blockHitResult.getType() != HitResult.Type.MISS)
                return blockHitResult;

        return blockHitResults[0];

    }

}
