package ts.andrey.eventservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseError {

    private String title;
    private Integer status;
    private String detail;
    private String instance;
    private String trace;

}
