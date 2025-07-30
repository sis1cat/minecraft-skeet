package net.minecraft.client.gui.screens;

import java.net.URI;
import java.nio.file.Path;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DisconnectedScreen extends Screen {
    private static final Component TO_SERVER_LIST = Component.translatable("gui.toMenu");
    private static final Component TO_TITLE = Component.translatable("gui.toTitle");
    private static final Component REPORT_TO_SERVER_TITLE = Component.translatable("gui.report_to_server");
    private static final Component OPEN_REPORT_DIR_TITLE = Component.translatable("gui.open_report_dir");
    private final Screen parent;
    private final DisconnectionDetails details;
    private final Component buttonText;
    private final LinearLayout layout = LinearLayout.vertical();

    public DisconnectedScreen(Screen pParent, Component pTitle, Component pReason) {
        this(pParent, pTitle, new DisconnectionDetails(pReason));
    }

    public DisconnectedScreen(Screen pParent, Component pTitle, Component pReason, Component pButtonText) {
        this(pParent, pTitle, new DisconnectionDetails(pReason), pButtonText);
    }

    public DisconnectedScreen(Screen pParent, Component pTitle, DisconnectionDetails pDetails) {
        this(pParent, pTitle, pDetails, TO_SERVER_LIST);
    }

    public DisconnectedScreen(Screen pParent, Component pTitle, DisconnectionDetails pDetails, Component pButtonText) {
        super(pTitle);
        this.parent = pParent;
        this.details = pDetails;
        this.buttonText = pButtonText;
    }

    @Override
    protected void init() {
        this.layout.defaultCellSetting().alignHorizontallyCenter().padding(10);
        this.layout.addChild(new StringWidget(this.title, this.font));
        this.layout.addChild(new MultiLineTextWidget(this.details.reason(), this.font).setMaxWidth(this.width - 50).setCentered(true));
        this.layout.defaultCellSetting().padding(2);
        this.details
            .bugReportLink()
            .ifPresent(
                p_340800_ -> this.layout
                        .addChild(Button.builder(REPORT_TO_SERVER_TITLE, ConfirmLinkScreen.confirmLink(this, p_340800_, false)).width(200).build())
            );
        this.details
            .report()
            .ifPresent(
                p_340799_ -> this.layout
                        .addChild(Button.builder(OPEN_REPORT_DIR_TITLE, p_340802_ -> Util.getPlatform().openPath(p_340799_.getParent())).width(200).build())
            );
        Button button;
        if (this.minecraft.allowsMultiplayer()) {
            button = Button.builder(this.buttonText, p_280799_ -> this.minecraft.setScreen(this.parent)).width(200).build();
        } else {
            button = Button.builder(TO_TITLE, p_280800_ -> this.minecraft.setScreen(new TitleScreen())).width(200).build();
        }

        this.layout.addChild(button);
        this.layout.arrangeElements();
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(this.title, this.details.reason());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}