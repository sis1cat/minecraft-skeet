package sisicat.events;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import com.mojang.blaze3d.vertex.PoseStack;

public class InputChatEvent extends EventCancellable {

    String text;
    public static PoseStack ms;

    public InputChatEvent(String text, PoseStack ms) {
        this.text = text; this.ms = ms;
    }

    public String getMessage() {
        return this.text;
    }

    public void setMessage(String message) {
        this.text = message;
    }

}