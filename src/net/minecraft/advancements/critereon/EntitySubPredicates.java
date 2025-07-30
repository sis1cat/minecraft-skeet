package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.WolfVariant;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.phys.Vec3;

public class EntitySubPredicates {
    public static final MapCodec<LightningBoltPredicate> LIGHTNING = register("lightning", LightningBoltPredicate.CODEC);
    public static final MapCodec<FishingHookPredicate> FISHING_HOOK = register("fishing_hook", FishingHookPredicate.CODEC);
    public static final MapCodec<PlayerPredicate> PLAYER = register("player", PlayerPredicate.CODEC);
    public static final MapCodec<SlimePredicate> SLIME = register("slime", SlimePredicate.CODEC);
    public static final MapCodec<RaiderPredicate> RAIDER = register("raider", RaiderPredicate.CODEC);
    public static final MapCodec<SheepPredicate> SHEEP = register("sheep", SheepPredicate.CODEC);
    public static final EntitySubPredicates.EntityVariantPredicateType<Axolotl.Variant> AXOLOTL = register(
        "axolotl",
        EntitySubPredicates.EntityVariantPredicateType.create(
            Axolotl.Variant.CODEC, p_334006_ -> p_334006_ instanceof Axolotl axolotl ? Optional.of(axolotl.getVariant()) : Optional.empty()
        )
    );
    public static final EntitySubPredicates.EntityVariantPredicateType<Fox.Variant> FOX = register(
        "fox",
        EntitySubPredicates.EntityVariantPredicateType.create(
            Fox.Variant.CODEC, p_357623_ -> p_357623_ instanceof Fox fox ? Optional.of(fox.getVariant()) : Optional.empty()
        )
    );
    public static final EntitySubPredicates.EntityVariantPredicateType<MushroomCow.Variant> MOOSHROOM = register(
        "mooshroom",
        EntitySubPredicates.EntityVariantPredicateType.create(
            MushroomCow.Variant.CODEC, p_357622_ -> p_357622_ instanceof MushroomCow mushroomcow ? Optional.of(mushroomcow.getVariant()) : Optional.empty()
        )
    );
    public static final EntitySubPredicates.EntityVariantPredicateType<Rabbit.Variant> RABBIT = register(
        "rabbit",
        EntitySubPredicates.EntityVariantPredicateType.create(
            Rabbit.Variant.CODEC, p_334309_ -> p_334309_ instanceof Rabbit rabbit ? Optional.of(rabbit.getVariant()) : Optional.empty()
        )
    );
    public static final EntitySubPredicates.EntityVariantPredicateType<Variant> HORSE = register(
        "horse",
        EntitySubPredicates.EntityVariantPredicateType.create(
            Variant.CODEC, p_334549_ -> p_334549_ instanceof Horse horse ? Optional.of(horse.getVariant()) : Optional.empty()
        )
    );
    public static final EntitySubPredicates.EntityVariantPredicateType<Llama.Variant> LLAMA = register(
        "llama",
        EntitySubPredicates.EntityVariantPredicateType.create(
            Llama.Variant.CODEC, p_336380_ -> p_336380_ instanceof Llama llama ? Optional.of(llama.getVariant()) : Optional.empty()
        )
    );
    public static final EntitySubPredicates.EntityVariantPredicateType<VillagerType> VILLAGER = register(
        "villager",
        EntitySubPredicates.EntityVariantPredicateType.create(
            BuiltInRegistries.VILLAGER_TYPE.byNameCodec(),
            p_334803_ -> p_334803_ instanceof VillagerDataHolder villagerdataholder ? Optional.of(villagerdataholder.getVariant()) : Optional.empty()
        )
    );
    public static final EntitySubPredicates.EntityVariantPredicateType<Parrot.Variant> PARROT = register(
        "parrot",
        EntitySubPredicates.EntityVariantPredicateType.create(
            Parrot.Variant.CODEC, p_327673_ -> p_327673_ instanceof Parrot parrot ? Optional.of(parrot.getVariant()) : Optional.empty()
        )
    );
    public static final EntitySubPredicates.EntityVariantPredicateType<Salmon.Variant> SALMON = register(
        "salmon",
        EntitySubPredicates.EntityVariantPredicateType.create(
            Salmon.Variant.CODEC, p_357624_ -> p_357624_ instanceof Salmon salmon ? Optional.of(salmon.getVariant()) : Optional.empty()
        )
    );
    public static final EntitySubPredicates.EntityVariantPredicateType<TropicalFish.Pattern> TROPICAL_FISH = register(
        "tropical_fish",
        EntitySubPredicates.EntityVariantPredicateType.create(
            TropicalFish.Pattern.CODEC,
            p_330151_ -> p_330151_ instanceof TropicalFish tropicalfish ? Optional.of(tropicalfish.getVariant()) : Optional.empty()
        )
    );
    public static final EntitySubPredicates.EntityHolderVariantPredicateType<PaintingVariant> PAINTING = register(
        "painting",
        EntitySubPredicates.EntityHolderVariantPredicateType.create(
            Registries.PAINTING_VARIANT, p_329680_ -> p_329680_ instanceof Painting painting ? Optional.of(painting.getVariant()) : Optional.empty()
        )
    );
    public static final EntitySubPredicates.EntityHolderVariantPredicateType<CatVariant> CAT = register(
        "cat",
        EntitySubPredicates.EntityHolderVariantPredicateType.create(
            Registries.CAT_VARIANT, p_331742_ -> p_331742_ instanceof Cat cat ? Optional.of(cat.getVariant()) : Optional.empty()
        )
    );
    public static final EntitySubPredicates.EntityHolderVariantPredicateType<FrogVariant> FROG = register(
        "frog",
        EntitySubPredicates.EntityHolderVariantPredicateType.create(
            Registries.FROG_VARIANT, p_334670_ -> p_334670_ instanceof Frog frog ? Optional.of(frog.getVariant()) : Optional.empty()
        )
    );
    public static final EntitySubPredicates.EntityHolderVariantPredicateType<WolfVariant> WOLF = register(
        "wolf",
        EntitySubPredicates.EntityHolderVariantPredicateType.create(
            Registries.WOLF_VARIANT, p_334632_ -> p_334632_ instanceof Wolf wolf ? Optional.of(wolf.getVariant()) : Optional.empty()
        )
    );

    private static <T extends EntitySubPredicate> MapCodec<T> register(String pName, MapCodec<T> pCodec) {
        return Registry.register(BuiltInRegistries.ENTITY_SUB_PREDICATE_TYPE, pName, pCodec);
    }

    private static <V> EntitySubPredicates.EntityVariantPredicateType<V> register(
        String pName, EntitySubPredicates.EntityVariantPredicateType<V> pPredicateType
    ) {
        Registry.register(BuiltInRegistries.ENTITY_SUB_PREDICATE_TYPE, pName, pPredicateType.codec);
        return pPredicateType;
    }

    private static <V> EntitySubPredicates.EntityHolderVariantPredicateType<V> register(
        String pName, EntitySubPredicates.EntityHolderVariantPredicateType<V> pPredicateType
    ) {
        Registry.register(BuiltInRegistries.ENTITY_SUB_PREDICATE_TYPE, pName, pPredicateType.codec);
        return pPredicateType;
    }

    public static MapCodec<? extends EntitySubPredicate> bootstrap(Registry<MapCodec<? extends EntitySubPredicate>> pRegistry) {
        return LIGHTNING;
    }

    public static EntitySubPredicate catVariant(Holder<CatVariant> pCatVariant) {
        return CAT.createPredicate(HolderSet.direct(pCatVariant));
    }

    public static EntitySubPredicate frogVariant(Holder<FrogVariant> pFrogVariant) {
        return FROG.createPredicate(HolderSet.direct(pFrogVariant));
    }

    public static EntitySubPredicate wolfVariant(HolderSet<WolfVariant> pWolfVariant) {
        return WOLF.createPredicate(pWolfVariant);
    }

    public static class EntityHolderVariantPredicateType<V> {
        final MapCodec<EntitySubPredicates.EntityHolderVariantPredicateType<V>.Instance> codec;
        final Function<Entity, Optional<Holder<V>>> getter;

        public static <V> EntitySubPredicates.EntityHolderVariantPredicateType<V> create(
            ResourceKey<? extends Registry<V>> pRegistryKey, Function<Entity, Optional<Holder<V>>> pGetter
        ) {
            return new EntitySubPredicates.EntityHolderVariantPredicateType<>(pRegistryKey, pGetter);
        }

        public EntityHolderVariantPredicateType(ResourceKey<? extends Registry<V>> pRegistryKey, Function<Entity, Optional<Holder<V>>> pGetter) {
            this.getter = pGetter;
            this.codec = RecordCodecBuilder.mapCodec(
                p_330908_ -> p_330908_.group(RegistryCodecs.homogeneousList(pRegistryKey).fieldOf("variant").forGetter(p_329421_ -> p_329421_.variants))
                        .apply(p_330908_, p_331166_ -> new EntitySubPredicates.EntityHolderVariantPredicateType<V>.Instance(p_331166_))
            );
        }

        public EntitySubPredicate createPredicate(HolderSet<V> pVariants) {
            return new EntitySubPredicates.EntityHolderVariantPredicateType.Instance(pVariants);
        }

        class Instance implements EntitySubPredicate {
            final HolderSet<V> variants;

            Instance(final HolderSet<V> pVariants) {
                this.variants = pVariants;
            }

            @Override
            public MapCodec<EntitySubPredicates.EntityHolderVariantPredicateType<V>.Instance> codec() {
                return EntityHolderVariantPredicateType.this.codec;
            }

            @Override
            public boolean matches(Entity p_330194_, ServerLevel p_330112_, @Nullable Vec3 p_329192_) {
                return EntityHolderVariantPredicateType.this.getter.apply(p_330194_).filter(this.variants::contains).isPresent();
            }
        }
    }

    public static class EntityVariantPredicateType<V> {
        final MapCodec<EntitySubPredicates.EntityVariantPredicateType<V>.Instance> codec;
        final Function<Entity, Optional<V>> getter;

        public static <V> EntitySubPredicates.EntityVariantPredicateType<V> create(Registry<V> pVariantRegistry, Function<Entity, Optional<V>> pGetter) {
            return new EntitySubPredicates.EntityVariantPredicateType<>(pVariantRegistry.byNameCodec(), pGetter);
        }

        public static <V> EntitySubPredicates.EntityVariantPredicateType<V> create(Codec<V> pCodec, Function<Entity, Optional<V>> pGetter) {
            return new EntitySubPredicates.EntityVariantPredicateType<>(pCodec, pGetter);
        }

        public EntityVariantPredicateType(Codec<V> pCodec, Function<Entity, Optional<V>> pGetter) {
            this.getter = pGetter;
            this.codec = RecordCodecBuilder.mapCodec(
                p_330838_ -> p_330838_.group(pCodec.fieldOf("variant").forGetter(p_332763_ -> p_332763_.variant))
                        .apply(p_330838_, p_327954_ -> new EntitySubPredicates.EntityVariantPredicateType<V>.Instance(p_327954_))
            );
        }

        public EntitySubPredicate createPredicate(V pVariant) {
            return new EntitySubPredicates.EntityVariantPredicateType.Instance(pVariant);
        }

        class Instance implements EntitySubPredicate {
            final V variant;

            Instance(final V pVariant) {
                this.variant = pVariant;
            }

            @Override
            public MapCodec<EntitySubPredicates.EntityVariantPredicateType<V>.Instance> codec() {
                return EntityVariantPredicateType.this.codec;
            }

            @Override
            public boolean matches(Entity p_333217_, ServerLevel p_332166_, @Nullable Vec3 p_334706_) {
                return EntityVariantPredicateType.this.getter.apply(p_333217_).filter(this.variant::equals).isPresent();
            }
        }
    }
}