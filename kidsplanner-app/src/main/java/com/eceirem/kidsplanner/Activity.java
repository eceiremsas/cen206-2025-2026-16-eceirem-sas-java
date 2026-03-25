/**

@file Activity.java
@brief Scheduled activity entity (one child, one day, overlap rules).
@details Maps to kid-linked rows; overlap rules enforced in {@link Kidsplanner}.
*/

/**

@package com.eceirem.kidsplanner
@brief Domain package for {@link Activity} and related types.
*/
package com.eceirem.kidsplanner;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Scheduled activity for a specific child (kidId). Overlap rules apply per kid and date.
 */
public class Activity implements HasId<Integer> {
  /** Serialization version. */
  private static final long serialVersionUID = 2L;

  /** Primary key; null before persistence. */
  private Integer id;
  /** Owning child id. */
  private Integer kidId;
  /** Short title shown in lists. */
  private String title;
  /** Calendar date of the activity. */
  private LocalDate date;
  /** Start time of day. */
  private LocalTime startTime;
  /** End time of day. */
  private LocalTime endTime;
  /** Category (persisted as string in SQL backends). */
  private ActivityType type;
  /** Optional place. */
  private String location;
  /** Optional free text. */
  private String notes;

  /** No-arg constructor for serialization. */
  public Activity() {
  }

  /**
   * Delegates to {@link #Activity(String, LocalDate, LocalTime, LocalTime, ActivityType, String, String, Integer)}
   * with {@code kidId == null}.
   */
  public Activity(String title, LocalDate date, LocalTime startTime, LocalTime endTime, ActivityType type, String location,
      String notes) {
    this(title, date, startTime, endTime, type, location, notes, null);
  }

  /**
   * @param title activity title
   * @param date calendar date
   * @param startTime start time of day
   * @param endTime end time of day
   * @param type {@link ActivityType}
   * @param location optional
   * @param notes optional
   * @param kidId owning child id; may be {@code null} until linked
   */
  public Activity(String title, LocalDate date, LocalTime startTime, LocalTime endTime, ActivityType type, String location,
      String notes, Integer kidId) {
    this.title = title;
    this.date = date;
    this.startTime = startTime;
    this.endTime = endTime;
    this.type = type;
    this.location = location;
    this.notes = notes;
    this.kidId = kidId;
  }

  /** @return id or {@code null} before first save */
  @Override public Integer getId() {
    return id;
  }

  /** @param id key from persistence */
  @Override public void setId(Integer id) {
    this.id = id;
  }

  /** @return owning child id */
  public Integer getKidId() {
    return kidId;
  }

  /** @param kidId owning child id */
  public void setKidId(Integer kidId) {
    this.kidId = kidId;
  }

  /** @return title */
  public String getTitle() {
    return title;
  }

  /** @param title activity title */
  public void setTitle(String title) {
    this.title = title;
  }

  /** @return activity date */
  public LocalDate getDate() {
    return date;
  }

  /** @param date calendar date */
  public void setDate(LocalDate date) {
    this.date = date;
  }

  /** @return start time */
  public LocalTime getStartTime() {
    return startTime;
  }

  /** @param startTime start time of day */
  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  /** @return end time */
  public LocalTime getEndTime() {
    return endTime;
  }

  /** @param endTime end time of day */
  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
  }

  /** @return activity type */
  public ActivityType getType() {
    return type;
  }

  /** @param type category */
  public void setType(ActivityType type) {
    this.type = type;
  }

  /** @return location or null */
  public String getLocation() {
    return location;
  }

  /** @param location optional place */
  public void setLocation(String location) {
    this.location = location;
  }

  /** @return notes or null */
  public String getNotes() {
    return notes;
  }

  /** @param notes optional text */
  public void setNotes(String notes) {
    this.notes = notes;
  }

  /** @return compact debug representation */
  @Override
  public String toString() {
    return "Activity{id=" + id + ", kidId=" + kidId + ", title='" + title + "', date=" + date + ", start=" + startTime
        + ", end=" + endTime + ", type=" + type + ", location='" + location + "'}";
  }
}
