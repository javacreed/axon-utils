package com.javacreed.api.axon.command;

/**
 * Provided a no-operation callback
 *
 * @author Albert Attard
 */
public class NoopCallback {

  private static final Callback NOOP = e -> {};

  public static Callback instance() {
    return NoopCallback.NOOP;
  }

  private NoopCallback() {}
}
