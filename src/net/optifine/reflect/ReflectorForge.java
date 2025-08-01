package net.optifine.reflect;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.optifine.Log;
import net.optifine.util.StrUtils;
import org.joml.Vector3f;

public class ReflectorForge {
    public static final boolean FORGE_ENTITY_CAN_UPDATE = Reflector.IForgeEntity_canUpdate.exists();

    public static void putLaunchBlackboard(String key, Object value) {
        Map map = (Map)Reflector.getFieldValue(Reflector.Launch_blackboard);
        if (map != null) {
            map.put(key, value);
        }
    }

    public static InputStream getOptiFineResourceStream(String path) {
        if (!Reflector.OptiFineResourceLocator.exists()) {
            return null;
        } else {
            path = StrUtils.removePrefix(path, "/");
            return (InputStream)Reflector.call(Reflector.OptiFineResourceLocator_getOptiFineResourceStream, path);
        }
    }

    public static ReflectorClass getReflectorClassOptiFineResourceLocator() {
        String s = "optifine.OptiFineResourceLocator";
        return System.getProperties().get(s + ".class") instanceof Class oclass ? new ReflectorClass(oclass) : new ReflectorClass(s);
    }

    public static boolean calculateFaceWithoutAO(
        BlockAndTintGetter getter, BlockState state, BlockPos pos, BakedQuad quad, boolean isFaceCubic, float[] brightness, int[] lightmap
    ) {
        if (quad.hasAmbientOcclusion()) {
            return false;
        } else {
            BlockPos blockpos = isFaceCubic ? pos.relative(quad.getDirection()) : pos;
            brightness[0] = brightness[1] = brightness[2] = brightness[3] = getter.getShade(quad.getDirection(), quad.isShade());
            lightmap[0] = lightmap[1] = lightmap[2] = lightmap[3] = LevelRenderer.getPackedLightmapCoords(getter, state, blockpos, false);
            return true;
        }
    }

    public static String[] getForgeModIds() {
        if (!Reflector.ModList.exists()) {
            return new String[0];
        } else {
            Object object = Reflector.ModList_get.call();
            List list = (List)Reflector.getFieldValue(object, Reflector.ModList_mods);
            if (list == null) {
                return new String[0];
            } else {
                List<String> list1 = new ArrayList<>();

                for (Object object1 : list) {
                    if (Reflector.ModContainer.isInstance(object1)) {
                        String s = Reflector.callString(object1, Reflector.ModContainer_getModId);
                        if (s != null && !s.equals("minecraft") && !s.equals("forge")) {
                            list1.add(s);
                        }
                    }
                }

                return list1.toArray(new String[list1.size()]);
            }
        }
    }

    public static Button makeButtonMods(TitleScreen guiMainMenu, int yIn, int rowHeightIn) {
        return !Reflector.ModListScreen_Constructor.exists() ? null : Button.builder(Component.translatable("fml.menu.mods"), button -> {
            Screen screen = (Screen)Reflector.ModListScreen_Constructor.newInstance(guiMainMenu);
            Minecraft.getInstance().setScreen(screen);
        }).pos(guiMainMenu.width / 2 - 100, yIn).size(98, 20).build();
    }

    public static void setForgeLightPipelineEnabled(boolean value) {
        if (Reflector.ForgeConfig_Client_forgeLightPipelineEnabled.exists()) {
            setConfigClientBoolean(Reflector.ForgeConfig_Client_forgeLightPipelineEnabled, value);
        }
    }

    public static boolean getConfigClientBoolean(ReflectorField configField, boolean def) {
        if (!configField.exists()) {
            return def;
        } else {
            Object object = Reflector.ForgeConfig_CLIENT.getValue();
            if (object == null) {
                return def;
            } else {
                Object object1 = Reflector.getFieldValue(object, configField);
                return object1 == null ? def : Reflector.callBoolean(object1, Reflector.ForgeConfigSpec_ConfigValue_get);
            }
        }
    }

    private static void setConfigClientBoolean(ReflectorField clientField, final boolean value) {
        if (clientField.exists()) {
            Object object = Reflector.ForgeConfig_CLIENT.getValue();
            if (object != null) {
                Object object1 = Reflector.getFieldValue(object, clientField);
                if (object1 != null) {
                    Supplier<Boolean> supplier = new Supplier<Boolean>() {
                        public Boolean get() {
                            return value;
                        }
                    };
                    Reflector.setFieldValue(object1, Reflector.ForgeConfigSpec_ConfigValue_defaultSupplier, supplier);
                    Object object2 = Reflector.getFieldValue(object1, Reflector.ForgeConfigSpec_ConfigValue_spec);
                    if (object2 != null) {
                        Reflector.setFieldValue(object2, Reflector.ForgeConfigSpec_childConfig, null);
                    }

                    Log.dbg("Set ForgeConfig.CLIENT." + clientField.getTargetField().getName() + "=" + value);
                }
            }
        }
    }

    public static boolean canUpdate(Entity entity) {
        return FORGE_ENTITY_CAN_UPDATE ? Reflector.callBoolean(entity, Reflector.IForgeEntity_canUpdate) : true;
    }

    private static Vector3f getVertexPos(int[] data, int vertex) {
        int i = data.length / 4;
        int j = vertex * i;
        float f = Float.intBitsToFloat(data[j]);
        float f1 = Float.intBitsToFloat(data[j + 1]);
        float f2 = Float.intBitsToFloat(data[j + 2]);
        return new Vector3f(f, f1, f2);
    }

    public static void postModLoaderEvent(ReflectorConstructor constr, Object... params) {
        Object object = Reflector.newInstance(constr, params);
        if (object != null) {
            postModLoaderEvent(object);
        }
    }

    public static void postModLoaderEvent(Object event) {
        if (event != null) {
            Object object = Reflector.ModLoader_get.call();
            if (object != null) {
                Reflector.callVoid(object, Reflector.ModLoader_postEvent, event);
            }
        }
    }
}