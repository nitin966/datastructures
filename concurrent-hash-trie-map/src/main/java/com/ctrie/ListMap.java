package com.ctrie;

import java.util.LinkedHashMap;
import java.util.Map;

public class ListMap<K, V> extends LinkedHashMap<K, V> {
    public ListMap() {
        super();
    }

    public ListMap(int initialCapacity) {
        super(initialCapacity);
    }

    public ListMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public ListMap(Map<? extends K, ? extends V> m) {
        super(m);
    }

    public static <K, V> ListMap<K, V> of(K k1, V v1) {
        ListMap<K, V> map = new ListMap<>();
        map.put(k1, v1);
        return map;
    }

    public static <K, V> ListMap<K, V> of(K k1, V v1, K k2, V v2) {
        ListMap<K, V> map = new ListMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    public static <K, V> ListMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        ListMap<K, V> map = new ListMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    public static <K, V> ListMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        ListMap<K, V> map = new ListMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }
}

