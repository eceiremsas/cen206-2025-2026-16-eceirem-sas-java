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

/**
 * Rich console styling (ANSI) and keyboard navigation (arrow keys / Tab / Home / End) for menus.
 * Menus are navigated with the keyboard only — no number-key selection (course guideline).
 */
public final class ConsoleUi {

  public static final String RESET = "\u001B[0m";
  public static final String BOLD = "\u001B[1m";
  public static final String DIM = "\u001B[2m";
  public static final String FG_CYAN = "\u001B[36m";
  public static final String FG_GREEN = "\u001B[32m";
  public static final String FG_YELLOW = "\u001B[33m";
  public static final String FG_MAGENTA = "\u001B[35m";
  public static final String FG_WHITE = "\u001B[97m";
  public static final String FG_BLUE = "\u001B[34m";
  public static final String FG_RED = "\u001B[31m";
  public static final String BG_BLUE = "\u001B[44m";
  private static final String HLINE = "\u2500";
  private static final String VLINE = "\u2502";
  private static final String TLC = "\u250c";
  private static final String TRC = "\u2510";
  private static final String BLC = "\u2514";
  private static final String BRC = "\u2518";
  private static final String TEE_L = "\u251c";
  private static final String TEE_R = "\u2524";
  private static final String CROSS = "\u253c";
  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

  private ConsoleUi() {
  }

  public static void clearScreen(PrintStream out) {
    out.print("\u001B[2J\u001B[H");
    out.flush();
  }

  public static void printBanner(PrintStream out) {
    out.println();
    out.println(DIM + "  ┌────────────────────────────────────────────────────────────┐" + RESET);
    out.println(FG_CYAN + BOLD + "  │" + RESET + "   " + FG_WHITE + BOLD + "KIDS  PLANNER" + RESET + "  "
        + FG_MAGENTA + "\u2022" + RESET + "  " + DIM + "CEN206 \u00b7 Konsol Midterm" + "          " + FG_CYAN + BOLD + "│" + RESET);
    out.println(FG_GREEN + BOLD + "  │" + RESET + "   " + DIM + "Cocuk ve aktivite yonetimi \u00b7 Binary \u00b7 SQLite \u00b7 MySQL" + "     " + FG_GREEN + BOLD + "│" + RESET);
    out.println(DIM + "  └────────────────────────────────────────────────────────────┘" + RESET);
    out.println();
    if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
      out.println(DIM + "  (Windows: renk ve UTF-8 icin Windows Terminal + chcp 65001 onerilir.)" + RESET);
      out.println();
    }
  }

  public static void rule(PrintStream out) {
    out.println(DIM + "  " + repeat(HLINE, 58) + RESET);
  }

  public static void title(PrintStream out, String text) {
    out.println();
    out.println("  " + FG_WHITE + BG_BLUE + BOLD + " " + text + " " + RESET);
    rule(out);
  }

  public static void muted(PrintStream out, String text) {
    out.println(DIM + "  " + text + RESET);
  }

  public static void successLine(PrintStream out, String text) {
    out.println("  " + FG_GREEN + text + RESET);
  }

  /**
   * Reads one line of UTF-8 from stdin without using {@link java.util.Scanner}, so it does not
   * compete with {@link #readMenuChoice} on the same stream. Use the same {@link PushbackInputStream}
   * instance that wraps {@link System#in} in menu mode.
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
   * Reads a menu choice with optional status line under the menu box.
   *
   * @param statusLine may be null
   * @return selected index
   */
  /**
   * @param in should be a {@link PushbackInputStream} around {@link System#in} on Windows so
   *           {@code \\r\\n} line endings do not leave a stray {@code \\n} for the next read.
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
      out.println("  " + DIM + "Saat: " + RESET + FG_CYAN + clock + RESET);
      out.println();
      drawMenuBox(out, title, options, index);
      out.println();
      if (statusLine != null && !statusLine.isEmpty()) {
        out.println("  " + DIM + statusLine + RESET);
        out.println();
      }
      out.println("  " + DIM + "Ok asagi/yukari \u00b7 Tab: sonraki \u00b7 Home/End \u00b7 W/S: yedek \u00b7 Enter: sec" + RESET);
      out.println("  " + DIM + "Cikis: son satiri secin veya Q" + RESET);
      out.flush();

      int read = readUtf8Byte(in, buffer);
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
      if (read == 27) {
        int nav = readAnsiNavigation(in);
        if (nav == NAV_UP) {
          index = (index - 1 + options.size()) % options.size();
        } else if (nav == NAV_DOWN) {
          index = (index + 1) % options.size();
        } else if (nav == NAV_HOME) {
          index = 0;
        } else if (nav == NAV_END) {
          index = options.size() - 1;
        } else if (nav == NAV_PGUP) {
          index = Math.max(0, index - 3);
        } else if (nav == NAV_PGDN) {
          index = Math.min(options.size() - 1, index + 3);
        } else if (nav == NAV_QUIT) {
          return options.size() - 1;
        }
        continue;
      }
      if (read == 'q' || read == 'Q') {
        return options.size() - 1;
      }
    }
  }

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
      String plain = (i == selectedIndex) ? ("\u25b8 " + label) : ("  " + label);
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

  private static String repeat(String ch, int n) {
    StringBuilder sb = new StringBuilder(n * ch.length());
    for (int i = 0; i < n; i++) {
      sb.append(ch);
    }
    return sb.toString();
  }

  public static void printKidTable(PrintStream out, List<Kid> kids) {
    if (kids.isEmpty()) {
      muted(out, "(cocuk kaydi yok)");
      return;
    }
    String[] headers = {"ID", "Ad", "D.Yili", "Not"};
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

  public static void printActivityTable(PrintStream out, List<Activity> activities) {
    if (activities.isEmpty()) {
      muted(out, "(aktivite yok)");
      return;
    }
    int[] w = {4, 4, 12, 11, 7, 18};
    printTableTop(out, w);
    printTableRow(out, w, new String[] {"ID", "CID", "Tarih", "Saat", "Tip", "Baslik"}, true);
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

  private static void printTableRow(PrintStream out, int[] widths, String[] cells) {
    printTableRow(out, widths, cells, false);
  }

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

  private static String truncate(String s, int max) {
    if (s == null) {
      return "";
    }
    if (s.length() <= max) {
      return s;
    }
    return s.substring(0, Math.max(0, max - 1)) + "\u2026";
  }

  private static final int NAV_NONE = 0;
  private static final int NAV_UP = 1;
  private static final int NAV_DOWN = 2;
  private static final int NAV_HOME = 3;
  private static final int NAV_END = 4;
  private static final int NAV_PGUP = 5;
  private static final int NAV_PGDN = 6;
  private static final int NAV_QUIT = 7;

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
    if (second != '[') {
      return NAV_NONE;
    }
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
    if (decoded.isEmpty()) {
      return first;
    }
    return decoded.charAt(0);
  }
}
