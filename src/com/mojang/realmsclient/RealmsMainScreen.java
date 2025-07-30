package com.mojang.realmsclient;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.mojang.realmsclient.client.Ping;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerPlayerLists;
import com.mojang.realmsclient.dto.RegionPingResult;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.RealmsServerList;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsCreateRealmScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPendingInvitesScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.task.DataFetcher;
import com.mojang.realmsclient.util.RealmsPersistence;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientActivePlayersTooltip;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsMainScreen extends RealmsScreen {
    static final ResourceLocation INFO_SPRITE = ResourceLocation.withDefaultNamespace("icon/info");
    static final ResourceLocation NEW_REALM_SPRITE = ResourceLocation.withDefaultNamespace("icon/new_realm");
    static final ResourceLocation EXPIRED_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/expired");
    static final ResourceLocation EXPIRES_SOON_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/expires_soon");
    static final ResourceLocation OPEN_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/open");
    static final ResourceLocation CLOSED_SPRITE = ResourceLocation.withDefaultNamespace("realm_status/closed");
    private static final ResourceLocation INVITE_SPRITE = ResourceLocation.withDefaultNamespace("icon/invite");
    private static final ResourceLocation NEWS_SPRITE = ResourceLocation.withDefaultNamespace("icon/news");
    public static final ResourceLocation HARDCORE_MODE_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/hardcore_full");
    static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation LOGO_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/title/realms.png");
    private static final ResourceLocation NO_REALMS_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/realms/no_realms.png");
    private static final Component TITLE = Component.translatable("menu.online");
    private static final Component LOADING_TEXT = Component.translatable("mco.selectServer.loading");
    static final Component SERVER_UNITIALIZED_TEXT = Component.translatable("mco.selectServer.uninitialized");
    static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredList");
    private static final Component SUBSCRIPTION_RENEW_TEXT = Component.translatable("mco.selectServer.expiredRenew");
    static final Component TRIAL_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredTrial");
    private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
    private static final Component LEAVE_SERVER_TEXT = Component.translatable("mco.selectServer.leave");
    private static final Component CONFIGURE_SERVER_TEXT = Component.translatable("mco.selectServer.configure");
    static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
    static final Component SERVER_EXPIRES_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
    static final Component SERVER_EXPIRES_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
    static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
    static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
    static final Component UNITIALIZED_WORLD_NARRATION = Component.translatable("gui.narrate.button", SERVER_UNITIALIZED_TEXT);
    private static final Component NO_REALMS_TEXT = Component.translatable("mco.selectServer.noRealms");
    private static final Component NO_PENDING_INVITES = Component.translatable("mco.invites.nopending");
    private static final Component PENDING_INVITES = Component.translatable("mco.invites.pending");
    private static final Component INCOMPATIBLE_POPUP_TITLE = Component.translatable("mco.compatibility.incompatible.popup.title");
    private static final Component INCOMPATIBLE_RELEASE_TYPE_POPUP_MESSAGE = Component.translatable("mco.compatibility.incompatible.releaseType.popup.message");
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_COLUMNS = 3;
    private static final int BUTTON_SPACING = 4;
    private static final int CONTENT_WIDTH = 308;
    private static final int LOGO_WIDTH = 128;
    private static final int LOGO_HEIGHT = 34;
    private static final int LOGO_TEXTURE_WIDTH = 128;
    private static final int LOGO_TEXTURE_HEIGHT = 64;
    private static final int LOGO_PADDING = 5;
    private static final int HEADER_HEIGHT = 44;
    private static final int FOOTER_PADDING = 11;
    private static final int NEW_REALM_SPRITE_WIDTH = 40;
    private static final int NEW_REALM_SPRITE_HEIGHT = 20;
    private static final int ENTRY_WIDTH = 216;
    private static final int ITEM_HEIGHT = 36;
    private static final boolean SNAPSHOT = !SharedConstants.getCurrentVersion().isStable();
    private static boolean snapshotToggle = SNAPSHOT;
    private final CompletableFuture<RealmsAvailability.Result> availability = RealmsAvailability.get();
    @Nullable
    private DataFetcher.Subscription dataSubscription;
    private final Set<UUID> handledSeenNotifications = new HashSet<>();
    private static boolean regionsPinged;
    private final RateLimiter inviteNarrationLimiter;
    private final Screen lastScreen;
    private Button playButton;
    private Button backButton;
    private Button renewButton;
    private Button configureButton;
    private Button leaveButton;
    RealmsMainScreen.RealmSelectionList realmSelectionList;
    RealmsServerList serverList;
    List<RealmsServer> availableSnapshotServers = List.of();
    RealmsServerPlayerLists onlinePlayersPerRealm = new RealmsServerPlayerLists();
    private volatile boolean trialsAvailable;
    @Nullable
    private volatile String newsLink;
    long lastClickTime;
    final List<RealmsNotification> notifications = new ArrayList<>();
    private Button addRealmButton;
    private RealmsMainScreen.NotificationButton pendingInvitesButton;
    private RealmsMainScreen.NotificationButton newsButton;
    private RealmsMainScreen.LayoutState activeLayoutState;
    @Nullable
    private HeaderAndFooterLayout layout;

    public RealmsMainScreen(Screen pLastScreen) {
        super(TITLE);
        this.lastScreen = pLastScreen;
        this.inviteNarrationLimiter = RateLimiter.create(0.016666668F);
    }

    @Override
    public void init() {
        this.serverList = new RealmsServerList(this.minecraft);
        this.realmSelectionList = new RealmsMainScreen.RealmSelectionList();
        Component component = Component.translatable("mco.invites.title");
        this.pendingInvitesButton = new RealmsMainScreen.NotificationButton(
            component, INVITE_SPRITE, p_296029_ -> this.minecraft.setScreen(new RealmsPendingInvitesScreen(this, component))
        );
        Component component1 = Component.translatable("mco.news");
        this.newsButton = new RealmsMainScreen.NotificationButton(component1, NEWS_SPRITE, p_296035_ -> {
            String s = this.newsLink;
            if (s != null) {
                ConfirmLinkScreen.confirmLinkNow(this, s);
                if (this.newsButton.notificationCount() != 0) {
                    RealmsPersistence.RealmsPersistenceData realmspersistence$realmspersistencedata = RealmsPersistence.readFile();
                    realmspersistence$realmspersistencedata.hasUnreadNews = false;
                    RealmsPersistence.writeFile(realmspersistence$realmspersistencedata);
                    this.newsButton.setNotificationCount(0);
                }
            }
        });
        this.newsButton.setTooltip(Tooltip.create(component1));
        this.playButton = Button.builder(PLAY_TEXT, p_86659_ -> play(this.getSelectedServer(), this)).width(100).build();
        this.configureButton = Button.builder(CONFIGURE_SERVER_TEXT, p_86672_ -> this.configureClicked(this.getSelectedServer())).width(100).build();
        this.renewButton = Button.builder(SUBSCRIPTION_RENEW_TEXT, p_86622_ -> this.onRenew(this.getSelectedServer())).width(100).build();
        this.leaveButton = Button.builder(LEAVE_SERVER_TEXT, p_86679_ -> this.leaveClicked(this.getSelectedServer())).width(100).build();
        this.addRealmButton = Button.builder(Component.translatable("mco.selectServer.purchase"), p_296032_ -> this.openTrialAvailablePopup()).size(100, 20).build();
        this.backButton = Button.builder(CommonComponents.GUI_BACK, p_325094_ -> this.onClose()).width(100).build();
        if (RealmsClient.ENVIRONMENT == RealmsClient.Environment.STAGE) {
            this.addRenderableWidget(
                CycleButton.booleanBuilder(Component.literal("Snapshot"), Component.literal("Release"))
                    .create(5, 5, 100, 20, Component.literal("Realm"), (p_308035_, p_308036_) -> {
                        snapshotToggle = p_308036_;
                        this.availableSnapshotServers = List.of();
                        this.debugRefreshDataFetchers();
                    })
            );
        }

        this.updateLayout(RealmsMainScreen.LayoutState.LOADING);
        this.updateButtonStates();
        this.availability.thenAcceptAsync(p_296034_ -> {
            Screen screen = p_296034_.createErrorScreen(this.lastScreen);
            if (screen == null) {
                this.dataSubscription = this.initDataFetcher(this.minecraft.realmsDataFetcher());
            } else {
                this.minecraft.setScreen(screen);
            }
        }, this.screenExecutor);
    }

    public static boolean isSnapshot() {
        return SNAPSHOT && snapshotToggle;
    }

    @Override
    protected void repositionElements() {
        if (this.layout != null) {
            this.realmSelectionList.updateSize(this.width, this.layout);
            this.layout.arrangeElements();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private void updateLayout() {
        if (this.serverList.isEmpty() && this.availableSnapshotServers.isEmpty() && this.notifications.isEmpty()) {
            this.updateLayout(RealmsMainScreen.LayoutState.NO_REALMS);
        } else {
            this.updateLayout(RealmsMainScreen.LayoutState.LIST);
        }
    }

    private void updateLayout(RealmsMainScreen.LayoutState pLayoutState) {
        if (this.activeLayoutState != pLayoutState) {
            if (this.layout != null) {
                this.layout.visitWidgets(p_325098_ -> this.removeWidget(p_325098_));
            }

            this.layout = this.createLayout(pLayoutState);
            this.activeLayoutState = pLayoutState;
            this.layout.visitWidgets(p_325096_ -> {
                AbstractWidget abstractwidget = this.addRenderableWidget(p_325096_);
            });
            this.repositionElements();
        }
    }

    private HeaderAndFooterLayout createLayout(RealmsMainScreen.LayoutState pLayoutState) {
        HeaderAndFooterLayout headerandfooterlayout = new HeaderAndFooterLayout(this);
        headerandfooterlayout.setHeaderHeight(44);
        headerandfooterlayout.addToHeader(this.createHeader());
        Layout layout = this.createFooter(pLayoutState);
        layout.arrangeElements();
        headerandfooterlayout.setFooterHeight(layout.getHeight() + 22);
        headerandfooterlayout.addToFooter(layout);
        switch (pLayoutState) {
            case LOADING:
                headerandfooterlayout.addToContents(new LoadingDotsWidget(this.font, LOADING_TEXT));
                break;
            case NO_REALMS:
                headerandfooterlayout.addToContents(this.createNoRealmsContent());
                break;
            case LIST:
                headerandfooterlayout.addToContents(this.realmSelectionList);
        }

        return headerandfooterlayout;
    }

    private Layout createHeader() {
        int i = 90;
        LinearLayout linearlayout = LinearLayout.horizontal().spacing(4);
        linearlayout.defaultCellSetting().alignVerticallyMiddle();
        linearlayout.addChild(this.pendingInvitesButton);
        linearlayout.addChild(this.newsButton);
        LinearLayout linearlayout1 = LinearLayout.horizontal();
        linearlayout1.defaultCellSetting().alignVerticallyMiddle();
        linearlayout1.addChild(SpacerElement.width(90));
        linearlayout1.addChild(ImageWidget.texture(128, 34, LOGO_LOCATION, 128, 64), LayoutSettings::alignHorizontallyCenter);
        linearlayout1.addChild(new FrameLayout(90, 44)).addChild(linearlayout, LayoutSettings::alignHorizontallyRight);
        return linearlayout1;
    }

    private Layout createFooter(RealmsMainScreen.LayoutState pLayoutState) {
        GridLayout gridlayout = new GridLayout().spacing(4);
        GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(3);
        if (pLayoutState == RealmsMainScreen.LayoutState.LIST) {
            gridlayout$rowhelper.addChild(this.playButton);
            gridlayout$rowhelper.addChild(this.configureButton);
            gridlayout$rowhelper.addChild(this.renewButton);
            gridlayout$rowhelper.addChild(this.leaveButton);
        }

        gridlayout$rowhelper.addChild(this.addRealmButton);
        gridlayout$rowhelper.addChild(this.backButton);
        return gridlayout;
    }

    private LinearLayout createNoRealmsContent() {
        LinearLayout linearlayout = LinearLayout.vertical().spacing(8);
        linearlayout.defaultCellSetting().alignHorizontallyCenter();
        linearlayout.addChild(ImageWidget.texture(130, 64, NO_REALMS_LOCATION, 130, 64));
        FocusableTextWidget focusabletextwidget = new FocusableTextWidget(308, NO_REALMS_TEXT, this.font, false, 4);
        linearlayout.addChild(focusabletextwidget);
        return linearlayout;
    }

    void updateButtonStates() {
        RealmsServer realmsserver = this.getSelectedServer();
        this.addRealmButton.active = this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING;
        this.playButton.active = realmsserver != null && this.shouldPlayButtonBeActive(realmsserver);
        this.renewButton.active = realmsserver != null && this.shouldRenewButtonBeActive(realmsserver);
        this.leaveButton.active = realmsserver != null && this.shouldLeaveButtonBeActive(realmsserver);
        this.configureButton.active = realmsserver != null && this.shouldConfigureButtonBeActive(realmsserver);
    }

    boolean shouldPlayButtonBeActive(RealmsServer pRealmsServer) {
        boolean flag = !pRealmsServer.expired && pRealmsServer.state == RealmsServer.State.OPEN;
        return flag && (pRealmsServer.isCompatible() || pRealmsServer.needsUpgrade() || isSelfOwnedServer(pRealmsServer));
    }

    private boolean shouldRenewButtonBeActive(RealmsServer pRealmsServer) {
        return pRealmsServer.expired && isSelfOwnedServer(pRealmsServer);
    }

    private boolean shouldConfigureButtonBeActive(RealmsServer pRealmsServer) {
        return isSelfOwnedServer(pRealmsServer) && pRealmsServer.state != RealmsServer.State.UNINITIALIZED;
    }

    private boolean shouldLeaveButtonBeActive(RealmsServer pRealmsServer) {
        return !isSelfOwnedServer(pRealmsServer);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.dataSubscription != null) {
            this.dataSubscription.tick();
        }
    }

    public static void refreshPendingInvites() {
        Minecraft.getInstance().realmsDataFetcher().pendingInvitesTask.reset();
    }

    public static void refreshServerList() {
        Minecraft.getInstance().realmsDataFetcher().serverListUpdateTask.reset();
    }

    private void debugRefreshDataFetchers() {
        for (DataFetcher.Task<?> task : this.minecraft.realmsDataFetcher().getTasks()) {
            task.reset();
        }
    }

    private DataFetcher.Subscription initDataFetcher(RealmsDataFetcher pDataFetcher) {
        DataFetcher.Subscription datafetcher$subscription = pDataFetcher.dataFetcher.createSubscription();
        datafetcher$subscription.subscribe(pDataFetcher.serverListUpdateTask, p_308037_ -> {
            this.serverList.updateServersList(p_308037_.serverList());
            this.availableSnapshotServers = p_308037_.availableSnapshotServers();
            this.refreshListAndLayout();
            boolean flag = false;

            for (RealmsServer realmsserver : this.serverList) {
                if (this.isSelfOwnedNonExpiredServer(realmsserver)) {
                    flag = true;
                }
            }

            if (!regionsPinged && flag) {
                regionsPinged = true;
                this.pingRegions();
            }
        });
        callRealmsClient(RealmsClient::getNotifications, p_274622_ -> {
            this.notifications.clear();
            this.notifications.addAll(p_274622_);

            for (RealmsNotification realmsnotification : p_274622_) {
                if (realmsnotification instanceof RealmsNotification.InfoPopup realmsnotification$infopopup) {
                    PopupScreen popupscreen = realmsnotification$infopopup.buildScreen(this, this::dismissNotification);
                    if (popupscreen != null) {
                        this.minecraft.setScreen(popupscreen);
                        this.markNotificationsAsSeen(List.of(realmsnotification));
                        break;
                    }
                }
            }

            if (!this.notifications.isEmpty() && this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING) {
                this.refreshListAndLayout();
            }
        });
        datafetcher$subscription.subscribe(pDataFetcher.pendingInvitesTask, p_296027_ -> {
            this.pendingInvitesButton.setNotificationCount(p_296027_);
            this.pendingInvitesButton.setTooltip(p_296027_ == 0 ? Tooltip.create(NO_PENDING_INVITES) : Tooltip.create(PENDING_INVITES));
            if (p_296027_ > 0 && this.inviteNarrationLimiter.tryAcquire(1)) {
                this.minecraft.getNarrator().sayNow(Component.translatable("mco.configure.world.invite.narration", p_296027_));
            }
        });
        datafetcher$subscription.subscribe(pDataFetcher.trialAvailabilityTask, p_296031_ -> this.trialsAvailable = p_296031_);
        datafetcher$subscription.subscribe(pDataFetcher.onlinePlayersTask, p_340705_ -> this.onlinePlayersPerRealm = p_340705_);
        datafetcher$subscription.subscribe(pDataFetcher.newsTask, p_296037_ -> {
            pDataFetcher.newsManager.updateUnreadNews(p_296037_);
            this.newsLink = pDataFetcher.newsManager.newsLink();
            this.newsButton.setNotificationCount(pDataFetcher.newsManager.hasUnreadNews() ? Integer.MAX_VALUE : 0);
        });
        return datafetcher$subscription;
    }

    void markNotificationsAsSeen(Collection<RealmsNotification> pNotifications) {
        List<UUID> list = new ArrayList<>(pNotifications.size());

        for (RealmsNotification realmsnotification : pNotifications) {
            if (!realmsnotification.seen() && !this.handledSeenNotifications.contains(realmsnotification.uuid())) {
                list.add(realmsnotification.uuid());
            }
        }

        if (!list.isEmpty()) {
            callRealmsClient(p_274625_ -> {
                p_274625_.notificationsSeen(list);
                return null;
            }, p_274630_ -> this.handledSeenNotifications.addAll(list));
        }
    }

    private static <T> void callRealmsClient(RealmsMainScreen.RealmsCall<T> pCall, Consumer<T> pOnFinish) {
        Minecraft minecraft = Minecraft.getInstance();
        CompletableFuture.<T>supplyAsync(() -> {
            try {
                return pCall.request(RealmsClient.create(minecraft));
            } catch (RealmsServiceException realmsserviceexception) {
                throw new RuntimeException(realmsserviceexception);
            }
        }).thenAcceptAsync(pOnFinish, minecraft).exceptionally(p_274626_ -> {
            LOGGER.error("Failed to execute call to Realms Service", p_274626_);
            return null;
        });
    }

    private void refreshListAndLayout() {
        this.realmSelectionList.refreshEntries(this, this.getSelectedServer());
        this.updateLayout();
        this.updateButtonStates();
    }

    private void pingRegions() {
        new Thread(() -> {
            List<RegionPingResult> list = Ping.pingAllRegions();
            RealmsClient realmsclient = RealmsClient.create();
            PingResult pingresult = new PingResult();
            pingresult.pingResults = list;
            pingresult.realmIds = this.getOwnedNonExpiredRealmIds();

            try {
                realmsclient.sendPingResults(pingresult);
            } catch (Throwable throwable) {
                LOGGER.warn("Could not send ping result to Realms: ", throwable);
            }
        }).start();
    }

    private List<Long> getOwnedNonExpiredRealmIds() {
        List<Long> list = Lists.newArrayList();

        for (RealmsServer realmsserver : this.serverList) {
            if (this.isSelfOwnedNonExpiredServer(realmsserver)) {
                list.add(realmsserver.id);
            }
        }

        return list;
    }

    private void onRenew(@Nullable RealmsServer pRealmsServer) {
        if (pRealmsServer != null) {
            String s = CommonLinks.extendRealms(pRealmsServer.remoteSubscriptionId, this.minecraft.getUser().getProfileId(), pRealmsServer.expiredTrial);
            this.minecraft.keyboardHandler.setClipboard(s);
            Util.getPlatform().openUri(s);
        }
    }

    private void configureClicked(@Nullable RealmsServer pRealmsServer) {
        if (pRealmsServer != null && this.minecraft.isLocalPlayer(pRealmsServer.ownerUUID)) {
            this.minecraft.setScreen(new RealmsConfigureWorldScreen(this, pRealmsServer.id));
        }
    }

    private void leaveClicked(@Nullable RealmsServer pRealmsServer) {
        if (pRealmsServer != null && !this.minecraft.isLocalPlayer(pRealmsServer.ownerUUID)) {
            Component component = Component.translatable("mco.configure.world.leave.question.line1");
            this.minecraft.setScreen(RealmsPopups.infoPopupScreen(this, component, p_340701_ -> this.leaveServer(pRealmsServer)));
        }
    }

    @Nullable
    private RealmsServer getSelectedServer() {
        return this.realmSelectionList.getSelected() instanceof RealmsMainScreen.ServerEntry realmsmainscreen$serverentry ? realmsmainscreen$serverentry.getServer() : null;
    }

    private void leaveServer(final RealmsServer pServer) {
        (new Thread("Realms-leave-server") {
                @Override
                public void run() {
                    try {
                        RealmsClient realmsclient = RealmsClient.create();
                        realmsclient.uninviteMyselfFrom(pServer.id);
                        RealmsMainScreen.this.minecraft.execute(RealmsMainScreen::refreshServerList);
                    } catch (RealmsServiceException realmsserviceexception) {
                        RealmsMainScreen.LOGGER.error("Couldn't configure world", (Throwable)realmsserviceexception);
                        RealmsMainScreen.this.minecraft
                            .execute(() -> RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, RealmsMainScreen.this)));
                    }
                }
            })
            .start();
        this.minecraft.setScreen(this);
    }

    void dismissNotification(UUID pUuid) {
        callRealmsClient(p_274628_ -> {
            p_274628_.notificationsDismiss(List.of(pUuid));
            return null;
        }, p_274632_ -> {
            this.notifications.removeIf(p_274621_ -> p_274621_.dismissable() && pUuid.equals(p_274621_.uuid()));
            this.refreshListAndLayout();
        });
    }

    public void resetScreen() {
        this.realmSelectionList.setSelected(null);
        refreshServerList();
    }

    @Override
    public Component getNarrationMessage() {
        return (Component)(switch (this.activeLayoutState) {
            case LOADING -> CommonComponents.joinForNarration(super.getNarrationMessage(), LOADING_TEXT);
            case NO_REALMS -> CommonComponents.joinForNarration(super.getNarrationMessage(), NO_REALMS_TEXT);
            case LIST -> super.getNarrationMessage();
        });
    }

    @Override
    public void render(GuiGraphics p_282736_, int p_283347_, int p_282480_, float p_283485_) {
        super.render(p_282736_, p_283347_, p_282480_, p_283485_);
        if (isSnapshot()) {
            p_282736_.drawString(this.font, "Minecraft " + SharedConstants.getCurrentVersion().getName(), 2, this.height - 10, -1);
        }

        if (this.trialsAvailable && this.addRealmButton.active) {
            AddRealmPopupScreen.renderDiamond(p_282736_, this.addRealmButton);
        }

        switch (RealmsClient.ENVIRONMENT) {
            case STAGE:
                this.renderEnvironment(p_282736_, "STAGE!", -256);
                break;
            case LOCAL:
                this.renderEnvironment(p_282736_, "LOCAL!", 8388479);
        }
    }

    private void openTrialAvailablePopup() {
        this.minecraft.setScreen(new AddRealmPopupScreen(this, this.trialsAvailable));
    }

    public static void play(@Nullable RealmsServer pRealmsServer, Screen pLastScreen) {
        play(pRealmsServer, pLastScreen, false);
    }

    public static void play(@Nullable RealmsServer pRealmsServer, Screen pLastScreen, boolean pAllowSnapshots) {
        if (pRealmsServer != null) {
            if (!isSnapshot() || pAllowSnapshots || pRealmsServer.isMinigameActive()) {
                Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(pLastScreen, new GetServerDetailsTask(pLastScreen, pRealmsServer)));
                return;
            }

            switch (pRealmsServer.compatibility) {
                case COMPATIBLE:
                    Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(pLastScreen, new GetServerDetailsTask(pLastScreen, pRealmsServer)));
                    break;
                case UNVERIFIABLE:
                    confirmToPlay(
                        pRealmsServer,
                        pLastScreen,
                        Component.translatable("mco.compatibility.unverifiable.title").withColor(-171),
                        Component.translatable("mco.compatibility.unverifiable.message"),
                        CommonComponents.GUI_CONTINUE
                    );
                    break;
                case NEEDS_DOWNGRADE:
                    confirmToPlay(
                        pRealmsServer,
                        pLastScreen,
                        Component.translatable("selectWorld.backupQuestion.downgrade").withColor(-2142128),
                        Component.translatable(
                            "mco.compatibility.downgrade.description",
                            Component.literal(pRealmsServer.activeVersion).withColor(-171),
                            Component.literal(SharedConstants.getCurrentVersion().getName()).withColor(-171)
                        ),
                        Component.translatable("mco.compatibility.downgrade")
                    );
                    break;
                case NEEDS_UPGRADE:
                    upgradeRealmAndPlay(pRealmsServer, pLastScreen);
                    break;
                case INCOMPATIBLE:
                    Minecraft.getInstance()
                        .setScreen(
                            new PopupScreen.Builder(pLastScreen, INCOMPATIBLE_POPUP_TITLE)
                                .setMessage(
                                    Component.translatable(
                                        "mco.compatibility.incompatible.series.popup.message",
                                        Component.literal(pRealmsServer.activeVersion).withColor(-171),
                                        Component.literal(SharedConstants.getCurrentVersion().getName()).withColor(-171)
                                    )
                                )
                                .addButton(CommonComponents.GUI_BACK, PopupScreen::onClose)
                                .build()
                        );
                    break;
                case RELEASE_TYPE_INCOMPATIBLE:
                    Minecraft.getInstance()
                        .setScreen(
                            new PopupScreen.Builder(pLastScreen, INCOMPATIBLE_POPUP_TITLE)
                                .setMessage(INCOMPATIBLE_RELEASE_TYPE_POPUP_MESSAGE)
                                .addButton(CommonComponents.GUI_BACK, PopupScreen::onClose)
                                .build()
                        );
            }
        }
    }

    private static void confirmToPlay(RealmsServer pRealmsServer, Screen pLastScreen, Component pTitle, Component pMessage, Component pConfirmButton) {
        Minecraft.getInstance().setScreen(new PopupScreen.Builder(pLastScreen, pTitle).setMessage(pMessage).addButton(pConfirmButton, p_340704_ -> {
            Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(pLastScreen, new GetServerDetailsTask(pLastScreen, pRealmsServer)));
            refreshServerList();
        }).addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose).build());
    }

    private static void upgradeRealmAndPlay(RealmsServer pServer, Screen pLastScreen) {
        Component component = Component.translatable("mco.compatibility.upgrade.title").withColor(-171);
        Component component1 = Component.translatable("mco.compatibility.upgrade");
        Component component2 = Component.literal(pServer.activeVersion).withColor(-171);
        Component component3 = Component.literal(SharedConstants.getCurrentVersion().getName()).withColor(-171);
        Component component4 = isSelfOwnedServer(pServer)
            ? Component.translatable("mco.compatibility.upgrade.description", component2, component3)
            : Component.translatable("mco.compatibility.upgrade.friend.description", component2, component3);
        confirmToPlay(pServer, pLastScreen, component, component4, component1);
    }

    public static Component getVersionComponent(String pVersion, boolean pCompatible) {
        return getVersionComponent(pVersion, pCompatible ? -8355712 : -2142128);
    }

    public static Component getVersionComponent(String pVersion, int pColor) {
        return (Component)(StringUtils.isBlank(pVersion) ? CommonComponents.EMPTY : Component.literal(pVersion).withColor(pColor));
    }

    public static Component getGameModeComponent(int pGamemode, boolean pHardcore) {
        return (Component)(pHardcore ? Component.translatable("gameMode.hardcore").withColor(-65536) : GameType.byId(pGamemode).getLongDisplayName());
    }

    static boolean isSelfOwnedServer(RealmsServer pServer) {
        return Minecraft.getInstance().isLocalPlayer(pServer.ownerUUID);
    }

    private boolean isSelfOwnedNonExpiredServer(RealmsServer pServer) {
        return isSelfOwnedServer(pServer) && !pServer.expired;
    }

    private void renderEnvironment(GuiGraphics pGuiGraphics, String pText, int pColor) {
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate((float)(this.width / 2 - 25), 20.0F, 0.0F);
        pGuiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-20.0F));
        pGuiGraphics.pose().scale(1.5F, 1.5F, 1.5F);
        pGuiGraphics.drawString(this.font, pText, 0, 0, pColor);
        pGuiGraphics.pose().popPose();
    }

    @OnlyIn(Dist.CLIENT)
    class AvailableSnapshotEntry extends RealmsMainScreen.Entry {
        private static final Component START_SNAPSHOT_REALM = Component.translatable("mco.snapshot.start");
        private static final int TEXT_PADDING = 5;
        private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();
        private final RealmsServer parent;

        public AvailableSnapshotEntry(final RealmsServer pParent) {
            this.parent = pParent;
            this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.tooltip")));
        }

        @Override
        public void render(
            GuiGraphics p_310547_,
            int p_310078_,
            int p_309934_,
            int p_311127_,
            int p_310500_,
            int p_311639_,
            int p_311442_,
            int p_309408_,
            boolean p_312327_,
            float p_309422_
        ) {
            p_310547_.blitSprite(RenderType::guiTextured, RealmsMainScreen.NEW_REALM_SPRITE, p_311127_ - 5, p_309934_ + p_311639_ / 2 - 10, 40, 20);
            int i = p_309934_ + p_311639_ / 2 - 9 / 2;
            p_310547_.drawString(RealmsMainScreen.this.font, START_SNAPSHOT_REALM, p_311127_ + 40 - 2, i - 5, 8388479);
            p_310547_.drawString(
                RealmsMainScreen.this.font,
                Component.translatable("mco.snapshot.description", Objects.requireNonNullElse(this.parent.name, "unknown server")),
                p_311127_ + 40 - 2,
                i + 5,
                -8355712
            );
            this.tooltip.refreshTooltipForNextRenderPass(p_312327_, this.isFocused(), new ScreenRectangle(p_311127_, p_309934_, p_310500_, p_311639_));
        }

        @Override
        public boolean mouseClicked(double p_310312_, double p_309519_, int p_313156_) {
            this.addSnapshotRealm();
            return true;
        }

        @Override
        public boolean keyPressed(int p_309531_, int p_310526_, int p_312670_) {
            if (CommonInputs.selected(p_309531_)) {
                this.addSnapshotRealm();
                return false;
            } else {
                return super.keyPressed(p_309531_, p_310526_, p_312670_);
            }
        }

        private void addSnapshotRealm() {
            RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            RealmsMainScreen.this.minecraft
                .setScreen(
                    new PopupScreen.Builder(RealmsMainScreen.this, Component.translatable("mco.snapshot.createSnapshotPopup.title"))
                        .setMessage(Component.translatable("mco.snapshot.createSnapshotPopup.text"))
                        .addButton(
                            Component.translatable("mco.selectServer.create"),
                            p_357548_ -> RealmsMainScreen.this.minecraft.setScreen(new RealmsCreateRealmScreen(RealmsMainScreen.this, this.parent, true))
                        )
                        .addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose)
                        .build()
                );
        }

        @Override
        public Component getNarration() {
            return Component.translatable(
                "gui.narrate.button",
                CommonComponents.joinForNarration(
                    START_SNAPSHOT_REALM, Component.translatable("mco.snapshot.description", Objects.requireNonNullElse(this.parent.name, "unknown server"))
                )
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    class ButtonEntry extends RealmsMainScreen.Entry {
        private final Button button;

        public ButtonEntry(final Button pButton) {
            this.button = pButton;
        }

        @Override
        public boolean mouseClicked(double p_275240_, double p_275616_, int p_275528_) {
            this.button.mouseClicked(p_275240_, p_275616_, p_275528_);
            return super.mouseClicked(p_275240_, p_275616_, p_275528_);
        }

        @Override
        public boolean keyPressed(int p_275630_, int p_275328_, int p_275519_) {
            return this.button.keyPressed(p_275630_, p_275328_, p_275519_) ? true : super.keyPressed(p_275630_, p_275328_, p_275519_);
        }

        @Override
        public void render(
            GuiGraphics p_283542_,
            int p_282029_,
            int p_281480_,
            int p_281377_,
            int p_283160_,
            int p_281920_,
            int p_283267_,
            int p_281282_,
            boolean p_281269_,
            float p_282372_
        ) {
            this.button.setPosition(RealmsMainScreen.this.width / 2 - 75, p_281480_ + 4);
            this.button.render(p_283542_, p_283267_, p_281282_, p_282372_);
        }

        @Override
        public void setFocused(boolean p_311570_) {
            super.setFocused(p_311570_);
            this.button.setFocused(p_311570_);
        }

        @Override
        public Component getNarration() {
            return this.button.getMessage();
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class CrossButton extends ImageButton {
        private static final WidgetSprites SPRITES = new WidgetSprites(
            ResourceLocation.withDefaultNamespace("widget/cross_button"), ResourceLocation.withDefaultNamespace("widget/cross_button_highlighted")
        );

        protected CrossButton(Button.OnPress pOnPress, Component pMessage) {
            super(0, 0, 14, 14, SPRITES, pOnPress);
            this.setTooltip(Tooltip.create(pMessage));
        }
    }

    @OnlyIn(Dist.CLIENT)
    class EmptyEntry extends RealmsMainScreen.Entry {
        @Override
        public void render(
            GuiGraphics p_301870_,
            int p_301858_,
            int p_301868_,
            int p_301866_,
            int p_301860_,
            int p_301859_,
            int p_301864_,
            int p_301865_,
            boolean p_301869_,
            float p_301861_
        ) {
        }

        @Override
        public Component getNarration() {
            return Component.empty();
        }
    }

    @OnlyIn(Dist.CLIENT)
    abstract class Entry extends ObjectSelectionList.Entry<RealmsMainScreen.Entry> {
        protected static final int STATUS_LIGHT_WIDTH = 10;
        private static final int STATUS_LIGHT_HEIGHT = 28;
        protected static final int PADDING_X = 7;
        protected static final int PADDING_Y = 2;

        protected void renderStatusLights(RealmsServer pRealmsServer, GuiGraphics pGuiGraphics, int pX, int pY, int pMouseX, int pMouseY) {
            int i = pX - 10 - 7;
            int j = pY + 2;
            if (pRealmsServer.expired) {
                this.drawRealmStatus(pGuiGraphics, i, j, pMouseX, pMouseY, RealmsMainScreen.EXPIRED_SPRITE, () -> RealmsMainScreen.SERVER_EXPIRED_TOOLTIP);
            } else if (pRealmsServer.state == RealmsServer.State.CLOSED) {
                this.drawRealmStatus(pGuiGraphics, i, j, pMouseX, pMouseY, RealmsMainScreen.CLOSED_SPRITE, () -> RealmsMainScreen.SERVER_CLOSED_TOOLTIP);
            } else if (RealmsMainScreen.isSelfOwnedServer(pRealmsServer) && pRealmsServer.daysLeft < 7) {
                this.drawRealmStatus(
                    pGuiGraphics,
                    i,
                    j,
                    pMouseX,
                    pMouseY,
                    RealmsMainScreen.EXPIRES_SOON_SPRITE,
                    () -> {
                        if (pRealmsServer.daysLeft <= 0) {
                            return RealmsMainScreen.SERVER_EXPIRES_SOON_TOOLTIP;
                        } else {
                            return (Component)(pRealmsServer.daysLeft == 1
                                ? RealmsMainScreen.SERVER_EXPIRES_IN_DAY_TOOLTIP
                                : Component.translatable("mco.selectServer.expires.days", pRealmsServer.daysLeft));
                        }
                    }
                );
            } else if (pRealmsServer.state == RealmsServer.State.OPEN) {
                this.drawRealmStatus(pGuiGraphics, i, j, pMouseX, pMouseY, RealmsMainScreen.OPEN_SPRITE, () -> RealmsMainScreen.SERVER_OPEN_TOOLTIP);
            }
        }

        private void drawRealmStatus(
            GuiGraphics pGuiGraphics, int pX, int pY, int pMouseX, int pMouseY, ResourceLocation pSpriteLocation, Supplier<Component> pTooltipSupplier
        ) {
            pGuiGraphics.blitSprite(RenderType::guiTextured, pSpriteLocation, pX, pY, 10, 28);
            if (RealmsMainScreen.this.realmSelectionList.isMouseOver((double)pMouseX, (double)pMouseY)
                && pMouseX >= pX
                && pMouseX <= pX + 10
                && pMouseY >= pY
                && pMouseY <= pY + 28) {
                RealmsMainScreen.this.setTooltipForNextRenderPass(pTooltipSupplier.get());
            }
        }

        protected void renderThirdLine(GuiGraphics pGuiGraphics, int pTop, int pLeft, RealmsServer pServer) {
            int i = this.textX(pLeft);
            int j = this.firstLineY(pTop);
            int k = this.thirdLineY(j);
            if (!RealmsMainScreen.isSelfOwnedServer(pServer)) {
                pGuiGraphics.drawString(RealmsMainScreen.this.font, pServer.owner, i, this.thirdLineY(j), -8355712);
            } else if (pServer.expired) {
                Component component = pServer.expiredTrial ? RealmsMainScreen.TRIAL_EXPIRED_TEXT : RealmsMainScreen.SUBSCRIPTION_EXPIRED_TEXT;
                pGuiGraphics.drawString(RealmsMainScreen.this.font, component, i, k, -2142128);
            }
        }

        protected void renderClampedString(GuiGraphics pGuiGraphics, @Nullable String pText, int pMinX, int pY, int pMaxX, int pColor) {
            if (pText != null) {
                int i = pMaxX - pMinX;
                if (RealmsMainScreen.this.font.width(pText) > i) {
                    String s = RealmsMainScreen.this.font.plainSubstrByWidth(pText, i - RealmsMainScreen.this.font.width("... "));
                    pGuiGraphics.drawString(RealmsMainScreen.this.font, s + "...", pMinX, pY, pColor);
                } else {
                    pGuiGraphics.drawString(RealmsMainScreen.this.font, pText, pMinX, pY, pColor);
                }
            }
        }

        protected int versionTextX(int pLeft, int pWidth, Component pVersionComponent) {
            return pLeft + pWidth - RealmsMainScreen.this.font.width(pVersionComponent) - 20;
        }

        protected int gameModeTextX(int pLeft, int pWidth, Component pComponent) {
            return pLeft + pWidth - RealmsMainScreen.this.font.width(pComponent) - 20;
        }

        protected int renderGameMode(RealmsServer pServer, GuiGraphics pGuiGraphics, int pLeft, int pWidth, int pFirstLineY) {
            boolean flag = pServer.isHardcore;
            int i = pServer.gameMode;
            int j = pLeft;
            if (GameType.isValidId(i)) {
                Component component = RealmsMainScreen.getGameModeComponent(i, flag);
                j = this.gameModeTextX(pLeft, pWidth, component);
                pGuiGraphics.drawString(RealmsMainScreen.this.font, component, j, this.secondLineY(pFirstLineY), -8355712);
            }

            if (flag) {
                j -= 10;
                pGuiGraphics.blitSprite(RenderType::guiTextured, RealmsMainScreen.HARDCORE_MODE_SPRITE, j, this.secondLineY(pFirstLineY), 8, 8);
            }

            return j;
        }

        protected int firstLineY(int pTop) {
            return pTop + 1;
        }

        protected int lineHeight() {
            return 2 + 9;
        }

        protected int textX(int pLeft) {
            return pLeft + 36 + 2;
        }

        protected int secondLineY(int pFirstLineY) {
            return pFirstLineY + this.lineHeight();
        }

        protected int thirdLineY(int pFirstLineY) {
            return pFirstLineY + this.lineHeight() * 2;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum LayoutState {
        LOADING,
        NO_REALMS,
        LIST;
    }

    @OnlyIn(Dist.CLIENT)
    static class NotificationButton extends SpriteIconButton.CenteredIcon {
        private static final ResourceLocation[] NOTIFICATION_ICONS = new ResourceLocation[]{
            ResourceLocation.withDefaultNamespace("notification/1"),
            ResourceLocation.withDefaultNamespace("notification/2"),
            ResourceLocation.withDefaultNamespace("notification/3"),
            ResourceLocation.withDefaultNamespace("notification/4"),
            ResourceLocation.withDefaultNamespace("notification/5"),
            ResourceLocation.withDefaultNamespace("notification/more")
        };
        private static final int UNKNOWN_COUNT = Integer.MAX_VALUE;
        private static final int SIZE = 20;
        private static final int SPRITE_SIZE = 14;
        private int notificationCount;

        public NotificationButton(Component pMessage, ResourceLocation pSprite, Button.OnPress pOnPress) {
            super(20, 20, pMessage, 14, 14, pSprite, pOnPress, null);
        }

        int notificationCount() {
            return this.notificationCount;
        }

        public void setNotificationCount(int pNotificationCount) {
            this.notificationCount = pNotificationCount;
        }

        @Override
        public void renderWidget(GuiGraphics p_301337_, int p_300699_, int p_300272_, float p_300587_) {
            super.renderWidget(p_301337_, p_300699_, p_300272_, p_300587_);
            if (this.active && this.notificationCount != 0) {
                this.drawNotificationCounter(p_301337_);
            }
        }

        private void drawNotificationCounter(GuiGraphics pGuiGraphics) {
            pGuiGraphics.blitSprite(
                RenderType::guiTextured, NOTIFICATION_ICONS[Math.min(this.notificationCount, 6) - 1], this.getX() + this.getWidth() - 5, this.getY() - 3, 8, 8
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    class NotificationMessageEntry extends RealmsMainScreen.Entry {
        private static final int SIDE_MARGINS = 40;
        private static final int OUTLINE_COLOR = -12303292;
        private final Component text;
        private final int frameItemHeight;
        private final List<AbstractWidget> children = new ArrayList<>();
        @Nullable
        private final RealmsMainScreen.CrossButton dismissButton;
        private final MultiLineTextWidget textWidget;
        private final GridLayout gridLayout;
        private final FrameLayout textFrame;
        private int lastEntryWidth = -1;

        public NotificationMessageEntry(final Component pText, final int pFrameItemHeight, final RealmsNotification pNotification) {
            this.text = pText;
            this.frameItemHeight = pFrameItemHeight;
            this.gridLayout = new GridLayout();
            int i = 7;
            this.gridLayout.addChild(ImageWidget.sprite(20, 20, RealmsMainScreen.INFO_SPRITE), 0, 0, this.gridLayout.newCellSettings().padding(7, 7, 0, 0));
            this.gridLayout.addChild(SpacerElement.width(40), 0, 0);
            this.textFrame = this.gridLayout.addChild(new FrameLayout(0, 9 * 3 * (pFrameItemHeight - 1)), 0, 1, this.gridLayout.newCellSettings().paddingTop(7));
            this.textWidget = this.textFrame
                .addChild(
                    new MultiLineTextWidget(pText, RealmsMainScreen.this.font).setCentered(true), this.textFrame.newChildLayoutSettings().alignHorizontallyCenter().alignVerticallyTop()
                );
            this.gridLayout.addChild(SpacerElement.width(40), 0, 2);
            if (pNotification.dismissable()) {
                this.dismissButton = this.gridLayout
                    .addChild(
                        new RealmsMainScreen.CrossButton(
                            p_275478_ -> RealmsMainScreen.this.dismissNotification(pNotification.uuid()), Component.translatable("mco.notification.dismiss")
                        ),
                        0,
                        2,
                        this.gridLayout.newCellSettings().alignHorizontallyRight().padding(0, 7, 7, 0)
                    );
            } else {
                this.dismissButton = null;
            }

            this.gridLayout.visitWidgets(this.children::add);
        }

        @Override
        public boolean keyPressed(int p_275646_, int p_275453_, int p_275621_) {
            return this.dismissButton != null && this.dismissButton.keyPressed(p_275646_, p_275453_, p_275621_) ? true : super.keyPressed(p_275646_, p_275453_, p_275621_);
        }

        private void updateEntryWidth(int pEntryWidth) {
            if (this.lastEntryWidth != pEntryWidth) {
                this.refreshLayout(pEntryWidth);
                this.lastEntryWidth = pEntryWidth;
            }
        }

        private void refreshLayout(int pWidth) {
            int i = pWidth - 80;
            this.textFrame.setMinWidth(i);
            this.textWidget.setMaxWidth(i);
            this.gridLayout.arrangeElements();
        }

        @Override
        public void renderBack(
            GuiGraphics p_281374_,
            int p_282622_,
            int p_283656_,
            int p_281830_,
            int p_281651_,
            int p_283685_,
            int p_281784_,
            int p_282510_,
            boolean p_283146_,
            float p_283324_
        ) {
            super.renderBack(p_281374_, p_282622_, p_283656_, p_281830_, p_281651_, p_283685_, p_281784_, p_282510_, p_283146_, p_283324_);
            p_281374_.renderOutline(p_281830_ - 2, p_283656_ - 2, p_281651_, 36 * this.frameItemHeight - 2, -12303292);
        }

        @Override
        public void render(
            GuiGraphics p_281768_,
            int p_275375_,
            int p_275358_,
            int p_275447_,
            int p_275694_,
            int p_275477_,
            int p_275710_,
            int p_275677_,
            boolean p_275542_,
            float p_275323_
        ) {
            this.gridLayout.setPosition(p_275447_, p_275358_);
            this.updateEntryWidth(p_275694_ - 4);
            this.children.forEach(p_280688_ -> p_280688_.render(p_281768_, p_275710_, p_275677_, p_275323_));
        }

        @Override
        public boolean mouseClicked(double p_275209_, double p_275338_, int p_275560_) {
            if (this.dismissButton != null) {
                this.dismissButton.mouseClicked(p_275209_, p_275338_, p_275560_);
            }

            return super.mouseClicked(p_275209_, p_275338_, p_275560_);
        }

        @Override
        public Component getNarration() {
            return this.text;
        }
    }

    @OnlyIn(Dist.CLIENT)
    class ParentEntry extends RealmsMainScreen.Entry {
        private final RealmsServer server;
        private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

        public ParentEntry(final RealmsServer pServer) {
            this.server = pServer;
            if (!pServer.expired) {
                this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.parent.tooltip")));
            }
        }

        @Override
        public void render(
            GuiGraphics p_312282_,
            int p_310045_,
            int p_311515_,
            int p_311448_,
            int p_310278_,
            int p_312055_,
            int p_311895_,
            int p_310535_,
            boolean p_312546_,
            float p_313200_
        ) {
            int i = this.textX(p_311448_);
            int j = this.firstLineY(p_311515_);
            RealmsUtil.renderPlayerFace(p_312282_, p_311448_, p_311515_, 32, this.server.ownerUUID);
            Component component = RealmsMainScreen.getVersionComponent(this.server.activeVersion, -8355712);
            int k = this.versionTextX(p_311448_, p_310278_, component);
            this.renderClampedString(p_312282_, this.server.getName(), i, j, k, -8355712);
            if (component != CommonComponents.EMPTY) {
                p_312282_.drawString(RealmsMainScreen.this.font, component, k, j, -8355712);
            }

            int l = p_311448_;
            if (!this.server.isMinigameActive()) {
                l = this.renderGameMode(this.server, p_312282_, p_311448_, p_310278_, j);
            }

            this.renderClampedString(p_312282_, this.server.getDescription(), i, this.secondLineY(j), l, -8355712);
            this.renderThirdLine(p_312282_, p_311515_, p_311448_, this.server);
            this.renderStatusLights(this.server, p_312282_, p_311448_ + p_310278_, p_311515_, p_311895_, p_310535_);
            this.tooltip.refreshTooltipForNextRenderPass(p_312546_, this.isFocused(), new ScreenRectangle(p_311448_, p_311515_, p_310278_, p_312055_));
        }

        @Override
        public Component getNarration() {
            return Component.literal(Objects.requireNonNullElse(this.server.name, "unknown server"));
        }
    }

    @OnlyIn(Dist.CLIENT)
    class RealmSelectionList extends ObjectSelectionList<RealmsMainScreen.Entry> {
        public RealmSelectionList() {
            super(Minecraft.getInstance(), RealmsMainScreen.this.width, RealmsMainScreen.this.height, 0, 36);
        }

        public void setSelected(@Nullable RealmsMainScreen.Entry p_86849_) {
            super.setSelected(p_86849_);
            RealmsMainScreen.this.updateButtonStates();
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        void refreshEntries(RealmsMainScreen pScreen, @Nullable RealmsServer pServer) {
            this.clearEntries();

            for (RealmsNotification realmsnotification : RealmsMainScreen.this.notifications) {
                if (realmsnotification instanceof RealmsNotification.VisitUrl realmsnotification$visiturl) {
                    this.addEntriesForNotification(realmsnotification$visiturl, pScreen);
                    RealmsMainScreen.this.markNotificationsAsSeen(List.of(realmsnotification));
                    break;
                }
            }

            this.refreshServerEntries(pServer);
        }

        private void refreshServerEntries(@Nullable RealmsServer pServer) {
            for (RealmsServer realmsserver : RealmsMainScreen.this.availableSnapshotServers) {
                this.addEntry(RealmsMainScreen.this.new AvailableSnapshotEntry(realmsserver));
            }

            for (RealmsServer realmsserver1 : RealmsMainScreen.this.serverList) {
                RealmsMainScreen.Entry realmsmainscreen$entry;
                if (RealmsMainScreen.isSnapshot() && !realmsserver1.isSnapshotRealm()) {
                    if (realmsserver1.state == RealmsServer.State.UNINITIALIZED) {
                        continue;
                    }

                    realmsmainscreen$entry = RealmsMainScreen.this.new ParentEntry(realmsserver1);
                } else {
                    realmsmainscreen$entry = RealmsMainScreen.this.new ServerEntry(realmsserver1);
                }

                this.addEntry(realmsmainscreen$entry);
                if (pServer != null && pServer.id == realmsserver1.id) {
                    this.setSelected(realmsmainscreen$entry);
                }
            }
        }

        private void addEntriesForNotification(RealmsNotification.VisitUrl pUrl, RealmsMainScreen pMainScreen) {
            Component component = pUrl.getMessage();
            int i = RealmsMainScreen.this.font.wordWrapHeight(component, 216);
            int j = Mth.positiveCeilDiv(i + 7, 36) - 1;
            this.addEntry(RealmsMainScreen.this.new NotificationMessageEntry(component, j + 2, pUrl));

            for (int k = 0; k < j; k++) {
                this.addEntry(RealmsMainScreen.this.new EmptyEntry());
            }

            this.addEntry(RealmsMainScreen.this.new ButtonEntry(pUrl.buildOpenLinkButton(pMainScreen)));
        }
    }

    @OnlyIn(Dist.CLIENT)
    interface RealmsCall<T> {
        T request(RealmsClient pRealmsClient) throws RealmsServiceException;
    }

    @OnlyIn(Dist.CLIENT)
    class ServerEntry extends RealmsMainScreen.Entry {
        private static final Component ONLINE_PLAYERS_TOOLTIP_HEADER = Component.translatable("mco.onlinePlayers");
        private static final int PLAYERS_ONLINE_SPRITE_SIZE = 9;
        private static final int SKIN_HEAD_LARGE_WIDTH = 36;
        private final RealmsServer serverData;
        private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

        public ServerEntry(final RealmsServer pServerData) {
            this.serverData = pServerData;
            boolean flag = RealmsMainScreen.isSelfOwnedServer(pServerData);
            if (RealmsMainScreen.isSnapshot() && flag && pServerData.isSnapshotRealm()) {
                this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.paired", pServerData.parentWorldName)));
            } else if (!flag && pServerData.needsDowngrade()) {
                this.tooltip.set(Tooltip.create(Component.translatable("mco.snapshot.friendsRealm.downgrade", pServerData.activeVersion)));
            }
        }

        @Override
        public void render(
            GuiGraphics p_283093_,
            int p_281645_,
            int p_283047_,
            int p_283525_,
            int p_282321_,
            int p_282391_,
            int p_281913_,
            int p_282475_,
            boolean p_282378_,
            float p_282843_
        ) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                p_283093_.blitSprite(RenderType::guiTextured, RealmsMainScreen.NEW_REALM_SPRITE, p_283525_ - 5, p_283047_ + p_282391_ / 2 - 10, 40, 20);
                int i = p_283047_ + p_282391_ / 2 - 9 / 2;
                p_283093_.drawString(RealmsMainScreen.this.font, RealmsMainScreen.SERVER_UNITIALIZED_TEXT, p_283525_ + 40 - 2, i, 8388479);
            } else {
                this.renderStatusLights(this.serverData, p_283093_, p_283525_ + 36, p_283047_, p_281913_, p_282475_);
                RealmsUtil.renderPlayerFace(p_283093_, p_283525_, p_283047_, 32, this.serverData.ownerUUID);
                this.renderFirstLine(p_283093_, p_283047_, p_283525_, p_282321_);
                this.renderSecondLine(p_283093_, p_283047_, p_283525_, p_282321_);
                this.renderThirdLine(p_283093_, p_283047_, p_283525_, this.serverData);
                boolean flag = this.renderOnlinePlayers(p_283093_, p_283047_, p_283525_, p_282321_, p_282391_, p_281913_, p_282475_);
                this.renderStatusLights(this.serverData, p_283093_, p_283525_ + p_282321_, p_283047_, p_281913_, p_282475_);
                if (!flag) {
                    this.tooltip.refreshTooltipForNextRenderPass(p_282378_, this.isFocused(), new ScreenRectangle(p_283525_, p_283047_, p_282321_, p_282391_));
                }
            }
        }

        private void renderFirstLine(GuiGraphics pGuiGraphics, int pTop, int pLeft, int pWidth) {
            int i = this.textX(pLeft);
            int j = this.firstLineY(pTop);
            Component component = RealmsMainScreen.getVersionComponent(this.serverData.activeVersion, this.serverData.isCompatible());
            int k = this.versionTextX(pLeft, pWidth, component);
            this.renderClampedString(pGuiGraphics, this.serverData.getName(), i, j, k, -1);
            if (component != CommonComponents.EMPTY && !this.serverData.isMinigameActive()) {
                pGuiGraphics.drawString(RealmsMainScreen.this.font, component, k, j, -8355712);
            }
        }

        private void renderSecondLine(GuiGraphics pGuiGraphics, int pTop, int pLeft, int pWidth) {
            int i = this.textX(pLeft);
            int j = this.firstLineY(pTop);
            int k = this.secondLineY(j);
            String s = this.serverData.getMinigameName();
            boolean flag = this.serverData.isMinigameActive();
            if (flag && s != null) {
                Component component = Component.literal(s).withStyle(ChatFormatting.GRAY);
                pGuiGraphics.drawString(RealmsMainScreen.this.font, Component.translatable("mco.selectServer.minigameName", component).withColor(-171), i, k, -1);
            } else {
                int l = this.renderGameMode(this.serverData, pGuiGraphics, pLeft, pWidth, j);
                this.renderClampedString(pGuiGraphics, this.serverData.getDescription(), i, this.secondLineY(j), l, -8355712);
            }
        }

        private boolean renderOnlinePlayers(GuiGraphics pGuiGraphics, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY) {
            List<ProfileResult> list = RealmsMainScreen.this.onlinePlayersPerRealm.getProfileResultsFor(this.serverData.id);
            if (!list.isEmpty()) {
                int i = pLeft + pWidth - 21;
                int j = pTop + pHeight - 9 - 2;
                int k = i;

                for (int l = 0; l < list.size(); l++) {
                    k -= 9 + (l == 0 ? 0 : 3);
                    PlayerFaceRenderer.draw(pGuiGraphics, Minecraft.getInstance().getSkinManager().getInsecureSkin(list.get(l).profile()), k, j, 9);
                }

                if (pMouseX >= k && pMouseX <= i && pMouseY >= j && pMouseY <= j + 9) {
                    pGuiGraphics.renderTooltip(
                        RealmsMainScreen.this.font,
                        List.of(ONLINE_PLAYERS_TOOLTIP_HEADER),
                        Optional.of(new ClientActivePlayersTooltip.ActivePlayersTooltip(list)),
                        pMouseX,
                        pMouseY
                    );
                    return true;
                }
            }

            return false;
        }

        private void playRealm() {
            RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            RealmsMainScreen.play(this.serverData, RealmsMainScreen.this);
        }

        private void createUnitializedRealm() {
            RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            RealmsCreateRealmScreen realmscreaterealmscreen = new RealmsCreateRealmScreen(RealmsMainScreen.this, this.serverData, this.serverData.isSnapshotRealm());
            RealmsMainScreen.this.minecraft.setScreen(realmscreaterealmscreen);
        }

        @Override
        public boolean mouseClicked(double p_86858_, double p_86859_, int p_86860_) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                this.createUnitializedRealm();
            } else if (RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
                if (Util.getMillis() - RealmsMainScreen.this.lastClickTime < 250L && this.isFocused()) {
                    this.playRealm();
                }

                RealmsMainScreen.this.lastClickTime = Util.getMillis();
            }

            return true;
        }

        @Override
        public boolean keyPressed(int p_279120_, int p_279121_, int p_279296_) {
            if (CommonInputs.selected(p_279120_)) {
                if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                    this.createUnitializedRealm();
                    return true;
                }

                if (RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
                    this.playRealm();
                    return true;
                }
            }

            return super.keyPressed(p_279120_, p_279121_, p_279296_);
        }

        @Override
        public Component getNarration() {
            return (Component)(this.serverData.state == RealmsServer.State.UNINITIALIZED
                ? RealmsMainScreen.UNITIALIZED_WORLD_NARRATION
                : Component.translatable("narrator.select", Objects.requireNonNullElse(this.serverData.name, "unknown server")));
        }

        public RealmsServer getServer() {
            return this.serverData;
        }
    }
}