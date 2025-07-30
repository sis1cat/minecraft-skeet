package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.CompiledShader;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.optifine.Config;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class ShaderManager extends SimplePreparableReloadListener<ShaderManager.Configs> implements AutoCloseable {
    static final Logger LOGGER = LogUtils.getLogger();
    public static final String SHADER_PATH = "shaders";
    public static final String SHADER_INCLUDE_PATH = "shaders/include/";
    private static final FileToIdConverter PROGRAM_ID_CONVERTER = FileToIdConverter.json("shaders");
    private static final FileToIdConverter POST_CHAIN_ID_CONVERTER = FileToIdConverter.json("post_effect");
    public static final int MAX_LOG_LENGTH = 32768;
    final TextureManager textureManager;
    private final Consumer<Exception> recoveryHandler;
    private ShaderManager.CompilationCache compilationCache = new ShaderManager.CompilationCache(ShaderManager.Configs.EMPTY);

    public ShaderManager(TextureManager pTextureManager, Consumer<Exception> pRecoveryHandler) {
        this.textureManager = pTextureManager;
        this.recoveryHandler = pRecoveryHandler;
    }

    protected ShaderManager.Configs prepare(ResourceManager p_363890_, ProfilerFiller p_362646_) {
        Builder<ResourceLocation, ShaderProgramConfig> builder = ImmutableMap.builder();
        Builder<ShaderManager.ShaderSourceKey, String> builder1 = ImmutableMap.builder();
        Map<ResourceLocation, Resource> map = p_363890_.listResources("shaders", locIn -> isProgram(locIn) || isShader(locIn));
        String[] astring = new String[]{"fxaa_of_2x", "fxaa_of_4x"};
        String[] astring1 = new String[]{".json", ".vsh", ".fsh"};

        for (int i = 0; i < astring.length; i++) {
            String s = astring[i];

            for (int j = 0; j < astring1.length; j++) {
                String s1 = astring1[j];
                String s2 = "shaders/post/" + s + s1;
                ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace(s2);
                Optional<Resource> optional = Config.getResourceSafe(resourcelocation);
                if (optional.isEmpty()) {
                    Config.warn("Resource not found: " + resourcelocation);
                } else {
                    map.put(resourcelocation, optional.get());
                }
            }
        }

        for (Entry<ResourceLocation, Resource> entry : map.entrySet()) {
            ResourceLocation resourcelocation1 = entry.getKey();
            CompiledShader.Type compiledshader$type = CompiledShader.Type.byLocation(resourcelocation1);
            if (compiledshader$type != null) {
                loadShader(resourcelocation1, entry.getValue(), compiledshader$type, map, builder1);
            } else if (isProgram(resourcelocation1)) {
                loadProgram(resourcelocation1, entry.getValue(), builder);
            }
        }

        Builder<ResourceLocation, PostChainConfig> builder2 = ImmutableMap.builder();
        Map<ResourceLocation, Resource> map1 = POST_CHAIN_ID_CONVERTER.listMatchingResources(p_363890_);

        for (int k = 0; k < astring.length; k++) {
            String s3 = astring[k];
            String s4 = "post_effect/" + s3 + ".json";
            ResourceLocation resourcelocation2 = ResourceLocation.withDefaultNamespace(s4);
            Optional<Resource> optional1 = Config.getResourceSafe(resourcelocation2);
            if (optional1.isEmpty()) {
                Config.warn("Resource not found: " + resourcelocation2);
            } else {
                map1.put(resourcelocation2, optional1.get());
            }
        }

        for (Entry<ResourceLocation, Resource> entry1 : map1.entrySet()) {
            loadPostChain(entry1.getKey(), entry1.getValue(), builder2);
        }

        return new ShaderManager.Configs(builder.build(), builder1.build(), builder2.build());
    }

    private static void loadShader(
        ResourceLocation pLocation,
        Resource pShader,
        CompiledShader.Type pType,
        Map<ResourceLocation, Resource> pShaderResources,
        Builder<ShaderManager.ShaderSourceKey, String> pOutput
    ) {
        ResourceLocation resourcelocation = pType.idConverter().fileToId(pLocation);
        GlslPreprocessor glslpreprocessor = createPreprocessor(pShaderResources, pLocation);

        try (Reader reader = pShader.openAsReader()) {
            String s = IOUtils.toString(reader);
            pOutput.put(new ShaderManager.ShaderSourceKey(resourcelocation, pType), String.join("", glslpreprocessor.process(s)));
        } catch (IOException ioexception1) {
            LOGGER.error("Failed to load shader source at {}", pLocation, ioexception1);
        }
    }

    private static GlslPreprocessor createPreprocessor(final Map<ResourceLocation, Resource> pShaderResources, ResourceLocation pShaderLocation) {
        final ResourceLocation resourcelocation = pShaderLocation.withPath(FileUtil::getFullResourcePath);
        return new GlslPreprocessor() {
            private final Set<ResourceLocation> importedLocations = new ObjectArraySet<>();

            @Override
            public String applyImport(boolean p_365562_, String p_361440_) {
                ResourceLocation resourcelocation1;
                try {
                    if (p_365562_) {
                        resourcelocation1 = resourcelocation.withPath(pathIn -> FileUtil.normalizeResourcePath(pathIn + p_361440_));
                    } else {
                        resourcelocation1 = ResourceLocation.parse(p_361440_).withPrefix("shaders/include/");
                    }
                } catch (ResourceLocationException resourcelocationexception) {
                    ShaderManager.LOGGER.error("Malformed GLSL import {}: {}", p_361440_, resourcelocationexception.getMessage());
                    return "#error " + resourcelocationexception.getMessage();
                }

                if (!this.importedLocations.add(resourcelocation1)) {
                    return null;
                } else {
                    try {
                        String s;
                        try (Reader reader = pShaderResources.get(resourcelocation1).openAsReader()) {
                            s = IOUtils.toString(reader);
                        }

                        return s;
                    } catch (IOException ioexception1) {
                        ShaderManager.LOGGER.error("Could not open GLSL import {}: {}", resourcelocation1, ioexception1.getMessage());
                        return "#error " + ioexception1.getMessage();
                    }
                }
            }
        };
    }

    private static void loadProgram(ResourceLocation pLocation, Resource pResource, Builder<ResourceLocation, ShaderProgramConfig> pOutput) {
        ResourceLocation resourcelocation = PROGRAM_ID_CONVERTER.fileToId(pLocation);

        try (Reader reader = pResource.openAsReader()) {
            JsonElement jsonelement = JsonParser.parseReader(reader);
            ShaderProgramConfig shaderprogramconfig = ShaderProgramConfig.CODEC.parse(JsonOps.INSTANCE, jsonelement).getOrThrow(JsonSyntaxException::new);
            pOutput.put(resourcelocation, shaderprogramconfig);
        } catch (IOException | JsonParseException jsonparseexception) {
            LOGGER.error("Failed to parse shader config at {}", pLocation, jsonparseexception);
        }
    }

    private static void loadPostChain(ResourceLocation pLocation, Resource pPostChain, Builder<ResourceLocation, PostChainConfig> pOutput) {
        ResourceLocation resourcelocation = POST_CHAIN_ID_CONVERTER.fileToId(pLocation);

        try (Reader reader = pPostChain.openAsReader()) {
            JsonElement jsonelement = JsonParser.parseReader(reader);
            pOutput.put(resourcelocation, PostChainConfig.CODEC.parse(JsonOps.INSTANCE, jsonelement).getOrThrow(JsonSyntaxException::new));
        } catch (IOException | JsonParseException jsonparseexception) {
            LOGGER.error("Failed to parse post chain at {}", pLocation, jsonparseexception);
        }
    }

    private static boolean isProgram(ResourceLocation pLocation) {
        return pLocation.getPath().endsWith(".json");
    }

    private static boolean isShader(ResourceLocation pLocation) {
        return CompiledShader.Type.byLocation(pLocation) != null || pLocation.getPath().endsWith(".glsl");
    }

    protected void apply(ShaderManager.Configs p_360858_, ResourceManager p_369986_, ProfilerFiller p_364135_) {
        ShaderManager.CompilationCache shadermanager$compilationcache = new ShaderManager.CompilationCache(p_360858_);
        Map<ShaderProgram, ShaderManager.CompilationException> map = new HashMap<>();
        Set<ShaderProgram> set = new HashSet<>(CoreShaders.getProgramsToPreload());

        for (PostChainConfig postchainconfig : p_360858_.postChains.values()) {
            for (PostChainConfig.Pass postchainconfig$pass : postchainconfig.passes()) {
                set.add(postchainconfig$pass.program());
            }
        }

        for (ShaderProgram shaderprogram : set) {
            try {
                shadermanager$compilationcache.programs.put(shaderprogram, Optional.of(shadermanager$compilationcache.compileProgram(shaderprogram)));
            } catch (ShaderManager.CompilationException shadermanager$compilationexception) {
                map.put(shaderprogram, shadermanager$compilationexception);
            }
        }

        if (!map.isEmpty()) {
            shadermanager$compilationcache.close();
            throw new RuntimeException(
                "Failed to load required shader programs:\n"
                    + map.entrySet()
                        .stream()
                        .map(entryIn -> " - " + entryIn.getKey() + ": " + entryIn.getValue().getMessage())
                        .collect(Collectors.joining("\n"))
            );
        } else {
            this.compilationCache.close();
            this.compilationCache = shadermanager$compilationcache;
        }
    }

    @Override
    public String getName() {
        return "Shader Loader";
    }

    private void tryTriggerRecovery(Exception pException) {
        if (!this.compilationCache.triggeredRecovery) {
            this.recoveryHandler.accept(pException);
            this.compilationCache.triggeredRecovery = true;
        }
    }

    public void preloadForStartup(ResourceProvider pResourceProvider, ShaderProgram... pPrograms) throws IOException, ShaderManager.CompilationException {
        for (ShaderProgram shaderprogram : pPrograms) {
            Resource resource = pResourceProvider.getResourceOrThrow(PROGRAM_ID_CONVERTER.idToFile(shaderprogram.configId()));

            try (Reader reader = resource.openAsReader()) {
                JsonElement jsonelement = JsonParser.parseReader(reader);
                ShaderProgramConfig shaderprogramconfig = ShaderProgramConfig.CODEC
                    .parse(JsonOps.INSTANCE, jsonelement)
                    .getOrThrow(JsonSyntaxException::new);
                ShaderDefines shaderdefines = shaderprogramconfig.defines().withOverrides(shaderprogram.defines());
                CompiledShader compiledshader = this.preloadShader(pResourceProvider, shaderprogramconfig.vertex(), CompiledShader.Type.VERTEX, shaderdefines);
                CompiledShader compiledshader1 = this.preloadShader(pResourceProvider, shaderprogramconfig.fragment(), CompiledShader.Type.FRAGMENT, shaderdefines);
                CompiledShaderProgram compiledshaderprogram = linkProgram(shaderprogram, shaderprogramconfig, compiledshader, compiledshader1);
                this.compilationCache.programs.put(shaderprogram, Optional.of(compiledshaderprogram));
            }
        }
    }

    private CompiledShader preloadShader(ResourceProvider pResourceProvider, ResourceLocation pShader, CompiledShader.Type pType, ShaderDefines pDefines) throws IOException, ShaderManager.CompilationException {
        ResourceLocation resourcelocation = pType.idConverter().idToFile(pShader);

        CompiledShader compiledshader;
        try (Reader reader = pResourceProvider.getResourceOrThrow(resourcelocation).openAsReader()) {
            String s = IOUtils.toString(reader);
            String s1 = GlslPreprocessor.injectDefines(s, pDefines);
            CompiledShader compiledshader1 = CompiledShader.compile(pShader, pType, s1);
            this.compilationCache.shaders.put(new ShaderManager.ShaderCompilationKey(pShader, pType, pDefines), compiledshader1);
            compiledshader = compiledshader1;
        }

        return compiledshader;
    }

    @Nullable
    public CompiledShaderProgram getProgram(ShaderProgram pProgram) {
        try {
            return this.compilationCache.getOrCompileProgram(pProgram);
        } catch (ShaderManager.CompilationException shadermanager$compilationexception) {
            LOGGER.error("Failed to load shader program: {}", pProgram, shadermanager$compilationexception);
            this.compilationCache.programs.put(pProgram, Optional.empty());
            this.tryTriggerRecovery(shadermanager$compilationexception);
            return null;
        }
    }

    public CompiledShaderProgram getProgramForLoading(ShaderProgram pProgram) throws ShaderManager.CompilationException {
        CompiledShaderProgram compiledshaderprogram = this.compilationCache.getOrCompileProgram(pProgram);
        if (compiledshaderprogram == null) {
            throw new ShaderManager.CompilationException("Shader '" + pProgram + "' could not be found");
        } else {
            return compiledshaderprogram;
        }
    }

    static CompiledShaderProgram linkProgram(ShaderProgram pProgram, ShaderProgramConfig pConfig, CompiledShader pVertexShader, CompiledShader pFragmentShader) throws ShaderManager.CompilationException {
        CompiledShaderProgram compiledshaderprogram = CompiledShaderProgram.link(pVertexShader, pFragmentShader, pProgram.vertexFormat());
        compiledshaderprogram.setupUniforms(pConfig.uniforms(), pConfig.samplers());
        return compiledshaderprogram;
    }

    @Nullable
    public PostChain getPostChain(ResourceLocation pId, Set<ResourceLocation> pExternalTargets) {
        try {
            return this.compilationCache.getOrLoadPostChain(pId, pExternalTargets);
        } catch (ShaderManager.CompilationException shadermanager$compilationexception) {
            LOGGER.error("Failed to load post chain: {}", pId, shadermanager$compilationexception);
            this.compilationCache.postChains.put(pId, Optional.empty());
            this.tryTriggerRecovery(shadermanager$compilationexception);
            return null;
        }
    }

    @Override
    public void close() {
        this.compilationCache.close();
    }

    class CompilationCache implements AutoCloseable {
        private final ShaderManager.Configs configs;
        final Map<ShaderProgram, Optional<CompiledShaderProgram>> programs = new HashMap<>();
        final Map<ShaderManager.ShaderCompilationKey, CompiledShader> shaders = new HashMap<>();
        final Map<ResourceLocation, Optional<PostChain>> postChains = new HashMap<>();
        boolean triggeredRecovery;

        CompilationCache(final ShaderManager.Configs pConfigs) {
            this.configs = pConfigs;
        }

        @Nullable
        public CompiledShaderProgram getOrCompileProgram(ShaderProgram pProgram) throws ShaderManager.CompilationException {
            Optional<CompiledShaderProgram> optional = this.programs.get(pProgram);
            if (optional != null) {
                return optional.orElse(null);
            } else {
                CompiledShaderProgram compiledshaderprogram = this.compileProgram(pProgram);
                this.programs.put(pProgram, Optional.of(compiledshaderprogram));
                return compiledshaderprogram;
            }
        }

        CompiledShaderProgram compileProgram(ShaderProgram pProgram) throws ShaderManager.CompilationException {
            ShaderProgramConfig shaderprogramconfig = this.configs.programs.get(pProgram.configId());
            if (shaderprogramconfig == null) {
                throw new ShaderManager.CompilationException("Could not find program with id: " + pProgram.configId());
            } else {
                ShaderDefines shaderdefines = shaderprogramconfig.defines().withOverrides(pProgram.defines());
                CompiledShader compiledshader = this.getOrCompileShader(shaderprogramconfig.vertex(), CompiledShader.Type.VERTEX, shaderdefines);
                CompiledShader compiledshader1 = this.getOrCompileShader(shaderprogramconfig.fragment(), CompiledShader.Type.FRAGMENT, shaderdefines);
                return ShaderManager.linkProgram(pProgram, shaderprogramconfig, compiledshader, compiledshader1);
            }
        }

        private CompiledShader getOrCompileShader(ResourceLocation pId, CompiledShader.Type pType, ShaderDefines pDefines) throws ShaderManager.CompilationException {
            ShaderManager.ShaderCompilationKey shadermanager$shadercompilationkey = new ShaderManager.ShaderCompilationKey(pId, pType, pDefines);
            CompiledShader compiledshader = this.shaders.get(shadermanager$shadercompilationkey);
            if (compiledshader == null) {
                compiledshader = this.compileShader(shadermanager$shadercompilationkey);
                this.shaders.put(shadermanager$shadercompilationkey, compiledshader);
            }

            return compiledshader;
        }

        private CompiledShader compileShader(ShaderManager.ShaderCompilationKey pCompilationKey) throws ShaderManager.CompilationException {
            String s = this.configs.shaderSources.get(new ShaderManager.ShaderSourceKey(pCompilationKey.id, pCompilationKey.type));
            if (s == null) {
                throw new ShaderManager.CompilationException("Could not find shader: " + pCompilationKey);
            } else {
                CompiledShader.Type compiledshader$type = pCompilationKey.type;
                if (compiledshader$type == CompiledShader.Type.VERTEX) {
                    s = s.replace("texelFetch(Sampler2, UV2 / 16, 0)", "texture(Sampler2, (UV2 / 256.0) + (0.5 / 16.0))");
                    s = s.replace("minecraft_sample_lightmap(Sampler2, UV2)", "texture(Sampler2, (UV2 / 256.0) + (0.5 / 16.0))");
                }

                if (compiledshader$type == CompiledShader.Type.FRAGMENT) {
                    pCompilationKey = pCompilationKey.replaceDefineValue("ALPHA_CUTOUT", "0.5", "0.1");
                }

                String s1 = GlslPreprocessor.injectDefines(s, pCompilationKey.defines);
                return CompiledShader.compile(pCompilationKey.id, pCompilationKey.type, s1);
            }
        }

        @Nullable
        public PostChain getOrLoadPostChain(ResourceLocation pName, Set<ResourceLocation> pExternalTargets) throws ShaderManager.CompilationException {
            Optional<PostChain> optional = this.postChains.get(pName);
            if (optional != null) {
                return optional.orElse(null);
            } else {
                PostChain postchain = this.loadPostChain(pName, pExternalTargets);
                this.postChains.put(pName, Optional.of(postchain));
                return postchain;
            }
        }

        private PostChain loadPostChain(ResourceLocation pName, Set<ResourceLocation> pExternalTargets) throws ShaderManager.CompilationException {
            PostChainConfig postchainconfig = this.configs.postChains.get(pName);
            if (postchainconfig == null) {
                throw new ShaderManager.CompilationException("Could not find post chain with id: " + pName);
            } else {
                return PostChain.load(postchainconfig, ShaderManager.this.textureManager, ShaderManager.this, pExternalTargets);
            }
        }

        @Override
        public void close() {
            RenderSystem.assertOnRenderThread();
            this.programs.values().forEach(progIn -> progIn.ifPresent(CompiledShaderProgram::close));
            this.shaders.values().forEach(CompiledShader::close);
            this.programs.clear();
            this.shaders.clear();
            this.postChains.clear();
        }
    }

    public static class CompilationException extends Exception {
        public CompilationException(String pMessage) {
            super(pMessage);
        }
    }

    public static record Configs(
        Map<ResourceLocation, ShaderProgramConfig> programs,
        Map<ShaderManager.ShaderSourceKey, String> shaderSources,
        Map<ResourceLocation, PostChainConfig> postChains
    ) {
        public static final ShaderManager.Configs EMPTY = new ShaderManager.Configs(Map.of(), Map.of(), Map.of());
    }

    static record ShaderCompilationKey(ResourceLocation id, CompiledShader.Type type, ShaderDefines defines) {
        @Override
        public String toString() {
            String s = this.id + " (" + this.type + ")";
            return !this.defines.isEmpty() ? s + " with " + this.defines : s;
        }

        public ShaderManager.ShaderCompilationKey replaceDefineValue(String key, String valOld, String valNew) {
            if (!this.defines.values().containsKey(key)) {
                return this;
            } else {
                Map<String, String> map = new HashMap<>(this.defines.values());
                if (!map.replace(key, valOld, valNew)) {
                    return this;
                } else {
                    ShaderDefines shaderdefines = new ShaderDefines(map, this.defines.flags());
                    return new ShaderManager.ShaderCompilationKey(this.id, this.type, shaderdefines);
                }
            }
        }
    }

    static record ShaderSourceKey(ResourceLocation id, CompiledShader.Type type) {
        @Override
        public String toString() {
            return this.id + " (" + this.type + ")";
        }
    }
}