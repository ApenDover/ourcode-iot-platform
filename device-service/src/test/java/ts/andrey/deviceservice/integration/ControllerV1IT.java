package ts.andrey.deviceservice.integration;

import org.junit.jupiter.api.Test;
import ts.andrey.deviceservice.BaseIntegrationTest;
import ts.andrey.deviceservice.tdf.DummyTDF;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ControllerV1IT extends BaseIntegrationTest {

    @Test
    void createDevice() {
        // GIVEN
        final var request = DummyTDF.deviceCreateRequest.getDefault();

        // WHEN
        final var actual = sendPost("/api/v1/devices", request);

        // THEN
        assertNotNull(actual);
    }

}
