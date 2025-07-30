package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.network.chat.*;
import net.minecraft.util.*;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.slf4j.Logger;
import sisicat.IDefault;
import sisicat.main.functions.FunctionsManager;

import static sisicat.IDefault.displayClientChatMessage;

public class ChatComponent {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_CHAT_HISTORY = 100;
    private static final int MESSAGE_NOT_FOUND = -1;
    private static final int MESSAGE_INDENT = 4;
    private static final int MESSAGE_TAG_MARGIN_LEFT = 4;
    private static final int BOTTOM_MARGIN = 40;
    private static final int TIME_BEFORE_MESSAGE_DELETION = 60;
    private static final Component DELETED_CHAT_MESSAGE = Component.translatable("chat.deleted_marker").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    private final Minecraft minecraft;
    private final ArrayListDeque<String> recentChat = new ArrayListDeque<>(100);
    private final List<GuiMessage> allMessages = Lists.newArrayList();
    private final List<GuiMessage.Line> trimmedMessages = Lists.newArrayList();
    private int chatScrollbarPos;
    private boolean newMessageSinceScroll;
    private final List<ChatComponent.DelayedMessageDeletion> messageDeletionQueue = new ArrayList<>();
    private int lastChatWidth = 0;


    protected int xOffset = 0;

    public void setXOffset(int offset) {
        xOffset = offset;
    }


    public ChatComponent(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
        this.recentChat.addAll(pMinecraft.commandHistory().history());
    }

    public void tick() {
        if (!this.messageDeletionQueue.isEmpty()) {
            this.processMessageDeletionQueue();
        }
    }

    public void render(GuiGraphics pGuiGraphics, int pTickCount, int pMouseX, int pMouseY, boolean pFocused, float xOffset) {
        int i = this.getWidth();
        if (this.lastChatWidth != i) {
            this.lastChatWidth = i;
            this.rescaleChat();
        }

        if(!trimmedMessages.isEmpty() && FunctionsManager.getFunctionByName("Integrated UI animations").getSettingByName("Integrated UI animations").getSelectedOptionsList().contains("Smooth chat"))
            for (GuiMessage.Line line : trimmedMessages)
                if (line != null)
                    line.interpolation().interpolate();

        if (!this.isChatHidden()) {
            int maxMessages = this.getLinesPerPage();
            int k = this.trimmedMessages.size();
            if (k > 0) {
                ProfilerFiller profilerfiller = Profiler.get();
                profilerfiller.push("chat");
                float chatScale = (float)this.getScale();
                int scaledWidth = Mth.ceil((float)this.getWidth() / chatScale);
                int guiHeight = pGuiGraphics.guiHeight();
                pGuiGraphics.pose().pushPose();
                pGuiGraphics.pose().scale(chatScale, chatScale, 1.0F);
                pGuiGraphics.pose().translate(4.0F + xOffset, 0.0F, 0.0F);
                int j1 = Mth.floor((float)(guiHeight - 40) / chatScale);
                int k1 = this.getMessageEndIndexAt(this.screenToChatX((double)pMouseX), this.screenToChatY((double)pMouseY));
                double chatOpacity = this.minecraft.options.chatOpacity().get() * 0.9 + 0.1;
                double backgroundOpacity = this.minecraft.options.textBackgroundOpacity().get();
                double lineSpacing = this.minecraft.options.chatLineSpacing().get();
                int lineHeight = this.getLineHeight();
                int i2 = (int)Math.round(-8.0 * (lineSpacing + 1.0) + 4.0 * lineSpacing);
                int j2 = 0;

                for (int k2 = 0; k2 + this.chatScrollbarPos < this.trimmedMessages.size() && k2 < maxMessages; k2++) {
                    int l2 = k2 + this.chatScrollbarPos;
                    GuiMessage.Line guimessage$line = this.trimmedMessages.get(l2);
                    if (guimessage$line != null) {
                        int i3 = pTickCount - guimessage$line.addedTime();
                        if (i3 < 200 || pFocused) {
                            double messageAlpha = pFocused ? 1.0 : getTimeFactor(i3);
                            int textAlpha = (int)(255.0 * messageAlpha * chatOpacity);
                            int backgroundAlpha = (int)(255.0 * messageAlpha * backgroundOpacity);
                            j2++;
                            if (textAlpha > 3) {

                                int j4 = j1 - k2 * lineHeight;

                                int k4 = j4 + i2;


                                if (this.minecraft.options.ofChatBackground == 5) {
                                    scaledWidth = this.minecraft.font.width(guimessage$line.content()) - 2;
                                }

                                int messageXOffset = (int) guimessage$line.interpolation().value;

                                if(!FunctionsManager.getFunctionByName("Integrated UI animations").getSettingByName("Integrated UI animations").getSelectedOptionsList().contains("Smooth chat"))
                                    messageXOffset = 0;

                                if (this.minecraft.options.ofChatBackground != 3) {

                                    pGuiGraphics.fill(-4 + messageXOffset, j4 - lineHeight, scaledWidth + 8 + messageXOffset, j4, backgroundAlpha << 24);

                                }

                                GuiMessageTag guimessagetag = guimessage$line.tag();
                                if (guimessagetag != null) {
                                    int l4 = guimessagetag.indicatorColor() | textAlpha << 24;
                                    pGuiGraphics.fill(-4, j4 - lineHeight, -2, j4, l4);
                                    if (l2 == k1 && guimessagetag.icon() != null) {
                                        int i5 = this.getTagIconLeft(guimessage$line);
                                        int j5 = k4 + 9;
                                        this.drawTagIcon(pGuiGraphics, i5, j5, guimessagetag.icon());
                                    }
                                }

                                pGuiGraphics.pose().pushPose();
                                pGuiGraphics.pose().translate(0.0F, 0.0F, 50.0F);
                                if (!this.minecraft.options.ofChatShadow) {
                                    pGuiGraphics.drawString(this.minecraft.font, guimessage$line.content(), messageXOffset, k4, 16777215 + (textAlpha << 24));
                                } else {
                                    pGuiGraphics.drawString(this.minecraft.font, guimessage$line.content(), messageXOffset, k4, ARGB.color(textAlpha, -1));
                                }

                                pGuiGraphics.pose().popPose();
                            }
                        }
                    }
                }

                long k5 = this.minecraft.getChatListener().queueSize();
                if (k5 > 0L) {
                    int l5 = (int)(128.0 * chatOpacity);
                    int j6 = (int)(255.0 * backgroundOpacity);
                    pGuiGraphics.pose().pushPose();
                    pGuiGraphics.pose().translate(0.0F, (float)j1, 0.0F);
                    pGuiGraphics.fill(-2, 0, scaledWidth + 4, 9, j6 << 24);
                    pGuiGraphics.pose().translate(0.0F, 0.0F, 50.0F);
                    pGuiGraphics.drawString(this.minecraft.font, Component.translatable("chat.queue", k5), 0, 1, 16777215 + (l5 << 24));
                    pGuiGraphics.pose().popPose();
                }

                if (pFocused) {
                    int i6 = this.getLineHeight();
                    int k6 = k * i6;
                    int l6 = j2 * i6;
                    int j3 = this.chatScrollbarPos * l6 / k - j1;
                    int i7 = l6 * l6 / k6;
                    if (k6 != l6) {
                        int j7 = j3 > 0 ? 170 : 96;
                        int k7 = this.newMessageSinceScroll ? 13382451 : 3355562;
                        int l7 = scaledWidth + 4;
                        pGuiGraphics.fill(l7, -j3, l7 + 2, -j3 - i7, 100, k7 + (j7 << 24));
                        pGuiGraphics.fill(l7 + 2, -j3, l7 + 1, -j3 - i7, 100, 13421772 + (j7 << 24));
                    }
                }

                pGuiGraphics.pose().popPose();
                profilerfiller.pop();
            }
        }
    }

    private void drawTagIcon(GuiGraphics pGuiGraphics, int pLeft, int pBottom, GuiMessageTag.Icon pTagIcon) {
        int i = pBottom - pTagIcon.height - 1;
        pTagIcon.draw(pGuiGraphics, pLeft, i);
    }

    private int getTagIconLeft(GuiMessage.Line pLine) {
        return this.minecraft.font.width(pLine.content()) + 4;
    }

    private boolean isChatHidden() {
        return this.minecraft.options.chatVisibility().get() == ChatVisiblity.HIDDEN;
    }

    private static double getTimeFactor(int pCounter) {
        double d0 = (double)pCounter / 200.0;
        d0 = 1.0 - d0;
        d0 *= 10.0;
        d0 = Mth.clamp(d0, 0.0, 1.0);
        return d0 * d0;
    }

    public void clearMessages(boolean pClearSentMsgHistory) {
        this.minecraft.getChatListener().clearQueue();
        this.messageDeletionQueue.clear();
        this.trimmedMessages.clear();
        this.allMessages.clear();
        if (pClearSentMsgHistory) {
            this.recentChat.clear();
            this.recentChat.addAll(this.minecraft.commandHistory().history());
        }
    }

    public void addMessage(Component pChatComponent) {
        this.addMessage(pChatComponent, null, this.minecraft.isSingleplayer() ? GuiMessageTag.systemSinglePlayer() : GuiMessageTag.system());
    }

    public void addMessage(Component pChatComponent, @Nullable MessageSignature pHeaderSignature, @Nullable GuiMessageTag pTag) {
        GuiMessage guimessage = new GuiMessage(this.minecraft.gui.getGuiTicks(), pChatComponent, pHeaderSignature, pTag);
        this.logChatMessage(guimessage);
        this.addMessageToDisplayQueue(guimessage, false);
        this.addMessageToQueue(guimessage);
    }

    private void logChatMessage(GuiMessage pMessage) {
        String s = pMessage.content().getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n");
        String s1 = Optionull.map(pMessage.tag(), GuiMessageTag::logTag);
        if (s1 != null) {
            LOGGER.info("[{}] [CHAT] {}", s1, s);
        } else {
            LOGGER.info("[CHAT] {}", s);
        }
    }

    private void addMessageToDisplayQueue(GuiMessage pMessage, boolean isRepeated) {
        int i = Mth.floor((double)this.getWidth() / this.getScale());
        GuiMessageTag.Icon guimessagetag$icon = pMessage.icon();
        if (guimessagetag$icon != null) {
            i -= guimessagetag$icon.width + 4 + 2;
        }

        List<FormattedCharSequence> list = ComponentRenderUtils.wrapComponents(pMessage.content(), i, this.minecraft.font);
        boolean flag = this.isChatFocused();

        if (!this.allMessages.isEmpty() && FunctionsManager.getFunctionByName("Integrated UI animations").getSettingByName("Integrated UI animations").getSelectedOptionsList().contains("Smooth chat")) {
            boolean isRepeated1 = this.allMessages.getFirst().content().getString().equals(pMessage.content().getString());

            if (isRepeated1) {

                Component originalContent = pMessage.content();
                Style originalStyle = originalContent.getStyle();

                int j = 1;

                String firstElement = allMessages.getFirst().content().getString();

                for(GuiMessage guiMessage : allMessages) {

                    if(guiMessage.content().getString().equals(firstElement))
                        j++;

                    else break;

                }

                Component newContent = originalContent.copy()
                        .append(Component.literal("\u00A77 x" + j).setStyle(originalStyle));

                GuiMessage modifiedMessage = new GuiMessage(
                        pMessage.addedTime(),
                        newContent,
                        pMessage.signature(),
                        pMessage.tag()
                );

                try {
                    this.trimmedMessages.removeFirst();
                } catch (Exception e) {

                }

                addMessageToDisplayQueue(modifiedMessage, true);

                return;

            }
        }

        for (int j = 0; j < list.size(); j++) {
            FormattedCharSequence formattedcharsequence = list.get(j);
            if (flag && this.chatScrollbarPos > 0) {
                this.newMessageSinceScroll = true;
                this.scrollChat(1);
            }

            boolean flag1 = j == list.size() - 1;
            this.trimmedMessages.add(0, new GuiMessage.Line(pMessage.addedTime(), formattedcharsequence, pMessage.tag(), flag1, new GuiMessage.Interpolation(isRepeated)));
        }

        while (this.trimmedMessages.size() > 100) {

            this.trimmedMessages.removeLast();

        }
    }

    private void addMessageToQueue(GuiMessage pMessage) {
        this.allMessages.add(0, pMessage);

        while (this.allMessages.size() > 100) {
            this.allMessages.removeLast();
        }
    }

    private void processMessageDeletionQueue() {
        int i = this.minecraft.gui.getGuiTicks();
        this.messageDeletionQueue.removeIf(delIn -> i >= delIn.deletableAfter() ? this.deleteMessageOrDelay(delIn.signature()) == null : false);
    }

    public void deleteMessage(MessageSignature pMessageSignature) {
        ChatComponent.DelayedMessageDeletion chatcomponent$delayedmessagedeletion = this.deleteMessageOrDelay(pMessageSignature);
        if (chatcomponent$delayedmessagedeletion != null) {
            this.messageDeletionQueue.add(chatcomponent$delayedmessagedeletion);
        }
    }

    @Nullable
    private ChatComponent.DelayedMessageDeletion deleteMessageOrDelay(MessageSignature pMessageSignature) {
        int i = this.minecraft.gui.getGuiTicks();
        ListIterator<GuiMessage> listiterator = this.allMessages.listIterator();

        while (listiterator.hasNext()) {
            GuiMessage guimessage = listiterator.next();
            if (pMessageSignature.equals(guimessage.signature())) {
                if (pMessageSignature.equals(LevelRenderer.loadVisibleChunksMessageId)) {
                    listiterator.remove();
                    this.refreshTrimmedMessages();
                    return null;
                }

                int j = guimessage.addedTime() + 60;
                if (i >= j) {
                    listiterator.set(this.createDeletedMarker(guimessage));
                    this.refreshTrimmedMessages();
                    return null;
                }

                return new ChatComponent.DelayedMessageDeletion(pMessageSignature, j);
            }
        }

        return null;
    }

    private GuiMessage createDeletedMarker(GuiMessage pMessage) {
        return new GuiMessage(pMessage.addedTime(), DELETED_CHAT_MESSAGE, null, GuiMessageTag.system());
    }

    public void rescaleChat() {
        this.resetChatScroll();
        this.refreshTrimmedMessages();
    }

    private void refreshTrimmedMessages() {
        this.trimmedMessages.clear();

        for (GuiMessage guimessage : Lists.reverse(this.allMessages)) {
            this.addMessageToDisplayQueue(guimessage, false);
        }
    }

    public ArrayListDeque<String> getRecentChat() {
        return this.recentChat;
    }

    public void addRecentChat(String pMessage) {
        if (!pMessage.equals(this.recentChat.peekLast())) {
            if (this.recentChat.size() >= 100) {
                this.recentChat.removeFirst();
            }

            this.recentChat.addLast(pMessage);
        }

        if (pMessage.startsWith("/")) {
            this.minecraft.commandHistory().addCommand(pMessage);
        }
    }

    public void resetChatScroll() {
        this.chatScrollbarPos = 0;
        this.newMessageSinceScroll = false;
    }

    public void scrollChat(int pPosInc) {
        this.chatScrollbarPos += pPosInc;
        int i = this.trimmedMessages.size();
        if (this.chatScrollbarPos > i - this.getLinesPerPage()) {
            this.chatScrollbarPos = i - this.getLinesPerPage();
        }

        if (this.chatScrollbarPos <= 0) {
            this.chatScrollbarPos = 0;
            this.newMessageSinceScroll = false;
        }
    }

    public boolean handleChatQueueClicked(double pMouseX, double pMouseY) {
        if (this.isChatFocused() && !this.minecraft.options.hideGui && !this.isChatHidden()) {
            ChatListener chatlistener = this.minecraft.getChatListener();
            if (chatlistener.queueSize() == 0L) {
                return false;
            } else {
                double d0 = pMouseX - 2.0;
                double d1 = (double)this.minecraft.getWindow().getGuiScaledHeight() - pMouseY - 40.0;
                if (d0 <= (double)Mth.floor((double)this.getWidth() / this.getScale()) && d1 < 0.0 && d1 > (double)Mth.floor(-9.0 * this.getScale())) {
                    chatlistener.acceptNextDelayedMessage();
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    @Nullable
    public Style getClickedComponentStyleAt(double pMouseX, double pMouseY) {
        double d0 = this.screenToChatX(pMouseX);
        double d1 = this.screenToChatY(pMouseY);
        int i = this.getMessageLineIndexAt(d0, d1);
        if (i >= 0 && i < this.trimmedMessages.size()) {
            GuiMessage.Line guimessage$line = this.trimmedMessages.get(i);
            return this.minecraft.font.getSplitter().componentStyleAtWidth(guimessage$line.content(), Mth.floor(d0));
        } else {
            return null;
        }
    }

    @Nullable
    public GuiMessageTag getMessageTagAt(double pMouseX, double pMouseY) {
        double d0 = this.screenToChatX(pMouseX);
        double d1 = this.screenToChatY(pMouseY);
        int i = this.getMessageEndIndexAt(d0, d1);
        if (i >= 0 && i < this.trimmedMessages.size()) {
            GuiMessage.Line guimessage$line = this.trimmedMessages.get(i);
            GuiMessageTag guimessagetag = guimessage$line.tag();
            if (guimessagetag != null && this.hasSelectedMessageTag(d0, guimessage$line, guimessagetag)) {
                return guimessagetag;
            }
        }

        return null;
    }

    private boolean hasSelectedMessageTag(double pX, GuiMessage.Line pLine, GuiMessageTag pTag) {
        if (pX < 0.0) {
            return true;
        } else {
            GuiMessageTag.Icon guimessagetag$icon = pTag.icon();
            if (guimessagetag$icon == null) {
                return false;
            } else {
                int i = this.getTagIconLeft(pLine);
                int j = i + guimessagetag$icon.width;
                return pX >= (double)i && pX <= (double)j;
            }
        }
    }

    private double screenToChatX(double pX) {
        return pX / this.getScale() - 4.0;
    }

    private double screenToChatY(double pY) {
        double d0 = (double)this.minecraft.getWindow().getGuiScaledHeight() - pY - 40.0;
        return d0 / (this.getScale() * (double)this.getLineHeight());
    }

    private int getMessageEndIndexAt(double pMouseX, double pMouseY) {
        int i = this.getMessageLineIndexAt(pMouseX, pMouseY);
        if (i == -1) {
            return -1;
        } else {
            while (i >= 0) {
                if (this.trimmedMessages.get(i).endOfEntry()) {
                    return i;
                }

                i--;
            }

            return i;
        }
    }

    private int getMessageLineIndexAt(double pMouseX, double pMouseY) {
        if (this.isChatFocused() && !this.isChatHidden()) {
            if (!(pMouseX < -4.0) && !(pMouseX > (double)Mth.floor((double)this.getWidth() / this.getScale()))) {
                int i = Math.min(this.getLinesPerPage(), this.trimmedMessages.size());
                if (pMouseY >= 0.0 && pMouseY < (double)i) {
                    int j = Mth.floor(pMouseY + (double)this.chatScrollbarPos);
                    if (j >= 0 && j < this.trimmedMessages.size()) {
                        return j;
                    }
                }

                return -1;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    public boolean isChatFocused() {
        return this.minecraft.screen instanceof ChatScreen;
    }

    public int getWidth() {
        int i = getWidth(this.minecraft.options.chatWidth().get());
        Window window = Minecraft.getInstance().getWindow();
        int j = (int)((double)(window.getWidth() - 3) / window.getGuiScale());
        return Mth.clamp(i, 0, j);
    }

    public int getHeight() {
        return getHeight(this.isChatFocused() ? this.minecraft.options.chatHeightFocused().get() : this.minecraft.options.chatHeightUnfocused().get());
    }

    public double getScale() {
        return this.minecraft.options.chatScale().get();
    }

    public static int getWidth(double pWidth) {
        int i = 320;
        int j = 40;
        return Mth.floor(pWidth * 280.0 + 40.0);
    }

    public static int getHeight(double pHeight) {
        int i = 180;
        int j = 20;
        return Mth.floor(pHeight * 160.0 + 20.0);
    }

    public static double defaultUnfocusedPct() {
        int i = 180;
        int j = 20;
        return 70.0 / (double)(getHeight(1.0) - 20);
    }

    public int getLinesPerPage() {
        return this.getHeight() / this.getLineHeight();
    }

    private int getLineHeight() {
        return (int)(9.0 * (this.minecraft.options.chatLineSpacing().get() + 1.0));
    }

    public ChatComponent.State storeState() {
        return new ChatComponent.State(List.copyOf(this.allMessages), List.copyOf(this.recentChat), List.copyOf(this.messageDeletionQueue));
    }

    public void restoreState(ChatComponent.State pState) {
        this.recentChat.clear();
        this.recentChat.addAll(pState.history);
        this.messageDeletionQueue.clear();
        this.messageDeletionQueue.addAll(pState.delayedMessageDeletions);
        this.allMessages.clear();
        this.allMessages.addAll(pState.messages);
        this.refreshTrimmedMessages();
    }

    static record DelayedMessageDeletion(MessageSignature signature, int deletableAfter) {
    }

    public static class State {
        final List<GuiMessage> messages;
        final List<String> history;
        final List<ChatComponent.DelayedMessageDeletion> delayedMessageDeletions;

        public State(List<GuiMessage> pMessages, List<String> pHistory, List<ChatComponent.DelayedMessageDeletion> pDelayedMessageDeletions) {
            this.messages = pMessages;
            this.history = pHistory;
            this.delayedMessageDeletions = pDelayedMessageDeletions;
        }
    }
}