package sandbox;

import java.util.Locale;

/** Command line entry point: prints a greeting. */
public final class GreetingCli {

  private GreetingCli() {}

  /**
   * Prints a greeting for the name given as first argument.
   *
   * @param args the name, then optionally {@code --shout}
   */
  public static void main(String[] args) {
    System.out.println(run(args));
  }

  static String run(String[] args) {
    if (args.length == 0 || args[0].isBlank()) {
      return "Usage: greet <name> [--shout]";
    }
    String greeting = new Greeter().greet(args[0].strip());
    if (args.length > 1 && "--shout".equals(args[1])) {
      return greeting.toUpperCase(Locale.ROOT);
    }
    return greeting;
  }
}
