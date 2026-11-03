package sandbox;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Languages a greeting can be written in. */
public enum Language {
  /** English. */
  EN("Hello, "),
  /** Spanish. */
  ES("¡Hola, ");

  private final String prefix;

  Language(String prefix) {
    this.prefix = prefix;
  }

  /**
   * Returns the greeting for the given name in this language.
   *
   * @param name the name to greet
   * @return the greeting, e.g. "Hello, Ana!"
   */
  String greeting(String name) {
    return prefix + name + "!";
  }

  /**
   * Finds a language by its code, ignoring case.
   *
   * @param code the language code, e.g. "en" or "es"
   * @return the matching language, or empty if the code is unknown
   * @throws NullPointerException if code is null
   */
  public static Optional<Language> fromCode(String code) {
    Objects.requireNonNull(code, "code");
    String normalized = code.toLowerCase(Locale.ROOT);
    for (Language language : values()) {
      if (language.name().toLowerCase(Locale.ROOT).equals(normalized)) {
        return Optional.of(language);
      }
    }
    return Optional.empty();
  }
}
