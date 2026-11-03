# 0003. Abandonar la Feature 12 (dummy)

Fecha: 2026-11-03 · Estado: Abandoned · Feature 13 (`feature/13-abandon-feature-12`)

## Context

La Feature 12 (`feature/12-dummy`, abierta el 2026-11-03) era una prueba. No tenía necesidad, diseño ni plan, así que no pasó de la fase de necesidad. Solo tenía un commit, `b579065` ("chore: dummy feature to test abandon"), que añade `dummy.txt`. La rama solo existía en local y nunca se subió a origin.

Archivo de la Feature 12: etiqueta [`archive/feature-12`](https://github.com/BrunoAmeneiros/agent-dev-sandbox/tree/archive/feature-12) (commit `b579065`).

## Decision

Se abandona la Feature 12. Motivo, en palabras de Bruno: "era una prueba y ya no hace falta".

## Consequences

- No queda nada pendiente: la Feature no tenía ningún objetivo de producto.
- El trabajo se conserva en la etiqueta `archive/feature-12`. Para recuperarlo: `git switch -c feature/12-dummy archive/feature-12`.
- La Feature 12 no produjo efectos externos que haya que revertir.
