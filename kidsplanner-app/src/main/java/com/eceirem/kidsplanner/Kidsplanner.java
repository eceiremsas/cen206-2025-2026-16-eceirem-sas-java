/**

@file Kidsplanner.java
@brief This file serves as a demonstration file for the Kidsplanner class.
@details This file contains the implementation of the Kidsplanner class, which provides various mathematical operations.
*/

/**

@package com.eceirem.kidsplanner
@brief The com.eceirem.kidsplanner package contains all the classes and files related to the Kidsplanner App.
*/
package com.eceirem.kidsplanner;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**

@class Kidsplanner
@brief This class represents a Kidsplanner that performs mathematical operations.
@details The Kidsplanner class provides methods to perform mathematical operations such as addition, subtraction, multiplication, and division. It also supports logging functionality using the logger object.
@author ugur.coruh
*/
public class Kidsplanner {

  private final Scanner scanner;
  private final IRepository<Activity> repository;

  public Kidsplanner() {
    this.scanner = new Scanner(System.in);
    this.repository = RepositoryFactory.createActivityRepository(StorageType.BINARY);
  }

  public void run() {
    System.out.println("=== Kids Activity Planner (Midterm Console) ===");
    boolean running = true;
    while (running) {
      printMenu();
      String cmd = scanner.nextLine().trim().toLowerCase();
      try {
        if ("up".equals(cmd) || "tab".equals(cmd)) {
          System.out.println("Komut seciniz: open, add, update, delete, list, exit");
          continue;
        }
        if ("open".equals(cmd)) {
          showHelp();
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
        } else {
          System.out.println("Gecersiz komut. open/add/update/delete/list/exit");
        }
      } catch (RepositoryException | ValidationException e) {
        System.out.println("Hata: " + e.getMessage());
      }
    }
    System.out.println("Uygulama kapatildi.");
  }

  private void printMenu() {
    System.out.println();
    System.out.println("Komutlar (number-entry yok):");
    System.out.println("- open  : Yardim menusu");
    System.out.println("- add   : Aktivite ekle");
    System.out.println("- update: Aktivite guncelle");
    System.out.println("- delete: Aktivite sil");
    System.out.println("- list  : Aktivite listele");
    System.out.println("- exit  : Cikis");
    System.out.print(">> ");
  }

  private void showHelp() {
    System.out.println("Tarih formati: yyyy-MM-dd");
    System.out.println("Saat formati : HH:mm");
    System.out.println("Tipler       : STUDY, PLAY, SPORT, ART, SLEEP, SOCIAL");
  }

  private void addActivity() {
    Activity activity = readActivityFields(null);
    validateActivity(activity, -1);
    repository.save(activity);
    System.out.println("Aktivite eklendi. ID = " + activity.getId());
  }

  private void updateActivity() {
    System.out.print("Guncellenecek ID: ");
    int id = Integer.parseInt(scanner.nextLine().trim());
    Activity existing = repository.findById(id);
    if (existing == null) {
      throw new ValidationException("ID bulunamadi: " + id);
    }
    Activity updated = readActivityFields(existing);
    updated.setId(id);
    validateActivity(updated, id);
    repository.update(updated);
    System.out.println("Aktivite guncellendi.");
  }

  private void deleteActivity() {
    System.out.print("Silinecek ID: ");
    int id = Integer.parseInt(scanner.nextLine().trim());
    repository.delete(id);
    System.out.println("Aktivite silindi (varsa).");
  }

  private void listActivities() {
    List<Activity> activities = new ArrayList<Activity>(repository.findAll());
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
    System.out.println("--- Aktivite Listesi ---");
    for (Activity activity : activities) {
      System.out.println(activity);
    }
    if (activities.isEmpty()) {
      System.out.println("(kayit yok)");
    }
  }

  private Activity readActivityFields(Activity base) {
    String title = ask("Baslik", base == null ? "" : base.getTitle());
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
          ActivityType.valueOf(typeText.toUpperCase()),
          location,
          notes);
    } catch (DateTimeParseException e) {
      throw new ValidationException("Tarih/saat formati gecersiz.");
    } catch (IllegalArgumentException e) {
      throw new ValidationException("Aktivite tipi gecersiz.");
    }
  }

  private String ask(String label, String defaultValue) {
    System.out.print(label + (defaultValue.isEmpty() ? "" : " [" + defaultValue + "]") + ": ");
    String value = scanner.nextLine().trim();
    return value.isEmpty() ? defaultValue : value;
  }

  private void validateActivity(Activity candidate, int currentId) {
    if (candidate.getTitle() == null || candidate.getTitle().trim().isEmpty()) {
      throw new ValidationException("Baslik bos olamaz.");
    }
    if (!candidate.getStartTime().isBefore(candidate.getEndTime())) {
      throw new ValidationException("Baslangic saati, bitis saatinden once olmali.");
    }
    List<Activity> all = repository.findAll();
    for (Activity activity : all) {
      if (activity.getId() == currentId) {
        continue;
      }
      if (activity.getDate().equals(candidate.getDate())
          && overlaps(activity.getStartTime().toSecondOfDay(), activity.getEndTime().toSecondOfDay(),
              candidate.getStartTime().toSecondOfDay(), candidate.getEndTime().toSecondOfDay())) {
        throw new ValidationException("Ayni gunde saat cakismasi var. Cakisan ID: " + activity.getId());
      }
    }
  }

  private boolean overlaps(int startA, int endA, int startB, int endB) {
    return startA < endB && startB < endA;
  }

}
