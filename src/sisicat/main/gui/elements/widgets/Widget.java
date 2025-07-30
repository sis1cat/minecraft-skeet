package sisicat.main.gui.elements.widgets;

import com.darkmagician6.eventapi.EventManager;

public abstract class Widget {

    public Group parent;
    public static float[] themeColor = new float[]{255, 255, 255, 255};

    public static float menuAlpha = 0f;

    public Widget(){
        EventManager.register(this);
    }

    public abstract static class AdditionAbleWidget extends Widget {
        Bind bind;
        public Color color;
    }

}
