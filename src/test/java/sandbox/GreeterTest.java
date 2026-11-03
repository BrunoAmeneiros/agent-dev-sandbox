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

  @Test
  void greetsInSpanish() {
    assertEquals("¡Hola, Ana!", new Greeter().greet("Ana", Language.ES));
  }

  @Test
  void greetsInEnglishExplicitly() {
    assertEquals("Hello, Ana!", new Greeter().greet("Ana", Language.EN));
  }

  @Test
  void rejectsNullNameWithLanguage() {
    NullPointerException e =
        assertThrows(NullPointerException.class, () -> new Greeter().greet(null, Language.ES));
    assertEquals("name", e.getMessage());
  }

  @Test
  void rejectsNullLanguage() {
    NullPointerException e =
        assertThrows(NullPointerException.class, () -> new Greeter().greet("Ana", null));
    assertEquals("language", e.getMessage());
  }
}
