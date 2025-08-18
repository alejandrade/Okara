package io.shrouded.okara.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {


    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        return Map.of(
            "application", "Okara",
            "version", "1.0.0",
            "framework", "Spring Boot 3.3.5",
            "frontend", "React TypeScript",
            "java", "21"
        );
    }
}