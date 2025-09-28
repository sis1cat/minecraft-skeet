package sisicat.main.functions.miscellaneous;

import com.darkmagician6.eventapi.EventTarget;
import sisicat.events.GraphicsEvent;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;
import sisicat.main.gui.elements.widgets.Widget;
import sisicat.main.utilities.Color;

public class MenuColor extends Function {

    private final FunctionSetting color;
    private static MenuColor instance;

    public MenuColor(String name) {
        super(name);

        color =
                new FunctionSetting(
                        "Color",
                        new float[]{80, 0.8f, 0.8f, 1f}
                );

        this.addSetting(color);

        setCanBeActivated(true);

        instance = this;

    }

    public static void updateMenuColor() {
        Widget.themeColor = instance.color.getRGBAColor();
    }

}
