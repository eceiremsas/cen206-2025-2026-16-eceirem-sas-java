package com.eceirem.kidsplanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Minimal consent capture aligned with KVKK/GDPR data-minimization narrative for the planner.
 */
public final class PrivacyConsent {

  private static final String FLAG = "kidsplanner-consent.flag";

  private PrivacyConsent() {
  }

  public static boolean hasConsentFlag() {
    return Files.exists(Paths.get(FLAG));
  }

  public static void writeConsentFlag() {
    try {
      Path path = Paths.get(FLAG);
      Files.write(path, "consent-granted".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new RepositoryException("Onay dosyasi yazilamadi.", e);
    }
  }

  public static void ensureInteractiveConsent(Scanner scanner, boolean menuMode) {
    if (hasConsentFlag()) {
      return;
    }
    if (menuMode) {
      ConsoleUi.clearScreen(System.out);
      ConsoleUi.printBanner(System.out);
      ConsoleUi.title(System.out, "Gizlilik / Kisisel Veri");
      System.out.println("Bu uygulama yalnizca planlama icin asgari cocuk profili ve aktivite bilgisi tutar.");
      System.out.println("KVKK kapsaminda amac sinirlama ve veri minimizasyonu uygulanir.");
      System.out.println();
      System.out.print("Devam etmek icin EVET yazin: ");
      String line = scanner.nextLine().trim();
      if (!"EVET".equalsIgnoreCase(line)) {
        System.out.println("Onay verilmedi. Uygulama kapaniyor.");
        System.exit(0);
      }
      writeConsentFlag();
      return;
    }
    System.out.println("[Bilgi] Metin modu: gizlilik ozeti - yalnizca planlama verisi, minimizasyon uygulanir.");
  }
}
