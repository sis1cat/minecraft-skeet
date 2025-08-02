package sisicat.main.utilities;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.mlomb.freetypejni.*;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

import org.lwjgl.BufferUtils;

import sisicat.IDefault;
import sisicat.main.gui.elements.Window;

import javax.imageio.ImageIO;


public class Font implements IDefault, AutoCloseable {

    private final Library library;
    public Face face;

    private BufferedImage atlasImage;
    public DynamicTexture loadedTexture;

    private final boolean antiAliasing;

    private int baseAscender;
    private final int maxGlyphHeight;
    public int glyphRenderOffset;

    public Map<Character, int[]> charactersMap = new HashMap<>();

    public Font(InputStream fontStream, float fontSize, boolean antiAliasing) throws Exception {

        this.antiAliasing = antiAliasing;

        this.library = FreeType.newLibrary();

        if (this.library == null) throw new RuntimeException("ft failed to init");

        byte[] fontBytes = fontStream.readAllBytes();

        ByteBuffer fontBuffer = BufferUtils.createByteBuffer(fontBytes.length);
        fontBuffer.put(fontBytes).flip();

        this.face = library.newFace(fontBuffer, 0);

        if (this.face == null) {
            library.delete();
            throw new RuntimeException("face failed to init");
        }

        face.selectCharmap(1970170211);
        face.setPixelSizes(0, (int) fontSize);

        this.maxGlyphHeight = (this.face.getSize().getMetrics().getAscender() - this.face.getSize().getMetrics().getDescender()) >> 6;

        createFontAtlas();

    }

    private void createFontAtlas() {

        atlasImage = new BufferedImage(2048, 2048, BufferedImage.TYPE_INT_ARGB);

        generateChars(32, 126);
        generateChars(1024, 1279);
        generateChars(176, 176);

        loadTexture();

        this.glyphRenderOffset = this.maxGlyphHeight - ((this.face.getSize().getMetrics().getAscender() >> 6) - this.baseAscender);

    }

    private int
            atlasX = 0,
            atlasY = 0;

    private void generateChars(int from, int to) {

        for (char c = (char) from; c <= (char) to; c++) {

            int flags = antiAliasing ? 0 : 65536;

            if (face.loadChar(c, flags | 4)) {
                System.err.println("glyph nf: " + c);
                continue;
            }

            GlyphSlot glyph = face.getGlyphSlot();
            Bitmap bitmap = glyph.getBitmap();

            if(c == 'A')
                this.baseAscender = glyph.getBitmap().getRows();

            int charWidth = glyph.getAdvance().getX() >> 6;
            int charHeight = this.maxGlyphHeight;

            if (atlasX + charWidth >= atlasImage.getWidth()) {
                atlasX = 0;
                atlasY += charHeight;
            }

            if (atlasY + charHeight >= atlasImage.getHeight()) {
                System.err.println("atlas overflow");
                break;
            }

            drawGlyphToAtlas(bitmap, glyph, atlasX, atlasY);

            charactersMap.put(c, new int[]{atlasX, atlasY, charWidth, charHeight});

            atlasX += charWidth + 1;

        }

    }

    private void drawGlyphToAtlas(Bitmap bitmap, GlyphSlot glyph, int atlasX, int atlasY) {

        ByteBuffer buffer = bitmap.getBuffer();

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getRows();

        int drawY = atlasY + (face.getSize().getMetrics().getAscender() >> 6) - glyph.getBitmapTop();
        int drawX = atlasX + glyph.getBitmapLeft();

        for (int j = 0; j < bitmapHeight; j++) {

            for (int i = 0; i < bitmapWidth; i++) {

                int alpha = buffer.get(j * bitmap.getPitch() + i) & 0xFF;

                if (alpha > 0) {

                    int color = (alpha << 24) | 0x00FFFFFF;

                    if (drawX + i >= 0 && drawX + i < atlasImage.getWidth() && drawY + j >= 0 && drawY + j < atlasImage.getHeight())
                        atlasImage.setRGB(drawX + i, drawY + j, color);

                }

            }

        }

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

    public int getStringWidth(String text) {

        int width = 0;
        String cleanText = removeParagraphPairs(text);

        for (char c : cleanText.toCharArray())
            if(charactersMap.containsKey(c))
                width += charactersMap.get(c)[2];
            else width += charactersMap.get('?')[2];

        return width;

    }

    public int getBaseAscender() {
        return this.baseAscender;
    }

    public void renderText(String text, float x, float y, float[] color) {

        y = Window.gameWindowHeight - y - this.glyphRenderOffset;

        float currentX = x;

        for (char c : text.toCharArray()) {

            if (c == '\u00A7') continue;

            int[] charData = charactersMap.get(c);

            if (charData == null) {
                charData = charactersMap.get('?');
                if (charData == null) continue;
            }

            int charWidth = charData[2];

            Render.drawCharacter(c, currentX, y, color, 255, this);

            currentX += charWidth;

        }

    }

    public void renderHVCenteredCharacter(String text, float x, float y, float[] color) {
        if (text.isEmpty()) return;
        renderText(text, x - (float) getStringWidth(String.valueOf(text.charAt(0))) / 2, y - (float) getBaseAscender() / 2, color);
    }

    public void renderVCenteredText(String text, float x, float y, float[] color) {
        renderText(text, x, y - (float) this.getBaseAscender() / 2, color);
    }

    public void renderHVCenteredText(String text, float x, float y, float[] color) {
        renderText(text, x - (float) getStringWidth(text) / 2, y - (float) getBaseAscender() / 2, color);
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

    public void renderOutlinedText(String text, float x, float y, float[] textColor, float[] outlineColor, float alpha) {
        if(textColor.length == 4) textColor = new float[]{textColor[0], textColor[1], textColor[2], textColor[3] / 255 * (alpha / 255) * 255}; else textColor = new float[]{textColor[0], textColor[1], textColor[2], alpha};
        if(outlineColor.length == 4) outlineColor = new float[]{outlineColor[0], outlineColor[1], outlineColor[2], outlineColor[3] / 255 * (alpha / 255) * 255}; else outlineColor = new float[]{outlineColor[0], outlineColor[1], outlineColor[2], alpha};
        this.renderText(text, x - 1, y, outlineColor); this.renderText(text, x + 1, y, outlineColor); this.renderText(text, x, y + 1, outlineColor); this.renderText(text, x, y - 1, outlineColor);
        this.renderText(text, x + 1, y + 1, outlineColor); this.renderText(text, x - 1, y - 1, outlineColor); this.renderText(text, x + 1, y - 1, outlineColor); this.renderText(text, x - 1, y + 1, outlineColor);
        this.renderText(text, x, y, textColor);
    }

    public void renderTextWithShadow(String text, float x, float y, float[] color) {
        renderText(text, x + 1, y + 1, sisicat.main.utilities.Color.c12);
        renderText(text, x, y, color);
    }

    @Override
    public void close() {

        if (library != null) library.delete();
        if (face != null) face.delete();

    }

}