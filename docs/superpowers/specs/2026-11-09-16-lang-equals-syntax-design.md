# Design: `--lang=<code>` y error con flags desconocidos

Feature 16 · feature/16-lang-equals-syntax · 2026-11-09

Necesidad aprobada: [2026-11-09-16-lang-equals-syntax-need.md](2026-11-09-16-lang-equals-syntax-need.md). Allí están el problema (el kiosco recibe `Hello` con `--lang=es`, en silencio), el éxito, las restricciones y por qué es `build-missing`. Este documento no lo repite.

## Dimensions

| Dimension | Applies | Section / reason |
| --- | --- | --- |
| alternatives | yes | Alternativas |
| threats | no | CLI local sin red, sin permisos y sin datos sensibles; solo repite en su propio stderr el argumento que recibió |
| invalid-input | yes | Entradas inválidas y casos límite |
| failures | no | No hay dependencias (base de datos, colas, servicios); cada llamada es un proceso independiente sin estado |
| non-functional | yes | Compatibilidad |
| observability | yes | Observabilidad |
| deployment | yes | Despliegue y aviso a operaciones |
| acceptance | yes | Criterios de aceptación |

## Intención

El script del kiosco de operaciones llama a la CLI con `--lang=es` y debe recibir `¡Hola, <name>!` sin cambiar el script. Además, la CLI deja de fallar en silencio: cualquier argumento que empiece por `--` y no sea un flag conocido es un error con salida 2, esté donde esté. Los argumentos sueltos que no son flags (`Ana foo`) siguen ignorándose como hoy.

## Objetivos, no objetivos y antiobjetivos

- **Objetivos:** `--lang=<code>` equivale a `--lang <code>`; un flag `--` desconocido da `Unknown option` con salida 2; la lista de cambios para operaciones existe antes de publicar.
- **No objetivos:** sugerir el flag correcto ("¿quisiste decir…?"); tratar `--` como fin de opciones; aceptar `--shout=<valor>`; cambiar el texto de uso; flags con un solo guion; más idiomas.
- **Antiobjetivos:** que cambie la salida de una llamada sin flags desconocidos ni `--lang=` (salvo `--` suelto en primera posición, decidido abajo); añadir dependencias; un periodo de transición o un modo "compatible" (decisión de Bruno en la necesidad).

## Alternativas

Decidido en la necesidad: el parsing se amplía a mano en `GreetingCli`, sin librería (picocli o commons-cli añadirían una dependencia y obligarían a rehacer un parsing cuyos rincones fijan los tests). Rehacer el parsing desde cero tampoco: no aporta nada a estas dos reglas. No es una decisión de arquitectura: no lleva ADR.

## Comportamiento de la CLI

Forma: `greet <name> [--shout] [--lang <code> | --lang=<code>]`.

**Flags conocidos:** `--shout` (sin valor), `--lang` (el valor es el argumento siguiente) y `--lang=<code>` (el valor es lo que va tras el **primer** `=`). Los nombres de flag distinguen mayúsculas (`--LANG` no es conocido); el código de idioma no, como hoy (`Language.fromCode`).

**Flag desconocido:** cualquier argumento que empiece por `--` y no sea uno de los tres anteriores. Incluye `--` suelto, `--shout=` y `--shout=<valor>`, `--LANG`, `--lnag`. Un argumento con un solo guion (`-x`) no es un flag: es un argumento suelto.

**La llamada "lleva `--lang`"** si algún argumento es exactamente `--lang` o empieza por `--lang=`. En el primero se mira tras recortarlo, como hoy. Es lo que decide si se usa el camino con idioma o el de siempre.

**Mensaje nuevo** (decisión de Bruno), una línea por stderr, nada por stdout, salida 2, sin la pista de uso (`HINT`), igual que `Unknown language`:

```
Unknown option: <argumento> (supported: --lang <code>, --lang=<code>, --shout)
```

`<argumento>` es el argumento tal como llegó; el primero, recortado (como en `The name must come first, got: …`) *(decisión mía: el primero ya se recorta hoy para todo)*. Los argumentos detrás del nombre no se recortan, como hoy: `" --foo"` no empieza por `--` y es un argumento suelto.

**Orden de comprobación.** Se informa solo del **primer problema de izquierda a derecha** (decisión de Bruno):

1. **Primer argumento** (recortado):
   - vacío: texto de uso con salida 0 si no lleva `--lang`; `Missing name (usage: …)` con salida 2 si lo lleva. Como hoy.
   - empieza por `--` y es un flag **desconocido**: `Unknown option: <arg>`, salida 2, lleve o no `--lang` (decisión de Bruno, fila 18).
   - empieza por `--` y es un flag **conocido** (`--shout`, `--lang`, `--lang=…`): como hoy. Texto de uso con salida 0 si la llamada no lleva `--lang`; `The name must come first, got: <arg> (usage: …)` con salida 2 si lo lleva. Como `--lang=es` cuenta como `--lang`, `--lang=es Ana` y `--lang=es` solo dan ese error.
   - **El texto de uso no es un problema** (decisión de Bruno): cuando el primer argumento daría el texto de uso con salida 0 (vacío o flag conocido, sin `--lang`), antes se miran los argumentos restantes. Si alguno es un flag desconocido, el resultado es `Unknown option: <el primero de ellos>` con salida 2. Si no hay ninguno, el texto de uso, como hoy. Los dos errores del primer argumento (`Missing name`, `The name must come first`) sí son problemas en la posición 0 y ganan a cualquier flag desconocido posterior *(consecuencia de "el primer problema de izquierda a derecha")*.
2. **Resto de argumentos**, en orden. Problemas posibles, cada uno en su posición:
   - un flag desconocido, en su posición;
   - el **último** `--lang` (en cualquiera de sus formas), en su posición: valor que falta (`--lang` al final o `--lang=` vacío) da `Missing value for --lang (supported: en, es)`; código desconocido da `Unknown language: <code> (supported: en, es)`. Un `--lang` sobrescrito por otro posterior **no** se valida (decisión de Bruno): así siguen `lastLangWins` y `trailingLangAfterUnknownCodeIsMissingValue`.
   - `--lang` (separado) toma siempre el argumento siguiente como código, aunque empiece por `--`, como hoy. Ese argumento no se mira como flag.
3. Sin problemas: el saludo, como hoy. En el camino sin `--lang`, `--shout` solo cuenta en segunda posición (`Ana foo --shout` da `Hello, Ana!`); en el camino con `--lang`, en cualquier posición. stdout en UTF-8 si el idioma final es `es`.

## Entradas inválidas y casos límite

Decididas con Bruno, una a una. "2" es la salida.

| # | Entrada | Resultado |
| --- | --- | --- |
| 1 | `Ana --lang=es` | `¡Hola, Ana!`, 0, stdout UTF-8 |
| 2 | `Ana --lang=ES` | `¡Hola, Ana!`, 0 |
| 3 | `Ana --lang=xx` | `Unknown language: xx (supported: en, es)`, 2 |
| 4 | `Ana --lang=` | `Missing value for --lang (supported: en, es)`, 2 |
| 5 | `Ana --lang=es=x` | `Unknown language: es=x (supported: en, es)`, 2 |
| 6 | `Ana --LANG=es`, `Ana --LANG es` | `Unknown option: --LANG=es` / `Unknown option: --LANG`, 2 |
| 7 | `Ana --lang=xx --lang es`, `Ana --lang xx --lang=es` | gana el último: `¡Hola, Ana!`, 0 |
| 8 | `Ana --lang --shout` | `Unknown language: --shout (supported: en, es)`, 2 (como hoy) |
| 9 | `Ana --shout=yes`, `Ana --shout=` | `Unknown option: --shout=yes` / `Unknown option: --shout=`, 2 |
| 10 | `Ana --foo`, `Ana --lnag es` | `Unknown option: --foo` / `Unknown option: --lnag`, 2 |
| 11 | `Ana --foo --bar` | `Unknown option: --foo`, 2 (solo el primero) |
| 12 | `Ana --lang xx --foo` | `Unknown language: xx (supported: en, es)`, 2 (primer problema) |
| 12b | `Ana --lang xx --lang es --foo` | `Unknown option: --foo`, 2 (el `xx` sobrescrito no cuenta) |
| 12c | `Ana --foo --lang xx` | `Unknown option: --foo`, 2 |
| 13 | `Ana --` | `Unknown option: --`, 2 |
| 14 | `Ana -x` | `Hello, Ana!`, 0 (argumento suelto, como hoy) |
| 15 | `Ana foo --shout` | `Hello, Ana!`, 0 (como hoy) |
| 16 | `Ana --shout --shout` | `HELLO, ANA!`, 0 (como hoy) |
| 17 | `--lang=es Ana`, `--lang=es` | `The name must come first, got: --lang=es (usage: greet <name> [--shout] [--lang en\|es])`, 2 |
| 18 | `--foo Ana`, `--foo`, `--`, `  --foo  Ana` | `Unknown option: --foo` / `Unknown option: --`, 2 |
| 19 | `--shout Ana`, `--shout` | texto de uso, 0 (como hoy, bugfix 10) |
| 20 | `--foo Ana --lang es` | `Unknown option: --foo`, 2 (primer problema) |
| 21 | `Ana --lang=--shout` | `Unknown language: --shout (supported: en, es)`, 2 *(decisión mía: el valor tras `=` se toma tal cual, como con `--lang`)* |
| 22 | `Ana --lang=es --shout`, `Ana --shout --lang=es` | `¡HOLA, ANA!`, 0 |
| 23 | `--shout Ana --foo`, `--shout --foo` | `Unknown option: --foo`, 2 (decisión de Bruno: gana al texto de uso) |
| 24 | `"" --foo`, `"   " Ana --foo` | `Unknown option: --foo`, 2 (decisión de Bruno) |
| 25 | `""`, `"   "`, sin argumentos | texto de uso, 0 (como hoy) |
| 26 | `"" --foo --lang es`, `--shout Ana --lang es --foo` | `Missing name (usage: …)` / `The name must come first, got: --shout (usage: …)`, 2 (el error del primer argumento va antes) |

Filas 12b, 12c, 20, 22 y 26 son consecuencia de las reglas acordadas, no filas que se discutieran una a una.

## Compatibilidad

Cambio que rompe, aceptado en la necesidad sin transición: toda llamada con un flag `--` desconocido (en cualquier posición) pasa de saludo o texto de uso con salida 0 a `Unknown option` con salida 2. También cambian las llamadas con `--lang=`, que hoy se ignora (filas 1–5, 7 segundo caso, 17, 21 y 22), y `--` suelto en primera posición (fila 18). Una llamada sin flags desconocidos, sin `--lang=` y sin `--` suelto da exactamente lo mismo que hoy. Rendimiento y límites no cambian (un recorrido de los argumentos).

**Texto de uso sin cambios** *(decisión mía, aceptada por Bruno)*: `Usage: greet <name> [--shout] [--lang en|es]` y `HINT` se quedan; la forma `--lang=<code>` ya aparece en el `supported` del error nuevo.

**Test existente que cambia** (decisión de Bruno, filas 13 y 18): `GreetingCliTest.doubleDashAlonePrintsUsage` (`run("--")` espera hoy el texto de uso con salida 0) pasa a esperar `Unknown option: -- (supported: --lang <code>, --lang=<code>, --shout)` con salida 2. Es el único test del bugfix 10 que cambia: los demás de `GreetingCliTest` con un flag en primera posición y `FlagAsNameAcceptanceTest` usan flags conocidos. El nombre del test cambia con él *(decisión mía: p. ej. `doubleDashAloneIsUnknownOption`)*.

## Observabilidad

No hay logs ni métricas en la CLI. La señal es el error en stderr y la salida 2: operaciones puede buscar `Unknown option:` en los logs de sus scripts. Confirmación en producción: el kiosco saluda `¡Hola, …!` sin haber tocado su script.

## Despliegue y aviso a operaciones

Sin migraciones ni feature flags. Revertir es volver a la versión anterior.

Antes de publicar, operaciones recibe la lista de cambios (decisión de Bruno en la necesidad): un fichero `docs/changes/2026-11-09-16-unknown-options.md` *(decisión mía, aceptada por Bruno)* con una tabla "antes → ahora" de **cada** llamada de la tabla de entradas cuyo resultado cambia respecto a hoy *(decisión mía, tras la revisión)*: filas 1–6, 7 (segundo caso, `Ana --lang xx --lang=es`: hoy `Unknown language: xx`), 9–11, 12b, 12c, 13, 17, 18, 20, 21, 22, 23 y 24, el texto exacto de `Unknown option` y la nota de que no hay periodo de transición. Se enlaza desde el PR; quien publica se lo pasa a operaciones antes de sacar la versión.

Lista comprobada (corrección de Bruno: la fila 12 no cambia) ejecutando la CLI de `develop` (71bdbd7) con cada entrada de la tabla: todas las filas de la lista cambian de resultado, y las que no están (7 primer caso, 8, 12, 14–16, 19, 25, 26) dan hoy lo mismo que tras la Feature. Tres de la lista ya salen hoy con 2: en 12c (`Unknown language: xx` → `Unknown option: --foo`) y 20 (`The name must come first, got: --foo` → `Unknown option: --foo`) solo cambia el mensaje, y en 7 segundo caso la salida pasa de 2 a 0 con `¡Hola, Ana!`. La tabla para operaciones lo distingue.

Documentación a actualizar: el Javadoc de `main` y de `run` (formas de `--lang`, flags desconocidos) y la fila Run de `docs/project-map.md` (vía el mapper). El README no documenta los flags de la CLI: no se toca.

## Criterios de aceptación

Por subproceso o por `main`, comprobando stdout, stderr y salida:

1. `Ana --lang=es` da `¡Hola, Ana!` (bytes UTF-8 también con `LANG=C`), salida 0. (Necesidad: éxito 1.)
2. `Ana --lang=xx` da por stderr `Unknown language: xx (supported: en, es)`, stdout vacío, salida 2, igual que `Ana --lang xx`. (Necesidad: éxito 2.)
3. `Ana --lnag es` y `Ana --shout=yes` dan `Unknown option: --lnag (…)` / `Unknown option: --shout=yes (…)` con el texto exacto del mensaje, stdout vacío, salida 2. (Necesidad: éxito 3.)
4. `--foo Ana` y `--shout Ana --foo` dan `Unknown option: --foo (…)`, salida 2. (Filas 18 y 23.)
5. `Ana foo` sigue dando `Hello, Ana!` con salida 0. (Necesidad: éxito 4.)
6. Todas las filas de la tabla de entradas se cumplen.
7. Existe `docs/changes/2026-11-09-16-unknown-options.md` con la tabla antes → ahora. (Necesidad: éxito 5.)

Test de aceptación nuevo: `tests/acceptance/UnknownOptionAcceptanceTest.java`, que lanza la CLI como subproceso igual que `SpanishGreetingAcceptanceTest`, con los criterios 1–5. Se escribe antes de aprobar el plan y se aprueba con él. Con el código de hoy falla como clase: los métodos de los criterios 1, 3 y 4 fallan; los que ya pasan hoy (criterio 5 y la mitad `--lang xx` del 2) se quedan como guardas *(decisión mía, tras la revisión)*. Los tests de aceptación existentes no se tocan.

## Impact

**Environment gate:** passed - `mvn -B verify -Pacceptance`, 2026-11-09 (23b0627): 58 tests run, 0 failed, 0 already known; by kind: unit 49/0, acceptance 9/0.

Del análisis del mapper ([2026-11-09-16-lang-equals-syntax-impact.md](2026-11-09-16-lang-equals-syntax-impact.md)), actualizado con lo que decide este diseño.

### Zonas

| Zone | Path | Change | Why |
| --- | --- | --- | --- |
| CLI | src/main/java/sandbox/GreetingCli.java | modified | `run` acepta `--lang=<code>`, detecta `--lang=` para decidir el camino y da `Unknown option` con flags `--` desconocidos en cualquier posición |

`Greeter` y `Language` no se tocan (`Language.fromCode` se reutiliza).

### Contratos

- Argumentos de la CLI: `--lang=<code>` nuevo, no rompe. Consumidores: el script del kiosco, `SpanishGreetingAcceptanceTest`, `GreetingCliTest`, la fila Run del mapa.
- Comportamiento: flag `--` desconocido en cualquier posición pasa a salida 2 (rompe, aceptado). `--` suelto en primera posición pasa de texto de uso a error.
- Salida: mensaje nuevo `Unknown option: …` por stderr. Texto de uso, `HINT`, `Unknown language`, `Missing value for --lang`, `Missing name` y `The name must come first` no cambian.

### Datos

Ninguno.

### Cobertura

`GreetingCli`: 86 % de líneas (37 de 43), 20 de 24 ramas (jacoco del gate). La CI pide 80 % del código cambiado: cada rama nueva lleva su test en `GreetingCliTest`.

### Characterization needed

Corrección de Bruno al análisis: la lista del mapper es de **candidatos**. Tras aprobar el diseño se fija solo lo que el diseño mantiene. Lo que la necesidad ya decidió cambiar (flags `--` desconocidos detrás del nombre, con o sin `--lang`) no se caracteriza: lo cubren los tests de aceptación. Lo que decide el diseño (`--lang` repetido, `-x`, `--` suelto, `--lang --shout`) se caracteriza si se mantiene.

Con lo que decide este diseño:

| Zone | Behaviour to pin | Why |
| --- | --- | --- |
| CLI | `--lang` repetido, gana el último (`Ana --lang xx --lang es` → `¡Hola, Ana!`; `Ana --lang xx --lang` → `Missing value for --lang`) | Se mantiene (fila 7) |
| CLI | `Ana --lang --shout` → `Unknown language: --shout`, 2 | Se mantiene (fila 8) |
| CLI | `Ana -x` → `Hello, Ana!`, 0; `-x` → `Hello, -x!`, 0 | Se mantiene (fila 14) |

No se caracterizan: `--` suelto (cambia, filas 13 y 18) ni los flags desconocidos (cambian). `Ana foo`, `Ana foo --shout` y `Ana --shout --shout` ya los fija `GreetingCliCharacterizationTest`.

Hoy algunos de estos casos están en `GreetingCliTest`, que no es ruta protegida. Los nuevos tests van en una clase nueva, `src/test/java/sandbox/GreetingCliOptionsCharacterizationTest.java` *(decisión mía: así no se reabre la `GreetingCliCharacterizationTest` ya aprobada)*, y se aprueban con el plan.

### Riesgos

- `hasLang` hoy solo mira `--lang` exacto: si no se amplía a `--lang=`, `--lang=es` cae en el camino sin idioma y ahora daría `Unknown option`.
- La regla del primer argumento (bugfix 10) convive con la nueva: flag conocido en primera posición, como hoy; desconocido, error.
