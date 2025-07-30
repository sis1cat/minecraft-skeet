package net.minecraft.client.resources.model;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.optifine.CustomItems;
import org.slf4j.Logger;

public class ModelDiscovery {
    static final Logger LOGGER = LogUtils.getLogger();
    private final Map<ResourceLocation, UnbakedModel> inputModels;
    final UnbakedModel missingModel;
    private final List<ResolvableModel> topModels = new ArrayList<>();
    private final Map<ResourceLocation, UnbakedModel> referencedModels = new HashMap<>();

    public ModelDiscovery(Map<ResourceLocation, UnbakedModel> pInputModels, UnbakedModel pMissingModel) {
        this.inputModels = pInputModels;
        this.missingModel = pMissingModel;
        this.referencedModels.put(MissingBlockModel.LOCATION, pMissingModel);
    }

    public void registerSpecialModels() {
        this.referencedModels.put(ItemModelGenerator.GENERATED_ITEM_MODEL_ID, new ItemModelGenerator());
    }

    public void addRoot(ResolvableModel pModel) {
        this.topModels.add(pModel);
    }

    public void discoverDependencies() {
        this.topModels.forEach(modelIn -> modelIn.resolveDependencies(new ModelDiscovery.ResolverImpl()));
    }

    public Map<ResourceLocation, UnbakedModel> getReferencedModels() {
        return this.referencedModels;
    }

    public Set<ResourceLocation> getUnreferencedModels() {
        return Sets.difference(this.inputModels.keySet(), this.referencedModels.keySet());
    }

    UnbakedModel getBlockModel(ResourceLocation pModelLocation) {
        return this.referencedModels.computeIfAbsent(pModelLocation, this::loadBlockModel);
    }

    private UnbakedModel loadBlockModel(ResourceLocation pModelLocation) {
        UnbakedModel unbakedmodel = this.inputModels.get(pModelLocation);
        if (unbakedmodel == null) {
            LOGGER.warn("Missing block model: '{}'", pModelLocation);
            return this.missingModel;
        } else {
            return unbakedmodel;
        }
    }

    public void resolveCustomModels() {
        Map<ResourceLocation, Resource> map = CustomItems.getModelResources(false);

        for (ResourceLocation resourcelocation : map.keySet()) {
            ResourceLocation resourcelocation1 = resourcelocation.removePrefixSuffix("models/", ".json");
            ResolvableModel.Resolver resolvablemodel$resolver = new ModelDiscovery.ResolverImpl();
            UnbakedModel unbakedmodel = resolvablemodel$resolver.resolve(resourcelocation1);
        }
    }

    class ResolverImpl implements ResolvableModel.Resolver {
        private final List<ResourceLocation> stack = new ArrayList<>();
        private final Set<ResourceLocation> resolvedModels = new HashSet<>();

        @Override
        public UnbakedModel resolve(ResourceLocation p_360973_) {
            if (this.stack.contains(p_360973_)) {
                ModelDiscovery.LOGGER.warn("Detected model loading loop: {}->{}", this.stacktraceToString(), p_360973_);
                return ModelDiscovery.this.missingModel;
            } else {
                UnbakedModel unbakedmodel = ModelDiscovery.this.getBlockModel(p_360973_);
                if (this.resolvedModels.add(p_360973_)) {
                    this.stack.add(p_360973_);
                    unbakedmodel.resolveDependencies(this);
                    this.stack.remove(p_360973_);
                }

                return unbakedmodel;
            }
        }

        private String stacktraceToString() {
            return this.stack.stream().map(ResourceLocation::toString).collect(Collectors.joining("->"));
        }
    }
}