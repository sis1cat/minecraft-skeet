package net.optifine.entity.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.optifine.Config;
import net.optifine.IRandomEntity;
import net.optifine.Log;
import net.optifine.RandomEntities;
import net.optifine.RandomEntityContext;
import net.optifine.RandomEntityProperties;
import net.optifine.entity.model.anim.IModelRendererVariable;
import net.optifine.entity.model.anim.ModelResolver;
import net.optifine.entity.model.anim.ModelUpdater;
import net.optifine.entity.model.anim.ModelVariableUpdater;
import net.optifine.reflect.Reflector;
import net.optifine.util.ArrayUtils;
import net.optifine.util.DebugUtils;
import net.optifine.util.Either;
import net.optifine.util.StrUtils;

public class CustomEntityModels {
    private static boolean active = false;
    private static Map<EntityType, RandomEntityProperties<IEntityRenderer>> mapEntityProperties = new HashMap<>();
    private static Map<BlockEntityType, RandomEntityProperties<IEntityRenderer>> mapBlockEntityProperties = new HashMap<>();
    private static int matchingRuleIndex;
    private static Renderers originalRenderers;
    private static List<BlockEntityType> customBlockEntityTypes = new ArrayList<>();
    private static boolean debugModels = Boolean.getBoolean("cem.debug.models");
    public static final String PREFIX_OPTIFINE_CEM = "optifine/cem/";
    public static final String SUFFIX_JEM = ".jem";
    public static final String SUFFIX_PROPERTIES = ".properties";

    public static void update() {
        if (originalRenderers == null) {
            originalRenderers = Renderers.collectRenderers();
        }

        active = false;
        originalRenderers.restoreRenderers();
        CustomStaticModels.clear();
        customBlockEntityTypes.clear();
        BlockEntityRenderer.CACHED_TYPES.clear();
        if (Minecraft.getInstance().level != null) {
            for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
                Map map = entity.getEntityData().modelVariables;
                if (map != null) {
                    map.clear();
                }
            }
        }

        mapEntityProperties.clear();
        mapBlockEntityProperties.clear();
        if (Config.isCustomEntityModels()) {
            RandomEntityContext.Models randomentitycontext$models = new RandomEntityContext.Models();
            RendererCache renderercache = randomentitycontext$models.getRendererCache();
            Map<String, ResourceLocation> map1 = getModelLocations();

            for (String s : map1.keySet()) {
                ResourceLocation resourcelocation = map1.get(s);
                Config.dbg("CustomEntityModel: " + s + ", model: " + resourcelocation.getPath());
                IEntityRenderer ientityrenderer = parseEntityRender(s, resourcelocation, renderercache, 0);
                if (ientityrenderer != null) {
                    if (ientityrenderer instanceof EntityRenderer entityrenderer) {
                        EntityType entitytype = ientityrenderer.getType().getLeft().get();
                        getEntityRenderMap().put(entitytype, entityrenderer);
                        renderercache.put(entitytype, 0, entityrenderer);
                    } else if (ientityrenderer instanceof BlockEntityRenderer blockentityrenderer) {
                        BlockEntityType blockentitytype = ientityrenderer.getType().getRight().get();
                        getBlockEntityRenderMap().put(blockentitytype, blockentityrenderer);
                        renderercache.put(blockentitytype, 0, blockentityrenderer);
                        customBlockEntityTypes.add(blockentitytype);
                    } else {
                        if (!(ientityrenderer instanceof VirtualEntityRenderer virtualentityrenderer)) {
                            Config.warn("Unknown renderer type: " + ientityrenderer.getClass().getName());
                            continue;
                        }

                        virtualentityrenderer.register();
                    }

                    active = true;
                }
            }

            updateRandomProperties(randomentitycontext$models);
        }
    }

    private static void updateRandomProperties(RandomEntityContext.Models context) {
        String[] astring3 = new String[]{"optifine/cem/"};
        astring3 = new String[]{".jem", ".properties"};
        String[] astring = CustomModelRegistry.getModelNames();

        for (int i = 0; i < astring.length; i++) {
            String s = astring[i];
            String[] astring1 = CustomModelRegistry.getAliases(s);
            ModelAdapter modeladapter = CustomModelRegistry.getModelAdapter(s);
            Either<EntityType, BlockEntityType> either = modeladapter.getType();
            String[] astring2 = makeNameVariants(s, astring1);
            RandomEntityProperties randomentityproperties = makeProperties(s, astring2, context);
            if (randomentityproperties != null) {
                if (either != null && either.getLeft().isPresent()) {
                    mapEntityProperties.put(either.getLeft().get(), randomentityproperties);
                }

                if (either != null && either.getRight().isPresent()) {
                    mapBlockEntityProperties.put(either.getRight().get(), randomentityproperties);
                }
            }
        }
    }

    private static String[] makeNameVariants(String baseName, String[] aliases) {
        List<String> list = new ArrayList<>();
        aliases = ArrayUtils.normalize(aliases);
        String[] astring = (String[])ArrayUtils.addObjectToArray(aliases, baseName, 0);

        for (int i = 0; i < astring.length; i++) {
            String s = astring[i];
            list.add(s);
            list.add(s + "/" + s);
        }

        return list.toArray(new String[list.size()]);
    }

    private static RandomEntityProperties makeProperties(String name, String[] fileNames, RandomEntityContext.Models context) {
        for (int i = 0; i < fileNames.length; i++) {
            String s = fileNames[i];
            RandomEntityProperties randomentityproperties = makeProperties(name, s, context);
            if (randomentityproperties != null) {
                return randomentityproperties;
            }
        }

        return null;
    }

    private static RandomEntityProperties makeProperties(String name, String fileName, RandomEntityContext.Models context) {
        ResourceLocation resourcelocation = new ResourceLocation("optifine/cem/" + name + ".jem");
        ResourceLocation resourcelocation1 = new ResourceLocation("optifine/cem/" + name + ".properties");
        if (Config.hasResource(resourcelocation1)) {
            RandomEntityProperties randomentityproperties = RandomEntityProperties.parse(resourcelocation1, resourcelocation, context);
            if (randomentityproperties != null) {
                return randomentityproperties;
            }
        }

        if (!Config.hasResource(resourcelocation)) {
            return null;
        } else {
            int[] aint = RandomEntities.getLocationsVariants(resourcelocation, false, context);
            if (aint == null) {
                return null;
            } else {
                RandomEntityProperties<IEntityRenderer> randomentityproperties1 = new RandomEntityProperties<>(name, resourcelocation, aint, context);
                return !randomentityproperties1.isValid(resourcelocation.getPath()) ? null : randomentityproperties1;
            }
        }
    }

    public static Map getEntityRenderMap() {
        return RendererUtils.getEntityRenderMap();
    }

    public static Map<BlockEntityType, BlockEntityRenderer> getBlockEntityRenderMap() {
        return RendererUtils.getBlockEntityRenderMap();
    }

    public static Map<SkullBlock.Type, SkullModelBase> getSkullModelMap() {
        return RendererUtils.getSkullModelMap();
    }

    private static Map<String, ResourceLocation> getModelLocations() {
        String s = "optifine/cem/";
        String s1 = ".jem";
        Map<String, ResourceLocation> map = new LinkedHashMap<>();
        String[] astring = CustomModelRegistry.getModelNames();

        for (String s2 : astring) {
            String s3 = s + s2 + s1;
            ResourceLocation resourcelocation = new ResourceLocation(s3);
            if (debugModels) {
                map.put(s2, resourcelocation);
            } else if (Config.hasResource(resourcelocation)) {
                map.put(s2, resourcelocation);
            } else {
                String[] astring1 = CustomModelRegistry.getAliases(s2);
                if (astring1 != null) {
                    for (String s4 : astring1) {
                        String s5 = s + s4 + s1;
                        ResourceLocation resourcelocation1 = new ResourceLocation(s5);
                        if (Config.hasResource(resourcelocation1)) {
                            Config.log("CustomEntityModel alias: " + s2 + " -> " + s4);
                            map.put(s2, resourcelocation1);
                            break;
                        }
                    }
                }
            }
        }

        return map;
    }

    public static IEntityRenderer parseEntityRender(String name, ResourceLocation location, RendererCache rendererCache, int index) {
        try {
            if (debugModels && index == 0) {
                return makeDebugEntityRenderer(location, rendererCache, index);
            } else {
                JsonObject jsonobject = CustomEntityModelParser.loadJson(location);
                return parseEntityRender(name, location.getPath(), jsonobject, rendererCache, index);
            }
        } catch (IOException ioexception) {
            Config.error(ioexception.getClass().getName() + ": " + ioexception.getMessage());
            return null;
        } catch (JsonParseException jsonparseexception) {
            Config.error(jsonparseexception.getClass().getName() + ": " + jsonparseexception.getMessage());
            return null;
        } catch (Exception exception) {
            Log.warn("Error loading CEM: " + location, exception);
            return null;
        }
    }

    private static IEntityRenderer makeDebugEntityRenderer(ResourceLocation loc, RendererCache rendererCache, int index) {
        String s = loc.getPath();
        String s1 = StrUtils.removePrefix(s, "optifine/cem/");
        String s2 = StrUtils.removeSuffix(s1, ".jem");
        ModelAdapter modeladapter = CustomModelRegistry.getModelAdapter(s2);
        Model model = modeladapter.makeModel();
        DyeColor[] adyecolor = DyeColor.values();
        adyecolor = (DyeColor[])ArrayUtils.removeObjectFromArray(adyecolor, DyeColor.BLACK);
        int i = Math.abs(loc.hashCode()) % 256;
        String[] astring = modeladapter.getModelRendererNames();

        for (int j = 0; j < astring.length; j++) {
            String s3 = astring[j];
            ModelPart modelpart = modeladapter.getModelRenderer(model, s3);
            if (modelpart != null) {
                DyeColor dyecolor = adyecolor[(j + i) % adyecolor.length];
                ResourceLocation resourcelocation = new ResourceLocation("textures/block/" + dyecolor.getSerializedName() + "_stained_glass.png");
                modelpart.setTextureLocation(resourcelocation);
                Config.dbg("  " + s3 + ": " + dyecolor.getSerializedName());
            }
        }

        IEntityRenderer ientityrenderer = modeladapter.makeEntityRender(model, rendererCache, index);
        if (ientityrenderer == null) {
            return null;
        } else {
            ientityrenderer.setType(modeladapter.getType());
            return ientityrenderer;
        }
    }

    private static IEntityRenderer parseEntityRender(String name, String path, JsonObject obj, RendererCache rendererCache, int index) {
        CustomEntityRenderer customentityrenderer = CustomEntityModelParser.parseEntityRender(obj, path);
        ModelAdapter modeladapter = CustomModelRegistry.getModelAdapter(name);
        checkNull(modeladapter, "Entity not found: " + name);
        Either<EntityType, BlockEntityType> either = modeladapter.getType();
        IEntityRenderer ientityrenderer = makeEntityRender(modeladapter, customentityrenderer, rendererCache, index);
        if (ientityrenderer == null) {
            return null;
        } else {
            ientityrenderer.setType(either);
            return ientityrenderer;
        }
    }

    private static IEntityRenderer makeEntityRender(ModelAdapter modelAdapter, CustomEntityRenderer cer, RendererCache rendererCache, int index) {
        ResourceLocation resourcelocation = cer.getTextureLocation();
        CustomModelRenderer[] acustommodelrenderer = cer.getCustomModelRenderers();
        float f = cer.getShadowSize();
        Model model = modelAdapter.makeModel();
        if (model == null) {
            return null;
        } else {
            ModelResolver modelresolver = new ModelResolver(modelAdapter, model, acustommodelrenderer);
            if (!modifyModel(modelAdapter, model, acustommodelrenderer, modelresolver)) {
                return null;
            } else {
                IEntityRenderer ientityrenderer = modelAdapter.makeEntityRender(model, rendererCache, index);
                if (ientityrenderer == null) {
                    throw new JsonParseException(
                        "Entity renderer is null, model: " + modelAdapter.getName() + ", adapter: " + modelAdapter.getClass().getName()
                    );
                } else {
                    if (f >= 0.0F) {
                        ientityrenderer.setShadowSize(f);
                    }

                    if (resourcelocation != null) {
                        setTextureLocation(modelAdapter, model, ientityrenderer, resourcelocation);
                    }

                    return ientityrenderer;
                }
            }
        }
    }

    private static void setTextureLocation(ModelAdapter modelAdapter, Model model, IEntityRenderer er, ResourceLocation textureLocation) {
        if (!modelAdapter.setTextureLocation(er, textureLocation)) {
            if (er instanceof LivingEntityRenderer) {
                model.locationTextureCustom = textureLocation;
            } else {
                if (model.isRenderRoot() && er.getType() != null) {
                    model.root().setTextureLocation(textureLocation);
                } else {
                    setTextureTopModelRenderers(modelAdapter, model, textureLocation);
                }
            }
        }
    }

    public static void setTextureTopModelRenderers(ModelAdapter modelAdapter, Model model, ResourceLocation textureLocation) {
        String[] astring = modelAdapter.getModelRendererNames();

        for (int i = 0; i < astring.length; i++) {
            String s = astring[i];
            ModelPart modelpart = modelAdapter.getModelRenderer(model, s);
            if (modelpart != null && modelpart.getTextureLocation() == null) {
                modelpart.setTextureLocation(textureLocation);
            }
        }
    }

    private static boolean modifyModel(ModelAdapter modelAdapter, Model model, CustomModelRenderer[] modelRenderers, ModelResolver mr) {
        List<ModelVariableUpdater> list = new ArrayList<>();

        for (int i = 0; i < modelRenderers.length; i++) {
            CustomModelRenderer custommodelrenderer = modelRenderers[i];
            if (!modifyModel(modelAdapter, model, custommodelrenderer, mr)) {
                return false;
            }

            if (custommodelrenderer.getModelRenderer().getModelUpdater() != null) {
                list.addAll(Arrays.asList(custommodelrenderer.getModelRenderer().getModelUpdater().getModelVariableUpdaters()));
            }
        }

        ModelVariableUpdater[] amodelvariableupdater = list.toArray(new ModelVariableUpdater[list.size()]);
        ModelUpdater modelupdater = new ModelUpdater(amodelvariableupdater);

        for (int j = 0; j < modelRenderers.length; j++) {
            CustomModelRenderer custommodelrenderer1 = modelRenderers[j];
            if (custommodelrenderer1.getModelRenderer().getModelUpdater() != null) {
                custommodelrenderer1.getModelRenderer().setModelUpdater(modelupdater);
            }
        }

        for (int k = 0; k < amodelvariableupdater.length; k++) {
            ModelVariableUpdater modelvariableupdater = amodelvariableupdater[k];
            if (modelvariableupdater.getModelVariable() instanceof IModelRendererVariable imodelrenderervariable) {
                imodelrenderervariable.getModelRenderer().setModelUpdater(modelupdater);
            }
        }

        return true;
    }

    private static boolean modifyModel(ModelAdapter modelAdapter, Model model, CustomModelRenderer customModelRenderer, ModelResolver modelResolver) {
        String s = customModelRenderer.getModelPart();
        ModelPart modelpart = modelAdapter.getModelRenderer(model, s);
        if (modelpart == null) {
            if (modelAdapter.isModelRendererOptional(s)) {
                return true;
            } else {
                Config.warn("Model part not found: " + s + ", model: " + DebugUtils.getClassName(model) + ", modelName: " + modelAdapter.getName());
                return false;
            }
        } else {
            if (!customModelRenderer.isAttach()) {
                if (modelpart.cubes != null) {
                    modelpart.cubes.clear();
                }

                if (modelpart.spriteList != null) {
                    modelpart.spriteList.clear();
                }

                if (modelpart.children != null) {
                    ModelPart[] amodelpart = modelAdapter.getModelRenderers(model);
                    Set<ModelPart> set = Collections.newSetFromMap(new IdentityHashMap<>());
                    set.addAll(Arrays.asList(amodelpart));

                    for (String s1 : new HashSet<>(modelpart.children.keySet())) {
                        ModelPart modelpart1 = modelpart.children.get(s1);
                        if (!set.contains(modelpart1)) {
                            modelpart.children.remove(s1);
                        }
                    }
                }
            }

            String s2 = modelpart.getUniqueChildModelName("CEM-" + s);
            modelpart.addChildModel(s2, customModelRenderer.getModelRenderer());
            ModelUpdater modelupdater = customModelRenderer.getModelUpdater();
            if (modelupdater != null) {
                modelResolver.setThisModelRenderer(customModelRenderer.getModelRenderer());
                modelResolver.setPartModelRenderer(modelpart);
                if (!modelupdater.initialize(modelResolver)) {
                    return false;
                }

                customModelRenderer.getModelRenderer().setModelUpdater(modelupdater);
            }

            return true;
        }
    }

    private static void checkNull(Object obj, String msg) {
        if (obj == null) {
            throw new JsonParseException(msg);
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isCustomModel(BlockState blockStateIn) {
        for (int i = 0; i < customBlockEntityTypes.size(); i++) {
            BlockEntityType blockentitytype = customBlockEntityTypes.get(i);
            if (blockentitytype.isValid(blockStateIn)) {
                return true;
            }
        }

        return false;
    }

    public static void onRenderScreen(Screen screen) {
        BookModel bookmodel = CustomStaticModels.getBookModel();
        if (bookmodel != null && screen instanceof EnchantmentScreen enchantmentscreen) {
            Reflector.GuiEnchantment_bookModel.setValue(enchantmentscreen, bookmodel);
        }
    }

    public static EntityRenderer getEntityRenderer(Entity entityIn, EntityRenderer renderer) {
        if (mapEntityProperties.isEmpty()) {
            return renderer;
        } else {
            IRandomEntity irandomentity = RandomEntities.getRandomEntity(entityIn);
            if (irandomentity == null) {
                return renderer;
            } else {
                RandomEntityProperties<IEntityRenderer> randomentityproperties = mapEntityProperties.get(entityIn.getType());
                if (randomentityproperties == null) {
                    return renderer;
                } else {
                    IEntityRenderer ientityrenderer = randomentityproperties.getResource(irandomentity, renderer);
                    if (!(ientityrenderer instanceof EntityRenderer)) {
                        return null;
                    } else {
                        matchingRuleIndex = randomentityproperties.getMatchingRuleIndex();
                        return (EntityRenderer)ientityrenderer;
                    }
                }
            }
        }
    }

    public static BlockEntityRenderer getBlockEntityRenderer(BlockEntity entityIn, BlockEntityRenderer renderer) {
        if (mapBlockEntityProperties.isEmpty()) {
            return renderer;
        } else {
            IRandomEntity irandomentity = RandomEntities.getRandomBlockEntity(entityIn);
            if (irandomentity == null) {
                return renderer;
            } else {
                RandomEntityProperties<IEntityRenderer> randomentityproperties = mapBlockEntityProperties.get(entityIn.getType());
                if (randomentityproperties == null) {
                    return renderer;
                } else {
                    IEntityRenderer ientityrenderer = randomentityproperties.getResource(irandomentity, renderer);
                    if (!(ientityrenderer instanceof BlockEntityRenderer)) {
                        return null;
                    } else {
                        matchingRuleIndex = randomentityproperties.getMatchingRuleIndex();
                        return (BlockEntityRenderer)ientityrenderer;
                    }
                }
            }
        }
    }

    public static int getMatchingRuleIndex() {
        return matchingRuleIndex;
    }
}
