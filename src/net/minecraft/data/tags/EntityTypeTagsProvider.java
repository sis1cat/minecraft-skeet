package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;

public class EntityTypeTagsProvider extends IntrinsicHolderTagsProvider<EntityType<?>> {
    public EntityTypeTagsProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pProvider) {
        super(pOutput, Registries.ENTITY_TYPE, pProvider, p_256665_ -> p_256665_.builtInRegistryHolder().key());
    }

    @Override
    protected void addTags(HolderLookup.Provider p_255894_) {
        this.tag(EntityTypeTags.SKELETONS)
            .add(EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON, EntityType.SKELETON_HORSE, EntityType.BOGGED);
        this.tag(EntityTypeTags.ZOMBIES)
            .add(
                EntityType.ZOMBIE_HORSE,
                EntityType.ZOMBIE,
                EntityType.ZOMBIE_VILLAGER,
                EntityType.ZOMBIFIED_PIGLIN,
                EntityType.ZOGLIN,
                EntityType.DROWNED,
                EntityType.HUSK
            );
        this.tag(EntityTypeTags.RAIDERS)
            .add(EntityType.EVOKER, EntityType.PILLAGER, EntityType.RAVAGER, EntityType.VINDICATOR, EntityType.ILLUSIONER, EntityType.WITCH);
        this.tag(EntityTypeTags.UNDEAD)
            .addTag(EntityTypeTags.SKELETONS)
            .addTag(EntityTypeTags.ZOMBIES)
            .add(EntityType.WITHER)
            .add(EntityType.PHANTOM);
        this.tag(EntityTypeTags.BEEHIVE_INHABITORS).add(EntityType.BEE);
        this.tag(EntityTypeTags.ARROWS).add(EntityType.ARROW, EntityType.SPECTRAL_ARROW);
        this.tag(EntityTypeTags.IMPACT_PROJECTILES)
            .addTag(EntityTypeTags.ARROWS)
            .add(EntityType.FIREWORK_ROCKET)
            .add(
                EntityType.SNOWBALL,
                EntityType.FIREBALL,
                EntityType.SMALL_FIREBALL,
                EntityType.EGG,
                EntityType.TRIDENT,
                EntityType.DRAGON_FIREBALL,
                EntityType.WITHER_SKULL,
                EntityType.WIND_CHARGE,
                EntityType.BREEZE_WIND_CHARGE
            );
        this.tag(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS).add(EntityType.RABBIT, EntityType.ENDERMITE, EntityType.SILVERFISH, EntityType.FOX);
        this.tag(EntityTypeTags.AXOLOTL_HUNT_TARGETS)
            .add(
                EntityType.TROPICAL_FISH,
                EntityType.PUFFERFISH,
                EntityType.SALMON,
                EntityType.COD,
                EntityType.SQUID,
                EntityType.GLOW_SQUID,
                EntityType.TADPOLE
            );
        this.tag(EntityTypeTags.AXOLOTL_ALWAYS_HOSTILES).add(EntityType.DROWNED, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN);
        this.tag(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES).add(EntityType.STRAY, EntityType.POLAR_BEAR, EntityType.SNOW_GOLEM, EntityType.WITHER);
        this.tag(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES).add(EntityType.STRIDER, EntityType.BLAZE, EntityType.MAGMA_CUBE);
        this.tag(EntityTypeTags.CAN_BREATHE_UNDER_WATER)
            .addTag(EntityTypeTags.UNDEAD)
            .add(
                EntityType.AXOLOTL,
                EntityType.FROG,
                EntityType.GUARDIAN,
                EntityType.ELDER_GUARDIAN,
                EntityType.TURTLE,
                EntityType.GLOW_SQUID,
                EntityType.COD,
                EntityType.PUFFERFISH,
                EntityType.SALMON,
                EntityType.SQUID,
                EntityType.TROPICAL_FISH,
                EntityType.TADPOLE,
                EntityType.ARMOR_STAND
            );
        this.tag(EntityTypeTags.FROG_FOOD).add(EntityType.SLIME, EntityType.MAGMA_CUBE);
        this.tag(EntityTypeTags.FALL_DAMAGE_IMMUNE)
            .add(
                EntityType.IRON_GOLEM,
                EntityType.SNOW_GOLEM,
                EntityType.SHULKER,
                EntityType.ALLAY,
                EntityType.BAT,
                EntityType.BEE,
                EntityType.BLAZE,
                EntityType.CAT,
                EntityType.CHICKEN,
                EntityType.GHAST,
                EntityType.PHANTOM,
                EntityType.MAGMA_CUBE,
                EntityType.OCELOT,
                EntityType.PARROT,
                EntityType.WITHER,
                EntityType.BREEZE
            );
        this.tag(EntityTypeTags.DISMOUNTS_UNDERWATER)
            .add(
                EntityType.CAMEL,
                EntityType.CHICKEN,
                EntityType.DONKEY,
                EntityType.HORSE,
                EntityType.LLAMA,
                EntityType.MULE,
                EntityType.PIG,
                EntityType.RAVAGER,
                EntityType.SPIDER,
                EntityType.STRIDER,
                EntityType.TRADER_LLAMA,
                EntityType.ZOMBIE_HORSE
            );
        this.tag(EntityTypeTags.NON_CONTROLLING_RIDER).add(EntityType.SLIME, EntityType.MAGMA_CUBE);
        this.tag(EntityTypeTags.ILLAGER)
            .add(EntityType.EVOKER)
            .add(EntityType.ILLUSIONER)
            .add(EntityType.PILLAGER)
            .add(EntityType.VINDICATOR);
        this.tag(EntityTypeTags.AQUATIC)
            .add(EntityType.TURTLE)
            .add(EntityType.AXOLOTL)
            .add(EntityType.GUARDIAN)
            .add(EntityType.ELDER_GUARDIAN)
            .add(EntityType.COD)
            .add(EntityType.PUFFERFISH)
            .add(EntityType.SALMON)
            .add(EntityType.TROPICAL_FISH)
            .add(EntityType.DOLPHIN)
            .add(EntityType.SQUID)
            .add(EntityType.GLOW_SQUID)
            .add(EntityType.TADPOLE);
        this.tag(EntityTypeTags.ARTHROPOD)
            .add(EntityType.BEE)
            .add(EntityType.ENDERMITE)
            .add(EntityType.SILVERFISH)
            .add(EntityType.SPIDER)
            .add(EntityType.CAVE_SPIDER);
        this.tag(EntityTypeTags.IGNORES_POISON_AND_REGEN).addTag(EntityTypeTags.UNDEAD);
        this.tag(EntityTypeTags.INVERTED_HEALING_AND_HARM).addTag(EntityTypeTags.UNDEAD);
        this.tag(EntityTypeTags.WITHER_FRIENDS).addTag(EntityTypeTags.UNDEAD);
        this.tag(EntityTypeTags.ILLAGER_FRIENDS).addTag(EntityTypeTags.ILLAGER);
        this.tag(EntityTypeTags.NOT_SCARY_FOR_PUFFERFISH)
            .add(EntityType.TURTLE)
            .add(EntityType.GUARDIAN)
            .add(EntityType.ELDER_GUARDIAN)
            .add(EntityType.COD)
            .add(EntityType.PUFFERFISH)
            .add(EntityType.SALMON)
            .add(EntityType.TROPICAL_FISH)
            .add(EntityType.DOLPHIN)
            .add(EntityType.SQUID)
            .add(EntityType.GLOW_SQUID)
            .add(EntityType.TADPOLE);
        this.tag(EntityTypeTags.SENSITIVE_TO_IMPALING).addTag(EntityTypeTags.AQUATIC);
        this.tag(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS).addTag(EntityTypeTags.ARTHROPOD);
        this.tag(EntityTypeTags.SENSITIVE_TO_SMITE).addTag(EntityTypeTags.UNDEAD);
        this.tag(EntityTypeTags.REDIRECTABLE_PROJECTILE).add(EntityType.FIREBALL, EntityType.WIND_CHARGE, EntityType.BREEZE_WIND_CHARGE);
        this.tag(EntityTypeTags.DEFLECTS_PROJECTILES).add(EntityType.BREEZE);
        this.tag(EntityTypeTags.CAN_TURN_IN_BOATS).add(EntityType.BREEZE);
        this.tag(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE)
            .add(
                EntityType.BREEZE,
                EntityType.SKELETON,
                EntityType.BOGGED,
                EntityType.STRAY,
                EntityType.ZOMBIE,
                EntityType.HUSK,
                EntityType.SPIDER,
                EntityType.CAVE_SPIDER,
                EntityType.SLIME
            );
        this.tag(EntityTypeTags.IMMUNE_TO_INFESTED).add(EntityType.SILVERFISH);
        this.tag(EntityTypeTags.IMMUNE_TO_OOZING).add(EntityType.SLIME);
        this.tag(EntityTypeTags.BOAT)
            .add(
                EntityType.OAK_BOAT,
                EntityType.SPRUCE_BOAT,
                EntityType.BIRCH_BOAT,
                EntityType.JUNGLE_BOAT,
                EntityType.ACACIA_BOAT,
                EntityType.CHERRY_BOAT,
                EntityType.DARK_OAK_BOAT,
                EntityType.PALE_OAK_BOAT,
                EntityType.MANGROVE_BOAT,
                EntityType.BAMBOO_RAFT
            );
    }
}