package net.minecraft.world.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public interface Shearable {
    void shear(ServerLevel pLevel, SoundSource pSoundSource, ItemStack pShears);

    boolean readyForShearing();
}