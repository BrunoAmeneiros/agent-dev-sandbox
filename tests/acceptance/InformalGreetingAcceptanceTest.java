import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import sandbox.Greeter;

/** Approved acceptance test (Feature 4): protected path, the implementer must not change it. */
class InformalGreetingAcceptanceTest {

  @Test
  void informalGreetingIncludesTheName() {
    assertEquals("Hi, Ana!", new Greeter().greetInformally("Ana"));
  }

  @Test
  void formalGreetingIsUnchanged() {
    assertEquals("Hello, Ana!", new Greeter().greet("Ana"));
  }
}
