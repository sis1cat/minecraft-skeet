package sisicat.main.utilities;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import sisicat.IDefault;

public class World implements IDefault { // pozaimstvoval

    public static Vec2 project(double x, double y, double z) {
        Vec3 camera_pos = mc.getEntityRenderDispatcher().camera.getPosition();
        Quaternionf cameraRotation = new Quaternionf(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        cameraRotation.conjugate();

        Vector3f result3f = new Vector3f((float) (x - camera_pos.x), (float) (y - camera_pos.y), (float) (z -camera_pos.z));

        Quaternionf quaternion = new Quaternionf(cameraRotation);
        quaternion.mul(new Quaternionf(result3f.x, result3f.y, result3f.z, 0.0F));
        Quaternionf quaternion1 = new Quaternionf(cameraRotation);
        quaternion1.conjugate();
        quaternion.mul(quaternion1);
        result3f.set(quaternion.x, quaternion.y, quaternion.z);

        if (mc.options.bobView().get()) {
            Entity renderViewEntity = mc.getCameraEntity();
            if (renderViewEntity instanceof Player playerentity) {
                calculateViewBobbing(playerentity, result3f);
            }
        }

        double fov = mc.gameRenderer.getFov(mc.getEntityRenderDispatcher().camera, mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), true);

        return calculateScreenPosition(result3f, fov);
    }

    private static void calculateViewBobbing(Player playerentity, Vector3f result3f) {
        float walked = ((LocalPlayer)playerentity).walkDist;
        float f = walked - ((LocalPlayer)playerentity).walkDistO;
        float f1 = -(walked + f * mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        float f2 = Mth.lerp(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), playerentity.oBob, playerentity.bob);

        Quaternionf quaternion = quaternionf(new Vector3f(1, 0, 0), -(float) Math.abs(Math.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F, true);
        quaternion.conjugate();

        transform(quaternion, result3f);

        Quaternionf quaternion1 = quaternionf(new Vector3f(0, 0, 1), -(float) Math.sin(f1 * (float) Math.PI) * f2 * 3.0F, true);
        quaternion1.conjugate();

        transform(quaternion1, result3f);

        Vector3f bobTranslation = new Vector3f((float)(Math.sin(f1 * (float) Math.PI) * f2 * 0.5F), (float)(-Math.abs(Math.cos(f1 * (float) Math.PI) * f2)), 0.0f);

        result3f.add(bobTranslation);

    }

    static Quaternionf quaternionf(Vector3f axis, float angle, boolean degrees)
    {
        Quaternionf quaternionf = new Quaternionf();
        if (degrees)
        {
            angle *= ((float)Math.PI / 180F);
        }

        float f = (float) Math.sin(angle / 2.0F);
        quaternionf.x = axis.x * f;
        quaternionf.y = axis.y * f;
        quaternionf.z = axis.z * f;
        quaternionf.w = (float) Math.cos(angle / 2.0F);

        return quaternionf;
    }

    public static void transform(Quaternionf quaternionIn, Vector3f vector3f)
    {
        Quaternionf quaternion = new Quaternionf(quaternionIn);
        quaternion.mul(new Quaternionf(vector3f.x, vector3f.y, vector3f.z, 0.0F));
        Quaternionf quaternion1 = new Quaternionf(quaternionIn);
        quaternion1.conjugate();
        quaternion.mul(quaternion1);
        vector3f.set(quaternion.x, quaternion.y, quaternion.z);
    }

    private static Vec2 calculateScreenPosition(Vector3f result3f, double fov) {
        float halfHeight = mc.getWindow().getHeight() / 2f;
        float scaleFactor = halfHeight / (result3f.z * (float) Math.tan(Math.toRadians(fov / 2f)));
        if (result3f.z < 0.0F) {
            return new Vec2(-result3f.x * scaleFactor + mc.getWindow().getWidth() / 2f, mc.getWindow().getHeight() - (mc.getWindow().getHeight() / 2f - result3f.y * scaleFactor));
        }
        return null;
    }


}
