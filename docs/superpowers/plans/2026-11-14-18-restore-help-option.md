# `--help` shows the help Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `--help`, wherever it is, prints the help text to stdout with exit 0 and no greeting, unless it is the value of `--lang`; every call without such a `--help` behaves exactly as today; operations get a short note.

**Architecture:** Only `GreetingCli` changes. Both paths get a help check: `run`, inside its existing scan after `--lang` has consumed its value, and `firstArgumentResult`, before any other check. A new `HELP_TEXT` constant holds the help. `USAGE`, `HINT` and `UNKNOWN_OPTION_HINT` stay. A note for operations is added under `docs/changes/`.

**Tech Stack:** Java 21, Maven, JUnit 5. No new dependencies.

**Spec:** [docs/superpowers/specs/2026-11-14-18-restore-help-option-design.md](../specs/2026-11-14-18-restore-help-option-design.md) (need: [2026-11-14-18-restore-help-option-need.md](../specs/2026-11-14-18-restore-help-option-need.md)). The design's "Entradas inválidas y casos límite" table (rows 1–14) and the two review cases below it are the source of truth for every input.

## Global Constraints

- No new dependencies; do not touch `pom.xml`.
- `GreetingCli.run(String[])` keeps its signature and returns `GreetingCli.CliResult(stdout, stderr, exitCode, utf8Stdout)`: the characterization tests call it.
- Protected, never edited by any Task: `src/test/java/sandbox/GreetingCliCharacterizationTest.java`, `src/test/java/sandbox/GreetingCliOptionsCharacterizationTest.java`, `src/test/java/sandbox/GreetingCliHelpCharacterizationTest.java`, and every file under `tests/acceptance/` (including `HelpOptionAcceptanceTest.java` and `HelpOptionGuardsAcceptanceTest.java`). If one of them seems wrong, stop and report BLOCKED with the clash; never special-case an input to pass a test.
- **Help rule.** A call shows the help when an argument is exactly `--help` and is not the value of `--lang`. The first argument is compared after `strip()`; later arguments are compared exactly (case-sensitive, not stripped).
  - After a name (`run`): the value of `--lang` is the argument it consumes today (the next one), or the text after `--lang=`. A consumed argument is never checked for help.
  - First-argument path (`firstArgumentResult`, no value consumption): an argument `--help` at index `i ≥ 1` is the value of `--lang` when the argument at `i - 1` is exactly `--lang` (index 0 compared after `strip()`). Such a `--help` is not help.
  - Help wins over every other problem: unknown option, unknown or missing language, `Missing name`, `The name must come first`, and the bugfix 10 usage text.
- **Help result:** `new CliResult(HELP_TEXT, "", 0, false)`: stdout is the help, stderr is empty, exit is 0, and `utf8Stdout` is `false` even with `--lang es`. `HELP_TEXT` is these five lines joined with `"\n"`, with no trailing newline:
  ```
  Usage: greet <name> [--shout] [--lang <code> | --lang=<code>] [--help]
    --shout          print the greeting in upper case
    --lang <code>    language of the greeting: en, es (default en)
    --lang=<code>    same as --lang <code>
    --help           show this help
  ```
- **Unchanged texts:** `USAGE` (`Usage: greet <name> [--shout] [--lang en|es]`), `HINT`, `UNKNOWN_OPTION_HINT` (` (supported: --lang <code>, --lang=<code>, --shout)`), `Unknown language: …`, `Missing value for --lang …`, `Missing name …`, `The name must come first, got: …`. `isUnknownOption` must return `false` for `--help` without changing `UNKNOWN_OPTION_HINT`.
- Non-ASCII characters in Java sources are written as Unicode escapes.
- Checkstyle (`checkstyle.xml`) and Javadoc on public methods. CI needs 80 % coverage on changed code.
- Commands (from `docs/project-map.md`):
  - one unit test class: `mvn -B test -Dtest=<Class>`;
  - one acceptance test class: `mvn -B verify -Pacceptance -Dit.test=<Class>`;
  - all tests: `mvn -B verify -Pacceptance`;
  - lint: `mvn -B checkstyle:check spotbugs:check`.
- Each commit's `Co-Authored-By` names the model that made it.
- Not a Task: the update of `docs/project-map.md` (CLI zone and Run row) is done by the mapper after the Feature.

## Review Focus

These inputs are not rows of the design. Each follows from the help rule and is pinned by a unit test in Task 2:

1. `--foo --help`: an unknown option in first position followed by `--help` → help, exit 0, because help wins.
2. `Ana --shout --help` → help, with no shouted greeting.
3. `Ana --lang es --help` → help with `utf8Stdout` false.
4. `"" --lang --help` → `Missing name (usage: greet <name> [--shout] [--lang en|es])`, 2: in the first-argument path, `--help` is the value of `--lang`.
5. `"  --lang  " --help` → `The name must come first, got: --lang (usage: …)`, 2: the stripped first argument is `--lang`, so `--help` is its value.

---

### Task 1: Baseline: the protected tests on today's code

**Type:** chore
**Acceptance criteria:**
- `GreetingCliHelpCharacterizationTest` passes on today's code: 11 tests, 0 failures (design: Impact, Characterization needed).
- `HelpOptionGuardsAcceptanceTest` passes on today's code and `HelpOptionAcceptanceTest` fails on it, every method by assertion (design: Criterios de aceptación).
**Security criteria:** None: the design's threats dimension does not apply (local CLI, no network, no sensitive data).
**Acceptance tests:** None: baseline only, no code change.

**Files:**
- None changed.

**Interfaces:**
- Consumes: nothing.
- Produces: the baseline counts for the controller's report.

- [x] **Step 1: Run the characterization tests**

Run: `mvn -B test -Dtest=GreetingCliHelpCharacterizationTest`
Expected: `Tests run: 11, Failures: 0, Errors: 0`

- [x] **Step 2: Run the acceptance tests**

Run: `mvn -B verify -Pacceptance -Dit.test='HelpOption*AcceptanceTest'`
Expected: `HelpOptionGuardsAcceptanceTest` 3 run, 0 failures. `HelpOptionAcceptanceTest` 6 run, 6 failures (`Unknown option: --help` and exit 2 instead of the help; `opsNoteExists`: the note is missing).

### Task 2: `--help` in `GreetingCli`

**Type:** feature
**Acceptance criteria:**
- `--help`, `--help Ana` and `Ana --help` print `HELP_TEXT` to stdout, with stderr empty, exit 0 and no greeting (design: criterion 1, row 1).
- `--help --help` and `Ana --help --help` give the help (row 2).
- Help wins: `--help --foo`, `Ana --foo --help`, `Ana --lang xx --help`, `Ana --lang= --help`, `--lang es --help`, `--lang=es --help`, `"" --help` and `--shout --help` give the help (criterion 2, rows 3–5).
- `--help` as the value of `--lang` is not help: `Ana --lang --help` and `Ana --lang=--help` give `Unknown language: --help (supported: en, es)`, 2; `--lang --help` gives `The name must come first, got: --lang (usage: greet <name> [--shout] [--lang en|es])`, 2 (criterion 3, rows 6, 7 and 9).
- `Ana --lang --help --help` and `Ana --lang --lang --help` give the help. `--shout --lang --lang --help` gives `The name must come first, got: --shout (usage: …)`, 2 (row 8 and the review cases).
- Variants that are not help keep today's result: `Ana --HELP`, `Ana --Help`, `Ana --help=yes` and `Ana --help=` give `Unknown option: <arg> (supported: --lang <code>, --lang=<code>, --shout)`, 2; `Ana -h`, `Ana -help`, `Ana help` and `Ana " --help"` give `Hello, Ana!`, 0 (criterion 4, rows 10–12 and 14).
- `"  --help  "`, alone or followed by `Ana`, gives the help (row 13).
- `--shout Ana` still prints `USAGE` with exit 0, and `Ana --foo` still gives `Unknown option: --foo (…)`, 2 (criterion 5).
- The help is not UTF-8 (`utf8Stdout` false), also with `--lang es` (design: Comportamiento de la CLI).
- The Review Focus inputs 1–5 give the results listed there.
- The Javadoc of the class, `main` and `run` describes `--help` and its precedence (design: Despliegue, documentation).
**Security criteria:** None: the design's threats dimension does not apply (local CLI, no network, no sensitive data; the help text is fixed).
**Acceptance tests:** `tests/acceptance/HelpOptionAcceptanceTest.java` (all methods except `#opsNoteExists`, which is Task 3); `tests/acceptance/HelpOptionGuardsAcceptanceTest.java` stays green.

**Files:**
- Modify: `src/main/java/sandbox/GreetingCli.java`
- Test: `src/test/java/sandbox/GreetingCliTest.java` (not protected; add tests, change none)

**Interfaces:**
- Consumes: nothing from other Tasks.
- Produces: `private static final String HELP_TEXT` in `GreetingCli`. The signatures of `run`, `main` and `CliResult` do not change.

- [ ] **Step 1: Write the failing unit tests**

In `GreetingCliTest`, add `private static final String HELP = String.join("\n", <the five lines of HELP_TEXT from Global Constraints>);`. Then add these tests, each using the existing `assertResult(stdout, stderr, exitCode, utf8, run(...))`:

| Test | Call | Expected |
| --- | --- | --- |
| `helpAloneShowsHelp` | `run("--help")` | `HELP, "", 0, false` |
| `helpAfterNameShowsHelp` | `run("Ana", "--help")` | `HELP, "", 0, false` |
| `helpWinsOverUnknownOptionAfterName` | `run("Ana", "--foo", "--help")` | `HELP, "", 0, false` |
| `helpWinsOverUnknownLanguage` | `run("Ana", "--lang", "xx", "--help")` | `HELP, "", 0, false` |
| `helpWinsOverNameNotFirst` | `run("--lang", "es", "--help")` | `HELP, "", 0, false` |
| `helpWinsOverUnknownFirstOption` | `run("--foo", "--help")` | `HELP, "", 0, false` |
| `helpWinsOverShout` | `run("Ana", "--shout", "--help")` | `HELP, "", 0, false` |
| `helpWithSpanishIsNotUtf8` | `run("Ana", "--lang", "es", "--help")` | `HELP, "", 0, false` |
| `helpAfterConsumedLangShowsHelp` | `run("Ana", "--lang", "--lang", "--help")` | `HELP, "", 0, false` |
| `paddedHelpFirstShowsHelp` | `run("  --help  ", "Ana")` | `HELP, "", 0, false` |
| `helpAsLangValueWithEmptyNameIsMissingName` | `run("", "--lang", "--help")` | `"", "Missing name" + HINT, 2, false` |
| `helpAsValueOfPaddedLangIsNameNotFirst` | `run("  --lang  ", "--help")` | `"", "The name must come first, got: --lang" + HINT, 2, false` |
| `upperCaseHelpIsUnknownOption` | `run("Ana", "--HELP")` | `"", "Unknown option: --HELP" + UNKNOWN, 2, false` |

- [ ] **Step 2: Run them to verify they fail**

Run: `mvn -B test -Dtest=GreetingCliTest`
Expected: the 10 help tests fail with `Unknown option: --help`, an unknown language or a name error instead of the help. The last three already pass and stay as guards.

- [ ] **Step 3: Implement the help rule in `GreetingCli`**

- Add `HELP = "--help"` and `HELP_TEXT` (exact text from Global Constraints, joined with `"\n"`) and a `private static CliResult help()` returning `new CliResult(HELP_TEXT, "", 0, false)`.
- `isUnknownOption(String)` also returns `false` for `HELP`.
- `run`: inside the existing loop, an argument equal to `HELP` sets `boolean help = true`. The loop already consumes the `--lang` value with `++i`, so a consumed `--help` is never seen. Right after the loop, before the unknown-option and language checks, `if (help) return help();`. The `HELP` check goes before the `arg.startsWith("--")` branch.
- `firstArgumentResult(name, args)`: first of all, return `help()` if `HELP.equals(name)`, or if some `args[i]` (`i ≥ 1`) equals `HELP` and the previous argument (`name` for `i == 1`, `args[i - 1]` otherwise) is not exactly `LANG`. Then the existing logic, unchanged.
- Update the Javadoc of the class, `main` and `run`: `--help` in any position prints the help with exit 0 and wins over every error, unless it is the value of `--lang`.

- [ ] **Step 4: Run the unit tests and the characterization tests**

Run: `mvn -B test`
Expected: `BUILD SUCCESS`, 0 failures. That includes `GreetingCliHelpCharacterizationTest` (11), `GreetingCliCharacterizationTest`, `GreetingCliOptionsCharacterizationTest` and the new `GreetingCliTest` methods.

- [ ] **Step 5: Run the acceptance tests**

Run: `mvn -B verify -Pacceptance -Dit.test='HelpOption*AcceptanceTest'`
Expected: `HelpOptionGuardsAcceptanceTest` 3/0. `HelpOptionAcceptanceTest`: 6 run, only `opsNoteExists` fails (Task 3).

- [ ] **Step 6: Lint**

Run: `mvn -B checkstyle:check spotbugs:check`
Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add src/main/java/sandbox/GreetingCli.java src/test/java/sandbox/GreetingCliTest.java
git commit -m "feat: --help shows the help (Feature 18)"
```

### Task 3: Note for operations

**Type:** chore
**Acceptance criteria:**
- `docs/changes/2026-11-14-18-help-option.md` exists and is written in Spanish, like the Feature 16 note (design: criterion 7; Despliegue y aviso a operaciones). It has:
  - a line saying it corrects the section "Otras opciones desconocidas: `--help`" of `docs/changes/2026-11-09-16-unknown-options.md`, with a relative link to that file;
  - a table `| Llamada | Hoy (salida, código) | Ahora (salida, código) |` with two rows: `--help` and `Ana --help`, both from `Unknown option: --help (supported: --lang <code>, --lang=<code>, --shout)`, 2 to the help text, 0;
  - one sentence: any call with `--help` shows the help, unless `--help` is the value of `--lang` (`Ana --lang --help`, `Ana --lang=--help`).
- `docs/changes/2026-11-09-16-unknown-options.md` is not modified (design: Despliegue).
**Security criteria:** None: documentation only.
**Acceptance tests:** `tests/acceptance/HelpOptionAcceptanceTest.java#opsNoteExists`; after this Task the whole class is green.

**Files:**
- Create: `docs/changes/2026-11-14-18-help-option.md`

**Interfaces:**
- Consumes: the help behaviour from Task 2 (for the note's "Ahora" column).
- Produces: nothing.

- [ ] **Step 1: Run the test to verify it fails**

Run: `mvn -B verify -Pacceptance -Dit.test=HelpOptionAcceptanceTest`
Expected: `opsNoteExists` fails with `ops note exists`.

- [ ] **Step 2: Write `docs/changes/2026-11-14-18-help-option.md`** with the content listed in the acceptance criteria.

- [ ] **Step 3: Run all tests**

Run: `mvn -B verify -Pacceptance`
Expected: `BUILD SUCCESS`, 0 failures: unit tests, characterization tests and all acceptance tests, including both `HelpOption*AcceptanceTest` classes.

- [ ] **Step 4: Commit**

```bash
git add docs/changes/2026-11-14-18-help-option.md
git commit -m "docs: note for operations on --help (Feature 18)"
```
