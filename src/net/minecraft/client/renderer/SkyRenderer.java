package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.CustomSky;
import net.optifine.shaders.RenderStage;
import net.optifine.shaders.Shaders;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

public class SkyRenderer implements AutoCloseable {
    private static final ResourceLocation SUN_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    private static final ResourceLocation MOON_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
    public static final ResourceLocation END_SKY_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/end_sky.png");
    private static final float SKY_DISC_RADIUS = 512.0F;
    private final VertexBuffer starBuffer = VertexBuffer.uploadStatic(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION, this::buildStars);
    private final VertexBuffer topSkyBuffer = VertexBuffer.uploadStatic(
        VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION, bufferIn -> this.buildSkyDisc(bufferIn, 16.0F)
    );
    private final VertexBuffer bottomSkyBuffer = VertexBuffer.uploadStatic(
        VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION, bufferIn -> this.buildSkyDisc(bufferIn, -16.0F)
    );
    private final VertexBuffer endSkyBuffer = VertexBuffer.uploadStatic(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR, this::buildEndSky);
    private ClientLevel world;
    private float partialTicks;

    private void buildStars(VertexConsumer pBuffer) {
        RandomSource randomsource = RandomSource.create(10842L);
        int i = 1500;
        float f = 100.0F;

        for (int j = 0; j < 1500; j++) {
            float f1 = randomsource.nextFloat() * 2.0F - 1.0F;
            float f2 = randomsource.nextFloat() * 2.0F - 1.0F;
            float f3 = randomsource.nextFloat() * 2.0F - 1.0F;
            float f4 = 0.15F + randomsource.nextFloat() * 0.1F;
            float f5 = Mth.lengthSquared(f1, f2, f3);
            if (!(f5 <= 0.010000001F) && !(f5 >= 1.0F)) {
                Vector3f vector3f = new Vector3f(f1, f2, f3).normalize(100.0F);
                float f6 = (float)(randomsource.nextDouble() * (float) Math.PI * 2.0);
                Matrix3f matrix3f = new Matrix3f().rotateTowards(new Vector3f(vector3f).negate(), new Vector3f(0.0F, 1.0F, 0.0F)).rotateZ(-f6);
                pBuffer.addVertex(new Vector3f(f4, -f4, 0.0F).mul(matrix3f).add(vector3f));
                pBuffer.addVertex(new Vector3f(f4, f4, 0.0F).mul(matrix3f).add(vector3f));
                pBuffer.addVertex(new Vector3f(-f4, f4, 0.0F).mul(matrix3f).add(vector3f));
                pBuffer.addVertex(new Vector3f(-f4, -f4, 0.0F).mul(matrix3f).add(vector3f));
            }
        }
    }

    private void buildSkyDisc(VertexConsumer pBuffer, float pY) {
        float f = Math.signum(pY) * 512.0F;
        pBuffer.addVertex(0.0F, pY, 0.0F);

        for (int i = -180; i <= 180; i += 45) {
            pBuffer.addVertex(f * Mth.cos((float)i * (float) (Math.PI / 180.0)), pY, 512.0F * Mth.sin((float)i * (float) (Math.PI / 180.0)));
        }
    }

    public void renderSkyDisc(float pRed, float pGreen, float pBlue) {
        RenderSystem.setShaderColor(pRed, pGreen, pBlue, 1.0F);
        this.topSkyBuffer.drawWithRenderType(RenderType.sky());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void renderDarkDisc(PoseStack pPoseStack) {
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        pPoseStack.pushPose();
        pPoseStack.translate(0.0F, 12.0F, 0.0F);
        this.bottomSkyBuffer.drawWithRenderType(RenderType.sky());
        pPoseStack.popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void renderSunMoonAndStars(
        PoseStack pPoseStack,
        MultiBufferSource.BufferSource pBufferSource,
        float pTimeOfDay,
        int pMoonPhase,
        float pRainLevel,
        float pStarBrightness,
        FogParameters pFog
    ) {
        pPoseStack.pushPose();
        pPoseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        CustomSky.renderSky(this.world, pPoseStack, this.partialTicks);
        boolean flag = Config.isShaders();
        if (flag) {
            Shaders.preCelestialRotate(pPoseStack);
        }

        pPoseStack.mulPose(Axis.XP.rotationDegrees(pTimeOfDay * 360.0F));
        if (flag) {
            Shaders.postCelestialRotate(pPoseStack);
        }

        if (Config.isSunTexture()) {
            if (flag) {
                Shaders.setRenderStage(RenderStage.SUN);
            }

            this.renderSun(pRainLevel, pBufferSource, pPoseStack);
        }

        if (Config.isMoonTexture()) {
            if (flag) {
                Shaders.setRenderStage(RenderStage.MOON);
            }

            this.renderMoon(pMoonPhase, pRainLevel, pBufferSource, pPoseStack);
            pBufferSource.endBatch();
        }

        if (flag) {
            Shaders.disableTexture2D();
        }

        if (pStarBrightness > 0.0F && Config.isStarsEnabled() && !CustomSky.hasSkyLayers(this.world)) {
            if (flag) {
                Shaders.setRenderStage(RenderStage.STARS);
            }

            this.renderStars(pFog, pStarBrightness, pPoseStack);
        }

        if (flag) {
            Shaders.enableFog();
        }

        pPoseStack.popPose();
    }

    private void renderSun(float pAlpha, MultiBufferSource pBufferSource, PoseStack pPoseStack) {
        float f = 30.0F;
        float f1 = 100.0F;
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.celestial(SUN_LOCATION));
        int i = ARGB.white(pAlpha);
        Matrix4f matrix4f = pPoseStack.last().pose();
        vertexconsumer.addVertex(matrix4f, -30.0F, 100.0F, -30.0F).setUv(0.0F, 0.0F).setColor(i);
        vertexconsumer.addVertex(matrix4f, 30.0F, 100.0F, -30.0F).setUv(1.0F, 0.0F).setColor(i);
        vertexconsumer.addVertex(matrix4f, 30.0F, 100.0F, 30.0F).setUv(1.0F, 1.0F).setColor(i);
        vertexconsumer.addVertex(matrix4f, -30.0F, 100.0F, 30.0F).setUv(0.0F, 1.0F).setColor(i);
    }

    private void renderMoon(int pPhase, float pAlpha, MultiBufferSource pBufferSource, PoseStack pPoseStack) {
        float f = 20.0F;
        int i = pPhase % 4;
        int j = pPhase / 4 % 2;
        float f1 = (float)(i + 0) / 4.0F;
        float f2 = (float)(j + 0) / 2.0F;
        float f3 = (float)(i + 1) / 4.0F;
        float f4 = (float)(j + 1) / 2.0F;
        float f5 = 100.0F;
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.celestial(MOON_LOCATION));
        int k = ARGB.white(pAlpha);
        Matrix4f matrix4f = pPoseStack.last().pose();
        vertexconsumer.addVertex(matrix4f, -20.0F, -100.0F, 20.0F).setUv(f3, f4).setColor(k);
        vertexconsumer.addVertex(matrix4f, 20.0F, -100.0F, 20.0F).setUv(f1, f4).setColor(k);
        vertexconsumer.addVertex(matrix4f, 20.0F, -100.0F, -20.0F).setUv(f1, f2).setColor(k);
        vertexconsumer.addVertex(matrix4f, -20.0F, -100.0F, -20.0F).setUv(f3, f2).setColor(k);
    }

    private void renderStars(FogParameters pFog, float pStarBrightness, PoseStack pPoseStack) {
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.mul(pPoseStack.last().pose());
        RenderSystem.setShaderColor(pStarBrightness, pStarBrightness, pStarBrightness, pStarBrightness);
        RenderSystem.setShaderFog(FogParameters.NO_FOG);
        this.starBuffer.drawWithRenderType(RenderType.stars());
        RenderSystem.setShaderFog(pFog);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        matrix4fstack.popMatrix();
    }

    public void renderSunriseAndSunset(PoseStack pPoseStack, MultiBufferSource.BufferSource pBufferSource, float pSunAngle, int pColor) {
        pPoseStack.pushPose();
        pPoseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        float f = Mth.sin(pSunAngle) < 0.0F ? 180.0F : 0.0F;
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(f));
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        Matrix4f matrix4f = pPoseStack.last().pose();
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.sunriseSunset());
        float f1 = ARGB.alphaFloat(pColor);
        vertexconsumer.addVertex(matrix4f, 0.0F, 100.0F, 0.0F).setColor(pColor);
        int i = ARGB.transparent(pColor);
        int j = 16;

        for (int k = 0; k <= 16; k++) {
            float f2 = (float)k * (float) (Math.PI * 2) / 16.0F;
            float f3 = Mth.sin(f2);
            float f4 = Mth.cos(f2);
            vertexconsumer.addVertex(matrix4f, f3 * 120.0F, f4 * 120.0F, -f4 * 40.0F * f1).setColor(i);
        }

        pPoseStack.popPose();
    }

    private void buildEndSky(VertexConsumer pBuffer) {
        for (int i = 0; i < 6; i++) {
            Matrix4f matrix4f = new Matrix4f();
            switch (i) {
                case 1:
                    matrix4f.rotationX((float) (Math.PI / 2));
                    break;
                case 2:
                    matrix4f.rotationX((float) (-Math.PI / 2));
                    break;
                case 3:
                    matrix4f.rotationX((float) Math.PI);
                    break;
                case 4:
                    matrix4f.rotationZ((float) (Math.PI / 2));
                    break;
                case 5:
                    matrix4f.rotationZ((float) (-Math.PI / 2));
            }

            int j = -1;
            pBuffer.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setUv(0.0F, 0.0F).setColor(j);
            pBuffer.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setUv(0.0F, 16.0F).setColor(j);
            pBuffer.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setUv(16.0F, 16.0F).setColor(j);
            pBuffer.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setUv(16.0F, 0.0F).setColor(j);
        }
    }

    public void renderEndSky() {
        if (Config.isSkyEnabled()) {
            int i = -14145496;
            float f = ARGB.redFloat(i);
            float f1 = ARGB.greenFloat(i);
            float f2 = ARGB.blueFloat(i);
            Minecraft minecraft = Minecraft.getInstance();
            if (Config.isCustomColors()) {
                Vec3 vec3 = new Vec3((double)f, (double)f1, (double)f2);
                vec3 = CustomColors.getWorldSkyColor(vec3, minecraft.level, minecraft.getCameraEntity(), 0.0F);
                f = (float)vec3.x;
                f1 = (float)vec3.y;
                f2 = (float)vec3.z;
            }

            RenderSystem.setShaderColor(f, f1, f2, 1.0F);
            this.endSkyBuffer.drawWithRenderType(RenderType.endSky());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            CustomSky.renderSky(minecraft.level, 0.0F);
        }
    }

    @Override
    public void close() {
        this.starBuffer.close();
        this.topSkyBuffer.close();
        this.bottomSkyBuffer.close();
        this.endSkyBuffer.close();
    }

    public void setRenderParameters(ClientLevel world, float partialTicks) {
        this.world = world;
        this.partialTicks = partialTicks;
    }
}