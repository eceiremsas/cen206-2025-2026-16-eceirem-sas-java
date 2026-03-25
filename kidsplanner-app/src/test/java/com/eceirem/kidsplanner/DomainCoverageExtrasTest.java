/**
 * @file DomainCoverageExtrasTest.java
 * @brief Extra unit tests for JaCoCo-included domain types (no coverage drop).
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DomainCoverageExtrasTest {

  @Test
  void activityDefaultConstructorThenAllSetters() {
    Activity a = new Activity();
    a.setId(1);
    a.setKidId(2);
    a.setTitle("T");
    LocalDate d = LocalDate.of(2027, 1, 15);
    LocalTime s = LocalTime.of(12, 0);
    LocalTime e = LocalTime.of(13, 0);
    a.setDate(d);
    a.setStartTime(s);
    a.setEndTime(e);
    a.setType(ActivityType.ART);
    a.setLocation("Studio");
    a.setNotes("paint");
    assertEquals(Integer.valueOf(1), a.getId());
    assertEquals(Integer.valueOf(2), a.getKidId());
    assertEquals("T", a.getTitle());
    assertEquals(d, a.getDate());
    assertEquals(s, a.getStartTime());
    assertEquals(e, a.getEndTime());
    assertEquals(ActivityType.ART, a.getType());
    assertEquals("Studio", a.getLocation());
    assertEquals("paint", a.getNotes());
  }

  @Test
  void activityToStringWithAllFieldsStillNull() {
    String t = new Activity().toString();
    assertTrue(t.contains("id=null"));
    assertTrue(t.contains("kidId=null"));
    // title is formatted as title='null' when the field is null
    assertTrue(t.contains("title="));
    assertTrue(t.contains("date=null"));
    assertTrue(t.contains("type=null"));
  }

  @Test
  void activityToStringWithPopulatedFieldsButNullIds() {
    Activity a = new Activity(
        "X",
        LocalDate.of(2027, 2, 1),
        LocalTime.of(10, 0),
        LocalTime.of(11, 0),
        ActivityType.SOCIAL,
        "Cafe",
        "friends");
    String t = a.toString();
    assertTrue(t.contains("id=null"));
    assertTrue(t.contains("kidId=null"));
    assertTrue(t.contains("type=SOCIAL"));
    assertTrue(t.contains("location='Cafe'"));
  }

  @ParameterizedTest
  @EnumSource(ActivityType.class)
  void activityCanHoldEachActivityType(ActivityType type) {
    Activity a = new Activity(
        "slot",
        LocalDate.of(2027, 3, 1),
        LocalTime.of(9, 0),
        LocalTime.of(10, 0),
        type,
        "here",
        "n");
    assertEquals(type, a.getType());
  }

  @Test
  void kidToStringWithEmptyDisplayName() {
    Kid k = new Kid("", 2015, "n");
    k.setId(0);
    String s = k.toString();
    assertTrue(s.contains("displayName=''"));
    assertTrue(s.contains("birthYear=2015"));
  }

  @Test
  void kidNotesCanBeClearedWithSetter() {
    Kid k = new Kid("Z", 2014, "old");
    k.setNotes(null);
    assertNull(k.getNotes());
  }

  @Test
  void repositoryExceptionMessageOnlyConstructor() {
    RepositoryException ex = new RepositoryException("only");
    assertEquals("only", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  void repositoryExceptionWithCausePreservesMessage() {
    Exception cause = new Exception("inner");
    RepositoryException ex = new RepositoryException("outer", cause);
    assertEquals("outer", ex.getMessage());
    assertEquals(cause, ex.getCause());
  }

  @Test
  void validationExceptionIndependentInstances() {
    assertEquals("a", new ValidationException("a").getMessage());
    assertEquals("b", new ValidationException("b").getMessage());
  }

  @Test
  void activityTypeEnumExhaustiveCount() {
    assertEquals(6, ActivityType.values().length);
  }

  @Test
  void storageTypeEnumExhaustiveCount() {
    assertEquals(3, StorageType.values().length);
  }

  @Test
  void storageTypesAreNamedAsExpected() {
    assertNotNull(StorageType.BINARY);
    assertNotNull(StorageType.SQLITE);
    assertNotNull(StorageType.MYSQL);
  }
}
