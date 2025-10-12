package ts.andrey.orchestrator.infrastructure.config;

import feign.Client;
import feign.Request;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import ts.andrey.orchestrator.infrastructure.util.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class LoggingFeignClient extends Client.Default {


    public LoggingFeignClient() {
        super(null, null);
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        if (request.httpMethod().equals(Request.HttpMethod.POST)) {
            log.info("Request: {}", new String(request.body(), StandardCharsets.UTF_8));
        } else {
            log.info("GET request to {}", request.url());
        }

        try (var response = super.execute(request, options)) {
            final var bodyStream = response.body().asInputStream();
            final var responseBody = StreamUtils.copyToString(bodyStream, StandardCharsets.UTF_8);
            final var unformattedJson = JsonUtils.minifyJson(responseBody);
            log.debug("Response status: {}, headers: {}, body: {}", response.status(), response.headers(), unformattedJson);
            return response.toBuilder().body(responseBody, StandardCharsets.UTF_8).build();
        }
    }

}
