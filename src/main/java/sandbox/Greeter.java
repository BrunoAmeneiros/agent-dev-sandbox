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
    Objects.requireNonNull(name, "name");
    return "Hello, " + name + "!";
  }
}
