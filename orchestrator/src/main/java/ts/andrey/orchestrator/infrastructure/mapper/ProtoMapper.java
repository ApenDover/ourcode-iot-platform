package ts.andrey.orchestrator.infrastructure.mapper;

import lombok.experimental.UtilityClass;
import ts.andrey.orchestrator.dto.AckCommandResponse;
import ts.andrey.orchestrator.dto.Command;
import ts.andrey.orchestrator.dto.PollCommandsResponse;
import ts.andrey.orchestrator.dto.SendCommandRequest;
import ts.andrey.orchestrator.dto.SendCommandResponse;
import ts.andrey.orchestrator.infrastructure.util.TimeUtils;
import ts.andrey.routermanager.Roma;

@UtilityClass
public class ProtoMapper {

    public Roma.SendCommandRequest mapToSendCommandRequestProto(SendCommandRequest sendCommandRequest) {
        final var payloadStruct = ProtoPayloadMapper.toStruct(sendCommandRequest.getPayload());
        return Roma.SendCommandRequest.newBuilder()
                .setRouterSerial(sendCommandRequest.getRouterSerial())
                .setCommandType(sendCommandRequest.getCommandType())
                .setPayload(payloadStruct)
                .build();
    }

    public SendCommandRequest mapToSendCommandRequestDto(Roma.SendCommandRequest sendCommandRequest) {
        final var request = new SendCommandRequest();
        request.setCommandType(sendCommandRequest.getCommandType());
        request.setRouterSerial(sendCommandRequest.getRouterSerial());
        request.setPayload(ProtoPayloadMapper.structToMap(sendCommandRequest.getPayload()));
        return request;
    }

    public Command mapToCommandDto(Roma.Command command) {
        final var commandDto = new Command();
        commandDto.setId(command.getId());
        commandDto.setCommandType(command.getCommandType());
        commandDto.setRouterSerial(command.getRouterSerial());
        commandDto.setStatus(command.getStatus());
        commandDto.setPayload(ProtoPayloadMapper.toStruct(command.getPayload()));
        commandDto.setAckedAt(TimeUtils.toOffsetDateTime(command.getAckedAt()));
        commandDto.setCreatedAt(TimeUtils.toOffsetDateTime(command.getCreatedAt()));
        commandDto.setSentAt(TimeUtils.toOffsetDateTime(command.getSentAt()));
        return commandDto;
    }

    public Roma.SendCommandResponse mapToSendCommandResponseProto(SendCommandResponse sendCommandResponse) {
        return Roma.SendCommandResponse.newBuilder()
                .setCreated(sendCommandResponse.getCreated())
                .build();
    }

    public SendCommandResponse mapToSendCommandResponseDto(Roma.SendCommandResponse sendCommandResponse) {
        final var response = new SendCommandResponse();
        response.setCreated(sendCommandResponse.getCreated());
        return response;
    }

    public static AckCommandResponse mapToAckCommandResponseDto(Roma.AckCommandResponse ackResponse) {
        final var response = new AckCommandResponse();
        response.setStatus(ackResponse.getStatus());
        return response;
    }

    public static PollCommandsResponse mapToPollCommandsResponseDto(Roma.PollCommandsResponse pollResponse) {
        final var commands = pollResponse.getCommandsList().stream()
                .map(ProtoMapper::mapToCommandDto)
                .toList();
        final var response = new PollCommandsResponse();
        response.setCommands(commands);
        return response;
    }

}
