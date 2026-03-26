/**
 * @file ConsoleUiJlineTest.java
 * @brief Covers ConsoleUi JLine (Terminal) menu path without real terminal providers.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PushbackInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.Test;

public class ConsoleUiJlineTest {

  private static PrintStream silentOut() throws Exception {
    return new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8.name());
  }

  private static final class SeqReader extends NonBlockingReader {
    private final int[] seq;
    private int i = 0;

    SeqReader(int... seq) {
      this.seq = seq;
    }

    @Override
    public int read(long timeout, boolean isPeek) {
      if (i >= seq.length) {
        return -1;
      }
      return seq[i++];
    }

    @Override
    public int readBuffered(char[] b, int off, int len, long timeout) {
      return -1;
    }

    @Override
    public int read(char[] cbuf, int off, int len) {
      return -1;
    }

    @Override
    public void close() {
    }
  }

  private static Terminal fakeTerminal(NonBlockingReader reader, InputStream input) {
    return (Terminal) Proxy.newProxyInstance(
        Terminal.class.getClassLoader(),
        new Class<?>[] {Terminal.class},
        (proxy, method, args) -> {
          String n = method.getName();
          if ("enterRawMode".equals(n)) {
            return new Attributes();
          }
          if ("reader".equals(n)) {
            return reader;
          }
          if ("input".equals(n)) {
            return input;
          }
          if ("setAttributes".equals(n)) {
            return null;
          }
          Class<?> rt = method.getReturnType();
          if (rt == boolean.class) {
            return false;
          }
          if (rt == int.class) {
            return 0;
          }
          return null;
        });
  }

  @Test
  public void jlineMenuSelectsOnEnter() throws Exception {
    Terminal t = fakeTerminal(new SeqReader('\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu",
        Arrays.asList("A", "B", "Exit"), "status");
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuSelectsOnCarriageReturn() throws Exception {
    Terminal t = fakeTerminal(new SeqReader('\r'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuCyclesWithTabThenEnter() throws Exception {
    Terminal t = fakeTerminal(new SeqReader('\t', '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void jlineMenuReturnsLastOnQOrEOF() throws Exception {
    List<String> options = Arrays.asList("A", "B", "Exit");
    Terminal t1 = fakeTerminal(new SeqReader('q'), new ByteArrayInputStream(new byte[0]));
    assertEquals(2, ConsoleUi.readMenuChoice(t1, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu", options, null));

    Terminal t1b = fakeTerminal(new SeqReader('Q'), new ByteArrayInputStream(new byte[0]));
    assertEquals(2, ConsoleUi.readMenuChoice(t1b, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu", options, null));

    Terminal t2 = fakeTerminal(new SeqReader(-1), new ByteArrayInputStream(new byte[0]));
    assertEquals(2, ConsoleUi.readMenuChoice(t2, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu", options, null));
  }

  @Test
  public void jlineMenuHandlesAnsiEscapeNavigation() throws Exception {
    InputStream escInput = new PushbackInputStream(new ByteArrayInputStream(new byte[] {'[', 'B'}), 8);
    Terminal t = fakeTerminal(new SeqReader(27, '\n'), escInput);
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void jlineMenuHandlesAnsiEscapeUnknownDoesNothing() throws Exception {
    InputStream escInput = new ByteArrayInputStream(new byte[] {'X'});
    Terminal t = fakeTerminal(new SeqReader(27, '\n'), escInput);
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuHandlesAnsiEscapeQuit() throws Exception {
    InputStream escInput = new ByteArrayInputStream(new byte[] {'q'});
    Terminal t = fakeTerminal(new SeqReader(27), escInput);
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(2, idx);
  }

  @Test
  public void jlineMenuHandlesWindowsScanCodes() throws Exception {
    // 224 (extended scan prefix), 80 (down), then Enter => select index 1
    Terminal t = fakeTerminal(new SeqReader(224, 80, '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void jlineMenuIgnoresUnknownWindowsScanCode() throws Exception {
    // 224 prefix + unknown scan => NAV_NONE, then Enter selects current index 0
    Terminal t = fakeTerminal(new SeqReader(224, 1, '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuMovesWithVimAndWsKeys() throws Exception {
    // s (down), w (up), j (down), k (up), then Enter => ends at 0
    Terminal t = fakeTerminal(new SeqReader('s', 'w', 'j', 'k', '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuReturnsMinusOneWhenOptionsEmpty() throws Exception {
    Terminal t = fakeTerminal(new SeqReader('\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu",
        Arrays.<String>asList(), "");
    assertEquals(-1, idx);
  }

  @Test
  public void jlineMenuMovesWithZeroPrefixedScanCode() throws Exception {
    // 0 prefix, scan 80 down, then Enter => select index 1
    Terminal t = fakeTerminal(new SeqReader(0, 80, '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu",
        Arrays.asList("A", "B", "Exit"), "");
    assertEquals(1, idx);
  }

  @Test
  public void jlineMenuMovesWithUppercaseVariants() throws Exception {
    // S down, W up, J down, K up, then Enter => ends at 0
    Terminal t = fakeTerminal(new SeqReader('S', 'W', 'J', 'K', '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu",
        Arrays.asList("A", "B", "Exit"), "");
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuIgnoresUnhandledCharactersAndKeepsLooping() throws Exception {
    // Unhandled 'x' should not change selection; then Enter selects current index 0.
    Terminal t = fakeTerminal(new SeqReader('x', '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), silentOut(), "Menu",
        Arrays.asList("A", "B", "Exit"), "");
    assertEquals(0, idx);
  }

  @Test
  public void bannerPrintedInJlineLoopDoesNotCrash() throws Exception {
    Terminal t = fakeTerminal(new SeqReader('\n'), new ByteArrayInputStream(new byte[0]));
    PrintStream out = silentOut();
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), out, "Menu",
        Arrays.asList("A", "Exit"), "x");
    assertTrue(idx == 0 || idx == 1);
  }
}

