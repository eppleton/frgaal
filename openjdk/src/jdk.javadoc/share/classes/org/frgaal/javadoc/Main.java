/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.frgaal.javadoc;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import org.frgaal.AbstractMain;

/**
 *
 * @author frgaal
 */
public class Main extends AbstractMain {
    public static void main(String... args) {
        System.exit(doRun(cl -> {
            Class<?> javacMain = cl.loadClass("jdk.javadoc.internal.tool.Main");
            Method compile = javacMain.getDeclaredMethod("execute", String[].class);
            return (int) compile.invoke(null, (Object) args);
        }));
    }

    public static int execute(String[] args, PrintWriter out, PrintWriter err) {
        return doRun(cl -> {
            Class<?> javacMain = cl.loadClass("jdk.javadoc.internal.tool.Main");
            Method compile = javacMain.getDeclaredMethod("execute", String[].class, PrintWriter.class, PrintWriter.class);
            return (int) compile.invoke(null, args, out, err);
        });
    }
}
