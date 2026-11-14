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
 * Guards of Feature 18: criteria 3, 4 and 5 already hold today and must keep holding. One
 * subprocess per call.
 */
class HelpOptionGuardsAcceptanceTest {

  private static final String NL = System.lineSeparator();
  private static final String SUPPORTED = " (supported: --lang <code>, --lang=<code>, --shout)";

  private record Result(int exitCode, String stdout, String stderr) {}

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
    String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    if (!process.waitFor(30, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new AssertionError("the CLI did not finish");
    }
    return new Result(process.exitValue(), stdout, stderr);
  }

  private static void assertOutcome(int exit, String out, String err, String... args)
      throws Exception {
    Result result = runCli(args);
    String call = String.join(" ", args);
    assertEquals(out, result.stdout(), "stdout of: " + call);
    assertEquals(err, result.stderr(), "stderr of: " + call);
    assertEquals(exit, result.exitCode(), "exit code of: " + call);
  }

  /** Criterion 3 (rows 6 and 7): help as the value of --lang is not help. */
  @Test
  void helpAsLangValueIsAnUnknownLanguage() throws Exception {
    String err = "Unknown language: --help (supported: en, es)" + NL;
    assertOutcome(2, "", err, "Ana", "--lang", "--help");
    assertOutcome(2, "", err, "Ana", "--lang=--help");
  }

  /** Criterion 4: a single-dash -h is a loose argument. */
  @Test
  void singleDashHStillGreets() throws Exception {
    assertOutcome(0, "Hello, Ana!" + NL, "", "Ana", "-h");
  }

  /** Criterion 5: the bugfix 10 usage text and the unknown option error are unchanged. */
  @Test
  void usageTextAndUnknownOptionErrorAreUnchanged() throws Exception {
    assertOutcome(
        0, "Usage: greet <name> [--shout] [--lang en|es]" + NL, "", "--shout", "Ana");
    assertOutcome(2, "", "Unknown option: --foo" + SUPPORTED + NL, "Ana", "--foo");
  }
}
