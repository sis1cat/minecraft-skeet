package net.minecraft.client;

import com.darkmagician6.eventapi.EventManager;
import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.util.SmoothDouble;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFWDropCallback;
import org.slf4j.Logger;
import sisicat.IDefault;
import sisicat.events.MouseEvent;
import sisicat.events.MouseHandlerOnActionEvent;
import sisicat.events.MouseMoveEvent;
import sisicat.events.ScrollEvent;
import sisicat.main.functions.combat.Rage;

@OnlyIn(Dist.CLIENT)
public class MouseHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft;
    private boolean isLeftPressed;
    private boolean isMiddlePressed;
    private boolean isRightPressed;
    private double xpos;
    private double ypos;
    private int fakeRightMouse;
    private int activeButton = -1;
    private boolean ignoreFirstMove = true;
    private int clickDepth;
    private double mousePressedTime;
    private final SmoothDouble smoothTurnX = new SmoothDouble();
    private final SmoothDouble smoothTurnY = new SmoothDouble();
    private double accumulatedDX;
    private double accumulatedDY;
    private final ScrollWheelHandler scrollWheelHandler;
    private double lastHandleMovementTime = Double.MIN_VALUE;
    private boolean mouseGrabbed;

    private float customMouseX = 0, customMouseY = 0;

    public MouseHandler(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
        this.scrollWheelHandler = new ScrollWheelHandler();
    }

    private void onPress(long pWindowPointer, int pButton, int pAction, int pModifiers) {
        if (pWindowPointer == this.minecraft.getWindow().getWindow()) {

            EventManager.call(new MouseEvent(customMouseX, customMouseY, pButton, pAction));

            MouseHandlerOnActionEvent mouseHandlerOnActionEvent = new MouseHandlerOnActionEvent();
            EventManager.call(mouseHandlerOnActionEvent);
            if(mouseHandlerOnActionEvent.isCancelled()) return;

            this.minecraft.getFramerateLimitTracker().onInputReceived();
            if (this.minecraft.screen != null) {
                this.minecraft.setLastInputType(InputType.MOUSE);
            }

            boolean flag = pAction == 1;
            if (Minecraft.ON_OSX && pButton == 0) {
                if (flag) {
                    if ((pModifiers & 2) == 2) {
                        pButton = 1;
                        this.fakeRightMouse++;
                    }
                } else if (this.fakeRightMouse > 0) {
                    pButton = 1;
                    this.fakeRightMouse--;
                }
            }

            int i = pButton;
            if (flag) {
                if (this.minecraft.options.touchscreen().get() && this.clickDepth++ > 0) {
                    return;
                }

                this.activeButton = pButton;
                this.mousePressedTime = Blaze3D.getTime();
            } else if (this.activeButton != -1) {
                if (this.minecraft.options.touchscreen().get() && --this.clickDepth > 0) {
                    return;
                }

                this.activeButton = -1;
            }

            if (this.minecraft.getOverlay() == null) {
                if (this.minecraft.screen == null) {
                    if (!this.mouseGrabbed && flag) {
                        this.grabMouse();
                    }
                } else {
                    double d0 = this.xpos * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
                    double d1 = this.ypos * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();
                    Screen screen = this.minecraft.screen;
                    if (flag) {
                        screen.afterMouseAction();

                        try {
                            if (screen.mouseClicked(d0, d1, i)) {
                                return;
                            }
                        } catch (Throwable throwable1) {
                            CrashReport crashreport = CrashReport.forThrowable(throwable1, "mouseClicked event handler");
                            screen.fillCrashDetails(crashreport);
                            CrashReportCategory crashreportcategory = crashreport.addCategory("Mouse");
                            crashreportcategory.setDetail("Scaled X", d0);
                            crashreportcategory.setDetail("Scaled Y", d1);
                            crashreportcategory.setDetail("Button", pButton);
                            throw new ReportedException(crashreport);
                        }
                    } else {
                        try {
                            if (screen.mouseReleased(d0, d1, i)) {
                                return;
                            }
                        } catch (Throwable throwable) {
                            CrashReport crashreport1 = CrashReport.forThrowable(throwable, "mouseReleased event handler");
                            screen.fillCrashDetails(crashreport1);
                            CrashReportCategory crashreportcategory1 = crashreport1.addCategory("Mouse");
                            crashreportcategory1.setDetail("Scaled X", d0);
                            crashreportcategory1.setDetail("Scaled Y", d1);
                            crashreportcategory1.setDetail("Button", pButton);
                            throw new ReportedException(crashreport1);
                        }
                    }
                }
            }

            if (this.minecraft.screen == null && this.minecraft.getOverlay() == null) {
                if (pButton == 0) {
                    this.isLeftPressed = flag;
                } else if (pButton == 2) {
                    this.isMiddlePressed = flag;
                } else if (pButton == 1) {
                    this.isRightPressed = flag;
                }

                KeyMapping.set(InputConstants.Type.MOUSE.getOrCreate(pButton), flag);
                if (flag) {
                    if (this.minecraft.player.isSpectator() && pButton == 2) {
                        this.minecraft.gui.getSpectatorGui().onMouseMiddleClick();
                    } else {
                        KeyMapping.click(InputConstants.Type.MOUSE.getOrCreate(pButton));
                    }
                }
            }
        }
    }

    private void onScroll(long pWindowPointer, double pXOffset, double pYOffset) {
        if (pWindowPointer == Minecraft.getInstance().getWindow().getWindow()) {

            EventManager.call(new ScrollEvent((float)pYOffset));

            MouseHandlerOnActionEvent mouseHandlerOnActionEvent = new MouseHandlerOnActionEvent();
            EventManager.call(mouseHandlerOnActionEvent);
            if(mouseHandlerOnActionEvent.isCancelled()) return;

            this.minecraft.getFramerateLimitTracker().onInputReceived();
            boolean flag = this.minecraft.options.discreteMouseScroll().get();
            double d0 = this.minecraft.options.mouseWheelSensitivity().get();
            double d1 = (flag ? Math.signum(pXOffset) : pXOffset) * d0;
            double d2 = (flag ? Math.signum(pYOffset) : pYOffset) * d0;
            if (this.minecraft.getOverlay() == null) {
                if (this.minecraft.screen != null) {
                    double d3 = this.xpos * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
                    double d4 = this.ypos * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();
                    this.minecraft.screen.mouseScrolled(d3, d4, d1, d2);
                    this.minecraft.screen.afterMouseAction();
                } else if (this.minecraft.player != null) {
                    Vector2i vector2i = this.scrollWheelHandler.onMouseScroll(d1, d2);
                    if (vector2i.x == 0 && vector2i.y == 0) {
                        return;
                    }

                    int i = vector2i.y == 0 ? -vector2i.x : vector2i.y;
                    if (this.minecraft.player.isSpectator()) {
                        if (this.minecraft.gui.getSpectatorGui().isMenuActive()) {
                            this.minecraft.gui.getSpectatorGui().onMouseScrolled(-i);
                        } else {
                            float f = Mth.clamp(this.minecraft.player.getAbilities().getFlyingSpeed() + (float)vector2i.y * 0.005F, 0.0F, 0.2F);
                            this.minecraft.player.getAbilities().setFlyingSpeed(f);
                        }
                    } else {
                        Inventory inventory = this.minecraft.player.getInventory();
                        inventory.setSelectedHotbarSlot(ScrollWheelHandler.getNextScrollWheelSelection((double)i, inventory.selected, Inventory.getSelectionSize()));
                    }
                }
            }
        }
    }

    private void onDrop(long pWindowPointer, List<Path> pFiles, int pFailedFiles) {
        this.minecraft.getFramerateLimitTracker().onInputReceived();
        if (this.minecraft.screen != null) {
            this.minecraft.screen.onFilesDrop(pFiles);
        }

        if (pFailedFiles > 0) {
            SystemToast.onFileDropFailure(this.minecraft, pFailedFiles);
        }
    }

    public void setup(long pWindowPointer) {
        InputConstants.setupMouseCallbacks(
            pWindowPointer,
            (p_91591_, p_91592_, p_91593_) -> this.minecraft.execute(() -> this.onMove(p_91591_, p_91592_, p_91593_)),
            (p_91566_, p_91567_, p_91568_, p_91569_) -> this.minecraft.execute(() -> this.onPress(p_91566_, p_91567_, p_91568_, p_91569_)),
            (p_91576_, p_91577_, p_91578_) -> this.minecraft.execute(() -> this.onScroll(p_91576_, p_91577_, p_91578_)),
            (p_340767_, p_340768_, p_340769_) -> {
                List<Path> list = new ArrayList<>(p_340768_);
                int i = 0;

                for (int j = 0; j < p_340768_; j++) {
                    String s = GLFWDropCallback.getName(p_340769_, j);

                    try {
                        list.add(Paths.get(s));
                    } catch (InvalidPathException invalidpathexception) {
                        i++;
                        LOGGER.error("Failed to parse path '{}'", s, invalidpathexception);
                    }
                }

                if (!list.isEmpty()) {
                    int k = i;
                    this.minecraft.execute(() -> this.onDrop(p_340767_, list, k));
                }
            }
        );
    }

    private void onMove(long pWindowPointer, double pXpos, double pYpos) {
        if (pWindowPointer == Minecraft.getInstance().getWindow().getWindow()) {

            customMouseX = (float) pXpos;
            customMouseY = (float) pYpos;

            EventManager.call(new MouseMoveEvent(customMouseX, customMouseY));

            MouseHandlerOnActionEvent mouseHandlerOnActionEvent = new MouseHandlerOnActionEvent();
            EventManager.call(mouseHandlerOnActionEvent);
            if(mouseHandlerOnActionEvent.isCancelled()) return;

            if (this.ignoreFirstMove) {
                this.xpos = pXpos;
                this.ypos = pYpos;
                this.ignoreFirstMove = false;
            } else {
                if (this.minecraft.isWindowActive()) {
                    this.accumulatedDX = this.accumulatedDX + (pXpos - this.xpos);
                    this.accumulatedDY = this.accumulatedDY + (pYpos - this.ypos);
                }

                this.xpos = pXpos;
                this.ypos = pYpos;
            }
        }
    }

    public void handleAccumulatedMovement() {
        double d0 = Blaze3D.getTime();
        double d1 = d0 - this.lastHandleMovementTime;
        this.lastHandleMovementTime = d0;
        if (this.minecraft.isWindowActive()) {
            Screen screen = this.minecraft.screen;
            boolean flag = this.accumulatedDX != 0.0 || this.accumulatedDY != 0.0;
            if (flag) {
                this.minecraft.getFramerateLimitTracker().onInputReceived();
            }

            if (screen != null && this.minecraft.getOverlay() == null && flag) {
                double d2 = this.xpos * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
                double d3 = this.ypos * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();

                try {
                    screen.mouseMoved(d2, d3);
                } catch (Throwable throwable1) {
                    CrashReport crashreport = CrashReport.forThrowable(throwable1, "mouseMoved event handler");
                    screen.fillCrashDetails(crashreport);
                    CrashReportCategory crashreportcategory = crashreport.addCategory("Mouse");
                    crashreportcategory.setDetail("Scaled X", d2);
                    crashreportcategory.setDetail("Scaled Y", d3);
                    throw new ReportedException(crashreport);
                }

                if (this.activeButton != -1 && this.mousePressedTime > 0.0) {
                    double d4 = this.accumulatedDX * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
                    double d5 = this.accumulatedDY * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();

                    try {
                        screen.mouseDragged(d2, d3, this.activeButton, d4, d5);
                    } catch (Throwable throwable) {
                        CrashReport crashreport1 = CrashReport.forThrowable(throwable, "mouseDragged event handler");
                        screen.fillCrashDetails(crashreport1);
                        CrashReportCategory crashreportcategory1 = crashreport1.addCategory("Mouse");
                        crashreportcategory1.setDetail("Scaled X", d2);
                        crashreportcategory1.setDetail("Scaled Y", d3);
                        throw new ReportedException(crashreport1);
                    }
                }

                screen.afterMouseMove();
            }

            if (this.isMouseGrabbed() && this.minecraft.player != null) {
                this.turnPlayer(d1);
            }
        }

        this.accumulatedDX = 0.0;
        this.accumulatedDY = 0.0;
    }

    private void turnPlayer(double pMovementTime) {
        double d2 = this.minecraft.options.sensitivity().get() * 0.6F + 0.2F;
        double d3 = d2 * d2 * d2;
        double d4 = d3 * 8.0;
        double d0;
        double d1;
        if (this.minecraft.options.smoothCamera) {
            double d5 = this.smoothTurnX.getNewDeltaValue(this.accumulatedDX * d4, pMovementTime * d4);
            double d6 = this.smoothTurnY.getNewDeltaValue(this.accumulatedDY * d4, pMovementTime * d4);
            d0 = d5;
            d1 = d6;
        } else if (this.minecraft.options.getCameraType().isFirstPerson() && this.minecraft.player.isScoping()) {
            this.smoothTurnX.reset();
            this.smoothTurnY.reset();
            d0 = this.accumulatedDX * d3;
            d1 = this.accumulatedDY * d3;
        } else {
            this.smoothTurnX.reset();
            this.smoothTurnY.reset();
            d0 = this.accumulatedDX * d4 ;
            d1 = this.accumulatedDY * d4;
        }

        int i = 1;
        if (this.minecraft.options.invertYMouse().get()) {
            i = -1;
        }

        this.minecraft.getTutorial().onMouse(d0, d1);
        if (this.minecraft.player != null) {
            this.minecraft.player.turn(d0, d1 * (double)i);
        }
    }

    public boolean isLeftPressed() {
        return this.isLeftPressed;
    }

    public boolean isMiddlePressed() {
        return this.isMiddlePressed;
    }

    public boolean isRightPressed() {
        return this.isRightPressed;
    }

    public double xpos() {
        return this.xpos;
    }

    public double ypos() {
        return this.ypos;
    }

    public void setIgnoreFirstMove() {
        this.ignoreFirstMove = true;
    }

    public boolean isMouseGrabbed() {
        return this.mouseGrabbed;
    }

    public void grabMouse() {

        MouseHandlerOnActionEvent mouseHandlerOnActionEvent = new MouseHandlerOnActionEvent(MouseHandlerOnActionEvent.TYPE.GRAB_MOUSE);
        EventManager.call(mouseHandlerOnActionEvent);
        if(mouseHandlerOnActionEvent.isCancelled()) return;

        if (this.minecraft.isWindowActive()) {
            if (!this.mouseGrabbed) {
                if (!Minecraft.ON_OSX) {
                    KeyMapping.setAll();
                }

                this.mouseGrabbed = true;
                this.xpos = (double)(this.minecraft.getWindow().getScreenWidth() / 2);
                this.ypos = (double)(this.minecraft.getWindow().getScreenHeight() / 2);
                InputConstants.grabOrReleaseMouse(this.minecraft.getWindow().getWindow(), 212995, this.xpos, this.ypos);
                this.minecraft.setScreen(null);
                this.minecraft.missTime = 10000;
                this.ignoreFirstMove = true;
            }
        }
    }

    public void releaseMouse() {
        if (this.mouseGrabbed) {
            this.mouseGrabbed = false;
            this.xpos = (double)(this.minecraft.getWindow().getScreenWidth() / 2);
            this.ypos = (double)(this.minecraft.getWindow().getScreenHeight() / 2);
            InputConstants.grabOrReleaseMouse(this.minecraft.getWindow().getWindow(), 212993, this.xpos, this.ypos);
        }
    }

    public void cursorEntered() {
        this.ignoreFirstMove = true;
    }
}