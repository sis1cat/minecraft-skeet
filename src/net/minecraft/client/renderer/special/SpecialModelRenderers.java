package net.minecraft.client.renderer.special;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpecialModelRenderers {
    private static final ExtraCodecs.LateBoundIdMapper<ResourceLocation, MapCodec<? extends SpecialModelRenderer.Unbaked>> ID_MAPPER = new ExtraCodecs.LateBoundIdMapper<>();
    public static final Codec<SpecialModelRenderer.Unbaked> CODEC = ID_MAPPER.codec(ResourceLocation.CODEC)
        .dispatch(SpecialModelRenderer.Unbaked::type, p_377423_ -> p_377423_);
    private static final Map<Block, SpecialModelRenderer.Unbaked> STATIC_BLOCK_MAPPING = ImmutableMap.<Block, SpecialModelRenderer.Unbaked>builder()
        .put(Blocks.SKELETON_SKULL, new SkullSpecialRenderer.Unbaked(SkullBlock.Types.SKELETON))
        .put(Blocks.ZOMBIE_HEAD, new SkullSpecialRenderer.Unbaked(SkullBlock.Types.ZOMBIE))
        .put(Blocks.CREEPER_HEAD, new SkullSpecialRenderer.Unbaked(SkullBlock.Types.CREEPER))
        .put(Blocks.DRAGON_HEAD, new SkullSpecialRenderer.Unbaked(SkullBlock.Types.DRAGON))
        .put(Blocks.PIGLIN_HEAD, new SkullSpecialRenderer.Unbaked(SkullBlock.Types.PIGLIN))
        .put(Blocks.PLAYER_HEAD, new SkullSpecialRenderer.Unbaked(SkullBlock.Types.PLAYER))
        .put(Blocks.WITHER_SKELETON_SKULL, new SkullSpecialRenderer.Unbaked(SkullBlock.Types.WITHER_SKELETON))
        .put(Blocks.SKELETON_WALL_SKULL, new SkullSpecialRenderer.Unbaked(SkullBlock.Types.SKELETON))
        .put(Blocks.ZOMBIE_WALL_HEAD, new SkullSpecialRenderer.Unbaked(SkullBlock.Types.ZOMBIE))
        .put(Blocks.CREEPER_WALL_HEAD, new SkullSpecialRenderer.Unbaked(SkullBlock.Types.CREEPER))
        .put(Blocks.DRAGON_WALL_HEAD, new SkullSpecialRenderer.Unbaked(SkullBlock.Types.DRAGON))
        .put(Blocks.PIGLIN_WALL_HEAD, new SkullSpecialRenderer.Unbaked(SkullBlock.Types.PIGLIN))
        .put(Blocks.PLAYER_WALL_HEAD, new SkullSpecialRenderer.Unbaked(SkullBlock.Types.PLAYER))
        .put(Blocks.WITHER_SKELETON_WALL_SKULL, new SkullSpecialRenderer.Unbaked(SkullBlock.Types.WITHER_SKELETON))
        .put(Blocks.WHITE_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.WHITE))
        .put(Blocks.ORANGE_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.ORANGE))
        .put(Blocks.MAGENTA_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.MAGENTA))
        .put(Blocks.LIGHT_BLUE_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.LIGHT_BLUE))
        .put(Blocks.YELLOW_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.YELLOW))
        .put(Blocks.LIME_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.LIME))
        .put(Blocks.PINK_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.PINK))
        .put(Blocks.GRAY_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.GRAY))
        .put(Blocks.LIGHT_GRAY_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.LIGHT_GRAY))
        .put(Blocks.CYAN_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.CYAN))
        .put(Blocks.PURPLE_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.PURPLE))
        .put(Blocks.BLUE_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.BLUE))
        .put(Blocks.BROWN_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.BROWN))
        .put(Blocks.GREEN_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.GREEN))
        .put(Blocks.RED_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.RED))
        .put(Blocks.BLACK_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.BLACK))
        .put(Blocks.WHITE_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.WHITE))
        .put(Blocks.ORANGE_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.ORANGE))
        .put(Blocks.MAGENTA_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.MAGENTA))
        .put(Blocks.LIGHT_BLUE_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.LIGHT_BLUE))
        .put(Blocks.YELLOW_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.YELLOW))
        .put(Blocks.LIME_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.LIME))
        .put(Blocks.PINK_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.PINK))
        .put(Blocks.GRAY_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.GRAY))
        .put(Blocks.LIGHT_GRAY_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.LIGHT_GRAY))
        .put(Blocks.CYAN_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.CYAN))
        .put(Blocks.PURPLE_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.PURPLE))
        .put(Blocks.BLUE_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.BLUE))
        .put(Blocks.BROWN_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.BROWN))
        .put(Blocks.GREEN_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.GREEN))
        .put(Blocks.RED_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.RED))
        .put(Blocks.BLACK_WALL_BANNER, new BannerSpecialRenderer.Unbaked(DyeColor.BLACK))
        .put(Blocks.WHITE_BED, new BedSpecialRenderer.Unbaked(DyeColor.WHITE))
        .put(Blocks.ORANGE_BED, new BedSpecialRenderer.Unbaked(DyeColor.ORANGE))
        .put(Blocks.MAGENTA_BED, new BedSpecialRenderer.Unbaked(DyeColor.MAGENTA))
        .put(Blocks.LIGHT_BLUE_BED, new BedSpecialRenderer.Unbaked(DyeColor.LIGHT_BLUE))
        .put(Blocks.YELLOW_BED, new BedSpecialRenderer.Unbaked(DyeColor.YELLOW))
        .put(Blocks.LIME_BED, new BedSpecialRenderer.Unbaked(DyeColor.LIME))
        .put(Blocks.PINK_BED, new BedSpecialRenderer.Unbaked(DyeColor.PINK))
        .put(Blocks.GRAY_BED, new BedSpecialRenderer.Unbaked(DyeColor.GRAY))
        .put(Blocks.LIGHT_GRAY_BED, new BedSpecialRenderer.Unbaked(DyeColor.LIGHT_GRAY))
        .put(Blocks.CYAN_BED, new BedSpecialRenderer.Unbaked(DyeColor.CYAN))
        .put(Blocks.PURPLE_BED, new BedSpecialRenderer.Unbaked(DyeColor.PURPLE))
        .put(Blocks.BLUE_BED, new BedSpecialRenderer.Unbaked(DyeColor.BLUE))
        .put(Blocks.BROWN_BED, new BedSpecialRenderer.Unbaked(DyeColor.BROWN))
        .put(Blocks.GREEN_BED, new BedSpecialRenderer.Unbaked(DyeColor.GREEN))
        .put(Blocks.RED_BED, new BedSpecialRenderer.Unbaked(DyeColor.RED))
        .put(Blocks.BLACK_BED, new BedSpecialRenderer.Unbaked(DyeColor.BLACK))
        .put(Blocks.SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked())
        .put(Blocks.WHITE_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.WHITE))
        .put(Blocks.ORANGE_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.ORANGE))
        .put(Blocks.MAGENTA_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.MAGENTA))
        .put(Blocks.LIGHT_BLUE_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.LIGHT_BLUE))
        .put(Blocks.YELLOW_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.YELLOW))
        .put(Blocks.LIME_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.LIME))
        .put(Blocks.PINK_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.PINK))
        .put(Blocks.GRAY_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.GRAY))
        .put(Blocks.LIGHT_GRAY_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.LIGHT_GRAY))
        .put(Blocks.CYAN_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.CYAN))
        .put(Blocks.PURPLE_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.PURPLE))
        .put(Blocks.BLUE_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.BLUE))
        .put(Blocks.BROWN_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.BROWN))
        .put(Blocks.GREEN_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.GREEN))
        .put(Blocks.RED_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.RED))
        .put(Blocks.BLACK_SHULKER_BOX, new ShulkerBoxSpecialRenderer.Unbaked(DyeColor.BLACK))
        .put(Blocks.OAK_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.OAK))
        .put(Blocks.SPRUCE_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.SPRUCE))
        .put(Blocks.BIRCH_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.BIRCH))
        .put(Blocks.ACACIA_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.ACACIA))
        .put(Blocks.CHERRY_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.CHERRY))
        .put(Blocks.JUNGLE_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.JUNGLE))
        .put(Blocks.DARK_OAK_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.DARK_OAK))
        .put(Blocks.PALE_OAK_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.PALE_OAK))
        .put(Blocks.MANGROVE_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.MANGROVE))
        .put(Blocks.BAMBOO_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.BAMBOO))
        .put(Blocks.CRIMSON_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.CRIMSON))
        .put(Blocks.WARPED_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.WARPED))
        .put(Blocks.OAK_WALL_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.OAK))
        .put(Blocks.SPRUCE_WALL_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.SPRUCE))
        .put(Blocks.BIRCH_WALL_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.BIRCH))
        .put(Blocks.ACACIA_WALL_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.ACACIA))
        .put(Blocks.CHERRY_WALL_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.CHERRY))
        .put(Blocks.JUNGLE_WALL_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.JUNGLE))
        .put(Blocks.DARK_OAK_WALL_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.DARK_OAK))
        .put(Blocks.PALE_OAK_WALL_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.PALE_OAK))
        .put(Blocks.MANGROVE_WALL_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.MANGROVE))
        .put(Blocks.BAMBOO_WALL_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.BAMBOO))
        .put(Blocks.CRIMSON_WALL_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.CRIMSON))
        .put(Blocks.WARPED_WALL_SIGN, new StandingSignSpecialRenderer.Unbaked(WoodType.WARPED))
        .put(Blocks.OAK_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.OAK))
        .put(Blocks.SPRUCE_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.SPRUCE))
        .put(Blocks.BIRCH_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.BIRCH))
        .put(Blocks.ACACIA_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.ACACIA))
        .put(Blocks.CHERRY_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.CHERRY))
        .put(Blocks.JUNGLE_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.JUNGLE))
        .put(Blocks.DARK_OAK_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.DARK_OAK))
        .put(Blocks.PALE_OAK_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.PALE_OAK))
        .put(Blocks.MANGROVE_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.MANGROVE))
        .put(Blocks.BAMBOO_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.BAMBOO))
        .put(Blocks.CRIMSON_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.CRIMSON))
        .put(Blocks.WARPED_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.WARPED))
        .put(Blocks.OAK_WALL_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.OAK))
        .put(Blocks.SPRUCE_WALL_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.SPRUCE))
        .put(Blocks.BIRCH_WALL_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.BIRCH))
        .put(Blocks.ACACIA_WALL_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.ACACIA))
        .put(Blocks.CHERRY_WALL_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.CHERRY))
        .put(Blocks.JUNGLE_WALL_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.JUNGLE))
        .put(Blocks.DARK_OAK_WALL_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.DARK_OAK))
        .put(Blocks.PALE_OAK_WALL_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.PALE_OAK))
        .put(Blocks.MANGROVE_WALL_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.MANGROVE))
        .put(Blocks.BAMBOO_WALL_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.BAMBOO))
        .put(Blocks.CRIMSON_WALL_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.CRIMSON))
        .put(Blocks.WARPED_WALL_HANGING_SIGN, new HangingSignSpecialRenderer.Unbaked(WoodType.WARPED))
        .put(Blocks.CONDUIT, new ConduitSpecialRenderer.Unbaked())
        .put(Blocks.CHEST, new ChestSpecialRenderer.Unbaked(ChestSpecialRenderer.NORMAL_CHEST_TEXTURE))
        .put(Blocks.TRAPPED_CHEST, new ChestSpecialRenderer.Unbaked(ChestSpecialRenderer.TRAPPED_CHEST_TEXTURE))
        .put(Blocks.ENDER_CHEST, new ChestSpecialRenderer.Unbaked(ChestSpecialRenderer.ENDER_CHEST_TEXTURE))
        .put(Blocks.DECORATED_POT, new DecoratedPotSpecialRenderer.Unbaked())
        .build();
    private static final ChestSpecialRenderer.Unbaked GIFT_CHEST = new ChestSpecialRenderer.Unbaked(ChestSpecialRenderer.GIFT_CHEST_TEXTURE);

    public static void bootstrap() {
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("bed"), BedSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("banner"), BannerSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("conduit"), ConduitSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("chest"), ChestSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("head"), SkullSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("shulker_box"), ShulkerBoxSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("shield"), ShieldSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("trident"), TridentSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("decorated_pot"), DecoratedPotSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("standing_sign"), StandingSignSpecialRenderer.Unbaked.MAP_CODEC);
        ID_MAPPER.put(ResourceLocation.withDefaultNamespace("hanging_sign"), HangingSignSpecialRenderer.Unbaked.MAP_CODEC);
    }

    public static Map<Block, SpecialModelRenderer<?>> createBlockRenderers(EntityModelSet pModelSet) {
        Map<Block, SpecialModelRenderer.Unbaked> map = new HashMap<>(STATIC_BLOCK_MAPPING);
        if (ChestRenderer.xmasTextures()) {
            map.put(Blocks.CHEST, GIFT_CHEST);
            map.put(Blocks.TRAPPED_CHEST, GIFT_CHEST);
        }

        Builder<Block, SpecialModelRenderer<?>> builder = ImmutableMap.builder();
        map.forEach((p_377014_, p_376072_) -> {
            SpecialModelRenderer<?> specialmodelrenderer = p_376072_.bake(pModelSet);
            if (specialmodelrenderer != null) {
                builder.put(p_377014_, specialmodelrenderer);
            }
        });
        return builder.build();
    }
}