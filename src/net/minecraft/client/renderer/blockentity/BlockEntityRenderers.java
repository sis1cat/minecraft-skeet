package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockEntityRenderers {
    private static final Map<BlockEntityType<?>, BlockEntityRendererProvider<?>> PROVIDERS = Maps.newHashMap();

    private static <T extends BlockEntity> void register(BlockEntityType<? extends T> pType, BlockEntityRendererProvider<T> pRenderProvider) {
        PROVIDERS.put(pType, pRenderProvider);
    }

    public static Map<BlockEntityType<?>, BlockEntityRenderer<?>> createEntityRenderers(BlockEntityRendererProvider.Context pContext) {
        Builder<BlockEntityType<?>, BlockEntityRenderer<?>> builder = ImmutableMap.builder();
        PROVIDERS.forEach((p_325522_, p_325523_) -> {
            try {
                builder.put((BlockEntityType<?>)p_325522_, p_325523_.create(pContext));
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to create model for " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey((BlockEntityType<?>)p_325522_), exception);
            }
        });
        return builder.build();
    }

    static {
        register(BlockEntityType.SIGN, SignRenderer::new);
        register(BlockEntityType.HANGING_SIGN, HangingSignRenderer::new);
        register(BlockEntityType.MOB_SPAWNER, SpawnerRenderer::new);
        register(BlockEntityType.PISTON, PistonHeadRenderer::new);
        register(BlockEntityType.CHEST, ChestRenderer::new);
        register(BlockEntityType.ENDER_CHEST, ChestRenderer::new);
        register(BlockEntityType.TRAPPED_CHEST, ChestRenderer::new);
        register(BlockEntityType.ENCHANTING_TABLE, EnchantTableRenderer::new);
        register(BlockEntityType.LECTERN, LecternRenderer::new);
        register(BlockEntityType.END_PORTAL, TheEndPortalRenderer::new);
        register(BlockEntityType.END_GATEWAY, TheEndGatewayRenderer::new);
        register(BlockEntityType.BEACON, BeaconRenderer::new);
        register(BlockEntityType.SKULL, SkullBlockRenderer::new);
        register(BlockEntityType.BANNER, BannerRenderer::new);
        register(BlockEntityType.STRUCTURE_BLOCK, StructureBlockRenderer::new);
        register(BlockEntityType.SHULKER_BOX, ShulkerBoxRenderer::new);
        register(BlockEntityType.BED, BedRenderer::new);
        register(BlockEntityType.CONDUIT, ConduitRenderer::new);
        register(BlockEntityType.BELL, BellRenderer::new);
        register(BlockEntityType.CAMPFIRE, CampfireRenderer::new);
        register(BlockEntityType.BRUSHABLE_BLOCK, BrushableBlockRenderer::new);
        register(BlockEntityType.DECORATED_POT, DecoratedPotRenderer::new);
        register(BlockEntityType.TRIAL_SPAWNER, TrialSpawnerRenderer::new);
        register(BlockEntityType.VAULT, VaultRenderer::new);
    }
}