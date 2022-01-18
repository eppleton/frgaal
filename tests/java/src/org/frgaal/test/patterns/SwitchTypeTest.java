package org.frgaal.test.patterns;

import java.nio.file.Path;
import org.junit.Test;
import org.frgaal.test.TestBase;

public class SwitchTypeTest extends TestBase {

    @Test
    public void testCanCompileTypeTestSwitchToJDK8() throws Exception {
        Path recordSource = writeFile("Test.java",
                                      """
                                      package p;
                                      public class Test {
                                          public static void main(String... args) {
                                              System.out.println(categorize(""));
                                              System.out.println(categorize(-1));
                                              System.out.println(categorize(1));
                                              System.out.println(categorize(null));
                                              System.out.println(categorize(1.0));
                                          }
                                          private static int categorize(Object o) {
                                              return switch (o) {
                                                  case String s -> 0;
                                                  case Integer i && i < 0 -> 1;
                                                  case Integer i -> 2;
                                                  case null -> 3;
                                                  case Object x -> 4;
                                              };
                                          }
                                      }
                                      """);
        runCompareTest("-d", classDir.toString(), "--enable-preview", "-source", "17", "-target", "8", recordSource.toString());
    }

    @Test
    public void testCanCompileTypeTestSwitchToJDK8WithEnum() throws Exception {
        Path recordSource = writeFile("Test.java",
                                      """
                                      package p;
                                      public class Test {
                                          public static void main(String... args) {
                                              System.out.println(categorize(E.A));
                                              System.out.println(categorize(E.B));
                                              System.out.println(categorize(E.C));
                                              System.out.println(categorize(null));
                                              System.out.println(categorize(E.D));
                                          }
                                          private static int categorize(E e) {
                                              return switch (e) {
                                                  case A -> 0;
                                                  case E n && n == E.B -> 1;
                                                  case C -> 2;
                                                  case null -> 3;
                                                  case E x -> 4;
                                              };
                                          }
                                          enum E {
                                              A, B, C, D;
                                          }
                                      }
                                      """);
        runCompareTest("-d", classDir.toString(), "--enable-preview", "-source", "17", "-target", "8", recordSource.toString());
    }

    @Test
    public void testCanCompileTypeTestSwitchToJDK8Default() throws Exception {
        Path recordSource = writeFile("Test.java",
                                      """
                                      package p;
                                      public class Test {
                                          public static void main(String... args) {
                                              System.out.println(categorize(""));
                                              System.out.println(categorize(-1));
                                              System.out.println(categorize(1));
                                              System.out.println(categorize(null));
                                              System.out.println(categorize(1.0));
                                          }
                                          private static int categorize(Object o) {
                                              return switch (o) {
                                                  case default -> 4;
                                                  case String s -> 0;
                                                  case Integer i && i < 0 -> 1;
                                                  case Integer i -> 2;
                                                  case null -> 3;
                                              };
                                          }
                                      }
                                      """);
        runCompareTest("-d", classDir.toString(), "--enable-preview", "-source", "17", "-target", "8", recordSource.toString());
    }

}
