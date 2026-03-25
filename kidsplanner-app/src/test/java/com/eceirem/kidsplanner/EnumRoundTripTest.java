/**
 * @file EnumRoundTripTest.java
 * @brief Ensures every enum constant is reachable via {@code valueOf(name())}.
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EnumRoundTripTest {

  @ParameterizedTest
  @EnumSource(ActivityType.class)
  void activityTypeNameRoundTrip(ActivityType expected) {
    assertEquals(expected, ActivityType.valueOf(expected.name()));
  }

  @ParameterizedTest
  @EnumSource(StorageType.class)
  void storageTypeNameRoundTrip(StorageType expected) {
    assertEquals(expected, StorageType.valueOf(expected.name()));
  }
}
