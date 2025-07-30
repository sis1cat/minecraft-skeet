package sisicat.main.utilities;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import sisicat.IDefault;
import sisicat.events.WindowResizeEvent;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.WGL.wglGetCurrentContext;

public class ItemsBuffer implements IDefault {

    public static HashMap<String, ItemTexture> texturesMap;
    private static final int ATLAS_SIZE = 1440;
    public static int texturesAtlas = 0;
    private static BufferedImage atlasImage;
    private static Graphics2D g2d;

    private float progress = 0;

    private int i = 0;
    private static Iterator<Item> iterator;

    public static AbstractTexture
            potionTexture,
            splashPotionTexture,
            lingeringPotionTexture,
            potionOverlayTexture;

    public static AbstractTexture
            enchantmentTexture;

    int framebuffer, fbDepthBuffer, fbTexture;

    public ItemsBuffer(){

        atlasImage = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);

        iterator = BuiltInRegistries.ITEM.iterator();
        texturesMap = new HashMap<>();

        g2d = atlasImage.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setColor(new java.awt.Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, ATLAS_SIZE, ATLAS_SIZE);

        int prevFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        framebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);

        fbTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, mc.getWindow().getWidth(), mc.getWindow().getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, fbTexture, 0);

        fbDepthBuffer = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, fbDepthBuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, fbDepthBuffer);

        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("=(");
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFramebuffer);

        EventManager.register(this);

    }

    @EventTarget
    void _event(WindowResizeEvent windowResizeEvent) {
        updateFrameBuffer();
    }

    private void updateFrameBuffer() {

        int prevFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, 0, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0);

        glDeleteTextures(fbTexture);
        glDeleteRenderbuffers(fbDepthBuffer);

        fbTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, fbTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, mc.getWindow().getWidth(), mc.getWindow().getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fbTexture, 0);

        fbDepthBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, fbDepthBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, fbDepthBuffer);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new RuntimeException("Bad framebuffer after size update");

        glBindFramebuffer(GL_FRAMEBUFFER, prevFramebuffer);

    }

    int
            x = 0,
            y = 0;

    private static final int newSize = 32;

    public void load(GuiGraphics guiGraphics) {

        guiGraphics.flush();

        int prevFramebuffer = glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        ArrayList<BatchedTexture> batched = new ArrayList<>();

        for(int x = 0; x < 5; x++)
            for(int y = 0; y < 2; y++)
                if(iterator.hasNext()) {

                    Item item = iterator.next();

                    int
                            screenX = x * 16,
                            screenY = y * 16;

                    guiGraphics.renderItem(new ItemStack(item), screenX, screenY);
                    guiGraphics.flush();
                    batched.add(new BatchedTexture(item, screenX * (int) mc.getWindow().getGuiScale(), screenY * (int) mc.getWindow().getGuiScale()));

                } else break;

        for(BatchedTexture batchedTexture : batched) {

            i += 1;

            progress = (float) i / (float) BuiltInRegistries.ITEM.size();

            BufferedImage bufferedItemTexture = getItemIcon(batchedTexture.x, batchedTexture.y);

            BufferedImage scaledItemTexture = new BufferedImage(newSize, newSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d2 = scaledItemTexture.createGraphics();

            g2d2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d2.drawImage(bufferedItemTexture, 0, 0, newSize, newSize, null);
            g2d2.dispose();

            bufferedItemTexture.flush();

            if (x + newSize > ATLAS_SIZE) {
                x = 0;
                y += newSize;
            }

            if (y + newSize > ATLAS_SIZE)
                System.out.println("=(.");

            g2d.drawImage(scaledItemTexture, x, y, null);
            scaledItemTexture.flush();

            texturesMap.put(batchedTexture.item.toString(), new ItemTexture(x, y, newSize, newSize));

            x += newSize;

        }

        glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFramebuffer);

        if(isDone()) {
            g2d.dispose();
            saveAtlas(atlasImage);
            destroy();
        }

    }

    private void destroy() {

        int prevFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, 0, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0);

        GL11.glDeleteTextures(fbTexture);
        GL30.glDeleteRenderbuffers(fbDepthBuffer);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFramebuffer);

        GL30.glDeleteFramebuffers(framebuffer);

        if(potionTexture != null) {
            potionTexture.close();
            splashPotionTexture.close();
            lingeringPotionTexture.close();
            potionOverlayTexture.close();
            enchantmentTexture.close();
        }

        potionTexture = mc.getTextureManager().getTexture(
                new ResourceLocation("minecraft", "textures/item/potion.png")
        );
        splashPotionTexture = mc.getTextureManager().getTexture(
                new ResourceLocation("minecraft", "textures/item/splash_potion.png")
        );
        lingeringPotionTexture = mc.getTextureManager().getTexture(
                new ResourceLocation("minecraft", "textures/item/lingering_potion.png")
        );
        potionOverlayTexture = mc.getTextureManager().getTexture(
                new ResourceLocation("minecraft", "textures/item/potion_overlay.png")
        );
        enchantmentTexture = mc.getTextureManager().getTexture(
                new ResourceLocation("minecraft", "textures/misc/enchanted_glint_item.png")
        );

        EventManager.unregister(this);

    }

    private NativeImage bufferedImageToNativeImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bufferedImage.getRGB(x, y);
                nativeImage.setPixel(x, y, pixel);
            }
        }

        return nativeImage;
    }

    private void saveAtlas(final BufferedImage atlasImage) {

        if(texturesAtlas > 0)
            glDeleteTextures(texturesAtlas);

        texturesAtlas = registerTexture(atlasImage);

        atlasImage.flush();

    }

    public boolean isDone() {
        return progress >= 1;
    }

    public float getProgress() {
        return progress;
    }

    public static BufferedImage getItemIcon(int x, int y) {

        int textureSize = (int) (16 * Minecraft.getInstance().getWindow().getGuiScale());
        ByteBuffer data = BufferUtils.createByteBuffer(textureSize * textureSize * 4);

        GL11.glReadPixels(
                x,
                mc.getWindow().getHeight() - y - textureSize,
                textureSize,
                textureSize,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                data
        );

        data.rewind();

        BufferedImage image = new BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB);

        for (int y1 = 0; y1 < textureSize; y1++) {
            for (int x1 = 0; x1 < textureSize; x1++) {

                int i = (x1 + (textureSize - y1 - 1) * textureSize) * 4;
                int r = data.get(i)     & 0xFF;
                int g = data.get(i + 1) & 0xFF;
                int b = data.get(i + 2) & 0xFF;
                int a = data.get(i + 3) & 0xFF;

                int argb = (a << 24) | (r << 16) | (g << 8) | b;

                image.setRGB(x1, y1, argb);

            }
        }

        return image;

    }

    public static void saveByteBufferToPNG(ByteBuffer buffer, int width, int height, File file) throws IOException {

        ByteBuffer copy = buffer.duplicate();
        copy.rewind();

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = (x + y * width) * 4;
                int r = copy.get(i) & 0xFF;
                int g = copy.get(i + 1) & 0xFF;
                int b = copy.get(i + 2) & 0xFF;
                int a = copy.get(i + 3) & 0xFF;

                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                img.setRGB(x, y, argb);
            }
        }

        ImageIO.write(img, "PNG", file);
    }

    private int registerTexture(BufferedImage bufferedImage) {

        /*int textureId = glGenTextures();
        int prevTexture = GL11.glGetInteger(GL_TEXTURE_BINDING_2D);

        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        try {
            saveByteBufferToPNG(byteBuffer, width, height, new File("png.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RGBA8,
                width,
                height,
                0,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                byteBuffer
        );

        glBindTexture(GL_TEXTURE_2D, prevTexture);*/

        return new DynamicTexture(bufferedImageToNativeImage(bufferedImage)).getId();

    }

    public record ItemTexture(int x, int y, int width, int height) {
    }

    private record BatchedTexture(Item item, int x, int y) {
    }

}
