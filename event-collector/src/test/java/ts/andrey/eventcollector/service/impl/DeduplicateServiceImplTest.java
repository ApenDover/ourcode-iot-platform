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
    void shouldReturnEmptyListWhenInputIsNull() {
        // WHEN
        final var result = deduplicateService.getUniqueDevices(null);

        // THEN
        assertTrue(result.isEmpty());
    }

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
        final var input = new ArrayList<>(
                List.of(new Device("id1"),
                        new Device("id2"))
        );
        input.add(null);

        Mockito.when(cache.contains(Mockito.anyString())).thenReturn(false);
        Mockito.when(dataService.getUnsavedDeviceIds(anyList())).thenReturn(List.of("id1", "id2"));

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
                        new Device("id1"),
                        new Device(""),
                        new Device(null),
                        new Device("id2")
                )
        );

        Mockito.when(cache.contains(Mockito.anyString())).thenReturn(false);
        Mockito.when(dataService.getUnsavedDeviceIds(anyList())).thenReturn(List.of("id1", "id2"));

        // WHEN
        final var result = deduplicateService.getUniqueDevices(input);

        // THEN
        assertEquals(2, result.size());
    }

    @Test
    void shouldFilterDevicesInCache() {
        // GIVEN
        final var cachedDevice = new Device("cached");
        final var uncachedDevice = new Device("uncached");

        Mockito.when(cache.contains("cached")).thenReturn(true);
        Mockito.when(cache.contains("uncached")).thenReturn(false);
        Mockito.when(dataService.getUnsavedDeviceIds(List.of("uncached"))).thenReturn(List.of("uncached"));

        // WHEN
        final var result = deduplicateService.getUniqueDevices(List.of(cachedDevice, uncachedDevice));

        // THEN
        assertEquals(1, result.size());
        assertEquals("uncached", result.get(0).getDeviceId());
    }

    @Test
    void shouldFilterDevicesInDatabase() {
        // GIVEN
        final var newDevice = new Device("new");
        final var existingDevice = new Device("existing");

        Mockito.when(cache.contains(Mockito.anyString())).thenReturn(false);
        Mockito.when(dataService.getUnsavedDeviceIds(List.of("new", "existing")))
                .thenReturn(List.of("new"));

        // WHEN
        final var result = deduplicateService.getUniqueDevices(List.of(newDevice, existingDevice));

        // THEN
        assertEquals(1, result.size());
        assertEquals("new", result.get(0).getDeviceId());
    }

    @Test
    void shouldReturnOnlyDevicesNotInCacheAndNotInDb() {
        // GIVEN
        final var device1 = new Device("id1");
        final var device2 = new Device("id2");
        final var device3 = new Device("id3");

        Mockito.when(cache.contains("id1")).thenReturn(true);
        Mockito.when(cache.contains("id2")).thenReturn(false);
        Mockito.when(cache.contains("id3")).thenReturn(false);

        Mockito.when(dataService.getUnsavedDeviceIds(List.of("id2", "id3")))
                .thenReturn(List.of("id3"));

        // WHEN
        final var result = deduplicateService.getUniqueDevices(List.of(device1, device2, device3));

        // THEN
        assertEquals(1, result.size());
        assertEquals("id3", result.get(0).getDeviceId());
    }

    @Test
    void shouldDeduplicateDeviceIds() {
        // GIVEN
        final var device1 = new Device("id1");
        final var device2 = new Device("id1");

        Mockito.when(cache.contains("id1")).thenReturn(false);
        Mockito.when(dataService.getUnsavedDeviceIds(List.of("id1")))
                .thenReturn(List.of("id1"));

        // WHEN
        final var result = deduplicateService.getUniqueDevices(List.of(device1, device2));

        // THEN
        assertEquals(1, result.size());
    }

}
