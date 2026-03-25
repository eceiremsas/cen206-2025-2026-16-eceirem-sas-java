package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

class ConsoleUiCoverageTest {

  @Test
  void readMenuChoiceEmptyOptions() throws Exception {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    assertEquals(-1, ConsoleUi.readMenuChoice(System.in, new PrintStream(bout), "t", Collections.<String>emptyList(), null));
  }

  @Test
  void readMenuChoiceFirstWithNewline() throws Exception {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bout);
    InputStream in = new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8));
    int idx = ConsoleUi.readMenuChoice(in, out, "title", Arrays.asList("one", "two"), "status");
    assertEquals(0, idx);
  }

  @Test
  void readMenuChoiceTabAndWsAndQ() throws Exception {
    PrintStream out = new PrintStream(new ByteArrayOutputStream());
    assertEquals(1, ConsoleUi.readMenuChoice(new ByteArrayInputStream("\t\n".getBytes(StandardCharsets.UTF_8)), out,
        "t", Arrays.asList("a", "b"), null));
    assertEquals(1, ConsoleUi.readMenuChoice(new ByteArrayInputStream("w\n".getBytes(StandardCharsets.UTF_8)), out,
        "t", Arrays.asList("a", "b"), null));
    assertEquals(1, ConsoleUi.readMenuChoice(new ByteArrayInputStream("s\n".getBytes(StandardCharsets.UTF_8)), out,
        "t", Arrays.asList("a", "b"), null));
    assertEquals(1, ConsoleUi.readMenuChoice(new ByteArrayInputStream("k\n".getBytes(StandardCharsets.UTF_8)), out,
        "t", Arrays.asList("a", "b"), null));
    assertEquals(1, ConsoleUi.readMenuChoice(new ByteArrayInputStream("j\n".getBytes(StandardCharsets.UTF_8)), out,
        "t", Arrays.asList("a", "b"), null));
    assertEquals(1, ConsoleUi.readMenuChoice(new ByteArrayInputStream("q".getBytes(StandardCharsets.UTF_8)), out,
        "t", Arrays.asList("a", "b"), null));
  }

  @Test
  void readMenuChoiceCarriageReturnLinefeedPushback() throws Exception {
    PrintStream out = new PrintStream(new ByteArrayOutputStream());
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream("\r\nx".getBytes(StandardCharsets.UTF_8)));
    assertEquals(0, ConsoleUi.readMenuChoice(in, out, "t", Arrays.asList("a", "b"), "s"));
  }

  @Test
  void readMenuChoiceWindowsArrowScans() throws Exception {
    PrintStream out = new PrintStream(new ByteArrayOutputStream());
    // down (80) then enter
    byte[] d = new byte[] { (byte) 224, 80, '\n' };
    assertEquals(1, ConsoleUi.readMenuChoice(new ByteArrayInputStream(d), out, "t", Arrays.asList("a", "b"), null));
    byte[] u = new byte[] { 0, 72, '\n' };
    assertEquals(1, ConsoleUi.readMenuChoice(new ByteArrayInputStream(u), out, "t", Arrays.asList("a", "b"), null));
    byte[] home = new byte[] { (byte) 224, 71, '\n' };
    assertEquals(0, ConsoleUi.readMenuChoice(new ByteArrayInputStream(home), out, "t", Arrays.asList("a", "b", "c"), null));
    byte[] end = new byte[] { (byte) 224, 79, '\n' };
    assertEquals(2, ConsoleUi.readMenuChoice(new ByteArrayInputStream(end), out, "t", Arrays.asList("a", "b", "c"), null));
    byte[] pg = new byte[] { (byte) 224, 73, '\n' };
    assertEquals(0, ConsoleUi.readMenuChoice(new ByteArrayInputStream(pg), out, "t", Arrays.asList("a", "b", "c", "d"), null));
  }

  @Test
  void readMenuChoiceAnsiEsc() throws Exception {
    PrintStream out = new PrintStream(new ByteArrayOutputStream());
    // ESC [ B = down
    byte[] down = new byte[] { 27, '[', 'B', '\n' };
    assertEquals(1, ConsoleUi.readMenuChoice(new ByteArrayInputStream(down), out, "t", Arrays.asList("a", "b"), null));
    byte[] csiQuit = new byte[] { (byte) 0x9B, '1', '~', '\n' };
    assertEquals(0, ConsoleUi.readMenuChoice(new ByteArrayInputStream(csiQuit), out, "t", Arrays.asList("only"), null));
    byte[] escOA = new byte[] { 27, 'O', 'B', '\n' };
    assertEquals(1, ConsoleUi.readMenuChoice(new ByteArrayInputStream(escOA), out, "t", Arrays.asList("a", "b"), null));
  }

  @Test
  void readMenuChoiceUtf8LeadByteSkipped() throws Exception {
    PrintStream out = new PrintStream(new ByteArrayOutputStream());
    byte[] utf = "é".getBytes(StandardCharsets.UTF_8);
    byte[] script = new byte[utf.length + 1];
    System.arraycopy(utf, 0, script, 0, utf.length);
    script[script.length - 1] = '\n';
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(script));
    assertEquals(0, ConsoleUi.readMenuChoice(in, out, "t", Arrays.asList("a", "b"), null));
  }

  @Test
  void readMenuChoiceEofReturnsLast() throws Exception {
    PrintStream out = new PrintStream(new ByteArrayOutputStream());
    assertEquals(1, ConsoleUi.readMenuChoice(new ByteArrayInputStream(new byte[0]), out, "t", Arrays.asList("a", "b"), null));
  }

  @Test
  void readLineUtf8CrLfPushback() throws Exception {
    PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream("\ra".getBytes(StandardCharsets.UTF_8)));
    assertEquals("", ConsoleUi.readLineUtf8(in));
    assertEquals('a', in.read());
  }

  @Test
  void readLineUtf8LfOnly() throws Exception {
    assertEquals("hi", ConsoleUi.readLineUtf8(new ByteArrayInputStream("hi\n".getBytes(StandardCharsets.UTF_8))));
  }

  @Test
  void tablesAndDecorations() throws Exception {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bout);
    ConsoleUi.clearScreen(out);
    ConsoleUi.rule(out);
    ConsoleUi.title(out, "X");
    ConsoleUi.muted(out, "m");
    ConsoleUi.successLine(out, "ok");
    ConsoleUi.printKidTable(out, Collections.<Kid>emptyList());
    List<Kid> kids = new ArrayList<Kid>();
    Kid k = new Kid("LongnameLongnameLongnameLongname", 2000, "notesnotesnotesnotesnotes");
    k.setId(1);
    kids.add(k);
    ConsoleUi.printKidTable(out, kids);
    ConsoleUi.printActivityTable(out, Collections.<Activity>emptyList());
    List<Activity> acts = new ArrayList<Activity>();
    Activity a =
        new Activity("T", LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), ActivityType.STUDY, "L", "N", 1);
    a.setId(2);
    acts.add(a);
    ConsoleUi.printActivityTable(out, acts);
    String os = System.getProperty("os.name");
    System.setProperty("os.name", "Windows 10");
    try {
      ConsoleUi.printBanner(out);
    } finally {
      System.setProperty("os.name", os);
    }
    assertTrue(bout.toString("UTF-8").length() > 10);
  }

  @Test
  void readMenuChoiceJlinePathSelectFirst() throws Exception {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(bout);
    InputStream in = new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8));
    Terminal t = TerminalBuilder.builder().system(false).streams(in, bout).dumb(true).build();
    try {
      int idx = ConsoleUi.readMenuChoice(t, in, out, "T", Arrays.asList("a", "b"), "s");
      assertEquals(0, idx);
    } finally {
      t.close();
    }
  }

  @Test
  void readMenuChoiceJlineEmptyOptions() throws Exception {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    InputStream in = new ByteArrayInputStream(new byte[0]);
    Terminal t = TerminalBuilder.builder().system(false).streams(in, bout).dumb(true).build();
    try {
      assertEquals(-1, ConsoleUi.readMenuChoice(t, in, new PrintStream(bout), "T", Collections.<String>emptyList(), null));
    } finally {
      t.close();
    }
  }
}
