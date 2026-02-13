package ts.andrey.orchestrator.infrastructure.adapter.out;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.orchestrator.application.port.RouterManagerGrpcPort;
import ts.andrey.orchestrator.dto.*;
import ts.andrey.orchestrator.infrastructure.mapper.ProtoMapper;
import ts.andrey.orchestrator.infrastructure.mapper.ProtoPayloadMapper;
import ts.andrey.orchestrator.infrastructure.out.port.grpc.RouterManagerGrpcClient;
import ts.andrey.orchestrator.infrastructure.util.RequestTimerUtil;
import ts.andrey.routermanager.Roma;

@Service
@RequiredArgsConstructor
public class RouterManagerGrpcAdapter implements RouterManagerGrpcPort {

    private final RouterManagerGrpcClient routerManagerGrpcClient;
    private final RequestTimerUtil requestTimerUtil;

    @WithSpan("RouterManagerTransportCommand")
    public SendCommandResponse sendCommand(SendCommandRequest sendCommandRequest) {
        return requestTimerUtil.recordExternal("router-manager-service", "sendCommand", "grpc", () -> {
            final var payloadStruct = ProtoPayloadMapper.toStruct(sendCommandRequest.getPayload());
            final var commandRequest = Roma.SendCommandRequest.newBuilder()
                    .setRouterSerial(sendCommandRequest.getRouterSerial())
                    .setCommandType(sendCommandRequest.getCommandType())
                    .setPayload(payloadStruct)
                    .build();

            final var sendCommandResponse = routerManagerGrpcClient.sendCommand(commandRequest);
            return ProtoMapper.mapToSendCommandResponseDto(sendCommandResponse);
        });
    }

    @WithSpan("RouterManagerTransportAck")
    public AckCommandResponse ackCommand(AckCommandRequest ackCommandRequest) {
        return requestTimerUtil.recordExternal("router-manager-service", "ackCommand", "grpc", () -> {
            final var ackRequest = Roma.AckCommandRequest.newBuilder()
                    .setRouterSerial(ackCommandRequest.getRouterSerial())
                    .setCommandId(ackCommandRequest.getCommandId())
                    .build();

            final var ackResponse = routerManagerGrpcClient.ackCommand(ackRequest);
            return ProtoMapper.mapToAckCommandResponseDto(ackResponse);
        });
    }

    @WithSpan("RouterManagerTransportPoll")
    public PollCommandsResponse pollCommands(String routerSerial) {
        return requestTimerUtil.recordExternal("router-manager-service", "pollCommands", "grpc", () -> {
            final var pollRequest = Roma.PollCommandsRequest.newBuilder()
                    .setRouterSerial(routerSerial)
                    .build();

            final var pollResponse = routerManagerGrpcClient.pollCommands(pollRequest);
            return ProtoMapper.mapToPollCommandsResponseDto(pollResponse);
        });
    }

}
