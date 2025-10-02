package ts.andrey.eventservice.utils;

import lombok.experimental.UtilityClass;
import ts.andrey.eventservice.data.entity.DeviceEventEntity;
import ts.andrey.eventservice.model.EventFilterRequest;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@UtilityClass
public class PageUtil {

    public List<DeviceEventEntity> getPageableList(List<DeviceEventEntity> entities, EventFilterRequest request) {
        entities.sort(Comparator.comparing(deviceEventEntity -> deviceEventEntity.getKey().getTimestamp()));
        final var requestSize = request.getSize();
        final var requestPage = request.getPage();
        if (Objects.isNull(requestSize) || requestSize < 1) {
            return entities;
        }
        if (Objects.isNull(requestPage) || requestPage < 1) {
            return entities.subList(0, requestSize);
        }
        if (entities.size() < requestSize) {
            return entities;
        }
        final var pageCount = MathUtil.divideCeiling(entities.size(), requestSize);
        if (requestPage >= pageCount) {
            return entities.subList(requestSize * (pageCount - 1), entities.size());
        }
        final var startPosition = requestSize * (requestPage - 1);
        final var endPosition = requestSize * requestPage;
        return entities.subList(startPosition, endPosition);
    }

}
