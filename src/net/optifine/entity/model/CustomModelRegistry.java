package net.optifine.entity.model;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.optifine.Config;
import net.optifine.util.Either;

public class CustomModelRegistry {
    private static Map<String, ModelAdapter> mapModelAdapters = makeMapModelAdapters();

    private static Map<String, ModelAdapter> makeMapModelAdapters() {
        Map<String, ModelAdapter> map = new LinkedHashMap<>();
        addModelAdapter(map, new ModelAdapterAllay());
        addModelAdapter(map, new ModelAdapterArmadillo());
        addModelAdapter(map, new ModelAdapterArmorStand());
        addModelAdapter(map, new ModelAdapterArrow());
        addModelAdapter(map, new ModelAdapterAxolotl());
        addModelAdapter(map, new ModelAdapterBat());
        addModelAdapter(map, new ModelAdapterBee());
        addModelAdapter(map, new ModelAdapterBlaze());
        addModelAdapter(map, new ModelAdapterBogged());
        addModelAdapter(map, new ModelAdapterBreeze());
        addModelAdapter(map, new ModelAdapterBreezeWindCharge());
        addModelAdapter(map, new ModelAdapterCamel());
        addModelAdapter(map, new ModelAdapterCat());
        addModelAdapter(map, new ModelAdapterCaveSpider());
        addModelAdapter(map, new ModelAdapterChicken());
        addModelAdapter(map, new ModelAdapterCod());
        addModelAdapter(map, new ModelAdapterCow());
        addModelAdapter(map, new ModelAdapterCreaking());
        addModelAdapter(map, new ModelAdapterCreeper());
        addModelAdapter(map, new ModelAdapterDragon());
        addModelAdapter(map, new ModelAdapterDonkey());
        addModelAdapter(map, new ModelAdapterDolphin());
        addModelAdapter(map, new ModelAdapterDrowned());
        addModelAdapter(map, new ModelAdapterElderGuardian());
        addModelAdapter(map, new ModelAdapterEnderCrystal());
        addModelAdapter(map, new ModelAdapterEnderman());
        addModelAdapter(map, new ModelAdapterEndermite());
        addModelAdapter(map, new ModelAdapterEvoker());
        addModelAdapter(map, new ModelAdapterEvokerFangs());
        addModelAdapter(map, new ModelAdapterFox());
        addModelAdapter(map, new ModelAdapterFrog());
        addModelAdapter(map, new ModelAdapterGhast());
        addModelAdapter(map, new ModelAdapterGiant());
        addModelAdapter(map, new ModelAdapterGlowSquid());
        addModelAdapter(map, new ModelAdapterGoat());
        addModelAdapter(map, new ModelAdapterGuardian());
        addModelAdapter(map, new ModelAdapterHoglin());
        addModelAdapter(map, new ModelAdapterHorse());
        addModelAdapter(map, new ModelAdapterHusk());
        addModelAdapter(map, new ModelAdapterIllusioner());
        addModelAdapter(map, new ModelAdapterIronGolem());
        addModelAdapter(map, new ModelAdapterLeadKnot());
        addModelAdapter(map, new ModelAdapterLlama());
        addModelAdapter(map, new ModelAdapterLlamaSpit());
        addModelAdapter(map, new ModelAdapterMagmaCube());
        addModelAdapter(map, new ModelAdapterMinecart());
        addModelAdapter(map, new ModelAdapterMinecartChest());
        addModelAdapter(map, new ModelAdapterMinecartCommandBlock());
        addModelAdapter(map, new ModelAdapterMinecartFurnace());
        addModelAdapter(map, new ModelAdapterMinecartHopper());
        addModelAdapter(map, new ModelAdapterMinecartTnt());
        addModelAdapter(map, new ModelAdapterMinecartMobSpawner());
        addModelAdapter(map, new ModelAdapterMooshroom());
        addModelAdapter(map, new ModelAdapterMule());
        addModelAdapter(map, new ModelAdapterOcelot());
        addModelAdapter(map, new ModelAdapterPanda());
        addModelAdapter(map, new ModelAdapterParrot());
        addModelAdapter(map, new ModelAdapterPhantom());
        addModelAdapter(map, new ModelAdapterPig());
        addModelAdapter(map, new ModelAdapterPiglin());
        addModelAdapter(map, new ModelAdapterPiglinBrute());
        addModelAdapter(map, new ModelAdapterPolarBear());
        addModelAdapter(map, new ModelAdapterPillager());
        addModelAdapter(map, new ModelAdapterPufferFishBig());
        addModelAdapter(map, new ModelAdapterPufferFishMedium());
        addModelAdapter(map, new ModelAdapterPufferFishSmall());
        addModelAdapter(map, new ModelAdapterRabbit());
        addModelAdapter(map, new ModelAdapterRavager());
        addModelAdapter(map, new ModelAdapterSalmon());
        addModelAdapter(map, new ModelAdapterSalmonSmall());
        addModelAdapter(map, new ModelAdapterSalmonLarge());
        addModelAdapter(map, new ModelAdapterSheep());
        addModelAdapter(map, new ModelAdapterShulker());
        addModelAdapter(map, new ModelAdapterShulkerBullet());
        addModelAdapter(map, new ModelAdapterSilverfish());
        addModelAdapter(map, new ModelAdapterSkeleton());
        addModelAdapter(map, new ModelAdapterSkeletonHorse());
        addModelAdapter(map, new ModelAdapterSlime());
        addModelAdapter(map, new ModelAdapterSnowman());
        addModelAdapter(map, new ModelAdapterSniffer());
        addModelAdapter(map, new ModelAdapterSpectralArrow());
        addModelAdapter(map, new ModelAdapterSpider());
        addModelAdapter(map, new ModelAdapterSquid());
        addModelAdapter(map, new ModelAdapterStray());
        addModelAdapter(map, new ModelAdapterStrider());
        addModelAdapter(map, new ModelAdapterTadpole());
        addModelAdapter(map, new ModelAdapterTraderLlama());
        addModelAdapter(map, new ModelAdapterTrident());
        addModelAdapter(map, new ModelAdapterTropicalFishA());
        addModelAdapter(map, new ModelAdapterTropicalFishB());
        addModelAdapter(map, new ModelAdapterTurtle());
        addModelAdapter(map, new ModelAdapterVex());
        addModelAdapter(map, new ModelAdapterVillager());
        addModelAdapter(map, new ModelAdapterVindicator());
        addModelAdapter(map, new ModelAdapterWanderingTrader());
        addModelAdapter(map, new ModelAdapterWarden());
        addModelAdapter(map, new ModelAdapterWindCharge());
        addModelAdapter(map, new ModelAdapterWitch());
        addModelAdapter(map, new ModelAdapterWither());
        addModelAdapter(map, new ModelAdapterWitherSkeleton());
        addModelAdapter(map, new ModelAdapterWitherSkull());
        addModelAdapter(map, new ModelAdapterWolf());
        addModelAdapter(map, new ModelAdapterZoglin());
        addModelAdapter(map, new ModelAdapterZombie());
        addModelAdapter(map, new ModelAdapterZombieHorse());
        addModelAdapter(map, new ModelAdapterZombieVillager());
        addModelAdapter(map, new ModelAdapterZombifiedPiglin());

        for (BoatType boattype : BoatType.values()) {
            addModelAdapter(map, new ModelAdapterBoat(boattype.getEntityType(), boattype.getName(), boattype.getModelLayer()));
        }

        for (BoatType boattype1 : BoatType.values()) {
            addModelAdapter(map, new ModelAdapterBoatPatch(boattype1.getEntityType(), boattype1.getName(), boattype1.getModelLayer()));
        }

        for (ChestBoatType chestboattype : ChestBoatType.values()) {
            addModelAdapter(map, new ModelAdapterChestBoat(chestboattype.getEntityType(), chestboattype.getName(), chestboattype.getModelLayer()));
        }

        for (ChestBoatType chestboattype1 : ChestBoatType.values()) {
            addModelAdapter(map, new ModelAdapterChestBoatPatch(chestboattype1.getEntityType(), chestboattype1.getName(), chestboattype1.getModelLayer()));
        }

        addModelAdapter(map, new ModelAdapterRaft());
        addModelAdapter(map, new ModelAdapterChestRaft());
        addModelAdapter(map, new ModelAdapterBoggedOuter());
        addModelAdapter(map, new ModelAdapterBreezeWind());
        addModelAdapter(map, new ModelAdapterCatCollar());
        addModelAdapter(map, new ModelAdapterCreeperCharge());
        addModelAdapter(map, new ModelAdapterDrownedOuter());
        addModelAdapter(map, new ModelAdapterHorseArmor());
        addModelAdapter(map, new ModelAdapterLlamaDecor());
        addModelAdapter(map, new ModelAdapterPigSaddle());
        addModelAdapter(map, new ModelAdapterSheepWool());
        addModelAdapter(map, new ModelAdapterSlimeOuter());
        addModelAdapter(map, new ModelAdapterStrayOuter());
        addModelAdapter(map, new ModelAdapterStriderSaddle());
        addModelAdapter(map, new ModelAdapterTraderLlamaDecor());
        addModelAdapter(map, new ModelAdapterTropicalFishPatternA());
        addModelAdapter(map, new ModelAdapterTropicalFishPatternB());
        addModelAdapter(map, new ModelAdapterWitherArmor());
        addModelAdapter(map, new ModelAdapterWolfArmor());
        addModelAdapter(map, new ModelAdapterWolfCollar());
        addModelAdapter(map, new ModelAdapterBannerBase(true));
        addModelAdapter(map, new ModelAdapterBannerBase(false));
        addModelAdapter(map, new ModelAdapterBannerFlag(true));
        addModelAdapter(map, new ModelAdapterBannerFlag(false));
        addModelAdapter(map, new ModelAdapterBed());
        addModelAdapter(map, new ModelAdapterBell());
        addModelAdapter(map, new ModelAdapterBook());
        addModelAdapter(map, new ModelAdapterBookLectern());
        addModelAdapter(map, new ModelAdapterChest());
        addModelAdapter(map, new ModelAdapterChestDoubleLeft());
        addModelAdapter(map, new ModelAdapterChestDoubleRight());
        addModelAdapter(map, new ModelAdapterConduit());
        addModelAdapter(map, new ModelAdapterDecoratedPot());
        addModelAdapter(map, new ModelAdapterEnderChest());
        addModelAdapter(map, new ModelAdapterHeadCreeper());
        addModelAdapter(map, new ModelAdapterHeadDragon());
        addModelAdapter(map, new ModelAdapterHeadPiglin());
        addModelAdapter(map, new ModelAdapterHeadPlayer());
        addModelAdapter(map, new ModelAdapterHeadSkeleton());
        addModelAdapter(map, new ModelAdapterHeadWitherSkeleton());
        addModelAdapter(map, new ModelAdapterHeadZombie());
        addModelAdapter(map, new ModelAdapterShulkerBox());
        addModelAdapter(map, new ModelAdapterTrappedChest());
        addModelAdapter(map, new ModelAdapterTrappedChestLeft());
        addModelAdapter(map, new ModelAdapterTrappedChestRight());

        for (WoodType woodtype : WoodType.values().toList()) {
            addModelAdapter(map, new ModelAdapterSign(woodtype, true));
        }

        for (WoodType woodtype1 : WoodType.values().toList()) {
            addModelAdapter(map, new ModelAdapterSign(woodtype1, false));
        }

        for (WoodType woodtype2 : WoodType.values().toList()) {
            for (HangingSignRenderer.AttachmentType hangingsignrenderer$attachmenttype : HangingSignRenderer.AttachmentType.values()) {
                addModelAdapter(map, new ModelAdapterHangingSign(woodtype2, hangingsignrenderer$attachmenttype));
            }
        }

        addModelAdapter(map, new ModelAdapterBookScreen());
        addModelAdapter(map, new ModelAdapterParrotShoulder());
        addModelAdapter(map, new ModelAdapterShield());
        addModelAdapter(map, new ModelAdapterTridentHand());
        return map;
    }

    private static void addModelAdapter(Map<String, ModelAdapter> map, ModelAdapter modelAdapter) {
        try {
            addModelAdapterSingle(map, modelAdapter);
            if (modelAdapter instanceof IModelAdapterAgeable imodeladapterageable) {
                ModelAdapter modeladapter = imodeladapterageable.makeBaby();
                if (modeladapter != null) {
                    addModelAdapterSingle(map, modeladapter);
                }
            }
        } catch (Exception exception) {
            Config.warn("Error registering CEM model: " + modelAdapter.getName() + ", " + exception.getClass().getName() + ": " + exception.getMessage());
        }
    }

    private static void addModelAdapterSingle(Map<String, ModelAdapter> map, ModelAdapter modelAdapter) {
        addModelAdapter(map, modelAdapter, modelAdapter.getName());
        Model model = modelAdapter.makeModel();
        String[] astring = modelAdapter.getModelRendererNames();
        Set<ModelPart> set = new HashSet<>();

        for (int i = 0; i < astring.length; i++) {
            String s = astring[i];
            ModelPart modelpart = modelAdapter.getModelRenderer(model, s);
            if (modelpart == null) {
                Config.warn("Model renderer not found, model: " + modelAdapter.getName() + ", name: " + s);
            } else if (set.contains(modelpart)) {
                Config.warn("Model renderer duplicate, model: " + modelAdapter.getName() + ", name: " + s);
            } else {
                set.add(modelpart);
            }
        }
    }

    private static void addModelAdapter(Map<String, ModelAdapter> map, ModelAdapter modelAdapter, String name) {
        if (map.containsKey(name)) {
            String s = "?";
            Either<EntityType, BlockEntityType> either = modelAdapter.getType();
            if (either == null) {
                s = "";
            } else if (either.getLeft().isPresent()) {
                s = either.getLeft().get().getDescriptionId();
            } else if (either.getRight().isPresent()) {
                s = BlockEntityType.getKey(either.getRight().get()) + "";
            }

            Config.warn("Model adapter already registered for id: " + name + ", type: " + s);
        }

        map.put(name, modelAdapter);
    }

    public static ModelAdapter getModelAdapter(String name) {
        return mapModelAdapters.get(name);
    }

    public static String[] getModelNames() {
        Set<String> set = mapModelAdapters.keySet();
        return set.toArray(new String[set.size()]);
    }

    public static String[] getAliases(String name) {
        ModelAdapter modeladapter = getModelAdapter(name);
        return modeladapter == null ? null : modeladapter.getAliases();
    }
}