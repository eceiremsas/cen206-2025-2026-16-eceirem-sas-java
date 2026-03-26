/**
 * @file ConsoleUi.java
 * @brief ANSI console styling and keyboard-driven menus.
 */
package com.eceirem.kidsplanner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;

/**
 * Rich console styling (ANSI) and keyboard navigation (arrow keys / Tab / Home / End) for menus.
 * Menus are navigated with the keyboard only — no number-key selection (course guideline).
 */
public final class ConsoleUi {

  /** ANSI SGR reset. */
  public static final String RESET = "\u001B[0m";
  /** Bold style on. */
  public static final String BOLD = "\u001B[1m";
  /** Dim / faint style. */
  public static final String DIM = "\u001B[2m";
  /** Foreground cyan. */
  public static final String FG_CYAN = "\u001B[36m";
  /** Foreground green. */
  public static final String FG_GREEN = "\u001B[32m";
  /** Foreground yellow. */
  public static final String FG_YELLOW = "\u001B[33m";
  /** Foreground magenta. */
  public static final String FG_MAGENTA = "\u001B[35m";
  /** Foreground bright white. */
  public static final String FG_WHITE = "\u001B[97m";
  /** Foreground blue. */
  public static final String FG_BLUE = "\u001B[34m";
  /** Foreground red. */
  public static final String FG_RED = "\u001B[31m";
  /** Background blue. */
  public static final String BG_BLUE = "\u001B[44m";
  /**
   * ASCII box drawing for maximum compatibility (some Windows consoles render Unicode line-art as {@code ?}).
   */
  private static final String HLINE = "-";
  /** Vertical border character for tables/boxes. */
  private static final String VLINE = "|";
  /** Top-left corner character for tables/boxes. */
  private static final String TLC = "+";
  /** Top-right corner character for tables/boxes. */
  private static final String TRC = "+";
  /** Bottom-left corner character for tables/boxes. */
  private static final String BLC = "+";
  /** Bottom-right corner character for tables/boxes. */
  private static final String BRC = "+";
  /** Left tee junction character for tables/boxes. */
  private static final String TEE_L = "+";
  /** Right tee junction character for tables/boxes. */
  private static final String TEE_R = "+";
  /** Crossing junction character for tables/boxes. */
  private static final String CROSS = "+";
  /** Time formatting for tables. */
  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

  /** Prevents instantiation. */
  private ConsoleUi() {
  }

  /** Clears the screen and moves the cursor home (ANSI). */
  public static void clearScreen(PrintStream out) {
    out.print("\u001B[2J\u001B[H");
    out.flush();
  }

  /** Prints the application banner and optional Windows UTF-8 hint. */
  public static void printBanner(PrintStream out) {
    out.println();
    out.println(DIM + "  +------------------------------------------------------------+" + RESET);
    out.println(FG_CYAN + BOLD + "  |" + RESET + "   " + FG_WHITE + BOLD + "KIDS  PLANNER" + RESET + "  "
        + FG_MAGENTA + "." + RESET + "  " + DIM + "CEN206 Kids Activity Planner Midterm Project" + " " + FG_CYAN + BOLD + "|" + RESET);
    out.println(FG_GREEN + BOLD + "  |" + RESET + "   " + DIM + "Child & activity management - Binary - SQLite - MySQL" + "     "
        + FG_GREEN + BOLD + "|" + RESET);
    out.println(DIM + "  +------------------------------------------------------------+" + RESET);
    out.println();
    if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
      out.println(DIM + "  (Windows: for colors and UTF-8, Windows Terminal + chcp 65001 is recommended.)" + RESET);
      out.println();
    }
  }

  /** Horizontal rule for visual separation. */
  public static void rule(PrintStream out) {
    out.println(DIM + "  " + repeat(HLINE, 58) + RESET);
  }

  /** Section title with highlighted bar. */
  public static void title(PrintStream out, String text) {
    out.println();
    out.println("  " + FG_WHITE + BG_BLUE + BOLD + " " + text + " " + RESET);
    rule(out);
  }

  /** Dimmed single line (secondary text). */
  public static void muted(PrintStream out, String text) {
    out.println(DIM + "  " + text + RESET);
  }

  /** Success / confirmation line in green. */
  public static void successLine(PrintStream out, String text) {
    out.println("  " + FG_GREEN + text + RESET);
  }

  /**
   * Reads one line of UTF-8 from stdin without using java.util.Scanner, so it does not
   * compete with {@link #readMenuChoice} on the same stream. Use the same java.io.PushbackInputStream
   * instance that wraps {@code System.in} in menu mode.
   */
  public static String readLineUtf8(InputStream in) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    while (true) {
      int b = in.read();
      if (b == -1) {
        break;
      }
      if (b == '\n') {
        break;
      }
      if (b == '\r') {
        int n = in.read();
        if (n != '\n' && n != -1) {
          if (in instanceof PushbackInputStream) {
            ((PushbackInputStream) in).unread(n);
          }
        }
        break;
      }
      buf.write(b);
    }
    return new String(buf.toByteArray(), StandardCharsets.UTF_8);
  }

  /**
   * Prefer this overload in menu mode when a JLine org.jline.terminal.Terminal is available (works with
   * {@code java -jar} on Windows).
   */
  public static int readMenuChoice(Terminal terminal, InputStream fallbackIn, PrintStream out, String title,
      List<String> options, String statusLine) throws IOException {
    if (terminal != null) {
      return readMenuChoiceJline(terminal, out, title, options, statusLine);
    }
    return readMenuChoice(fallbackIn, out, title, options, statusLine);
  }

  /** JLine-based menu when a real terminal is available. */
  private static int readMenuChoiceJline(Terminal terminal, PrintStream out, String title, List<String> options,
      String statusLine) throws IOException {
    if (options.isEmpty()) {
      return -1;
    }
    int index = 0;
    Attributes original = terminal.enterRawMode();
    try {
      NonBlockingReader reader = terminal.reader();
      while (true) {
        clearScreen(out);
        printBanner(out);
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        String clock = today + "  " + now.format(TIME_FMT);
        out.println("  " + DIM + "Time: " + RESET + FG_CYAN + clock + RESET);
        out.println();
        drawMenuBox(out, title, options, index);
        out.println();
        if (statusLine != null && !statusLine.isEmpty()) {
          out.println("  " + DIM + statusLine + RESET);
          out.println();
        }
        out.println("  " + DIM + "Arrow keys / Tab / Enter / Esc/Q: exit" + RESET);
        out.flush();

        int ch = reader.read();
        if (ch == -1) {
          return options.size() - 1;
        }
        if (ch == '\r' || ch == '\n') {
          return index;
        }
        if (ch == '\t') {
          index = (index + 1) % options.size();
          continue;
        }
        if (ch == 'q' || ch == 'Q') {
          return options.size() - 1;
        }
        // Arrow keys / navigation (ANSI)
        if (ch == 27) {
          int nav = readAnsiNavigation(terminal.input());
          if (nav == NAV_QUIT) {
            return options.size() - 1;
          }
          if (nav != NAV_NONE) {
            index = applyNav(nav, index, options.size());
          }
          continue;
        }
        // Windows scan codes also work in some environments even in raw
        if (ch == 0 || ch == 224) {
          int scan = reader.read();
          int nav = windowsScanToNav(scan);
          if (nav != NAV_NONE) {
            index = applyNav(nav, index, options.size());
          }
          continue;
        }
        // Fallback keys
        if (ch == 'w' || ch == 'W' || ch == 'k' || ch == 'K') {
          index = (index - 1 + options.size()) % options.size();
          continue;
        }
        if (ch == 's' || ch == 'S' || ch == 'j' || ch == 'J') {
          index = (index + 1) % options.size();
          continue;
        }
      }
    } finally {
      terminal.setAttributes(original);
    }
  }

  /**
   * Reads a menu choice with optional status line under the menu box.
   *
   * @param in should be a java.io.PushbackInputStream around {@code System.in} on Windows so
   *           {@code \r\n} line endings do not leave a stray {@code \n} for the next read.
   * @param out menu output stream
   * @param title menu title to show inside the box
   * @param options menu options (last option is typically "exit")
   * @param statusLine optional status line under the menu box (may be null)
   * @return selected index
   */
  public static int readMenuChoice(InputStream in, PrintStream out, String title, List<String> options, String statusLine)
      throws IOException {
    if (options.isEmpty()) {
      return -1;
    }
    int index = 0;
    byte[] buffer = new byte[1];
    while (true) {
      clearScreen(out);
      printBanner(out);
      LocalDate today = LocalDate.now();
      LocalTime now = LocalTime.now();
      String clock = today + "  " + now.format(TIME_FMT);
      out.println("  " + DIM + "Time: " + RESET + FG_CYAN + clock + RESET);
      out.println();
      drawMenuBox(out, title, options, index);
      out.println();
      if (statusLine != null && !statusLine.isEmpty()) {
        out.println("  " + DIM + statusLine + RESET);
        out.println();
      }
      out.println("  " + DIM + "Up/down / Tab / Home/End / W/S (fallback) / Enter: select" + RESET);
      out.println("  " + DIM + "Windows cmd: arrows may need W/S; Windows Terminal is recommended." + RESET);
      out.println("  " + DIM + "Exit: choose the last row or press Q" + RESET);
      out.flush();

      // Do not use readUtf8Byte for the first byte: on Windows, arrow keys send 0xE0 (same as UTF-8 lead)
      // for extended scan codes, which must be handled before UTF-8 decoding.
      int read = in.read();
      if (read == -1) {
        return options.size() - 1;
      }
      if (read == '\r') {
        int n = in.read();
        if (n != '\n' && n != -1 && in instanceof PushbackInputStream) {
          ((PushbackInputStream) in).unread(n);
        }
        return index;
      }
      if (read == '\n') {
        return index;
      }
      if (read == '\t') {
        index = (index + 1) % options.size();
        continue;
      }
      if (read == 'w' || read == 'W') {
        index = (index - 1 + options.size()) % options.size();
        continue;
      }
      if (read == 's' || read == 'S') {
        index = (index + 1) % options.size();
        continue;
      }
      // vim-style fallback (some IDE consoles swallow arrow keys)
      if (read == 'k' || read == 'K') {
        index = (index - 1 + options.size()) % options.size();
        continue;
      }
      if (read == 'j' || read == 'J') {
        index = (index + 1) % options.size();
        continue;
      }
      // Windows conhost (classic cmd, VT off): prefix 0x00 or 0xE0 + virtual-key scan code
      if (read == 0 || read == 224) {
        int scan = in.read();
        if (scan == -1) {
          continue;
        }
        int nav = windowsScanToNav(scan);
        if (nav != NAV_NONE) {
          index = applyNav(nav, index, options.size());
          continue;
        }
        if (in instanceof PushbackInputStream) {
          PushbackInputStream pin = (PushbackInputStream) in;
          pin.unread(scan);
          pin.unread(read);
        }
        read = readUtf8Byte(in, buffer);
        if (read == -1) {
          return options.size() - 1;
        }
        continue;
      }
      // 8-bit CSI (single byte control sequence introducer)
      if (read == 0x9B) {
        int nav = readCsiNavigation(in);
        if (nav != NAV_NONE) {
          index = applyNav(nav, index, options.size());
        }
        continue;
      }
      if (read >= 0x80) {
        if (in instanceof PushbackInputStream) {
          ((PushbackInputStream) in).unread(read);
        }
        readUtf8Byte(in, buffer);
        continue;
      }
      if (read == 27) {
        int nav = readAnsiNavigation(in);
        if (nav == NAV_QUIT) {
          return options.size() - 1;
        }
        if (nav != NAV_NONE) {
          index = applyNav(nav, index, options.size());
        }
        continue;
      }
      if (read == 'q' || read == 'Q') {
        return options.size() - 1;
      }
    }
  }

  /** Draws a framed menu with a highlighted option row. */
  private static void drawMenuBox(PrintStream out, String title, List<String> options, int selectedIndex) {
    int inner = Math.max(title.length(), 12);
    for (String o : options) {
      inner = Math.max(inner, o.length() + 2);
    }
    inner = Math.min(inner + 2, 54);
    out.print("  ");
    out.println(TLC + repeat(HLINE, inner + 2) + TRC);
    String titlePlain = " " + title + " ";
    while (titlePlain.length() < inner + 2) {
      titlePlain += " ";
    }
    out.print("  ");
    out.println(VLINE + FG_WHITE + BOLD + titlePlain + RESET + VLINE);
    out.print("  ");
    out.println(TEE_L + repeat(HLINE, inner + 2) + TEE_R);
    for (int i = 0; i < options.size(); i++) {
      String label = options.get(i);
      // Use a simple ASCII dot so it renders reliably in all Windows consoles.
      String plain = (i == selectedIndex) ? (". " + label) : ("  " + label);
      while (plain.length() < inner + 2) {
        plain += " ";
      }
      out.print("  ");
      out.print(VLINE + " ");
      if (i == selectedIndex) {
        out.print(FG_GREEN + BOLD + plain + RESET);
      } else {
        out.print(DIM + plain + RESET);
      }
      out.println(" " + VLINE);
    }
    out.print("  ");
    out.println(BLC + repeat(HLINE, inner + 2) + BRC);
  }

  /** Repeats a string {@code n} times (for box drawing). */
  private static String repeat(String ch, int n) {
    StringBuilder sb = new StringBuilder(n * ch.length());
    for (int i = 0; i < n; i++) {
      sb.append(ch);
    }
    return sb.toString();
  }

  /** Prints {@link Kid} rows as a UTF-8 table. */
  public static void printKidTable(PrintStream out, List<Kid> kids) {
    if (kids.isEmpty()) {
      muted(out, "(no kids on file)");
      return;
    }
    String[] headers = {"ID", "Name", "Birth yr.", "Notes"};
    int[] w = {4, 22, 7, 18};
    printTableTop(out, w);
    printTableRow(out, w, headers, true);
    printTableSep(out, w);
    for (Kid k : kids) {
      String note = k.getNotes() == null ? "" : k.getNotes();
      if (note.length() > 16) {
        note = note.substring(0, 15) + "\u2026";
      }
      String y = k.getBirthYear() == null ? "-" : String.valueOf(k.getBirthYear());
      printTableRow(out, w, new String[] {
          String.valueOf(k.getId()),
          truncate(k.getDisplayName(), 22),
          y,
          note
      });
    }
    printTableBottom(out, w);
  }

  /** Prints {@link Activity} rows as a UTF-8 table. */
  public static void printActivityTable(PrintStream out, List<Activity> activities) {
    if (activities.isEmpty()) {
      muted(out, "(no activities)");
      return;
    }
    int[] w = {4, 4, 12, 11, 7, 18};
    printTableTop(out, w);
    printTableRow(out, w, new String[] {"ID", "CID", "Date", "Time", "Type", "Title"}, true);
    printTableSep(out, w);
    for (Activity a : activities) {
      String timeR = a.getStartTime() + "-" + a.getEndTime();
      if (timeR.length() > 11) {
        timeR = timeR.substring(0, 8) + "\u2026";
      }
      printTableRow(out, w, new String[] {
          String.valueOf(a.getId()),
          String.valueOf(a.getKidId()),
          a.getDate().toString(),
          timeR,
          a.getType().name(),
          truncate(a.getTitle(), 18)
      });
    }
    printTableBottom(out, w);
  }

  /** Top border for a column-width table. */
  private static void printTableTop(PrintStream out, int[] widths) {
    out.print("  ");
    out.print(TLC);
    for (int i = 0; i < widths.length; i++) {
      out.print(repeat(HLINE, widths[i] + 2));
      if (i < widths.length - 1) {
        out.print(CROSS);
      }
    }
    out.println(TRC);
  }

  /** Separator row between header and body. */
  private static void printTableSep(PrintStream out, int[] widths) {
    out.print("  ");
    out.print(TEE_L);
    for (int i = 0; i < widths.length; i++) {
      out.print(repeat(HLINE, widths[i] + 2));
      if (i < widths.length - 1) {
        out.print(CROSS);
      }
    }
    out.println(TEE_R);
  }

  /** Bottom border closing the table. */
  private static void printTableBottom(PrintStream out, int[] widths) {
    out.print("  ");
    out.print(BLC);
    for (int i = 0; i < widths.length; i++) {
      out.print(repeat(HLINE, widths[i] + 2));
      if (i < widths.length - 1) {
        out.print(CROSS);
      }
    }
    out.println(BRC);
  }

  /** Data row using default (non-header) styling. */
  private static void printTableRow(PrintStream out, int[] widths, String[] cells) {
    printTableRow(out, widths, cells, false);
  }

  /** Prints one row; {@code header} uses yellow emphasis. */
  private static void printTableRow(PrintStream out, int[] widths, String[] cells, boolean header) {
    out.print("  ");
    out.print(VLINE);
    for (int i = 0; i < widths.length; i++) {
      String c = i < cells.length ? cells[i] : "";
      String padded = padCell(c, widths[i]);
      out.print(" ");
      if (header) {
        out.print(FG_YELLOW + BOLD);
      }
      out.print(padded);
      if (header) {
        out.print(RESET);
      }
      out.print(" " + VLINE);
    }
    out.println();
  }

  /** Pads or truncates cell text to a fixed display width. */
  private static String padCell(String cell, int width) {
    String t = cell == null ? "" : cell;
    if (t.length() > width) {
      t = t.substring(0, Math.max(0, width - 1)) + "\u2026";
    }
    StringBuilder sb = new StringBuilder(t);
    while (sb.length() < width) {
      sb.append(' ');
    }
    return sb.toString();
  }

  /** Truncates with ellipsis; null becomes empty. */
  private static String truncate(String s, int max) {
    if (s == null) {
      return "";
    }
    if (s.length() <= max) {
      return s;
    }
    return s.substring(0, Math.max(0, max - 1)) + "\u2026";
  }

  /** Internal navigation: no-op. */
  private static final int NAV_NONE = 0;
  /** Move selection up. */
  private static final int NAV_UP = 1;
  /** Move selection down. */
  private static final int NAV_DOWN = 2;
  /** Jump to first item. */
  private static final int NAV_HOME = 3;
  /** Jump to last item. */
  private static final int NAV_END = 4;
  /** Page up in long lists. */
  private static final int NAV_PGUP = 5;
  /** Page down in long lists. */
  private static final int NAV_PGDN = 6;
  /** User requested quit / back. */
  private static final int NAV_QUIT = 7;

  /** Windows ReadConsole scan codes after 0x00 / 0xE0 prefix (arrow keys, etc.). */
  private static int windowsScanToNav(int scan) {
    switch (scan) {
      case 72:
        return NAV_UP;
      case 80:
        return NAV_DOWN;
      case 75:
        return NAV_NONE;
      case 77:
        return NAV_NONE;
      case 71:
        return NAV_HOME;
      case 79:
        return NAV_END;
      case 73:
        return NAV_PGUP;
      case 81:
        return NAV_PGDN;
      default:
        return NAV_NONE;
    }
  }

  /** Applies a navigation code to the current menu index. */
  private static int applyNav(int nav, int index, int size) {
    if (nav == NAV_UP) {
      return (index - 1 + size) % size;
    }
    if (nav == NAV_DOWN) {
      return (index + 1) % size;
    }
    if (nav == NAV_HOME) {
      return 0;
    }
    if (nav == NAV_END) {
      return size - 1;
    }
    if (nav == NAV_PGUP) {
      return Math.max(0, index - 3);
    }
    if (nav == NAV_PGDN) {
      return Math.min(size - 1, index + 3);
    }
    return index;
  }

  /**
   * Reads a CSI navigation sequence when the introducer is already consumed.
   * Expected forms: A/B (arrows), H/F (home/end) or {@code n~} (home/end/pgup/pgdn).
   */
  private static int readCsiNavigation(InputStream in) throws IOException {
    StringBuilder params = new StringBuilder();
    while (true) {
      int c = in.read();
      if (c == -1) {
        return NAV_NONE;
      }
      if (c >= 0x40 && c <= 0x7E) {
        if (c == 'A') {
          return NAV_UP;
        }
        if (c == 'B') {
          return NAV_DOWN;
        }
        if (c == 'H') {
          return NAV_HOME;
        }
        if (c == 'F') {
          return NAV_END;
        }
        if (c == '~') {
          String p = params.toString();
          if ("1".equals(p) || "7".equals(p)) {
            return NAV_HOME;
          }
          if ("4".equals(p) || "8".equals(p)) {
            return NAV_END;
          }
          if ("5".equals(p)) {
            return NAV_PGUP;
          }
          if ("6".equals(p)) {
            return NAV_PGDN;
          }
          return NAV_NONE;
        }
        return NAV_NONE;
      }
      params.append((char) c);
    }
  }

  /**
   * After leading ESC (27), interprets the rest of an ANSI navigation sequence.
   */
  private static int readAnsiNavigation(InputStream in) throws IOException {
    int second = in.read();
    if (second == -1) {
      return NAV_QUIT;
    }
    if (second == 'q' || second == 'Q') {
      return NAV_QUIT;
    }
    // SS3 cursor keys: ESC O A / ESC O B (some terminals)
    if (second == 'O') {
      int third = in.read();
      if (third == 'A') {
        return NAV_UP;
      }
      if (third == 'B') {
        return NAV_DOWN;
      }
      if (third == 'H') {
        return NAV_HOME;
      }
      if (third == 'F') {
        return NAV_END;
      }
      return NAV_NONE;
    }
    if (second != '[') {
      return NAV_NONE;
    }
    return readCsiNavigation(in);
  }

  /** Reads one Unicode code point from UTF-8, or raw byte if invalid; used after pushback. */
  private static int readUtf8Byte(InputStream in, byte[] single) throws IOException {
    int first = in.read();
    if (first == -1) {
      return -1;
    }
    if (first < 0x80) {
      return first;
    }
    int expected;
    if ((first & 0xE0) == 0xC0) {
      expected = 2;
    } else if ((first & 0xF0) == 0xE0) {
      expected = 3;
    } else if ((first & 0xF8) == 0xF0) {
      expected = 4;
    } else {
      return first;
    }
    byte[] bytes = new byte[expected];
    bytes[0] = (byte) first;
    for (int i = 1; i < expected; i++) {
      int next = in.read();
      if (next == -1) {
        return first;
      }
      bytes[i] = (byte) next;
    }
    String decoded = new String(bytes, StandardCharsets.UTF_8);
    return decoded.charAt(0);
  }
}
