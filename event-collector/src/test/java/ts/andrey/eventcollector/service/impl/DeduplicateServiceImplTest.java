package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.Device;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ts.andrey.eventcollector.data.dao.DeviceDataService;
import ts.andrey.eventcollector.mapper.DeviceEventMapper;
import ts.andrey.eventcollector.service.component.SimpleCache;
import ts.andrey.eventcollector.tdf.DummyTDF;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeduplicateServiceImplTest {

    @Mock
    private DeviceDataService deviceDataService;

    @Mock
    private DeviceEventMapper deviceEventMapper;

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
    void shouldFilterDevicesWithEmptyId() {
        // GIVEN
        final var input = new ArrayList<>(
                List.of(
                        new Device("deviceId-0", "type", "meta", Instant.ofEpochMilli(312L)),
                        new Device("deviceId-1", "type", "meta", Instant.ofEpochMilli(312L)),
                        new Device("", "type", "meta", Instant.ofEpochMilli(312L)),
                        new Device(null, "type", "meta", Instant.ofEpochMilli(312L))
                )
        );

        when(cache.contains(Mockito.anyString())).thenReturn(false);
        when(deviceDataService.getUnsavedDeviceIds(anyList())).thenReturn(DummyTDF.deviceEntity.getList(2));

        // WHEN
        final var result = deduplicateService.getUniqueDevices(input);

        // THEN
        assertEquals(2, result.size());
    }

    @Test
    void shouldFilterDevicesInCache() {
        // GIVEN
        final var cachedDevice = DummyTDF.device.getDefault(0);
        final var uncachedDevice = DummyTDF.device.getDefault(1);

        when(cache.contains("deviceId-0")).thenReturn(true);
        when(cache.contains("deviceId-1")).thenReturn(false);

        when(deviceEventMapper.deviceToEntityList(List.of(uncachedDevice)))
                .thenReturn(List.of(DummyTDF.deviceEntity.getDefault(1)));
        when(deviceDataService.getUnsavedDeviceIds(
                List.of(DummyTDF.deviceEntity.getDefault(1))))
                .thenReturn(List.of(DummyTDF.deviceEntity.getDefault(1)));

        // WHEN
        final var result = deduplicateService.getUniqueDevices(List.of(cachedDevice, uncachedDevice));

        // THEN
        assertEquals(1, result.size());
        assertEquals("deviceId-1", result.get(0).getDeviceId());
    }

    @Test
    void shouldFilterDevicesInDatabase() {
        // GIVEN
        final var newDevice = DummyTDF.device.getDefault(0);
        final var existingDevice = DummyTDF.device.getDefault(1);

        when(cache.contains(Mockito.anyString())).thenReturn(false);

        when(deviceEventMapper.deviceToEntityList(List.of(newDevice, existingDevice)))
                .thenReturn(DummyTDF.deviceEntity.getList(2));

        when(deviceDataService.getUnsavedDeviceIds(DummyTDF.deviceEntity.getList(2)))
                .thenReturn(List.of(DummyTDF.deviceEntity.getDefault(1)));

        // WHEN
        final var result = deduplicateService.getUniqueDevices(List.of(newDevice, existingDevice));

        // THEN
        assertEquals(1, result.size());
        assertEquals("deviceId-1", result.get(0).getDeviceId());
    }

    @Test
    void shouldReturnOnlyDevicesNotInCacheAndNotInDb() {
        // GIVEN
        final var device1 = DummyTDF.device.getDefault(0);
        final var device2 = DummyTDF.device.getDefault(1);
        final var device3 = DummyTDF.device.getDefault(2);

        when(cache.contains("deviceId-0")).thenReturn(false);
        when(cache.contains("deviceId-1")).thenReturn(false);
        when(cache.contains("deviceId-2")).thenReturn(true);

        when(deviceEventMapper.deviceToEntityList(List.of(device1, device2)))
                .thenReturn(DummyTDF.deviceEntity.getList(2));

        when(deviceDataService.getUnsavedDeviceIds(
                DummyTDF.deviceEntity.getList(2)))
                .thenReturn(List.of(DummyTDF.deviceEntity.getDefault(1)));

        // WHEN
        final var result = deduplicateService.getUniqueDevices(List.of(device1, device2, device3));

        // THEN
        assertEquals(1, result.size());
        assertEquals("deviceId-1", result.get(0).getDeviceId());
    }

}
