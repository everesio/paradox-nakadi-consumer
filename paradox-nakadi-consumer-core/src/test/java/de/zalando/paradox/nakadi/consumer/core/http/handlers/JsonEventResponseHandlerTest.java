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

import org.assertj.core.groups.Tuple;

import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.zalando.paradox.nakadi.consumer.core.DefaultObjectMapper;
import de.zalando.paradox.nakadi.consumer.core.domain.EventTypeCursor;
import de.zalando.paradox.nakadi.consumer.core.partitioned.PartitionCoordinator;

public class JsonEventResponseHandlerTest {

    @Mock
    private PartitionCoordinator coordinator;

    @Mock
    private JsonEventHandler delegate;

    @Captor
    private ArgumentCaptor<EventTypeCursor> eventCursorCaptor;

    @Captor
    private ArgumentCaptor<JsonNode> jsonEventCaptor;

    private ObjectMapper objectMapper = new DefaultObjectMapper().jacksonObjectMapper();

    private JsonEventResponseHandler handler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.handler = new JsonEventResponseHandler(EVENT_TYPE_PARTITION, OBJECT_MAPPER, coordinator, delegate);
    }

    @Test
    public void testOneEvent() throws Exception {

        new DefaultObjectMapper().jacksonObjectMapper();

        handler.onResponse(ONE_EVENT);

        verify(delegate, times(1)).onEvent(eventCursorCaptor.capture(), jsonEventCaptor.capture());
        assertThat(eventCursorCaptor.getValue()).extracting("eventTypePartition", "offset").containsExactly(
            EVENT_TYPE_PARTITION, "5");

        assertThat(objectMapper.writeValueAsString(jsonEventCaptor.getValue())).isEqualTo(ONE_EVENT_1);

        final ArgumentCaptor<EventTypeCursor> coordinatorCursorCaptor = ArgumentCaptor.forClass(EventTypeCursor.class);
        verify(coordinator, times(1)).commit(coordinatorCursorCaptor.capture());
        assertThat(coordinatorCursorCaptor.getValue()).extracting("eventTypePartition", "offset").containsExactly(
            EVENT_TYPE_PARTITION, "5");
    }

    @Test
    public void testTwoEvents() throws Exception {
        handler.onResponse(TWO_EVENTS);

        verify(delegate, times(2)).onEvent(eventCursorCaptor.capture(), jsonEventCaptor.capture());

        assertThat(objectMapper.writeValueAsString(jsonEventCaptor.getAllValues().get(0))).isEqualTo(TWO_EVENTS_1);
        assertThat(objectMapper.writeValueAsString(jsonEventCaptor.getAllValues().get(1))).isEqualTo(TWO_EVENTS_2);

        final ArgumentCaptor<EventTypeCursor> coordinatorCursorCaptor = ArgumentCaptor.forClass(EventTypeCursor.class);
        verify(coordinator, times(2)).commit(coordinatorCursorCaptor.capture());

        assertThat(coordinatorCursorCaptor.getAllValues()).extracting("eventTypePartition", "offset").containsExactly(
            Tuple.tuple(EVENT_TYPE_PARTITION, "8"), Tuple.tuple(EVENT_TYPE_PARTITION, "9"));
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
