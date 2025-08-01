package net.optifine;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.optifine.config.BiomeId;
import net.optifine.config.ConnectedParser;
import net.optifine.config.Matches;
import net.optifine.config.RangeListInt;
import net.optifine.render.Blender;
import net.optifine.util.MathUtils;
import net.optifine.util.NumUtils;
import net.optifine.util.SmoothFloat;
import net.optifine.util.TextureUtils;
import org.joml.Matrix4f;

public class CustomSkyLayer {
    public String source = null;
    private int startFadeIn = -1;
    private int endFadeIn = -1;
    private int startFadeOut = -1;
    private int endFadeOut = -1;
    private int blend = 1;
    private boolean rotate = false;
    private float speed = 1.0F;
    private float[] axis;
    private RangeListInt days;
    private int daysLoop;
    private boolean weatherClear;
    private boolean weatherRain;
    private boolean weatherThunder;
    public BiomeId[] biomes;
    public RangeListInt heights;
    private float transition;
    private SmoothFloat smoothPositionBrightness;
    public int textureId;
    private Level lastWorld;
    public static final float[] DEFAULT_AXIS = new float[]{1.0F, 0.0F, 0.0F};
    private static final String WEATHER_CLEAR = "clear";
    private static final String WEATHER_RAIN = "rain";
    private static final String WEATHER_THUNDER = "thunder";

    public CustomSkyLayer(Properties props, String defSource) {
        this.axis = DEFAULT_AXIS;
        this.days = null;
        this.daysLoop = 8;
        this.weatherClear = true;
        this.weatherRain = false;
        this.weatherThunder = false;
        this.biomes = null;
        this.heights = null;
        this.transition = 1.0F;
        this.smoothPositionBrightness = null;
        this.textureId = -1;
        this.lastWorld = null;
        ConnectedParser connectedparser = new ConnectedParser("CustomSky");
        this.source = props.getProperty("source", defSource);
        this.startFadeIn = this.parseTime(props.getProperty("startFadeIn"));
        this.endFadeIn = this.parseTime(props.getProperty("endFadeIn"));
        this.startFadeOut = this.parseTime(props.getProperty("startFadeOut"));
        this.endFadeOut = this.parseTime(props.getProperty("endFadeOut"));
        this.blend = Blender.parseBlend(props.getProperty("blend"));
        this.rotate = this.parseBoolean(props.getProperty("rotate"), true);
        this.speed = this.parseFloat(props.getProperty("speed"), 1.0F);
        this.axis = this.parseAxis(props.getProperty("axis"), DEFAULT_AXIS);
        this.days = connectedparser.parseRangeListInt(props.getProperty("days"));
        this.daysLoop = connectedparser.parseInt(props.getProperty("daysLoop"), 8);
        List<String> list = this.parseWeatherList(props.getProperty("weather", "clear"));
        this.weatherClear = list.contains("clear");
        this.weatherRain = list.contains("rain");
        this.weatherThunder = list.contains("thunder");
        this.biomes = connectedparser.parseBiomes(props.getProperty("biomes"));
        this.heights = connectedparser.parseRangeListIntNeg(props.getProperty("heights"));
        this.transition = this.parseFloat(props.getProperty("transition"), 1.0F);
    }

    private List<String> parseWeatherList(String str) {
        List<String> list = Arrays.asList("clear", "rain", "thunder");
        List<String> list1 = new ArrayList<>();
        String[] astring = Config.tokenize(str, " ");

        for (int i = 0; i < astring.length; i++) {
            String s = astring[i];
            if (!list.contains(s)) {
                Config.warn("Unknown weather: " + s);
            } else {
                list1.add(s);
            }
        }

        return list1;
    }

    private int parseTime(String str) {
        if (str == null) {
            return -1;
        } else {
            String[] astring = Config.tokenize(str, ":");
            if (astring.length != 2) {
                Config.warn("Invalid time: " + str);
                return -1;
            } else {
                String s = astring[0];
                String s1 = astring[1];
                int i = Config.parseInt(s, -1);
                int j = Config.parseInt(s1, -1);
                if (i >= 0 && i <= 23 && j >= 0 && j <= 59) {
                    i -= 6;
                    if (i < 0) {
                        i += 24;
                    }

                    return i * 1000 + (int)((double)j / 60.0 * 1000.0);
                } else {
                    Config.warn("Invalid time: " + str);
                    return -1;
                }
            }
        }
    }

    private boolean parseBoolean(String str, boolean defVal) {
        if (str == null) {
            return defVal;
        } else if (str.toLowerCase().equals("true")) {
            return true;
        } else if (str.toLowerCase().equals("false")) {
            return false;
        } else {
            Config.warn("Unknown boolean: " + str);
            return defVal;
        }
    }

    private float parseFloat(String str, float defVal) {
        if (str == null) {
            return defVal;
        } else {
            float f = Config.parseFloat(str, Float.MIN_VALUE);
            if (f == Float.MIN_VALUE) {
                Config.warn("Invalid value: " + str);
                return defVal;
            } else {
                return f;
            }
        }
    }

    private float[] parseAxis(String str, float[] defVal) {
        if (str == null) {
            return defVal;
        } else {
            String[] astring = Config.tokenize(str, " ");
            if (astring.length != 3) {
                Config.warn("Invalid axis: " + str);
                return defVal;
            } else {
                float[] afloat = new float[3];

                for (int i = 0; i < astring.length; i++) {
                    afloat[i] = Config.parseFloat(astring[i], Float.MIN_VALUE);
                    if (afloat[i] == Float.MIN_VALUE) {
                        Config.warn("Invalid axis: " + str);
                        return defVal;
                    }
                }

                float f2 = afloat[0];
                float f = afloat[1];
                float f1 = afloat[2];
                if (f2 * f2 + f * f + f1 * f1 < 1.0E-5F) {
                    Config.warn("Invalid axis values: " + str);
                    return defVal;
                } else {
                    return new float[]{f1, f, -f2};
                }
            }
        }
    }

    public boolean isValid(String path) {
        if (this.source == null) {
            Config.warn("No source texture: " + path);
            return false;
        } else {
            this.source = TextureUtils.fixResourcePath(this.source, TextureUtils.getBasePath(path));
            if (this.startFadeIn < 0 && this.endFadeIn < 0 && this.startFadeOut < 0 && this.endFadeOut < 0) {
                this.startFadeIn = 0;
                this.endFadeIn = 0;
                this.startFadeOut = 24000;
                this.endFadeOut = 24000;
            }

            if (this.startFadeIn >= 0 && this.endFadeIn >= 0 && this.endFadeOut >= 0) {
                int i = this.normalizeTime(this.endFadeIn - this.startFadeIn);
                if (this.startFadeOut < 0) {
                    this.startFadeOut = this.normalizeTime(this.endFadeOut - i);
                    if (this.timeBetween(this.startFadeOut, this.startFadeIn, this.endFadeIn)) {
                        this.startFadeOut = this.endFadeIn;
                    }
                }

                int j = this.normalizeTime(this.startFadeOut - this.endFadeIn);
                int k = this.normalizeTime(this.endFadeOut - this.startFadeOut);
                int l = this.normalizeTime(this.startFadeIn - this.endFadeOut);
                int i1 = i + j + k + l;
                if (i1 != 0 && i1 != 24000) {
                    Config.warn("Invalid fadeIn/fadeOut times, sum is not 24h: " + i1);
                    return false;
                } else if (this.speed < 0.0F) {
                    Config.warn("Invalid speed: " + this.speed);
                    return false;
                } else if (this.daysLoop <= 0) {
                    Config.warn("Invalid daysLoop: " + this.daysLoop);
                    return false;
                } else {
                    return true;
                }
            } else {
                Config.warn("Invalid times, required are: startFadeIn, endFadeIn and endFadeOut.");
                return false;
            }
        }
    }

    private int normalizeTime(int timeMc) {
        while (timeMc >= 24000) {
            timeMc -= 24000;
        }

        while (timeMc < 0) {
            timeMc += 24000;
        }

        return timeMc;
    }

    public void render(Level world, PoseStack matrixStackIn, int timeOfDay, float celestialAngle, float rainStrength, float thunderStrength) {
        float f = this.getPositionBrightness(world);
        float f1 = this.getWeatherBrightness(rainStrength, thunderStrength);
        float f2 = this.getFadeBrightness(timeOfDay);
        float f3 = f * f1 * f2;
        f3 = Config.limit(f3, 0.0F, 1.0F);
        if (!(f3 < 1.0E-4F)) {
            RenderSystem.setShaderTexture(0, this.textureId);
            Blender.setupBlend(this.blend, f3);
            matrixStackIn.pushPose();
            if (this.rotate) {
                float f4 = 0.0F;
                if (this.speed != (float)Math.round(this.speed)) {
                    long i = (world.getDayTime() + 18000L) / 24000L;
                    double d0 = (double)(this.speed % 1.0F);
                    double d1 = (double)i * d0;
                    f4 = (float)(d1 % 1.0);
                }

                matrixStackIn.rotateDeg(360.0F * (f4 + celestialAngle * this.speed), this.axis[0], this.axis[1], this.axis[2]);
            }

            Tesselator tesselator = Tesselator.getInstance();
            matrixStackIn.rotateDegXp(90.0F);
            matrixStackIn.rotateDegZp(-90.0F);
            this.renderSide(matrixStackIn, tesselator, 4);
            matrixStackIn.pushPose();
            matrixStackIn.rotateDegXp(90.0F);
            this.renderSide(matrixStackIn, tesselator, 1);
            matrixStackIn.popPose();
            matrixStackIn.pushPose();
            matrixStackIn.rotateDegXp(-90.0F);
            this.renderSide(matrixStackIn, tesselator, 0);
            matrixStackIn.popPose();
            matrixStackIn.rotateDegZp(90.0F);
            this.renderSide(matrixStackIn, tesselator, 5);
            matrixStackIn.rotateDegZp(90.0F);
            this.renderSide(matrixStackIn, tesselator, 2);
            matrixStackIn.rotateDegZp(90.0F);
            this.renderSide(matrixStackIn, tesselator, 3);
            matrixStackIn.popPose();
        }
    }

    private float getPositionBrightness(Level world) {
        if (this.biomes == null && this.heights == null) {
            return 1.0F;
        } else {
            float f = this.getPositionBrightnessRaw(world);
            if (this.smoothPositionBrightness == null) {
                this.smoothPositionBrightness = new SmoothFloat(f, this.transition);
            }

            return this.smoothPositionBrightness.getSmoothValue(f);
        }
    }

    private float getPositionBrightnessRaw(Level world) {
        Entity entity = Minecraft.getInstance().getCameraEntity();
        if (entity == null) {
            return 0.0F;
        } else {
            BlockPos blockpos = entity.blockPosition();
            if (this.biomes != null) {
                Biome biome = world.getBiome(blockpos).value();
                if (biome == null) {
                    return 0.0F;
                }

                if (!Matches.biome(biome, this.biomes)) {
                    return 0.0F;
                }
            }

            return this.heights != null && !this.heights.isInRange(blockpos.getY()) ? 0.0F : 1.0F;
        }
    }

    private float getWeatherBrightness(float rainStrength, float thunderStrength) {
        float f = 1.0F - rainStrength;
        float f1 = rainStrength - thunderStrength;
        float f2 = 0.0F;
        if (this.weatherClear) {
            f2 += f;
        }

        if (this.weatherRain) {
            f2 += f1;
        }

        if (this.weatherThunder) {
            f2 += thunderStrength;
        }

        return NumUtils.limit(f2, 0.0F, 1.0F);
    }

    private float getFadeBrightness(int timeOfDay) {
        if (this.timeBetween(timeOfDay, this.startFadeIn, this.endFadeIn)) {
            int k = this.normalizeTime(this.endFadeIn - this.startFadeIn);
            int l = this.normalizeTime(timeOfDay - this.startFadeIn);
            return (float)l / (float)k;
        } else if (this.timeBetween(timeOfDay, this.endFadeIn, this.startFadeOut)) {
            return 1.0F;
        } else if (this.timeBetween(timeOfDay, this.startFadeOut, this.endFadeOut)) {
            int i = this.normalizeTime(this.endFadeOut - this.startFadeOut);
            int j = this.normalizeTime(timeOfDay - this.startFadeOut);
            return 1.0F - (float)j / (float)i;
        } else {
            return 0.0F;
        }
    }

    private void renderSide(PoseStack matrixStackIn, Tesselator tess, int side) {
        float f = (float)(side % 3) / 3.0F;
        float f1 = (float)(side / 3) / 2.0F;
        Matrix4f matrix4f = matrixStackIn.last().pose();
        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        BufferBuilder bufferbuilder = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        this.addVertex(matrix4f, bufferbuilder, -100.0F, -100.0F, -100.0F, f, f1);
        this.addVertex(matrix4f, bufferbuilder, -100.0F, -100.0F, 100.0F, f, f1 + 0.5F);
        this.addVertex(matrix4f, bufferbuilder, 100.0F, -100.0F, 100.0F, f + 0.33333334F, f1 + 0.5F);
        this.addVertex(matrix4f, bufferbuilder, 100.0F, -100.0F, -100.0F, f + 0.33333334F, f1);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
    }

    private void addVertex(Matrix4f matrix4f, BufferBuilder buffer, float x, float y, float z, float u, float v) {
        float f = MathUtils.getTransformX(matrix4f, x, y, z);
        float f1 = MathUtils.getTransformY(matrix4f, x, y, z);
        float f2 = MathUtils.getTransformZ(matrix4f, x, y, z);
        buffer.addVertex(f, f1, f2).setUv(u, v);
    }

    public boolean isActive(Level world, int timeOfDay) {
        if (world != this.lastWorld) {
            this.lastWorld = world;
            this.smoothPositionBrightness = null;
        }

        if (this.timeBetween(timeOfDay, this.endFadeOut, this.startFadeIn)) {
            return false;
        } else {
            if (this.days != null) {
                long i = world.getDayTime();
                long j = i - (long)this.startFadeIn;

                while (j < 0L) {
                    j += (long)(24000 * this.daysLoop);
                }

                int k = (int)(j / 24000L);
                int l = k % this.daysLoop;
                if (!this.days.isInRange(l)) {
                    return false;
                }
            }

            return true;
        }
    }

    private boolean timeBetween(int timeOfDay, int timeStart, int timeEnd) {
        return timeStart <= timeEnd ? timeOfDay >= timeStart && timeOfDay <= timeEnd : timeOfDay >= timeStart || timeOfDay <= timeEnd;
    }

    @Override
    public String toString() {
        return this.source + ", " + this.startFadeIn + "-" + this.endFadeIn + " " + this.startFadeOut + "-" + this.endFadeOut;
    }
}