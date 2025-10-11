package ts.andrey.orchestrator.out.adapter;

import io.grpc.ManagedChannel;
import org.springframework.stereotype.Component;
import ts.andrey.routermanager.Roma;
import ts.andrey.routermanager.RouterManagerServiceGrpc;

@Component
class RouterManagerGrpcAdapterImpl implements RouterManagerGrpcAdapter {

    private final RouterManagerServiceGrpc.RouterManagerServiceBlockingStub blockingStub;

    public RouterManagerGrpcAdapterImpl(ManagedChannel managedChannel) {
        this.blockingStub = RouterManagerServiceGrpc.newBlockingStub(managedChannel);
    }

    public Roma.SendCommandResponse sendCommand(Roma.SendCommandRequest request) {
        return blockingStub.sendCommand(request);
    }

    public Roma.PollCommandsResponse pollCommands(Roma.PollCommandsRequest request) {
        return blockingStub.pollCommands(request);
    }

    public Roma.AckCommandResponse ackCommand(Roma.AckCommandRequest request) {
        return blockingStub.ackCommand(request);
    }

}
