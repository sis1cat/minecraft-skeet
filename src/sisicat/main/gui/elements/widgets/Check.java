package sisicat.main.gui.elements.widgets;


import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.world.phys.Vec2;
import sisicat.MineSense;
import sisicat.events.MouseEvent;
import sisicat.events.MouseMoveEvent;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;
import sisicat.main.gui.elements.Window;
import sisicat.main.utilities.Color;
import sisicat.main.utilities.Render;
import sisicat.main.utilities.Text;

public class Check extends Widget.AdditionAbleWidget {

    private static final int DEFAULT_WIDGET_WIDTH = 8, DEFAULT_WIDGET_HEIGHT = 8;
    private static final float[][] WIDGET_DISABLED_COLOR =
            {
                    new float[]{75, 75, 75},
                    new float[]{50, 50, 50}
            };
    private static float[][] WIDGET_ENABLED_COLOR =
            {
                    new float[]{themeColor[0], themeColor[1], themeColor[2]},
                    new float[]{themeColor[0] - 35, themeColor[1] - 35, themeColor[2] - 35}
            };
    private static final float[][] WIDGET_HOVERED_COLOR =
            {
                    new float[]{85, 85, 85},
                    new float[]{55, 55, 55}
            };

    private Function function;
    private FunctionSetting functionSetting;

    private final String text;
    private final boolean isSpecial;

    int x, y;
    private int
            widgetWidth,
            widgetHeight;
    boolean isWidgetHovered = false;

    private final Object element;
    private final int tab;

    public int occupiedSpace = 0;

    public Check(Function function, boolean isSpecial, int tab){
        this.function = function;
        this.text = function.getName();
        this.isSpecial = isSpecial;
        this.element = function;
        this.tab = tab;
    }

    public Check(FunctionSetting functionSetting, boolean isSpecial, int tab){
        this.functionSetting = functionSetting;
        this.text = functionSetting.getName();
        this.isSpecial = isSpecial;
        this.element = functionSetting;
        this.tab = tab;
    }

    public Check(Function function, String text, boolean isSpecial, int tab){
        this.function = function;
        this.text = text;
        this.isSpecial = isSpecial;
        this.element = function;
        this.tab = tab;
    }

    public Check(FunctionSetting functionSetting, String text, boolean isSpecial, int tab){
        this.functionSetting = functionSetting;
        this.text = text;
        this.isSpecial = isSpecial;
        this.element = functionSetting;
        this.tab = tab;
    }


    public void draw(int x, int y, Vec2 gameWindowSize){

        WIDGET_ENABLED_COLOR =
                new float[][]{
                        new float[]{themeColor[0], themeColor[1], themeColor[2]},
                        new float[]{themeColor[0] - 35, themeColor[1] - 35, themeColor[2] - 35}
                };

        this.x = x;
        this.y = y;

        widgetWidth = Math.round(DEFAULT_WIDGET_WIDTH * Window.windowScale);
        widgetHeight = Math.round(DEFAULT_WIDGET_HEIGHT * Window.windowScale);

        occupiedSpace = widgetHeight;

        boolean state;

        if(element instanceof Function)
            state = function.canBeActivated();
         else
            state = functionSetting.getCanBeActivated();

        Render.drawRectangleBorders(
                x, y,
                widgetWidth, widgetHeight,
                1, Color.c12,
                255
        );

        Render.drawGradientRectangle(
                x + 1, y + 1,
                widgetWidth - 2, widgetHeight - 2,
                state ? WIDGET_ENABLED_COLOR[1] : isWidgetHovered ? WIDGET_HOVERED_COLOR[1] : WIDGET_DISABLED_COLOR[1],
                state ? WIDGET_ENABLED_COLOR[1] : isWidgetHovered ? WIDGET_HOVERED_COLOR[1] : WIDGET_DISABLED_COLOR[1],
                state ? WIDGET_ENABLED_COLOR[0] : isWidgetHovered ? WIDGET_HOVERED_COLOR[0] : WIDGET_DISABLED_COLOR[0],
                state ? WIDGET_ENABLED_COLOR[0] : isWidgetHovered ? WIDGET_HOVERED_COLOR[0] : WIDGET_DISABLED_COLOR[0],
                255
        );

        Text.getMenuFont().renderText(text, x + widgetWidth + Math.round(12 * Window.windowScale), y, isSpecial ? Color.cSpecial : Color.c205);

    }

    public Check addBind(Bind bind){

        this.bind = bind;
        return this;

    }

    public Check addColor(sisicat.main.gui.elements.widgets.Color color){

        this.color = color;
        return this;

    }

    private boolean startUse = false;

    @EventTarget
    void _event(MouseEvent mouseEvent){

        if(Tabs.getSelectedTab() != tab || !MineSense.mainRender.isMenuOpened || mouseEvent.getIsAlreadyUsed() || Window.isAnyInteractableHovered)
            return;

        if(isWidgetHovered && mouseEvent.getAction() == 1 &&
                mouseEvent.getButton() == 0)
            startUse = true;

        if (
            isWidgetHovered &&
            mouseEvent.getAction() == 0 &&
            mouseEvent.getButton() == 0 && startUse
        ) {
            if(element instanceof  Function)
                function.toggleCanBeActivated();
            else
                functionSetting.setCanBeActivated(!functionSetting.getCanBeActivated());
        }

        if(mouseEvent.getAction() == 0 &&
                mouseEvent.getButton() == 0)
            startUse = false;

    }

    @EventTarget
    void _event(MouseMoveEvent mouseMoveEvent){

        isWidgetHovered =
                mouseMoveEvent.getX() >= x &&
                mouseMoveEvent.getX() <= x + widgetWidth + 150 &&
                mouseMoveEvent.getY() >= y - Math.round(3 * Window.windowScale) &&
                mouseMoveEvent.getY() <= y + widgetHeight + Math.round(3 * Window.windowScale) &&
                this.parent.isHovered();

        if(isWidgetHovered)
            Window.isAnyInteractableHovered = true;

    }

}
