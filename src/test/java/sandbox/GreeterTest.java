package sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GreeterTest {

  @Test
  void greetsByName() {
    assertEquals("Hello, Ana!", new Greeter().greet("Ana"));
  }

  @Test
  void rejectsNullName() {
    NullPointerException e =
        assertThrows(NullPointerException.class, () -> new Greeter().greet(null));
    assertEquals("name", e.getMessage());
  }
}
