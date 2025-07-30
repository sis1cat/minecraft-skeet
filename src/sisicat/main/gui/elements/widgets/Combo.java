package sisicat.main.gui.elements.widgets;

import com.darkmagician6.eventapi.EventTarget;
import com.darkmagician6.eventapi.types.Priority;
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

import java.util.ArrayList;

public class Combo extends Widget {

    private static final int DEFAULT_MAX_WIDGET_WIDTH = 200;
    private static final int DEFAULT_MIN_WIDGET_WIDTH = 40;
    private static final int DEFAULT_WIDGET_HEIGHT = 20;

    private int
            maxWidgetWidth = 200,
            minWidgetWidth = 40,
            widgetHeight = 7;

    private boolean isWidgetHovered = false;
    private int x, y, width;

    public final Object link;

    private final int tab;

    private static final float[][] WIDGET_DEFAULT_COLOR =
            {
                    new float[]{30, 30, 30},
                    new float[]{35, 35, 35}
            };

    private static final float[][] WIDGET_HOVERED_COLOR =
            {
                    new float[]{40, 40, 40},
                    new float[]{45, 45, 45}
            };

    private final FunctionSetting functionSetting;
    final String text;

    public int occupiedSpace = 0;

    public Combo(FunctionSetting functionSetting, String text, Object link, int tab){
        this.functionSetting = functionSetting;
        this.text = text;
        this.link = link;
        this.tab = tab;
    }

    public Combo(FunctionSetting functionSetting, String text, int tab){
        this.functionSetting = functionSetting;
        this.text = text;
        this.link = null;
        this.tab = tab;
    }

    public void draw(int x, int y, int groupWidth, Vec2 gameWindowSize) {

        widgetHeight = Math.round(DEFAULT_WIDGET_HEIGHT * Window.windowScale);
        minWidgetWidth = Math.round(DEFAULT_MIN_WIDGET_WIDTH * Window.windowScale);
        maxWidgetWidth = Math.round(DEFAULT_MAX_WIDGET_WIDTH * Window.windowScale);

        occupiedSpace = widgetHeight;

        if(!text.isEmpty()) {
            Text.getMenuFont().renderText(text, x + Math.round(20 * Window.windowScale), y, Color.c205);
            occupiedSpace += Text.getMenuFont().getFontHeight() + Math.round(4 * Window.windowScale);
        }

        width = Math.min(Math.max(groupWidth - Math.round(100 * Window.windowScale), minWidgetWidth), maxWidgetWidth);

        this.x = x + Math.round(20 * Window.windowScale);
        this.y = y + (text.isEmpty() ? 0 : Math.round(4 * Window.windowScale) + Text.getMenuFont().getFontHeight());

        if(isWidgetHovered)
            Window.isAnyInteractableHovered = true;

        float alpha = 255;

        Render.drawRectangleBorders(this.x, this.y, width, widgetHeight,  1, Color.c12, alpha);

        boolean triggerColor = isWidgetHovered || isOpened;

        Render.drawGradientRectangle(
                this.x + 1, this.y + 1,
                width - 2, widgetHeight - 2,
                triggerColor ? WIDGET_HOVERED_COLOR[1] : WIDGET_DEFAULT_COLOR[0],
                triggerColor ? WIDGET_HOVERED_COLOR[1] : WIDGET_DEFAULT_COLOR[0],
                triggerColor ? WIDGET_HOVERED_COLOR[0] : WIDGET_DEFAULT_COLOR[1],
                triggerColor ? WIDGET_HOVERED_COLOR[0] : WIDGET_DEFAULT_COLOR[1],
                alpha
        );

        String selectedOption = functionSetting.getStringValue();

        while((int) Math.round(6 * Window.windowScale) + Text.getMenuFont().getStringWidth(selectedOption) > width - (int) Math.round(15 * Window.windowScale))
            selectedOption = selectedOption.substring(0, selectedOption.toCharArray().length - 2);

        Text.getMenuFont().renderVCenteredText(
                selectedOption,
                this.x + (int) Math.round(6 * Window.windowScale),
                (int)(this.y + (float) widgetHeight / 2 ),
                new float[]{155, 155, 155}
        );

        float[] noAlphaColor1 = new float[]{WIDGET_HOVERED_COLOR[1][0], WIDGET_HOVERED_COLOR[1][1], WIDGET_HOVERED_COLOR[1][2], 0};
        float[] noAlphaColor2 = new float[]{WIDGET_DEFAULT_COLOR[1][0], WIDGET_DEFAULT_COLOR[1][1], WIDGET_DEFAULT_COLOR[1][2], 0};

        Render.drawGradientRectangle(
                this.x + width - (int) Math.round(45 * Window.windowScale),
                this.y + 1,
                (int) Math.round(30 * Window.windowScale),
                widgetHeight - 2,
                triggerColor ? noAlphaColor1 : noAlphaColor2,
                triggerColor ? WIDGET_HOVERED_COLOR[1] : WIDGET_DEFAULT_COLOR[0],
                triggerColor ? noAlphaColor1 : noAlphaColor2,
                triggerColor ? WIDGET_HOVERED_COLOR[0] : WIDGET_DEFAULT_COLOR[1],
                alpha
        );

        Render.drawTriangle2(
                this.x + width - (int) Math.round(12 * Window.windowScale),
                this.y + (int) Math.round((float) widgetHeight / 2) - 2,
                5, 3,
                new float[]{0, 0, 0},
                alpha
        );

        Render.drawTriangle2(
                this.x + width - (int) Math.round(12 * Window.windowScale),
                this.y + (int) Math.round((float) widgetHeight / 2) - 1,
                5, 3,
                new float[]{150, 150, 150},
                alpha
        );

    }

    private boolean isOpened = false;
    private int hoveredOption = -1;

    public void drawOptions(Vec2 gameWindowSize){

        if(!isOpened)
            return;

        Window.isAnyInteractableHovered = true;

        int elementsCount = functionSetting.getOptionsList().size();
        int height = elementsCount * Math.round(18 * Window.windowScale) + 2 * (elementsCount - 1) + 2;

        Render.drawRectangleBorders(this.x, this.y + widgetHeight + 1, width, height, 1, Color.c12, 255);
        Render.drawRectangle(this.x + 1, this.y + widgetHeight + 2, width - 2, height - 2, new float[]{35, 35, 35}, 255);

        if(hoveredOption != -1) {

            int i = hoveredOption;
            int startY = this.y + widgetHeight + 2;
            int buttonHeight = Math.round(18 * Window.windowScale);
            int buttonY = startY + i * (buttonHeight + 2);

            Render.drawRectangle(
                    this.x + 1,
                    buttonY,
                    width - 2, buttonHeight, new float[]{25, 25, 25}, 255
            );

        }

        for(int i = 0; i < functionSetting.getOptionsList().size(); i++) {

            int startY = this.y + widgetHeight + 2;
            int buttonHeight = Math.round(18 * Window.windowScale);
            int textY = startY + i * (buttonHeight + 2) + buttonHeight / 2;

            ArrayList<String> options = functionSetting.getOptionsList();

            Text.getMenuFont(hoveredOption == i || functionSetting.getStringValue().equals(functionSetting.getOptionsList().get(i))).renderVCenteredText(
                    options.get(i),
                    this.x + Math.round(6 * Window.windowScale),
                    textY,
                    functionSetting.getStringValue().equals(options.get(i)) ? Widget.themeColor : Color.c205
            );

        }

    }

    @EventTarget
    void _event(MouseMoveEvent mouseMoveEvent){

        isWidgetHovered =
                mouseMoveEvent.getX() >= this.x &&
                        mouseMoveEvent.getY() >= this.y &&
                        mouseMoveEvent.getX() <= this.x + width &&
                        mouseMoveEvent.getY() < this.y + widgetHeight &&
                        this.parent.isHovered();

        hoveredOption = -1;

        for(int i = 0; i < functionSetting.getOptionsList().size(); i++){

            int buttonHeight = Math.round(18 * Window.windowScale) + 2;
            int buttonY = this.y + widgetHeight + i * buttonHeight;

            if(
                mouseMoveEvent.getY() >= buttonY &&
                mouseMoveEvent.getY() <= buttonY + buttonHeight &&
                mouseMoveEvent.getX() >= this.x &&
                mouseMoveEvent.getX() <= this.x + width
            ) {
                hoveredOption = i;
            }

        }

    }

    @EventTarget(value = Priority.HIGHEST)
    void _event(MouseEvent mouseEvent){

        if(Tabs.getSelectedTab() != tab || !MineSense.mainRender.isMenuOpened || mouseEvent.getIsAlreadyUsed())
            return;

        if(link instanceof Function function && !function.canBeActivated())
            return;

        if(link instanceof FunctionSetting functionSetting && !functionSetting.getCanBeActivated())
            return;

        if(hoveredOption == -1 && !isWidgetHovered)
            isOpened = false;

        if(
                isWidgetHovered &&
                        mouseEvent.getAction() == 0 &&
                        mouseEvent.getButton() == 0
        ) {

            isOpened = !isOpened;

        }

        if(!isOpened)
            return;

        if(hoveredOption != -1)
            mouseEvent.setIsAlreadyUsed(true);

        if(hoveredOption != -1 && mouseEvent.getButton() == 0 && mouseEvent.getAction() == 0) {

            ArrayList<String> optionsList = functionSetting.getOptionsList();
            String hoveredOptionName = optionsList.get(hoveredOption);

            functionSetting.setStringValue(hoveredOptionName);

            isOpened = false;

        }

    }

}

