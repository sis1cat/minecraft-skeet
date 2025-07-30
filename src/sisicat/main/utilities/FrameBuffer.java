package sisicat.main.utilities;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import org.lwjgl.opengl.GL11;
import sisicat.IDefault;
import sisicat.events.WindowResizeEvent;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

public class FrameBuffer implements IDefault {

    public int
            frameBufferId,
            textureId;

    public FrameBuffer() {

        setupFrameBuffer();

        EventManager.register(this);

    }

    private void setupFrameBuffer() {

        frameBufferId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId);

        textureId = glGenTextures();
        int prevTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, mc.getWindow().getWidth(), mc.getWindow().getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new RuntimeException("Bad framebuffer");

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        glBindTexture(GL_TEXTURE_2D, prevTexture);

    }

    public void updateFrameBuffer() {
        glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId);

        glDeleteTextures(textureId);

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        int prevTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, mc.getWindow().getWidth(), mc.getWindow().getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, textureId, 0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Bad framebuffer after size update");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, mc.getMainRenderTarget().frameBufferId);
        glBindTexture(GL_TEXTURE_2D, prevTexture);

    }

    public void bind() {

        glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId);

    }

    public void unbind() {

        glBindFramebuffer(GL_FRAMEBUFFER, mc.getMainRenderTarget().frameBufferId);

    }

    @EventTarget
    void _event(WindowResizeEvent windowResizeEvent) {
        this.updateFrameBuffer();
    }

}
