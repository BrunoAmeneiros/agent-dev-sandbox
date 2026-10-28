# agent-dev-sandbox

Repo de pruebas del plugin agent-dev-process. Proyecto Maven mínimo con los raíles instalados:

- `.process/config.json`: rutas protegidas; `tests/acceptance/GreeterAcceptanceTest.java` está aprobado.
- `.process/bin/`: comprobación de rutas protegidas para la CI y `--approve`.
- `.github/workflows/ci.yml`: puertas.

```bash
mvn -B verify                 # tests
mvn -B verify -Pacceptance    # + tests de aceptación
```
