package ts.andrey.orchestrator.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ts.andrey.orchestrator.dto.AckCommandRequest;
import ts.andrey.orchestrator.dto.AckCommandResponse;
import ts.andrey.orchestrator.dto.PollCommandsResponse;
import ts.andrey.orchestrator.dto.SendCommandRequest;
import ts.andrey.orchestrator.dto.SendCommandResponse;
import ts.andrey.orchestrator.out.port.RouterManagerPort;

@Component
@RequiredArgsConstructor
public class RouterManagerProxyService implements RouterManagerPort {

    private final RouterManagerPort routerManagerAdapter;

    @Override
    public SendCommandResponse sendCommand(SendCommandRequest request) {
        return routerManagerAdapter.sendCommand(request);
    }

    @Override
    public AckCommandResponse ackCommand(AckCommandRequest ackCommandRequest) {
        return routerManagerAdapter.ackCommand(ackCommandRequest);
    }

    @Override
    public PollCommandsResponse pollCommands(String routerSerial) {
        return routerManagerAdapter.pollCommands(routerSerial);
    }


}
