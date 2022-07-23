import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ListClasses {
    public static void main(String... args) throws IOException {
        Path root = Paths.get(".");
        Files.find(root, Integer.MAX_VALUE, (p, a) -> a.isRegularFile() && p.getFileName().toString().endsWith("Test.java")).map(p -> root.relativize(p).toString().replaceAll(".java$", "").replace("/", ".")).forEach(p -> System.out.println(p));
    }
}
