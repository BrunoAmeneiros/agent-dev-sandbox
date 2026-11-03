package sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class LanguageTest {

  @Test
  void fromCodeFindsEnglish() {
    assertEquals(Optional.of(Language.EN), Language.fromCode("en"));
  }

  @Test
  void fromCodeFindsSpanish() {
    assertEquals(Optional.of(Language.ES), Language.fromCode("es"));
  }

  @Test
  void fromCodeIgnoresCase() {
    assertEquals(Optional.of(Language.ES), Language.fromCode("ES"));
  }

  @Test
  void fromCodeRejectsUnknown() {
    assertEquals(Optional.empty(), Language.fromCode("xx"));
    assertEquals(Optional.empty(), Language.fromCode(""));
  }

  @Test
  void fromCodeRejectsNull() {
    NullPointerException e = assertThrows(NullPointerException.class, () -> Language.fromCode(null));
    assertEquals("code", e.getMessage());
  }
}
