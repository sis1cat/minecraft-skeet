package sisicat.main.gui.elements.widgets;

import com.darkmagician6.eventapi.EventTarget;
import org.lwjgl.glfw.GLFW;
import sisicat.events.KeyEvent;
import sisicat.events.MouseEvent;
import sisicat.events.MouseMoveEvent;
import sisicat.main.functions.FunctionSetting;
import sisicat.main.gui.elements.Window;
import sisicat.main.utilities.Color;
import sisicat.main.utilities.Render;
import sisicat.main.utilities.Text;

public class Edit extends Widget {

    private static final int DEFAULT_MAX_WIDGET_WIDTH = 200;
    private static final int DEFAULT_MIN_WIDGET_WIDTH = 40;
    private static final int DEFAULT_WIDGET_HEIGHT = 20;

    private int
            maxWidgetWidth = 200,
            minWidgetWidth = 40,
            widgetHeight = 20;

    private boolean isWidgetHovered = false;
    private int x, y, width;
    private final int tab;

    private final FunctionSetting functionSetting;

    public int occupiedSpace = 0;

    private boolean isEditing = false;

    public Edit(FunctionSetting functionSetting, int tab) {

        this.functionSetting = functionSetting;
        this.tab = tab;

    }

    public void draw(int x, int y, int groupWidth) {

        widgetHeight = Math.round(DEFAULT_WIDGET_HEIGHT * Window.windowScale);
        minWidgetWidth = Math.round(DEFAULT_MIN_WIDGET_WIDTH * Window.windowScale);
        maxWidgetWidth = Math.round(DEFAULT_MAX_WIDGET_WIDTH * Window.windowScale);

        occupiedSpace = widgetHeight - Math.round(10 * Window.windowScale) + 5;

        width = Math.min(Math.max(groupWidth - Math.round(100 * Window.windowScale), minWidgetWidth), maxWidgetWidth);

        this.x = x + Math.round(20 * Window.windowScale);
        this.y = y;

        if (isWidgetHovered)
            Window.isAnyInteractableHovered = true;

        Render.drawRectangleBorders(this.x, y, width, widgetHeight, 1, Color.c12, 255);
        Render.drawRectangleBorders(this.x + 1, y + 1, width - 2, widgetHeight - 2, 1, Color.c50, 255);
        Render.drawRectangleBorders(this.x + 2, y + 2, width - 4, widgetHeight - 4, 1, Color.c12, 255);
        Render.drawRectangle(this.x + 3, y + 3, width - 6, widgetHeight - 6, Color.c25, 255);

        Text.getMenuFont().renderVCenteredText(functionSetting.getStringValue() + "_", this.x + 5, this.y + Math.round((float) widgetHeight / 2), isEditing ? Widget.themeColor : Color.c205);

    }

    @EventTarget
    void _event(MouseMoveEvent mouseMoveEvent) {

        this.isWidgetHovered =
                mouseMoveEvent.getX() >= this.x && mouseMoveEvent.getX() < this.x + this.width &&
                mouseMoveEvent.getY() >= this.y && mouseMoveEvent.getY() < this.y + this.widgetHeight;

    }

    @EventTarget
    void _event(MouseEvent mouseEvent) {

        if(mouseEvent.getAction() == 1 && mouseEvent.getButton() == 0)
            this.isEditing = this.isWidgetHovered;

    }

    @EventTarget
    void _event(KeyEvent keyEvent) {

        if(!isEditing)
            return;

        if(keyEvent.getAction() == 1 && keyEvent.getKeyCode() == GLFW.GLFW_KEY_BACKSPACE)
            this.functionSetting.setStringValue(this.functionSetting.getStringValue().substring(0, this.functionSetting.getStringValue().length() - 1));

        if(keyEvent.getCodePoint() != 0)
            this.functionSetting.setStringValue(this.functionSetting.getStringValue() + (char) keyEvent.getCodePoint());

    }

}
