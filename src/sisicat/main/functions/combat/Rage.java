package sisicat.main.functions.combat;

import com.darkmagician6.eventapi.EventTarget;
import com.darkmagician6.eventapi.types.Priority;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.*;
import net.minecraft.world.scores.ScoreHolder;
import sisicat.IDefault;
import sisicat.events.*;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;
import sisicat.main.utilities.Inventory;
import sisicat.main.utilities.PlayerPredictions;
import sisicat.main.utilities.RayTrace;

import java.util.*;

public class Rage extends Function implements IDefault {

    private final FunctionSetting
            targetSelection,
            attackDistance,
            rotationMode,
            multipointScale,
            backtrack;

    private final FunctionSetting
            sprintResetMode,
            forceCriticalAttack,
            attackThroughBlocks,
            automaticShieldBreak,
            strafeAroundTheTarget;

    public Rage(String name) {

        super(name);

        final ArrayList<String> targetSelectionOptions = new ArrayList<>(List.of(
                new String[]{
                        "Players",
                        "Invisible",
                        "Monsters",
                        "Animals",
                        "Cats & dogs",
                        "Villagers"
                }
        ));

        targetSelection =
                new FunctionSetting(
                        "Target selection",
                        targetSelectionOptions,
                        new ArrayList<>(List.of(
                                "Players", "Monsters"
                        ))
                );

        attackDistance =
                new FunctionSetting(
                        "Attack distance",
                        3, 1, 6,
                        "u", 0.05f
                );

        rotationMode =
                new FunctionSetting(
                        "Rotation mode",
                        new ArrayList<>(List.of(
                                "Default", "Multi-point", "Floating dot"
                        )),
                        "Multi-point"
                );

        multipointScale =
                new FunctionSetting(
                        "Multi-point scale",
                        80, 25, 100,
                        "%", 1
                );

        backtrack =
                new FunctionSetting(
                "Backtrack"
                );


        sprintResetMode =
                new FunctionSetting(
                        "Sprint reset mode",
                        new ArrayList<>(List.of(
                                "W-Tap", "Shift", "Packet"
                        )),
                        "W-Tap"
                );

        forceCriticalAttack =
                new FunctionSetting(
                        "Force critical attack",
                        new ArrayList<>(List.of(
                                "Off", "Predict", "Packet"
                        )),
                        "Predict"
                );

        attackThroughBlocks =
                new FunctionSetting(
                        "Attack through blocks"
                );

        automaticShieldBreak =
                new FunctionSetting(
                        "Automatic shield break"
                );

        strafeAroundTheTarget =
                new FunctionSetting(
                        "Strafe around the target"
                );

        backtrack.floatValue = 3;
        backtrack.minFloatValue = 1;
        backtrack.maxFloatValue = 5;
        backtrack.unitName = "t";
        backtrack.unitSize = 1;


        sprintResetMode.floatValue = 2;
        sprintResetMode.minFloatValue = 1;
        sprintResetMode.maxFloatValue = 5;
        sprintResetMode.unitName = "t";
        sprintResetMode.unitSize = 1;

        attackThroughBlocks.optionsList =  new ArrayList<>(List.of(
                "All", "Without collider"
        ));
        attackThroughBlocks.stringValue = "Without collider";


        this.addSetting(

                targetSelection,
                attackDistance,
                rotationMode,
                multipointScale,
                backtrack,

                sprintResetMode,
                forceCriticalAttack,
                attackThroughBlocks,
                automaticShieldBreak,
                strafeAroundTheTarget

        );

        this.setBindType(1);

    }

    public static LivingEntity currentTarget;
    public static Vec2 currentRotation = null;
    public static Vec3 targetDot = null;

    private static int shieldBlockDelay = 0;

    private static int legitStopSprinting = 0;
    private static boolean wasSprinting = false;


    private static final ArrayList<LivingEntity.BacktrackProperty> removedBPS = new ArrayList<>();

    private void reset() {

        targetDot = null;
        currentTarget = null;
        currentRotation = null;

        shieldBlockDelay = 0;

        legitStopSprinting = 0;
        wasSprinting = false;

    }

    private void startSettingUp() {

        if (currentRotation == null)
            currentRotation = new Vec2(mc.player.getYRot(), mc.player.getXRot());

        if (wasSprinting) {
            wasSprinting = false;
            mc.options.keySprint.setDown(true);
        }

        if (shieldBlockDelay > 0)
            shieldBlockDelay--;

        if (legitStopSprinting > 0)
            legitStopSprinting--;

    }

    @EventTarget(value = Priority.HIGHEST)
    void _event(TickEvent ignored) {

        if (mc.level == null || mc.player == null || !mc.player.isAlive()) {
            reset();
            return;
        }

        startSettingUp();

        currentTarget = sortTarget();

        if(!removedBPS.isEmpty()) {

            for (LivingEntity.BacktrackProperty removedBP : removedBPS) {
                LivingEntity livingEntity = removedBP.livingEntity();
                if (
                        livingEntity.tickCount - removedBP.timePoint() <= backtrack.floatValue
                )   livingEntity.backtrackProperties.add(removedBP);

            }

            removedBPS.clear();

        }

        final Vec2 rotation = getRawRotationToTarget();

        if (currentTarget.distanceTo(mc.player) > this.attackDistance.getFloatValue() + 1 || rotation == null)
            rotate(new Vec2(mc.player.getYRot(), mc.player.getXRot()));
        else
            rotate(rotation);

        breakShield();

        if (canBeAttacked() && isTargetPicked() && isFalling())
            attackTarget();

    }

    @EventTarget
    void _event(UseItemEvent useItemEvent) {

        if (
                shieldBlockDelay > 0
        ) useItemEvent.cancel();

    }

    @EventTarget
    void _event(MovementUpdateEvent movementUpdateEvent) {


        if (!this.isActivated() || !mc.player.isAlive())
            return;

        if (movementUpdateEvent.getType() != MovementUpdateEvent.TYPE.PRE)
            return;

        movementUpdateEvent.setYaw(currentRotation.x);
        movementUpdateEvent.setPitch(currentRotation.y);

        mc.player.yHeadRot = currentRotation.x;
        mc.player.xHeadRot = currentRotation.y;

    }

    @EventTarget
    void _event(BodyRotationEvent bodyRotationEvent) {
        bodyRotationEvent.yaw = currentRotation.x;
    }

    @EventTarget
    void _event(MovementCorrectionEvent movementCorrectionEvent) {

        movementCorrectionEvent.setYawRotation(currentRotation.x);
        movementCorrectionEvent.setPitchRotation(currentRotation.y);

    }

    @EventTarget
    void _event(ControllerInputEvent controllerInputEvent) {

        if (strafeAroundTheTarget.isActivated() || currentRotation == null)
            return;

        float[] closestImpulses = getClosestImpulses(controllerInputEvent.forwardImpulse, controllerInputEvent.leftImpulse);

        if (closestImpulses != null) {
            controllerInputEvent.forwardImpulse = closestImpulses[0];
            controllerInputEvent.leftImpulse = closestImpulses[1];
        }

    }

    private float[] getClosestImpulses(float xImpulse, float zImpulse) {

        if (
                xImpulse == 0 &&
                        zImpulse == 0
        ) return null;

        final double angle =
                getRotationFromImpulses(mc.player.getYRot(), xImpulse, zImpulse);

        float[][] directions =
                {
                        {-1, -1}, {-1, 0},
                        {-1, 1}, {0, -1},
                        {0, 1}, {1, -1},
                        {1, 0}, {1, 1}
                };

        float
                closestXImpulse = 0,
                closestZImpulse = 0;

        double closestDifference = Double.MAX_VALUE;

        for (float[] direction : directions) {

            double impulsesAngle =
                    getRotationFromImpulses(currentRotation.x, direction[0], direction[1]);

            double difference = Math.abs(angle - impulsesAngle);

            if (difference < closestDifference) {
                closestDifference = difference;
                closestXImpulse = direction[0];
                closestZImpulse = direction[1];
            }

        }

        return new float[]{
                closestXImpulse,
                closestZImpulse
        };

    }

    public static void fixHealth(Player player) {

        if (player == mc.player)
            return;

        for (Integer objective : mc.level.getScoreboard().listPlayerScores(ScoreHolder.fromGameProfile(player.getGameProfile())).values())
            if (objective != null && objective != 0)
                player.setHealth(objective);

    }

    private void breakShield() {

        if (
                currentTarget instanceof Player &&
                        automaticShieldBreak.isActivated() &&
                        currentTarget.isBlocking() &&
                        isTargetPicked() &&
                        !canBypassShield()
        ) {

            if (mc.player.getInventory().getItem(mc.player.getInventory().selected).getItem() instanceof AxeItem) {

                forceAttack();

                return;

            }

            int hotbarAxeSlot = Inventory.getHotbarAxeSlot();

            if (hotbarAxeSlot != -1) {

                mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotbarAxeSlot));

                forceAttack();

                mc.player.connection.send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().selected));


            }

        }

    }

    private boolean canBypassShield() {

        Vec3 vec32 = mc.player.getPosition(1.0F);
        Vec3 vec3 = currentTarget.getViewVector(1.0F);
        Vec3 vec31 = vec32.vectorTo(currentTarget.getPosition(1.0F)).normalize();

        vec31 = new Vec3(vec31.x, 0.0D, vec31.z);

        return vec31.dot(vec3) >= 0.0D;

    }

    private boolean canBeAttacked() {

        return
                currentTarget.hurtTime == 0 &&
                mc.player.getAttackStrengthScale(0.5F) > 0.9F;

    }

    private double getRotationFromImpulses(float rotation, float xImpulse, float zImpulse) {

        rotation += xImpulse < 0 ? 180 : 0;

        float xMultiplier = xImpulse != 0 ?
                Math.signum(xImpulse) * 0.5f : 1;

        rotation += zImpulse > 0 ?
                -90 * xMultiplier :
                zImpulse < 0 ? 90 * xMultiplier : 0;

        return Mth.wrapDegrees(rotation);

    }

    private void sendAttackPacket() {

        ItemStack itemStack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

        if (!itemStack.isItemEnabled(mc.level.enabledFeatures()) || mc.player.isHandsBusy() || mc.missTime > 0)
            return;

        mc.gameMode.attack(mc.player, currentTarget);
        mc.player.swing(InteractionHand.MAIN_HAND);

    }

    private void attackTarget() {

        if (mc.player.isUsingItem() && mc.player.getUseItem().getItem() instanceof ShieldItem) {
            mc.gameMode.releaseUsingItem(mc.player);
            shieldBlockDelay = 2;
        }

        boolean isPacketSRM = sprintResetMode.getStringValue().equals("Packet");

        if(!isPacketSRM) {

            if (legitStopSprinting == 0)
                legitStopSprinting = 2 + (int) sprintResetMode.floatValue;

            if (mc.player.wasSprinting || !isFalling() || !canBeAttacked() || !isTargetPicked())
                return;

        }

        if(forceCriticalAttack.getStringValue().equals("Packet"))
            mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(false, mc.player.horizontalCollision));

        if(isPacketSRM) {

            boolean wasSprinting = false;

            if(mc.player.wasSprinting) {
                mc.player.setSprinting(false);
                mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                wasSprinting = true;
            }

            this.sendAttackPacket();

            if(wasSprinting) {
                mc.player.setSprinting(true);
                mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
            }

        } else
            this.sendAttackPacket();

        if(this.getNearestBacktrack() != null)
            IDefault.displayClientChatMessage("  Tracked player at: " + (currentTarget.tickCount - this.getNearestBacktrack().timePoint()) + "t old AABB");

    }

    @EventTarget
    private void event(PacketReceiveEvent packetReceiveEvent) {

        if (mc.level == null || mc.player == null)
            return;

        if (packetReceiveEvent.getPacket() instanceof ClientboundMoveEntityPacket clientboundMoveEntityPacket && clientboundMoveEntityPacket.getEntity(mc.level) == mc.player)
            mc.player.wasGrounded = clientboundMoveEntityPacket.isOnGround();

    }

    @EventTarget
    private void event(PacketSendEvent packetSendEvent) {

        if (mc.level == null || mc.player == null)
            return;

        if (packetSendEvent.getPacket() instanceof ServerboundMovePlayerPacket serverboundMovePlayerPacket)
            mc.player.wasGrounded = serverboundMovePlayerPacket.isOnGround();

        if(packetSendEvent.getPacket() instanceof ServerboundSwingPacket packet)
            System.out.println(packet + " " + mc.player.tickCount);

        if(packetSendEvent.getPacket() instanceof ServerboundPlayerActionPacket packet)
            System.out.println(packet.getAction() + " " + mc.player.tickCount);

    }

    @EventTarget
    private void event(ControllerInputEvent controllerInputEvent) {

        if (legitStopSprinting >= 3) {

            if(sprintResetMode.getStringValue().equals("Shift"))
                controllerInputEvent.shiftKeyDown = true;
            else
                controllerInputEvent.forwardImpulse = 0;

        }

    }

    private void forceAttack() {

        if (mc.player.isUsingItem() && mc.player.getUseItem().getItem() instanceof ShieldItem) {
            mc.gameMode.releaseUsingItem(mc.player);
            shieldBlockDelay = 1;
        }

        sendAttackPacket();

    }

    private boolean isTargetPicked() {

        final AABB lastAABB = currentTarget.getBoundingBox();

        LivingEntity.BacktrackProperty backtrackProperty = this.getNearestBacktrack();

        if(backtrackProperty != null)
            currentTarget.setBoundingBox(backtrackProperty.boundingBox());

        final HitResult hitResult = RayTrace.getHitResult(currentRotation.x, currentRotation.y, this.attackDistance.getFloatValue(), this.attackThroughBlocks.isActivated() ? this.attackThroughBlocks.stringValue.equals("All") ? 1 : 0 : 2);

        currentTarget.setBoundingBox(lastAABB);

        return hitResult instanceof EntityHitResult && ((EntityHitResult) hitResult).getEntity() == currentTarget;

    }

    private boolean isFalling() {

        if(forceCriticalAttack.getStringValue().equals("Predict")) {

            boolean blockAboveHead = RayTrace.getBlockAboveHitbox(0.4).getType() == HitResult.Type.BLOCK;
            double nextY = PlayerPredictions.predictNextYDelta(mc.player) + mc.player.getBoundingBox().minY;

            return
                    mc.player.fallDistance > 0.0F && !mc.player.wasGrounded &&
                            RayTrace.getBlockUnderHitbox(nextY, blockAboveHead ? 0 : 0.1).getType() == HitResult.Type.MISS;

        } else
            return !mc.player.wasGrounded && mc.player.fallDistance > 0.0F;

    }

    public void rotate(Vec2 to) {

        float
                rawYawDelta = Mth.wrapDegrees(to.x - currentRotation.x),
                rawPitchDelta = (to.y - currentRotation.y);

        final float
                yawLimit = 45f + (float) Math.random() * 15f,
                pitchLimit = 15f + (float) Math.random() * 10f;

        float yawDelta = Mth.clamp(rawYawDelta * (0.75f + (float) Math.random() * 0.05f), -yawLimit, yawLimit);
        float pitchDelta = Mth.clamp(rawPitchDelta * (0.65f + (float) Math.random() * 0.05f), -pitchLimit, pitchLimit);

        currentRotation.x += applySensitivityMultiplier(yawDelta + (float) Math.random() * 2f - 1f);
        currentRotation.y += applySensitivityMultiplier(pitchDelta + (float) Math.random() * 2f - 1f);

        currentRotation.y = Mth.clamp(currentRotation.y, -90f, 90f);

    }

    public static double getSensitivityMultiplier() {

        double d2 = mc.options.sensitivity().get() * 0.6F + 0.2F;
        double d3 = d2 * d2 * d2;

        return d3 * 8.0;

    }

    public static float applySensitivityMultiplier(float angle) {

        double d5 = getSensitivityMultiplier();

        double step = d5 * 0.15;
        int noiseSteps = Math.max((int) Math.round(Math.random() * 6) - 3, 2);

        return (float) (Math.round((double)(angle / 0.15f) / d5) * d5) * 0.15f + noiseSteps * (float) step;

    }

    private static final Vec3 hitBoxDotPosition = new Vec3(0, 0, 0);
    private static final Vec3 hitBoxDotVector = new Vec3(Math.random() * 0.6 + .4, Math.random() * 0.6 + .4, Math.random() * 0.6 + .4);

    int lastSortTick = 0;

    private LivingEntity.BacktrackProperty getNearestBacktrack() {

        if(!backtrack.getCanBeActivated())
            return null;

        if(lastSortTick != mc.player.tickCount) {

            currentTarget.backtrackProperties.removeIf(e -> currentTarget.tickCount - e.timePoint() > backtrack.floatValue);
            currentTarget.backtrackProperties.removeIf(e -> currentTarget.tickCount - e.timePoint() == 0);
            currentTarget.backtrackProperties.sort((Comparator.comparingDouble(o -> mc.player.position().distanceTo(o.position()))));

            lastSortTick = mc.player.tickCount;

        }

        if(currentTarget.backtrackProperties.isEmpty())
            return null;

        return currentTarget.backtrackProperties.getFirst();

    }

    private Vec2 getRawRotationToTarget() {

        AABB boundingBox = currentTarget.getBoundingBox();
        Vec3 position = currentTarget.position();

        LivingEntity.BacktrackProperty backtrackProperty = this.getNearestBacktrack();

        targetDot = null;

        if(backtrackProperty != null) {

            boundingBox = backtrackProperty.boundingBox();
            position = backtrackProperty.position();

        }

        float targetY = (float) Mth.clamp(mc.player.getEyeY(), boundingBox.minY, boundingBox.maxY);

        double
                xDifference = position.x - mc.player.getX(),
                yDifference = targetY - mc.player.getEyeY(),
                zDifference = position.z - mc.player.getZ();

        Vec3 tempDot = new Vec3(0, targetY - position.y, 0);

        float multipointGridSize = 1F;

        if (rotationMode.getStringValue().equals("Multi-point")) {

            float bbWidth = (float) (boundingBox.getXsize() * multipointGridSize);
            float bbHeight = (float) (boundingBox.getYsize() * multipointGridSize);

            final int
                    maxMultipointCountXZ = (int) (bbWidth * 20f),
                    maxMultipointCountY = (int) (bbHeight * 20f);

            final int
                    multipointCountXZ = (int) Math.round(multipointScale.getFloatValue() * 0.01d * maxMultipointCountXZ),
                    multipointCountY = (int) Math.round(multipointScale.getFloatValue() * 0.01d * maxMultipointCountY);

            final double
                    multipointOffsetXZ = bbWidth / multipointCountXZ,
                    multipointOffsetY = bbHeight / multipointCountY;

            final double
                    startX = position.x - bbWidth / 2,
                    startY = position.y,
                    startZ = position.z - bbWidth / 2;

            final ArrayList<Vec3> allMultiPoints = new ArrayList<>();

            // first face
            for (int xMultipoint = 0; xMultipoint < multipointCountXZ; xMultipoint++) {

                final double x = startX + multipointOffsetXZ * xMultipoint;

                for (int yMultipoint = 0; yMultipoint < multipointCountY; yMultipoint++) {

                    final double y = startY + multipointOffsetY * yMultipoint;

                    allMultiPoints.add(new Vec3(x, y, startZ));

                }

            }

            // second face
            for (int zMultipoint = 0; zMultipoint < multipointCountXZ; zMultipoint++) {

                final double z = startZ + multipointOffsetXZ * (zMultipoint + 1);

                for (int yMultipoint = 0; yMultipoint < multipointCountY; yMultipoint++) {

                    final double y = startY + multipointOffsetY * yMultipoint;

                    allMultiPoints.add(new Vec3(startX, y, z));

                }

            }

            // third face
            for (int xMultipoint = 0; xMultipoint < multipointCountXZ; xMultipoint++) {

                final double x = startX + multipointOffsetXZ * (xMultipoint + 1);

                for (int yMultipoint = 0; yMultipoint < multipointCountY; yMultipoint++) {

                    final double y = startY + multipointOffsetY * yMultipoint;

                    allMultiPoints.add(new Vec3(x, y, startZ + bbWidth));

                }

            }

            // fourth face
            for (int zMultipoint = 0; zMultipoint < multipointCountXZ; zMultipoint++) {

                final double z = startZ + multipointOffsetXZ * zMultipoint;

                for (int yMultipoint = 0; yMultipoint < multipointCountY; yMultipoint++) {

                    final double y = startY + multipointOffsetY * yMultipoint;

                    allMultiPoints.add(new Vec3(startX + bbWidth, y, z));

                }

            }

            // fifth face
            for (int xMultipoint = 0; xMultipoint < (multipointCountXZ - 1); xMultipoint++) {

                final double x = startX + multipointOffsetXZ * (xMultipoint + 1);

                for (int zMultipoint = 0; zMultipoint < (multipointCountXZ - 1); zMultipoint++) {

                    final double z = startZ + multipointOffsetXZ * (zMultipoint + 1);

                    allMultiPoints.add(new Vec3(x, startY, z));

                }

            }

            // sixth face
            for (int xMultipoint = 0; xMultipoint < (multipointCountXZ + 1); xMultipoint++) {

                final double x = startX + multipointOffsetXZ * xMultipoint;

                for (int zMultipoint = 0; zMultipoint < (multipointCountXZ + 1); zMultipoint++) {

                    final double z = startZ + multipointOffsetXZ * zMultipoint;

                    allMultiPoints.add(new Vec3(x, startY + bbHeight, z));

                }

            }

            final ArrayList<Vec3> visibleMultiPoints = new ArrayList<>();

            if(this.attackThroughBlocks.isActivated() && this.attackThroughBlocks.getStringValue().equals("All"))
                visibleMultiPoints.addAll(allMultiPoints);
            else {

                float prevDistance = Float.MAX_VALUE;

                for (Vec3 point : allMultiPoints) {

                    float actualDistance = (float) mc.player.getEyePosition().distanceTo(point); // some optimisation

                    if(actualDistance > prevDistance)
                        continue;

                    prevDistance = actualDistance;

                    final double
                            xPointDifference = point.x - mc.player.getX(),
                            yPointDifference = point.y - mc.player.getEyeY(),
                            zPointDifference = point.z - mc.player.getZ();

                    final AABB lastAABB = currentTarget.getBoundingBox();

                    currentTarget.setBoundingBox(boundingBox);

                    final HitResult hitResult =
                            RayTrace.getHitResultByDifferences(
                                    xPointDifference,
                                    yPointDifference,
                                    zPointDifference,
                                    6,
                                    this.attackThroughBlocks.isActivated() ? this.attackThroughBlocks.stringValue.equals("All") ? 1 : 0 : 2
                            );

                    currentTarget.setBoundingBox(lastAABB);

                    if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() == currentTarget)
                        visibleMultiPoints.add(point);

                }

            }
            if (visibleMultiPoints.isEmpty()) {
                if(backtrackProperty != null) {
                    removedBPS.add(backtrackProperty);
                    currentTarget.backtrackProperties.remove(backtrackProperty);
                    return getRawRotationToTarget();
                }
                return null;
            }

            double lastNearestVisiblePointDistance = Double.MAX_VALUE;
            Vec3 nearestVisiblePoint = new Vec3(currentTarget.getX(), currentTarget.getEyeY(), currentTarget.getZ());

            for (Vec3 visiblePoint : visibleMultiPoints) {

                final double distanceToVisiblePoint = visiblePoint.distanceTo(new Vec3(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ()));

                if (distanceToVisiblePoint < lastNearestVisiblePointDistance) {
                    lastNearestVisiblePointDistance = distanceToVisiblePoint;
                    nearestVisiblePoint = visiblePoint;
                }

            }

            tempDot = new Vec3(nearestVisiblePoint.x - position.x, nearestVisiblePoint.y - position.y, nearestVisiblePoint.z - position.z);

            xDifference = nearestVisiblePoint.x - mc.player.getX();
            yDifference = nearestVisiblePoint.y - mc.player.getEyeY();
            zDifference = nearestVisiblePoint.z - mc.player.getZ();

        } else if (rotationMode.getStringValue().equals("Floating dot")) {

            if (mc.player.tickCount % 20 == 0) {
                hitBoxDotVector.x = Math.random() * 0.6 + .4;
                hitBoxDotVector.y = Math.random() * 0.6 + .4;
                hitBoxDotVector.z = Math.random() * 0.6 + .4;
            }

            final double
                    bbWidth = boundingBox.getXsize(),
                    bbHeight = boundingBox.getYsize(),
                    bbDepth = boundingBox.getZsize();

            hitBoxDotPosition.x += hitBoxDotVector.x * 0.1f;
            hitBoxDotPosition.y += hitBoxDotVector.y * 0.1f;
            hitBoxDotPosition.z += hitBoxDotVector.z * 0.1f;

            final float restitution = 1f;

            if (hitBoxDotPosition.x >= bbWidth || hitBoxDotPosition.x <= 0) {
                hitBoxDotVector.x = -hitBoxDotVector.x * restitution;
            }
            if (hitBoxDotPosition.y >= bbHeight || hitBoxDotPosition.y <= 0) {
                hitBoxDotVector.y = -hitBoxDotVector.y * restitution;
            }
            if (hitBoxDotPosition.z >= bbDepth || hitBoxDotPosition.z <= 0) {
                hitBoxDotVector.z = -hitBoxDotVector.z * restitution;
            }

            hitBoxDotPosition.x = Math.max(0, Math.min(hitBoxDotPosition.x, bbWidth));
            hitBoxDotPosition.y = Math.max(0, Math.min(hitBoxDotPosition.y, bbHeight));
            hitBoxDotPosition.z = Math.max(0, Math.min(hitBoxDotPosition.z, bbDepth));

            xDifference = position.x + hitBoxDotPosition.x - bbWidth / 2 - mc.player.getX();
            yDifference = position.y + hitBoxDotPosition.y - mc.player.getEyeY();
            zDifference = position.z + hitBoxDotPosition.z - bbDepth / 2 - mc.player.getZ();

            tempDot = new Vec3(hitBoxDotPosition.x - bbWidth / 2, hitBoxDotPosition.y, hitBoxDotPosition.z - bbDepth / 2);

        }

        final AABB lastAABB = currentTarget.getBoundingBox();

        currentTarget.setBoundingBox(boundingBox);

        final HitResult hitResult =
                RayTrace.getHitResultByDifferences(
                        xDifference,
                        yDifference,
                        zDifference,
                        6,
                        this.attackThroughBlocks.isActivated() ? this.attackThroughBlocks.stringValue.equals("All") ? 1 : 0 : 2
                );

        currentTarget.setBoundingBox(lastAABB);

        if (!(hitResult instanceof EntityHitResult entityHitResult) || entityHitResult.getEntity().getId() != currentTarget.getId()) {
            if(backtrackProperty != null) {
                removedBPS.add(backtrackProperty);
                currentTarget.backtrackProperties.remove(backtrackProperty);
                return getRawRotationToTarget();
            }
            return null;
        }

        targetDot = tempDot;

        final Vec2 rotation =
                RayTrace.getRotationForDifferences(
                        xDifference,
                        yDifference,
                        zDifference
                );

        return new Vec2(
                rotation.x,
                rotation.y
        );

    }

    private LivingEntity sortTarget() {

        ArrayList<LivingEntity> potentialTargets = new ArrayList<>();

        for (Entity entity : mc.level.entitiesForRendering())
            if (isSelectedEntity(entity))
                potentialTargets.add((LivingEntity) entity);

        if (potentialTargets.isEmpty())
            return null;

        return Collections.min(potentialTargets, (firstTarget, secondTarget) -> {

            double
                    distanceToFirstTarget = mc.player.distanceTo(firstTarget),
                    distanceToSecondTarget = mc.player.distanceTo(secondTarget);

            return Double.compare(distanceToFirstTarget, distanceToSecondTarget);

        });

    }

    private boolean isSelectedEntity(Entity entity) {

        if (entity instanceof ArmorStand || entity.is(mc.player))
            return false;

        if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isAlive() || livingEntity.isInvulnerable())
            return false;

        if (
                livingEntity.isInvisible() &&
                        !targetSelection.getSelectedOptionsList().contains("Invisible") && livingEntity.armorAttribute.armor.isEmpty()
        ) return false;

        if (
                targetSelection.getSelectedOptionsList().contains("Players") &&
                        livingEntity instanceof Player player && !player.isCreative() && !player.isSpectator() && !isBot(player)
        ) {
            fixHealth(player);
            return true;
        }

        if (
                targetSelection.getSelectedOptionsList().contains("Monsters") &&
                        entity instanceof Monster
        ) return true;

        if (
                !targetSelection.getSelectedOptionsList().contains("Cats & dogs") &&
                        (entity instanceof Wolf || entity instanceof Cat)
        ) return false;

        if (
                targetSelection.getSelectedOptionsList().contains("Animals") &&
                        entity instanceof Animal
        ) return true;

        return targetSelection.getSelectedOptionsList().contains("Villagers") &&
                entity instanceof Villager;

    }

    public static boolean isBot(LivingEntity livingEntity) {

        if (!(livingEntity instanceof Player hypBot))
            return false;

        if (!hypBot.getGameProfile().getProperties().containsKey("textures"))
            return false;

        return hypBot.getUUID().version() == 2;

    }

    public void onDeactivated() {

        if (mc.player != null) {

            float delta = currentRotation.x - mc.player.getYRot();
            float compensatedY = mc.player.getYRot() + (delta - delta % 360);

            float yawDelta = compensatedY - currentRotation.x;
            float pitchDelta = mc.player.getXRot() - currentRotation.y;

            mc.player.setYRot(currentRotation.x + applySensitivityMultiplier(yawDelta));
            mc.player.setXRot(currentRotation.y + applySensitivityMultiplier(pitchDelta));

            mc.player.yRotO = mc.player.getYRot() + (delta - delta % 360);
            mc.player.yRot = mc.player.getYRot() + (delta - delta % 360);
            mc.player.yBob = mc.player.getViewYRot(mc.getDeltaTracker().getRealtimeDeltaTicks());
            mc.player.yBobO = mc.player.getViewYRot(mc.getDeltaTracker().getRealtimeDeltaTicks());

        }

        reset();

    }

}
