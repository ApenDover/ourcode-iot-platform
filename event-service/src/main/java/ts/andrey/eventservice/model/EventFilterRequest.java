package ts.andrey.eventservice.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    @Min(0)
    private Integer page = 0;

    @Min(1)
    @Max(1000)
    private Integer size = 100;

    public Pageable toPageable() {
        return PageRequest.of(
                this.page,
                this.size
        );
    }

}
