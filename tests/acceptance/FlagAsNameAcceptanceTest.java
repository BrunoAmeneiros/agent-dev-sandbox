import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sandbox.GreetingCli;

/**
 * Approved acceptance test (bugfix 10): protected path, the implementer must not change it. A
 * first argument that starts with {@code --}, after stripping, is not a name: the CLI prints the
 * usage text.
 */
class FlagAsNameAcceptanceTest {

  private static final String USAGE = "Usage: greet <name> [--shout]" + System.lineSeparator();

  private final PrintStream originalOut = System.out;
  private final ByteArrayOutputStream out = new ByteArrayOutputStream();

  @BeforeEach
  void captureStdout() {
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
  }

  @AfterEach
  void restoreStdout() {
    System.setOut(originalOut);
  }

  private String runMain(String... args) {
    GreetingCli.main(args);
    return out.toString(StandardCharsets.UTF_8);
  }

  @Test
  void flagBeforeNamePrintsUsage() {
    assertEquals(USAGE, runMain("--shout", "Ana"));
  }

  @Test
  void flagAlonePrintsUsage() {
    assertEquals(USAGE, runMain("--shout"));
  }

  @Test
  void flagWithLeadingSpacesPrintsUsage() {
    assertEquals(USAGE, runMain("  --shout", "Ana"));
  }

  @Test
  void nameStillGreets() {
    assertEquals("Hello, Ana!" + System.lineSeparator(), runMain("Ana"));
  }
}
