package net.optifine.player;

import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.optifine.Config;
import net.optifine.http.FileDownloadThread;
import net.optifine.http.IFileDownloadListener;

public class CapeUtils {
    private static final Pattern PATTERN_USERNAME = Pattern.compile("[a-zA-Z0-9_]+");
    private static final Map CHECKED_USERNAMES = CacheBuilder.newBuilder().maximumSize(1000L).build().asMap();

    public static void downloadCape(final AbstractClientPlayer player) {
        String s = player.getNameClear();
        if (s != null && !s.isEmpty() && !s.contains("\u0000") && PATTERN_USERNAME.matcher(s).matches()) {
            String s1 = "http://s.optifine.net/capes/" + s + ".png";
            final ResourceLocation resourcelocation = new ResourceLocation("capeof/" + s);
            final TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            if (texturemanager.hasTexture(resourcelocation)
                && texturemanager.getTexture(resourcelocation) instanceof DynamicTexture dynamictexture
                && dynamictexture.isCapeTexture()) {
                player.setLocationOfCape(resourcelocation);
                player.setElytraOfCape(dynamictexture.isElytraCapeTexture());
                return;
            }

            if (CHECKED_USERNAMES.put(s, s) != null) {
                return;
            }

            IFileDownloadListener ifiledownloadlistener = new IFileDownloadListener() {
                @Override
                public void fileDownloadFinished(String url, byte[] bytes, Throwable exception) {
                    try {
                        if (exception != null) {
                            return;
                        }

                        NativeImage nativeimage = NativeImage.read(bytes);
                        NativeImage nativeimage1 = CapeUtils.parseCape(nativeimage);
                        DynamicTexture dynamictexture1 = new DynamicTexture(nativeimage1);
                        dynamictexture1.setCapeTexture(true);
                        dynamictexture1.setElytraCapeTexture(CapeUtils.isElytraCape(nativeimage, nativeimage1));
                        texturemanager.register(resourcelocation, dynamictexture1);
                        player.setLocationOfCape(resourcelocation);
                        player.setElytraOfCape(dynamictexture1.isElytraCapeTexture());
                    } catch (IOException ioexception) {
                        ioexception.printStackTrace();
                    }
                }
            };
            FileDownloadThread filedownloadthread = new FileDownloadThread(s1, ifiledownloadlistener);
            filedownloadthread.start();
        }
    }

    public static NativeImage parseCape(NativeImage img) {
        int i = 64;
        int j = 32;
        int k = img.getWidth();

        for (int l = img.getHeight(); i < k || j < l; j *= 2) {
            i *= 2;
        }

        NativeImage nativeimage = new NativeImage(i, j, true);
        nativeimage.copyFrom(img);
        img.close();
        return nativeimage;
    }

    public static boolean isElytraCape(NativeImage imageRaw, NativeImage imageFixed) {
        return imageRaw.getWidth() > imageFixed.getHeight();
    }

    public static void reloadCape(AbstractClientPlayer player) {
        String s = player.getNameClear();
        ResourceLocation resourcelocation = new ResourceLocation("capeof/" + s);
        TextureManager texturemanager = Config.getTextureManager();
        if (texturemanager.hasTexture(resourcelocation)) {
            AbstractTexture abstracttexture = texturemanager.getTexture(resourcelocation);
            abstracttexture.releaseId();
            texturemanager.release(resourcelocation);
        }

        player.setLocationOfCape(null);
        player.setElytraOfCape(false);
        CHECKED_USERNAMES.remove(s);
        downloadCape(player);
    }
}