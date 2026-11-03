# Project map

Index for agents. Written by the mapper (agent-dev-process); update it with the mapper, not by hand.

## Commands

| What | Command | Notes |
| --- | --- | --- |
| Build | `mvn -B -DskipTests package` | |
| All tests | `mvn -B verify -Pacceptance` | ~6 s |
| One test | `mvn -B test -Dtest=GreeterTest#greetsByName` | acceptance: `mvn -B verify -Pacceptance -Dit.test=GreeterAcceptanceTest` |
| Acceptance tests | `mvn -B verify -Pacceptance` | Failsafe, `tests/acceptance/`; also runs unit tests |
| Coverage | `mvn -B verify` | report: target/site/jacoco/index.html (jacoco.xml); CI needs 80 % on changed code (diff-cover) |
| Lint / static analysis | `mvn -B checkstyle:check spotbugs:check` | config: checkstyle.xml |
| Run | `unknown` | `sandbox.GreetingCli` main class; no exec plugin configured |

## Environment

Java 21 (temurin in CI), Maven 3.9+. No services, Docker or environment variables needed.

## Conventions

- Package `sandbox`; sources in `src/main/java`, unit tests in `src/test/java` (JUnit 5, `*Test`).
- Acceptance tests in `tests/acceptance/`, named `*AcceptanceTest`; excluded from Surefire, run by Failsafe under `-Pacceptance`. Approved ones are protected (`.process/config.json`).
- Null arguments rejected with `Objects.requireNonNull(arg, "arg")`.
- Checkstyle enforced (`checkstyle.xml`); Javadoc on public API.

## Zones

| Zone | Path | Purpose | Tests |
| --- | --- | --- | --- |
| Greeter | src/main/java/sandbox/Greeter.java | Builds the greeting | src/test/java/sandbox/GreeterTest.java, tests/acceptance/ |
| CLI | src/main/java/sandbox/GreetingCli.java | Args parsing, `--shout`, usage text | none |
| Process rails | .process/, .github/ | Protected paths, CI gates | n/a |

## Domain glossary

| Term | Meaning | Where in the code |
| --- | --- | --- |
| Greeting | `"Hello, <name>!"` | Greeter.greet |
| Shout | Upper-cased greeting via `--shout` | GreetingCli.run |

## Technical docs

- [README](../README.md)
- [CI workflow](../.github/workflows/ci.yml)

## Known failing tests

none
