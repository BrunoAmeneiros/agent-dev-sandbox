package sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import sandbox.GreetingCli.CliResult;

/**
 * Characterization of GreetingCli before Feature 18 ({@code --help}): pins what the design keeps
 * and the change can break (rows 6, 7, 9, 10, 11, 12 and 14, precedence without {@code --help},
 * the bugfix 10 usage text). Goes through {@code run} because some calls end with exit code 2.
 */
class GreetingCliHelpCharacterizationTest {

  private static final String SUPPORTED = " (supported: --lang <code>, --lang=<code>, --shout)";
  private static final String HINT = " (usage: greet <name> [--shout] [--lang en|es])";
  private static final String USAGE = "Usage: greet <name> [--shout] [--lang en|es]";

  private static void assertResult(String stdout, String stderr, int exitCode, CliResult actual) {
    assertEquals(stdout, actual.stdout(), "stdout");
    assertEquals(stderr, actual.stderr(), "stderr");
    assertEquals(exitCode, actual.exitCode(), "exit code");
    assertEquals(false, actual.utf8Stdout(), "UTF-8 stdout");
  }

  private static CliResult run(String... args) {
    return GreetingCli.run(args);
  }

  /** Row 6: {@code --help} after {@code --lang} is its value. */
  @Test
  void helpAsLangValueIsUnknownLanguage() {
    assertResult(
        "", "Unknown language: --help (supported: en, es)", 2, run("Ana", "--lang", "--help"));
  }

  /** Row 7: {@code --lang=--help} is an unknown language. */
  @Test
  void helpGluedToLangIsUnknownLanguage() {
    assertResult(
        "", "Unknown language: --help (supported: en, es)", 2, run("Ana", "--lang=--help"));
  }

  /** Row 9: without a name, {@code --lang --help} fails on the name. */
  @Test
  void langHelpWithoutNameMustPutNameFirst() {
    assertResult(
        "", "The name must come first, got: --lang" + HINT, 2, run("--lang", "--help"));
  }

  /** Row 10: other cases of {@code --help} are unknown options. */
  @Test
  void upperAndMixedCaseHelpAreUnknownOptions() {
    assertResult("", "Unknown option: --HELP" + SUPPORTED, 2, run("Ana", "--HELP"));
    assertResult("", "Unknown option: --Help" + SUPPORTED, 2, run("Ana", "--Help"));
  }

  /** Row 11: {@code --help=value} is an unknown option. */
  @Test
  void helpWithValueIsUnknownOption() {
    assertResult("", "Unknown option: --help=yes" + SUPPORTED, 2, run("Ana", "--help=yes"));
    assertResult("", "Unknown option: --help=" + SUPPORTED, 2, run("Ana", "--help="));
  }

  /** Row 12: single-dash and bare words are loose arguments. */
  @Test
  void singleDashAndBareHelpWordsAreIgnored() {
    assertResult("Hello, Ana!", "", 0, run("Ana", "-h"));
    assertResult("Hello, Ana!", "", 0, run("Ana", "-help"));
    assertResult("Hello, Ana!", "", 0, run("Ana", "help"));
  }

  /** Row 14: after the name nothing is stripped, so " --help" is a loose argument. */
  @Test
  void paddedHelpAfterNameIsIgnored() {
    assertResult("Hello, Ana!", "", 0, run("Ana", " --help"));
  }

  /** Precedence without {@code --help}: unknown options in first position and after the name. */
  @Test
  void unknownOptionsWithoutHelpKeepTheirError() {
    assertResult("", "Unknown option: --foo" + SUPPORTED, 2, run("--foo"));
    assertResult("", "Unknown option: --foo" + SUPPORTED, 2, run("Ana", "--foo"));
    assertResult("", "Unknown option: --foo" + SUPPORTED, 2, run("--shout", "--foo"));
  }

  /** Precedence: the first problem from left to right between an unknown option and --lang. */
  @Test
  void firstProblemLeftToRightWithoutHelp() {
    assertResult(
        "", "Unknown option: --foo" + SUPPORTED, 2, run("Ana", "--foo", "--lang", "xx"));
    assertResult(
        "", "Unknown language: xx (supported: en, es)", 2, run("Ana", "--lang", "xx", "--foo"));
  }

  /** Review case: in first-argument path a help right after --lang is its value. */
  @Test
  void helpAfterLangInFirstArgumentPathStillFailsOnTheName() {
    assertResult(
        "",
        "The name must come first, got: --shout" + HINT,
        2,
        run("--shout", "--lang", "--lang", "--help"));
  }

  /** Bugfix 10 usage text stays for a known flag in first position. */
  @Test
  void usageTextStaysForKnownFirstFlag() {
    assertResult(USAGE, "", 0, run("--shout", "Ana"));
    assertResult(USAGE, "", 0, run("--shout"));
  }
}
