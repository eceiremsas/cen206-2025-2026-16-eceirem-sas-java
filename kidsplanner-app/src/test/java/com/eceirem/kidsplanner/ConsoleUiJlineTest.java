/**
 * @file ConsoleUiJlineTest.java
 * @brief Covers ConsoleUi JLine (Terminal) menu path using a real "dumb" terminal.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

public class ConsoleUiJlineTest {

  private static Terminal dumbTerminalWithInput(byte[] inputBytes) throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream(inputBytes);
    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    return TerminalBuilder.builder()
        .dumb(true)
        .streams(in, outBytes)
        .encoding(StandardCharsets.UTF_8)
        .build();
  }

  @Test
  public void jlineMenuSelectsOnEnter() throws Exception {
    Terminal t = dumbTerminalWithInput(new byte[] {'\n'});
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), "status");
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuSelectsOnCarriageReturn() throws Exception {
    Terminal t = dumbTerminalWithInput(new byte[] {'\r'});
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuCyclesWithTabThenEnter() throws Exception {
    Terminal t = dumbTerminalWithInput(new byte[] {'\t', '\n'});
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void jlineMenuReturnsLastOnQOrEOF() throws Exception {
    List<String> options = Arrays.asList("A", "B", "Exit");
    Terminal t1 = dumbTerminalWithInput(new byte[] {'q'});
    assertEquals(2, ConsoleUi.readMenuChoice(t1, new ByteArrayInputStream(new byte[0]), System.out, "Menu", options, null));

    Terminal t1b = dumbTerminalWithInput(new byte[] {'Q'});
    assertEquals(2, ConsoleUi.readMenuChoice(t1b, new ByteArrayInputStream(new byte[0]), System.out, "Menu", options, null));

    Terminal t2 = dumbTerminalWithInput(new byte[] {});
    assertEquals(2, ConsoleUi.readMenuChoice(t2, new ByteArrayInputStream(new byte[0]), System.out, "Menu", options, null));
  }

  @Test
  public void jlineMenuHandlesAnsiEscapeNavigation() throws Exception {
    // ESC [ B (down) then Enter
    Terminal t = dumbTerminalWithInput(new byte[] {27, '[', 'B', '\n'});
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void jlineMenuHandlesAnsiEscapeUnknownDoesNothing() throws Exception {
    // ESC X (unknown) then Enter
    Terminal t = dumbTerminalWithInput(new byte[] {27, 'X', '\n'});
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuHandlesAnsiEscapeQuit() throws Exception {
    // ESC q => quit => last option
    Terminal t = dumbTerminalWithInput(new byte[] {27, 'q'});
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(2, idx);
  }

  @Test
  public void jlineMenuHandlesFallbackWAndSKeys() throws Exception {
    // Start at 0, 's' => 1, 'w' => 0, Enter => 0
    Terminal t = dumbTerminalWithInput(new byte[] {'s', 'w', '\n'});
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuHandlesFallbackUppercaseVariants() throws Exception {
    // 'S' down, 'W' up, 'J' down, 'K' up, Enter.
    Terminal t = dumbTerminalWithInput(new byte[] {'S', 'W', 'J', 'K', '\n'});
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuCoversKAndJFallbackKeys() throws Exception {
    // j down to 1, k back to 0, Enter.
    Terminal t = dumbTerminalWithInput(new byte[] {'j', 'k', '\n'});
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), "");
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuCoversLowercaseJBranch() throws Exception {
    Terminal t = dumbTerminalWithInput(new byte[] {'j', '\n'});
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void jlineMenuUnknownKeyFallsThroughToNextLoopIteration() throws Exception {
    // 'x' is ignored (falls through to end of while loop), then Enter selects 0.
    Terminal t = dumbTerminalWithInput(new byte[] {'x', '\n'});
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void jlineEmptyOptionsReturnsMinusOne() throws Exception {
    Terminal t = dumbTerminalWithInput(new byte[] {'\n'});
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList(), null);
    assertEquals(-1, idx);
  }

  @Test
  public void bannerPrintedInJlineLoopDoesNotCrash() throws Exception {
    // Smoke: ensure PrintStream path works.
    Terminal t = dumbTerminalWithInput(new byte[] {'\n'});
    PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8.name());
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), out, "Menu",
        Arrays.asList("A", "Exit"), "x");
    assertTrue(idx == 0 || idx == 1);
  }
}

