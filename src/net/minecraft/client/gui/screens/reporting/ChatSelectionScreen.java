package net.minecraft.client.gui.screens.reporting;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.ChatTrustLevel;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.multiplayer.chat.report.ChatReport;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatSelectionScreen extends Screen {
    static final ResourceLocation CHECKMARK_SPRITE = ResourceLocation.withDefaultNamespace("icon/checkmark");
    private static final Component TITLE = Component.translatable("gui.chatSelection.title");
    private static final Component CONTEXT_INFO = Component.translatable("gui.chatSelection.context");
    @Nullable
    private final Screen lastScreen;
    private final ReportingContext reportingContext;
    private Button confirmSelectedButton;
    private MultiLineLabel contextInfoLabel;
    @Nullable
    private ChatSelectionScreen.ChatSelectionList chatSelectionList;
    final ChatReport.Builder report;
    private final Consumer<ChatReport.Builder> onSelected;
    private ChatSelectionLogFiller chatLogFiller;

    public ChatSelectionScreen(@Nullable Screen pLastScreen, ReportingContext pReportingContext, ChatReport.Builder pReport, Consumer<ChatReport.Builder> pOnSelected) {
        super(TITLE);
        this.lastScreen = pLastScreen;
        this.reportingContext = pReportingContext;
        this.report = pReport.copy();
        this.onSelected = pOnSelected;
    }

    @Override
    protected void init() {
        this.chatLogFiller = new ChatSelectionLogFiller(this.reportingContext, this::canReport);
        this.contextInfoLabel = MultiLineLabel.create(this.font, CONTEXT_INFO, this.width - 16);
        this.chatSelectionList = this.addRenderableWidget(new ChatSelectionScreen.ChatSelectionList(this.minecraft, (this.contextInfoLabel.getLineCount() + 1) * 9));
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_BACK, p_239860_ -> this.onClose())
                .bounds(this.width / 2 - 155, this.height - 32, 150, 20)
                .build()
        );
        this.confirmSelectedButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, p_296214_ -> {
            this.onSelected.accept(this.report);
            this.onClose();
        }).bounds(this.width / 2 - 155 + 160, this.height - 32, 150, 20).build());
        this.updateConfirmSelectedButton();
        this.extendLog();
        this.chatSelectionList.setScrollAmount((double)this.chatSelectionList.maxScrollAmount());
    }

    private boolean canReport(LoggedChatMessage pMessage) {
        return pMessage.canReport(this.report.reportedProfileId());
    }

    private void extendLog() {
        int i = this.chatSelectionList.getMaxVisibleEntries();
        this.chatLogFiller.fillNextPage(i, this.chatSelectionList);
    }

    void onReachedScrollTop() {
        this.extendLog();
    }

    void updateConfirmSelectedButton() {
        this.confirmSelectedButton.active = !this.report.reportedMessages().isEmpty();
    }

    @Override
    public void render(GuiGraphics p_282899_, int p_239287_, int p_239288_, float p_239289_) {
        super.render(p_282899_, p_239287_, p_239288_, p_239289_);
        p_282899_.drawCenteredString(this.font, this.title, this.width / 2, 10, -1);
        AbuseReportLimits abusereportlimits = this.reportingContext.sender().reportLimits();
        int i = this.report.reportedMessages().size();
        int j = abusereportlimits.maxReportedMessageCount();
        Component component = Component.translatable("gui.chatSelection.selected", i, j);
        p_282899_.drawCenteredString(this.font, component, this.width / 2, 26, -1);
        this.contextInfoLabel.renderCentered(p_282899_, this.width / 2, this.chatSelectionList.getFooterTop());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), CONTEXT_INFO);
    }

    @OnlyIn(Dist.CLIENT)
    public class ChatSelectionList extends ObjectSelectionList<ChatSelectionScreen.ChatSelectionList.Entry> implements ChatSelectionLogFiller.Output {
        @Nullable
        private ChatSelectionScreen.ChatSelectionList.Heading previousHeading;

        public ChatSelectionList(final Minecraft pMinecraft, final int pHeight) {
            super(pMinecraft, ChatSelectionScreen.this.width, ChatSelectionScreen.this.height - pHeight - 80, 40, 16);
        }

        @Override
        public void setScrollAmount(double p_239021_) {
            double d0 = this.scrollAmount();
            super.setScrollAmount(p_239021_);
            if ((float)this.maxScrollAmount() > 1.0E-5F && p_239021_ <= 1.0E-5F && !Mth.equal(p_239021_, d0)) {
                ChatSelectionScreen.this.onReachedScrollTop();
            }
        }

        @Override
        public void acceptMessage(int p_242846_, LoggedChatMessage.Player p_242909_) {
            boolean flag = p_242909_.canReport(ChatSelectionScreen.this.report.reportedProfileId());
            ChatTrustLevel chattrustlevel = p_242909_.trustLevel();
            GuiMessageTag guimessagetag = chattrustlevel.createTag(p_242909_.message());
            ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = new ChatSelectionScreen.ChatSelectionList.MessageEntry(
                p_242846_, p_242909_.toContentComponent(), p_242909_.toNarrationComponent(), guimessagetag, flag, true
            );
            this.addEntryToTop(chatselectionscreen$chatselectionlist$entry);
            this.updateHeading(p_242909_, flag);
        }

        private void updateHeading(LoggedChatMessage.Player pLoggedPlayerChatMessage, boolean pCanReport) {
            ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = new ChatSelectionScreen.ChatSelectionList.MessageHeadingEntry(
                pLoggedPlayerChatMessage.profile(), pLoggedPlayerChatMessage.toHeadingComponent(), pCanReport
            );
            this.addEntryToTop(chatselectionscreen$chatselectionlist$entry);
            ChatSelectionScreen.ChatSelectionList.Heading chatselectionscreen$chatselectionlist$heading = new ChatSelectionScreen.ChatSelectionList.Heading(
                pLoggedPlayerChatMessage.profileId(), chatselectionscreen$chatselectionlist$entry
            );
            if (this.previousHeading != null && this.previousHeading.canCombine(chatselectionscreen$chatselectionlist$heading)) {
                this.removeEntryFromTop(this.previousHeading.entry());
            }

            this.previousHeading = chatselectionscreen$chatselectionlist$heading;
        }

        @Override
        public void acceptDivider(Component p_239876_) {
            this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
            this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.DividerEntry(p_239876_));
            this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
            this.previousHeading = null;
        }

        @Override
        public int getRowWidth() {
            return Math.min(350, this.width - 50);
        }

        public int getMaxVisibleEntries() {
            return Mth.positiveCeilDiv(this.height, this.itemHeight);
        }

        @Override
        protected void renderItem(
            GuiGraphics p_281532_, int p_239775_, int p_239776_, float p_239777_, int p_239778_, int p_239779_, int p_239780_, int p_239781_, int p_239782_
        ) {
            ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = this.getEntry(p_239778_);
            if (this.shouldHighlightEntry(chatselectionscreen$chatselectionlist$entry)) {
                boolean flag = this.getSelected() == chatselectionscreen$chatselectionlist$entry;
                int i = this.isFocused() && flag ? -1 : -8355712;
                this.renderSelection(p_281532_, p_239780_, p_239781_, p_239782_, i, -16777216);
            }

            chatselectionscreen$chatselectionlist$entry.render(
                p_281532_,
                p_239778_,
                p_239780_,
                p_239779_,
                p_239781_,
                p_239782_,
                p_239775_,
                p_239776_,
                this.getHovered() == chatselectionscreen$chatselectionlist$entry,
                p_239777_
            );
        }

        private boolean shouldHighlightEntry(ChatSelectionScreen.ChatSelectionList.Entry pEntry) {
            if (pEntry.canSelect()) {
                boolean flag = this.getSelected() == pEntry;
                boolean flag1 = this.getSelected() == null;
                boolean flag2 = this.getHovered() == pEntry;
                return flag || flag1 && flag2 && pEntry.canReport();
            } else {
                return false;
            }
        }

        @Nullable
        protected ChatSelectionScreen.ChatSelectionList.Entry nextEntry(ScreenDirection p_265203_) {
            return this.nextEntry(p_265203_, ChatSelectionScreen.ChatSelectionList.Entry::canSelect);
        }

        public void setSelected(@Nullable ChatSelectionScreen.ChatSelectionList.Entry p_265249_) {
            super.setSelected(p_265249_);
            ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = this.nextEntry(ScreenDirection.UP);
            if (chatselectionscreen$chatselectionlist$entry == null) {
                ChatSelectionScreen.this.onReachedScrollTop();
            }
        }

        @Override
        public boolean keyPressed(int p_239322_, int p_239323_, int p_239324_) {
            ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = this.getSelected();
            return chatselectionscreen$chatselectionlist$entry != null && chatselectionscreen$chatselectionlist$entry.keyPressed(p_239322_, p_239323_, p_239324_)
                ? true
                : super.keyPressed(p_239322_, p_239323_, p_239324_);
        }

        public int getFooterTop() {
            return this.getBottom() + 9;
        }

        @OnlyIn(Dist.CLIENT)
        public class DividerEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
            private final Component text;

            public DividerEntry(final Component pText) {
                this.text = pText;
            }

            @Override
            public void render(
                GuiGraphics p_283635_,
                int p_239815_,
                int p_239816_,
                int p_239817_,
                int p_239818_,
                int p_239819_,
                int p_239820_,
                int p_239821_,
                boolean p_239822_,
                float p_239823_
            ) {
                int i = p_239816_ + p_239819_ / 2;
                int j = p_239817_ + p_239818_ - 8;
                int k = ChatSelectionScreen.this.font.width(this.text);
                int l = (p_239817_ + j - k) / 2;
                int i1 = i - 9 / 2;
                p_283635_.drawString(ChatSelectionScreen.this.font, this.text, l, i1, -6250336);
            }

            @Override
            public Component getNarration() {
                return this.text;
            }
        }

        @OnlyIn(Dist.CLIENT)
        public abstract static class Entry extends ObjectSelectionList.Entry<ChatSelectionScreen.ChatSelectionList.Entry> {
            @Override
            public Component getNarration() {
                return CommonComponents.EMPTY;
            }

            public boolean isSelected() {
                return false;
            }

            public boolean canSelect() {
                return false;
            }

            public boolean canReport() {
                return this.canSelect();
            }

            @Override
            public boolean mouseClicked(double p_377929_, double p_377504_, int p_377351_) {
                return this.canSelect();
            }
        }

        @OnlyIn(Dist.CLIENT)
        static record Heading(UUID sender, ChatSelectionScreen.ChatSelectionList.Entry entry) {
            public boolean canCombine(ChatSelectionScreen.ChatSelectionList.Heading pOther) {
                return pOther.sender.equals(this.sender);
            }
        }

        @OnlyIn(Dist.CLIENT)
        public class MessageEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
            private static final int CHECKMARK_WIDTH = 9;
            private static final int CHECKMARK_HEIGHT = 8;
            private static final int INDENT_AMOUNT = 11;
            private static final int TAG_MARGIN_LEFT = 4;
            private final int chatId;
            private final FormattedText text;
            private final Component narration;
            @Nullable
            private final List<FormattedCharSequence> hoverText;
            @Nullable
            private final GuiMessageTag.Icon tagIcon;
            @Nullable
            private final List<FormattedCharSequence> tagHoverText;
            private final boolean canReport;
            private final boolean playerMessage;

            public MessageEntry(
                final int pChatId,
                final Component pText,
                final Component pNarration,
                @Nullable final GuiMessageTag pTagIcon,
                final boolean pCanReport,
                final boolean pPlayerMessage
            ) {
                this.chatId = pChatId;
                this.tagIcon = Optionull.map(pTagIcon, GuiMessageTag::icon);
                this.tagHoverText = pTagIcon != null && pTagIcon.text() != null
                    ? ChatSelectionScreen.this.font.split(pTagIcon.text(), ChatSelectionList.this.getRowWidth())
                    : null;
                this.canReport = pCanReport;
                this.playerMessage = pPlayerMessage;
                FormattedText formattedtext = ChatSelectionScreen.this.font
                    .substrByWidth(pText, this.getMaximumTextWidth() - ChatSelectionScreen.this.font.width(CommonComponents.ELLIPSIS));
                if (pText != formattedtext) {
                    this.text = FormattedText.composite(formattedtext, CommonComponents.ELLIPSIS);
                    this.hoverText = ChatSelectionScreen.this.font.split(pText, ChatSelectionList.this.getRowWidth());
                } else {
                    this.text = pText;
                    this.hoverText = null;
                }

                this.narration = pNarration;
            }

            @Override
            public void render(
                GuiGraphics p_281361_,
                int p_239596_,
                int p_239597_,
                int p_239598_,
                int p_239599_,
                int p_239600_,
                int p_239601_,
                int p_239602_,
                boolean p_239603_,
                float p_239604_
            ) {
                if (this.isSelected() && this.canReport) {
                    this.renderSelectedCheckmark(p_281361_, p_239597_, p_239598_, p_239600_);
                }

                int i = p_239598_ + this.getTextIndent();
                int j = p_239597_ + 1 + (p_239600_ - 9) / 2;
                p_281361_.drawString(ChatSelectionScreen.this.font, Language.getInstance().getVisualOrder(this.text), i, j, this.canReport ? -1 : -1593835521);
                if (this.hoverText != null && p_239603_) {
                    ChatSelectionScreen.this.setTooltipForNextRenderPass(this.hoverText);
                }

                int k = ChatSelectionScreen.this.font.width(this.text);
                this.renderTag(p_281361_, i + k + 4, p_239597_, p_239600_, p_239601_, p_239602_);
            }

            private void renderTag(GuiGraphics pGuiGraphics, int pX, int pY, int pHeight, int pMouseX, int pMouseY) {
                if (this.tagIcon != null) {
                    int i = pY + (pHeight - this.tagIcon.height) / 2;
                    this.tagIcon.draw(pGuiGraphics, pX, i);
                    if (this.tagHoverText != null
                        && pMouseX >= pX
                        && pMouseX <= pX + this.tagIcon.width
                        && pMouseY >= i
                        && pMouseY <= i + this.tagIcon.height) {
                        ChatSelectionScreen.this.setTooltipForNextRenderPass(this.tagHoverText);
                    }
                }
            }

            private void renderSelectedCheckmark(GuiGraphics pGuiGraphics, int pTop, int pLeft, int pHeight) {
                int i = pTop + (pHeight - 8) / 2;
                pGuiGraphics.blitSprite(RenderType::guiTextured, ChatSelectionScreen.CHECKMARK_SPRITE, pLeft, i, 9, 8);
            }

            private int getMaximumTextWidth() {
                int i = this.tagIcon != null ? this.tagIcon.width + 4 : 0;
                return ChatSelectionList.this.getRowWidth() - this.getTextIndent() - 4 - i;
            }

            private int getTextIndent() {
                return this.playerMessage ? 11 : 0;
            }

            @Override
            public Component getNarration() {
                return (Component)(this.isSelected() ? Component.translatable("narrator.select", this.narration) : this.narration);
            }

            @Override
            public boolean mouseClicked(double p_239729_, double p_239730_, int p_239731_) {
                ChatSelectionList.this.setSelected(null);
                return this.toggleReport();
            }

            @Override
            public boolean keyPressed(int p_239368_, int p_239369_, int p_239370_) {
                return CommonInputs.selected(p_239368_) ? this.toggleReport() : false;
            }

            @Override
            public boolean isSelected() {
                return ChatSelectionScreen.this.report.isReported(this.chatId);
            }

            @Override
            public boolean canSelect() {
                return true;
            }

            @Override
            public boolean canReport() {
                return this.canReport;
            }

            private boolean toggleReport() {
                if (this.canReport) {
                    ChatSelectionScreen.this.report.toggleReported(this.chatId);
                    ChatSelectionScreen.this.updateConfirmSelectedButton();
                    return true;
                } else {
                    return false;
                }
            }
        }

        @OnlyIn(Dist.CLIENT)
        public class MessageHeadingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
            private static final int FACE_SIZE = 12;
            private static final int PADDING = 4;
            private final Component heading;
            private final Supplier<PlayerSkin> skin;
            private final boolean canReport;

            public MessageHeadingEntry(final GameProfile pProfile, final Component pHeading, final boolean pCanReport) {
                this.heading = pHeading;
                this.canReport = pCanReport;
                this.skin = ChatSelectionList.this.minecraft.getSkinManager().lookupInsecure(pProfile);
            }

            @Override
            public void render(
                GuiGraphics p_281320_,
                int p_283177_,
                int p_282422_,
                int p_282017_,
                int p_282555_,
                int p_283255_,
                int p_283682_,
                int p_281582_,
                boolean p_282259_,
                float p_283561_
            ) {
                int i = p_282017_ - 12 + 4;
                int j = p_282422_ + (p_283255_ - 12) / 2;
                PlayerFaceRenderer.draw(p_281320_, this.skin.get(), i, j, 12);
                int k = p_282422_ + 1 + (p_283255_ - 9) / 2;
                p_281320_.drawString(ChatSelectionScreen.this.font, this.heading, i + 12 + 4, k, this.canReport ? -1 : -1593835521);
            }
        }

        @OnlyIn(Dist.CLIENT)
        public static class PaddingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
            @Override
            public void render(
                GuiGraphics p_282007_,
                int p_240110_,
                int p_240111_,
                int p_240112_,
                int p_240113_,
                int p_240114_,
                int p_240115_,
                int p_240116_,
                boolean p_240117_,
                float p_240118_
            ) {
            }
        }
    }
}