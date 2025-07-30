package net.minecraft.core;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

public class IdMapper<T> implements IdMap<T> {
    private int nextId;
    private final Reference2IntMap<T> tToId;
    private final List<T> idToT;

    public IdMapper() {
        this(512);
    }

    public IdMapper(int pExpectedSize) {
        this.idToT = Lists.newArrayListWithExpectedSize(pExpectedSize);
        this.tToId = new Reference2IntOpenHashMap<>(pExpectedSize);
        this.tToId.defaultReturnValue(-1);
    }

    public void addMapping(T pKey, int pValue) {
        this.tToId.put(pKey, pValue);

        while (this.idToT.size() <= pValue) {
            this.idToT.add(null);
        }

        this.idToT.set(pValue, pKey);
        if (this.nextId <= pValue) {
            this.nextId = pValue + 1;
        }
    }

    public void add(T pKey) {
        this.addMapping(pKey, this.nextId);
    }

    @Override
    public int getId(T p_122663_) {
        return this.tToId.getInt(p_122663_);
    }

    @Nullable
    @Override
    public final T byId(int p_122661_) {
        return p_122661_ >= 0 && p_122661_ < this.idToT.size() ? this.idToT.get(p_122661_) : null;
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.filter(this.idToT.iterator(), Objects::nonNull);
    }

    public boolean contains(int pId) {
        return this.byId(pId) != null;
    }

    @Override
    public int size() {
        return this.tToId.size();
    }
}