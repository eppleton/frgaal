package org.frgaal.tests.patterns;

public class Test {
    public static void main(String... args) {
        test("a");
        test(1);
    }
    private static void test(Object o) {
        if (o instanceof String s) {
            System.err.println("String: " + s);
        } else {
            System.err.println("Other.");
        }
    }
}
