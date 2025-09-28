package net.optifine.util;

import java.util.Map;
import java.util.Set;

public abstract class AbstractMapMap<K1, K2, V> {
    private Map<K1, Map<K2, V>> mapMap = this.makeMap();

    abstract Map makeMap();

    public V put(K1 key1, K2 key2, V val) {
        Map<K2, V> map = this.mapMap.get(key1);
        if (map == null) {
            map = this.makeMap();
            this.mapMap.put(key1, map);
        }

        return map.put(key2, val);
    }

    public V get(K1 key1, K2 key2) {
        Map<K2, V> map = this.mapMap.get(key1);
        return map == null ? null : map.get(key2);
    }

    public Map<K2, V> get(K1 key1) {
        return this.mapMap.get(key1);
    }

    public Set<K2> keySet(K1 key1) {
        Map<K2, V> map = this.mapMap.get(key1);
        return map == null ? Set.of() : map.keySet();
    }

    public boolean containsKey(K1 key1) {
        Map<K2, V> map = this.mapMap.get(key1);
        return map == null ? false : !map.isEmpty();
    }

    public boolean containsKey(K1 key1, K2 key2) {
        Map<K2, V> map = this.mapMap.get(key1);
        return map == null ? false : map.containsKey(key2);
    }
}