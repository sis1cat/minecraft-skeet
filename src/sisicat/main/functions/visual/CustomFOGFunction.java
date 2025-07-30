package sisicat.main.functions.visual;

import com.darkmagician6.eventapi.EventTarget;
import sisicat.events.LevelRendererEvent;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;

public class CustomFOGFunction extends Function {

    private final FunctionSetting
            fogStart,
            fogEnd,
            fogColor,
            fogInversion,
            fogBrightness;

    public CustomFOGFunction(String name) {
        super(name);

        fogColor = new FunctionSetting(
                "Fog color",
                new float[]{220, 0.5f, 1, 0.5f}
        );

        fogStart = new FunctionSetting(
                "Fog start",
                10, 1, 550,
                "u", 1
        );

        fogEnd = new FunctionSetting(
                "Fog end",
                50, 1, 550,
                "u", 1
        );

        fogInversion = new FunctionSetting(
                "Fog inversion",
                0, 0, 100,
                "%", 1
        );

        fogBrightness = new FunctionSetting(
                "Fog brightness",
                0, 0, 100,
                "%", 1
        );

        this.addSetting(fogColor);
        this.addSetting(fogStart);
        this.addSetting(fogEnd);
        this.addSetting(fogInversion);
        this.addSetting(fogBrightness);

    }

    @EventTarget
    void _event(LevelRendererEvent levelRendererEvent){

        levelRendererEvent.fogStart = fogStart.getFloatValue();
        levelRendererEvent.fogEnd = fogEnd.getFloatValue();
        levelRendererEvent.fogColor = fogColor.getRGBAColor();

        if(fogInversion.getFloatValue() != 0)
            levelRendererEvent.fogColor[3] = (255f + fogInversion.getFloatValue()) / 255f;
        else levelRendererEvent.fogColor[3] = levelRendererEvent.fogColor[3] / 255f;

        levelRendererEvent.fogColor[0] = (levelRendererEvent.fogColor[0] + fogBrightness.getFloatValue()) / 255f;
        levelRendererEvent.fogColor[1] = (levelRendererEvent.fogColor[1] + fogBrightness.getFloatValue()) / 255f;
        levelRendererEvent.fogColor[2] = (levelRendererEvent.fogColor[2] + fogBrightness.getFloatValue()) / 255f;

    }

}
