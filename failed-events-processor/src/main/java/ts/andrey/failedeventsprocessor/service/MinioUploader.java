package ts.andrey.failedeventsprocessor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioUploader {

    private static final String CONTENT_TYPE = "application/json";

    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;

    public void uploadToMinio(String fileName, Object object, String bucketName) throws Exception {
        final var json = objectMapper.writeValueAsString(object);
        final var bytes = json.getBytes(StandardCharsets.UTF_8);

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                        .contentType(CONTENT_TYPE)
                        .build()
        );
        log.info("Uploaded file: " + fileName);
    }

}
