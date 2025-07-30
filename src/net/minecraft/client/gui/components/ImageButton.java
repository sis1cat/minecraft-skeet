package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ImageButton extends Button {
    protected final WidgetSprites sprites;

    public ImageButton(int pX, int pY, int pWidth, int pHeight, WidgetSprites pSprites, Button.OnPress pOnPress) {
        this(pX, pY, pWidth, pHeight, pSprites, pOnPress, CommonComponents.EMPTY);
    }

    public ImageButton(int pX, int pY, int pWidth, int pHeight, WidgetSprites pSprites, Button.OnPress pOnPress, Component pMessage) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress, DEFAULT_NARRATION);
        this.sprites = pSprites;
    }

    public ImageButton(int pWidth, int pHeight, WidgetSprites pSprites, Button.OnPress pOnPress, Component pMessage) {
        this(0, 0, pWidth, pHeight, pSprites, pOnPress, pMessage);
    }

    @Override
    public void renderWidget(GuiGraphics p_283502_, int p_281473_, int p_283021_, float p_282518_) {
        ResourceLocation resourcelocation = this.sprites.get(this.isActive(), this.isHoveredOrFocused());
        p_283502_.blitSprite(RenderType::guiTextured, resourcelocation, this.getX(), this.getY(), this.width, this.height);
    }
}