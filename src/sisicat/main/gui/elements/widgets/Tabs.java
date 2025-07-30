package sisicat.main.gui.elements.widgets;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.world.phys.Vec2;
import sisicat.MineSense;
import sisicat.events.MouseEvent;
import sisicat.events.MouseMoveEvent;
import sisicat.main.gui.elements.Window;
import sisicat.main.utilities.Color;
import sisicat.main.utilities.Render;
import sisicat.main.utilities.Text;

public class Tabs extends Widget {

    private static final int DEFAULT_WIDGET_WIDTH = 75;
    private static final int WIDGET_HEIGHT = 544;
    private static final int DEFAULT_WIDGET_SPACE_OFFSET = 65;

    private int 
            widgetWidth = 544,
            widgetSpaceOffset = 65;
    
    
    private static int selectedTab = 0;
    private static int hoveredTab = 0;


    private int x, y;

    public void draw(int x, int y, Vec2 gameWindowSize){

        this.x = x;
        this.y = y;

        int selectedTabOffset = selectedTab * (widgetSpaceOffset + 1);

        // 1st tab part

        int firstVerticalOffset = 10;

        widgetWidth = Math.round(DEFAULT_WIDGET_WIDTH * Window.windowScale);
        widgetSpaceOffset = Math.round(DEFAULT_WIDGET_SPACE_OFFSET * Window.windowScale);

        Render.drawRectangle(
                x, y,
                widgetWidth - 2, firstVerticalOffset - 2 + selectedTabOffset,
                Color.c12, 255
        );

        Render.drawRectangle(
                x + widgetWidth - 2, y,
                1, firstVerticalOffset - 1 + selectedTabOffset, 
                new float[]{0, 0, 0}, 255
        );

        Render.drawRectangle(
                x, y + firstVerticalOffset - 2 + selectedTabOffset,
                widgetWidth - 1, 1,
                new float[]{0, 0, 0}, 255
        );

        Render.drawRectangle(
                x + widgetWidth - 1, y,
                1, firstVerticalOffset + selectedTabOffset,
                 new float[]{40, 40, 40}, 255
        );

        Render.drawRectangle(
                x, y + firstVerticalOffset - 1  + selectedTabOffset,
                widgetWidth, 1,
                new float[]{40, 40, 40}, 255
        );

        // 2nd tab part

        int secondVerticalOffset = MineSense.mainRender.getWindow().getHeight() - ( 560 - 469 );
        //secondVerticalOffset = Math.round(secondVerticalOffset * Window.windowScale);

        int height = Window.windowHeight - (widgetSpaceOffset + 2 + firstVerticalOffset + selectedTabOffset + 16);

        Render.drawRectangle(
                x, y + widgetSpaceOffset + 2 + firstVerticalOffset + selectedTabOffset,
                widgetWidth - 2, height,
                Color.c12, 255
        );

        Render.drawRectangle(
                x + widgetWidth - 2, y + firstVerticalOffset + widgetSpaceOffset + 2 + selectedTabOffset,
                1, height,
                new float[]{0, 0, 0}, 255
        );

        Render.drawRectangle(
                x, y + firstVerticalOffset + widgetSpaceOffset + 1 + selectedTabOffset,
                widgetWidth - 1, 1,
                new float[]{0, 0, 0}, 255
        );

        Render.drawRectangle(
                x + widgetWidth - 1, y + firstVerticalOffset + widgetSpaceOffset + 1 + selectedTabOffset,
                1, height + 1,
                new float[]{40, 40, 40}, 255
        );

        Render.drawRectangle(
                x, y + firstVerticalOffset + widgetSpaceOffset + selectedTabOffset,
                widgetWidth, 1,
                new float[]{40, 40, 40}, 255
        );

        // icons render

        int iconXOffset = Math.round((float) widgetWidth / 2);

        Text.MENU_ICONS.renderHVCenteredCharacter("C", x + iconXOffset, y + 10 + Math.round((float) (widgetSpaceOffset + 1) / 2), selectedTab == 0 ? Color.c205 : hoveredTab == 0 ? Color.c165 : Color.c90); // + 66 every tab
        Text.MENU_ICONS.renderHVCenteredCharacter("E", x + iconXOffset, y + 10 + Math.round((float) (widgetSpaceOffset + 1) / 2 * 3), selectedTab == 1 ? Color.c205 : hoveredTab == 1 ? Color.c165 : Color.c90);
        Text.MENU_ICONS.renderHVCenteredCharacter("F", x + iconXOffset, y + 10 + Math.round((float) (widgetSpaceOffset + 1) / 2 * 5), selectedTab == 2 ? Color.c205 : hoveredTab == 2 ? Color.c165 : Color.c90);
        Text.MENU_ICONS.renderHVCenteredCharacter("G", x + iconXOffset, y + 10 + Math.round((float) (widgetSpaceOffset + 1) / 2 * 7), selectedTab == 3 ? Color.c205 : hoveredTab == 3 ? Color.c165 : Color.c90);
        Text.MENU_ICONS.renderHVCenteredCharacter("D", x + iconXOffset, y + 10 + Math.round((float) (widgetSpaceOffset + 1) / 2 * 9), selectedTab == 4 ? Color.c205 : hoveredTab == 4 ? Color.c165 : Color.c90);
        Text.MENU_ICONS.renderHVCenteredCharacter("B", x + iconXOffset, y + 10 + Math.round((float) (widgetSpaceOffset + 1) / 2 * 11), selectedTab == 5 ? Color.c205 : hoveredTab == 5 ? Color.c165 : Color.c90);
        Text.MENU_ICONS.renderHVCenteredCharacter("A", x + iconXOffset, y + 10 + Math.round((float) (widgetSpaceOffset + 1) / 2 * 13), selectedTab == 6 ? Color.c205 : hoveredTab == 6 ? Color.c165 : Color.c90);
        Text.MENU_ICONS.renderHVCenteredCharacter("H", x + iconXOffset, y + 10 + Math.round((float) (widgetSpaceOffset + 1) / 2 * 15), selectedTab == 7 ? Color.c205 : hoveredTab == 7 ? Color.c165 : Color.c90);

    }

    public static int getSelectedTab(){
        return selectedTab;
    }

    @EventTarget
    void _event(MouseMoveEvent mouseMoveEvent){
        float mouseX = mouseMoveEvent.getX();
        float mouseY = mouseMoveEvent.getY();

        hoveredTab = -1;

        if(mouseX < x || mouseX >= x + widgetWidth) return;

        int minimalOffset = 1;

        if(mouseY - y >= 8 + (widgetSpaceOffset + minimalOffset) * 7) hoveredTab = 7;
        else
        if(mouseY - y >= 8 + (widgetSpaceOffset + minimalOffset) * 6) hoveredTab = 6;
        else
        if(mouseY - y >= 8 + (widgetSpaceOffset + minimalOffset) * 5) hoveredTab = 5;
        else
        if(mouseY - y >= 8 + (widgetSpaceOffset + minimalOffset) * 4) hoveredTab = 4;
        else
        if(mouseY - y >= 8 + (widgetSpaceOffset + minimalOffset) * 3) hoveredTab = 3;
        else
        if(mouseY - y >= 8 + (widgetSpaceOffset + minimalOffset) * 2) hoveredTab = 2;
        else
        if(mouseY - y >= 8 + (widgetSpaceOffset + minimalOffset)) hoveredTab = 1;
        else
        if(mouseY - y >= 8) hoveredTab = 0;
    }

    @EventTarget
    void _event(MouseEvent mouseEvent){

        if(mouseEvent.getAction() != 1 || mouseEvent.getButton() != 0 || !MineSense.mainRender.isMenuOpened) return;

        float mouseX = mouseEvent.getX();
        float mouseY = mouseEvent.getY();

        if(mouseX < x || mouseX >= x + widgetWidth) return;

        if(mouseY >= y && mouseY <= y + 8 + (widgetSpaceOffset + 1) * 8)
            Window.isAnyInteractableHovered = true;

        int minimalOffset = 1;

            if(mouseY - y >= 8 + (widgetSpaceOffset + minimalOffset) * 7) selectedTab = 7;
        else
            if(mouseY - y >= 8 + (widgetSpaceOffset + minimalOffset) * 6) selectedTab = 6;
        else
            if(mouseY - y >= 8 + (widgetSpaceOffset + minimalOffset) * 5) selectedTab = 5;
        else
            if(mouseY - y >= 8 + (widgetSpaceOffset + minimalOffset) * 4) selectedTab = 4;
        else
            if(mouseY - y >= 8 + (widgetSpaceOffset + minimalOffset) * 3) selectedTab = 3;
        else
            if(mouseY - y >= 8 + (widgetSpaceOffset + minimalOffset) * 2) selectedTab = 2;
        else
            if(mouseY - y >= 8 + (widgetSpaceOffset + minimalOffset)) selectedTab = 1;
        else
            if(mouseY - y >= 8) selectedTab = 0;

    }

}
