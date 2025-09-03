package ts.andrey.failedeventsprocessor.service;

import com.nashkod.avro.DeviceError;
import com.nashkod.avro.DeviceEventError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ts.andrey.failedeventsprocessor.utils.JsonAvroConverter;
import ts.andrey.failedeventsprocessor.utils.MinioNameGenerator;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorHandlerService {

    private final MinioUploader minioUploader;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public void start(DeviceError deviceError) {
        log.debug("Starting error handler service for {}", deviceError);
        final var name = MinioNameGenerator.generateName(deviceError.getReceivedAt(),
                deviceError.getErrorMeta().getErrorSource());
        final var bytes = JsonAvroConverter.toJsonAvro(deviceError);
        minioUploader.uploadToMinio(name, bytes, bucketName);
    }

    public void start(DeviceEventError deviceEventError) {
        log.debug("Starting error handler service for {}", deviceEventError);
        final var name = MinioNameGenerator.generateName(deviceEventError.getReceivedAt(),
                deviceEventError.getErrorMeta().getErrorSource());
        final var bytes = JsonAvroConverter.toJsonAvro(deviceEventError);
        minioUploader.uploadToMinio(name, bytes, bucketName);
    }

}
