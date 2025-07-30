package sisicat.main.utilities;

import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class KeyBind {

    private static final String[] mouseCodes = {
        "-",
        "M1",
        "M2",
        "M3",
        "M4",
        "M5",
    };

    public static String getKeyName(int keyCode) {

        if(keyCode < 6)
            return "[" + mouseCodes[keyCode] + "]";

        String keyName = GLFW.glfwGetKeyName(keyCode, 0);

        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            char englishChar = (char) ('A' + (keyCode - GLFW.GLFW_KEY_A));
            keyName = String.valueOf(englishChar);
        }

        if(keyCode == GLFW.GLFW_KEY_GRAVE_ACCENT)
            keyName = "`";

        if(keyName != null && keyName.length() > 3)
            keyName = keyName.substring(0, 2);

        else if(keyName == null){
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT_SHIFT -> keyName = "LSH";
                case GLFW.GLFW_KEY_RIGHT_SHIFT -> keyName = "RSH";
                case GLFW.GLFW_KEY_LEFT_CONTROL -> keyName = "LCT";
                case GLFW.GLFW_KEY_RIGHT_CONTROL -> keyName = "RCT";
                case GLFW.GLFW_KEY_LEFT_ALT -> keyName = "LAL";
                case GLFW.GLFW_KEY_RIGHT_ALT -> keyName = "RAL";
                case GLFW.GLFW_KEY_ENTER -> keyName = "ENT";
                case GLFW.GLFW_KEY_TAB -> keyName = "TAB";
                case GLFW.GLFW_KEY_ESCAPE -> keyName = "ESC";
                case GLFW.GLFW_KEY_CAPS_LOCK -> keyName = "CAP";
                case GLFW.GLFW_KEY_SPACE -> keyName = "SPA";
                case GLFW.GLFW_KEY_F1 -> keyName = "F1";
                case GLFW.GLFW_KEY_F2 -> keyName = "F2";
                case GLFW.GLFW_KEY_F3 -> keyName = "F3";
                case GLFW.GLFW_KEY_F4 -> keyName = "F4";
                case GLFW.GLFW_KEY_F5 -> keyName = "F5";
                case GLFW.GLFW_KEY_F6 -> keyName = "F6";
                case GLFW.GLFW_KEY_F7 -> keyName = "F7";
                case GLFW.GLFW_KEY_F8 -> keyName = "F8";
                case GLFW.GLFW_KEY_F9 -> keyName = "F9";
                case GLFW.GLFW_KEY_F10 -> keyName = "F10";
                case GLFW.GLFW_KEY_F11 -> keyName = "F11";
                case GLFW.GLFW_KEY_F12 -> keyName = "F12";
                case GLFW.GLFW_KEY_DELETE -> keyName = "DEL";
                default -> keyName = "UNK";
            }
        }

        return "[" + keyName.toUpperCase() + "]";

    }

}
