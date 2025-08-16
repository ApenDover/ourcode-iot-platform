package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.Device;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ts.andrey.eventcollector.cassandra.dao.DeviceEventDataService;
import ts.andrey.eventcollector.service.component.SimpleCache;
import ts.andrey.eventcollector.tdf.DummyTDF;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;

@ExtendWith(MockitoExtension.class)
class DeduplicateServiceImplTest {

    @Mock
    private DeviceEventDataService dataService;

    @Mock
    private SimpleCache cache;

    @InjectMocks
    private DeduplicateServiceImpl deduplicateService;

    @Test
    void shouldReturnEmptyListWhenInputIsEmpty() {
        // WHEN
        final var result = deduplicateService.getUniqueDevices(List.of());

        // THEN
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFilterNullDevices() {
        // GIVEN
        final var input = DummyTDF.device.getList(2);
        input.add(null);

        Mockito.when(cache.contains(Mockito.anyString())).thenReturn(false);
        Mockito.when(dataService.getUnsavedDeviceIds(anyList())).thenReturn(List.of("deviceId-0", "deviceId-1"));

        // WHEN
        final var result = deduplicateService.getUniqueDevices(input);

        // THEN
        assertEquals(2, result.size());
    }

    @Test
    void shouldFilterDevicesWithEmptyId() {
        // GIVEN
        final var input = new ArrayList<>(
                List.of(
                        new Device("deviceId-1", "type", "meta", Instant.ofEpochMilli(312L)),
                        new Device("deviceId-2", "type", "meta", Instant.ofEpochMilli(312L)),
                        new Device("", "type", "meta", Instant.ofEpochMilli(312L)),
                        new Device(null, "type", "meta", Instant.ofEpochMilli(312L))
                )
        );

        Mockito.when(cache.contains(Mockito.anyString())).thenReturn(false);
        Mockito.when(dataService.getUnsavedDeviceIds(anyList())).thenReturn(List.of("deviceId-1", "deviceId-2"));

        // WHEN
        final var result = deduplicateService.getUniqueDevices(input);

        // THEN
        assertEquals(2, result.size());
    }

    @Test
    void shouldFilterDevicesInCache() {
        // GIVEN
        final var cachedDevice = DummyTDF.device.getDefault(1);
        final var uncachedDevice = DummyTDF.device.getDefault(2);

        Mockito.when(cache.contains("deviceId-1")).thenReturn(true);
        Mockito.when(cache.contains("deviceId-2")).thenReturn(false);
        Mockito.when(dataService.getUnsavedDeviceIds(List.of("deviceId-2"))).thenReturn(List.of("deviceId-2"));

        // WHEN
        final var result = deduplicateService.getUniqueDevices(List.of(cachedDevice, uncachedDevice));

        // THEN
        assertEquals(1, result.size());
        assertEquals("deviceId-2", result.get(0).getDeviceId());
    }

    @Test
    void shouldFilterDevicesInDatabase() {
        // GIVEN
        final var newDevice = DummyTDF.device.getDefault(1);
        final var existingDevice = DummyTDF.device.getDefault(2);

        Mockito.when(cache.contains(Mockito.anyString())).thenReturn(false);
        Mockito.when(dataService.getUnsavedDeviceIds(List.of("deviceId-1", "deviceId-2")))
                .thenReturn(List.of("deviceId-1"));

        // WHEN
        final var result = deduplicateService.getUniqueDevices(List.of(newDevice, existingDevice));

        // THEN
        assertEquals(1, result.size());
        assertEquals("deviceId-1", result.get(0).getDeviceId());
    }

    @Test
    void shouldReturnOnlyDevicesNotInCacheAndNotInDb() {
        // GIVEN
        final var device1 = DummyTDF.device.getDefault(1);
        final var device2 = DummyTDF.device.getDefault(2);
        final var device3 = DummyTDF.device.getDefault(3);

        Mockito.when(cache.contains("deviceId-1")).thenReturn(true);
        Mockito.when(cache.contains("deviceId-2")).thenReturn(false);
        Mockito.when(cache.contains("deviceId-3")).thenReturn(false);

        Mockito.when(dataService.getUnsavedDeviceIds(List.of("deviceId-2", "deviceId-3")))
                .thenReturn(List.of("deviceId-3"));

        // WHEN
        final var result = deduplicateService.getUniqueDevices(List.of(device1, device2, device3));

        // THEN
        assertEquals(1, result.size());
        assertEquals("deviceId-3", result.get(0).getDeviceId());
    }

    @Test
    void shouldDeduplicateDeviceIds() {
        // GIVEN
        final var device1 = DummyTDF.device.getDefault(1);
        final var device2 = DummyTDF.device.getDefault(2);

        Mockito.when(cache.contains("deviceId-1")).thenReturn(false);
        Mockito.when(cache.contains("deviceId-2")).thenReturn(true);
        Mockito.when(dataService.getUnsavedDeviceIds(List.of("deviceId-1")))
                .thenReturn(List.of("deviceId-1"));

        // WHEN
        final var result = deduplicateService.getUniqueDevices(List.of(device1, device2));

        // THEN
        assertEquals(1, result.size());
    }

}
