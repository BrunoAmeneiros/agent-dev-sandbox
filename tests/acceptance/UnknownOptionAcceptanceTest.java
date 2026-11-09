import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import sandbox.GreetingCli;

/**
 * Approved acceptance test (Feature 16): protected path, the implementer must not change it.
 *
 * <p>Runs the CLI as the kiosk script does, in its own JVM, and checks the real exit code, the
 * bytes of stdout and the text of stderr. Criteria 1 to 5 of the Feature 16 design.
 */
class UnknownOptionAcceptanceTest {

  private static final String SUPPORTED_OPTIONS =
      " (supported: --lang <code>, --lang=<code>, --shout)";

  private record Result(int exitCode, byte[] stdout, String stderr) {}

  private static Result runCli(String... args)
      throws IOException, InterruptedException, URISyntaxException {
    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    String classpath =
        Path.of(GreetingCli.class.getProtectionDomain().getCodeSource().getLocation().toURI())
            .toString();
    List<String> command = new ArrayList<>();
    command.add(java);
    command.add("-Dstdout.encoding=US-ASCII");
    command.add("-cp");
    command.add(classpath);
    command.add("sandbox.GreetingCli");
    command.addAll(List.of(args));

    ProcessBuilder builder = new ProcessBuilder(command);
    builder.environment().put("LANG", "C");
    builder.environment().remove("LC_ALL");
    Process process = builder.start();
    byte[] stdout = process.getInputStream().readAllBytes();
    String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    if (!process.waitFor(30, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new AssertionError("the CLI did not finish");
    }
    return new Result(process.exitValue(), stdout, stderr);
  }

  private static byte[] utf8Line(String text) {
    return (text + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
  }

  private static void assertError(String expectedStderr, Result result) {
    assertEquals(2, result.exitCode(), "exit code, stderr: " + result.stderr());
    assertEquals(0, result.stdout().length, "no greeting on stdout");
    assertEquals(expectedStderr + System.lineSeparator(), result.stderr(), "stderr");
  }

  /** Criterion 1: the kiosk call greets in Spanish, as UTF-8 bytes even under LANG=C. */
  @Test
  void greetsInSpanishWithLangEquals() throws Exception {
    Result result = runCli("Ana", "--lang=es");

    assertEquals(0, result.exitCode(), "stderr: " + result.stderr());
    assertArrayEquals(utf8Line("¡Hola, Ana!"), result.stdout());
    assertEquals("", result.stderr(), "nothing on stderr");
  }

  /** Criterion 2: {@code --lang=xx} fails exactly like {@code --lang xx}. */
  @Test
  void langEqualsUnknownLanguageFailsLikeSeparate() throws Exception {
    assertError("Unknown language: xx (supported: en, es)", runCli("Ana", "--lang=xx"));
  }

  /** Criterion 2, guard: the separate form keeps its error (already true today). */
  @Test
  void separateUnknownLanguageStillFails() throws Exception {
    assertError("Unknown language: xx (supported: en, es)", runCli("Ana", "--lang", "xx"));
  }

  /** Criterion 3: a misspelled flag is reported as given. */
  @Test
  void misspelledLangIsUnknownOption() throws Exception {
    assertError("Unknown option: --lnag" + SUPPORTED_OPTIONS, runCli("Ana", "--lnag", "es"));
  }

  /** Criterion 3: {@code --shout} takes no value. */
  @Test
  void shoutWithValueIsUnknownOption() throws Exception {
    assertError("Unknown option: --shout=yes" + SUPPORTED_OPTIONS, runCli("Ana", "--shout=yes"));
  }

  /** Criterion 4: an unknown flag in first position is an error, not the usage text. */
  @Test
  void unknownFlagBeforeNameIsUnknownOption() throws Exception {
    assertError("Unknown option: --foo" + SUPPORTED_OPTIONS, runCli("--foo", "Ana"));
  }

  /** Criterion 4: an unknown flag wins over the usage text of a known first flag. */
  @Test
  void unknownFlagAfterUsageFlagIsUnknownOption() throws Exception {
    assertError("Unknown option: --foo" + SUPPORTED_OPTIONS, runCli("--shout", "Ana", "--foo"));
  }

  /** Criterion 5, guard: loose words that are not flags stay ignored (already true today). */
  @Test
  void looseWordsStillIgnored() throws Exception {
    Result result = runCli("Ana", "foo");

    assertEquals(0, result.exitCode(), "stderr: " + result.stderr());
    assertArrayEquals(utf8Line("Hello, Ana!"), result.stdout());
    assertEquals("", result.stderr(), "nothing on stderr");
  }
}
