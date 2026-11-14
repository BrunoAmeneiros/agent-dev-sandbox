# Impact: Feature 18 restore-help-option

**Environment gate:** passed - `mvn -B verify -Pacceptance`, 2026-11-14 (901b343): 101 tests run, 0 failed, 0 already known; by kind: unit 84/0, acceptance 17/0.

Need: [2026-11-14-18-restore-help-option-need.md](2026-11-14-18-restore-help-option-need.md). Map: [docs/project-map.md](../../project-map.md).

## Zones

| Zone | Path | Change | Why |
| --- | --- | --- | --- |
| CLI | src/main/java/sandbox/GreetingCli.java | modified | `--help` becomes a known option in any position: exit 0, help text, no greeting. Touches `run`, `firstArgumentResult`, `isUnknownOption`, the `UNKNOWN_OPTION_HINT` text and the Javadoc. |
| Ops change note | docs/changes/ | modified or new | Short note: `--help` and `Ana --help` go from error exit 2 to help exit 0. Either a new file or a section in `2026-11-09-16-unknown-options.md`. |
| Project map | docs/project-map.md | modified | The CLI zone and the Run row describe `--help` as an unknown option. The mapper updates them after the Feature. |

## Contracts

- CLI arguments: `--help` in any position. Today it is an unknown option (stderr, exit 2). Consumers are people (no script calls it), `GreetingCliTest`, `GreetingCliCharacterizationTest`, `UnknownOptionAcceptanceTest`, `FlagAsNameAcceptanceTest`, and the Feature 16 change list. This is a compatibility change on purpose.
- CLI output: the `Unknown option: <arg> (supported: ...)` text lists the supported options. Whether it now lists `--help` is a design decision. If it changes, tests that assert the exact text break.
- CLI output: the usage text (`Usage: greet <name> [--shout] [--lang en|es]`) is also shown by the bugfix 10 rule (`--shout Ana`, exit 0). Changing it for help changes that path too.
- Unchanged: `-h` and other single-dash arguments, `--lang`, `--lang=<code>`, `--shout`, the exit codes of the other paths.

## Data

None. The CLI stores nothing and handles no personal data beyond the name it prints.

## Coverage

| Zone | Line coverage | Tests that exercise it | Source |
| --- | --- | --- | --- |
| CLI | 91.7 % (66 of 72 lines; 74 of 78 branches) | GreetingCliTest, GreetingCliCharacterizationTest, GreetingCliOptionsCharacterizationTest, FlagAsNameAcceptanceTest, SpanishGreetingAcceptanceTest, UnknownOptionAcceptanceTest | target/site/jacoco/jacoco.csv from the gate run |
| Ops change note | n/a | none (docs) | n/a |

## Characterization needed

| Zone | Behaviour to pin | Why |
| --- | --- | --- |
| CLI | Interaction of an unknown option with the first-argument path. Pin `--foo` first, `Ana --foo`, `--shout --foo`, and the current "first problem left to right" rule between an unknown option and `--lang` errors (`Ana --foo --lang xx` against `Ana --lang xx --foo`). | The help change adds a case to this precedence logic. These cases are the ones it can break, and the existing tests may not all assert the exact outcome. The need lists them as open points. |
| CLI | The usage text on the `--shout Ana` path (stdout, exit 0). | The help text may reuse `USAGE`. A change there must not alter the bugfix 10 behaviour silently. |

No test mentions `--help` today (searched `src/` and `tests/`). The current error behaviour of `--help` is documented only in `docs/changes/2026-11-09-16-unknown-options.md`, so tests that assert the new behaviour are new.

## Risks and open questions for the design

- Help text: reuse `USAGE` as is, or extend it? It does not mention `--lang=<code>` or `--help`.
- Output stream for help: stdout or stderr? Exit code is 0 either way, so the choice matters to anyone who pipes the output.
- Precedence when `--help` is combined with other problems: `--help --foo`, `Ana --foo --help`, `Ana --lang xx --help`, `--lang es --help` (today "name must come first"), `Ana --lang --help` (`--help` as the value of `--lang`), repeated `--help`. Each needs one stated rule.
- `Ana --lang --help` is a risk in `run`: the loop consumes the next argument as the `--lang` value, so `--help` would be taken as a language code unless the design says otherwise.
- The acceptance tests are protected paths. New acceptance tests for `--help` need the process to allow them.
- The Feature 16 change note says `--help` gives an error. The new note must not leave the two notes in conflict.
- The `UNKNOWN_OPTION_HINT` text is asserted by existing tests. Adding `--help` to it would break them, so the design must decide that and update them knowingly.
