package ts.andrey.devicecollector.testutils;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.OffsetDateTime;

@UtilityClass
public class DateUtil {

    public Instant getFromString(String date) {
        final var odt = OffsetDateTime.parse(
                date.replace(" ", "T")
        );
        return odt.toInstant();
    }

}
