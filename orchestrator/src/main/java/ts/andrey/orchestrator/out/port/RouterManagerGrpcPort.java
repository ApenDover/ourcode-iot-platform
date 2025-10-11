package ts.andrey.orchestrator.out.port;

import ts.andrey.routermanager.Roma;

public interface RouterManagerGrpcPort {

    Roma.SendCommandResponse sendCommand(Roma.SendCommandRequest request);

    Roma.AckCommandResponse ackCommand(Roma.AckCommandRequest ackCommandRequest);

    Roma.PollCommandsResponse pollCommands(Roma.PollCommandsRequest request);

}
