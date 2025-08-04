package sisicat.main.utilities;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.blaze3d.platform.NativeImage;
import generaloss.freetype.FTLibrary;
import generaloss.freetype.bitmap.FTBitmap;
import generaloss.freetype.face.FTFace;
import generaloss.freetype.face.FTLoad;
import generaloss.freetype.face.FTLoadFlags;
import generaloss.freetype.glyph.FTGlyphSlot;
import net.minecraft.client.renderer.texture.DynamicTexture;

import org.lwjgl.BufferUtils;

import sisicat.IDefault;
import sisicat.main.gui.elements.Window;

import javax.imageio.ImageIO;


public class Font implements IDefault, AutoCloseable {

    private final FTLibrary library;
    public FTFace face;

    private BufferedImage atlasImage;
    public DynamicTexture loadedTexture;

    private int baseAscender;
    private final int maxGlyphHeight;
    public int glyphRenderOffset;

    public Map<Integer, int[]> charactersMap = new HashMap<>();

    public Font(InputStream fontStream, float fontSize) throws Exception {

        this.library = FTLibrary.init();

        if (this.library == null) throw new RuntimeException("ft failed to init");

        byte[] fontBytes = fontStream.readAllBytes();

        ByteBuffer fontBuffer = BufferUtils.createByteBuffer(fontBytes.length);
        fontBuffer.put(fontBytes).flip();

        this.face = library.newMemoryFace(fontBuffer, 0);

        if (this.face == null) {
            library.done();
            throw new RuntimeException("face failed to init");
        }

        //face.(FTEncoding.UNICODE);
        face.setPixelSizes(0, (int) fontSize);

        this.maxGlyphHeight = (this.face.getSize().getMetrics().getAscender() - this.face.getSize().getMetrics().getDescender());

        createFontAtlas();

    }

    private void createFontAtlas() {

        atlasImage = new BufferedImage(2048, 2048, BufferedImage.TYPE_INT_ARGB);

        generateChars(32, 126);
        generateChars(176, 176);
        generateChars(1024, 1279);

        generateChars(592, 687);
        generateChars(7424, 7551);
        generateChars(7468, 7500);

        generateChars(9728, 9983);
        generateChars(126976, 127231);
        generateChars(127232, 127487);
        generateChars(127488, 127743);
        generateChars(127744, 128511);
        generateChars(128512, 128591);
        generateChars(128640, 128767);
        generateChars(128768, 128895);
        generateChars(129280, 129535);
        generateChars(129536, 129647);

        File outputFile = new File("output.png");

        try {
            ImageIO.write(atlasImage, "png", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        loadTexture();

        this.glyphRenderOffset = this.maxGlyphHeight - ((this.face.getSize().getMetrics().getAscender()) - this.baseAscender);

    }

    private int
            atlasX = 0,
            atlasY = 0;

    private void generateChars(int from, int to) {

        for (int c = from; c <= to; c++) {

            if(face.getCharIndex(c) == 0)
                continue;

            FTLoadFlags ftLoadFlags = new FTLoadFlags();
            ftLoadFlags.set(FTLoad.RENDER);

            face.loadChar(c, ftLoadFlags);

            FTGlyphSlot glyph = face.getGlyph();
            FTBitmap bitmap = glyph.getBitmap();

            if(c == 'A')
                this.baseAscender = glyph.getBitmap().getRows();

            int charWidth = glyph.getAdvanceX() >> 6;
            int charHeight = this.maxGlyphHeight;

            if (atlasX + charWidth >= atlasImage.getWidth()) {
                atlasX = 0;
                atlasY += charHeight + 4;
            }

            if (atlasY + charHeight >= atlasImage.getHeight()) {
                System.err.println("atlas overflow");
                break;
            }

            drawGlyphToAtlas(bitmap, glyph, atlasX, atlasY);

            charactersMap.put(c, new int[]{atlasX, atlasY, charWidth, charHeight});

            atlasX += charWidth + 4;

        }

    }

    private void drawGlyphToAtlas(FTBitmap bitmap, FTGlyphSlot glyph, int atlasX, int atlasY) {

        ByteBuffer buffer = bitmap.getBuffer();

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getRows();

        int drawY = atlasY + (face.getSize().getMetrics().getAscender()) - glyph.getBitmapTop();
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

        AtomicInteger width = new AtomicInteger();

        text = removeParagraphPairs(text);

        text.codePoints().forEach(c -> {
            if(charactersMap.containsKey(c))
                width.addAndGet(charactersMap.get(c)[2]);
            else width.addAndGet(charactersMap.get((int) '?')[2]);
        });

        return width.get();

    }

    public int getBaseAscender() {
        return this.baseAscender;
    }

    public void renderText(String text, float x, float y, float[] color) {

        y = Window.gameWindowHeight - y - this.glyphRenderOffset;

        float currentX = x;

        for (char c : text.toCharArray()) {

            if (c == '\u00A7') continue;

            int[] charData = charactersMap.get((int)c);

            if (charData == null) {
                charData = charactersMap.get((int)'?');
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

        if (library != null) library.done();
        if (face != null) face.done();

    }

}