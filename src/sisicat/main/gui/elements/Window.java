package sisicat.main.gui.elements;

import com.darkmagician6.eventapi.EventTarget;
import com.darkmagician6.eventapi.types.Priority;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import sisicat.IDefault;

import sisicat.MineSense;
import sisicat.events.MouseEvent;
import sisicat.events.MouseMoveEvent;
import sisicat.main.functions.FunctionsManager;
import sisicat.main.gui.MainRender;
import sisicat.main.gui.elements.widgets.*;
import sisicat.main.utilities.Color;
import sisicat.main.utilities.Render;
import sisicat.main.utilities.Text;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class Window extends Widget implements IDefault {

    public int
            configX,
            configY,
            configWidth,
            configHeight;

    private boolean isFirstOpened = false;

    private static final int
            DEFAULT_WINDOW_WIDTH = 660,
            DEFAULT_WINDOW_HEIGHT = 560;

    private static int
            windowX;
    private static int windowY;

    public static int
            windowWidth = 660;
    public static int windowHeight = 560;

    public static float
            windowScale = 1.f;

    public static int
            gameWindowWidth;
    public static int gameWindowHeight;

    public static final Vec2 gameWindowSize = new Vec2(0, 0);

    public static boolean isAnyInteractableHovered = false;

    public static final Vec2
            meshPosition = new Vec2(0, 0),
            meshSize = new Vec2(0, 0),
            meshStep = new Vec2(0, 0);

    private static final Tabs TABS = new Tabs();

    // RAGE TAB
    private static final Group
            RAGE_AIMBOT = new Group(0, new Vec2(0, 0), new Vec2(12, 24),"Aimbot"),
            RAGE_OTHER = new Group(0, new Vec2(12, 0), new Vec2(12, 24),"Other");

    // MOVEMENT TAB
    private static final Group
            MOVEMENT_PLAYER = new Group(2, new Vec2(0, 0), new Vec2(12, 24), "Player"),
            MOVEMENT_EXPLOITS = new Group(2, new Vec2(12, 0), new Vec2(12, 24), "Exploits");

    // VISUALS TAB
    private static final Group
            VISUALS_PLAYER_ESP = new Group(3, new Vec2(0, 0), new Vec2(12, 13), "Player ESP"),
            VISUALS_COLORED_MODELS = new Group(3, new Vec2(0, 13), new Vec2(12, 11), "Colored models"),
            VISUALS_OTHER_ESP = new Group(3, new Vec2(12, 0), new Vec2(12, 12), "Other ESP"),
            VISUALS_EFFECTS = new Group(3, new Vec2(12, 12), new Vec2(12, 12), "Effects");

    // MISCELLANEOUS TAB
    private static final Group
            MISCELLANEOUS_MISCELLANEOUS = new Group(4, new Vec2(0, 0), new Vec2(12, 24), "Miscellaneous");
    private static final Group
            MISCELLANEOUS_SETTINGS = new Group(4, new Vec2(12, 12), new Vec2(12, 12), "Settings");

    // SAVES TAB
    private static final Group
            SAVES_PRESETS = new Group(6, new Vec2(0, 0), new Vec2(12, 24), "Presets");
    private static final Group
            SAVES_LUA = new Group(6, new Vec2(12, 0), new Vec2(12, 24), "Lua");

    // LUA TAB
    private static final Group
            LUA_A = new Group(7, new Vec2(0, 0), new Vec2(12, 24),"A"),
            LUA_B = new Group(7, new Vec2(12, 0), new Vec2(12, 24),"B");

    public Window(){

        groups.addAll(List.of(
                RAGE_AIMBOT,
                RAGE_OTHER,
                MOVEMENT_PLAYER,
                MOVEMENT_EXPLOITS,
                VISUALS_PLAYER_ESP,
                VISUALS_COLORED_MODELS,
                VISUALS_OTHER_ESP,
                VISUALS_EFFECTS,
                MISCELLANEOUS_MISCELLANEOUS,
                MISCELLANEOUS_SETTINGS,
                SAVES_PRESETS,
                SAVES_LUA,
                LUA_A,
                LUA_B
        ));

        RAGE_AIMBOT.addElement(
                new Check(
                        FunctionsManager.getFunctionByName("Rage"),
                        "Enabled", false, 0
                ).addBind(new Bind(FunctionsManager.getFunctionByName("Rage"), 0)),
                new Selectable(
                        FunctionsManager.getFunctionByName("Rage").getSettingByName("Target selection"),
                        "Target selection", 0
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Rage").getSettingByName("Attack distance"),
                        "Attack distance", 0
                ),
                new Combo(
                        FunctionsManager.getFunctionByName("Rage").getSettingByName("Rotation mode"),
                        "Rotation mode", 0
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Rage").getSettingByName("Multi-point scale"),
                        "Multi-point scale",
                        FunctionsManager.getFunctionByName("Rage").getSettingByName("Rotation mode"),
                        0,
                        new String[]{"Multi-point"}
                ),
                new Check(
                        FunctionsManager.getFunctionByName("Rage").getSettingByName("Backtrack"),
                        false, 0
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Rage").getSettingByName("Backtrack"),
                        "",
                        FunctionsManager.getFunctionByName("Rage").getSettingByName("Backtrack"),
                        0
                )
        );

        RAGE_OTHER.addElement(
                new Combo(
                        FunctionsManager.getFunctionByName("Rage").getSettingByName("Sprint reset mode"),
                        "Sprint reset mode", 0
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Rage").getSettingByName("Sprint reset mode"),
                        "", FunctionsManager.getFunctionByName("Rage").getSettingByName("Sprint reset mode"),
                        0,
                        new String[] {"W-Tap", "Shift"}
                ),
                new Combo(
                        FunctionsManager.getFunctionByName("Rage").getSettingByName("Force critical attack"),
                        "Force critical attack", 0
                ),
                new Check(
                        FunctionsManager.getFunctionByName("Rage").getSettingByName("Attack through blocks"),
                        false, 0
                ),
                new Combo(
                        FunctionsManager.getFunctionByName("Rage").getSettingByName("Attack through blocks"),
                        "", FunctionsManager.getFunctionByName("Rage").getSettingByName("Attack through blocks"), 0
                ),
                new Check(
                        FunctionsManager.getFunctionByName("Rage").getSettingByName("Automatic shield break"),
                        false, 0
                ),
                new Check(
                        FunctionsManager.getFunctionByName("Rage").getSettingByName("Strafe around the target"),
                        false, 0
                )
        );

        MOVEMENT_PLAYER.addElement(
                new Check(
                        FunctionsManager.getFunctionByName("Custom jump delay"),
                        "Custom jump delay", false, 2
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom jump delay").getSettingByName("Delay"),
                        "", FunctionsManager.getFunctionByName("Custom jump delay"), 2
                ),
                new Check(
                        FunctionsManager.getFunctionByName("Automatic sprint"),
                        false, 2
                ),
                new Check(
                        FunctionsManager.getFunctionByName("Automatic elytra jump"),
                        false, 2
                ).addBind(new Bind(FunctionsManager.getFunctionByName("Automatic elytra jump"), 2)),
                new Check(
                        FunctionsManager.getFunctionByName("Infinity elytra fly"),
                        true, 2
                )
        );

        MOVEMENT_EXPLOITS.addElement(
                new Check(
                        FunctionsManager.getFunctionByName("Client-side movement"),
                        true, 2
                ).addBind(new Bind(FunctionsManager.getFunctionByName("Client-side movement"), 2))
        );

        VISUALS_PLAYER_ESP.addElement(
                new Check(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Visualize aimbot"),
                        false, 3
                ).addColor(new sisicat.main.gui.elements.widgets.Color(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Dot color"), 3)),
                new Check(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Shield break sound"),
                        false, 3
                ),
                new Check(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Bounding box"),
                        false, 3
                ),
                new BindText(
                        "Border color", FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Bounding box")
                ).addColor(new sisicat.main.gui.elements.widgets.Color(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("bb Border color"), 3)),
                new BindText(
                        "First fill color", FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Bounding box")
                ).addColor(new sisicat.main.gui.elements.widgets.Color(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("bb First fill color"), 3)),
                new BindText(
                        "Second fill color", FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Bounding box")
                ).addColor(new sisicat.main.gui.elements.widgets.Color(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("bb Second fill color"), 3)),
                new Check(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Health bar"),
                        false, 3
                ),
                new BindText(
                        "Border color", FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Health bar")
                ).addColor(new sisicat.main.gui.elements.widgets.Color(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("hb Border color"), 3)),
                new BindText(
                        "First fill color", FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Health bar")
                ).addColor(new sisicat.main.gui.elements.widgets.Color(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("hb First fill color"), 3)),
                new BindText(
                        "Second fill color", FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Health bar")
                ).addColor(new sisicat.main.gui.elements.widgets.Color(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("hb Second fill color"), 3)),
                new BindText(
                        "Text color", FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Health bar")
                ).addColor(new sisicat.main.gui.elements.widgets.Color(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("hb Text color"), 3)),
                new Check(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Name"),
                        false, 3
                ).addColor(new sisicat.main.gui.elements.widgets.Color(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Name color"), 3)),
                new Check(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Flags"),
                        false, 3
                ).addColor(new sisicat.main.gui.elements.widgets.Color(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Flags color"), 3)),
                new Check(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Item icon"),
                        false, 3
                ).addColor(new sisicat.main.gui.elements.widgets.Color(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Item icon color"), 3)),
                new Check(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Armor icons"),
                        false, 3
                ).addColor(new sisicat.main.gui.elements.widgets.Color(
                        FunctionsManager.getFunctionByName("Player ESP").getSettingByName("Armor icons color"), 3))
        );

        VISUALS_EFFECTS.addElement(
                new Check(
                        FunctionsManager.getFunctionByName("Custom fog"),
                        false, 3
                ).addColor(new sisicat.main.gui.elements.widgets.Color(
                        FunctionsManager.getFunctionByName("Custom fog").getSettingByName("Fog color"), 3)),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom fog").getSettingByName("Fog start"),
                        "Fog start", FunctionsManager.getFunctionByName("Custom fog"), 3
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom fog").getSettingByName("Fog end"),
                        "Fog end", FunctionsManager.getFunctionByName("Custom fog"), 3
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom fog").getSettingByName("Fog inversion"),
                        "Fog inversion", FunctionsManager.getFunctionByName("Custom fog"), 3
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom fog").getSettingByName("Fog brightness"),
                        "Fog brightness", FunctionsManager.getFunctionByName("Custom fog"), 3
                ),
                new Check(
                        FunctionsManager.getFunctionByName("Custom day time"),
                        false, 3
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom day time").getSettingByName("Ticks"),
                        "", FunctionsManager.getFunctionByName("Custom day time"), 3
                ),
                new Check(
                        FunctionsManager.getFunctionByName("Chunks appearance animation"),
                        false, 3
                ),
                new Combo(
                        FunctionsManager.getFunctionByName("Chunks appearance animation").getSettingByName("Type"),
                        "", FunctionsManager.getFunctionByName("Chunks appearance animation"), 3
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Chunks appearance animation").getSettingByName("Speed"),
                        "Speed", FunctionsManager.getFunctionByName("Chunks appearance animation"), 3
                ),
                new Check(
                        FunctionsManager.getFunctionByName("Remove hurt effect"),
                        false, 3
                )
        );

        MISCELLANEOUS_MISCELLANEOUS.addElement(
                new Combo(
                        FunctionsManager.getFunctionByName("Custom version compatibility").getSettingByName("Version"),
                        "Custom version compatibility", 4
                ),
                new Selectable(
                        FunctionsManager.getFunctionByName("Integrated UI animations").getSettingByName("Integrated UI animations"),
                        "Integrated UI animations", 4
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Override FOV").getSettingByName("Override FOV"),
                        4
                ),
                new Check(
                        FunctionsManager.getFunctionByName("Custom block place delay"),
                        false, 4
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom block place delay").getSettingByName("Delay"),
                        "", FunctionsManager.getFunctionByName("Custom block place delay"), 4
                ),
                new Check(
                        FunctionsManager.getFunctionByName("Custom view model"),
                        false, 4
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom view model").getSettingByName("Position X"),
                        "Position", FunctionsManager.getFunctionByName("Custom view model"), 4
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom view model").getSettingByName("Position Y"),
                        "", FunctionsManager.getFunctionByName("Custom view model"), 4
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom view model").getSettingByName("Position Z"),
                        "", FunctionsManager.getFunctionByName("Custom view model"), 4
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom view model").getSettingByName("Rotation X"),
                        "Rotation", FunctionsManager.getFunctionByName("Custom view model"), 4
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom view model").getSettingByName("Rotation Y"),
                        "", FunctionsManager.getFunctionByName("Custom view model"), 4
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom view model").getSettingByName("Rotation Z"),
                        "", FunctionsManager.getFunctionByName("Custom view model"), 4
                ),
                new Check(
                        FunctionsManager.getFunctionByName("Custom attack animation"),
                        false, 4
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom attack animation").getSettingByName("Swing amplitude"),
                        "Swing amplitude", FunctionsManager.getFunctionByName("Custom attack animation"), 4
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom attack animation").getSettingByName("Position animation intensity Y"),
                        "Position animation intensity Y", FunctionsManager.getFunctionByName("Custom attack animation"), 4
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom attack animation").getSettingByName("Rotation animation intensity X"),
                        "Rotation animation intensity", FunctionsManager.getFunctionByName("Custom attack animation"), 4
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom attack animation").getSettingByName("Rotation animation intensity Y"),
                        "", FunctionsManager.getFunctionByName("Custom attack animation"), 4
                ),
                new Slider(
                        FunctionsManager.getFunctionByName("Custom attack animation").getSettingByName("Rotation animation intensity Z"),
                        "", FunctionsManager.getFunctionByName("Custom attack animation"), 4
                )
        );

        MISCELLANEOUS_SETTINGS.addElement(
                new BindText("Menu key")
                        .addBind(new Bind(FunctionsManager.getFunctionByName("Menu key"), 4)),
                new BindText("Menu color")
                        .addColor(new sisicat.main.gui.elements.widgets.Color(
                                FunctionsManager.getFunctionByName("Menu color").getSettingByName("Color"), 4)),
                new Combo(
                        FunctionsManager.getFunctionByName("DPI scale").getSettingByName("DPI scale"),
                        "DPI scale", 4
                ),
                new Check(
                        FunctionsManager.getFunctionByName("Unlock mouse handler"),
                        false, 4
                ),
                new Check(
                        FunctionsManager.getFunctionByName("Unlock keyboard handler"),
                        false, 4
                )
        );

        SAVES_PRESETS.addElement(
                new Configs(6),
                new Edit(
                        FunctionsManager.getFunctionByName("Configuration").getSettingByName("Edit"),
                        6
                ),
                new Button(
                        FunctionsManager.getFunctionByName("Configuration").getSettingByName("Load"),
                        "Load", 6
                ),
                new Button(
                        FunctionsManager.getFunctionByName("Configuration").getSettingByName("Save"),
                        "Save", 6
                ),
                new Button(
                        FunctionsManager.getFunctionByName("Configuration").getSettingByName("Delete"),
                        "Delete", 6
                )
        );

    }

    private final ArrayList<Group> groups = new ArrayList<>();

    private void drawGroups() {

        for (Group group : groups)
            group.draw(meshPosition, meshStep, gameWindowSize);

        for (Group group : groups)
            for(Widget widget : group.elements) {

                if (
                        widget instanceof Check check &&
                                check.color != null
                ) check.color.drawPicker(gameWindowSize);

                if (
                        widget instanceof BindText bindText &&
                                bindText.color != null
                ) bindText.color.drawPicker(gameWindowSize);

            }

    }


    public void draw(){

        windowScale = Float.parseFloat(FunctionsManager.getFunctionByName("DPI scale").getSettingByName("DPI scale").getStringValue().replace("%", "f")) * 0.01f;

        isAnyInteractableHovered = false;

        setFirstValues(); // why not in constructor? incorrect window sizes
        updateDPI();

        updateSizes();

        Render.drawRectangleBorders(windowX, windowY, windowWidth, windowHeight, 1, Color.c12, 255 );
        Render.drawRectangleBorders(windowX + 1, windowY + 1, (windowWidth - 2), (windowHeight - 2), 1, Color.c60, 255);
        Render.drawRectangleBorders(windowX + 2, windowY + 2, (windowWidth - 4), (windowHeight - 4), 3, Color.c40, 255);
        Render.drawRectangleBorders(windowX + 5, windowY + 5, (windowWidth - 10), (windowHeight - 10), 1, Color.c60, 255);
        Render.drawMenuTexture(windowX + 6, windowY + 10, (windowWidth - 12), (windowHeight - 16), 255);
        Render.drawMenuTexture(windowX + 6, windowY + 10, (windowWidth - 12), (windowHeight - 16), 255);

        Render.drawRectangle(windowX + 6, windowY + 6, (windowWidth - 12), 4, Color.c12, 255);

        float[] blueColor = {55,175,220};
        float[] purpleColor = {205, 70, 205};
        float[] greenColor = {205,225,55};

        float[] darkBlueColor = {30,95,115};
        float[] darkPurpleColor = {110,35,110};
        float[] darkGreenColor = {110,120,30};

        // 1st line
        Render.drawGradientRectangle(windowX + 7, windowY + 7, ((windowWidth - 14) / 2), 1, blueColor, purpleColor, blueColor, purpleColor, 255);
        Render.drawGradientRectangle(windowX + 7 + (windowWidth - 14) / 2, windowY + 7, (int) Math.ceil(((double) (windowWidth - 14) / 2)), 1, purpleColor, greenColor, purpleColor, greenColor, 255);

        // 2nd line
        Render.drawGradientRectangle(windowX + 7, windowY + 8, ((windowWidth - 14) / 2), 1, darkBlueColor, darkPurpleColor, darkBlueColor, darkPurpleColor, 255);
        Render.drawGradientRectangle(windowX + 7 + (windowWidth - 14) / 2, windowY + 8, (int) Math.ceil((double) (windowWidth - 14) / 2), 1, darkPurpleColor, darkGreenColor, darkPurpleColor, darkGreenColor, 255);

        TABS.draw(windowX + 6, windowY + 10, gameWindowSize);

        drawGroups();

        MainRender.isFullInitialized = true;

    }

    private void updateSizes(){

        meshPosition.x = windowX + 6 + Math.round(75 * windowScale) + Math.round(20 * windowScale);
        meshPosition.y = windowY + 10 + (int)(20 * windowScale);

        meshSize.x = windowWidth - Math.round(26 * windowScale) - (meshPosition.x - windowX) + Math.round(20 * windowScale);
        meshSize.y = windowHeight - Math.round(26 * windowScale) - (meshPosition.y - windowY) + Math.round(20 * windowScale);

        meshStep.x = meshSize.x / 24;
        meshStep.y = meshSize.y / 24;

    }

    private boolean firstValuesSetted = false;

    private void setFirstValues(){

        if(firstValuesSetted)
            return;

        lastDPI = windowScale;

        updateWindowSizes();

        windowWidth = Math.round(DEFAULT_WINDOW_WIDTH * Window.windowScale);
        windowHeight = Math.round(DEFAULT_WINDOW_HEIGHT * Window.windowScale);

        windowX = gameWindowWidth / 2 - windowWidth / 2;
        windowY = gameWindowHeight / 2 - windowHeight / 2;

        firstValuesSetted = true;

    }

    private float lastDPI;

    private void updateDPI(){

        if(lastDPI != windowScale) {
            windowWidth = Math.round(windowWidth / lastDPI * Window.windowScale);
            windowHeight = Math.round(windowHeight / lastDPI * Window.windowScale);
            lastDPI = windowScale;
        }

    }

    public void updateWindowSizes() {

        try (MemoryStack stack = MemoryStack.stackPush()) {

            IntBuffer widthBuffer = stack.mallocInt(1);
            IntBuffer heightBuffer = stack.mallocInt(1);

            GLFW.glfwGetWindowSize(mc.getWindow().getWindow(), widthBuffer, heightBuffer);

            gameWindowWidth = widthBuffer.get(0);
            gameWindowHeight = heightBuffer.get(0);

        }

    }

    private boolean
            isHovering = false,
            isResizing = false;

    private int
            menuXCursorPos,
            menuYCursorPos;

    @EventTarget(Priority.LOW)
    void _event(MouseEvent mouseEvent){

        if(!MineSense.mainRender.isMenuOpened) { isHovering = false; isResizing = false; return; }

        if(mouseEvent.getAction() == 0 && (isHovering || isResizing)) { isHovering = false; isResizing = false; }

        if(
                mouseEvent.getAction() == 1 &&
                mouseEvent.getButton() == 0 &&
                !isHovering &&
                mouseEvent.getX() >= windowX + windowWidth - 15 &&
                mouseEvent.getX() <= windowX + windowWidth &&
                mouseEvent.getY() >= windowY + windowHeight - 15 &&
                mouseEvent.getY() <= windowY + windowHeight
        ) {

            isResizing = true;

            menuXCursorPos = windowWidth - ((int) mouseEvent.getX() - windowX);
            menuYCursorPos = windowHeight - ((int) mouseEvent.getY() - windowY);

        }

        if(
                mouseEvent.getAction() == 1 &&
                mouseEvent.getButton() == 0 &&
                !isHovering &&
                !isResizing &&
                mouseEvent.getX() >= windowX &&
                mouseEvent.getX() <= windowX + windowWidth &&
                mouseEvent.getY() >= windowY &&
                mouseEvent.getY() <= windowY + windowHeight &&
                !isAnyInteractableHovered
        ) {

            isHovering = true;

            menuXCursorPos = (int) mouseEvent.getX() - windowX;
            menuYCursorPos = (int) mouseEvent.getY() - windowY;

        }

    }

    @EventTarget
    void _event(MouseMoveEvent mouseMoveEvent){

        if(isResizing){
            windowWidth = (int) Mth.clamp(mouseMoveEvent.getX() - windowX + menuXCursorPos, DEFAULT_WINDOW_WIDTH * windowScale, gameWindowWidth);
            windowHeight = (int) Mth.clamp(mouseMoveEvent.getY() - windowY + menuYCursorPos, DEFAULT_WINDOW_HEIGHT * windowScale, gameWindowHeight);
        }

        if(!isHovering) return;

        /*if(mouseMoveEvent.getX() - menuXCursorPos > gameWindowWidth - windowWidth) {

            windowX = gameWindowWidth - windowWidth;
            menuXCursorPos = (int) mouseMoveEvent.getX() - windowX;

        } else if (mouseMoveEvent.getX() - menuXCursorPos < 0) {

            windowX = 0;
            menuXCursorPos = (int) mouseMoveEvent.getX() - windowX;

        }*/

        windowX = (int) mouseMoveEvent.getX() - menuXCursorPos;

        /*if(mouseMoveEvent.getY() - menuYCursorPos > gameWindowHeight - windowHeight) {

            windowY = gameWindowHeight - windowHeight;
            menuYCursorPos = (int) mouseMoveEvent.getY() - windowY;

        } else if (mouseMoveEvent.getY() - menuYCursorPos < 0) {

            windowY = 0;
            menuYCursorPos = (int) mouseMoveEvent.getY() - windowY;

        }*/

        windowY = (int) mouseMoveEvent.getY() - menuYCursorPos;

    }

    public int getWidth(){
        return windowWidth;
    }

    public int getHeight(){
        return windowHeight;
    }

}
