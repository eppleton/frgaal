package org.frgaal.tests.ctsym;

import com.sun.management.OperatingSystemMXBean;
import java.util.jar.Pack200;

public class Test {
    public static void main(OperatingSystemMXBean m) {
        Pack200 p;
        System.getSecurityManager().checkTopLevelWindow(null);
        System.err.println(m.getFreePhysicalMemorySize());
    }
}
