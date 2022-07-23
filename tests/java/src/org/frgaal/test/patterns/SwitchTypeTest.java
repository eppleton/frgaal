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
                                                  case Integer i when i < 0 -> 1;
                                                  case Integer i -> 2;
                                                  case null -> 3;
                                                  case Object x -> 4;
                                              };
                                          }
                                      }
                                      """);
        runCompareTest("-d", classDir.toString(), "--enable-preview", "-source", "19", "-target", "8", recordSource.toString());
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
                                                  case C -> 2;
                                                  case E n when n == E.B -> 1;
                                                  case null -> 3;
                                                  case E x -> 4;
                                              };
                                          }
                                          enum E {
                                              A, B, C, D;
                                          }
                                      }
                                      """);
        runCompareTest("-d", classDir.toString(), "--enable-preview", "-source", "19", "-target", "8", recordSource.toString());
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
                                                  case Integer i when i < 0 -> 1;
                                                  case Integer i -> 2;
                                                  case null -> 3;
                                              };
                                          }
                                      }
                                      """);
        runCompareTest("-d", classDir.toString(), "--enable-preview", "-source", "19", "-target", "8", recordSource.toString());
    }

    @Test
    public void testRecordPatterns() throws Exception {
        Path recordSource = writeFile("Test.java",
                                      """
                                      package p;
                                      public class Test {
                                          public static void main(String... args) {
                                              System.out.println(categorize(new R("", "")));
                                              System.out.println(categorize(new R("a", "")));
                                              System.out.println(categorize(new R(1, 2)));
                                              System.out.println(categorize(null));
                                              System.out.println(categorize(1.0));
                                          }
                                          private static int categorize(Object o) {
                                              return switch (o) {
                                                  case R(String s1, String s2) when s1.isEmpty() && s2.isEmpty() -> 0;
                                                  case R(String s1, String s2) -> 1;
                                                  case R(Object o1, Object o2) -> 2;
                                                  case null -> 3;
                                                  case Object x -> 4;
                                              };
                                          }
                                          record R(Object o1, Object o2) {}
                                      }
                                      """);
        runCompareTest("-d", classDir.toString(), "--enable-preview", "-source", "19", "-target", "8", recordSource.toString());
    }

}
