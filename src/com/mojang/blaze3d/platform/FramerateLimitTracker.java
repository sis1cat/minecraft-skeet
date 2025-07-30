package com.mojang.blaze3d.platform;

import net.minecraft.Util;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

public class FramerateLimitTracker {
    private static final int OUT_OF_LEVEL_MENU_LIMIT = 60;
    private static final int ICONIFIED_WINDOW_LIMIT = 10;
    private static final int AFK_LIMIT = 30;
    private static final int LONG_AFK_LIMIT = 10;
    private static final long AFK_THRESHOLD_MS = 60000L;
    private static final long LONG_AFK_THRESHOLD_MS = 600000L;
    private final Options options;
    private final Minecraft minecraft;
    private int framerateLimit;
    private long latestInputTime;

    public FramerateLimitTracker(Options pOptions, Minecraft pMinecraft) {
        this.options = pOptions;
        this.minecraft = pMinecraft;
        this.framerateLimit = pOptions.framerateLimit().get();
    }

    public int getFramerateLimit() {
        if (Minecraft.getInstance().options.enableVsync().get()) {
            this.framerateLimit = 260;
        }

        if (this.framerateLimit <= 0) {
            this.framerateLimit = 260;
        }

        InactivityFpsLimit inactivityfpslimit = this.options.inactivityFpsLimit().get();
        if (this.minecraft.getWindow().isIconified()) {
            return 10;
        } else {
            if (inactivityfpslimit == InactivityFpsLimit.AFK) {
                long i = Util.getMillis() - this.latestInputTime;
                if (i > 600000L) {
                    return this.framerateLimit; // 10
                }

                if (i > 60000L) {
                    return this.framerateLimit; //Math.min(this.framerateLimit, 30);
                }
            }

            return this.framerateLimit;
        }
    }

    public void setFramerateLimit(int pFramerateLimit) {
        this.framerateLimit = pFramerateLimit;
    }

    public void onInputReceived() {
        this.latestInputTime = Util.getMillis();
    }
}