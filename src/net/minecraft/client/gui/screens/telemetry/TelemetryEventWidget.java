package net.minecraft.client.gui.screens.telemetry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.DoubleConsumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TelemetryEventWidget extends AbstractTextAreaWidget {
    private static final int HEADER_HORIZONTAL_PADDING = 32;
    private static final String TELEMETRY_REQUIRED_TRANSLATION_KEY = "telemetry.event.required";
    private static final String TELEMETRY_OPTIONAL_TRANSLATION_KEY = "telemetry.event.optional";
    private static final String TELEMETRY_OPTIONAL_DISABLED_TRANSLATION_KEY = "telemetry.event.optional.disabled";
    private static final Component PROPERTY_TITLE = Component.translatable("telemetry_info.property_title").withStyle(ChatFormatting.UNDERLINE);
    private final Font font;
    private TelemetryEventWidget.Content content;
    @Nullable
    private DoubleConsumer onScrolledListener;

    public TelemetryEventWidget(int pX, int pY, int pWidth, int pHeight, Font pFont) {
        super(pX, pY, pWidth, pHeight, Component.empty());
        this.font = pFont;
        this.content = this.buildContent(Minecraft.getInstance().telemetryOptInExtra());
    }

    public void onOptInChanged(boolean pOptIn) {
        this.content = this.buildContent(pOptIn);
        this.refreshScrollAmount();
    }

    public void updateLayout() {
        this.content = this.buildContent(Minecraft.getInstance().telemetryOptInExtra());
        this.refreshScrollAmount();
    }

    private TelemetryEventWidget.Content buildContent(boolean pOptIn) {
        TelemetryEventWidget.ContentBuilder telemetryeventwidget$contentbuilder = new TelemetryEventWidget.ContentBuilder(this.containerWidth());
        List<TelemetryEventType> list = new ArrayList<>(TelemetryEventType.values());
        list.sort(Comparator.comparing(TelemetryEventType::isOptIn));

        for (int i = 0; i < list.size(); i++) {
            TelemetryEventType telemetryeventtype = list.get(i);
            boolean flag = telemetryeventtype.isOptIn() && !pOptIn;
            this.addEventType(telemetryeventwidget$contentbuilder, telemetryeventtype, flag);
            if (i < list.size() - 1) {
                telemetryeventwidget$contentbuilder.addSpacer(9);
            }
        }

        return telemetryeventwidget$contentbuilder.build();
    }

    public void setOnScrolledListener(@Nullable DoubleConsumer pOnScrolledListener) {
        this.onScrolledListener = pOnScrolledListener;
    }

    @Override
    public void setScrollAmount(double p_261736_) {
        super.setScrollAmount(p_261736_);
        if (this.onScrolledListener != null) {
            this.onScrolledListener.accept(this.scrollAmount());
        }
    }

    @Override
    protected int getInnerHeight() {
        return this.content.container().getHeight();
    }

    @Override
    protected double scrollRate() {
        return 9.0;
    }

    @Override
    protected void renderContents(GuiGraphics p_283081_, int p_283426_, int p_282414_, float p_283358_) {
        int i = this.getInnerTop();
        int j = this.getInnerLeft();
        p_283081_.pose().pushPose();
        p_283081_.pose().translate((double)j, (double)i, 0.0);
        this.content.container().visitWidgets(p_280896_ -> p_280896_.render(p_283081_, p_283426_, p_282414_, p_283358_));
        p_283081_.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_261538_) {
        p_261538_.add(NarratedElementType.TITLE, this.content.narration());
    }

    private Component grayOutIfDisabled(Component pComponent, boolean pDisabled) {
        return (Component)(pDisabled ? pComponent.copy().withStyle(ChatFormatting.GRAY) : pComponent);
    }

    private void addEventType(TelemetryEventWidget.ContentBuilder pContentBuilder, TelemetryEventType pEventType, boolean pDisabled) {
        String s = pEventType.isOptIn() ? (pDisabled ? "telemetry.event.optional.disabled" : "telemetry.event.optional") : "telemetry.event.required";
        pContentBuilder.addHeader(this.font, this.grayOutIfDisabled(Component.translatable(s, pEventType.title()), pDisabled));
        pContentBuilder.addHeader(this.font, pEventType.description().withStyle(ChatFormatting.GRAY));
        pContentBuilder.addSpacer(9 / 2);
        pContentBuilder.addLine(this.font, this.grayOutIfDisabled(PROPERTY_TITLE, pDisabled), 2);
        this.addEventTypeProperties(pEventType, pContentBuilder, pDisabled);
    }

    private void addEventTypeProperties(TelemetryEventType pEventType, TelemetryEventWidget.ContentBuilder pContentBuilder, boolean pDisabled) {
        for (TelemetryProperty<?> telemetryproperty : pEventType.properties()) {
            pContentBuilder.addLine(this.font, this.grayOutIfDisabled(telemetryproperty.title(), pDisabled));
        }
    }

    private int containerWidth() {
        return this.width - this.totalInnerPadding();
    }

    @OnlyIn(Dist.CLIENT)
    static record Content(Layout container, Component narration) {
    }

    @OnlyIn(Dist.CLIENT)
    static class ContentBuilder {
        private final int width;
        private final LinearLayout layout;
        private final MutableComponent narration = Component.empty();

        public ContentBuilder(int pWidth) {
            this.width = pWidth;
            this.layout = LinearLayout.vertical();
            this.layout.defaultCellSetting().alignHorizontallyLeft();
            this.layout.addChild(SpacerElement.width(pWidth));
        }

        public void addLine(Font pFont, Component pMessage) {
            this.addLine(pFont, pMessage, 0);
        }

        public void addLine(Font pFont, Component pMessage, int pPadding) {
            this.layout.addChild(new MultiLineTextWidget(pMessage, pFont).setMaxWidth(this.width), p_300900_ -> p_300900_.paddingBottom(pPadding));
            this.narration.append(pMessage).append("\n");
        }

        public void addHeader(Font pFont, Component pMessage) {
            this.layout
                .addChild(
                    new MultiLineTextWidget(pMessage, pFont).setMaxWidth(this.width - 64).setCentered(true),
                    p_298721_ -> p_298721_.alignHorizontallyCenter().paddingHorizontal(32)
                );
            this.narration.append(pMessage).append("\n");
        }

        public void addSpacer(int pHeight) {
            this.layout.addChild(SpacerElement.height(pHeight));
        }

        public TelemetryEventWidget.Content build() {
            this.layout.arrangeElements();
            return new TelemetryEventWidget.Content(this.layout, this.narration);
        }
    }
}