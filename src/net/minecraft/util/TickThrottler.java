package net.minecraft.util;

public class TickThrottler {
    private final int incrementStep;
    private final int threshold;
    private int count;

    public TickThrottler(int pIncrementStep, int pThreshold) {
        this.incrementStep = pIncrementStep;
        this.threshold = pThreshold;
    }

    public void increment() {
        this.count = this.count + this.incrementStep;
    }

    public void tick() {
        if (this.count > 0) {
            this.count--;
        }
    }

    public boolean isUnderThreshold() {
        return this.count < this.threshold;
    }
}