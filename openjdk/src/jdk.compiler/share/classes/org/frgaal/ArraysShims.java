/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.frgaal;

import java.util.Comparator;
import java.util.Objects;

public class ArraysShims {
    public static <T> int compare(T[] a, T[] b,
                                  Comparator<? super T> cmp) {
        Objects.requireNonNull(cmp);
        if (a == b)
            return 0;
        if (a == null || b == null)
            return a == null ? -1 : 1;

        int length = Math.min(a.length, b.length);
        for (int i = 0; i < length; i++) {
            T oa = a[i];
            T ob = b[i];
            if (oa != ob) {
                // Null-value comparison is deferred to the comparator
                int v = cmp.compare(oa, ob);
                if (v != 0) {
                    return v;
                }
            }
        }

        return a.length - b.length;
    }
}
