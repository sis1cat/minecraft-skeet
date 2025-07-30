package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.optifine.Config;
import net.optifine.render.GlBlendState;
import net.optifine.shaders.config.ShaderPackParser;
import net.optifine.util.PropertiesOrdered;
import sisicat.main.utilities.ItemsBuffer;

public class LoadingOverlay extends Overlay {
    public static final ResourceLocation MOJANG_STUDIOS_LOGO_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/title/mojangstudios.png");
    private static final int LOGO_BACKGROUND_COLOR = ARGB.color(255, 239, 50, 61);
    private static final int LOGO_BACKGROUND_COLOR_DARK = ARGB.color(255, 0, 0, 0);
    private static final IntSupplier BRAND_BACKGROUND = () -> Minecraft.getInstance().options.darkMojangStudiosBackground().get() ? LOGO_BACKGROUND_COLOR_DARK : LOGO_BACKGROUND_COLOR;
    private static final int LOGO_SCALE = 240;
    private static final float LOGO_QUARTER_FLOAT = 60.0F;
    private static final int LOGO_QUARTER = 60;
    private static final int LOGO_HALF = 120;
    private static final float LOGO_OVERLAP = 0.0625F;
    private static final float SMOOTHING = 0.95F;
    public static final long FADE_OUT_TIME = 1000L;
    public static final long FADE_IN_TIME = 500L;
    private final Minecraft minecraft;
    private final ReloadInstance reload;
    private final Consumer<Optional<Throwable>> onFinish;
    private final boolean fadeIn;
    private float currentProgress;
    private long fadeOutStart = -1L;
    private long fadeInStart = -1L;
    private int colorBackground = BRAND_BACKGROUND.getAsInt();
    private int colorBar = BRAND_BACKGROUND.getAsInt();
    private int colorOutline = 16777215;
    private int colorProgress = 16777215;
    private GlBlendState blendState = null;
    private boolean fadeOut = false;
    private final ItemsBuffer itemsBuffer;

    public LoadingOverlay(Minecraft pMinecraft, ReloadInstance pReload, Consumer<Optional<Throwable>> pOnFinish, boolean pFadeIn) {
        this.minecraft = pMinecraft;
        this.reload = pReload;
        this.onFinish = pOnFinish;
        this.fadeIn = false;
        this.itemsBuffer = new ItemsBuffer();
    }

    public static void registerTextures(TextureManager pTextureManager) {
        pTextureManager.registerAndLoad(MOJANG_STUDIOS_LOGO_LOCATION, new LoadingOverlay.LogoTexture());
    }

    private static int replaceAlpha(int pColor, int pAlpha) {
        return pColor & 16777215 | pAlpha << 24;
    }

    @Override
    public void render(GuiGraphics p_281839_, int p_282704_, int p_283650_, float p_283394_) {
        int i = p_281839_.guiWidth();
        int j = p_281839_.guiHeight();
        long k = Util.getMillis();
        if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = k;
        }

        float f = this.fadeOutStart > -1L ? (float)(k - this.fadeOutStart) / 1000.0F : -1.0F;
        float f1 = this.fadeInStart > -1L ? (float)(k - this.fadeInStart) / 500.0F : -1.0F;
        float f2;
        if (f >= 1.0F) {
            this.fadeOut = true;
            if (this.minecraft.screen != null) {
                this.minecraft.screen.render(p_281839_, 0, 0, p_283394_);
            }

            int l = Mth.ceil((1.0F - Mth.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
            p_281839_.fill(RenderType.guiOverlay(), 0, 0, i, j, replaceAlpha(this.colorBackground, l));
            f2 = 1.0F - Mth.clamp(f - 1.0F, 0.0F, 1.0F);
        } else if (this.fadeIn) {
            if (this.minecraft.screen != null && f1 < 1.0F) {
                this.minecraft.screen.render(p_281839_, p_282704_, p_283650_, p_283394_);
            }

            int i2 = Mth.ceil(Mth.clamp((double)f1, 0.15, 1.0) * 255.0);
            p_281839_.fill(RenderType.guiOverlay(), 0, 0, i, j, replaceAlpha(this.colorBackground, i2));
            f2 = Mth.clamp(f1, 0.0F, 1.0F);
        } else {
            int j2 = this.colorBackground;
            float f3 = (float)(j2 >> 16 & 0xFF) / 255.0F;
            float f4 = (float)(j2 >> 8 & 0xFF) / 255.0F;
            float f5 = (float)(j2 & 0xFF) / 255.0F;
            GlStateManager._clearColor(f3, f4, f5, 1.0F);
            GlStateManager._clear(16384);
            f2 = 1.0F;
        }
        boolean done = this.reload.isDone() && this.itemsBuffer.isDone();
        if (this.renderContents(p_281839_, f2)) {
            int k2 = (int)((double)p_281839_.guiWidth() * 0.5);
            int l2 = (int)((double)p_281839_.guiHeight() * 0.5);
            double d1 = Math.min((double)p_281839_.guiWidth() * 0.75, (double)p_281839_.guiHeight()) * 0.25;
            int i1 = (int)(d1 * 0.5);
            double d0 = d1 * 4.0;
            int j1 = (int)(d0 * 0.5);
            int k1 = ARGB.white(f2);
            boolean flag = true;
            if (this.blendState != null) {
                this.blendState.apply();
                if (!this.blendState.isEnabled() && this.fadeOut) {
                    flag = false;
                }
            }

            if (flag) {
                p_281839_.blit(locIn -> RenderType.mojangLogo(), MOJANG_STUDIOS_LOGO_LOCATION, k2 - j1, l2 - i1, -0.0625F, 0.0F, j1, (int)d1, 120, 60, 120, 120, k1);
                p_281839_.blit(locIn -> RenderType.mojangLogo(), MOJANG_STUDIOS_LOGO_LOCATION, k2, l2 - i1, 0.0625F, 60.0F, j1, (int)d1, 120, 60, 120, 120, k1);
            }

            int l1 = (int)((double)p_281839_.guiHeight() * 0.8325);
            float f6 = this.reload.getActualProgress();

            this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + f6 * 0.050000012F - 0.015f + itemsBuffer.getProgress() * 0.015f, 0.0F, 1.0F);

            if (f < 1.0F) {
                this.drawProgressBar(p_281839_, i / 2 - j1, l1 - 5, i / 2 + j1, l1 + 5, 1.0F - Mth.clamp(f, 0.0F, 1.0F));
            }

            if(this.reload.getActualProgress() == 1 && !itemsBuffer.isDone())
                itemsBuffer.load(p_281839_);

        }

        if (f >= 2.0F) {
            this.minecraft.setOverlay(null);
        }

        if (this.fadeOutStart == -1L && done && (!this.fadeIn || f1 >= 2.0F)) {
            this.fadeOutStart = Util.getMillis();

            try {
                this.reload.checkExceptions();
                this.onFinish.accept(Optional.empty());
            } catch (Throwable throwable1) {
                this.onFinish.accept(Optional.of(throwable1));
            }

            if (this.minecraft.screen != null) {
                this.minecraft.screen.init(this.minecraft, p_281839_.guiWidth(), p_281839_.guiHeight());
            }
        }
    }

    private void drawProgressBar(GuiGraphics pGuiGraphics, int pMinX, int pMinY, int pMaxX, int pMaxY, float pPartialTick) {
        int i = Mth.ceil((float)(pMaxX - pMinX - 2) * this.currentProgress);
        int j = Math.round(pPartialTick * 255.0F);
        if (this.colorBar != this.colorBackground) {
            int k = this.colorBar >> 16 & 0xFF;
            int l = this.colorBar >> 8 & 0xFF;
            int i1 = this.colorBar & 0xFF;
            int j1 = ARGB.color(j, k, l, i1);
            pGuiGraphics.fill(pMinX, pMinY, pMaxX, pMaxY, j1);
        }

        int j2 = this.colorProgress >> 16 & 0xFF;
        int k2 = this.colorProgress >> 8 & 0xFF;
        int l2 = this.colorProgress & 0xFF;
        int i3 = ARGB.color(j, j2, k2, l2);
        pGuiGraphics.fill(pMinX + 2, pMinY + 2, Math.clamp(pMinX + i, pMinX + 2, 1000), pMaxY - 2, i3);
        int k1 = this.colorOutline >> 16 & 0xFF;
        int l1 = this.colorOutline >> 8 & 0xFF;
        int i2 = this.colorOutline & 0xFF;
        i3 = ARGB.color(j, k1, l1, i2);
        pGuiGraphics.fill(pMinX + 1, pMinY, pMaxX - 1, pMinY + 1, i3);
        pGuiGraphics.fill(pMinX + 1, pMaxY, pMaxX - 1, pMaxY - 1, i3);
        pGuiGraphics.fill(pMinX, pMinY, pMinX + 1, pMaxY, i3);
        pGuiGraphics.fill(pMaxX, pMinY, pMaxX - 1, pMaxY, i3);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    protected boolean renderContents(GuiGraphics gui, float alpha) {
        return true;
    }

    public void update() {
        this.colorBackground = BRAND_BACKGROUND.getAsInt();
        this.colorBar = BRAND_BACKGROUND.getAsInt();
        this.colorOutline = 16777215;
        this.colorProgress = 16777215;
        if (Config.isCustomColors()) {
            try {
                String s = "optifine/color.properties";
                ResourceLocation resourcelocation = new ResourceLocation(s);
                if (!Config.hasResource(resourcelocation)) {
                    return;
                }

                InputStream inputstream = Config.getResourceStream(resourcelocation);
                Config.dbg("Loading " + s);
                Properties properties = new PropertiesOrdered();
                properties.load(inputstream);
                inputstream.close();
                this.colorBackground = readColor(properties, "screen.loading", this.colorBackground);
                this.colorOutline = readColor(properties, "screen.loading.outline", this.colorOutline);
                this.colorBar = readColor(properties, "screen.loading.bar", this.colorBar);
                this.colorProgress = readColor(properties, "screen.loading.progress", this.colorProgress);
                this.blendState = ShaderPackParser.parseBlendState(properties.getProperty("screen.loading.blend"));
            } catch (Exception exception) {
                Config.warn(exception.getClass().getName() + ": " + exception.getMessage());
            }
        }
    }

    private static int readColor(Properties props, String name, int colDef) {
        String s = props.getProperty(name);
        if (s == null) {
            return colDef;
        } else {
            s = s.trim();
            int i = parseColor(s, colDef);
            if (i < 0) {
                Config.warn("Invalid color: " + name + " = " + s);
                return i;
            } else {
                Config.dbg(name + " = " + s);
                return i;
            }
        }
    }

    private static int parseColor(String str, int colDef) {
        if (str == null) {
            return colDef;
        } else {
            str = str.trim();

            try {
                return Integer.parseInt(str, 16) & 16777215;
            } catch (NumberFormatException numberformatexception) {
                return colDef;
            }
        }
    }

    public boolean isFadeOut() {
        return this.fadeOut;
    }

    public static String getGuiChatText(ChatScreen guiChat) {
        return guiChat.input.getValue();
    }

    static class LogoTexture extends ReloadableTexture {
        public LogoTexture() {
            super(LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION);
        }

        @Override
        public TextureContents loadContents(ResourceManager p_376459_) throws IOException {
            ResourceProvider resourceprovider = Minecraft.getInstance().getVanillaPackResources().asProvider();

            TextureContents texturecontents;
            try (InputStream inputstream = getLogoInputStream(resourceprovider)) {
                texturecontents = new TextureContents(NativeImage.read(inputstream), new TextureMetadataSection(true, true));
            }

            return texturecontents;
        }

        private static InputStream getLogoInputStream(ResourceProvider resourceManager) throws IOException {
            return resourceManager.getResource(LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION).isPresent()
                ? resourceManager.getResource(LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION).get().open()
                : resourceManager.open(LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION);
        }
    }
}