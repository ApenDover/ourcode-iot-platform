package ts.andrey.kafkaproducer.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ts.andrey.kafkaproducer.service.TokenService;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final TokenService tokenService;

    @GetMapping("/auth/token")
    public ResponseEntity<Map> getToken(
            @RequestParam("login") String login,
            @RequestParam("password") String password
    ) {
        return tokenService.getToken(login, password);
    }

}
