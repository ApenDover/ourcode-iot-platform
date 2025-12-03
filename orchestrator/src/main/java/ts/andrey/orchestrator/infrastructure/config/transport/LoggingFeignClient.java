package ts.andrey.orchestrator.infrastructure.config.transport;

import feign.Client;
import feign.Request;
import feign.Response;
import feign.httpclient.ApacheHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import ts.andrey.orchestrator.domain.exception.OrchestratorException;
import ts.andrey.orchestrator.infrastructure.util.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LoggingFeignClient implements Client {

    private final Client delegate;

    public LoggingFeignClient() {
        this.delegate = new ApacheHttpClient();
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        if (request.httpMethod().equals(Request.HttpMethod.POST)) {
            List<String> heads = new ArrayList<String>();
            request.headers().forEach((key, values) ->
                    heads.add(key + ": " + String.join(",", values))
            );
            log.info("Request:{}, {}", heads, new String(request.body(), StandardCharsets.UTF_8));
        } else {
            List<String> heads = new ArrayList<String>();
            request.headers().forEach((key, values) ->
                    heads.add(key + ": " + String.join(",", values))
            );
            log.info("GET request to {}, headers: {}", request.url(), request.headers());
        }

        try (var response = delegate.execute(request, options)) {
            if  (response.status() != HttpStatus.OK.value()) {
                log.debug(
                        "Response status: {}, headers: {}",
                        response.status(), response.headers());
                throw new OrchestratorException(String.valueOf(response.status()));
            }
            final var bodyStream = response.body().asInputStream();
            final var responseBody = StreamUtils.copyToString(bodyStream, StandardCharsets.UTF_8);
            final var unformattedJson = JsonUtils.minifyJson(responseBody);
            log.debug(
                    "Response status: {}, headers: {}, body: {}",
                    response.status(), response.headers(), unformattedJson
            );
            return response.toBuilder().body(responseBody, StandardCharsets.UTF_8).build();
        }
    }

}
