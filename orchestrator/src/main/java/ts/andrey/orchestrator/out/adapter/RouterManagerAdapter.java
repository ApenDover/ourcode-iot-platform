package ts.andrey.orchestrator.out.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.orchestrator.dto.AckCommandRequest;
import ts.andrey.orchestrator.dto.AckCommandResponse;
import ts.andrey.orchestrator.dto.PollCommandsResponse;
import ts.andrey.orchestrator.dto.SendCommandRequest;
import ts.andrey.orchestrator.dto.SendCommandResponse;
import ts.andrey.orchestrator.mapper.ProtoMapper;
import ts.andrey.orchestrator.out.port.RouterManagerGrpcPort;
import ts.andrey.orchestrator.out.port.RouterManagerPort;
import ts.andrey.orchestrator.utils.ProtoStructMapper;
import ts.andrey.routermanager.Roma;

@Service
@RequiredArgsConstructor
public class RouterManagerAdapter implements RouterManagerPort {

    private final RouterManagerGrpcPort routerManagerGrpcPort;

    public SendCommandResponse sendCommand(SendCommandRequest sendCommandRequest) {

        final var payloadStruct = ProtoStructMapper.toStruct(sendCommandRequest.getPayload());

        final var commandRequest = Roma.SendCommandRequest.newBuilder()
                .setRouterSerial(sendCommandRequest.getRouterSerial())
                .setCommandType(sendCommandRequest.getCommandType())
                .setPayload(payloadStruct)
                .build();

        final var sendCommandResponse = routerManagerGrpcPort.sendCommand(commandRequest);
        return ProtoMapper.mapToSendCommandResponseDto(sendCommandResponse);
    }

    public AckCommandResponse ackCommand(AckCommandRequest ackCommandRequest) {

        final var ackRequest = Roma.AckCommandRequest.newBuilder()
                .setRouterSerial(ackCommandRequest.getRouterSerial())
                .setCommandId(ackCommandRequest.getCommandId())
                .build();

        final var ackResponse = routerManagerGrpcPort.ackCommand(ackRequest);
        return ProtoMapper.mapToAckCommandResponseDto(ackResponse);
    }

    public PollCommandsResponse pollCommands(String routerSerial) {

        final var pollRequest = Roma.PollCommandsRequest.newBuilder()
                .setRouterSerial(routerSerial)
                .build();

        final var pollResponse = routerManagerGrpcPort.pollCommands(pollRequest);
        return ProtoMapper.mapToPollCommandsResponseDto(pollResponse);
    }

}
