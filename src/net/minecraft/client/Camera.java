package net.minecraft.client;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.NewMinecartBehavior;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.optifine.reflect.Reflector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Camera {
    private static final float DEFAULT_CAMERA_DISTANCE = 4.0F;
    private static final Vector3f FORWARDS = new Vector3f(0.0F, 0.0F, -1.0F);
    private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);
    private static final Vector3f LEFT = new Vector3f(-1.0F, 0.0F, 0.0F);
    private boolean initialized;
    private BlockGetter level;
    private Entity entity;
    private Vec3 position = Vec3.ZERO;
    private final BlockPos.MutableBlockPos blockPosition = new BlockPos.MutableBlockPos();
    private final Vector3f forwards = new Vector3f(FORWARDS);
    private final Vector3f up = new Vector3f(UP);
    private final Vector3f left = new Vector3f(LEFT);
    private float xRot;
    private float yRot;
    private final Quaternionf rotation = new Quaternionf();
    private boolean detached;
    private float eyeHeight;
    private float eyeHeightOld;
    private float partialTickTime;
    public static final float FOG_DISTANCE_SCALE = 0.083333336F;

    public void setup(BlockGetter pLevel, Entity pEntity, boolean pDetached, boolean pThirdPersonReverse, float pPartialTick) {
        this.initialized = true;
        this.level = pLevel;
        this.entity = pEntity;
        this.detached = pDetached;
        this.partialTickTime = pPartialTick;
        if (pEntity.isPassenger()
            && pEntity.getVehicle() instanceof Minecart minecart
            && minecart.getBehavior() instanceof NewMinecartBehavior newminecartbehavior
            && newminecartbehavior.cartHasPosRotLerp()) {
            Vec3 vec3 = minecart.getPassengerRidingPosition(pEntity)
                .subtract(minecart.position())
                .subtract(pEntity.getVehicleAttachmentPoint(minecart))
                .add(new Vec3(0.0, (double)Mth.lerp(pPartialTick, this.eyeHeightOld, this.eyeHeight), 0.0));
            this.setRotation(pEntity.getViewYRot(pPartialTick), pEntity.getViewXRot(pPartialTick));
            this.setPosition(newminecartbehavior.getCartLerpPosition(pPartialTick).add(vec3));
        } else {
            this.setRotation(pEntity.getViewYRot(pPartialTick), pEntity.getViewXRot(pPartialTick));
            this.setPosition(
                Mth.lerp((double)pPartialTick, pEntity.xo, pEntity.getX()),
                Mth.lerp((double)pPartialTick, pEntity.yo, pEntity.getY()) + (double)Mth.lerp(pPartialTick, this.eyeHeightOld, this.eyeHeight),
                Mth.lerp((double)pPartialTick, pEntity.zo, pEntity.getZ())
            );
        }

        if (pDetached) {
            if (pThirdPersonReverse) {
                this.setRotation(this.yRot + 180.0F, -this.xRot);
            }

            float f = pEntity instanceof LivingEntity livingentity ? livingentity.getScale() : 1.0F;
            this.move(-this.getMaxZoom(4.0F * f), 0.0F, 0.0F);
        } else if (pEntity instanceof LivingEntity && ((LivingEntity)pEntity).isSleeping()) {
            Direction direction = ((LivingEntity)pEntity).getBedOrientation();
            this.setRotation(direction != null ? direction.toYRot() - 180.0F : 0.0F, 0.0F);
            this.move(0.0F, 0.3F, 0.0F);
        }
    }

    public void tick() {
        if (this.entity != null) {
            this.eyeHeightOld = this.eyeHeight;
            this.eyeHeight = this.eyeHeight + (this.entity.getEyeHeight() - this.eyeHeight) * 0.5F;
        }
    }

    private float getMaxZoom(float pMaxZoom) {
        float f = 0.1F;

        for (int i = 0; i < 8; i++) {
            float f1 = (float)((i & 1) * 2 - 1);
            float f2 = (float)((i >> 1 & 1) * 2 - 1);
            float f3 = (float)((i >> 2 & 1) * 2 - 1);
            Vec3 vec3 = this.position.add((double)(f1 * 0.1F), (double)(f2 * 0.1F), (double)(f3 * 0.1F));
            Vec3 vec31 = vec3.add(new Vec3(this.forwards).scale((double)(-pMaxZoom)));
            HitResult hitresult = this.level.clip(new ClipContext(vec3, vec31, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, this.entity));
            if (hitresult.getType() != HitResult.Type.MISS) {
                float f4 = (float)hitresult.getLocation().distanceToSqr(this.position);
                if (f4 < Mth.square(pMaxZoom)) {
                    pMaxZoom = Mth.sqrt(f4);
                }
            }
        }

        return pMaxZoom;
    }

    protected void move(float pZoom, float pDy, float pDx) {
        Vector3f vector3f = new Vector3f(pDx, pDy, -pZoom).rotate(this.rotation);
        this.setPosition(
            new Vec3(this.position.x + (double)vector3f.x, this.position.y + (double)vector3f.y, this.position.z + (double)vector3f.z)
        );
    }

    protected void setRotation(float pYRot, float pXRot) {
        this.setRotation(pYRot, pXRot, 0.0F);
    }

    public void setRotation(float pitchIn, float yawIn, float z) {
        this.xRot = yawIn;
        this.yRot = pitchIn;
        this.rotation.rotationYXZ((float) Math.PI - pitchIn * (float) (Math.PI / 180.0), -yawIn * (float) (Math.PI / 180.0), z * (float) (Math.PI / 180.0));
        FORWARDS.rotate(this.rotation, this.forwards);
        UP.rotate(this.rotation, this.up);
        LEFT.rotate(this.rotation, this.left);
    }

    protected void setPosition(double pX, double pY, double pZ) {
        this.setPosition(new Vec3(pX, pY, pZ));
    }

    protected void setPosition(Vec3 pPos) {
        this.position = pPos;
        this.blockPosition.set(pPos.x, pPos.y, pPos.z);
    }

    public Vec3 getPosition() {
        return this.position;
    }

    public BlockPos getBlockPosition() {
        return this.blockPosition;
    }

    public float getXRot() {
        return this.xRot;
    }

    public float getYRot() {
        return this.yRot;
    }

    public Quaternionf rotation() {
        return this.rotation;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public boolean isDetached() {
        return this.detached;
    }

    public Camera.NearPlane getNearPlane() {
        Minecraft minecraft = Minecraft.getInstance();
        double d0 = (double)minecraft.getWindow().getWidth() / (double)minecraft.getWindow().getHeight();
        double d1 = Math.tan((double)((float)minecraft.options.fov().get().intValue() * (float) (Math.PI / 180.0)) / 2.0) * 0.05F;
        double d2 = d1 * d0;
        Vec3 vec3 = new Vec3(this.forwards).scale(0.05F);
        Vec3 vec31 = new Vec3(this.left).scale(d2);
        Vec3 vec32 = new Vec3(this.up).scale(d1);
        return new Camera.NearPlane(vec3, vec31, vec32);
    }

    public FogType getFluidInCamera() {
        if (!this.initialized) {
            return FogType.NONE;
        } else {
            FluidState fluidstate = this.level.getFluidState(this.blockPosition);
            if (fluidstate.is(FluidTags.WATER)
                && this.position.y < (double)((float)this.blockPosition.getY() + fluidstate.getHeight(this.level, this.blockPosition))) {
                return FogType.WATER;
            } else {
                Camera.NearPlane camera$nearplane = this.getNearPlane();

                for (Vec3 vec3 : Arrays.asList(
                    camera$nearplane.forward,
                    camera$nearplane.getTopLeft(),
                    camera$nearplane.getTopRight(),
                    camera$nearplane.getBottomLeft(),
                    camera$nearplane.getBottomRight()
                )) {
                    Vec3 vec31 = this.position.add(vec3);
                    BlockPos blockpos = BlockPos.containing(vec31);
                    FluidState fluidstate1 = this.level.getFluidState(blockpos);
                    if (fluidstate1.is(FluidTags.LAVA)) {
                        if (vec31.y <= (double)(fluidstate1.getHeight(this.level, blockpos) + (float)blockpos.getY())) {
                            return FogType.LAVA;
                        }
                    } else {
                        BlockState blockstate = this.level.getBlockState(blockpos);
                        if (blockstate.is(Blocks.POWDER_SNOW)) {
                            return FogType.POWDER_SNOW;
                        }
                    }
                }

                return FogType.NONE;
            }
        }
    }

    public BlockState getBlockState() {
        return !this.initialized ? Blocks.AIR.defaultBlockState() : this.level.getBlockState(this.blockPosition);
    }

    public void setAnglesInternal(float yaw, float pitch) {
        this.yRot = yaw;
        this.xRot = pitch;
    }

    public BlockState getBlockAtCamera() {
        if (!this.initialized) {
            return Blocks.AIR.defaultBlockState();
        } else {
            BlockState blockstate = this.level.getBlockState(this.blockPosition);
            if (Reflector.IForgeBlockState_getStateAtViewpoint.exists()) {
                blockstate = (BlockState)Reflector.call(blockstate, Reflector.IForgeBlockState_getStateAtViewpoint, this.level, this.blockPosition, this.position);
            }

            return blockstate;
        }
    }

    public final Vector3f getLookVector() {
        return this.forwards;
    }

    public final Vector3f getUpVector() {
        return this.up;
    }

    public final Vector3f getLeftVector() {
        return this.left;
    }

    public void reset() {
        this.level = null;
        this.entity = null;
        this.initialized = false;
    }

    public float getPartialTickTime() {
        return this.partialTickTime;
    }

    public static class NearPlane {
        final Vec3 forward;
        private final Vec3 left;
        private final Vec3 up;

        NearPlane(Vec3 pForward, Vec3 pLeft, Vec3 pUp) {
            this.forward = pForward;
            this.left = pLeft;
            this.up = pUp;
        }

        public Vec3 getTopLeft() {
            return this.forward.add(this.up).add(this.left);
        }

        public Vec3 getTopRight() {
            return this.forward.add(this.up).subtract(this.left);
        }

        public Vec3 getBottomLeft() {
            return this.forward.subtract(this.up).add(this.left);
        }

        public Vec3 getBottomRight() {
            return this.forward.subtract(this.up).subtract(this.left);
        }

        public Vec3 getPointOnPlane(float pLeftScale, float pUpScale) {
            return this.forward.add(this.up.scale((double)pUpScale)).subtract(this.left.scale((double)pLeftScale));
        }
    }
}