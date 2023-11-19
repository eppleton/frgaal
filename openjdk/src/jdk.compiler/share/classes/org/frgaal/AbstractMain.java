/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.frgaal;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 *
 * @author frgaal
 */
public class AbstractMain {
    protected static int doRun(Run toRun) {
        System.setProperty("frgaal.disable.java.class.path", "true"); //do not pick up frgaal classpath
        try {
            return toRun.run(getFrgaalCL());
        } catch (ReflectiveOperationException | RuntimeException ex) {
            System.err.println("An internal error occurred, please report a bug:");
            ex.printStackTrace();
            return 1;
        }
    }

    public interface Run {
        public int run(ClassLoader loader) throws ReflectiveOperationException;
    }

    private static ClassLoader FRGAAL_CL;

    private static synchronized ClassLoader getFrgaalCL() {
        if (FRGAAL_CL == null) {
            CodeSource cs = AbstractMain.class.getProtectionDomain().getCodeSource();
            URL location = cs != null ? cs.getLocation() : null;
            if (location == null) {
                throw new IllegalStateException("Cannot find frgaal compiler, stopping.");
            }
            List<URL> locations = new ArrayList<>();
            locations.add(location);
            try {
                Class<?> javadocMain = Class.forName("org.frgaal.javadoc.Main", false, AbstractMain.class.getClassLoader());
                cs = javadocMain.getProtectionDomain().getCodeSource();
                location = cs != null ? cs.getLocation() : null;
                if (location != null) {
                    locations.add(location);
                }
            } catch (ClassNotFoundException ignore) {
            }
            final ClassLoader parent = AbstractMain.class.getClassLoader().getParent();
            FRGAAL_CL = new URLClassLoader(locations.toArray(new URL[0]), new FilteringCL(parent));
        }
        return FRGAAL_CL;
    }

    private static final class FilteringCL extends ClassLoader {
        FilteringCL(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("javax.annotation.processing") ||
                name.startsWith("javax.lang.model.") ||
                name.startsWith("javax.tools.") ||
                name.startsWith("com.sun.source.") ||
                name.startsWith("com.sun.tools.javac.") ||
                name.startsWith("com.sun.tools.doclint.") ||
                name.startsWith("jdk.javadoc.") ||
                name.startsWith("sun.reflect.annotation.") ||
                name.startsWith("jdk.internal.opt.")) {
                throw new ClassNotFoundException();
            }
            return super.loadClass(name, resolve);
        }
        
    }
}
