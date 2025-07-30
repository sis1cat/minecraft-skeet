package sisicat.main.functions.visual;

import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;

import java.util.ArrayList;
import java.util.List;

public class ChunksAppearanceAnimationFunction extends Function {

    public ChunksAppearanceAnimationFunction(String name) {
        super(name);

        FunctionSetting

                type = new FunctionSetting(
                    "Type",
                    new ArrayList<>(List.of(
                            "Up",
                            "Down"
                    )),
                    "Up"
                ),

                speed = new FunctionSetting(
                    "Speed", 1.5f,
                    1, 2,
                    "", 0.1f
                );

        this.addSetting(type);
        this.addSetting(speed);

    }



}
