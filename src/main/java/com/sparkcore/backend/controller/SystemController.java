package com.sparkcore.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")

public class SystemController {
    @GetMapping("/ping")
    public String ping() {
        return "SparkCore System ist online und bereit für die FI!";
    }
}
