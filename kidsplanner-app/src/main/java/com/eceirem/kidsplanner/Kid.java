/**

@file Kid.java
@brief Child profile entity (minimal fields for KVKK / data minimization).
*/
package com.eceirem.kidsplanner;

/**
 * Domain entity representing a child profile (minimal fields for KVKK / data minimization).
 */
public class Kid implements HasId<Integer> {
  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /** Primary key; null before persistence. */
  private Integer id;
  /** Name shown in UI. */
  private String displayName;
  /** Optional birth year. */
  private Integer birthYear;
  /** Optional note. */
  private String notes;

  /** No-arg constructor for serialization. */
  public Kid() {
  }

  /**
   * @param displayName shown in UI
   * @param birthYear optional
   * @param notes optional
   */
  public Kid(String displayName, Integer birthYear, String notes) {
    this.displayName = displayName;
    this.birthYear = birthYear;
    this.notes = notes;
  }

  /** @return id or {@code null} before first save */
  @Override public Integer getId() {
    return id;
  }

  /** @param id key from persistence */
  @Override public void setId(Integer id) {
    this.id = id;
  }

  /** @return display name */
  public String getDisplayName() {
    return displayName;
  }

  /** @param displayName shown name */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /** @return birth year or null */
  public Integer getBirthYear() {
    return birthYear;
  }

  /** @param birthYear optional year */
  public void setBirthYear(Integer birthYear) {
    this.birthYear = birthYear;
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
    return "Kid{id=" + id + ", displayName='" + displayName + "', birthYear=" + birthYear + "}";
  }
}
