package sisicat.main.gui.elements.widgets;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.world.phys.Vec2;
import sisicat.MineSense;
import sisicat.events.KeyEvent;
import sisicat.events.MouseEvent;
import sisicat.events.MouseMoveEvent;
import sisicat.events.ScrollEvent;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;
import sisicat.main.gui.elements.Window;
import sisicat.main.utilities.Color;
import sisicat.main.utilities.KeyBind;
import sisicat.main.utilities.Render;
import sisicat.main.utilities.Text;

public class Bind extends Widget {

    public static String bindingElementName = "";

    private boolean isWidgetHovered;
    private int
            x,
            y,
            width,
            height;
    private final Object element;

    private String name;

    private final String[] options = {"Always on", "On hotkey", "Toggle", "Off hotkey"};

    boolean isComboOpened = false;

    private int hoveredOption = -1;
    private boolean isParentHovered;

    private final int tab;

    private String keyBindName;

    public Bind(Object bindingElement, int tab){
        this.element = bindingElement;
        this.tab = tab;
    }

    public void draw(int x, int y, int groupX, int groupWidth, boolean isParentHovered, Vec2 gameWindowSize){

        this.x = x;
        this.y = y;
        this.isParentHovered = isParentHovered;

        int keyBind;

        if(element instanceof Function) {
            keyBind = ((Function) element).getKeyBind();
            name = ((Function) element).getName();
        }else {
            keyBind = ((FunctionSetting)element).getKeyBind();
            name = ((FunctionSetting) element).getName();
        }

        keyBindName = KeyBind.getKeyName(keyBind);

        this.x = groupX + groupWidth - Math.round(18 * Window.windowScale) - Text.getMenuBindsFont().getStringWidth(keyBindName);
        this.y = this.y + 1;

        width = Text.getMenuBindsFont().getStringWidth(keyBindName);
        height = Text.getMenuBindsFont().getBaseAscender();

        if(!isComboOpened) return;

        Window.isAnyInteractableHovered = true;

        int elementsCount = 4;
        int height = elementsCount * Math.round(18 * Window.windowScale) + 2 * (elementsCount - 1) + 2;

        Render.drawRectangleBorders(this.x - Math.round(110 * Window.windowScale), this.y - 2, Math.round(100 * Window.windowScale), height, 1, Color.c12, 255);
        Render.drawRectangle(this.x - Math.round(110 * Window.windowScale) + 1, this.y - 1, Math.round(100 * Window.windowScale) - 2, height - 2, new float[]{35, 35, 35}, 255);

        if(hoveredOption != -1) {

            int i = hoveredOption - 1;
            int startY = this.y - 1;
            int buttonHeight = Math.round(18 * Window.windowScale);
            int buttonY = startY + i * (buttonHeight + 2);

            Render.drawRectangle(
                    this.x - Math.round(110 * Window.windowScale) + 1,
                    buttonY,
                    Math.round(100 * Window.windowScale) - 2, buttonHeight, new float[]{25, 25, 25}, 255);

        }

        for(int i = 0; i < options.length; i++) {

            int startY = this.y - 1;
            int buttonHeight = Math.round(18 * Window.windowScale);
            int textY = startY + i * (buttonHeight + 2) + buttonHeight / 2;

            Text.getMenuFont(hoveredOption == i + 1).renderVCenteredText(
                    options[i],
                    this.x - Math.round(110 * Window.windowScale) + 1 + Math.round(6 * Window.windowScale),
                    textY,
                    element instanceof Function ? ((Function) element).getBindType() == i ? Widget.themeColor : Color.c205 : ((FunctionSetting) element).getBindType() == i ? Widget.themeColor : Color.c205
                    );

        }

    }

    public void drawBindName(Vec2 gameWindowSize){

        if(Window.windowScale == 1)
            Text.getMenuBindsFont().renderOutlinedText(
                    keyBindName, this.x, this.y + 1,
                    Bind.bindingElementName.equals(name) ? Color.red : isWidgetHovered ? Color.c140 : Color.c115,
                    Color.c15
            );
        else
            Text.getMenuBindsFont().renderTextWithShadow(
                    keyBindName, this.x, this.y + 1,
                    Bind.bindingElementName.equals(name) ? Color.red : isWidgetHovered ? Color.c140 : Color.c115
            );
    }

    @EventTarget
    void _event(MouseMoveEvent mouseMoveEvent){

        isWidgetHovered =
                mouseMoveEvent.getX() >= x - (int) Math.round((float) width / 2) &&
                mouseMoveEvent.getX() <= x + width + (int) Math.round((float) width / 2) &&
                mouseMoveEvent.getY() >= y - (int) Math.round((float) height / 2) + 1 &&
                mouseMoveEvent.getY() <= y + height + (int) Math.round((float)height / 2) - 1 &&
                this.parent.isHovered();

        if(!isComboOpened)
            return;

        int startY = this.y - 2;

        hoveredOption = -1;

        for(int i = 0; i < 4; i++){

            int buttonHeight = Math.round(18 * Window.windowScale) + 2;
            int buttonY = startY + i * buttonHeight;

            if(
                mouseMoveEvent.getY() >= buttonY &&
                mouseMoveEvent.getY() <= buttonY + buttonHeight &&
                mouseMoveEvent.getX() >= this.x - Math.round(110 * Window.windowScale) + 1 &&
                mouseMoveEvent.getX() <= this.x - Math.round(110 * Window.windowScale) + 1 + Math.round(100 * Window.windowScale) - 2
            ) {
                hoveredOption = i + 1;
            }

        }

    }

    @EventTarget
    void _event(KeyEvent keyEvent){

        if(Tabs.getSelectedTab() != tab || !MineSense.mainRender.isMenuOpened)
            return;

        if(element instanceof Function) {

            if (Bind.bindingElementName.equals(((Function) element).getName())) {
                ((Function) element).setKeyBind(keyEvent.getKeyCode() == 256 ? 0 : keyEvent.getKeyCode());
            }

        }
        else
        {

            if(Bind.bindingElementName.equals(((FunctionSetting) element).getName()))
                ((FunctionSetting) element).setKeyBind(keyEvent.getKeyCode() == 256 ? 0 : keyEvent.getKeyCode());

        }

        Bind.bindingElementName = "";

    }

    @EventTarget()
    void _event(MouseEvent mouseEvent){

        if(Tabs.getSelectedTab() != tab || !MineSense.mainRender.isMenuOpened || mouseEvent.getIsAlreadyUsed())
            return;

        if(
            isWidgetHovered &&
            mouseEvent.getAction() == 0 &&
            mouseEvent.getButton() == 1
        )
            isComboOpened = true;

        if(mouseEvent.getAction() == 1 && mouseEvent.getButton() == 0 && isComboOpened && hoveredOption == -1) {
            isComboOpened = false;
            return;
        }

        if(
                isComboOpened &&
                mouseEvent.getAction() == 1 &&
                mouseEvent.getButton() == 0 &&
                hoveredOption != -1
        )
            mouseEvent.setIsAlreadyUsed(true);

        if(
            isComboOpened &&
            mouseEvent.getAction() == 0 &&
            mouseEvent.getButton() == 0
        ) {

            if(element instanceof Function function)
                function.setBindType(hoveredOption - 1);
            else
                ((FunctionSetting) element).setBindType(hoveredOption - 1);

            isComboOpened = false;

            hoveredOption = -1;
            mouseEvent.setIsAlreadyUsed(true);

        }

        if(
            Bind.bindingElementName.equals(name) &&
            mouseEvent.getAction() == 1 &&
            mouseEvent.getButton() == 0 &&
            isWidgetHovered
        ) {

            if(element instanceof Function function)
                function.setKeyBind(mouseEvent.getButton() + 1);
            else
                ((FunctionSetting)element).setKeyBind(mouseEvent.getButton() + 1);

            Bind.bindingElementName = "";

            return;

        }

        if (
            Bind.bindingElementName.equals(name) &&
            mouseEvent.getAction() == 1
        ) {

            if(element instanceof Function function)
                function.setKeyBind(mouseEvent.getButton() + 1);
            else
                ((FunctionSetting)element).setKeyBind(mouseEvent.getButton() + 1);

            Bind.bindingElementName = "";

        }

        if (
            isWidgetHovered &&
            mouseEvent.getAction() == 1 &&
            mouseEvent.getButton() == 0 &&
            !isParentHovered
        ) {

            Window.isAnyInteractableHovered = true;
            Bind.bindingElementName = name;

        }

    }

    @EventTarget
    void _event(ScrollEvent scrollEvent) {
        isComboOpened = false;
    }

}
