package sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Bugfix 10: a first argument starting with {@code --} is a flag, not a name. */
class GreetingCliTest {

  private static final String USAGE = "Usage: greet <name> [--shout]";

  @Test
  void flagBeforeNamePrintsUsage() {
    assertEquals(USAGE, GreetingCli.run(new String[] {"--shout", "Ana"}));
  }

  @Test
  void flagAlonePrintsUsage() {
    assertEquals(USAGE, GreetingCli.run(new String[] {"--shout"}));
  }

  @Test
  void strippedFlagPrintsUsage() {
    assertEquals(USAGE, GreetingCli.run(new String[] {"  --shout  ", "Ana"}));
  }

  @Test
  void doubleDashAlonePrintsUsage() {
    assertEquals(USAGE, GreetingCli.run(new String[] {"--"}));
  }

  @Test
  void singleDashIsStillAName() {
    assertEquals("Hello, -x!", GreetingCli.run(new String[] {"-x"}));
  }

  @Test
  void innerDoubleDashIsStillAName() {
    assertEquals("Hello, Ana--!", GreetingCli.run(new String[] {"Ana--"}));
  }
}
