package ts.andrey.failedeventsprocessor.testutils;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.InputStream;

@UtilityClass
public class MinioFileReader {

    @SneakyThrows
    public String getFile(MinioClient minioClient, String bucketName, String fileName) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build())) {
            byte[] buffer = stream.readAllBytes();
            return new String(buffer);
        }
    }

}
