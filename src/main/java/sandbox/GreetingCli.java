package sandbox;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Command line entry point: prints a greeting, in English or Spanish. */
public final class GreetingCli {

  private static final String USAGE = "Usage: greet <name> [--shout] [--lang en|es]";
  private static final String HINT = " (usage: greet <name> [--shout] [--lang en|es])";
  private static final String UNKNOWN_OPTION_HINT =
      " (supported: --lang <code>, --lang=<code>, --shout)";
  private static final String MISSING_LANG_VALUE = "Missing value for --lang (supported: en, es)";
  private static final String LANG = "--lang";
  private static final String LANG_EQUALS = "--lang=";
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
   * @param args the name, then optionally {@code --shout}, {@code --lang en|es} and {@code
   *     --lang=en|es}; any other argument after the name starting with {@code --} is an unknown
   *     option (an error); a first argument starting with {@code --} is not a name and prints the
   *     usage text (an error when {@code --lang} or {@code --lang=} is present, or when it is
   *     itself an unknown option)
   */
  public static void main(String[] args) {
    CliResult result = run(args);
    if (!result.stdout().isEmpty()) {
      if (result.utf8Stdout()) {
        PrintStream out =
            new PrintStream(System.out, false, StandardCharsets.UTF_8);
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

  /**
   * Decides what a call prints and how it ends.
   *
   * <p>After the name, {@code --lang <code>} and {@code --lang=<code>} choose the language (the
   * last one wins and the value is taken as is) and {@code --shout} shouts. Any other argument
   * starting with {@code --} is an unknown option and fails with exit code 2; words not starting
   * with {@code --} are ignored.
   *
   * @param args the command line arguments
   * @return what to print and how to end
   */
  static CliResult run(String[] args) {
    Objects.requireNonNull(args, "args");
    String name = args.length == 0 ? "" : args[0].strip();
    if (name.isEmpty() || name.startsWith("--")) {
      return firstArgumentResult(name, args);
    }
    boolean hasLang = false;
    boolean shout = false;
    String code = "en";
    String langError = null;
    int langPos = -1;
    String unknown = null;
    int unknownPos = -1;
    for (int i = 1; i < args.length; i++) {
      String arg = args[i];
      if (isLang(arg)) {
        hasLang = true;
        langPos = i;
        String value = null;
        if (LANG.equals(arg)) {
          if (i + 1 < args.length) {
            value = args[++i];
          }
        } else if (arg.length() > LANG_EQUALS.length()) {
          value = arg.substring(LANG_EQUALS.length());
        }
        langError = value == null ? MISSING_LANG_VALUE : null;
        if (value != null) {
          code = value;
        }
      } else if (SHOUT.equals(arg)) {
        shout = true;
      } else if (arg.startsWith("--") && unknown == null) {
        unknown = arg;
        unknownPos = i;
      }
    }
    Optional<Language> language = Language.fromCode(code);
    if (langError == null && language.isEmpty()) {
      langError = "Unknown language: " + code + " (supported: en, es)";
    }
    if (unknown != null && (langError == null || unknownPos < langPos)) {
      return unknownOption(unknown);
    }
    if (langError != null) {
      return error(langError);
    }
    if (!hasLang) {
      shout = args.length > 1 && SHOUT.equals(args[1]);
    }
    String greeting = new Greeter().greet(name, language.get());
    return new CliResult(
        shout ? greeting.toUpperCase(Locale.ROOT) : greeting, "", 0, language.get() == Language.ES);
  }

  private static CliResult firstArgumentResult(String name, String[] args) {
    if (isUnknownOption(name)) {
      return unknownOption(name);
    }
    boolean hasLang = isLang(name);
    for (int i = 1; i < args.length; i++) {
      hasLang |= isLang(args[i]);
    }
    if (hasLang) {
      return name.isEmpty()
          ? error("Missing name" + HINT)
          : error("The name must come first, got: " + name + HINT);
    }
    for (int i = 1; i < args.length; i++) {
      if (isUnknownOption(args[i])) {
        return unknownOption(args[i]);
      }
    }
    return new CliResult(USAGE, "", 0, false);
  }

  private static boolean isLang(String arg) {
    return LANG.equals(arg) || arg.startsWith(LANG_EQUALS);
  }

  private static boolean isUnknownOption(String arg) {
    return arg.startsWith("--") && !SHOUT.equals(arg) && !isLang(arg);
  }

  private static CliResult unknownOption(String option) {
    return error("Unknown option: " + option + UNKNOWN_OPTION_HINT);
  }

  private static CliResult error(String message) {
    return new CliResult("", message, ERROR_EXIT_CODE, false);
  }
}
