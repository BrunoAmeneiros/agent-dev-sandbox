# Cambios en la CLI `greet` (Feature 16): `--lang=<code>` y opciones desconocidas

Para operaciones: lista de **todas** las llamadas cuyo resultado cambia respecto a la versión actual (`develop` 71bdbd7). Revisad vuestros scripts del kiosco contra esta tabla antes de publicar.

## Qué cambia

- `--lang=<code>` pasa a ser válido (hoy se ignoraba).
- Cualquier opción `--` desconocida (en cualquier posición) falla con salida 2 y el mensaje:

  `Unknown option: <opción> (supported: --lang <code>, --lang=<code>, --shout)`

  Hoy esas llamadas saludan o imprimen el texto de uso con salida 0.
- `--` suelto también es opción desconocida (`Unknown option: --`).

**No hay periodo de transición**: es un cambio que rompe, aceptado en la necesidad. Operaciones puede buscar `Unknown option:` en los logs de sus scripts. Una llamada sin opciones desconocidas, sin `--lang=` y sin `--` suelto da exactamente lo mismo que hoy.

En la tabla, `UO` abrevia `Unknown option: <opción> (supported: --lang <code>, --lang=<code>, --shout)` (el texto completo está arriba). `Usage` es el texto de uso `Usage: greet <name> [--shout] [--lang en|es]`. Los argumentos `""` y `"   "` son un argumento vacío y uno en blanco. Las salidas de error van a stderr.

## Tabla antes → ahora

| Fila | Llamada | Hoy (salida, código) | Ahora (salida, código) | Nota |
| --- | --- | --- | --- | --- |
| 1 | `Ana --lang=es` | `Hello, Ana!`, 0 | `¡Hola, Ana!`, 0 | `--lang=` ya se respeta (stdout UTF-8) |
| 2 | `Ana --lang=ES` | `Hello, Ana!`, 0 | `¡Hola, Ana!`, 0 | |
| 3 | `Ana --lang=xx` | `Hello, Ana!`, 0 | `Unknown language: xx (supported: en, es)`, 2 | |
| 4 | `Ana --lang=` | `Hello, Ana!`, 0 | `Missing value for --lang (supported: en, es)`, 2 | |
| 5 | `Ana --lang=es=x` | `Hello, Ana!`, 0 | `Unknown language: es=x (supported: en, es)`, 2 | |
| 6 | `Ana --LANG=es` | `Hello, Ana!`, 0 | `Unknown option: --LANG=es (supported: --lang <code>, --lang=<code>, --shout)`, 2 | mayúsculas: no es `--lang` |
| 6 | `Ana --LANG es` | `Hello, Ana!`, 0 | `Unknown option: --LANG (supported: --lang <code>, --lang=<code>, --shout)`, 2 | |
| 7 (segundo caso) | `Ana --lang xx --lang=es` | `Unknown language: xx (supported: en, es)`, 2 | `¡Hola, Ana!`, 0 | salida 2 → 0: gana el último `--lang` |
| 9 | `Ana --shout=yes` | `Hello, Ana!`, 0 | UO con `--shout=yes`, 2 | |
| 9 | `Ana --shout=` | `Hello, Ana!`, 0 | UO con `--shout=`, 2 | |
| 10 | `Ana --foo` | `Hello, Ana!`, 0 | UO con `--foo`, 2 | |
| 10 | `Ana --lnag es` | `Hello, Ana!`, 0 | UO con `--lnag`, 2 | errata de `--lang` |
| 11 | `Ana --foo --bar` | `Hello, Ana!`, 0 | UO con `--foo`, 2 | solo se informa el primero |
| 12b | `Ana --lang xx --lang es --foo` | `¡Hola, Ana!`, 0 | UO con `--foo`, 2 | el `xx` sobrescrito no cuenta |
| 12c | `Ana --foo --lang xx` | `Unknown language: xx (supported: en, es)`, 2 | UO con `--foo`, 2 | solo cambia el mensaje (salida 2 igual) |
| 13 | `Ana --` | `Hello, Ana!`, 0 | UO con `--`, 2 | |
| 17 | `--lang=es Ana` | Usage, 0 | `The name must come first, got: --lang=es (usage: greet <name> [--shout] [--lang en\|es])`, 2 | |
| 17 | `--lang=es` | Usage, 0 | `The name must come first, got: --lang=es (usage: greet <name> [--shout] [--lang en\|es])`, 2 | |
| 18 | `--foo Ana` | Usage, 0 | UO con `--foo`, 2 | |
| 18 | `--foo` | Usage, 0 | UO con `--foo`, 2 | |
| 18 | `--` | Usage, 0 | UO con `--`, 2 | |
| 18 | `  --foo  Ana` (blancos delante) | Usage, 0 | UO con `--foo`, 2 | los argumentos vacíos o en blanco no cambian el resultado |
| 20 | `--foo Ana --lang es` | `The name must come first, got: --foo (usage: greet <name> [--shout] [--lang en\|es])`, 2 | UO con `--foo`, 2 | solo cambia el mensaje (salida 2 igual) |
| 21 | `Ana --lang=--shout` | `Hello, Ana!`, 0 | `Unknown language: --shout (supported: en, es)`, 2 | el valor tras `=` se toma tal cual |
| 22 | `Ana --lang=es --shout` | `Hello, Ana!`, 0 | `¡HOLA, ANA!`, 0 | |
| 22 | `Ana --shout --lang=es` | `HELLO, ANA!`, 0 | `¡HOLA, ANA!`, 0 | |
| 23 | `--shout Ana --foo` | Usage, 0 | UO con `--foo`, 2 | gana al texto de uso |
| 23 | `--shout --foo` | Usage, 0 | UO con `--foo`, 2 | |
| 24 | `"" --foo` | Usage, 0 | UO con `--foo`, 2 | |
| 24 | `"   " Ana --foo` | Usage, 0 | UO con `--foo`, 2 | |
