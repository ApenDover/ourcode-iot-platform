package ts.andrey.deviceservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ts.andrey.deviceservice.data.dao.DeviceDbService;
import ts.andrey.deviceservice.mapper.DeviceMapper;
import ts.andrey.deviceservice.metrics.DeviceMetrics;
import ts.andrey.deviceservice.tdf.DummyTDF;
import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceUpdateRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceDataServiceImplTest {

    @Mock
    private DeviceMetrics deviceMetrics;

    @Mock
    private DeviceDbService deviceDbService;

    @Mock
    private DeviceMapper deviceMapper;

    @InjectMocks
    private DeviceDataServiceImpl deviceDataServiceImpl;

    @Test
    void getDeviceShouldReturnMappedDeviceAndRecordMetrics() {
        // GIVEN
        String deviceId = "deviceId";
        var entity = DummyTDF.deviceEntity.getDefault();
        var mapped = DummyTDF.device.getDefault();
        when(deviceDbService.getDeviceByDeviceId(deviceId)).thenReturn(entity);
        when(deviceMapper.toDevice(entity)).thenReturn(mapped);

        // WHEN
        Device result = deviceDataServiceImpl.getDevice(deviceId);

        // THEN
        assertEquals(mapped, result);
        verify(deviceDbService).getDeviceByDeviceId(deviceId);
        verify(deviceMetrics).getDeviceSuccess();
        verify(deviceMapper).toDevice(entity);
    }

    @Test
    void saveDeviceShouldPersistAndReturnMappedDeviceAndRecordMetrics() {
        // GIVEN
        var request = DummyTDF.deviceCreateRequest.getDefault();
        var entity = DummyTDF.deviceEntity.getDefault();
        var created = DummyTDF.deviceEntity.getDefault();
        var mapped = DummyTDF.device.getDefault();
        when(deviceMapper.createDeviceEntity(request)).thenReturn(entity);
        when(deviceDbService.save(entity)).thenReturn(created);
        when(deviceMapper.toDevice(created)).thenReturn(mapped);

        // WHEN
        Device result = deviceDataServiceImpl.saveDevice(request);

        // THEN
        assertEquals(mapped, result);
        verify(deviceMapper).createDeviceEntity(request);
        verify(deviceDbService).save(entity);
        verify(deviceMetrics).createDeviceSuccess();
        verify(deviceMapper).toDevice(created);
    }

    @Test
    void updateDeviceShouldUpdateTypeAndMeta() {
        // GIVEN
        String deviceId = "deviceId";
        var request = DummyTDF.deviceUpdateRequest.getDefault();
        var updatedEntity = DummyTDF.deviceEntity.getUpdatedTypeMeta();
        var mapped = DummyTDF.device.getForUpdate();
        when(deviceDbService.updateTypeMeta(deviceId, request.getDeviceType(), request.getMeta()))
                .thenReturn(updatedEntity);
        when(deviceMapper.toDevice(updatedEntity)).thenReturn(mapped);

        // WHEN
        Device result = deviceDataServiceImpl.updateDevice(deviceId, request);

        // THEN
        assertEquals(mapped, result);
        verify(deviceDbService).updateTypeMeta(deviceId, request.getDeviceType(), request.getMeta());
        verify(deviceMetrics).updateDeviceSuccess();
        verify(deviceMapper).toDevice(updatedEntity);
    }

    @Test
    void updateDeviceShouldUpdateOnlyType() {
        // GIVEN
        String deviceId = "deviceId";
        var request = new DeviceUpdateRequest();
        request.setDeviceType("updatedType");
        var updatedEntity = DummyTDF.deviceEntity.getUpdatedType();
        var mapped = DummyTDF.device.getForUpdate();
        when(deviceDbService.updateType(deviceId, request.getDeviceType())).thenReturn(updatedEntity);
        when(deviceMapper.toDevice(updatedEntity)).thenReturn(mapped);

        // WHEN
        Device result = deviceDataServiceImpl.updateDevice(deviceId, request);

        // THEN
        assertEquals(mapped, result);
        verify(deviceDbService).updateType(deviceId, request.getDeviceType());
        verify(deviceMetrics).updateDeviceSuccess();
        verify(deviceMapper).toDevice(updatedEntity);
    }

    @Test
    void updateDeviceShouldUpdateOnlyMeta() {
        // GIVEN
        String deviceId = "deviceId";
        var request = new DeviceUpdateRequest();
        request.setMeta("updatedMeta");
        var updatedEntity = DummyTDF.deviceEntity.getUpdatedMeta();
        var mapped = DummyTDF.device.getForUpdate();
        when(deviceDbService.updateMeta(deviceId, request.getMeta())).thenReturn(updatedEntity);
        when(deviceMapper.toDevice(updatedEntity)).thenReturn(mapped);

        // WHEN
        Device result = deviceDataServiceImpl.updateDevice(deviceId, request);

        // THEN
        assertEquals(mapped, result);
        verify(deviceDbService).updateMeta(deviceId, request.getMeta());
        verify(deviceMetrics).updateDeviceSuccess();
        verify(deviceMapper).toDevice(updatedEntity);
    }

    @Test
    void updateDeviceShouldReturnCurrentDeviceWhenNoFieldsProvided() {
        // GIVEN
        String deviceId = "deviceId";
        var emptyRequest = new DeviceUpdateRequest(); // ничего не задано
        var entity = DummyTDF.deviceEntity.getDefault();
        var mapped = DummyTDF.device.getDefault();
        when(deviceDbService.getDeviceByDeviceId(deviceId)).thenReturn(entity);
        when(deviceMapper.toDevice(entity)).thenReturn(mapped);

        // WHEN
        Device result = deviceDataServiceImpl.updateDevice(deviceId, emptyRequest);

        // THEN
        assertEquals(mapped, result);
        verify(deviceDbService).getDeviceByDeviceId(deviceId);
        verify(deviceMapper).toDevice(entity);
        verifyNoInteractions(deviceMetrics);
    }

    @Test
    void deleteDeviceShouldDeleteAndRecordMetrics() {
        // GIVEN
        String deviceId = "deviceId";

        // WHEN
        deviceDataServiceImpl.deleteDevice(deviceId);

        // THEN
        verify(deviceDbService).deleteByDeviceId(deviceId);
        verify(deviceMetrics).deleteDeviceSuccess();
    }

}
