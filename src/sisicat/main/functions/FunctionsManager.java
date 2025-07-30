package sisicat.main.functions;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import org.lwjgl.glfw.GLFW;
import sisicat.IDefault;
import sisicat.MineSense;
import sisicat.events.GraphicsEvent;
import sisicat.events.KeyEvent;
import sisicat.events.MouseEvent;
import sisicat.main.functions.combat.Rage;
import sisicat.main.functions.miscellaneous.*;
import sisicat.main.functions.movement.*;
import sisicat.main.functions.saves.ConfigurationFunction;
import sisicat.main.functions.visual.*;

import java.util.ArrayList;
import java.util.List;

public class FunctionsManager implements IDefault {

    private static ArrayList<Function> functionsArray = new ArrayList<>();

    public FunctionsManager() {

        EventManager.register(this);

        functionsArray.addAll(

                List.of(

                        new Rage("Rage"),

                        new CustomJumpDelay("Custom jump delay"),
                        new ClientSideMovementFunction("Client-side movement"),
                        new AutomaticSprint("Automatic sprint"),
                        new AutomaticElytraJump("Automatic elytra jump"),
                        new InfinityElytraFlyFunction("Infinity elytra fly"),

                        new CustomVersionCompatibility("Custom version compatibility"),
                        new OverrideFOVFunction("Override FOV"),
                        new CustomBlockPlaceDelay("Custom block place delay"),
                        new CustomAttackAnimation("Custom attack animation"),
                        new CustomViewModelFunction("Custom view model"),
                        new IntegratedUIAnimations("Integrated UI animations"),

                        new PlayerESPFunction("Player ESP"),
                        new CustomFOGFunction("Custom fog"),
                        new CustomDayTimeFunction("Custom day time"),
                        new ChunksAppearanceAnimationFunction("Chunks appearance animation"),
                        new RemoveHurtEffectFunction("Remove hurt effect"),

                        new MenuKey("Menu key"),
                        new MenuColor("Menu color"),
                        new DPIScale("DPI scale"),
                        new UnlockMouseHandlerFunction("Unlock mouse handler"),
                        new UnlockKeyboardHandlerFunction("Unlock keyboard handler"),

                        new ConfigurationFunction("Configuration")

                )

        );

    }

    public static ArrayList<Function> getFunctionsArray(){
        return functionsArray;
    }

    public static void setFunctionsArray(ArrayList<Function> functionsArray){
        FunctionsManager.functionsArray = functionsArray;
    }

    public static Function getFunctionByName(String functionName) {

        for(Function function : functionsArray)
            if(function.getName().equalsIgnoreCase(functionName))
                return function;

        return new Function("Function not found");

    }

    @EventTarget
    void _event(KeyEvent keyEvent){

        if(MineSense.mainRender.isMenuOpened) return;

        for(Function function : getFunctionsArray())

            if (
                function.getBindType() == 2 &&
                keyEvent.getKeyCode() == function.getKeyBind() &&
                keyEvent.getAction() == 0
            )
                function.setActivated(!function.isActivated());

    }

    @EventTarget
    void _event(MouseEvent mouseEvent){

        if(MineSense.mainRender.isMenuOpened) return;

        for(Function function : getFunctionsArray())

            if (
                function.getBindType() == 2 &&
                mouseEvent.getButton() == function.getKeyBind() - 1 &&
                mouseEvent.getAction() == 1
            )
                function.setActivated(!function.isActivated());

    }

    @EventTarget
    void _event(GraphicsEvent graphicsEvent){

        for(Function function : getFunctionsArray()) {

            if (function.getBindType() == 0) {
                boolean b = function.canBeActivated();
                function.setActivated(b);
            }
            if(function.getBindType() == 1 && !MineSense.mainRender.isMenuOpened)
                function.setActivated(function.getKeyBind() - 1 <= 5 ? GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), function.getKeyBind() - 1 == -1 ? 0 : function.getKeyBind() - 1) == 1 : GLFW.glfwGetKey(mc.getWindow().getWindow(), function.getKeyBind()) == 1);

            if(function.getBindType() == 3 && !MineSense.mainRender.isMenuOpened)
                function.setActivated(function.getKeyBind() - 1 <= 5 ? GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), function.getKeyBind() - 1 == -1 ? 0 : function.getKeyBind() - 1) == 0 : GLFW.glfwGetKey(mc.getWindow().getWindow(), function.getKeyBind()) == 0);

            for(FunctionSetting functionSetting : function.getFunctionSettings()){

                if (functionSetting.getBindType() == 0)
                    functionSetting.setActivated(functionSetting.getCanBeActivated());

                if(functionSetting.getBindType() == 1 && !MineSense.mainRender.isMenuOpened)
                    functionSetting.setActivated(functionSetting.getKeyBind() - 1 <= 5 ? GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), functionSetting.getKeyBind() - 1 == -1 ? 0 : functionSetting.getKeyBind() - 1) == 1 : GLFW.glfwGetKey(mc.getWindow().getWindow(), functionSetting.getKeyBind()) == 1);

                if(functionSetting.getBindType() == 3 && !MineSense.mainRender.isMenuOpened)
                    functionSetting.setActivated(functionSetting.getKeyBind() - 1 <= 5 ? GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), functionSetting.getKeyBind() - 1 == -1 ? 0 : functionSetting.getKeyBind() - 1) == 0 : GLFW.glfwGetKey(mc.getWindow().getWindow(), functionSetting.getKeyBind()) == 0);


            }

        }

    }

}
