package sisicat.events;

import net.minecraft.world.inventory.ClickType;

public class EventWindowClick implements com.darkmagician6.eventapi.events.Event {
    private final int windowId;
    private final int slot;
    private final int button;
    private final ClickType clickType;
    private final ClickStage clickStage;

    public EventWindowClick(ClickStage clickStage, int windowId, int slot, int button, ClickType clickType) {
        this.clickStage = clickStage;
        this.windowId = windowId;
        this.slot = slot;
        this.button = button;
        this.clickType = clickType;
    }


    public int getWindowId() {
        return this.windowId;
    }

    public int getSlot() {
        return this.slot;
    }

    public int getButton() {
        return this.button;
    }

    public ClickType getClickType() {
        return this.clickType;
    }

    public ClickStage getClickStage() {
        return this.clickStage;
    }

    public enum ClickStage {
        PRE, POST
    }
}
