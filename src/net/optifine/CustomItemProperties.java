package net.optifine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.GlStateManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.optifine.config.IParserInt;
import net.optifine.config.NbtTagValue;
import net.optifine.config.ParserEnchantmentId;
import net.optifine.config.RangeInt;
import net.optifine.config.RangeListInt;
import net.optifine.render.Blender;
import net.optifine.util.ArrayUtils;
import net.optifine.util.Json;
import net.optifine.util.PathUtils;
import net.optifine.util.StrUtils;
import net.optifine.util.TextureUtils;
import org.lwjgl.opengl.GL11;

public class CustomItemProperties {
    public String name = null;
    public String basePath = null;
    public int type = 1;
    public int[] items = null;
    public String texture = null;
    public Map<String, String> mapTextures = null;
    public String model = null;
    public Map<String, String> mapModels = null;
    public RangeListInt damage = null;
    public boolean damagePercent = false;
    public int damageMask = 0;
    public RangeListInt stackSize = null;
    public int[] enchantmentIds = null;
    public RangeListInt enchantmentLevels = null;
    public NbtTagValue[] nbtTagValues = null;
    public int hand = 0;
    public int blend = 1;
    public float speed = 0.0F;
    public float rotation = 0.0F;
    public int layer = 0;
    public float duration = 1.0F;
    public int weight = 0;
    public ResourceLocation textureLocation = null;
    public Map mapTextureLocations = null;
    public TextureAtlasSprite sprite = null;
    public Map mapSprites = null;
    public BakedModel bakedModelTexture = null;
    public Map<String, BakedModel> mapBakedModelsTexture = null;
    public BakedModel bakedModelFull = null;
    public Map<String, BakedModel> mapBakedModelsFull = null;
    public Set<ResourceLocation> modelSpriteLocations = null;
    private int textureWidth = 0;
    private int textureHeight = 0;
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_ITEM = 1;
    public static final int TYPE_ENCHANTMENT = 2;
    public static final int TYPE_ARMOR = 3;
    public static final int TYPE_ELYTRA = 4;
    public static final int HAND_ANY = 0;
    public static final int HAND_MAIN = 1;
    public static final int HAND_OFF = 2;
    public static final String INVENTORY = "inventory";
    private static final String PREFIX_NBT = "nbt.";
    private static final String PREFIX_COMPONENTS = "components.";
    private static final String[] SUBTEXTURE_PREFIXES = new String[]{"bow", "brush", "crossbow", "elytra", "fishing_rod", "shield", "spyglass", "trident"};

    public CustomItemProperties(Properties props, String path) {
        this.name = parseName(path);
        this.basePath = parseBasePath(path);
        this.type = this.parseType(props.getProperty("type"));
        this.items = this.parseItems(props.getProperty("items"), props.getProperty("matchItems"));
        this.mapModels = parseModels(props, this.basePath);
        this.model = parseModel(props.getProperty("model"), path, this.basePath, this.type, this.mapModels);
        this.mapTextures = parseTextures(props, this.basePath);
        boolean flag = this.mapModels == null && this.model == null;
        this.texture = parseTexture(
            props.getProperty("texture"), props.getProperty("tile"), props.getProperty("source"), path, this.basePath, this.type, this.mapTextures, flag
        );
        String s = props.getProperty("damage");
        if (s != null) {
            this.damagePercent = s.contains("%");
            s = s.replace("%", "");
            this.damage = this.parseRangeListInt(s);
            this.damageMask = this.parseInt(props.getProperty("damageMask"), 0);
        }

        this.stackSize = this.parseRangeListInt(props.getProperty("stackSize"));
        this.enchantmentIds = this.parseInts(getProperty(props, "enchantmentIDs", "enchantments"), new ParserEnchantmentId());
        this.enchantmentLevels = this.parseRangeListInt(props.getProperty("enchantmentLevels"));
        this.nbtTagValues = this.parseNbtTagValues(props);
        this.hand = this.parseHand(props.getProperty("hand"));
        this.blend = Blender.parseBlend(props.getProperty("blend"));
        this.speed = this.parseFloat(props.getProperty("speed"), 0.0F);
        this.rotation = this.parseFloat(props.getProperty("rotation"), 0.0F);
        this.layer = this.parseInt(props.getProperty("layer"), 0);
        this.weight = this.parseInt(props.getProperty("weight"), 0);
        this.duration = this.parseFloat(props.getProperty("duration"), 1.0F);
    }

    private static String getProperty(Properties props, String... names) {
        for (int i = 0; i < names.length; i++) {
            String s = names[i];
            String s1 = props.getProperty(s);
            if (s1 != null) {
                return s1;
            }
        }

        return null;
    }

    private static String parseName(String path) {
        String s = path;
        int i = path.lastIndexOf(47);
        if (i >= 0) {
            s = path.substring(i + 1);
        }

        int j = s.lastIndexOf(46);
        if (j >= 0) {
            s = s.substring(0, j);
        }

        return s;
    }

    private static String parseBasePath(String path) {
        int i = path.lastIndexOf(47);
        return i < 0 ? "" : path.substring(0, i);
    }

    private int parseType(String str) {
        if (str == null) {
            return 1;
        } else if (str.equals("item")) {
            return 1;
        } else if (str.equals("enchantment")) {
            return 2;
        } else if (str.equals("armor")) {
            return 3;
        } else if (str.equals("elytra")) {
            return 4;
        } else {
            Config.warn("Unknown method: " + str);
            return 0;
        }
    }

    private int[] parseItems(String str, String str2) {
        if (str == null) {
            str = str2;
        }

        if (str == null) {
            return null;
        } else {
            str = str.trim();
            Set set = new TreeSet();
            String[] astring = Config.tokenize(str, " ");

            for (int i = 0; i < astring.length; i++) {
                String s = astring[i];
                Item item = this.getItemByName(s);
                if (item == null) {
                    Config.warn("Item not found: " + s);
                } else {
                    int j = Item.getId(item);
                    if (j < 0) {
                        Config.warn("Item ID not found: " + s);
                    } else {
                        set.add(new Integer(j));
                    }
                }
            }

            Integer[] ainteger = (Integer[]) set.toArray(new Integer[set.size()]);
            int[] aint = new int[ainteger.length];

            for (int k = 0; k < aint.length; k++) {
                aint[k] = ainteger[k];
            }

            return aint;
        }
    }

    private Item getItemByName(String name) {
        ResourceLocation resourcelocation = new ResourceLocation(name);
        return !BuiltInRegistries.ITEM.containsKey(resourcelocation) ? null : BuiltInRegistries.ITEM.getValue(resourcelocation);
    }

    private static String parseTexture(
        String texStr, String texStr2, String texStr3, String path, String basePath, int type, Map<String, String> mapTexs, boolean textureFromPath
    ) {
        if (texStr == null) {
            texStr = texStr2;
        }

        if (texStr == null) {
            texStr = texStr3;
        }

        if (texStr != null) {
            String s2 = ".png";
            if (texStr.endsWith(s2)) {
                texStr = texStr.substring(0, texStr.length() - s2.length());
            }

            return fixTextureName(texStr, basePath);
        } else if (type == 3) {
            return null;
        } else {
            if (mapTexs != null) {
                String s = mapTexs.get("texture.bow_standby");
                if (s != null) {
                    return s;
                }
            }

            if (!textureFromPath) {
                return null;
            } else {
                String s1 = path;
                int i = path.lastIndexOf(47);
                if (i >= 0) {
                    s1 = path.substring(i + 1);
                }

                int j = s1.lastIndexOf(46);
                if (j >= 0) {
                    s1 = s1.substring(0, j);
                }

                return fixTextureName(s1, basePath);
            }
        }
    }

    private static Map parseTextures(Properties props, String basePath) {
        String s = "texture.";
        Map map = getMatchingProperties(props, s);
        if (map.size() <= 0) {
            return null;
        } else {
            Set set = map.keySet();
            Map map1 = new LinkedHashMap();

            for (String s1 : (Set<String>)(Set<?>) set) {
                String s2 = (String)map.get(s1);
                s2 = fixTextureName(s2, basePath);
                map1.put(s1, s2);
            }

            return map1;
        }
    }

    private static String fixTextureName(String iconName, String basePath) {
        iconName = TextureUtils.fixResourcePath(iconName, basePath);
        if (!iconName.startsWith(basePath) && !iconName.startsWith("textures/") && !iconName.startsWith("optifine/")) {
            iconName = basePath + "/" + iconName;
        }

        if (iconName.endsWith(".png")) {
            iconName = iconName.substring(0, iconName.length() - 4);
        }

        if (iconName.startsWith("/")) {
            iconName = iconName.substring(1);
        }

        return iconName;
    }

    private static String parseModel(String modelStr, String path, String basePath, int type, Map<String, String> mapModelNames) {
        if (modelStr != null) {
            String s1 = ".json";
            if (modelStr.endsWith(s1)) {
                modelStr = modelStr.substring(0, modelStr.length() - s1.length());
            }

            return fixModelName(modelStr, basePath);
        } else if (type == 3) {
            return null;
        } else {
            if (mapModelNames != null) {
                String s = mapModelNames.get("model.bow_standby");
                if (s != null) {
                    return s;
                }
            }

            return modelStr;
        }
    }

    private static Map parseModels(Properties props, String basePath) {
        String s = "model.";
        Map map = getMatchingProperties(props, s);
        if (map.size() <= 0) {
            return null;
        } else {
            Set set = map.keySet();
            Map map1 = new LinkedHashMap();

            for (String s1 : (Set<String>)(Set<?>) set) {
                String s2 = (String)map.get(s1);
                s2 = fixModelName(s2, basePath);
                map1.put(s1, s2);
            }

            return map1;
        }
    }

    private static String fixModelName(String modelName, String basePath) {
        modelName = TextureUtils.fixResourcePath(modelName, basePath);
        boolean flag = modelName.startsWith("block/") || modelName.startsWith("item/");
        if (!modelName.startsWith(basePath) && !flag && !modelName.startsWith("optifine/")) {
            modelName = basePath + "/" + modelName;
        }

        String s = ".json";
        if (modelName.endsWith(s)) {
            modelName = modelName.substring(0, modelName.length() - s.length());
        }

        if (modelName.startsWith("/")) {
            modelName = modelName.substring(1);
        }

        return modelName;
    }

    private int parseInt(String str, int defVal) {
        if (str == null) {
            return defVal;
        } else {
            str = str.trim();
            int i = Config.parseInt(str, Integer.MIN_VALUE);
            if (i == Integer.MIN_VALUE) {
                Config.warn("Invalid integer: " + str);
                return defVal;
            } else {
                return i;
            }
        }
    }

    private float parseFloat(String str, float defVal) {
        if (str == null) {
            return defVal;
        } else {
            str = str.trim();
            float f = Config.parseFloat(str, Float.MIN_VALUE);
            if (f == Float.MIN_VALUE) {
                Config.warn("Invalid float: " + str);
                return defVal;
            } else {
                return f;
            }
        }
    }

    private int[] parseInts(String str, IParserInt parser) {
        if (str == null) {
            return null;
        } else {
            String[] astring = Config.tokenize(str, " ");
            List<Integer> list = new ArrayList<>();

            for (int i = 0; i < astring.length; i++) {
                String s = astring[i];
                int j = parser.parse(s, Integer.MIN_VALUE);
                if (j == Integer.MIN_VALUE) {
                    Config.warn("Invalid value: " + s);
                } else {
                    list.add(j);
                }
            }

            Integer[] ainteger = list.toArray(new Integer[list.size()]);
            return Config.toPrimitive(ainteger);
        }
    }

    private RangeListInt parseRangeListInt(String str) {
        if (str == null) {
            return null;
        } else {
            String[] astring = Config.tokenize(str, " ");
            RangeListInt rangelistint = new RangeListInt();

            for (int i = 0; i < astring.length; i++) {
                String s = astring[i];
                RangeInt rangeint = this.parseRangeInt(s);
                if (rangeint == null) {
                    Config.warn("Invalid range list: " + str);
                    return null;
                }

                rangelistint.addRange(rangeint);
            }

            return rangelistint;
        }
    }

    private RangeInt parseRangeInt(String str) {
        if (str == null) {
            return null;
        } else {
            str = str.trim();
            int i = str.length() - str.replace("-", "").length();
            if (i > 1) {
                Config.warn("Invalid range: " + str);
                return null;
            } else {
                String[] astring = Config.tokenize(str, "- ");
                int[] aint = new int[astring.length];

                for (int j = 0; j < astring.length; j++) {
                    String s = astring[j];
                    int k = Config.parseInt(s, -1);
                    if (k < 0) {
                        Config.warn("Invalid range: " + str);
                        return null;
                    }

                    aint[j] = k;
                }

                if (aint.length == 1) {
                    int i1 = aint[0];
                    if (str.startsWith("-")) {
                        return new RangeInt(0, i1);
                    } else {
                        return str.endsWith("-") ? new RangeInt(i1, 65535) : new RangeInt(i1, i1);
                    }
                } else if (aint.length == 2) {
                    int l = Math.min(aint[0], aint[1]);
                    int j1 = Math.max(aint[0], aint[1]);
                    return new RangeInt(l, j1);
                } else {
                    Config.warn("Invalid range: " + str);
                    return null;
                }
            }
        }
    }

    private NbtTagValue[] parseNbtTagValues(Properties props) {
        Map<String, String> map = getMatchingProperties(props, "components.");
        Map<String, String> map1 = getMatchingProperties(props, "nbt.");
        Map<String, String> map2 = this.generateComponentsFromNbt(map1);
        if (map.isEmpty()) {
            if (map1.size() != map2.size()) {
                return new NbtTagValue[0];
            }

            map.putAll(map2);
            if (map.isEmpty()) {
                return null;
            }
        }

        List<NbtTagValue> list = new ArrayList<>();

        for (Entry<String, String> entry : map.entrySet()) {
            String s = entry.getKey();
            String s1 = entry.getValue();
            String s2 = s.substring("components.".length());
            s2 = this.fixNamespaces(s2);
            NbtTagValue nbttagvalue = new NbtTagValue(s2, s1);
            list.add(nbttagvalue);
        }

        return list.toArray(new NbtTagValue[list.size()]);
    }

    private Map<String, String> generateComponentsFromNbt(Map<String, String> mapNbt) {
        Map<String, String> map = new LinkedHashMap<>();

        for (Entry<String, String> entry : mapNbt.entrySet()) {
            String s = entry.getKey();
            String s1 = entry.getValue();
            String s2 = s.substring("nbt.".length());
            int i = map.size();
            if (s2.equals("display.Name")) {
                map.putIfAbsent("components.minecraft:custom_name", s1);
            } else if (s2.equals("display.Lore")) {
                map.putIfAbsent("components.minecraft:lore", s1);
            } else if (s2.equals("Potion")) {
                map.putIfAbsent("components.minecraft:potion_contents.potion", s1);
            }

            if (s2.equals("Damage")) {
                map.putIfAbsent("components.minecraft:damage", s1);
            }

            if (s2.equals("Variant") && this.type == 1 && ArrayUtils.contains(this.items, Item.getId(Items.AXOLOTL_BUCKET))) {
                map.putIfAbsent("components.minecraft:bucket_entity_data.Variant", s1);
            }

            if (map.size() > i) {
                Config.warn("Deprecated NBT check: " + s + "=" + s1);
            } else {
                Config.warn("Invalid NBT check: " + s + "=" + s1);
            }
        }

        return map;
    }

    private String fixNamespaces(String id) {
        id = id.replace("~", "minecraft:");
        if (id.startsWith("*")) {
            return id;
        } else {
            int i = id.indexOf(46);
            int j = i >= 0 ? i : id.length();
            if (id.indexOf(58, 0, j) < 0) {
                id = "minecraft:" + id;
            }

            return id;
        }
    }

    private static Map<String, String> getMatchingProperties(Properties props, String keyPrefix) {
        Map map = new LinkedHashMap();

        for (String s : (Set<String>)(Set<?>) props.keySet()) {
            String s1 = props.getProperty(s);
            if (s.startsWith(keyPrefix)) {
                map.put(s, s1);
            }
        }

        return map;
    }

    private int parseHand(String str) {
        if (str == null) {
            return 0;
        } else {
            str = str.toLowerCase();
            if (str.equals("any")) {
                return 0;
            } else if (str.equals("main")) {
                return 1;
            } else if (str.equals("off")) {
                return 2;
            } else {
                Config.warn("Invalid hand: " + str);
                return 0;
            }
        }
    }

    public boolean isValid(String path) {
        if (this.name == null || this.name.length() <= 0) {
            Config.warn("No name found: " + path);
            return false;
        } else if (this.basePath == null) {
            Config.warn("No base path found: " + path);
            return false;
        } else if (this.type == 0) {
            Config.warn("No type defined: " + path);
            return false;
        } else {
            if (this.type == 4 && this.items == null) {
                this.items = new int[]{Item.getId(Items.ELYTRA)};
            }

            if (this.type == 1 || this.type == 3 || this.type == 4) {
                if (this.items == null) {
                    this.items = this.detectItems();
                }

                if (this.items == null) {
                    Config.warn("No items defined: " + path);
                    return false;
                }
            }

            if (this.texture == null && this.mapTextures == null && this.model == null && this.mapModels == null) {
                Config.warn("No texture or model specified: " + path);
                return false;
            } else if (this.type == 2 && this.enchantmentIds == null) {
                Config.warn("No enchantmentIDs specified: " + path);
                return false;
            } else if (this.nbtTagValues != null && this.nbtTagValues.length == 0) {
                Config.warn("Invalid NBT checks specified: " + path);
                return false;
            } else {
                return true;
            }
        }
    }

    private int[] detectItems() {
        Item item = this.getItemByName(this.name);
        if (item == null) {
            return null;
        } else {
            int i = Item.getId(item);
            return i < 0 ? null : new int[]{i};
        }
    }

    public void registerIcons(TextureAtlas textureMap) {
        if (this.texture != null) {
            this.textureLocation = this.getTextureLocation(this.texture);
            if (this.type == 1) {
                ResourceLocation resourcelocation = this.getSpriteLocation(this.textureLocation);
                this.sprite = textureMap.registerSprite(resourcelocation);
            }
        }

        if (this.mapTextures != null) {
            this.mapTextureLocations = new HashMap();
            this.mapSprites = new HashMap();

            for (String s : this.mapTextures.keySet()) {
                String s1 = this.mapTextures.get(s);
                ResourceLocation resourcelocation1 = this.getTextureLocation(s1);
                this.mapTextureLocations.put(s, resourcelocation1);
                if (this.type == 1) {
                    ResourceLocation resourcelocation2 = this.getSpriteLocation(resourcelocation1);
                    TextureAtlasSprite textureatlassprite = textureMap.registerSprite(resourcelocation2);
                    this.mapSprites.put(s, textureatlassprite);
                }
            }
        }

        for (ResourceLocation resourcelocation3 : this.modelSpriteLocations) {
            textureMap.registerSprite(resourcelocation3);
        }
    }

    public void updateIcons(TextureAtlas textureMap) {
        if (this.sprite != null) {
            this.sprite = textureMap.getSprite(this.sprite.getName());
        }

        if (this.mapSprites != null) {
            for (String s : (Set<String>)(Set<?>) this.mapSprites.keySet()) {
                TextureAtlasSprite textureatlassprite = (TextureAtlasSprite)this.mapSprites.get(s);
                if (textureatlassprite != null) {
                    ResourceLocation resourcelocation = textureatlassprite.getName();
                    TextureAtlasSprite textureatlassprite1 = textureMap.getSprite(resourcelocation);
                    if (textureatlassprite1 == null || MissingTextureAtlasSprite.isMisingSprite(textureatlassprite1)) {
                        Config.warn("Missing CIT sprite: " + resourcelocation + ", properties: " + this.basePath);
                    }

                    this.mapSprites.put(s, textureatlassprite1);
                }
            }
        }
    }

    private ResourceLocation getTextureLocation(String texName) {
        if (texName == null) {
            return null;
        } else {
            ResourceLocation resourcelocation = new ResourceLocation(texName);
            String s = resourcelocation.getNamespace();
            String s1 = resourcelocation.getPath();
            if (!s1.contains("/")) {
                s1 = "textures/item/" + s1;
            }

            String s2 = s1 + ".png";
            ResourceLocation resourcelocation1 = new ResourceLocation(s, s2);
            boolean flag = Config.hasResource(resourcelocation1);
            if (!flag) {
                Config.warn("File not found: " + s2);
            }

            return resourcelocation1;
        }
    }

    private ResourceLocation getSpriteLocation(ResourceLocation resLoc) {
        String s = resLoc.getPath();
        s = StrUtils.removePrefix(s, "textures/");
        s = StrUtils.removeSuffix(s, ".png");
        return new ResourceLocation(resLoc.getNamespace(), s);
    }

    public void updateModelTexture(TextureAtlas textureMap, ItemModelGenerator itemModelGenerator) {
        if (this.texture != null || this.mapTextures != null) {
            String[] astring = this.getModelTextures();
            boolean flag = this.isUseTint();
            this.bakedModelTexture = makeBakedModel(textureMap, itemModelGenerator, astring, flag);
            if (this.type == 1 && this.mapTextures != null) {
                for (String s : this.mapTextures.keySet()) {
                    String s1 = this.mapTextures.get(s);
                    String s2 = StrUtils.removePrefix(s, "texture.");
                    if (this.isSubTexture(s2)) {
                        String[] astring1 = new String[]{s1};
                        BakedModel bakedmodel = makeBakedModel(textureMap, itemModelGenerator, astring1, flag);
                        if (this.mapBakedModelsTexture == null) {
                            this.mapBakedModelsTexture = new HashMap<>();
                        }

                        String s3 = "item/" + s2;
                        this.mapBakedModelsTexture.put(s3, bakedmodel);
                    }
                }
            }
        }
    }

    private boolean isSubTexture(String path) {
        return StrUtils.startsWith(path, SUBTEXTURE_PREFIXES);
    }

    private boolean isUseTint() {
        return true;
    }

    private static BakedModel makeBakedModel(TextureAtlas textureMap, ItemModelGenerator itemModelGenerator, String[] textures, boolean useTint) {
        Map<String, Material> map = new HashMap<>();
        ResourceLocation resourcelocation = Config.getTextureMap().location();
        String s = "unknown";

        for (int i = 0; i < textures.length; i++) {
            String s1 = textures[i];
            String s2 = StrUtils.removePrefix(s1, "textures/");
            ResourceLocation resourcelocation1 = ResourceLocation.withDefaultNamespace(s2);
            Material material = new Material(resourcelocation, resourcelocation1);
            String s3 = "layer" + i;
            map.put(s3, material);
            if (i == 0) {
                s = s1;
                map.put("particle", material);
            }
        }

        TextureSlots textureslots = new TextureSlots(map);
        final ModelDebugName modeldebugname = () -> ArrayUtils.arrayToString((Object[])textures);
        ModelBaker modelbaker = new ModelBaker() {
            @Override
            public ModelDebugName rootName() {
                return modeldebugname;
            }

            @Override
            public SpriteGetter sprites() {
                return new SpriteGetter() {
                    @Override
                    public TextureAtlasSprite get(Material materialIn) {
                        return materialIn.sprite();
                    }

                    @Override
                    public TextureAtlasSprite reportMissingReference(String nameIn) {
                        Config.warn("Sprite not found: " + nameIn);
                        return Config.getTextureMap().getUploadedSprite(MissingTextureAtlasSprite.getLocation());
                    }
                };
            }

            @Override
            public BakedModel bake(ResourceLocation locIn, ModelState stateIn) {
                return null;
            }
        };
        ModelState modelstate = BlockModelRotation.X0_Y0;
        boolean flag = false;
        boolean flag1 = true;
        ItemTransforms itemtransforms = ItemTransforms.NO_TRANSFORMS;
        BakedModel bakedmodel = itemModelGenerator.bake(textureslots, modelbaker, modelstate, flag, flag1, itemtransforms);
        if (bakedmodel instanceof SimpleBakedModel simplebakedmodel) {
            ResourceLocation resourcelocation2 = ResourceLocation.withDefaultNamespace(s + ".png");
            simplebakedmodel.setModelLocation(resourcelocation2);
        }

        return bakedmodel;
    }

    public static TextureAtlasSprite getSprite(Material material) {
        TextureAtlas textureatlas = Minecraft.getInstance().getModelManager().getAtlas(material.atlasLocation());
        return textureatlas.getSprite(material.texture());
    }

    private String[] getModelTextures() {
        if (this.type == 1 && this.items.length == 1) {
            Item item = Item.byId(this.items[0]);
            boolean flag = item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION;
            if (flag && this.damage != null && this.damage.getCountRanges() > 0) {
                RangeInt rangeint = this.damage.getRange(0);
                int i = rangeint.getMin();
                boolean flag1 = (i & 16384) != 0;
                String s5 = this.getMapTexture(this.mapTextures, "texture.potion_overlay", "item/potion_overlay");
                String s6 = null;
                if (flag1) {
                    s6 = this.getMapTexture(this.mapTextures, "texture.potion_bottle_splash", "item/potion_bottle_splash");
                } else {
                    s6 = this.getMapTexture(this.mapTextures, "texture.potion_bottle_drinkable", "item/potion_bottle_drinkable");
                }

                return new String[]{s5, s6};
            }

            if (item instanceof ArmorItem armoritem && armoritem.getArmorMaterial() == ArmorMaterials.LEATHER) {
                String s = "leather";
                String s1 = "helmet";
                EquipmentSlot equipmentslot = armoritem.getArmorType().getSlot();
                if (equipmentslot == EquipmentSlot.HEAD) {
                    s1 = "helmet";
                }

                if (equipmentslot == EquipmentSlot.CHEST) {
                    s1 = "chestplate";
                }

                if (equipmentslot == EquipmentSlot.LEGS) {
                    s1 = "leggings";
                }

                if (equipmentslot == EquipmentSlot.FEET) {
                    s1 = "boots";
                }

                String s2 = s + "_" + s1;
                String s3 = this.getMapTexture(this.mapTextures, "texture." + s2, "item/" + s2);
                String s4 = this.getMapTexture(this.mapTextures, "texture." + s2 + "_overlay", "item/" + s2 + "_overlay");
                return new String[]{s3, s4};
            }
        }

        return new String[]{this.texture};
    }

    private String getMapTexture(Map<String, String> map, String key, String def) {
        if (map == null) {
            return def;
        } else {
            String s = map.get(key);
            return s == null ? def : s;
        }
    }

    @Override
    public String toString() {
        return this.basePath + "/" + this.name + ", type: " + this.type + ", items: [" + Config.arrayToString(this.items) + "], texture: " + this.texture;
    }

    public float getTextureWidth(TextureManager textureManager) {
        if (this.textureWidth <= 0) {
            if (this.textureLocation != null) {
                AbstractTexture abstracttexture = textureManager.getTexture(this.textureLocation);
                int i = abstracttexture.getId();
                int j = GlStateManager.getBoundTexture();
                GlStateManager._bindTexture(i);
                this.textureWidth = GL11.glGetTexLevelParameteri(3553, 0, 4096);
                GlStateManager._bindTexture(j);
            }

            if (this.textureWidth <= 0) {
                this.textureWidth = 16;
            }
        }

        return (float)this.textureWidth;
    }

    public float getTextureHeight(TextureManager textureManager) {
        if (this.textureHeight <= 0) {
            if (this.textureLocation != null) {
                AbstractTexture abstracttexture = textureManager.getTexture(this.textureLocation);
                int i = abstracttexture.getId();
                int j = GlStateManager.getBoundTexture();
                GlStateManager._bindTexture(i);
                this.textureHeight = GL11.glGetTexLevelParameteri(3553, 0, 4097);
                GlStateManager._bindTexture(j);
            }

            if (this.textureHeight <= 0) {
                this.textureHeight = 16;
            }
        }

        return (float)this.textureHeight;
    }

    public BakedModel getBakedModel(ResourceLocation modelLocation, boolean fullModel) {
        BakedModel bakedmodel;
        Map<String, BakedModel> map;
        if (fullModel) {
            bakedmodel = this.bakedModelFull;
            map = this.mapBakedModelsFull;
        } else {
            bakedmodel = this.bakedModelTexture;
            map = this.mapBakedModelsTexture;
        }

        if (modelLocation != null && map != null) {
            String s = modelLocation.getPath();
            BakedModel bakedmodel1 = map.get(s);
            if (bakedmodel1 != null) {
                return bakedmodel1;
            }
        }

        return bakedmodel;
    }

    public void registerModels(Map<ResourceLocation, Resource> mapModelsIn, boolean checkParents) {
        for (ResourceLocation resourcelocation : this.getModelLocations(checkParents)) {
            ResourceLocation resourcelocation1 = this.getModelLocationJson(resourcelocation);
            if (!mapModelsIn.containsKey(resourcelocation1)) {
                Optional<Resource> optional = Config.getResourceSafe(resourcelocation1);
                if (optional.isEmpty()) {
                    Config.warn("Model not found: " + resourcelocation);
                } else {
                    Resource resource = optional.get();
                    mapModelsIn.put(resourcelocation1, resource);
                }
            }
        }
    }

    public void collectModelSprites(Map<ResourceLocation, UnbakedModel> mapModelsIn) {
        this.modelSpriteLocations = new LinkedHashSet<>();

        for (ResourceLocation resourcelocation : this.getModelLocations(false)) {
            UnbakedModel unbakedmodel = mapModelsIn.get(resourcelocation);
            if (unbakedmodel == null) {
                Config.warn("Unbaked model not found: " + resourcelocation);
            } else {
                this.collectModelSprites(unbakedmodel, resourcelocation);
            }
        }
    }

    public Set<ResourceLocation> getModelLocations(boolean checkParents) {
        Set<ResourceLocation> set = new LinkedHashSet<>();
        if (this.model != null) {
            ResourceLocation resourcelocation = getModelLocation(this.model);
            this.addModelLocation(set, resourcelocation, checkParents);
        }

        if (this.type == 1 && this.mapModels != null) {
            for (String s : this.mapModels.keySet()) {
                String s1 = this.mapModels.get(s);
                String s2 = StrUtils.removePrefix(s, "model.");
                if (this.isSubTexture(s2)) {
                    ResourceLocation resourcelocation1 = getModelLocation(s1);
                    this.addModelLocation(set, resourcelocation1, checkParents);
                }
            }
        }

        return set;
    }

    private void addModelLocation(Set<ResourceLocation> set, ResourceLocation loc, boolean checkParents) {
        if (!set.contains(loc)) {
            set.add(loc);
            if (checkParents) {
                ResourceLocation resourcelocation = this.getModelLocationJson(loc);
                Optional<Resource> optional = Config.getResourceSafe(resourcelocation);
                if (!optional.isEmpty()) {
                    try {
                        JsonParser jsonparser = new JsonParser();
                        JsonObject jsonobject = (JsonObject)jsonparser.parse(optional.get().openAsReader());
                        String s = Json.getString(jsonobject, "parent");
                        if (s != null && !s.startsWith("builtin/")) {
                            s = PathUtils.resolveRelative(s, this.basePath);
                            ResourceLocation resourcelocation1 = getModelLocation(s);
                            this.addModelLocation(set, resourcelocation1, checkParents);
                        }
                    } catch (Exception exception) {
                        Config.warn("Error loading custom model: " + resourcelocation + ", " + exception.getClass().getName() + ": " + exception.getMessage());
                    }
                }
            }
        }
    }

    public void updateModelsFull() {
        ModelManager modelmanager = Config.getModelManager();
        BakedModel bakedmodel = modelmanager.getMissingModel();
        if (this.model != null) {
            ResourceLocation resourcelocation = getItemLocation(this.model);
            this.bakedModelFull = modelmanager.getBakedItemModel(resourcelocation);
            if (this.bakedModelFull == bakedmodel) {
                Config.warn("Custom Items: Model not found " + resourcelocation.toString());
                this.bakedModelFull = null;
            }
        }

        if (this.type == 1 && this.mapModels != null) {
            for (String s : this.mapModels.keySet()) {
                String s1 = this.mapModels.get(s);
                String s2 = StrUtils.removePrefix(s, "model.");
                if (this.isSubTexture(s2)) {
                    ResourceLocation resourcelocation1 = getItemLocation(s1);
                    BakedModel bakedmodel1 = modelmanager.getBakedItemModel(resourcelocation1);
                    if (bakedmodel1 == bakedmodel) {
                        Config.warn("Custom Items: Model not found " + resourcelocation1.toString());
                    } else {
                        if (this.mapBakedModelsFull == null) {
                            this.mapBakedModelsFull = new HashMap<>();
                        }

                        String s3 = "item/" + s2;
                        this.mapBakedModelsFull.put(s3, bakedmodel1);
                    }
                }
            }
        }
    }

    private void collectModelSprites(UnbakedModel modelIn, final ResourceLocation locIn) {
        ModelDebugName modeldebugname = new ModelDebugName() {
            public String get() {
                return locIn.toString();
            }
        };
        TextureSlots textureslots = UnbakedModel.getTopTextureSlots(modelIn, modeldebugname);
        if (textureslots != null) {
            Map<String, Material> map = textureslots.getResolvedValues();

            for (String s : map.keySet()) {
                Material material = map.get(s);
                ResourceLocation resourcelocation = material.texture();
                this.modelSpriteLocations.add(resourcelocation);
            }
        }
    }

    private static ResourceLocation getModelLocation(String name) {
        return new ResourceLocation(name);
    }

    private ResourceLocation getModelLocationJson(ResourceLocation loc) {
        String s = StrUtils.addSuffixCheck(loc.getPath(), ".json");
        s = PathUtils.resolveRelative(s, this.basePath);
        if (!s.startsWith("optifine/")) {
            s = StrUtils.addPrefixCheck("models/", s);
        }

        return loc.withPath(s);
    }

    private static ResourceLocation getItemLocation(String modelName) {
        modelName = StrUtils.removePrefix(modelName, "item/");
        return getModelLocation(modelName);
    }
}
