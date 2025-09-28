package sisicat.main.gui.elements.widgets;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.world.phys.Vec2;
import sisicat.MineSense;
import sisicat.events.MouseEvent;
import sisicat.events.MouseMoveEvent;
import sisicat.main.Config;
import sisicat.main.functions.FunctionSetting;
import sisicat.main.functions.FunctionsManager;
import sisicat.main.gui.elements.Window;
import sisicat.main.utilities.Color;
import sisicat.main.utilities.Render;
import sisicat.main.utilities.Text;

import java.io.File;
import java.util.ArrayList;

public class Configs extends Widget {

    private static final int DEFAULT_MAX_WIDGET_WIDTH = 200;
    private static final int DEFAULT_MIN_WIDGET_WIDTH = 40;
    private static final int DEFAULT_WIDGET_HEIGHT = 126;

    private int
            maxWidgetWidth = 200,
            minWidgetWidth = 40,
            widgetHeight = 126;

    private int x, y, width;

    private static int tab = 0;
    private static boolean isWidgetHovered = false;

    public int occupiedSpace = 0;

    public static final ArrayList<Config> configurations = new ArrayList<>();

    public static void reload() {
        File dir = new File("configs");
        File[] files = dir.listFiles();

        if(files == null)
            return;

        configurations.clear();

        for(File file : files) {

            if(file.getName().startsWith("."))
                continue;

            configurations.add(new Config(file.getName()));

        }
    }

    public Configs(int tab){

        Configs.tab = tab;

        reload();
        Config.selected = sisicat.main.Config.loaded;
    }

    public void draw(int x, int y, int groupWidth){

        widgetHeight = Math.round(DEFAULT_WIDGET_HEIGHT * Window.windowScale);
        minWidgetWidth = Math.round(DEFAULT_MIN_WIDGET_WIDTH * Window.windowScale);
        maxWidgetWidth = Math.round(DEFAULT_MAX_WIDGET_WIDTH * Window.windowScale);

        occupiedSpace = widgetHeight - Math.round(10 * Window.windowScale) + 5;

        width = Math.min(Math.max(groupWidth - Math.round(100 * Window.windowScale), minWidgetWidth), maxWidgetWidth);

        this.x = x + Math.round(20 * Window.windowScale);
        this.y = y;

        Render.drawRectangleBorders(this.x, this.y, width, widgetHeight, 1, Color.c12, 255);
        Render.drawRectangle(this.x + 1, this.y + 1, width - 2, widgetHeight - 2, Color.c35, 255);

        int widgetHeight = Math.round(16 * Window.windowScale);

        int yOffset = 1;
        int heightOfOne = widgetHeight + 2;

        for (Config configuration : configurations) {

            configuration.draw(this.x + 1, this.y + yOffset, width - 2, widgetHeight);
            yOffset += heightOfOne;

        }

    }

    @EventTarget
    void _event(MouseMoveEvent mouseMoveEvent){

        isWidgetHovered =
                mouseMoveEvent.getX() >= x &&
                        mouseMoveEvent.getX() <= x + width &&
                        mouseMoveEvent.getY() >= y &&
                        mouseMoveEvent.getY() <= y + widgetHeight;

    }

    public static class Config extends Widget {

        private int
                x, y,
                width, height;

        public String name;

        public static String selected = "";
        private boolean isUsed = true;

        public Config(String name) {
            this.name = name;
        }

        public void draw(int x, int y, int width, int height) {

            isUsed = sisicat.main.Config.loaded.equals(name);

            this.x = x;
            this.y = y;

            this.width = width;
            this.height = height;

            Render.drawRectangle(x, y, width, height, Color.c25, selected.equals(this.name) ? 255 : 0);
            Text.getMenuFont(selected.equals(this.name)).renderVCenteredText(name, x + 10, y + (float) Math.floor((float) height / 2), isUsed ? Widget.themeColor : Color.c205);

        }

        @EventTarget
        void _event(MouseEvent mouseEvent){

            if(Tabs.getSelectedTab() != tab || !MineSense.mainRender.isMenuOpened || mouseEvent.getIsAlreadyUsed() || !isWidgetHovered)
                return;

            boolean isHovered =
                    mouseEvent.getX() >= this.x &&
                    mouseEvent.getX() <= this.x + this.width &&
                    mouseEvent.getY() >= this.y - 1 &&
                    mouseEvent.getY() <= this.y + this.height + 1;

            if(mouseEvent.getAction() == 1 && isHovered) {
                Window.isAnyInteractableHovered = true;
                selected = this.name;
                FunctionsManager.getFunctionByName("Configuration").getSettingByName("Edit").setStringValue(selected);
            }

        }

    }

}
