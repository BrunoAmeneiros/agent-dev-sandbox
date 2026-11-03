package sandbox;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/** Command line entry point: prints a greeting, in English or Spanish. */
public final class GreetingCli {

  private static final String USAGE = "Usage: greet <name> [--shout] [--lang en|es]";
  private static final String HINT = " (usage: greet <name> [--shout] [--lang en|es])";
  private static final String LANG = "--lang";
  private static final String SHOUT = "--shout";
  private static final int ERROR_EXIT_CODE = 2;

  private GreetingCli() {}

  /**
   * What a call prints and how it ends.
   *
   * @param stdout text for standard output, without line terminator; empty means nothing
   * @param stderr text for standard error, without line terminator; empty means nothing
   * @param exitCode the process exit code
   * @param utf8Stdout whether stdout must be written as UTF-8 bytes
   */
  record CliResult(String stdout, String stderr, int exitCode, boolean utf8Stdout) {}

  /**
   * Prints a greeting for the name given as first argument.
   *
   * @param args the name, then optionally {@code --shout} and {@code --lang en|es}; a first
   *     argument starting with {@code --} is not a name and prints the usage text (an error when
   *     {@code --lang} is present)
   */
  public static void main(String[] args) {
    CliResult result = run(args);
    if (!result.stdout().isEmpty()) {
      if (result.utf8Stdout()) {
        PrintStream out =
            new PrintStream(new FileOutputStream(FileDescriptor.out), false, StandardCharsets.UTF_8);
        out.println(result.stdout());
        out.flush();
      } else {
        System.out.println(result.stdout());
      }
    }
    if (!result.stderr().isEmpty()) {
      System.err.println(result.stderr());
    }
    if (result.exitCode() != 0) {
      System.exit(result.exitCode());
    }
  }

  static CliResult run(String[] args) {
    String name = args.length == 0 ? "" : args[0].strip();
    boolean hasLang = name.equals(LANG);
    for (int i = 1; i < args.length; i++) {
      hasLang |= LANG.equals(args[i]);
    }
    if (name.isEmpty() || name.startsWith("--")) {
      if (!hasLang) {
        return new CliResult(USAGE, "", 0, false);
      }
      return name.isEmpty()
          ? error("Missing name" + HINT)
          : error("The name must come first, got: " + name + HINT);
    }
    if (!hasLang) {
      String greeting = new Greeter().greet(name);
      boolean shout = args.length > 1 && SHOUT.equals(args[1]);
      return new CliResult(shout ? greeting.toUpperCase(Locale.ROOT) : greeting, "", 0, false);
    }
    String code = "en";
    boolean shout = false;
    for (int i = 1; i < args.length; i++) {
      if (LANG.equals(args[i])) {
        if (i + 1 >= args.length) {
          return error("Missing value for --lang (supported: en, es)");
        }
        code = args[++i];
      } else if (SHOUT.equals(args[i])) {
        shout = true;
      }
    }
    Optional<Language> language = Language.fromCode(code);
    if (language.isEmpty()) {
      return error("Unknown language: " + code + " (supported: en, es)");
    }
    String greeting = new Greeter().greet(name, language.get());
    return new CliResult(
        shout ? greeting.toUpperCase(Locale.ROOT) : greeting, "", 0, language.get() == Language.ES);
  }

  private static CliResult error(String message) {
    return new CliResult("", message, ERROR_EXIT_CODE, false);
  }
}
