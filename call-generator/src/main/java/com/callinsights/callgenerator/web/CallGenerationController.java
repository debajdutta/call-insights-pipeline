package com.callinsights.callgenerator.web;

import com.callinsights.callgenerator.model.CallCompletedEvent;
import com.callinsights.callgenerator.service.CallGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calls")
public class CallGenerationController {

    private final CallGeneratorService callGeneratorService;

    public CallGenerationController(CallGeneratorService callGeneratorService) {
        this.callGeneratorService = callGeneratorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<CallCompletedEvent> generate() {
        return ResponseEntity.ok(callGeneratorService.generateCall());
    }
}
