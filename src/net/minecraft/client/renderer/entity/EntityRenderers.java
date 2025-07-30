package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.optifine.player.PlayerItemsLayer;
import org.slf4j.Logger;

public class EntityRenderers {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<EntityType<?>, EntityRendererProvider<?>> PROVIDERS = new Object2ObjectOpenHashMap<>();
    private static final Map<PlayerSkin.Model, EntityRendererProvider<AbstractClientPlayer>> PLAYER_PROVIDERS = Map.of(PlayerSkin.Model.WIDE, contextIn -> {
        PlayerRenderer playerrenderer = new PlayerRenderer(contextIn, false);
        playerrenderer.addLayer(new PlayerItemsLayer(playerrenderer));
        return playerrenderer;
    }, PlayerSkin.Model.SLIM, context2In -> {
        PlayerRenderer playerrenderer = new PlayerRenderer(context2In, true);
        playerrenderer.addLayer(new PlayerItemsLayer(playerrenderer));
        return playerrenderer;
    });

    private static <T extends Entity> void register(EntityType<? extends T> pEntityType, EntityRendererProvider<T> pProvider) {
        PROVIDERS.put(pEntityType, pProvider);
    }

    public static Map<EntityType<?>, EntityRenderer<?, ?>> createEntityRenderers(EntityRendererProvider.Context pContext) {
        Builder<EntityType<?>, EntityRenderer<?, ?>> builder = ImmutableMap.builder();
        PROVIDERS.forEach((typeIn, providerIn) -> {
            try {
                builder.put((EntityType<?>)typeIn, providerIn.create(pContext));
            } catch (Exception exception) {
                throw new IllegalArgumentException("Failed to create model for " + BuiltInRegistries.ENTITY_TYPE.getKey((EntityType<?>)typeIn), exception);
            }
        });
        return builder.build();
    }

    public static Map<PlayerSkin.Model, EntityRenderer<? extends Player, ?>> createPlayerRenderers(EntityRendererProvider.Context pContext) {
        Builder<PlayerSkin.Model, EntityRenderer<? extends Player, ?>> builder = ImmutableMap.builder();
        PLAYER_PROVIDERS.forEach((modelIn, providerIn) -> {
            try {
                builder.put(modelIn, providerIn.create(pContext));
            } catch (Exception exception) {
                throw new IllegalArgumentException("Failed to create player model for " + modelIn, exception);
            }
        });
        return builder.build();
    }

    public static boolean validateRegistrations() {
        boolean flag = true;

        for (EntityType<?> entitytype : BuiltInRegistries.ENTITY_TYPE) {
            if (entitytype != EntityType.PLAYER && !PROVIDERS.containsKey(entitytype)) {
                LOGGER.warn("No renderer registered for {}", BuiltInRegistries.ENTITY_TYPE.getKey(entitytype));
                flag = false;
            }
        }

        return !flag;
    }

    static {
        register(EntityType.ALLAY, AllayRenderer::new);
        register(EntityType.AREA_EFFECT_CLOUD, NoopRenderer::new);
        register(EntityType.ARMADILLO, ArmadilloRenderer::new);
        register(EntityType.ARMOR_STAND, ArmorStandRenderer::new);
        register(EntityType.ARROW, TippableArrowRenderer::new);
        register(EntityType.AXOLOTL, AxolotlRenderer::new);
        register(EntityType.BAT, BatRenderer::new);
        register(EntityType.BEE, BeeRenderer::new);
        register(EntityType.BLAZE, BlazeRenderer::new);
        register(EntityType.BLOCK_DISPLAY, DisplayRenderer.BlockDisplayRenderer::new);
        register(EntityType.OAK_BOAT, contextIn -> new BoatRenderer(contextIn, ModelLayers.OAK_BOAT));
        register(EntityType.SPRUCE_BOAT, p_349106_0_ -> new BoatRenderer(p_349106_0_, ModelLayers.SPRUCE_BOAT));
        register(EntityType.BIRCH_BOAT, p_349112_0_ -> new BoatRenderer(p_349112_0_, ModelLayers.BIRCH_BOAT));
        register(EntityType.JUNGLE_BOAT, p_349107_0_ -> new BoatRenderer(p_349107_0_, ModelLayers.JUNGLE_BOAT));
        register(EntityType.ACACIA_BOAT, p_349103_0_ -> new BoatRenderer(p_349103_0_, ModelLayers.ACACIA_BOAT));
        register(EntityType.CHERRY_BOAT, p_349115_0_ -> new BoatRenderer(p_349115_0_, ModelLayers.CHERRY_BOAT));
        register(EntityType.DARK_OAK_BOAT, p_349091_0_ -> new BoatRenderer(p_349091_0_, ModelLayers.DARK_OAK_BOAT));
        register(EntityType.PALE_OAK_BOAT, p_367244_0_ -> new BoatRenderer(p_367244_0_, ModelLayers.PALE_OAK_BOAT));
        register(EntityType.MANGROVE_BOAT, p_349104_0_ -> new BoatRenderer(p_349104_0_, ModelLayers.MANGROVE_BOAT));
        register(EntityType.BAMBOO_RAFT, p_349116_0_ -> new RaftRenderer(p_349116_0_, ModelLayers.BAMBOO_RAFT));
        register(EntityType.BOGGED, BoggedRenderer::new);
        register(EntityType.BREEZE, BreezeRenderer::new);
        register(EntityType.BREEZE_WIND_CHARGE, WindChargeRenderer::new);
        register(EntityType.CAT, CatRenderer::new);
        register(EntityType.CAMEL, CamelRenderer::new);
        register(EntityType.CAVE_SPIDER, CaveSpiderRenderer::new);
        register(EntityType.OAK_CHEST_BOAT, p_349101_0_ -> new BoatRenderer(p_349101_0_, ModelLayers.OAK_CHEST_BOAT));
        register(EntityType.SPRUCE_CHEST_BOAT, p_349093_0_ -> new BoatRenderer(p_349093_0_, ModelLayers.SPRUCE_CHEST_BOAT));
        register(EntityType.BIRCH_CHEST_BOAT, p_349105_0_ -> new BoatRenderer(p_349105_0_, ModelLayers.BIRCH_CHEST_BOAT));
        register(EntityType.JUNGLE_CHEST_BOAT, p_349102_0_ -> new BoatRenderer(p_349102_0_, ModelLayers.JUNGLE_CHEST_BOAT));
        register(EntityType.ACACIA_CHEST_BOAT, p_349108_0_ -> new BoatRenderer(p_349108_0_, ModelLayers.ACACIA_CHEST_BOAT));
        register(EntityType.CHERRY_CHEST_BOAT, p_349114_0_ -> new BoatRenderer(p_349114_0_, ModelLayers.CHERRY_CHEST_BOAT));
        register(EntityType.DARK_OAK_CHEST_BOAT, p_349094_0_ -> new BoatRenderer(p_349094_0_, ModelLayers.DARK_OAK_CHEST_BOAT));
        register(EntityType.PALE_OAK_CHEST_BOAT, p_367245_0_ -> new BoatRenderer(p_367245_0_, ModelLayers.PALE_OAK_CHEST_BOAT));
        register(EntityType.MANGROVE_CHEST_BOAT, p_349095_0_ -> new BoatRenderer(p_349095_0_, ModelLayers.MANGROVE_CHEST_BOAT));
        register(EntityType.BAMBOO_CHEST_RAFT, p_349099_0_ -> new RaftRenderer(p_349099_0_, ModelLayers.BAMBOO_CHEST_RAFT));
        register(EntityType.CHEST_MINECART, p_174089_0_ -> new MinecartRenderer(p_174089_0_, ModelLayers.CHEST_MINECART));
        register(EntityType.CHICKEN, ChickenRenderer::new);
        register(EntityType.COD, CodRenderer::new);
        register(EntityType.COMMAND_BLOCK_MINECART, p_174087_0_ -> new MinecartRenderer(p_174087_0_, ModelLayers.COMMAND_BLOCK_MINECART));
        register(EntityType.COW, CowRenderer::new);
        register(EntityType.CREAKING, CreakingRenderer::new);
        register(EntityType.CREEPER, CreeperRenderer::new);
        register(EntityType.DOLPHIN, DolphinRenderer::new);
        register(EntityType.DONKEY, p_372555_0_ -> new DonkeyRenderer<>(p_372555_0_, ModelLayers.DONKEY, ModelLayers.DONKEY_BABY, false));
        register(EntityType.DRAGON_FIREBALL, DragonFireballRenderer::new);
        register(EntityType.DROWNED, DrownedRenderer::new);
        register(EntityType.EGG, ThrownItemRenderer::new);
        register(EntityType.ELDER_GUARDIAN, ElderGuardianRenderer::new);
        register(EntityType.ENDERMAN, EndermanRenderer::new);
        register(EntityType.ENDERMITE, EndermiteRenderer::new);
        register(EntityType.ENDER_DRAGON, EnderDragonRenderer::new);
        register(EntityType.ENDER_PEARL, ThrownItemRenderer::new);
        register(EntityType.END_CRYSTAL, EndCrystalRenderer::new);
        register(EntityType.EVOKER, EvokerRenderer::new);
        register(EntityType.EVOKER_FANGS, EvokerFangsRenderer::new);
        register(EntityType.EXPERIENCE_BOTTLE, ThrownItemRenderer::new);
        register(EntityType.EXPERIENCE_ORB, ExperienceOrbRenderer::new);
        register(EntityType.EYE_OF_ENDER, p_174083_0_ -> new ThrownItemRenderer<>(p_174083_0_, 1.0F, true));
        register(EntityType.FALLING_BLOCK, FallingBlockRenderer::new);
        register(EntityType.FIREBALL, p_174059_0_ -> new ThrownItemRenderer<>(p_174059_0_, 3.0F, true));
        register(EntityType.FIREWORK_ROCKET, FireworkEntityRenderer::new);
        register(EntityType.FISHING_BOBBER, FishingHookRenderer::new);
        register(EntityType.FOX, FoxRenderer::new);
        register(EntityType.FROG, FrogRenderer::new);
        register(EntityType.FURNACE_MINECART, p_174079_0_ -> new MinecartRenderer(p_174079_0_, ModelLayers.FURNACE_MINECART));
        register(EntityType.GHAST, GhastRenderer::new);
        register(EntityType.GIANT, p_174077_0_ -> new GiantMobRenderer(p_174077_0_, 6.0F));
        register(EntityType.GLOW_ITEM_FRAME, ItemFrameRenderer::new);
        register(
            EntityType.GLOW_SQUID,
            p_349117_0_ -> new GlowSquidRenderer(
                    p_349117_0_, new SquidModel(p_349117_0_.bakeLayer(ModelLayers.GLOW_SQUID)), new SquidModel(p_349117_0_.bakeLayer(ModelLayers.GLOW_SQUID_BABY))
                )
        );
        register(EntityType.GOAT, GoatRenderer::new);
        register(EntityType.GUARDIAN, GuardianRenderer::new);
        register(EntityType.HOGLIN, HoglinRenderer::new);
        register(EntityType.HOPPER_MINECART, p_174073_0_ -> new MinecartRenderer(p_174073_0_, ModelLayers.HOPPER_MINECART));
        register(EntityType.HORSE, HorseRenderer::new);
        register(EntityType.HUSK, HuskRenderer::new);
        register(EntityType.ILLUSIONER, IllusionerRenderer::new);
        register(EntityType.INTERACTION, NoopRenderer::new);
        register(EntityType.IRON_GOLEM, IronGolemRenderer::new);
        register(EntityType.ITEM, ItemEntityRenderer::new);
        register(EntityType.ITEM_DISPLAY, DisplayRenderer.ItemDisplayRenderer::new);
        register(EntityType.ITEM_FRAME, ItemFrameRenderer::new);
        register(EntityType.OMINOUS_ITEM_SPAWNER, OminousItemSpawnerRenderer::new);
        register(EntityType.LEASH_KNOT, LeashKnotRenderer::new);
        register(EntityType.LIGHTNING_BOLT, LightningBoltRenderer::new);
        register(EntityType.LLAMA, p_349118_0_ -> new LlamaRenderer(p_349118_0_, ModelLayers.LLAMA, ModelLayers.LLAMA_BABY));
        register(EntityType.LLAMA_SPIT, LlamaSpitRenderer::new);
        register(EntityType.MAGMA_CUBE, MagmaCubeRenderer::new);
        register(EntityType.MARKER, NoopRenderer::new);
        register(EntityType.MINECART, p_174069_0_ -> new MinecartRenderer(p_174069_0_, ModelLayers.MINECART));
        register(EntityType.MOOSHROOM, MushroomCowRenderer::new);
        register(EntityType.MULE, p_372556_0_ -> new DonkeyRenderer<>(p_372556_0_, ModelLayers.MULE, ModelLayers.MULE_BABY, true));
        register(EntityType.OCELOT, OcelotRenderer::new);
        register(EntityType.PAINTING, PaintingRenderer::new);
        register(EntityType.PANDA, PandaRenderer::new);
        register(EntityType.PARROT, ParrotRenderer::new);
        register(EntityType.PHANTOM, PhantomRenderer::new);
        register(EntityType.PIG, PigRenderer::new);
        register(
            EntityType.PIGLIN,
            p_349096_0_ -> new PiglinRenderer(
                    p_349096_0_,
                    ModelLayers.PIGLIN,
                    ModelLayers.PIGLIN_BABY,
                    ModelLayers.PIGLIN_INNER_ARMOR,
                    ModelLayers.PIGLIN_OUTER_ARMOR,
                    ModelLayers.PIGLIN_BABY_INNER_ARMOR,
                    ModelLayers.PIGLIN_BABY_OUTER_ARMOR
                )
        );
        register(
            EntityType.PIGLIN_BRUTE,
            p_349097_0_ -> new PiglinRenderer(
                    p_349097_0_,
                    ModelLayers.PIGLIN_BRUTE,
                    ModelLayers.PIGLIN_BRUTE,
                    ModelLayers.PIGLIN_BRUTE_INNER_ARMOR,
                    ModelLayers.PIGLIN_BRUTE_OUTER_ARMOR,
                    ModelLayers.PIGLIN_BRUTE_INNER_ARMOR,
                    ModelLayers.PIGLIN_BRUTE_OUTER_ARMOR
                )
        );
        register(EntityType.PILLAGER, PillagerRenderer::new);
        register(EntityType.POLAR_BEAR, PolarBearRenderer::new);
        register(EntityType.POTION, ThrownItemRenderer::new);
        register(EntityType.PUFFERFISH, PufferfishRenderer::new);
        register(EntityType.RABBIT, RabbitRenderer::new);
        register(EntityType.RAVAGER, RavagerRenderer::new);
        register(EntityType.SALMON, SalmonRenderer::new);
        register(EntityType.SHEEP, SheepRenderer::new);
        register(EntityType.SHULKER, ShulkerRenderer::new);
        register(EntityType.SHULKER_BULLET, ShulkerBulletRenderer::new);
        register(EntityType.SILVERFISH, SilverfishRenderer::new);
        register(EntityType.SKELETON, SkeletonRenderer::new);
        register(EntityType.SKELETON_HORSE, p_349092_0_ -> new UndeadHorseRenderer(p_349092_0_, ModelLayers.SKELETON_HORSE, ModelLayers.SKELETON_HORSE_BABY, true));
        register(EntityType.SLIME, SlimeRenderer::new);
        register(EntityType.SMALL_FIREBALL, p_174081_0_ -> new ThrownItemRenderer<>(p_174081_0_, 0.75F, true));
        register(EntityType.SNIFFER, SnifferRenderer::new);
        register(EntityType.SNOWBALL, ThrownItemRenderer::new);
        register(EntityType.SNOW_GOLEM, SnowGolemRenderer::new);
        register(EntityType.SPAWNER_MINECART, p_174057_0_ -> new MinecartRenderer(p_174057_0_, ModelLayers.SPAWNER_MINECART));
        register(EntityType.SPECTRAL_ARROW, SpectralArrowRenderer::new);
        register(EntityType.SPIDER, SpiderRenderer::new);
        register(
            EntityType.SQUID,
            p_349110_0_ -> new SquidRenderer<>(
                    p_349110_0_, new SquidModel(p_349110_0_.bakeLayer(ModelLayers.SQUID)), new SquidModel(p_349110_0_.bakeLayer(ModelLayers.SQUID_BABY))
                )
        );
        register(EntityType.STRAY, StrayRenderer::new);
        register(EntityType.STRIDER, StriderRenderer::new);
        register(EntityType.TADPOLE, TadpoleRenderer::new);
        register(EntityType.TEXT_DISPLAY, DisplayRenderer.TextDisplayRenderer::new);
        register(EntityType.TNT, TntRenderer::new);
        register(EntityType.TNT_MINECART, TntMinecartRenderer::new);
        register(EntityType.TRADER_LLAMA, p_349113_0_ -> new LlamaRenderer(p_349113_0_, ModelLayers.TRADER_LLAMA, ModelLayers.TRADER_LLAMA_BABY));
        register(EntityType.TRIDENT, ThrownTridentRenderer::new);
        register(EntityType.TROPICAL_FISH, TropicalFishRenderer::new);
        register(EntityType.TURTLE, TurtleRenderer::new);
        register(EntityType.VEX, VexRenderer::new);
        register(EntityType.VILLAGER, VillagerRenderer::new);
        register(EntityType.VINDICATOR, VindicatorRenderer::new);
        register(EntityType.WARDEN, WardenRenderer::new);
        register(EntityType.WANDERING_TRADER, WanderingTraderRenderer::new);
        register(EntityType.WIND_CHARGE, WindChargeRenderer::new);
        register(EntityType.WITCH, WitchRenderer::new);
        register(EntityType.WITHER, WitherBossRenderer::new);
        register(EntityType.WITHER_SKELETON, WitherSkeletonRenderer::new);
        register(EntityType.WITHER_SKULL, WitherSkullRenderer::new);
        register(EntityType.WOLF, WolfRenderer::new);
        register(EntityType.ZOGLIN, ZoglinRenderer::new);
        register(EntityType.ZOMBIE, ZombieRenderer::new);
        register(EntityType.ZOMBIE_HORSE, p_349100_0_ -> new UndeadHorseRenderer(p_349100_0_, ModelLayers.ZOMBIE_HORSE, ModelLayers.ZOMBIE_HORSE_BABY, false));
        register(EntityType.ZOMBIE_VILLAGER, ZombieVillagerRenderer::new);
        register(
            EntityType.ZOMBIFIED_PIGLIN,
            p_349111_0_ -> new ZombifiedPiglinRenderer(
                    p_349111_0_,
                    ModelLayers.ZOMBIFIED_PIGLIN,
                    ModelLayers.ZOMBIFIED_PIGLIN_BABY,
                    ModelLayers.ZOMBIFIED_PIGLIN_INNER_ARMOR,
                    ModelLayers.ZOMBIFIED_PIGLIN_OUTER_ARMOR,
                    ModelLayers.ZOMBIFIED_PIGLIN_BABY_INNER_ARMOR,
                    ModelLayers.ZOMBIFIED_PIGLIN_BABY_OUTER_ARMOR
                )
        );
    }
}