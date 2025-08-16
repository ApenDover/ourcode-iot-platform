package ts.andrey.devicecollector.service.impl;

import com.nashkod.avro.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.devicecollector.mapper.DeviceMapper;
import ts.andrey.devicecollector.postgres.dao.DeviceDataService;
import ts.andrey.devicecollector.service.DeviceService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceDataService deviceDataService;
    private final DeviceMapper deviceMapper;

    @Override
    public List<Device> createOrUpdateDevice(List<Device> devices) {
        final var saved = deviceDataService.saveAll(devices);
        return deviceMapper.toDeviceList(saved);
    }

}
