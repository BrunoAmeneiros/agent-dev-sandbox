import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import sandbox.GreetingCli;

/** Acceptance tests of Feature 18 ({@code --help} shows the help), one subprocess per call. */
class HelpOptionAcceptanceTest {

  private static final String NL = System.lineSeparator();
  private static final String HELP =
      "Usage: greet <name> [--shout] [--lang <code> | --lang=<code>] [--help]\n"
          + "  --shout          print the greeting in upper case\n"
          + "  --lang <code>    language of the greeting: en, es (default en)\n"
          + "  --lang=<code>    same as --lang <code>\n"
          + "  --help           show this help";

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

  private static void assertHelp(String... args) throws Exception {
    Result result = runCli(args);
    String call = String.join(" ", args);
    assertEquals(HELP + NL, result.stdout(), "stdout of: " + call);
    assertEquals("", result.stderr(), "stderr of: " + call);
    assertEquals(0, result.exitCode(), "exit code of: " + call);
  }

  /** Criterion 1 (row 1): help alone, before and after the name. */
  @Test
  void helpShowsTheHelpText() throws Exception {
    assertHelp("--help");
    assertHelp("Ana", "--help");
    assertHelp("--help", "Ana");
  }

  /** Criterion 2 (rows 3 to 5): help wins over other problems. */
  @Test
  void helpWinsOverOtherProblems() throws Exception {
    assertHelp("--help", "--foo");
    assertHelp("Ana", "--foo", "--help");
    assertHelp("Ana", "--lang", "xx", "--help");
    assertHelp("Ana", "--lang=", "--help");
    assertHelp("--lang", "es", "--help");
    assertHelp("--lang=es", "--help");
    assertHelp("", "--help");
    assertHelp("--shout", "--help");
  }

  /** Row 2: repeating help changes nothing. */
  @Test
  void repeatedHelpShowsTheHelpText() throws Exception {
    assertHelp("--help", "--help");
    assertHelp("Ana", "--help", "--help");
  }

  /** Row 8 and the review cases: only a help that is a value of --lang is not help. */
  @Test
  void secondHelpAfterLangValueShowsTheHelpText() throws Exception {
    assertHelp("Ana", "--lang", "--help", "--help");
    assertHelp("Ana", "--lang", "--lang", "--help");
  }

  /** Row 13: the first argument is stripped. */
  @Test
  void paddedFirstArgumentHelpShowsTheHelpText() throws Exception {
    assertHelp("  --help  ");
    assertHelp("  --help  ", "Ana");
  }

  /** Criterion 7: the ops note exists and points at the Feature 16 note. */
  @Test
  void opsNoteExists() throws Exception {
    Path note = Path.of("docs", "changes", "2026-11-14-18-help-option.md");
    assertTrue(Files.isRegularFile(note), "ops note exists");
    String text = Files.readString(note, StandardCharsets.UTF_8);
    assertTrue(text.contains("2026-11-09-16-unknown-options.md"), "refers to the Feature 16 note");
  }
}
