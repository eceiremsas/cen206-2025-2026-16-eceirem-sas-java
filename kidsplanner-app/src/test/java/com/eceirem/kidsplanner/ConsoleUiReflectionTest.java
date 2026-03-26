/**
 * @file ConsoleUiReflectionTest.java
 * @brief Covers remaining ConsoleUi private branches via reflection.
 *
 * This is intentional to satisfy the course requirement of high/total coverage
 * without excluding ConsoleUi from JaCoCo.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PushbackInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class ConsoleUiReflectionTest {

  private static Object callPrivateStatic(Class<?> clazz, String name, Class<?>[] paramTypes, Object... args) throws Exception {
    Method m = clazz.getDeclaredMethod(name, paramTypes);
    m.setAccessible(true);
    return m.invoke(null, args);
  }

  @Test
  public void windowsScanToNavCoversAllCases() throws Exception {
    Class<?> c = ConsoleUi.class;
    int up = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 72);
    int down = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 80);
    int left = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 75);
    int right = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 77);
    int home = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 71);
    int end = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 79);
    int pgup = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 73);
    int pgdn = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 81);
    int other = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 1);

    assertNotEquals(0, up);
    assertNotEquals(0, down);
    assertEquals(0, left);
    assertEquals(0, right);
    assertNotEquals(0, home);
    assertNotEquals(0, end);
    assertNotEquals(0, pgup);
    assertNotEquals(0, pgdn);
    assertEquals(0, other);
  }

  @Test
  public void applyNavCoversAllBranches() throws Exception {
    Class<?> c = ConsoleUi.class;
    // We don't rely on constants; we discover them by calling windowsScanToNav.
    int NAV_UP = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 72);
    int NAV_DOWN = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 80);
    int NAV_HOME = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 71);
    int NAV_END = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 79);
    int NAV_PGUP = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 73);
    int NAV_PGDN = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 81);

    int size = 5;
    assertEquals(4, (Integer) callPrivateStatic(c, "applyNav", new Class<?>[] {int.class, int.class, int.class}, NAV_UP, 0, size));
    assertEquals(1, (Integer) callPrivateStatic(c, "applyNav", new Class<?>[] {int.class, int.class, int.class}, NAV_DOWN, 0, size));
    assertEquals(0, (Integer) callPrivateStatic(c, "applyNav", new Class<?>[] {int.class, int.class, int.class}, NAV_HOME, 3, size));
    assertEquals(4, (Integer) callPrivateStatic(c, "applyNav", new Class<?>[] {int.class, int.class, int.class}, NAV_END, 1, size));
    assertEquals(0, (Integer) callPrivateStatic(c, "applyNav", new Class<?>[] {int.class, int.class, int.class}, NAV_PGUP, 2, size));
    assertEquals(4, (Integer) callPrivateStatic(c, "applyNav", new Class<?>[] {int.class, int.class, int.class}, NAV_PGDN, 2, size));
    assertEquals(2, (Integer) callPrivateStatic(c, "applyNav", new Class<?>[] {int.class, int.class, int.class}, 999, 2, size));
  }

  @Test
  public void readCsiNavigationParsesArrowHomeEndAndTildeParams() throws Exception {
    Class<?> c = ConsoleUi.class;
    int NAV_UP = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 72);
    int NAV_DOWN = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 80);
    int NAV_HOME = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 71);
    int NAV_END = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 79);
    int NAV_PGUP = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 73);
    int NAV_PGDN = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 81);

    assertEquals(NAV_UP, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {'A'})));
    assertEquals(NAV_DOWN, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {'B'})));
    assertEquals(NAV_HOME, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {'H'})));
    assertEquals(NAV_END, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {'F'})));

    // n~ forms
    assertEquals(NAV_HOME, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream("1~".getBytes(StandardCharsets.UTF_8))));
    assertEquals(NAV_HOME, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream("7~".getBytes(StandardCharsets.UTF_8))));
    assertEquals(NAV_END, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream("4~".getBytes(StandardCharsets.UTF_8))));
    assertEquals(NAV_END, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream("8~".getBytes(StandardCharsets.UTF_8))));
    assertEquals(NAV_PGUP, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream("5~".getBytes(StandardCharsets.UTF_8))));
    assertEquals(NAV_PGDN, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream("6~".getBytes(StandardCharsets.UTF_8))));
    assertEquals(0, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream("9~".getBytes(StandardCharsets.UTF_8))));
  }

  @Test
  public void readAnsiNavigationCoversQuitSs3AndBracketPaths() throws Exception {
    Class<?> c = ConsoleUi.class;
    int NAV_UP = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 72);
    int NAV_DOWN = (Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 80);

    // After ESC, second=-1 => quit
    assertNotEquals(0, (Integer) callPrivateStatic(c, "readAnsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {})));

    // ESC q => quit
    assertNotEquals(0, (Integer) callPrivateStatic(c, "readAnsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {'q'})));
    // ESC Q => quit
    assertNotEquals(0, (Integer) callPrivateStatic(c, "readAnsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {'Q'})));

    // SS3: 'O' + 'A'/'B'
    assertEquals(NAV_UP, (Integer) callPrivateStatic(c, "readAnsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {'O', 'A'})));
    assertEquals(NAV_DOWN, (Integer) callPrivateStatic(c, "readAnsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {'O', 'B'})));
    // SS3: home/end, and unknown => NAV_NONE
    assertEquals((Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 71),
        (Integer) callPrivateStatic(c, "readAnsiNavigation", new Class<?>[] {java.io.InputStream.class},
            new ByteArrayInputStream(new byte[] {'O', 'H'})));
    assertEquals((Integer) callPrivateStatic(c, "windowsScanToNav", new Class<?>[] {int.class}, 79),
        (Integer) callPrivateStatic(c, "readAnsiNavigation", new Class<?>[] {java.io.InputStream.class},
            new ByteArrayInputStream(new byte[] {'O', 'F'})));
    assertEquals(0, (Integer) callPrivateStatic(c, "readAnsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {'O', 'X'})));

    // Not '[' => none
    assertEquals(0, (Integer) callPrivateStatic(c, "readAnsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {'X'})));

    // '[' => delegate to CSI
    assertEquals(NAV_DOWN, (Integer) callPrivateStatic(c, "readAnsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {'[', 'B'})));
  }

  @Test
  public void readUtf8ByteCoversInvalidAndValidMultiByte() throws Exception {
    Class<?> c = ConsoleUi.class;
    byte[] single = new byte[1];

    // -1 => -1
    assertEquals(-1, (Integer) callPrivateStatic(c, "readUtf8Byte", new Class<?>[] {java.io.InputStream.class, byte[].class},
        new ByteArrayInputStream(new byte[] {}), single));

    // ASCII
    assertEquals((int) 'A', (Integer) callPrivateStatic(c, "readUtf8Byte", new Class<?>[] {java.io.InputStream.class, byte[].class},
        new ByteArrayInputStream(new byte[] {'A'}), single));

    // Invalid leading byte (not 2/3/4-byte header) => returns first
    assertEquals(0xFF, (Integer) callPrivateStatic(c, "readUtf8Byte", new Class<?>[] {java.io.InputStream.class, byte[].class},
        new ByteArrayInputStream(new byte[] {(byte) 0xFF}), single));

    // Valid 2-byte UTF-8 for 'é' (0xC3 0xA9) => returns a character (not the raw first byte)
    int ch = (Integer) callPrivateStatic(c, "readUtf8Byte", new Class<?>[] {java.io.InputStream.class, byte[].class},
        new ByteArrayInputStream("é".getBytes(StandardCharsets.UTF_8)), single);
    assertTrue(ch > 0);

    // Incomplete 3-byte sequence (0xE2 0x82 EOF) => returns first byte
    int ch3 = (Integer) callPrivateStatic(c, "readUtf8Byte", new Class<?>[] {java.io.InputStream.class, byte[].class},
        new ByteArrayInputStream(new byte[] {(byte) 0xE2, (byte) 0x82}), single);
    assertEquals(0xE2, ch3);

    // Incomplete 4-byte sequence (0xF0 0x9F EOF...) => returns first byte
    int ch4 = (Integer) callPrivateStatic(c, "readUtf8Byte", new Class<?>[] {java.io.InputStream.class, byte[].class},
        new ByteArrayInputStream(new byte[] {(byte) 0xF0, (byte) 0x9F, (byte) 0x92}), single);
    assertEquals(0xF0, ch4);
  }

  @Test
  public void readMenuChoiceCoversEightBitCsiBranchInPublicMethod() throws Exception {
    // Trigger the 0x9B branch in readMenuChoice (public path).
    byte[] seq = new byte[] {(byte) 0x9B, '6', '~', '\n'};
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 16);
    int idx = ConsoleUi.readMenuChoice(in, System.out, "Menu", Arrays.asList("A", "B", "C", "D", "Exit"), null);
    // At least it should not crash; selection must be within bounds.
    assertTrue(idx >= 0 && idx < 5);
  }

  @Test
  public void readMenuChoiceCoversWindowsScanFallbackToUtf8Decode() throws Exception {
    // Prefix 224 then unknown scan code -> pushes back, then reads UTF-8 byte path.
    // Feed: 224, 1 (unknown scan), then 'q' => should quit to last.
    byte[] seq = new byte[] {(byte) 224, (byte) 1, (byte) 'q'};
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 16);
    int idx = ConsoleUi.readMenuChoice(in, System.out, "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(2, idx);
  }

  @Test
  public void privateHelpersCoverNullAndShortPaths() throws Exception {
    Class<?> c = ConsoleUi.class;

    // truncate: null, short, long
    assertEquals("", (String) callPrivateStatic(c, "truncate", new Class<?>[] {String.class, int.class}, null, 3));
    assertEquals("ab", (String) callPrivateStatic(c, "truncate", new Class<?>[] {String.class, int.class}, "ab", 3));
    String tr = (String) callPrivateStatic(c, "truncate", new Class<?>[] {String.class, int.class}, "abcdef", 3);
    assertTrue(tr.endsWith("\u2026"));

    // padCell: null and long
    String p1 = (String) callPrivateStatic(c, "padCell", new Class<?>[] {String.class, int.class}, null, 4);
    assertEquals(4, p1.length());
    String p2 = (String) callPrivateStatic(c, "padCell", new Class<?>[] {String.class, int.class}, "abcdef", 4);
    assertTrue(p2.endsWith("\u2026"));

    // printTableRow: cells shorter than widths (i < cells.length false branch)
    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8.name());
    callPrivateStatic(c, "printTableRow", new Class<?>[] {PrintStream.class, int[].class, String[].class, boolean.class},
        out, new int[] {3, 3, 3}, new String[] {"A"}, true);
    String printed = outBytes.toString(StandardCharsets.UTF_8.name());
    assertTrue(printed.contains("A"));
  }

  @Test
  public void readCsiNavigationReturnsNoneOnEof() throws Exception {
    Class<?> c = ConsoleUi.class;
    assertEquals(0, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {})));
  }

  @Test
  public void readCsiNavigationCoversUnknownFinalAndBareTilde() throws Exception {
    Class<?> c = ConsoleUi.class;
    // Unknown final in 0x40..0x7E range => NAV_NONE
    assertEquals(0, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {'Z'})));
    // '~' with empty params => NAV_NONE
    assertEquals(0, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {'~'})));
    // Param bytes (< 0x40) are collected until a final byte
    assertNotEquals(0, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {'1', 'A'})));
    // Bytes > 0x7E should not trigger final-byte handling (covers c<=0x7E false branch)
    assertNotEquals(0, (Integer) callPrivateStatic(c, "readCsiNavigation", new Class<?>[] {java.io.InputStream.class},
        new ByteArrayInputStream(new byte[] {(byte) 0x7F, 'A'})));
  }
}

