package com.eceirem.kidsplanner;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Scheduled activity for a specific child (kidId). Overlap rules apply per kid and date.
 */
public class Activity implements HasId<Integer> {
  private static final long serialVersionUID = 2L;

  private Integer id;
  private Integer kidId;
  private String title;
  private LocalDate date;
  private LocalTime startTime;
  private LocalTime endTime;
  private ActivityType type;
  private String location;
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

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getKidId() {
    return kidId;
  }

  public void setKidId(Integer kidId) {
    this.kidId = kidId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
  }

  public ActivityType getType() {
    return type;
  }

  public void setType(ActivityType type) {
    this.type = type;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  @Override
  public String toString() {
    return "Activity{id=" + id + ", kidId=" + kidId + ", title='" + title + "', date=" + date + ", start=" + startTime
        + ", end=" + endTime + ", type=" + type + ", location='" + location + "'}";
  }
}
