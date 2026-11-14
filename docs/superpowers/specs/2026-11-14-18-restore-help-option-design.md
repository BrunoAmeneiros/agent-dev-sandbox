# Design: `--help` muestra la ayuda

Feature 18 · feature/18-restore-help-option · 2026-11-14

Necesidad aprobada: [2026-11-14-18-restore-help-option-need.md](2026-11-14-18-restore-help-option-need.md). Allí están el problema (alguien de operaciones escribe `greet --help` para ver las opciones y recibe un error con salida 2), el éxito, las restricciones y por qué es `build-missing`. Este documento no lo repite.

## Dimensions

| Dimension | Applies | Section / reason |
| --- | --- | --- |
| alternatives | yes | Alternativas |
| threats | no | CLI local sin red, sin permisos y sin datos sensibles; la ayuda es un texto fijo que no repite nada de la entrada |
| invalid-input | yes | Entradas inválidas y casos límite |
| failures | no | No hay dependencias (base de datos, colas, servicios); cada llamada es un proceso independiente sin estado |
| non-functional | yes | Compatibilidad |
| observability | no | CLI local sin logs ni métricas; la confirmación en producción es que operaciones ejecute `greet --help` a mano y vea la ayuda con salida 0 |
| deployment | yes | Despliegue y aviso a operaciones |
| acceptance | yes | Criterios de aceptación |

## Intención

Quien escribe `--help` está pidiendo ayuda, y debe recibirla: el texto de ayuda por stdout, con salida 0 y sin saludo, esté donde esté `--help`. Hoy recibe `Unknown option: --help` con salida 2. Las llamadas sin `--help` hacen exactamente lo mismo que hoy.

## Objetivos, no objetivos y antiobjetivos

- **Objetivos:** `--help` en cualquier posición muestra la ayuda con salida 0 y sin saludo, aunque haya otros problemas en la línea. La única excepción es que vaya como valor de `--lang`. Operaciones recibe una nota corta.
- **No objetivos:** `-h` u otras formas con un solo guion; ayuda traducida con `--lang es`; añadir `--help` a los mensajes de error; cambiar el texto de uso del bugfix 10.
- **Antiobjetivos:** que cambie el resultado de cualquier llamada sin `--help`; añadir dependencias; tocar los tests aprobados de la Feature 16.

## Alternativas

Las tres decisiones de comportamiento, cada una con sus opciones y lo que decidió Bruno:

1. **Precedencia de `--help` frente a otros problemas.**
   - A. `--help` gana siempre.
   - B. El primer problema de izquierda a derecha, como entre una opción desconocida y `--lang` en la Feature 16.

   **Decisión de Bruno: A**, porque responde a quien pide ayuda y es lo que hacen git y mvn. Hay una excepción, por coherencia con la Feature 16: `--help` justo detrás de `--lang`, o pegado como `--lang=--help`, es el valor de `--lang`.
2. **Texto de la ayuda.**
   - A. Reutilizar el uso actual.
   - B. Un texto propio solo para `--help`.
   - C. Ampliar el uso actual y mostrarlo también en el camino del bugfix 10.

   **Decisión de Bruno: B**, por stdout, con el texto de la sección "Comportamiento de la CLI". A no muestra todas las opciones, y C cambiaría lo que ve `--shout Ana`.
3. **Mensajes de error que listan opciones** (`Unknown option … (supported: …)` y el `(usage: …)` de `Missing name` y `The name must come first`).
   - A. Dejarlos como están.
   - B. Añadir `--help`.

   **Decisión de Bruno: A**, porque mantiene el cambio pequeño y no toca los textos que fijan los tests aprobados de la Feature 16.

Implementación: el parseo se amplía a mano en `GreetingCli`, como en la Feature 16. Una librería (picocli) añadiría una dependencia, y la necesidad la descarta. No es una decisión de arquitectura, así que no lleva ADR.

## Comportamiento de la CLI

**Regla.** Si algún argumento es exactamente `--help`, la llamada muestra la ayuda: stdout con el texto de abajo, stderr vacío y salida 0. Da igual lo que haya en el resto de la línea, salvo que ese `--help` sea el valor de `--lang`. Un argumento es el valor de `--lang` cuando va justo detrás de un `--lang` que está detrás del nombre (ese `--lang` lo consume, como hoy) o cuando va pegado tras `--lang=`. También cuenta como `--help` el primer argumento que, recortado, es `--help`, porque el primer argumento ya se recorta hoy (fila 13). Detrás del nombre la comparación es exacta: no se recorta y distingue mayúsculas.

En el camino del primer argumento, el que empieza por `--` o está vacío, `--lang` no consume valores: hoy ya falla antes con `The name must come first`. Para la excepción vale lo mismo: un `--help` justo detrás de un `--lang`, esté donde esté ese `--lang`, es su valor y no da la ayuda (filas 6, 8 y 9).

**Texto de la ayuda** (decisión de Bruno, con su ajuste en la línea de uso). Exactamente estas líneas, separadas por saltos de línea y sin salto final, porque `main` añade el último con `println`:

```
Usage: greet <name> [--shout] [--lang <code> | --lang=<code>] [--help]
  --shout          print the greeting in upper case
  --lang <code>    language of the greeting: en, es (default en)
  --lang=<code>    same as --lang <code>
  --help           show this help
```

Es ASCII, así que se escribe como el resto de las salidas sin `--lang es` (`utf8Stdout` falso), también cuando la línea lleva `--lang es` *(decisión mía: la ayuda está en inglés y es la misma en todos los casos)*. El separador de líneas es `\n` *(decisión mía: lo que imprima `println` al final es el separador de la plataforma, como hoy)*.

**Sin cambios:**
- el texto de uso del bugfix 10 (`Usage: greet <name> [--shout] [--lang en|es]`), que siguen mostrando `--shout Ana`, `--shout`, `""`, `"   "` y la llamada sin argumentos;
- `HINT`;
- `Unknown option …`, `Unknown language …`, `Missing value for --lang`, `Missing name` y `The name must come first`;
- cualquier llamada sin `--help`.

## Entradas inválidas y casos límite

Las 14 filas, confirmadas por Bruno. "ayuda, 0" significa el texto de ayuda por stdout, stderr vacío y salida 0, sin saludo. La columna "Hoy" es lo que da `develop` (901b343).

| # | Llamada | Resultado | Hoy |
| --- | --- | --- | --- |
| 1 | `--help`, `--help Ana`, `Ana --help` | ayuda, 0 | `Unknown option: --help (…)`, 2 |
| 2 | `--help --help`, `Ana --help --help` | ayuda, 0 (repetir no cambia nada) | `Unknown option: --help (…)`, 2 |
| 3 | `--help --foo`, `Ana --foo --help` | ayuda, 0 (`--help` gana) | `Unknown option: --help` / `--foo`, 2 |
| 4 | `Ana --lang xx --help`, `Ana --lang= --help` | ayuda, 0 (`--help` gana) | `Unknown language: xx` / `Missing value for --lang`, 2 |
| 5 | `--lang es --help`, `--lang=es --help`, `"" --help`, `--shout --help` | ayuda, 0 (`--help` gana también en el camino del primer argumento) | `The name must come first` (las dos con `--lang`) / `Unknown option: --help` (`""` y `--shout`), 2 |
| 6 | `Ana --lang --help` | `Unknown language: --help (supported: en, es)`, 2 | igual |
| 7 | `Ana --lang=--help` | `Unknown language: --help (supported: en, es)`, 2 (como la fila 21 de la Feature 16) | igual |
| 8 | `Ana --lang --help --help` | ayuda, 0 (el primer `--help` es el valor de `--lang`; el segundo sí es ayuda) | `Unknown language: --help`, 2 |
| 9 | `--lang --help` (sin nombre) | `The name must come first, got: --lang (usage: greet <name> [--shout] [--lang en\|es])`, 2 (`--help` es el valor de `--lang`) | igual |
| 10 | `Ana --HELP`, `Ana --Help` | `Unknown option: --HELP` / `--Help (supported: --lang <code>, --lang=<code>, --shout)`, 2 (como `--LANG`, fila 6 de la Feature 16) | igual |
| 11 | `Ana --help=yes`, `Ana --help=` | `Unknown option: --help=yes` / `--help= (…)`, 2 (como `--shout=yes`, fila 9 de la Feature 16) | igual |
| 12 | `Ana -h`, `Ana -help`, `Ana help` | `Hello, Ana!`, 0 (argumentos sueltos) | igual |
| 13 | `"  --help  "` como primer argumento, solo o seguido de `Ana` | ayuda, 0 (el primer argumento se recorta) | `Unknown option: --help`, 2 |
| 14 | `Ana " --help"` | `Hello, Ana!`, 0 (detrás del nombre no se recorta y no empieza por `--`: argumento suelto) | igual |

Filas que cambian: 1–5, 8 y 13. Filas que no cambian: 6, 7, 9–12 y 14.

Hay dos casos que no están en la tabla pero que la regla resuelve (revisión):
- `Ana --lang --lang --help` muestra la ayuda: el primer `--lang` consume el segundo, así que `--help` no es un valor.
- `--shout --lang --lang --help` da `The name must come first, got: --shout (usage: …)`, 2: en el camino del primer argumento, un `--help` justo detrás de un `--lang` es su valor.

La columna "Hoy" está comprobada ejecutando la CLI de `develop` (901b343) con cada llamada de la tabla.

## Compatibilidad

Es un cambio que rompe, a propósito: toda llamada en la que `--help` da la ayuda según la regla (filas 1–5, 8 y 13) pasa de un error con salida 2 a la ayuda con salida 0. Ningún script usa `--help` (necesidad). Las llamadas sin `--help`, y aquellas cuyo único `--help` es el valor de `--lang` (filas 6, 7 y 9), dan exactamente lo mismo que hoy.

Rendimiento y límites no cambian: sigue siendo un recorrido de los argumentos.

**Tests existentes:** ningún test menciona hoy `--help` (análisis del mapper). Ningún test aprobado cambia, y los textos que fijan los tests de la Feature 16 no se tocan.

## Despliegue y aviso a operaciones

No hay migraciones ni feature flags. Revertir es volver a la versión anterior.

**Nota para operaciones** (decisión de Bruno: un fichero nuevo). Va en `docs/changes/2026-11-14-18-help-option.md` y es corta:
- dice que corrige la sección "Otras opciones desconocidas: `--help`" de `docs/changes/2026-11-09-16-unknown-options.md`;
- incluye una tabla "Hoy → Ahora" con `--help` y `Ana --help`, que pasan de `Unknown option: --help (supported: --lang <code>, --lang=<code>, --shout)` con salida 2 a la ayuda con salida 0;
- añade una frase: cualquier llamada con `--help` da la ayuda, salvo cuando `--help` es el valor de `--lang`.

*(Decisión mía: la frase cubre el resto de las filas que cambian sin alargar la tabla.)* El fichero de la Feature 16 no se toca: es el registro de aquel cambio. La nota se enlaza desde el PR.

**Documentación a actualizar:**
- el Javadoc de la clase, de `main` y de `run` (`--help` y su precedencia);
- la zona CLI y la fila Run de `docs/project-map.md`, a través del mapper.

El README no documenta las opciones de la CLI: no se toca.

## Criterios de aceptación

Por subproceso, comprobando stdout, stderr y salida:

1. `greet --help` y `greet Ana --help` muestran el texto de ayuda exacto por stdout, con stderr vacío y salida 0, sin saludo. (Necesidad: éxito 1.)
2. `--help` gana a otros problemas: `--help --foo`, `Ana --foo --help`, `Ana --lang xx --help` y `--lang es --help` dan la ayuda con salida 0. (Alternativa 1, filas 3–5.)
3. `--help` como valor de `--lang` no es ayuda: `Ana --lang --help` y `Ana --lang=--help` dan por stderr `Unknown language: --help (supported: en, es)`, con stdout vacío y salida 2. (Filas 6 y 7.)
4. `Ana -h` sigue dando `Hello, Ana!` con salida 0. (Necesidad: éxito 2.)
5. `--shout Ana` sigue mostrando el texto de uso del bugfix 10 (`Usage: greet <name> [--shout] [--lang en|es]`) con salida 0, y `Ana --foo` sigue dando `Unknown option: --foo (supported: --lang <code>, --lang=<code>, --shout)` con salida 2. (Necesidad: éxito 3; alternativas 2 y 3.)
6. Se cumplen todas las filas de la tabla de entradas.
7. Existe `docs/changes/2026-11-14-18-help-option.md` con la tabla "Hoy → Ahora" de `--help` y `Ana --help` y la referencia a la nota de la Feature 16. (Necesidad: éxito 4.)

Los tests de aceptación los escribe el test-author antes del plan y se aprueban con él. Los criterios 4 y 5 ya pasan hoy: quedan como guardas.

## Impact

**Environment gate:** passed - `mvn -B verify -Pacceptance`, 2026-11-14 (901b343): 101 tests run, 0 failed, 0 already known; by kind: unit 84/0, acceptance 17/0.

Parte del análisis del mapper ([2026-11-14-18-restore-help-option-impact.md](2026-11-14-18-restore-help-option-impact.md)), actualizado con lo que decide este diseño.

### Zonas

| Zone | Path | Change | Why |
| --- | --- | --- | --- |
| CLI | src/main/java/sandbox/GreetingCli.java | modified | `--help` pasa a ser una opción conocida que gana en los dos caminos (`run` y `firstArgumentResult`), salvo como valor de `--lang`. Hay un texto de ayuda nuevo, y cambian el Javadoc de la clase, de `main` y de `run`. `UNKNOWN_OPTION_HINT`, `USAGE` y `HINT` no cambian. |
| Ops change note | docs/changes/2026-11-14-18-help-option.md | new | Nota corta para operaciones. El fichero de la Feature 16 no se toca. |
| Project map | docs/project-map.md | modified | La zona CLI y la fila Run describen `--help` como opción desconocida. El mapper las actualiza después de la Feature. |

`Greeter` y `Language` no se tocan.

### Contratos

- Argumentos de la CLI: `--help` en cualquier posición pasa de opción desconocida (stderr, salida 2) a ayuda (stdout, salida 0), salvo como valor de `--lang`. El cambio rompe, a propósito. Consumidores: personas (ningún script lo usa), la nota de la Feature 16 (corregida por la nota nueva) y la fila Run del mapa.
- Salida: hay un texto de ayuda nuevo. No cambian `Unknown option … (supported: --lang <code>, --lang=<code>, --shout)` ni el texto de uso del bugfix 10, ni `HINT`, `Unknown language`, `Missing value for --lang`, `Missing name` y `The name must come first`.
- No cambian `-h` ni los demás argumentos con un solo guion, ni `--lang`, `--lang=<code>`, `--shout` y los códigos de salida de las llamadas sin `--help`.

### Datos

Ninguno.

### Cobertura

`GreetingCli`: 91,7 % de líneas (66 de 72) y 74 de 78 ramas, según el jacoco del gate. La CI pide un 80 % del código cambiado, así que cada rama nueva (`--help` en el primer argumento, detrás del nombre y como valor de `--lang`) lleva su test.

### Characterization needed

Se caracteriza lo que este diseño mantiene y que el cambio puede romper. Lo que cambia (filas 1–5, 8 y 13) no se caracteriza: lo cubren los tests de aceptación.

| Zone | Behaviour to pin | Why |
| --- | --- | --- |
| CLI | `--help` como valor de `--lang`: `Ana --lang --help` y `Ana --lang=--help` dan `Unknown language: --help (supported: en, es)`, 2; `--lang --help` da `The name must come first, got: --lang (usage: …)`, 2 | Se mantiene (filas 6, 7 y 9), y es justo el borde de la regla nueva |
| CLI | Variantes que no son `--help`: `Ana --HELP`, `Ana --Help`, `Ana --help=yes` y `Ana --help=` dan `Unknown option: <arg> (…)`, 2; `Ana -h`, `Ana -help`, `Ana help` y `Ana " --help"` dan `Hello, Ana!`, 0 | Se mantiene (filas 10–12 y 14) |
| CLI | Precedencia sin `--help`: `--foo` primero, `Ana --foo`, `--shout --foo` (`Unknown option: --foo`), y el "primer problema de izquierda a derecha" entre una opción desconocida y `--lang` (`Ana --foo --lang xx` → `Unknown option: --foo`; `Ana --lang xx --foo` → `Unknown language: xx`) | Se mantiene; el cambio toca esta lógica (análisis del mapper) |
| CLI | Texto de uso del bugfix 10 en `--shout Ana` y `--shout` (stdout, salida 0) | Se mantiene (alternativa 2); la ayuda nueva no debe cambiarlo |

Lo que ya fija un test aprobado (`FlagAsNameAcceptanceTest`, `UnknownOptionAcceptanceTest`, `GreetingCliCharacterizationTest`, `GreetingCliOptionsCharacterizationTest`) no se repite. Los tests nuevos van en una clase nueva, para no reabrir las ya aprobadas *(decisión mía)*. El nombre lo elige el test-author.

### Riesgos

- `run` consume el argumento siguiente a `--lang` como valor. Detectar `--help` antes de ese recorrido haría que `Ana --lang --help` diera la ayuda, contra la fila 6. La detección tiene que respetar ese consumo.
- `firstArgumentResult` comprueba primero si el primer argumento es una opción desconocida. Si `--help` no se trata antes, `--help` primero sigue dando `Unknown option`. En ese camino también hay que respetar la excepción de `--lang --help` (fila 9).
- La lista de "supported" no incluye `--help`, aunque `--help` deje de ser desconocida. Es una decisión (alternativa 3), no un descuido. Por eso `isUnknownOption` debe excluir `--help` sin cambiar el texto.
