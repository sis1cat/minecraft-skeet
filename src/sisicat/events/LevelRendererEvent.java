package sisicat.events;

import com.darkmagician6.eventapi.events.Event;

public class LevelRendererEvent implements Event {

    public float
            fogStart,
            fogEnd;

    public float[]
            fogColor;

    public float
            gameTime;

    public LevelRendererEvent(float fogStart, float fogEnd, float[] fogColor, float gameTime){
        this.fogStart = fogStart;
        this.fogEnd = fogEnd;
        this.fogColor = new float[]{ fogColor[0], fogColor[1], fogColor[2], fogColor[3] };
        this.gameTime = gameTime;
    }

}
