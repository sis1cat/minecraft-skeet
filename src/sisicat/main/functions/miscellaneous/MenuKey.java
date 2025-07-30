package sisicat.main.functions.miscellaneous;

import org.lwjgl.glfw.GLFW;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;

public class MenuKey extends Function {

    public MenuKey(String name) {
        super(name);

        this.setKeyBind(GLFW.GLFW_KEY_DELETE);

    }

}
