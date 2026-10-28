import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import sandbox.Greeter;

/** Approved acceptance test: protected path, the implementer must not change it. */
/** Prueba de edicion **/
class GreeterAcceptanceTest {

  @Test
  void greetingIncludesTheName() {
    assertEquals("Hello, Ana!", new Greeter().greet("Ana"));
  }
}
