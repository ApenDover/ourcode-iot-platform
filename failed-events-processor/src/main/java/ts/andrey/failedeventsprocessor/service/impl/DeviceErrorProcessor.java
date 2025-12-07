package ts.andrey.failedeventsprocessor.service.impl;

import com.nashkod.avro.DeviceError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ts.andrey.failedeventsprocessor.metrics.ErrorType;
import ts.andrey.failedeventsprocessor.metrics.FailEventMetrics;
import ts.andrey.failedeventsprocessor.service.ErrorProcessor;
import ts.andrey.failedeventsprocessor.service.MinioUploader;
import ts.andrey.failedeventsprocessor.utils.JsonAvroConverter;
import ts.andrey.failedeventsprocessor.utils.MinioNameGenerator;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceErrorProcessor implements ErrorProcessor<DeviceError> {

    private final MinioUploader minioUploader;

    private final FailEventMetrics metrics;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public void start(DeviceError deviceError) {
        log.debug("Starting error handler service for {}", deviceError);
        final var name = MinioNameGenerator.generateName(deviceError.getReceivedAt(),
                deviceError.getErrorMeta().getErrorSource().name());
        final var bytes = JsonAvroConverter.toJsonAvro(deviceError);
        minioUploader.uploadToMinio(name, bytes, bucketName);
        metrics.recordProcessed(ErrorType.DEVICE_ERROR);
    }

}
