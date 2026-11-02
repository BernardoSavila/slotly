package com.bernardoavila.slotly.shared;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class IndexController {

    @GetMapping("/")
    public Map<String, Object> index() {
        return Map.of(
                "name", "Slotly",
                "description", "Resource booking API — prevents overlapping bookings",
                "repository", "https://github.com/BernardoSavila/slotly",
                "authentication", "Bearer token required on all endpoints except /api/auth/**",
                "endpoints", Map.of(
                        "register", "POST /api/auth/register",
                        "login", "POST /api/auth/login",
                        "resources", "GET /api/resources",
                        "bookings", "GET /api/bookings"
                )
        );
    }
}