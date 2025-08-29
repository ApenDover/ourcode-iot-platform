package ts.andrey.deviceservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ts.andrey.deviceservice.data.dao.DeviceCacheService;
import ts.andrey.deviceservice.service.DeviceService;
import ts.andrey.deviceservice.tdf.DummyTDF;
import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;
import ts.andrey.dto.DeviceUpdateRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DeviceCacheServiceImplTest {

    @Mock
    private DeviceCacheService deviceCacheService;

    @Mock
    private DeviceService deviceDataServiceImpl;

    @InjectMocks
    private DeviceCacheServiceImpl deviceCacheServiceImpl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getDevice_shouldReturnFromCache_whenExists() {
        // GIVEN
        String deviceId = "deviceId";
        Device cachedDevice = DummyTDF.device.getDefault();
        when(deviceCacheService.getDevice(deviceId)).thenReturn(Optional.of(cachedDevice));

        // WHEN
        Device result = deviceCacheServiceImpl.getDevice(deviceId);

        // THEN
        assertEquals(cachedDevice, result);
        verify(deviceCacheService).getDevice(deviceId);
        verifyNoInteractions(deviceDataServiceImpl);
    }

    @Test
    void getDevice_shouldFetchFromDataServiceAndCache_whenNotInCache() {
        // GIVEN
        String deviceId = "deviceId";
        Device deviceFromDb = DummyTDF.device.getDefault();
        when(deviceCacheService.getDevice(deviceId)).thenReturn(Optional.empty());
        when(deviceDataServiceImpl.getDevice(deviceId)).thenReturn(deviceFromDb);

        // WHEN
        Device result = deviceCacheServiceImpl.getDevice(deviceId);

        // THEN
        assertEquals(deviceFromDb, result);
        verify(deviceCacheService).getDevice(deviceId);
        verify(deviceDataServiceImpl).getDevice(deviceId);
        verify(deviceCacheService).saveDevice(deviceFromDb);
    }

    @Test
    void saveDevice_shouldSaveToDataServiceAndCache() {
        // GIVEN
        DeviceCreateRequest request = DummyTDF.deviceCreateRequest.getDefault();
        Device createdDevice = DummyTDF.device.getDefault();
        when(deviceDataServiceImpl.saveDevice(request)).thenReturn(createdDevice);

        // WHEN
        Device result = deviceCacheServiceImpl.saveDevice(request);

        // THEN
        assertEquals(createdDevice, result);
        verify(deviceDataServiceImpl).saveDevice(request);
        verify(deviceCacheService).saveDevice(createdDevice);
    }

    @Test
    void updateDevice_shouldUpdateAndCache() {
        // GIVEN
        String deviceId = "deviceId";
        DeviceUpdateRequest request = DummyTDF.deviceUpdateRequest.getDefault();
        Device updatedDevice = DummyTDF.device.getForUpdate();
        when(deviceDataServiceImpl.updateDevice(deviceId, request)).thenReturn(updatedDevice);

        // WHEN
        Device result = deviceCacheServiceImpl.updateDevice(deviceId, request);

        // THEN
        assertEquals(updatedDevice, result);
        verify(deviceDataServiceImpl).updateDevice(deviceId, request);
        verify(deviceCacheService).saveDevice(updatedDevice);
    }

    @Test
    void deleteDevice_shouldDeleteFromBothServices() {
        // GIVEN
        String deviceId = "deviceId";

        // WHEN
        deviceCacheServiceImpl.deleteDevice(deviceId);

        // THEN
        verify(deviceDataServiceImpl).deleteDevice(deviceId);
        verify(deviceCacheService).deleteDevice(deviceId);
    }
}
