package sisicat.events;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import net.minecraft.world.item.ItemStack;

public class UseItemEvent extends EventCancellable {

    private final ItemStack item;

    public UseItemEvent(ItemStack item){
        this.item = item;
    }

    public ItemStack getItem(){
        return item;
    }

}
