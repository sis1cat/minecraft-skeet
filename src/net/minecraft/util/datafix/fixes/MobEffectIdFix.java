package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class MobEffectIdFix extends DataFix {
    private static final Int2ObjectMap<String> ID_MAP = Util.make(new Int2ObjectOpenHashMap<>(), p_298157_ -> {
        p_298157_.put(1, "minecraft:speed");
        p_298157_.put(2, "minecraft:slowness");
        p_298157_.put(3, "minecraft:haste");
        p_298157_.put(4, "minecraft:mining_fatigue");
        p_298157_.put(5, "minecraft:strength");
        p_298157_.put(6, "minecraft:instant_health");
        p_298157_.put(7, "minecraft:instant_damage");
        p_298157_.put(8, "minecraft:jump_boost");
        p_298157_.put(9, "minecraft:nausea");
        p_298157_.put(10, "minecraft:regeneration");
        p_298157_.put(11, "minecraft:resistance");
        p_298157_.put(12, "minecraft:fire_resistance");
        p_298157_.put(13, "minecraft:water_breathing");
        p_298157_.put(14, "minecraft:invisibility");
        p_298157_.put(15, "minecraft:blindness");
        p_298157_.put(16, "minecraft:night_vision");
        p_298157_.put(17, "minecraft:hunger");
        p_298157_.put(18, "minecraft:weakness");
        p_298157_.put(19, "minecraft:poison");
        p_298157_.put(20, "minecraft:wither");
        p_298157_.put(21, "minecraft:health_boost");
        p_298157_.put(22, "minecraft:absorption");
        p_298157_.put(23, "minecraft:saturation");
        p_298157_.put(24, "minecraft:glowing");
        p_298157_.put(25, "minecraft:levitation");
        p_298157_.put(26, "minecraft:luck");
        p_298157_.put(27, "minecraft:unluck");
        p_298157_.put(28, "minecraft:slow_falling");
        p_298157_.put(29, "minecraft:conduit_power");
        p_298157_.put(30, "minecraft:dolphins_grace");
        p_298157_.put(31, "minecraft:bad_omen");
        p_298157_.put(32, "minecraft:hero_of_the_village");
        p_298157_.put(33, "minecraft:darkness");
    });
    private static final Set<String> MOB_EFFECT_INSTANCE_CARRIER_ITEMS = Set.of("minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow");

    public MobEffectIdFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    private static <T> Optional<Dynamic<T>> getAndConvertMobEffectId(Dynamic<T> pDynamic, String pKey) {
        return pDynamic.get(pKey).asNumber().result().map(p_298913_ -> ID_MAP.get(p_298913_.intValue())).map(pDynamic::createString);
    }

    private static <T> Dynamic<T> updateMobEffectIdField(Dynamic<T> pOldDynamic, String pOldName, Dynamic<T> pNewDynamic, String pNewName) {
        Optional<Dynamic<T>> optional = getAndConvertMobEffectId(pOldDynamic, pOldName);
        return pNewDynamic.replaceField(pOldName, pNewName, optional);
    }

    private static <T> Dynamic<T> updateMobEffectIdField(Dynamic<T> pDynamic, String pOldName, String pNewName) {
        return updateMobEffectIdField(pDynamic, pOldName, pDynamic, pNewName);
    }

    private static <T> Dynamic<T> updateMobEffectInstance(Dynamic<T> pDynamic) {
        pDynamic = updateMobEffectIdField(pDynamic, "Id", "id");
        pDynamic = pDynamic.renameField("Ambient", "ambient");
        pDynamic = pDynamic.renameField("Amplifier", "amplifier");
        pDynamic = pDynamic.renameField("Duration", "duration");
        pDynamic = pDynamic.renameField("ShowParticles", "show_particles");
        pDynamic = pDynamic.renameField("ShowIcon", "show_icon");
        Optional<Dynamic<T>> optional = pDynamic.get("HiddenEffect").result().map(MobEffectIdFix::updateMobEffectInstance);
        return pDynamic.replaceField("HiddenEffect", "hidden_effect", optional);
    }

    private static <T> Dynamic<T> updateMobEffectInstanceList(Dynamic<T> pTag, String pOldName, String pNewName) {
        Optional<Dynamic<T>> optional = pTag.get(pOldName)
            .asStreamOpt()
            .result()
            .map(p_297707_ -> pTag.createList(p_297707_.map(MobEffectIdFix::updateMobEffectInstance)));
        return pTag.replaceField(pOldName, pNewName, optional);
    }

    private static <T> Dynamic<T> updateSuspiciousStewEntry(Dynamic<T> pOldDynamic, Dynamic<T> pNewDynamic) {
        pNewDynamic = updateMobEffectIdField(pOldDynamic, "EffectId", pNewDynamic, "id");
        Optional<Dynamic<T>> optional = pOldDynamic.get("EffectDuration").result();
        return pNewDynamic.replaceField("EffectDuration", "duration", optional);
    }

    private static <T> Dynamic<T> updateSuspiciousStewEntry(Dynamic<T> pSuspiciousStewEntry) {
        return updateSuspiciousStewEntry(pSuspiciousStewEntry, pSuspiciousStewEntry);
    }

    private Typed<?> updateNamedChoice(Typed<?> pTyped, TypeReference pReference, String pId, Function<Dynamic<?>, Dynamic<?>> pFixer) {
        Type<?> type = this.getInputSchema().getChoiceType(pReference, pId);
        Type<?> type1 = this.getOutputSchema().getChoiceType(pReference, pId);
        return pTyped.updateTyped(DSL.namedChoice(pId, type), type1, p_299360_ -> p_299360_.update(DSL.remainderFinder(), pFixer));
    }

    private TypeRewriteRule blockEntityFixer() {
        Type<?> type = this.getInputSchema().getType(References.BLOCK_ENTITY);
        return this.fixTypeEverywhereTyped(
            "BlockEntityMobEffectIdFix", type, p_297729_ -> this.updateNamedChoice(p_297729_, References.BLOCK_ENTITY, "minecraft:beacon", p_298165_ -> {
                    p_298165_ = updateMobEffectIdField(p_298165_, "Primary", "primary_effect");
                    return updateMobEffectIdField(p_298165_, "Secondary", "secondary_effect");
                })
        );
    }

    private static <T> Dynamic<T> fixMooshroomTag(Dynamic<T> pMooshroomTag) {
        Dynamic<T> dynamic = pMooshroomTag.emptyMap();
        Dynamic<T> dynamic1 = updateSuspiciousStewEntry(pMooshroomTag, dynamic);
        if (!dynamic1.equals(dynamic)) {
            pMooshroomTag = pMooshroomTag.set("stew_effects", pMooshroomTag.createList(Stream.of(dynamic1)));
        }

        return pMooshroomTag.remove("EffectId").remove("EffectDuration");
    }

    private static <T> Dynamic<T> fixArrowTag(Dynamic<T> pArrowTag) {
        return updateMobEffectInstanceList(pArrowTag, "CustomPotionEffects", "custom_potion_effects");
    }

    private static <T> Dynamic<T> fixAreaEffectCloudTag(Dynamic<T> pAreaEffectCloudTag) {
        return updateMobEffectInstanceList(pAreaEffectCloudTag, "Effects", "effects");
    }

    private static Dynamic<?> updateLivingEntityTag(Dynamic<?> pLivingEntityTag) {
        return updateMobEffectInstanceList(pLivingEntityTag, "ActiveEffects", "active_effects");
    }

    private TypeRewriteRule entityFixer() {
        Type<?> type = this.getInputSchema().getType(References.ENTITY);
        return this.fixTypeEverywhereTyped("EntityMobEffectIdFix", type, p_299717_ -> {
            p_299717_ = this.updateNamedChoice(p_299717_, References.ENTITY, "minecraft:mooshroom", MobEffectIdFix::fixMooshroomTag);
            p_299717_ = this.updateNamedChoice(p_299717_, References.ENTITY, "minecraft:arrow", MobEffectIdFix::fixArrowTag);
            p_299717_ = this.updateNamedChoice(p_299717_, References.ENTITY, "minecraft:area_effect_cloud", MobEffectIdFix::fixAreaEffectCloudTag);
            return p_299717_.update(DSL.remainderFinder(), MobEffectIdFix::updateLivingEntityTag);
        });
    }

    private TypeRewriteRule playerFixer() {
        Type<?> type = this.getInputSchema().getType(References.PLAYER);
        return this.fixTypeEverywhereTyped("PlayerMobEffectIdFix", type, p_297935_ -> p_297935_.update(DSL.remainderFinder(), MobEffectIdFix::updateLivingEntityTag));
    }

    private static <T> Dynamic<T> fixSuspiciousStewTag(Dynamic<T> pSuspiciousStewTag) {
        Optional<Dynamic<T>> optional = pSuspiciousStewTag.get("Effects")
            .asStreamOpt()
            .result()
            .map(p_299334_ -> pSuspiciousStewTag.createList(p_299334_.map(MobEffectIdFix::updateSuspiciousStewEntry)));
        return pSuspiciousStewTag.replaceField("Effects", "effects", optional);
    }

    private TypeRewriteRule itemStackFixer() {
        OpticFinder<Pair<String, String>> opticfinder = DSL.fieldFinder("id", DSL.named(References.ITEM_NAME.typeName(), NamespacedSchema.namespacedString()));
        Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder<?> opticfinder1 = type.findField("tag");
        return this.fixTypeEverywhereTyped(
            "ItemStackMobEffectIdFix",
            type,
            p_300076_ -> {
                Optional<Pair<String, String>> optional = p_300076_.getOptional(opticfinder);
                if (optional.isPresent()) {
                    String s = optional.get().getSecond();
                    if (s.equals("minecraft:suspicious_stew")) {
                        return p_300076_.updateTyped(opticfinder1, p_301412_ -> p_301412_.update(DSL.remainderFinder(), MobEffectIdFix::fixSuspiciousStewTag));
                    }

                    if (MOB_EFFECT_INSTANCE_CARRIER_ITEMS.contains(s)) {
                        return p_300076_.updateTyped(
                            opticfinder1,
                            p_299091_ -> p_299091_.update(
                                    DSL.remainderFinder(), p_300524_ -> updateMobEffectInstanceList(p_300524_, "CustomPotionEffects", "custom_potion_effects")
                                )
                        );
                    }
                }

                return p_300076_;
            }
        );
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return TypeRewriteRule.seq(this.blockEntityFixer(), this.entityFixer(), this.playerFixer(), this.itemStackFixer());
    }
}