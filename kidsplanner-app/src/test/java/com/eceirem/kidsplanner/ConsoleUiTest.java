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

  private static PrintStream silentOut() throws Exception {
    return new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8.name());
  }

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
  public void bannerPrintsWindowsHintOnWindows() throws Exception {
    String originalOs = System.getProperty("os.name");
    try {
      System.setProperty("os.name", "Windows 11");
      ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
      PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8.name());
      ConsoleUi.printBanner(out);
      String printed = outBytes.toString(StandardCharsets.UTF_8.name());
      assertTrue(printed.toLowerCase().contains("windows"));
      assertTrue(printed.toLowerCase().contains("chcp"));
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
  public void readLineUtf8HandlesEof() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
    String line = ConsoleUi.readLineUtf8(in);
    assertEquals("", line);
  }

  @Test
  public void readLineUtf8HandlesCrLf() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream("hello\r\nx".getBytes(StandardCharsets.UTF_8));
    String line = ConsoleUi.readLineUtf8(in);
    assertEquals("hello", line);
  }

  @Test
  public void readLineUtf8HandlesCrWithoutLfAndUnreadsNextByte() throws Exception {
    // When CR is followed by a non-LF byte and the stream supports pushback,
    // ConsoleUi should unread that byte so it can be consumed later.
    PushbackInputStream in = new PushbackInputStream(
        new ByteArrayInputStream("hello\rX".getBytes(StandardCharsets.UTF_8)),
        8);
    String line = ConsoleUi.readLineUtf8(in);
    assertEquals("hello", line);
    int next = in.read();
    assertEquals((int) 'X', next);
  }

  @Test
  public void readLineUtf8HandlesCrAtEof() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream("hello\r".getBytes(StandardCharsets.UTF_8));
    String line = ConsoleUi.readLineUtf8(in);
    assertEquals("hello", line);
  }

  @Test
  public void readLineUtf8CrWithoutLfDoesNotUnreadWhenNotPushback() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream("hello\rX".getBytes(StandardCharsets.UTF_8));
    String line = ConsoleUi.readLineUtf8(in);
    assertEquals("hello", line);
  }

  @Test
  public void readMenuChoiceReturnsMinusOneWhenNoOptions() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "T", Collections.<String>emptyList(), "");
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
  public void readMenuChoiceDoesNotPrintStatusWhenEmptyString() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8));
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "Exit"), "");
    assertEquals(0, idx);
  }

  @Test
  public void readMenuChoiceCyclesWithTabThenEnter() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream("\t\n".getBytes(StandardCharsets.UTF_8));
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void readMenuChoiceMovesWithWSFallback() throws Exception {
    // s = down, Enter selects
    ByteArrayInputStream in = new ByteArrayInputStream("s\n".getBytes(StandardCharsets.UTF_8));
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void readMenuChoiceReturnsLastOptionOnQ() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream("q".getBytes(StandardCharsets.UTF_8));
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(2, idx);
  }

  @Test
  public void readMenuChoiceReturnsLastOptionOnUppercaseQ() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream("Q".getBytes(StandardCharsets.UTF_8));
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(2, idx);
  }

  @Test
  public void readMenuChoiceHandlesCarriageReturnAndPushback() throws Exception {
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream("\rX".getBytes(StandardCharsets.UTF_8)), 8);
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
    assertEquals((int) 'X', in.read());
  }

  @Test
  public void readMenuChoiceHandlesCarriageReturnAtEof() throws Exception {
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream("\r".getBytes(StandardCharsets.UTF_8)), 8);
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void readMenuChoiceHandlesCarriageReturnLineEndingCrLf() throws Exception {
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream("\r\n".getBytes(StandardCharsets.UTF_8)), 8);
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void readMenuChoiceCarriageReturnDoesNotUnreadWhenNotPushback() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream("\rX".getBytes(StandardCharsets.UTF_8));
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void readMenuChoiceMovesWithVimKeysAndWS() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream("jksw\n".getBytes(StandardCharsets.UTF_8));
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void readMenuChoiceMovesWithUppercaseVariants() throws Exception {
    // S down, W up, J down, K up, then Enter.
    ByteArrayInputStream in = new ByteArrayInputStream("SWJK\n".getBytes(StandardCharsets.UTF_8));
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void readMenuChoiceHandlesEightBitCsiPageDown() throws Exception {
    byte[] seq = new byte[] {(byte) 0x9B, '6', '~', '\n'};
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 16);
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "C", "D", "Exit"), null);
    assertEquals(3, idx);
  }

  @Test
  public void readMenuChoiceHandlesScanCodePrefixWithMissingScanByte() throws Exception {
    ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {(byte) 224});
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(2, idx);
  }

  @Test
  public void readMenuChoiceHandlesZeroPrefixScanCodes() throws Exception {
    // 0 prefix, scan 80 down, enter => index 1
    byte[] seq = new byte[] {0, 80, '\n'};
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 8);
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void readMenuChoiceHandlesScanCodesThatMapToNone() throws Exception {
    // 224 prefix, scan 75 => NAV_NONE, then q => last
    byte[] seq = new byte[] {(byte) 224, (byte) 75, 'q'};
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 16);
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(2, idx);
  }

  @Test
  public void readMenuChoiceHandlesEightBitCsiNoop() throws Exception {
    // 0x9B 9~ is not mapped => NAV_NONE; then Enter selects 0.
    byte[] seq = new byte[] {(byte) 0x9B, '9', '~', '\n'};
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 16);
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void readMenuChoiceEscapeUnknownIsNoop() throws Exception {
    // ESC X => NAV_NONE; then Enter selects 0.
    byte[] seq = new byte[] {27, 'X', '\n'};
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 16);
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void readMenuChoiceWindowsScanUnknownWithNoPushbackFallsBackToEofReturnLast() throws Exception {
    // Not a PushbackInputStream => nav==NAV_NONE => readUtf8Byte hits EOF => returns last.
    ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {(byte) 224, (byte) 1});
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(2, idx);
  }

  @Test
  public void readMenuChoiceWindowsScanUnknownWithPushbackDecodesNextByte() throws Exception {
    // With pushback, unknown scan codes are unread and then decoded via readUtf8Byte.
    // Feed: 224, 1 (unknown scan), then 'q' => should quit to last.
    byte[] seq = new byte[] {(byte) 224, (byte) 1, (byte) 'q'};
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 16);
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
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
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "C", "Exit"), null);
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
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "C", "D", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void readMenuChoiceHandlesSs3Sequences() throws Exception {
    // ESC O B (down), Enter
    byte[] seq = new byte[] {27, 'O', 'B', '\n'};
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 8);
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(1, idx);
  }

  @Test
  public void readMenuChoiceIgnoresUtf8MultibyteCharacters() throws Exception {
    // "é" is multibyte in UTF-8. Provide it then Enter; it should be ignored and selection stays at 0.
    byte[] seq = "é\n".getBytes(StandardCharsets.UTF_8);
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 8);
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void readMenuChoiceIgnoresUtf8MultibyteCharactersWhenNotPushback() throws Exception {
    byte[] seq = "é\n".getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream in = new ByteArrayInputStream(seq);
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
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
  public void printTablesTruncateAndEllipsisBranches() throws Exception {
    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8.name());

    Kid k = new Kid("This name is definitely longer than twenty two", null,
        "this note is definitely longer than sixteen chars");
    k.setId(12);

    Activity a = new Activity(
        "This title is definitely longer than eighteen chars",
        LocalDate.of(2026, 3, 25),
        LocalTime.of(9, 0, 1),
        LocalTime.of(23, 59, 59),
        ActivityType.SOCIAL,
        "Somewhere",
        "Notes",
        Integer.valueOf(12));
    a.setId(34);

    ConsoleUi.printKidTable(out, Arrays.asList(k));
    ConsoleUi.printActivityTable(out, Arrays.asList(a));

    String printed = outBytes.toString(StandardCharsets.UTF_8.name());
    assertTrue(printed.contains("12"));
    assertTrue(printed.contains("34"));
    assertTrue(printed.contains("\u2026"));
  }

  @Test
  public void printKidTableCoversNullNotes() throws Exception {
    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8.name());
    Kid k = new Kid("A", 2010, null);
    k.setId(1);
    ConsoleUi.printKidTable(out, Arrays.asList(k));
    assertTrue(outBytes.toString(StandardCharsets.UTF_8.name()).contains("A"));
  }

  @Test
  public void windowsScanCodesLeftRightAreIgnored() throws Exception {
    // left/right scan codes are mapped to NAV_NONE.
    byte[] seq = new byte[] {(byte) 224, (byte) 75, (byte) 224, (byte) 77, (byte) '\n'};
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 8);
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(0, idx);
  }

  @Test
  public void ansiEscapeQQuitsToLastOption() throws Exception {
    // ESC q should trigger quit (NAV_QUIT)
    byte[] seq = new byte[] {27, 'q'};
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(seq), 8);
    int idx = ConsoleUi.readMenuChoice(in, silentOut(), "Menu", Arrays.asList("A", "B", "Exit"), null);
    assertEquals(2, idx);
  }
}

