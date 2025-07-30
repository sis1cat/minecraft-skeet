package sisicat.main.functions.miscellaneous;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import sisicat.events.MovementUpdateEvent;
import sisicat.events.TickEvent;
import sisicat.main.functions.Function;
import sisicat.main.functions.FunctionSetting;

public class CustomBlockPlaceDelay extends Function {

    private final FunctionSetting delay;

    public CustomBlockPlaceDelay(String name) {
        super(name);

        delay = new FunctionSetting(
                "Delay", 1,
                1, 4,
                "t", 1
        );

        this.addSetting(delay);

    }

    @EventTarget
    void _event(TickEvent ignored) {

        if(Minecraft.getInstance().rightClickDelay == 4)
            Minecraft.getInstance().rightClickDelay = (int) delay.getFloatValue();

    }

}

