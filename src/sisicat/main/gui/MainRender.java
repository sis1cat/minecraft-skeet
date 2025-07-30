package sisicat.main.gui;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.Minecraft;

import org.lwjgl.glfw.GLFW;
import sisicat.IDefault;
import sisicat.MineSense;
import sisicat.events.*;
import sisicat.main.functions.FunctionsManager;
import sisicat.main.functions.miscellaneous.MenuColor;
import sisicat.main.gui.elements.Window;
import sisicat.main.gui.elements.widgets.Bind;
import sisicat.main.gui.elements.widgets.Widget;
import sisicat.main.utilities.Animation;

public class MainRender implements IDefault {

    private final Window window;
    public boolean isMenuOpened = true;
    public static boolean isFullInitialized = false;

    public MainRender(){
        EventManager.register(this);
        window = new Window();

    }

    private final Animation animation = new Animation();

    public void render(){

        MenuColor.updateMenuColor();

        window.updateWindowSizes();

        Window.gameWindowSize.x = Window.gameWindowWidth;
        Window.gameWindowSize.y = Window.gameWindowHeight;

        if(!isMenuOpened) {
            Widget.menuAlpha = animation.interpolate(Widget.menuAlpha, 0, 50d);
            if(Widget.menuAlpha < 3) {
                Widget.menuAlpha = 0;
                return;
            }
        } else if(isFullInitialized) {
            mc.mouseHandler.releaseMouse();
            Widget.menuAlpha = animation.interpolate(Widget.menuAlpha, 255, 50d);
        }

        window.draw();

    }

    public Window getWindow(){
        return window;
    }

    @EventTarget
    void _event(KeyEvent keyEvent){

        int bind = GLFW.GLFW_KEY_DELETE;

        try {
            bind = FunctionsManager.getFunctionByName("Menu key").getKeyBind();
        }catch (Exception ignored){}

        if(keyEvent.getKeyCode() == bind && keyEvent.getAction() == 1)
            isMenuOpened = !isMenuOpened;

        if(!isMenuOpened)
            Bind.bindingElementName = "";

    }

    @EventTarget
    void _event(MovementUpdateEvent ignored){

        if(!MineSense.mainRender.isMenuOpened && !Minecraft.getInstance().mouseHandler.isMouseGrabbed() && mc.screen == null)
            Minecraft.getInstance().mouseHandler.grabMouse();

    }

    @EventTarget
    void _event(GraphicsEvent ignored){

        render();

    }

    @EventTarget
    void _event(PauseGameEvent pauseGameEvent){
        if(isMenuOpened) pauseGameEvent.cancel();
    }

    @EventTarget
    void _event(MouseHandlerOnActionEvent mouseHandlerOnActionEvent){

        if(FunctionsManager.getFunctionByName("Unlock mouse handler").isActivated()) {
            if(mouseHandlerOnActionEvent.getType() == MouseHandlerOnActionEvent.TYPE.GRAB_MOUSE && isMenuOpened)
                mouseHandlerOnActionEvent.cancel();
            return;
        }

        if(isMenuOpened)
            mouseHandlerOnActionEvent.cancel();

    }

    @EventTarget
    void _event(KeyboardHandlerOnActionEvent keyboardHandlerOnActionEvent){

        if(FunctionsManager.getFunctionByName("Unlock keyboard handler").isActivated())
            return;

        if(isMenuOpened)
            keyboardHandlerOnActionEvent.cancel();

    }

}
