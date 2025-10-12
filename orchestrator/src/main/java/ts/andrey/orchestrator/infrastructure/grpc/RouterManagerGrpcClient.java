package ts.andrey.orchestrator.infrastructure.grpc;

import ts.andrey.routermanager.Roma;

public interface RouterManagerGrpcClient {

    Roma.SendCommandResponse sendCommand(Roma.SendCommandRequest request);

    Roma.AckCommandResponse ackCommand(Roma.AckCommandRequest ackCommandRequest);

    Roma.PollCommandsResponse pollCommands(Roma.PollCommandsRequest request);

}
