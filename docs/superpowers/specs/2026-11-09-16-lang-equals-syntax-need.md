# Need: `--lang=es` y flags desconocidos sin fallar en silencio

Feature 16 · feature/16-lang-equals-syntax · 2026-11-09

**Outcome:** build-missing

## Problem

El equipo de operaciones genera los saludos del kiosco en España con un script que llama a la CLI con `--lang=es`, porque `--opción=valor` es la forma habitual en otras herramientas. La CLI solo reconoce `--lang` como palabra suelta: `--lang=es` le parece un argumento desconocido, lo ignora y saluda en inglés. Así, `greet Ana --lang=es` imprime `Hello, Ana!` con salida 0, y `greet Ana --lang=xx` hace lo mismo, sin dar error.

Llevan semanas recibiendo "Hello" en vez de "¡Hola!" sin enterarse, porque no hay aviso y la salida es 0. El problema de fondo es que la CLI falla en silencio: un flag mal escrito (`--lnag es`) o con un valor que no admite (`--shout=yes`) también se ignora. Si no se hace nada, cualquier script que use esas formas seguirá saliendo mal sin que nadie lo sepa hasta que un cliente se queje.

No es un bug: el diseño aprobado de la Feature 9 (`docs/superpowers/specs/2026-11-03-9-spanish-greeting-design.md`, línea 39) dice que los argumentos que no se reconocen se siguen ignorando. Arreglarlo cambia ese comportamiento aprobado, así que es una Feature. Se abrió primero como bug (`bugfix/16`, solo en local, ya borrada) y se convirtió en Feature con el mismo id.

## What exists

- `GreetingCli.run` reconoce `--lang <código>` (en cualquier posición después del nombre) y `--shout`. Con `--lang`, un código desconocido da `Unknown language: <código> (supported: en, es)` con salida 2 y un `--lang` sin valor da `Missing value for --lang (supported: en, es)`.
- Sin `--lang`, solo se mira el segundo argumento para `--shout`. Lo demás se ignora: `Ana foo` da `Hello, Ana!` y `Ana foo --shout` da `Hello, Ana!`. Lo fija `GreetingCliCharacterizationTest`.
- Regla del bugfix 10: un primer argumento que empieza por `--` no es un nombre (texto de uso, o error si hay `--lang`). La fijan `GreetingCliTest` y `FlagAsNameAcceptanceTest`.
- No hay ADRs sobre la sintaxis de los argumentos, ni Features abiertas que se solapen.
- El proyecto no usa ninguna librería de parsing de argumentos. La Feature 9 se hizo sin dependencias nuevas.

## Success

- `greet Ana --lang=es` imprime `¡Hola, Ana!`, igual que `greet Ana --lang es`, sin tocar el script del kiosco.
- `greet Ana --lang=xx` da el mismo error que `greet Ana --lang xx`.
- Un flag que empiece por `--` y la CLI no reconozca (`--lnag es`, `--shout=yes` porque `--shout` no admite valor) da un error claro, no saluda y termina con salida distinta de cero.
- Los argumentos sueltos que no son flags (`Ana foo`) se quedan como están ahora.
- Antes de publicar, operaciones recibe la lista de lo que cambia: qué flags dejan de ignorarse y qué error dan.

## Constraints

- El cambio de comportamiento se acepta para todos los scripts: un flag desconocido pasa a ser un error, sin periodo de transición. Esto sustituye, para los flags `--`, la restricción de la Feature 9 de que los scripts actuales no cambien.
- Hay que avisar a operaciones con la lista de cambios antes de publicar.
- Sin fecha límite, pero cuanto antes mejor: el kiosco sigue saludando mal.
- Lo mantiene el mismo equipo.

## Outcomes considered

1. `dont-build`: descartada. El kiosco lleva semanas en producción saludando mal sin que nadie lo sepa, y cualquier otro script que use `--opción=valor` o un flag mal escrito puede estar igual.
2. `adopt`: descartada. Una librería de parsing (picocli, commons-cli) trae de serie `--lang=es` y el error con flags desconocidos, pero añade una dependencia y obliga a cambiar todo el parsing. Eso pone en riesgo comportamientos que los tests tienen fijados (la regla `--` del bugfix 10, `Ana foo` ignorado, `Ana foo --shout`). Para dos reglas no compensa.
3. `build-missing`: **recomendada**. `GreetingCli` ya resuelve `--lang` y `--shout`. Solo faltan dos cosas: aceptar `--lang=<código>` igual que `--lang <código>`, y un error claro con salida distinta de cero para un flag `--` desconocido. Los argumentos sueltos se quedan como están. Además hay que preparar la lista de cambios para operaciones.
4. `build-all`: descartada. Rehacer el parsing desde cero no aporta nada a estas dos reglas y arriesga todo lo que ya está fijado.

## Decision

Bruno decide `build-missing`: aceptar `--lang=<código>` y dar error con los flags `--` desconocidos sobre el `GreetingCli` actual, sin dependencias nuevas, porque el problema es fallar en silencio y lo demás de la CLI ya funciona.

## For the design

- Texto exacto del error de un flag desconocido, y si lleva el texto de uso como pista (como `HINT`).
- Entradas límite de la forma con `=`: `--lang=` vacío, `--lang=es=x`, `--LANG=es`, `--lang=ES`, `--lang=es` repetido o combinado con `--lang en`.
- `--shout=<valor>`: cualquier valor es un error, ¿también `--shout=` vacío?
- Si `Ana foo --shout` sigue ignorando `--shout`: es un flag conocido en otra posición, no uno desconocido, y lo fija la caracterización actual.
- Flags con un solo guion (`-x`) y el argumento `--` suelto: ¿cuentan como flags?
- Si el error con flag desconocido aplica también sin `--lang` (camino del bugfix 10) y cómo convive con la regla del primer argumento `--`.
- Cómo y dónde se entrega a operaciones la lista de cambios (dimensión deployment).
