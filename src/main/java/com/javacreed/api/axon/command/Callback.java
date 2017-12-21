package com.javacreed.api.axon.command;

@FunctionalInterface
public interface Callback {

  void apply(Object event);
}