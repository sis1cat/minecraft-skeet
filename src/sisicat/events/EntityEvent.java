package sisicat.events;

import com.darkmagician6.eventapi.events.Event;
import net.minecraft.world.entity.LivingEntity;

public class EntityEvent implements Event {

    public LivingEntity livingEntity;
    public byte Id;

    public EntityEvent(LivingEntity livingEntity, byte Id){
        this.livingEntity = livingEntity;
        this.Id = Id;
    }

}
