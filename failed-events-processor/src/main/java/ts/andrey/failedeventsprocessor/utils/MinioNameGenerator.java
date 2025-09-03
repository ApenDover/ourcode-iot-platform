package ts.andrey.failedeventsprocessor.utils;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class MinioNameGenerator {

    private static final String DEVICE_EVENT_ERROR = "event";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String generateName(long receivedAt, String errorSource) {
        List<String> elements = new ArrayList<>();
        LocalDate date = Instant.ofEpochMilli(receivedAt)
                .atZone(ZoneId.systemDefault()).toLocalDate();

        elements.add(date.format(DATE_FMT));
        elements.add(DEVICE_EVENT_ERROR);
        elements.add(errorSource);
        elements.add(String.valueOf(receivedAt));
        elements.add(UUID.randomUUID().toString());
        return String.join("_", elements) + ".json";
    }

}
