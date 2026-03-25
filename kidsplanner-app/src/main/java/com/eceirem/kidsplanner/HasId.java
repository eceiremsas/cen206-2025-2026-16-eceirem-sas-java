package com.eceirem.kidsplanner;

import java.io.Serializable;

/**
 * Surrogate-key contract for {@link IRepository} entities and Java serialization.
 *
 * @param <T> identifier type (e.g. {@code Integer})
 */
public interface HasId<T extends Serializable> extends Serializable {

  /** @return id or {@code null} before first save */
  T getId();

  /** @param id key from persistence */
  void setId(T id);
}
