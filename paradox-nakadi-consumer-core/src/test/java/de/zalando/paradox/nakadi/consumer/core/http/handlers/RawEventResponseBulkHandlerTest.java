package de.zalando.paradox.nakadi.consumer.core.http.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Matchers.any;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static de.zalando.paradox.nakadi.consumer.core.http.handlers.TestEvents.EVENT_TYPE_PARTITION;
import static de.zalando.paradox.nakadi.consumer.core.http.handlers.TestEvents.KEEP_ALIVE_EVENT;
import static de.zalando.paradox.nakadi.consumer.core.http.handlers.TestEvents.OBJECT_MAPPER;
import static de.zalando.paradox.nakadi.consumer.core.http.handlers.TestEvents.ONE_EVENT;
import static de.zalando.paradox.nakadi.consumer.core.http.handlers.TestEvents.ONE_EVENT_1;
import static de.zalando.paradox.nakadi.consumer.core.http.handlers.TestEvents.TWO_EVENTS;
import static de.zalando.paradox.nakadi.consumer.core.http.handlers.TestEvents.TWO_EVENTS_1;
import static de.zalando.paradox.nakadi.consumer.core.http.handlers.TestEvents.TWO_EVENTS_2;

import java.util.List;

import org.assertj.core.groups.Tuple;

import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.zalando.paradox.nakadi.consumer.core.domain.EventTypeCursor;
import de.zalando.paradox.nakadi.consumer.core.partitioned.PartitionCoordinator;

public class RawEventResponseBulkHandlerTest {

    @Mock
    private PartitionCoordinator coordinator;

    @Mock
    private RawEventBulkHandler delegate;

    @Captor
    private ArgumentCaptor<EventTypeCursor> eventCursorCaptor;

    @Captor
    private ArgumentCaptor<List<String>> rawEventsCaptor;

    private RawEventResponseBulkHandler handler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.handler = new RawEventResponseBulkHandler(EVENT_TYPE_PARTITION, OBJECT_MAPPER, coordinator, delegate);
    }

    @Test
    public void testOneEvent() {
        handler.onResponse(ONE_EVENT);

        verify(delegate, times(1)).onEvent(eventCursorCaptor.capture(), rawEventsCaptor.capture());
        assertThat(eventCursorCaptor.getValue()).extracting("eventTypePartition", "offset").containsExactly(
            EVENT_TYPE_PARTITION, "5");

        assertThat(rawEventsCaptor.getValue().get(0)).isEqualTo(ONE_EVENT_1);

        final ArgumentCaptor<EventTypeCursor> coordinatorCursorCaptor = ArgumentCaptor.forClass(EventTypeCursor.class);
        verify(coordinator, times(1)).commit(coordinatorCursorCaptor.capture());
        assertThat(coordinatorCursorCaptor.getValue()).extracting("eventTypePartition", "offset").containsExactly(
            EVENT_TYPE_PARTITION, "5");
    }

    @Test
    public void testTwoEvents() {
        handler.onResponse(TWO_EVENTS);

        verify(delegate, times(1)).onEvent(eventCursorCaptor.capture(), rawEventsCaptor.capture());

        assertThat(rawEventsCaptor.getValue().get(0)).isEqualTo(TWO_EVENTS_1);
        assertThat(rawEventsCaptor.getValue().get(1)).isEqualTo(TWO_EVENTS_2);

        final ArgumentCaptor<EventTypeCursor> coordinatorCursorCaptor = ArgumentCaptor.forClass(EventTypeCursor.class);
        verify(coordinator, times(1)).commit(coordinatorCursorCaptor.capture());

        assertThat(coordinatorCursorCaptor.getAllValues()).extracting("eventTypePartition", "offset").containsExactly(
            Tuple.tuple(EVENT_TYPE_PARTITION, "9"));
    }

    @Test
    public void testKeepAlive() {
        handler.onResponse(KEEP_ALIVE_EVENT);
        verify(delegate, times(0)).onEvent(any(), any());

        final ArgumentCaptor<EventTypeCursor> coordinatorCursorCaptor = ArgumentCaptor.forClass(EventTypeCursor.class);
        verify(coordinator, times(1)).commit(coordinatorCursorCaptor.capture());
        assertThat(coordinatorCursorCaptor.getValue()).extracting("eventTypePartition", "offset").containsExactly(
            EVENT_TYPE_PARTITION, "9");
    }
}
