# Impact: 16 lang-equals-syntax

**Environment gate:** passed - `mvn -B verify -Pacceptance`, 2026-11-09 (23b0627): 58 tests run, 0 failed, 0 already known; by kind: unit 49/0, acceptance 9/0.

Need: [2026-11-09-16-lang-equals-syntax-need.md](2026-11-09-16-lang-equals-syntax-need.md). Map: [docs/project-map.md](../../project-map.md).

## Zones

| Zone | Path | Change | Why |
| --- | --- | --- | --- |
| CLI | src/main/java/sandbox/GreetingCli.java | modified | `run` must accept `--lang=<code>` and fail on unknown `--` flags; today it only matches `--lang` and `--shout` by exact equality and ignores the rest |

Greeter and Language are not touched (`Language.fromCode` is reused as is).

## Contracts

- CLI arguments: `--lang=<code>` becomes valid (new form). Consumers: the kiosco script (external), `SpanishGreetingAcceptanceTest`, `GreetingCliTest`, the usage text (`USAGE`, `HINT`) and the Run row of the map. Not breaking.
- CLI behaviour: an unknown `--` flag (`--lnag es`, `--shout=yes`) goes from ignored (exit 0, greeting) to an error with exit 2. Breaking for any script that passes such flags. Accepted in the need, without transition; operations must get the list of changes before release.
- CLI output: new stderr message for the unknown flag (text open, see questions). Exit code 2 matches the existing `ERROR_EXIT_CODE`.
- Kept: loose non-flag arguments (`Ana foo`), the bugfix 10 rule for a first argument starting with `--`, and the `Unknown language` / `Missing value for --lang` messages.

## Data

None. No entities, storage or personal data; the CLI only reads arguments.

## Coverage

| Zone | Line coverage | Tests that exercise it | Source |
| --- | --- | --- | --- |
| CLI (GreetingCli) | 86 % lines (37 of 43), branches 20 of 24 | GreetingCliTest, GreetingCliCharacterizationTest, FlagAsNameAcceptanceTest, SpanishGreetingAcceptanceTest | target/site/jacoco/jacoco.csv from the gate run |

## Characterization needed

| Zone | Behaviour to pin | Why |
| --- | --- | --- |
| CLI | The no-`--lang` path with extra `--` arguments in any position (`Ana --foo`, `Ana foo --shout`, `Ana --shout --foo`): which are ignored today | The Feature changes which of them become errors; the existing characterization fixes `Ana foo` and `Ana foo --shout`, but not an unknown `--` flag after the name |
| CLI | `--lang` path with unknown `--` flags and with repeated `--lang` (`--lang en --lang es`, last wins) | The loop silently ignores unknown flags and the last value wins; the new rule and the `=` form must keep or change this on purpose |
| CLI | Single-dash arguments (`-x`) and a lone `--` after the name | No test asserts them; need to know whether they count as flags |

## Risks and open questions for the design

- Exact error text for an unknown flag, and whether it carries `HINT`.
- `--lang=` empty, `--lang=es=x`, `--LANG=es`, `--lang=ES` (`fromCode` is case-insensitive, flag name is not today), `--lang=es` mixed with `--lang en`: which wins or fails.
- `--shout=` empty: error like any `--shout=<value>`?
- `Ana foo --shout` ignores `--shout` today (known flag, other position, pinned by characterization); the new rule must not turn it into an error, nor change its output.
- `hasLang` detection is by exact `--lang`, so `--lang=es` must be added to it, or the no-`--lang` branch will greet in English and now error on it.
- Interaction with the first-argument `--` rule (bugfix 10): `--lang=es` as first argument, with and without a name. `FlagAsNameAcceptanceTest` and `GreetingCliTest` pin it.
- `-x` and a lone `--`: flags or loose arguments?
- A value after `--lang` that starts with `--` is consumed as the code today (`--lang --shout`); keep or change.
- Delivering the change list to operations (where, and before which release).
- Docs to update: usage/`HINT` strings, `main` Javadoc and the Run row of the map (`--lang=es`).
- CI needs 80 % coverage of changed code: each new branch needs a test.
