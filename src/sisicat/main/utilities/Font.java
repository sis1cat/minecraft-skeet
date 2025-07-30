package sisicat.main.utilities;

import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.phys.Vec2;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import sisicat.IDefault;
import sisicat.main.gui.elements.Window;
import sisicat.main.gui.elements.widgets.Widget;

import javax.imageio.ImageIO;

import static com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class Font implements IDefault {

    private final java.awt.Font font;
    private BufferedImage atlasImage;
    public DynamicTexture loadedTexture;
    private final boolean antiAliasing;
    public FontMetrics fontMetrics;

    private float size;

    private final int
            spacing;
    public final int yOffset;

    public Font(InputStream fontStream, float fontSize, boolean antiAliasing, boolean bold, int spacing, int yOffset) throws Exception {

        this.antiAliasing = antiAliasing;
        this.spacing = spacing;
        this.yOffset = yOffset;
        this.size = fontSize;

        font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fontStream).deriveFont(bold ? 1 : 0, fontSize);
        createFontAtlas();

    }

    public Map<Character, int[]> charactersMap = new HashMap<>();

    private void createFontAtlas() {

        atlasImage = new BufferedImage(2048,  2048, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = atlasImage.createGraphics();

        if(antiAliasing) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        g.setFont(font);
        g.setColor(Color.WHITE);

        fontMetrics = g.getFontMetrics();

        int x = 0, y = 0;

        for (char c = 32; c <= 126; c++) {

            int charWidth = fontMetrics.charWidth(c) + spacing;
            int charHeight = fontMetrics.getHeight();

            if (x + charWidth >= 2048) {
                x = 0;
                y += charHeight + 5;
            }

            g.drawString(String.valueOf(c), x, y + fontMetrics.getAscent());

            charactersMap.put(c, new int[]{x, y, charWidth, charHeight});

            x += charWidth + 2;

        }

        for (char c = 1024; c <= 1279; c++) {

            int charWidth = fontMetrics.charWidth(c) + spacing;
            int charHeight = fontMetrics.getHeight();

            if (x + charWidth >= 2048) {
                x = 0;
                y += charHeight + 5;
            }

            g.drawString(String.valueOf(c), x, y + fontMetrics.getAscent());

            charactersMap.put(c, new int[]{x, y, charWidth, charHeight});

            x += charWidth + 2;

        }

        char c = 176;
        int charWidth = fontMetrics.charWidth(c) + spacing;
        int charHeight = fontMetrics.getHeight();

        if (x + charWidth >= 2048) {
            x = 0;
            y += charHeight + 5;
        }

        g.drawString(String.valueOf(c), x, y + fontMetrics.getAscent());

        charactersMap.put(c, new int[]{x, y, charWidth, charHeight});

        x += charWidth + 2;

        g.dispose();
        if(this.size == 9 || this.size == 11)
            increaseAlpha();
        loadTexture();

    }

    public void loadTexture() {

        try {

            ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
            ImageIO.write(atlasImage, "png", BAOS);

            byte[] bytes = BAOS.toByteArray();

            ByteBuffer data = BufferUtils.createByteBuffer(bytes.length).put(bytes);
            data.flip();

            loadedTexture = new DynamicTexture(NativeImage.read(data));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void increaseAlpha() {
        for (int x = 0; x < atlasImage.getWidth(); x++) {
            for (int y = 0; y < atlasImage.getHeight(); y++) {
                int argb = atlasImage.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                int red   = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue  = argb & 0xFF;

                alpha = Math.min(255, (int) (alpha * 1.5));

                atlasImage.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
    }

    public void renderHVCenteredCharacter(String text, float x, float y, float[] color){
        renderText(text, x - (float) fontMetrics.charWidth(text.charAt(0)) / 2, y - (int)((float) fontMetrics.getHeight() / 2), color);
    }

    public void renderVCenteredText(String text, float x, float y, float[] color){
        renderText(text, x, y - (int)((float) getFontHeight() / 2), color);
    }

    public void renderHVCenteredText(String text, float x, float y, float[] color){
        renderText(text, x - (int)((float) getStringWidth(text) / 2), y - (int)((float) getFontHeight() / 2), color);
    }

    public int getStringWidth(String text){

        return fontMetrics.stringWidth(removeParagraphPairs(text));

    }

    public static String removeParagraphPairs(String input) {

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '\u00A7' && i + 1 < input.length()) {
                i++;
            } else {
                result.append(input.charAt(i));
            }
        }

        return result.toString();
    }

    public int getFontHeight() {
        return fontMetrics.getAscent() + yOffset;
    }

    public void renderOutlinedText(String text, float x, float y, float[] textColor, float[] outlineColor, float alpha){

        if(textColor.length == 4)
            textColor = new float[]{textColor[0], textColor[1], textColor[2], textColor[3] / 255 * (alpha / 255) * 255};
        else textColor = new float[]{textColor[0], textColor[1], textColor[2], alpha};

        if(outlineColor.length == 4)
            outlineColor = new float[]{outlineColor[0], outlineColor[1], outlineColor[2], outlineColor[3] / 255 * (alpha / 255) * 255};
        else outlineColor = new float[]{outlineColor[0], outlineColor[1], outlineColor[2], alpha};
        
        this.renderText(text, x - 1, y, outlineColor);
        this.renderText(text, x + 1, y, outlineColor);

        this.renderText(text, x, y + 1, outlineColor);
        this.renderText(text, x, y - 1, outlineColor);

        this.renderText(text, x + 1, y + 1, outlineColor);
        this.renderText(text, x - 1, y - 1, outlineColor);

        this.renderText(text, x + 1, y - 1, outlineColor);
        this.renderText(text, x - 1, y + 1, outlineColor);

        this.renderText(text, x, y, textColor);

    }

    public void renderTextWithShadow(String text, float x, float y, float[] color){

        renderText(text, x + 1, y + 1, sisicat.main.utilities.Color.c12);
        renderText(text, x , y , color);

    }

    public void renderText(String text, float x, float y, float[] color) {

        y = Window.gameWindowHeight - y - fontMetrics.getHeight() - yOffset;

        for (char c : text.toCharArray()) {

            int[] charData = charactersMap.get(c);
            if(charData == null)
                continue;

            int charX = charData[0];
            int charY = charData[1];
            int charWidth = charData[2];

            Render.drawCharacter(c, x, y, color, 255, this);

            x += charWidth;

        }

    }

}