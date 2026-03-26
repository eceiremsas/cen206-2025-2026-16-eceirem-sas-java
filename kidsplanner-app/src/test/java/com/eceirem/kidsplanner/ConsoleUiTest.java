/**
 * @file ConsoleUiTest.java
 * @brief Tests for ConsoleUi formatting and input helpers.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PushbackInputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

public class ConsoleUiTest {

  @Test
  public void bannerAndHelpersPrintSomething() throws Exception {
    String originalOs = System.getProperty("os.name");
    try {
      System.setProperty("os.name", "Linux");
      ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
      PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8.name());
      ConsoleUi.printBanner(out);
      ConsoleUi.rule(out);
      ConsoleUi.title(out, "T");
      ConsoleUi.muted(out, "m");
      ConsoleUi.successLine(out, "ok");
      ConsoleUi.clearScreen(out);
      String printed = outBytes.toString(StandardCharsets.UTF_8.name());
      assertTrue(printed.contains("KIDS  PLANNER"));
    } finally {
      if (originalOs == null) {
        System.clearProperty("os.name");
      } else {
        System.setProperty("os.name", originalOs);
      }
    }
  }

  @Test
  public void readLineUtf8HandlesLf() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream("hello\nrest".getBytes(StandardCharsets.UTF_8));
    String line = ConsoleUi.readLineUtf8(in);
    assertEquals("hello", line);
  }

  @Test
  public void readLineUtf8HandlesCrLf() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream("hello\r\nx".getBytes(StandardCharsets.UTF_8));
    String line = ConsoleUi.readLineUtf8(in);
    assertEquals("hello", line);
  }

  @Test
  public void readMenuChoiceReturnsMinusOneWhenNoOptions() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
    int idx = ConsoleUi.readMenuChoice(in, System.out, "T", Collections.<String>emptyList(), "");
    assertEquals(-1, idx);
  }

  @Test
  public void readMenuChoiceSelectsCurrentOnEnter() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8.name());
    int idx = ConsoleUi.readMenuChoice(in, out, "Menu", Arrays.asList("A", "B", "Exit"), "status");
    assertEquals(0, idx);
    String printed = outBytes.toString(StandardCharsets.UTF_8.name());
    assertTrue(printed.contains("Menu"));
  }

  @Test
  public void readMenuChoiceCyclesWithTabThenEnter() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream("\t\n".getBytes(StandardCharsets.UTF_8));
    int idx = ConsoleUi.readMenuChoice(in, System.out, "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void readMenuChoiceMovesWithWSFallback() throws Exception {
    // s = down, Enter selects
    ByteArrayInputStream in = new ByteArrayInputStream("s\n".getBytes(StandardCharsets.UTF_8));
    int idx = ConsoleUi.readMenuChoice(in, System.out, "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void readMenuChoiceReturnsLastOptionOnQ() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream("q".getBytes(StandardCharsets.UTF_8));
    int idx = ConsoleUi.readMenuChoice(in, System.out, "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(2, idx);
  }

  @Test
  public void readMenuChoiceHandlesWindowsScanCodesAndHomeEnd() throws Exception {
    // Sequence:
    // - 224 80  : down
    // - 224 79  : end
    // - 224 71  : home
    // - Enter  : select
    byte[] seq = new byte[] {(byte) 224, (byte) 80, (byte) 224, (byte) 79, (byte) 224, (byte) 71, (byte) '\n'};
    ByteArrayInputStream raw = new ByteArrayInputStream(seq);
    PushbackInputStream in = new PushbackInputStream(raw, 8);
    int idx = ConsoleUi.readMenuChoice(in, System.out, "Menu", Arrays.asList("A", "B", "C", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void readMenuChoiceHandlesAnsiArrowsHomeEndAndPage() throws Exception {
    // ESC [ B (down), ESC [ 6 ~ (pgdn), ESC [ 5 ~ (pgup), ESC [ F (end), ESC [ H (home), Enter
    byte[] seq = new byte[] {
        27, '[', 'B',
        27, '[', '6', '~',
        27, '[', '5', '~',
        27, '[', 'F',
        27, '[', 'H',
        '\n'
    };
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 32);
    int idx = ConsoleUi.readMenuChoice(in, System.out, "Menu", Arrays.asList("A", "B", "C", "D", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void readMenuChoiceHandlesSs3Sequences() throws Exception {
    // ESC O B (down), Enter
    byte[] seq = new byte[] {27, 'O', 'B', '\n'};
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 8);
    int idx = ConsoleUi.readMenuChoice(in, System.out, "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void readMenuChoiceIgnoresUtf8MultibyteCharacters() throws Exception {
    // "é" is multibyte in UTF-8. Provide it then Enter; it should be ignored and selection stays at 0.
    byte[] seq = "é\n".getBytes(StandardCharsets.UTF_8);
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 8);
    int idx = ConsoleUi.readMenuChoice(in, System.out, "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void printTablesHandleEmptyAndNonEmpty() throws Exception {
    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8.name());

    ConsoleUi.printKidTable(out, Collections.<Kid>emptyList());
    ConsoleUi.printActivityTable(out, Collections.<Activity>emptyList());

    Kid k = new Kid("Ada", 2018, "notes");
    k.setId(1);
    Activity a = new Activity("Math", LocalDate.of(2026, 3, 25), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, "Home", "N", Integer.valueOf(1));
    a.setId(2);
    ConsoleUi.printKidTable(out, Arrays.asList(k));
    ConsoleUi.printActivityTable(out, Arrays.asList(a));

    String printed = outBytes.toString(StandardCharsets.UTF_8.name());
    assertTrue(printed.contains("Ada"));
    assertTrue(printed.contains("Math"));
  }

  @Test
  public void windowsScanCodesLeftRightAreIgnored() throws Exception {
    // left/right scan codes are mapped to NAV_NONE.
    byte[] seq = new byte[] {(byte) 224, (byte) 75, (byte) 224, (byte) 77, (byte) '\n'};
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 8);
    int idx = ConsoleUi.readMenuChoice(in, System.out, "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void ansiEscapeQQuitsToLastOption() throws Exception {
    // ESC q should trigger quit (NAV_QUIT)
    byte[] seq = new byte[] {27, 'q'};
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 8);
    int idx = ConsoleUi.readMenuChoice(in, System.out, "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(2, idx);
  }
}

