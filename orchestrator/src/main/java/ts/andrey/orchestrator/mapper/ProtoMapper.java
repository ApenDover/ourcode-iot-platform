package ts.andrey.orchestrator.mapper;

import lombok.experimental.UtilityClass;
import ts.andrey.orchestrator.dto.SendCommandRequest;
import ts.andrey.orchestrator.utils.ProtoStructMapper;
import ts.andrey.routermanager.Roma;

@UtilityClass
public class ProtoMapper {

    public Roma.SendCommandRequest mapToSendCommandRequestProto(SendCommandRequest sendCommandRequest) {
        final var payloadStruct = ProtoStructMapper.toStruct(sendCommandRequest.getPayload());
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
        request.setPayload(ProtoStructMapper.fromStruct(sendCommandRequest.getPayload()));
        return request;
    }

}
