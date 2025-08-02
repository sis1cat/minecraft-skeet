package sisicat.main.gui.elements.widgets;

import com.darkmagician6.eventapi.EventTarget;
import com.darkmagician6.eventapi.types.Priority;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import sisicat.MineSense;
import sisicat.events.MouseEvent;
import sisicat.events.MouseMoveEvent;
import sisicat.events.ScrollEvent;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;
import sisicat.main.gui.elements.Window;
import sisicat.main.utilities.Color;
import sisicat.main.utilities.Render;
import sisicat.main.utilities.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Group extends Widget {

    private final Vec2 positionInMesh;
    private final Vec2 sizeInMesh;

    private int x, y, groupWidth, groupHeight;

    private final int tab;
    private final String groupName;
    public final ArrayList<Widget> elements = new ArrayList<>();

    private static final HashMap<Integer, ArrayList<Group>> groups = new HashMap<>();

    public Group(int tab, Vec2 positionInMesh, Vec2 sizeInMesh, String groupName){

        this.tab = tab;
        this.positionInMesh = positionInMesh;
        this.sizeInMesh = sizeInMesh;
        this.groupName = groupName;

        groups.computeIfAbsent(tab, k -> new ArrayList<>());
        groups.get(tab).add(this);

    }

    public void draw(Vec2 meshPosition, Vec2 meshStep, Vec2 gameWindowSize){

        if(Tabs.getSelectedTab() != tab)
            return;

        x = (int) (meshPosition.x + positionInMesh.x * meshStep.x);
        y = (int) (meshPosition.y + positionInMesh.y * meshStep.y);

        groupWidth = (int) (meshStep.x * sizeInMesh.x) - Math.round(20 * Window.windowScale);
        groupHeight = (int) (Math.ceil (meshStep.y * sizeInMesh.y) - Math.round(19 * Window.windowScale));

        float[] outlineColor = isResizing ? themeColor : Color.c40;

        float alpha = 255;

        Render.drawRectangle(x + 2, y + 2, groupWidth - 3, groupHeight - 4, new float[]{23, 23, 23}, alpha);

        Render.drawRectangle(x + 1, y + 2, 1, groupHeight - 4, outlineColor, alpha);
        Render.drawRectangle(x + 1, y + groupHeight - 2, groupWidth - 1, 1, outlineColor,alpha);
        Render.drawRectangle(x + groupWidth - 1, y + 2, 1, groupHeight - 4, outlineColor, alpha);

        Render.drawRectangle(x, y + 1, 1, groupHeight - 2, Color.c12, alpha);
        Render.drawRectangle(x, y + groupHeight - 1, groupWidth, 1, Color.c12, alpha);
        Render.drawRectangle(x + groupWidth, y + 1, 1, groupHeight - 1, Color.c12, alpha);

        Render.drawRectangle(x + 1, y + 1, 8, 1, outlineColor, alpha);
        Render.drawRectangle(x, y, 9, 1, Color.c12, alpha);

        Render.drawRectangle(x + 12 + Text.getMenuBoldFont().getStringWidth(groupName) + 3, y + 1, groupWidth - (12 + Text.getMenuBoldFont().getStringWidth(groupName) + 3), 1, outlineColor, alpha);
        Render.drawRectangle(x + 12 + Text.getMenuBoldFont().getStringWidth(groupName) + 3, y, groupWidth - (12 + Text.getMenuBoldFont().getStringWidth(groupName) + 2), 1, Color.c12, alpha);

        drawElements(x, y, gameWindowSize);

        Text.getMenuBoldFont().renderTextWithShadow(groupName, x + 12, y - (int)((float) Text.getMenuBoldFont().getBaseAscender() / 2), isDragging ? themeColor : Color.c205);

    }

    private void drawElements(int x, int y, Vec2 gameWindowSize){

        if(scrolledOffset > occupiedSpace - (groupHeight - 2))
            scrolledOffset = occupiedSpace - (groupHeight - 2);

        if(scrolledOffset < 0)
            scrolledOffset = 0;

        int elementX = x + Math.round(20 * Window.windowScale);
        int elementY = y + Math.round(20 * Window.windowScale - scrolledOffset) + 3;

        occupiedSpace = Math.round(38 * Window.windowScale);
        Render.drawAll();
        RenderSystem.enableScissor(
                x + 20,
                (int)gameWindowSize.y - y - groupHeight + 2,
                groupWidth - 21,
                groupHeight - 4
        );

        int elementSpace = Math.round(10 * Window.windowScale);

        for(Widget widget : elements){

            if(widget instanceof BindText bindText) {

                Object link = bindText.link;

                if(link != null){

                    if (
                            link instanceof Function function && function.canBeActivated() ||
                            link instanceof FunctionSetting functionSetting && functionSetting.getCanBeActivated()
                    ) {

                        bindText.draw(elementX, elementY, gameWindowSize);

                        elementY += bindText.occupiedSpace + elementSpace;
                        occupiedSpace += bindText.occupiedSpace + elementSpace;

                    }

                } else {

                    bindText.draw(elementX, elementY, gameWindowSize);

                    elementY += bindText.occupiedSpace + elementSpace;
                    occupiedSpace += bindText.occupiedSpace + elementSpace;

                }

            }

            if(widget instanceof Check check) {

                check.draw(elementX, elementY, gameWindowSize);

                elementY += check.occupiedSpace + elementSpace;
                occupiedSpace += check.occupiedSpace + elementSpace;

            }

            if(widget instanceof Slider slider) {

                Object link = slider.link;

                if(link != null){

                    if(slider.stringLink != null && link instanceof FunctionSetting functionSetting) {

                        boolean contains = false;

                        for(String s : slider.stringLink)
                            if (functionSetting.getStringValue().equals(s)) {
                                contains = true;
                                break;
                            }

                        if(!contains)
                            continue;

                        slider.draw(elementX, elementY, groupWidth, gameWindowSize);

                        elementY += slider.occupiedSpace + elementSpace;
                        occupiedSpace += slider.occupiedSpace + elementSpace;

                    } else

                    if (
                        link instanceof Function function && function.canBeActivated() ||
                        link instanceof FunctionSetting functionSetting && functionSetting.getCanBeActivated()
                    ) {


                        slider.draw(elementX, elementY, groupWidth, gameWindowSize);

                        elementY += slider.occupiedSpace + elementSpace;
                        occupiedSpace += slider.occupiedSpace + elementSpace;

                    }

                } else {

                    slider.draw(elementX, elementY, groupWidth, gameWindowSize);

                    elementY += slider.occupiedSpace + elementSpace;
                    occupiedSpace += slider.occupiedSpace + elementSpace;

                }

            }

            if(widget instanceof Selectable selectable) {

                selectable.draw(elementX, elementY, groupWidth, gameWindowSize);

                elementY += selectable.occupiedSpace + elementSpace;
                occupiedSpace += selectable.occupiedSpace + elementSpace;

            }

            if(widget instanceof Combo combo) {

                Object link = combo.link;

                if(link != null){

                    if (
                            link instanceof Function function && function.canBeActivated() ||
                                    link instanceof FunctionSetting functionSetting && functionSetting.getCanBeActivated()
                    ) {

                        combo.draw(elementX, elementY, groupWidth, gameWindowSize);

                        elementY += combo.occupiedSpace + elementSpace;
                        occupiedSpace += combo.occupiedSpace + elementSpace;

                    }

                } else {

                    combo.draw(elementX, elementY, groupWidth, gameWindowSize);

                    elementY += combo.occupiedSpace + elementSpace;
                    occupiedSpace += combo.occupiedSpace + elementSpace;

                }

            }

            if(widget instanceof Button button) {

                button.draw(elementX, elementY, groupWidth, gameWindowSize);

                elementY += button.occupiedSpace + elementSpace;
                occupiedSpace += button.occupiedSpace + elementSpace;

            }

            if(widget instanceof Configs configs) {

                configs.draw(elementX, elementY, groupWidth);

                elementY += configs.occupiedSpace + elementSpace;
                occupiedSpace += configs.occupiedSpace + elementSpace;

            }

            if(widget instanceof Edit edit) {

                edit.draw(elementX, elementY, groupWidth);

                elementY += edit.occupiedSpace + elementSpace;
                occupiedSpace += edit.occupiedSpace + elementSpace;

            }

        }

        Render.drawAll();
        RenderSystem.disableScissor();

        // small triangle for show able to resize group
        Render.drawTriangle1(x + groupWidth - 6, y + groupHeight - 7, 5, 5, isResizing ? themeColor : Color.c40, 255);

        needToScroll = occupiedSpace > (groupHeight - 4);

        if(needToScroll) {

            // faded rectangles for able to scroll
            Render.drawGradientRectangle(x + 2, y + 2, groupWidth - 3, Math.round(20 * Window.windowScale), new float[]{23, 23, 23, 0}, new float[]{23, 23, 23, 0}, new float[]{23, 23, 23, 255}, new float[]{23, 23, 23, 255}, 255);
            Render.drawGradientRectangle(x + 2, y + groupHeight - Math.round(20 * Window.windowScale) - 2, groupWidth - 3, Math.round(20 * Window.windowScale), new float[]{23, 23, 23, 255}, new float[]{23, 23, 23, 255}, new float[]{23, 23, 23, 0}, new float[]{23, 23, 23, 0}, 255);

        }

        for(Widget widget : elements) {

            if (
                    widget instanceof BindText bindText &&
                            bindText.bind != null
            ) {
                bindText.bind.draw(bindText.x, bindText.y, x, groupWidth, false, gameWindowSize);
                Render.drawAll();
                RenderSystem.enableScissor(
                        x + 20,
                        (int) gameWindowSize.y - y - groupHeight + 2,
                        groupWidth - 21,
                        groupHeight - 4
                );
                bindText.bind.drawBindName(gameWindowSize);
                Render.drawAll();
                RenderSystem.disableScissor();
            }

            if (
                    widget instanceof BindText bindText &&
                            bindText.color != null
            ) {

                RenderSystem.enableScissor(
                        x + 20,
                        (int) gameWindowSize.y - y - groupHeight + 2,
                        groupWidth - 21,
                        groupHeight - 4
                );

                if(bindText.link != null){

                    if(
                        bindText.link instanceof Function function && function.canBeActivated() ||
                        bindText.link instanceof FunctionSetting functionSetting && functionSetting.getCanBeActivated()
                    ) {
                        bindText.color.draw(bindText.x, bindText.y, x, groupWidth, false, gameWindowSize);
                        Render.drawAll();
                    } else
                        bindText.color.isAvailable = false;

                } else {
                    bindText.color.draw(bindText.x, bindText.y, x, groupWidth, false, gameWindowSize);
                    Render.drawAll();
                }
                RenderSystem.disableScissor();

            }

            if (
                    widget instanceof Check check &&
                            check.bind != null
            ) {
                check.bind.draw(check.x, check.y, x, groupWidth, check.isWidgetHovered, gameWindowSize);
                Render.drawAll();
                RenderSystem.enableScissor(
                        x + 20,
                        (int) gameWindowSize.y - y - groupHeight + 2,
                        groupWidth - 21,
                        groupHeight - 4
                );
                check.bind.drawBindName(gameWindowSize);
                Render.drawAll();
                RenderSystem.disableScissor();
            }

            if (
                    widget instanceof Check check &&
                            check.color != null
            ) {
                Render.drawAll();
                RenderSystem.enableScissor(
                        x + 20,
                        (int) gameWindowSize.y - y - groupHeight + 2,
                        groupWidth - 21,
                        groupHeight - 4
                );
                check.color.draw(check.x, check.y, x, groupWidth, check.isWidgetHovered, gameWindowSize);
                Render.drawAll();
                RenderSystem.disableScissor();

            }

            if (
                widget instanceof Selectable selectable
            ) selectable.drawOptions(gameWindowSize);

            if (
                    widget instanceof Combo combo
            ) combo.drawOptions(gameWindowSize);


        }

        if(!needToScroll)
            return;

        // occupied space = scroll bar space
        float scaleMultiplier = (float) occupiedSpace / (groupHeight - 3);
        scrollBarHeight = Math.max(20, (int) ((groupHeight - 3) / scaleMultiplier));

        int maxScrollBarYOffset = groupHeight - 4 - scrollBarHeight;
        int maxScrolledOffset = occupiedSpace - (groupHeight - 3);

        if (!isScrolling)
            scrollBarYOffset = (int) Math.ceil((float) scrolledOffset / maxScrolledOffset * maxScrollBarYOffset);
        else
            scrolledOffset = (int) ((float) scrollBarYOffset / maxScrollBarYOffset * maxScrolledOffset);

        scrollBarYOffset = Mth.clamp(scrollBarYOffset, 0, maxScrollBarYOffset);
        scrolledOffset = Mth.clamp(scrolledOffset, 0, maxScrolledOffset);

        if (scrollBarYOffset < 0)
            scrollBarYOffset = 0;

        // scroll bar
        Render.drawRectangle(x + groupWidth - Math.round(7 * Window.windowScale), y + 1, Math.round(7 * Window.windowScale), groupHeight - 3, new float[]{40, 40, 40}, 255);
        Render.drawRectangle(x + groupWidth - Math.round(7 * Window.windowScale) + 1, y + 2 + scrollBarYOffset, (int)Math.ceil(7 * Window.windowScale) - 2, scrollBarHeight, new float[]{65, 65, 65}, 255);

        //if(isScrollBarHovered) Window.isAnyInteractableHovered = true;

    }

    public void addElement(Widget... widget){

        for(Widget widget1 : widget) {

            widget1.parent = this;

            if(widget1 instanceof AdditionAbleWidget widget2) {
                if(widget2.bind != null)
                    widget2.bind.parent = this;
                if(widget2.color != null)
                    widget2.color.parent = this;
            }

        }

        Collections.addAll(elements, widget);

    }

    private int
            scrolledOffset = 0,
            occupiedSpace = 0,
            scrollBarYOffset = 0,
            scrollBarHeight = 0;

    @EventTarget
    void _event(ScrollEvent scrollEvent){

        if(
            !isGroupHovered ||
            Tabs.getSelectedTab() != tab ||
            !MineSense.mainRender.isMenuOpened ||
            !needToScroll
        ) return;

        if(scrollEvent.getY() < 0){

            scrolledOffset += Math.round(20 * Window.windowScale);

            if(scrolledOffset > occupiedSpace - (groupHeight - 2))
                scrolledOffset = occupiedSpace - (groupHeight - 2);

        } else {

            scrolledOffset -= Math.round(20 * Window.windowScale);

            if(scrolledOffset < 0)
                scrolledOffset = 0;

        }

    }

    private boolean
            isDragging = false,
            isResizing = false,
            isScrolling = false;

    private boolean
            isCapHovered = false,
            isCornerHovered = false,
            isGroupHovered = false,
            isScrollBarHovered = false,
            isScrollBarSpaceHovered = false;

    private boolean
            needToScroll = false;

    private int
            lastScrollingCursorY;

    private int
            groupXCursorPos,
            groupYCursorPos;

    public boolean isHovered() {
        return isGroupHovered;
    }

    @EventTarget(value = Priority.HIGH)
    void _event(MouseMoveEvent mouseMoveEvent){

        if(Tabs.getSelectedTab() != tab || !MineSense.mainRender.isMenuOpened)
            return;

        int initialX = (int) positionInMesh.x;
        int initialY = (int) positionInMesh.y;

        isCapHovered =
                mouseMoveEvent.getX() >= x &&
                mouseMoveEvent.getY() >= y &&
                mouseMoveEvent.getX() <= x + groupWidth &&
                mouseMoveEvent.getY() <= y + 15;

        isCornerHovered =
                mouseMoveEvent.getX() >= x + groupWidth - 15 &&
                mouseMoveEvent.getY() >= y + groupHeight - 15 &&
                mouseMoveEvent.getX() <= x + groupWidth &&
                mouseMoveEvent.getY() <= y + groupHeight;

        isGroupHovered =
                mouseMoveEvent.getX() >= x &&
                mouseMoveEvent.getY() >= y &&
                mouseMoveEvent.getX() <= x + groupWidth &&
                mouseMoveEvent.getY() <= y + groupHeight;

        isScrollBarHovered =
                mouseMoveEvent.getX() >= x + groupWidth - Math.round(7 * Window.windowScale) &&
                mouseMoveEvent.getY() >= y + 2 + scrollBarYOffset &&
                mouseMoveEvent.getX() <= x + groupWidth - Math.round(7 * Window.windowScale) + Math.round(7 * Window.windowScale) &&
                mouseMoveEvent.getY() <= y + 2 + scrollBarYOffset + scrollBarHeight && needToScroll;

        isScrollBarSpaceHovered =
                mouseMoveEvent.getX() >= x + groupWidth - Math.round(7 * Window.windowScale) &&
                mouseMoveEvent.getY() >= y + 1 + Math.round(7 * Window.windowScale) &&
                mouseMoveEvent.getX() <= x + groupWidth - Math.round(7 * Window.windowScale) + Math.round(7 * Window.windowScale) &&
                mouseMoveEvent.getY() <= y + 1 + groupHeight - 3;

        if(isResizing){

            sizeInMesh.x = Math.round((mouseMoveEvent.getX() - x + groupXCursorPos) / Window.meshStep.x);
            sizeInMesh.y = Math.round((mouseMoveEvent.getY() - y + groupYCursorPos) / Window.meshStep.y);

            sizeInMesh.x = Mth.clamp(sizeInMesh.x, 3, 24 - positionInMesh.x);
            sizeInMesh.y = Mth.clamp(sizeInMesh.y, 2, 24 - positionInMesh.y);

        }

        if(isScrolling){

            scrollBarYOffset += (int) (mouseMoveEvent.getY() - lastScrollingCursorY);
            lastScrollingCursorY = (int) mouseMoveEvent.getY();

            if(scrollBarYOffset > groupHeight - 4 - scrollBarHeight)
                scrollBarYOffset = groupHeight - 4 - scrollBarHeight;

        }

        if(!isDragging) return;

        x = (int)(mouseMoveEvent.getX() - groupXCursorPos);
        y = (int)(mouseMoveEvent.getY() - groupYCursorPos);

        positionInMesh.x = Math.round((x - Window.meshPosition.x) / Window.meshStep.x);
        positionInMesh.y = Math.round((y - Window.meshPosition.y) / Window.meshStep.y);

        if(positionInMesh.x > 24 - sizeInMesh.x) positionInMesh.x = 24 - sizeInMesh.x;
        if(positionInMesh.x < 0) positionInMesh.x = 0;

        if(positionInMesh.y > 24 - sizeInMesh.y) positionInMesh.y = 24 - sizeInMesh.y;
        if(positionInMesh.y < 0) positionInMesh.y = 0;

        for (Group group2 : groups.get(tab)) {
            if (this == group2) continue;

            int group1X = (int) positionInMesh.x;
            int group1Y = (int) positionInMesh.y;
            int group1Width = (int) sizeInMesh.x;
            int group1Height = (int) sizeInMesh.y;

            int group2X = (int) group2.positionInMesh.x;
            int group2Y = (int) group2.positionInMesh.y;
            int group2Width = (int) group2.sizeInMesh.x;
            int group2Height = (int) group2.sizeInMesh.y;

            boolean isIntersecting =
                    group1X < group2X + group2Width &&
                            group1X + group1Width > group2X &&
                            group1Y < group2Y + group2Height &&
                            group1Y + group1Height > group2Y;

            if (isIntersecting) {

                positionInMesh.x = group2X;
                positionInMesh.y = group2Y;

                group2.positionInMesh.x = initialX;
                group2.positionInMesh.y = initialY;

                group2.sizeInMesh.x = sizeInMesh.x;
                group2.sizeInMesh.y = sizeInMesh.y;

                sizeInMesh.x = group2Width;
                sizeInMesh.y = group2Height;

                x = (int) (Window.meshPosition.x + positionInMesh.x * Window.meshStep.x);
                y = (int) (Window.meshPosition.y + positionInMesh.y * Window.meshStep.y);

                groupXCursorPos = (int) mouseMoveEvent.getX() - x;
                groupYCursorPos = (int) mouseMoveEvent.getY() - y;

                break;

            }
        }
    }

    @EventTarget(value = Priority.HIGH)
    void _event(MouseEvent mouseEvent){


        if(Tabs.getSelectedTab() != tab || !MineSense.mainRender.isMenuOpened)
            return;

        if(mouseEvent.getAction() == 0) {

            isDragging = false;
            isResizing = false;
            isScrolling = false;

        }

        if(
            mouseEvent.getAction() == 1 &&
            mouseEvent.getButton() == 0 &&
            isCapHovered && !Window.isAnyInteractableHovered
        ) {

            isDragging = true;

            groupXCursorPos = (int) mouseEvent.getX() - x;
            groupYCursorPos = (int) mouseEvent.getY() - y;

            Window.isAnyInteractableHovered = true;
            mouseEvent.setIsAlreadyUsed(true);

            return;

        }

        if(
            mouseEvent.getAction() == 1 &&
            mouseEvent.getButton() == 0 &&
            isCornerHovered &&
            !Window.isAnyInteractableHovered
        ) {

            isResizing = true;

            groupXCursorPos = groupWidth - ((int) mouseEvent.getX() - x) + 20;
            groupYCursorPos = groupHeight - ((int) mouseEvent.getY() - y) + 19;

            Window.isAnyInteractableHovered = true;
            mouseEvent.setIsAlreadyUsed(true);

            return;

        }

        if(
            mouseEvent.getAction() == 1 &&
            mouseEvent.getButton() == 0 &&
            isScrollBarHovered &&
            !Window.isAnyInteractableHovered
        ) {

            isScrolling = true;

            lastScrollingCursorY = (int) mouseEvent.getY();

            Window.isAnyInteractableHovered = true;
            mouseEvent.setIsAlreadyUsed(true);

        }

    }

}
