package net.minecraft.client.renderer.culling;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import net.optifine.util.MathUtils;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Frustum {
    public static final int OFFSET_STEP = 4;
    private final FrustumIntersection intersection = new FrustumIntersection();
    private final Matrix4f matrix = new Matrix4f();
    protected Vector4f viewVector;
    private double camX;
    private double camY;
    private double camZ;
    public boolean disabled = false;
    protected boolean usePlanes = false;
    protected final Vector4f[] frustum = new Vector4f[6];
    protected static final int INSIDE = -2;
    protected static final int OUTSIDE = -3;

    public Frustum(Matrix4f pFrustum, Matrix4f pProjection) {
        this.calculateFrustum(pFrustum, pProjection);
    }

    public Frustum(Frustum pOther) {
        this.intersection.set(pOther.matrix);
        this.matrix.set(pOther.matrix);
        this.camX = pOther.camX;
        this.camY = pOther.camY;
        this.camZ = pOther.camZ;
        this.viewVector = pOther.viewVector;
        this.disabled = pOther.disabled;
        this.usePlanes = pOther.usePlanes;
        System.arraycopy(pOther.frustum, 0, this.frustum, 0, pOther.frustum.length);
    }

    public Frustum offsetToFullyIncludeCameraCube(int pOffset) {
        double d0 = Math.floor(this.camX / (double)pOffset) * (double)pOffset;
        double d1 = Math.floor(this.camY / (double)pOffset) * (double)pOffset;
        double d2 = Math.floor(this.camZ / (double)pOffset) * (double)pOffset;
        double d3 = Math.ceil(this.camX / (double)pOffset) * (double)pOffset;
        double d4 = Math.ceil(this.camY / (double)pOffset) * (double)pOffset;
        int i = 0;

        for (double d5 = Math.ceil(this.camZ / (double)pOffset) * (double)pOffset;
            this.intersection
                    .intersectAab(
                        (float)(d0 - this.camX),
                        (float)(d1 - this.camY),
                        (float)(d2 - this.camZ),
                        (float)(d3 - this.camX),
                        (float)(d4 - this.camY),
                        (float)(d5 - this.camZ)
                    )
                != -2;
            this.camZ = this.camZ - (double)(this.viewVector.z() * 4.0F)
        ) {
            this.camX = this.camX - (double)(this.viewVector.x() * 4.0F);
            this.camY = this.camY - (double)(this.viewVector.y() * 4.0F);
            if (i++ > 10) {
                break;
            }
        }

        return this;
    }

    public void prepare(double pCamX, double pCamY, double pCamZ) {
        this.camX = pCamX;
        this.camY = pCamY;
        this.camZ = pCamZ;
    }

    private void calculateFrustum(Matrix4f pFrustum, Matrix4f pProjection) {
        pProjection.mul(pFrustum, this.matrix);
        this.intersection.set(this.matrix);
        this.viewVector = this.matrix.transformTranspose(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
        Matrix4f matrix4f = new Matrix4f(this.matrix).transpose();
        this.setFrustumPlane(matrix4f, -1, 0, 0, 0);
        this.setFrustumPlane(matrix4f, 1, 0, 0, 1);
        this.setFrustumPlane(matrix4f, 0, -1, 0, 2);
        this.setFrustumPlane(matrix4f, 0, 1, 0, 3);
        this.setFrustumPlane(matrix4f, 0, 0, -1, 4);
        this.setFrustumPlane(matrix4f, 0, 0, 1, 5);
    }

    public boolean isVisible(AABB pAabb) {
        if (pAabb == IForgeBlockEntity.INFINITE_EXTENT_AABB) {
            return true;
        } else {
            int i = this.cubeInFrustum(pAabb.minX, pAabb.minY, pAabb.minZ, pAabb.maxX, pAabb.maxY, pAabb.maxZ);
            return i == -2 || i == -1;
        }
    }

    public int cubeInFrustum(BoundingBox pBoundingBox) {
        return this.cubeInFrustum(
            (double)pBoundingBox.minX(),
            (double)pBoundingBox.minY(),
            (double)pBoundingBox.minZ(),
            (double)(pBoundingBox.maxX() + 1),
            (double)(pBoundingBox.maxY() + 1),
            (double)(pBoundingBox.maxZ() + 1)
        );
    }

    private int cubeInFrustum(double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ) {
        if (this.disabled) {
            return -2;
        } else {
            float f = (float)(pMinX - this.camX);
            float f1 = (float)(pMinY - this.camY);
            float f2 = (float)(pMinZ - this.camZ);
            float f3 = (float)(pMaxX - this.camX);
            float f4 = (float)(pMaxY - this.camY);
            float f5 = (float)(pMaxZ - this.camZ);
            return this.usePlanes ? this.isBoxInFrustumRaw(f, f1, f2, f3, f4, f5) : this.intersection.intersectAab(f, f1, f2, f3, f4, f5);
        }
    }

    public Vector4f[] getFrustumPoints() {
        Vector4f[] avector4f = new Vector4f[]{
            new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F),
            new Vector4f(1.0F, -1.0F, -1.0F, 1.0F),
            new Vector4f(1.0F, 1.0F, -1.0F, 1.0F),
            new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F),
            new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F),
            new Vector4f(1.0F, -1.0F, 1.0F, 1.0F),
            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
            new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F)
        };
        Matrix4f matrix4f = this.matrix.invert(new Matrix4f());

        for (int i = 0; i < 8; i++) {
            matrix4f.transform(avector4f[i]);
            avector4f[i].div(avector4f[i].w());
        }

        return avector4f;
    }

    public double getCamX() {
        return this.camX;
    }

    public double getCamY() {
        return this.camY;
    }

    public double getCamZ() {
        return this.camZ;
    }

    private int isBoxInFrustumRaw(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        for (int i = 0; i < 6; i++) {
            Vector4f vector4f = this.frustum[i];
            float f = vector4f.x();
            float f1 = vector4f.y();
            float f2 = vector4f.z();
            float f3 = vector4f.w();
            if (f * minX + f1 * minY + f2 * minZ + f3 <= 0.0F
                && f * maxX + f1 * minY + f2 * minZ + f3 <= 0.0F
                && f * minX + f1 * maxY + f2 * minZ + f3 <= 0.0F
                && f * maxX + f1 * maxY + f2 * minZ + f3 <= 0.0F
                && f * minX + f1 * minY + f2 * maxZ + f3 <= 0.0F
                && f * maxX + f1 * minY + f2 * maxZ + f3 <= 0.0F
                && f * minX + f1 * maxY + f2 * maxZ + f3 <= 0.0F
                && f * maxX + f1 * maxY + f2 * maxZ + f3 <= 0.0F) {
                return -3;
            }
        }

        return -2;
    }

    public boolean isBoxInFrustumFully(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (this.disabled) {
            return true;
        } else {
            float f = (float)minX;
            float f1 = (float)minY;
            float f2 = (float)minZ;
            float f3 = (float)maxX;
            float f4 = (float)maxY;
            float f5 = (float)maxZ;

            for (int i = 0; i < 6; i++) {
                Vector4f vector4f = this.frustum[i];
                float f6 = vector4f.x();
                float f7 = vector4f.y();
                float f8 = vector4f.z();
                float f9 = vector4f.w();
                if (i < 4) {
                    if (f6 * f + f7 * f1 + f8 * f2 + f9 <= 0.0F
                        || f6 * f3 + f7 * f1 + f8 * f2 + f9 <= 0.0F
                        || f6 * f + f7 * f4 + f8 * f2 + f9 <= 0.0F
                        || f6 * f3 + f7 * f4 + f8 * f2 + f9 <= 0.0F
                        || f6 * f + f7 * f1 + f8 * f5 + f9 <= 0.0F
                        || f6 * f3 + f7 * f1 + f8 * f5 + f9 <= 0.0F
                        || f6 * f + f7 * f4 + f8 * f5 + f9 <= 0.0F
                        || f6 * f3 + f7 * f4 + f8 * f5 + f9 <= 0.0F) {
                        return false;
                    }
                } else if (f6 * f + f7 * f1 + f8 * f2 + f9 <= 0.0F
                    && f6 * f3 + f7 * f1 + f8 * f2 + f9 <= 0.0F
                    && f6 * f + f7 * f4 + f8 * f2 + f9 <= 0.0F
                    && f6 * f3 + f7 * f4 + f8 * f2 + f9 <= 0.0F
                    && f6 * f + f7 * f1 + f8 * f5 + f9 <= 0.0F
                    && f6 * f3 + f7 * f1 + f8 * f5 + f9 <= 0.0F
                    && f6 * f + f7 * f4 + f8 * f5 + f9 <= 0.0F
                    && f6 * f3 + f7 * f4 + f8 * f5 + f9 <= 0.0F) {
                    return false;
                }
            }

            return true;
        }
    }

    public double getCameraX() {
        return this.camX;
    }

    public double getCameraY() {
        return this.camY;
    }

    public double getCameraZ() {
        return this.camZ;
    }

    private void setFrustumPlane(Matrix4f matrixIn, int xIn, int yIn, int zIn, int wIn) {
        Vector4f vector4f = new Vector4f((float)xIn, (float)yIn, (float)zIn, 1.0F);
        MathUtils.transform(vector4f, matrixIn);
        vector4f.normalize();
        this.frustum[wIn] = vector4f;
    }
}