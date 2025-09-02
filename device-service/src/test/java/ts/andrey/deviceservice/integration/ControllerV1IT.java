package ts.andrey.deviceservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import ts.andrey.deviceservice.BaseIntegrationTest;
import ts.andrey.deviceservice.tdf.DummyTDF;
import ts.andrey.dto.Device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerV1IT extends BaseIntegrationTest {

    @Test
    void createDevice() {
        // GIVEN
        final var request = DummyTDF.deviceCreateRequest.getDefault();

        // WHEN
        final var response = sendPost("/api/v1/devices", request, Device.class);

        assertTrue(response.getStatusCode().is2xxSuccessful());

        // THEN
        final var body = response.getBody();

        assertNotNull(response);
        assertNotNull(body);
        assertNotNull(body.getDeviceId());
        assertNotNull(body.getCreatedAt());
        assertEquals("meta", body.getMeta());
        assertEquals("deviceType", body.getDeviceType());

        // THEN REDIS
        final var redisActual = redisTemplate.opsForValue().get(body.getDeviceId());
        assertNotNull(redisActual);
        assertEquals("deviceType", redisActual.getDeviceType());
        assertEquals("meta", redisActual.getMeta());
        assertEquals(body.getDeviceId(), redisActual.getDeviceId());
        assertEquals(body.getCreatedAt(), redisActual.getCreatedAt());

        // THEN DATABASE
        final var actual = deviceRepository.findAll()
                .stream()
                .filter(device -> device.getDeviceId()
                        .equals(body.getDeviceId()))
                .findFirst().orElse(null);

        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertNotNull(actual.getCreatedAt());
        assertEquals(body.getDeviceId(), actual.getDeviceId());
        assertEquals("deviceType", actual.getDeviceType());
        assertEquals("meta", actual.getMeta());
    }

    @Test
    void updateDevice() {
        // WHEN
        final var update = DummyTDF.device.getForUpdate();
        final var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final var entity = new HttpEntity<>(update, headers);

        final var response = sendRequest("/api/v1/devices/DEV-001", HttpMethod.PUT, entity, Device.class);

        // THEN ASSERT RESPONSE
        final var body = response.getBody();

        assertNotNull(response);
        assertNotNull(body);
        assertNotNull(body.getDeviceId());
        assertNotNull(body.getCreatedAt());
        assertEquals("updatedMeta", body.getMeta());
        assertEquals("updatedType", body.getDeviceType());

        // THEN ASSERT DATABASE ENTITY
        final var actualOpt = deviceRepository.findByDeviceId("DEV-001");
        assertTrue(actualOpt.isPresent());

        final var actual = actualOpt.get();
        assertNotNull(actual.getId());
        assertNotNull(actual.getCreatedAt());
        assertEquals(body.getDeviceId(), actual.getDeviceId());
        assertEquals("updatedType", actual.getDeviceType());
        assertEquals("updatedMeta", actual.getMeta());

        //THEN REDIS
        final var redisActual = redisTemplate.opsForValue().get(body.getDeviceId());
        assertNotNull(redisActual);
        assertEquals("updatedType", redisActual.getDeviceType());
        assertEquals("updatedMeta", redisActual.getMeta());
        assertEquals(body.getDeviceId(), redisActual.getDeviceId());
        assertEquals(body.getCreatedAt(), redisActual.getCreatedAt());
    }

    @Test
    void getDevice() {
        // WHEN
        final var response = sendRequest("/api/v1/devices/DEV-002", HttpMethod.GET, null, Device.class);

        // THEN
        assertTrue(response.getStatusCode().is2xxSuccessful());
        final var body = response.getBody();
        assertNotNull(body);
        assertEquals("DEV-002", body.getDeviceId());
        assertEquals("SENSOR", body.getDeviceType());
        assertEquals(1756365012345L, body.getCreatedAt());
        assertEquals("meta-text-two", body.getMeta());
    }

    @Test
    void deleteDevice() {
        //GIVEN
        final var actualOptBefore = deviceRepository.findByDeviceId("DEV-DELETE");
        assertTrue(actualOptBefore.isPresent());

        // WHEN
        final var response = sendRequest("/api/v1/devices/DEV-DELETE", HttpMethod.DELETE, null, null);

        // THEN
        assertTrue(response.getStatusCode().is2xxSuccessful());

        final var actualOpt = deviceRepository.findByDeviceId("DEV-DELETE");
        assertTrue(actualOpt.isEmpty());
    }

}
