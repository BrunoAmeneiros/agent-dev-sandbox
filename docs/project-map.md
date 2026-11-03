# Project map

Index for agents. Written by the mapper (agent-dev-process); update it with the mapper, not by hand.

## Commands

| What | Command | Notes |
| --- | --- | --- |
| Build | `mvn -B -DskipTests package` | |
| All tests | `mvn -B verify -Pacceptance` | ~6 s |
| One test | `mvn -B test -Dtest=GreeterTest#<method>` | acceptance: `mvn -B verify -Pacceptance -Dit.test=<Class>` |
| Acceptance tests | `mvn -B verify -Pacceptance` | also runs unit tests |
| Coverage | `mvn -B verify` | report: target/site/jacoco/index.html and target/site/jacoco/jacoco.xml; CI needs 80 % on changed code (diff-cover) |
| Lint / static analysis | `mvn -B checkstyle:check spotbugs:check` | |
| Run | `java -cp target/classes sandbox.GreetingCli <name> [--shout] [--lang en|es]` | |

## Environment

Java 21 (Temurin in CI), Maven 3.9. No services, no Docker, no environment variables.

## Conventions

- Main code in `src/main/java/sandbox`, unit tests in `src/test/java/sandbox` (`*Test`).
- Acceptance tests in `tests/acceptance/` (`*AcceptanceTest`, default package); run only with `-Pacceptance` via Failsafe. They are protected paths ([.process/config.json](../.process/config.json)).
- Style: [checkstyle.xml](../checkstyle.xml); Javadoc on public methods. Errors: `Objects.requireNonNull(arg, "arg")`.
- `pom.xml` sets `project.build.sourceEncoding` UTF-8; non-ASCII characters in Java sources are still written as Unicode escapes.
- CI gates: [ci.yml](../.github/workflows/ci.yml) (80 % coverage of changed code).

## Zones

| Zone | Path | Purpose | Tests |
| --- | --- | --- | --- |
| Greeter | src/main/java/sandbox/Greeter.java | Builds the greeting; `greet(String)` and `greet(String, Language)` | src/test GreeterTest; tests/acceptance Greeter*AcceptanceTest |
| Language | src/main/java/sandbox/Language.java | Supported languages (`EN`, `ES`), `fromCode` | src/test LanguageTest |
| CLI | src/main/java/sandbox/GreetingCli.java | Argument parsing, `--shout`, `--lang en|es`, usage (a first argument starting with `--` prints the usage text, not a name). Errors go to stderr with exit code 2 (unknown or missing language; with `--lang`, missing name or a flag before the name). UTF-8 stdout with `--lang es`. Without `--lang` behaviour is unchanged (bugfix 10 rule kept) | src/test GreetingCliTest, GreetingCliCharacterizationTest; tests/acceptance FlagAsNameAcceptanceTest, SpanishGreetingAcceptanceTest |
| Process rails | .process/, .github/ | Protected paths, CI | none |

## Domain glossary

| Term | Meaning | Where in the code |
| --- | --- | --- |
| Greeting | `Hello, <name>!` | Greeter.greet |
| Spanish greeting | `¡Hola, <name>!` | Language.ES, Greeter.greet(String, Language) |
| Language code | `en`, `es`, case-insensitive | Language.fromCode |
| Shout | Upper-cased greeting via `--shout` | GreetingCli.run |

## Technical docs

- [README](../README.md)
- [CI workflow](../.github/workflows/ci.yml)

## Known failing tests

none
