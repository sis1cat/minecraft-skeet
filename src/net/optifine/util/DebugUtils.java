package net.optifine.util;

import com.mojang.blaze3d.vertex.VertexBuffer;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.optifine.Config;
import org.joml.Matrix4f;
import org.joml.Runtime;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class DebugUtils {
    private static FloatBuffer floatBuffer16 = BufferUtils.createFloatBuffer(16);
    private static float[] floatArray16 = new float[16];
    private static String[] FRAME_UNIFORMS = new String[0];
    private static NumberFormat NUMBER_FORMAT = new DecimalFormat(" 0.00E0;-", new DecimalFormatSymbols(Locale.ROOT));
    private static Pattern PATTERN_EXP = Pattern.compile("E(\\d)");

    public static String getGlModelView() {
        floatBuffer16.clear();
        GL11.glGetFloatv(2982, floatBuffer16);
        floatBuffer16.get(floatArray16);
        float[] afloat = transposeMat4(floatArray16);
        return getMatrix4(afloat);
    }

    public static String getGlProjection() {
        floatBuffer16.clear();
        GL11.glGetFloatv(2983, floatBuffer16);
        floatBuffer16.get(floatArray16);
        float[] afloat = transposeMat4(floatArray16);
        return getMatrix4(afloat);
    }

    private static float[] transposeMat4(float[] arr) {
        float[] afloat = new float[16];

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                afloat[i * 4 + j] = arr[j * 4 + i];
            }
        }

        return afloat;
    }

    public static String getMatrix4(Matrix4f mat) {
        MathUtils.write(mat, floatArray16);
        return getMatrix4(floatArray16);
    }

    public static String getMatrix3(float[] fs) {
        return getMatrix3(fs, "\n");
    }

    public static String getMatrix3(float[] fs, String lineSep) {
        return getMatrix(fs, 3, lineSep);
    }

    public static String getMatrix4(float[] fs) {
        return getMatrix4(fs, "\n");
    }

    public static String getMatrix4(float[] fs, String lineSep) {
        return getMatrix(fs, 4, lineSep);
    }

    public static String getMatrix(float[] fs, int cols, String lineSep) {
        StringBuffer stringbuffer = new StringBuffer();

        for (int i = 0; i < fs.length; i++) {
            String s = format((double)fs[i]);
            if (i > 0) {
                if (i % cols == 0) {
                    stringbuffer.append(lineSep);
                } else {
                    stringbuffer.append(" ");
                }
            }

            s = StrUtils.fillLeft(s, 5, ' ');
            stringbuffer.append(s);
        }

        return stringbuffer.toString();
    }

    private static String format(double val) {
        String s = Runtime.format(val, NUMBER_FORMAT);
        s = PATTERN_EXP.matcher(s).replaceAll("E+$1");
        return s.replace('E', 'e');
    }

    public static void debugVboMemory(SectionRenderDispatcher.RenderSection[] renderChunks) {
        if (TimedEvent.isActive("DbgVbos", 3000L)) {
            int i = 0;
            int j = 0;
            int k = 0;
            int l = 0;

            for (int i1 = 0; i1 < renderChunks.length; i1++) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = renderChunks[i1];
                int j1 = i;

                for (RenderType rendertype : SectionRenderDispatcher.BLOCK_RENDER_LAYERS) {
                    VertexBuffer vertexbuffer = sectionrenderdispatcher$rendersection.getBuffer(rendertype);
                    if (vertexbuffer.getIndexCount() > 0) {
                        i += vertexbuffer.getIndexCount() * vertexbuffer.getFormat().getVertexSize();
                        k++;
                    }

                    if (sectionrenderdispatcher$rendersection.getCompiled().isLayerUsed(rendertype)) {
                        l++;
                    }
                }

                if (i > j1) {
                    j++;
                }
            }

            Config.dbg("VRAM: " + i / 1048576 + " MB, vbos: " + k + ", layers: " + l + ", chunks: " + j);
            Config.dbg("VBOs: " + GpuMemory.getBufferAllocated() / 1048576L + " MB");
        }
    }

    public static void debugTextures() {
        Config.dbg(" *** TEXTURES ***");
        TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
        long i = 0L;
        Collection<ResourceLocation> collection = texturemanager.getTextureLocations();
        List<ResourceLocation> list = new ArrayList<>(collection);
        Collections.sort(list);

        for (ResourceLocation resourcelocation : list) {
            AbstractTexture abstracttexture = texturemanager.getTexture(resourcelocation);
            long j = GpuMemory.getTextureSize(abstracttexture);
            if (Config.isShaders()) {
                j *= 3L;
            }

            Config.dbg(resourcelocation + " = " + j);
            i += j;
        }

        Config.dbg("All: " + i);
    }

    public static String getClassName(Object obj) {
        return obj == null ? "null" : obj.getClass().getName();
    }
}