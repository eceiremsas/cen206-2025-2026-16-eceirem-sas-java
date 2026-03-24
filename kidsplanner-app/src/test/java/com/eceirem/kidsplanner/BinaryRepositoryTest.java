/**
 * @file BinaryRepositoryTest.java
 * @brief Unit tests for BinaryRepository class.
 * @details Verifies CRUD and exceptional persistence paths.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * @class BinaryRepositoryTest
 * @brief Unit tests for BinaryRepository.
 * @details This class tests only BinaryRepository behavior.
 */
public class BinaryRepositoryTest {

  /**
   * @brief Verifies save assigns id and findById returns entity.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testSaveAssignsIdAndFindByIdReturnsEntity() {
    String file = "target/test-repo-save.bin";
    cleanup(file);
    BinaryRepository<Activity> repository = new BinaryRepository<Activity>(file);
    Activity activity = sample("A", 9, 10);

    repository.save(activity);

    assertNotNull(activity.getId());
    assertEquals(activity, repository.findById(activity.getId().intValue()));
    cleanup(file);
  }

  /**
   * @brief Verifies findAll returns all saved entities.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testFindAllReturnsSavedEntities() {
    String file = "target/test-repo-findall.bin";
    cleanup(file);
    BinaryRepository<Activity> repository = new BinaryRepository<Activity>(file);
    repository.save(sample("A", 9, 10));
    repository.save(sample("B", 10, 11));

    List<Activity> all = repository.findAll();

    assertEquals(2, all.size());
    cleanup(file);
  }

  /**
   * @brief Verifies update changes existing entity.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testUpdateReplacesExistingEntity() {
    String file = "target/test-repo-update.bin";
    cleanup(file);
    BinaryRepository<Activity> repository = new BinaryRepository<Activity>(file);
    Activity activity = sample("A", 9, 10);
    repository.save(activity);

    Activity updated = sample("Updated", 11, 12);
    updated.setId(activity.getId());
    repository.update(updated);

    assertEquals("Updated", repository.findById(activity.getId().intValue()).getTitle());
    cleanup(file);
  }

  /**
   * @brief Verifies update throws when id is missing.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testUpdateThrowsWhenEntityNotFound() {
    String file = "target/test-repo-update-missing.bin";
    cleanup(file);
    BinaryRepository<Activity> repository = new BinaryRepository<Activity>(file);
    Activity entity = sample("A", 9, 10);
    entity.setId(99);

    RepositoryException exception = assertThrows(RepositoryException.class, () -> repository.update(entity));
    assertTrue(exception.getMessage().contains("Guncellenecek kayit bulunamadi"));
    cleanup(file);
  }

  /**
   * @brief Verifies update throws when entity id is null.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testUpdateThrowsWhenEntityIdIsNull() {
    String file = "target/test-repo-update-null-id.bin";
    cleanup(file);
    BinaryRepository<Activity> repository = new BinaryRepository<Activity>(file);
    Activity entity = sample("A", 9, 10);

    RepositoryException exception = assertThrows(RepositoryException.class, () -> repository.update(entity));
    assertTrue(exception.getMessage().contains("Guncellenecek kayit bulunamadi"));
    cleanup(file);
  }

  /**
   * @brief Verifies delete removes existing entity.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testDeleteRemovesEntity() {
    String file = "target/test-repo-delete.bin";
    cleanup(file);
    BinaryRepository<Activity> repository = new BinaryRepository<Activity>(file);
    Activity activity = sample("A", 9, 10);
    repository.save(activity);

    repository.delete(activity.getId().intValue());

    assertNull(repository.findById(activity.getId().intValue()));
    cleanup(file);
  }

  /**
   * @brief Verifies persist throws when path cannot be written.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testSaveThrowsWhenPathIsUnwritable() {
    BinaryRepository<Activity> repository = new BinaryRepository<Activity>(".");
    Activity activity = sample("A", 9, 10);

    RepositoryException exception = assertThrows(RepositoryException.class, () -> repository.save(activity));
    assertTrue(exception.getMessage().contains("Binary veri yazilamadi"));
  }

  /**
   * @brief Verifies constructor loads existing binary map and increments id.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If file write fails.
   */
  @Test
  public void testConstructorLoadsExistingDataAndCalculatesNextId() throws java.io.IOException {
    String file = "target/test-repo-load-existing.bin";
    cleanup(file);
    Activity existing = sample("Existing", 8, 9);
    existing.setId(7);
    Map<Integer, Activity> seeded = new HashMap<Integer, Activity>();
    seeded.put(existing.getId(), existing);
    try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file))) {
      output.writeObject(seeded);
    }

    BinaryRepository<Activity> repository = new BinaryRepository<Activity>(file);
    Activity next = sample("Next", 10, 11);
    repository.save(next);

    assertEquals(Integer.valueOf(8), next.getId());
    cleanup(file);
  }

  /**
   * @brief Verifies constructor throws when class cannot be resolved.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws java.io.IOException If file operation fails.
   */
  @Test
  public void testConstructorThrowsWhenSerializedClassIsMissing() throws java.io.IOException {
    String file = "target/test-repo-class-not-found.bin";
    cleanup(file);
    Activity existing = sample("Existing", 8, 9);
    existing.setId(1);
    Map<Integer, Activity> seeded = new HashMap<Integer, Activity>();
    seeded.put(existing.getId(), existing);
    try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file))) {
      output.writeObject(seeded);
    }

    byte[] bytes = Files.readAllBytes(Paths.get(file));
    String text = new String(bytes, StandardCharsets.ISO_8859_1);
    String originalName = "com.eceirem.kidsplanner.Activity";
    String replacedName = "com.eceirem.kidsplanner.MissType";
    text = text.replace(originalName, replacedName);
    Files.write(Paths.get(file), text.getBytes(StandardCharsets.ISO_8859_1));

    RepositoryException exception = assertThrows(
        RepositoryException.class,
        () -> new BinaryRepository<Activity>(file));
    assertTrue(exception.getMessage().contains("Binary veri okunamadi"));
    cleanup(file);
  }

  private Activity sample(String title, int startHour, int endHour) {
    return new Activity(
        title,
        LocalDate.of(2026, 3, 25),
        LocalTime.of(startHour, 0),
        LocalTime.of(endHour, 0),
        ActivityType.STUDY,
        "Home",
        "Notes");
  }

  private void cleanup(String path) {
    File file = new File(path);
    if (file.exists()) {
      file.delete();
    }
  }
}
