package sisicat.main.functions.miscellaneous;

import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;

import java.util.ArrayList;
import java.util.List;

public class IntegratedUIAnimations extends Function {

    private final FunctionSetting animations;

    public IntegratedUIAnimations(String name) {
        super(name);

        animations = new FunctionSetting(
                "Integrated UI animations",
                new ArrayList<>(List.of(
                        "Smooth chat",
                        "Smooth tab list"
                )),
                new ArrayList<>()
        );

        this.addSetting(animations);

    }

}
