package net.minecraft.world.level.storage.loot;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;

public class LootParams {
    private final ServerLevel level;
    private final ContextMap params;
    private final Map<ResourceLocation, LootParams.DynamicDrop> dynamicDrops;
    private final float luck;

    public LootParams(ServerLevel pLevel, ContextMap pParams, Map<ResourceLocation, LootParams.DynamicDrop> pDynamicDrops, float pLuck) {
        this.level = pLevel;
        this.params = pParams;
        this.dynamicDrops = pDynamicDrops;
        this.luck = pLuck;
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public ContextMap contextMap() {
        return this.params;
    }

    public void addDynamicDrops(ResourceLocation pLocation, Consumer<ItemStack> pConsumer) {
        LootParams.DynamicDrop lootparams$dynamicdrop = this.dynamicDrops.get(pLocation);
        if (lootparams$dynamicdrop != null) {
            lootparams$dynamicdrop.add(pConsumer);
        }
    }

    public float getLuck() {
        return this.luck;
    }

    public static class Builder {
        private final ServerLevel level;
        private final ContextMap.Builder params = new ContextMap.Builder();
        private final Map<ResourceLocation, LootParams.DynamicDrop> dynamicDrops = Maps.newHashMap();
        private float luck;

        public Builder(ServerLevel pLevel) {
            this.level = pLevel;
        }

        public ServerLevel getLevel() {
            return this.level;
        }

        public <T> LootParams.Builder withParameter(ContextKey<T> pParamater, T pValue) {
            this.params.withParameter(pParamater, pValue);
            return this;
        }

        public <T> LootParams.Builder withOptionalParameter(ContextKey<T> pParameter, @Nullable T pValue) {
            this.params.withOptionalParameter(pParameter, pValue);
            return this;
        }

        public <T> T getParameter(ContextKey<T> pParameter) {
            return this.params.getParameter(pParameter);
        }

        @Nullable
        public <T> T getOptionalParameter(ContextKey<T> pParameter) {
            return this.params.getOptionalParameter(pParameter);
        }

        public LootParams.Builder withDynamicDrop(ResourceLocation pName, LootParams.DynamicDrop pDynamicDrop) {
            LootParams.DynamicDrop lootparams$dynamicdrop = this.dynamicDrops.put(pName, pDynamicDrop);
            if (lootparams$dynamicdrop != null) {
                throw new IllegalStateException("Duplicated dynamic drop '" + this.dynamicDrops + "'");
            } else {
                return this;
            }
        }

        public LootParams.Builder withLuck(float pLuck) {
            this.luck = pLuck;
            return this;
        }

        public LootParams create(ContextKeySet pContextKeySet) {
            ContextMap contextmap = this.params.create(pContextKeySet);
            return new LootParams(this.level, contextmap, this.dynamicDrops, this.luck);
        }
    }

    @FunctionalInterface
    public interface DynamicDrop {
        void add(Consumer<ItemStack> pOutput);
    }
}