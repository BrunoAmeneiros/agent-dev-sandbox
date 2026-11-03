# 0001. Abandonar la Feature 3 (saludo en español en `Greeter`)

Fecha: 2026-11-03 · Estado: aceptado · Feature 11 (`feature/11-abandon-3-4`)

## Context

La Feature 3 (`feature/3-spanish-greeting`, abierta el 2026-10-29) tenía aprobados la necesidad, el diseño, el plan y el test de aceptación `SpanishGreetingAcceptanceTest`. Proponía que `Greeter` recibiera el idioma del cliente (`"es"` o `"en"`) y devolviera `"¡Hola, <name>!"` en español. No llegó a tener código de producción.

Después se abrió la Feature 9 (`feature/9-spanish-greeting`), que responde a la misma necesidad (saludar en español a un cliente nuevo de España) desde la CLI, con `--lang es`. Si se mantienen las dos, habrá dos diseños y dos planes que cambian `Greeter` para lo mismo.

Archivo de la Feature 3: etiqueta [`archive/feature-3`](https://github.com/BrunoAmeneiros/agent-dev-sandbox/tree/archive/feature-3) (commit `2bf2372`). Contiene:

- `docs/superpowers/specs/2026-10-29-3-spanish-greeting-need.md`
- `docs/superpowers/specs/2026-10-29-3-spanish-greeting-design.md`
- `docs/superpowers/plans/2026-10-29-3-spanish-greeting.md`
- `tests/acceptance/SpanishGreetingAcceptanceTest.java`

## Alternatives

1. **Seguir con la 3 y la 9 a la vez.** Se descarta: duplica el trabajo y genera conflictos en `Greeter`.
2. **Fusionar la 3 en la 9.** Se descarta: la 9 ya tiene su propia necesidad, diseño, plan y tests, y no les falta nada de la 3.
3. **Abandonar la 3.** Se elige esta.

## Decision

Se abandona la Feature 3. La necesidad del saludo en español queda cubierta por la Feature 9. No se rescata nada de la rama: sus documentos se conservan en la etiqueta `archive/feature-3`. La rama local se ha borrado y nunca se subió a origin.

## Consequences

- La única vía para el saludo en español es la Feature 9.
- Quien quiera consultar el diseño original (idioma pasado a `Greeter` y el uso de `ResourceBundle` o de un mapa) lo encontrará en la etiqueta.
- La Feature 3 no produjo efectos externos que haya que revertir.
