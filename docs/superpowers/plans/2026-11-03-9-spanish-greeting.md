# Spanish greeting (`--lang es`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `GreetingCli` accepts `--lang en|es`; `--lang es` prints `¡Hola, <name>!` in UTF-8, an unknown or missing language fails with exit code 2 and no greeting, and every call without `--lang` behaves exactly as today.

**Architecture:** A new public `enum Language` (`EN`, `ES`) holds the greeting text and the code lookup. `Greeter` gains `greet(String, Language)`; `greet(String)` delegates to it with `EN`. `GreetingCli.run` returns a package-private record with stdout, stderr, exit code and a UTF-8 flag; `main` prints each part to its stream and calls `System.exit` only for a non-zero code.

**Tech Stack:** Java 21, Maven, JUnit 5. No new dependencies.

**Spec:** [docs/superpowers/specs/2026-11-03-9-spanish-greeting-design.md](../specs/2026-11-03-9-spanish-greeting-design.md) (need: [2026-11-03-9-spanish-greeting-need.md](../specs/2026-11-03-9-spanish-greeting-need.md)).

## Global Constraints

- No new dependencies; do not touch `pom.xml`.
- Every call without `--lang` anywhere in the arguments: same stdout, same encoding (plain `System.out`), exit code 0, as today. Only the usage text changes.
- Non-ASCII characters in Java sources are written as Unicode escapes (`¡` = `\u00a1`); `pom.xml` sets no source encoding.
- Protected, never edited by any Task: `src/test/java/sandbox/GreetingCliCharacterizationTest.java`, `tests/acceptance/SpanishGreetingAcceptanceTest.java`, `tests/acceptance/GreeterAcceptanceTest.java`, `tests/acceptance/GreeterNullAcceptanceTest.java`. If one of them seems wrong, stop and report; do not change it.
- `Greeter.greet(String)` keeps its signature and behaviour (`Hello, <name>!`, `NullPointerException` with message `name`).
- Exact texts:
  - usage: `Usage: greet <name> [--shout] [--lang en|es]`
  - unknown language: `Unknown language: <code> (supported: en, es)` (the code as typed)
  - missing value: `Missing value for --lang (supported: en, es)`
  - error exit code: `2`
- Checkstyle (`checkstyle.xml`) and Javadoc on all public API; CI needs 80 % coverage on changed code.
- Commands (from `docs/project-map.md`): one test `mvn -B test -Dtest=<Class>`; all tests `mvn -B verify -Pacceptance`; lint `mvn -B checkstyle:check spotbugs:check`.

## Review Focus

- `--lang ES` (upper case): accepted as Spanish, since codes are matched case-insensitively. Pinned in Task 3 (`langCodeIsCaseInsensitive`).
- `--lang xx --lang es`: the last `--lang` wins, so it greets in Spanish. Pinned in Task 3 (`lastLangWins`).
- `Ana --lang --shout`: the word after `--lang` is always its value, so this is the unknown-language error for `--shout`. Pinned in Task 3 (`langTakesNextWordEvenIfFlag`).
- `José --lang es --shout`: `¡HOLA, JOSÉ!`, with the accent kept by `toUpperCase(Locale.ROOT)`. Pinned in Task 3 (`spanishShoutKeepsAccents`).
- `Ana foo --lang es`: unknown words are still ignored when `--lang` is present, so this gives `¡Hola, Ana!`. Pinned in Task 3 (`unknownWordsIgnoredWithLang`).

---

### Task 1: Baseline: the protected tests on today's code

**Files:**
- Test (read only): `src/test/java/sandbox/GreetingCliCharacterizationTest.java`, `tests/acceptance/SpanishGreetingAcceptanceTest.java`

**Interfaces:**
- Consumes: today's `GreetingCli.main(String[])`.
- Produces: nothing. No code changes and no commit.

- [ ] **Step 1: Run the characterization test on the current code**

Run: `mvn -B test -Dtest=GreetingCliCharacterizationTest`
Expected: `Tests run: 9, Failures: 0, Errors: 0`. If any of them fails, stop and report: the baseline is wrong, and it is not this Task's job to change the test.

- [ ] **Step 2: Record the acceptance baseline**

Run: `mvn -B verify -Pacceptance`
Expected: unit tests pass. `SpanishGreetingAcceptanceTest` has 3 tests: `greetsInEnglishWithoutLang` passes, and `greetsInSpanishWithLangEs` and `unknownLanguageFailsWithoutGreeting` fail (the feature does not exist yet). The other acceptance tests pass.

### Task 2: `Language` and `Greeter.greet(String, Language)`

**Files:**
- Create: `src/main/java/sandbox/Language.java`
- Modify: `src/main/java/sandbox/Greeter.java`
- Test: `src/test/java/sandbox/LanguageTest.java` (new), `src/test/java/sandbox/GreeterTest.java` (add tests)

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `public enum Language { EN, ES }` in package `sandbox`, with `public static Optional<Language> fromCode(String code)`. It matches `"en"`/`"es"` ignoring case (`Locale.ROOT`), and returns empty for any other value. `null` is rejected with `Objects.requireNonNull(code, "code")`.
  - `public String greet(String name, Language language)` in `Greeter`. `EN` gives `Hello, <name>!` and `ES` gives `¡Hola, <name>!`. `name` is rejected with `requireNonNull(name, "name")` and `language` with `requireNonNull(language, "language")`, in that order.
  - `greet(String)` becomes `return greet(name, Language.EN);`.

- [ ] **Step 1: Write the failing tests**

`LanguageTest`:
- `fromCodeFindsEnglish`: `assertEquals(Optional.of(Language.EN), Language.fromCode("en"))`
- `fromCodeFindsSpanish`: `assertEquals(Optional.of(Language.ES), Language.fromCode("es"))`
- `fromCodeIgnoresCase`: `assertEquals(Optional.of(Language.ES), Language.fromCode("ES"))`
- `fromCodeRejectsUnknown`: `assertEquals(Optional.empty(), Language.fromCode("xx"))` and `assertEquals(Optional.empty(), Language.fromCode(""))`
- `fromCodeRejectsNull`: `NullPointerException` with message `"code"`

`GreeterTest` (add these; keep the existing two tests unchanged):
- `greetsInSpanish`: `assertEquals("\u00a1Hola, Ana!", new Greeter().greet("Ana", Language.ES))`
- `greetsInEnglishExplicitly`: `assertEquals("Hello, Ana!", new Greeter().greet("Ana", Language.EN))`
- `rejectsNullNameWithLanguage`: `greet(null, Language.ES)` throws `NullPointerException` with message `"name"`
- `rejectsNullLanguage`: `greet("Ana", null)` throws `NullPointerException` with message `"language"`

- [ ] **Step 2: Run them to verify they fail**

Run: `mvn -B test -Dtest=LanguageTest+GreeterTest`
Expected: compilation failure, because `Language` does not exist.

- [ ] **Step 3: Implement `Language` and `Greeter.greet(String, Language)`**

Each enum constant carries its greeting text, for example a prefix: `"Hello, "` for `EN` and `"\u00a1Hola, "` for `ES`. The greeting is the prefix + name + `"!"`. Add Javadoc to the enum, its constants, `fromCode` and the new overload.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `mvn -B verify -Pacceptance`
Expected: `LanguageTest`, `GreeterTest`, `GreetingCliCharacterizationTest`, `GreeterAcceptanceTest` and `GreeterNullAcceptanceTest` all pass. `SpanishGreetingAcceptanceTest` still fails on the same 2 tests as in Task 1.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/sandbox/Language.java src/main/java/sandbox/Greeter.java src/test/java/sandbox/LanguageTest.java src/test/java/sandbox/GreeterTest.java
git commit -m "feat: add Language and Spanish greeting to Greeter"
```

### Task 3: `--lang` in the CLI, error exit code, and UTF-8 output

**Files:**
- Modify: `src/main/java/sandbox/GreetingCli.java`
- Test: `src/test/java/sandbox/GreetingCliTest.java` (new)

**Interfaces:**
- Consumes: `Language.fromCode(String)`, `Language.ES`, `Greeter.greet(String, Language)`, `Greeter.greet(String)` (Task 2).
- Produces:
  - `record CliResult(String stdout, String stderr, int exitCode, boolean utf8Stdout)`: package-private, nested in `GreetingCli`. `stdout` and `stderr` hold the text without a line terminator, and an empty string means nothing is printed to that stream.
  - `static CliResult run(String[] args)`: package-private, replacing today's `static String run(String[])`.
  - `public static void main(String[] args)`: same signature as today.

Parsing rules (these come from the spec; implement them exactly):

1. With no arguments, or with `args[0].isBlank()`, `stdout` is the usage text and the exit code is 0.
2. `name = args[0].strip()`.
3. **If no element of `args[1..]` equals `"--lang"`**, apply today's rule: shout only if `args.length > 1 && "--shout".equals(args[1])`. Then `stdout` is `greet(name)`, upper-cased with `toUpperCase(Locale.ROOT)` if it shouts, the exit code is 0 and `utf8Stdout` is false.
4. **Otherwise**, scan `args[1..]` from left to right:
   - `"--lang"` takes the next word as the code, whatever it is (the last one wins). If no word follows, the result is the error "missing value".
   - `"--shout"` anywhere else turns on shouting.
   - Any other word is ignored.
5. If `Language.fromCode(code)` is empty, the result is the error "unknown language" with the code as typed.
6. An error has empty `stdout`, the exact message in `stderr`, exit code `2` and `utf8Stdout` false.
7. A success has `stdout = greet(name, language)` (shouted as in rule 3), exit code 0 and `utf8Stdout = (language == Language.ES)`.

`main`:
- If `stdout` is not empty, print it with `println`. When `utf8Stdout` is true, use a `PrintStream` over `new FileOutputStream(FileDescriptor.out)` with `StandardCharsets.UTF_8`; otherwise use `System.out`. Flush the UTF-8 stream, but do not close it.
- If `stderr` is not empty, print it with `System.err.println`.
- If `exitCode != 0`, call `System.exit(exitCode)`.

- [ ] **Step 1: Write the failing tests**

In `GreetingCliTest` (package `sandbox`), every test calls `GreetingCli.run(new String[] {...})` and checks all four fields of the `CliResult`. Use a helper `assertResult(String stdout, String stderr, int exitCode, boolean utf8, CliResult actual)`.

| Test | args | stdout | stderr | exit | utf8 |
| --- | --- | --- | --- | --- | --- |
| `greetsInEnglishByDefault` | `Ana` | `Hello, Ana!` | `""` | 0 | false |
| `shoutsInEnglish` | `Ana --shout` | `HELLO, ANA!` | `""` | 0 | false |
| `oldRuleShoutOnlySecondArgument` | `Ana foo --shout` | `Hello, Ana!` | `""` | 0 | false |
| `greetsInSpanish` | `Ana --lang es` | `\u00a1Hola, Ana!` | `""` | 0 | true |
| `shoutsInSpanishLangFirst` | `Ana --lang es --shout` | `\u00a1HOLA, ANA!` | `""` | 0 | true |
| `shoutsInSpanishShoutFirst` | `Ana --shout --lang es` | `\u00a1HOLA, ANA!` | `""` | 0 | true |
| `explicitEnglish` | `Ana --lang en` | `Hello, Ana!` | `""` | 0 | false |
| `unknownLanguage` | `Ana --lang xx` | `""` | `Unknown language: xx (supported: en, es)` | 2 | false |
| `missingLangValue` | `Ana --lang` | `""` | `Missing value for --lang (supported: en, es)` | 2 | false |
| `usageWithoutArguments` | (none) | `Usage: greet <name> [--shout] [--lang en\|es]` | `""` | 0 | false |
| `usageWithBlankName` | `"   "` | `Usage: greet <name> [--shout] [--lang en\|es]` | `""` | 0 | false |
| `stripsNameWithLang` | `"  Ana  " --lang es` | `\u00a1Hola, Ana!` | `""` | 0 | true |
| `langCodeIsCaseInsensitive` | `Ana --lang ES` | `\u00a1Hola, Ana!` | `""` | 0 | true |
| `lastLangWins` | `Ana --lang xx --lang es` | `\u00a1Hola, Ana!` | `""` | 0 | true |
| `langTakesNextWordEvenIfFlag` | `Ana --lang --shout` | `""` | `Unknown language: --shout (supported: en, es)` | 2 | false |
| `spanishShoutKeepsAccents` | `Jos\u00e9 --lang es --shout` | `\u00a1HOLA, JOS\u00c9!` | `""` | 0 | true |
| `unknownWordsIgnoredWithLang` | `Ana foo --lang es` | `\u00a1Hola, Ana!` | `""` | 0 | true |

- [ ] **Step 2: Run them to verify they fail**

Run: `mvn -B test -Dtest=GreetingCliTest`
Expected: compilation failure, because `CliResult` does not exist.

- [ ] **Step 3: Implement `CliResult`, `run(String[]) -> CliResult` and the new `main` in `GreetingCli.java`**

Follow the parsing rules and the `main` rules above. Update the class and method Javadoc (`@param args` mentions `--lang`).

- [ ] **Step 4: Run the unit tests**

Run: `mvn -B test -Dtest=GreetingCliTest+GreetingCliCharacterizationTest`
Expected: `GreetingCliTest` 17/17 and `GreetingCliCharacterizationTest` 9/9 pass.

- [ ] **Step 5: Run everything, including acceptance**

Run: `mvn -B verify -Pacceptance`
Expected: all tests pass, including the 3 in `SpanishGreetingAcceptanceTest`.

- [ ] **Step 6: Run lint**

Run: `mvn -B checkstyle:check spotbugs:check`
Expected: `BUILD SUCCESS`. SpotBugs may report `DM_EXIT` for `System.exit` in `main`. If it does, stop and report it. Do not add exclusions or suppressions: that is a decision for the person.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/sandbox/GreetingCli.java src/test/java/sandbox/GreetingCliTest.java
git commit -m "feat: add --lang to the CLI with error exit code and UTF-8 Spanish output"
```
