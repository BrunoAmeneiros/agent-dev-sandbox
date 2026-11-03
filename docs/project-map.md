# Project map

Index for agents. Written by the mapper (agent-dev-process); update it with the mapper, not by hand.

## Commands

| What | Command | Notes |
| --- | --- | --- |
| Build | `mvn -B -DskipTests package` | |
| All tests | `mvn -B verify -Pacceptance` | ~6 s |
| One test | `mvn -B test -Dtest=GreeterTest#<method>` | acceptance: `mvn -B verify -Pacceptance -Dit.test=<Class>` |
| Acceptance tests | `mvn -B verify -Pacceptance` | also runs unit tests |
| Coverage | `mvn -B verify` | report: target/site/jacoco/jacoco.xml |
| Lint / static analysis | `mvn -B checkstyle:check spotbugs:check` | |
| Run | `java -cp target/classes sandbox.GreetingCli <name> [--shout]` | |

## Environment

Java 21 (Temurin in CI), Maven 3.9. No services, no Docker, no environment variables.

## Conventions

- Main code in `src/main/java/sandbox`, unit tests in `src/test/java/sandbox` (`*Test`).
- Acceptance tests in `tests/acceptance/` (`*AcceptanceTest`, default package); run only with `-Pacceptance` via Failsafe. They are protected paths ([.process/config.json](../.process/config.json)).
- Style: [checkstyle.xml](../checkstyle.xml); Javadoc on public methods. Errors: `Objects.requireNonNull(arg, "arg")`.
- CI gates: [ci.yml](../.github/workflows/ci.yml) (80 % coverage of changed code).

## Zones

| Zone | Path | Purpose | Tests |
| --- | --- | --- | --- |
| Greeter | src/main/java/sandbox/Greeter.java | Builds the greeting | src/test GreeterTest; tests/acceptance Greeter*AcceptanceTest |
| CLI | src/main/java/sandbox/GreetingCli.java | Argument parsing, `--shout`, usage | none found at this commit |
| Process rails | .process/, .github/ | Protected paths, CI | none |

## Domain glossary

| Term | Meaning | Where in the code |
| --- | --- | --- |
| Greeting | `Hello, <name>!` | Greeter.greet |
| Shout | Upper-cased greeting via `--shout` | GreetingCli.run |

## Technical docs

- [README](../README.md)

## Known failing tests

none
