# Need: `--help` muestra la ayuda

Feature 18 · feature/18-restore-help-option · 2026-11-14

**Outcome:** build-missing

## Problem

Desde la Feature 16, `greet --help` termina con `Unknown option: --help (supported: --lang <code>, --lang=<code>, --shout)` y salida 2, y `greet Ana --help` también (antes saludaba). Lo encontró alguien de operaciones: al revisar sus scripts con la lista de cambios de la Feature 16, escribió `greet --help` para ver qué opciones hay y le salió un error.

Lo usan personas, no scripts: ningún script llama con `--help`. Si no se hace nada, para ver las opciones hay que provocar un error y leer la lista de "supported", o mirar el código.

## What exists

- `GreetingCli` tiene un texto de uso (`Usage: greet <name> [--shout] [--lang en|es]`). Hoy solo se muestra por la regla del bugfix 10: un primer argumento que empieza por `--` y es una opción conocida no es un nombre. No existe una opción de ayuda propiamente dicha.
- La Feature 16 convirtió `--help` en opción desconocida a propósito. Lo dice la lista de cambios para operaciones, `docs/changes/2026-11-09-16-unknown-options.md` (sección "Otras opciones desconocidas: `--help`"): `--help` pasa de mostrar el uso con salida 0 a error con salida 2, y `Ana --help` pasa de `Hello, Ana!` con salida 0 a error con salida 2.
- No hay ADR sobre el tema ni otra Feature abierta que lo toque.
- La biblioteca estándar de Java no trae un parser de argumentos. Una librería como picocli lo daría, pero el proyecto no quiere dependencias nuevas.

## Success

- `greet --help` y `greet Ana --help`, es decir `--help` en cualquier posición, muestran las opciones y terminan con salida 0, sin saludar.
- `-h` no cambia: en la Feature 16 los argumentos con un solo guion quedaron como argumentos sueltos, y así siguen.
- El resto de comportamientos de la Feature 16 no cambia.
- Operaciones recibe una nota corta con lo que cambia: `--help` y `Ana --help` pasan de error con salida 2 a la ayuda con salida 0.

## Constraints

- No hay fecha límite.
- Sin dependencias nuevas.
- Hay que avisar a operaciones con una nota corta, aunque ningún script use `--help`, porque la lista de cambios de la Feature 16 dice que `--help` da error.

## Outcomes considered

1. `dont-build`: no encaja. Una persona real se queda sin forma de pedir ayuda, y la lista de cambios de la Feature 16 la remite a un error.
2. `adopt`: no hay nada en el stack que dé `--help`. Una librería (picocli) iría contra "sin dependencias nuevas" y obligaría a rehacer el parseo de la Feature 16 que se quiere mantener.
3. `build-missing`: `--help` pasa a ser una opción conocida en `GreetingCli`, en cualquier posición, con salida 0 y sin saludo. Se aprovecha el texto de uso que ya existe y se añade la nota para operaciones. El resto del parseo no cambia. **Recomendada.**
4. `build-all`: rehacer el parseo de opciones. Sería desproporcionado para una sola opción, y pone en riesgo comportamientos que se quieren conservar.

## Decision

`build-missing`, como se recomendó: añadir solo `--help` como ayuda sobre la CLI actual, sin dependencias, y avisar a operaciones.

## For the design

- Qué texto muestra la ayuda. El uso actual no menciona `--lang=<code>` ni `--help`. ¿Se reutiliza tal cual o se amplía?
- Si la ayuda sale por stdout o por stderr.
- Qué pasa cuando `--help` va junto a otro problema: `--help --foo`, `Ana --foo --help`, `Ana --lang xx --help`, `--lang es --help`, `--help` como valor de `--lang` (`Ana --lang --help`), o `--help` repetido.
- Dónde va la nota para operaciones: un fichero nuevo en `docs/changes/` o una sección en el de la Feature 16.
