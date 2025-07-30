package net.minecraft.util;

import java.util.Locale;
import java.util.UUID;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import net.minecraft.Util;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.optifine.util.MathUtils;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.lang3.math.NumberUtils;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Mth {
    private static final long UUID_VERSION = 61440L;
    private static final long UUID_VERSION_TYPE_4 = 16384L;
    private static final long UUID_VARIANT = -4611686018427387904L;
    private static final long UUID_VARIANT_2 = Long.MIN_VALUE;
    public static final float PI = (float) Math.PI;
    public static final float HALF_PI = (float) (Math.PI / 2);
    public static final float TWO_PI = (float) (Math.PI * 2);
    public static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    public static final float RAD_TO_DEG = 180.0F / (float)Math.PI;
    public static final float EPSILON = 1.0E-5F;
    public static final float SQRT_OF_TWO = sqrt(2.0F);
    private static final float SIN_SCALE = 10430.378F;
    public static final Vector3f Y_AXIS = new Vector3f(0.0F, 1.0F, 0.0F);
    public static final Vector3f X_AXIS = new Vector3f(1.0F, 0.0F, 0.0F);
    public static final Vector3f Z_AXIS = new Vector3f(0.0F, 0.0F, 1.0F);
    private static final float[] SIN = Util.make(new float[65536], p_14076_0_ -> {
        for (int i = 0; i < p_14076_0_.length; i++) {
            p_14076_0_[i] = (float)Math.sin((double)i * Math.PI * 2.0 / 65536.0);
        }
    });
    private static final RandomSource RANDOM = RandomSource.createThreadSafe();
    private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{
        0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9
    };
    private static final double ONE_SIXTH = 0.16666666666666666;
    private static final int FRAC_EXP = 8;
    private static final int LUT_SIZE = 257;
    private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
    private static final double[] ASIN_TAB = new double[257];
    private static final double[] COS_TAB = new double[257];
    private static final int SIN_BITS = 12;
    private static final int SIN_MASK = 4095;
    private static final int SIN_COUNT = 4096;
    private static final int SIN_COUNT_D4 = 1024;
    public static final float PI2 = MathUtils.roundToFloat(Math.PI * 2);
    public static final float PId2 = MathUtils.roundToFloat(Math.PI / 2);
    private static final float radToIndex = MathUtils.roundToFloat(651.8986469044033);
    public static final float deg2Rad = MathUtils.roundToFloat(Math.PI / 180.0);
    private static final float[] SIN_TABLE_FAST = new float[4096];
    public static boolean fastMath = false;

    public static float sin(float pValue) {
        return fastMath ? SIN_TABLE_FAST[(int)(pValue * radToIndex) & 4095] : SIN[(int)(pValue * 10430.378F) & 65535];
    }

    public static float cos(float pValue) {
        return fastMath ? SIN_TABLE_FAST[(int)(pValue * radToIndex + 1024.0F) & 4095] : SIN[(int)(pValue * 10430.378F + 16384.0F) & 65535];
    }

    public static float sqrt(float pValue) {
        return (float)Math.sqrt((double)pValue);
    }

    public static int floor(float pValue) {
        int i = (int)pValue;
        return pValue < (float)i ? i - 1 : i;
    }

    public static int floor(double pValue) {
        int i = (int)pValue;
        return pValue < (double)i ? i - 1 : i;
    }

    public static long lfloor(double pValue) {
        long i = (long)pValue;
        return pValue < (double)i ? i - 1L : i;
    }

    public static float abs(float pValue) {
        return Math.abs(pValue);
    }

    public static int abs(int pValue) {
        return Math.abs(pValue);
    }

    public static int ceil(float pValue) {
        int i = (int)pValue;
        return pValue > (float)i ? i + 1 : i;
    }

    public static int ceil(double pValue) {
        int i = (int)pValue;
        return pValue > (double)i ? i + 1 : i;
    }

    public static int clamp(int pValue, int pMin, int pMax) {
        return Math.min(Math.max(pValue, pMin), pMax);
    }

    public static long clamp(long pValue, long pMin, long pMax) {
        return Math.min(Math.max(pValue, pMin), pMax);
    }

    public static float clamp(float pValue, float pMin, float pMax) {
        return pValue < pMin ? pMin : Math.min(pValue, pMax);
    }

    public static double clamp(double pValue, double pMin, double pMax) {
        return pValue < pMin ? pMin : Math.min(pValue, pMax);
    }

    public static double clampedLerp(double pStart, double pEnd, double pDelta) {
        if (pDelta < 0.0) {
            return pStart;
        } else {
            return pDelta > 1.0 ? pEnd : lerp(pDelta, pStart, pEnd);
        }
    }

    public static float clampedLerp(float pStart, float pEnd, float pDelta) {
        if (pDelta < 0.0F) {
            return pStart;
        } else {
            return pDelta > 1.0F ? pEnd : lerp(pDelta, pStart, pEnd);
        }
    }

    public static double absMax(double pX, double pY) {
        if (pX < 0.0) {
            pX = -pX;
        }

        if (pY < 0.0) {
            pY = -pY;
        }

        return Math.max(pX, pY);
    }

    public static int floorDiv(int pDividend, int pDivisor) {
        return Math.floorDiv(pDividend, pDivisor);
    }

    public static int nextInt(RandomSource pRandom, int pMinimum, int pMaximum) {
        return pMinimum >= pMaximum ? pMinimum : pRandom.nextInt(pMaximum - pMinimum + 1) + pMinimum;
    }

    public static float nextFloat(RandomSource pRandom, float pMinimum, float pMaximum) {
        return pMinimum >= pMaximum ? pMinimum : pRandom.nextFloat() * (pMaximum - pMinimum) + pMinimum;
    }

    public static double nextDouble(RandomSource pRandom, double pMinimum, double pMaximum) {
        return pMinimum >= pMaximum ? pMinimum : pRandom.nextDouble() * (pMaximum - pMinimum) + pMinimum;
    }

    public static boolean equal(float pX, float pY) {
        return Math.abs(pY - pX) < 1.0E-5F;
    }

    public static boolean equal(double pX, double pY) {
        return Math.abs(pY - pX) < 1.0E-5F;
    }

    public static int positiveModulo(int pX, int pY) {
        return Math.floorMod(pX, pY);
    }

    public static float positiveModulo(float pNumerator, float pDenominator) {
        return (pNumerator % pDenominator + pDenominator) % pDenominator;
    }

    public static double positiveModulo(double pNumerator, double pDenominator) {
        return (pNumerator % pDenominator + pDenominator) % pDenominator;
    }

    public static boolean isMultipleOf(int pNumber, int pMultiple) {
        return pNumber % pMultiple == 0;
    }

    public static byte packDegrees(float pDegrees) {
        return (byte)floor(pDegrees * 256.0F / 360.0F);
    }

    public static float unpackDegrees(byte pDegrees) {
        return (float)(pDegrees * 360) / 256.0F;
    }

    public static int wrapDegrees(int pAngle) {
        int i = pAngle % 360;
        if (i >= 180) {
            i -= 360;
        }

        if (i < -180) {
            i += 360;
        }

        return i;
    }

    public static float wrapDegrees(long pAngle) {
        float f = (float)(pAngle % 360L);
        if (f >= 180.0F) {
            f -= 360.0F;
        }

        if (f < -180.0F) {
            f += 360.0F;
        }

        return f;
    }

    public static float wrapDegrees(float pValue) {
        float f = pValue % 360.0F;
        if (f >= 180.0F) {
            f -= 360.0F;
        }

        if (f < -180.0F) {
            f += 360.0F;
        }

        return f;
    }

    public static double wrapDegrees(double pValue) {
        double d0 = pValue % 360.0;
        if (d0 >= 180.0) {
            d0 -= 360.0;
        }

        if (d0 < -180.0) {
            d0 += 360.0;
        }

        return d0;
    }

    public static float degreesDifference(float pStart, float pEnd) {
        return wrapDegrees(pEnd - pStart);
    }

    public static float degreesDifferenceAbs(float pStart, float pEnd) {
        return abs(degreesDifference(pStart, pEnd));
    }

    public static float rotateIfNecessary(float pRotationToAdjust, float pActualRotation, float pMaxDifference) {
        float f = degreesDifference(pRotationToAdjust, pActualRotation);
        float f1 = clamp(f, -pMaxDifference, pMaxDifference);
        return pActualRotation - f1;
    }

    public static float approach(float pValue, float pLimit, float pStepSize) {
        pStepSize = abs(pStepSize);
        return pValue < pLimit ? clamp(pValue + pStepSize, pValue, pLimit) : clamp(pValue - pStepSize, pLimit, pValue);
    }

    public static float approachDegrees(float pAngle, float pLimit, float pStepSize) {
        float f = degreesDifference(pAngle, pLimit);
        return approach(pAngle, pAngle + f, pStepSize);
    }

    public static int getInt(String pValue, int pDefaultValue) {
        return NumberUtils.toInt(pValue, pDefaultValue);
    }

    public static int smallestEncompassingPowerOfTwo(int pValue) {
        int i = pValue - 1;
        i |= i >> 1;
        i |= i >> 2;
        i |= i >> 4;
        i |= i >> 8;
        i |= i >> 16;
        return i + 1;
    }

    public static boolean isPowerOfTwo(int pValue) {
        return pValue != 0 && (pValue & pValue - 1) == 0;
    }

    public static int ceillog2(int pValue) {
        pValue = isPowerOfTwo(pValue) ? pValue : smallestEncompassingPowerOfTwo(pValue);
        return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int)((long)pValue * 125613361L >> 27) & 31];
    }

    public static int log2(int pValue) {
        return ceillog2(pValue) - (isPowerOfTwo(pValue) ? 0 : 1);
    }

    public static float frac(float pNumber) {
        return pNumber - (float)floor(pNumber);
    }

    public static double frac(double pNumber) {
        return pNumber - (double)lfloor(pNumber);
    }

    @Deprecated
    public static long getSeed(Vec3i pPos) {
        return getSeed(pPos.getX(), pPos.getY(), pPos.getZ());
    }

    @Deprecated
    public static long getSeed(int pX, int pY, int pZ) {
        long i = (long)(pX * 3129871) ^ (long)pZ * 116129781L ^ (long)pY;
        i = i * i * 42317861L + i * 11L;
        return i >> 16;
    }

    public static UUID createInsecureUUID(RandomSource pRandom) {
        long i = pRandom.nextLong() & -61441L | 16384L;
        long j = pRandom.nextLong() & 4611686018427387903L | Long.MIN_VALUE;
        return new UUID(i, j);
    }

    public static UUID createInsecureUUID() {
        return createInsecureUUID(RANDOM);
    }

    public static double inverseLerp(double pDelta, double pStart, double pEnd) {
        return (pDelta - pStart) / (pEnd - pStart);
    }

    public static float inverseLerp(float pDelta, float pStart, float pEnd) {
        return (pDelta - pStart) / (pEnd - pStart);
    }

    public static boolean rayIntersectsAABB(Vec3 pStart, Vec3 pEnd, AABB pBoundingBox) {
        double d0 = (pBoundingBox.minX + pBoundingBox.maxX) * 0.5;
        double d1 = (pBoundingBox.maxX - pBoundingBox.minX) * 0.5;
        double d2 = pStart.x - d0;
        if (Math.abs(d2) > d1 && d2 * pEnd.x >= 0.0) {
            return false;
        } else {
            double d3 = (pBoundingBox.minY + pBoundingBox.maxY) * 0.5;
            double d4 = (pBoundingBox.maxY - pBoundingBox.minY) * 0.5;
            double d5 = pStart.y - d3;
            if (Math.abs(d5) > d4 && d5 * pEnd.y >= 0.0) {
                return false;
            } else {
                double d6 = (pBoundingBox.minZ + pBoundingBox.maxZ) * 0.5;
                double d7 = (pBoundingBox.maxZ - pBoundingBox.minZ) * 0.5;
                double d8 = pStart.z - d6;
                if (Math.abs(d8) > d7 && d8 * pEnd.z >= 0.0) {
                    return false;
                } else {
                    double d9 = Math.abs(pEnd.x);
                    double d10 = Math.abs(pEnd.y);
                    double d11 = Math.abs(pEnd.z);
                    double d12 = pEnd.y * d8 - pEnd.z * d5;
                    if (Math.abs(d12) > d4 * d11 + d7 * d10) {
                        return false;
                    } else {
                        d12 = pEnd.z * d2 - pEnd.x * d8;
                        if (Math.abs(d12) > d1 * d11 + d7 * d9) {
                            return false;
                        } else {
                            d12 = pEnd.x * d5 - pEnd.y * d2;
                            return Math.abs(d12) < d1 * d10 + d4 * d9;
                        }
                    }
                }
            }
        }
    }

    public static double atan2(double pY, double pX) {
        double d0 = pX * pX + pY * pY;
        if (Double.isNaN(d0)) {
            return Double.NaN;
        } else {
            boolean flag = pY < 0.0;
            if (flag) {
                pY = -pY;
            }

            boolean flag1 = pX < 0.0;
            if (flag1) {
                pX = -pX;
            }

            boolean flag2 = pY > pX;
            if (flag2) {
                double d1 = pX;
                pX = pY;
                pY = d1;
            }

            double d9 = fastInvSqrt(d0);
            pX *= d9;
            pY *= d9;
            double d2 = FRAC_BIAS + pY;
            int i = (int)Double.doubleToRawLongBits(d2);
            double d3 = ASIN_TAB[i];
            double d4 = COS_TAB[i];
            double d5 = d2 - FRAC_BIAS;
            double d6 = pY * d4 - pX * d5;
            double d7 = (6.0 + d6 * d6) * d6 * 0.16666666666666666;
            double d8 = d3 + d7;
            if (flag2) {
                d8 = (Math.PI / 2) - d8;
            }

            if (flag1) {
                d8 = Math.PI - d8;
            }

            if (flag) {
                d8 = -d8;
            }

            return d8;
        }
    }

    public static float invSqrt(float pNumber) {
        return org.joml.Math.invsqrt(pNumber);
    }

    public static double invSqrt(double pNumber) {
        return org.joml.Math.invsqrt(pNumber);
    }

    @Deprecated
    public static double fastInvSqrt(double pNumber) {
        double d0 = 0.5 * pNumber;
        long i = Double.doubleToRawLongBits(pNumber);
        i = 6910469410427058090L - (i >> 1);
        pNumber = Double.longBitsToDouble(i);
        return pNumber * (1.5 - d0 * pNumber * pNumber);
    }

    public static float fastInvCubeRoot(float pNumber) {
        int i = Float.floatToIntBits(pNumber);
        i = 1419967116 - i / 3;
        float f = Float.intBitsToFloat(i);
        f = 0.6666667F * f + 1.0F / (3.0F * f * f * pNumber);
        return 0.6666667F * f + 1.0F / (3.0F * f * f * pNumber);
    }

    public static int hsvToRgb(float pHue, float pSaturation, float pValue) {
        return hsvToArgb(pHue, pSaturation, pValue, 0);
    }

    public static int hsvToArgb(float pHue, float pSaturation, float pValue, int pAlpha) {
        int i = (int)(pHue * 6.0F) % 6;
        float f = pHue * 6.0F - (float)i;
        float f1 = pValue * (1.0F - pSaturation);
        float f2 = pValue * (1.0F - f * pSaturation);
        float f3 = pValue * (1.0F - (1.0F - f) * pSaturation);
        float f4;
        float f5;
        float f6;
        switch (i) {
            case 0:
                f4 = pValue;
                f5 = f3;
                f6 = f1;
                break;
            case 1:
                f4 = f2;
                f5 = pValue;
                f6 = f1;
                break;
            case 2:
                f4 = f1;
                f5 = pValue;
                f6 = f3;
                break;
            case 3:
                f4 = f1;
                f5 = f2;
                f6 = pValue;
                break;
            case 4:
                f4 = f3;
                f5 = f1;
                f6 = pValue;
                break;
            case 5:
                f4 = pValue;
                f5 = f1;
                f6 = f2;
                break;
            default:
                throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + pHue + ", " + pSaturation + ", " + pValue);
        }

        return ARGB.color(pAlpha, clamp((int)(f4 * 255.0F), 0, 255), clamp((int)(f5 * 255.0F), 0, 255), clamp((int)(f6 * 255.0F), 0, 255));
    }

    public static int murmurHash3Mixer(int pInput) {
        pInput ^= pInput >>> 16;
        pInput *= -2048144789;
        pInput ^= pInput >>> 13;
        pInput *= -1028477387;
        return pInput ^ pInput >>> 16;
    }

    public static int binarySearch(int pMin, int pMax, IntPredicate pIsTargetBeforeOrAt) {
        int i = pMax - pMin;

        while (i > 0) {
            int j = i / 2;
            int k = pMin + j;
            if (pIsTargetBeforeOrAt.test(k)) {
                i = j;
            } else {
                pMin = k + 1;
                i -= j + 1;
            }
        }

        return pMin;
    }

    public static int lerpInt(float pDelta, int pStart, int pEnd) {
        return pStart + floor(pDelta * (float)(pEnd - pStart));
    }

    public static int lerpDiscrete(float pDelta, int pStart, int pEnd) {
        int i = pEnd - pStart;
        return pStart + floor(pDelta * (float)(i - 1)) + (pDelta > 0.0F ? 1 : 0);
    }

    public static float lerp(float pDelta, float pStart, float pEnd) {
        return pStart + pDelta * (pEnd - pStart);
    }

    public static Vec3 lerp(double pDelta, Vec3 pStart, Vec3 pEnd) {
        return new Vec3(
            lerp(pDelta, pStart.x, pEnd.x),
            lerp(pDelta, pStart.y, pEnd.y),
            lerp(pDelta, pStart.z, pEnd.z)
        );
    }

    public static double lerp(double pDelta, double pStart, double pEnd) {
        return pStart + pDelta * (pEnd - pStart);
    }

    public static double lerp2(double pDelta1, double pDelta2, double pStart1, double pEnd1, double pStart2, double pEnd2) {
        return lerp(pDelta2, lerp(pDelta1, pStart1, pEnd1), lerp(pDelta1, pStart2, pEnd2));
    }

    public static double lerp3(
        double pDelta1,
        double pDelta2,
        double pDelta3,
        double pStart1,
        double pEnd1,
        double pStart2,
        double pEnd2,
        double pStart3,
        double pEnd3,
        double pStart4,
        double pEnd4
    ) {
        return lerp(
            pDelta3,
            lerp2(pDelta1, pDelta2, pStart1, pEnd1, pStart2, pEnd2),
            lerp2(pDelta1, pDelta2, pStart3, pEnd3, pStart4, pEnd4)
        );
    }

    public static float catmullrom(float pDelta, float pControlPoint1, float pControlPoint2, float pControlPoint3, float pControlPoint4) {
        return 0.5F
            * (
                2.0F * pControlPoint2
                    + (pControlPoint3 - pControlPoint1) * pDelta
                    + (2.0F * pControlPoint1 - 5.0F * pControlPoint2 + 4.0F * pControlPoint3 - pControlPoint4) * pDelta * pDelta
                    + (3.0F * pControlPoint2 - pControlPoint1 - 3.0F * pControlPoint3 + pControlPoint4) * pDelta * pDelta * pDelta
            );
    }

    public static double smoothstep(double pInput) {
        return pInput * pInput * pInput * (pInput * (pInput * 6.0 - 15.0) + 10.0);
    }

    public static double smoothstepDerivative(double pInput) {
        return 30.0 * pInput * pInput * (pInput - 1.0) * (pInput - 1.0);
    }

    public static int sign(double pX) {
        if (pX == 0.0) {
            return 0;
        } else {
            return pX > 0.0 ? 1 : -1;
        }
    }

    public static float rotLerp(float pDelta, float pStart, float pEnd) {
        return pStart + pDelta * wrapDegrees(pEnd - pStart);
    }

    public static double rotLerp(double pDelta, double pStart, double pEnd) {
        return pStart + pDelta * wrapDegrees(pEnd - pStart);
    }

    public static float rotLerpRad(float pDelta, float pStart, float pEnd) {
        float f = pEnd - pStart;

        while (f < (float) -Math.PI) {
            f += (float) (Math.PI * 2);
        }

        while (f >= (float) Math.PI) {
            f -= (float) (Math.PI * 2);
        }

        return pStart + pDelta * f;
    }

    public static float triangleWave(float pInput, float pPeriod) {
        return (Math.abs(pInput % pPeriod - pPeriod * 0.5F) - pPeriod * 0.25F) / (pPeriod * 0.25F);
    }

    public static float square(float pValue) {
        return pValue * pValue;
    }

    public static double square(double pValue) {
        return pValue * pValue;
    }

    public static int square(int pValue) {
        return pValue * pValue;
    }

    public static long square(long pValue) {
        return pValue * pValue;
    }

    public static double clampedMap(double pInput, double pInputMin, double pInputMax, double pOuputMin, double pOutputMax) {
        return clampedLerp(pOuputMin, pOutputMax, inverseLerp(pInput, pInputMin, pInputMax));
    }

    public static float clampedMap(float pInput, float pInputMin, float pInputMax, float pOutputMin, float pOutputMax) {
        return clampedLerp(pOutputMin, pOutputMax, inverseLerp(pInput, pInputMin, pInputMax));
    }

    public static double map(double pInput, double pInputMin, double pInputMax, double pOutputMin, double pOutputMax) {
        return lerp(inverseLerp(pInput, pInputMin, pInputMax), pOutputMin, pOutputMax);
    }

    public static float map(float pInput, float pInputMin, float pInputMax, float pOutputMin, float pOutputMax) {
        return lerp(inverseLerp(pInput, pInputMin, pInputMax), pOutputMin, pOutputMax);
    }

    public static double wobble(double pInput) {
        return pInput + (2.0 * RandomSource.create((long)floor(pInput * 3000.0)).nextDouble() - 1.0) * 1.0E-7 / 2.0;
    }

    public static int roundToward(int pValue, int pFactor) {
        return positiveCeilDiv(pValue, pFactor) * pFactor;
    }

    public static int positiveCeilDiv(int pX, int pY) {
        return -Math.floorDiv(-pX, pY);
    }

    public static int randomBetweenInclusive(RandomSource pRandom, int pMinInclusive, int pMaxInclusive) {
        return pRandom.nextInt(pMaxInclusive - pMinInclusive + 1) + pMinInclusive;
    }

    public static float randomBetween(RandomSource pRandom, float pMinInclusive, float pMaxExclusive) {
        return pRandom.nextFloat() * (pMaxExclusive - pMinInclusive) + pMinInclusive;
    }

    public static float normal(RandomSource pRandom, float pMean, float pDeviation) {
        return pMean + (float)pRandom.nextGaussian() * pDeviation;
    }

    public static double lengthSquared(double pXDistance, double pYDistance) {
        return pXDistance * pXDistance + pYDistance * pYDistance;
    }

    public static double length(double pXDistance, double pYDistance) {
        return Math.sqrt(lengthSquared(pXDistance, pYDistance));
    }

    public static float length(float pXDistance, float pYDistance) {
        return (float)Math.sqrt(lengthSquared((double)pXDistance, (double)pYDistance));
    }

    public static double lengthSquared(double pXDistance, double pYDistance, double pZDistance) {
        return pXDistance * pXDistance + pYDistance * pYDistance + pZDistance * pZDistance;
    }

    public static double length(double pXDistance, double pYDistance, double pZDistance) {
        return Math.sqrt(lengthSquared(pXDistance, pYDistance, pZDistance));
    }

    public static float lengthSquared(float pXDistance, float pYDistance, float pZDistance) {
        return pXDistance * pXDistance + pYDistance * pYDistance + pZDistance * pZDistance;
    }

    public static int quantize(double pValue, int pFactor) {
        return floor(pValue / (double)pFactor) * pFactor;
    }

    public static IntStream outFromOrigin(int pInput, int pLowerBound, int pUpperBound) {
        return outFromOrigin(pInput, pLowerBound, pUpperBound, 1);
    }

    public static IntStream outFromOrigin(int pInput, int pLowerBound, int pUpperBound, int pSteps) {
        if (pLowerBound > pUpperBound) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "upperbound %d expected to be > lowerBound %d", pUpperBound, pLowerBound));
        } else if (pSteps < 1) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "steps expected to be >= 1, was %d", pSteps));
        } else {
            return pInput >= pLowerBound && pInput <= pUpperBound ? IntStream.iterate(pInput, p_216278_3_ -> {
                int i = Math.abs(pInput - p_216278_3_);
                return pInput - i >= pLowerBound || pInput + i <= pUpperBound;
            }, p_216255_4_ -> {
                boolean flag = p_216255_4_ <= pInput;
                int i = Math.abs(pInput - p_216255_4_);
                boolean flag1 = pInput + i + pSteps <= pUpperBound;
                if (!flag || !flag1) {
                    int j = pInput - i - (flag ? pSteps : 0);
                    if (j >= pLowerBound) {
                        return j;
                    }
                }

                return pInput + i + pSteps;
            }) : IntStream.empty();
        }
    }

    public static Quaternionf rotationAroundAxis(Vector3f pAxis, Quaternionf pCameraOrentation, Quaternionf pOutput) {
        float f = pAxis.dot(pCameraOrentation.x, pCameraOrentation.y, pCameraOrentation.z);
        return pOutput.set(pAxis.x * f, pAxis.y * f, pAxis.z * f, pCameraOrentation.w).normalize();
    }

    public static int mulAndTruncate(Fraction pFraction, int pFactor) {
        return pFraction.getNumerator() * pFactor / pFraction.getDenominator();
    }

    public static float easeInOutSine(float pValue) {
        return -(cos((float) Math.PI * pValue) - 1.0F) / 2.0F;
    }

    static {
        for (int i = 0; i < 257; i++) {
            double d0 = (double)i / 256.0;
            double d1 = Math.asin(d0);
            COS_TAB[i] = Math.cos(d1);
            ASIN_TAB[i] = d1;
        }

        for (int j = 0; j < SIN_TABLE_FAST.length; j++) {
            SIN_TABLE_FAST[j] = MathUtils.roundToFloat(Math.sin((double)j * Math.PI * 2.0 / 4096.0));
        }
    }
}