package ts.andrey.eventservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventFilterRequest {

    @NotNull
    private String deviceId;

    private Long fromTimestamp;

    private Long toTimestamp;

    private String type;

    private String pageToken;

}
