package ts.andrey.deviceservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import ts.andrey.deviceservice.BaseIntegrationTest;
import ts.andrey.deviceservice.tdf.DummyTDF;
import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceStatus;
import ts.andrey.dto.DeviceVersionResponse;

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
        assertEquals("1.0.0", body.getVersion());
        assertEquals(0, body.getEtag());

        // THEN REDIS
        final var redisActual = redisTemplate.opsForValue().get(body.getDeviceId());
        assertNotNull(redisActual);
        assertEquals("deviceType", redisActual.getDeviceType());
        assertEquals("meta", redisActual.getMeta());
        assertEquals(body.getDeviceId(), redisActual.getDeviceId());
        assertEquals(body.getCreatedAt(), redisActual.getCreatedAt());
        assertEquals(body.getVersion(), redisActual.getVersion());
        assertEquals(body.getEtag(), redisActual.getEtag());
        assertEquals(body.getMeta(), redisActual.getMeta());
        assertEquals(body.getStatus().toString(), redisActual.getStatus().toString());

        // THEN DATABASE
        final var actual = deviceRepository.findAll()
                .stream()
                .filter(device -> device.getDeviceId()
                        .equals(body.getDeviceId()))
                .findFirst().orElse(null);

        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertNotNull(actual.getCreatedAt());
        assertNotNull(actual.getEtag());
        assertNotNull(actual.getVersion());
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
        assertNotNull(actual.getEtag());
        assertNotNull(actual.getVersion());
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
        assertEquals(body.getVersion(), redisActual.getVersion());
        assertEquals(body.getEtag(), redisActual.getEtag());
    }

    @Test
    void updateVersionDevice() {
        // WHEN
        final var request = DummyTDF.deviceVersionUpdateRequest.getDefault();
        final var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final var entity = new HttpEntity<>(request, headers);

        final var response = sendRequest("/api/v1/devices/DEV-001/version", HttpMethod.PATCH, entity, DeviceVersionResponse.class);

        // THEN ASSERT RESPONSE
        final var body = response.getBody();

        assertNotNull(response);
        assertNotNull(body);
        assertEquals("DEV-001", body.getDeviceId());
        assertEquals(1L, body.getEtag());
        assertEquals("0.0.1", body.getPrevVersion());
        assertEquals("2.1.1", body.getTargetVersion());
        assertEquals(DeviceStatus.UPDATING, body.getStatus());

        // THEN ASSERT DATABASE ENTITY
        final var actualOpt = deviceRepository.findByDeviceId("DEV-001");
        assertTrue(actualOpt.isPresent());

        final var actual = actualOpt.get();
        assertNotNull(actual.getId());
        assertNotNull(actual.getCreatedAt());
        assertEquals("DEV-001", actual.getDeviceId());
        assertEquals(1L, actual.getEtag());
        assertEquals("2.1.1", actual.getVersion());
        assertEquals(DeviceStatus.UPDATING, actual.getStatus());

        //THEN REDIS
        final var redisActual = redisTemplate.opsForValue().get(body.getDeviceId());
        assertNotNull(redisActual);
        assertNotNull(redisActual.getCreatedAt());
        assertEquals("DEV-001", redisActual.getDeviceId());
        assertEquals(1L, redisActual.getEtag());
        assertEquals("2.1.1", redisActual.getVersion());
        assertEquals(DeviceStatus.UPDATING, redisActual.getStatus());
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
        assertEquals(0, body.getEtag());
        assertEquals("0.0.1", body.getVersion());
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
