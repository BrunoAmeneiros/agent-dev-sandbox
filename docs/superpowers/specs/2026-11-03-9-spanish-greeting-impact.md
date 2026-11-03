# Impact: 9 Spanish greeting (`--lang es`)

**Environment gate:** passed - `mvn -B verify -Pacceptance`, 2026-11-03 (3b8af7e): 4 tests run, 0 failed, 0 already known.

Need: [2026-11-03-9-spanish-greeting-need.md](2026-11-03-9-spanish-greeting-need.md). Map: [docs/project-map.md](../../project-map.md).

## Zones

| Zone | Path | Change | Why |
| --- | --- | --- | --- |
| CLI | src/main/java/sandbox/GreetingCli.java | modified | Parse `--lang`, error for unknown language, exit code, usage text |
| Greeter | src/main/java/sandbox/Greeter.java | modified | Spanish text (`¡Hola, <name>!`); today it always returns `Hello, <name>!` |

## Contracts

- CLI arguments: `<name> [--shout]` gains `--lang <code>`. Consumers: users' welcome scripts (external), the usage text. Must stay compatible: no `--lang` gives the same output, and `--shout` is unchanged.
- CLI output and exit code: `main` always exits 0 today, and the new error must exit non-zero. Scripts that rely on exit 0 for unknown flags could break (today a second argument other than `--shout` is silently ignored).
- Usage string `Usage: greet <name> [--shout]` changes. Scripts that match it could break.
- Java API `Greeter.greet(String)`: public, used by `GreeterTest` and the acceptance tests. Keep this signature (add an overload if needed).
- `GreeterAcceptanceTest` is protected (approved) and must not be edited.

## Data

None. No entities, storage or personal data beyond the name printed.

## Coverage

| Zone | Line coverage | Tests that exercise it | Source |
| --- | --- | --- | --- |
| Greeter | 100 % (3/3) | GreeterTest, GreeterAcceptanceTest, GreeterNullAcceptanceTest | target/site/jacoco/jacoco.csv (gate run) |
| CLI | 0 % (0/8 lines, 0/8 branches) | none | target/site/jacoco/jacoco.csv (gate run) |

## Characterization needed

| Zone | Behaviour to pin | Why |
| --- | --- | --- |
| CLI | `run` with a name returns `Hello, Ana!`; `--shout` returns `HELLO, ANA!`; the name is stripped | No test touches `GreetingCli`; the request changes this method |
| CLI | No args or a blank name returns the usage text; an unknown second argument is ignored | Same method is rewritten; the usage text will change |
| CLI | `main` prints the result and exits 0 | The exit code is changing and nothing asserts it today |
| Greeter | `greet(null)` throws NullPointerException with message `name` | Already covered by acceptance tests; no extra work |

## Risks and open questions for the design

- The CI needs 80 % coverage on changed code. The CLI is at 0 %, so the new code needs tests, and pinning today's behaviour first is the safe way.
- The exit code must be testable: `run` returns a `String` and `main` only prints. The design must decide how `run` reports the error and who calls `System.exit`.
- Error message: stdout or stderr, and its exact text (open in the need).
- Flag order and combination: `--shout` is read only from `args[1]`, so `Ana --lang es --shout` and `--shout --lang es` need a defined behaviour. Spanish shout is `¡HOLA, ANA!`; `toUpperCase(Locale.ROOT)` keeps `¡` and works for this text, but check accented names.
- `--lang en` explicit, `--lang` with no value, and a repeated `--lang`: undefined in the need.
- Should an extra unknown argument stay ignored? Rejecting it would break current scripts.
- Where the Spanish text lives (Greeter overload with `Locale` or another piece); the need suggests `ResourceBundle`, but one language may not justify it. Non-ASCII `¡` in source or properties files needs a UTF-8 encoding check in the build, and console output encoding on Windows may print it wrongly.
- Checkstyle and SpotBugs run on new code; the lint run was not part of the gate.
- Deadline of about three weeks: the scope is small.
