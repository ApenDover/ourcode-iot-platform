package ts.andrey.eventcollector.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ts.andrey.eventcollector.cassandra.dataService.DeviceEventDataService;
import ts.andrey.eventcollector.mapper.DeviceEventMapper;
import ts.andrey.eventcollector.service.component.SimpleCache;
import ts.andrey.eventcollector.tdf.DummyTDF;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceEventServiceTest {

    @Mock
    private DeviceEventDataService deviceEventDataService;

    @Mock
    private SimpleCache simpleCache;

    @Mock
    private DeviceEventMapper deviceEventMapper;

    @InjectMocks
    private DeviceEventService deviceEventService;

    @Test
    void saveCashedEventsShouldReturnAllEventsWhenNoCachedDevices() {
        // GIVEN
        final var events = DummyTDF.deviceEvent.getList(2);
        final var cachedEventEntities = DummyTDF.deviceEventEntity.getList(0);

        when(simpleCache.contains(anyString())).thenReturn(false);
        when(deviceEventMapper.toEntityList(anyList())).thenReturn(cachedEventEntities);

        // WHEN
        final var result = deviceEventService.saveCashedEvents(events);

        // THEN
        assertThat(result).hasSize(2).containsExactlyElementsOf(events);
        verify(deviceEventDataService).saveAll(cachedEventEntities);
        verify(simpleCache, times(2)).contains(anyString());
    }

    @Test
    void saveCashedEventsShouldReturnOnlyUncachedEventsWhenSomeDevicesCached() {
        // GIVEN
        final var events = DummyTDF.deviceEvent.getList(2);
        final var event1 = events.get(0);
        final var event2 = events.get(1);
        final var cachedEvents = List.of(event1);
        final var cachedEventEntities = DummyTDF.deviceEventEntity.getList(1);

        when(simpleCache.contains("deviceId-0")).thenReturn(true);
        when(simpleCache.contains("deviceId-1")).thenReturn(false);
        when(deviceEventMapper.toEntityList(cachedEvents)).thenReturn(cachedEventEntities);

        // WHEN
        final var result = deviceEventService.saveCashedEvents(events);

        // THEN
        assertThat(result).hasSize(1).containsExactly(event2);
        verify(deviceEventDataService).saveAll(cachedEventEntities);
    }

    @Test
    void saveCashedEventsShouldReturnEmptyListWhenAllDevicesCached() {
        // GIVEN
        final var events = DummyTDF.deviceEvent.getList(2);
        final var cachedEventEntities = DummyTDF.deviceEventEntity.getList(2);

        when(simpleCache.contains(anyString())).thenReturn(true);
        when(deviceEventMapper.toEntityList(events)).thenReturn(cachedEventEntities);

        // WHEN
        final var result = deviceEventService.saveCashedEvents(events);

        // THEN
        assertThat(result).isEmpty();
        verify(deviceEventDataService).saveAll(cachedEventEntities);
    }

    @Test
    void saveCashedEventsShouldSaveCachedEventsToDatabase() {
        // GIVEN
        final var event = DummyTDF.deviceEvent.getDefault();
        final var events = List.of(event);
        final var cachedEventEntities = List.of(DummyTDF.deviceEventEntity.getDefault());

        when(simpleCache.contains(event.getDeviceId())).thenReturn(true);
        when(deviceEventMapper.toEntityList(events)).thenReturn(cachedEventEntities);

        // WHEN
        final var result = deviceEventService.saveCashedEvents(events);

        // THEN
        assertThat(result).isEmpty();
        verify(deviceEventDataService).saveAll(cachedEventEntities);
        verify(deviceEventMapper).toEntityList(events);
    }

    @Test
    void saveCashedEventsShouldNotModifyInputList() {
        // GIVEN
        final var originalEvents = DummyTDF.deviceEvent.getList(3);
        final var inputEvents = new ArrayList<>(originalEvents);
        final var cachedEventEntities = DummyTDF.deviceEventEntity.getList(1);

        when(simpleCache.contains(anyString())).thenReturn(false);
        when(deviceEventMapper.toEntityList(anyList())).thenReturn(cachedEventEntities);

        // WHEN
        deviceEventService.saveCashedEvents(inputEvents);

        // THEN
        assertThat(inputEvents).containsExactlyElementsOf(originalEvents);
    }

}
