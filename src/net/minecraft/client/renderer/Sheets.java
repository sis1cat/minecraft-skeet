package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Sheets {
    public static final ResourceLocation SHULKER_SHEET = ResourceLocation.withDefaultNamespace("textures/atlas/shulker_boxes.png");
    public static final ResourceLocation BED_SHEET = ResourceLocation.withDefaultNamespace("textures/atlas/beds.png");
    public static final ResourceLocation BANNER_SHEET = ResourceLocation.withDefaultNamespace("textures/atlas/banner_patterns.png");
    public static final ResourceLocation SHIELD_SHEET = ResourceLocation.withDefaultNamespace("textures/atlas/shield_patterns.png");
    public static final ResourceLocation SIGN_SHEET = ResourceLocation.withDefaultNamespace("textures/atlas/signs.png");
    public static final ResourceLocation CHEST_SHEET = ResourceLocation.withDefaultNamespace("textures/atlas/chest.png");
    public static final ResourceLocation ARMOR_TRIMS_SHEET = ResourceLocation.withDefaultNamespace("textures/atlas/armor_trims.png");
    public static final ResourceLocation DECORATED_POT_SHEET = ResourceLocation.withDefaultNamespace("textures/atlas/decorated_pot.png");
    private static final RenderType SHULKER_BOX_SHEET_TYPE = RenderType.entityCutoutNoCull(SHULKER_SHEET);
    private static final RenderType BED_SHEET_TYPE = RenderType.entitySolid(BED_SHEET);
    private static final RenderType BANNER_SHEET_TYPE = RenderType.entityNoOutline(BANNER_SHEET);
    private static final RenderType SHIELD_SHEET_TYPE = RenderType.entityNoOutline(SHIELD_SHEET);
    private static final RenderType SIGN_SHEET_TYPE = RenderType.entityCutoutNoCull(SIGN_SHEET);
    private static final RenderType CHEST_SHEET_TYPE = RenderType.entityCutout(CHEST_SHEET);
    private static final RenderType ARMOR_TRIMS_SHEET_TYPE = RenderType.armorCutoutNoCull(ARMOR_TRIMS_SHEET);
    private static final RenderType ARMOR_TRIMS_DECAL_SHEET_TYPE = RenderType.createArmorDecalCutoutNoCull(ARMOR_TRIMS_SHEET);
    private static final RenderType SOLID_BLOCK_SHEET = RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS);
    private static final RenderType CUTOUT_BLOCK_SHEET = RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS);
    private static final RenderType TRANSLUCENT_ITEM_CULL_BLOCK_SHEET = RenderType.itemEntityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);
    public static final Material DEFAULT_SHULKER_TEXTURE_LOCATION = createShulkerMaterial(ResourceLocation.withDefaultNamespace("shulker"));
    public static final List<Material> SHULKER_TEXTURE_LOCATION = Arrays.stream(DyeColor.values())
        .sorted(Comparator.comparingInt(DyeColor::getId))
        .map(Sheets::createShulkerMaterial)
        .collect(ImmutableList.toImmutableList());
    public static final Map<WoodType, Material> SIGN_MATERIALS = WoodType.values().collect(Collectors.toMap(Function.identity(), Sheets::createSignMaterial));
    public static final Map<WoodType, Material> HANGING_SIGN_MATERIALS = WoodType.values().collect(Collectors.toMap(Function.identity(), Sheets::createHangingSignMaterial));
    public static final Material BANNER_BASE = new Material(BANNER_SHEET, ResourceLocation.withDefaultNamespace("entity/banner/base"));
    public static final Material SHIELD_BASE = new Material(SHIELD_SHEET, ResourceLocation.withDefaultNamespace("entity/shield/base"));
    private static final Map<ResourceLocation, Material> BANNER_MATERIALS = new HashMap<>();
    private static final Map<ResourceLocation, Material> SHIELD_MATERIALS = new HashMap<>();
    public static final Map<ResourceKey<DecoratedPotPattern>, Material> DECORATED_POT_MATERIALS = BuiltInRegistries.DECORATED_POT_PATTERN
        .listElements()
        .collect(Collectors.toMap(Holder.Reference::key, p_340905_ -> createDecoratedPotMaterial(p_340905_.value().assetId())));
    public static final Material DECORATED_POT_BASE = createDecoratedPotMaterial(ResourceLocation.withDefaultNamespace("decorated_pot_base"));
    public static final Material DECORATED_POT_SIDE = createDecoratedPotMaterial(ResourceLocation.withDefaultNamespace("decorated_pot_side"));
    private static final Material[] BED_TEXTURES = Arrays.stream(DyeColor.values())
        .sorted(Comparator.comparingInt(DyeColor::getId))
        .map(Sheets::createBedMaterial)
        .toArray(Material[]::new);
    public static final Material CHEST_TRAP_LOCATION = chestMaterial("trapped");
    public static final Material CHEST_TRAP_LOCATION_LEFT = chestMaterial("trapped_left");
    public static final Material CHEST_TRAP_LOCATION_RIGHT = chestMaterial("trapped_right");
    public static final Material CHEST_XMAS_LOCATION = chestMaterial("christmas");
    public static final Material CHEST_XMAS_LOCATION_LEFT = chestMaterial("christmas_left");
    public static final Material CHEST_XMAS_LOCATION_RIGHT = chestMaterial("christmas_right");
    public static final Material CHEST_LOCATION = chestMaterial("normal");
    public static final Material CHEST_LOCATION_LEFT = chestMaterial("normal_left");
    public static final Material CHEST_LOCATION_RIGHT = chestMaterial("normal_right");
    public static final Material ENDER_CHEST_LOCATION = chestMaterial("ender");

    public static RenderType bannerSheet() {
        return BANNER_SHEET_TYPE;
    }

    public static RenderType shieldSheet() {
        return SHIELD_SHEET_TYPE;
    }

    public static RenderType bedSheet() {
        return BED_SHEET_TYPE;
    }

    public static RenderType shulkerBoxSheet() {
        return SHULKER_BOX_SHEET_TYPE;
    }

    public static RenderType signSheet() {
        return SIGN_SHEET_TYPE;
    }

    public static RenderType hangingSignSheet() {
        return SIGN_SHEET_TYPE;
    }

    public static RenderType chestSheet() {
        return CHEST_SHEET_TYPE;
    }

    public static RenderType armorTrimsSheet(boolean pDecal) {
        return pDecal ? ARMOR_TRIMS_DECAL_SHEET_TYPE : ARMOR_TRIMS_SHEET_TYPE;
    }

    public static RenderType solidBlockSheet() {
        return SOLID_BLOCK_SHEET;
    }

    public static RenderType cutoutBlockSheet() {
        return CUTOUT_BLOCK_SHEET;
    }

    public static RenderType translucentItemSheet() {
        return TRANSLUCENT_ITEM_CULL_BLOCK_SHEET;
    }

    public static Material getBedMaterial(DyeColor pColor) {
        return BED_TEXTURES[pColor.getId()];
    }

    public static ResourceLocation colorToResourceMaterial(DyeColor pColor) {
        return ResourceLocation.withDefaultNamespace(pColor.getName());
    }

    public static Material createBedMaterial(DyeColor pColor) {
        return createBedMaterial(colorToResourceMaterial(pColor));
    }

    public static Material createBedMaterial(ResourceLocation pAssetId) {
        return new Material(BED_SHEET, pAssetId.withPrefix("entity/bed/"));
    }

    public static Material getShulkerBoxMaterial(DyeColor pColor) {
        return SHULKER_TEXTURE_LOCATION.get(pColor.getId());
    }

    public static ResourceLocation colorToShulkerMaterial(DyeColor pColor) {
        return ResourceLocation.withDefaultNamespace("shulker_" + pColor.getName());
    }

    public static Material createShulkerMaterial(DyeColor pColor) {
        return createShulkerMaterial(colorToShulkerMaterial(pColor));
    }

    public static Material createShulkerMaterial(ResourceLocation pAssetId) {
        return new Material(SHULKER_SHEET, pAssetId.withPrefix("entity/shulker/"));
    }

    private static Material createSignMaterial(WoodType pWoodType) {
        return createSignMaterial(ResourceLocation.withDefaultNamespace(pWoodType.name()));
    }

    public static Material createSignMaterial(ResourceLocation pAssetId) {
        return new Material(SIGN_SHEET, pAssetId.withPrefix("entity/signs/"));
    }

    private static Material createHangingSignMaterial(WoodType pWoodType) {
        return createHangingSignMaterial(ResourceLocation.withDefaultNamespace(pWoodType.name()));
    }

    public static Material createHangingSignMaterial(ResourceLocation pAssetId) {
        return new Material(SIGN_SHEET, pAssetId.withPrefix("entity/signs/hanging/"));
    }

    public static Material getSignMaterial(WoodType pWoodType) {
        return SIGN_MATERIALS.get(pWoodType);
    }

    public static Material getHangingSignMaterial(WoodType pWoodType) {
        return HANGING_SIGN_MATERIALS.get(pWoodType);
    }

    public static Material getBannerMaterial(Holder<BannerPattern> pPattern) {
        return BANNER_MATERIALS.computeIfAbsent(pPattern.value().assetId(), p_325514_ -> {
            ResourceLocation resourcelocation = p_325514_.withPrefix("entity/banner/");
            return new Material(BANNER_SHEET, resourcelocation);
        });
    }

    public static Material getShieldMaterial(Holder<BannerPattern> pPattern) {
        return SHIELD_MATERIALS.computeIfAbsent(pPattern.value().assetId(), p_325515_ -> {
            ResourceLocation resourcelocation = p_325515_.withPrefix("entity/shield/");
            return new Material(SHIELD_SHEET, resourcelocation);
        });
    }

    private static Material chestMaterial(String pChestName) {
        return new Material(CHEST_SHEET, ResourceLocation.withDefaultNamespace("entity/chest/" + pChestName));
    }

    public static Material chestMaterial(ResourceLocation pAssetId) {
        return new Material(CHEST_SHEET, pAssetId.withPrefix("entity/chest/"));
    }

    private static Material createDecoratedPotMaterial(ResourceLocation pAssetId) {
        return new Material(DECORATED_POT_SHEET, pAssetId.withPrefix("entity/decorated_pot/"));
    }

    @Nullable
    public static Material getDecoratedPotMaterial(@Nullable ResourceKey<DecoratedPotPattern> pKey) {
        return pKey == null ? null : DECORATED_POT_MATERIALS.get(pKey);
    }

    public static Material chooseMaterial(BlockEntity pBlockEntity, ChestType pChestType, boolean pHoliday) {
        if (pBlockEntity instanceof EnderChestBlockEntity) {
            return ENDER_CHEST_LOCATION;
        } else if (pHoliday) {
            return chooseMaterial(pChestType, CHEST_XMAS_LOCATION, CHEST_XMAS_LOCATION_LEFT, CHEST_XMAS_LOCATION_RIGHT);
        } else {
            return pBlockEntity instanceof TrappedChestBlockEntity
                ? chooseMaterial(pChestType, CHEST_TRAP_LOCATION, CHEST_TRAP_LOCATION_LEFT, CHEST_TRAP_LOCATION_RIGHT)
                : chooseMaterial(pChestType, CHEST_LOCATION, CHEST_LOCATION_LEFT, CHEST_LOCATION_RIGHT);
        }
    }

    private static Material chooseMaterial(ChestType pChestType, Material pDoubleMaterial, Material pLeftMaterial, Material pRightMaterial) {
        switch (pChestType) {
            case LEFT:
                return pLeftMaterial;
            case RIGHT:
                return pRightMaterial;
            case SINGLE:
            default:
                return pDoubleMaterial;
        }
    }
}