package de.zalando.paradox.nakadi.consumer.core.http.handlers;

import de.zalando.paradox.nakadi.consumer.core.EventHandler;

public interface BatchEventsHandler<T> extends EventHandler<T>, EventClassProvider<T> { }
