/*
 * Copyright (c)  RWTH-LeckerSchmecker
 */

package util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.function.BiFunction;

public class MultiKeyMap<K1, K2, V> {

    private final LinkedHashMap<K1, V> valuesByK1 = new LinkedHashMap<>();
    private final LinkedHashMap<K2, V> valuesByK2 = new LinkedHashMap<>();

    public void put(K1 key1, K2 key2, V value) {
        this.valuesByK1.put(key1, value);
        this.valuesByK2.put(key2, value);
    }

    public V remove(K1 key1, K2 key2) {
        this.valuesByK2.remove(key2);
        return this.valuesByK1.remove(key1);
    }

    public V get1(K1 key) {
        return this.valuesByK1.get(key);
    }

    public V get2(K2 key) {
        return this.valuesByK2.get(key);
    }

    public boolean containsKey1(K1 key) {
        return this.valuesByK1.containsKey(key);
    }

    public boolean containsKey2(K2 key) {
        return this.valuesByK2.containsKey(key);
    }

    public V compueIfAbsent(K1 key1, K2 key2, BiFunction<K1, K2, V> mappingFunction) {
        V value = mappingFunction.apply(key1, key2);
        this.valuesByK2.computeIfAbsent(key2, k -> value);
        this.valuesByK1.computeIfAbsent(key1, k -> value);
        return value;
    }

    public Collection<V> values() {
        return this.valuesByK1.values();
    }

    public int size() {
        return this.valuesByK1.size();
    }
}
