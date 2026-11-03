package sandbox;

import java.util.Objects;

/** Builds greetings. */
public final class Greeter {

  /**
   * Returns a greeting for the given name.
   *
   * @param name the name to greet
   * @return the greeting, e.g. "Hello, Ana!"
   * @throws NullPointerException if name is null
   */
  public String greet(String name) {
    return greet(name, Language.EN);
  }

  /**
   * Returns a greeting for the given name in the given language.
   *
   * @param name the name to greet
   * @param language the language of the greeting
   * @return the greeting, e.g. "Hello, Ana!" or "¡Hola, Ana!"
   * @throws NullPointerException if name or language is null
   */
  public String greet(String name, Language language) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(language, "language");
    return language.greeting(name);
  }
}
