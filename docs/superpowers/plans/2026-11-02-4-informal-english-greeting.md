# Saludo informal en inglés: plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Añadir a `Greeter` el método `greetInformally(String name)`, que devuelve `"Hi, <name>!"`, sin cambiar `greet(String name)`.

**Architecture:** Un método público nuevo en la clase existente `sandbox.Greeter`. Quien llama a la librería decide cuándo usarlo. No hay tipos, configuración ni dependencias nuevas.

**Tech Stack:** Java 21, Maven, JUnit 5.11, Checkstyle.

**Spec:** `docs/superpowers/specs/2026-11-02-4-informal-english-greeting-design.md` (necesidad: `docs/superpowers/specs/2026-11-01-4-informal-english-greeting-need.md`).

## Global Constraints

- `greet(String name)` no cambia ni de firma ni de comportamiento: sigue devolviendo `"Hello, " + name + "!"`.
- Sin dependencias nuevas: `pom.xml` no se toca.
- Formato informal: `"Hi, " + name + "!"`. El nombre se usa tal cual llega, igual que en `greet`.
- `tests/acceptance/GreeterAcceptanceTest.java` y `tests/acceptance/InformalGreetingAcceptanceTest.java` son rutas protegidas (aprobadas): no se modifican.

## Review Focus

1. Nombre vacío `""`: se espera `"Hi, !"`, igual que `greet("")` devuelve `"Hello, !"`. Lo cubre `greetsInformallyLikeGreetForEdgeNames`.
2. Nombre `null`: se espera `"Hi, null!"`, en paridad con `greet(null)` (el nombre se usa tal cual llega). Lo cubre la misma prueba.
3. Nombre con espacios alrededor (`" Ana "`): no se recorta, `"Hi,  Ana !"`. Lo cubre la misma prueba.
4. Que `greet` siga intacto: lo cubren `greetsByName` y las dos pruebas de aceptación.

---

### Task 1: Saludo informal en `Greeter`

**Files:**
- Modify: `src/main/java/sandbox/Greeter.java`
- Test: `src/test/java/sandbox/GreeterTest.java`
- Acceptance (ya escrita y aprobada, no tocar): `tests/acceptance/InformalGreetingAcceptanceTest.java`

**Interfaces:**
- Consumes: `public String greet(String name)` (existente, sin cambios).
- Produces: `public String greetInformally(String name)` en `sandbox.Greeter`.

- [ ] **Step 1: Write the failing tests** in `GreeterTest`

```java
@Test
void greetsInformallyByName() {
  assertEquals("Hi, Ana!", new Greeter().greetInformally("Ana"));
}

@Test
void greetsInformallyLikeGreetForEdgeNames() {
  Greeter greeter = new Greeter();
  assertEquals("Hi, !", greeter.greetInformally(""));
  assertEquals("Hi, null!", greeter.greetInformally(null));
  assertEquals("Hi,  Ana !", greeter.greetInformally(" Ana "));
}
```

`greetsByName` se queda como está.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -B test`
Expected: FAIL de compilación, `cannot find symbol ... greetInformally`.

- [ ] **Step 3: Implement `public String greetInformally(String name)` in `src/main/java/sandbox/Greeter.java`**

Devuelve `"Hi, " + name + "!"`. No toca `greet`.

- [ ] **Step 4: Run all tests, including acceptance and checkstyle**

Run: `mvn -B verify -Pacceptance` y `mvn -B checkstyle:check`
Expected: `BUILD SUCCESS`. Pasan `GreeterTest` (3 pruebas), `GreeterAcceptanceTest` y `InformalGreetingAcceptanceTest`, y checkstyle no da errores.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/sandbox/Greeter.java src/test/java/sandbox/GreeterTest.java
git commit -m "feat: add informal English greeting (Feature 4)"
```
