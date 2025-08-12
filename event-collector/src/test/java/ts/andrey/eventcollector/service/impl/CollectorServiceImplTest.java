package ts.andrey.eventcollector.service.impl;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectorServiceImplTest {

    @Mock
    private DeviceEventDataService deviceEventDataService;

    @Mock
    private DeviceEventProducer deviceEventProducer;

    @Mock
    private DeviceEventMapper deviceEventMapper;

    @Mock
    private SimpleCache simpleCache;

    @InjectMocks
    private CollectorServiceImpl collectorService;

    @Test
    void shouldSaveEventWhenDeviceCached() {
        // GIVEN
        final var event = DummyTDF.deviceEvent.getDefault();
        final var device = DummyTDF.device.getDefault();
        final var entity = DummyTDF.deviceEventEntity.getDefault();

        when(deviceEventMapper.toDeviceId(event)).thenReturn(device);
        when(deviceEventMapper.toEntity(event)).thenReturn(entity);
        when(simpleCache.contains("deviceId")).thenReturn(true);

        // WHEN
        collectorService.collect(event);

        // THEN
        verify(deviceEventDataService).save(entity);
        verify(deviceEventProducer, never()).sendEvent(any());
        verify(simpleCache, never()).put(any());
    }

    @Test
    void shouldSendEventAndSaveWhenDeviceNotExists() {
        // GIVEN
        final var event = DummyTDF.deviceEvent.getDefault();
        final var device = DummyTDF.device.getDefault();
        final var entity = DummyTDF.deviceEventEntity.getDefault();

        when(deviceEventMapper.toDeviceId(event)).thenReturn(device);
        when(deviceEventMapper.toEntity(event)).thenReturn(entity);
        when(simpleCache.contains("deviceId")).thenReturn(false);
        when(deviceEventDataService.isExistDeviceId("deviceId")).thenReturn(false);

        // WHEN
        collectorService.collect(event);

        // THEN
        verify(deviceEventProducer).sendEvent(device);
        verify(simpleCache).put("deviceId");
        verify(deviceEventDataService).save(entity);
    }

    @Test
    void shouldNotSendEventWhenDeviceExists() {
        // GIVEN
        final var event = DummyTDF.deviceEvent.getDefault();
        final var device = DummyTDF.device.getDefault();
        final var entity = DummyTDF.deviceEventEntity.getDefault();

        when(deviceEventMapper.toDeviceId(event)).thenReturn(device);
        when(deviceEventMapper.toEntity(event)).thenReturn(entity);
        when(simpleCache.contains("deviceId")).thenReturn(false);
        when(deviceEventDataService.isExistDeviceId("deviceId")).thenReturn(true);

        // WHEN
        collectorService.collect(event);

        // THEN
        verify(deviceEventProducer, never()).sendEvent(any());
        verify(simpleCache).put("deviceId");
        verify(deviceEventDataService).save(entity);
    }

    @Test
    void shouldLogErrorWhenExceptionOccurs() {
        // GIVEN
        final var event = DummyTDF.deviceEvent.getDefault();
        when(deviceEventMapper.toDeviceId(event)).thenThrow(new RuntimeException("Test error"));

        // WHEN
        collectorService.collect(event);

        // THEN
        verify(deviceEventDataService, never()).save(any());
        verify(deviceEventProducer, never()).sendEvent(any());
        verify(simpleCache, never()).put(any());
    }

    @Test
    void shouldCacheDeviceAfterFirstSave() {
        // GIVEN
        final var event = DummyTDF.deviceEvent.getDefault();
        final var device = DummyTDF.device.getDefault();
        final var entity = DummyTDF.deviceEventEntity.getDefault();

        when(deviceEventMapper.toDeviceId(event)).thenReturn(device);
        when(deviceEventMapper.toEntity(event)).thenReturn(entity);
        when(simpleCache.contains("deviceId")).thenReturn(false, true); // Первый вызов false, второй true
        when(deviceEventDataService.isExistDeviceId("deviceId")).thenReturn(false);

        collectorService.collect(event);
        collectorService.collect(event);

        // THEN
        verify(deviceEventProducer, times(1)).sendEvent(device);
        verify(simpleCache, times(1)).put("deviceId");
        verify(deviceEventDataService, times(2)).save(entity);
    }

}
