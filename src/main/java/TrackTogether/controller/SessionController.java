package TrackTogether.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

    @GetMapping("/session/keep-alive")
    public ResponseEntity<Void> keepAlive() {
        return ResponseEntity.noContent().build();
    }
}