package ts.andrey.orchestrator.application.outport;

public interface RouterManagerPort {

    Integer sendCommand(String deviceId, String commandType, Object payload);

}
