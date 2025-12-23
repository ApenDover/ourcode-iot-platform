package ts.andrey.orchestrator.infrastructure.adapter.out;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.orchestrator.application.port.RouterManagerGrpcPort;
import ts.andrey.orchestrator.application.port.RouterManagerPort;
import ts.andrey.orchestrator.dto.SendCommandRequest;

@Service
@RequiredArgsConstructor
public class RouterManagerAdapter implements RouterManagerPort {

    private final RouterManagerGrpcPort routerManagerGrpcPort;

    @Override
    @WithSpan("RouterManagerTransportSendCommand")
    public Integer sendCommand(String deviceId, String commandType, Object payload) {
        final var sendCommandRequest = new SendCommandRequest();
        sendCommandRequest.setCommandType(commandType);
        sendCommandRequest.setRouterSerial(deviceId);
        sendCommandRequest.setPayload(payload);
        final var response = routerManagerGrpcPort.sendCommand(sendCommandRequest);
        return response.getCreated();
    }

}
