package sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Characterization of GreetingCli before Feature 9 (--lang): pins today's behaviour of every
 * call without --lang. Written before the change; approved with the plan, protected path, the
 * implementer must not change it.
 *
 * <p>It goes through the public {@code main}, the entry point the welcome scripts use. A call
 * that returns normally has not called {@code System.exit}, so it ends with exit code 0.
 */
class GreetingCliCharacterizationTest {

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

  private static String line(String text) {
    return text + System.lineSeparator();
  }

  @Test
  void greetsByName() {
    assertEquals(line("Hello, Ana!"), runMain("Ana"));
  }

  @Test
  void stripsTheName() {
    assertEquals(line("Hello, Ana!"), runMain("  Ana  "));
  }

  @Test
  void shoutsWhenSecondArgumentIsShout() {
    assertEquals(line("HELLO, ANA!"), runMain("Ana", "--shout"));
  }

  @Test
  void shoutKeepsAccentedLetters() {
    assertEquals(line("HELLO, JOS\u00c9!"), runMain("Jos\u00e9", "--shout"));
  }

  @Test
  void ignoresUnknownSecondArgument() {
    assertEquals(line("Hello, Ana!"), runMain("Ana", "foo"));
  }

  @Test
  void shoutCountsOnlyRightAfterTheName() {
    assertEquals(line("Hello, Ana!"), runMain("Ana", "foo", "--shout"));
  }

  @Test
  void repeatedShoutStillShouts() {
    assertEquals(line("HELLO, ANA!"), runMain("Ana", "--shout", "--shout"));
  }

  /** Folded in from the bugfix 10 characterization: inner dashes do not make a flag. */
  @Test
  void nameWithInnerDashesIsStillAName() {
    assertEquals(line("Hello, Ana-María!"), runMain("Ana-María"));
  }

  @Test
  void printsUsageWithNoArguments() {
    assertUsage(runMain());
  }

  @Test
  void printsUsageWithBlankName() {
    assertUsage(runMain("   "));
  }

  /**
   * Feature 9 adds {@code [--lang en|es]} to the usage text on purpose, so only its start and
   * that it is a single line are pinned.
   */
  private static void assertUsage(String printed) {
    assertTrue(
        printed.startsWith("Usage: greet <name> [--shout]"), "usage text, got: " + printed);
    assertTrue(printed.endsWith(System.lineSeparator()), "ends with a newline");
    assertEquals(
        1, printed.split(System.lineSeparator(), -1).length - 1, "a single line, got: " + printed);
  }
}
