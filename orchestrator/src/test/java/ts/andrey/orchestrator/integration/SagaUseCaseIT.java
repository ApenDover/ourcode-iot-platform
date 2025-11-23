package ts.andrey.orchestrator.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.grpc.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ts.andrey.orchestrator.application.outport.RouterManagerGrpcPort;
import ts.andrey.orchestrator.dto.ApiV1DevicesDeviceIdVersionPost200Response;
import ts.andrey.orchestrator.dto.ApiV1DevicesDeviceIdVersionPost502Response;
import ts.andrey.orchestrator.dto.ApiV1DevicesDeviceIdVersionPostRequest;
import ts.andrey.orchestrator.dto.SendCommandResponse;
import ts.andrey.orchestrator.testutils.ReadJsonFileUtil;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SagaUseCaseIT extends BaseIntegrationTest {

    @Autowired
    private WireMockServer wireMockServer;

    @MockitoBean
    RouterManagerGrpcPort routerManagerGrpcPort;

    @AfterEach
    void tearDown() {
        wireMockServer.resetAll();
    }

    @Test
    void updateVersionSuccessCase() {
        //GIVEN
        final var request = new ApiV1DevicesDeviceIdVersionPostRequest();
        request.setTargetVersion("1.1.1");
        request.setIdempotencyKey("12345");

        final var deviceResponse = ReadJsonFileUtil.readStringFromFile("wiremock/device/SuccessGetDeviceResponse.json");
        stubFor(get(urlEqualTo("/api/v1/devices/DEV-001"))
                .withHeader("Authorization", containing("Bearer"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(deviceResponse))
        );

        final var deviceVersionResponse = ReadJsonFileUtil.readStringFromFile(
                "wiremock/device/SuccessPatchVersionResponse.json"
        );
        stubFor(patch(urlEqualTo("/api/v1/devices/DEV-001/version"))
                .withHeader("Authorization", containing("Bearer"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(deviceVersionResponse)
                )
        );

        final var grpcSendCommandResponse = new SendCommandResponse();
        grpcSendCommandResponse.setCreated(1);
        when(routerManagerGrpcPort.sendCommand(any())).thenReturn(grpcSendCommandResponse);

        //WHEN
        final var response = sendRequest("/api/v1/devices/DEV-001/version",
                HttpMethod.POST, request, ApiV1DevicesDeviceIdVersionPost200Response.class);


        //THEN
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        final var body = response.getBody();
        assertEquals("DEV-001", body.getDeviceId());
        assertEquals("0.0.0", body.getPrevVersion());
        assertEquals("1.1.1", body.getTargetVersion());
        assertEquals("1", body.getCommandId());

        // REDIS TEST

        //WHEN
        final var redisResponse = sendRequest("/api/v1/devices/DEV-001/version",
                HttpMethod.POST, request, ApiV1DevicesDeviceIdVersionPost200Response.class);

        //THEN
        assertTrue(redisResponse.getStatusCode().is2xxSuccessful());
        assertNotNull(redisResponse.getBody());
        final var redisBody = redisResponse.getBody();
        assertEquals("DEV-001", redisBody.getDeviceId());
        assertEquals("0.0.0", redisBody.getPrevVersion());
        assertEquals("1.1.1", redisBody.getTargetVersion());
        assertEquals("1", redisBody.getCommandId());

        verify(1, getRequestedFor(urlEqualTo("/api/v1/devices/DEV-001")));
        verify(1, patchRequestedFor(urlEqualTo("/api/v1/devices/DEV-001/version")));

        verify(routerManagerGrpcPort, times(1)).sendCommand(any());
        verifyNoMoreInteractions(routerManagerGrpcPort);
    }

    @Test
    void updateVersionRollbackCaseWithCompensation() {
        //GIVEN
        final var request = new ApiV1DevicesDeviceIdVersionPostRequest();
        request.setTargetVersion("2.0.0");
        request.setIdempotencyKey("12345");

        final var deviceResponse = ReadJsonFileUtil.readStringFromFile("wiremock/device/SuccessGetDeviceResponse.json");
        stubFor(get(urlEqualTo("/api/v1/devices/DEV-001"))
                .withHeader("Authorization", containing("Bearer"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(deviceResponse))
        );

        final var deviceVersionResponse = ReadJsonFileUtil.readStringFromFile(
                "wiremock/device/SuccessPatchVersionResponse.json"
        );
        stubFor(patch(urlEqualTo("/api/v1/devices/DEV-001/version"))
                .withHeader("Authorization", containing("Bearer"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(deviceVersionResponse)
                )
        );

        final var grpcException = Status.INTERNAL
                .withDescription("failed to create command")
                .withCause(new RuntimeException("Database connection failed"))
                .asRuntimeException();

        when(routerManagerGrpcPort.sendCommand(any())).thenThrow(grpcException);

        final var rollbackVersionResponse = ReadJsonFileUtil.readStringFromFile(
                "wiremock/device/SuccessRollbackVersionResponse.json"
        );
        stubFor(post(urlEqualTo("/api/v1/devices/DEV-001/version/rollback"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(rollbackVersionResponse)
                )
        );

        //WHEN
        final var response = sendRequest("/api/v1/devices/DEV-001/version",
                HttpMethod.POST, request, ApiV1DevicesDeviceIdVersionPost502Response.class);


        //THEN
        assertTrue(response.getStatusCode().is5xxServerError());
        assertNotNull(response.getBody());
        final var body = response.getBody();
        assertEquals("ROUTER_MANAGER_FAILED", body.getError());
        assertTrue(body.getCompensated());

        verify(1, getRequestedFor(urlEqualTo("/api/v1/devices/DEV-001")));
        verify(1, patchRequestedFor(urlEqualTo("/api/v1/devices/DEV-001/version")));
        verify(1, postRequestedFor(urlEqualTo("/api/v1/devices/DEV-001/version/rollback")));

        verify(routerManagerGrpcPort, times(1)).sendCommand(any());
        verifyNoMoreInteractions(routerManagerGrpcPort);
    }

    @Test
    void updateVersionRollbackCaseNoCompensation() {
        //GIVEN
        final var request = new ApiV1DevicesDeviceIdVersionPostRequest();
        request.setTargetVersion("2.0.0");
        request.setIdempotencyKey("12345");

        final var deviceResponse = ReadJsonFileUtil.readStringFromFile("wiremock/device/SuccessGetDeviceResponse.json");
        stubFor(get(urlEqualTo("/api/v1/devices/DEV-001"))
                .withHeader("Authorization", containing("Bearer"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(deviceResponse))
        );

        final var deviceVersionResponse = ReadJsonFileUtil.readStringFromFile(
                "wiremock/device/SuccessPatchVersionResponse.json"
        );
        stubFor(patch(urlEqualTo("/api/v1/devices/DEV-001/version"))
                .withHeader("Authorization", containing("Bearer"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(deviceVersionResponse)
                )
        );

        final var grpcException = Status.INTERNAL
                .withDescription("failed to create command")
                .withCause(new RuntimeException("Database connection failed"))
                .asRuntimeException();

        when(routerManagerGrpcPort.sendCommand(any())).thenThrow(grpcException);

        stubFor(post(urlEqualTo("/api/v1/devices/DEV-001/version/rollback"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                )
        );

        //WHEN
        final var response = sendRequest("/api/v1/devices/DEV-001/version",
                HttpMethod.POST, request, ApiV1DevicesDeviceIdVersionPost502Response.class);


        //THEN
        assertTrue(response.getStatusCode().is5xxServerError());
        assertNotNull(response.getBody());
        final var body = response.getBody();
        assertEquals("ROUTER_MANAGER_FAILED", body.getError());
        assertFalse(body.getCompensated());

        verify(1, getRequestedFor(urlEqualTo("/api/v1/devices/DEV-001")));
        verify(1, patchRequestedFor(urlEqualTo("/api/v1/devices/DEV-001/version")));
        verify(1, postRequestedFor(urlEqualTo("/api/v1/devices/DEV-001/version/rollback")));

        verify(routerManagerGrpcPort, times(1)).sendCommand(any());
        verifyNoMoreInteractions(routerManagerGrpcPort);
    }

}
