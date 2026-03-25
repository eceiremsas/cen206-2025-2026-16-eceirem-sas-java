package com.eceirem.kidsplanner;

/**
 * Category of an {@link Activity} (persisted as a string in SQL backends).
 */
public enum ActivityType {
  /** Study / homework */
  STUDY,
  /** Free play */
  PLAY,
  /** Physical activity */
  SPORT,
  /** Creative time */
  ART,
  /** Rest or sleep */
  SLEEP,
  /** Social / outings */
  SOCIAL
}
