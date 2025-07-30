package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Quaternionf;

public abstract class TextureSheetParticle extends SingleQuadParticle {
    protected TextureAtlasSprite sprite;

    protected TextureSheetParticle(ClientLevel p_108323_, double p_108324_, double p_108325_, double p_108326_) {
        super(p_108323_, p_108324_, p_108325_, p_108326_);
    }

    protected TextureSheetParticle(
        ClientLevel p_108328_, double p_108329_, double p_108330_, double p_108331_, double p_108332_, double p_108333_, double p_108334_
    ) {
        super(p_108328_, p_108329_, p_108330_, p_108331_, p_108332_, p_108333_, p_108334_);
    }

    protected void setSprite(TextureAtlasSprite pSprite) {
        this.sprite = pSprite;
    }

    @Override
    protected float getU0() {
        return this.sprite.getU0();
    }

    @Override
    protected float getU1() {
        return this.sprite.getU1();
    }

    @Override
    protected float getV0() {
        return this.sprite.getV0();
    }

    @Override
    protected float getV1() {
        return this.sprite.getV1();
    }

    public void pickSprite(SpriteSet pSprite) {
        this.setSprite(pSprite.get(this.random));
    }

    public void setSpriteFromAge(SpriteSet pSprite) {
        if (!this.removed) {
            this.setSprite(pSprite.get(this.age, this.lifetime));
        }
    }

    @Override
    protected void renderRotatedQuad(VertexConsumer bufferIn, Quaternionf quatIn, float xIn, float yIn, float zIn, float partialTicks) {
        bufferIn.setSprite(this.sprite);
        super.renderRotatedQuad(bufferIn, quatIn, xIn, yIn, zIn, partialTicks);
    }
}