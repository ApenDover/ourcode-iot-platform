package ts.andrey.orchestrator.application.port;

public interface RouterManagerPort {

    Integer sendCommand(String deviceId, String commandType, Object payload);

}
