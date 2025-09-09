package ts.andrey.failedeventsprocessor.service.impl;

import com.nashkod.avro.DeviceEventError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ts.andrey.failedeventsprocessor.metrics.ErrorType;
import ts.andrey.failedeventsprocessor.metrics.GlobalMetrics;
import ts.andrey.failedeventsprocessor.service.ErrorProcessor;
import ts.andrey.failedeventsprocessor.service.MinioUploader;
import ts.andrey.failedeventsprocessor.utils.JsonAvroConverter;
import ts.andrey.failedeventsprocessor.utils.MinioNameGenerator;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventsErrorProcessor implements ErrorProcessor<DeviceEventError> {

    private final MinioUploader minioUploader;

    private final GlobalMetrics metrics;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public void start(DeviceEventError deviceEventError) {
        log.debug("Starting error handler service for {}", deviceEventError);
        final var name = MinioNameGenerator.generateName(deviceEventError.getReceivedAt(),
                deviceEventError.getErrorMeta().getErrorSource().name());
        final var bytes = JsonAvroConverter.toJsonAvro(deviceEventError);
        minioUploader.uploadToMinio(name, bytes, bucketName);
        metrics.recordProcessed(ErrorType.EVENTS_ERROR);
    }

}
