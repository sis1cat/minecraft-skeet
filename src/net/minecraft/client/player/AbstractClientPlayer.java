package net.minecraft.client.player;

import com.mojang.authlib.GameProfile;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.ShoulderRidingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.optifine.Config;
import net.optifine.RandomEntities;
import net.optifine.player.CapeUtils;
import net.optifine.player.PlayerConfigurations;
import net.optifine.reflect.Reflector;

public abstract class AbstractClientPlayer extends Player {
    @Nullable
    private PlayerInfo playerInfo;
    protected Vec3 deltaMovementOnPreviousTick = Vec3.ZERO;
    public float elytraRotX;
    public float elytraRotY;
    public float elytraRotZ;
    public final ClientLevel clientLevel;
    public float walkDistO;
    public float walkDist;
    private ResourceLocation locationOfCape = null;
    private long reloadCapeTimeMs = 0L;
    private boolean elytraOfCape = false;
    private String nameClear = null;
    public ShoulderRidingEntity entityShoulderLeft;
    public ShoulderRidingEntity entityShoulderRight;
    public ShoulderRidingEntity lastAttachedEntity;
    public float capeFlap;
    public float capeLean;
    public float capeLean2;
    private static final ResourceLocation TEXTURE_ELYTRA = new ResourceLocation("textures/entity/elytra.png");

    public AbstractClientPlayer(ClientLevel pClientLevel, GameProfile pGameProfile) {
        super(pClientLevel, pClientLevel.getSharedSpawnPos(), pClientLevel.getSharedSpawnAngle(), pGameProfile);
        this.clientLevel = pClientLevel;
        this.nameClear = pGameProfile.getName();
        if (this.nameClear != null && !this.nameClear.isEmpty()) {
            this.nameClear = StringUtil.stripColor(this.nameClear);
        }

        CapeUtils.downloadCape(this);
        PlayerConfigurations.getPlayerConfiguration(this);
    }

    @Override
    public boolean isSpectator() {
        PlayerInfo playerinfo = this.getPlayerInfo();
        return playerinfo != null && playerinfo.getGameMode() == GameType.SPECTATOR;
    }

    @Override
    public boolean isCreative() {
        PlayerInfo playerinfo = this.getPlayerInfo();
        return playerinfo != null && playerinfo.getGameMode() == GameType.CREATIVE;
    }

    @Nullable
    protected PlayerInfo getPlayerInfo() {
        if (this.playerInfo == null) {
            this.playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(this.getUUID());
        }

        return this.playerInfo;
    }

    @Override
    public void tick() {
        this.walkDistO = this.walkDist;
        this.deltaMovementOnPreviousTick = this.getDeltaMovement();
        super.tick();
        if (this.lastAttachedEntity != null) {
            RandomEntities.checkEntityShoulder(this.lastAttachedEntity, true);
            this.lastAttachedEntity = null;
        }
    }

    public Vec3 getDeltaMovementLerped(float pPatialTick) {
        return this.deltaMovementOnPreviousTick.lerp(this.getDeltaMovement(), (double)pPatialTick);
    }

    public PlayerSkin getSkin() {
        PlayerInfo playerinfo = this.getPlayerInfo();
        return playerinfo == null ? DefaultPlayerSkin.get(this.getUUID()) : playerinfo.getSkin();
    }

    public float getFieldOfViewModifier(boolean pIsFirstPerson, float pFovEffectScale) {
        float f = 1.0F;
        if (this.getAbilities().flying) {
            f *= 1.1F;
        }

        float f1 = this.getAbilities().getWalkingSpeed();
        if (f1 != 0.0F) {
            float f2 = (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) / f1;
            f *= (f2 + 1.0F) / 2.0F;
        }

        if (this.isUsingItem()) {
            if (this.getUseItem().is(Items.BOW)) {
                float f3 = Math.min((float)this.getTicksUsingItem() / 20.0F, 1.0F);
                f *= 1.0F - Mth.square(f3) * 0.15F;
            } else if (pIsFirstPerson && this.isScoping()) {
                return 0.1F;
            }
        }

        if (Reflector.ForgeEventFactoryClient_fireFovModifierEvent.exists()) {
            ComputeFovModifierEvent computefovmodifierevent = (ComputeFovModifierEvent)Reflector.ForgeEventFactoryClient_fireFovModifierEvent
                .call(this, f, pFovEffectScale);
            if (computefovmodifierevent != null) {
                return computefovmodifierevent.getNewFovModifier();
            }
        }

        return Mth.lerp(pFovEffectScale, 1.0F, f);
    }

    public String getNameClear() {
        return this.nameClear;
    }

    public ResourceLocation getLocationOfCape() {
        return this.locationOfCape;
    }

    public void setLocationOfCape(ResourceLocation locationOfCape) {
        this.locationOfCape = locationOfCape;
    }

    public boolean hasElytraCape() {
        ResourceLocation resourcelocation = this.getLocationCape();
        if (resourcelocation == null) {
            return false;
        } else {
            return resourcelocation == this.locationOfCape ? this.elytraOfCape : true;
        }
    }

    public void setElytraOfCape(boolean elytraOfCape) {
        this.elytraOfCape = elytraOfCape;
    }

    public boolean isElytraOfCape() {
        return this.elytraOfCape;
    }

    public long getReloadCapeTimeMs() {
        return this.reloadCapeTimeMs;
    }

    public void setReloadCapeTimeMs(long reloadCapeTimeMs) {
        this.reloadCapeTimeMs = reloadCapeTimeMs;
    }

    @Nullable
    public ResourceLocation getLocationCape() {
        if (!Config.isShowCapes()) {
            return null;
        } else {
            if (this.reloadCapeTimeMs != 0L && System.currentTimeMillis() > this.reloadCapeTimeMs) {
                CapeUtils.reloadCape(this);
                this.reloadCapeTimeMs = 0L;
                PlayerConfigurations.setPlayerConfiguration(this.getNameClear(), null);
            }

            return this.locationOfCape != null ? this.locationOfCape : this.getSkin().capeTexture();
        }
    }

    public ResourceLocation getLocationElytra() {
        return this.hasElytraCape() ? this.locationOfCape : this.getSkin().elytraTexture();
    }

    public ResourceLocation getSkinTextureLocation() {
        PlayerInfo playerinfo = this.getPlayerInfo();
        return playerinfo == null ? DefaultPlayerSkin.get(this.getUUID()).texture() : playerinfo.getSkin().texture();
    }
}