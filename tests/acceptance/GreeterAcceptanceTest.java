import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import sandbox.Greeter;

/** Approved acceptance test: protected path, the implementer must not change it. */
class GreeterAcceptanceTest {

  @Test
  void greetingIncludesTheName() {
    assertEquals("Hello, Bruno!", new Greeter().greet("Bruno"));
  }
}
