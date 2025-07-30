package net.minecraft.client.server;

import com.mojang.authlib.GameProfile;
import java.net.SocketAddress;
import javax.annotation.Nullable;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IntegratedPlayerList extends PlayerList {
    @Nullable
    private CompoundTag playerData;

    public IntegratedPlayerList(IntegratedServer pServer, LayeredRegistryAccess<RegistryLayer> pRegistries, PlayerDataStorage pPlayerIo) {
        super(pServer, pRegistries, pPlayerIo, 8);
        this.setViewDistance(10);
    }

    @Override
    protected void save(ServerPlayer pPlayer) {
        if (this.getServer().isSingleplayerOwner(pPlayer.getGameProfile())) {
            this.playerData = pPlayer.saveWithoutId(new CompoundTag());
        }

        super.save(pPlayer);
    }

    @Override
    public Component canPlayerLogin(SocketAddress p_120007_, GameProfile p_120008_) {
        return (Component)(this.getServer().isSingleplayerOwner(p_120008_) && this.getPlayerByName(p_120008_.getName()) != null
            ? Component.translatable("multiplayer.disconnect.name_taken")
            : super.canPlayerLogin(p_120007_, p_120008_));
    }

    public IntegratedServer getServer() {
        return (IntegratedServer)super.getServer();
    }

    @Nullable
    @Override
    public CompoundTag getSingleplayerData() {
        return this.playerData;
    }
}