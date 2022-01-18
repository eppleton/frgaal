/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.frgaal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    @SafeVarargs
    public static <T> Set<T> set(T... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }

    public static <K, V> Map<K, V> map() {
        return Collections.emptyMap();
    }
}
