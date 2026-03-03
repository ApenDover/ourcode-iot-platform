package ts.andrey.orchestrator.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.openapitools.jackson.nullable.JsonNullable;
import ts.andrey.orchestrator.dto.Event;
import ts.andrey.orchestrator.dto.EventPage;

@Mapper(componentModel = "spring")
public interface EventMapper {

    Event toOrchestratorEventDto(ts.andrey.event.model.Event event);

    ts.andrey.event.model.Event toEventDto(Event event);

    EventPage toOrchestratorEventPageDto(ts.andrey.event.model.EventPage eventPage);

    ts.andrey.event.model.EventPage toEventPageDto(EventPage eventPage);

    default JsonNullable<String> map(String value) {
        return JsonNullable.of(value);
    }

}
