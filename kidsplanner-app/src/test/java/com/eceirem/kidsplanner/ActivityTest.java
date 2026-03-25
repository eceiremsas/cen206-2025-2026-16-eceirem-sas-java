/**
 * @file ActivityTest.java
 * @brief Unit tests for Activity class.
 * @details Verifies constructor, getters, id methods, and toString.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

/**
 * @class ActivityTest
 * @brief Unit tests for Activity.
 * @details This class tests only Activity behavior.
 */
public class ActivityTest {

  /**
   * @brief Verifies no-args constructor creates instance.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testDefaultConstructorCreatesActivity() {
    Activity activity = new Activity();
    activity.setId(5);
    assertEquals(Integer.valueOf(5), activity.getId());
  }

  /**
   * @brief Verifies full constructor maps all fields correctly.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testFullConstructorAndGetters() {
    Activity activity = new Activity(
        "Homework",
        LocalDate.of(2026, 3, 25),
        LocalTime.of(10, 0),
        LocalTime.of(11, 0),
        ActivityType.STUDY,
        "Home",
        "Math notes",
        Integer.valueOf(1));

    assertEquals("Homework", activity.getTitle());
    assertEquals(Integer.valueOf(1), activity.getKidId());
    assertEquals(LocalDate.of(2026, 3, 25), activity.getDate());
    assertEquals(LocalTime.of(10, 0), activity.getStartTime());
    assertEquals(LocalTime.of(11, 0), activity.getEndTime());
    assertEquals(ActivityType.STUDY, activity.getType());
    assertEquals("Home", activity.getLocation());
    assertEquals("Math notes", activity.getNotes());
  }

  /**
   * @brief Verifies toString includes key activity fields.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testToStringContainsTitleAndType() {
    Activity activity = new Activity(
        "Play",
        LocalDate.of(2026, 3, 26),
        LocalTime.of(15, 0),
        LocalTime.of(16, 0),
        ActivityType.PLAY,
        "Park",
        "Ball",
        Integer.valueOf(2));
    activity.setId(3);

    String text = activity.toString();
    assertTrue(text.contains("id=3"));
    assertTrue(text.contains("title='Play'"));
    assertTrue(text.contains("type=PLAY"));
    assertTrue(text.contains("kidId=2"));
  }

  /**
   * @brief Verifies default constructor keeps object fields null.
   *
   * @param none This method has no parameter.
   * @return void This method returns no value.
   * @throws none This method does not throw checked exceptions.
   */
  @Test
  public void testDefaultConstructorLeavesOptionalFieldsNull() {
    Activity activity = new Activity();
    assertNull(activity.getTitle());
    assertNull(activity.getDate());
    assertNull(activity.getStartTime());
    assertNull(activity.getEndTime());
    assertNull(activity.getType());
    assertNull(activity.getLocation());
    assertNull(activity.getNotes());
    assertNull(activity.getKidId());
  }

  /**
   * Seven-arg constructor delegates to eight-arg with {@code kidId == null}.
   */
  @Test
  public void testSevenArgConstructorLeavesKidIdNull() {
    Activity activity = new Activity(
        "Reading",
        LocalDate.of(2026, 6, 1),
        LocalTime.of(8, 30),
        LocalTime.of(9, 0),
        ActivityType.STUDY,
        "Library",
        "chapter 3");
    assertNull(activity.getKidId());
    assertEquals("Reading", activity.getTitle());
    assertEquals(ActivityType.STUDY, activity.getType());
  }

  @Test
  public void testSetKidIdAfterConstruction() {
    Activity activity = new Activity(
        "Sport",
        LocalDate.of(2026, 6, 2),
        LocalTime.of(17, 0),
        LocalTime.of(18, 0),
        ActivityType.SPORT,
        "Field",
        "bring water");
    activity.setKidId(5);
    assertEquals(Integer.valueOf(5), activity.getKidId());
  }
}
