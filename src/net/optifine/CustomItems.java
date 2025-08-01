package net.optifine;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.optifine.config.NbtTagValue;
import net.optifine.shaders.Shaders;
import net.optifine.util.EnchantmentUtils;
import net.optifine.util.ItemUtils;
import net.optifine.util.PotionUtils;
import net.optifine.util.PropertiesOrdered;
import net.optifine.util.ResUtils;
import net.optifine.util.StrUtils;

public class CustomItems {
    private static CustomItemProperties[][] itemProperties = null;
    private static CustomItemProperties[][] enchantmentProperties = null;
    private static Map mapPotionIds = null;
    private static ItemModelGenerator itemModelGenerator = new ItemModelGenerator();
    private static boolean useGlint = true;
    private static boolean renderOffHand = false;
    private static AtomicBoolean modelSpritesUpdated = new AtomicBoolean(false);
    public static final int MASK_POTION_SPLASH = 16384;
    public static final int MASK_POTION_NAME = 63;
    public static final int MASK_POTION_EXTENDED = 64;
    public static final String KEY_TEXTURE_OVERLAY = "texture.potion_overlay";
    public static final String KEY_TEXTURE_SPLASH = "texture.potion_bottle_splash";
    public static final String KEY_TEXTURE_DRINKABLE = "texture.potion_bottle_drinkable";
    public static final String DEFAULT_TEXTURE_OVERLAY = "item/potion_overlay";
    public static final String DEFAULT_TEXTURE_SPLASH = "item/potion_bottle_splash";
    public static final String DEFAULT_TEXTURE_DRINKABLE = "item/potion_bottle_drinkable";
    private static final int[][] EMPTY_INT2_ARRAY = new int[0][];
    private static final Map<String, Integer> mapPotionDamages = makeMapPotionDamages();
    private static final String TYPE_POTION_NORMAL = "normal";
    private static final String TYPE_POTION_SPLASH = "splash";
    private static final String TYPE_POTION_LINGER = "linger";

    public static void update() {
        itemProperties = null;
        enchantmentProperties = null;
        useGlint = true;
        modelSpritesUpdated.set(false);
        if (Config.isCustomItems()) {
            readCitProperties("optifine/cit.properties");
            PackResources[] apackresources = Config.getResourcePacks();

            for (int i = apackresources.length - 1; i >= 0; i--) {
                PackResources packresources = apackresources[i];
                update(packresources);
            }

            update(Config.getDefaultResourcePack());
            if (itemProperties.length <= 0) {
                itemProperties = null;
            }

            if (enchantmentProperties.length <= 0) {
                enchantmentProperties = null;
            }
        }
    }

    private static void readCitProperties(String fileName) {
        try {
            ResourceLocation resourcelocation = new ResourceLocation(fileName);
            InputStream inputstream = Config.getResourceStream(resourcelocation);
            if (inputstream == null) {
                return;
            }

            Config.dbg("CustomItems: Loading " + fileName);
            Properties properties = new PropertiesOrdered();
            properties.load(inputstream);
            inputstream.close();
            useGlint = Config.parseBoolean(properties.getProperty("useGlint"), true);
        } catch (FileNotFoundException filenotfoundexception) {
            return;
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }
    }

    private static void update(PackResources rp) {
        String[] astring = ResUtils.collectFiles(rp, "optifine/cit/", ".properties", null);
        Map<String, CustomItemProperties> map = makeAutoImageProperties(rp);
        if (map.size() > 0) {
            Set<String> set = map.keySet();
            String[] astring1 = set.toArray(new String[set.size()]);
            astring = (String[])Config.addObjectsToArray(astring, astring1);
        }

        Arrays.sort((Object[])astring);
        List<List<CustomItemProperties>> list = makePropertyList(itemProperties);
        List<List<CustomItemProperties>> list1 = makePropertyList(enchantmentProperties);

        for (int i = 0; i < astring.length; i++) {
            String s = astring[i];
            Config.dbg("CustomItems: " + s);

            try {
                CustomItemProperties customitemproperties = null;
                if (map.containsKey(s)) {
                    customitemproperties = map.get(s);
                }

                if (customitemproperties == null) {
                    ResourceLocation resourcelocation = new ResourceLocation(s);
                    InputStream inputstream = Config.getResourceStream(rp, PackType.CLIENT_RESOURCES, resourcelocation);
                    if (inputstream == null) {
                        Config.warn("CustomItems file not found: " + s);
                        continue;
                    }

                    Properties properties = new PropertiesOrdered();
                    properties.load(inputstream);
                    inputstream.close();
                    customitemproperties = new CustomItemProperties(properties, s);
                }

                if (customitemproperties.isValid(s)) {
                    addToItemList(customitemproperties, list);
                    addToEnchantmentList(customitemproperties, list1);
                }
            } catch (FileNotFoundException filenotfoundexception) {
                Config.warn("CustomItems file not found: " + s);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        itemProperties = propertyListToArray(list);
        enchantmentProperties = propertyListToArray(list1);
        Comparator comparator = getPropertiesComparator();

        for (int j = 0; j < itemProperties.length; j++) {
            CustomItemProperties[] acustomitemproperties = itemProperties[j];
            if (acustomitemproperties != null) {
                Arrays.sort(acustomitemproperties, comparator);
            }
        }

        for (int k = 0; k < enchantmentProperties.length; k++) {
            CustomItemProperties[] acustomitemproperties1 = enchantmentProperties[k];
            if (acustomitemproperties1 != null) {
                Arrays.sort(acustomitemproperties1, comparator);
            }
        }
    }

    private static Comparator getPropertiesComparator() {
        return new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                CustomItemProperties customitemproperties = (CustomItemProperties)o1;
                CustomItemProperties customitemproperties1 = (CustomItemProperties)o2;
                if (customitemproperties.layer != customitemproperties1.layer) {
                    return customitemproperties.layer - customitemproperties1.layer;
                } else if (customitemproperties.weight != customitemproperties1.weight) {
                    return customitemproperties1.weight - customitemproperties.weight;
                } else {
                    return !customitemproperties.basePath.equals(customitemproperties1.basePath)
                        ? customitemproperties.basePath.compareTo(customitemproperties1.basePath)
                        : customitemproperties.name.compareTo(customitemproperties1.name);
                }
            }
        };
    }

    public static void registerIcons(TextureAtlas textureMap) {
        if (Config.isCustomItems()) {
            for (int i = 0; !modelSpritesUpdated.get(); Config.sleep(100L)) {
                if (++i % 50 == 0) {
                    Config.dbg("Waiting for model sprites");
                }
            }

            Config.dbg("CustomItems: Registering sprites");

            for (CustomItemProperties customitemproperties : getAllProperties()) {
                customitemproperties.registerIcons(textureMap);
            }
        }
    }

    public static void updateIcons(TextureAtlas textureMap) {
        for (CustomItemProperties customitemproperties : getAllProperties()) {
            customitemproperties.updateIcons(textureMap);
        }
    }

    public static void registerModels(Map<ResourceLocation, Resource> mapModelsIn) {
        registerModels(mapModelsIn, true);
    }

    private static void registerModels(Map<ResourceLocation, Resource> mapModelsIn, boolean checkParents) {
        for (CustomItemProperties customitemproperties : getAllProperties()) {
            customitemproperties.registerModels(mapModelsIn, checkParents);
        }
    }

    public static Map<ResourceLocation, Resource> getModelResources(boolean checkParents) {
        Map<ResourceLocation, Resource> map = new HashMap<>();
        registerModels(map, checkParents);
        return map;
    }

    public static void collectModelSprites(Map<ResourceLocation, UnbakedModel> mapModelsIn) {
        Config.dbg("CustomItems: Collecting model sprites");

        for (CustomItemProperties customitemproperties : getAllProperties()) {
            customitemproperties.collectModelSprites(mapModelsIn);
        }

        modelSpritesUpdated.set(true);
    }

    public static void updateModels() {
        for (CustomItemProperties customitemproperties : getAllProperties()) {
            if (customitemproperties.type == 1) {
                TextureAtlas textureatlas = Config.getTextureMap();
                customitemproperties.updateModelTexture(textureatlas, itemModelGenerator);
                customitemproperties.updateModelsFull();
            }
        }
    }

    private static List<CustomItemProperties> getAllProperties() {
        List<CustomItemProperties> list = new ArrayList<>();
        addAll(itemProperties, list);
        addAll(enchantmentProperties, list);
        return list;
    }

    private static void addAll(CustomItemProperties[][] cipsArr, List<CustomItemProperties> list) {
        if (cipsArr != null) {
            for (int i = 0; i < cipsArr.length; i++) {
                CustomItemProperties[] acustomitemproperties = cipsArr[i];
                if (acustomitemproperties != null) {
                    for (int j = 0; j < acustomitemproperties.length; j++) {
                        CustomItemProperties customitemproperties = acustomitemproperties[j];
                        if (customitemproperties != null) {
                            list.add(customitemproperties);
                        }
                    }
                }
            }
        }
    }

    private static Map<String, CustomItemProperties> makeAutoImageProperties(PackResources rp) {
        Map<String, CustomItemProperties> map = new HashMap<>();
        map.putAll(makePotionImageProperties(rp, "normal", BuiltInRegistries.ITEM.getKey(Items.POTION)));
        map.putAll(makePotionImageProperties(rp, "splash", BuiltInRegistries.ITEM.getKey(Items.SPLASH_POTION)));
        map.putAll(makePotionImageProperties(rp, "linger", BuiltInRegistries.ITEM.getKey(Items.LINGERING_POTION)));
        return map;
    }

    private static Map<String, CustomItemProperties> makePotionImageProperties(PackResources rp, String type, ResourceLocation itemId) {
        Map<String, CustomItemProperties> map = new HashMap<>();
        String s = type + "/";
        String[] astring = new String[]{"optifine/cit/potion/" + s, "optifine/cit/Potion/" + s};
        String[] astring1 = new String[]{".png"};
        String[] astring2 = ResUtils.collectFiles(rp, astring, astring1);

        for (int i = 0; i < astring2.length; i++) {
            String s1 = astring2[i];
            String name = StrUtils.removePrefixSuffix(s1, astring, astring1);
            Properties properties = makePotionProperties(name, type, itemId, s1);
            if (properties != null) {
                String s3 = StrUtils.removeSuffix(s1, astring1) + ".properties";
                CustomItemProperties customitemproperties = new CustomItemProperties(properties, s3);
                map.put(s3, customitemproperties);
            }
        }

        return map;
    }

    private static Properties makePotionProperties(String name, String type, ResourceLocation itemId, String path) {
        if (StrUtils.endsWith(name, new String[]{"_n", "_s"})) {
            return null;
        } else if (name.equals("empty") && type.equals("normal")) {
            itemId = BuiltInRegistries.ITEM.getKey(Items.GLASS_BOTTLE);
            Properties properties = new PropertiesOrdered();
            properties.put("type", "item");
            properties.put("items", itemId.toString());
            return properties;
        } else {
            int[] aint = (int[])getMapPotionIds().get(name);
            if (aint == null) {
                Config.warn("Potion not found for image: " + path);
                return null;
            } else {
                StringBuffer stringbuffer = new StringBuffer();

                for (int i = 0; i < aint.length; i++) {
                    int j = aint[i];
                    if (type.equals("splash")) {
                        j |= 16384;
                    }

                    if (i > 0) {
                        stringbuffer.append(" ");
                    }

                    stringbuffer.append(j);
                }

                int k = 16447;
                if (name.equals("water") || name.equals("mundane")) {
                    k |= 64;
                }

                Properties properties1 = new PropertiesOrdered();
                properties1.put("type", "item");
                properties1.put("items", itemId.toString());
                properties1.put("damage", stringbuffer.toString());
                properties1.put("damageMask", k + "");
                if (type.equals("splash")) {
                    properties1.put("texture.potion_bottle_splash", name);
                } else {
                    properties1.put("texture.potion_bottle_drinkable", name);
                }

                return properties1;
            }
        }
    }

    private static Map getMapPotionIds() {
        if (mapPotionIds == null) {
            mapPotionIds = new LinkedHashMap();
            mapPotionIds.put("water", getPotionId(0, 0));
            mapPotionIds.put("awkward", getPotionId(0, 1));
            mapPotionIds.put("thick", getPotionId(0, 2));
            mapPotionIds.put("potent", getPotionId(0, 3));
            mapPotionIds.put("regeneration", getPotionIds(1));
            mapPotionIds.put("movespeed", getPotionIds(2));
            mapPotionIds.put("fireresistance", getPotionIds(3));
            mapPotionIds.put("poison", getPotionIds(4));
            mapPotionIds.put("heal", getPotionIds(5));
            mapPotionIds.put("nightvision", getPotionIds(6));
            mapPotionIds.put("clear", getPotionId(7, 0));
            mapPotionIds.put("bungling", getPotionId(7, 1));
            mapPotionIds.put("charming", getPotionId(7, 2));
            mapPotionIds.put("rank", getPotionId(7, 3));
            mapPotionIds.put("weakness", getPotionIds(8));
            mapPotionIds.put("damageboost", getPotionIds(9));
            mapPotionIds.put("moveslowdown", getPotionIds(10));
            mapPotionIds.put("leaping", getPotionIds(11));
            mapPotionIds.put("harm", getPotionIds(12));
            mapPotionIds.put("waterbreathing", getPotionIds(13));
            mapPotionIds.put("invisibility", getPotionIds(14));
            mapPotionIds.put("thin", getPotionId(15, 0));
            mapPotionIds.put("debonair", getPotionId(15, 1));
            mapPotionIds.put("sparkling", getPotionId(15, 2));
            mapPotionIds.put("stinky", getPotionId(15, 3));
            mapPotionIds.put("mundane", getPotionId(0, 4));
            mapPotionIds.put("speed", mapPotionIds.get("movespeed"));
            mapPotionIds.put("fire_resistance", mapPotionIds.get("fireresistance"));
            mapPotionIds.put("instant_health", mapPotionIds.get("heal"));
            mapPotionIds.put("night_vision", mapPotionIds.get("nightvision"));
            mapPotionIds.put("strength", mapPotionIds.get("damageboost"));
            mapPotionIds.put("slowness", mapPotionIds.get("moveslowdown"));
            mapPotionIds.put("instant_damage", mapPotionIds.get("harm"));
            mapPotionIds.put("water_breathing", mapPotionIds.get("waterbreathing"));
        }

        return mapPotionIds;
    }

    private static int[] getPotionIds(int baseId) {
        return new int[]{baseId, baseId + 16, baseId + 32, baseId + 48};
    }

    private static int[] getPotionId(int baseId, int subId) {
        return new int[]{baseId + subId * 16};
    }

    private static int getPotionNameDamage(String name) {
        String s = "effect." + name;

        for (ResourceLocation resourcelocation : BuiltInRegistries.MOB_EFFECT.keySet()) {
            if (BuiltInRegistries.MOB_EFFECT.containsKey(resourcelocation)) {
                MobEffect mobeffect = BuiltInRegistries.MOB_EFFECT.getValue(resourcelocation);
                String s1 = mobeffect.getDescriptionId();
                if (s.equals(s1)) {
                    return PotionUtils.getId(mobeffect);
                }
            }
        }

        return -1;
    }

    private static List<List<CustomItemProperties>> makePropertyList(CustomItemProperties[][] propsArr) {
        List<List<CustomItemProperties>> list = new ArrayList<>();
        if (propsArr != null) {
            for (int i = 0; i < propsArr.length; i++) {
                CustomItemProperties[] acustomitemproperties = propsArr[i];
                List<CustomItemProperties> list1 = null;
                if (acustomitemproperties != null) {
                    list1 = new ArrayList<>(Arrays.asList(acustomitemproperties));
                }

                list.add(list1);
            }
        }

        return list;
    }

    private static CustomItemProperties[][] propertyListToArray(List list) {
        CustomItemProperties[][] acustomitemproperties = new CustomItemProperties[list.size()][];

        for (int i = 0; i < list.size(); i++) {
            List listx = (List)list.get(i);
            if (listx != null) {
                CustomItemProperties[] acustomitemproperties1 = (CustomItemProperties[]) listx.toArray(new CustomItemProperties[listx.size()]);
                Arrays.sort(acustomitemproperties1, new CustomItemsComparator());
                acustomitemproperties[i] = acustomitemproperties1;
            }
        }

        return acustomitemproperties;
    }

    private static void addToItemList(CustomItemProperties cp, List<List<CustomItemProperties>> itemList) {
        if (cp.items != null) {
            for (int i = 0; i < cp.items.length; i++) {
                int j = cp.items[i];
                if (j <= 0) {
                    Config.warn("Invalid item ID: " + j);
                } else {
                    addToList(cp, itemList, j);
                }
            }
        }
    }

    private static void addToEnchantmentList(CustomItemProperties cp, List<List<CustomItemProperties>> enchantmentList) {
        if (cp.type == 2) {
            if (cp.enchantmentIds != null) {
                int i = getMaxEnchantmentId() + 1;

                for (int j = 0; j < i; j++) {
                    if (Config.equalsOne(j, cp.enchantmentIds)) {
                        addToList(cp, enchantmentList, j);
                    }
                }
            }
        }
    }

    private static int getMaxEnchantmentId() {
        return EnchantmentUtils.getMaxEnchantmentId();
    }

    private static void addToList(CustomItemProperties cp, List<List<CustomItemProperties>> list, int id) {
        while (id >= list.size()) {
            list.add(null);
        }

        List<CustomItemProperties> listx = list.get(id);
        if (listx == null) {
            listx = new ArrayList<>();
            list.set(id, listx);
        }

        listx.add(cp);
    }

    public static BakedModel getCustomItemModel(ItemStack itemStack, BakedModel model, ResourceLocation modelLocation, boolean fullModel) {
        if (!fullModel && model.isGui3d()) {
            return model;
        } else if (itemProperties == null) {
            return model;
        } else {
            CustomItemProperties customitemproperties = getCustomItemProperties(itemStack, 1);
            if (customitemproperties == null) {
                return model;
            } else {
                BakedModel bakedmodel = customitemproperties.getBakedModel(modelLocation, fullModel);
                return bakedmodel != null ? bakedmodel : model;
            }
        }
    }

    public static ResourceLocation getCustomArmorTexture(
        ItemStack itemStack, EquipmentClientInfo.LayerType layerType, EquipmentClientInfo.Layer layer, ResourceLocation locArmor
    ) {
        if (itemProperties == null) {
            return locArmor;
        } else {
            int i = getArmorType(layerType);
            if (i == 0) {
                return locArmor;
            } else {
                CustomItemProperties customitemproperties = getCustomItemProperties(itemStack, i);
                if (customitemproperties == null) {
                    return locArmor;
                } else {
                    boolean flag = layerType == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS;
                    String s = layer.textureId().getPath().endsWith("_overlay") ? "_overlay" : "";
                    ResourceLocation resourcelocation = getCustomArmorLocation(customitemproperties, itemStack, flag, s);
                    return resourcelocation == null ? locArmor : resourcelocation;
                }
            }
        }
    }

    private static int getArmorType(EquipmentClientInfo.LayerType layerType) {
        switch (layerType) {
            case HUMANOID:
            case HUMANOID_LEGGINGS:
                return 3;
            default:
                return 0;
        }
    }

    private static ResourceLocation getCustomArmorLocation(CustomItemProperties props, ItemStack itemStack, boolean legSlot, String overlay) {
        if (props.mapTextureLocations == null) {
            return props.textureLocation;
        } else if (!(itemStack.getItem() instanceof ArmorItem armoritem)) {
            return null;
        } else {
            String s = armoritem.getArmorMaterial().assetId().location().getPath();
            int i = legSlot ? 2 : 1;
            StringBuffer stringbuffer = new StringBuffer();
            stringbuffer.append("texture.");
            stringbuffer.append(s);
            stringbuffer.append("_layer_");
            stringbuffer.append(i);
            stringbuffer.append(overlay);
            String s1 = stringbuffer.toString();
            ResourceLocation resourcelocation = (ResourceLocation)props.mapTextureLocations.get(s1);
            return resourcelocation == null ? props.textureLocation : resourcelocation;
        }
    }

    public static ResourceLocation getCustomElytraTexture(ItemStack itemStack, ResourceLocation locElytra) {
        if (itemProperties == null) {
            return locElytra;
        } else {
            CustomItemProperties customitemproperties = getCustomItemProperties(itemStack, 4);
            if (customitemproperties == null) {
                return locElytra;
            } else {
                return customitemproperties.textureLocation == null ? locElytra : customitemproperties.textureLocation;
            }
        }
    }

    private static CustomItemProperties getCustomItemProperties(ItemStack itemStack, int type) {
        CustomItemProperties[][] acustomitemproperties = itemProperties;
        if (acustomitemproperties == null) {
            return null;
        } else if (itemStack == null) {
            return null;
        } else {
            Item item = itemStack.getItem();
            int i = Item.getId(item);
            if (i >= 0 && i < acustomitemproperties.length) {
                CustomItemProperties[] acustomitemproperties1 = acustomitemproperties[i];
                if (acustomitemproperties1 != null) {
                    for (int j = 0; j < acustomitemproperties1.length; j++) {
                        CustomItemProperties customitemproperties = acustomitemproperties1[j];
                        if (customitemproperties.type == type && matchesProperties(customitemproperties, itemStack, null)) {
                            return customitemproperties;
                        }
                    }
                }
            }

            return null;
        }
    }

    private static boolean matchesProperties(CustomItemProperties cip, ItemStack itemStack, int[][] enchantmentIdLevels) {
        Item item = itemStack.getItem();
        if (cip.damage != null) {
            int i = getItemStackDamage(itemStack);
            if (i < 0) {
                return false;
            }

            if (cip.damageMask != 0) {
                i &= cip.damageMask;
            }

            if (cip.damagePercent) {
                int j = itemStack.getMaxDamage();
                i = (int)((double)(i * 100) / (double)j);
            }

            if (!cip.damage.isInRange(i)) {
                return false;
            }
        }

        if (cip.stackSize != null && !cip.stackSize.isInRange(itemStack.getCount())) {
            return false;
        } else {
            int[][] aint = enchantmentIdLevels;
            if (cip.enchantmentIds != null) {
                if (enchantmentIdLevels == null) {
                    aint = getEnchantmentIdLevels(itemStack);
                }

                boolean flag = false;

                for (int k = 0; k < aint.length; k++) {
                    int l = aint[k][0];
                    if (Config.equalsOne(l, cip.enchantmentIds)) {
                        flag = true;
                        break;
                    }
                }

                if (!flag) {
                    return false;
                }
            }

            if (cip.enchantmentLevels != null) {
                if (aint == null) {
                    aint = getEnchantmentIdLevels(itemStack);
                }

                boolean flag1 = false;

                for (int i1 = 0; i1 < aint.length; i1++) {
                    int k1 = aint[i1][1];
                    if (cip.enchantmentLevels.isInRange(k1)) {
                        flag1 = true;
                        break;
                    }
                }

                if (!flag1) {
                    return false;
                }
            }

            if (cip.nbtTagValues != null) {
                CompoundTag compoundtag = ItemUtils.getTag(itemStack);

                for (int j1 = 0; j1 < cip.nbtTagValues.length; j1++) {
                    NbtTagValue nbttagvalue = cip.nbtTagValues[j1];
                    if (!nbttagvalue.matches(compoundtag)) {
                        return false;
                    }
                }
            }

            if (cip.hand != 0) {
                if (cip.hand == 1 && renderOffHand) {
                    return false;
                }

                if (cip.hand == 2 && !renderOffHand) {
                    return false;
                }
            }

            return true;
        }
    }

    private static int getItemStackDamage(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return item instanceof PotionItem ? getPotionDamage(itemStack) : itemStack.getDamageValue();
    }

    private static int getPotionDamage(ItemStack itemStack) {
        Potion potion = PotionUtils.getPotion(itemStack);
        if (potion == null) {
            return 0;
        } else {
            String s = PotionUtils.getPotionBaseName(potion);
            if (s != null && !s.equals("")) {
                Integer integer = mapPotionDamages.get(s);
                if (integer == null) {
                    return -1;
                } else {
                    int i = integer;
                    if (itemStack.getItem() == Items.SPLASH_POTION) {
                        i |= 16384;
                    }

                    return i;
                }
            } else {
                return 0;
            }
        }
    }

    private static Map<String, Integer> makeMapPotionDamages() {
        Map<String, Integer> map = new HashMap<>();
        addPotion("water", 0, false, map);
        addPotion("awkward", 16, false, map);
        addPotion("thick", 32, false, map);
        addPotion("mundane", 64, false, map);
        addPotion("regeneration", 1, true, map);
        addPotion("swiftness", 2, true, map);
        addPotion("fire_resistance", 3, true, map);
        addPotion("poison", 4, true, map);
        addPotion("healing", 5, true, map);
        addPotion("night_vision", 6, true, map);
        addPotion("weakness", 8, true, map);
        addPotion("strength", 9, true, map);
        addPotion("slowness", 10, true, map);
        addPotion("leaping", 11, true, map);
        addPotion("harming", 12, true, map);
        addPotion("water_breathing", 13, true, map);
        addPotion("invisibility", 14, true, map);
        return map;
    }

    private static void addPotion(String name, int value, boolean extended, Map<String, Integer> map) {
        if (extended) {
            value |= 8192;
        }

        map.put("minecraft:" + name, value);
        if (extended) {
            int i = value | 32;
            map.put("minecraft:strong_" + name, i);
            int j = value | 64;
            map.put("minecraft:long_" + name, j);
        }
    }

    private static int[][] getEnchantmentIdLevels(ItemStack itemStack) {
        ItemEnchantments itemenchantments = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (itemenchantments.isEmpty()) {
            itemenchantments = itemStack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        }

        if (itemenchantments.isEmpty()) {
            return EMPTY_INT2_ARRAY;
        } else {
            Set<Entry<Holder<Enchantment>>> set = itemenchantments.entrySet();
            int[][] aint = new int[set.size()][2];
            int i = 0;

            for (Entry<Holder<Enchantment>> entry : set) {
                Holder<Enchantment> holder = entry.getKey();
                if (holder.isBound()) {
                    Enchantment enchantment = holder.value();
                    int j = EnchantmentUtils.getId(enchantment);
                    int k = entry.getIntValue();
                    aint[i][0] = j;
                    aint[i][1] = k;
                    i++;
                }
            }

            return aint;
        }
    }

    public static boolean renderCustomEffect(ItemRenderer renderItem, ItemStack itemStack, BakedModel model) {
        CustomItemProperties[][] acustomitemproperties = enchantmentProperties;
        if (acustomitemproperties == null) {
            return false;
        } else if (itemStack == null) {
            return false;
        } else {
            int[][] aint = getEnchantmentIdLevels(itemStack);
            if (aint.length <= 0) {
                return false;
            } else {
                Set set = null;
                return false;
            }
        }
    }

    public static boolean renderCustomArmorEffect(
        LivingEntity entity,
        ItemStack itemStack,
        EntityModel model,
        float limbSwing,
        float prevLimbSwing,
        float partialTicks,
        float timeLimbSwing,
        float yaw,
        float pitch,
        float scale
    ) {
        CustomItemProperties[][] acustomitemproperties = enchantmentProperties;
        if (acustomitemproperties == null) {
            return false;
        } else if (Config.isShaders() && Shaders.isShadowPass) {
            return false;
        } else if (itemStack == null) {
            return false;
        } else {
            int[][] aint = getEnchantmentIdLevels(itemStack);
            if (aint.length <= 0) {
                return false;
            } else {
                Set set = null;
                return false;
            }
        }
    }

    public static boolean isUseGlint() {
        return useGlint;
    }

    public static void setRenderOffHand(boolean renderOffHand) {
        CustomItems.renderOffHand = renderOffHand;
    }
}
