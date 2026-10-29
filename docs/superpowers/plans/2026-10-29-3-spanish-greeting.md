# Spanish Greeting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `Greeter` greets in Spanish (`"¡Hola, <name>!"`) when the caller passes language `"es"`, and in English otherwise.

**Architecture:** Add an overload `greet(String name, String language)` to `sandbox.Greeter`; the existing `greet(String name)` delegates to it with `"en"`. Language selection is a plain conditional, case-insensitive via `Locale.ROOT`.

**Tech Stack:** Java, Maven, JUnit Jupiter.

**Spec:** `docs/superpowers/specs/2026-10-29-3-spanish-greeting-design.md` (need: `docs/superpowers/specs/2026-10-29-3-spanish-greeting-need.md`).

## Global Constraints

- No new dependencies (JDK and existing code only).
- Exact outputs: Spanish `"¡Hola, <name>!"`, English `"Hello, <name>!"`.
- Any language other than Spanish (`null`, `""`, unknown, `" es "`, `"es-ES"`) → English, never throws.
- Case-insensitive comparison using `Locale.ROOT`.
- Do not modify `tests/acceptance/GreeterAcceptanceTest.java` or `tests/acceptance/SpanishGreetingAcceptanceTest.java` (approved, protected).
- Code must pass `checkstyle.xml` (run by `mvn -B verify`).

## Review Focus

1. `null` language → English, no `NullPointerException` (covered: `fallsBackToEnglishForNullLanguage`).
2. Upper/mixed case `"ES"` → Spanish regardless of JVM default locale (covered: `ignoresLanguageCase`).
3. Regional/padded codes `"es-ES"`, `" es "` → English per strict-code decision (covered: `fallsBackToEnglishForNonExactCodes`).
4. Existing callers of `greet(name)` unaffected, and English clients still see `"Hello, Ana!"` (covered: existing `greetsByName`, `GreeterAcceptanceTest`, and `SpanishGreetingAcceptanceTest.greetsInEnglishForEnglishClients`).
5. `¡` survives compilation (UTF-8 already set in `pom.xml`; covered by the acceptance test running under `-Pacceptance`).

---

### Task 1: Language-aware greeting

**Files:**
- Modify: `src/main/java/sandbox/Greeter.java`
- Test: `src/test/java/sandbox/GreeterTest.java`
- Acceptance (already written, approved with this plan, do not edit): `tests/acceptance/SpanishGreetingAcceptanceTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `public String greet(String name, String language)` in `sandbox.Greeter`; `public String greet(String name)` unchanged in behavior.

- [ ] **Step 1: Write the failing unit tests** in `GreeterTest` (keep `greetsByName`):

```java
@Test
void greetsInSpanish() {
  assertEquals("¡Hola, Ana!", new Greeter().greet("Ana", "es"));
}

@Test
void greetsInEnglish() {
  assertEquals("Hello, Ana!", new Greeter().greet("Ana", "en"));
}

@Test
void ignoresLanguageCase() {
  assertEquals("¡Hola, Ana!", new Greeter().greet("Ana", "ES"));
}

@Test
void fallsBackToEnglishForNullLanguage() {
  assertEquals("Hello, Ana!", new Greeter().greet("Ana", null));
}

@Test
void fallsBackToEnglishForUnknownLanguage() {
  assertEquals("Hello, Ana!", new Greeter().greet("Ana", ""));
  assertEquals("Hello, Ana!", new Greeter().greet("Ana", "fr"));
}

@Test
void fallsBackToEnglishForNonExactCodes() {
  assertEquals("Hello, Ana!", new Greeter().greet("Ana", "es-ES"));
  assertEquals("Hello, Ana!", new Greeter().greet("Ana", " es "));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -B verify`
Expected: compilation failure, `greet(String,String)` not found.

- [ ] **Step 3: Implement `public String greet(String name, String language)` in `Greeter.java`**

Spanish when `"es".equals(language.toLowerCase(Locale.ROOT))` (null-safe), English otherwise. Make `greet(String name)` delegate with `"en"`. Javadoc on the new method states the English fallback.

- [ ] **Step 4: Run all tests, including acceptance**

Run: `mvn -B verify -Pacceptance`
Expected: BUILD SUCCESS; `GreeterTest`, `GreeterAcceptanceTest` and `SpanishGreetingAcceptanceTest` pass; no checkstyle violations.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/sandbox/Greeter.java src/test/java/sandbox/GreeterTest.java
git commit -m "feat: greet in Spanish for language es"
```
