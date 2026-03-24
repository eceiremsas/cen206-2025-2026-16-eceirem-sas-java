package com.eceirem.kidsplanner;

/**
 * Domain entity representing a child profile (minimal fields for KVKK / data minimization).
 */
public class Kid implements HasId<Integer> {
  private static final long serialVersionUID = 1L;

  private Integer id;
  private String displayName;
  private Integer birthYear;
  private String notes;

  public Kid() {
  }

  public Kid(String displayName, Integer birthYear, String notes) {
    this.displayName = displayName;
    this.birthYear = birthYear;
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

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public Integer getBirthYear() {
    return birthYear;
  }

  public void setBirthYear(Integer birthYear) {
    this.birthYear = birthYear;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  @Override
  public String toString() {
    return "Kid{id=" + id + ", displayName='" + displayName + "', birthYear=" + birthYear + "}";
  }
}
