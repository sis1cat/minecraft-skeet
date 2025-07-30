package sisicat.main.gui.elements.widgets;

import net.minecraft.world.phys.Vec2;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;
import sisicat.main.gui.elements.Window;
import sisicat.main.utilities.Color;
import sisicat.main.utilities.Text;

public class BindText extends Widget.AdditionAbleWidget {

    private final String text;

    int x, y;

    public int occupiedSpace = 0;

    public Object link;

    public BindText(Function function){
        this.text = function.getName();
    }

    public BindText(FunctionSetting functionSetting){
        this.text = functionSetting.getName();
    }

    public BindText(String text){
        this.text = text;
    }

    public BindText(String text, Object link){
        this.text = text;
        this.link = link;
    }

    public void draw(int x, int y, Vec2 gameWindowSize){

        occupiedSpace = Math.round(8 * Window.windowScale);

        this.x = x;
        this.y = y;

        Text.getMenuFont().renderText(text, x + Math.round(8 * Window.windowScale) + Math.round(12 * Window.windowScale), y, Color.c205);

    }

    public BindText addBind(Bind bind){

        this.bind = bind;
        return this;

    }

    public BindText addColor(sisicat.main.gui.elements.widgets.Color color){

        this.color = color;
        return this;

    }

}
