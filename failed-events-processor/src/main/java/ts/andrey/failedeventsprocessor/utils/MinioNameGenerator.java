package ts.andrey.failedeventsprocessor.utils;

import lombok.experimental.UtilityClass;
import org.slf4j.MDC;
import ts.andrey.failedeventsprocessor.configuration.MdcInterceptor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
        elements.add(MDC.get(MdcInterceptor.TRACE));
        return String.join("_", elements) + ".json";
    }

}
