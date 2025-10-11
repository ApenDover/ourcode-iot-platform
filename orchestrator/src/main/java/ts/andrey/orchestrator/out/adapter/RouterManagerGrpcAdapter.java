package ts.andrey.orchestrator.out.adapter;

import ts.andrey.routermanager.Roma;

interface RouterManagerGrpcAdapter {

    Roma.SendCommandResponse sendCommand(Roma.SendCommandRequest request);

    Roma.AckCommandResponse ackCommand(Roma.AckCommandRequest ackCommandRequest);

    Roma.PollCommandsResponse pollCommands(Roma.PollCommandsRequest request);

}
