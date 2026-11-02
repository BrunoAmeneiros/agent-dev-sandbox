import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import sandbox.Greeter;

/** Approved acceptance test (bugfix 5): protected path, the implementer must not change it. */
class GreeterNullAcceptanceTest {

  @Test
  void greetingRejectsNullName() {
    NullPointerException e =
        assertThrows(NullPointerException.class, () -> new Greeter().greet(null));
    assertEquals("name", e.getMessage());
  }
}
