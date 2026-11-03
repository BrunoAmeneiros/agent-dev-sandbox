import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
 * Approved acceptance test (Feature 9): protected path, the implementer must not change it.
 *
 * <p>Runs the CLI as the welcome scripts do, in its own JVM, and checks the real exit code and
 * the bytes of stdout. The JVM runs with an ASCII stdout encoding and {@code LANG=C}, as under
 * cron, so the Spanish greeting must still arrive as UTF-8.
 */
class SpanishGreetingAcceptanceTest {

  private static final String INVERTED_EXCLAMATION = String.valueOf((char) 0xA1);

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

  @Test
  void greetsInSpanishWithLangEs() throws Exception {
    Result result = runCli("Ana", "--lang", "es");

    assertEquals(0, result.exitCode(), "stderr: " + result.stderr());
    assertArrayEquals(utf8Line(INVERTED_EXCLAMATION + "Hola, Ana!"), result.stdout());
  }

  @Test
  void greetsInEnglishWithoutLang() throws Exception {
    Result result = runCli("Ana");

    assertEquals(0, result.exitCode(), "stderr: " + result.stderr());
    assertArrayEquals(utf8Line("Hello, Ana!"), result.stdout());
  }

  @Test
  void unknownLanguageFailsWithoutGreeting() throws Exception {
    Result result = runCli("Ana", "--lang", "xx");

    assertNotEquals(0, result.exitCode());
    assertEquals(0, result.stdout().length, "no greeting on stdout");
    assertFalse(result.stderr().isBlank(), "the error is reported on stderr");
  }
}
