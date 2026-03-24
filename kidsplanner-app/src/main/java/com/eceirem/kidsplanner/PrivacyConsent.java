package com.eceirem.kidsplanner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

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

  /**
   * @param menuStdin same stream used for {@link ConsoleUi} menus (e.g. wrapped {@link System#in}); ignored in text mode
   */
  public static void ensureInteractiveConsent(InputStream menuStdin, boolean menuMode) throws IOException {
    if (hasConsentFlag()) {
      return;
    }
    if (menuMode) {
      List<String> choices = Arrays.asList(
          "EVET - planlama icin asgari veriyi islemeye onay veriyorum",
          "Hayir - uygulamayi kapat");
      ConsoleUi.clearScreen(System.out);
      ConsoleUi.printBanner(System.out);
      ConsoleUi.muted(System.out,
          "Bu uygulama yalnizca planlama icin asgari cocuk profili ve aktivite bilgisi tutar.");
      ConsoleUi.muted(System.out, "KVKK/GDPR: amac sinirlama, veri minimizasyonu, gereksiz alan tutulmaz.");
      ConsoleUi.muted(System.out, "Ok / Tab / Enter ile secin; numara ile menu yok (ders kurali).");
      int idx = ConsoleUi.readMenuChoice(menuStdin, System.out, "Gizlilik / Kisisel Veri", choices,
          "Secim hukuki onay niteligindedir.");
      if (idx != 0) {
        System.out.println("Onay verilmedi. Uygulama kapaniyor.");
        System.exit(0);
      }
      writeConsentFlag();
      return;
    }
    System.out.println("[Bilgi] Metin modu: gizlilik ozeti - yalnizca planlama verisi, minimizasyon uygulanir.");
  }
}
