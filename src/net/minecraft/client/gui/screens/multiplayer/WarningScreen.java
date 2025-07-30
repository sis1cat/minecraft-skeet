package net.minecraft.client.gui.screens.multiplayer;

import javax.annotation.Nullable;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class WarningScreen extends Screen {
    private static final int MESSAGE_PADDING = 100;
    private final Component message;
    @Nullable
    private final Component check;
    private final Component narration;
    @Nullable
    protected Checkbox stopShowing;
    @Nullable
    private FocusableTextWidget messageWidget;
    private final FrameLayout layout;

    protected WarningScreen(Component pTitle, Component pContent, Component pNarration) {
        this(pTitle, pContent, null, pNarration);
    }

    protected WarningScreen(Component pTitle, Component pContent, @Nullable Component pCheck, Component pNarration) {
        super(pTitle);
        this.message = pContent;
        this.check = pCheck;
        this.narration = pNarration;
        this.layout = new FrameLayout(0, 0, this.width, this.height);
    }

    protected abstract Layout addFooterButtons();

    @Override
    protected void init() {
        LinearLayout linearlayout = this.layout.addChild(LinearLayout.vertical().spacing(8));
        linearlayout.defaultCellSetting().alignHorizontallyCenter();
        linearlayout.addChild(new StringWidget(this.getTitle(), this.font));
        this.messageWidget = linearlayout.addChild(
            new FocusableTextWidget(this.width - 100, this.message, this.font, 12), p_328910_ -> p_328910_.padding(12)
        );
        this.messageWidget.setCentered(false);
        LinearLayout linearlayout1 = linearlayout.addChild(LinearLayout.vertical().spacing(8));
        linearlayout1.defaultCellSetting().alignHorizontallyCenter();
        if (this.check != null) {
            this.stopShowing = linearlayout1.addChild(Checkbox.builder(this.check, this.font).build());
        }

        linearlayout1.addChild(this.addFooterButtons());
        this.layout.visitWidgets(p_330212_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_330212_);
        });
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        if (this.messageWidget != null) {
            this.messageWidget.setMaxWidth(this.width - 100);
        }

        this.layout.arrangeElements();
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
    }

    @Override
    public Component getNarrationMessage() {
        return this.narration;
    }
}