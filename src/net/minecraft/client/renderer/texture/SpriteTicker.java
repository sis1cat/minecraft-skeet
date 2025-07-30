package net.minecraft.client.renderer.texture;

public interface SpriteTicker extends AutoCloseable {
    void tickAndUpload(int pX, int pY);

    @Override
    void close();

    default TextureAtlasSprite getSprite() {
        return null;
    }

    default void setSprite(TextureAtlasSprite sprite) {
    }
}