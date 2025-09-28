package sisicat.main.utilities;

public class Animation {

    private long lastFrameTime;
    private boolean isFirstUse = true;

    public float interpolate(float current, float target, double durationMs) {

        if(isFirstUse) {
            lastFrameTime = System.nanoTime();
            isFirstUse = false;
        }

        long currentTime = System.nanoTime();
        float deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f;

        lastFrameTime = currentTime;

        float speed = (float) (Math.abs(target - current) / (durationMs / 1000.0f));
        float step = speed * deltaTime;

        if (Math.abs(target - current) <= step)
            return target;

        return current + Math.signum(target - current) * step;

    }

}