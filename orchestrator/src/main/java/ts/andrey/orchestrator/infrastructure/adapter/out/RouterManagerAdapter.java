package ts.andrey.orchestrator.infrastructure.adapter.out;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.orchestrator.dto.AckCommandRequest;
import ts.andrey.orchestrator.dto.AckCommandResponse;
import ts.andrey.orchestrator.dto.PollCommandsResponse;
import ts.andrey.orchestrator.dto.SendCommandRequest;
import ts.andrey.orchestrator.dto.SendCommandResponse;
import ts.andrey.orchestrator.infrastructure.grpc.RouterManagerGrpcClient;
import ts.andrey.orchestrator.infrastructure.mapper.ProtoMapper;
import ts.andrey.orchestrator.application.outport.RouterManagerPort;
import ts.andrey.orchestrator.infrastructure.util.ProtoStructMapper;
import ts.andrey.routermanager.Roma;

@Service
@RequiredArgsConstructor
public class RouterManagerAdapter implements RouterManagerPort {

    private final RouterManagerGrpcClient routerManagerGrpcClient;

    public SendCommandResponse sendCommand(SendCommandRequest sendCommandRequest) {

        final var payloadStruct = ProtoStructMapper.toStruct(sendCommandRequest.getPayload());

        final var commandRequest = Roma.SendCommandRequest.newBuilder()
                .setRouterSerial(sendCommandRequest.getRouterSerial())
                .setCommandType(sendCommandRequest.getCommandType())
                .setPayload(payloadStruct)
                .build();

        final var sendCommandResponse = routerManagerGrpcClient.sendCommand(commandRequest);
        return ProtoMapper.mapToSendCommandResponseDto(sendCommandResponse);
    }

    public AckCommandResponse ackCommand(AckCommandRequest ackCommandRequest) {

        final var ackRequest = Roma.AckCommandRequest.newBuilder()
                .setRouterSerial(ackCommandRequest.getRouterSerial())
                .setCommandId(ackCommandRequest.getCommandId())
                .build();

        final var ackResponse = routerManagerGrpcClient.ackCommand(ackRequest);
        return ProtoMapper.mapToAckCommandResponseDto(ackResponse);
    }

    public PollCommandsResponse pollCommands(String routerSerial) {

        final var pollRequest = Roma.PollCommandsRequest.newBuilder()
                .setRouterSerial(routerSerial)
                .build();

        final var pollResponse = routerManagerGrpcClient.pollCommands(pollRequest);
        return ProtoMapper.mapToPollCommandsResponseDto(pollResponse);
    }

}
