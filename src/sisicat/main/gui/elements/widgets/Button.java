package sisicat.main.gui.elements.widgets;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.world.phys.Vec2;
import sisicat.MineSense;
import sisicat.events.MouseEvent;
import sisicat.events.MouseMoveEvent;
import sisicat.main.functions.FunctionSetting;
import sisicat.main.gui.elements.Window;
import sisicat.main.utilities.Color;
import sisicat.main.utilities.Render;
import sisicat.main.utilities.Text;

public class Button extends Widget {

    private static final int DEFAULT_MAX_WIDGET_WIDTH = 200;
    private static final int DEFAULT_MIN_WIDGET_WIDTH = 40;
    private static final int DEFAULT_WIDGET_HEIGHT = 25;

    private int
            maxWidgetWidth = 200,
            minWidgetWidth = 40,
            widgetHeight = 25;

    private boolean isWidgetHovered = false;
    private int x, y, width;

    private final int tab;

    private static final float[][] WIDGET_DEFAULT_COLOR =
            {
                    new float[]{35, 35, 35},
                    new float[]{30, 30, 30}
            };
    private static float[][] WIDGET_ACTIVE_COLOR =
            {
                    new float[]{30, 30, 30},
                    new float[]{20, 20, 20}
            };
    private static final float[][] WIDGET_HOVERED_COLOR =
            {
                    new float[]{40, 40, 40},
                    new float[]{35, 35, 35}
            };

    public int occupiedSpace = 0;
    final String text;
    private final FunctionSetting functionSetting;
    private boolean isClicked = false;

    public Button(FunctionSetting functionSetting, String text, int tab){
        this.functionSetting = functionSetting;
        this.text = text;
        this.tab = tab;
    }

    public void draw(int x, int y, int groupWidth, Vec2 gameWindowSize){

        widgetHeight = Math.round(DEFAULT_WIDGET_HEIGHT * Window.windowScale);
        minWidgetWidth = Math.round(DEFAULT_MIN_WIDGET_WIDTH * Window.windowScale);
        maxWidgetWidth = Math.round(DEFAULT_MAX_WIDGET_WIDTH * Window.windowScale);

        occupiedSpace = widgetHeight - Math.round(10 * Window.windowScale) + 8;

        width = Math.min(Math.max(groupWidth - Math.round(100 * Window.windowScale), minWidgetWidth), maxWidgetWidth);

        this.x = x + Math.round(20 * Window.windowScale);
        this.y = y;

        Render.drawRectangleBorders(this.x, this.y, width, widgetHeight, 1, Color.c12, 255);
        Render.drawRectangleBorders(this.x + 1, this.y + 1, width - 2, widgetHeight - 2, 1, new float[]{50, 50, 50}, 255);

        Render.drawGradientRectangle(
                this.x + 2, this.y + 2,
                width - 4, widgetHeight - 4,
                isWidgetHovered ? isClicked ? WIDGET_ACTIVE_COLOR[1] : WIDGET_HOVERED_COLOR[1] : WIDGET_DEFAULT_COLOR[1],
                isWidgetHovered ? isClicked ? WIDGET_ACTIVE_COLOR[1] : WIDGET_HOVERED_COLOR[1] : WIDGET_DEFAULT_COLOR[1],
                isWidgetHovered ? isClicked ? WIDGET_ACTIVE_COLOR[0] : WIDGET_HOVERED_COLOR[0] : WIDGET_DEFAULT_COLOR[0],
                isWidgetHovered ? isClicked ? WIDGET_ACTIVE_COLOR[0] : WIDGET_HOVERED_COLOR[0] : WIDGET_DEFAULT_COLOR[0],
                Widget.menuAlpha
        );

        Text.getMenuFont().renderHVCenteredText(text, this.x + (int) ((float) width / 2 + 1), (int) (this.y + (float) widgetHeight / 2 + 1), new float[]{0, 0, 0, 100});
        Text.getMenuFont().renderHVCenteredText(text, this.x + (int) ((float) width / 2), (int) (this.y + (float) widgetHeight / 2), Color.c205);

    }

    @EventTarget
    void _event(MouseMoveEvent mouseMoveEvent){

        isWidgetHovered =
                mouseMoveEvent.getX() >= x &&
                mouseMoveEvent.getX() <= x + width &&
                mouseMoveEvent.getY() >= y &&
                mouseMoveEvent.getY() <= y + widgetHeight &&
                this.parent.isHovered();

        if(!isWidgetHovered)
            isClicked = false;

    }

    @EventTarget
    void _event(MouseEvent mouseEvent){

        if(Tabs.getSelectedTab() != tab || !MineSense.mainRender.isMenuOpened || mouseEvent.getIsAlreadyUsed())
            return;

        if(mouseEvent.getAction() == 0 && isWidgetHovered) {

            isClicked = false;
            functionSetting.isClicked = true;

        }

        if(isWidgetHovered && mouseEvent.getButton() == 0 && mouseEvent.getAction() == 1) {
            isClicked = true;
            Window.isAnyInteractableHovered = true;
        }

    }

}
