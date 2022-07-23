package org.frgaal.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestBase {
    protected static final File jdk8 = new File(System.getProperty("java.home.8"));
    protected static final File jdkCurrent = new File(System.getProperty("java.home.current"));

    @Rule
    public TestName testName = new TestName();

    protected void runCompareTest(String... args) throws Exception {
        String[] newArgs = Arrays.copyOf(args, args.length);
        String source = null;
        for (int i = 0; i < newArgs.length; i++) {
            if ("-source".equals(newArgs[i])) {
                source = newArgs[i + 1];
            }
            if ("-target".equals(newArgs[i])) {
                newArgs[i + 1] = source;
            }
        }
        Output output = run(new File(new File(jdkCurrent, "bin"), "javac").getAbsolutePath(), newArgs);

        assertEquals(output.output(), 0, output.exitCode());
        buildJar();

        String expectedOutput = runAndGetOutput(jdkCurrent, "p.Test", 0);

        deleteRecursivelly(classDir);
        try (StringWriter out = new StringWriter();
             PrintWriter pout = new PrintWriter(out)) {
            int compilationResult = org.frgaal.Main.compile(args, pout);
            pout.close();
            assertEquals(out.toString(), 0, compilationResult);
            buildJar();
            for (File jdk : new File[] {jdk8, jdkCurrent}) {
                run(jdk, "p.Test", expectedOutput, 0);
            }
        }
    }

    private static Path allTestTempDir;
    private Path tempDir;
    private Path srcDir;
    protected Path classDir;
    private Path outputJar;

    protected Path writeFile(String name, String content) throws IOException {
        Path file = srcDir.resolve(name);
        try (OutputStream out = Files.newOutputStream(file)) {
            out.write(content.getBytes());
        }
        return file;
    }

    protected void buildJar() throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new Attributes.Name("Multi-Release"), "true");
        try (OutputStream out = Files.newOutputStream(outputJar = tempDir.resolve("test.jar"));
             JarOutputStream jos = new JarOutputStream(out, manifest)) {
            Files.find(classDir, Integer.MAX_VALUE, (p, attr) -> true)
                 .forEach(p -> {
                     if (classDir.equals(p)) {
                         return ;
                     }
                     try {
                         String relPath = classDir.relativize(p).toString();
                         if (Files.isDirectory(p)) {
                             jos.putNextEntry(new ZipEntry(relPath + "/"));
                         } else {
                             jos.putNextEntry(new ZipEntry(relPath));
                             try (InputStream in = Files.newInputStream(p)) {
                                 int r;

                                 while ((r = in.read()) != (-1)) {
                                     jos.write(r);
                                 }
                             }
                         }
                     } catch (IOException ex) {
                     }
                 });
        }
    }

    protected Output run(File jdk, String className) throws IOException, InterruptedException {
        return run(new File(new File(jdk, "bin"), "java").getAbsolutePath(), "-classpath", outputJar.toString(), className);
    }

    protected void run(File jdk, String className, String expectedOutput, int expectedResult) throws IOException, InterruptedException {
        Output output = run(new File(new File(jdk, "bin"), "java").getAbsolutePath(), "-classpath", outputJar.toString(), className);
        int actualResult = output.exitCode();
        String actualOutput = output.output();
        assertEquals(actualOutput, expectedResult, actualResult);
        assertEquals(expectedOutput, actualOutput);
    }

    private String runAndGetOutput(File jdk, String className, int expectedResult) throws IOException, InterruptedException {
        Output output = run(new File(new File(jdk, "bin"), "java").getAbsolutePath(), "-classpath", outputJar.toString(), "--enable-preview", className);
        int actualResult = output.exitCode();
        String actualOutput = output.output();
        assertEquals(actualOutput, expectedResult, actualResult);
        return actualOutput;
    }

    private Output run(String what, String... args) throws IOException, InterruptedException {
        List<String> allArgs = new ArrayList<>();
        allArgs.add(what);
        allArgs.addAll(Arrays.asList(args));
        Process p = new ProcessBuilder(allArgs).start();
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        Thread copyOut = copyStream(p.getInputStream(), data);
        Thread copyErr = copyStream(p.getErrorStream(), data);
        int actualResult = p.waitFor();
        copyOut.join();
        copyErr.join();
        return new Output(actualResult, data.toString());
    }

    protected Output compile(String... args) {
        StringWriter out = new StringWriter();
        PrintWriter pout = new PrintWriter(out);
        int compilationResult = org.frgaal.Main.compile(args, pout);
        pout.close();
        return new Output(compilationResult, out.toString());
    }

    private Thread copyStream(InputStream input, ByteArrayOutputStream output) {
        Thread copyThread = new Thread(() -> {
            try {
                int r;

                while ((r = input.read()) != (-1)) {
                    output.write(r);
                }
            } catch (IOException ex) {
                PrintStream ps = new PrintStream(output);
                ex.printStackTrace(ps);
                ps.flush();
            }
        });
        copyThread.start();
        return copyThread;
    }

    @Before
    public void setUpTempDir() throws IOException {
        tempDir = allTestTempDir.resolve(testName.getMethodName());
        srcDir = tempDir.resolve("src");
        classDir = tempDir.resolve("classes");
        Files.createDirectories(srcDir);
        Files.createDirectories(classDir);
    }

    @BeforeClass
    public static void setUpAllTestTempDir() throws IOException {
        allTestTempDir = Files.createTempDirectory("frgaal-test");
        assertTrue(new File(new File(jdk8, "bin"), "java").canExecute());
        assertTrue(new File(new File(jdkCurrent, "bin"), "java").canExecute());
    }

    @AfterClass
    public static void deleteAllTestTempDir() throws IOException {
        deleteRecursivelly(allTestTempDir);
    }

    private static void deleteRecursivelly(Path p) throws IOException {
        if (Files.isDirectory(p)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(p)) {
                for (Path c : ds) {
                    deleteRecursivelly(c);
                }
            }
        }
        Files.delete(p);
    }

    public record Output(int exitCode, String output) {
        public void assertOutput(int expectedExitCode, String expectedOutput) {
            assertEquals(output, expectedExitCode, exitCode);
            assertEquals(output, expectedOutput, output);
        }
        public void assertOutputContains(int expectedExitCode, String expectedOutputContains) {
            assertEquals(output, expectedExitCode, exitCode);
            assertTrue(output, output.contains(expectedOutputContains));
        }
    }
}
