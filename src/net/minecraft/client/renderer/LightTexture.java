package net.minecraft.client.renderer;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.shaders.Shaders;
import net.optifine.util.TextureUtils;
import org.joml.Vector3f;

public class LightTexture implements AutoCloseable {
    public static final int FULL_BRIGHT = 15728880;
    public static final int FULL_SKY = 15728640;
    public static final int FULL_BLOCK = 240;
    private static final int TEXTURE_SIZE = 16;
    private final TextureTarget target;
    private boolean updateLightTexture;
    private float blockLightRedFlicker;
    private final GameRenderer renderer;
    private final Minecraft minecraft;
    private boolean allowed = true;
    private boolean custom = false;
    public static final int MAX_BRIGHTNESS = pack(15, 15);
    public static final int VANILLA_EMISSIVE_BRIGHTNESS = 15794417;
    private final DynamicTexture dynamicTexture;
    private final NativeImage nativeImage;

    public LightTexture(GameRenderer pRenderer, Minecraft pMinecraft) {
        this.renderer = pRenderer;
        this.minecraft = pMinecraft;
        this.target = new TextureTarget(16, 16, false);
        this.target.setFilterMode(9729);
        this.target.setClearColor(1.0F, 1.0F, 1.0F, 1.0F);
        this.target.clear();
        this.dynamicTexture = new DynamicTexture(16, 16, true);
        this.nativeImage = this.dynamicTexture.getPixels();
        this.nativeImage.fillRect(0, 0, 16, 16, -1);
        this.dynamicTexture.upload();
    }

    @Override
    public void close() {
        this.target.destroyBuffers();
    }

    public void tick() {
        this.blockLightRedFlicker = this.blockLightRedFlicker + (float)((Math.random() - Math.random()) * Math.random() * Math.random() * 0.1);
        this.blockLightRedFlicker *= 0.9F;
        this.updateLightTexture = true;
    }

    public void turnOffLightLayer() {
        RenderSystem.setShaderTexture(2, 0);
        if (Config.isShaders()) {
            Shaders.disableLightmap();
        }
    }

    public void turnOnLightLayer() {
        if (!this.allowed) {
            RenderSystem.setShaderTexture(2, TextureUtils.WHITE16_TEXTURE_LOCATION);
            this.minecraft.getTextureManager().bindTexture(TextureUtils.WHITE16_TEXTURE_LOCATION);
            RenderSystem.texParameter(3553, 10241, 9729);
            RenderSystem.texParameter(3553, 10240, 9729);
        } else if (this.custom) {
            RenderSystem.setShaderTexture(2, this.dynamicTexture.getId());
            this.dynamicTexture.bind();
            RenderSystem.texParameter(3553, 10241, 9729);
            RenderSystem.texParameter(3553, 10240, 9729);
        } else {
            RenderSystem.setShaderTexture(2, this.target.getColorTextureId());
        }

        if (Config.isShaders()) {
            Shaders.enableLightmap();
        }
    }

    private float getDarknessGamma(float pPartialTick) {
        MobEffectInstance mobeffectinstance = this.minecraft.player.getEffect(MobEffects.DARKNESS);
        return mobeffectinstance != null ? mobeffectinstance.getBlendFactor(this.minecraft.player, pPartialTick) : 0.0F;
    }

    private float calculateDarknessScale(LivingEntity pEntity, float pGamma, float pPartialTick) {
        float f = 0.45F * pGamma;
        return Math.max(0.0F, Mth.cos(((float)pEntity.tickCount - pPartialTick) * (float) Math.PI * 0.025F) * f);
    }

    public void updateLightTexture(float pPartialTicks) {
        if (this.updateLightTexture) {
            this.updateLightTexture = false;
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.push("lightTex");
            ClientLevel clientlevel = this.minecraft.level;
            if (clientlevel != null) {
                this.custom = false;
                if (Config.isCustomColors()) {
                    boolean flag = this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION) || this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER);
                    float f = this.getDarknessGammaFactor(pPartialTicks);
                    float f1 = this.getDarknessLightFactor(clientlevel, pPartialTicks);
                    float f2 = f * 0.25F + f1 * 0.75F;
                    if (CustomColors.updateLightmap(clientlevel, this.blockLightRedFlicker, this.nativeImage, flag, f2, pPartialTicks)) {
                        this.dynamicTexture.upload();
                        this.updateLightTexture = false;
                        profilerfiller.pop();
                        this.custom = true;
                        return;
                    }
                }

                this.custom = false;
                float f9 = clientlevel.getSkyDarken(1.0F);
                float f10;
                if (clientlevel.getSkyFlashTime() > 0) {
                    f10 = 1.0F;
                } else {
                    f10 = f9 * 0.95F + 0.05F;
                }

                float f11 = this.minecraft.options.darknessEffectScale().get().floatValue();
                float f12 = this.getDarknessGamma(pPartialTicks) * f11;
                float f3 = this.calculateDarknessScale(this.minecraft.player, f12, pPartialTicks) * f11;
                if (Config.isShaders()) {
                    Shaders.setDarknessFactor(f12);
                    Shaders.setDarknessLightFactor(f3);
                }

                float f4 = this.minecraft.player.getWaterVision();
                float f5;
                if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
                    f5 = GameRenderer.getNightVisionScale(this.minecraft.player, pPartialTicks);
                } else if (f4 > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
                    f5 = f4;
                } else {
                    f5 = 0.0F;
                }

                Vector3f vector3f = new Vector3f(f9, f9, 1.0F).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
                float f6 = this.blockLightRedFlicker + 1.5F;
                float f7 = clientlevel.dimensionType().ambientLight();
                boolean flag1 = clientlevel.effects().forceBrightLightmap();
                float f8 = this.minecraft.options.gamma().get().floatValue();
                CompiledShaderProgram compiledshaderprogram = Objects.requireNonNull(
                    RenderSystem.setShader(CoreShaders.LIGHTMAP), "Lightmap shader not loaded"
                );
                compiledshaderprogram.safeGetUniform("AmbientLightFactor").set(f7);
                compiledshaderprogram.safeGetUniform("SkyFactor").set(f10);
                compiledshaderprogram.safeGetUniform("BlockFactor").set(f6);
                compiledshaderprogram.safeGetUniform("UseBrightLightmap").set(flag1 ? 1 : 0);
                compiledshaderprogram.safeGetUniform("SkyLightColor").set(vector3f);
                compiledshaderprogram.safeGetUniform("NightVisionFactor").set(f5);
                compiledshaderprogram.safeGetUniform("DarknessScale").set(f3);
                compiledshaderprogram.safeGetUniform("DarkenWorldFactor").set(this.renderer.getDarkenWorldAmount(pPartialTicks));
                compiledshaderprogram.safeGetUniform("BrightnessFactor").set(Math.max(0.0F, f8 - f12));
                this.target.bindWrite(true);
                BufferBuilder bufferbuilder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLIT_SCREEN);
                bufferbuilder.addVertex(0.0F, 0.0F, 0.0F);
                bufferbuilder.addVertex(1.0F, 0.0F, 0.0F);
                bufferbuilder.addVertex(1.0F, 1.0F, 0.0F);
                bufferbuilder.addVertex(0.0F, 1.0F, 0.0F);
                BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
                this.target.unbindWrite();
                profilerfiller.pop();
            }
        }
    }

    public static float getBrightness(DimensionType pDimensionType, int pLightLevel) {
        return getBrightness(pDimensionType.ambientLight(), pLightLevel);
    }

    public static float getBrightness(float pAmbientLight, int pLightLevel) {
        float f = (float)pLightLevel / 15.0F;
        float f1 = f / (4.0F - 3.0F * f);
        return Mth.lerp(pAmbientLight, f1, 1.0F);
    }

    public static int pack(int pBlockLight, int pSkyLight) {
        return pBlockLight << 4 | pSkyLight << 20;
    }

    public static int block(int pPackedLight) {
        return (pPackedLight & 65535) >> 4;
    }

    public static int sky(int pPackedLight) {
        return pPackedLight >>> 20 & 15;
    }

    public static int lightCoordsWithEmission(int pPackedLight, int pEmission) {
        if (pEmission == 0) {
            return pPackedLight;
        } else {
            int i = Math.max(sky(pPackedLight), pEmission);
            int j = Math.max(block(pPackedLight), pEmission);
            return pack(j, i);
        }
    }

    public boolean isAllowed() {
        return this.allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public boolean isCustom() {
        return this.custom;
    }

    public float getDarknessGammaFactor(float partialTicks) {
        float f = this.minecraft.options.darknessEffectScale().get().floatValue();
        return this.getDarknessGamma(partialTicks) * f;
    }

    public float getDarknessLightFactor(ClientLevel clientLevel, float partialTicks) {
        boolean flag = clientLevel.effects().forceBrightLightmap();
        if (flag) {
            return 0.0F;
        } else {
            float f = this.minecraft.options.darknessEffectScale().get().floatValue();
            float f1 = this.getDarknessGamma(partialTicks) * f;
            return this.calculateDarknessScale(this.minecraft.player, f1, partialTicks) * f;
        }
    }
}