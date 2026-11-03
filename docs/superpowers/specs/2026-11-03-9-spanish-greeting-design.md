# Design: saludo en español con `--lang es`

Feature 9 · feature/9-spanish-greeting · 2026-11-03

Necesidad aprobada: [2026-11-03-9-spanish-greeting-need.md](2026-11-03-9-spanish-greeting-need.md). Allí están el problema, el éxito, las restricciones y por qué es `build-missing`. Este documento no lo repite.

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
| sin argumentos o con nombre en blanco | `Usage: greet <name> [--shout] [--lang en\|es]` | — | 0 (como hoy) |

- Si hay error de idioma, **no se imprime nada por stdout**.
- Los mensajes de error van en inglés, como el texto de uso actual *(decisión mía)*. Son para el registro del script, no para el usuario final.
- El texto de uso cambia para mencionar `--lang`. Lo pide la necesidad, y ningún script conocido compara ese texto.
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

- **Caracterización de la CLI, ya escrita antes del plan** (decisión de Bruno: quien cambia la CLI no escribe los tests que fijan su comportamiento actual): `src/test/java/sandbox/GreetingCliCharacterizationTest.java`, 9 tests que pasan con el código de hoy. Entra por el `main` público, el punto de entrada de los scripts, y no por `run`, así sigue compilando cuando `run` cambie de tipo. Fija que `Ana` da `Hello, Ana!`, que `--shout` da `HELLO, ANA!`, que el nombre se recorta, que sin argumentos o con nombre en blanco sale el texto de uso, que un segundo argumento desconocido se ignora y que la salida es 0. También que `Ana foo --shout` da `Hello, Ana!`, que `Ana --shout --shout` da `HELLO, ANA!` y que `José --shout` da `HELLO, JOSÉ!`. Del texto de uso solo se fija que empieza por `Usage: greet <name> [--shout]` y que es una línea *(decisión mía: así admite el `[--lang en|es]` que añade esta Feature y no hace falta tocar el test)*. Se aprueba con el plan y queda protegido: quien implementa no puede editarlo. La primera Task del plan solo comprueba que sigue pasando. Lo pide el análisis de impacto.
- **Unitarios de `Greeter` y `Language`:** el saludo en español y en inglés, el `null` en cada parámetro, la búsqueda por código (`es`, `ES`, `xx`).
- **Unitarios de la CLI:** una fila por cada caso de la tabla de resultados, comprobando stdout, stderr y código de salida.
- **Aceptación:** un test nuevo en `tests/acceptance/` (`SpanishGreetingAcceptanceTest`) sobre el comportamiento de la necesidad: `--lang es`, sin `--lang` y `--lang xx` con código distinto de cero y sin saludo. Incluye un caso de `--lang es` con el subproceso lanzado con `LANG=C` (o `-Dstdout.encoding=US-ASCII`) que comprueba que los bytes de stdout son el UTF-8 de `¡Hola, Ana!`. Lanza la CLI como **subproceso** (`java -cp target/classes sandbox.GreetingCli …`) y comprueba el código de salida real, stdout y stderr, igual que lo ven los scripts del cliente *(decisión mía: así también prueba `main` y `System.exit`, y no obliga a hacer público `run`)*. Ya está escrito (`tests/acceptance/SpanishGreetingAcceptanceTest.java`). Hoy falla en `--lang es` y en `--lang xx`, como se espera, y pasa sin `--lang`. Se aprueba con el plan y queda protegido, como los tests de aceptación anteriores. `GreeterAcceptanceTest` no se toca.
- Quién escribe cada test: el test-author (pieza 6) todavía no existe. Los tests de caracterización y de aceptación los he escrito yo en el diseño, no quien implementa.
- Los tests unitarios de la CLI van en `src/test/java/sandbox` y llaman a `run` directamente (es privado del paquete). Son los que cuentan para la cobertura.
- La cobertura del código que cambia debe llegar al 80 % (CI con diff-cover). Checkstyle y SpotBugs deben pasar: Javadoc en `Language` y en la sobrecarga nueva.

## Impact

**Environment gate:** passed - `mvn -B verify -Pacceptance`, 2026-11-03 (3b8af7e): 4 tests run, 0 failed, 0 already known.

Parte del [análisis de impacto](2026-11-03-9-spanish-greeting-impact.md), actualizado con lo que decide este diseño.

### Zonas

| Zona | Ruta | Cambio | Por qué |
| --- | --- | --- | --- |
| CLI | src/main/java/sandbox/GreetingCli.java | modificada | Parseo de `--lang` (sin `--lang` se mantiene la regla antigua), error con salida 2 por stderr, UTF-8 con `--lang es`, nuevo tipo de resultado de `run`, texto de uso |
| Greeter | src/main/java/sandbox/Greeter.java | modificada | Sobrecarga `greet(String, Language)`; `greet(String)` sin cambios visibles |
| Language | src/main/java/sandbox/Language.java | nueva | `enum` con `EN` y `ES`, sus plantillas y la búsqueda por código |

### Contratos

- **Argumentos de la CLI:** `<name> [--shout] [--lang <code>]`. Los flags van en cualquier orden solo si hay `--lang`. Sin `--lang` se mantiene la regla de hoy (`--shout` solo en `args[1]`), así que toda llamada sin `--lang` da la misma salida, la misma codificación y el mismo código que hoy. La excepción es el texto de uso, más abajo.
- **Salida y código de salida:** solo cambian con `--lang`. Con `--lang es`, stdout va en UTF-8. Si hay error, el mensaje sale por stderr, el código de salida es 2 y no se escribe nada por stdout.
- **Texto de uso:** pasa a `Usage: greet <name> [--shout] [--lang en|es]`. La salida sigue siendo 0.
- **API Java:** `Greeter.greet(String)` sin cambios. Se añaden `Greeter.greet(String, Language)` y el `enum Language`, que son públicos.
- **`GreeterAcceptanceTest`:** protegido, no se edita.

### Datos

Ninguno.

### Cobertura

| Zona | Antes | Después (objetivo) |
| --- | --- | --- |
| Greeter | 100 % | 100 % |
| CLI | 0 % | ≥ 80 % del código que cambia, con las ramas de la tabla de resultados |
| Language | — | ≥ 80 % |

### Caracterización necesaria

| Zona | Comportamiento que se fija | Estado |
| --- | --- | --- |
| CLI | Saludo con nombre, `--shout`, nombre recortado, texto de uso, segundo argumento desconocido ignorado, salida 0 | Escrita: `GreetingCliCharacterizationTest`, 9 tests, pasan con el código actual. Se aprueba con el plan y queda protegida; la primera Task solo comprueba que sigue pasando |
| Greeter | `greet(null)` lanza NullPointerException con el mensaje `name` | Ya cubierto |

### Riesgos

- Con `--lang es`, la salida va siempre en UTF-8. Una consola de Windows que no esté en UTF-8 mostrará mal `¡`, pero los scripts que leen los bytes lo reciben bien. Queda fuera de alcance, es un no objetivo.
- Hay dos reglas de parseo, según haya o no `--lang`. Es el precio de no cambiar nada sin `--lang`, y los tests de caracterización fijan la regla antigua.
- Que `--lang` se coma la palabra siguiente puede sorprender en `Ana --lang --shout`. Se acepta porque es un error de uso y avisa con salida 2.
