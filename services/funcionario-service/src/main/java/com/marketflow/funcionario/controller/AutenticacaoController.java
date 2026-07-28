package com.marketflow.funcionario.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.marketflow.funcionario.service.AutenticacaoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Validated
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    public AutenticacaoController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @PostMapping("/login")
    public AutenticacaoDtos.LoginResponse login(
            @Valid @RequestBody AutenticacaoDtos.LoginRequest request
    ) {
        return AutenticacaoDtos.LoginResponse.from(
                autenticacaoService.login(request.matricula(), request.senha())
        );
    }

    @GetMapping("/validar")
    public AutenticacaoDtos.ValidacaoResponse validar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(value = "token", required = false) String token
    ) {
        return AutenticacaoDtos.ValidacaoResponse.from(
                autenticacaoService.validar(extrairToken(authorization, token))
        );
    }

    private String extrairToken(String authorization, String token) {
        if (authorization != null && !authorization.isBlank()) {
            if (authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return authorization.substring(7).trim();
            }
            return authorization.trim();
        }
        return token;
    }
}
