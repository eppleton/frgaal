package org.frgaal.test.lombok;

import java.nio.file.Path;
import org.junit.Test;
import org.frgaal.test.TestBase;

import static org.junit.Assert.assertEquals;

public class LombokTest extends TestBase {

    @Test
    public void testLombokAndMultiRelease() throws Exception {
        Path recordSource = writeFile("Test.java",
                                      """
                                      package p;
                                      public record Test(int i) {
                                      }
                                      """);
        Output output = compile("-d", classDir.toString(), "-cp", System.getProperty("lombok.jar"), "-proc:full", "-source", "25", "-target", "8", recordSource.toString());
        System.err.println(output.output());
        int exitCode = output.exitCode();
        assertEquals(0, exitCode);
    }

}
