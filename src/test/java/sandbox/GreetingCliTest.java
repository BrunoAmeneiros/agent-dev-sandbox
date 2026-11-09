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
  private static final String UNKNOWN = " (supported: --lang <code>, --lang=<code>, --shout)";
  private static final String UNKNOWN_LANG = " (supported: en, es)";
  private static final String MISSING_VALUE = "Missing value for --lang (supported: en, es)";
  private static final String HELLO_ANA_ES = "\u00a1Hola, Ana!";

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
    assertResult("\u00a1HOLA, ANA!", "", 0, true, run("Ana", "--lang", "es", "--shout"));
  }

  @Test
  void shoutsInSpanishShoutFirst() {
    assertResult("\u00a1HOLA, ANA!", "", 0, true, run("Ana", "--shout", "--lang", "es"));
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
  void trailingLangAfterUnknownCodeIsMissingValue() {
    assertResult(
        "",
        "Missing value for --lang (supported: en, es)",
        2,
        false,
        run("Ana", "--lang", "xx", "--lang"));
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
        "\u00a1HOLA, JOS\u00c9!",
        "",
        0,
        true,
        run("Jos\u00e9", "--lang", "es", "--shout"));
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
  void singleDashIsStillAName() {
    assertResult("Hello, -x!", "", 0, false, run("-x"));
  }

  @Test
  void innerDoubleDashIsStillAName() {
    assertResult("Hello, Ana--!", "", 0, false, run("Ana--"));
  }

  @Test
  void langEqualsGreetsInSpanish() {
    assertResult(HELLO_ANA_ES, "", 0, true, run("Ana", "--lang=es"));
  }

  @Test
  void langEqualsCodeIgnoresCase() {
    assertResult(HELLO_ANA_ES, "", 0, true, run("Ana", "--lang=ES"));
  }

  @Test
  void langEqualsUnknownCode() {
    assertResult("", "Unknown language: xx" + UNKNOWN_LANG, 2, false, run("Ana", "--lang=xx"));
  }

  @Test
  void langEqualsEmptyIsMissingValue() {
    assertResult("", MISSING_VALUE, 2, false, run("Ana", "--lang="));
  }

  @Test
  void langEqualsSplitsAtFirstEquals() {
    assertResult("", "Unknown language: es=x" + UNKNOWN_LANG, 2, false, run("Ana", "--lang=es=x"));
  }

  @Test
  void upperCaseLangEqualsIsUnknownOption() {
    assertResult("", "Unknown option: --LANG=es" + UNKNOWN, 2, false, run("Ana", "--LANG=es"));
  }

  @Test
  void upperCaseLangIsUnknownOption() {
    assertResult("", "Unknown option: --LANG" + UNKNOWN, 2, false, run("Ana", "--LANG", "es"));
  }

  @Test
  void lastLangWinsAcrossForms() {
    assertResult(HELLO_ANA_ES, "", 0, true, run("Ana", "--lang=xx", "--lang", "es"));
    assertResult(HELLO_ANA_ES, "", 0, true, run("Ana", "--lang", "xx", "--lang=es"));
  }

  @Test
  void shoutWithValueIsUnknownOption() {
    assertResult("", "Unknown option: --shout=yes" + UNKNOWN, 2, false, run("Ana", "--shout=yes"));
  }

  @Test
  void shoutWithEmptyValueIsUnknownOption() {
    assertResult("", "Unknown option: --shout=" + UNKNOWN, 2, false, run("Ana", "--shout="));
  }

  @Test
  void unknownOptionAfterName() {
    assertResult("", "Unknown option: --foo" + UNKNOWN, 2, false, run("Ana", "--foo"));
  }

  @Test
  void misspelledLangIsUnknownOption() {
    assertResult("", "Unknown option: --lnag" + UNKNOWN, 2, false, run("Ana", "--lnag", "es"));
  }

  @Test
  void firstUnknownOptionIsReported() {
    assertResult("", "Unknown option: --foo" + UNKNOWN, 2, false, run("Ana", "--foo", "--bar"));
  }

  @Test
  void unknownLanguageBeforeUnknownOption() {
    assertResult("", "Unknown language: xx" + UNKNOWN_LANG, 2, false, run("Ana", "--lang", "xx", "--foo"));
  }

  @Test
  void overriddenLangIsNotAProblem() {
    assertResult("", "Unknown option: --foo" + UNKNOWN, 2, false, run("Ana", "--lang", "xx", "--lang", "es", "--foo"));
  }

  @Test
  void unknownOptionBeforeUnknownLanguage() {
    assertResult("", "Unknown option: --foo" + UNKNOWN, 2, false, run("Ana", "--foo", "--lang", "xx"));
  }

  @Test
  void doubleDashAfterNameIsUnknownOption() {
    assertResult("", "Unknown option: --" + UNKNOWN, 2, false, run("Ana", "--"));
  }

  @Test
  void langEqualsFirstIsNameNotFirst() {
    assertResult("", "The name must come first, got: --lang=es" + HINT, 2, false, run("--lang=es", "Ana"));
    assertResult("", "The name must come first, got: --lang=es" + HINT, 2, false, run("--lang=es"));
  }

  @Test
  void unknownOptionFirst() {
    assertResult("", "Unknown option: --foo" + UNKNOWN, 2, false, run("--foo", "Ana"));
    assertResult("", "Unknown option: --foo" + UNKNOWN, 2, false, run("--foo"));
    assertResult("", "Unknown option: --foo" + UNKNOWN, 2, false, run("  --foo  ", "Ana"));
  }

  @Test
  void doubleDashAloneIsUnknownOption() {
    assertResult("", "Unknown option: --" + UNKNOWN, 2, false, run("--"));
  }

  @Test
  void unknownOptionFirstBeatsLang() {
    assertResult("", "Unknown option: --foo" + UNKNOWN, 2, false, run("--foo", "Ana", "--lang", "es"));
  }

  @Test
  void langEqualsValueTakenAsIs() {
    assertResult("", "Unknown language: --shout" + UNKNOWN_LANG, 2, false, run("Ana", "--lang=--shout"));
  }

  @Test
  void langEqualsWithShout() {
    assertResult("¡HOLA, ANA!", "", 0, true, run("Ana", "--lang=es", "--shout"));
    assertResult("¡HOLA, ANA!", "", 0, true, run("Ana", "--shout", "--lang=es"));
  }

  @Test
  void unknownOptionBeatsUsage() {
    assertResult("", "Unknown option: --foo" + UNKNOWN, 2, false, run("--shout", "Ana", "--foo"));
    assertResult("", "Unknown option: --foo" + UNKNOWN, 2, false, run("--shout", "--foo"));
  }

  @Test
  void unknownOptionBeatsUsageForBlankName() {
    assertResult("", "Unknown option: --foo" + UNKNOWN, 2, false, run("", "--foo"));
    assertResult("", "Unknown option: --foo" + UNKNOWN, 2, false, run("   ", "Ana", "--foo"));
  }

  @Test
  void firstArgumentErrorBeatsUnknownOption() {
    assertResult("", "Missing name" + HINT, 2, false, run("", "--foo", "--lang", "es"));
    assertResult("", "The name must come first, got: --shout" + HINT, 2, false, run("--shout", "Ana", "--lang", "es", "--foo"));
  }

  @Test
  void upperCaseShoutIsUnknownOption() {
    assertResult("", "Unknown option: --SHOUT" + UNKNOWN, 2, false, run("Ana", "--SHOUT"));
  }

  @Test
  void emptyArgumentAfterNameIsIgnored() {
    assertResult("Hello, Ana!", "", 0, false, run("Ana", ""));
  }

  @Test
  void langEqualsValueIsNotStripped() {
    assertResult("", "Unknown language:  es" + UNKNOWN_LANG, 2, false, run("Ana", "--lang= es"));
  }

  @Test
  void emptyLangEqualsFirstIsNameNotFirst() {
    assertResult("", "The name must come first, got: --lang=" + HINT, 2, false, run("--lang=", "Ana"));
  }

  @Test
  void trailingLangAfterLangEqualsIsMissingValue() {
    assertResult("", MISSING_VALUE, 2, false, run("Ana", "--lang=es", "--lang"));
  }
}
