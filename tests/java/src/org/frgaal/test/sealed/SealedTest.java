package org.frgaal.test.sealed;

import java.nio.file.Path;
import org.junit.Test;
import org.frgaal.test.TestBase;

public class SealedTest extends TestBase {

    @Test
    public void testCanCompileSealedToJDK8() throws Exception {
        Path sealedSource = writeFile("Test.java",
                                      """
                                      package p;
                                      public class Test {
                                          sealed interface I {}
                                          final static class C implements I {}
                                          public static void main(String... args) {
                                              I i = new C();
                                          }
                                      }
                                      """);
        runCompareTest("-d", classDir.toString(), "-source", "21", "-target", "8", sealedSource.toString());
    }

    @Test
    public void testViolation1() throws Exception {
        Path baseSource = writeFile("Base.java",
                                    """
                                    package p;
                                    public sealed interface Base {
                                        final class U implements Base {}
                                    }
                                    """);
        this.compile("-d", classDir.toString(), "-source", "21", "-target", "8", baseSource.toString())
            .assertOutput(0, "");
        Path extraSource = writeFile("Extra.java",
                                     """
                                     package p;
                                     public final class Extra implements Base {
                                     }
                                     """);
        this.compile("-XDrawDiagnostics", "-d", classDir.toString(), "-classpath", classDir.toString(), "-source", "21", "-target", "8", extraSource.toString())
            .assertOutput(1,
                          """
                          Extra.java:2:14: compiler.err.cant.inherit.from.sealed: p.Base
                          1 error
                          """);
    }

    @Test
    public void testViolation2() throws Exception {
        Path baseSource = writeFile("Base.java",
                                    """
                                    package p;
                                    public interface Base {
                                        final class U implements Base {}
                                    }
                                    """);
        this.compile("-d", classDir.toString(), "-source", "21", "-target", "8", baseSource.toString())
            .assertOutput(0, "");
        Path extraSource = writeFile("Extra.java",
                                     """
                                     package p;
                                     public final class Extra implements Base {
                                         public void log() {
                                             System.out.println("called.");
                                         }
                                     }
                                     """);
        this.compile("-d", classDir.toString(), "-classpath", classDir.toString(), "-source", "21", "-target", "8", extraSource.toString())
            .assertOutput(0, "");
        /**/ baseSource = writeFile("Base.java",
                                    """
                                    package p;
                                    public sealed interface Base {
                                        final class U implements Base {}
                                        public static void main(String... args) {
                                            new Extra().log();
                                        }
                                    }
                                    """);
        this.compile("-d", classDir.toString(), "-classpath", classDir.toString(), "-source", "21", "-target", "8", baseSource.toString())
            .assertOutput(0, "");
        buildJar();
        this.run(jdk8,
                 "p.Base")
            .assertOutputContains(0, "called.");
        this.run(jdkCurrent,
                 "p.Base")
            .assertOutputContains(1, "class p.Extra cannot implement sealed interface p.Base");
    }

}
