package ts.andrey.orchestrator.out.adapter;

import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.orchestrator.dto.SendCommandRequest;
import ts.andrey.orchestrator.utils.ProtoStructMapper;
import ts.andrey.routermanager.RouterManagerServiceGrpc;

@Service
@RequiredArgsConstructor
public class RouterManagerAdapter {

    private final ManagedChannel managedChannel;

    public void sendCommand(SendCommandRequest sendCommandRequest) {

        // создаём gRPC stub
        final var stub = RouterManagerServiceGrpc.newBlockingStub(managedChannel);

        final var payloadStruct = ProtoStructMapper.toStruct(sendCommandRequest.getPayload());

        // собираем protobuf-сообщение
//        final var commandRequest = Roma.SendCommandRequest.newBuilder()
//                .setRouterSerial(sendCommandRequest.getRouterSerial())
//                .setCommandType(sendCommandRequest.getCommandType())
//                .setPayload(payloadStruct)
//                .build();

        // отправляем команду
//        final var result = stub.sendCommand(commandRequest);

        // (при желании можно обработать result)
    }

}
