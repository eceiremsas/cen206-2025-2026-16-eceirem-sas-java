/**
 * @file PrivacyConsent.java
 * @brief Interactive KVKK/GDPR-style consent gate.
 */
package com.eceirem.kidsplanner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.jline.terminal.Terminal;

/**
 * Minimal consent capture aligned with KVKK/GDPR data-minimization narrative for the planner.
 */
public final class PrivacyConsent {

  /** Local flag file path indicating the user accepted processing. */
  private static final String FLAG = "kidsplanner-consent.flag";

  /** Prevents instantiation. */
  private PrivacyConsent() {
  }

  /** @return {@code true} if the consent flag file exists */
  public static boolean hasConsentFlag() {
    return Files.exists(Paths.get(FLAG));
  }

  /** Creates the on-disk consent flag after successful approval. */
  public static void writeConsentFlag() {
    try {
      Path path = Paths.get(FLAG);
      Files.write(path, "consent-granted".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new RepositoryException("Could not write consent flag file.", e);
    }
  }

  /**
   * @param menuStdin same stream used for {@link ConsoleUi} menus (e.g. wrapped {@code System.in}); ignored in text mode
   * @param menuMode {@code true} when running with interactive menu UI
   */
  public static void ensureInteractiveConsent(InputStream menuStdin, boolean menuMode) throws IOException {
    ensureInteractiveConsent(null, menuStdin, menuMode);
  }

  /**
   * Shows consent UI once until flag file exists.
   *
   * @param terminal JLine terminal if available; may be {@code null}
   * @param menuStdin stdin shared with {@link ConsoleUi} in menu mode
   * @param menuMode {@code true} for full-screen menus
   * @throws IOException on I/O errors
   */
  public static void ensureInteractiveConsent(Terminal terminal, InputStream menuStdin, boolean menuMode) throws IOException {
    if (hasConsentFlag()) {
      return;
    }
    if (menuMode) {
      List<String> choices = Arrays.asList(
          "YES - I consent to processing minimal data for planning",
          "NO - exit the application");
      ConsoleUi.clearScreen(System.out);
      ConsoleUi.printBanner(System.out);
      ConsoleUi.muted(System.out,
          "This app stores only minimal child profile and activity data for planning.");
      ConsoleUi.muted(System.out, "KVKK/GDPR: purpose limitation, data minimization; no unnecessary fields.");
      ConsoleUi.muted(System.out, "Use arrows / Tab / Enter to choose; no numeric menu (course rule).");
      int idx = ConsoleUi.readMenuChoice(terminal, menuStdin, System.out, "Privacy / personal data", choices,
          "Your choice constitutes legal consent.");
      if (idx != 0) {
        System.out.println("Consent not given. Closing application.");
        throw new RepositoryException("Consent not given.");
      }
      writeConsentFlag();
      return;
    }
    System.out.println("[Info] Text mode: privacy summary — planning data only; minimization applies.");
  }
}
