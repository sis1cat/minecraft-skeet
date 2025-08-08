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

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Slider extends Widget {

    private static final int DEFAULT_MAX_WIDGET_WIDTH = 200;
    private static final int DEFAULT_MIN_WIDGET_WIDTH = 40;
    private static final int DEFAULT_WIDGET_HEIGHT = 7;

    private int 
            maxWidgetWidth = 200,
            minWidgetWidth = 40,
            widgetHeight = 7;
    
    
    private boolean isWidgetHovered = false;
    private int x, y, width;

    public final Object link;
    public final String[] stringLink;

    private final int tab;

    private static final float[][] WIDGET_DEFAULT_COLOR =
            {
                    new float[]{70, 70, 70},
                    new float[]{50, 50, 50}
            };
    private static float[][] WIDGET_ACTIVE_COLOR =
            {
                    new float[]{themeColor[0] - 50, themeColor[1] - 50, themeColor[2] - 50},
                    new float[]{themeColor[0], themeColor[1], themeColor[2]}
            };
    private static final float[][] WIDGET_HOVERED_COLOR =
            {
                    new float[]{60, 60, 60},
                    new float[]{80, 80, 80}
            };
    private final FunctionSetting functionSetting;
    final String text;
    private final float value, minValue, maxValue;

    public int occupiedSpace = 0;

    public Slider(FunctionSetting functionSetting, int tab) {
        this.functionSetting = functionSetting;
        this.value = functionSetting.getFloatValue();
        this.minValue = functionSetting.getMinFloatValue();
        this.maxValue = functionSetting.getMaxFloatValue();
        this.text = functionSetting.getName();
        this.link = null;
        this.tab = tab;
        this.stringLink = null;
    }

    public Slider(FunctionSetting functionSetting, String text, int tab){
        this.functionSetting = functionSetting;
        this.value = functionSetting.getFloatValue();
        this.minValue = functionSetting.getMinFloatValue();
        this.maxValue = functionSetting.getMaxFloatValue();
        this.text = text;
        this.link = null;
        this.tab = tab;
        this.stringLink = null;
    }

    public Slider(FunctionSetting functionSetting, String text, Object link, int tab){
        this.functionSetting = functionSetting;
        this.value = functionSetting.getFloatValue();
        this.minValue = functionSetting.getMinFloatValue();
        this.maxValue = functionSetting.getMaxFloatValue();
        this.text = text;
        this.link = link;
        this.tab = tab;
        this.stringLink = null;
    }

    public Slider(FunctionSetting functionSetting, String text, Object link, int tab, String[] stringLink){
        this.functionSetting = functionSetting;
        this.value = functionSetting.getFloatValue();
        this.minValue = functionSetting.getMinFloatValue();
        this.maxValue = functionSetting.getMaxFloatValue();
        this.text = text;
        this.link = link;
        this.tab = tab;
        this.stringLink = stringLink;
    }

    public void draw(int x, int y, int groupWidth, Vec2 gameWindowSize){

        occupiedSpace = 0;

        WIDGET_ACTIVE_COLOR =
                new float[][]{
                        new float[]{themeColor[0] - 50, themeColor[1] - 50, themeColor[2] - 50},
                        new float[]{themeColor[0], themeColor[1], themeColor[2]}
                };

        if(!text.isEmpty()) {
            Text.getMenuFont().renderText(text, x + Math.round(20 * Window.windowScale), y, Color.c205);
            occupiedSpace += Text.getMenuFont().getBaseAscender();
            occupiedSpace += Math.round(6 * Window.windowScale);
        }

        widgetHeight = Math.round(DEFAULT_WIDGET_HEIGHT * Window.windowScale);
        minWidgetWidth = Math.round(DEFAULT_MIN_WIDGET_WIDTH * Window.windowScale);
        maxWidgetWidth = Math.round(DEFAULT_MAX_WIDGET_WIDTH * Window.windowScale);

        occupiedSpace += widgetHeight;

        width = Math.min(Math.max(groupWidth - Math.round(100 * Window.windowScale), minWidgetWidth), maxWidgetWidth);

        this.x = x + Math.round(20 * Window.windowScale);
        this.y = y + (text.isEmpty() ? 0 : Math.round(Text.getMenuFont().getBaseAscender() + 6 * Window.windowScale));

        Render.drawRectangleBorders(this.x, this.y, width, widgetHeight, 1, Color.c12, 255);

        boolean triggerColor = isWidgetHovered || isDragging;

        Render.drawGradientRectangle(
                this.x + 1, this.y + 1,
                width - 2, widgetHeight - 2,
                triggerColor ? WIDGET_HOVERED_COLOR[1] : WIDGET_DEFAULT_COLOR[0],
                triggerColor ? WIDGET_HOVERED_COLOR[1] : WIDGET_DEFAULT_COLOR[0],
                triggerColor ? WIDGET_HOVERED_COLOR[0] : WIDGET_DEFAULT_COLOR[1],
                triggerColor ? WIDGET_HOVERED_COLOR[0] : WIDGET_DEFAULT_COLOR[1],
                255
        );

        float delta = (functionSetting.getFloatValue() - functionSetting.getMinFloatValue()) / (functionSetting.getMaxFloatValue() - functionSetting.getMinFloatValue());

        int valueWidth = (int)(width * delta);

        valueWidth = Math.min(valueWidth, width - 2);

        Render.drawGradientRectangle(
                this.x + 1, this.y + 1,
                valueWidth, widgetHeight - 2,
                WIDGET_ACTIVE_COLOR[0],
                WIDGET_ACTIVE_COLOR[0],
                WIDGET_ACTIVE_COLOR[1],
                WIDGET_ACTIVE_COLOR[1],
                255
        );

        String textValue = truncateToUnitSize(functionSetting.getFloatValue(), functionSetting.getUnitSize()) + functionSetting.getUnitName();

        Text.getMenuBoldFont().renderOutlinedText(textValue, this.x + (valueWidth - (float) Text.getMenuBoldFont().getStringWidth(textValue) / 2), this.y + widgetHeight - (int)((float) Text.getMenuBoldFont().getBaseAscender() / 2 + 1), Color.c190, new float[]{0, 0, 0, 150});

    }

    private boolean isDragging = false;

    @EventTarget
    void _event(MouseMoveEvent mouseMoveEvent){

        isWidgetHovered =
                mouseMoveEvent.getX() >= this.x &&
                mouseMoveEvent.getX() <= this.x + this.width &&
                mouseMoveEvent.getY() >= this.y - Math.round(5 * Window.windowScale) &&
                mouseMoveEvent.getY() <= this.y + this.widgetHeight + Math.round(4 * Window.windowScale) &&
                this.parent.isHovered();

        if(isDragging) {

            int sliderMouseX = (int) Math.min(mouseMoveEvent.getX() - (this.x + 1), (width - 2));
            float pixelsInUnit = (width - 2) / (functionSetting.getMaxFloatValue() - functionSetting.getMinFloatValue());

            functionSetting.setFloatValue(
                    Math.round(
                            (sliderMouseX / pixelsInUnit) / functionSetting.getUnitSize()
                    ) * functionSetting.getUnitSize() + functionSetting.getMinFloatValue()
            );

        }

    }

    @EventTarget
    void _event(MouseEvent mouseEvent){

        if(Tabs.getSelectedTab() != tab || !MineSense.mainRender.isMenuOpened || mouseEvent.getIsAlreadyUsed())
            return;

        if(link != null){
            if(link instanceof Function && !((Function) link).canBeActivated()) return;
            if(link instanceof FunctionSetting && stringLink == null && !((FunctionSetting) link).getCanBeActivated()) return;
        }

        if(stringLink != null && link instanceof FunctionSetting) {

            boolean contains = false;

            for(String s : this.stringLink)
                if (((FunctionSetting) link).getStringValue().equals(s)) {
                    contains = true;
                    break;
                }

            if(!contains)
                return;

        }

        if(mouseEvent.getAction() == 0)
            isDragging = false;

        if(this.isWidgetHovered && mouseEvent.getAction() == 1) {
            Window.isAnyInteractableHovered = true;
            isDragging = true;

            int sliderMouseX = (int) Math.min(mouseEvent.getX() - (this.x + 1), (width - 2));
            float pixelsInUnit = (width - 2) / (functionSetting.getMaxFloatValue() - functionSetting.getMinFloatValue());

            functionSetting.setFloatValue(
                Math.round(
                        (sliderMouseX / pixelsInUnit) / functionSetting.getUnitSize()
                ) * functionSetting.getUnitSize() + functionSetting.getMinFloatValue()
            );

        }

    }

    public String truncateToUnitSize(float value, float unitSize) {

        BigDecimal unit = new BigDecimal(Float.toString(unitSize));
        int scale = Math.max(0, unit.stripTrailingZeros().scale());

        BigDecimal bdValue = new BigDecimal(Float.toString(value));

        bdValue = bdValue.setScale(scale, RoundingMode.DOWN);

        return bdValue.toPlainString();

    }

}
