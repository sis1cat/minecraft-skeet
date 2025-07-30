package net.minecraft.client.resources;

import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MobEffectTextureManager extends TextureAtlasHolder {
    public MobEffectTextureManager(TextureManager pTextureManager) {
        super(pTextureManager, ResourceLocation.withDefaultNamespace("textures/atlas/mob_effects.png"), ResourceLocation.withDefaultNamespace("mob_effects"));
    }

    public TextureAtlasSprite get(Holder<MobEffect> pEffect) {
        return this.getSprite(pEffect.unwrapKey().map(ResourceKey::location).orElseGet(MissingTextureAtlasSprite::getLocation));
    }
}