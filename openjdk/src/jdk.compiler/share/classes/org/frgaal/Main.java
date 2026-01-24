/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.frgaal;

import java.io.PrintWriter;
import java.lang.reflect.Method;

/**
 *
 * @author frgaal
 */
public class Main extends AbstractMain {
    public static void main(String... args) {
        System.exit(compile(args));
    }

    public static int compile(String[] args) {
        return doRun(cl -> {
            Class<?> javacMain = cl.loadClass("com.sun.tools.javac.Main");
            Method compile = javacMain.getDeclaredMethod("compile2", String[].class);
            return (int) compile.invoke(null, (Object) args);
        });
    }

    public static int compile(String[] args, PrintWriter out) {
        return doRun(cl -> {
            Class<?> javacMain = cl.loadClass("com.sun.tools.javac.Main");
            Method compile = javacMain.getDeclaredMethod("compile2", String[].class, PrintWriter.class);
            return (int) compile.invoke(null, args, out);
        });
    }

}
