package net.minecraft.world.inventory;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.util.StringRepresentable;

public interface SlotRange extends StringRepresentable {
    IntList slots();

    default int size() {
        return this.slots().size();
    }

    static SlotRange of(final String pName, final IntList pValues) {
        return new SlotRange() {
            @Override
            public IntList slots() {
                return pValues;
            }

            @Override
            public String getSerializedName() {
                return pName;
            }

            @Override
            public String toString() {
                return pName;
            }
        };
    }
}