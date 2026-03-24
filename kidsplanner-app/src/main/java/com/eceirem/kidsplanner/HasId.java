package com.eceirem.kidsplanner;

import java.io.Serializable;

public interface HasId<T extends Serializable> extends Serializable {
  T getId();

  void setId(T id);
}
