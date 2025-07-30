package net.minecraft.client.renderer.texture;

import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.optifine.Config;
import net.optifine.reflect.Reflector;
import net.optifine.util.TextureUtils;
import org.slf4j.Logger;

public class SpriteLoader {
    public static final Set<MetadataSectionType<?>> DEFAULT_METADATA_SECTIONS = Set.of(AnimationMetadataSection.TYPE);
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ResourceLocation location;
    private final int maxSupportedTextureSize;
    private final int minWidth;
    private final int minHeight;
    private TextureAtlas atlas;

    public SpriteLoader(ResourceLocation pLocation, int pMaxSupportedTextureSize, int pMinWidth, int pMinHeight) {
        this.location = pLocation;
        this.maxSupportedTextureSize = pMaxSupportedTextureSize;
        this.minWidth = pMinWidth;
        this.minHeight = pMinHeight;
    }

    public static SpriteLoader create(TextureAtlas pAtlas) {
        SpriteLoader spriteloader = new SpriteLoader(pAtlas.location(), pAtlas.maxSupportedTextureSize(), pAtlas.getWidth(), pAtlas.getHeight());
        spriteloader.atlas = pAtlas;
        return spriteloader;
    }

    public SpriteLoader.Preparations stitch(List<SpriteContents> pContents, int pMipLevel, Executor pExecutor) {
        int i = this.atlas.mipmapLevel;
        int j = this.atlas.getIconGridSize();
        int k = this.maxSupportedTextureSize;
        Stitcher<SpriteContents> stitcher = new Stitcher<>(k, k, pMipLevel);
        int l = Integer.MAX_VALUE;
        int i1 = 1 << pMipLevel;

        for (SpriteContents spritecontents : pContents) {
            int j1 = spritecontents.getSpriteWidth();
            int k1 = spritecontents.getSpriteHeight();
            if (j1 >= 1 && k1 >= 1) {
                if (j1 < j || i > 0) {
                    int l1 = i > 0 ? TextureUtils.scaleToGrid(j1, j) : TextureUtils.scaleToMin(j1, j);
                    if (l1 != j1) {
                        if (!TextureUtils.isPowerOfTwo(j1)) {
                            Config.log("Scaled non power of 2: " + spritecontents.getSpriteLocation() + ", " + j1 + " -> " + l1);
                        } else {
                            Config.log("Scaled too small texture: " + spritecontents.getSpriteLocation() + ", " + j1 + " -> " + l1);
                        }

                        int i2 = k1 * l1 / j1;
                        double d0 = (double)l1 * 1.0 / (double)j1;
                        spritecontents.setSpriteWidth(l1);
                        spritecontents.setSpriteHeight(i2);
                        spritecontents.setScaleFactor(d0);
                        spritecontents.rescale();
                    }
                }

                l = Math.min(l, Math.min(spritecontents.width(), spritecontents.height()));
                int j3 = Math.min(Integer.lowestOneBit(spritecontents.width()), Integer.lowestOneBit(spritecontents.height()));
                if (j3 < i1) {
                    LOGGER.warn(
                        "Texture {} with size {}x{} limits mip level from {} to {}",
                        spritecontents.name(),
                        spritecontents.width(),
                        spritecontents.height(),
                        Mth.log2(i1),
                        Mth.log2(j3)
                    );
                    i1 = j3;
                }

                stitcher.registerSprite(spritecontents);
            } else {
                Config.warn("Invalid sprite size: " + spritecontents.getSpriteLocation());
            }
        }

        int j2 = Math.min(l, i1);
        int k2 = Mth.log2(j2);
        if (k2 < 0) {
            k2 = 0;
        }

        int l2;
        if (k2 < pMipLevel) {
            LOGGER.warn("{}: dropping miplevel from {} to {}, because of minimum power of two: {}", this.location, pMipLevel, k2, j2);
            l2 = k2;
        } else {
            l2 = pMipLevel;
        }

        try {
            stitcher.stitch();
        } catch (StitcherException stitcherexception) {
            CrashReport crashreport = CrashReport.forThrowable(stitcherexception, "Stitching");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Stitcher");
            crashreportcategory.setDetail(
                "Sprites",
                stitcherexception.getAllSprites()
                    .stream()
                    .map(entryIn -> String.format(Locale.ROOT, "%s[%dx%d]", entryIn.name(), entryIn.width(), entryIn.height()))
                    .collect(Collectors.joining(","))
            );
            crashreportcategory.setDetail("Max Texture Size", k);
            throw new ReportedException(crashreport);
        }

        int i3 = Math.max(stitcher.getWidth(), this.minWidth);
        int k3 = Math.max(stitcher.getHeight(), this.minHeight);
        Map<ResourceLocation, TextureAtlasSprite> map = this.getStitchedSprites(stitcher, i3, k3);
        TextureAtlasSprite textureatlassprite = map.get(MissingTextureAtlasSprite.getLocation());
        CompletableFuture<Void> completablefuture;
        if (l2 > 0) {
            completablefuture = CompletableFuture.runAsync(() -> map.values().forEach(spriteIn -> {
                    spriteIn.setTextureAtlas(this.atlas);
                    spriteIn.increaseMipLevel(l2);
                }), pExecutor);
        } else {
            completablefuture = CompletableFuture.completedFuture(null);
        }

        return new SpriteLoader.Preparations(i3, k3, l2, textureatlassprite, map, completablefuture);
    }

    public static CompletableFuture<List<SpriteContents>> runSpriteSuppliers(
        SpriteResourceLoader pSpriteResourceLoader, List<Function<SpriteResourceLoader, SpriteContents>> pFactories, Executor pExecutor
    ) {
        List<CompletableFuture<SpriteContents>> list = pFactories.stream()
            .map(functionIn -> CompletableFuture.supplyAsync(() -> (SpriteContents)functionIn.apply(pSpriteResourceLoader), pExecutor))
            .toList();
        return Util.sequence(list).thenApply(listSpritesIn -> listSpritesIn.stream().filter(Objects::nonNull).toList());
    }

    public CompletableFuture<SpriteLoader.Preparations> loadAndStitch(ResourceManager pResouceManager, ResourceLocation pLocation, int pMipLevel, Executor pExecutor) {
        return this.loadAndStitch(pResouceManager, pLocation, pMipLevel, pExecutor, DEFAULT_METADATA_SECTIONS);
    }

    public CompletableFuture<SpriteLoader.Preparations> loadAndStitch(
        ResourceManager pResourceManager, ResourceLocation pLocation, int pMipLevel, Executor pExecutor, Collection<MetadataSectionType<?>> pSectionSerializers
    ) {
        SpriteResourceLoader spriteresourceloader = SpriteResourceLoader.create(pSectionSerializers);
        return CompletableFuture.<List<Function<SpriteResourceLoader, SpriteContents>>>supplyAsync(() -> {
                SpriteSourceList spritesourcelist = SpriteSourceList.load(pResourceManager, pLocation);
                Set<ResourceLocation> set = spritesourcelist.getSpriteNames(pResourceManager);
                spritesourcelist.filterSpriteNames(set);
                Set<ResourceLocation> set1 = new LinkedHashSet<>(set);
                this.atlas.preStitch(set1, pResourceManager, pMipLevel);
                set1.removeAll(set);
                spritesourcelist.addSpriteSources(set1);
                return spritesourcelist.list(pResourceManager);
            }, pExecutor)
            .thenCompose(functionsIn -> runSpriteSuppliers(spriteresourceloader, (List<Function<SpriteResourceLoader, SpriteContents>>)functionsIn, pExecutor))
            .thenApply(contentsIn -> this.stitch((List<SpriteContents>)contentsIn, pMipLevel, pExecutor));
    }

    private Map<ResourceLocation, TextureAtlasSprite> getStitchedSprites(Stitcher<SpriteContents> pStitcher, int pX, int pY) {
        Map<ResourceLocation, TextureAtlasSprite> map = new HashMap<>();
        pStitcher.gatherSprites(
            (contentsIn, xIn, yIn) -> {
                if (Reflector.ForgeHooksClient_loadTextureAtlasSprite.exists()) {
                    TextureAtlasSprite textureatlassprite = (TextureAtlasSprite)Reflector.ForgeHooksClient_loadTextureAtlasSprite
                        .call(this.location, contentsIn, pX, pY, xIn, yIn, contentsIn.byMipLevel.length - 1);
                    if (textureatlassprite != null) {
                        map.put(contentsIn.name(), textureatlassprite);
                        return;
                    }
                }

                TextureAtlasSprite textureatlassprite1 = this.atlas.getRegisteredSprite(contentsIn.name());
                if (textureatlassprite1 != null) {
                    textureatlassprite1.init(this.location, contentsIn, pX, pY, xIn, yIn);
                } else {
                    textureatlassprite1 = new TextureAtlasSprite(this.location, contentsIn, pX, pY, xIn, yIn, this.atlas, null);
                }

                textureatlassprite1.update(Config.getResourceManager());
                map.put(contentsIn.name(), textureatlassprite1);
            }
        );
        return map;
    }

    public static record Preparations(
        int width,
        int height,
        int mipLevel,
        TextureAtlasSprite missing,
        Map<ResourceLocation, TextureAtlasSprite> regions,
        CompletableFuture<Void> readyForUpload
    ) {
        public CompletableFuture<SpriteLoader.Preparations> waitForUpload() {
            return this.readyForUpload.thenApply(voidIn -> this);
        }
    }
}