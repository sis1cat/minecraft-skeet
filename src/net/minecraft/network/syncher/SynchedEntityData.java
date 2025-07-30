package net.minecraft.network.syncher;

import com.mojang.logging.LogUtils;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.ClassTreeIdRegistry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.optifine.util.BiomeUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;

public class SynchedEntityData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_ID_VALUE = 254;
    static final ClassTreeIdRegistry ID_REGISTRY = new ClassTreeIdRegistry();
    private final SyncedDataHolder entity;
    private final SynchedEntityData.DataItem<?>[] itemsById;
    private boolean isDirty;
    public Biome spawnBiome = BiomeUtils.PLAINS;
    public BlockPos spawnPosition = BlockPos.ZERO;
    public BlockState blockStateOn = Blocks.AIR.defaultBlockState();
    public long blockStateOnUpdateMs = 0L;
    public Map<String, Object> modelVariables;
    public CompoundTag nbtTag;
    public long nbtTagUpdateMs = 0L;

    SynchedEntityData(SyncedDataHolder pEntity, SynchedEntityData.DataItem<?>[] pItemsById) {
        this.entity = pEntity;
        this.itemsById = pItemsById;
    }

    public static <T> EntityDataAccessor<T> defineId(Class<? extends SyncedDataHolder> pClazz, EntityDataSerializer<T> pSerializer) {
        if (LOGGER.isDebugEnabled()) {
            try {
                Class<?> oclass = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName());
                if (!oclass.equals(pClazz)) {
                    LOGGER.debug("defineId called for: {} from {}", pClazz, oclass, new RuntimeException());
                }
            } catch (ClassNotFoundException classnotfoundexception) {
            }
        }

        int i = ID_REGISTRY.define(pClazz);
        if (i > 254) {
            throw new IllegalArgumentException("Data value id is too big with " + i + "! (Max is 254)");
        } else {
            return pSerializer.createAccessor(i);
        }
    }

    private <T> SynchedEntityData.DataItem<T> getItem(EntityDataAccessor<T> pKey) {
        return (SynchedEntityData.DataItem<T>)this.itemsById[pKey.id()];
    }

    public <T> T get(EntityDataAccessor<T> pKey) {
        return this.getItem(pKey).getValue();
    }

    public <T> void set(EntityDataAccessor<T> pKey, T pValue) {
        this.set(pKey, pValue, false);
    }

    public <T> void set(EntityDataAccessor<T> pKey, T pValue, boolean pForce) {
        SynchedEntityData.DataItem<T> dataitem = this.getItem(pKey);
        if (pForce || ObjectUtils.notEqual(pValue, dataitem.getValue())) {
            dataitem.setValue(pValue);
            this.entity.onSyncedDataUpdated(pKey);
            dataitem.setDirty(true);
            this.isDirty = true;
            this.nbtTag = null;
        }
    }

    public boolean isDirty() {
        return this.isDirty;
    }

    @Nullable
    public List<SynchedEntityData.DataValue<?>> packDirty() {
        if (!this.isDirty) {
            return null;
        } else {
            this.isDirty = false;
            List<SynchedEntityData.DataValue<?>> list = new ArrayList<>();

            for (SynchedEntityData.DataItem<?> dataitem : this.itemsById) {
                if (dataitem.isDirty()) {
                    dataitem.setDirty(false);
                    list.add(dataitem.value());
                }
            }

            return list;
        }
    }

    @Nullable
    public List<SynchedEntityData.DataValue<?>> getNonDefaultValues() {
        List<SynchedEntityData.DataValue<?>> list = null;

        for (SynchedEntityData.DataItem<?> dataitem : this.itemsById) {
            if (!dataitem.isSetToDefault()) {
                if (list == null) {
                    list = new ArrayList<>();
                }

                list.add(dataitem.value());
            }
        }

        return list;
    }

    public void assignValues(List<SynchedEntityData.DataValue<?>> pEntries) {
        for (SynchedEntityData.DataValue<?> datavalue : pEntries) {
            SynchedEntityData.DataItem<?> dataitem = this.itemsById[datavalue.id];
            this.assignValue(dataitem, datavalue);
            this.entity.onSyncedDataUpdated(dataitem.getAccessor());
            this.nbtTag = null;
        }

        this.entity.onSyncedDataUpdated(pEntries);
    }

    private <T> void assignValue(SynchedEntityData.DataItem<T> pTarget, SynchedEntityData.DataValue<?> pEntry) {
        if (!Objects.equals(pEntry.serializer(), pTarget.accessor.serializer())) {
            /*throw new IllegalStateException(
                String.format(
                    Locale.ROOT,
                    "Invalid entity data item type for field %d on entity %s: old=%s(%s), new=%s(%s)",
                    pTarget.accessor.id(),
                    this.entity,
                    pTarget.value,
                    pTarget.value.getClass(),
                    pEntry.value,
                    pEntry.value.getClass()
                )
            );*/
        } else {
            pTarget.setValue((T)pEntry.value);
        }
    }

    public static class Builder {
        private final SyncedDataHolder entity;
        private final SynchedEntityData.DataItem<?>[] itemsById;

        public Builder(SyncedDataHolder pEntity) {
            this.entity = pEntity;
            this.itemsById = new SynchedEntityData.DataItem[SynchedEntityData.ID_REGISTRY.getCount(pEntity.getClass())];
        }

        public <T> SynchedEntityData.Builder define(EntityDataAccessor<T> pAccessor, T pValue) {
            int i = pAccessor.id();
            if (i > this.itemsById.length) {
                throw new IllegalArgumentException("Data value id is too big with " + i + "! (Max is " + this.itemsById.length + ")");
            } else if (this.itemsById[i] != null) {
                throw new IllegalArgumentException("Duplicate id value for " + i + "!");
            } else if (EntityDataSerializers.getSerializedId(pAccessor.serializer()) < 0) {
                throw new IllegalArgumentException("Unregistered serializer " + pAccessor.serializer() + " for " + i + "!");
            } else {
                this.itemsById[pAccessor.id()] = new SynchedEntityData.DataItem<>(pAccessor, pValue);
                return this;
            }
        }

        public SynchedEntityData build() {
            for (int i = 0; i < this.itemsById.length; i++) {
                if (this.itemsById[i] == null) {
                    throw new IllegalStateException("Entity " + this.entity.getClass() + " has not defined synched data value " + i);
                }
            }

            return new SynchedEntityData(this.entity, this.itemsById);
        }
    }

    public static class DataItem<T> {
        final EntityDataAccessor<T> accessor;
        T value;
        private final T initialValue;
        private boolean dirty;

        public DataItem(EntityDataAccessor<T> pAccessor, T pValue) {
            this.accessor = pAccessor;
            this.initialValue = pValue;
            this.value = pValue;
        }

        public EntityDataAccessor<T> getAccessor() {
            return this.accessor;
        }

        public void setValue(T pValue) {
            this.value = pValue;
        }

        public T getValue() {
            return this.value;
        }

        public boolean isDirty() {
            return this.dirty;
        }

        public void setDirty(boolean pDirty) {
            this.dirty = pDirty;
        }

        public boolean isSetToDefault() {
            return this.initialValue.equals(this.value);
        }

        public SynchedEntityData.DataValue<T> value() {
            return SynchedEntityData.DataValue.create(this.accessor, this.value);
        }
    }

    public static record DataValue<T>(int id, EntityDataSerializer<T> serializer, T value) {
        public static <T> SynchedEntityData.DataValue<T> create(EntityDataAccessor<T> pDataAccessor, T pValue) {
            EntityDataSerializer<T> entitydataserializer = pDataAccessor.serializer();
            return new SynchedEntityData.DataValue<>(pDataAccessor.id(), entitydataserializer, entitydataserializer.copy(pValue));
        }

        public void write(RegistryFriendlyByteBuf pBuffer) {
            int i = EntityDataSerializers.getSerializedId(this.serializer);
            if (i < 0) {
                throw new EncoderException("Unknown serializer type " + this.serializer);
            } else {
                pBuffer.writeByte(this.id);
                pBuffer.writeVarInt(i);
                this.serializer.codec().encode(pBuffer, this.value);
            }
        }

        public static SynchedEntityData.DataValue<?> read(RegistryFriendlyByteBuf pBuffer, int pId) {
            int i = pBuffer.readVarInt();
            EntityDataSerializer<?> entitydataserializer = EntityDataSerializers.getSerializer(i);
            if (entitydataserializer == null) {
                throw new DecoderException("Unknown serializer type " + i);
            } else {
                return read(pBuffer, pId, entitydataserializer);
            }
        }

        private static <T> SynchedEntityData.DataValue<T> read(RegistryFriendlyByteBuf pBuffer, int pId, EntityDataSerializer<T> pSerializer) {
            return new SynchedEntityData.DataValue<>(pId, pSerializer, pSerializer.codec().decode(pBuffer));
        }
    }
}