package sisicat.main.utilities;

import sisicat.main.gui.elements.Window;

public class Text {

    private static final String iconsFontPath = "/fonts/Icons.ttf";
    private static final String verdanaBoldFontPath = "/fonts/Verdana_bold.ttf";
    private static final String verdanaFontPath = "/fonts/Verdana.ttf";
    private static final String smallFontsPath = "/fonts/SmallFonts.ttf";

    public static Font MENU_ICONS;

    public static Font MENU_BOLD100;
    public static Font MENU_BOLD125;
    public static Font MENU_BOLD150;
    public static Font MENU_BOLD175;
    public static Font MENU_BOLD200;

    public static Font MENU_SMALL100;
    public static Font MENU_SMALL125;
    public static Font MENU_SMALL150;
    public static Font MENU_SMALL175;
    public static Font MENU_SMALL200;

    public static Font MENU_BINDS100;

    public static void initialize(){

        try {


            MENU_SMALL100 = new Font(Text.class.getResourceAsStream(verdanaFontPath), 9f, true, false, 0, -2);
            MENU_SMALL125 = new Font(Text.class.getResourceAsStream(verdanaFontPath), 11f, true, false, 0, -4); //
            MENU_SMALL150 = new Font(Text.class.getResourceAsStream(verdanaFontPath), 14f, true, true, 0, -5);
            MENU_SMALL175 = new Font(Text.class.getResourceAsStream(verdanaFontPath), 16f, true, true, -1, -5);
            MENU_SMALL200 = new Font(Text.class.getResourceAsStream(verdanaFontPath), 18f, true, false, 0, -6);

            MENU_BOLD100 = new Font(Text.class.getResourceAsStream(verdanaBoldFontPath), 10f, true, false, 0, -4);
            MENU_BOLD125 = new Font(Text.class.getResourceAsStream(verdanaBoldFontPath), 11f, true, false, 0, -4); //
            MENU_BOLD150 = new Font(Text.class.getResourceAsStream(verdanaBoldFontPath), 14f, true, true, 0, -5);
            MENU_BOLD175 = new Font(Text.class.getResourceAsStream(verdanaBoldFontPath), 16f, true, true, 0, -5);
            MENU_BOLD200 = new Font(Text.class.getResourceAsStream(verdanaBoldFontPath), 18f, true, false, 0, -6);

            MENU_ICONS = new Font(Text.class.getResourceAsStream(iconsFontPath), 32f, true, false, 1, 0);

            MENU_BINDS100 = new Font(Text.class.getResourceAsStream(smallFontsPath), 8f, false, false, 0, -1);

        }catch (Exception exception){
            exception.printStackTrace();
        }

    }

    public static Font getMenuFont(){

        if(Window.windowScale == 1.25f)
            return MENU_SMALL125;
        else if(Window.windowScale == 1.5f)
            return MENU_SMALL150;
        else if(Window.windowScale == 1.75f)
            return MENU_SMALL175;
        else if(Window.windowScale == 2f)
            return MENU_SMALL200;

        return MENU_SMALL100;

    }

    public static Font getMenuFont(boolean isBold){

        if(Window.windowScale == 1.25f)
            return isBold ? MENU_BOLD125 : MENU_SMALL125;
        else if (Window.windowScale == 1.5f)
            return isBold ? MENU_BOLD150 : MENU_SMALL150;
        else if (Window.windowScale == 1.75f)
            return isBold ? MENU_BOLD175 : MENU_SMALL175;
        else if (Window.windowScale == 2f)
            return isBold ? MENU_BOLD200 : MENU_SMALL200;

        return isBold ? MENU_BOLD100 : MENU_SMALL100;

    }

    public static Font getMenuBoldFont(){

        if(Window.windowScale == 1.25f)
            return MENU_BOLD125;
        else if(Window.windowScale == 1.5f)
            return MENU_BOLD150;
        else if(Window.windowScale == 1.75f)
            return MENU_BOLD175;
        else if(Window.windowScale == 2f)
            return MENU_BOLD200;

        return MENU_BOLD100;

    }

    public static Font getMenuBindsFont(){

        if(Window.windowScale == 1.25f)
            return MENU_SMALL100;
        if(Window.windowScale == 1.5f)
            return MENU_SMALL125;
        if(Window.windowScale == 1.75f)
            return MENU_SMALL150;
        if(Window.windowScale == 2f)
            return MENU_SMALL150;

        return MENU_BINDS100;

    }

}
