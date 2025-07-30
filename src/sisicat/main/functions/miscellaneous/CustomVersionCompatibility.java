package sisicat.main.functions.miscellaneous;

import com.darkmagician6.eventapi.EventTarget;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import sisicat.events.GraphicsEvent;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;
import sisicat.main.utilities.Timer;


import java.util.ArrayList;
import java.util.List;


public class CustomVersionCompatibility extends Function {

    private final FunctionSetting version;
    private final Timer timer = new Timer();

    public CustomVersionCompatibility(String name) {
        super(name);

        this.version = new FunctionSetting("Version",
                new ArrayList<>(List.of(
                        "1.21.7",
                        "1.21.6",
                        "1.21.5",
                        "1.21.4",
                        "1.21.2-1.21.3",
                        "1.21-1.21.1",
                        "1.20.5-1.20.6",
                        "1.20.3-1.20.4",
                        "1.20.2",
                        "1.20-1.20.1",
                        "1.19.4",
                        "1.19.3",
                        "1.19.1-1.19.2",
                        "1.19",
                        "1.18.2",
                        "1.18-1.18.1",
                        "1.17.1",
                        "1.17",
                        "1.16.4-1.16.5",
                        "1.16.3",
                        "1.16.2",
                        "1.16.1",
                        "1.16",
                        "1.12.2",
                        "1.8.9"
                )), "1.21.4");

        this.addSetting(this.version);
        this.setCanBeActivated(true);

        timer.updateLastTime();

    }

    @EventTarget
    void _event(GraphicsEvent graphicsEvent) {

        if(mc.level == null && timer.hasPassed(500) && ViaLoadingBase.getInstance().getTargetVersion() != ProtocolVersion.getClosest(this.version.getStringValue())) {
            timer.updateLastTime();
            ViaLoadingBase.getInstance().reload(ProtocolVersion.getClosest(this.version.getStringValue()));
        }

    }

}
