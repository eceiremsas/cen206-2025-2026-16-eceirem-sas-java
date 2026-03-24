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

/**

@class Kidsplanner
@brief Console controller for the Kids Activity Planner.
@details Uses arrow/Tab menus when a real console is attached; falls back to text commands for CI/tests.
*/
public class Kidsplanner {

  private final Scanner scanner;
  private final boolean menuMode;
  private IRepository<Kid> kidRepository;
  private IRepository<Activity> activityRepository;

  public Kidsplanner() {
    this(new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8.name()), System.console() != null);
  }

  public Kidsplanner(Scanner scanner, boolean menuMode) {
    this.scanner = scanner;
    this.menuMode = menuMode;
    reloadRepositories();
  }

  private void reloadRepositories() {
    this.kidRepository = RepositoryFactory.createKidRepository();
    this.activityRepository = RepositoryFactory.createActivityRepository();
  }

  public void run() {
    try {
      PrivacyConsent.ensureInteractiveConsent(scanner, menuMode);
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

  private void runMenuMode() throws IOException {
    List<String> main = Arrays.asList(
        "Cocuk yonetimi",
        "Aktivite yonetimi",
        "Depolama (Binary / SQLite / MySQL)",
        "Yardim",
        "Cikis");
    while (true) {
      int choice = ConsoleUi.readMenuChoice(System.in, System.out, "Ana Menu", main);
      if (choice == 0) {
        runKidMenu();
      } else if (choice == 1) {
        runActivityMenu();
      } else if (choice == 2) {
        runStorageMenu();
      } else if (choice == 3) {
        showHelp();
        System.out.println("Devam icin Enter...");
        scanner.nextLine();
      } else {
        break;
      }
    }
  }

  private void runKidMenu() throws IOException {
    List<String> items = Arrays.asList("Cocuk ekle", "Cocuklari listele", "Cocuk sil", "Geri");
    while (true) {
      int choice = ConsoleUi.readMenuChoice(System.in, System.out, "Cocuk Yonetimi", items);
      if (choice == 0) {
        addKidInteractive();
      } else if (choice == 1) {
        listKidsPlain();
        System.out.println("Devam icin Enter...");
        scanner.nextLine();
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
      int choice = ConsoleUi.readMenuChoice(System.in, System.out, "Aktivite Yonetimi", items);
      if (choice == 0) {
        addActivityInteractiveMenu();
      } else if (choice == 1) {
        listActivitiesPlain();
        System.out.println("Devam icin Enter...");
        scanner.nextLine();
      } else if (choice == 2) {
        updateActivityInteractive();
      } else if (choice == 3) {
        deleteActivityInteractive();
      } else if (choice == 4) {
        listTodayPlan();
        System.out.println("Devam icin Enter...");
        scanner.nextLine();
      } else {
        break;
      }
    }
  }

  private void runStorageMenu() throws IOException {
    List<String> items = Arrays.asList("Binary dosya", "SQLite", "MySQL", "Geri");
    while (true) {
      int choice = ConsoleUi.readMenuChoice(System.in, System.out, "Depolama Motoru", items);
      if (choice == 0) {
        RepositoryFactory.setActiveStorage(StorageType.BINARY);
        reloadRepositories();
        System.out.println("Aktif depolama: BINARY");
      } else if (choice == 1) {
        RepositoryFactory.setActiveStorage(StorageType.SQLITE);
        reloadRepositories();
        System.out.println("Aktif depolama: SQLITE");
      } else if (choice == 2) {
        RepositoryFactory.setActiveStorage(StorageType.MYSQL);
        reloadRepositories();
        System.out.println("Aktif depolama: MYSQL");
      } else {
        break;
      }
      System.out.println(RepositoryFactory.describeJdbcTargets());
      System.out.println("Uyari: Backend degisince veriler otomatik tasınmaz; yeni depoda bos veya mevcut kayitlar.");
      System.out.println("Devam icin Enter...");
      scanner.nextLine();
    }
  }

  private void addKidInteractive() {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.title(System.out, "Yeni Cocuk");
    System.out.print("Gorunen ad: ");
    String name = scanner.nextLine().trim();
    System.out.print("Dogum yili (bos birakilabilir): ");
    String yearText = scanner.nextLine().trim();
    System.out.print("Not (bos birakilabilir): ");
    String notes = scanner.nextLine();
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
    scanner.nextLine();
  }

  private void deleteKidInteractive() {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.title(System.out, "Cocuk Sil");
    listKidsPlain();
    System.out.print("Silinecek cocuk ID: ");
    int id = Integer.parseInt(scanner.nextLine().trim());
    kidRepository.delete(id);
    System.out.println("Cocuk ve bagli aktiviteler silindi (varsa).");
    System.out.println("Devam icin Enter...");
    scanner.nextLine();
  }

  private void addActivityInteractiveMenu() throws IOException {
    int kidId = pickKidIdForMenu();
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.title(System.out, "Yeni Aktivite");
    Activity draft = readActivityBodyFromScanner(null, kidId);
    validateActivity(draft, -1);
    activityRepository.save(draft);
    System.out.println("Aktivite eklendi. ID = " + draft.getId());
    System.out.println("Devam icin Enter...");
    scanner.nextLine();
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
    labels.add("+ Yeni cocuk");
    int idx = ConsoleUi.readMenuChoice(System.in, System.out, "Cocuk secin", labels);
    if (idx == labels.size() - 1) {
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
    return kids.get(idx).getId();
  }

  private void updateActivityInteractive() {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.title(System.out, "Aktivite Guncelle");
    listActivitiesPlain();
    System.out.print("Guncellenecek aktivite ID: ");
    int id = Integer.parseInt(scanner.nextLine().trim());
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
    scanner.nextLine();
  }

  private void deleteActivityInteractive() {
    ConsoleUi.clearScreen(System.out);
    ConsoleUi.title(System.out, "Aktivite Sil");
    listActivitiesPlain();
    System.out.print("Silinecek aktivite ID: ");
    int id = Integer.parseInt(scanner.nextLine().trim());
    activityRepository.delete(id);
    System.out.println("Aktivite silindi (varsa).");
    System.out.println("Devam icin Enter...");
    scanner.nextLine();
  }

  private Activity readActivityBodyFromScanner(Activity base, int kidId) {
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
      String raw = scanner.nextLine();
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

  private void addKidText() {
    System.out.print("Gorunen ad: ");
    String name = scanner.nextLine().trim();
    System.out.print("Dogum yili (bos): ");
    String yearText = scanner.nextLine().trim();
    System.out.print("Not (bos): ");
    String notes = scanner.nextLine();
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

  private void deleteKidText() {
    System.out.print("Silinecek cocuk ID: ");
    int id = Integer.parseInt(scanner.nextLine().trim());
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

  private void addActivity() {
    Activity activity = readActivityFields(null);
    validateActivity(activity, -1);
    activityRepository.save(activity);
    System.out.println("Aktivite eklendi. ID = " + activity.getId());
  }

  private void updateActivity() {
    System.out.print("Guncellenecek ID: ");
    int id = Integer.parseInt(scanner.nextLine().trim());
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

  private void deleteActivity() {
    System.out.print("Silinecek ID: ");
    int id = Integer.parseInt(scanner.nextLine().trim());
    activityRepository.delete(id);
    System.out.println("Aktivite silindi (varsa).");
  }

  private void listActivities() {
    List<Activity> activities = new ArrayList<Activity>(activityRepository.findAll());
    sortActivities(activities);
    System.out.println("--- Aktivite Listesi ---");
    for (Activity activity : activities) {
      System.out.println(activity);
    }
    if (activities.isEmpty()) {
      System.out.println("(kayit yok)");
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
    System.out.println("--- Cocuk Listesi ---");
    for (Kid kid : kids) {
      System.out.println(kid);
    }
    if (kids.isEmpty()) {
      System.out.println("(cocuk yok)");
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
    System.out.println("--- Bugunun Programi (" + today + ") ---");
    for (Activity activity : activities) {
      System.out.println(activity);
    }
    if (activities.isEmpty()) {
      System.out.println("(bugun icin kayit yok)");
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

  private Activity readActivityFields(Activity base) {
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

  private String ask(String label, String defaultValue) {
    String def = defaultValue == null ? "" : defaultValue;
    System.out.print(label + (def.isEmpty() ? "" : " [" + def + "]") + ": ");
    String value = scanner.nextLine().trim();
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
