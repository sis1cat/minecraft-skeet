package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;

public record DamageSourcePredicate(
    List<TagPredicate<DamageType>> tags, Optional<EntityPredicate> directEntity, Optional<EntityPredicate> sourceEntity, Optional<Boolean> isDirect
) {
    public static final Codec<DamageSourcePredicate> CODEC = RecordCodecBuilder.create(
        p_340752_ -> p_340752_.group(
                    TagPredicate.codec(Registries.DAMAGE_TYPE).listOf().optionalFieldOf("tags", List.of()).forGetter(DamageSourcePredicate::tags),
                    EntityPredicate.CODEC.optionalFieldOf("direct_entity").forGetter(DamageSourcePredicate::directEntity),
                    EntityPredicate.CODEC.optionalFieldOf("source_entity").forGetter(DamageSourcePredicate::sourceEntity),
                    Codec.BOOL.optionalFieldOf("is_direct").forGetter(DamageSourcePredicate::isDirect)
                )
                .apply(p_340752_, DamageSourcePredicate::new)
    );

    public boolean matches(ServerPlayer pPlayer, DamageSource pSource) {
        return this.matches(pPlayer.serverLevel(), pPlayer.position(), pSource);
    }

    public boolean matches(ServerLevel pLevel, Vec3 pPosition, DamageSource pSource) {
        for (TagPredicate<DamageType> tagpredicate : this.tags) {
            if (!tagpredicate.matches(pSource.typeHolder())) {
                return false;
            }
        }

        if (this.directEntity.isPresent() && !this.directEntity.get().matches(pLevel, pPosition, pSource.getDirectEntity())) {
            return false;
        } else {
            return this.sourceEntity.isPresent() && !this.sourceEntity.get().matches(pLevel, pPosition, pSource.getEntity())
                ? false
                : !this.isDirect.isPresent() || this.isDirect.get() == pSource.isDirect();
        }
    }

    public static class Builder {
        private final ImmutableList.Builder<TagPredicate<DamageType>> tags = ImmutableList.builder();
        private Optional<EntityPredicate> directEntity = Optional.empty();
        private Optional<EntityPredicate> sourceEntity = Optional.empty();
        private Optional<Boolean> isDirect = Optional.empty();

        public static DamageSourcePredicate.Builder damageType() {
            return new DamageSourcePredicate.Builder();
        }

        public DamageSourcePredicate.Builder tag(TagPredicate<DamageType> pTag) {
            this.tags.add(pTag);
            return this;
        }

        public DamageSourcePredicate.Builder direct(EntityPredicate.Builder pDirectEntity) {
            this.directEntity = Optional.of(pDirectEntity.build());
            return this;
        }

        public DamageSourcePredicate.Builder source(EntityPredicate.Builder pSourceEntity) {
            this.sourceEntity = Optional.of(pSourceEntity.build());
            return this;
        }

        public DamageSourcePredicate.Builder isDirect(boolean pIsDirect) {
            this.isDirect = Optional.of(pIsDirect);
            return this;
        }

        public DamageSourcePredicate build() {
            return new DamageSourcePredicate(this.tags.build(), this.directEntity, this.sourceEntity, this.isDirect);
        }
    }
}