/**

@file Kidsplanner.java
@brief Kids Activity Planner console application (menu + text mode).
@details Coordinates CRUD for kids and activities, overlap checks, storage switching, and privacy consent.
*/

/**

@package com.eceirem.kidsplanner
@brief The com.eceirem.kidsplanner package contains all the classes and files related to the Kidsplanner App.
*/
package com.eceirem.kidsplanner;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**

@class Kidsplanner
@brief Console controller for the Kids Activity Planner.
@details Uses arrow/Tab menus when a real console is attached; falls back to text commands for CI/tests.
*/
public class Kidsplanner {

  /**
   * Interactive menus when a real console is expected. {@code java -jar} from Windows {@code .bat}
   * often yields {@code System.console() == null} even though the user has a console; on Windows we
   * still enable menu mode unless {@value #PROP_TEXT_MODE} is {@code true} (set by Surefire for tests).
   */
  public static final String PROP_TEXT_MODE = "kidsplanner.text.mode";

  /** Non-null only in text (CI / piped) mode — do not use with raw {@link System#in} when menus run. */
  private final Scanner scanner;
  /** Shared with {@link ConsoleUi} in menu mode so {@link Scanner} buffering does not steal keys from menus. */
  private final PushbackInputStream menuStdin;
  private final boolean menuMode;
  private final Terminal terminal;
  private final LineReader lineReader;
  private IRepository<Kid> kidRepository;
  private IRepository<Activity> activityRepository;

  /**
   * @return {@code false} when {@link #PROP_TEXT_MODE} is {@code true}; else {@code true} if
   *         {@link System#console()} is non-null, or on Windows (jar/bat console without Console API).
   */
  static boolean detectMenuMode() {
    if ("true".equals(System.getProperty(PROP_TEXT_MODE))) {
      return false;
    }
    if (System.console() != null) {
      return true;
    }
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    return os.contains("win");
  }

  public Kidsplanner() {
    boolean mm = detectMenuMode();
    if (mm) {
      this.menuStdin = new PushbackInputStream(System.in, 64);
      this.scanner = null;
      Terminal t = null;
      try {
        t = TerminalBuilder.builder().system(true).jna(true).build();
      } catch (Exception e) {
        t = null;
      }
      this.terminal = t;
      this.lineReader = t == null ? null : LineReaderBuilder.builder().terminal(t).build();
      this.menuMode = true;
    } else {
      this.menuStdin = null;
      this.scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
      this.menuMode = false;
      this.terminal = null;
      this.lineReader = null;
    }
    reloadRepositories();
  }

  public Kidsplanner(Scanner scanner, boolean menuMode) {
    if (menuMode) {
      throw new IllegalArgumentException("Menu mode uses default constructor only (real console).");
    }
    if (scanner == null) {
      throw new IllegalArgumentException("scanner required in text mode");
    }
    this.scanner = scanner;
    this.menuStdin = null;
    this.menuMode = false;
    this.terminal = null;
    this.lineReader = null;
    reloadRepositories();
  }

  private String nextLine() throws IOException {
    if (menuMode) {
      if (lineReader != null) {
        // menu input uses raw mode temporarily; line input should be normal line editing
        return lineReader.readLine("");
      }
      return ConsoleUi.readLineUtf8(menuStdin);
    }
    return scanner.nextLine();
  }

  private void reloadRepositories() {
    this.kidRepository = RepositoryFactory.createKidRepository();
    this.activityRepository = RepositoryFactory.createActivityRepository();
  }

  public void run() {
    try {
      PrivacyConsent.ensureInteractiveConsent(terminal, menuMode ? menuStdin : System.in, menuMode);
      if (menuMode) {
        runMenuMode();
      } else {
        runTextMode();
      }
    } catch (RepositoryException | ValidationException e) {
      System.out.println("Hata: " + e.getMessage());
    } catch (IOException e) {
      System.out.println("Girdi hatasi: " + e.getMessage());
    }
    System.out.println("Uygulama kapatildi.");
  }

  private String menuStatusLine() {
    int nk = kidRepository.findAll().size();
    int na = activityRepository.findAll().size();
    StorageType st = RepositoryFactory.getActiveStorage();
    return "Depolama: " + st.name() + "  |  Cocuk: " + nk + "  |  Aktivite: " + na;
  }

  private void runMenuMode() throws IOException {
    List<String> main = Arrays.asList(
        "Cocuk yonetimi",
        "Aktivite yonetimi",
        "Depolama (Binary / SQLite / MySQL)",
        "Ders gereksinimleri (checklist)",
        "Cikis");
    while (true) {
      int choice = ConsoleUi.readMenuChoice(terminal, menuStdin, System.out, "Ana Menu", main, menuStatusLine());
      if (choice == 0) {
        runKidMenu();
      } else if (choice == 1) {
        runActivityMenu();
      } else if (choice == 2) {
        runStorageMenu();
      } else if (choice == 3) {
        showHelpMenu();
        System.out.println("Devam icin Enter...");
        nextLine();
      } else {
        break;
      }
    }
  }

  private void runKidMenu() throws IOException {
    List<String> items = Arrays.asList("Cocuk ekle", "Cocuklari listele", "Cocuk sil", "Geri");
    while (true) {
      int choice = ConsoleUi.readMenuChoice(terminal, menuStdin, System.out, "Cocuk Yonetimi", items, menuStatusLine());
      if (choice == 0) {
        addKidInteractive();
      } else if (choice == 1) {
        ConsoleUi.clearScreen(System.out);
        ConsoleUi.printBanner(System.out);
        ConsoleUi.title(System.out, "Kayitli cocuklar");
        listKidsPlain();
        System.out.println("Devam icin Enter...");
        nextLine();
      } else if (choice == 2) {
        deleteKidInteractive();
      } else {
        break;
      }
    }
  }

  private void runActivityMenu() throws IOException {
    List<String> items = Arrays.asList(
        "Aktivite ekle",
        "Aktiviteleri listele",
        "Aktivite guncelle",
        "Aktivite sil",
        "Bugunun programi (sirali)",
        "Geri");
    while (true) {
      int choice = ConsoleUi.readMenuChoice(terminal, menuStdin, System.out, "Aktivite Yonetimi", items, menuStatusLine());
      if (choice == 0) {
        addActivityInteractiveMenu();
      } else if (choice == 1) {
        ConsoleUi.clearScreen(System.out);
        ConsoleUi.printBanner(System.out);
        ConsoleUi.title(System.out, "Tum aktiviteler");
        listActivitiesPlain();
        System.out.println("Devam icin Enter...");
        nextLine();
      } else if (choice == 2) {
        updateActivityInteractive();
      } else if (choice == 3) {
        deleteActivityInteractive();
      } else if (choice == 4) {
        listTodayPlan();
        System.out.println("Devam icin Enter...");
        nextLine();
      } else {
        break;
      }
    }
  }

  private void runStorageMenu() throws IOException {
    List<String> items = Arrays.asList("Binary dosya", "SQLite", "MySQL", "Geri");
    while (true) {
      String info = "Binary: .bin dosyasi | SQLite: kidsplanner.db | MySQL: docker ile veritabani";
      int choice = ConsoleUi.readMenuChoice(terminal, menuStdin, System.out, "Depolama Motoru", items, info);
      if (choice == 0) {
        switchStorageWithUi(StorageType.BINARY);
      } else if (choice == 1) {
        switchStorageWithUi(StorageType.SQLITE);
      } else if (choice == 2) {
        switchStorageWithUi(StorageType.MYSQL);
      } else {
        break;
      }
    }
  }

  private void switchStorageWithUi(StorageType target) throws IOException {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.printBanner(System.out);
    ConsoleUi.title(System.out, "Depolama degistir");
    try {
      RepositoryFactory.setActiveStorage(target);
      reloadRepositories(); // may create schema
      ConsoleUi.successLine(System.out, "Aktif depolama: " + target.name());
      System.out.println();
      explainStorage(target);
      System.out.println();
      System.out.println(RepositoryFactory.describeJdbcTargets());
      System.out.println("Uyari: Backend degisince veriler otomatik tasinmaz (her depo ayri).");
    } catch (RepositoryException e) {
      RepositoryFactory.setActiveStorage(StorageType.BINARY);
      reloadRepositories();
      System.out.println("Hata: " + e.getMessage());
      System.out.println();
      System.out.println("Cozum onergisi:");
      if (target == StorageType.MYSQL) {
        System.out.println("- docker compose ile MySQL'i calistir (proje kokundeki docker-compose.yml).");
        System.out.println("- Sonra tekrar Depolama > MySQL sec.");
      } else if (target == StorageType.SQLITE) {
        System.out.println("- kidsplanner.db ayni klasorde olusur; yazma izni var mi kontrol et.");
      } else {
        System.out.println("- Binary icin proje klasorunde .bin dosyalarina yazma izni gerekir.");
      }
      System.out.println();
      System.out.println("Not: Guvenli mod icin BINARY'ye geri donuldu.");
    }
    System.out.println();
    System.out.println("Devam icin Enter...");
    nextLine();
  }

  private void explainStorage(StorageType storage) {
    if (storage == StorageType.BINARY) {
      System.out.println("Binary (dosya): kidsplanner-kids.bin ve kidsplanner-activities.bin dosyalarina kaydeder.");
      System.out.println("Avantaj: kurulum yok. Dezavantaj: dosya tabanli oldugu icin sorgu esnek degil.");
    } else if (storage == StorageType.SQLITE) {
      System.out.println("SQLite (tek dosya DB): kidsplanner.db icine kid/activity tablolarini olusturur ve kaydeder.");
      System.out.println("Avantaj: kurulum yok, SQL var. Dezavantaj: buyuk/cok kullanici icin sinirli.");
    } else if (storage == StorageType.MYSQL) {
      System.out.println("MySQL (sunucu DB): Docker ile calisan MySQL'e baglanir ve tablolari olusturur.");
      System.out.println("Avantaj: gercek sunucu DB. Dezavantaj: docker/mysql calismali (port 3306).");
    }
  }

  private void addKidInteractive() throws IOException {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.printBanner(System.out);
    ConsoleUi.title(System.out, "Yeni Cocuk");
    System.out.print("Gorunen ad: ");
    String name = nextLine().trim();
    System.out.print("Dogum yili (bos birakilabilir): ");
    String yearText = nextLine().trim();
    System.out.print("Not (bos birakilabilir): ");
    String notes = nextLine();
    Integer year = null;
    if (!yearText.isEmpty()) {
      year = Integer.valueOf(Integer.parseInt(yearText));
    }
    Kid kid = new Kid(name, year, notes);
    if (kid.getDisplayName() == null || kid.getDisplayName().isEmpty()) {
      throw new ValidationException("Cocuk adi bos olamaz.");
    }
    kidRepository.save(kid);
    System.out.println("Cocuk kaydedildi. ID = " + kid.getId());
    System.out.println("Devam icin Enter...");
    nextLine();
  }

  private void deleteKidInteractive() throws IOException {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.printBanner(System.out);
    ConsoleUi.title(System.out, "Cocuk Sil");
    listKidsPlain();
    System.out.print("Silinecek cocuk ID: ");
    int id = Integer.parseInt(nextLine().trim());
    kidRepository.delete(id);
    System.out.println("Cocuk ve bagli aktiviteler silindi (varsa).");
    System.out.println("Devam icin Enter...");
    nextLine();
  }

  private void addActivityInteractiveMenu() throws IOException {
    int kidId = pickKidIdForMenu();
    if (kidId < 0) {
      return;
    }
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.printBanner(System.out);
    ConsoleUi.title(System.out, "Yeni Aktivite");
    Activity draft = readActivityBodyFromScanner(null, kidId);
    validateActivity(draft, -1);
    activityRepository.save(draft);
    System.out.println("Aktivite eklendi. ID = " + draft.getId());
    System.out.println("Devam icin Enter...");
    nextLine();
  }

  private int pickKidIdForMenu() throws IOException {
    List<Kid> kids = new ArrayList<Kid>(kidRepository.findAll());
    if (kids.isEmpty()) {
      Kid auto = new Kid("Genel Cocuk", null, "");
      kidRepository.save(auto);
      kids.add(auto);
    }
    Collections.sort(kids, new Comparator<Kid>() {
      @Override
      public int compare(Kid a, Kid b) {
        return Integer.compare(a.getId(), b.getId());
      }
    });
    List<String> labels = new ArrayList<String>();
    for (Kid kid : kids) {
      labels.add(kid.getId() + " - " + kid.getDisplayName());
    }
    labels.add("Geri (aktivite menusune don)");
    labels.add("+ Yeni cocuk");
    int idx = ConsoleUi.readMenuChoice(terminal, menuStdin, System.out, "Cocuk secin", labels, menuStatusLine());
    int idxGeri = labels.size() - 2;
    int idxYeni = labels.size() - 1;
    if (idx == idxYeni) {
      addKidInteractive();
      List<Kid> refreshed = kidRepository.findAll();
      Kid newest = Collections.max(refreshed, new Comparator<Kid>() {
        @Override
        public int compare(Kid a, Kid b) {
          return Integer.compare(a.getId(), b.getId());
        }
      });
      return newest.getId();
    }
    if (idx == idxGeri) {
      return -1;
    }
    return kids.get(idx).getId();
  }

  private void updateActivityInteractive() throws IOException {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.printBanner(System.out);
    ConsoleUi.title(System.out, "Aktivite Guncelle");
    listActivitiesPlain();
    System.out.print("Guncellenecek aktivite ID: ");
    int id = Integer.parseInt(nextLine().trim());
    Activity existing = activityRepository.findById(id);
    if (existing == null) {
      throw new ValidationException("ID bulunamadi: " + id);
    }
    Activity updated = readActivityFields(existing);
    updated.setId(id);
    validateActivity(updated, id);
    activityRepository.update(updated);
    System.out.println("Aktivite guncellendi.");
    System.out.println("Devam icin Enter...");
    nextLine();
  }

  private void deleteActivityInteractive() throws IOException {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.printBanner(System.out);
    ConsoleUi.title(System.out, "Aktivite Sil");
    listActivitiesPlain();
    System.out.print("Silinecek aktivite ID: ");
    int id = Integer.parseInt(nextLine().trim());
    activityRepository.delete(id);
    System.out.println("Aktivite silindi (varsa).");
    System.out.println("Devam icin Enter...");
    nextLine();
  }

  private Activity readActivityBodyFromScanner(Activity base, int kidId) throws IOException {
    String title = ask("Baslik", base == null ? "" : base.getTitle());
    if (title == null || title.trim().isEmpty()) {
      throw new ValidationException("Baslik bos olamaz.");
    }
    String dateText = ask("Tarih (yyyy-MM-dd)", base == null ? "" : base.getDate().toString());
    String startText = ask("Baslangic (HH:mm)", base == null ? "" : base.getStartTime().toString());
    String endText = ask("Bitis (HH:mm)", base == null ? "" : base.getEndTime().toString());
    String typeText = ask("Tip (STUDY/PLAY/SPORT/ART/SLEEP/SOCIAL)",
        base == null ? ActivityType.STUDY.name() : base.getType().name());
    String location = ask("Lokasyon", base == null ? "" : base.getLocation());
    String notes = ask("Not", base == null ? "" : base.getNotes());
    try {
      Activity activity = new Activity(
          title,
          LocalDate.parse(dateText),
          LocalTime.parse(startText),
          LocalTime.parse(endText),
          ActivityType.valueOf(typeText.toUpperCase(Locale.ROOT)),
          location,
          notes,
          Integer.valueOf(kidId));
      return activity;
    } catch (DateTimeParseException e) {
      throw new ValidationException("Tarih/saat formati gecersiz.");
    } catch (IllegalArgumentException e) {
      throw new ValidationException("Aktivite tipi gecersiz.");
    }
  }

  private void runTextMode() {
    System.out.println("=== Kids Activity Planner (Metin modu) ===");
    boolean running = true;
    while (running) {
      printTextMenu();
      String raw;
      try {
        raw = nextLine();
      } catch (IOException e) {
        System.out.println("Girdi hatasi: " + e.getMessage());
        continue;
      }
      String cmd = raw.trim().toLowerCase(Locale.ROOT);
      try {
        if ("up".equals(cmd) || "tab".equals(cmd)) {
          System.out.println("Komut seciniz: open, add, update, delete, list, kid-add, kid-list, kid-delete, storage, exit");
          continue;
        }
        if (cmd.startsWith("storage")) {
          handleStorageCommand(cmd);
          continue;
        }
        if ("open".equals(cmd)) {
          showHelp();
        } else if ("kid-add".equals(cmd)) {
          addKidText();
        } else if ("kid-list".equals(cmd)) {
          listKidsPlain();
        } else if ("kid-delete".equals(cmd)) {
          deleteKidText();
        } else if ("add".equals(cmd)) {
          addActivity();
        } else if ("update".equals(cmd)) {
          updateActivity();
        } else if ("delete".equals(cmd)) {
          deleteActivity();
        } else if ("list".equals(cmd)) {
          listActivities();
        } else if ("exit".equals(cmd)) {
          running = false;
        } else if (cmd.isEmpty()) {
          System.out.println("Gecersiz komut. open/add/update/delete/list/kid-add/kid-list/kid-delete/storage/exit");
        } else {
          System.out.println("Gecersiz komut. open/add/update/delete/list/kid-add/kid-list/kid-delete/storage/exit");
        }
      } catch (RepositoryException | ValidationException e) {
        System.out.println("Hata: " + e.getMessage());
      } catch (NumberFormatException e) {
        System.out.println("Hata: Sayi formati gecersiz.");
      } catch (IOException e) {
        System.out.println("Girdi hatasi: " + e.getMessage());
      }
    }
  }

  private void handleStorageCommand(String cmd) {
    String[] parts = cmd.split("\\s+");
    if (parts.length < 2) {
      System.out.println("Kullanim: storage binary|sqlite|mysql");
      return;
    }
    StorageType parsed = RepositoryFactory.parseStorageTypeOrNull(parts[1]);
    if (parsed == null) {
      System.out.println("Gecersiz depolama. binary, sqlite veya mysql yazin.");
      return;
    }
    RepositoryFactory.setActiveStorage(parsed);
    reloadRepositories();
    System.out.println("Depolama guncellendi: " + parsed);
    System.out.println(RepositoryFactory.describeJdbcTargets());
  }

  private void addKidText() throws IOException {
    System.out.print("Gorunen ad: ");
    String name = nextLine().trim();
    System.out.print("Dogum yili (bos): ");
    String yearText = nextLine().trim();
    System.out.print("Not (bos): ");
    String notes = nextLine();
    Integer year = null;
    if (!yearText.isEmpty()) {
      year = Integer.valueOf(Integer.parseInt(yearText));
    }
    Kid kid = new Kid(name, year, notes);
    if (kid.getDisplayName() == null || kid.getDisplayName().isEmpty()) {
      throw new ValidationException("Cocuk adi bos olamaz.");
    }
    kidRepository.save(kid);
    System.out.println("Cocuk eklendi. ID = " + kid.getId());
  }

  private void deleteKidText() throws IOException {
    System.out.print("Silinecek cocuk ID: ");
    int id = Integer.parseInt(nextLine().trim());
    kidRepository.delete(id);
    System.out.println("Cocuk silindi (varsa).");
  }

  private void printTextMenu() {
    System.out.println();
    System.out.println("Komutlar (number-entry yok):");
    System.out.println("- open      : Yardim menusu");
    System.out.println("- add       : Aktivite ekle");
    System.out.println("- update    : Aktivite guncelle");
    System.out.println("- delete    : Aktivite sil");
    System.out.println("- list      : Aktivite listele");
    System.out.println("- kid-add   : Cocuk ekle");
    System.out.println("- kid-list  : Cocuklari listele");
    System.out.println("- kid-delete: Cocuk sil");
    System.out.println("- storage X : Depolama (binary|sqlite|mysql)");
    System.out.println("- exit      : Cikis");
    System.out.print(">> ");
  }

  private void showHelp() {
    System.out.println("Tarih formati: yyyy-MM-dd");
    System.out.println("Saat formati : HH:mm");
    System.out.println("Tipler       : STUDY, PLAY, SPORT, ART, SLEEP, SOCIAL");
    System.out.println("Cakisma kurali: Ayni cocuk + ayni gun icinde saat araliklari kesismemeli.");
    System.out.println("MySQL icin: docker compose ile MySQL ayaga kaldirin (kok dizindeki docker-compose.yml).");
    System.out.println(RepositoryFactory.describeJdbcTargets());
  }

  private void showHelpMenu() {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.printBanner(System.out);
    ConsoleUi.title(System.out, "Ders gereksinimleri (checklist)");
    ConsoleUi.muted(System.out, "Bu sayfa yalnizca ders gereksinimlerini ve projede olanlari listeler.");
    ConsoleUi.rule(System.out);
    System.out.println("  [VAR] Klavye ile gezinilebilir menu (ok/Tab/Enter) - sayisal secim yok");
    System.out.println("  [VAR] CRUD (cocuk + aktivite) ekranlari");
    System.out.println("  [VAR] Veri dogrulama/kurallar (saat cakismasi engeli)");
    System.out.println("  [VAR] Kalici veri: Binary + SQLite + MySQL (calisirken secilebilir)");
    System.out.println("  [VAR] MySQL icin Docker compose destegi (docker-compose.yml)");
    System.out.println("  [VAR] Unit testler: JUnit 5 (metin modu ile otomatik)");
    System.out.println("  [VAR] Code coverage: JaCoCo raporu (target/site/jacoco)");
    ConsoleUi.rule(System.out);
    System.out.println("  Format ozeti");
    System.out.println("  - Tarih : yyyy-MM-dd");
    System.out.println("  - Saat  : HH:mm");
    System.out.println("  - Tipler: STUDY, PLAY, SPORT, ART, SLEEP, SOCIAL");
    System.out.println();
    System.out.println("  Not: Depolama > MySQL secince 'sema olusturulamadi' gorursen, once docker ile MySQL'i baslat.");
  }

  private void addActivity() throws IOException {
    Activity activity = readActivityFields(null);
    validateActivity(activity, -1);
    activityRepository.save(activity);
    System.out.println("Aktivite eklendi. ID = " + activity.getId());
  }

  private void updateActivity() throws IOException {
    System.out.print("Guncellenecek ID: ");
    int id = Integer.parseInt(nextLine().trim());
    Activity existing = activityRepository.findById(id);
    if (existing == null) {
      throw new ValidationException("ID bulunamadi: " + id);
    }
    Activity updated = readActivityFields(existing);
    updated.setId(id);
    validateActivity(updated, id);
    activityRepository.update(updated);
    System.out.println("Aktivite guncellendi.");
  }

  private void deleteActivity() throws IOException {
    System.out.print("Silinecek ID: ");
    int id = Integer.parseInt(nextLine().trim());
    activityRepository.delete(id);
    System.out.println("Aktivite silindi (varsa).");
  }

  private void listActivities() {
    List<Activity> activities = new ArrayList<Activity>(activityRepository.findAll());
    sortActivities(activities);
    if (menuMode) {
      ConsoleUi.printActivityTable(System.out, activities);
    } else {
      System.out.println("--- Aktivite Listesi ---");
      for (Activity activity : activities) {
        System.out.println(activity);
      }
      if (activities.isEmpty()) {
        System.out.println("(kayit yok)");
      }
    }
  }

  private void listActivitiesPlain() {
    listActivities();
  }

  private void listKidsPlain() {
    List<Kid> kids = new ArrayList<Kid>(kidRepository.findAll());
    Collections.sort(kids, new Comparator<Kid>() {
      @Override
      public int compare(Kid a, Kid b) {
        return Integer.compare(a.getId(), b.getId());
      }
    });
    if (menuMode) {
      ConsoleUi.printKidTable(System.out, kids);
    } else {
      System.out.println("--- Cocuk Listesi ---");
      for (Kid kid : kids) {
        System.out.println(kid);
      }
      if (kids.isEmpty()) {
        System.out.println("(cocuk yok)");
      }
    }
  }

  private void listTodayPlan() {
    LocalDate today = LocalDate.now();
    List<Activity> activities = new ArrayList<Activity>();
    for (Activity activity : activityRepository.findAll()) {
      if (today.equals(activity.getDate())) {
        activities.add(activity);
      }
    }
    sortActivities(activities);
    if (menuMode) {
      ConsoleUi.clearScreen(System.out);
      ConsoleUi.printBanner(System.out);
      ConsoleUi.title(System.out, "Bugunun programi - " + today);
      if (activities.isEmpty()) {
        ConsoleUi.muted(System.out, "(bugun icin kayit yok)");
      } else {
        ConsoleUi.printActivityTable(System.out, activities);
      }
    } else {
      System.out.println("--- Bugunun Programi (" + today + ") ---");
      for (Activity activity : activities) {
        System.out.println(activity);
      }
      if (activities.isEmpty()) {
        System.out.println("(bugun icin kayit yok)");
      }
    }
  }

  private void sortActivities(List<Activity> activities) {
    Collections.sort(activities, new Comparator<Activity>() {
      @Override
      public int compare(Activity a1, Activity a2) {
        int dateCompare = a1.getDate().compareTo(a2.getDate());
        if (dateCompare != 0) {
          return dateCompare;
        }
        int startCompare = a1.getStartTime().compareTo(a2.getStartTime());
        if (startCompare != 0) {
          return startCompare;
        }
        return Integer.compare(a1.getId(), a2.getId());
      }
    });
  }

  private Activity readActivityFields(Activity base) throws IOException {
    String kidDefault = "";
    if (base != null && base.getKidId() != null) {
      kidDefault = base.getKidId().toString();
    }
    String kidLine = ask("Cocuk ID (bos = otomatik)", kidDefault);
    int kidId = resolveKidIdFromLine(kidLine);
    String title = ask("Baslik", base == null ? "" : base.getTitle());
    if (title == null || title.trim().isEmpty()) {
      throw new ValidationException("Baslik bos olamaz.");
    }
    String dateText = ask("Tarih (yyyy-MM-dd)", base == null ? "" : base.getDate().toString());
    String startText = ask("Baslangic (HH:mm)", base == null ? "" : base.getStartTime().toString());
    String endText = ask("Bitis (HH:mm)", base == null ? "" : base.getEndTime().toString());
    String typeText = ask("Tip (STUDY/PLAY/SPORT/ART/SLEEP/SOCIAL)",
        base == null ? ActivityType.STUDY.name() : base.getType().name());
    String location = ask("Lokasyon", base == null ? "" : base.getLocation());
    String notes = ask("Not", base == null ? "" : base.getNotes());
    try {
      return new Activity(
          title,
          LocalDate.parse(dateText),
          LocalTime.parse(startText),
          LocalTime.parse(endText),
          ActivityType.valueOf(typeText.toUpperCase(Locale.ROOT)),
          location,
          notes,
          Integer.valueOf(kidId));
    } catch (DateTimeParseException e) {
      throw new ValidationException("Tarih/saat formati gecersiz.");
    } catch (IllegalArgumentException e) {
      throw new ValidationException("Aktivite tipi gecersiz.");
    }
  }

  private int resolveKidIdFromLine(String line) {
    String trimmed = line == null ? "" : line.trim();
    if (trimmed.isEmpty()) {
      List<Kid> kids = kidRepository.findAll();
      if (kids.isEmpty()) {
        Kid created = new Kid("Genel Cocuk", null, "");
        kidRepository.save(created);
        return created.getId().intValue();
      }
      List<Kid> sorted = new ArrayList<Kid>(kids);
      Collections.sort(sorted, new Comparator<Kid>() {
        @Override
        public int compare(Kid a, Kid b) {
          return Integer.compare(a.getId(), b.getId());
        }
      });
      return sorted.get(0).getId().intValue();
    }
    int id = Integer.parseInt(trimmed);
    if (kidRepository.findById(id) == null) {
      throw new ValidationException("Cocuk bulunamadi: " + id);
    }
    return id;
  }

  private String ask(String label, String defaultValue) throws IOException {
    String def = defaultValue == null ? "" : defaultValue;
    System.out.print(label + (def.isEmpty() ? "" : " [" + def + "]") + ": ");
    String value = nextLine().trim();
    return value.isEmpty() ? def : value;
  }

  private void validateActivity(Activity candidate, int currentId) {
    if (candidate.getTitle() == null || candidate.getTitle().trim().isEmpty()) {
      throw new ValidationException("Baslik bos olamaz.");
    }
    if (candidate.getKidId() == null) {
      throw new ValidationException("Cocuk secilmeli.");
    }
    if (!candidate.getStartTime().isBefore(candidate.getEndTime())) {
      throw new ValidationException("Baslangic saati, bitis saatinden once olmali.");
    }
    List<Activity> all = activityRepository.findAll();
    for (Activity activity : all) {
      if (activity.getId() != null && activity.getId() == currentId) {
        continue;
      }
      if (!Objects.equals(activity.getKidId(), candidate.getKidId())) {
        continue;
      }
      if (!activity.getDate().equals(candidate.getDate())) {
        continue;
      }
      if (overlaps(activity.getStartTime().toSecondOfDay(), activity.getEndTime().toSecondOfDay(),
          candidate.getStartTime().toSecondOfDay(), candidate.getEndTime().toSecondOfDay())) {
        throw new ValidationException("Ayni gunde saat cakismasi var. Cakisan ID: " + activity.getId());
      }
    }
  }

  private boolean overlaps(int startA, int endA, int startB, int endB) {
    return startA < endB && startB < endA;
  }
}
