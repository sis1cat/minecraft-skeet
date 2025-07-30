package net.optifine.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CollectionUtils {
    public static boolean noneMatch(Set s1, Set s2) {
        return !anyMatch(s1, s2);
    }

    public static boolean anyMatch(Set s1, Set s2) {
        if (!s1.isEmpty() && !s2.isEmpty()) {
            if (s2.size() < s1.size()) {
                Set set = s1;
                s1 = s2;
                s2 = set;
            }

            for (Object object : s1) {
                if (s2.contains(object)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public static boolean isMutable(Map map) {
        return map == null ? false : isMutableClass(map.getClass());
    }

    public static boolean isMutable(Collection coll) {
        return coll == null ? false : isMutableClass(coll.getClass());
    }

    private static boolean isMutableClass(Class cls) {
        String s = cls.getName();
        return s.contains("Immutable") ? false : !s.contains("Unmodifiable");
    }

    public static Set getKeysLike(Map map, String keyPart) {
        Set set = new LinkedHashSet();

        for (Object object : map.keySet()) {
            String s = String.valueOf(object);
            if (s.contains(keyPart)) {
                set.add(object);
            }
        }

        return set;
    }

    public static Map getMapLike(Map map, String keyPart) {
        Map mapx = new LinkedHashMap();

        for (Object object : map.keySet()) {
            String s = String.valueOf(object);
            if (s.contains(keyPart)) {
                mapx.put(object, map.get(object));
            }
        }

        return mapx;
    }
}