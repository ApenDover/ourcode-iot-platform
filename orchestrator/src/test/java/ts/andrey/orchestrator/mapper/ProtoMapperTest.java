package ts.andrey.orchestrator.mapper;

import org.junit.jupiter.api.Test;
import ts.andrey.orchestrator.infrastructure.mapper.ProtoMapper;
import ts.andrey.orchestrator.dto.SendCommandRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProtoMapperTest {

    @Test
    void toSendCommandRequestProto() {
        // GIVEN
        final var domainRequest = new SendCommandRequest();
        domainRequest.setCommandType("commandType");
        domainRequest.setRouterSerial("routerSerial");
        domainRequest.setPayload(Map.of("key", "value"));

        // WHEN TO PROTO
        final var actualProto = ProtoMapper.mapToSendCommandRequestProto(domainRequest);

        // THEN TO PROTO
        assertNotNull(actualProto);
        assertEquals("commandType", actualProto.getCommandType());
        assertEquals("routerSerial", actualProto.getRouterSerial());
        assertEquals("value", actualProto.getPayload().getFieldsOrThrow("key").getStringValue());

        // WHEN TO DTO
        final var actualDto = ProtoMapper.mapToSendCommandRequestDto(actualProto);

        // THEN TO DTO
        assertNotNull(actualDto);
        assertEquals("commandType", actualDto.getCommandType());
        assertEquals("routerSerial", actualDto.getRouterSerial());
        assertInstanceOf(Map.class, actualDto.getPayload());
        final var map = (Map<String, String>) actualDto.getPayload();
        assertEquals("value", map.get("key"));
    }

}
