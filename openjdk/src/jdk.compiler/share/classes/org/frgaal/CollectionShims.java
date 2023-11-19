/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.frgaal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author frgaal
 */
public class CollectionShims {

    @SafeVarargs
    public static <T> List<T> list(T... values) {
        return Collections.unmodifiableList(Arrays.asList(values.clone()));
    }

    public static <T> List<T> listCopyOf(List<T> from) {
        return (List<T>) list(from.toArray());
    }

    @SafeVarargs
    public static <T> Set<T> set(T... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }

    public static <T> Set<T> setCopyOf(Set<? extends T> from) {
        return (Set<T>) set(from.toArray());
    }

    public static <K, V> Map<K, V> map() {
        return Collections.emptyMap();
    }

    public static <K, V> Map<K, V> map(K key, V value) {
        return Collections.singletonMap(key, value);
    }

    public static <K, V> Map<K, V> mapCopyOf(Map<K, V> from) {
        return Collections.unmodifiableMap(new HashMap<>(from));
    }
    public static <K, V> Map.Entry<K, V> mapEntry(K k, V v) {
        record EntryImpl<K, V>(K k, V v) implements Map.Entry<K, V> {
            public EntryImpl {
                Objects.requireNonNull(k);
                Objects.requireNonNull(v);
            }
            public K getKey() { return k(); }
            public V getValue() { return v(); }
            public V setValue(V v) { throw new UnsupportedOperationException(); }
        }
        return new EntryImpl<K, V>(k, v);
    }
}
