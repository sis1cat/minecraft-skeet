package net.minecraft.client.renderer;

import com.darkmagician6.eventapi.EventManager;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.shaders.CompiledShader;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.optifine.Config;
import net.optifine.render.RenderUtils;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersTex;
import org.jetbrains.annotations.VisibleForTesting;
import org.joml.Matrix4f;
import sisicat.events.LevelRendererEvent;

public class CompiledShaderProgram implements AutoCloseable {
    private static final AbstractUniform DUMMY_UNIFORM = new AbstractUniform();
    private static final int NO_SAMPLER_TEXTURE = -1;
    private final List<ShaderProgramConfig.Sampler> samplers = new ArrayList<>();
    private final Object2IntMap<String> samplerTextures = new Object2IntArrayMap<>();
    private final IntList samplerLocations = new IntArrayList();
    private final List<Uniform> uniforms = new ArrayList<>();
    private final Map<String, Uniform> uniformsByName = new HashMap<>();
    private final Map<String, ShaderProgramConfig.Uniform> uniformConfigs = new HashMap<>();
    private final int programId;
    @Nullable
    public Uniform MODEL_VIEW_MATRIX;
    @Nullable
    public Uniform PROJECTION_MATRIX;
    @Nullable
    public Uniform TEXTURE_MATRIX;
    @Nullable
    public Uniform SCREEN_SIZE;
    @Nullable
    public Uniform COLOR_MODULATOR;
    @Nullable
    public Uniform LIGHT0_DIRECTION;
    @Nullable
    public Uniform LIGHT1_DIRECTION;
    @Nullable
    public Uniform GLINT_ALPHA;
    @Nullable
    public Uniform FOG_START;
    @Nullable
    public Uniform FOG_END;
    @Nullable
    public Uniform FOG_COLOR;
    @Nullable
    public Uniform FOG_SHAPE;
    @Nullable
    public Uniform LINE_WIDTH;
    @Nullable
    public Uniform GAME_TIME;
    @Nullable
    public Uniform MODEL_OFFSET;
    private static int lastProgramId = -1;

    private CompiledShaderProgram(int pProgramId) {
        this.programId = pProgramId;
        this.samplerTextures.defaultReturnValue(-1);
    }

    public static CompiledShaderProgram link(CompiledShader pVertexShader, CompiledShader pFragmentShader, VertexFormat pVertexFormat) throws ShaderManager.CompilationException {
        int i = GlStateManager.glCreateProgram();
        if (i <= 0) {
            throw new ShaderManager.CompilationException("Could not create shader program (returned program ID " + i + ")");
        } else {
            pVertexFormat.bindAttributes(i);
            GlStateManager.glAttachShader(i, pVertexShader.getShaderId());
            GlStateManager.glAttachShader(i, pFragmentShader.getShaderId());
            GlStateManager.glLinkProgram(i);
            int j = GlStateManager.glGetProgrami(i, 35714);
            if (j == 0) {
                String s = GlStateManager.glGetProgramInfoLog(i, 32768);
                throw new ShaderManager.CompilationException(
                    "Error encountered when linking program containing VS " + pVertexShader.getId() + " and FS " + pFragmentShader.getId() + ". Log output: " + s
                );
            } else {
                return new CompiledShaderProgram(i);
            }
        }
    }

    public void setupUniforms(List<ShaderProgramConfig.Uniform> pUniforms, List<ShaderProgramConfig.Sampler> pSamplers) {
        RenderSystem.assertOnRenderThread();

        for (ShaderProgramConfig.Uniform shaderprogramconfig$uniform : pUniforms) {
            String s = shaderprogramconfig$uniform.name();
            int i = Uniform.glGetUniformLocation(this.programId, s);
            if (i != -1) {
                Uniform uniform = this.parseUniformNode(shaderprogramconfig$uniform);
                uniform.setLocation(i);
                this.uniforms.add(uniform);
                this.uniformsByName.put(s, uniform);
                this.uniformConfigs.put(s, shaderprogramconfig$uniform);
            }
        }

        for (ShaderProgramConfig.Sampler shaderprogramconfig$sampler : pSamplers) {
            int j = Uniform.glGetUniformLocation(this.programId, shaderprogramconfig$sampler.name());
            if (j != -1) {
                this.samplers.add(shaderprogramconfig$sampler);
                this.samplerLocations.add(j);
            }
        }

        this.MODEL_VIEW_MATRIX = this.getUniform("ModelViewMat");
        this.PROJECTION_MATRIX = this.getUniform("ProjMat");
        this.TEXTURE_MATRIX = this.getUniform("TextureMat");
        this.SCREEN_SIZE = this.getUniform("ScreenSize");
        this.COLOR_MODULATOR = this.getUniform("ColorModulator");
        this.LIGHT0_DIRECTION = this.getUniform("Light0_Direction");
        this.LIGHT1_DIRECTION = this.getUniform("Light1_Direction");
        this.GLINT_ALPHA = this.getUniform("GlintAlpha");
        this.FOG_START = this.getUniform("FogStart");
        this.FOG_END = this.getUniform("FogEnd");
        this.FOG_COLOR = this.getUniform("FogColor");
        this.FOG_SHAPE = this.getUniform("FogShape");
        this.LINE_WIDTH = this.getUniform("LineWidth");
        this.GAME_TIME = this.getUniform("GameTime");
        this.MODEL_OFFSET = this.getUniform("ModelOffset");
    }

    @Override
    public void close() {
        this.uniforms.forEach(Uniform::close);
        GlStateManager.glDeleteProgram(this.programId);
    }

    public void clear() {
        RenderSystem.assertOnRenderThread();
        lastProgramId = -1;
        int i = GlStateManager._getActiveTexture();
        if (Boolean.FALSE) {
            for (int j = 0; j < this.samplerLocations.size(); j++) {
                ShaderProgramConfig.Sampler shaderprogramconfig$sampler = this.samplers.get(j);
                if (!this.samplerTextures.containsKey(shaderprogramconfig$sampler.name())) {
                    int k = this.getTextureUnit(shaderprogramconfig$sampler.name(), j);
                    GlStateManager._activeTexture(33984 + k);
                    GlStateManager._bindTexture(0);
                }
            }
        }

        GlStateManager._activeTexture(i);
    }

    public void apply() {
        RenderSystem.assertOnRenderThread();
        if (Config.getMinecraft().getOverlay() != null) {
            GlStateManager._glUseProgram(0);
        }

        GlStateManager._glUseProgram(this.programId);
        lastProgramId = this.programId;
        int i = GlStateManager._getActiveTexture();

        for (int j = 0; j < this.samplerLocations.size(); j++) {
            String s = this.samplers.get(j).name();
            int k = this.samplerTextures.getInt(s);
            if (k != -1) {
                int l = this.samplerLocations.getInt(j);
                int i1 = this.getTextureUnit(s, j);
                Uniform.uploadInteger(l, i1);
                RenderSystem.activeTexture(33984 + i1);
                if (i1 != 1 || Shaders.activeProgramID <= 0 || !Shaders.isOverlayDisabled()) {
                    if (Config.isShaders()) {
                        ShadersTex.bindTexture(k);
                    } else {
                        RenderSystem.bindTexture(k);
                    }
                }
            }
        }

        GlStateManager._activeTexture(i);

        for (Uniform uniform : this.uniforms) {
            uniform.upload();
        }

        if (Config.isShaders() && Shaders.activeProgramID > 0) {
            GlStateManager._glUseProgram(Shaders.activeProgramID);
            boolean flag = RenderUtils.setFlushRenderBuffers(false);
            Shaders.uniform_atlasSize.setValue(Shaders.atlasSizeX, Shaders.atlasSizeY);
            RenderUtils.setFlushRenderBuffers(flag);
        }
    }

    @Nullable
    public Uniform getUniform(String pName) {
        RenderSystem.assertOnRenderThread();
        return this.uniformsByName.get(pName);
    }

    @Nullable
    public ShaderProgramConfig.Uniform getUniformConfig(String pName) {
        return this.uniformConfigs.get(pName);
    }

    public AbstractUniform safeGetUniform(String pName) {
        Uniform uniform = this.getUniform(pName);
        return (AbstractUniform)(uniform == null ? DUMMY_UNIFORM : uniform);
    }

    public void bindSampler(String pSamplerName, int pTextureId) {
        this.samplerTextures.put(pSamplerName, pTextureId);
    }

    private Uniform parseUniformNode(ShaderProgramConfig.Uniform pUniform) {
        int i = Uniform.getTypeFromString(pUniform.type());
        int j = pUniform.count();
        int k = j > 1 && j <= 4 && i < 8 ? j - 1 : 0;
        Uniform uniform = new Uniform(pUniform.name(), i + k, j);
        uniform.setFromConfig(pUniform);
        return uniform;
    }

    public void setDefaultUniforms(VertexFormat.Mode pMode, Matrix4f pFrustumMatrix, Matrix4f pProjectionMatrix, Window pWindow) {
        for (int i = 0; i < 12; i++) {
            int j = RenderSystem.getShaderTexture(i);
            this.bindSampler(getSamplerName(i), j);
        }

        if (this.MODEL_VIEW_MATRIX != null) {
            this.MODEL_VIEW_MATRIX.set(pFrustumMatrix);
        }

        if (this.PROJECTION_MATRIX != null) {
            this.PROJECTION_MATRIX.set(pProjectionMatrix);
        }

        if (this.COLOR_MODULATOR != null) {
            this.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }

        if (this.GLINT_ALPHA != null) {
            this.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        }

        FogParameters fogparameters = RenderSystem.getShaderFog();

        LevelRendererEvent levelRendererEvent = new LevelRendererEvent(
                fogparameters.start(),
                fogparameters.end(),
                new float[] {fogparameters.red(), fogparameters.green(), fogparameters.blue(), fogparameters.alpha()},
                RenderSystem.getShaderGameTime()
        );


            //EventManager.call(levelRendererEvent);

        if (this.FOG_START != null) {
            this.FOG_START.set(levelRendererEvent.fogStart);
        }

        if (this.FOG_END != null) {
            this.FOG_END.set(levelRendererEvent.fogEnd);
        }

        if (this.FOG_COLOR != null) {
            this.FOG_COLOR.set(levelRendererEvent.fogColor[0], levelRendererEvent.fogColor[1], levelRendererEvent.fogColor[2], levelRendererEvent.fogColor[3]);
        }

        if (this.FOG_SHAPE != null) {
            this.FOG_SHAPE.set(fogparameters.shape().getIndex());
        }

        if (this.TEXTURE_MATRIX != null) {
            this.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }

        if (this.GAME_TIME != null) {
            this.GAME_TIME.set(levelRendererEvent.gameTime);
        }

        if (this.SCREEN_SIZE != null) {
            this.SCREEN_SIZE.set((float)pWindow.getWidth(), (float)pWindow.getHeight());
        }

        if (this.LINE_WIDTH != null && (pMode == VertexFormat.Mode.LINES || pMode == VertexFormat.Mode.LINE_STRIP)) {
            this.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
        }

        RenderSystem.setupShaderLights(this);
    }

    public void setDefaultUniformsFog(VertexFormat.Mode pMode, Matrix4f pFrustumMatrix, Matrix4f pProjectionMatrix, Window pWindow) {
        for (int i = 0; i < 12; i++) {
            int j = RenderSystem.getShaderTexture(i);
            this.bindSampler(getSamplerName(i), j);
        }

        if (this.MODEL_VIEW_MATRIX != null) {
            this.MODEL_VIEW_MATRIX.set(pFrustumMatrix);
        }

        if (this.PROJECTION_MATRIX != null) {
            this.PROJECTION_MATRIX.set(pProjectionMatrix);
        }

        if (this.COLOR_MODULATOR != null) {
            this.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }

        if (this.GLINT_ALPHA != null) {
            this.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        }

        FogParameters fogparameters = RenderSystem.getShaderFog();

        LevelRendererEvent levelRendererEvent = new LevelRendererEvent(
                fogparameters.start(),
                fogparameters.end(),
                new float[] {fogparameters.red(), fogparameters.green(), fogparameters.blue(), fogparameters.alpha()},
                RenderSystem.getShaderGameTime()
        );

        EventManager.call(levelRendererEvent);

        if (this.FOG_START != null) {
            this.FOG_START.set(levelRendererEvent.fogStart);
        }

        if (this.FOG_END != null) {
            this.FOG_END.set(levelRendererEvent.fogEnd);
        }

        if (this.FOG_COLOR != null) {
            this.FOG_COLOR.set(levelRendererEvent.fogColor[0], levelRendererEvent.fogColor[1], levelRendererEvent.fogColor[2], levelRendererEvent.fogColor[3]);
        }

        if (this.FOG_SHAPE != null) {
            this.FOG_SHAPE.set(fogparameters.shape().getIndex());
        }

        if (this.TEXTURE_MATRIX != null) {
            this.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }

        if (this.GAME_TIME != null) {
            this.GAME_TIME.set(levelRendererEvent.gameTime);
        }

        if (this.SCREEN_SIZE != null) {
            this.SCREEN_SIZE.set((float)pWindow.getWidth(), (float)pWindow.getHeight());
        }

        if (this.LINE_WIDTH != null && (pMode == VertexFormat.Mode.LINES || pMode == VertexFormat.Mode.LINE_STRIP)) {
            this.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
        }

        RenderSystem.setupShaderLights(this);
    }

    @VisibleForTesting
    public void registerUniform(Uniform pUniform) {
        this.uniforms.add(pUniform);
        this.uniformsByName.put(pUniform.getName(), pUniform);
    }

    @VisibleForTesting
    public int getProgramId() {
        return this.programId;
    }

    public static void useVanillaProgram() {
        if (lastProgramId > 0) {
            GlStateManager._glUseProgram(lastProgramId);
        }
    }

    private int getTextureUnit(String sampler, int index) {
        if (sampler.equals("Sampler0")) {
            return 0;
        } else if (sampler.equals("Sampler1")) {
            return 1;
        } else {
            return sampler.equals("Sampler2") ? 2 : index;
        }
    }

    public static String getSamplerName(int indexIn) {
        switch (indexIn) {
            case 0:
                return "Sampler0";
            case 1:
                return "Sampler1";
            case 2:
                return "Sampler2";
            case 3:
                return "Sampler3";
            case 4:
                return "Sampler4";
            case 5:
                return "Sampler5";
            case 6:
                return "Sampler6";
            case 7:
                return "Sampler7";
            case 8:
                return "Sampler8";
            case 9:
                return "Sampler9";
            case 10:
                return "Sampler10";
            case 11:
                return "Sampler11";
            default:
                return "Sampler" + indexIn;
        }
    }
}