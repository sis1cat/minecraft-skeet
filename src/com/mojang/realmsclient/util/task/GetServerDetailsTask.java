package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerAddress;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.RealmsBrokenWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTickTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.screens.RealmsTermsScreen;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GetServerDetailsTask extends LongRunningTask {
    private static final Component APPLYING_PACK_TEXT = Component.translatable("multiplayer.applyingPack");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component TITLE = Component.translatable("mco.connect.connecting");
    private final RealmsServer server;
    private final Screen lastScreen;

    public GetServerDetailsTask(Screen pLastScreen, RealmsServer pServer) {
        this.lastScreen = pLastScreen;
        this.server = pServer;
    }

    @Override
    public void run() {
        RealmsServerAddress realmsserveraddress;
        try {
            realmsserveraddress = this.fetchServerAddress();
        } catch (CancellationException cancellationexception) {
            LOGGER.info("User aborted connecting to realms");
            return;
        } catch (RealmsServiceException realmsserviceexception) {
            switch (realmsserviceexception.realmsError.errorCode()) {
                case 6002:
                    setScreen(new RealmsTermsScreen(this.lastScreen, this.server));
                    return;
                case 6006:
                    boolean flag1 = Minecraft.getInstance().isLocalPlayer(this.server.ownerUUID);
                    setScreen(
                        (Screen)(flag1
                            ? new RealmsBrokenWorldScreen(this.lastScreen, this.server.id, this.server.isMinigameActive())
                            : new RealmsGenericErrorScreen(
                                Component.translatable("mco.brokenworld.nonowner.title"), Component.translatable("mco.brokenworld.nonowner.error"), this.lastScreen
                            ))
                    );
                    return;
                default:
                    this.error(realmsserviceexception);
                    LOGGER.error("Couldn't connect to world", (Throwable)realmsserviceexception);
                    return;
            }
        } catch (TimeoutException timeoutexception) {
            this.error(Component.translatable("mco.errorMessage.connectionFailure"));
            return;
        } catch (Exception exception) {
            LOGGER.error("Couldn't connect to world", (Throwable)exception);
            this.error(exception);
            return;
        }

        if (realmsserveraddress.address == null) {
            this.error(Component.translatable("mco.errorMessage.connectionFailure"));
        } else {
            boolean flag = realmsserveraddress.resourcePackUrl != null && realmsserveraddress.resourcePackHash != null;
            Screen screen = (Screen)(flag
                ? this.resourcePackDownloadConfirmationScreen(realmsserveraddress, generatePackId(this.server), this::connectScreen)
                : this.connectScreen(realmsserveraddress));
            setScreen(screen);
        }
    }

    private static UUID generatePackId(RealmsServer pRealmsServer) {
        return pRealmsServer.minigameName != null
            ? UUID.nameUUIDFromBytes(("minigame:" + pRealmsServer.minigameName).getBytes(StandardCharsets.UTF_8))
            : UUID.nameUUIDFromBytes(
                ("realms:" + Objects.requireNonNullElse(pRealmsServer.name, "") + ":" + pRealmsServer.activeSlot).getBytes(StandardCharsets.UTF_8)
            );
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    private RealmsServerAddress fetchServerAddress() throws RealmsServiceException, TimeoutException, CancellationException {
        RealmsClient realmsclient = RealmsClient.create();

        for (int i = 0; i < 40; i++) {
            if (this.aborted()) {
                throw new CancellationException();
            }

            try {
                return realmsclient.join(this.server.id);
            } catch (RetryCallException retrycallexception) {
                pause((long)retrycallexception.delaySeconds);
            }
        }

        throw new TimeoutException();
    }

    public RealmsLongRunningMcoTaskScreen connectScreen(RealmsServerAddress pServerAddress) {
        return new RealmsLongRunningMcoTickTaskScreen(this.lastScreen, new ConnectTask(this.lastScreen, this.server, pServerAddress));
    }

    private PopupScreen resourcePackDownloadConfirmationScreen(RealmsServerAddress pServerAddress, UUID pPackId, Function<RealmsServerAddress, Screen> pConnectScreen) {
        Component component = Component.translatable("mco.configure.world.resourcepack.question");
        return RealmsPopups.infoPopupScreen(this.lastScreen, component, p_340735_ -> {
            setScreen(new GenericMessageScreen(APPLYING_PACK_TEXT));
            this.scheduleResourcePackDownload(pServerAddress, pPackId).thenRun(() -> setScreen(pConnectScreen.apply(pServerAddress))).exceptionally(p_287306_ -> {
                Minecraft.getInstance().getDownloadedPackSource().cleanupAfterDisconnect();
                LOGGER.error("Failed to download resource pack from {}", pServerAddress, p_287306_);
                setScreen(new RealmsGenericErrorScreen(Component.translatable("mco.download.resourcePack.fail"), this.lastScreen));
                return null;
            });
        });
    }

    private CompletableFuture<?> scheduleResourcePackDownload(RealmsServerAddress pServerAddress, UUID pId) {
        try {
            if (pServerAddress.resourcePackUrl == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("resourcePackUrl was null"));
            } else if (pServerAddress.resourcePackHash == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("resourcePackHash was null"));
            } else {
                DownloadedPackSource downloadedpacksource = Minecraft.getInstance().getDownloadedPackSource();
                CompletableFuture<Void> completablefuture = downloadedpacksource.waitForPackFeedback(pId);
                downloadedpacksource.allowServerPacks();
                downloadedpacksource.pushPack(pId, new URL(pServerAddress.resourcePackUrl), pServerAddress.resourcePackHash);
                return completablefuture;
            }
        } catch (Exception exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}