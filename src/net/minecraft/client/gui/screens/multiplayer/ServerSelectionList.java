package net.minecraft.client.gui.screens.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerSelectionList extends ObjectSelectionList<ServerSelectionList.Entry> {
    static final ResourceLocation INCOMPATIBLE_SPRITE = ResourceLocation.withDefaultNamespace("server_list/incompatible");
    static final ResourceLocation UNREACHABLE_SPRITE = ResourceLocation.withDefaultNamespace("server_list/unreachable");
    static final ResourceLocation PING_1_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_1");
    static final ResourceLocation PING_2_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_2");
    static final ResourceLocation PING_3_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_3");
    static final ResourceLocation PING_4_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_4");
    static final ResourceLocation PING_5_SPRITE = ResourceLocation.withDefaultNamespace("server_list/ping_5");
    static final ResourceLocation PINGING_1_SPRITE = ResourceLocation.withDefaultNamespace("server_list/pinging_1");
    static final ResourceLocation PINGING_2_SPRITE = ResourceLocation.withDefaultNamespace("server_list/pinging_2");
    static final ResourceLocation PINGING_3_SPRITE = ResourceLocation.withDefaultNamespace("server_list/pinging_3");
    static final ResourceLocation PINGING_4_SPRITE = ResourceLocation.withDefaultNamespace("server_list/pinging_4");
    static final ResourceLocation PINGING_5_SPRITE = ResourceLocation.withDefaultNamespace("server_list/pinging_5");
    static final ResourceLocation JOIN_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("server_list/join_highlighted");
    static final ResourceLocation JOIN_SPRITE = ResourceLocation.withDefaultNamespace("server_list/join");
    static final ResourceLocation MOVE_UP_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("server_list/move_up_highlighted");
    static final ResourceLocation MOVE_UP_SPRITE = ResourceLocation.withDefaultNamespace("server_list/move_up");
    static final ResourceLocation MOVE_DOWN_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("server_list/move_down_highlighted");
    static final ResourceLocation MOVE_DOWN_SPRITE = ResourceLocation.withDefaultNamespace("server_list/move_down");
    static final Logger LOGGER = LogUtils.getLogger();
    static final ThreadPoolExecutor THREAD_POOL = new ScheduledThreadPoolExecutor(
        5,
        new ThreadFactoryBuilder()
            .setNameFormat("Server Pinger #%d")
            .setDaemon(true)
            .setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER))
            .build()
    );
    static final Component SCANNING_LABEL = Component.translatable("lanServer.scanning");
    static final Component CANT_RESOLVE_TEXT = Component.translatable("multiplayer.status.cannot_resolve").withColor(-65536);
    static final Component CANT_CONNECT_TEXT = Component.translatable("multiplayer.status.cannot_connect").withColor(-65536);
    static final Component INCOMPATIBLE_STATUS = Component.translatable("multiplayer.status.incompatible");
    static final Component NO_CONNECTION_STATUS = Component.translatable("multiplayer.status.no_connection");
    static final Component PINGING_STATUS = Component.translatable("multiplayer.status.pinging");
    static final Component ONLINE_STATUS = Component.translatable("multiplayer.status.online");
    private final JoinMultiplayerScreen screen;
    private final List<ServerSelectionList.OnlineServerEntry> onlineServers = Lists.newArrayList();
    private final ServerSelectionList.Entry lanHeader = new ServerSelectionList.LANHeader();
    private final List<ServerSelectionList.NetworkServerEntry> networkServers = Lists.newArrayList();

    public ServerSelectionList(JoinMultiplayerScreen pScreen, Minecraft pMinecraft, int pWidth, int pHeight, int pY, int pItemHeight) {
        super(pMinecraft, pWidth, pHeight, pY, pItemHeight);
        this.screen = pScreen;
    }

    private void refreshEntries() {
        this.clearEntries();
        this.onlineServers.forEach(p_169979_ -> this.addEntry(p_169979_));
        this.addEntry(this.lanHeader);
        this.networkServers.forEach(p_169976_ -> this.addEntry(p_169976_));
    }

    public void setSelected(@Nullable ServerSelectionList.Entry pEntry) {
        super.setSelected(pEntry);
        this.screen.onSelectedChange();
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        ServerSelectionList.Entry serverselectionlist$entry = this.getSelected();
        return serverselectionlist$entry != null && serverselectionlist$entry.keyPressed(pKeyCode, pScanCode, pModifiers)
            || super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    public void updateOnlineServers(ServerList pServers) {
        this.onlineServers.clear();

        for (int i = 0; i < pServers.size(); i++) {
            this.onlineServers.add(new ServerSelectionList.OnlineServerEntry(this.screen, pServers.get(i)));
        }

        this.refreshEntries();
    }

    public void updateNetworkServers(List<LanServer> pLanServers) {
        int i = pLanServers.size() - this.networkServers.size();
        this.networkServers.clear();

        for (LanServer lanserver : pLanServers) {
            this.networkServers.add(new ServerSelectionList.NetworkServerEntry(this.screen, lanserver));
        }

        this.refreshEntries();

        for (int i1 = this.networkServers.size() - i; i1 < this.networkServers.size(); i1++) {
            ServerSelectionList.NetworkServerEntry serverselectionlist$networkserverentry = this.networkServers.get(i1);
            int j = i1 - this.networkServers.size() + this.children().size();
            int k = this.getRowTop(j);
            int l = this.getRowBottom(j);
            if (l >= this.getY() && k <= this.getBottom()) {
                this.minecraft.getNarrator().say(Component.translatable("multiplayer.lan.server_found", serverselectionlist$networkserverentry.getServerNarration()));
            }
        }
    }

    @Override
    public int getRowWidth() {
        return 305;
    }

    public void removed() {
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class Entry extends ObjectSelectionList.Entry<ServerSelectionList.Entry> implements AutoCloseable {
        @Override
        public void close() {
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class LANHeader extends ServerSelectionList.Entry {
        private final Minecraft minecraft = Minecraft.getInstance();

        @Override
        public void render(
            GuiGraphics p_281475_,
            int p_282477_,
            int p_282819_,
            int p_282001_,
            int p_281911_,
            int p_283126_,
            int p_282303_,
            int p_281998_,
            boolean p_282625_,
            float p_281811_
        ) {
            int i = p_282819_ + p_283126_ / 2 - 9 / 2;
            p_281475_.drawString(
                this.minecraft.font,
                ServerSelectionList.SCANNING_LABEL,
                this.minecraft.screen.width / 2 - this.minecraft.font.width(ServerSelectionList.SCANNING_LABEL) / 2,
                i,
                -1
            );
            String s = LoadingDotsText.get(Util.getMillis());
            p_281475_.drawString(this.minecraft.font, s, this.minecraft.screen.width / 2 - this.minecraft.font.width(s) / 2, i + 9, -8355712);
        }

        @Override
        public Component getNarration() {
            return ServerSelectionList.SCANNING_LABEL;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class NetworkServerEntry extends ServerSelectionList.Entry {
        private static final int ICON_WIDTH = 32;
        private static final Component LAN_SERVER_HEADER = Component.translatable("lanServer.title");
        private static final Component HIDDEN_ADDRESS_TEXT = Component.translatable("selectServer.hiddenAddress");
        private final JoinMultiplayerScreen screen;
        protected final Minecraft minecraft;
        protected final LanServer serverData;
        private long lastClickTime;

        protected NetworkServerEntry(JoinMultiplayerScreen pScreen, LanServer pServerData) {
            this.screen = pScreen;
            this.serverData = pServerData;
            this.minecraft = Minecraft.getInstance();
        }

        @Override
        public void render(
            GuiGraphics p_282600_,
            int p_282649_,
            int p_283641_,
            int p_282277_,
            int p_283034_,
            int p_281533_,
            int p_282746_,
            int p_281454_,
            boolean p_283673_,
            float p_282694_
        ) {
            p_282600_.drawString(this.minecraft.font, LAN_SERVER_HEADER, p_282277_ + 32 + 3, p_283641_ + 1, -1);
            p_282600_.drawString(this.minecraft.font, this.serverData.getMotd(), p_282277_ + 32 + 3, p_283641_ + 12, -8355712);
            if (this.minecraft.options.hideServerAddress) {
                p_282600_.drawString(this.minecraft.font, HIDDEN_ADDRESS_TEXT, p_282277_ + 32 + 3, p_283641_ + 12 + 11, 3158064);
            } else {
                p_282600_.drawString(this.minecraft.font, this.serverData.getAddress(), p_282277_ + 32 + 3, p_283641_ + 12 + 11, 3158064);
            }
        }

        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            this.screen.setSelected(this);
            if (Util.getMillis() - this.lastClickTime < 250L) {
                this.screen.joinSelectedServer();
            }

            this.lastClickTime = Util.getMillis();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        }

        public LanServer getServerData() {
            return this.serverData;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", this.getServerNarration());
        }

        public Component getServerNarration() {
            return Component.empty().append(LAN_SERVER_HEADER).append(CommonComponents.SPACE).append(this.serverData.getMotd());
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class OnlineServerEntry extends ServerSelectionList.Entry {
        private static final int ICON_WIDTH = 32;
        private static final int ICON_HEIGHT = 32;
        private static final int SPACING = 5;
        private static final int STATUS_ICON_WIDTH = 10;
        private static final int STATUS_ICON_HEIGHT = 8;
        private final JoinMultiplayerScreen screen;
        private final Minecraft minecraft;
        private final ServerData serverData;
        private final FaviconTexture icon;
        @Nullable
        private byte[] lastIconBytes;
        private long lastClickTime;
        @Nullable
        private List<Component> onlinePlayersTooltip;
        @Nullable
        private ResourceLocation statusIcon;
        @Nullable
        private Component statusIconTooltip;

        protected OnlineServerEntry(final JoinMultiplayerScreen pScreen, final ServerData pServerData) {
            this.screen = pScreen;
            this.serverData = pServerData;
            this.minecraft = Minecraft.getInstance();
            this.icon = FaviconTexture.forServer(this.minecraft.getTextureManager(), pServerData.ip);
            this.refreshStatus();
        }

        @Override
        public void render(
            GuiGraphics p_281406_,
            int p_281506_,
            int p_282921_,
            int p_281363_,
            int p_283596_,
            int p_281630_,
            int p_283567_,
            int p_282864_,
            boolean p_282999_,
            float p_281423_
        ) {
            if (this.serverData.state() == ServerData.State.INITIAL) {
                this.serverData.setState(ServerData.State.PINGING);
                this.serverData.motd = CommonComponents.EMPTY;
                this.serverData.status = CommonComponents.EMPTY;
                ServerSelectionList.THREAD_POOL
                    .submit(
                        () -> {
                            try {
                                this.screen
                                    .getPinger()
                                    .pingServer(
                                        this.serverData,
                                        () -> this.minecraft.execute(this::updateServerList),
                                        () -> {
                                            this.serverData
                                                .setState(
                                                    this.serverData.protocol == SharedConstants.getCurrentVersion().getProtocolVersion()
                                                        ? ServerData.State.SUCCESSFUL
                                                        : ServerData.State.INCOMPATIBLE
                                                );
                                            this.minecraft.execute(this::refreshStatus);
                                        }
                                    );
                            } catch (UnknownHostException unknownhostexception) {
                                this.serverData.setState(ServerData.State.UNREACHABLE);
                                this.serverData.motd = ServerSelectionList.CANT_RESOLVE_TEXT;
                                this.minecraft.execute(this::refreshStatus);
                            } catch (Exception exception) {
                                this.serverData.setState(ServerData.State.UNREACHABLE);
                                this.serverData.motd = ServerSelectionList.CANT_CONNECT_TEXT;
                                this.minecraft.execute(this::refreshStatus);
                            }
                        }
                    );
            }

            p_281406_.drawString(this.minecraft.font, this.serverData.name, p_281363_ + 32 + 3, p_282921_ + 1, -1);
            List<FormattedCharSequence> list = this.minecraft.font.split(this.serverData.motd, p_283596_ - 32 - 2);

            for (int i = 0; i < Math.min(list.size(), 2); i++) {
                p_281406_.drawString(this.minecraft.font, list.get(i), p_281363_ + 32 + 3, p_282921_ + 12 + 9 * i, -8355712);
            }

            this.drawIcon(p_281406_, p_281363_, p_282921_, this.icon.textureLocation());
            if (this.serverData.state() == ServerData.State.PINGING) {
                int j1 = (int)(Util.getMillis() / 100L + (long)(p_281506_ * 2) & 7L);
                if (j1 > 4) {
                    j1 = 8 - j1;
                }
                this.statusIcon = switch (j1) {
                    case 1 -> ServerSelectionList.PINGING_2_SPRITE;
                    case 2 -> ServerSelectionList.PINGING_3_SPRITE;
                    case 3 -> ServerSelectionList.PINGING_4_SPRITE;
                    case 4 -> ServerSelectionList.PINGING_5_SPRITE;
                    default -> ServerSelectionList.PINGING_1_SPRITE;
                };
            }

            int k1 = p_281363_ + p_283596_ - 10 - 5;
            if (this.statusIcon != null) {
                p_281406_.blitSprite(RenderType::guiTextured, this.statusIcon, k1, p_282921_, 10, 8);
            }

            byte[] abyte = this.serverData.getIconBytes();
            if (!Arrays.equals(abyte, this.lastIconBytes)) {
                if (this.uploadServerIcon(abyte)) {
                    this.lastIconBytes = abyte;
                } else {
                    this.serverData.setIconBytes(null);
                    this.updateServerList();
                }
            }

            Component component = (Component)(this.serverData.state() == ServerData.State.INCOMPATIBLE
                ? this.serverData.version.copy().withStyle(ChatFormatting.RED)
                : this.serverData.status);
            int j = this.minecraft.font.width(component);
            int k = k1 - j - 5;
            p_281406_.drawString(this.minecraft.font, component, k, p_282921_ + 1, -8355712);
            if (this.statusIconTooltip != null && p_283567_ >= k1 && p_283567_ <= k1 + 10 && p_282864_ >= p_282921_ && p_282864_ <= p_282921_ + 8) {
                this.screen.setTooltipForNextRenderPass(this.statusIconTooltip);
            } else if (this.onlinePlayersTooltip != null && p_283567_ >= k && p_283567_ <= k + j && p_282864_ >= p_282921_ && p_282864_ <= p_282921_ - 1 + 9) {
                this.screen.setTooltipForNextRenderPass(Lists.transform(this.onlinePlayersTooltip, Component::getVisualOrderText));
            }

            if (this.minecraft.options.touchscreen().get() || p_282999_) {
                p_281406_.fill(p_281363_, p_282921_, p_281363_ + 32, p_282921_ + 32, -1601138544);
                int l = p_283567_ - p_281363_;
                int i1 = p_282864_ - p_282921_;
                if (this.canJoin()) {
                    if (l < 32 && l > 16) {
                        p_281406_.blitSprite(RenderType::guiTextured, ServerSelectionList.JOIN_HIGHLIGHTED_SPRITE, p_281363_, p_282921_, 32, 32);
                    } else {
                        p_281406_.blitSprite(RenderType::guiTextured, ServerSelectionList.JOIN_SPRITE, p_281363_, p_282921_, 32, 32);
                    }
                }

                if (p_281506_ > 0) {
                    if (l < 16 && i1 < 16) {
                        p_281406_.blitSprite(RenderType::guiTextured, ServerSelectionList.MOVE_UP_HIGHLIGHTED_SPRITE, p_281363_, p_282921_, 32, 32);
                    } else {
                        p_281406_.blitSprite(RenderType::guiTextured, ServerSelectionList.MOVE_UP_SPRITE, p_281363_, p_282921_, 32, 32);
                    }
                }

                if (p_281506_ < this.screen.getServers().size() - 1) {
                    if (l < 16 && i1 > 16) {
                        p_281406_.blitSprite(RenderType::guiTextured, ServerSelectionList.MOVE_DOWN_HIGHLIGHTED_SPRITE, p_281363_, p_282921_, 32, 32);
                    } else {
                        p_281406_.blitSprite(RenderType::guiTextured, ServerSelectionList.MOVE_DOWN_SPRITE, p_281363_, p_282921_, 32, 32);
                    }
                }
            }
        }

        private void refreshStatus() {
            this.onlinePlayersTooltip = null;
            switch (this.serverData.state()) {
                case INITIAL:
                case PINGING:
                    this.statusIcon = ServerSelectionList.PING_1_SPRITE;
                    this.statusIconTooltip = ServerSelectionList.PINGING_STATUS;
                    break;
                case INCOMPATIBLE:
                    this.statusIcon = ServerSelectionList.INCOMPATIBLE_SPRITE;
                    this.statusIconTooltip = ServerSelectionList.INCOMPATIBLE_STATUS;
                    this.onlinePlayersTooltip = this.serverData.playerList;
                    break;
                case UNREACHABLE:
                    this.statusIcon = ServerSelectionList.UNREACHABLE_SPRITE;
                    this.statusIconTooltip = ServerSelectionList.NO_CONNECTION_STATUS;
                    break;
                case SUCCESSFUL:
                    if (this.serverData.ping < 150L) {
                        this.statusIcon = ServerSelectionList.PING_5_SPRITE;
                    } else if (this.serverData.ping < 300L) {
                        this.statusIcon = ServerSelectionList.PING_4_SPRITE;
                    } else if (this.serverData.ping < 600L) {
                        this.statusIcon = ServerSelectionList.PING_3_SPRITE;
                    } else if (this.serverData.ping < 1000L) {
                        this.statusIcon = ServerSelectionList.PING_2_SPRITE;
                    } else {
                        this.statusIcon = ServerSelectionList.PING_1_SPRITE;
                    }

                    this.statusIconTooltip = Component.translatable("multiplayer.status.ping", this.serverData.ping);
                    this.onlinePlayersTooltip = this.serverData.playerList;
            }
        }

        public void updateServerList() {
            this.screen.getServers().save();
        }

        protected void drawIcon(GuiGraphics pGuiGraphics, int pX, int pY, ResourceLocation pIcon) {
            pGuiGraphics.blit(RenderType::guiTextured, pIcon, pX, pY, 0.0F, 0.0F, 32, 32, 32, 32);
        }

        private boolean canJoin() {
            return true;
        }

        private boolean uploadServerIcon(@Nullable byte[] pIconBytes) {
            if (pIconBytes == null) {
                this.icon.clear();
            } else {
                try {
                    this.icon.upload(NativeImage.read(pIconBytes));
                } catch (Throwable throwable) {
                    ServerSelectionList.LOGGER.error("Invalid icon for server {} ({})", this.serverData.name, this.serverData.ip, throwable);
                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
            if (Screen.hasShiftDown()) {
                ServerSelectionList serverselectionlist = this.screen.serverSelectionList;
                int i = serverselectionlist.children().indexOf(this);
                if (i == -1) {
                    return true;
                }

                if (pKeyCode == 264 && i < this.screen.getServers().size() - 1 || pKeyCode == 265 && i > 0) {
                    this.swap(i, pKeyCode == 264 ? i + 1 : i - 1);
                    return true;
                }
            }

            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
        }

        private void swap(int pPos1, int pPos2) {
            this.screen.getServers().swap(pPos1, pPos2);
            this.screen.serverSelectionList.updateOnlineServers(this.screen.getServers());
            ServerSelectionList.Entry serverselectionlist$entry = this.screen.serverSelectionList.children().get(pPos2);
            this.screen.serverSelectionList.setSelected(serverselectionlist$entry);
            ServerSelectionList.this.ensureVisible(serverselectionlist$entry);
        }

        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            double d0 = pMouseX - (double)ServerSelectionList.this.getRowLeft();
            double d1 = pMouseY - (double)ServerSelectionList.this.getRowTop(ServerSelectionList.this.children().indexOf(this));
            if (d0 <= 32.0) {
                if (d0 < 32.0 && d0 > 16.0 && this.canJoin()) {
                    this.screen.setSelected(this);
                    this.screen.joinSelectedServer();
                    return true;
                }

                int i = this.screen.serverSelectionList.children().indexOf(this);
                if (d0 < 16.0 && d1 < 16.0 && i > 0) {
                    this.swap(i, i - 1);
                    return true;
                }

                if (d0 < 16.0 && d1 > 16.0 && i < this.screen.getServers().size() - 1) {
                    this.swap(i, i + 1);
                    return true;
                }
            }

            this.screen.setSelected(this);
            if (Util.getMillis() - this.lastClickTime < 250L) {
                this.screen.joinSelectedServer();
            }

            this.lastClickTime = Util.getMillis();
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        }

        public ServerData getServerData() {
            return this.serverData;
        }

        @Override
        public Component getNarration() {
            MutableComponent mutablecomponent = Component.empty();
            mutablecomponent.append(Component.translatable("narrator.select", this.serverData.name));
            mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
            switch (this.serverData.state()) {
                case PINGING:
                    mutablecomponent.append(ServerSelectionList.PINGING_STATUS);
                    break;
                case INCOMPATIBLE:
                    mutablecomponent.append(ServerSelectionList.INCOMPATIBLE_STATUS);
                    mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
                    mutablecomponent.append(Component.translatable("multiplayer.status.version.narration", this.serverData.version));
                    mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
                    mutablecomponent.append(Component.translatable("multiplayer.status.motd.narration", this.serverData.motd));
                    break;
                case UNREACHABLE:
                    mutablecomponent.append(ServerSelectionList.NO_CONNECTION_STATUS);
                    break;
                default:
                    mutablecomponent.append(ServerSelectionList.ONLINE_STATUS);
                    mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
                    mutablecomponent.append(Component.translatable("multiplayer.status.ping.narration", this.serverData.ping));
                    mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
                    mutablecomponent.append(Component.translatable("multiplayer.status.motd.narration", this.serverData.motd));
                    if (this.serverData.players != null) {
                        mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
                        mutablecomponent.append(
                            Component.translatable(
                                "multiplayer.status.player_count.narration", this.serverData.players.online(), this.serverData.players.max()
                            )
                        );
                        mutablecomponent.append(CommonComponents.NARRATION_SEPARATOR);
                        mutablecomponent.append(ComponentUtils.formatList(this.serverData.playerList, Component.literal(", ")));
                    }
            }

            return mutablecomponent;
        }

        @Override
        public void close() {
            this.icon.close();
        }
    }
}