package net.minecraft.world.phys;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

public class Vec3 implements Position {
    public static final Codec<Vec3> CODEC = Codec.DOUBLE
        .listOf()
        .comapFlatMap(
            doublesIn -> Util.fixedSize((List<Double>)doublesIn, 3).map(doubles2In -> new Vec3(doubles2In.get(0), doubles2In.get(1), doubles2In.get(2))),
            vecIn -> List.of(vecIn.x(), vecIn.y(), vecIn.z())
        );
    public static final StreamCodec<ByteBuf, Vec3> STREAM_CODEC = new StreamCodec<ByteBuf, Vec3>() {
        public Vec3 decode(ByteBuf p_367785_) {
            return FriendlyByteBuf.readVec3(p_367785_);
        }

        public void encode(ByteBuf p_361343_, Vec3 p_364181_) {
            FriendlyByteBuf.writeVec3(p_361343_, p_364181_);
        }
    };
    public static final Vec3 ZERO = new Vec3(0.0, 0.0, 0.0);
    public double x;
    public double y;
    public double z;

    public static Vec3 fromRGB24(int pPacked) {
        double d0 = (double)(pPacked >> 16 & 0xFF) / 255.0;
        double d1 = (double)(pPacked >> 8 & 0xFF) / 255.0;
        double d2 = (double)(pPacked & 0xFF) / 255.0;
        return new Vec3(d0, d1, d2);
    }

    public static Vec3 atLowerCornerOf(Vec3i pToCopy) {
        return new Vec3((double)pToCopy.getX(), (double)pToCopy.getY(), (double)pToCopy.getZ());
    }

    public static Vec3 atLowerCornerWithOffset(Vec3i pToCopy, double pOffsetX, double pOffsetY, double pOffsetZ) {
        return new Vec3((double)pToCopy.getX() + pOffsetX, (double)pToCopy.getY() + pOffsetY, (double)pToCopy.getZ() + pOffsetZ);
    }

    public static Vec3 atCenterOf(Vec3i pToCopy) {
        return atLowerCornerWithOffset(pToCopy, 0.5, 0.5, 0.5);
    }

    public static Vec3 atBottomCenterOf(Vec3i pToCopy) {
        return atLowerCornerWithOffset(pToCopy, 0.5, 0.0, 0.5);
    }

    public static Vec3 upFromBottomCenterOf(Vec3i pToCopy, double pVerticalOffset) {
        return atLowerCornerWithOffset(pToCopy, 0.5, pVerticalOffset, 0.5);
    }

    public Vec3(double pX, double pY, double pZ) {
        this.x = pX;
        this.y = pY;
        this.z = pZ;
    }

    public Vec3(Vector3f pVector) {
        this((double)pVector.x(), (double)pVector.y(), (double)pVector.z());
    }

    public Vec3(Vec3i pVector) {
        this((double)pVector.getX(), (double)pVector.getY(), (double)pVector.getZ());
    }

    public Vec3 vectorTo(Vec3 pVec) {
        return new Vec3(pVec.x - this.x, pVec.y - this.y, pVec.z - this.z);
    }

    public Vec3 normalize() {
        double d0 = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        return d0 < 1.0E-5F ? ZERO : new Vec3(this.x / d0, this.y / d0, this.z / d0);
    }

    public double dot(Vec3 pVec) {
        return this.x * pVec.x + this.y * pVec.y + this.z * pVec.z;
    }

    public Vec3 cross(Vec3 pVec) {
        return new Vec3(
            this.y * pVec.z - this.z * pVec.y,
            this.z * pVec.x - this.x * pVec.z,
            this.x * pVec.y - this.y * pVec.x
        );
    }

    public Vec3 subtract(Vec3 pVec) {
        return this.subtract(pVec.x, pVec.y, pVec.z);
    }

    public Vec3 subtract(double pAmount) {
        return this.subtract(pAmount, pAmount, pAmount);
    }

    public Vec3 subtract(double pX, double pY, double pZ) {
        return this.add(-pX, -pY, -pZ);
    }

    public Vec3 add(double pAmount) {
        return this.add(pAmount, pAmount, pAmount);
    }

    public Vec3 add(Vec3 pVec) {
        return this.add(pVec.x, pVec.y, pVec.z);
    }

    public Vec3 add(double pX, double pY, double pZ) {
        return new Vec3(this.x + pX, this.y + pY, this.z + pZ);
    }

    public boolean closerThan(Position pPos, double pDistance) {
        return this.distanceToSqr(pPos.x(), pPos.y(), pPos.z()) < pDistance * pDistance;
    }

    public double distanceTo(Vec3 pVec) {
        double d0 = pVec.x - this.x;
        double d1 = pVec.y - this.y;
        double d2 = pVec.z - this.z;
        return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
    }

    public double distanceToSqr(Vec3 pVec) {
        double d0 = pVec.x - this.x;
        double d1 = pVec.y - this.y;
        double d2 = pVec.z - this.z;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public double distanceToSqr(double pX, double pY, double pZ) {
        double d0 = pX - this.x;
        double d1 = pY - this.y;
        double d2 = pZ - this.z;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public boolean closerThan(Vec3 pPos, double pHorizontalDistance, double pVerticalDistance) {
        double d0 = pPos.x() - this.x;
        double d1 = pPos.y() - this.y;
        double d2 = pPos.z() - this.z;
        return Mth.lengthSquared(d0, d2) < Mth.square(pHorizontalDistance) && Math.abs(d1) < pVerticalDistance;
    }

    public Vec3 scale(double pFactor) {
        return this.multiply(pFactor, pFactor, pFactor);
    }

    public Vec3 reverse() {
        return this.scale(-1.0);
    }

    public Vec3 multiply(Vec3 pVec) {
        return this.multiply(pVec.x, pVec.y, pVec.z);
    }

    public Vec3 multiply(double pFactorX, double pFactorY, double pFactorZ) {
        return new Vec3(this.x * pFactorX, this.y * pFactorY, this.z * pFactorZ);
    }

    public Vec3 horizontal() {
        return new Vec3(this.x, 0.0, this.z);
    }

    public Vec3 offsetRandom(RandomSource pRandom, float pFactor) {
        return this.add(
            (double)((pRandom.nextFloat() - 0.5F) * pFactor),
            (double)((pRandom.nextFloat() - 0.5F) * pFactor),
            (double)((pRandom.nextFloat() - 0.5F) * pFactor)
        );
    }

    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public double lengthSqr() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public double horizontalDistance() {
        return Math.sqrt(this.x * this.x + this.z * this.z);
    }

    public double horizontalDistanceSqr() {
        return this.x * this.x + this.z * this.z;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else if (pOther instanceof Vec3 vec3) {
            if (Double.compare(vec3.x, this.x) != 0) {
                return false;
            } else {
                return Double.compare(vec3.y, this.y) != 0 ? false : Double.compare(vec3.z, this.z) == 0;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        long i = Double.doubleToLongBits(this.x);
        int j = (int)(i ^ i >>> 32);
        i = Double.doubleToLongBits(this.y);
        j = 31 * j + (int)(i ^ i >>> 32);
        i = Double.doubleToLongBits(this.z);
        return 31 * j + (int)(i ^ i >>> 32);
    }

    @Override
    public String toString() {
        return "(" + this.x + ", " + this.y + ", " + this.z + ")";
    }

    public Vec3 lerp(Vec3 pTo, double pDelta) {
        return new Vec3(
            Mth.lerp(pDelta, this.x, pTo.x),
            Mth.lerp(pDelta, this.y, pTo.y),
            Mth.lerp(pDelta, this.z, pTo.z)
        );
    }

    public Vec3 xRot(float pPitch) {
        float f = Mth.cos(pPitch);
        float f1 = Mth.sin(pPitch);
        double d0 = this.x;
        double d1 = this.y * (double)f + this.z * (double)f1;
        double d2 = this.z * (double)f - this.y * (double)f1;
        return new Vec3(d0, d1, d2);
    }

    public Vec3 yRot(float pYaw) {
        float f = Mth.cos(pYaw);
        float f1 = Mth.sin(pYaw);
        double d0 = this.x * (double)f + this.z * (double)f1;
        double d1 = this.y;
        double d2 = this.z * (double)f - this.x * (double)f1;
        return new Vec3(d0, d1, d2);
    }

    public Vec3 zRot(float pRoll) {
        float f = Mth.cos(pRoll);
        float f1 = Mth.sin(pRoll);
        double d0 = this.x * (double)f + this.y * (double)f1;
        double d1 = this.y * (double)f - this.x * (double)f1;
        double d2 = this.z;
        return new Vec3(d0, d1, d2);
    }

    public static Vec3 directionFromRotation(Vec2 pVec) {
        return directionFromRotation(pVec.x, pVec.y);
    }

    public static Vec3 directionFromRotation(float pPitch, float pYaw) {
        float f = Mth.cos(-pYaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f1 = Mth.sin(-pYaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f2 = -Mth.cos(-pPitch * (float) (Math.PI / 180.0));
        float f3 = Mth.sin(-pPitch * (float) (Math.PI / 180.0));
        return new Vec3((double)(f1 * f2), (double)f3, (double)(f * f2));
    }

    public Vec3 align(EnumSet<Direction.Axis> pAxes) {
        double d0 = pAxes.contains(Direction.Axis.X) ? (double)Mth.floor(this.x) : this.x;
        double d1 = pAxes.contains(Direction.Axis.Y) ? (double)Mth.floor(this.y) : this.y;
        double d2 = pAxes.contains(Direction.Axis.Z) ? (double)Mth.floor(this.z) : this.z;
        return new Vec3(d0, d1, d2);
    }

    public double get(Direction.Axis pAxis) {
        return pAxis.choose(this.x, this.y, this.z);
    }

    public Vec3 with(Direction.Axis pAxis, double pLength) {
        double d0 = pAxis == Direction.Axis.X ? pLength : this.x;
        double d1 = pAxis == Direction.Axis.Y ? pLength : this.y;
        double d2 = pAxis == Direction.Axis.Z ? pLength : this.z;
        return new Vec3(d0, d1, d2);
    }

    public Vec3 relative(Direction pDirection, double pLength) {
        Vec3i vec3i = pDirection.getUnitVec3i();
        return new Vec3(
            this.x + pLength * (double)vec3i.getX(),
            this.y + pLength * (double)vec3i.getY(),
            this.z + pLength * (double)vec3i.getZ()
        );
    }

    @Override
    public final double x() {
        return this.x;
    }

    @Override
    public final double y() {
        return this.y;
    }

    @Override
    public final double z() {
        return this.z;
    }

    public Vector3f toVector3f() {
        return new Vector3f((float)this.x, (float)this.y, (float)this.z);
    }

    public Vec3 projectedOn(Vec3 pVector) {
        return pVector.lengthSqr() == 0.0 ? pVector : pVector.scale(this.dot(pVector)).scale(1.0 / pVector.lengthSqr());
    }
}