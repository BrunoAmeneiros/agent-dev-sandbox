package sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import sandbox.GreetingCli.CliResult;

/**
 * Tests of the command line parsing: bugfix 10 (a first argument starting with {@code --} is a
 * flag, not a name) and Feature 9 (the {@code --lang} option, the error exit code and the UTF-8
 * output flag).
 */
class GreetingCliTest {

  private static final String USAGE = "Usage: greet <name> [--shout] [--lang en|es]";
  private static final String HINT = " (usage: greet <name> [--shout] [--lang en|es])";
  private static final String HELLO_ANA_ES = "00a1Hola, Ana!";

  private static void assertResult(
      String stdout, String stderr, int exitCode, boolean utf8, CliResult actual) {
    assertEquals(stdout, actual.stdout());
    assertEquals(stderr, actual.stderr());
    assertEquals(exitCode, actual.exitCode());
    assertEquals(utf8, actual.utf8Stdout());
  }

  private static CliResult run(String... args) {
    return GreetingCli.run(args);
  }

  @Test
  void greetsInEnglishByDefault() {
    assertResult("Hello, Ana!", "", 0, false, run("Ana"));
  }

  @Test
  void shoutsInEnglish() {
    assertResult("HELLO, ANA!", "", 0, false, run("Ana", "--shout"));
  }

  @Test
  void oldRuleShoutOnlySecondArgument() {
    assertResult("Hello, Ana!", "", 0, false, run("Ana", "foo", "--shout"));
  }

  @Test
  void greetsInSpanish() {
    assertResult(HELLO_ANA_ES, "", 0, true, run("Ana", "--lang", "es"));
  }

  @Test
  void shoutsInSpanishLangFirst() {
    assertResult("00a1HOLA, ANA!", "", 0, true, run("Ana", "--lang", "es", "--shout"));
  }

  @Test
  void shoutsInSpanishShoutFirst() {
    assertResult("00a1HOLA, ANA!", "", 0, true, run("Ana", "--shout", "--lang", "es"));
  }

  @Test
  void explicitEnglish() {
    assertResult("Hello, Ana!", "", 0, false, run("Ana", "--lang", "en"));
  }

  @Test
  void unknownLanguage() {
    assertResult(
        "", "Unknown language: xx (supported: en, es)", 2, false, run("Ana", "--lang", "xx"));
  }

  @Test
  void missingLangValue() {
    assertResult(
        "", "Missing value for --lang (supported: en, es)", 2, false, run("Ana", "--lang"));
  }

  @Test
  void usageWithoutArguments() {
    assertResult(USAGE, "", 0, false, run());
  }

  @Test
  void usageWithBlankName() {
    assertResult(USAGE, "", 0, false, run("   "));
  }

  @Test
  void stripsNameWithLang() {
    assertResult(HELLO_ANA_ES, "", 0, true, run("  Ana  ", "--lang", "es"));
  }

  @Test
  void langCodeIsCaseInsensitive() {
    assertResult(HELLO_ANA_ES, "", 0, true, run("Ana", "--lang", "ES"));
  }

  @Test
  void lastLangWins() {
    assertResult(HELLO_ANA_ES, "", 0, true, run("Ana", "--lang", "xx", "--lang", "es"));
  }

  @Test
  void langTakesNextWordEvenIfFlag() {
    assertResult(
        "",
        "Unknown language: --shout (supported: en, es)",
        2,
        false,
        run("Ana", "--lang", "--shout"));
  }

  @Test
  void spanishShoutKeepsAccents() {
    assertResult(
        "00a1HOLA, JOS00c9!",
        "",
        0,
        true,
        run("Jos00e9", "--lang", "es", "--shout"));
  }

  @Test
  void unknownWordsIgnoredWithLang() {
    assertResult(HELLO_ANA_ES, "", 0, true, run("Ana", "foo", "--lang", "es"));
  }

  @Test
  void blankNameWithLangFails() {
    assertResult("", "Missing name" + HINT, 2, false, run("", "--lang", "es"));
  }

  @Test
  void blankNameBeatsUnknownLanguage() {
    assertResult("", "Missing name" + HINT, 2, false, run("   ", "--lang", "xx"));
  }

  @Test
  void langBeforeNameFails() {
    assertResult(
        "",
        "The name must come first, got: --lang" + HINT,
        2,
        false,
        run("  --lang  ", "es", "Ana"));
  }

  @Test
  void flagBeforeNameWithLangFails() {
    assertResult(
        "",
        "The name must come first, got: --shout" + HINT,
        2,
        false,
        run("--shout", "Ana", "--lang", "es"));
  }

  @Test
  void flagBeforeNamePrintsUsage() {
    assertResult(USAGE, "", 0, false, run("--shout", "Ana"));
  }

  @Test
  void flagAlonePrintsUsage() {
    assertResult(USAGE, "", 0, false, run("--shout"));
  }

  @Test
  void strippedFlagPrintsUsage() {
    assertResult(USAGE, "", 0, false, run("  --shout  ", "Ana"));
  }

  @Test
  void doubleDashAlonePrintsUsage() {
    assertResult(USAGE, "", 0, false, run("--"));
  }

  @Test
  void singleDashIsStillAName() {
    assertResult("Hello, -x!", "", 0, false, run("-x"));
  }

  @Test
  void innerDoubleDashIsStillAName() {
    assertResult("Hello, Ana--!", "", 0, false, run("Ana--"));
  }
}
