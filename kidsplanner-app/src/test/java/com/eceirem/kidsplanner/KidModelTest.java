/**
 * @file KidModelTest.java
 * @brief Unit tests for {@link Kid} (constructors, accessors, toString).
 */
package com.eceirem.kidsplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KidModelTest {

  @Test
  void defaultConstructorAllowsPropertyRoundTrip() {
    Kid k = new Kid();
    assertNull(k.getId());
    k.setId(42);
    k.setDisplayName("Ada");
    k.setBirthYear(2018);
    k.setNotes("allergies: none");
    assertEquals(Integer.valueOf(42), k.getId());
    assertEquals("Ada", k.getDisplayName());
    assertEquals(Integer.valueOf(2018), k.getBirthYear());
    assertEquals("allergies: none", k.getNotes());
  }

  @Test
  void threeArgConstructorStoresFields() {
    Kid k = new Kid("Beta", 2019, "park days");
    assertEquals("Beta", k.getDisplayName());
    assertEquals(Integer.valueOf(2019), k.getBirthYear());
    assertEquals("park days", k.getNotes());
    assertNull(k.getId());
  }

  @Test
  void toStringWithNullIdStillIncludesDisplayName() {
    Kid k = new Kid("Gamma", 2020, null);
    String s = k.toString();
    assertTrue(s.contains("id=null"));
    assertTrue(s.contains("displayName='Gamma'"));
    assertTrue(s.contains("birthYear=2020"));
  }

  @Test
  void implementsHasIdInteger() {
    Kid k = new Kid();
    k.setId(99);
    HasId<Integer> asHasId = k;
    assertEquals(Integer.valueOf(99), asHasId.getId());
    asHasId.setId(100);
    assertEquals(Integer.valueOf(100), k.getId());
  }
}
