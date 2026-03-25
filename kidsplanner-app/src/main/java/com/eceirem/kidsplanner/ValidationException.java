/**
 * @file ValidationException.java
 * @brief Unchecked validation / domain rule errors.
 */
package com.eceirem.kidsplanner;

/**
 * Unchecked failure for invalid input or domain rule violations (overlaps, required fields).
 */
public class ValidationException extends RuntimeException {
  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /** @param message explanation for console or tests */
  public ValidationException(String message) {
    super(message);
  }
}
