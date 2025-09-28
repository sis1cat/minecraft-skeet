package net.optifine.entity.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.optifine.Config;
import net.optifine.entity.model.anim.IRenderResolver;
import net.optifine.util.ArrayUtils;
import net.optifine.util.Either;

public abstract class ModelAdapter {
    private Either<EntityType, BlockEntityType> type;
    private String name;
    private String[] aliases;

    public ModelAdapter(EntityType entityType, String name) {
        this(Either.makeLeft(entityType), name);
    }

    public ModelAdapter(BlockEntityType tileEntityType, String name) {
        this(Either.makeRight(tileEntityType), name);
    }

    public ModelAdapter(Either<EntityType, BlockEntityType> type, String name) {
        this.type = type;
        this.name = name;
    }

    public Either<EntityType, BlockEntityType> getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public String[] getAliases() {
        return this.aliases;
    }

    public void setAliases(String... aliases) {
        this.aliases = aliases;
    }

    public void setAlias(String alias) {
        this.aliases = new String[]{alias};
    }

    public abstract Model makeModel();

    public abstract ModelPart getModelRenderer(Model var1, String var2);

    public abstract String[] getModelRendererNames();

    public String[] getIgnoredModelRendererNames() {
        return new String[0];
    }

    public abstract IEntityRenderer makeEntityRender(Model var1, RendererCache var2, int var3);

    public boolean setTextureLocation(IEntityRenderer er, ResourceLocation textureLocation) {
        return false;
    }

    public abstract IRenderResolver getRenderResolver();

    public ModelPart getModelRenderer(ModelPart root, String name, Supplier<ModelPart> supplier) {
        ModelPart modelpart = this.getModelRenderer(root, name);
        return modelpart != null ? modelpart : supplier.get();
    }

    public ModelPart getModelRenderer(ModelPart root, String name) {
        if (root == null) {
            return null;
        } else {
            return Config.equals(root.getName(), name) ? root : root.getChildModelDeep(name);
        }
    }

    public ModelPart[] getModelRenderers(Model model) {
        String[] astring = this.getModelRendererNames();
        List<ModelPart> list = new ArrayList<>();

        for (int i = 0; i < astring.length; i++) {
            String s = astring[i];
            ModelPart modelpart = this.getModelRenderer(model, s);
            if (modelpart != null) {
                list.add(modelpart);
            }
        }

        return list.toArray(new ModelPart[list.size()]);
    }

    public boolean isModelRendererOptional(String name) {
        return ArrayUtils.contains(this.getIgnoredModelRendererNames(), name);
    }

    public static ModelPart bakeModelLayer(ModelLayerLocation loc) {
        return Minecraft.getInstance().getEntityRenderDispatcher().getContext().bakeLayer(loc);
    }

    public BlockEntityRendererProvider.Context getBlockEntityContext() {
        BlockEntityRenderDispatcher blockentityrenderdispatcher = Config.getMinecraft().getBlockEntityRenderDispatcher();
        return blockentityrenderdispatcher.getContext();
    }

    public EntityRendererProvider.Context getEntityContext() {
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        return entityrenderdispatcher.getContext();
    }

    public static String[] toArray(Set<String> set) {
        return set.toArray(new String[set.size()]);
    }

    public static String[] toArray(List<String> list) {
        return list.toArray(new String[list.size()]);
    }

    public static String[] toArray(String[] strs, Set<String> set) {
        Set<String> setx = new LinkedHashSet<>();
        setx.addAll(Arrays.asList(strs));
        setx.addAll(set);
        return toArray(setx);
    }

    public boolean isEntity() {
        return this.type != null && this.type.getLeft().isPresent();
    }

    public boolean isBlockEntity() {
        return this.type != null && this.type.getRight().isPresent();
    }

    @Override
    public String toString() {
        StringBuilder stringbuilder = new StringBuilder();
        if (this.type != null) {
            if (this.type.getLeft().isPresent()) {
                stringbuilder.append("entity: " + this.type.getLeft().get());
            }

            if (this.type.getRight().isPresent()) {
                stringbuilder.append("blockEntity: " + this.type.getLeft().get());
            }
        } else {
            stringbuilder.append("type: null");
        }

        stringbuilder.append(", name: " + this.name);
        if (this.aliases != null && this.aliases.length > 0) {
            stringbuilder.append(", aliases: " + ArrayUtils.arrayToString((Object[])this.aliases));
        }

        return stringbuilder.toString();
    }
}