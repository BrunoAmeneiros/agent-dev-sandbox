package sandbox;

import java.util.Locale;

/** Command line entry point: prints a greeting. */
public final class GreetingCli {

  private GreetingCli() {}

  /**
   * Prints a greeting for the name given as first argument.
   *
   * @param args the name, then optionally {@code --shout}; a first argument starting with {@code
   *     --} is not a name and prints the usage text
   */
  public static void main(String[] args) {
    System.out.println(run(args));
  }

  static String run(String[] args) {
    String name = args.length == 0 ? "" : args[0].strip();
    if (name.isEmpty() || name.startsWith("--")) {
      return "Usage: greet <name> [--shout]";
    }
    String greeting = new Greeter().greet(name);
    if (args.length > 1 && "--shout".equals(args[1])) {
      return greeting.toUpperCase(Locale.ROOT);
    }
    return greeting;
  }
}
