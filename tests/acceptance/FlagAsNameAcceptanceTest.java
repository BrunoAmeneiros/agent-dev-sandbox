import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 *
 * <p>Feature 9 adds {@code [--lang en|es]} to the usage text, so only its start and that it is a
 * single line are checked.
 */
class FlagAsNameAcceptanceTest {

  private static final String USAGE_START = "Usage: greet <name> [--shout]";

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
    assertUsage(runMain("--shout", "Ana"));
  }

  @Test
  void flagAlonePrintsUsage() {
    assertUsage(runMain("--shout"));
  }

  @Test
  void flagWithLeadingSpacesPrintsUsage() {
    assertUsage(runMain("  --shout", "Ana"));
  }

  @Test
  void nameStillGreets() {
    assertEquals("Hello, Ana!" + System.lineSeparator(), runMain("Ana"));
  }

  private static void assertUsage(String printed) {
    assertTrue(printed.startsWith(USAGE_START), "usage text, got: " + printed);
    assertEquals(
        1, printed.split(System.lineSeparator(), -1).length - 1, "a single line, got: " + printed);
  }
}
