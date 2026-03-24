package com.eceirem.kidsplanner;

import java.time.LocalDate;
import java.time.LocalTime;

public class Activity implements HasId<Integer> {
  private static final long serialVersionUID = 1L;

  private Integer id;
  private String title;
  private LocalDate date;
  private LocalTime startTime;
  private LocalTime endTime;
  private ActivityType type;
  private String location;
  private String notes;

  public Activity() {
  }

  public Activity(String title, LocalDate date, LocalTime startTime, LocalTime endTime, ActivityType type, String location,
      String notes) {
    this.title = title;
    this.date = date;
    this.startTime = startTime;
    this.endTime = endTime;
    this.type = type;
    this.location = location;
    this.notes = notes;
  }

  @Override
  public Integer getId() {
    return id;
  }

  @Override
  public void setId(Integer id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public LocalDate getDate() {
    return date;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public ActivityType getType() {
    return type;
  }

  public String getLocation() {
    return location;
  }

  public String getNotes() {
    return notes;
  }

  @Override
  public String toString() {
    return "Activity{id=" + id + ", title='" + title + "', date=" + date + ", start=" + startTime + ", end=" + endTime + ", type=" + type
        + ", location='" + location + "'}";
  }
}
