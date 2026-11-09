package sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import sandbox.GreetingCli.CliResult;

/**
 * Characterization of GreetingCli before Feature 16 ({@code --lang=<code>} and unknown options):
 * pins today's behaviour of the calls the Feature 16 design keeps (repeated {@code --lang}, a
 * flag taken as the {@code --lang} value, single-dash arguments). Written before the change;
 * approved with the plan, protected path, the implementer must not change it.
 *
 * <p>It goes through {@code run} because some pinned calls end with exit code 2, and {@code
 * main} would call {@code System.exit}.
 */
class GreetingCliOptionsCharacterizationTest {

  private static void assertResult(
      String stdout, String stderr, int exitCode, boolean utf8, CliResult actual) {
    assertEquals(stdout, actual.stdout(), "stdout");
    assertEquals(stderr, actual.stderr(), "stderr");
    assertEquals(exitCode, actual.exitCode(), "exit code");
    assertEquals(utf8, actual.utf8Stdout(), "UTF-8 stdout");
  }

  private static CliResult run(String... args) {
    return GreetingCli.run(args);
  }

  /** Design row 7: the last {@code --lang} wins, an overridden unknown code is not an error. */
  @Test
  void lastLangWinsOverAnUnknownCode() {
    assertResult("¡Hola, Ana!", "", 0, true, run("Ana", "--lang", "xx", "--lang", "es"));
  }

  /** Design row 7: only the last {@code --lang} is validated, here it has no value. */
  @Test
  void trailingLangWithoutValueIsMissingValue() {
    assertResult(
        "",
        "Missing value for --lang (supported: en, es)",
        2,
        false,
        run("Ana", "--lang", "xx", "--lang"));
  }

  /** Design row 8: the word after {@code --lang} is always the code, even if it is a flag. */
  @Test
  void flagAfterLangIsTakenAsTheCode() {
    assertResult(
        "",
        "Unknown language: --shout (supported: en, es)",
        2,
        false,
        run("Ana", "--lang", "--shout"));
  }

  /** Design row 14: a single-dash argument after the name is a loose argument, ignored. */
  @Test
  void singleDashArgumentAfterNameIsIgnored() {
    assertResult("Hello, Ana!", "", 0, false, run("Ana", "-x"));
  }

  /** Design row 14: a single-dash first argument is still a name. */
  @Test
  void singleDashFirstArgumentIsAName() {
    assertResult("Hello, -x!", "", 0, false, run("-x"));
  }
}
