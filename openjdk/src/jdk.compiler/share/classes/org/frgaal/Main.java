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
import java.util.Arrays;

/**
 *
 * @author frgaal
 */
public class Main {
    public static void main(String... args) {
        System.setProperty("frgaal.disable.java.class.path", "true"); //do not pick up frgaal classpath
        try {
            Class<?> javacMain = getFrgaalCL().loadClass("com.sun.tools.javac.Main");
            Method compile = javacMain.getDeclaredMethod("compile", String[].class);
            System.exit((int) compile.invoke(null, (Object) args));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            System.err.println("An internal error occurred, please report a bug:");
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public static int compile(String[] args, PrintWriter out) {
        System.setProperty("frgaal.disable.java.class.path", "true"); //do not pick up frgaal classpath
        try {
            Class<?> javacMain = getFrgaalCL().loadClass("com.sun.tools.javac.Main");
            Method compile = javacMain.getDeclaredMethod("compile", String[].class, PrintWriter.class);
            return (int) compile.invoke(null, args, out);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            System.err.println("An internal error occurred, please report a bug:");
            ex.printStackTrace();
            return 1;
        }
    }

    private static ClassLoader FRGAAL_CL;

    private static synchronized ClassLoader getFrgaalCL() {
        if (FRGAAL_CL == null) {
            CodeSource cs = Main.class.getProtectionDomain().getCodeSource();
            URL location = cs != null ? cs.getLocation() : null;
            if (location == null) {
                throw new IllegalStateException("Cannot find frgaal compiler, stopping.");
            }
            final ClassLoader parent = Main.class.getClassLoader().getParent();
            FRGAAL_CL = new URLClassLoader(new URL[] {location}, new FilteringCL(parent));
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
                name.startsWith("sun.reflect.annotation.")) {
                throw new ClassNotFoundException();
            }
            return super.loadClass(name, resolve);
        }
        
    }
}
