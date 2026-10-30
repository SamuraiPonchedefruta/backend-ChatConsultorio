package com.autobot.backend.auth.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminTestController {

    @GetMapping("/admin/recurso")
    public ResponseEntity<String> adminRecurso() {
        return ResponseEntity.ok("OK");
    }
}
