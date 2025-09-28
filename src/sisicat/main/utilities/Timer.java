package sisicat.main.utilities;

public class Timer {

    private long lastRegisteredTime;

    public Timer() {
        updateLastTime();
    }

    public boolean hasPassed(double milliseconds) {
        return ((getTime() - this.lastRegisteredTime) >= milliseconds);
    }

    public long getTime() {
        return System.nanoTime() / 1000000L;
    }

    public long getElapsedTime() {
        return getTime() - this.lastRegisteredTime;
    }

    public void updateLastTime() {
        this.lastRegisteredTime = getTime();
    }

}
