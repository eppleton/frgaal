package org.frgaal.test.records;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import org.junit.Test;
import org.frgaal.test.TestBase;
import static org.junit.Assert.assertEquals;

public class BasicRecordTest extends TestBase {

    @Test
    public void testCanCompileRecordToJDK8() throws Exception {
        Path recordSource = writeFile("Test.java",
                                      """
                                      package p;
                                      public record Test(String str, Integer bi, boolean z, byte b, char c, short s, int i, long l, float f, double d) {
                                          public static void main(String... args) {
                                              Extra.main(args);
                                          }
                                      }
                                      """);
        Path extraSource = writeFile("Extra.java",
                                     """
                                     package p;
                                     import java.util.Objects;
                                     public class Extra {
                                         public static void main(String... args) {
                                             Test t1 = new Test("test", 1, true, (byte) 2, 'a', (short) 3, 4, 5, 6.5f, 7.5);
                                             Test t2 = new Test("test", 1, true, (byte) 2, 'a', (short) 3, 4, 5, 6.5f, 7.5);
                                             if (t1.hashCode() != t2.hashCode()) {
                                                 throw new AssertionError("The hashCodes are not the same.");
                                             }
                                             if (!Objects.equals(t1, t2)) {
                                                 throw new AssertionError("The objects are not equals to each other.");
                                             }
                                             System.out.println(t1.hashCode());
                                             System.out.println(t1);
                                         }
                                     }
                                     """);
        StringWriter out = new StringWriter();
        PrintWriter pout = new PrintWriter(out);
        int compilationResult = org.frgaal.Main.compile(new String[] {"-d", classDir.toString(), "-target", "8", recordSource.toString(), extraSource.toString()}, pout);
        pout.close();
        assertEquals(out.toString(), 0, compilationResult);
        buildJar();
        for (File jdk : new File[] {jdk8, jdkCurrent}) {
            run(jdk, "p.Test", "-246372219\nTest[str=test, bi=1, z=true, b=2, c=a, s=3, i=4, l=5, f=6.5, d=7.5]\n", 0);
        }
    }

    @Test
    public void testNoTargetDir() throws Exception {
        Path recordSource = writeFile("Test.java",
                                      """
                                      package p;
                                      public record Test(String str, Integer bi, boolean z, byte b, char c, short s, int i, long l, float f, double d) {}
                                      """);
        StringWriter out = new StringWriter();
        PrintWriter pout = new PrintWriter(out);
        int compilationResult = org.frgaal.Main.compile(new String[] {"-XDrawDiagnostics", "-target", "8", recordSource.toString()}, pout);
        pout.close();
        assertEquals(out.toString(), 0, compilationResult);
        assertEquals("""
                     - compiler.warn.no.target.no.multirelease
                     1 warning
                     """, out.toString());
    }

    @Test
    public void testSerializableRecords() throws Exception {
        Path recordSource = writeFile("Test.java",
                                      """
                                      package p;
                                      public record Test(int i) implements java.io.Serializable {
                                      }
                                      """);
        StringWriter out = new StringWriter();
        PrintWriter pout = new PrintWriter(out);
        int compilationResult = org.frgaal.Main.compile(new String[] {"-d", classDir.toString(), "-XDrawDiagnostics", "-target", "8", recordSource.toString()}, pout);
        pout.close();
        assertEquals(out.toString(), 1, compilationResult);
        assertEquals("""
                     Test.java:2:8: compiler.err.serializable.records
                     1 error
                     """,
                    out.toString());
    }

}
