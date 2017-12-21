package com.javacreed.api.axon.command;

import java.util.UUID;

public class ActionReference implements Comparable<ActionReference> {

  public static ActionReference random() {
    return new ActionReference(UUID.randomUUID());
  }

  private final UUID value;

  private ActionReference(final UUID value) {
    this.value = value;
  }

  @Override
  public int compareTo(final ActionReference other) {
    return value.compareTo(other.value);
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }

    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    return value.equals(((ActionReference) object).value);
  }

  public UUID getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
