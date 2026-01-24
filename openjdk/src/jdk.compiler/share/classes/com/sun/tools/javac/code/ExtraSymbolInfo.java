/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.code;

import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import java.util.HashMap;
import java.util.Map;

/**
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class ExtraSymbolInfo {
    protected static final Context.Key<ExtraSymbolInfo> extraSymbolInfoHandlerWrapperKey = new Context.Key<>();

    public static ExtraSymbolInfo instance(Context context) {
        ExtraSymbolInfo instance = context.get(extraSymbolInfoHandlerWrapperKey);
        if (instance == null)
            instance = new ExtraSymbolInfo(context);
        return instance;
    }

    private final Check chk;
    private final Map<Symbol, Integer> symbol2RemovedRelease = new HashMap<>();
    private final Map<Symbol, Integer> symbol2DeprecatedRelease = new HashMap<>();

    protected ExtraSymbolInfo(Context context) {
        context.put(extraSymbolInfoHandlerWrapperKey, this);
        this.chk = Check.instance(context);
    }

    public void symbolRemovedInRelease(Symbol s, Integer removed) {
        symbol2RemovedRelease.put(s, removed);
    }

    public void symbolDeprecatedInRelease(Symbol s, Integer removed) {
        symbol2DeprecatedRelease.put(s, removed);
    }

    public void checkSymbolRemovedDeprecatedInFutureRelease(DiagnosticPosition pos, Symbol s) {
        Integer removed = symbol2RemovedRelease.get(s);

        if (removed != null) {
            chk.reportWarningToRemovalHandler(pos, Warnings.SymbolRemovedInFutureVersion(Kinds.kindName(s), s.name, removed));
        } else {
            Integer deprecated = symbol2DeprecatedRelease.get(s);

            if (deprecated != null) {
                chk.reportWarningToDeprecationHandler(pos, Warnings.SymbolDeprecatedInFutureVersion(Kinds.kindName(s), s.name, deprecated));
            }
        }
    }

}
