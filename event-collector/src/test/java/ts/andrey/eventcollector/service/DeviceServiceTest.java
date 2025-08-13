package ts.andrey.eventcollector.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.CollectionUtils;
import ts.andrey.eventcollector.cassandra.dataService.DeviceEventDataService;
import ts.andrey.eventcollector.service.component.SimpleCache;
import ts.andrey.eventcollector.tdf.DummyTDF;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceEventDataService deviceEventDataService;

    @Mock
    private DeviceEventProducer deviceIdProducerImpl;

    @Mock
    private SimpleCache simpleCache;

    @InjectMocks
    private DeviceService deviceService;

    @Test
    void processShouldReturnEmptyListWhenAllDevicesExist() {
        // GIVEN
        var devices = DummyTDF.device.getList(1);

        when(deviceEventDataService.isExistDeviceId(devices.get(0).getDeviceId())).thenReturn(true);

        // WHEN
        var result = deviceService.process(devices);

        // THEN
        assertTrue(result.isEmpty());
        verify(deviceIdProducerImpl, never()).send(anyList());
        verify(simpleCache, never()).putAll(anyList());
    }

    @Test
    void processShouldSendToKafkaAndCacheWhenNewDevicesFound() {
        // GIVEN
        var devices = DummyTDF.device.getList(2);

        when(deviceEventDataService.isExistDeviceId(devices.get(0).getDeviceId())).thenReturn(false);
        when(deviceEventDataService.isExistDeviceId(devices.get(1).getDeviceId())).thenReturn(false);

        // WHEN
        var result = deviceService.process(devices);

        // THEN
        assertEquals(2, result.size());
        assertTrue(result.containsAll(devices));

        verify(deviceIdProducerImpl).send(devices);
        verify(simpleCache).putAll(List.of(devices.get(0).getDeviceId(), devices.get(1).getDeviceId()));
    }

    @Test
    void processShouldNotSendToKafkaWhenNoNewDevices() {
        // GIVEN
        var devices = DummyTDF.device.getList(1);

        when(deviceEventDataService.isExistDeviceId(devices.get(0).getDeviceId())).thenReturn(true);

        // WHEN
        var result = deviceService.process(devices);

        // THEN
        assertTrue(CollectionUtils.isEmpty(result));
        verify(deviceIdProducerImpl, never()).send(anyList());
        verify(simpleCache, never()).putAll(anyList());
    }

    @Test
    void processShouldHandleEmptyInput() {
        // WHEN
        var result = deviceService.process(List.of());

        // THEN
        assertTrue(result.isEmpty());
        verify(deviceEventDataService, never()).isExistDeviceId(anyString());
        verify(deviceIdProducerImpl, never()).send(anyList());
        verify(simpleCache, never()).putAll(anyList());
    }

}
