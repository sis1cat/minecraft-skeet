package net.minecraft.client.renderer.texture.atlas;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.Mth;
import net.optifine.reflect.Reflector;
import org.slf4j.Logger;

@FunctionalInterface
public interface SpriteResourceLoader {
    Logger LOGGER = LogUtils.getLogger();

    static SpriteResourceLoader create(Collection<MetadataSectionType<?>> pSectionSerializers) {
        return (locIn, resIn) -> {
            ResourceMetadata resourcemetadata;
            try {
                resourcemetadata = resIn.metadata().copySections(pSectionSerializers);
            } catch (Exception exception) {
                LOGGER.error("Unable to parse metadata from {}", locIn, exception);
                return null;
            }

            NativeImage nativeimage;
            try (InputStream inputstream = resIn.open()) {
                nativeimage = NativeImage.read(inputstream);
            } catch (IOException ioexception) {
                LOGGER.error("Using missing texture, unable to load {}", locIn, ioexception);
                return null;
            }

            Optional<AnimationMetadataSection> optional = resourcemetadata.getSection(AnimationMetadataSection.TYPE);
            FrameSize framesize;
            if (optional.isPresent()) {
                framesize = optional.get().calculateFrameSize(nativeimage.getWidth(), nativeimage.getHeight());
                if (!Mth.isMultipleOf(nativeimage.getWidth(), framesize.width()) || !Mth.isMultipleOf(nativeimage.getHeight(), framesize.height())) {
                    LOGGER.error(
                        "Image {} size {},{} is not multiple of frame size {},{}",
                        locIn,
                        nativeimage.getWidth(),
                        nativeimage.getHeight(),
                        framesize.width(),
                        framesize.height()
                    );
                    nativeimage.close();
                    return null;
                }
            } else {
                framesize = new FrameSize(nativeimage.getWidth(), nativeimage.getHeight());
            }

            if (Reflector.ForgeHooksClient_loadSpriteContents.exists()) {
                SpriteContents spritecontents = (SpriteContents)Reflector.ForgeHooksClient_loadSpriteContents
                    .call(locIn, resIn, framesize, nativeimage, resourcemetadata);
                if (spritecontents != null) {
                    return spritecontents;
                }
            }

            return new SpriteContents(locIn, framesize, nativeimage, resourcemetadata);
        };
    }

    @Nullable
    SpriteContents loadSprite(ResourceLocation pLocation, Resource pResource);
}