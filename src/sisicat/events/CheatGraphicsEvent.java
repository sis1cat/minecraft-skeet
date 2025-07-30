package sisicat.events;

import com.darkmagician6.eventapi.events.Event;
import com.mojang.blaze3d.vertex.PoseStack;

public class CheatGraphicsEvent implements Event {

    public static PoseStack ms;

    public CheatGraphicsEvent(PoseStack ms){
        this.ms = ms;
    }

}
