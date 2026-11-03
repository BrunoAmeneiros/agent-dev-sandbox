package sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Characterization of GreetingCli before bugfix 10: pins today's behaviour of the calls the fix
 * must not change (a first argument that does not start with {@code --}). Written before the
 * change; approved with the plan, protected path, the implementer must not change it.
 */
class GreetingCliCharacterizationTest {

  private static final String USAGE = "Usage: greet <name> [--shout]";

  @Test
  void printsUsageWithNoArguments() {
    assertEquals(USAGE, GreetingCli.run(new String[] {}));
  }

  @Test
  void printsUsageWithBlankName() {
    assertEquals(USAGE, GreetingCli.run(new String[] {"   "}));
  }

  @Test
  void greetsByName() {
    assertEquals("Hello, Ana!", GreetingCli.run(new String[] {"Ana"}));
  }

  @Test
  void stripsTheName() {
    assertEquals("Hello, Ana!", GreetingCli.run(new String[] {"  Ana  "}));
  }

  @Test
  void shoutsWhenSecondArgumentIsShout() {
    assertEquals("HELLO, ANA!", GreetingCli.run(new String[] {"Ana", "--shout"}));
  }

  @Test
  void shoutKeepsAccentedLetters() {
    assertEquals("HELLO, JOSÉ!", GreetingCli.run(new String[] {"José", "--shout"}));
  }

  @Test
  void ignoresUnknownSecondArgument() {
    assertEquals("Hello, Ana!", GreetingCli.run(new String[] {"Ana", "foo"}));
  }

  @Test
  void nameWithInnerDashesIsStillAName() {
    assertEquals("Hello, Ana-María!", GreetingCli.run(new String[] {"Ana-María"}));
  }
}
