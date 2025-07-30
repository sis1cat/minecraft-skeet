package net.minecraft.client.gui.components.events;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;

@OnlyIn(Dist.CLIENT)
public interface ContainerEventHandler extends GuiEventListener {
    List<? extends GuiEventListener> children();

    default Optional<GuiEventListener> getChildAt(double pMouseX, double pMouseY) {
        for (GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener.isMouseOver(pMouseX, pMouseY)) {
                return Optional.of(guieventlistener);
            }
        }

        return Optional.empty();
    }

    @Override
    default boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        Optional<GuiEventListener> optional = this.getChildAt(pMouseX, pMouseY);
        if (optional.isEmpty()) {
            return false;
        } else {
            GuiEventListener guieventlistener = optional.get();
            if (guieventlistener.mouseClicked(pMouseX, pMouseY, pButton)) {
                this.setFocused(guieventlistener);
                if (pButton == 0) {
                    this.setDragging(true);
                }
            }

            return true;
        }
    }

    @Override
    default boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 0 && this.isDragging()) {
            this.setDragging(false);
            if (this.getFocused() != null) {
                return this.getFocused().mouseReleased(pMouseX, pMouseY, pButton);
            }
        }

        return this.getChildAt(pMouseX, pMouseY).filter(p_94708_ -> p_94708_.mouseReleased(pMouseX, pMouseY, pButton)).isPresent();
    }

    @Override
    default boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        return this.getFocused() != null && this.isDragging() && pButton == 0 ? this.getFocused().mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY) : false;
    }

    boolean isDragging();

    void setDragging(boolean pIsDragging);

    @Override
    default boolean mouseScrolled(double p_94686_, double p_94687_, double p_94688_, double p_299502_) {
        return this.getChildAt(p_94686_, p_94687_).filter(p_296182_ -> p_296182_.mouseScrolled(p_94686_, p_94687_, p_94688_, p_299502_)).isPresent();
    }

    @Override
    default boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        return this.getFocused() != null && this.getFocused().keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    default boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        return this.getFocused() != null && this.getFocused().keyReleased(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    default boolean charTyped(char pCodePoint, int pModifiers) {
        return this.getFocused() != null && this.getFocused().charTyped(pCodePoint, pModifiers);
    }

    @Nullable
    GuiEventListener getFocused();

    void setFocused(@Nullable GuiEventListener pFocused);

    @Override
    default void setFocused(boolean p_265504_) {
    }

    @Override
    default boolean isFocused() {
        return this.getFocused() != null;
    }

    @Nullable
    @Override
    default ComponentPath getCurrentFocusPath() {
        GuiEventListener guieventlistener = this.getFocused();
        return guieventlistener != null ? ComponentPath.path(this, guieventlistener.getCurrentFocusPath()) : null;
    }

    @Nullable
    @Override
    default ComponentPath nextFocusPath(FocusNavigationEvent p_265668_) {
        GuiEventListener guieventlistener = this.getFocused();
        if (guieventlistener != null) {
            ComponentPath componentpath = guieventlistener.nextFocusPath(p_265668_);
            if (componentpath != null) {
                return ComponentPath.path(this, componentpath);
            }
        }

        if (p_265668_ instanceof FocusNavigationEvent.TabNavigation focusnavigationevent$tabnavigation) {
            return this.handleTabNavigation(focusnavigationevent$tabnavigation);
        } else {
            return p_265668_ instanceof FocusNavigationEvent.ArrowNavigation focusnavigationevent$arrownavigation
                ? this.handleArrowNavigation(focusnavigationevent$arrownavigation)
                : null;
        }
    }

    @Nullable
    private ComponentPath handleTabNavigation(FocusNavigationEvent.TabNavigation pTabNavigation) {
        boolean flag = pTabNavigation.forward();
        GuiEventListener guieventlistener = this.getFocused();
        List<? extends GuiEventListener> list = new ArrayList<>(this.children());
        Collections.sort(list, Comparator.comparingInt(p_374572_ -> p_374572_.getTabOrderGroup()));
        int j = list.indexOf(guieventlistener);
        int i;
        if (guieventlistener != null && j >= 0) {
            i = j + (flag ? 1 : 0);
        } else if (flag) {
            i = 0;
        } else {
            i = list.size();
        }

        ListIterator<? extends GuiEventListener> listiterator = list.listIterator(i);
        BooleanSupplier booleansupplier = flag ? listiterator::hasNext : listiterator::hasPrevious;
        Supplier<? extends GuiEventListener> supplier = flag ? listiterator::next : listiterator::previous;

        while (booleansupplier.getAsBoolean()) {
            GuiEventListener guieventlistener1 = supplier.get();
            ComponentPath componentpath = guieventlistener1.nextFocusPath(pTabNavigation);
            if (componentpath != null) {
                return ComponentPath.path(this, componentpath);
            }
        }

        return null;
    }

    @Nullable
    private ComponentPath handleArrowNavigation(FocusNavigationEvent.ArrowNavigation pArrowNavigation) {
        GuiEventListener guieventlistener = this.getFocused();
        if (guieventlistener == null) {
            ScreenDirection screendirection = pArrowNavigation.direction();
            ScreenRectangle screenrectangle1 = this.getBorderForArrowNavigation(screendirection.getOpposite());
            return ComponentPath.path(this, this.nextFocusPathInDirection(screenrectangle1, screendirection, null, pArrowNavigation));
        } else {
            ScreenRectangle screenrectangle = guieventlistener.getRectangle();
            return ComponentPath.path(this, this.nextFocusPathInDirection(screenrectangle, pArrowNavigation.direction(), guieventlistener, pArrowNavigation));
        }
    }

    @Nullable
    private ComponentPath nextFocusPathInDirection(ScreenRectangle pRectangle, ScreenDirection pDirection, @Nullable GuiEventListener pListener, FocusNavigationEvent pEvent) {
        ScreenAxis screenaxis = pDirection.getAxis();
        ScreenAxis screenaxis1 = screenaxis.orthogonal();
        ScreenDirection screendirection = screenaxis1.getPositive();
        int i = pRectangle.getBoundInDirection(pDirection.getOpposite());
        List<GuiEventListener> list = new ArrayList<>();

        for (GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener != pListener) {
                ScreenRectangle screenrectangle = guieventlistener.getRectangle();
                if (screenrectangle.overlapsInAxis(pRectangle, screenaxis1)) {
                    int j = screenrectangle.getBoundInDirection(pDirection.getOpposite());
                    if (pDirection.isAfter(j, i)) {
                        list.add(guieventlistener);
                    } else if (j == i && pDirection.isAfter(screenrectangle.getBoundInDirection(pDirection), pRectangle.getBoundInDirection(pDirection))) {
                        list.add(guieventlistener);
                    }
                }
            }
        }

        Comparator<GuiEventListener> comparator = Comparator.comparing(
            p_264674_ -> p_264674_.getRectangle().getBoundInDirection(pDirection.getOpposite()), pDirection.coordinateValueComparator()
        );
        Comparator<GuiEventListener> comparator1 = Comparator.comparing(
            p_264676_ -> p_264676_.getRectangle().getBoundInDirection(screendirection.getOpposite()), screendirection.coordinateValueComparator()
        );
        list.sort(comparator.thenComparing(comparator1));

        for (GuiEventListener guieventlistener1 : list) {
            ComponentPath componentpath = guieventlistener1.nextFocusPath(pEvent);
            if (componentpath != null) {
                return componentpath;
            }
        }

        return this.nextFocusPathVaguelyInDirection(pRectangle, pDirection, pListener, pEvent);
    }

    @Nullable
    private ComponentPath nextFocusPathVaguelyInDirection(ScreenRectangle pRectangle, ScreenDirection pDirection, @Nullable GuiEventListener pListener, FocusNavigationEvent pEvent) {
        ScreenAxis screenaxis = pDirection.getAxis();
        ScreenAxis screenaxis1 = screenaxis.orthogonal();
        List<Pair<GuiEventListener, Long>> list = new ArrayList<>();
        ScreenPosition screenposition = ScreenPosition.of(screenaxis, pRectangle.getBoundInDirection(pDirection), pRectangle.getCenterInAxis(screenaxis1));

        for (GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener != pListener) {
                ScreenRectangle screenrectangle = guieventlistener.getRectangle();
                ScreenPosition screenposition1 = ScreenPosition.of(
                    screenaxis, screenrectangle.getBoundInDirection(pDirection.getOpposite()), screenrectangle.getCenterInAxis(screenaxis1)
                );
                if (pDirection.isAfter(screenposition1.getCoordinate(screenaxis), screenposition.getCoordinate(screenaxis))) {
                    long i = Vector2i.distanceSquared(
                        screenposition.x(), screenposition.y(), screenposition1.x(), screenposition1.y()
                    );
                    list.add(Pair.of(guieventlistener, i));
                }
            }
        }

        list.sort(Comparator.comparingDouble(Pair::getSecond));

        for (Pair<GuiEventListener, Long> pair : list) {
            ComponentPath componentpath = pair.getFirst().nextFocusPath(pEvent);
            if (componentpath != null) {
                return componentpath;
            }
        }

        return null;
    }
}