package com.eceirem.kidsplanner;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Rich console styling (ANSI) and keyboard navigation (arrow keys / Tab) for menus.
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
  public static final String BG_BLUE = "\u001B[44m";

  private ConsoleUi() {
  }

  public static void clearScreen(PrintStream out) {
    out.print("\u001B[2J\u001B[H");
    out.flush();
  }

  public static void printBanner(PrintStream out) {
    out.println(FG_CYAN + BOLD
        + " __ __ _  _   __   __  _  _  __  __ _  __  " + RESET);
    out.println(FG_MAGENTA + BOLD
        + "|  V  | || | |  \\ /  \\| || |/  \\|  V  |/  \\ " + RESET);
    out.println(FG_GREEN + BOLD
        + "| \\_/ | >< | | o ) o || || | o | \\_/ | o | " + RESET);
    out.println(FG_YELLOW + BOLD
        + "|_| |_|\\__/|__/ \\__/|_||_|\\__/|_| |_|\\__/ " + RESET);
    out.println(DIM + "      Kids Activity Planner - Midterm Console" + RESET);
    out.println();
  }

  public static void rule(PrintStream out) {
    out.println(DIM + "------------------------------------------------------------" + RESET);
  }

  public static void title(PrintStream out, String text) {
    out.println();
    out.println(FG_WHITE + BG_BLUE + BOLD + " " + text + " " + RESET);
    rule(out);
  }

  /**
   * Renders a menu navigable with Up/Down arrows or Tab; Enter confirms.
   *
   * @return selected index
   */
  public static int readMenuChoice(InputStream in, PrintStream out, String title, List<String> options) throws IOException {
    if (options.isEmpty()) {
      return -1;
    }
    int index = 0;
    byte[] buffer = new byte[1];
    while (true) {
      clearScreen(out);
      printBanner(out);
      ConsoleUi.title(out, title);
      for (int i = 0; i < options.size(); i++) {
        if (i == index) {
          out.println(FG_GREEN + BOLD + "  > " + options.get(i) + RESET);
        } else {
          out.println("    " + DIM + options.get(i) + RESET);
        }
      }
      out.println();
      out.println(DIM + "Ok / Asagi / Tab: gezin | Enter: sec | ESC sonra Q: cikis" + RESET);
      out.flush();

      int read = readUtf8Byte(in, buffer);
      if (read == -1) {
        return options.size() - 1;
      }
      if (read == '\n' || read == '\r') {
        return index;
      }
      if (read == '\t') {
        index = (index + 1) % options.size();
        continue;
      }
      if (read == 27) {
        int next = in.read();
        if (next == '[') {
          int arrow = in.read();
          if (arrow == 'A') {
            index = (index - 1 + options.size()) % options.size();
          } else if (arrow == 'B') {
            index = (index + 1) % options.size();
          }
        } else if (next == 'q' || next == 'Q') {
          return options.size() - 1;
        }
        continue;
      }
      if (read == 'q' || read == 'Q') {
        return options.size() - 1;
      }
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
