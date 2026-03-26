/**
 * @file ConsoleUiJlineTest.java
 * @brief Covers ConsoleUi JLine (Terminal) menu path without real terminal providers.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
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

  private static final class SeqReader extends NonBlockingReader {
    private final int[] seq;
    private int i = 0;

    SeqReader(int... seq) {
      this.seq = seq;
    }

    @Override
    public int read(long timeout, boolean isPeek) {
      // ignore timeout/peek, just return next key code deterministically
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
          // Safe defaults for other methods not used by ConsoleUi.readMenuChoiceJline
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
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), "status");
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuSelectsOnCarriageReturn() throws Exception {
    Terminal t = fakeTerminal(new SeqReader('\r'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuCyclesWithTabThenEnter() throws Exception {
    Terminal t = fakeTerminal(new SeqReader('\t', '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void jlineMenuReturnsLastOnQOrEOF() throws Exception {
    List<String> options = Arrays.asList("A", "B", "Exit");
    Terminal t1 = fakeTerminal(new SeqReader('q'), new ByteArrayInputStream(new byte[0]));
    assertEquals(2, ConsoleUi.readMenuChoice(t1, new ByteArrayInputStream(new byte[0]), System.out, "Menu", options, null));

    Terminal t1b = fakeTerminal(new SeqReader('Q'), new ByteArrayInputStream(new byte[0]));
    assertEquals(2, ConsoleUi.readMenuChoice(t1b, new ByteArrayInputStream(new byte[0]), System.out, "Menu", options, null));

    Terminal t2 = fakeTerminal(new SeqReader(-1), new ByteArrayInputStream(new byte[0]));
    assertEquals(2, ConsoleUi.readMenuChoice(t2, new ByteArrayInputStream(new byte[0]), System.out, "Menu", options, null));
  }

  @Test
  public void jlineMenuHandlesAnsiEscapeNavigation() throws Exception {
    // Reader emits ESC, then input stream provides '[', 'B' (down), then Enter.
    InputStream escInput = new PushbackInputStream(new ByteArrayInputStream(new byte[] {'[', 'B'}), 8);
    Terminal t = fakeTerminal(new SeqReader(27, '\n'), escInput);
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void jlineMenuHandlesAnsiEscapeUnknownDoesNothing() throws Exception {
    // ESC then input provides 'X' => NAV_NONE => loop continues => Enter selects 0.
    InputStream escInput = new ByteArrayInputStream(new byte[] {'X'});
    Terminal t = fakeTerminal(new SeqReader(27, '\n'), escInput);
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuHandlesAnsiEscapeQuit() throws Exception {
    // Reader emits ESC, input stream provides 'q' => NAV_QUIT => last option.
    InputStream escInput = new ByteArrayInputStream(new byte[] {'q'});
    Terminal t = fakeTerminal(new SeqReader(27), escInput);
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(2, idx);
  }

  @Test
  public void jlineMenuHandlesWindowsScanCodes() throws Exception {
    // Reader emits 0, then scan code 80 (down), then Enter.
    Terminal t = fakeTerminal(new SeqReader(0, 80, '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void jlineMenuHandlesWindowsScanCodesThatMapToNone() throws Exception {
    // Reader emits 0, then scan code 75 (left => NAV_NONE), then Enter selects 0.
    Terminal t = fakeTerminal(new SeqReader(0, 75, '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuHandlesWindowsScanCodesWithE0Prefix() throws Exception {
    // Reader emits 224, then scan code 80 (down), then Enter.
    Terminal t = fakeTerminal(new SeqReader(224, 80, '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void jlineMenuHandlesFallbackWAndSKeys() throws Exception {
    // Start at 0, 's' => 1, 'w' => 0, Enter => 0
    Terminal t = fakeTerminal(new SeqReader('s', 'w', '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuHandlesFallbackUppercaseVariants() throws Exception {
    // 'S' down, 'W' up, 'J' down, 'K' up, Enter.
    Terminal t = fakeTerminal(new SeqReader('S', 'W', 'J', 'K', '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuCoversKAndJFallbackKeys() throws Exception {
    // j down to 1, k back to 0, Enter.
    Terminal t = fakeTerminal(new SeqReader('j', 'k', '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), "");
    assertEquals(0, idx);
  }

  @Test
  public void jlineMenuCoversLowercaseJBranch() throws Exception {
    Terminal t = fakeTerminal(new SeqReader('j', '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void jlineMenuUnknownKeyFallsThroughToNextLoopIteration() throws Exception {
    // 'x' is ignored (falls through to end of while loop), then Enter selects 0.
    Terminal t = fakeTerminal(new SeqReader('x', '\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void jlineEmptyOptionsReturnsMinusOne() throws Exception {
    Terminal t = fakeTerminal(new SeqReader('\n'), new ByteArrayInputStream(new byte[0]));
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), System.out, "Menu",
        Arrays.asList(), null);
    assertEquals(-1, idx);
  }

  @Test
  public void bannerPrintedInJlineLoopDoesNotCrash() throws Exception {
    // Smoke: ensure PrintStream path works.
    Terminal t = fakeTerminal(new SeqReader('\n'), new ByteArrayInputStream(new byte[0]));
    PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8.name());
    int idx = ConsoleUi.readMenuChoice(t, new ByteArrayInputStream(new byte[0]), out, "Menu",
        Arrays.asList("A", "Exit"), "x");
    assertTrue(idx == 0 || idx == 1);
  }
}

