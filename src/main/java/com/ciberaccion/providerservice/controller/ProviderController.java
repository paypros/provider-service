package com.ciberaccion.providerservice.controller;

import com.ciberaccion.providerservice.dto.CardValidationRequest;
import com.ciberaccion.providerservice.dto.CardValidationResponse;
import com.ciberaccion.providerservice.service.ProviderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/provider")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @PostMapping("/validate")
    public ResponseEntity<CardValidationResponse> validate(
            @RequestBody CardValidationRequest request) {
        return ResponseEntity.ok(providerService.validate(request));
    }
}