# `--lang=<code>` and unknown options Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `GreetingCli` accepts `--lang=<code>` exactly like `--lang <code>`, and any argument starting with `--` that is not a known flag fails with `Unknown option: …` and exit code 2, wherever it is; loose words (`Ana foo`) stay ignored.

**Architecture:** Only `GreetingCli.run(String[])` changes: one left-to-right scan of the arguments that classifies each one (known flag, `--lang` value, unknown option, loose word) and reports the first problem. `Greeter` and `Language` are not touched. A change list for operations is added under `docs/changes/`.

**Tech Stack:** Java 21, Maven, JUnit 5. No new dependencies.

**Spec:** [docs/superpowers/specs/2026-11-09-16-lang-equals-syntax-design.md](../specs/2026-11-09-16-lang-equals-syntax-design.md) (need: [2026-11-09-16-lang-equals-syntax-need.md](../specs/2026-11-09-16-lang-equals-syntax-need.md)). The design's "Entradas inválidas y casos límite" table (rows 1–26) is the source of truth for every input.

## Global Constraints

- No new dependencies; do not touch `pom.xml`.
- `GreetingCli.run(String[])` keeps its signature and returns `GreetingCli.CliResult(stdout, stderr, exitCode, utf8Stdout)`: the characterization tests call it.
- Protected, never edited by any Task: `src/test/java/sandbox/GreetingCliCharacterizationTest.java`, `src/test/java/sandbox/GreetingCliOptionsCharacterizationTest.java`, every file under `tests/acceptance/` (including the new `UnknownOptionAcceptanceTest.java`). If one of them seems wrong, stop and report BLOCKED with the clash; never special-case an input to pass a test.
- Known flags (names are case-sensitive): `--shout`; `--lang` (value = next argument, taken even if it starts with `--`); `--lang=<code>` (value = everything after the **first** `=`, taken as is). Any other argument starting with `--` is an unknown option, including `--`, `--shout=`, `--shout=yes`, `--LANG`. A single-dash argument (`-x`) is a loose word.
- A call "has `--lang`" when `args[0].strip()` or any of `args[1..]` is `--lang` or starts with `--lang=`.
- Only `args[0]` is stripped; arguments after the name are used as given (`" --foo"` is a loose word).
- First problem from left to right wins:
  1. `args[0]` (stripped): blank → usage (exit 0) without `--lang`, `Missing name` with `--lang`; unknown option → `Unknown option: <args[0].strip()>`; known flag → usage (exit 0) without `--lang`, `The name must come first, got: <args[0].strip()>` with `--lang`.
  2. When step 1 would give the usage text, scan `args[1..]` first: the first unknown option there gives `Unknown option`, exit 2. No unknown option → the usage text.
  3. After a name, scan `args[1..]`: an unknown option is a problem at its position; only the **last** `--lang` (either form) is validated, at its position: no value (`--lang` last, or `--lang=` empty) → missing value; code not found by `Language.fromCode` → unknown language. An overridden `--lang` is never validated. The argument consumed as the value of `--lang` is never classified.
  4. No problem: greeting as today. Without `--lang`, `--shout` counts only as `args[1]`; with `--lang`, anywhere. `utf8Stdout` is true iff the final language is `ES`.
- Exact texts (stderr, no stdout, exit code `2`):
  - unknown option: `Unknown option: <argument> (supported: --lang <code>, --lang=<code>, --shout)`
  - unknown language: `Unknown language: <code> (supported: en, es)`
  - missing value: `Missing value for --lang (supported: en, es)`
  - missing name: `Missing name (usage: greet <name> [--shout] [--lang en|es])`
  - name not first: `The name must come first, got: <arg> (usage: greet <name> [--shout] [--lang en|es])`
  - usage text (stdout, exit 0) unchanged: `Usage: greet <name> [--shout] [--lang en|es]`
- Non-ASCII characters in Java sources are written as Unicode escapes (`¡` = `¡`).
- Checkstyle (`checkstyle.xml`) and Javadoc on public methods; CI needs 80 % coverage on changed code.
- Commands (from `docs/project-map.md`): one test `mvn -B test -Dtest=<Class>`; one acceptance test `mvn -B verify -Pacceptance -Dit.test=<Class>`; all tests `mvn -B verify -Pacceptance`; lint `mvn -B checkstyle:check spotbugs:check`.
- Each commit's `Co-Authored-By` names the model that made it.

## Review Focus

Inputs the design does not list as rows; each follows from the rules above and is pinned in Task 2:

- `--SHOUT` after the name: flag names are case-sensitive, so `Ana --SHOUT` is `Unknown option: --SHOUT`. Test `upperCaseShoutIsUnknownOption`.
- An empty argument after the name (`Ana ""`): a loose word, ignored → `Hello, Ana!`, exit 0. Test `emptyArgumentAfterNameIsIgnored`.
- A space after `=` (`Ana "--lang= es"`): the value is taken as is → `Unknown language:  es (supported: en, es)` (two spaces). Test `langEqualsValueIsNotStripped`.
- `--lang=` as first argument (`--lang= Ana`): it counts as `--lang` → `The name must come first, got: --lang= (usage: …)`. Test `emptyLangEqualsFirstIsNameNotFirst`.
- `Ana --lang=es --lang`: the last `--lang` has no value → missing value, whatever came before. Test `trailingLangAfterLangEqualsIsMissingValue`.

---

### Task 1: Baseline: the protected tests on today's code

**Type:** chore
**Acceptance criteria:**
- The characterization tests pass on today's code (design, Impact › Characterization needed).
- `UnknownOptionAcceptanceTest` fails on today's code as a class, with the guards passing (design, Criterios de aceptación).
**Security criteria:** None: the design's threats dimension does not apply (local CLI, no network, no sensitive data).
**Acceptance tests:** None: baseline only, no code change.

**Files:**
- Test (read only): `src/test/java/sandbox/GreetingCliCharacterizationTest.java`, `src/test/java/sandbox/GreetingCliOptionsCharacterizationTest.java`, `tests/acceptance/UnknownOptionAcceptanceTest.java`

**Interfaces:**
- Consumes: today's `GreetingCli`.
- Produces: nothing. No code changes and no commit.

- [x] **Step 1: Run the characterization tests**

Run: `mvn -B test -Dtest="GreetingCliCharacterizationTest,GreetingCliOptionsCharacterizationTest"`
Expected: `Tests run: 15, Failures: 0, Errors: 0` (10 + 5). If any fails, stop and report.

- [x] **Step 2: Record the acceptance baseline**

Run: `mvn -B verify -Pacceptance`
Expected: all unit tests and the existing acceptance tests pass. In `UnknownOptionAcceptanceTest`, the guards `looseWordsStillIgnored` and `separateUnknownLanguageStillFails` pass and the other methods fail.

### Task 2: `--lang=<code>` and unknown options in `GreetingCli.run`

**Type:** feature
**Acceptance criteria:**
- Every row 1–26 of the design's "Entradas inválidas y casos límite" gives exactly the stdout, stderr and exit code in the table (design, invalid-input; criterion 6).
- `Ana --lang=es` gives `¡Hola, Ana!` with UTF-8 stdout (criterion 1).
- `Ana --lang=xx` gives the same error as `Ana --lang xx` (criterion 2).
- `Ana --lnag es` and `Ana --shout=yes` give `Unknown option` with the exact text and exit 2 (criterion 3).
- `--foo Ana` and `--shout Ana --foo` give `Unknown option: --foo (…)`, exit 2 (criterion 4).
- `Ana foo` still gives `Hello, Ana!`, exit 0 (criterion 5).
- `GreetingCliTest.doubleDashAlonePrintsUsage` is replaced by `doubleDashAloneIsUnknownOption` (design, Compatibilidad).
- Usage text and `HINT` unchanged (design, Compatibilidad).
- Javadoc of `main` and `run` describes `--lang=<code>` and unknown options (design, Despliegue).
**Security criteria:** None: the design's threats dimension does not apply (local CLI, no network, no sensitive data).
**Acceptance tests:** `tests/acceptance/UnknownOptionAcceptanceTest.java` (all methods).

**Files:**
- Modify: `src/main/java/sandbox/GreetingCli.java` (`run`, constants, Javadoc of `main`)
- Test: `src/test/java/sandbox/GreetingCliTest.java` (add tests; replace `doubleDashAlonePrintsUsage`)

**Interfaces:**
- Consumes: `Language.fromCode(String): Optional<Language>`, `Greeter.greet(String, Language)`, `Greeter.greet(String)`.
- Produces: no new API. `static CliResult run(String[] args)` keeps its signature.

- [ ] **Step 1: Write the failing tests in `GreetingCliTest`**

Use the existing `assertResult(stdout, stderr, exitCode, utf8, run(...))` helper and constants. Add `private static final String UNKNOWN = " (supported: --lang <code>, --lang=<code>, --shout)";` and `private static final String UNKNOWN_LANG = " (supported: en, es)";`. One test per design row not already covered by an existing test or a characterization test, named after the behaviour:

| Test | Call | Expected (stdout, stderr, exit, utf8) |
| --- | --- | --- |
| `langEqualsGreetsInSpanish` | `Ana --lang=es` | `¡Hola, Ana!`, `""`, 0, true |
| `langEqualsCodeIgnoresCase` | `Ana --lang=ES` | `¡Hola, Ana!`, `""`, 0, true |
| `langEqualsUnknownCode` | `Ana --lang=xx` | `""`, `Unknown language: xx` + UNKNOWN_LANG, 2, false |
| `langEqualsEmptyIsMissingValue` | `Ana --lang=` | `""`, `Missing value for --lang (supported: en, es)`, 2, false |
| `langEqualsSplitsAtFirstEquals` | `Ana --lang=es=x` | `""`, `Unknown language: es=x` + UNKNOWN_LANG, 2, false |
| `upperCaseLangEqualsIsUnknownOption` | `Ana --LANG=es` | `""`, `Unknown option: --LANG=es` + UNKNOWN, 2, false |
| `upperCaseLangIsUnknownOption` | `Ana --LANG es` | `""`, `Unknown option: --LANG` + UNKNOWN, 2, false |
| `lastLangWinsAcrossForms` | `Ana --lang=xx --lang es` and `Ana --lang xx --lang=es` | `¡Hola, Ana!`, `""`, 0, true |
| `shoutWithValueIsUnknownOption` | `Ana --shout=yes` | `""`, `Unknown option: --shout=yes` + UNKNOWN, 2, false |
| `shoutWithEmptyValueIsUnknownOption` | `Ana --shout=` | `""`, `Unknown option: --shout=` + UNKNOWN, 2, false |
| `unknownOptionAfterName` | `Ana --foo` | `""`, `Unknown option: --foo` + UNKNOWN, 2, false |
| `misspelledLangIsUnknownOption` | `Ana --lnag es` | `""`, `Unknown option: --lnag` + UNKNOWN, 2, false |
| `firstUnknownOptionIsReported` | `Ana --foo --bar` | `""`, `Unknown option: --foo` + UNKNOWN, 2, false |
| `unknownLanguageBeforeUnknownOption` | `Ana --lang xx --foo` | `""`, `Unknown language: xx` + UNKNOWN_LANG, 2, false |
| `overriddenLangIsNotAProblem` | `Ana --lang xx --lang es --foo` | `""`, `Unknown option: --foo` + UNKNOWN, 2, false |
| `unknownOptionBeforeUnknownLanguage` | `Ana --foo --lang xx` | `""`, `Unknown option: --foo` + UNKNOWN, 2, false |
| `doubleDashAfterNameIsUnknownOption` | `Ana --` | `""`, `Unknown option: --` + UNKNOWN, 2, false |
| `langEqualsFirstIsNameNotFirst` | `--lang=es Ana` and `--lang=es` | `""`, `The name must come first, got: --lang=es` + HINT, 2, false |
| `unknownOptionFirst` | `--foo Ana`, `--foo`, `"  --foo  ", "Ana"` | `""`, `Unknown option: --foo` + UNKNOWN, 2, false |
| `doubleDashAloneIsUnknownOption` (replaces `doubleDashAlonePrintsUsage`) | `--` | `""`, `Unknown option: --` + UNKNOWN, 2, false |
| `unknownOptionFirstBeatsLang` | `--foo Ana --lang es` | `""`, `Unknown option: --foo` + UNKNOWN, 2, false |
| `langEqualsValueTakenAsIs` | `Ana --lang=--shout` | `""`, `Unknown language: --shout` + UNKNOWN_LANG, 2, false |
| `langEqualsWithShout` | `Ana --lang=es --shout` and `Ana --shout --lang=es` | `¡HOLA, ANA!`, `""`, 0, true |
| `unknownOptionBeatsUsage` | `--shout Ana --foo` and `--shout --foo` | `""`, `Unknown option: --foo` + UNKNOWN, 2, false |
| `unknownOptionBeatsUsageForBlankName` | `"", "--foo"` and `"   ", "Ana", "--foo"` | `""`, `Unknown option: --foo` + UNKNOWN, 2, false |
| `firstArgumentErrorBeatsUnknownOption` | `"", "--foo", "--lang", "es"` → `Missing name` + HINT; `--shout Ana --lang es --foo` → `The name must come first, got: --shout` + HINT | 2, false |
| `upperCaseShoutIsUnknownOption` | `Ana --SHOUT` | `""`, `Unknown option: --SHOUT` + UNKNOWN, 2, false |
| `emptyArgumentAfterNameIsIgnored` | `"Ana", ""` | `Hello, Ana!`, `""`, 0, false |
| `langEqualsValueIsNotStripped` | `"Ana", "--lang= es"` | `""`, `Unknown language:  es` + UNKNOWN_LANG, 2, false |
| `emptyLangEqualsFirstIsNameNotFirst` | `"--lang=", "Ana"` | `""`, `The name must come first, got: --lang=` + HINT, 2, false |
| `trailingLangAfterLangEqualsIsMissingValue` | `Ana --lang=es --lang` | `""`, `Missing value for --lang (supported: en, es)`, 2, false |

Rows already pinned elsewhere and not repeated: 8, 14 (characterization), 15, 16, 19, 25 (existing tests and characterization). Delete `doubleDashAlonePrintsUsage`; no other existing test changes.

- [ ] **Step 2: Run them to verify they fail**

Run: `mvn -B test -Dtest=GreetingCliTest`
Expected: the new tests fail, except four that already hold today and stay as guards: `emptyArgumentAfterNameIsIgnored`, `unknownLanguageBeforeUnknownOption`, `trailingLangAfterLangEqualsIsMissingValue` and `firstArgumentErrorBeatsUnknownOption`. `lastLangWinsAcrossForms` fails on its second call; `doubleDashAloneIsUnknownOption` fails.

- [ ] **Step 3: Implement the rules in `GreetingCli.run(String[] args)`**

Follow the Global Constraints rules 1–4 in one left-to-right scan. Add constants `LANG_EQUALS = "--lang="` and `UNKNOWN_OPTION_HINT = " (supported: --lang <code>, --lang=<code>, --shout)"`. Keep `USAGE`, `HINT` and every existing message unchanged. Update the Javadoc of `main` (the `@param args` text) for `--lang=<code>` and unknown options.

- [ ] **Step 4: Run the unit tests, including characterization**

Run: `mvn -B test`
Expected: all pass, including `GreetingCliCharacterizationTest` (10) and `GreetingCliOptionsCharacterizationTest` (5).

- [ ] **Step 5: Run the acceptance tests and lint**

Run: `mvn -B verify -Pacceptance` then `mvn -B checkstyle:check spotbugs:check`
Expected: all tests pass, including every method of `UnknownOptionAcceptanceTest`, `FlagAsNameAcceptanceTest` and `SpanishGreetingAcceptanceTest`; no checkstyle or spotbugs violations.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/sandbox/GreetingCli.java src/test/java/sandbox/GreetingCliTest.java
git commit -m "feat: accept --lang=<code> and reject unknown options"
```

### Task 3: Change list for operations

**Type:** chore
**Acceptance criteria:**
- `docs/changes/2026-11-09-16-unknown-options.md` exists with a "antes → ahora" table of every changed row: 1–6, 7 (second case only), 9–11, 12b, 12c, 13, 17, 18, 20, 21, 22, 23 and 24 (design, Despliegue y aviso a operaciones; criterion 7).
- Rows 12c and 20 are marked as "solo cambia el mensaje (salida 2 igual)", and row 7 second case as "salida 2 → 0" (design, Despliegue).
- It quotes the exact `Unknown option` text and says there is no transition period (design, Despliegue; need, Constraints).
- Rows not in the list (7 first case, 8, 12, 14–16, 19, 25, 26) do not appear.
**Security criteria:** None: the design's threats dimension does not apply (documentation only).
**Acceptance tests:** None: documentation, no behaviour.

**Files:**
- Create: `docs/changes/2026-11-09-16-unknown-options.md`

**Interfaces:**
- Consumes: the behaviour delivered by Task 2.
- Produces: the file linked from the PR.

- [ ] **Step 1: Write the file**

In Spanish, like the other docs. Columns: `Llamada | Hoy (salida, código) | Ahora (salida, código) | Nota`. The "Hoy" values are the ones in the design's Despliegue section and its verification against `develop` (71bdbd7).

- [ ] **Step 2: Check every "Ahora" value against the built CLI**

Run `mvn -B -DskipTests package`, then `java -cp target/classes sandbox.GreetingCli <args>` for each row of the table.
Expected: each stdout/stderr and exit code matches the "Ahora" column exactly.

- [ ] **Step 3: Commit**

```bash
git add docs/changes/2026-11-09-16-unknown-options.md
git commit -m "docs: change list for operations (Feature 16)"
```

---

Outside the Tasks: after Task 2, the controller dispatches the mapper in map mode to update the Run row of `docs/project-map.md` (`--lang en|es` / `--lang=<code>`, unknown options exit 2); the map is not edited by hand.
