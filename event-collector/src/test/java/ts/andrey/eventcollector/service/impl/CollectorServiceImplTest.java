package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.Device;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ts.andrey.eventcollector.cassandra.dataService.DeviceEventDataService;
import ts.andrey.eventcollector.mapper.DeviceEventMapper;
import ts.andrey.eventcollector.service.DeviceEventProducer;
import ts.andrey.eventcollector.service.component.SimpleCache;
import ts.andrey.eventcollector.tdf.DummyTDF;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectorServiceImplTest {

    @Mock
    private DeviceEventDataService deviceEventDataService;

    @Mock
    private DeviceEventProducer deviceIdProducerImpl;

    @Mock
    private DeviceEventService deviceEventService;

    @Mock
    private DeviceEventMapper deviceEventMapper;

    @Mock
    private SimpleCache simpleCache;

    @InjectMocks
    private CollectorServiceImpl collectorService;

    @Test
    void collectShouldProcessEventsWhenDevicesNotInCacheAndNotInDatabase() {
        // GIVEN
        final var events = List.of(DummyTDF.deviceEvent.getDefault());
        final var devices = List.of(DummyTDF.device.getDefault());
        final var entities = List.of(DummyTDF.deviceEventEntity.getDefault());
        final var deviceIds = List.of("deviceId");

        when(deviceEventService.saveCashedEvents(events)).thenReturn(events);
        when(deviceEventMapper.toDeviceIdList(events)).thenReturn(devices);
        when(deviceEventMapper.toEntityList(events)).thenReturn(entities);
        when(deviceEventDataService.isExistDeviceId(anyString())).thenReturn(false);

        // WHEN
        collectorService.collect(events);

        // THEN
        verify(deviceEventService).saveCashedEvents(events);
        verify(deviceEventMapper).toDeviceIdList(events);
        verify(deviceEventMapper).toEntityList(events);
        verify(deviceEventDataService).isExistDeviceId("deviceId");
        verify(deviceIdProducerImpl).sendEvents(devices);
        verify(simpleCache).putAll(deviceIds);
        verify(deviceEventDataService).saveAll(entities);
    }

    @Test
    void collectShouldNotSendEventsWhenDevicesAlreadyInDatabase() {
        // GIVEN
        final var event = DummyTDF.deviceEvent.getDefault();
        final var events = List.of(event);
        final var device = DummyTDF.device.getDefault();
        final var devices = List.of(device);
        final var entities = List.of(DummyTDF.deviceEventEntity.getDefault());

        when(deviceEventService.saveCashedEvents(events)).thenReturn(events);
        when(deviceEventMapper.toDeviceIdList(events)).thenReturn(devices);
        when(deviceEventMapper.toEntityList(events)).thenReturn(entities);
        when(deviceEventDataService.isExistDeviceId(device.getDeviceId())).thenReturn(true);

        // WHEN
        collectorService.collect(events);

        // THEN
        verify(deviceIdProducerImpl, never()).sendEvents(anyList());
        verify(simpleCache).putAll(Collections.emptyList()); // Ожидаем пустой список
        verify(deviceEventDataService).saveAll(entities);
    }

    @Test
    void collectShouldNotSendEventsWhenNoUnsavedDevices() {
        // GIVEN
        final var events = List.of(DummyTDF.deviceEvent.getDefault());
        final var devices = Collections.<Device>emptyList();
        final var entities = List.of(DummyTDF.deviceEventEntity.getDefault());

        when(deviceEventService.saveCashedEvents(events)).thenReturn(events);
        when(deviceEventMapper.toDeviceIdList(events)).thenReturn(devices);
        when(deviceEventMapper.toEntityList(events)).thenReturn(entities);

        // WHEN
        collectorService.collect(events);

        // THEN
        verify(deviceIdProducerImpl, never()).sendEvents(anyList());
        verify(deviceEventDataService).saveAll(entities);
    }

    @Test
    void collectShouldLogErrorWhenExceptionThrown() {
        // GIVEN
        final var events = List.of(DummyTDF.deviceEvent.getDefault());
        final var exception = new RuntimeException("Test exception");

        when(deviceEventService.saveCashedEvents(events)).thenThrow(exception);

        // WHEN
        collectorService.collect(events);

        // THEN
        verify(deviceIdProducerImpl, never()).sendEvents(anyList());
        verify(simpleCache, never()).putAll(anyList());
        verify(deviceEventDataService, never()).saveAll(anyList());
    }

    @Test
    void collectShouldProcessBatchWhenMultipleEventsProvided() {
        // GIVEN
        final var events = DummyTDF.deviceEvent.getList(3);
        final var devices = DummyTDF.device.getList(3);
        final var entities = DummyTDF.deviceEventEntity.getList(3);
        final var deviceIds = devices
                .stream()
                .map(Device::getDeviceId)
                .toList();

        when(deviceEventService.saveCashedEvents(events)).thenReturn(events);
        when(deviceEventMapper.toDeviceIdList(events)).thenReturn(devices);
        when(deviceEventMapper.toEntityList(events)).thenReturn(entities);
        when(deviceEventDataService.isExistDeviceId(anyString())).thenReturn(false);

        // WHEN
        collectorService.collect(events);

        // THEN
        verify(deviceIdProducerImpl).sendEvents(devices);
        verify(simpleCache).putAll(deviceIds);
        verify(deviceEventDataService).saveAll(entities);
    }

}
