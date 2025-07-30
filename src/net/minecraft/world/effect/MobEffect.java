package net.minecraft.world.effect;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;

public class MobEffect implements FeatureElement {
    public static final Codec<Holder<MobEffect>> CODEC = BuiltInRegistries.MOB_EFFECT.holderByNameCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<MobEffect>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.MOB_EFFECT);
    private static final int AMBIENT_ALPHA = Mth.floor(38.25F);
    private final Map<Holder<Attribute>, MobEffect.AttributeTemplate> attributeModifiers = new Object2ObjectOpenHashMap<>();
    private final MobEffectCategory category;
    private final int color;
    private final Function<MobEffectInstance, ParticleOptions> particleFactory;
    @Nullable
    private String descriptionId;
    private int blendDurationTicks;
    private Optional<SoundEvent> soundOnAdded = Optional.empty();
    private FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;

    protected MobEffect(MobEffectCategory pCategory, int pColor) {
        this.category = pCategory;
        this.color = pColor;
        this.particleFactory = p_358853_ -> {
            int i = p_358853_.isAmbient() ? AMBIENT_ALPHA : 255;
            return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ARGB.color(i, pColor));
        };
    }

    protected MobEffect(MobEffectCategory pCategory, int pColor, ParticleOptions pParticle) {
        this.category = pCategory;
        this.color = pColor;
        this.particleFactory = p_326745_ -> pParticle;
    }

    public int getBlendDurationTicks() {
        return this.blendDurationTicks;
    }

    public boolean applyEffectTick(ServerLevel pLevel, LivingEntity pEntity, int pAmplifier) {
        return true;
    }

    public void applyInstantenousEffect(ServerLevel pLevel, @Nullable Entity pSource, @Nullable Entity pIndirectSource, LivingEntity pEntity, int pAmplifier, double pHealth) {
        this.applyEffectTick(pLevel, pEntity, pAmplifier);
    }

    public boolean shouldApplyEffectTickThisTick(int pDuration, int pAmplifier) {
        return false;
    }

    public void onEffectStarted(LivingEntity pEntity, int pAmplifier) {
    }

    public void onEffectAdded(LivingEntity pEntity, int pAmplifier) {
        this.soundOnAdded
            .ifPresent(
                p_374927_ -> pEntity.level()
                        .playSound(null, pEntity.getX(), pEntity.getY(), pEntity.getZ(), p_374927_, pEntity.getSoundSource(), 1.0F, 1.0F)
            );
    }

    public void onMobRemoved(ServerLevel pLevel, LivingEntity pEntity, int pAmplifier, Entity.RemovalReason pReason) {
    }

    public void onMobHurt(ServerLevel pLevel, LivingEntity pEntity, int pAmplifier, DamageSource pDamageSource, float pAmount) {
    }

    public boolean isInstantenous() {
        return false;
    }

    protected String getOrCreateDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = Util.makeDescriptionId("effect", BuiltInRegistries.MOB_EFFECT.getKey(this));
        }

        return this.descriptionId;
    }

    public String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }

    public Component getDisplayName() {
        return Component.translatable(this.getDescriptionId());
    }

    public MobEffectCategory getCategory() {
        return this.category;
    }

    public int getColor() {
        return this.color;
    }

    public MobEffect addAttributeModifier(Holder<Attribute> pAttribute, ResourceLocation pId, double pAmount, AttributeModifier.Operation pOperation) {
        this.attributeModifiers.put(pAttribute, new MobEffect.AttributeTemplate(pId, pAmount, pOperation));
        return this;
    }

    public MobEffect setBlendDuration(int pBlendDuration) {
        this.blendDurationTicks = pBlendDuration;
        return this;
    }

    public void createModifiers(int pAmplifier, BiConsumer<Holder<Attribute>, AttributeModifier> pOutput) {
        this.attributeModifiers.forEach((p_341255_, p_341256_) -> pOutput.accept((Holder<Attribute>)p_341255_, p_341256_.create(pAmplifier)));
    }

    public void removeAttributeModifiers(AttributeMap pAttributeMap) {
        for (Entry<Holder<Attribute>, MobEffect.AttributeTemplate> entry : this.attributeModifiers.entrySet()) {
            AttributeInstance attributeinstance = pAttributeMap.getInstance(entry.getKey());
            if (attributeinstance != null) {
                attributeinstance.removeModifier(entry.getValue().id());
            }
        }
    }

    public void addAttributeModifiers(AttributeMap pAttributeMap, int pAmplifier) {
        for (Entry<Holder<Attribute>, MobEffect.AttributeTemplate> entry : this.attributeModifiers.entrySet()) {
            AttributeInstance attributeinstance = pAttributeMap.getInstance(entry.getKey());
            if (attributeinstance != null) {
                attributeinstance.removeModifier(entry.getValue().id());
                attributeinstance.addPermanentModifier(entry.getValue().create(pAmplifier));
            }
        }
    }

    public boolean isBeneficial() {
        return this.category == MobEffectCategory.BENEFICIAL;
    }

    public ParticleOptions createParticleOptions(MobEffectInstance pEffect) {
        return this.particleFactory.apply(pEffect);
    }

    public MobEffect withSoundOnAdded(SoundEvent pSound) {
        this.soundOnAdded = Optional.of(pSound);
        return this;
    }

    public MobEffect requiredFeatures(FeatureFlag... pRequiredFeatures) {
        this.requiredFeatures = FeatureFlags.REGISTRY.subset(pRequiredFeatures);
        return this;
    }

    @Override
    public FeatureFlagSet requiredFeatures() {
        return this.requiredFeatures;
    }

    static record AttributeTemplate(ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        public AttributeModifier create(int pLevel) {
            return new AttributeModifier(this.id, this.amount * (double)(pLevel + 1), this.operation);
        }
    }
}