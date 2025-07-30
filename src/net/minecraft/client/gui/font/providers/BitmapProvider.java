package net.minecraft.client.gui.font.providers;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.optifine.util.FontUtils;
import org.slf4j.Logger;

public class BitmapProvider implements GlyphProvider {
    static final Logger LOGGER = LogUtils.getLogger();
    private final NativeImage image;
    private final CodepointMap<BitmapProvider.Glyph> glyphs;

    BitmapProvider(NativeImage pImage, CodepointMap<BitmapProvider.Glyph> pGlyphs) {
        this.image = pImage;
        this.glyphs = pGlyphs;
    }

    @Override
    public void close() {
        this.image.close();
    }

    @Nullable
    @Override
    public GlyphInfo getGlyph(int p_232638_) {
        return this.glyphs.get(p_232638_);
    }

    @Override
    public IntSet getSupportedGlyphs() {
        return IntSets.unmodifiable(this.glyphs.keySet());
    }

    public static record Definition(ResourceLocation file, int height, int ascent, int[][] codepointGrid) implements GlyphProviderDefinition {
        private static final Codec<int[][]> CODEPOINT_GRID_CODEC = Codec.STRING.listOf().xmap(listIn -> {
            int i = listIn.size();
            int[][] aint = new int[i][];

            for (int j = 0; j < i; j++) {
                aint[j] = listIn.get(j).codePoints().toArray();
            }

            return aint;
        }, intsIn -> {
            List<String> list = new ArrayList<>(intsIn.length);

            for (int[] aint : intsIn) {
                list.add(new String(aint, 0, aint.length));
            }

            return list;
        }).validate(BitmapProvider.Definition::validateDimensions);
        public static final MapCodec<BitmapProvider.Definition> CODEC = RecordCodecBuilder.<BitmapProvider.Definition>mapCodec(
                defIn -> defIn.group(
                            ResourceLocation.CODEC.fieldOf("file").forGetter(BitmapProvider.Definition::file),
                            Codec.INT.optionalFieldOf("height", Integer.valueOf(8)).forGetter(BitmapProvider.Definition::height),
                            Codec.INT.fieldOf("ascent").forGetter(BitmapProvider.Definition::ascent),
                            CODEPOINT_GRID_CODEC.fieldOf("chars").forGetter(BitmapProvider.Definition::codepointGrid)
                        )
                        .apply(defIn, BitmapProvider.Definition::new)
            )
            .validate(BitmapProvider.Definition::validate);

        public Definition(ResourceLocation file, int height, int ascent, int[][] codepointGrid) {
            file = FontUtils.getHdFontLocation(file);
            this.file = file;
            this.height = height;
            this.ascent = ascent;
            this.codepointGrid = codepointGrid;
        }

        private static DataResult<int[][]> validateDimensions(int[][] pDimensions) {
            int i = pDimensions.length;
            if (i == 0) {
                return DataResult.error(() -> "Expected to find data in codepoint grid");
            } else {
                int[] aint = pDimensions[0];
                int j = aint.length;
                if (j == 0) {
                    return DataResult.error(() -> "Expected to find data in codepoint grid");
                } else {
                    for (int k = 1; k < i; k++) {
                        int[] aint1 = pDimensions[k];
                        if (aint1.length != j) {
                            return DataResult.error(
                                () -> "Lines in codepoint grid have to be the same length (found: "
                                        + aint1.length
                                        + " codepoints, expected: "
                                        + j
                                        + "), pad with \\u0000"
                            );
                        }
                    }

                    return DataResult.success(pDimensions);
                }
            }
        }

        private static DataResult<BitmapProvider.Definition> validate(BitmapProvider.Definition pDefinition) {
            return pDefinition.ascent > pDefinition.height
                ? DataResult.error(() -> "Ascent " + pDefinition.ascent + " higher than height " + pDefinition.height)
                : DataResult.success(pDefinition);
        }

        @Override
        public GlyphProviderType type() {
            return GlyphProviderType.BITMAP;
        }

        @Override
        public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
            return Either.left(this::load);
        }

        private GlyphProvider load(ResourceManager pResourceManager) throws IOException {
            ResourceLocation resourcelocation = this.file.withPrefix("textures/");

            BitmapProvider bitmapprovider;
            try (InputStream inputstream = pResourceManager.open(resourcelocation)) {
                NativeImage nativeimage = NativeImage.read(NativeImage.Format.RGBA, inputstream);
                int i = nativeimage.getWidth();
                int j = nativeimage.getHeight();
                int k = i / this.codepointGrid[0].length;
                int l = j / this.codepointGrid.length;
                float f = (float)this.height / (float)l;
                CodepointMap<BitmapProvider.Glyph> codepointmap = new CodepointMap<>(BitmapProvider.Glyph[]::new, BitmapProvider.Glyph[][]::new);

                for (int i1 = 0; i1 < this.codepointGrid.length; i1++) {
                    int j1 = 0;

                    for (int k1 : this.codepointGrid[i1]) {
                        int l1 = j1++;
                        if (k1 != 0) {
                            int i2 = this.getActualGlyphWidth(nativeimage, k, l, l1, i1);
                            BitmapProvider.Glyph bitmapprovider$glyph = codepointmap.put(
                                k1, new BitmapProvider.Glyph(f, nativeimage, l1 * k, i1 * l, k, l, (int)(0.5 + (double)((float)i2 * f)) + 1, this.ascent)
                            );
                            if (bitmapprovider$glyph != null) {
                                BitmapProvider.LOGGER.warn("Codepoint '{}' declared multiple times in {}", Integer.toHexString(k1), resourcelocation);
                            }
                        }
                    }
                }

                bitmapprovider = new BitmapProvider(nativeimage, codepointmap);
            }

            return bitmapprovider;
        }

        private int getActualGlyphWidth(NativeImage pImage, int pWidth, int pHeight, int pX, int pY) {
            int i;
            for (i = pWidth - 1; i >= 0; i--) {
                int j = pX * pWidth + i;

                for (int k = 0; k < pHeight; k++) {
                    int l = pY * pHeight + k;
                    if (pImage.getLuminanceOrAlpha(j, l) != 0) {
                        return i + 1;
                    }
                }
            }

            return i + 1;
        }
    }

    static record Glyph(float scale, NativeImage image, int offsetX, int offsetY, int width, int height, int advance, int ascent)
        implements GlyphInfo {
        @Override
        public float getAdvance() {
            return (float)this.advance;
        }

        @Override
        public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> p_232640_) {
            return p_232640_.apply(
                new SheetGlyphInfo() {
                    @Override
                    public float getOversample() {
                        return 1.0F / Glyph.this.scale;
                    }

                    @Override
                    public int getPixelWidth() {
                        return Glyph.this.width;
                    }

                    @Override
                    public int getPixelHeight() {
                        return Glyph.this.height;
                    }

                    @Override
                    public float getBearingTop() {
                        return (float)Glyph.this.ascent;
                    }

                    @Override
                    public void upload(int p_232658_, int p_232659_) {
                        Glyph.this.image
                            .upload(0, p_232658_, p_232659_, Glyph.this.offsetX, Glyph.this.offsetY, Glyph.this.width, Glyph.this.height, false);
                    }

                    @Override
                    public boolean isColored() {
                        return Glyph.this.image.format().components() > 1;
                    }
                }
            );
        }
    }
}