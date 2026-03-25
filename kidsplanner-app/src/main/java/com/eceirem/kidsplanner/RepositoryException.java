package com.eceirem.kidsplanner;

/**
 * Unchecked failure for persistence, consent, or similar non-validation errors.
 */
public class RepositoryException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /** @param message message for user or logs */
  public RepositoryException(String message) {
    super(message);
  }

  /**
   * @param message message for user or logs
   * @param cause root cause (e.g. {@code java.sql.SQLException})
   */
  public RepositoryException(String message, Throwable cause) {
    super(message, cause);
  }
}
