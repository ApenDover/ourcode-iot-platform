package ts.andrey.failedeventsprocessor.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import ts.andrey.failedeventsprocessor.exception.FailedEventException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUploader {

    private static final String CONTENT_TYPE = "application/json";

    private final MinioClient minioClient;

    @Retryable(
            retryFor = DataAccessException.class,
            maxAttemptsExpression = "${minio.retry.max-attempts:3}",
            backoff = @Backoff(
                    delayExpression = "${minio.retry.initial-delay:1000}",
                    multiplierExpression = "${minio.retry.multiplier:5}",
                    random = true
            )
    )
    public void uploadToMinio(String fileName, byte[] bytes, String bucketName) {
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                        .contentType(CONTENT_TYPE)
                        .build()
            );
            log.info("Uploaded file: " + fileName);
        } catch (ServerException
                 | InsufficientDataException | ErrorResponseException | IOException
                 | NoSuchAlgorithmException | InvalidKeyException | InvalidResponseException
                 | XmlParserException | InternalException e) {
            log.error(e.getMessage(), e);
            throw new FailedEventException(e);
        }
    }

}
