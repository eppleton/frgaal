package org.frgaal.tests.javadoc;

/**
 * Test comment.
 */
public class Javadoc {
    /**
     * test
     */
    public static boolean testMethod1(Object o) {
        return o instanceof String s && """
                                        """.equals(s);
    }

    /**
     * test
     */
    public static boolean testMethod2(Object o) {
        return o instanceof String s && """
                                        """.equals(s);
    }
}
