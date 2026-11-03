# 0002. Abandonar la Feature 4 (saludo informal en inglés)

Fecha: 2026-11-03 · Estado: aceptado · Feature 11 (`feature/11-abandon-3-4`)

## Context

La Feature 4 (`feature/4-informal-english-greeting`, abierta el 2026-11-01) tenía aprobados la necesidad, el diseño, el plan y el test de aceptación `InformalGreetingAcceptanceTest`. Proponía añadir la variante `"Hi, <name>!"` porque la pedía un cliente nuevo, una cadena de gimnasios, y sin cambiar `greet(name)`. No llegó a tener código de producción.

El cliente ya no pide el saludo informal, y ningún otro cliente lo había pedido.

Archivo de la Feature 4: etiqueta [`archive/feature-4`](https://github.com/BrunoAmeneiros/agent-dev-sandbox/tree/archive/feature-4) (commit `0084623`). Contiene:

- `docs/superpowers/specs/2026-11-01-4-informal-english-greeting-need.md`
- `docs/superpowers/specs/2026-11-02-4-informal-english-greeting-design.md`
- `docs/superpowers/plans/2026-11-02-4-informal-english-greeting.md`
- `tests/acceptance/InformalGreetingAcceptanceTest.java`

## Alternatives

1. **Construirla igualmente** por si otro cliente la pide. Se descarta: no hay demanda, y la necesidad ya decía que otro cliente sería otra Feature.
2. **Dejar la rama abierta y en pausa.** Se descarta: el plan se queda desfasado y la rama abierta da a entender que hay trabajo pendiente.
3. **Abandonarla.** Se elige esta.

## Decision

Se abandona la Feature 4 porque el cliente ya no la pide. No se rescata nada de la rama: sus documentos se conservan en la etiqueta `archive/feature-4`. La rama local se ha borrado y nunca se subió a origin.

## Consequences

- `Greeter` sigue devolviendo solo `"Hello, <name>!"` en inglés.
- Si un cliente vuelve a pedir un saludo informal, será una Feature nueva. Su fase de necesidad puede partir de la necesidad y el diseño que se guardan en la etiqueta.
- La Feature 4 no produjo efectos externos que haya que revertir.
