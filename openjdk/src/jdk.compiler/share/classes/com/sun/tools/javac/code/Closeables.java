package com.sun.tools.javac.code;

import com.sun.tools.javac.util.*;
import java.io.Closeable;

/**
 *
 * @author frgaal
 */
public class Closeables {

    public static final Context.Key<Closeables> closeablesKey = new Context.Key<>();

    public static Closeables instance(Context context) {
        Closeables instance = context.get(closeablesKey);
        if (instance == null)
            instance = new Closeables(context);
        return instance;
    }

    public List<Closeable> closeables = List.nil();

    public Closeables(Context context) {
        context.put(closeablesKey, this);
    }
}
