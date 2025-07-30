package sisicat.main.gui.elements.widgets;

import com.darkmagician6.eventapi.EventTarget;
import com.darkmagician6.eventapi.types.Priority;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import sisicat.MineSense;
import sisicat.events.MouseEvent;
import sisicat.events.MouseMoveEvent;
import sisicat.main.functions.FunctionSetting;
import sisicat.main.gui.elements.Window;
import sisicat.main.utilities.Render;
import sisicat.main.utilities.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static sisicat.main.gui.elements.Window.isAnyInteractableHovered;
import static sisicat.main.utilities.Color.hsvToRgb;

public class Color extends Widget{

    private static final int
            DEFAULT_WIDGET_WIDTH = 180,
            DEFAULT_WIDGET_HEIGHT = 175;

    private static final int
            DEFAULT_WIDGET_WIDTH_2 = 17,
            DEFAULT_WIDGET_HEIGHT_2 = 9;

    private static final int
            DEFAULT_WIDGET_WIDTH_3 = 100,
            DEFAULT_WIDGET_HEIGHT_3 = 40;

    private final int tab;

    private final FunctionSetting functionSetting;

    private int
        x, y,
        width, height,
        width2, height2,
        width3, height3;

    private boolean isWidgetHovered = false;

    public boolean isAvailable = false;

    public Color(FunctionSetting functionSetting, int tab){

        this.functionSetting = functionSetting;
        this.tab = tab;

    }

    public void draw(int x, int y, int groupX, int groupWidth, boolean isParentHovered, Vec2 gameWindowSize){

        this.x = groupX + groupWidth - Math.round(36 * Window.windowScale);
        this.y = y - 1;

        isAvailable = true;

        width = Math.round(DEFAULT_WIDGET_WIDTH * Window.windowScale + (Window.windowScale > 1 ? -3 * Window.windowScale : 0));
        height = Math.round(DEFAULT_WIDGET_HEIGHT * Window.windowScale + (Window.windowScale > 1 ? -3 * Window.windowScale : 0));
        width2 = Math.round(DEFAULT_WIDGET_WIDTH_2 * Window.windowScale);
        height2 = Math.round(DEFAULT_WIDGET_HEIGHT_2 * Window.windowScale);
        width3 = Math.round(DEFAULT_WIDGET_WIDTH_3 * Window.windowScale);
        height3 = Math.round(36 * Window.windowScale) + 4;

        Render.drawRectangleBorders(
                this.x, this.y,
                width2, height2,
                1,
                sisicat.main.utilities.Color.c12, 255
        );

        float[] color = hsvToRgb(functionSetting.getHSVAColor()[0], functionSetting.getHSVAColor()[1], functionSetting.getHSVAColor()[2]);

        float darkestValue = functionSetting.getHSVAColor()[2] / 2;

        float[] darkestColor = hsvToRgb(functionSetting.getHSVAColor()[0], functionSetting.getHSVAColor()[1],  darkestValue);

        Render.drawGradientRectangle(
                this.x + 1, this.y + 1, width2 - 2, height2 - 2,
                darkestColor, darkestColor,
                color, color, 255
        );

        if(isColorWidgetHovered && isOpened)
            isAnyInteractableHovered = true;

    }

    private boolean isOpened = false;
    private boolean isBufferOpened = false;

    private final float[]
            red = hsvToRgb(359, 0.85f, 0.85f),
            purple = hsvToRgb(300, 0.85f, 0.85f),
            blue = hsvToRgb(240, 0.85f, 0.85f),
            cyan = hsvToRgb(180, 0.85f, 0.85f),
            green = hsvToRgb(120, 0.85f, 0.85f),
            yellow = hsvToRgb(60, 0.85f, 0.85f);

    private float[] SVOffsets = {0, 0};
    private float
            HOffset,
            AOffset;
    
    public void drawPicker(Vec2 gameWindowSize){

        drawBuffer();

        if(!isOpened)
            return;

        HOffset = 1 - functionSetting.getHSVAColor()[0] / 360;
        SVOffsets[0] = functionSetting.getHSVAColor()[1];
        SVOffsets[1] = 1 - functionSetting.getHSVAColor()[2];
        AOffset = functionSetting.getHSVAColor()[3];

        calculatePicker();

        Render.drawRectangleBorders(
                this.x - 1, this.y + height2 + 1,
                width, height,1,
                sisicat.main.utilities.Color.c12, 255
        );

        Render.drawRectangleBorders(
                this.x, this.y + height2 + 2,
                width - 2, height - 2, 1,
                sisicat.main.utilities.Color.c60, 255
        );

        Render.drawRectangle(
                this.x + 1, this.y + height2 + 3,
                width - 4, height - 4,
                sisicat.main.utilities.Color.c40, 255
        );

        // dot picker

        Render.drawRectangleBorders(
                this.x + 3, this.y + height2 + 5,
                (int) Math.round(152 * Window.windowScale), (int) Math.round(152 * Window.windowScale), 1,
                sisicat.main.utilities.Color.c12, 255
        );

        Render.drawGradientRectangle(
                this.x + 4, this.y + height2 + 6,
                (int) Math.round(152 * Window.windowScale) - 2, (int) Math.round(152 * Window.windowScale) - 2,
                new float[]{0, 0, 0}, new float[]{0, 0, 0}, new float[]{255, 255, 255}, hsvToRgb(360 - HOffset * 360, 1f, 1f),
                255
        );

        // color slider

        Render.drawRectangleBorders(
                this.x + 4 + (int) Math.round(152 * Window.windowScale) + 2, this.y + height2 + 5,
                width - (int) Math.round(152 * Window.windowScale) - 11, (int) Math.round(152 * Window.windowScale), 1,
                sisicat.main.utilities.Color.c12, 255
        );

        int gradientOffset = ((Math.round(152 * Window.windowScale) - 2) / 6);

        Render.drawGradientRectangle(
                this.x + 5 + (int) Math.round(152 * Window.windowScale) + 2, this.y + height2 + 6,
                width - (int) Math.round(152 * Window.windowScale) - 13, gradientOffset,
                purple, purple, red, red,
                255
        );

        Render.drawGradientRectangle(
                this.x + 5 + (int) Math.round(152 * Window.windowScale) + 2, this.y + height2 + 6 + gradientOffset,
                width - (int) Math.round(152 * Window.windowScale) - 13, gradientOffset,
                blue, blue, purple, purple,
                255
        );

        Render.drawGradientRectangle(
                this.x + 5 + (int) Math.round(152 * Window.windowScale) + 2, this.y + height2 + 6 + gradientOffset * 2,
                width - (int) Math.round(152 * Window.windowScale) - 13, gradientOffset,
                cyan, cyan, blue, blue,
                255
        );

        Render.drawGradientRectangle(
                this.x + 5 + (int) Math.round(152 * Window.windowScale) + 2, this.y + height2 + 6 + gradientOffset * 3,
                width - (int) Math.round(152 * Window.windowScale) - 13, gradientOffset,
                green, green, cyan, cyan,
                255
        );

        Render.drawGradientRectangle(
                this.x + 5 + (int) Math.round(152 * Window.windowScale) + 2, this.y + height2 + 6 + gradientOffset * 4,
                width - (int) Math.round(152 * Window.windowScale) - 13, gradientOffset,
                yellow, yellow, green, green,
                255
        );

        Render.drawGradientRectangle(
                this.x + 5 + (int) Math.round(152 * Window.windowScale) + 2, this.y + height2 + 6 + gradientOffset * 5,
                width - (int) Math.round(152 * Window.windowScale) - 13, gradientOffset + ((int) Math.round(152 * Window.windowScale) - 2 - gradientOffset * 6),
                red, red, yellow, yellow,
                255
        );

        // alpha slider

        Render.drawRectangleBorders(
                this.x + 3, this.y + height2 + 5 + (int) Math.round(152 * Window.windowScale) + 3,
                (int) Math.round(152 * Window.windowScale), height - (int) Math.round(152 * Window.windowScale) - 11, 1,
                sisicat.main.utilities.Color.c12, 255
        );

        Render.drawRectangle(
                this.x + 4, this.y + height2 + 6 + (int) Math.round(152 * Window.windowScale) + 3,
                (int) Math.round(152 * Window.windowScale) - 2, height - (int) Math.round(152 * Window.windowScale) - 13,
                hsvToRgb((1 - HOffset) * 360, SVOffsets[0], 1 - SVOffsets[1]),
                functionSetting.getHSVAColor()[3] * 255
        );

        Render.drawRectangleBorders(
                this.x + 4 + (int)(SVOffsets[0] * Math.round(152 * Window.windowScale)) - 2, this.y + height2 + 6 + (int)(SVOffsets[1] * Math.round(152 * Window.windowScale)) - 2,
                4, 4, 1, sisicat.main.utilities.Color.c12, 255
        );

        Render.drawRectangle(
                this.x + 4 + (int)(SVOffsets[0] * Math.round(152 * Window.windowScale)) + 1 - 2, this.y + height2 + 6 + (int)(SVOffsets[1] * Math.round(152 * Window.windowScale)) + 1 - 2,
                2, 2, new float[]{255, 255, 255}, 75f
        );

        Render.drawRectangleBorders(
                this.x + 4 + (int) Math.round(152 * Window.windowScale) + 3, this.y + height2 + 5 + (int)(HOffset * Math.round(152 * Window.windowScale)) - 2,
                width - (int) Math.round(152 * Window.windowScale) - 13, 4, 1,
                sisicat.main.utilities.Color.c12, 255
        );

        Render.drawRectangle(
                this.x + 4 + (int) Math.round(152 * Window.windowScale) + 4, this.y + height2 + 5 + (int)(HOffset * Math.round(152 * Window.windowScale)) - 1,
                width - (int) Math.round(152 * Window.windowScale) - 15, 2,
                new float[]{255, 255, 255}, 75f
        );

        Render.drawRectangleBorders(
                this.x + 3 + (int)(functionSetting.getHSVAColor()[3] * Math.round(152 * Window.windowScale)) - 2, this.y + height2 + 6 + (int) Math.round(152 * Window.windowScale) + 3,
                4,height - (int) Math.round(152 * Window.windowScale) - 13, 1,
                sisicat.main.utilities.Color.c12, 255

        );

        Render.drawRectangle(
                this.x + 3 + (int)(functionSetting.getHSVAColor()[3] * Math.round(152 * Window.windowScale)) - 1, this.y + height2 + 6 + (int) Math.round(152 * Window.windowScale) + 4,
                2,height - (int) Math.round(152 * Window.windowScale) - 15,
                new float[]{255, 255, 255}, 75f

        );

    }

    private int hoveredOption = -1;

    private final ArrayList<String> options = new ArrayList<>(List.of("Copy", "Paste"));

    public void drawBuffer() {

        if(!isBufferOpened)
            return;

        Window.isAnyInteractableHovered = true;

        int elementsCount = 2;
        int height = elementsCount * Math.round(18 * Window.windowScale) + 2 * (elementsCount - 1) + 2;

        Render.drawRectangleBorders(this.x - width3 - 1, this.y - 1, width3, height3, 1, sisicat.main.utilities.Color.c12, 255);
        Render.drawRectangle(this.x - width3, this.y, width3 - 2, height3 - 2, sisicat.main.utilities.Color.c35, 255);

        if(hoveredOption != -1) {

            int i = hoveredOption;
            int startY = this.y;
            int buttonHeight = Math.round(18 * Window.windowScale);
            int buttonY = startY + i * (buttonHeight + 2);

            Render.drawRectangle(
                    this.x - width3,
                    buttonY,
                    width3 - 2, buttonHeight, new float[]{25, 25, 25}, 255
            );

        }

        for(int i = 0; i < options.size(); i++) {

            int startY = this.y;
            int buttonHeight = Math.round(18 * Window.windowScale);
            int textY = startY + i * (buttonHeight + 2) + buttonHeight / 2;

            Text.getMenuFont(hoveredOption == i).renderVCenteredText(
                    options.get(i),
                    this.x + Math.round(6 * Window.windowScale) - width3,
                    textY,
                    sisicat.main.utilities.Color.c205
            );

        }

    }

    private void calculatePicker(){

        if(isDPDragging) {

            float
                    xOffset = (float) Mth.clamp(mouseX - (this.x + 4), 0, Math.round(152 * Window.windowScale) - 2) / Math.round(152f * Window.windowScale),
                    yOffset = (float) Mth.clamp(mouseY - (this.y + height2 + 6), 0, Math.round(152 * Window.windowScale) - 2) / Math.round(152f * Window.windowScale);

            SVOffsets = new float[]{xOffset, yOffset};

        }

        if(isCSDragging)
            HOffset = (float) Mth.clamp(mouseY - (this.y + height2 + 5), 2, Math.round(152 * Window.windowScale) - 2) / Math.round(152f * Window.windowScale);

        if(isASDragging)
            AOffset = (float) Mth.clamp(mouseX - (this.x + 3), 2, Math.round(152 * Window.windowScale) - 2) / Math.round(152f * Window.windowScale);

        functionSetting.setColor(new float[]{
                (1 - HOffset) * 360,
                SVOffsets[0],
                1 - SVOffsets[1],
                AOffset
        });

    }

    private int
            mouseX,
            mouseY;

    private boolean
            isColorWidgetHovered = false,
            isBufferHovered = false,
            isDotPickerHovered = false,
            isColorSliderHovered = false,
            isAlphaSliderHovered = false;

    private boolean
            isDPDragging = false,
            isCSDragging = false,
            isASDragging = false;

    @EventTarget
    void _event(MouseMoveEvent mouseMoveEvent){

        if(!isAvailable)
            return;

        isWidgetHovered =
                mouseMoveEvent.getX() >= this.x &&
                mouseMoveEvent.getY() >= this.y &&
                mouseMoveEvent.getX() <= this.x + width2 &&
                mouseMoveEvent.getY() <= this.y + height2 &&
                this.parent.isHovered();

        isBufferHovered =
                mouseMoveEvent.getX() >= this.x - width3 - 1 &&
                        mouseMoveEvent.getY() >= this.y &&
                        mouseMoveEvent.getX() <= this.x - 2 &&
                        mouseMoveEvent.getY() <= this.y + height3;

        isColorWidgetHovered =
                mouseMoveEvent.getX() >= this.x - 1 &&
                mouseMoveEvent.getY() >= this.y + height2 + 1 &&
                mouseMoveEvent.getX() <= this.x - 1 + width &&
                mouseMoveEvent.getY() <= this.y + height2 + 1 + height;

        isDotPickerHovered =
                mouseMoveEvent.getX() >= this.x + 4 &&
                mouseMoveEvent.getY() >= this.y + height2 + 6 &&
                mouseMoveEvent.getX() <= this.x + 4 + (int) Math.round(152 * Window.windowScale) - 2 &&
                mouseMoveEvent.getY() <= this.y + height2 + 6 + (int) Math.round(152 * Window.windowScale) - 2;

        isColorSliderHovered =
                mouseMoveEvent.getX() >= this.x + 4 + (int) Math.round(152 * Window.windowScale) + 3 &&
                mouseMoveEvent.getY() >= this.y + height2 + 6 &&
                mouseMoveEvent.getX() <= this.x + 4 + (int) Math.round(152 * Window.windowScale) + 3 + width - (int) Math.round(152 * Window.windowScale) - 13 &&
                mouseMoveEvent.getY() <= this.y + height2 + 6 + (int) Math.round(152 * Window.windowScale) - 2;

        isAlphaSliderHovered =
                mouseMoveEvent.getX() >= this.x + 4 &&
                mouseMoveEvent.getY() >= this.y + height2 + 6 + (int) Math.round(152 * Window.windowScale) + 3 &&
                mouseMoveEvent.getX() <= this.x + 4 + (int) Math.round(152 * Window.windowScale) - 2 &&
                mouseMoveEvent.getY() <= this.y + height2 + 6 + (int) Math.round(152 * Window.windowScale) + 3 + height - (int) Math.round(152 * Window.windowScale) - 13;

        hoveredOption = -1;

        for(int i = 0; i < 2; i++){

            int buttonHeight = Math.round(18 * Window.windowScale) + 1;
            int buttonY = this.y + i * buttonHeight;

            if(
                    mouseMoveEvent.getY() >= buttonY &&
                            mouseMoveEvent.getY() <= buttonY + buttonHeight &&
                            mouseMoveEvent.getX() >= this.x - width3 - 1 &&
                            mouseMoveEvent.getX() <= this.x - 2
            ) {
                hoveredOption = i;
            }

        }

        mouseX = (int) mouseMoveEvent.getX();
        mouseY = (int) mouseMoveEvent.getY();

    }

    @EventTarget(value = Priority.HIGHEST)
    void _event(MouseEvent mouseEvent){

        if(Tabs.getSelectedTab() != tab || !MineSense.mainRender.isMenuOpened)
            return;

        if(!isAvailable)
            return;

        if(isBufferHovered && isBufferOpened)
            mouseEvent.setIsAlreadyUsed(true);

        if(!isColorWidgetHovered && mouseEvent.getButton() == 0 && mouseEvent.getAction() == 1 && isOpened && !isWidgetHovered) {
            isOpened = false;
            mouseEvent.setIsAlreadyUsed(true);
        }

        if(isBufferOpened && !isBufferHovered && mouseEvent.getButton() == 0 && mouseEvent.getAction() == 1) {
            isBufferOpened = false;
            mouseEvent.setIsAlreadyUsed(true);
        }

        if(hoveredOption != -1 && mouseEvent.getButton() == 0 && mouseEvent.getAction() == 0 && isBufferOpened) {

            if(hoveredOption == 0) {
                copyToClipboard(this.functionSetting.getHEXColor());
            } else {
                new Thread(() -> this.functionSetting.setHEXColor(getFromClipboard())).start();
            }

            isBufferOpened = false;

        }

        /*if(isBufferOpened && mouseEvent.getButton() == 0 && mouseEvent.getAction() == 1)
            isBufferOpened = false;*/

        if(mouseEvent.getAction() == 0) {
            isDPDragging = false;
            isCSDragging = false;
            isASDragging = false;
        }

        if(
            isWidgetHovered &&
            mouseEvent.getButton() == 0 &&
            mouseEvent.getAction() == 0 && !isAnyInteractableHovered
        ) isOpened = !isOpened;

        if(
            isWidgetHovered &&
            mouseEvent.getButton() == 1 &&
            mouseEvent.getAction() == 0 && !isAnyInteractableHovered && !isBufferOpened
        ) {
            isBufferOpened = true;
            isOpened = false;
        }

        if(!isOpened)
            return;

        if(isColorWidgetHovered)
            mouseEvent.setIsAlreadyUsed(true);

        if(
            isDotPickerHovered &&
            mouseEvent.getButton() == 0 &&
            mouseEvent.getAction() == 1
        ) isDPDragging = true;

        if(
            isColorSliderHovered &&
            mouseEvent.getButton() == 0 &&
            mouseEvent.getAction() == 1
        ) isCSDragging = true;

        if(
            isAlphaSliderHovered &&
            mouseEvent.getButton() == 0 &&
            mouseEvent.getAction() == 1
        ) isASDragging = true;

    }

    private static void copyToClipboard(String text) {
        try {
            String cmd = "cmd /c echo " + text + " | clip";
            Runtime.getRuntime().exec(cmd);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getFromClipboard() {
        try {
            // Команда PowerShell для получения содержимого буфера обмена
            String command = "powershell.exe Get-Clipboard";

            // Запуск процесса
            Process process = Runtime.getRuntime().exec(command);

            // Чтение вывода команды
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder clipboardText = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                clipboardText.append(line).append("\n");
            }

            // Завершаем процесс
            process.waitFor();
            process.destroy();

            return clipboardText.toString().trim();  // Возвращаем содержимое буфера обмена
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

}
