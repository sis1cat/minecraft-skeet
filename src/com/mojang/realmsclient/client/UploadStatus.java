package com.mojang.realmsclient.client;

import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UploadStatus {
    private volatile long bytesWritten;
    private volatile long totalBytes;
    private long previousTimeSnapshot = Util.getMillis();
    private long previousBytesWritten;
    private long bytesPerSecond;

    public void setTotalBytes(long pTotalBytes) {
        this.totalBytes = pTotalBytes;
    }

    public long getTotalBytes() {
        return this.totalBytes;
    }

    public long getBytesWritten() {
        return this.bytesWritten;
    }

    public void onWrite(long pBytes) {
        this.bytesWritten += pBytes;
    }

    public boolean uploadStarted() {
        return this.bytesWritten != 0L;
    }

    public boolean uploadCompleted() {
        return this.bytesWritten == this.getTotalBytes();
    }

    public double getPercentage() {
        return Math.min((double)this.getBytesWritten() / (double)this.getTotalBytes(), 1.0);
    }

    public void refreshBytesPerSecond() {
        long i = Util.getMillis();
        long j = i - this.previousTimeSnapshot;
        if (j >= 1000L) {
            long k = this.bytesWritten;
            this.bytesPerSecond = 1000L * (k - this.previousBytesWritten) / j;
            this.previousBytesWritten = k;
            this.previousTimeSnapshot = i;
        }
    }

    public long getBytesPerSecond() {
        return this.bytesPerSecond;
    }
}