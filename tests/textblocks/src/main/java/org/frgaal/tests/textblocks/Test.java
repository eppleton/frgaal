package org.frgaal.tests.textblocks;

public class Test {
    public static void main(String... args) {
        System.out.println("""
                           Hello, world!
                           continuation\
                           continuation\
                           space\s\s\send
                           space-continuation\s\s\s\
                           end
                           """);
    }
}
