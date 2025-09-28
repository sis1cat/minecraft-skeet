package net.minecraftforge.client;

import net.minecraft.client.renderer.RenderType;

public record RenderTypeGroup(RenderType block, RenderType entity, RenderType entityFabulous) {
    public static final RenderTypeGroup EMPTY = new RenderTypeGroup(null, null, null);

    public boolean isEmpty() {
        return this.block == null;
    }
}