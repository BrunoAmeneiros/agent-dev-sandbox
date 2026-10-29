# Design: Spanish greeting

Feature 3 · feature/3-spanish-greeting · 2026-10-29

Need (approved): [2026-10-29-3-spanish-greeting-need.md](2026-10-29-3-spanish-greeting-need.md). Why, success criteria, constraints and alternatives live there.

## Context

`Greeter` is a library (`src/main/java/sandbox/Greeter.java`). The caller already has the user's client, which carries a language field (`"es"` or `"en"`). Today `Greeter.greet(String name)` only receives the name and always returns `"Hello, <name>!"`.

## Goals

- The caller can pass the client's language and get the greeting in that language.
- `"es"` → `"¡Hola, <name>!"`; `"en"` → `"Hello, <name>!"`.

## Non-goals

- More languages.
- Per-user language preference.
- Accepting `java.util.Locale` instead of a language code.
- Message catalogs or resource files.

## Anti-goals

- Breaking current callers or the approved acceptance test (`tests/acceptance/GreeterAcceptanceTest.java`, `greet("Eva")` → `"Hello, Eva!"`).
- Throwing on an unknown language.
- Adding dependencies.

## Decided

- **Fallback (your human partner):** any language other than Spanish, including `null`, an empty string or an unknown code, yields the English greeting, without failing. It is what every user sees today.
- **API (my call, confirmed):** add `public String greet(String name, String language)`. Keep `greet(String name)` unchanged in behavior (English), implemented by delegating to `greet(name, "en")` or equivalent.
- **Case (my call, confirmed):** the language code is compared case-insensitively (`"ES"`, `"Es"` → Spanish). Use `Locale.ROOT` for the case conversion so the result does not depend on the JVM default locale.
- **Strict codes (my call):** no trimming and no regional codes. `" es "`, `"es-ES"` or `"es_MX"` fall back to English, since the client field only holds `"es"` or `"en"`.
- **Implementation (my call, confirmed):** a plain `switch` or conditional inside `Greeter`; no `ResourceBundle`. Two fixed languages do not justify a catalog.
- **Name handling:** unchanged; the name is inserted as is, as today.

## Tests

- **New acceptance test** `tests/acceptance/SpanishGreetingAcceptanceTest.java` (default package, like the existing one): `new Greeter().greet("Ana", "es")` returns `"¡Hola, Ana!"`. It is approved together with the plan.
- The existing acceptance test is not modified.
- **Unit tests** in `GreeterTest`:
  - `"es"` → `"¡Hola, Ana!"`;
  - `"en"` → `"Hello, Ana!"`;
  - `"ES"` → Spanish;
  - `null`, `""` and an unknown code (e.g. `"fr"`) → English;
  - `greet("Ana")` still → `"Hello, Ana!"`.

## Left to the builder

- Exact shape of the conditional and any private constant names.
- Javadoc wording on the new method (it must state the fallback to English).

Already checked: `pom.xml` sets `project.build.sourceEncoding` to UTF-8, so `¡` in sources is fine. Acceptance tests run with `mvn -B verify -Pacceptance` and must be named `*AcceptanceTest.java` (the default build excludes them).
