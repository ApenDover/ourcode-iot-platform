package ts.andrey.orchestrator.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApiEndpoint {

    // Device Service endpoints
    GET_DEVICES("GET", "/api/v1/devices", "Получить список устройств"),
    CREATE_DEVICE("POST", "/api/v1/devices", "Создать устройство"),
    GET_DEVICE_BY_ID("GET", "/api/v1/devices/{deviceId}", "Получить устройство по ID"),
    UPDATE_DEVICE("PUT", "/api/v1/devices/{deviceId}", "Обновить устройство"),
    DELETE_DEVICE("DELETE", "/api/v1/devices/{deviceId}", "Удалить устройство"),

    // Event Service endpoints
    GET_EVENTS("GET", "/api/v1/events", "Получить список событий по устройству и фильтрам"),
    GET_EVENT_BY_ID("GET", "/api/v1/events/{event_id}", "Получить событие по event_id и device_id"),

    // Router Manager endpoints
    SEND_COMMAND("POST", "/api/v1/commands", "Отправить команду устройству"),
    POLL_COMMANDS("GET", "/api/v1/commands/poll", "Получить команды для устройства"),
    ACK_COMMAND("POST", "/api/v1/commands/ack", "Подтвердить выполнение команды"),

    // Orchestrator-specific endpoints
    UPDATE_DEVICE_VERSION("POST", "/api/v1/devices/{deviceId}/version", "Обновить версию устройства (сага с компенсацией)");

    private final String method;
    private final String path;
    private final String description;

    @Override
    public String toString() {
        return String.format("%s %s - %s", method, path, description);
    }

}
