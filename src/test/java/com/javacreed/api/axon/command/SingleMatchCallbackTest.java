package com.javacreed.api.axon.command;

import java.awt.Rectangle;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

public class SingleMatchCallbackTest {

  private static class Holder<T> implements Consumer<T> {
    private T value;

    @Override
    public void accept(final T t) {
      this.value = t;
    }

    public void clear() {
      value = null;
    }

    public T getValue() {
      return value;
    }
  }

  @Test
  public void empty() {
    final Callback callback = SingleMatchCallback.builder().end();
    callback.apply(new Object());
    Assert.assertSame(NoopCallback.instance(), callback);
  }

  @Test
  public void orElse() {
    final Holder<Object> orElse = new Holder<>();
    final Callback callback = SingleMatchCallback.builder().orElse(orElse);

    final Object event = new Object();
    callback.apply(event);
    Assert.assertSame(event, orElse.getValue());
  }

  @Test
  public void whenThen() {
    final Holder<String> whenThen = new Holder<>();
    final Holder<Object> orElse = new Holder<>();
    final Callback callback = SingleMatchCallback.builder()
                                                 .whenThen(String.class, whenThen)
                                                 .orElse(orElse);

    Object event = new Object();
    callback.apply(event);
    Assert.assertNull(whenThen.getValue());
    Assert.assertSame(event, orElse.getValue());

    orElse.clear();
    event = "Some event";
    callback.apply(event);
    Assert.assertSame(event, whenThen.getValue());
    Assert.assertNull(orElse.getValue());

    whenThen.clear();
    event = new Rectangle();
    callback.apply(event);
    Assert.assertNull(whenThen.getValue());
    Assert.assertSame(event, orElse.getValue());
  }
}
