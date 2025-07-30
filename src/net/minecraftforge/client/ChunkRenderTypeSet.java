package net.minecraftforge.client;

import java.util.Iterator;
import net.minecraft.client.renderer.RenderType;

public class ChunkRenderTypeSet implements Iterable<RenderType> {
    public static ChunkRenderTypeSet of(RenderType... renderTypes) {
        return null;
    }

    @Override
    public Iterator<RenderType> iterator() {
        throw new UnsupportedOperationException();
    }
}