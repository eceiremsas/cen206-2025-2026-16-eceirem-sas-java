package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Scanner;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class KidsplannerReflectionCoverageTest {

  @AfterEach
  public void cleanupFactoryState() {
    RepositoryFactory.setActiveStorage(StorageType.BINARY);
    RepositoryFactory.configureSqliteUrl("jdbc:sqlite:kidsplanner.db");
    RepositoryFactory.configureMysql("jdbc:mysql://localhost:3306/kidsplanner?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "root", "root");
  }

  @Test
  public void listTodayPlanTextModeCoversNonEmptyAndEmpty() throws Exception {
    Kidsplanner app = new Kidsplanner(new Scanner("\n"), false);
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    PrintStream original = System.out;
    try {
      System.setOut(new PrintStream(bout, true, StandardCharsets.UTF_8.name()));
      Method m = Kidsplanner.class.getDeclaredMethod("listTodayPlan");
      m.setAccessible(true);

      // Ensure deterministic empty-state branch for first invocation.
      Field repoField = Kidsplanner.class.getDeclaredField("activityRepository");
      repoField.setAccessible(true);
      @SuppressWarnings("unchecked")
      IRepository<Activity> repo = (IRepository<Activity>) repoField.get(app);
      for (Activity existing : repo.findAll()) {
        if (existing.getId() != null) {
          repo.delete(existing.getId().intValue());
        }
      }

      m.invoke(app); // Empty path
      String first = new String(bout.toByteArray(), StandardCharsets.UTF_8);
      assertTrue(first.contains("(no entries for today)"));

      Activity a = new Activity("Math", LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0),
          ActivityType.STUDY, null, null, Integer.valueOf(1));
      repo.save(a);
      bout.reset();

      m.invoke(app); // Non-empty path
      String second = new String(bout.toByteArray(), StandardCharsets.UTF_8);
      assertTrue(second.contains("--- Today's schedule"));
      assertTrue(second.contains("Math"));
    } finally {
      System.setOut(original);
    }
  }

  @Test
  public void sortActivitiesCoversTieBreakerById() throws Exception {
    Kidsplanner app = new Kidsplanner(new Scanner("\n"), false);
    Activity a1 = new Activity("A1", LocalDate.of(2026, 3, 26), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, null, null, Integer.valueOf(1));
    a1.setId(2);
    Activity a2 = new Activity("A2", LocalDate.of(2026, 3, 26), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, null, null, Integer.valueOf(1));
    a2.setId(1);
    List<Activity> items = new java.util.ArrayList<Activity>();
    items.add(a1);
    items.add(a2);

    Method m = Kidsplanner.class.getDeclaredMethod("sortActivities", List.class);
    m.setAccessible(true);
    m.invoke(app, items);
    assertTrue(items.get(0).getId() == 1);
    assertTrue(items.get(1).getId() == 2);
  }

  @Test
  public void listKidsPlainAndResolveKidIdCoverSortByIdBranches() throws Exception {
    Kidsplanner app = new Kidsplanner(new Scanner("\n"), false);
    Field kidRepoField = Kidsplanner.class.getDeclaredField("kidRepository");
    kidRepoField.setAccessible(true);
    @SuppressWarnings("unchecked")
    IRepository<Kid> repo = (IRepository<Kid>) kidRepoField.get(app);

    Kid k1 = new Kid("B", null, null);
    repo.save(k1);
    Kid k2 = new Kid("A", null, null);
    repo.save(k2);
    // Force reverse ids to exercise id comparator branch clearly.
    k1.setId(2);
    k2.setId(1);

    Method listKids = Kidsplanner.class.getDeclaredMethod("listKidsPlain");
    listKids.setAccessible(true);
    listKids.invoke(app);

    Method resolve = Kidsplanner.class.getDeclaredMethod("resolveKidIdFromLine", String.class);
    resolve.setAccessible(true);
    int picked = (Integer) resolve.invoke(app, "");
    assertTrue(picked == 1);
  }

  @Test
  public void readActivityBodyFromScannerCoversValidationBranches() throws Exception {
    Kidsplanner app = new Kidsplanner(new Scanner("\n"), false);
    Method m = Kidsplanner.class.getDeclaredMethod("readActivityBodyFromScanner", Activity.class, int.class);
    m.setAccessible(true);

    // Empty title -> title validation branch
    Field scannerField = Kidsplanner.class.getDeclaredField("scanner");
    scannerField.setAccessible(true);
    scannerField.set(app, new Scanner("\n"));
    Exception e1 = assertThrows(Exception.class, () -> m.invoke(app, null, 1));
    assertTrue(e1.getCause() instanceof ValidationException);

    // Invalid date -> DateTimeParseException branch
    scannerField.set(app, new Scanner("Title\nbad-date\n09:00\n10:00\nSTUDY\nloc\nnote\n"));
    Exception e2 = assertThrows(Exception.class, () -> m.invoke(app, null, 1));
    assertTrue(e2.getCause() instanceof ValidationException);
    assertTrue(e2.getCause().getMessage().toLowerCase().contains("date/time"));

    // Invalid type -> IllegalArgumentException branch
    scannerField.set(app, new Scanner("Title\n2026-03-26\n09:00\n10:00\nNOT_A_TYPE\nloc\nnote\n"));
    Exception e3 = assertThrows(Exception.class, () -> m.invoke(app, null, 1));
    assertTrue(e3.getCause() instanceof ValidationException);
    assertTrue(e3.getCause().getMessage().toLowerCase().contains("activity type"));
  }

  @Test
  public void explainStorageCoversBinaryAndMysqlText() throws Exception {
    Kidsplanner app = new Kidsplanner(new Scanner("\n"), false);
    Method m = Kidsplanner.class.getDeclaredMethod("explainStorage", StorageType.class);
    m.setAccessible(true);

    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    PrintStream original = System.out;
    try {
      System.setOut(new PrintStream(bout, true, StandardCharsets.UTF_8.name()));
      m.invoke(app, StorageType.BINARY);
      m.invoke(app, StorageType.MYSQL);
    } finally {
      System.setOut(original);
    }
    String out = new String(bout.toByteArray(), StandardCharsets.UTF_8);
    assertTrue(out.contains("Binary (files)"));
    assertTrue(out.contains("MySQL (server DB)"));
  }

  @Test
  public void readActivityBodyFromScannerCoversSuccessWithBaseDefaults() throws Exception {
    Kidsplanner app = new Kidsplanner(new Scanner("\n"), false);
    Method m = Kidsplanner.class.getDeclaredMethod("readActivityBodyFromScanner", Activity.class, int.class);
    m.setAccessible(true);

    Activity base = new Activity("Base", LocalDate.of(2026, 3, 26), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, "Home", "N", Integer.valueOf(1));

    // Empty inputs should take defaults from base via ask(...).
    Field scannerField = Kidsplanner.class.getDeclaredField("scanner");
    scannerField.setAccessible(true);
    scannerField.set(app, new Scanner("\n\n\n\n\n\n\n"));

    Activity out = (Activity) m.invoke(app, base, 1);
    assertTrue("Base".equals(out.getTitle()));
    assertTrue(ActivityType.STUDY == out.getType());
    assertTrue(out.getKidId().intValue() == 1);
  }

  @Test
  public void validateActivityCoversNullKidBadTimeAndOverlapBranches() throws Exception {
    Kidsplanner app = new Kidsplanner(new Scanner("\n"), false);
    Method validate = Kidsplanner.class.getDeclaredMethod("validateActivity", Activity.class, int.class);
    validate.setAccessible(true);

    Activity nullKid = new Activity("T", LocalDate.of(2026, 3, 26), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, null, null, null);
    Exception e1 = assertThrows(Exception.class, () -> validate.invoke(app, nullKid, -1));
    assertTrue(e1.getCause() instanceof ValidationException);

    Activity badTime = new Activity("T", LocalDate.of(2026, 3, 26), LocalTime.of(10, 0), LocalTime.of(9, 0),
        ActivityType.STUDY, null, null, Integer.valueOf(1));
    Exception e2 = assertThrows(Exception.class, () -> validate.invoke(app, badTime, -1));
    assertTrue(e2.getCause() instanceof ValidationException);

    Field repoField = Kidsplanner.class.getDeclaredField("activityRepository");
    repoField.setAccessible(true);
    @SuppressWarnings("unchecked")
    IRepository<Activity> repo = (IRepository<Activity>) repoField.get(app);
    Activity existing = new Activity("E", LocalDate.of(2026, 3, 26), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, null, null, Integer.valueOf(1));
    repo.save(existing);

    Activity overlap = new Activity("O", LocalDate.of(2026, 3, 26), LocalTime.of(9, 30), LocalTime.of(10, 30),
        ActivityType.PLAY, null, null, Integer.valueOf(1));
    Exception e3 = assertThrows(Exception.class, () -> validate.invoke(app, overlap, -1));
    assertTrue(e3.getCause() instanceof ValidationException);
    assertTrue(e3.getCause().getMessage().toLowerCase().contains("overlap"));
  }

  @Test
  public void validateActivityCoversContinueBranchesAndNoThrowPath() throws Exception {
    Kidsplanner app = new Kidsplanner(new Scanner("\n"), false);
    Method validate = Kidsplanner.class.getDeclaredMethod("validateActivity", Activity.class, int.class);
    validate.setAccessible(true);

    Field repoField = Kidsplanner.class.getDeclaredField("activityRepository");
    repoField.setAccessible(true);
    @SuppressWarnings("unchecked")
    IRepository<Activity> repo = (IRepository<Activity>) repoField.get(app);

    Activity self = new Activity("Self", LocalDate.of(2026, 3, 26), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, null, null, Integer.valueOf(1));
    repo.save(self);
    Activity differentKid = new Activity("OtherKid", LocalDate.of(2026, 3, 26), LocalTime.of(9, 15), LocalTime.of(9, 45),
        ActivityType.PLAY, null, null, Integer.valueOf(2));
    repo.save(differentKid);
    Activity differentDate = new Activity("OtherDate", LocalDate.of(2026, 3, 27), LocalTime.of(9, 15), LocalTime.of(9, 45),
        ActivityType.PLAY, null, null, Integer.valueOf(1));
    repo.save(differentDate);

    Activity candidate = new Activity("Candidate", LocalDate.of(2026, 3, 26), LocalTime.of(10, 30), LocalTime.of(11, 0),
        ActivityType.SPORT, null, null, Integer.valueOf(1));
    // Should pass: self skipped by currentId, other records skipped by kid/date/non-overlap.
    validate.invoke(app, candidate, self.getId().intValue());
  }

  @Test
  public void readActivityFieldsCoversBaseKidDefaultAndInvalidType() throws Exception {
    Kidsplanner app = new Kidsplanner(new Scanner("\n"), false);
    Method m = Kidsplanner.class.getDeclaredMethod("readActivityFields", Activity.class);
    m.setAccessible(true);
    Field scannerField = Kidsplanner.class.getDeclaredField("scanner");
    scannerField.setAccessible(true);

    Activity base = new Activity("B", LocalDate.of(2026, 3, 26), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, "L", "N", null);

    // Invalid type path in readActivityFields
    scannerField.set(app, new Scanner("1\nTitle\n2026-03-26\n09:00\n10:00\nNOT_A_TYPE\nloc\nnote\n"));
    InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> m.invoke(app, base));
    assertTrue(ex.getCause() instanceof ValidationException);
    assertTrue(ex.getCause().getMessage().toLowerCase().contains("activity type"));
  }

  @Test
  public void addKidTextCoversYearBranchesAndNameValidation() throws Exception {
    Kidsplanner app = new Kidsplanner(new Scanner("\n"), false);
    Method addKidText = Kidsplanner.class.getDeclaredMethod("addKidText");
    addKidText.setAccessible(true);
    Field scannerField = Kidsplanner.class.getDeclaredField("scanner");
    scannerField.setAccessible(true);
    Field kidRepoField = Kidsplanner.class.getDeclaredField("kidRepository");
    kidRepoField.setAccessible(true);
    @SuppressWarnings("unchecked")
    IRepository<Kid> repo = (IRepository<Kid>) kidRepoField.get(app);

    // Empty year branch
    scannerField.set(app, new Scanner("Ali\n\nnote\n"));
    addKidText.invoke(app);

    // Non-empty year branch
    scannerField.set(app, new Scanner("Veli\n2015\nnote2\n"));
    addKidText.invoke(app);

    // Empty name validation branch
    scannerField.set(app, new Scanner("\n\nnote3\n"));
    InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> addKidText.invoke(app));
    assertTrue(ex.getCause() instanceof ValidationException);

    List<Kid> kids = repo.findAll();
    assertTrue(kids.size() >= 2);
  }

  @Test
  public void explainStorageAlsoCoversSqliteBranch() throws Exception {
    Kidsplanner app = new Kidsplanner(new Scanner("\n"), false);
    Method m = Kidsplanner.class.getDeclaredMethod("explainStorage", StorageType.class);
    m.setAccessible(true);
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    PrintStream original = System.out;
    try {
      System.setOut(new PrintStream(bout, true, StandardCharsets.UTF_8.name()));
      m.invoke(app, StorageType.SQLITE);
    } finally {
      System.setOut(original);
    }
    String out = new String(bout.toByteArray(), StandardCharsets.UTF_8);
    assertTrue(out.contains("SQLite (single-file DB)"));
  }

  @Test
  public void askCoversNullDefaultBranch() throws Exception {
    Kidsplanner app = new Kidsplanner(new Scanner("\n"), false);
    Method ask = Kidsplanner.class.getDeclaredMethod("ask", String.class, String.class);
    ask.setAccessible(true);
    Field scannerField = Kidsplanner.class.getDeclaredField("scanner");
    scannerField.setAccessible(true);
    scannerField.set(app, new Scanner("\n"));
    String value = (String) ask.invoke(app, "Label", null);
    assertTrue("".equals(value));
  }

  @Test
  public void resolveKidIdFromLineCoversExplicitExistingId() throws Exception {
    Kidsplanner app = new Kidsplanner(new Scanner("\n"), false);
    Method resolve = Kidsplanner.class.getDeclaredMethod("resolveKidIdFromLine", String.class);
    resolve.setAccessible(true);
    Field kidRepoField = Kidsplanner.class.getDeclaredField("kidRepository");
    kidRepoField.setAccessible(true);
    @SuppressWarnings("unchecked")
    IRepository<Kid> repo = (IRepository<Kid>) kidRepoField.get(app);
    Kid kid = new Kid("Kid", null, null);
    repo.save(kid);
    int id = (Integer) resolve.invoke(app, String.valueOf(kid.getId()));
    assertTrue(id == kid.getId().intValue());
  }

  @Test
  public void addKidInteractiveCoversYearAndValidationBranch() throws Exception {
    Kidsplanner app = new Kidsplanner(new Scanner("\n"), false);
    Method addKidInteractive = Kidsplanner.class.getDeclaredMethod("addKidInteractive");
    addKidInteractive.setAccessible(true);
    Field scannerField = Kidsplanner.class.getDeclaredField("scanner");
    scannerField.setAccessible(true);

    scannerField.set(app, new Scanner("Name\n2014\nnote\n\n"));
    addKidInteractive.invoke(app);

    scannerField.set(app, new Scanner("\n\nnote\n"));
    InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> addKidInteractive.invoke(app));
    assertTrue(ex.getCause() instanceof ValidationException);
  }

  @Test
  public void validateActivityCoversEmptyTitleBranch() throws Exception {
    Kidsplanner app = new Kidsplanner(new Scanner("\n"), false);
    Method validate = Kidsplanner.class.getDeclaredMethod("validateActivity", Activity.class, int.class);
    validate.setAccessible(true);
    Activity emptyTitle = new Activity(" ", LocalDate.of(2026, 3, 26), LocalTime.of(9, 0), LocalTime.of(10, 0),
        ActivityType.STUDY, null, null, Integer.valueOf(1));
    InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> validate.invoke(app, emptyTitle, -1));
    assertTrue(ex.getCause() instanceof ValidationException);
    assertTrue(ex.getCause().getMessage().toLowerCase().contains("title"));
  }
}
