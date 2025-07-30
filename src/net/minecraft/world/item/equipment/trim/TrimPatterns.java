package net.minecraft.world.item.equipment.trim;

import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TrimPatterns {
    public static final ResourceKey<TrimPattern> SENTRY = registryKey("sentry");
    public static final ResourceKey<TrimPattern> DUNE = registryKey("dune");
    public static final ResourceKey<TrimPattern> COAST = registryKey("coast");
    public static final ResourceKey<TrimPattern> WILD = registryKey("wild");
    public static final ResourceKey<TrimPattern> WARD = registryKey("ward");
    public static final ResourceKey<TrimPattern> EYE = registryKey("eye");
    public static final ResourceKey<TrimPattern> VEX = registryKey("vex");
    public static final ResourceKey<TrimPattern> TIDE = registryKey("tide");
    public static final ResourceKey<TrimPattern> SNOUT = registryKey("snout");
    public static final ResourceKey<TrimPattern> RIB = registryKey("rib");
    public static final ResourceKey<TrimPattern> SPIRE = registryKey("spire");
    public static final ResourceKey<TrimPattern> WAYFINDER = registryKey("wayfinder");
    public static final ResourceKey<TrimPattern> SHAPER = registryKey("shaper");
    public static final ResourceKey<TrimPattern> SILENCE = registryKey("silence");
    public static final ResourceKey<TrimPattern> RAISER = registryKey("raiser");
    public static final ResourceKey<TrimPattern> HOST = registryKey("host");
    public static final ResourceKey<TrimPattern> FLOW = registryKey("flow");
    public static final ResourceKey<TrimPattern> BOLT = registryKey("bolt");

    public static void bootstrap(BootstrapContext<TrimPattern> pContext) {
        register(pContext, Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, SENTRY);
        register(pContext, Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, DUNE);
        register(pContext, Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, COAST);
        register(pContext, Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, WILD);
        register(pContext, Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, WARD);
        register(pContext, Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, EYE);
        register(pContext, Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, VEX);
        register(pContext, Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, TIDE);
        register(pContext, Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, SNOUT);
        register(pContext, Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, RIB);
        register(pContext, Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, SPIRE);
        register(pContext, Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, WAYFINDER);
        register(pContext, Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, SHAPER);
        register(pContext, Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, SILENCE);
        register(pContext, Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, RAISER);
        register(pContext, Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, HOST);
        register(pContext, Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, FLOW);
        register(pContext, Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, BOLT);
    }

    public static Optional<Holder.Reference<TrimPattern>> getFromTemplate(HolderLookup.Provider pRegistries, ItemStack pStack) {
        return pRegistries.lookupOrThrow(Registries.TRIM_PATTERN).listElements().filter(p_369646_ -> pStack.is(p_369646_.value().templateItem())).findFirst();
    }

    public static void register(BootstrapContext<TrimPattern> pContext, Item pItem, ResourceKey<TrimPattern> pKey) {
        TrimPattern trimpattern = new TrimPattern(
            pKey.location(),
            BuiltInRegistries.ITEM.wrapAsHolder(pItem),
            Component.translatable(Util.makeDescriptionId("trim_pattern", pKey.location())),
            false
        );
        pContext.register(pKey, trimpattern);
    }

    private static ResourceKey<TrimPattern> registryKey(String pName) {
        return ResourceKey.create(Registries.TRIM_PATTERN, ResourceLocation.withDefaultNamespace(pName));
    }
}