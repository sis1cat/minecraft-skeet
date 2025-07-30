package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpriteGetter {
    TextureAtlasSprite get(Material pMaterial);

    TextureAtlasSprite reportMissingReference(String pReference);
}