package com.javacreed.api.axon.command;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.annotation.MetaDataValue;

public class DefaultCommandService implements CommandService {

  private static final String META_DATA_LABEL = "action-reference";

  private final CommandGateway commandGateway;

  private final ConcurrentMap<ActionReference, Callback> callbacks = new ConcurrentHashMap<>();

  public DefaultCommandService(final CommandGateway commandGateway) throws NullPointerException {
    this.commandGateway = Objects.requireNonNull(commandGateway);
  }

  @EventHandler
  public void apply(final Object event,
      @MetaDataValue(value = DefaultCommandService.META_DATA_LABEL, required = true) final ActionReference actionReference) {
    final Callback callback = callbacks.get(actionReference);
    if (callback != null) {
      try {
        callback.apply(event);
      } catch (final Throwable e) {
        /* TODO: deal with the errors */
      }
    }
  }

  private synchronized ActionReference linkCallback(final Callback callback) {
    for (int i = 0; i < 1000; i++) {
      final ActionReference actionReference = ActionReference.random();
      if (callbacks.containsKey(actionReference)) {
        continue;
      }

      callbacks.put(actionReference, callback);
      return actionReference;
    }

    throw new FailedToGenerateUniqueActionReferenceException();
  }

  @Override
  public <T, R> CompletableFuture<R> send(final T command) throws NullPointerException {
    return commandGateway.send(command);
  }

  @Override
  public <T> IgnoreHandle sendAndListen(final T command, final Callback callback)
      throws NullPointerException, FailedToGenerateUniqueActionReferenceException {
    final ActionReference actionReference = linkCallback(callback);

    /* Creating the metadata */
    final Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put(DefaultCommandService.META_DATA_LABEL, actionReference);

    /* Wrap the command into a command message and add the metadata (that includes the action-reference) */
    final CommandMessage<Object> message = GenericCommandMessage.asCommandMessage(command).andMetaData(metadata);

    /*
     * Once the command is executed we need to remove the callback and the action-reference from the map of callbacks to
     * prevent memory leaks. There is no point in storing these once the command has finished executing. Create the
     * command callback (from AXON) that removes the callback and the action-reference once ready. Also, in the event of
     * an error, pass the error to the callback so that they can deal with it.
     */
    final CommandCallback<CommandMessage<Object>, Object> cc = new CommandCallback<CommandMessage<Object>, Object>() {
      @Override
      public void onFailure(final CommandMessage<? extends CommandMessage<Object>> commandMessage,
          final Throwable cause) {
        /* Pass the exception so that the caller can deal with it too */
        callback.apply(cause);
        callbacks.remove(actionReference);
      }

      @Override
      public void onSuccess(final CommandMessage<? extends CommandMessage<Object>> commandMessage,
          final Object result) {
        /* Do not pass the result to the callback as we are only interested in the published events */
        callbacks.remove(actionReference);
      }
    };

    /* Send the command */
    commandGateway.send(message, cc);

    return () -> callbacks.remove(actionReference);
  }
}
