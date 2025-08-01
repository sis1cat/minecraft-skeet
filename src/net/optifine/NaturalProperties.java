package net.optifine;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public class NaturalProperties {
    public int rotation = 1;
    public boolean flip = false;
    private Map[] quadMaps = new Map[8];

    public NaturalProperties(String type) {
        if (type.equals("4")) {
            this.rotation = 4;
        } else if (type.equals("2")) {
            this.rotation = 2;
        } else if (type.equals("F")) {
            this.flip = true;
        } else if (type.equals("4F")) {
            this.rotation = 4;
            this.flip = true;
        } else if (type.equals("2F")) {
            this.rotation = 2;
            this.flip = true;
        } else {
            Config.warn("NaturalTextures: Unknown type: " + type);
        }
    }

    public boolean isValid() {
        return this.rotation == 2 || this.rotation == 4 ? true : this.flip;
    }

    public synchronized BakedQuad getQuad(BakedQuad quadIn, int rotate, boolean flipU) {
        int i = rotate;
        if (flipU) {
            i = rotate | 4;
        }

        if (i > 0 && i < this.quadMaps.length) {
            Map map = this.quadMaps[i];
            if (map == null) {
                map = new IdentityHashMap(1);
                this.quadMaps[i] = map;
            }

            BakedQuad bakedquad = (BakedQuad)map.get(quadIn);
            if (bakedquad == null) {
                bakedquad = this.makeQuad(quadIn, rotate, flipU);
                map.put(quadIn, bakedquad);
            }

            return bakedquad;
        } else {
            return quadIn;
        }
    }

    private BakedQuad makeQuad(BakedQuad quad, int rotate, boolean flipU) {
        int[] aint = quad.getVertices();
        int i = quad.getTintIndex();
        Direction direction = quad.getDirection();
        TextureAtlasSprite textureatlassprite = quad.getSprite();
        boolean flag = quad.isShade();
        int j = quad.getLightEmission();
        if (!this.isFullSprite(quad)) {
            rotate = 0;
        }

        aint = this.transformVertexData(aint, rotate, flipU);
        return new BakedQuad(aint, i, direction, textureatlassprite, flag, j);
    }

    private int[] transformVertexData(int[] vertexData, int rotate, boolean flipU) {
        int[] aint = (int[])vertexData.clone();
        int i = 4 - rotate;
        if (flipU) {
            i += 3;
        }

        i %= 4;
        int j = aint.length / 4;

        for (int k = 0; k < 4; k++) {
            int l = k * j;
            int i1 = i * j;
            aint[i1 + 4] = vertexData[l + 4];
            aint[i1 + 4 + 1] = vertexData[l + 4 + 1];
            if (flipU) {
                if (--i < 0) {
                    i = 3;
                }
            } else if (++i > 3) {
                i = 0;
            }
        }

        return aint;
    }

    private boolean isFullSprite(BakedQuad quad) {
        TextureAtlasSprite textureatlassprite = quad.getSprite();
        float f = textureatlassprite.getU0();
        float f1 = textureatlassprite.getU1();
        float f2 = f1 - f;
        float f3 = f2 / 256.0F;
        float f4 = textureatlassprite.getV0();
        float f5 = textureatlassprite.getV1();
        float f6 = f5 - f4;
        float f7 = f6 / 256.0F;
        int[] aint = quad.getVertices();
        int i = aint.length / 4;

        for (int j = 0; j < 4; j++) {
            int k = j * i;
            float f8 = Float.intBitsToFloat(aint[k + 4]);
            float f9 = Float.intBitsToFloat(aint[k + 4 + 1]);
            if (!this.equalsDelta(f8, f, f3) && !this.equalsDelta(f8, f1, f3)) {
                return false;
            }

            if (!this.equalsDelta(f9, f4, f7) && !this.equalsDelta(f9, f5, f7)) {
                return false;
            }
        }

        return true;
    }

    private boolean equalsDelta(float x1, float x2, float deltaMax) {
        float f = Mth.abs(x1 - x2);
        return f < deltaMax;
    }
}