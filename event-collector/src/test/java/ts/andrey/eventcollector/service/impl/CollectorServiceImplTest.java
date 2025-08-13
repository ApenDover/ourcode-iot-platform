package ts.andrey.eventcollector.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ts.andrey.eventcollector.mapper.DeviceEventMapper;
import ts.andrey.eventcollector.service.DeviceEventService;
import ts.andrey.eventcollector.service.DeviceService;
import ts.andrey.eventcollector.tdf.DummyTDF;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectorServiceImplTest {

    @Mock
    private DeviceEventService deviceEventService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private DeviceEventMapper deviceEventMapper;
    @InjectMocks
    private CollectorServiceImpl collectorService;

    @Test
    void collectShouldProcessUncachedEvents() {
        // GIVEN
        final var events = DummyTDF.deviceEvent.getList(2);
        final var uncachedEvents = List.of(events.get(1));
        final var uncachedDeviceIds = DummyTDF.device.getList(1);

        when(deviceEventService.saveCashedEvents(events)).thenReturn(uncachedEvents);
        when(deviceEventMapper.toDeviceIdList(uncachedEvents)).thenReturn(uncachedDeviceIds);

        // WHEN
        collectorService.collect(events);

        // THEN
        verify(deviceEventService).saveCashedEvents(events);
        verify(deviceService).process(uncachedDeviceIds);
        verify(deviceEventService).saveEvents(uncachedEvents);
    }

    @Test
    void collectShouldHandleAllCachedEvents() {
        // GIVEN
        final var events = DummyTDF.deviceEvent.getList(2);
        when(deviceEventService.saveCashedEvents(events)).thenReturn(List.of());

        // WHEN
        collectorService.collect(events);

        // THEN
        verify(deviceEventService).saveCashedEvents(events);
        verify(deviceEventService).saveEvents(List.of());
    }

    @Test
    void collectShouldHandleEmptyInput() {
        // WHEN
        collectorService.collect(List.of());

        // THEN
        verify(deviceEventService, never()).saveCashedEvents(any());
        verify(deviceService, never()).process(any());
        verify(deviceEventService, never()).saveEvents(any());
    }

    @Test
    void collectShouldLogErrorOnFailure() {
        // GIVEN
        final var events = DummyTDF.deviceEvent.getList(1);
        when(deviceEventService.saveCashedEvents(events)).thenThrow(new RuntimeException("DB error"));

        // WHEN
        collectorService.collect(events);

        // THEN
        verify(deviceService, never()).process(any());
        verify(deviceEventService, never()).saveEvents(any());
    }

}
