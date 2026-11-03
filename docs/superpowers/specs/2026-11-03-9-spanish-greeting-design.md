# Design: saludo en español con `--lang es`

Feature 9 · feature/9-spanish-greeting · 2026-11-03

Necesidad aprobada: [2026-11-03-9-spanish-greeting-need.md](2026-11-03-9-spanish-greeting-need.md). Allí están el problema, el éxito, las restricciones y por qué es `build-missing`. Este documento no lo repite.

**Revisión tras el bugfix 10** (merge de `develop`, 2026-11-03). El bugfix 10 ya está en la rama: un primer argumento que, recortado, empieza por `--` no es un nombre y muestra el texto de uso. Esta revisión incorpora esa regla y sus tests (`GreetingCliTest`, `FlagAsNameAcceptanceTest`, el caso `Ana-María` de su caracterización) al diseño. La intención, los objetivos y el resto de decisiones no cambian.

## Intención

Los scripts de bienvenida del cliente en España llaman a la CLI así:

```
java -cp app.jar sandbox.GreetingCli "$USER" --lang es
```

Esperan `¡Hola, <name>!`. Si la llamada falla, el script lo detecta **solo por el código de salida distinto de cero**, registra el error y sigue sin saludar. Lo demás no puede cambiar: los scripts que no pasan `--lang` deben ver exactamente lo mismo que hoy, incluido `--shout` y el código de salida.

## Objetivos, no objetivos y antiobjetivos

- **Objetivos:** `--lang es` saluda en español, y un idioma desconocido o `--lang` sin valor falla con un código de salida distinto de cero, sin saludo.
- **No objetivos:** más idiomas que `en` y `es`; detectar el idioma del sistema (`LANG` o `Locale.getDefault()`); traducir el texto de uso o los mensajes de error; cambiar la codificación de la salida sin `--lang es`; que la consola de Windows muestre bien `¡`, si su página de códigos no es UTF-8.
- **Antiobjetivos:** que cambie cualquier salida o código de salida de una llamada que no lleve `--lang`; añadir dependencias.

## Comportamiento de la CLI

Forma: `greet <name> [--shout] [--lang <code>]`.

- El nombre sigue siendo siempre `args[0]`, se le quitan los espacios de los extremos (`strip`) y las reglas de nombre vacío no cambian.
- **La llamada "lleva `--lang`"** si algún argumento es exactamente `--lang`. En el primero se mira después de recortarlo, igual que el nombre. Así, `--lang es Ana` lleva `--lang`.
- **Falta el nombre** (`args[0]` está en blanco o, recortado, empieza por `--`, que es la regla del bugfix 10). `-x` y `Ana--` siguen siendo nombres.
  - **Sin `--lang`:** igual que hoy y que el bugfix 10. Se muestra el texto de uso con salida 0. Por ejemplo, `--shout Ana`, `--` y un nombre en blanco.
  - **Con `--lang`** (decisión de Bruno): es un error. Va por stderr, con salida 2 y nada por stdout, para que el script lo detecte. Si el nombre está en blanco (`"" --lang es`, o `"$USER" --lang es` con `USER` vacío), el mensaje es `Missing name (usage: greet <name> [--shout] [--lang en|es])`. Si el primer argumento es un flag (`--lang es Ana`, `--shout Ana --lang es`), el mensaje es `The name must come first, got: <argumento recortado> (usage: greet <name> [--shout] [--lang en|es])` *(los textos son decisión mía)*.
  - Esta comprobación va antes que las de `--lang`. Por eso `"" --lang xx` da `Missing name` y no el error de idioma.
- **Sin `--lang` en ningún sitio de los argumentos, se aplica la regla de hoy, sin cambios** (decisión de Bruno). Solo se grita si `args[1]` es exactamente `--shout`. Así `Ana foo --shout` sigue dando `Hello, Ana!` y `Ana --shout --shout` sigue dando `HELLO, ANA!`. La salida, la codificación y el código de salida son idénticos a hoy en todos los casos.
- **Con `--lang` en cualquier posición detrás del nombre,** `--shout` y `--lang <code>` pueden ir en cualquier orden, y se grita si `--shout` aparece en cualquier sitio que no sea el valor de `--lang`. `Ana --lang es --shout` y `Ana --shout --lang es` dan los dos `¡HOLA, ANA!`. Una llamada que hoy lleva `--lang` (por ejemplo `Ana --shout --lang`) puede cambiar de comportamiento. Se acepta, porque es el flag nuevo.
- Códigos admitidos: `en` y `es`, en minúsculas y sin distinguir mayúsculas al compararlos (`ES` vale) *(decisión mía)*. `--lang en` da la misma salida que no poner el flag.
- Si `--lang` se repite, vale el último *(decisión mía)*.
- Los argumentos que no se reconocen se siguen ignorando, como hoy. Así, `Ana foo` sigue imprimiendo `Hello, Ana!` con salida 0.
- Hay una excepción para `--lang`: la palabra que va justo detrás se toma siempre como su valor, aunque sea `--shout`. Por eso `Ana --lang --shout` es el error de idioma desconocido `--shout` *(decisión mía: es más simple y no deja nada ambiguo)*.

### Resultados

| Caso | stdout | stderr | Código de salida |
| --- | --- | --- | --- |
| `Ana` | `Hello, Ana!` | — | 0 |
| `Ana --shout` | `HELLO, ANA!` | — | 0 |
| `Ana --lang es` | `¡Hola, Ana!` | — | 0 |
| `Ana --lang es --shout` (o al revés) | `¡HOLA, ANA!` | — | 0 |
| `Ana --lang en` | `Hello, Ana!` | — | 0 |
| `Ana --lang xx` | — | `Unknown language: xx (supported: en, es)` | 2 |
| `Ana --lang` (sin valor) | — | `Missing value for --lang (supported: en, es)` | 2 |
| sin argumentos o con nombre en blanco, sin `--lang` | `Usage: greet <name> [--shout] [--lang en\|es]` | — | 0 (como hoy) |
| primer argumento que empieza por `--`, sin `--lang` (`--shout Ana`, `--`) | `Usage: greet <name> [--shout] [--lang en\|es]` | — | 0 (bugfix 10) |
| nombre en blanco con `--lang` (`"" --lang es`, `"   " --lang xx`) | — | `Missing name (usage: greet <name> [--shout] [--lang en\|es])` | 2 |
| primer argumento que empieza por `--` con `--lang` (`--lang es Ana`, `--shout Ana --lang es`) | — | `The name must come first, got: --lang (usage: greet <name> [--shout] [--lang en\|es])` (con el argumento que toque) | 2 |
| `-x`, `Ana--`, `Ana-María` | `Hello, -x!`, `Hello, Ana--!`, `Hello, Ana-María!` | — | 0 (bugfix 10) |

- Si hay cualquier error (idioma o nombre), **no se imprime nada por stdout**.
- Los mensajes de error van en inglés, como el texto de uso actual *(decisión mía)*. Son para el registro del script, no para el usuario final.
- El texto de uso cambia para mencionar `--lang`. Lo pide la necesidad, y ningún script conocido compara ese texto. El único test que compara el texto exacto es `FlagAsNameAcceptanceTest` (bugfix 10). **Decisión de Bruno:** se cambia para que solo compruebe que empieza por `Usage: greet <name> [--shout]` y que es una línea, igual que la caracterización. Se aprueba con el plan.
- El texto de uso mantiene la salida 0. Cambiarla rompería el antiobjetivo.
- El código 2 sigue la convención de "uso incorrecto" de las herramientas Unix *(decisión mía)*. Los scripts solo miran que sea distinto de cero.
- **Codificación** (decisión de Bruno): con `--lang es` (resuelto a `Language.ES`), `main` escribe stdout en **UTF-8** pase lo que pase con la configuración regional del proceso. Así `¡Hola` llega bien aunque el script corra con `LANG=C`. Con cualquier otra llamada (sin `--lang` o con `--lang en`) se sigue usando el `System.out` de siempre, con la codificación de la plataforma, para no tocar la salida actual. Por ejemplo, `José` sin `--lang` no cambia sus bytes. Cómo se fuerza UTF-8 (un `PrintStream` sobre `FileOutputStream(FileDescriptor.out)` o sobre `System.out` con `UTF_8`, por ejemplo) lo decide quien lo construya.
- Mayúsculas: `toUpperCase(Locale.ROOT)`, como hoy. Para nombres con tilde (`José` → `JOSÉ`) da el resultado esperado. `¡` no cambia.

## Estructura (lo que está decidido)

- **Texto del saludo en `Greeter`.** Se mantiene `greet(String)` con su firma y su comportamiento exactos, porque lo usan `GreeterTest` y el test de aceptación protegido. Se añade una sobrecarga `greet(String name, Language language)` con la misma comprobación de `null` para `name` (`Objects.requireNonNull(name, "name")`) y para `language` (`"language"`). `greet(String)` delega en `greet(name, Language.EN)`.
- **Idiomas en un `enum Language`** público en el paquete `sandbox`, con `EN` y `ES`. Cada valor lleva su plantilla de saludo y un método estático que busca por código y devuelve `Optional<Language>` *(decisión mía)*. No se usa `ResourceBundle`: para dos textos fijos añade archivos y carga sin ganar nada, y el `enum` no necesita dependencias.
- **`¡` como escape `\u00a1` en el código** *(decisión mía)*, para que el resultado no dependa de la codificación del editor ni del build. Hoy `pom.xml` no fija `project.build.sourceEncoding`. Se deja así: con el escape no hace falta.
- **La CLI separa el cálculo de la salida.** `run` deja de devolver un `String` y devuelve un resultado con stdout, stderr, código de salida y si stdout va forzado a UTF-8, por ejemplo un `record` privado del paquete *(la forma exacta la decide quien lo construya)*. `main` imprime cada parte en su flujo y solo llama a `System.exit(code)` si el código no es 0. Así la ruta correcta sigue saliendo con 0 sin llamar a `exit`, igual que hoy, y los tests comprueban el código de salida sin terminar la JVM.

## Lo que queda para quien lo construya

- Los nombres internos del parseo y del tipo de resultado.
- El orden y los nombres de los tests, siempre que cubran la tabla de resultados.
- Si `Language` expone el código (`"en"` o `"es"`) como método o solo lo usa en la búsqueda.

## Tests

- **Caracterización de la CLI, ya escrita antes del plan** (decisión de Bruno: quien cambia la CLI no escribe los tests que fijan su comportamiento actual): `src/test/java/sandbox/GreetingCliCharacterizationTest.java`, 9 tests que pasan con el código de hoy. **Tras el bugfix 10** pasa a 10 tests: se le añade el caso que tenía la caracterización del bugfix 10 y esta no (`Ana-María` da `Hello, Ana-María!`), escrito con escapes `í` y entrando por `main`. Los otros casos del bugfix 10 ya estaban cubiertos. La versión del bugfix 10 llamaba a `run` como `String` y fijaba el texto de uso exacto, así que no sobreviviría a esta Feature. Esta versión fusionada se aprueba con el plan. Entra por el `main` público, el punto de entrada de los scripts, y no por `run`, así sigue compilando cuando `run` cambie de tipo. Fija que `Ana` da `Hello, Ana!`, que `--shout` da `HELLO, ANA!`, que el nombre se recorta, que sin argumentos o con nombre en blanco sale el texto de uso, que un segundo argumento desconocido se ignora y que la salida es 0. También que `Ana foo --shout` da `Hello, Ana!`, que `Ana --shout --shout` da `HELLO, ANA!` y que `José --shout` da `HELLO, JOSÉ!`. Del texto de uso solo se fija que empieza por `Usage: greet <name> [--shout]` y que es una línea *(decisión mía: así admite el `[--lang en|es]` que añade esta Feature y no hace falta tocar el test)*. Se aprueba con el plan y queda protegido: quien implementa no puede editarlo. La primera Task del plan solo comprueba que sigue pasando. Lo pide el análisis de impacto.
- **Unitarios de `Greeter` y `Language`:** el saludo en español y en inglés, el `null` en cada parámetro, la búsqueda por código (`es`, `ES`, `xx`).
- **Unitarios de la CLI:** una fila por cada caso de la tabla de resultados, comprobando stdout, stderr y código de salida. Van en `GreetingCliTest`, que **ya existe** (bugfix 10, no protegido). Sus 6 tests (`--shout Ana`, `--shout`, `  --shout  `, `--`, `-x`, `Ana--`) se mantienen y se adaptan al nuevo tipo de resultado y al texto de uso nuevo. Se añaden los de esta Feature, entre ellos los dos errores de nombre que falta con `--lang`.
- **`FlagAsNameAcceptanceTest`** (bugfix 10, protegido): solo cambia la comprobación del texto de uso, que pasa a ser "empieza por `Usage: greet <name> [--shout]` y es una línea". Sus cuatro casos se mantienen, porque ninguno lleva `--lang`.
- **Orden de trabajo** (decisión de Bruno). Cuando se apruebe este diseño, y antes de revisar el plan, yo mismo (no quien implementa) hago los dos cambios en tests protegidos: el caso `Ana-María` en la caracterización y la comprobación del principio del texto de uso en `FlagAsNameAcceptanceTest`. Los dos tienen que pasar con el código de hoy. Después se actualiza el plan aprobado, sin reescribirlo: Task 1 con 10 tests de caracterización, `FlagAsNameAcceptanceTest` en la lista de protegidos, la regla del nombre y los casos nuevos en la Task 3, y `GreetingCliTest` como archivo que se modifica, no que se crea. El plan y los dos tests se aprueban juntos, en una sola decisión, antes de ejecutar nada.
- **Aceptación:** un test nuevo en `tests/acceptance/` (`SpanishGreetingAcceptanceTest`) sobre el comportamiento de la necesidad: `--lang es`, sin `--lang` y `--lang xx` con código distinto de cero y sin saludo. Incluye un caso de `--lang es` con el subproceso lanzado con `LANG=C` (o `-Dstdout.encoding=US-ASCII`) que comprueba que los bytes de stdout son el UTF-8 de `¡Hola, Ana!`. Lanza la CLI como **subproceso** (`java -cp target/classes sandbox.GreetingCli …`) y comprueba el código de salida real, stdout y stderr, igual que lo ven los scripts del cliente *(decisión mía: así también prueba `main` y `System.exit`, y no obliga a hacer público `run`)*. Ya está escrito (`tests/acceptance/SpanishGreetingAcceptanceTest.java`). Hoy falla en `--lang es` y en `--lang xx`, como se espera, y pasa sin `--lang`. Se aprueba con el plan y queda protegido, como los tests de aceptación anteriores. `GreeterAcceptanceTest` no se toca.
- Quién escribe cada test: el test-author (pieza 6) todavía no existe. Los tests de caracterización y de aceptación los he escrito yo en el diseño, no quien implementa.
- Los tests unitarios de la CLI van en `src/test/java/sandbox` y llaman a `run` directamente (es privado del paquete). Son los que cuentan para la cobertura.
- La cobertura del código que cambia debe llegar al 80 % (CI con diff-cover). Checkstyle y SpotBugs deben pasar: Javadoc en `Language` y en la sobrecarga nueva.

## Impact

**Environment gate:** passed - `mvn -B verify -Pacceptance`, 2026-11-03 (3b8af7e): 4 tests run, 0 failed, 0 already known.

Tras el merge de `develop` (bugfix 10), el gate se volvió a pasar en a58ca2b el 2026-11-03: 26 tests, de los que fallan 2, `SpanishGreetingAcceptanceTest.greetsInSpanishWithLangEs` y `unknownLanguageFailsWithoutGreeting`. Es lo esperado, porque esta Feature aún no está construida. No hay fallos ya conocidos.

Parte del [análisis de impacto](2026-11-03-9-spanish-greeting-impact.md), actualizado con lo que decide este diseño y con el bugfix 10.

### Zonas

| Zona | Ruta | Cambio | Por qué |
| --- | --- | --- | --- |
| CLI | src/main/java/sandbox/GreetingCli.java | modificada | Parseo de `--lang` (sin `--lang` se mantiene la regla antigua), error con salida 2 por stderr, UTF-8 con `--lang es`, nuevo tipo de resultado de `run`, texto de uso |
| Greeter | src/main/java/sandbox/Greeter.java | modificada | Sobrecarga `greet(String, Language)`; `greet(String)` sin cambios visibles |
| Language | src/main/java/sandbox/Language.java | nueva | `enum` con `EN` y `ES`, sus plantillas y la búsqueda por código |
| Tests de la CLI (bugfix 10) | src/test/java/sandbox/GreetingCliTest.java | modificada | Sus 6 casos pasan al nuevo tipo de resultado de `run` y al texto de uso nuevo; se le añaden los casos de esta Feature |
| Aceptación del bugfix 10 | tests/acceptance/FlagAsNameAcceptanceTest.java | modificada (protegida) | Solo comprueba el comienzo del texto de uso (decisión de Bruno) |

### Contratos

- **Argumentos de la CLI:** `<name> [--shout] [--lang <code>]`. Los flags van en cualquier orden solo si hay `--lang`. Sin `--lang` se mantiene la regla de hoy (`--shout` solo en `args[1]`), así que toda llamada sin `--lang` da la misma salida, la misma codificación y el mismo código que hoy. La excepción es el texto de uso, más abajo. Sin `--lang`, si falta el nombre (en blanco, o un primer argumento que, recortado, empieza por `--`, que es la regla del bugfix 10), se sigue mostrando el texto de uso con salida 0. Con `--lang`, ese mismo caso es un error con salida 2.
- **Salida y código de salida:** solo cambian con `--lang`. Con `--lang es`, stdout va en UTF-8. Si hay error, el mensaje sale por stderr, el código de salida es 2 y no se escribe nada por stdout.
- **Texto de uso:** pasa a `Usage: greet <name> [--shout] [--lang en|es]`. La salida sigue siendo 0.
- **API Java:** `Greeter.greet(String)` sin cambios. Se añaden `Greeter.greet(String, Language)` y el `enum Language`, que son públicos.
- **`GreeterAcceptanceTest`:** protegido, no se edita.
- **`FlagAsNameAcceptanceTest`:** protegido. Su contrato (un primer argumento que empieza por `--` da el texto de uso y `Ana` saluda) se mantiene. Solo se relaja la comparación del texto de uso.

### Datos

Ninguno.

### Cobertura

| Zona | Antes | Después (objetivo) |
| --- | --- | --- |
| Greeter | 100 % | 100 % |
| CLI | 100 % de líneas y ramas (tests unitarios del bugfix 10) | ≥ 80 % del código que cambia, con las ramas de la tabla de resultados |
| Language | — | ≥ 80 % |

### Caracterización necesaria

| Zona | Comportamiento que se fija | Estado |
| --- | --- | --- |
| CLI | Saludo con nombre, `--shout`, nombre recortado, texto de uso, segundo argumento desconocido ignorado, salida 0 | Escrita: `GreetingCliCharacterizationTest`, 9 tests aprobados que pasan con el código actual, también tras el bugfix 10. Pasa a 10 con el caso `Ana-María` del bugfix 10, y esa versión se aprueba con el plan. La primera Task solo comprueba que sigue pasando. La regla `--` del bugfix 10 la fijan `GreetingCliTest` y `FlagAsNameAcceptanceTest` |
| Greeter | `greet(null)` lanza NullPointerException con el mensaje `name` | Ya cubierto |

### Riesgos

- Con `--lang es`, la salida va siempre en UTF-8. Una consola de Windows que no esté en UTF-8 mostrará mal `¡`, pero los scripts que leen los bytes lo reciben bien. Queda fuera de alcance, es un no objetivo.
- Hay dos reglas de parseo, según haya o no `--lang`. Es el precio de no cambiar nada sin `--lang`, y los tests de caracterización fijan la regla antigua.
- Que `--lang` se coma la palabra siguiente puede sorprender en `Ana --lang --shout`. Se acepta porque es un error de uso y avisa con salida 2.
- Hay dos respuestas distintas cuando falta el nombre: el texto de uso con salida 0 sin `--lang`, y un error con salida 2 con `--lang`. Es el precio de no cambiar nada sin `--lang` y de que los scripts nuevos detecten un `$USER` vacío o un flag delante del nombre.
