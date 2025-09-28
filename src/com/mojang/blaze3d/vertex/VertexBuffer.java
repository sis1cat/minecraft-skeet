package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.RenderType;
import net.optifine.Config;
import net.optifine.render.MultiTextureData;
import net.optifine.render.MultiTextureRenderer;
import net.optifine.render.VboRange;
import net.optifine.render.VboRegion;
import net.optifine.shaders.SVertexBuilder;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import net.optifine.util.GpuMemory;
import org.joml.Matrix4f;

public class VertexBuffer implements AutoCloseable {
    private final BufferUsage usage;
    private final GpuBuffer vertexBuffer;
    @Nullable
    private GpuBuffer indexBuffer = null;
    private int arrayObjectId;
    @Nullable
    private VertexFormat format;
    @Nullable
    private RenderSystem.AutoStorageIndexBuffer sequentialIndices;
    private VertexFormat.IndexType indexType;
    private int indexCount;
    private VertexFormat.Mode mode;
    private VboRegion vboRegion;
    private VboRange vboRange;
    private MultiTextureData multiTextureData;
    private static ByteBuffer emptyBuffer = GlUtil.allocateMemory(0);

    public VertexBuffer(BufferUsage pUsage) {
        this.usage = pUsage;
        RenderSystem.assertOnRenderThread();
        this.vertexBuffer = new GpuBuffer(BufferType.VERTICES, pUsage, 0);
        this.arrayObjectId = GlStateManager._glGenVertexArrays();
    }

    public static VertexBuffer uploadStatic(VertexFormat.Mode pMode, VertexFormat pFormat, Consumer<VertexConsumer> pBuilder) {
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(pMode, pFormat);
        pBuilder.accept(bufferbuilder);
        VertexBuffer vertexbuffer = new VertexBuffer(BufferUsage.STATIC_WRITE);
        vertexbuffer.bind();
        vertexbuffer.upload(bufferbuilder.buildOrThrow());
        unbind();
        return vertexbuffer;
    }

    public void upload(MeshData pMeshData) {
        MeshData meshdata = pMeshData;

        label48: {
            try {
                if (this.isInvalid()) {
                    break label48;
                }

                RenderSystem.assertOnRenderThread();
                GpuMemory.bufferFreed((long)this.getVertexBufferSize());
                GpuMemory.bufferFreed((long)this.getIndexBufferSize());
                MeshData.DrawState meshdata$drawstate = pMeshData.drawState();
                this.format = this.uploadVertexBuffer(meshdata$drawstate, pMeshData.vertexBuffer());
                this.sequentialIndices = this.uploadIndexBuffer(meshdata$drawstate, pMeshData.indexBuffer());
                this.indexCount = meshdata$drawstate.indexCount();
                this.indexType = meshdata$drawstate.indexType();
                this.mode = meshdata$drawstate.mode();
                GpuMemory.bufferAllocated((long)this.getVertexBufferSize());
                GpuMemory.bufferAllocated((long)this.getIndexBufferSize());
                if (this.vboRegion != null) {
                    ByteBuffer bytebuffer = pMeshData.vertexBuffer();
                    bytebuffer.position(0);
                    bytebuffer.limit(meshdata$drawstate.getVertexBufferSize());
                    this.vboRegion.bufferData(bytebuffer, this.vboRange);
                    bytebuffer.position(0);
                    bytebuffer.limit(meshdata$drawstate.getVertexBufferSize());
                }

                this.multiTextureData = pMeshData.getMultiTextureData();
                this.updateMultiTextureData();
            } catch (Throwable throwable11) {
                if (pMeshData != null) {
                    try {
                        meshdata.close();
                    } catch (Throwable throwable) {
                        throwable11.addSuppressed(throwable);
                    }
                }

                throw throwable11;
            }

            if (pMeshData != null) {
                pMeshData.close();
            }

            return;
        }

        if (pMeshData != null) {
            pMeshData.close();
        }
    }

    public void uploadIndexBuffer(ByteBufferBuilder.Result pResult) {
        ByteBufferBuilder.Result bytebufferbuilder$result = pResult;

        label48: {
            try {
                if (this.isInvalid()) {
                    break label48;
                }

                RenderSystem.assertOnRenderThread();
                if (this.indexBuffer != null) {
                    this.indexBuffer.close();
                }

                this.indexBuffer = new GpuBuffer(BufferType.INDICES, this.usage, pResult.byteBuffer());
                this.sequentialIndices = null;
                this.updateMultiTextureData();
            } catch (Throwable throwable1) {
                if (pResult != null) {
                    try {
                        bytebufferbuilder$result.close();
                    } catch (Throwable throwable) {
                        throwable1.addSuppressed(throwable);
                    }
                }

                throw throwable1;
            }

            if (pResult != null) {
                pResult.close();
            }

            return;
        }

        if (pResult != null) {
            pResult.close();
        }
    }

    private VertexFormat uploadVertexBuffer(MeshData.DrawState pDrawState, @Nullable ByteBuffer pBuffer) {
        if (this.vboRegion != null) {
            return pDrawState.format();
        } else {
            boolean flag = false;
            if (!pDrawState.format().equals(this.format)) {
                if (this.format != null) {
                    this.format.clearBufferState();
                }

                this.vertexBuffer.bind();
                pDrawState.format().setupBufferState();
                if (Config.isShaders()) {
                    ShadersRender.setupArrayPointersVbo();
                }

                flag = true;
            }

            if (pBuffer != null) {
                if (!flag) {
                    this.vertexBuffer.bind();
                }

                this.vertexBuffer.resize(pBuffer.remaining());
                this.vertexBuffer.write(pBuffer, 0);
            }

            return pDrawState.format();
        }
    }

    @Nullable
    private RenderSystem.AutoStorageIndexBuffer uploadIndexBuffer(MeshData.DrawState pDrawState, @Nullable ByteBuffer pBuffer) {
        if (pBuffer != null) {
            if (this.vboRegion != null) {
                return null;
            } else {
                if (this.indexBuffer != null) {
                    this.indexBuffer.close();
                }

                this.indexBuffer = new GpuBuffer(BufferType.INDICES, this.usage, pBuffer);
                return null;
            }
        } else {
            RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(pDrawState.mode());
            int i = pDrawState.indexCount();
            if (this.vboRegion != null && pDrawState.mode() == VertexFormat.Mode.QUADS) {
                i = 65536;
            }

            if (rendersystem$autostorageindexbuffer != this.sequentialIndices || !rendersystem$autostorageindexbuffer.hasStorage(i)) {
                rendersystem$autostorageindexbuffer.bind(i);
            }

            return rendersystem$autostorageindexbuffer;
        }
    }

    public void bind() {
        BufferUploader.invalidate();
        if (this.arrayObjectId >= 0) {
            GlStateManager._glBindVertexArray(this.arrayObjectId);
        }
    }

    public static void unbind() {
        BufferUploader.invalidate();
        GlStateManager._glBindVertexArray(0);
    }

    public void draw() {
        if (this.vboRegion != null) {
            this.vboRegion.drawArrays(VertexFormat.Mode.QUADS, this.vboRange);
        } else if (this.multiTextureData != null) {
            MultiTextureRenderer.draw(this.mode, this.getIndexType().asGLType, this.multiTextureData);
        } else {
            RenderSystem.drawElements(this.mode.asGLMode, this.indexCount, this.getIndexType().asGLType);
        }
    }

    private VertexFormat.IndexType getIndexType() {
        RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = this.sequentialIndices;
        return rendersystem$autostorageindexbuffer != null ? rendersystem$autostorageindexbuffer.type() : this.indexType;
    }

    public void drawWithShader(Matrix4f pFrustumMatrix, Matrix4f pProjectionMatrix, @Nullable CompiledShaderProgram pShader) {
        if (pShader != null) {
            RenderSystem.assertOnRenderThread();
            pShader.setDefaultUniforms(this.mode, pFrustumMatrix, pProjectionMatrix, Minecraft.getInstance().getWindow());
            pShader.apply();
            boolean flag = Config.isShaders() && Shaders.isRenderingWorld;
            boolean flag1 = flag && SVertexBuilder.preDrawArrays(this.format);
            if (flag) {
                Shaders.setModelViewMatrix(pFrustumMatrix);
                Shaders.setProjectionMatrix(pProjectionMatrix);
                Shaders.setTextureMatrix(RenderSystem.getTextureMatrix());
                Shaders.setColorModulator(RenderSystem.getShaderColor());
            }

            this.draw();
            if (flag1) {
                SVertexBuilder.postDrawArrays();
            }

            pShader.clear();
        }
    }

    public void drawWithRenderType(RenderType pRenderType) {
        pRenderType.setupRenderState();
        this.bind();
        this.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        unbind();
        pRenderType.clearRenderState();
    }

    @Override
    public void close() {
        this.vertexBuffer.close();
        GpuMemory.bufferFreed((long)this.getVertexBufferSize());
        if (this.indexBuffer != null) {
            this.indexBuffer.close();
            this.indexBuffer = null;
            GpuMemory.bufferFreed((long)this.getIndexBufferSize());
        }

        if (this.arrayObjectId >= 0) {
            RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
            this.arrayObjectId = -1;
        }

        this.indexCount = 0;
    }

    public VertexFormat getFormat() {
        return this.format;
    }

    public boolean isInvalid() {
        return this.vboRegion != null ? false : this.arrayObjectId == -1;
    }

    public void setVboRegion(VboRegion vboRegion) {
        if (vboRegion != null) {
            this.close();
            this.vboRegion = vboRegion;
            this.vboRange = new VboRange();
        }
    }

    public VboRegion getVboRegion() {
        return this.vboRegion;
    }

    public boolean isEmpty() {
        return this.indexCount <= 0;
    }

    public void clearData() {
        if (this.indexCount > 0) {
            if (this.vboRegion != null) {
                this.vboRegion.bufferData(emptyBuffer, this.vboRange);
                this.indexCount = 0;
            } else {
                this.bind();
                if (!this.vertexBuffer.isClosed() && this.vertexBuffer.isInitialized()) {
                    this.vertexBuffer.resize(0);
                    GpuMemory.bufferFreed((long)this.getVertexBufferSize());
                }

                if (this.indexBuffer != null && !this.indexBuffer.isClosed() && this.indexBuffer.isInitialized() && this.sequentialIndices == null) {
                    this.indexBuffer.resize(0);
                    GpuMemory.bufferFreed((long)this.getIndexBufferSize());
                }

                unbind();
                this.indexCount = 0;
            }
        }
    }

    public int getIndexCount() {
        return this.indexCount;
    }

    private int getVertexBufferSize() {
        return this.format == null ? 0 : this.indexCount * this.format.getVertexSize();
    }

    private int getIndexBufferSize() {
        if (this.sequentialIndices != null) {
            return 0;
        } else {
            return this.indexType == null ? 0 : this.indexCount * this.indexType.bytes;
        }
    }

    public void updateMultiTextureData() {
        if (this.multiTextureData != null) {
            this.multiTextureData.applySort();
        }
    }
}