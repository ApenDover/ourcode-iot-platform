package ts.andrey.orchestrator.out.port;

import ts.andrey.orchestrator.dto.AckCommandRequest;
import ts.andrey.orchestrator.dto.AckCommandResponse;
import ts.andrey.orchestrator.dto.PollCommandsResponse;
import ts.andrey.orchestrator.dto.SendCommandRequest;
import ts.andrey.orchestrator.dto.SendCommandResponse;

public interface RouterManagerPort {

    SendCommandResponse sendCommand(SendCommandRequest request);

    AckCommandResponse ackCommand(AckCommandRequest ackCommandRequest);

    PollCommandsResponse pollCommands(String routerSerial);

}
