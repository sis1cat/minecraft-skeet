package sisicat.main.functions.miscellaneous;

import com.darkmagician6.eventapi.EventTarget;
import com.darkmagician6.eventapi.types.Priority;
import sisicat.events.GraphicsEvent;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;
import sisicat.main.gui.elements.Window;

import java.util.ArrayList;
import java.util.List;

public class DPIScale extends Function {

    private final FunctionSetting dpiScale;

    public DPIScale(String name) {
        super(name);

        ArrayList<String> options = new ArrayList<>(
                List.of(new String[]{
                        "100%",
                        "125%",
                        "150%",
                        "175%",
                        "200%"
                })
        );

        dpiScale = new FunctionSetting("DPI scale", options, "100%");

        this.addSetting(dpiScale);
        this.setCanBeActivated(true);

    }

}
