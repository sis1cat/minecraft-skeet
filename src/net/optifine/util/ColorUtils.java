package net.optifine.util;

import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ColorUtils {
    public static float getRedFloat(int col) {
        return from8BitChannel(ARGB.red(col));
    }

    public static float getGreenFloat(int col) {
        return from8BitChannel(ARGB.green(col));
    }

    public static float getBlueFloat(int col) {
        return from8BitChannel(ARGB.blue(col));
    }

    public static float from8BitChannel(int val) {
        return (float)val / 255.0F;
    }

    public static Vec3 argbToVec3(int col) {
        float f = getRedFloat(col);
        float f1 = getGreenFloat(col);
        float f2 = getBlueFloat(col);
        return new Vec3((double)f, (double)f1, (double)f2);
    }

    public static Vector3f argbToVec3f(int col) {
        float f = from8BitChannel(ARGB.red(col));
        float f1 = from8BitChannel(ARGB.green(col));
        float f2 = from8BitChannel(ARGB.blue(col));
        return new Vector3f(f, f1, f2);
    }

    public static int vec3ToArgb(Vec3 colVec) {
        return ARGB.colorFromFloat(1.0F, (float)colVec.x, (float)colVec.y, (float)colVec.z);
    }

    public static int vec3fToArgb(Vector3f colVec) {
        return ARGB.colorFromFloat(1.0F, colVec.x, colVec.y, colVec.z);
    }

    public static int combineAlphaRbg(int colAlpha, int colRbg) {
        return colAlpha & 0xFF000000 | colRbg & 16777215;
    }
}